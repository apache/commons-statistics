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
 * Computes the variance of the available values. Uses the following definition
 * of the <em>sample variance</em>:
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
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>Note that this instance is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link DoubleStatisticAccumulator#combine(DoubleStatistic) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.DoubleConsumer#accept(double) accept}
 * and {@link DoubleStatisticAccumulator#combine(DoubleStatistic) combine}
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
 * @since 1.1
 */
public final class Variance implements DoubleStatistic, DoubleStatisticAccumulator<Variance> {

    /**
     * An instance of {@link SumOfSquaredDeviations}, which is used to
     * compute the variance.
     */
    private final SumOfSquaredDeviations ss;

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
    private Variance(SumOfSquaredDeviations ss) {
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
        final double sumOfSquaredDev = ss.getSumOfSquaredDeviations();
        final long n = ss.n;
        if (n == 0) {
            return Double.NaN;
        } else if (n == 1 && Double.isFinite(sumOfSquaredDev)) {
            return 0;
        }
        return sumOfSquaredDev / (n - 1.0);
    }

    @Override
    public Variance combine(Variance other) {
        ss.combine(other.ss);
        return this;
    }
}
