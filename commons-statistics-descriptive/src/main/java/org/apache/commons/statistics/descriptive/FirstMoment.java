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
 * <p> mean = sum(x_i) / n
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
 *       Algorithms for Computing the Sample Variance.
 *       American Statistician, vol. 37, no. 3, pp. 242-247.
 *   <li>Ling (1974)
 *       Comparison of Several Algorithms for Computing Sample Means and Variances.
 *       Journal of the American Statistical Association, Vol. 69, No. 348, pp. 859-866.
 * </ul>
 */
class FirstMoment implements DoubleConsumer {
    /** Count of values that have been added. */
    protected long n;

    /** First moment of values that have been added. */
    protected double m1;

    /**
     * Deviation of most recently added value from the previous first moment.
     * Retained to prevent repeated computation in higher order moments.
     */
    protected double dev;

    /**
     * Deviation of most recently added value from the previous first moment,
     * normalized by current sample size. Retained to prevent repeated
     * computation in higher order moments.
     */
    protected double nDev;

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
     * Returns a {@code FirstMoment} instance that has the arithmetic mean of all input
     * values, or {@code NaN} if the input array is empty.
     *
     * <p>Note: {@code FirstMoment} computed using {@link FirstMoment#accept
     * FirstMoment.accept()} may be different from this instance.
     *
     * @param values Values.
     * @return {@code FirstMoment} instance.
     */
    static FirstMoment of(double... values) {
        // "Corrected two-pass algorithm"

        // First pass
        final FirstMoment m1 = Statistics.add(new FirstMoment(), values);
        final double xbar = m1.getFirstMoment();
        if (!Double.isFinite(xbar)) {
            // Note: Also occurs when the input is empty
            return m1;
        }
        // Second pass
        double correction = 0;
        for (final double x : values) {
            correction += x - xbar;
        }
        // Note: Correction may be infinite
        if (Double.isFinite(correction)) {
            m1.m1 += correction / values.length;
        }
        return m1;
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

        n++;
        nonFiniteValue += value;
        // To prevent overflow, dev is computed by scaling down and then scaling up.
        // We choose to scale down by a factor of two to ensure that the scaling is lossless.
        dev = value * 0.5 - m1 * 0.5;
        // Here nDev cannot overflow as dev is <= MAX_VALUE when n > 1; or <= MAX_VALUE / 2 when n = 1
        nDev = (dev / n) * 2;
        m1 += nDev;
        // Scale up the deviation.
        dev *= 2;
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
        if (Double.isFinite(m1)) {
            return n == 0 ? Double.NaN : m1;
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
        if (n == 0) {
            n = other.n;
            nonFiniteValue = other.nonFiniteValue;
            dev = other.dev;
            nDev = other.nDev;
            m1 = other.m1;
        } else if (other.n != 0) {
            n += other.n;
            nonFiniteValue += other.nonFiniteValue;
            dev = other.m1 * 0.5 - m1 * 0.5;
            // In contrast to the accept method, here nDev can be close to MAX_VALUE
            // if the weight (other.n / n) approaches 1. So we cannot yet rescale nDev and
            // instead have to combine it with the scaled-down value of m1.
            nDev = dev * ((double) other.n / n);
            m1 = m1 * 0.5 + nDev;
            // Scale up the terms.
            m1 *= 2;
            dev *= 2;
            nDev *= 2;
        }
        return this;
    }
}
