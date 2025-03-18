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
 * Computes the skewness of the available values. The skewness is defined as:
 *
 * <p>\[ \gamma_1 = \operatorname{E}\left[ \left(\frac{X-\mu}{\sigma}\right)^3 \right] = \frac{\mu_3}{\sigma^3} \]
 *
 * <p>where \( \mu \) is the mean of \( X \), \( \sigma \) is the standard deviation of \( X \),
 * \( \operatorname{E} \) represents the expectation operator, and \( \mu_3 \) is the third
 * central moment.
 *
 * <p>The default implementation uses the following definition of the <em>sample skewness</em>:
 *
 * <p>\[ G_1 = \frac{k_3}{k_2^{3/2}} = \frac{\sqrt{n(n-1)}}{n-2}\; g_1 = \frac{n^2}{(n-1)(n-2)}\;
 *       \frac{\tfrac{1}{n} \sum_{i=1}^n (x_i-\overline{x})^3}
 *            {\left[\tfrac{1}{n-1} \sum_{i=1}^n (x_i-\overline{x})^2 \right]^{3/2}} \]
 *
 * <p>where \( k_3 \) is the unique symmetric unbiased estimator of the third cumulant,
 * \( k_2 \) is the symmetric unbiased estimator of the second cumulant (i.e. the <em>sample variance</em>),
 * \( g_1 \) is a method of moments estimator (see below), \( \overline{x} \) is the sample mean,
 * and \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is {@code NaN} if less than 3 values are added.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN} or infinite.
 *   <li>The result is {@code NaN} if the sum of the cubed deviations from the mean is infinite.
 * </ul>
 *
 * <p>The default computation is for the adjusted Fisher–Pearson standardized moment coefficient
 * \( G_1 \). If the {@link #setBiased(boolean) biased} option is enabled the following equation
 * applies:
 *
 * <p>\[ g_1 = \frac{m_3}{m_2^{3/2}} = \frac{\tfrac{1}{n} \sum_{i=1}^n (x_i-\overline{x})^3}
 *            {\left[\tfrac{1}{n} \sum_{i=1}^n (x_i-\overline{x})^2 \right]^{3/2}} \]
 *
 * <p>where \( g_2 \) is a method of moments estimator,
 * \( m_3 \) is the (biased) sample third central moment and \( m_2^{3/2} \) is the
 * (biased) sample second central moment.
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
 * @see <a href="https://en.wikipedia.org/wiki/Skewness">Skewness (Wikipedia)</a>
 * @since 1.1
 */
public final class Skewness implements DoubleStatistic, StatisticAccumulator<Skewness> {
    /** 2, the length limit where the biased skewness is undefined.
     * This limit effectively imposes the result m3 / m2^1.5 = 0 / 0 = NaN when 1 value
     * has been added. Note that when more samples are added and the variance
     * approaches zero the result is also returned as NaN. */
    private static final int LENGTH_TWO = 2;
    /** 3, the length limit where the unbiased skewness is undefined. */
    private static final int LENGTH_THREE = 3;

    /**
     * An instance of {@link SumOfCubedDeviations}, which is used to
     * compute the skewness.
     */
    private final SumOfCubedDeviations sc;

    /** Flag to control if the statistic is biased, or should use a bias correction. */
    private boolean biased;

    /**
     * Create an instance.
     */
    private Skewness() {
        this(new SumOfCubedDeviations());
    }

    /**
     * Creates an instance with the sum of cubed deviations from the mean.
     *
     * @param sc Sum of cubed deviations.
     */
    Skewness(SumOfCubedDeviations sc) {
        this.sc = sc;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code Skewness} instance.
     */
    public static Skewness create() {
        return new Skewness();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Skewness} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code Skewness} instance.
     */
    public static Skewness of(double... values) {
        return new Skewness(SumOfCubedDeviations.of(values));
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code Skewness} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Skewness} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static Skewness ofRange(double[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return new Skewness(SumOfCubedDeviations.ofRange(values, from, to));
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Skewness} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code Skewness} instance.
     */
    public static Skewness of(int... values) {
        return new Skewness(SumOfCubedDeviations.of(values));
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code Skewness} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Skewness} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static Skewness ofRange(int[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return new Skewness(SumOfCubedDeviations.ofRange(values, from, to));
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Skewness} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @return {@code Skewness} instance.
     */
    public static Skewness of(long... values) {
        return new Skewness(SumOfCubedDeviations.of(values));
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code Skewness} computed using {@link #accept(double) accept} may be
     * different from this instance.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Skewness} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static Skewness ofRange(long[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return new Skewness(SumOfCubedDeviations.ofRange(values, from, to));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        sc.accept(value);
    }

    /**
     * Gets the skewness of all input values.
     *
     * <p>When fewer than 3 values have been added, the result is {@code NaN}.
     *
     * @return skewness of all values.
     */
    @Override
    public double getAsDouble() {
        // This method checks the sum of squared or cubed deviations is finite
        // and the value of the biased variance
        // to provide a consistent result when the computation is not possible.

        if (sc.n < (biased ? LENGTH_TWO : LENGTH_THREE)) {
            return Double.NaN;
        }
        final double x2 = sc.getSumOfSquaredDeviations();
        if (!Double.isFinite(x2)) {
            return Double.NaN;
        }
        final double x3 = sc.getSumOfCubedDeviations();
        if (!Double.isFinite(x3)) {
            return Double.NaN;
        }
        // Avoid a divide by zero; for a negligible variance return NaN.
        // Note: Commons Math returns zero if variance is < 1e-19.
        final double m2 = x2 / sc.n;
        if (Statistics.zeroVariance(sc.getFirstMoment(), m2)) {
            return Double.NaN;
        }
        // denom = pow(m2, 1.5)
        final double denom = Math.sqrt(m2) * m2;
        final double m3 = x3 / sc.n;
        double g1 = m3 / denom;
        if (!biased) {
            final double n = sc.n;
            g1 *= Math.sqrt(n * (n - 1)) / (n - 2);
        }
        return g1;
    }

    @Override
    public Skewness combine(Skewness other) {
        sc.combine(other.sc);
        return this;
    }

    /**
     * Sets the value of the biased flag. The default value is {@code false}.
     * See {@link Skewness} for details on the computing algorithm.
     *
     * <p>This flag only controls the final computation of the statistic. The value of this flag
     * will not affect compatibility between instances during a {@link #combine(Skewness) combine}
     * operation.
     *
     * @param v Value.
     * @return {@code this} instance
     */
    public Skewness setBiased(boolean v) {
        biased = v;
        return this;
    }
}
