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
package org.apache.commons.statistics.descriptive;

/**
 * Returns the sum of the squares of the available values. Uses the following definition:
 *
 * <p>\[ \sum_{i=1}^n x_i^2 \]
 *
 * <p>where \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is zero if no values are observed.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN}.
 *   <li>The result is {@code +infinity} if any of the values is {@code infinity},
 *       or the sum overflows.
 * </ul>
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This instance is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link StatisticAccumulator#combine(StatisticResult) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.DoubleConsumer#accept(double) accept}
 * and {@link StatisticAccumulator#combine(StatisticResult) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel instance of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * @since 1.1
 */
public final class SumOfSquares implements DoubleStatistic, StatisticAccumulator<SumOfSquares> {

    /** Sum of squares of all values. */
    private double ss;

    /**
     * Create an instance.
     */
    private SumOfSquares() {
        // No-op
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is zero.
     *
     * @return {@code SumOfSquares} instance.
     */
    public static SumOfSquares create() {
        return new SumOfSquares();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}.
     *
     * <p>When the input is an empty array, the result is zero.
     *
     * @param values Values.
     * @return {@code SumOfSquares} instance.
     */
    public static SumOfSquares of(double... values) {
        return Statistics.add(new SumOfSquares(), values);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}.
     *
     * <p>When the range is empty, the result is zero.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code SumOfSquares} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static SumOfSquares ofRange(double[] values, int from, int to) {
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
     * @return {@code SumOfSquares} instance.
     */
    static SumOfSquares createFromRange(double[] values, int from, int to) {
        return Statistics.add(new SumOfSquares(), values, from, to);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        ss += value * value;
    }

    /**
     * Gets the sum of squares of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * @return sum of squares of all values.
     */
    @Override
    public double getAsDouble() {
        return ss;
    }

    @Override
    public SumOfSquares combine(SumOfSquares other) {
        ss += other.ss;
        return this;
    }
}
