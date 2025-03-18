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
 * Computes the sum of cubed deviations from the sample mean. This
 * statistic is related to the third moment.
 *
 * <p>Uses a recursive updating formula as defined in Manca and Marin (2010), equation 10.
 * Note that the denominator in the third term in that equation has been corrected to
 * \( (N_1 + N_2)^2 \). Two sum of cubed deviations (SC) can be combined using:
 *
 * <p>\[ SC(X) = {SC}_1 + {SC}_2 + \frac{3(m_1 - m_2)({s_1}^2 - {s_2}^2) N_1 N_2}{N_1 + N_2}
 *                               + \frac{(m_1 - m_2)^3((N_2 - N_1) N_1 N_2}{(N_1 + N_2)^2} \]
 *
 * <p>where \( N \) is the group size, \( m \) is the mean, and \( s^2 \) is the biased variance
 * such that \( s^2 * N \) is the sum of squared deviations from the mean. Note the term
 * \( ({s_1}^2 - {s_2}^2) N_1 N_2 == (ss_1 * N_2 - ss_2 * N_1 \) where \( ss \) is the sum
 * of square deviations.
 *
 * <p>If \( N_1 \) is size 1 this reduces to:
 *
 * <p>\[ SC_{N+1} = {SC}_N + \frac{3(x - m) -s^2 N}{N + 1}
 *                         + \frac{(x - m)^3((N - 1) N}{(N + 1)^2} \]
 *
 * <p>where \( s^2 N \) is the sum of squared deviations.
 * This updating formula is identical to that used in
 * {@code org.apache.commons.math3.stat.descriptive.moment.ThirdMoment}.
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
 *   <li>Manca and Marin (2020)
 *       Decomposition of the Sum of Cubes, the Sum Raised to the
 *       Power of Four and Codeviance.
 *       Applied Mathematics, 11, 1013-1020.
 *       <a href="https://doi.org/10.4236/am.2020.1110067">doi: 10.4236/am.2020.1110067</a>
 * </ul>
 *
 * @since 1.1
 */
class SumOfCubedDeviations extends SumOfSquaredDeviations {
    /** 2, the length limit where the sum-of-cubed deviations is zero. */
    static final int LENGTH_TWO = 2;

    /** Sum of cubed deviations of the values that have been added. */
    protected double sumCubedDev;

    /**
     * Create an instance.
     */
    SumOfCubedDeviations() {
        // No-op
    }

    /**
     * Copy constructor.
     *
     * @param source Source to copy.
     */
    SumOfCubedDeviations(SumOfCubedDeviations source) {
        super(source);
        sumCubedDev = source.sumCubedDev;
    }

    /**
     * Create an instance with the given sum of cubed and squared deviations.
     *
     * @param sc Sum of cubed deviations.
     * @param ss Sum of squared deviations.
     */
    SumOfCubedDeviations(double sc, SumOfSquaredDeviations ss) {
        super(ss);
        this.sumCubedDev = sc;
    }

    /**
     * Create an instance with the given sum of cubed and squared deviations,
     * and first moment.
     *
     * @param sc Sum of cubed deviations.
     * @param ss Sum of squared deviations.
     * @param m1 First moment.
     * @param n Count of values.
     */
    SumOfCubedDeviations(double sc, double ss, double m1, long n) {
        super(ss, m1, n);
        this.sumCubedDev = sc;
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code SumOfCubedDeviations} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code SumOfCubedDeviations} instance.
     */
    static SumOfCubedDeviations of(double... values) {
        if (values.length == 0) {
            return new SumOfCubedDeviations();
        }
        return create(SumOfSquaredDeviations.of(values), values, 0, values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code SumOfCubedDeviations} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * <p>Warning: No range checks are performed.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfCubedDeviations} instance.
     */
    static SumOfCubedDeviations ofRange(double[] values, int from, int to) {
        if (from == to) {
            return new SumOfCubedDeviations();
        }
        return create(SumOfSquaredDeviations.ofRange(values, from, to), values, from, to);
    }

    /**
     * Creates the sum of cubed deviations.
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
     * @return {@code SumOfCubedDeviations} instance.
     */
    static SumOfCubedDeviations createFromRange(org.apache.commons.numbers.core.Sum sum,
                                                double[] values, int from, int to) {
        if (from == to) {
            return new SumOfCubedDeviations();
        }
        return create(SumOfSquaredDeviations.createFromRange(sum, values, from, to), values, from, to);
    }

    /**
     * Creates the sum of cubed deviations.
     *
     * @param ss Sum of squared deviations.
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfCubedDeviations} instance.
     */
    private static SumOfCubedDeviations create(SumOfSquaredDeviations ss, double[] values, int from, int to) {
        // Edge cases
        final double xbar = ss.getFirstMoment();
        if (!Double.isFinite(xbar)) {
            return new SumOfCubedDeviations(Double.NaN, ss);
        }
        if (!Double.isFinite(ss.sumSquaredDev)) {
            // Note: If the sum-of-squared (SS) overflows then the same deviations when cubed
            // will overflow. The *smallest* deviation to overflow SS is a full-length array of
            // +/- values around a mean of zero, or approximately sqrt(MAX_VALUE / 2^31) = 2.89e149.
            // In this case the sum cubed could be finite due to cancellation
            // but this cannot be computed. Only a small array can be known to be zero.
            return new SumOfCubedDeviations(ss.n <= LENGTH_TWO ? 0 : Double.NaN, ss);
        }
        // Compute the sum of cubed deviations.
        double s = 0;
        // n=1: no deviation
        // n=2: the two deviations from the mean are equal magnitude
        // and opposite sign. So the sum-of-cubed deviations is zero.
        if (ss.n > LENGTH_TWO) {
            for (int i = from; i < to; i++) {
                s += pow3(values[i] - xbar);
            }
        }
        return new SumOfCubedDeviations(s, ss);
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code SumOfCubedDeviations} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code SumOfCubedDeviations} instance.
     */
    static SumOfCubedDeviations of(int... values) {
        return ofRange(values, 0, values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code SumOfCubedDeviations} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * <p>Warning: No range checks are performed.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfCubedDeviations} instance.
     */
    static SumOfCubedDeviations ofRange(int[] values, int from, int to) {
        // Logic shared with the double[] version with int[] lower order moments
        if (from == to) {
            return new SumOfCubedDeviations();
        }
        final IntVariance variance = IntVariance.createFromRange(values, from, to);
        final double xbar = variance.computeMean();
        final double ss = variance.computeSumOfSquaredDeviations();

        double sc = 0;
        if (to - from > LENGTH_TWO) {
            for (int i = from; i < to; i++) {
                sc += pow3(values[i] - xbar);
            }
        }
        return new SumOfCubedDeviations(sc, ss, xbar, to - from);
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code SumOfCubedDeviations} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code SumOfCubedDeviations} instance.
     */
    static SumOfCubedDeviations of(long... values) {
        return ofRange(values, 0, values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code SumOfCubedDeviations} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * <p>Warning: No range checks are performed.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfCubedDeviations} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    static SumOfCubedDeviations ofRange(long[] values, int from, int to) {
        // Logic shared with the double[] version with int[] lower order moments
        if (from == to) {
            return new SumOfCubedDeviations();
        }
        final LongVariance variance = LongVariance.createFromRange(values, from, to);
        final double xbar = variance.computeMean();
        final double ss = variance.computeSumOfSquaredDeviations();

        double sc = 0;
        if (to - from > LENGTH_TWO) {
            for (int i = from; i < to; i++) {
                sc += pow3(values[i] - xbar);
            }
        }
        return new SumOfCubedDeviations(sc, ss, xbar, to - from);
    }

    /**
     * Compute {@code x^3}.
     * Uses compound multiplication.
     *
     * @param x Value.
     * @return x^3
     */
    private static double pow3(double x) {
        return x * x * x;
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        // Require current s^2 * N == sum-of-square deviations
        final double ss = sumSquaredDev;
        final double np = n;
        super.accept(value);
        // Terms are arranged so that values that may be zero
        // (np, ss) are first. This will cancel any overflow in
        // multiplication of later terms (nDev * 3 and nDev^2).
        // This handles initialisation when np in {0, 1) to zero
        // for any deviation (e.g. series MAX_VALUE, -MAX_VALUE).
        // Note: account for the half-deviation representation by scaling by 6=3*2; 8=2^3
        sumCubedDev = sumCubedDev -
            ss * nDev * 6 +
            (np - 1.0) * np * nDev * nDev * dev * 8;
    }

    /**
     * Gets the sum of cubed deviations of all input values.
     *
     * <p>Note that the sum is subject to cancellation of potentially large
     * positive and negative terms. A non-finite result may be returned
     * due to intermediate overflow when the exact result may be a representable
     * {@code double}.
     *
     * <p>Note: Any non-finite result should be considered a failed computation.
     * The result is returned as computed and not consolidated to a single NaN.
     * This is done for testing purposes to allow the result to be reported.
     * In particular the sign of an infinity may not indicate the direction
     * of the asymmetry (if any), only the direction of the first overflow in the
     * computation. In the event of further overflow of a term to an opposite signed
     * infinity the sum will be {@code NaN}.
     *
     * @return sum of cubed deviations of all values.
     */
    double getSumOfCubedDeviations() {
        return Double.isFinite(getFirstMoment()) ? sumCubedDev : Double.NaN;
    }

    /**
     * Combines the state of another {@code SumOfCubedDeviations} into this one.
     *
     * @param other Another {@code SumOfCubedDeviations} to be combined.
     * @return {@code this} instance after combining {@code other}.
     */
    SumOfCubedDeviations combine(SumOfCubedDeviations other) {
        if (n == 0) {
            sumCubedDev = other.sumCubedDev;
        } else if (other.n != 0) {
            // Avoid overflow to compute the difference.
            // This allows any samples of size n=1 to be combined as their SS=0.
            // The result is a SC=0 for the combined n=2.
            final double halfDiffOfMean = getFirstMomentHalfDifference(other);
            sumCubedDev += other.sumCubedDev;
            // Add additional terms that do not cancel to zero
            if (halfDiffOfMean != 0) {
                final double n1 = n;
                final double n2 = other.n;
                if (n1 == n2) {
                    // Optimisation where sizes are equal in double-precision.
                    // This is of use in JDK streams as spliterators use a divide by two
                    // strategy for parallel streams.
                    sumCubedDev += (sumSquaredDev - other.sumSquaredDev) * halfDiffOfMean * 3;
                } else {
                    final double n1n2 = n1 + n2;
                    final double dm = 2 * (halfDiffOfMean / n1n2);
                    sumCubedDev += (sumSquaredDev * n2 - other.sumSquaredDev * n1) * dm * 3 +
                                   (n2 - n1) * (n1 * n2) * pow3(dm) * n1n2;
                }
            }
        }
        super.combine(other);
        return this;
    }
}
