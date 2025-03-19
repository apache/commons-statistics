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

import java.math.BigInteger;

/**
 * Returns the sum of the squares of the available values. Uses the following definition:
 *
 * <p>\[ \sum_{i=1}^n x_i^2 \]
 *
 * <p>where \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is zero if no values are observed.
 * </ul>
 *
 * <p>The implementation uses an exact integer sum to compute the sum of squared values.
 * The exact sum is returned using {@link #getAsBigInteger()}. Methods that return {@code int} or
 * {@code long} primitives will raise an exception if the result overflows.
 *
 * <p>Note that the implementation does not use {@code BigInteger} arithmetic; for
 * performance the sum is computed using primitives to create an unsigned 192-bit integer.
 * Support is provided for at least 2<sup>63</sup> observations.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This implementation is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least one of the threads invokes the {@link java.util.function.IntConsumer#accept(int) accept} or
 * {@link StatisticAccumulator#combine(StatisticResult) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.IntConsumer#accept(int) accept}
 * and {@link StatisticAccumulator#combine(StatisticResult) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * @since 1.1
 */
public final class LongSumOfSquares implements LongStatistic, StatisticAccumulator<LongSumOfSquares> {

    /** Sum of the squared values. */
    private final UInt192 sumSq;

    /**
     * Create an instance.
     */
    private LongSumOfSquares() {
        this(UInt192.create());
    }

    /**
     * Create an instance.
     *
     * @param sumSq Sum of the squared values.
     */
    private LongSumOfSquares(UInt192 sumSq) {
        this.sumSq = sumSq;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is zero.
     *
     * @return {@code LongSumOfSquares} instance.
     */
    public static LongSumOfSquares create() {
        return new LongSumOfSquares();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is zero.
     *
     * @param values Values.
     * @return {@code LongSumOfSquares} instance.
     */
    public static LongSumOfSquares of(long... values) {
        final UInt192 ss = UInt192.create();
        for (final long x : values) {
            ss.addSquare(x);
        }
        return new LongSumOfSquares(ss);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>When the range is empty, the result is zero.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code LongSumOfSquares} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static LongSumOfSquares ofRange(long[] values, int from, int to) {
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
     * @return {@code LongSumOfSquares} instance.
     */
    static LongSumOfSquares createFromRange(long[] values, int from, int to) {
        final UInt192 ss = UInt192.create();
        for (int i = from; i < to; i++) {
            ss.addSquare(values[i]);
        }
        return new LongSumOfSquares(ss);
    }

    /**
     * Gets the sum of squares.
     *
     * <p>This is package private for use in {@link IntStatistics}.
     *
     * @return the sum of squares
     */
    UInt192 getSumOfSquares() {
        return sumSq;
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(long value) {
        sumSq.addSquare(value);
    }

    /**
     * Gets the sum of squares of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * <p>Warning: This will raise an {@link ArithmeticException}
     * if the result is not within the range {@code [0, 2^31)}.
     *
     * @return sum of all values.
     * @throws ArithmeticException if the {@code result} overflows an {@code int}
     * @see #getAsBigInteger()
     */
    @Override
    public int getAsInt() {
        return sumSq.toIntExact();
    }

    /**
     * Gets the sum of squares of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * <p>Warning: This will raise an {@link ArithmeticException}
     * if the result is not within the range {@code [0, 2^63)}.
     *
     * @return sum of all values.
     * @throws ArithmeticException if the {@code result} overflows a {@code long}
     * @see #getAsBigInteger()
     */
    @Override
    public long getAsLong() {
        return sumSq.toLongExact();
    }

    /**
     * Gets the sum of squares of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * <p>Note that this conversion can lose information about the precision of the
     * {@code BigInteger} value.
     *
     * @return sum of squares of all values.
     * @see #getAsBigInteger()
     */
    @Override
    public double getAsDouble() {
        return sumSq.toDouble();
    }

    /**
     * Gets the sum of squares of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * @return sum of squares of all values.
     */
    @Override
    public BigInteger getAsBigInteger() {
        return sumSq.toBigInteger();
    }

    @Override
    public LongSumOfSquares combine(LongSumOfSquares other) {
        sumSq.add(other.sumSq);
        return this;
    }
}
