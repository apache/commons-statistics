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
 * Computes the kurtosis of the available values. The kurtosis is defined as:
 *
 * <p>\[ \operatorname{Kurt} = \operatorname{E}\left[ \left(\frac{X-\mu}{\sigma}\right)^4 \right] = \frac{\mu_4}{\sigma^4} \]
 *
 * <p>where \( \mu \) is the mean of \( X \), \( \sigma \) is the standard deviation of \( X \),
 * \( \operatorname{E} \) represents the expectation operator, and \( \mu_4 \) is the fourth
 * central moment.
 *
 * <p>The default implementation uses the following definition of the <em>sample kurtosis</em>:
 *
 * <p>\[ G_2 = \frac{k_4}{k_2^2} = \;
 *       \frac{n-1}{(n-2)\,(n-3)} \left[(n+1)\,\frac{m_4}{m_{2}^2} - 3\,(n-1) \right] \]
 *
 * <p>where \( k_4 \) is the unique symmetric unbiased estimator of the fourth cumulant,
 * \( k_2 \) is the symmetric unbiased estimator of the second cumulant (i.e. the <em>sample variance</em>),
 * \( m_4 \) is the fourth sample moment about the mean,
 * \( m_2 \) is the second sample moment about the mean,
 * \( \overline{x} \) is the sample mean,
 * and \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is {@code NaN} if less than 4 values are added.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN} or infinite.
 *   <li>The result is {@code NaN} if the sum of the fourth deviations from the mean is infinite.
 * </ul>
 *
 * <p>The default computation is for the adjusted Fisher–Pearson standardized moment coefficient
 * \( G_2 \). If the {@link #setBiased(boolean) biased} option is enabled the following equation
 * applies:
 *
 * <p>\[ g_2 = \frac{m_4}{m_2^2} - 3 = \frac{\tfrac{1}{n} \sum_{i=1}^n (x_i-\overline{x})^4}
 *            {\left[\tfrac{1}{n} \sum_{i=1}^n (x_i-\overline{x})^2 \right]^2} - 3 \]
 *
 * <p>In this case the computation only requires 2 values are added (i.e. the result is
 * {@code NaN} if less than 2 values are added).
 *
 * <p>Note that the computation requires division by the second central moment \( m_2 \).
 * If this is effectively zero then the result is {@code NaN}. This occurs when the value
 * \( m_2 \) approaches the machine precision of the mean: \( m_2 \le (m_1 \times 10^{-15})^2 \).
 *
 * <p>The {@link #accept(double)} method uses a recursive updating algorithm.
 *
 * <p>The {@link #of(double...)} method uses a two-pass algorithm, starting with computation
 * of the mean, and then computing the sum of deviations in a second pass.
 *
 * <p>Note that adding values using {@link #accept(double) accept} and then executing
 * {@link #getAsDouble() getAsDouble} will
 * sometimes give a different result than executing
 * {@link #of(double...) of} with the full array of values. The former approach
 * should only be used when the full array of values is not available.
 *
 * <p>Supports up to 2<sup>63</sup> (exclusive) observations.
 * This implementation does not check for overflow of the count.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>Note that this instance is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
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
 * @see <a href="https://en.wikipedia.org/wiki/Kurtosis">Kurtosis (Wikipedia)</a>
 * @since 1.1
 */
public final class Kurtosis implements DoubleStatistic, StatisticAccumulator<Kurtosis> {
    /** 2, the length limit where the biased skewness is undefined.
     * This limit effectively imposes the result m4 / m2^2 = 0 / 0 = NaN when 1 value
     * has been added. Note that when more samples are added and the variance
     * approaches zero the result is also returned as NaN. */
    private static final int LENGTH_TWO = 2;
    /** 4, the length limit where the kurtosis is undefined. */
    private static final int LENGTH_FOUR = 4;

    /**
     * An instance of {@link SumOfFourthDeviations}, which is used to
     * compute the kurtosis.
     */
    private final SumOfFourthDeviations sq;

    /** Flag to control if the statistic is biased, or should use a bias correction. */
    private boolean biased;

    /**
     * Create an instance.
     */
    private Kurtosis() {
        this(new SumOfFourthDeviations());
    }

    /**
     * Creates an instance with the sum of fourth deviations from the mean.
     *
     * @param sq Sum of fourth deviations.
     */
    Kurtosis(SumOfFourthDeviations sq) {
        this.sq = sq;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code Kurtosis} instance.
     */
    public static Kurtosis create() {
        return new Kurtosis();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Kurtosis} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code Kurtosis} instance.
     */
    public static Kurtosis of(double... values) {
        return new Kurtosis(SumOfFourthDeviations.of(values));
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code Kurtosis} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Kurtosis} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static Kurtosis ofRange(double[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return new Kurtosis(SumOfFourthDeviations.ofRange(values, from, to));
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Kurtosis} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code Kurtosis} instance.
     */
    public static Kurtosis of(int... values) {
        return new Kurtosis(SumOfFourthDeviations.of(values));
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Kurtosis} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code Kurtosis} instance.
     */
    public static Kurtosis of(long... values) {
        return new Kurtosis(SumOfFourthDeviations.of(values));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        sq.accept(value);
    }

    /**
     * Gets the kurtosis of all input values.
     *
     * <p>When fewer than 4 values have been added, the result is {@code NaN}.
     *
     * @return kurtosis of all values.
     */
    @Override
    public double getAsDouble() {
        // This method checks the sum of squared or fourth deviations is finite
        // to provide a consistent NaN when the computation is not possible.

        if (sq.n < (biased ? LENGTH_TWO : LENGTH_FOUR)) {
            return Double.NaN;
        }
        final double x2 = sq.getSumOfSquaredDeviations();
        if (!Double.isFinite(x2)) {
            return Double.NaN;
        }
        final double x4 = sq.getSumOfFourthDeviations();
        if (!Double.isFinite(x4)) {
            return Double.NaN;
        }
        // Avoid a divide by zero; for a negligible variance return NaN.
        // Note: Commons Math returns zero if variance is < 1e-19.
        final double m2 = x2 / sq.n;
        if (Statistics.zeroVariance(sq.getFirstMoment(), m2)) {
            return Double.NaN;
        }
        final double m4 = x4 / sq.n;
        if (biased) {
            return m4 / (m2 * m2) - 3;
        }
        final double n = sq.n;
        return ((n * n - 1) * m4 / (m2 * m2) - 3 * (n - 1) * (n - 1)) / ((n - 2) * (n - 3));
    }

    @Override
    public Kurtosis combine(Kurtosis other) {
        sq.combine(other.sq);
        return this;
    }

    /**
     * Sets the value of the biased flag. The default value is {@code false}.
     * See {@link Kurtosis} for details on the computing algorithm.
     *
     * <p>This flag only controls the final computation of the statistic. The value of this flag
     * will not affect compatibility between instances during a {@link #combine(Kurtosis) combine}
     * operation.
     *
     * @param v Value.
     * @return {@code this} instance
     */
    public Kurtosis setBiased(boolean v) {
        biased = v;
        return this;
    }
}
