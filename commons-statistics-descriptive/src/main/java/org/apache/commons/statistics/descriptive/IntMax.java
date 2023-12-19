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
 * Returns the maximum of the available values. Uses {@link Math#max(int, int) Math.max} as an
 * underlying function to compute the {@code maximum}.
 *
 * <ul>
 *   <li>The result is {@link Integer#MIN_VALUE} if no values are added.
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
 * @see Math#max(int, int)
 */
public final class IntMax implements IntStatistic, StatisticAccumulator<IntMax> {

    /** Current maximum. */
    private int maximum = Integer.MIN_VALUE;

    /**
     * Create an instance.
     */
    private IntMax() {
        // No-op
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@link Integer#MIN_VALUE}.
     *
     * @return {@code Max} instance.
     */
    public static IntMax create() {
        return new IntMax();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is
     * {@link Integer#MIN_VALUE}.
     *
     * @param values Values.
     * @return {@code Max} instance.
     */
    public static IntMax of(int... values) {
        return Statistics.add(new IntMax(), values);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(int value) {
        maximum = Math.max(maximum, value);
    }

    /**
     * Gets the maximum of all input values.
     *
     * <p>When no values have been added, the result is
     * {@link Integer#MIN_VALUE}.
     *
     * @return maximum of all values.
     */
    @Override
    public int getAsInt() {
        return maximum;
    }

    @Override
    public long getAsLong() {
        return maximum;
    }

    @Override
    public double getAsDouble() {
        return maximum;
    }

    @Override
    public BigInteger getAsBigInteger() {
        return BigInteger.valueOf(maximum);
    }

    @Override
    public IntMax combine(IntMax other) {
        accept(other.getAsInt());
        return this;
    }
}
