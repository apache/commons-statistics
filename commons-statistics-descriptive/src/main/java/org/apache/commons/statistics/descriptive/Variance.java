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
 * Computes the variance of a set of values. By default, the
 * "sample variance" is computed. The definitional formula for sample
 * variance is:
 * <p>
 * sum((x_i - mean)^2) / (n - 1)
 * <p>This formula does not have good numerical properties, so this
 * implementation does not use it to compute the statistic.
 * <ul>
 * <li> The {@link #accept(double)} method computes the variance using
 * updating formulae based on West's algorithm, as described in
 * <a href="http://doi.acm.org/10.1145/359146.359152"> Chan, T. F. and
 * J. G. Lewis 1979, <i>Communications of the ACM</i>,
 * vol. 22 no. 9, pp. 526-531.</a></li>
 *
 * <li> The {@link #of(double...)} method leverages the fact that it has the
 * full array of values in memory to execute a two-pass algorithm.
 * Specifically, this method uses the "corrected two-pass algorithm" from
 * Chan, Golub, Levesque, <i>Algorithms for Computing the Sample Variance</i>,
 * American Statistician, vol. 37, no. 3 (1983) pp. 242-247.</li></ul>
 *
 * Note that adding values using {@code accept} and then executing {@code getAsDouble} will
 * sometimes give a different, less accurate, result than executing
 * {@code of} with the full array of values. The former approach
 * should only be used when the full array of values is not available.
 *
 * <p>
 * Returns <code>Double.NaN</code> if no data values have been added and
 * returns <code>0</code> if there is just one finite value in the data set.
 * Note that <code>Double.NaN</code> may also be returned if the input includes
 * <code>Double.NaN</code> and / or infinite values.
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link DoubleStatisticAccumulator#combine(DoubleStatistic) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use <code>accept()</code> and <code>combine()</code>
 * as <code>accumulator</code> and <code>combiner</code> functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * @since 1.1
 */
public abstract class Variance implements DoubleStatistic, DoubleStatisticAccumulator<Variance> {

    /**
     * Create a Variance instance.
     */
    Variance() {
        // No-op
    }

    /**
     * Creates a {@code Variance} implementation which does not store the input value(s) it consumes.
     *
     * <p>The result is <code>NaN</code> if:
     * <ul>
     *     <li>no values have been added,</li>
     *     <li>any of the values is <code>NaN</code>, or</li>
     *     <li>an infinite value of either sign is encountered</li>
     * </ul>
     *
     * @return {@code Variance} implementation.
     */
    public static Variance create() {
        return new StorelessSampleVariance();
    }

    /**
     * Returns a {@code Variance} instance that has the variance of all input values, or <code>NaN</code>
     * if:
     * <ul>
     *     <li>the input array is empty,</li>
     *     <li>any of the values is <code>NaN</code>, or</li>
     *     <li>an infinite value of either sign is encountered</li>
     *     <li>if the sum of the squared deviations from the mean is infinite</li>
     * </ul>
     *
     * <p>Note: {@code Variance} computed using {@link Variance#accept Variance.accept()} may be different
     * from this variance.
     *
     * <p>See {@link Variance} for details on the computing algorithm.
     *
     * @param values Values.
     * @return {@code Variance} instance.
     */
    public static Variance of(double... values) {
        final double mean = Mean.of(values).getAsDouble();
        if (!Double.isFinite(mean)) {
            return StorelessSampleVariance.create(Math.abs(mean), mean, values.length, mean);
        }
        double accum = 0.0;
        double dev;
        double accum2 = 0.0;
        double squaredDevSum;
        for (final double value : values) {
            dev = value - mean;
            accum += dev * dev;
            accum2 += dev;
        }
        final double accum2Squared = accum2 * accum2;
        final long n = values.length;
        // The sum of squared deviations is accum - (accum2Squared / n).
        // To prevent squaredDevSum from spuriously attaining a NaN value
        // when accum is infinite, assign it an infinite value which is its intended value.
        if (accum == Double.POSITIVE_INFINITY) {
            squaredDevSum = Double.POSITIVE_INFINITY;
        } else {
            squaredDevSum = accum - (accum2Squared / n);
        }
        return StorelessSampleVariance.create(squaredDevSum, mean, n, accum2 + (mean * n));
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     * @param value Value.
     */
    @Override
    public abstract void accept(double value);

    /**
     * Gets the variance of all input values.
     *
     * <p>The result is <code>NaN</code> if :
     * <ul>
     *     <li>the input array is empty,</li>
     *     <li>any of the values is <code>NaN</code>, or</li>
     *     <li>an infinite value of either sign is encountered</li>
     * </ul>
     *
     * <p>The result is <code>0</code> if there is just one finite value in the data set.
     *
     * @return {@code Variance} of all values seen so far.
     */
    @Override
    public abstract double getAsDouble();

    /** {@inheritDoc} */
    @Override
    public abstract Variance combine(Variance other);

    /**
     * {@code Variance} implementation that does not store the input value(s) processed so far.
     */
    private static class StorelessSampleVariance extends Variance {

        /**
         * An instance of {@link SumOfSquaredDeviations}, which is used to
         * compute the variance.
         */
        private final SumOfSquaredDeviations squaredDeviationSum;

        /**
         * Creates a StorelessVariance instance with the sum of squared
         * deviations from the mean.
         *
         * @param squaredDevSum Sum of squared deviations.
         * @param mean Mean of values.
         * @param n Number of values.
         * @param sumOfValues Sum of values.
         */
        private StorelessSampleVariance(double squaredDevSum, double mean, long n, double sumOfValues) {
            squaredDeviationSum = new SumOfSquaredDeviations(squaredDevSum, mean, n, sumOfValues);
        }

        /**
         * Create a SumOfSquaredDeviations instance.
         */
        StorelessSampleVariance() {
            squaredDeviationSum = new SumOfSquaredDeviations();
        }

        /**
         * Creates a StorelessVariance instance with the sum of squared
         * deviations from the mean.
         *
         * @param squaredDevSum Sum of squared deviations.
         * @param mean Mean of values.
         * @param n Number of values.
         * @param sumOfValues Sum of values.
         * @return A StorelessVariance instance.
         */
        static StorelessSampleVariance create(double squaredDevSum, double mean, long n, double sumOfValues) {
            return new StorelessSampleVariance(squaredDevSum, mean, n, sumOfValues);
        }

        @Override
        public void accept(double value) {
            squaredDeviationSum.accept(value);
        }

        @Override
        public double getAsDouble() {
            final double sumOfSquaredDev = squaredDeviationSum.getSumOfSquaredDeviations();
            final double n = squaredDeviationSum.n;
            if (n == 0) {
                return Double.NaN;
            } else if (n == 1 && Double.isFinite(sumOfSquaredDev)) {
                return 0;
            }
            return sumOfSquaredDev / (n - 1);
        }

        @Override
        public Variance combine(Variance other) {
            final StorelessSampleVariance that = (StorelessSampleVariance) other;
            squaredDeviationSum.combine(that.squaredDeviationSum);
            return this;
        }
    }
}
