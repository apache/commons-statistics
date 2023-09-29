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
 * Computes the skewness of the available values. Uses the following definition
 * of the <em>sample skewness</em>:
 *
 * <p>\[ \frac{n^2}{(n-1)(n-2)}\;
 *       \frac{\tfrac{1}{n} \sum_{i=1}^n (x_i-\overline{x})^3}
 *            {\left[\tfrac{1}{n-1} \sum_{i=1}^n (x_i-\overline{x})^2 \right]^{3/2}} \]
 *
 * <p>where \( \overline{x} \) is the sample mean, and \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is {@code NaN} if less than 3 values are added.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN} or infinite.
 *   <li>The result is {@code NaN} if the sum of the cubed deviations from the mean is infinite.
 * </ul>
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
 * @see <a href="https://en.wikipedia.org/wiki/Skewness">Skewness (Wikipedia)</a>
 * @since 1.1
 */
public final class Skewness implements DoubleStatistic, DoubleStatisticAccumulator<Skewness> {
    /** 3, the length limit where the skewness is undefined. */
    private static final int LENGTH_THREE = 3;

    /**
     * An instance of {@link SumOfCubedDeviations}, which is used to
     * compute the skewness.
     */
    private final SumOfCubedDeviations sc;

    /**
     * Create an instance.
     */
    private Skewness() {
        this(new SumOfCubedDeviations());
    }

    /**
     * Creates an instance with the sum of cubed deviations from the mean.
     *
     * @param ss Sum of cubed deviations.
     */
    private Skewness(SumOfCubedDeviations ss) {
        this.sc = ss;
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
        // to provide a consistent NaN when the computation is not possible.

        if (sc.n < LENGTH_THREE) {
            return Double.NaN;
        }
        final double m2 = sc.getSumOfSquaredDeviations();
        if (!Double.isFinite(m2)) {
            return Double.NaN;
        }
        final double m3 = sc.getSumOfCubedDeviations();
        if (!Double.isFinite(m3)) {
            return Double.NaN;
        }
        // Avoid a divide by zero; for a negligible variance return 0.
        // Note: Commons Math returns zero if variance is < 1e-19.
        final double variance = m2 / (sc.n - 1);
        final double n = sc.n;
        final double denom = (n - 1) * (n - 2) * Math.sqrt(variance) * variance;
        if (denom == 0) {
            return 0;
        }
        return n * m3 / denom;
    }

    @Override
    public Skewness combine(Skewness other) {
        sc.combine(other.sc);
        return this;
    }
}
