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
 * <ul>
 *   <li>The result is zero if no values are added.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN}.
 *   <li>The result is {@code NaN} if the values contain positive and negative infinity.
 *   <li>The result is non-finite if the values contain infinities of the same sign.
 * </ul>
 *
 * <p>Note: In the event of infinite values of the same sign the result will be non-finite.
 * In this case the returned results may differ (either the same infinity or {@code NaN}) based on
 * input order if the sum of the finite-only values can overflow to an opposite signed infinity.
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
 * @see org.apache.commons.numbers.core.Sum
 */
public final class Sum implements DoubleStatistic, StatisticAccumulator<Sum> {

    /** {@link org.apache.commons.numbers.core.Sum Sum} used to compute the sum. */
    private final org.apache.commons.numbers.core.Sum delegate;

    /**
     * Create an instance.
     */
    private Sum() {
        this(org.apache.commons.numbers.core.Sum.create());
    }

    /**
     * Create an instance using the specified {@code sum}.
     *
     * @param sum Sum.
     */
    Sum(org.apache.commons.numbers.core.Sum sum) {
        delegate = sum;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is zero.
     *
     * @return {@code Sum} instance.
     */
    public static Sum create() {
        return new Sum();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}
     * or the sum at any point is a {@code NaN}.
     *
     * <p>When the input is an empty array, the result is zero.
     *
     * @param values Values.
     * @return {@code Sum} instance.
     */
    public static Sum of(double... values) {
        return new Sum(org.apache.commons.numbers.core.Sum.of(values));
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}
     * or the sum at any point is a {@code NaN}.
     *
     * <p>When the range is empty, the result is zero.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Sum} instance.
     * @since 1.2
     */
    public static Sum ofRange(double[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return new Sum(Statistics.sum(values, from, to));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        delegate.accept(value);
    }

    /**
     * Gets the sum of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * @return sum of all values.
     */
    @Override
    public double getAsDouble() {
        return delegate.getAsDouble();
    }

    @Override
    public Sum combine(Sum other) {
        delegate.add(other.delegate);
        return this;
    }
}
