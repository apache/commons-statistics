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
 * Returns the minimum of the available values. Uses {@link Math#min(int, int) Math.min} as an
 * underlying function to compute the {@code minimum}.
 *
 * <ul>
 *   <li>The result is {@link Integer#MAX_VALUE} if no values are added.
 * </ul>
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
 * @see Math#min(int, int)
 */
public final class IntMin implements IntStatistic, StatisticAccumulator<IntMin> {

    /** Current minimum. */
    private int minimum = Integer.MAX_VALUE;

    /**
     * Create an instance.
     */
    private IntMin() {
        // No-op
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@link Integer#MAX_VALUE}.
     *
     * @return {@code Min} instance.
     */
    public static IntMin create() {
        return new IntMin();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is
     * {@link Integer#MAX_VALUE}.
     *
     * @param values Values.
     * @return {@code Min} instance.
     */
    public static IntMin of(int... values) {
        return Statistics.add(new IntMin(), values);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(int value) {
        minimum = Math.min(minimum, value);
    }

    /**
     * Gets the minimum of all input values.
     *
     * <p>When no values have been added, the result is
     * {@link Integer#MAX_VALUE}.
     *
     * @return minimum of all values.
     */
    @Override
    public int getAsInt() {
        return minimum;
    }

    @Override
    public long getAsLong() {
        return minimum;
    }

    @Override
    public double getAsDouble() {
        return minimum;
    }

    @Override
    public BigInteger getAsBigInteger() {
        return BigInteger.valueOf(minimum);
    }

    @Override
    public IntMin combine(IntMin other) {
        accept(other.getAsInt());
        return this;
    }
}
