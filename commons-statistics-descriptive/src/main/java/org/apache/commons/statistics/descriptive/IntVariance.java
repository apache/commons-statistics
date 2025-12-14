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

import java.math.BigInteger;

/**
 * Computes the variance of the available values. The default implementation uses the
 * following definition of the <em>sample variance</em>:
 *
 * <p>\[ \tfrac{1}{n-1} \sum_{i=1}^n (x_i-\overline{x})^2 \]
 *
 * <p>where \( \overline{x} \) is the sample mean, and \( n \) is the number of samples.
 *
 * <ul>
 *   <li>The result is {@code NaN} if no values are added.</li>
 *   <li>The result is zero if there is one value in the data set.</li>
 * </ul>
 *
 * <p>The use of the term \( n − 1 \) is called Bessel's correction. This is an unbiased
 * estimator of the variance of a hypothetical infinite population. If the
 * {@link #setBiased(boolean) biased} option is enabled the normalisation factor is
 * changed to \( \frac{1}{n} \) for a biased estimator of the <em>sample variance</em>.
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
 * and at least one of the threads invokes the {@link java.util.function.IntConsumer#accept(int) accept} or
 * {@link StatisticAccumulator#combine(StatisticResult) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.IntConsumer#accept(int) accept}
 * and {@link StatisticAccumulator#combine(StatisticResult) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * @see <a href="https://en.wikipedia.org/wiki/variance">variance (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance">
 *   Algorithms for computing the variance (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Bessel%27s_correction">Bessel&#39;s correction</a>
 * @since 1.1
 */
public final class IntVariance implements IntStatistic, StatisticAccumulator<IntVariance> {
    /** Small array sample size.
     * Used to avoid computing with UInt96 then converting to UInt128. */
    static final int SMALL_SAMPLE = 10;

    /** Sum of the squared values. */
    private final UInt128 sumSq;
    /** Sum of the values. */
    private final Int128 sum;
    /** Count of values that have been added. */
    private long n;

    /** Flag to control if the statistic is biased, or should use a bias correction. */
    private boolean biased;

    /**
     * Create an instance.
     */
    private IntVariance() {
        this(UInt128.create(), Int128.create(), 0);
    }

    /**
     * Create an instance.
     *
     * @param sumSq Sum of the squared values.
     * @param sum Sum of the values.
     * @param n Count of values that have been added.
     */
    private IntVariance(UInt128 sumSq, Int128 sum, int n) {
        this.sumSq = sumSq;
        this.sum = sum;
        this.n = n;
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is {@code NaN}.
     *
     * @return {@code IntVariance} instance.
     */
    public static IntVariance create() {
        return new IntVariance();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * @param values Values.
     * @return {@code IntVariance} instance.
     */
    public static IntVariance of(int... values) {
        return createFromRange(values, 0, values.length);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code IntVariance} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static IntVariance ofRange(int[] values, int from, int to) {
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
     * @return {@code IntVariance} instance.
     */
    static IntVariance createFromRange(int[] values, int from, int to) {
        // Small arrays can be processed using the object
        final int length = to - from;
        if (length < SMALL_SAMPLE) {
            final IntVariance stat = new IntVariance();
            for (int i = from; i < to; i++) {
                stat.accept(values[i]);
            }
            return stat;
        }

        // Arrays can be processed using specialised counts knowing the maximum limit
        // for an array is 2^31 values.
        long s = 0;
        final UInt96 ss = UInt96.create();
        // Process pairs as we know two maximum value int^2 will not overflow
        // an unsigned long.
        final int end = from + (length & ~0x1);
        for (int i = from; i < end; i += 2) {
            final long x = values[i];
            final long y = values[i + 1];
            s += x + y;
            ss.addPositive(x * x + y * y);
        }
        if (end < to) {
            final long x = values[end];
            s += x;
            ss.addPositive(x * x);
        }

        // Convert
        return new IntVariance(UInt128.of(ss), Int128.of(s), length);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(int value) {
        sumSq.addPositive((long) value * value);
        sum.add(value);
        n++;
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
        return computeVarianceOrStd(sumSq, sum, n, biased, false);
    }

    /**
     * Compute the variance (or standard deviation).
     *
     * <p>The {@code std} flag controls if the result is returned as the standard deviation
     * using the {@link Math#sqrt(double) square root} function.
     *
     * @param sumSq Sum of the squared values.
     * @param sum Sum of the values.
     * @param n Count of values that have been added.
     * @param biased Flag to control if the statistic is biased, or should use a bias correction.
     * @param std Flag to control if the statistic is the standard deviation.
     * @return the variance (or standard deviation)
     */
    static double computeVarianceOrStd(UInt128 sumSq, Int128 sum, long n, boolean biased, boolean std) {
        if (n == 0) {
            return Double.NaN;
        }
        // Avoid a divide by zero
        if (n == 1) {
            return 0;
        }
        // Sum-of-squared deviations: sum(x^2) - sum(x)^2 / n
        // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
        // The precursor is computed in integer precision.
        // The divide uses double precision.
        // This ensures we avoid cancellation in the difference and use a fast divide.
        // The result is limited to by the rounding in the double computation.
        final double diff = computeSSDevN(sumSq, sum, n);
        final long n0 = biased ? n : n - 1;
        final double v = diff / IntMath.unsignedMultiplyToDouble(n, n0);
        if (std) {
            return Math.sqrt(v);
        }
        return v;
    }

    /**
     * Compute the sum-of-squared deviations multiplied by the count of values:
     * {@code n * sum(x^2) - sum(x)^2}.
     *
     * @param sumSq Sum of the squared values.
     * @param sum Sum of the values.
     * @param n Count of values that have been added.
     * @return the sum-of-squared deviations precursor
     */
    private static double computeSSDevN(UInt128 sumSq, Int128 sum, long n) {
        // Compute the term if possible using fast integer arithmetic.
        // 128-bit sum(x^2) * n will be OK when the upper 32-bits are zero.
        // 128-bit sum(x)^2 will be OK when the upper 64-bits are zero.
        // Both are safe when n < 2^32.
        if ((n >>> Integer.SIZE) == 0) {
            return sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toDouble();
        } else {
            return sumSq.toBigInteger().multiply(BigInteger.valueOf(n))
                .subtract(square(sum.toBigInteger())).doubleValue();
        }
    }

    /**
     * Compute the sum of the squared deviations from the mean.
     *
     * <p>This is a helper method used in higher order moments.
     *
     * @return the sum of the squared deviations
     */
    double computeSumOfSquaredDeviations() {
        return computeSSDevN(sumSq, sum, n) / n;
    }

    /**
     * Compute the mean.
     *
     * <p>This is a helper method used in higher order moments.
     *
     * @return the mean
     */
    double computeMean() {
        return IntMean.computeMean(sum, n);
    }

    /**
     * Convenience method to square a BigInteger.
     *
     * @param x Value
     * @return x^2
     */
    private static BigInteger square(BigInteger x) {
        return x.multiply(x);
    }

    @Override
    public IntVariance combine(IntVariance other) {
        sumSq.add(other.sumSq);
        sum.add(other.sum);
        n += other.n;
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
     * will not affect compatibility between instances during a {@link #combine(IntVariance) combine}
     * operation.
     *
     * @param v Value.
     * @return {@code this} instance
     */
    public IntVariance setBiased(boolean v) {
        biased = v;
        return this;
    }
}
