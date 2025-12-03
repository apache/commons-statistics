/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.examples.jmh.descriptive;

import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Computes the sum of cubed deviations from the sample mean. This
 * statistic is related to the third moment.
 *
 * <p>This is a specialized version of
 * {@code o.a.c.statistics.descriptive.SumOfCubedDeviations}
 * for integer ({@code int/long})
 * arguments. See that class for details of the updating algorithm.
 *
 * <p>When the input are integer values bounded to 2<sup>63</sup> some functionality
 * can be dropped from the {@code double} argument version:
 *
 * <ul>
 *  <li>No overflow is possible given the maximum values of intermediate terms;
 *      allows removing the scaling of input.
 *  <li>No handling of infinite or NaN is required; allows removing computation
 *      of the non-finite value.
 * </ul>
 *
 * <p>This class does not copy the moment hierarchy from the {@code double}
 * argument version. The lower moments (mean and variance) are computed
 * using exact integer arithmetic and not using the updating algorithm.
 * This allows some reuse of factors within the update methods to compute
 * first, second and third order moments together.
 *
 * <p>This class is used for benchmarking. Performance is only a small
 * percentage faster than using the {@code double} consuming version within
 * {@link org.apache.commons.statistics.descriptive.Skewness}. It combines logic
 * from {@code SumOfCubedDeviations} and {@code Skewness}.
 *
 * @since 1.1
 */
class IntegerSumOfCubedDeviations implements IntConsumer, LongConsumer, DoubleSupplier {
    /** 2, the length limit where the biased skewness is undefined.
     * This limit effectively imposes the result m3 / m2^1.5 = 0 / 0 = NaN when 1 value
     * has been added. Note that when more samples are added and the variance
     * approaches zero the result is also returned as NaN. */
    private static final int LENGTH_TWO = 2;
    /** 3, the length limit where the unbiased skewness is undefined. */
    private static final int LENGTH_THREE = 3;

    /** Count of values that have been added. */
    protected long n;

    /**
     * Deviation of most recently added value from the previous first moment:
     * (x - m1).
     * Retained to prevent repeated computation in higher order moments.
     */
    protected double dev;

    /**
     * Deviation of most recently added value from the previous first moment,
     * normalized by current sample size: (x - m1) / n.
     * Retained to prevent repeated computation in higher order moments.
     */
    protected double nDev;

    /**
     * Deviation of most recently added value from the previous first moment,
     * squared, multiplied by previsou sample size and normalised by current
     * sample size: (n-1) * (x - m1)^2 / n.
     * Retained to prevent repeated computation in higher order moments.
     */
    protected double term1;

    /** First moment of values that have been added. */
    protected double m1;

    /** Sum of squared deviations of the values that have been added. */
    protected double sumSquaredDev;

    /** Sum of cubed deviations of the values that have been added. */
    protected double sumCubedDev;

    /** Flag to control if the statistic is biased, or should use a bias correction. */
    private boolean biased;

    /**
     * Create an instance.
     */
    IntegerSumOfCubedDeviations() {
        // No-op
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(int value) {
        update(value);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(long value) {
        update(value);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    void update(double value) {
        final long np = n;
        dev = value - m1;
        nDev = dev / ++n;
        term1 = np * dev * nDev;

        sumCubedDev = sumCubedDev -
            sumSquaredDev * nDev * 3 +
            (np - 1) * term1 * nDev;
        sumSquaredDev += term1;
        m1 += nDev;
    }

    /**
     * Gets the skewness of all input values.
     *
     * <p>When fewer than 3 values have been added, the result is {@code NaN}.
     *
     * @return skewness of all values.
     */
    @Override
    public double getAsDouble() {
        // Adapted from o.a.c.statistics.descriptive.Skewness
        if (n < (biased ? LENGTH_TWO : LENGTH_THREE)) {
            return Double.NaN;
        }
        final double x2 = sumSquaredDev;
        // Avoid a divide by zero; for a negligible variance return NaN.
        // Note: Commons Math returns zero if variance is < 1e-19.
        final double m2 = x2 / n;
        // Simple check for zero variance
        if (m2 == 0) {
            return Double.NaN;
        }
        // denom = pow(m2, 1.5)
        final double denom = Math.sqrt(m2) * m2;
        final double x3 = sumCubedDev;
        final double m3 = x3 / n;
        double g1 = m3 / denom;
        if (!biased) {
            final double n0 = n;
            g1 *= Math.sqrt(n0 * (n0 - 1)) / (n0 - 2);
        }
        return g1;
    }

    /**
     * Sets the value of the biased flag. The default value is {@code false}.
     * See {@link org.apache.commons.statistics.descriptive.Skewness} for details on the computing algorithm.
     *
     * <p>This flag only controls the final computation of the statistic.
     *
     * @param v Value.
     */
    public void setBiased(boolean v) {
        biased = v;
    }
}
