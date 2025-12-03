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
 * Computes the standard deviation of the available values. The default implementation uses the
 * following definition of the <em>sample standard deviation</em>:
 *
 * <p>\[ \sqrt{ \tfrac{1}{n-1} \sum_{i=1}^n (x_i-\overline{x})^2 } \]
 *
 * <p>where \( \overline{x} \) is the sample mean, and \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is {@code NaN} if no values are added.
 *   <li>The result is zero if there is one value in the data set.
 * </ul>
 *
 * <p>The use of the term \( n − 1 \) is called Bessel's correction. Omitting the square root,
 * this provides an unbiased estimator of the variance of a hypothetical infinite population. If the
 * {@link #setBiased(boolean) biased} option is enabled the normalisation factor is
 * changed to \( \frac{1}{n} \) for a biased estimator of the <em>sample variance</em>.
 * Note however that square root is a concave function and thus introduces negative bias
 * (by Jensen's inequality), which depends on the distribution, and thus the corrected sample
 * standard deviation (using Bessel's correction) is less biased, but still biased.
 *
 * <p>The implementation uses an exact integer sum to compute the scaled (by \( n \))
 * sum of squared deviations from the mean; this is normalised by the scaled correction factor.
 *
 * <p>\[ \frac {n \times \sum_{i=1}^n x_i^2 - (\sum_{i=1}^n x_i)^2}{n \times (n - 1)} \]
 *
 * <p>Supports up to 2<sup>63</sup> (exclusive) observations.
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
 * @see <a href="https://en.wikipedia.org/wiki/Standard_deviation">Standard deviation (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Bessel%27s_correction">Bessel&#39;s correction</a>
 * @see <a href="https://en.wikipedia.org/wiki/Jensen%27s_inequality">Jensen&#39;s inequality</a>
 * @see LongVariance
 * @since 1.1
 */
public final class LongStandardDeviation implements LongStatistic, StatisticAccumulator<LongStandardDeviation> {

    /** Sum of the squared values. */
    private final UInt192 sumSq;
    /** Sum of the values. */
    private final Int128 sum;
    /** Count of values that have been added. */
    private long n;

    /** Flag to control if the statistic is biased, or should use a bias correction. */
    private boolean biased;

    /**
     * Create an instance.
     */
    private LongStandardDeviation() {
        this(UInt192.create(), Int128.create(), 0);
    }

    /**
     * Create an instance.
     *
     * @param sumSq Sum of the squared values.
     * @param sum Sum of the values.
     * @param n Count of values that have been added.
     */
    private LongStandardDeviation(UInt192 sumSq, Int128 sum, int n) {
        this.sumSq = sumSq;
        this.sum = sum;
        this.n = n;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code LongStandardDeviation} instance.
     */
    public static LongStandardDeviation create() {
        return new LongStandardDeviation();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * @param values Values.
     * @return {@code LongStandardDeviation} instance.
     */
    public static LongStandardDeviation of(long... values) {
        return createFromRange(values, 0, values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code LongStandardDeviation} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static LongStandardDeviation ofRange(long[] values, int from, int to) {
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
     * @return {@code LongStandardDeviation} instance.
     */
    static LongStandardDeviation createFromRange(long[] values, int from, int to) {
        // Note: Arrays could be processed using specialised counts knowing the maximum limit
        // for an array is 2^31 values. Requires a UInt160.

        final Int128 s = Int128.create();
        final UInt192 ss = UInt192.create();
        for (int i = from; i < to; i++) {
            final long x = values[i];
            s.add(x);
            ss.addSquare(x);
        }
        return new LongStandardDeviation(ss, s, to - from);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(long value) {
        sumSq.addSquare(value);
        sum.add(value);
        n++;
    }

    /**
     * Gets the standard deviation of all input values.
     *
     * <p>When no values have been added, the result is {@code NaN}.
     *
     * @return standard deviation of all values.
     */
    @Override
    public double getAsDouble() {
        return LongVariance.computeVarianceOrStd(sumSq, sum, n, biased, true);
    }

    @Override
    public LongStandardDeviation combine(LongStandardDeviation other) {
        sumSq.add(other.sumSq);
        sum.add(other.sum);
        n += other.n;
        return this;
    }

    /**
     * Sets the value of the biased flag. The default value is {@code false}. The bias
     * term refers to the computation of the variance; the standard deviation is returned
     * as the square root of the biased or unbiased <em>sample variance</em>. For further
     * details see {@link LongVariance#setBiased(boolean) LongVariance.setBiased}.
     *
     * <p>This flag only controls the final computation of the statistic. The value of
     * this flag will not affect compatibility between instances during a
     * {@link #combine(LongStandardDeviation) combine} operation.
     *
     * @param v Value.
     * @return {@code this} instance
     * @see LongVariance#setBiased(boolean)
     */
    public LongStandardDeviation setBiased(boolean v) {
        biased = v;
        return this;
    }
}
