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

import java.util.Arrays;

/**
 * Returns the minimum of the available values.
 *
 * <p>The result is <code>NaN</code> if any of the values is <code>NaN</code>.
 *
 * <p>The result is <code>POSITIVE_INFINITY</code> if no values are added.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This implementation is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least one of the threads invokes the <code>accept()</code> or
 * <code>combine()</code> method, it must be synchronized externally.
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
public abstract class Min implements DoubleStatistic, DoubleStatisticAccumulator<Min> {

    /**
     * Create a Min instance.
     */
    Min() {
        // No-op
    }

    /**
     * Creates a {@code Min} implementation which does not store the input value(s) it consumes.
     *
     * <p>The result is <code>NaN</code> if any of the values is <code>NaN</code>.
     *
     * <p>The result is {@link Double#POSITIVE_INFINITY POSITIVE_INFINITY}
     * if no values have been added.
     *
     * @return {@code Min} implementation.
     */
    public static Min create() {
        return new StorelessMin();
    }

    /**
     * Returns a {@code Min} instance that has the minimum of all input value(s).
     *
     * <p>The result is <code>NaN</code> if any of the values is <code>NaN</code>.
     *
     * <p>When the input is an empty array, the result is
     * {@link Double#POSITIVE_INFINITY POSITIVE_INFINITY}.
     *
     * @param values Values.
     * @return {@code Min} instance.
     */
    public static Min of(double... values) {
        final StorelessMin min = new StorelessMin();
        Arrays.stream(values).forEach(min);
        return min;
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     * @param value Value.
     */
    @Override
    public abstract void accept(double value);

    /**
     * Gets the minimum of all input values.
     *
     * <p>When no values have been added, the result is
     * {@link Double#POSITIVE_INFINITY POSITIVE_INFINITY}.
     *
     * @return {@code Minimum} of all values seen so far.
     */
    @Override
    public abstract double getAsDouble();

    /** {@inheritDoc} */
    @Override
    public abstract Min combine(Min other);

    /**
     * {@code Min} implementation that does not store the input value(s) processed so far.
     *
     * <p>Uses JDK's {@link Math#min Math.min} as an underlying function
     * to compute the {@code minimum}.
     */
    private static class StorelessMin extends Min {

        /** Current min. */
        private double min = Double.POSITIVE_INFINITY;

        @Override
        public void accept(double value) {
            min = Double.min(min, value);
        }

        @Override
        public double getAsDouble() {
            return min;
        }

        @Override
        public Min combine(Min other) {
            accept(other.getAsDouble());
            return this;
        }
    }
}
