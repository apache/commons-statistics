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
 * Computes the arithmetic mean of a set of values. Uses the following recursive
 * updating algorithm:
 * <ol>
 * <li>Initialize {@code m = } the first value</li>
 * <li>For each additional value, update using <br>
 *   {@code m = m + (new value - m) / (number of observations)}</li>
 * </ol>
 *
 * <p>If {@link #of(double...)} is used to compute the mean of a variable number
 * of values, a two-pass, corrected algorithm is used, starting with
 * the recursive updating algorithm mentioned above, which protects the mean from overflow,
 * and then correcting this by adding the mean deviation of the data values from the
 * arithmetic mean. See, e.g. "Comparison of Several Algorithms for Computing
 * Sample Means and Variances," Robert F. Ling, Journal of the American
 * Statistical Association, Vol. 69, No. 348 (Dec., 1974), pp. 859-866.
 *
 * <p>Returns {@code NaN} if the dataset is empty. Note that
 * {@code NaN} may also be returned if the input includes {@code NaN} and / or infinite
 * values of opposite sign.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
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
 * @since 1.1
 */
public abstract class Mean implements DoubleStatistic, DoubleStatisticAccumulator<Mean> {

    /**
     * Create a Mean instance.
     */
    Mean() {
        // No-op
    }

    /**
     * Creates a {@code Mean} implementation which does not store the input value(s) it consumes.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN} or
     * if no values have been added.
     *
     * @return {@code Mean} implementation.
     */
    public static Mean create() {
        return new StorelessMean();
    }

    /**
     * Returns a {@code Mean} instance that has the arithmetic mean of all input values, or {@code NaN}
     * if the input array is empty.
     *
     * <p>Note: {@code Mean} computed using {@link Mean#accept Mean.accept()} may be different
     * from this mean.
     *
     * <p>See {@link Mean} for details on the computing algorithm.
     *
     * @param values Values.
     * @return {@code Mean} instance.
     */
    public static Mean of(double... values) {
        return new StorelessMean(FirstMoment.of(values));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     * @param value Value.
     */
    @Override
    public abstract void accept(double value);

    /**
     * Gets the mean of all input values.
     *
     * <p>When no values have been added, the result is {@code NaN}.
     *
     * @return {@code Mean} of all values seen so far.
     */
    @Override
    public abstract double getAsDouble();

    /**
     * {@code Mean} implementation that does not store the input value(s) processed so far.
     */
    private static class StorelessMean extends Mean {

        /**
         * External Moment used to compute the mean.
         */
        private final FirstMoment firstMoment;

        /**
         * Creates an instance with a moment.
         *
         * @param m1 First moment.
         */
        StorelessMean(FirstMoment m1) {
            firstMoment = m1;
        }

        /**
         * Create an instance.
         */
        StorelessMean() {
            this(new FirstMoment());
        }

        @Override
        public void accept(double value) {
            firstMoment.accept(value);
        }

        @Override
        public double getAsDouble() {
            return firstMoment.getFirstMoment();
        }

        @Override
        public Mean combine(Mean other) {
            final StorelessMean that = (StorelessMean) other;
            firstMoment.combine(that.firstMoment);
            return this;
        }
    }
}
