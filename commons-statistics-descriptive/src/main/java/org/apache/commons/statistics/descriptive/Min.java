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
 * Returns the minimum of the available values. Uses {@link Math#min Math.min} as an
 * underlying function to compute the {@code minimum}.
 *
 * <ul>
 *   <li>The result is {@link Double#POSITIVE_INFINITY positive infinity} if no values are added.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN}.
 *   <li>The value {@code -0.0} is considered strictly smaller than {@code 0.0}.
 * </ul>
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
 * @see Math#min(double, double)
 */
public final class Min implements DoubleStatistic, DoubleStatisticAccumulator<Min> {

    /** Current minimum. */
    private double minimum = Double.POSITIVE_INFINITY;

    /**
     * Create an instance.
     */
    private Min() {
        // No-op
    }

    /**
     * Creates a {@code Min} instance.
     *
     * <p>The initial result is {@link Double#POSITIVE_INFINITY positive infinity}.
     *
     * @return {@code Min} instance.
     */
    public static Min create() {
        return new Min();
    }

    /**
     * Returns a {@code Min} instance that has the minimum of all input value(s).
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}.
     *
     * <p>When the input is an empty array, the result is
     * {@link Double#POSITIVE_INFINITY positive infinity}.
     *
     * @param values Values.
     * @return {@code Min} instance.
     */
    public static Min of(double... values) {
        return Statistics.add(new Min(), values);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        minimum = Math.min(minimum, value);
    }

    /**
     * Gets the minimum of all input values.
     *
     * <p>When no values have been added, the result is
     * {@link Double#POSITIVE_INFINITY positive infinity}.
     *
     * @return minimum of all values.
     */
    @Override
    public double getAsDouble() {
        return minimum;
    }

    @Override
    public Min combine(Min other) {
        accept(other.getAsDouble());
        return this;
    }
}
