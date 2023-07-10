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
 * Returns the minimum of the available values.
 *
 * <p>The result is <code>NaN</code> if any of the values is <code>NaN</code>.</p>
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This implementation is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least  one of the threads invokes the <code>accept()</code> or
 * <code>combine()</code> method, it must be synchronized externally.</p>
 * <p>However, it is safe to use <code>accept()</code> and <code>combine()</code>
 * as <code>accumulator</code> and <code>combiner</code> functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.</p>
 */
public abstract class Min implements DoubleStatistic, DoubleStatisticAccumulator<Min> {

    /**
     * Helper function that returns a new instance of {@link UnsupportedOperationException}.
     * @return a new {@code UnsupportedOperationException} instance
     */
    static UnsupportedOperationException uoe() {
        return new UnsupportedOperationException();
    }

    /**
     * {@code Min} implementation that does not store the input value(s) processed so far.
     */
    private static final class StorelessMin extends Min {

        /**Current min. */
        private double min;

        /**
         * Create a StorelessMin instance.
         */
        StorelessMin() {
            min = Double.POSITIVE_INFINITY;
        }

        /**
         * Updates the state of Min statistic to reflect the addition of the new value.
         * @param value  the new value.
         */
        @Override
        public void accept(double value) {
            min = Double.min(min, value);
        }

        @Override
        public double getAsDouble() {
            return min;
        }

        /** {@inheritDoc} */
        @Override
        public <U extends DoubleStatisticAccumulator<Min>> void combine(U other) {
            final Min otherMin = other.getDoubleStatistic();
            accept(otherMin.getAsDouble());
        }

        /** {@inheritDoc} */
        @Override
        public Min getDoubleStatistic() {
            return this;
        }
    }

    /**
     * Immutable {@code Min} implementation.
     */
    private static final class ImmutableMin extends Min {

        /**Min delegate. */
        private final Min delegate;

        /**
         * Initializes the Min delegate.
         * @param delegate  Min delegate
         */
        ImmutableMin(final Min delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(double value) {
            throw uoe();
        }

        @Override
        public double getAsDouble() {
            return delegate.getAsDouble();
        }

        @Override
        public <U extends DoubleStatisticAccumulator<Min>> void combine(U other) {
            throw uoe();
        }

        @Override
        public Min getDoubleStatistic() {
            return this;
        }
    }

    /**
     * Creates a {@code Min} implementation which does not store the input value(s) it consumes.
     *
     * @return {@code Min} implementation that does not store the input value(s) processed.
     */
    public static Min createStoreless() {
        return new StorelessMin();
    }

    /**
     * Returns an immutable {@code Min} object that has the minimum of all the input value(s).
     *
     * @param values  the input values for which we would need to compute the minimum
     * @return Immutable Min implementation that does not store the input value(s) processed.
     */
    public static Min of(double... values) {
        final StorelessMin storelessMin = new StorelessMin();
        for (final double d: values) {
            storelessMin.accept(d);
        }
        return new ImmutableMin(storelessMin);
    }
}
