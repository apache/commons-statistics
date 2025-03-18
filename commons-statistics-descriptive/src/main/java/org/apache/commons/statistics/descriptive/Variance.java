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
 * Computes the variance of the available values. The default implementation uses the
 * following definition of the <em>sample variance</em>:
 *
 * <p>\[ \tfrac{1}{n-1} \sum_{i=1}^n (x_i-\overline{x})^2 \]
 *
 * <p>where \( \overline{x} \) is the sample mean, and \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is {@code NaN} if no values are added.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN} or infinite.
 *   <li>The result is {@code NaN} if the sum of the squared deviations from the mean is infinite.
 *   <li>The result is zero if there is one finite value in the data set.
 * </ul>
 *
 * <p>The use of the term \( n − 1 \) is called Bessel's correction. This is an unbiased
 * estimator of the variance of a hypothetical infinite population. If the
 * {@link #setBiased(boolean) biased} option is enabled the normalisation factor is
 * changed to \( \frac{1}{n} \) for a biased estimator of the <em>sample variance</em>.
 *
 * <p>The {@link #accept(double)} method uses a recursive updating algorithm based on West's
 * algorithm (see Chan and Lewis (1979)).
 *
 * <p>The {@link #of(double...)} method uses the corrected two-pass algorithm from
 * Chan <i>et al</i>, (1983).
 *
 * <p>Note that adding values using {@link #accept(double) accept} and then executing
 * {@link #getAsDouble() getAsDouble} will
 * sometimes give a different, less accurate, result than executing
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
 * <p>References:
 * <ul>
 *   <li>Chan and Lewis (1979)
 *       Computing standard deviations: accuracy.
 *       Communications of the ACM, 22, 526-531.
 *       <a href="http://doi.acm.org/10.1145/359146.359152">doi: 10.1145/359146.359152</a>
 *   <li>Chan, Golub and Levesque (1983)
 *       Algorithms for Computing the Sample Variance: Analysis and Recommendations.
 *       American Statistician, 37, 242-247.
 *       <a href="https://doi.org/10.2307/2683386">doi: 10.2307/2683386</a>
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Variance">Variance (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Bessel%27s_correction">Bessel&#39;s correction</a>
 * @see StandardDeviation
 * @since 1.1
 */
public final class Variance implements DoubleStatistic, StatisticAccumulator<Variance> {

    /**
     * An instance of {@link SumOfSquaredDeviations}, which is used to
     * compute the variance.
     */
    private final SumOfSquaredDeviations ss;

    /** Flag to control if the statistic is biased, or should use a bias correction. */
    private boolean biased;

    /**
     * Create an instance.
     */
    private Variance() {
        this(new SumOfSquaredDeviations());
    }

    /**
     * Creates an instance with the sum of squared deviations from the mean.
     *
     * @param ss Sum of squared deviations.
     */
    Variance(SumOfSquaredDeviations ss) {
        this.ss = ss;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code Variance} instance.
     */
    public static Variance create() {
        return new Variance();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Variance} computed using {@link #accept(double) accept} may be
     * different from this variance.
     *
     * <p>See {@link Variance} for details on the computing algorithm.
     *
     * @param values Values.
     * @return {@code Variance} instance.
     */
    public static Variance of(double... values) {
        return new Variance(SumOfSquaredDeviations.of(values));
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>Note: {@code Variance} computed using {@link #accept(double) accept} may be
     * different from this variance.
     *
     * <p>See {@link Variance} for details on the computing algorithm.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Variance} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static Variance ofRange(double[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return new Variance(SumOfSquaredDeviations.ofRange(values, from, to));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        ss.accept(value);
    }

    /**
     * Gets the variance of all input values.
     *
     * <p>When no values have been added, the result is {@code NaN}.
     *
     * @return variance of all values.
     */
    @Override
    public double getAsDouble() {
        // This method checks the sum of squared is finite
        // to provide a consistent NaN when the computation is not possible.
        // Note: The SS checks for n=0 and returns NaN.
        final double m2 = ss.getSumOfSquaredDeviations();
        if (!Double.isFinite(m2)) {
            return Double.NaN;
        }
        final long n = ss.n;
        // Avoid a divide by zero
        if (n == 1) {
            return 0;
        }
        return biased ? m2 / n : m2 / (n - 1);
    }

    @Override
    public Variance combine(Variance other) {
        ss.combine(other.ss);
        return this;
    }

    /**
     * Sets the value of the biased flag. The default value is {@code false}.
     *
     * <p>If {@code false} the sum of squared deviations from the sample mean is normalised by
     * {@code n - 1} where {@code n} is the number of samples. This is Bessel's correction
     * for an unbiased estimator of the variance of a hypothetical infinite population.
     *
     * <p>If {@code true} the sum of squared deviations is normalised by the number of samples
     * {@code n}.
     *
     * <p>Note: This option only applies when {@code n > 1}. The variance of {@code n = 1} is
     * always 0.
     *
     * <p>This flag only controls the final computation of the statistic. The value of this flag
     * will not affect compatibility between instances during a {@link #combine(Variance) combine}
     * operation.
     *
     * @param v Value.
     * @return {@code this} instance
     */
    public Variance setBiased(boolean v) {
        biased = v;
        return this;
    }
}
