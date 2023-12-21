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
 *   <li>The result is {@code NaN} if any of the values is {@code NaN}, or the values include
 *       infinite values of opposite sign.
 *   <li>The result is {@code +/-infinity} if values include infinite values of same sign.
 *   <li>The result is finite if all input values are finite.
 * </ul>
 *
 * <p>The {@link #accept(double)} method uses the following recursive updating algorithm
 * that protects the mean from overflow:
 * <ol>
 * <li>Initialize \( m_1 \) using the first value</li>
 * <li>For each additional value, update using <br>
 *     \( m_{i+1} = m_i + (x - m_i) / (i + 1) \)</li>
 * </ol>
 *
 * <p>The {@link #of(double...)} method uses a corrected two-pass algorithm, starting with
 * the recursive updating algorithm mentioned above, and then correcting this by adding the
 * mean deviation of the data values from the one-pass mean (see Ling (1974)).
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link StatisticAccumulator#combine(StatisticResult) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.DoubleConsumer#accept(double) accept}
 * and {@link StatisticAccumulator#combine(StatisticResult) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * <p>References:
 * <ul>
 *   <li>Ling, R.F. (1974)
 *       Comparison of Several Algorithms for Computing Sample Means and Variances.
 *       Journal of the American Statistical Association, 69, 859-866.
 *       <a href="https://doi.org/10.2307/2286154">doi: 10.2307/2286154</a>
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Mean">Mean (Wikipedia)</a>
 * @since 1.1
 */
public final class Mean implements DoubleStatistic, StatisticAccumulator<Mean> {

    /**
     * First moment used to compute the mean.
     */
    private final FirstMoment firstMoment;

    /**
     * Create an instance.
     */
    private Mean() {
        this(new FirstMoment());
    }

    /**
     * Creates an instance with a moment.
     *
     * @param m1 First moment.
     */
    Mean(FirstMoment m1) {
        firstMoment = m1;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code Mean} instance.
     */
    public static Mean create() {
        return new Mean();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>Note: {@code Mean} computed using {@link #accept(double) accept} may be
     * different from this mean.
     *
     * <p>See {@link Mean} for details on the computing algorithm.
     *
     * @param values Values.
     * @return {@code Mean} instance.
     */
    public static Mean of(double... values) {
        return new Mean(FirstMoment.of(values));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        firstMoment.accept(value);
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
        return firstMoment.getFirstMoment();
    }

    @Override
    public Mean combine(Mean other) {
        firstMoment.combine(other.firstMoment);
        return this;
    }
}
