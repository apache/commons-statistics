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
 * Returns the maximum of the available values.
 *
 * <p>The result is {@code NaN} if any of the values is {@code NaN}.
 *
 * <p>The result is {@link Double#NEGATIVE_INFINITY negative infinity} if no values are added.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This implementation is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
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
public abstract class Max implements DoubleStatistic, DoubleStatisticAccumulator<Max> {

    /**
     * Create a Max instance.
     */
    Max() {
        //No-op
    }

    /**
     * Creates a {@code Max} implementation which does not store the input value(s) it consumes.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}.
     *
     * <p>The result is {@link Double#NEGATIVE_INFINITY negative infinity}
     * if no values have been added.
     *
     * @return {@code Max} implementation.
     */
    public static Max create() {
        return new StorelessMax();
    }

    /**
     * Returns a {@code Max} instance that has the maximum of all input value(s).
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}.
     *
     * <p>When the input is an empty array, the result is
     * {@link Double#NEGATIVE_INFINITY negative infinity}.
     *
     * @param values Values.
     * @return {@code Max} instance.
     */
    public static Max of(double... values) {
        return Statistics.add(new StorelessMax(), values);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     * @param value Value.
     */
    @Override
    public abstract void accept(double value);

    /**
     * Gets the maximum of all input values.
     *
     * <p>When no values have been added, the result is
     * {@link Double#NEGATIVE_INFINITY negative infinity}.
     *
     * @return {@code Maximum} of all values seen so far.
     */
    @Override
    public abstract double getAsDouble();

    /**
     * {@code Max} implementation that does not store the input value(s) processed so far.
     *
     * <p>Uses JDK's {@link Math#max Math.max} as an underlying function
     * to compute the {@code maximum}.
     */
    private static class StorelessMax extends Max {

        /** Current max. */
        private double max = Double.NEGATIVE_INFINITY;

        @Override
        public void accept(double value) {
            max = Double.max(max, value);
        }

        @Override
        public double getAsDouble() {
            return max;
        }

        @Override
        public Max combine(Max other) {
            accept(other.getAsDouble());
            return this;
        }
    }
}
