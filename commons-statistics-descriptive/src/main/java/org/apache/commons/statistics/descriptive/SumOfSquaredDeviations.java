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
 * <p><strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link DoubleStatisticAccumulator#combine(DoubleStatistic) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.DoubleConsumer#accept(double) accept}
 * and {@link DoubleStatisticAccumulator#combine(DoubleStatistic) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * <p>References:
 * <ul>
 *   <li>Chan, Golub, Levesque (1983)
 *       Algorithms for Computing the Sample Variance,
 *       American Statistician, vol. 37, no. 3, pp. 242-247.
 * </ul>
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
     * Returns a {@code SumOfSquaredDeviations} instance of all input values, or {@code NaN}
     * if:
     * <ul>
     *     <li>the input array is empty,</li>
     *     <li>any of the values is {@code NaN},</li>
     *     <li>an infinite value of either sign is encountered, or</li>
     *     <li>the sum of the squared deviations from the mean is infinite</li>
     * </ul>
     *
     * <p>Note: {@code SumOfSquaredDeviations} computed using
     * {@link #accept accept} may be different from this instance.
     *
     * <p>See {@link SumOfSquaredDeviations} for details on the computing algorithm.
     *
     * @param values Values.
     * @return {@code SumOfSquaredDeviations} instance.
     */
    static SumOfSquaredDeviations of(double... values) {
        if (values.length == 0) {
            return new SumOfSquaredDeviations();
        }

        // "Corrected two-pass algorithm"
        // See: Chan et al (1983) Equation 1.7

        final FirstMoment m1 = FirstMoment.of(values);
        final double xbar = m1.getFirstMoment();
        if (!Double.isFinite(xbar)) {
            return new SumOfSquaredDeviations(Double.NaN, m1);
        }
        double s = 0;
        double ss = 0;
        for (final double x : values) {
            final double dx = x - xbar;
            s += dx;
            ss += dx * dx;
        }
        final long n = values.length;
        // The sum of squared deviations is ss - (s * s / n).
        // The second term ideally should be zero; in practice it is a good approximation
        // of the error in the first term.
        // To prevent sumSquaredDev from spuriously attaining a NaN value
        // when ss is infinite, assign it an infinite value which is its intended value.
        final double sumSquaredDev = ss == Double.POSITIVE_INFINITY ?
            Double.POSITIVE_INFINITY :
            ss - (s * s / n);
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
        // Note: account for the half-deviation representation
        sumSquaredDev += (n - 1) * halfDev * nDev * 2;
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
            final double diffOfMean = other.m1 - m1;
            final double sqDiffOfMean = diffOfMean * diffOfMean;
            // Enforce symmetry
            sumSquaredDev = (sumSquaredDev + other.sumSquaredDev) +
                sqDiffOfMean * (((double) n * m) / ((double) n + m));
        }
        super.combine(other);
        return this;
    }
}
