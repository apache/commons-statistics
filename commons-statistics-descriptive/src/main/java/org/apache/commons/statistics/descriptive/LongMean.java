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
 * Computes the arithmetic mean of the available values. Uses the following definition
 * of the <em>sample mean</em>:
 *
 * <p>\[ \frac{1}{n} \sum_{i=1}^n x_i \]
 *
 * <p>where \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is {@code NaN} if no values are added.
 * </ul>
 *
 * <p>This class uses an exact integer sum to compute the mean.
 * Supports up to 2<sup>63</sup> (exclusive) observations.
 * This implementation does not check for overflow of the count.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This implementation is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least one of the threads invokes the {@link java.util.function.LongConsumer#accept(long) accept} or
 * {@link StatisticAccumulator#combine(StatisticResult) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.LongConsumer#accept(long) accept}
 * and {@link StatisticAccumulator#combine(StatisticResult) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * @since 1.1
 */
public final class LongMean implements LongStatistic, StatisticAccumulator<LongMean> {
    /** Limit where the absolute sum can exactly map to a double. Set to 2^53. */
    private static final long SMALL_SUM = 1L << 53;

    /** Sum of the values. */
    private final Int128 sum;
    /** Count of values that have been added. */
    private long n;

    /**
     * Create an instance.
     */
    private LongMean() {
        this(Int128.create(), 0);
    }

    /**
     * Create an instance.
     *
     * @param sum Sum of the values.
     * @param n Count of values that have been added.
     */
    private LongMean(Int128 sum, int n) {
        this.sum = sum;
        this.n = n;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code IntMean} instance.
     */
    public static LongMean create() {
        return new LongMean();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * @param values Values.
     * @return {@code IntMean} instance.
     */
    public static LongMean of(long... values) {
        final Int128 s = Int128.create();
        for (final long x : values) {
            s.add(x);
        }
        return new LongMean(s, values.length);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(long value) {
        sum.add(value);
        n++;
    }

    /**
     * Gets the mean of all input values.
     *
     * <p>When no values have been added, the result is {@code NaN}.
     *
     * @return mean of all values.
     */
    @Override
    public double getAsDouble() {
        return computeMean(sum, n);
    }

    /**
     * Compute the mean.
     *
     * <p>This is a helper method used in higher order moments.
     *
     * @param sum Sum of the values.
     * @param n Count of the values.
     * @return the mean
     */
    static double computeMean(Int128 sum, long n) {
        // Fast option when the sum fits within
        // the mantissa of a double.
        // Handles n=0 as NaN
        if (sum.hi64() == 0 && Math.abs(sum.lo64()) < SMALL_SUM) {
            return (double) sum.lo64() / n;
        }
        // Extended precision
        return IntMath.divide(sum, n);
    }

    @Override
    public LongMean combine(LongMean other) {
        sum.add(other.sum);
        n += other.n;
        return this;
    }
}
