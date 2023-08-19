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
 * Computes the first moment (arithmetic mean) using the definitional formula:
 *
 * <p> mean = sum(x_i) / n
 *
 * <p> To limit numeric errors, the value of the statistic is computed using the
 * following recursive updating algorithm:
 * <ol>
 * <li>Initialize <code>m = </code> the first value</li>
 * <li>For each additional value, update using <br>
 *   <code>m = m + (new value - m) / (number of observations)</code></li>
 * </ol>
 *
 * <p>Returns <code>Double.NaN</code> if the dataset is empty. Note that
 * <code>NaN</code> may also be returned if the input includes NaN and / or infinite
 *  values of opposite sign.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the <code>accept()</code> or
 * <code>combine()</code> method, it must be synchronized externally.
 *
 * <p>However, it is safe to use <code>accept()</code> and <code>combine()</code>
 * as <code>accumulator</code> and <code>combiner</code> functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 */
class FirstMoment implements DoubleStatistic, DoubleStatisticAccumulator<FirstMoment> {
    /** Count of values that have been added. */
    private long n;

    /** First moment of values that have been added. */
    private double m1;

    /**
     * Deviation of most recently added value from the previous first moment.
     * Retained to prevent repeated computation in higher order moments.
     */
    private double dev;

    /**
     * Deviation of most recently added value from the previous first moment,
     * normalized by current sample size. Retained to prevent repeated
     * computation in higher order moments
     */
    private double nDev;

    /**
     * Running sum of values seen so far.
     * This is not used in the computation of mean. Used as a return value for first moment when
     * it is non-finite.
     */
    private double nonFiniteValue;

    /**
     * Create a FirstMoment instance.
     */
    FirstMoment() {
        // No-op
    }

    /**
     * Create a FirstMoment instance with the given first moment and number of values.
     * @param m1 First moment.
     * @param n Number of values.
     * @param nonFiniteValue Running sum of values seen so far (Used as a return value for first moment when it is non-finite).
     */
    FirstMoment(final double m1, final long n, final double nonFiniteValue) {
        this.m1 = m1;
        this.n = n;
        this.nonFiniteValue = nonFiniteValue;
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        n++;
        nonFiniteValue += value;
        // To prevent overflow, dev is computed by scaling down and then scaling up.
        // We choose to scale down and scale up by a factor of two to ensure that the scaling is lossless.
        dev = (value * 0.5 - m1 * 0.5) * 2;
        nDev = dev / n;
        m1 += nDev;
    }

    /**
     * Gets the first moment of all input values.
     *
     * <p>When no values have been added, the result is <code>NaN</code>.
     *
     * @return {@code First moment} of all values seen so far, if it is finite.
     * <p> {@code Infinity}, if infinities of the same sign have been encountered.
     * <p> {@code NaN} otherwise.
     */
    @Override
    public double getAsDouble() {
        if (Double.isFinite(m1)) {
            return n == 0 ? Double.NaN : m1;
        }
        // A non-finite value must have been encountered, return nonFiniteValue which represents m1.
        return nonFiniteValue;
    }

    /** {@inheritDoc} */
    @Override
    public FirstMoment combine(FirstMoment other) {
        if (n == 0) {
            n = other.n;
            dev = other.dev;
            nDev = other.nDev;
            m1 = other.m1;
            nonFiniteValue = other.nonFiniteValue;
        } else if (other.n != 0) {
            n += other.n;
            dev = (other.m1 * 0.5 - m1 * 0.5) * 2;
            nDev = dev * ((double) other.n / n);
            m1 += nDev;
            nonFiniteValue += other.nonFiniteValue;
        }
        return this;
    }

    /**
     * @return Number of values seen so far.
     */
    long getN() {
        return n;
    }

    /**
     * Gets the running sum of the values seen so far.
     * @return Running Sum.
     */
    double getNonFiniteValue() {
        return nonFiniteValue;
    }

    /**
     * Gets the deviation of most recently added value from first moment.
     * @return Deviation.
     */
    double getDev() {
        return dev;
    }

    /**
     * Gets the deviation of most recently added value from first moment, normalized by sample size.
     * @return Normalized Deviation.
     */
    double getDevNormalizedByN() {
        return nDev;
    }
}
