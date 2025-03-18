/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.descriptive;

/**
 * Computes the sum of squared deviations from the sample mean. This
 * statistic is related to the second moment.
 *
 * <p>The following recursive updating formula is used:
 * <p>Let
 * <ul>
 *  <li> dev = (current obs - previous mean) </li>
 *  <li> n = number of observations (including current obs) </li>
 * </ul>
 * <p>Then
 * <p>new value = old value + dev^2 * (n - 1) / n
 * <p>returns the sum of squared deviations of all values seen so far.
 *
 * <p>Supports up to 2<sup>63</sup> (exclusive) observations.
 * This implementation does not check for overflow of the count.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link StatisticAccumulator#combine(StatisticResult) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.DoubleConsumer#accept(double) accept}
 * and {@link StatisticAccumulator#combine(StatisticResult) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * <p>References:
 * <ul>
 *   <li>Chan, Golub and Levesque (1983)
 *       Algorithms for Computing the Sample Variance: Analysis and Recommendations.
 *       American Statistician, 37, 242-247.
 *       <a href="https://doi.org/10.2307/2683386">doi: 10.2307/2683386</a>
 * </ul>
 *
 * @since 1.1
 */
class SumOfSquaredDeviations extends FirstMoment {
    /** Sum of squared deviations of the values that have been added. */
    protected double sumSquaredDev;

    /**
     * Create an instance.
     */
    SumOfSquaredDeviations() {
        // No-op
    }

    /**
     * Copy constructor.
     *
     * @param source Source to copy.
     */
    SumOfSquaredDeviations(SumOfSquaredDeviations source) {
        super(source);
        sumSquaredDev = source.sumSquaredDev;
    }

    /**
     * Create an instance with the given sum of squared deviations and first moment.
     *
     * @param sumSquaredDev Sum of squared deviations.
     * @param m1 First moment.
     */
    private SumOfSquaredDeviations(double sumSquaredDev, FirstMoment m1) {
        super(m1);
        this.sumSquaredDev = sumSquaredDev;
    }

    /**
     * Create an instance with the given sum of squared deviations and first moment.
     *
     * <p>This constructor is used when creating the moment from integer values.
     *
     * @param sumSquaredDev Sum of squared deviations.
     * @param m1 First moment.
     * @param n Count of values.
     */
    SumOfSquaredDeviations(double sumSquaredDev, double m1, long n) {
        super(m1, n);
        this.sumSquaredDev = sumSquaredDev;
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code SumOfSquaredDeviations} computed using {@link #accept accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code SumOfSquaredDeviations} instance.
     */
    static SumOfSquaredDeviations of(double... values) {
        if (values.length == 0) {
            return new SumOfSquaredDeviations();
        }
        return create(FirstMoment.of(values), values, 0, values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code SumOfSquaredDeviations} computed using {@link #accept accept} may be
     * different from this instance.
     *
     * <p>Warning: No range checks are performed.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfSquaredDeviations} instance.
     */
    static SumOfSquaredDeviations ofRange(double[] values, int from, int to) {
        if (from == to) {
            return new SumOfSquaredDeviations();
        }
        return create(FirstMoment.ofRange(values, from, to), values, from, to);
    }

    /**
     * Creates the sum of squared deviations.
     *
     * <p>Uses the provided {@code sum} to create the first moment.
     * This method is used by {@link DoubleStatistics} using a sum that can be reused
     * for the {@link Sum} statistic.
     *
     * <p>Warning: No range checks are performed.
     *
     * @param sum Sum of the values.
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfSquaredDeviations} instance.
     */
    static SumOfSquaredDeviations createFromRange(org.apache.commons.numbers.core.Sum sum,
                                                  double[] values, int from, int to) {
        if (from == to) {
            return new SumOfSquaredDeviations();
        }
        return create(FirstMoment.createFromRange(sum, values, from, to), values, from, to);
    }

    /**
     * Creates the sum of squared deviations.
     *
     * @param m1 First moment.
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfSquaredDeviations} instance.
     */
    private static SumOfSquaredDeviations create(FirstMoment m1, double[] values, int from, int to) {
        // "Corrected two-pass algorithm"
        // See: Chan et al (1983) Equation 1.7

        final double xbar = m1.getFirstMoment();
        if (!Double.isFinite(xbar)) {
            return new SumOfSquaredDeviations(Double.NaN, m1);
        }
        double s = 0;
        double ss = 0;
        for (int i = from; i < to; i++) {
            final double dx = values[i] - xbar;
            s += dx;
            ss += dx * dx;
        }
        // The sum of squared deviations is ss - (s * s / n).
        // The second term ideally should be zero; in practice it is a good approximation
        // of the error in the first term.
        // To prevent sumSquaredDev from spuriously attaining a NaN value
        // when ss is infinite, assign it an infinite value which is its intended value.
        final double sumSquaredDev = ss == Double.POSITIVE_INFINITY ?
            Double.POSITIVE_INFINITY :
            ss - (s * s / (to - from));
        return new SumOfSquaredDeviations(sumSquaredDev, m1);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        // "Updating one-pass algorithm"
        // See: Chan et al (1983) Equation 1.3b
        super.accept(value);
        // Note: account for the half-deviation representation by scaling by 4=2^2
        sumSquaredDev += (n - 1) * dev * nDev * 4;
    }

    /**
     * Gets the sum of squared deviations of all input values.
     *
     * @return sum of squared deviations of all values.
     */
    double getSumOfSquaredDeviations() {
        return Double.isFinite(getFirstMoment()) ? sumSquaredDev : Double.NaN;
    }

    /**
     * Combines the state of another {@code SumOfSquaredDeviations} into this one.
     *
     * @param other Another {@code SumOfSquaredDeviations} to be combined.
     * @return {@code this} instance after combining {@code other}.
     */
    SumOfSquaredDeviations combine(SumOfSquaredDeviations other) {
        final long m = other.n;
        if (n == 0) {
            sumSquaredDev = other.sumSquaredDev;
        } else if (m != 0) {
            // "Updating one-pass algorithm"
            // See: Chan et al (1983) Equation 1.5b (modified for the mean)
            final double diffOfMean = getFirstMomentDifference(other);
            final double sqDiffOfMean = diffOfMean * diffOfMean;
            // Enforce symmetry
            sumSquaredDev = (sumSquaredDev + other.sumSquaredDev) +
                sqDiffOfMean * (((double) n * m) / ((double) n + m));
        }
        super.combine(other);
        return this;
    }
}
