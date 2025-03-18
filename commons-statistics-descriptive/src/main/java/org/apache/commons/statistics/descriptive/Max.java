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
 * Returns the maximum of the available values. Uses {@link Math#max(double, double) Math.max} as an
 * underlying function to compute the {@code maximum}.
 *
 * <ul>
 *   <li>The result is {@link Double#NEGATIVE_INFINITY negative infinity} if no values are added.
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
 * @since 1.1
 * @see Math#max(double, double)
 */
public final class Max implements DoubleStatistic, StatisticAccumulator<Max> {

    /** Current maximum. */
    private double maximum = Double.NEGATIVE_INFINITY;

    /**
     * Create an instance.
     */
    private Max() {
        // No-op
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@link Double#NEGATIVE_INFINITY negative infinity}.
     *
     * @return {@code Max} instance.
     */
    public static Max create() {
        return new Max();
    }

    /**
     * Returns an instance populated using the input {@code values}.
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
        return Statistics.add(new Max(), values);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}.
     *
     * <p>When the range is empty, the result is
     * {@link Double#NEGATIVE_INFINITY negative infinity}.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Max} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static Max ofRange(double[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return createFromRange(values, from, to);
    }

    /**
     * Create an instance using the specified range of {@code values}.
     *
     * <p>Warning: No range checks are performed.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Max} instance.
     */
    static Max createFromRange(double[] values, int from, int to) {
        return Statistics.add(new Max(), values, from, to);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        maximum = Math.max(maximum, value);
    }

    /**
     * Gets the maximum of all input values.
     *
     * <p>When no values have been added, the result is
     * {@link Double#NEGATIVE_INFINITY negative infinity}.
     *
     * @return maximum of all values.
     */
    @Override
    public double getAsDouble() {
        return maximum;
    }

    @Override
    public Max combine(Max other) {
        accept(other.getAsDouble());
        return this;
    }
}
