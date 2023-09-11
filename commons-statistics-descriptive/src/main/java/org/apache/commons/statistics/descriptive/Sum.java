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
 * Returns the sum of the available values.
 *
 * <p>The result is <code>NaN</code> if any of the values is <code>NaN</code>.
 *
 * <p>The result is zero if no values are added.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This implementation is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link DoubleStatisticAccumulator#combine(DoubleStatistic) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use <code>accept()</code> and <code>combine()</code>
 * as <code>accumulator</code> and <code>combiner</code> functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * @since 1.1
 */
public abstract class Sum implements DoubleStatistic, DoubleStatisticAccumulator<Sum> {

    /**
     * Create a Sum instance.
     */
    Sum() {
        //No-op
    }

    /**
     * Creates a {@code Sum} implementation which does not store the input value(s) it consumes.
     *
     * <p>The result is <code>NaN</code> if any of the values is <code>NaN</code> or the sum
     * at any point is a <code>NaN</code>.
     *
     * <p>The result is zero if no values have been added.
     *
     * <p>Uses the {@link org.apache.commons.numbers.core.Sum Commons Numbers Sum} implementation.
     *
     * @return {@code Sum} implementation.
     */
    public static Sum create() {
        return new WrappedSum();
    }

    /**
     * Returns a {@code Sum} instance that has the sum of all input value(s).
     *
     * <p>The result is <code>NaN</code> if any of the values is <code>NaN</code>
     * or the sum at any point is a <code>NaN</code>.
     *
     * <p>When the input is an empty array, the result is zero.
     *
     * @param values Values.
     * @return {@code Sum} instance.
     */
    public static Sum of(double... values) {
        return Statistics.add(new WrappedSum(), values);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     * @param value Value.
     */
    @Override
    public abstract void accept(double value);

    /**
     * Gets the sum of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * @return {@code Sum} of all values seen so far.
     */
    @Override
    public abstract double getAsDouble();

    /**
     * {@code Sum} implementation that does not store the input value(s) processed so far.
     *
     * <p>Delegates to the {@link org.apache.commons.numbers.core.Sum} implementation.
     */
    private static class WrappedSum extends Sum {

        /** Create an instance of {@link org.apache.commons.numbers.core.Sum Sum}. */
        private final org.apache.commons.numbers.core.Sum delegate =
                org.apache.commons.numbers.core.Sum.create();

        @Override
        public void accept(double value) {
            delegate.add(value);
        }

        @Override
        public double getAsDouble() {
            return delegate.getAsDouble();
        }

        @Override
        public Sum combine(Sum other) {
            final WrappedSum that = (WrappedSum) other;
            delegate.add(that.delegate);
            return this;
        }
    }
}
