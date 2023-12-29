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

import java.util.function.DoubleConsumer;

/**
 * Computes the first moment (arithmetic mean) using the definitional formula:
 *
 * <pre>mean = sum(x_i) / n</pre>
 *
 * <p> To limit numeric errors, the value of the statistic is computed using the
 * following recursive updating algorithm:
 * <ol>
 * <li>Initialize {@code m = } the first value</li>
 * <li>For each additional value, update using <br>
 *   {@code m = m + (new value - m) / (number of observations)}</li>
 * </ol>
 *
 * <p>Returns {@code NaN} if the dataset is empty. Note that
 * {@code NaN} may also be returned if the input includes {@code NaN} and / or infinite
 * values of opposite sign.
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
 *   <li>Chan, Golub, Levesque (1983)
 *       Algorithms for Computing the Sample Variance.
 *       American Statistician, vol. 37, no. 3, pp. 242-247.
 *   <li>Ling (1974)
 *       Comparison of Several Algorithms for Computing Sample Means and Variances.
 *       Journal of the American Statistical Association, Vol. 69, No. 348, pp. 859-866.
 * </ul>
 */
class FirstMoment implements DoubleConsumer {
    /** The downscale constant. Used to avoid overflow for all finite input. */
    private static final double DOWNSCALE = 0.5;
    /** The rescale constant. */
    private static final double RESCALE = 2;

    /** Count of values that have been added. */
    protected long n;

    /**
     * Half the deviation of most recently added value from the previous first moment.
     * Retained to prevent repeated computation in higher order moments.
     *
     * <p>Note: This is (x - m1) / 2. It is computed as a half value to prevent overflow
     * when computing for any finite value x and m.
     *
     * <p>This value is not used in the {@link #combine(FirstMoment)} method.
     */
    protected double dev;

    /**
     * Half the deviation of most recently added value from the previous first moment,
     * normalized by current sample size. Retained to prevent repeated
     * computation in higher order moments.
     *
     * <p>Note: This is (x - m1) / 2n. It is computed as a half value to prevent overflow
     * when computing for any finite value x and m.
     *
     * Note: This value is not used in the {@link #combine(FirstMoment)} method.
     */
    protected double nDev;

    /** First moment of values that have been added.
     * This is stored as a half value to prevent overflow for any finite input.
     * Benchmarks show this has negligible performance impact. */
    private double m1;

    /**
     * Running sum of values seen so far.
     * This is not used in the computation of mean. Used as a return value for first moment when
     * it is non-finite.
     */
    private double nonFiniteValue;

    /**
     * Create an instance.
     */
    FirstMoment() {
        // No-op
    }

    /**
     * Copy constructor.
     *
     * @param source Source to copy.
     */
    FirstMoment(FirstMoment source) {
        m1 = source.m1;
        n = source.n;
        nonFiniteValue = source.nonFiniteValue;
    }

    /**
     * Create an instance with the given first moment.
     *
     * <p>This constructor is used when creating the moment from integer values. The
     * non-finite value is not required.
     *
     * @param m1 First moment.
     * @param n Count of values.
     */
    FirstMoment(double m1, long n) {
        this.m1 = m1 * DOWNSCALE;
        this.n = n;
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code FirstMoment} computed using {@link #accept} may be different from
     * this instance.
     *
     * @param values Values.
     * @return {@code FirstMoment} instance.
     */
    static FirstMoment of(double... values) {
        if (values.length == 0) {
            return new FirstMoment();
        }

        // "Corrected two-pass algorithm"

        // First pass
        final FirstMoment m1 = create(values);
        final double xbar = m1.getFirstMoment();
        if (!Double.isFinite(xbar)) {
            return m1;
        }
        // Second pass
        double correction = 0;
        for (final double x : values) {
            correction += x - xbar;
        }
        // Note: Correction may be infinite
        if (Double.isFinite(correction)) {
            // Down scale the correction to the half representation
            m1.m1 += DOWNSCALE * correction / values.length;
        }
        return m1;
    }

    /**
     * Creates the first moment using a rolling algorithm.
     *
     * <p>This duplicates the algorithm in the {@link #accept(double)} method
     * with optimisations due to the processing of an entire array:
     * <ul>
     *  <li>Avoid updating (unused) class level working variables.
     *  <li>Only computing the non-finite value if required.
     * </ul>
     *
     * @param values Values.
     * @return the first moment
     */
    private static FirstMoment create(double[] values) {
        double m1 = 0;
        int n = 0;
        for (final double x : values) {
            // Downscale to avoid overflow for all finite input
            m1 += (x * DOWNSCALE - m1) / ++n;
        }
        final FirstMoment m = new FirstMoment();
        m.n = n;
        // Note: m1 is already downscaled here
        m.m1 = m1;
        // The non-finite value is only relevant if the data contains inf/nan
        if (!Double.isFinite(m1 * RESCALE)) {
            m.nonFiniteValue = sum(values);
        }
        return m;
    }

    /**
     * Compute the sum of the values.
     *
     * @param values Values.
     * @return the sum
     */
    private static double sum(double[] values) {
        double sum = 0;
        for (final double x : values) {
            sum += x;
        }
        return sum;
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        // "Updating one-pass algorithm"
        // See: Chan et al (1983) Equation 1.3a
        // m_{i+1} = m_i + (x - m_i) / (i + 1)
        // This is modified with scaling to avoid overflow for all finite input.
        // Scaling the input down by a factor of two ensures that the scaling is lossless.
        // Sub-classes must alter their scaling factors when using the computed deviations.

        nonFiniteValue += value;
        // Scale down the input
        dev = value * DOWNSCALE - m1;
        nDev = dev / ++n;
        m1 += nDev;
    }

    /**
     * Gets the first moment of all input values.
     *
     * <p>When no values have been added, the result is {@code NaN}.
     *
     * @return {@code First moment} of all values, if it is finite;
     *         {@code +/-Infinity}, if infinities of the same sign have been encountered;
     *         {@code NaN} otherwise.
     */
    double getFirstMoment() {
        // Scale back to the original magnitude
        final double m = m1 * RESCALE;
        if (Double.isFinite(m)) {
            return n == 0 ? Double.NaN : m;
        }
        // A non-finite value must have been encountered, return nonFiniteValue which represents m1.
        return nonFiniteValue;
    }

    /**
     * Combines the state of another {@code FirstMoment} into this one.
     *
     * @param other Another {@code FirstMoment} to be combined.
     * @return {@code this} instance after combining {@code other}.
     */
    FirstMoment combine(FirstMoment other) {
        nonFiniteValue += other.nonFiniteValue;
        final double mu1 = this.m1;
        final double mu2 = other.m1;
        final long n1 = n;
        final long n2 = other.n;
        n = n1 + n2;
        // Adjust the mean with the weighted difference:
        // m1 = m1 + (m2 - m1) * n2 / (n1 + n2)
        // The half-representation ensures the difference of means is at most MAX_VALUE
        // so the combine can avoid scaling.
        if (n1 == n2) {
            // Optimisation for equal sizes: m1 = (m1 + m2) / 2
            m1 = (mu1 + mu2) * 0.5;
        } else {
            m1 = combine(mu1, mu2, n1, n2);
        }
        return this;
    }

    /**
     * Combine the moments. This method is used to enforce symmetry. It assumes that
     * the two sizes are not identical, and at least one size is non-zero.
     *
     * @param m1 Moment 1.
     * @param m2 Moment 2.
     * @param n1 Size of sample 1.
     * @param n2 Size of sample 2.
     * @return the combined first moment
     */
    private static double combine(double m1, double m2, long n1, long n2) {
        // Note: If either size is zero the weighted difference is zero and
        // the other moment is unchanged.
        return n2 < n1 ?
            m1 + (m2 - m1) * ((double) n2 / (n1 + n2)) :
            m2 + (m1 - m2) * ((double) n1 / (n1 + n2));
    }

    /**
     * Gets the difference of the first moment between {@code this} moment and the
     * {@code other} moment. This is provided for sub-classes.
     *
     * @param other Other moment.
     * @return the difference
     */
    double getFirstMomentDifference(FirstMoment other) {
        // Scale back to the original magnitude
        return (m1 - other.m1) * RESCALE;
    }

    /**
     * Gets the half the difference of the first moment between {@code this} moment and
     * the {@code other} moment. This is provided for sub-classes.
     *
     * @param other Other moment.
     * @return the difference
     */
    double getFirstMomentHalfDifference(FirstMoment other) {
        return m1 - other.m1;
    }
}
