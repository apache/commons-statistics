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
 * Computes the geometric mean of the available values. Uses the following definition
 * of the geometric mean:
 *
 * <p>\[ \left(\prod_{i=1}^n x_i\right)^\frac{1}{n} \]
 *
 * <p>where \( n \) is the number of samples. This implementation uses the log scale:
 *
 * <p>\[ \exp{\left( {\frac{1}{n}\sum_{i=1}^n \ln x_i} \right)} \]
 *
 * <ul>
 *   <li>The result is {@code NaN} if no values are added.</li>
 *   <li>The result is {@code NaN} if any of the values is {@code NaN}.</li>
 *   <li>The result is {@code NaN} if any of the values is negative.</li>
 *   <li>The result is {@code +infinity} if all values are in the range {@code (0, +infinity]}
 *       and at least one value is {@code +infinity}.</li>
 *   <li>The result is {@code 0} if all values are in the range {@code [0, +infinity)}
 *       and at least one value is zero.</li>
 *   <li>The result is {@code NaN} if all values are in the range {@code [0, +infinity]}
 *       and at least one value is zero, and one value is {@code +infinity}.</li>
 * </ul>
 *
 * <p>Supports up to 2<sup>63</sup> (exclusive) observations.
 * This implementation does not check for overflow of the count.
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
 * @see <a href="https://en.wikipedia.org/wiki/Geometric_mean">Geometric mean (Wikipedia)</a>
 * @see SumOfLogs
 * @since 1.1
 */
public final class GeometricMean implements DoubleStatistic, StatisticAccumulator<GeometricMean> {
    /** Count of values that have been added. */
    private long n;

    /**
     * Sum of logs used to compute the geometric mean.
     */
    private final SumOfLogs sumOfLogs;

    /**
     * Create an instance.
     */
    private GeometricMean() {
        this(SumOfLogs.create(), 0);
    }

    /**
     * Create an instance.
     *
     * @param sumOfLogs Sum of logs.
     * @param n Count of values.
     */
    private GeometricMean(SumOfLogs sumOfLogs, long n) {
        this.sumOfLogs = sumOfLogs;
        this.n = n;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code GeometricMean} instance.
     */
    public static GeometricMean create() {
        return new GeometricMean();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is {@code NaN}.
     *
     * @param values Values.
     * @return {@code GeometricMean} instance.
     */
    public static GeometricMean of(double... values) {
        return new GeometricMean(SumOfLogs.of(values), values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>When the range is empty, the result is {@code NaN}.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code GeometricMean} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static GeometricMean ofRange(double[] values, int from, int to) {
        // Range checks performed by the sum-of-logs
        return new GeometricMean(SumOfLogs.ofRange(values, from, to), to - from);
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is {@code NaN}.
     *
     * @param values Values.
     * @return {@code GeometricMean} instance.
     */
    public static GeometricMean of(int... values) {
        return new GeometricMean(SumOfLogs.of(values), values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>When the range is empty, the result is {@code NaN}.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code GeometricMean} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static GeometricMean ofRange(int[] values, int from, int to) {
        // Range checks performed by the sum-of-logs
        return new GeometricMean(SumOfLogs.ofRange(values, from, to), to - from);
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is {@code NaN}.
     *
     * @param values Values.
     * @return {@code GeometricMean} instance.
     */
    public static GeometricMean of(long... values) {
        return new GeometricMean(SumOfLogs.of(values), values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>When the range is empty, the result is {@code NaN}.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code GeometricMean} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static GeometricMean ofRange(long[] values, int from, int to) {
        // Range checks performed by the sum-of-logs
        return new GeometricMean(SumOfLogs.ofRange(values, from, to), to - from);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        n++;
        sumOfLogs.accept(value);
    }

    /**
     * Gets the geometric mean of all input values.
     *
     * <p>When no values have been added, the result is {@code NaN}.
     *
     * @return geometric mean of all values.
     */
    @Override
    public double getAsDouble() {
        return computeGeometricMean(n, sumOfLogs);
    }

    @Override
    public GeometricMean combine(GeometricMean other) {
        n += other.n;
        sumOfLogs.combine(other.sumOfLogs);
        return this;
    }

    /**
     * Compute the geometric mean.
     *
     * @param n Count of values.
     * @param sumOfLogs Sum of logs.
     * @return the geometric mean
     */
    static double computeGeometricMean(long n, SumOfLogs sumOfLogs) {
        return n == 0 ?
            Double.NaN :
            Math.exp(sumOfLogs.getAsDouble() / n);
    }
}
