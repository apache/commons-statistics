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
 * Returns the sum of the available values.
 *
 * <ul>
 *   <li>The result is zero if no values are added.
 * </ul>
 *
 * <p>This class uses an exact integer sum. The exact sum is
 * returned using {@link #getAsBigInteger()}. Methods that return {@code int} or
 * {@code long} primitives will raise an exception if the result overflows.
 * The {@code long} value is safe up to the maximum array length for any input
 * {@code int[]} data. The {@code long} value can overflow when instances are combined.
 *
 * <p>Note that the implementation does not use {@code BigInteger} arithmetic; for
 * performance the sum is computed using primitives to create a signed 128-bit integer.
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
public final class IntSum implements IntStatistic, StatisticAccumulator<IntSum> {
    /** Sum of the values. */
    private final Int128 sum;

    /**
     * Create an instance.
     */
    private IntSum() {
        this(Int128.create());
    }

    /**
     * Create an instance.
     *
     * @param sum Sum of the values.
     */
    private IntSum(Int128 sum) {
        this.sum = sum;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is zero.
     *
     * @return {@code IntSum} instance.
     */
    public static IntSum create() {
        return new IntSum();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is zero.
     *
     * <p>The {@link #getAsLong()} result is valid for any input {@code int[]} length;
     * the {@link #getAsInt()} result may overflow.
     *
     * @param values Values.
     * @return {@code IntSum} instance.
     */
    public static IntSum of(int... values) {
        // Sum of an array cannot exceed a 64-bit long
        long s = 0;
        for (final int x : values) {
            s += x;
        }
        // Convert
        return new IntSum(Int128.of(s));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(int value) {
        sum.add(value);
    }

    /**
     * Gets the sum of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * <p>Warning: This will raise an {@link ArithmeticException}
     * if the result is not within the range {@code [-2^31, 2^31)}.
     *
     * @return sum of all values.
     * @throws ArithmeticException if the {@code result} overflows an {@code int}
     * @see #getAsBigInteger()
     */
    @Override
    public int getAsInt() {
        return sum.toIntExact();
    }

    /**
     * Gets the sum of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * <p>Warning: This will raise an {@link ArithmeticException}
     * if the result is not within the range {@code [-2^63, 2^63)}.
     *
     * @return sum of all values.
     * @throws ArithmeticException if the {@code result} overflows a {@code long}
     * @see #getAsBigInteger()
     */
    @Override
    public long getAsLong() {
        return sum.toLongExact();
    }

    /**
     * Gets the sum of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * <p>Note that this conversion can lose information about the precision of the
     * {@code BigInteger} value.
     *
     * @return sum of all values.
     * @see #getAsBigInteger()
     */
    @Override
    public double getAsDouble() {
        return sum.toDouble();
    }

    /**
     * Gets the sum of all input values.
     *
     * <p>When no values have been added, the result is zero.
     *
     * @return sum of all values.
     */
    @Override
    public BigInteger getAsBigInteger() {
        return sum.toBigInteger();
    }

    @Override
    public IntSum combine(IntSum other) {
        sum.add(other.sum);
        return this;
    }
}
