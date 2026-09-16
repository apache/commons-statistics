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

package org.apache.commons.statistics.distribution;

import java.util.function.ToDoubleFunction;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Implementation of the zeta distribution.
 *
 * <p>The probability mass function of \( X \) is:
 *
 * <p>\[ f(k; s) = \frac{k^-s}{\zeta(s)} \]
 *
 * <p>for \( s \gt 1 \) the exponent characterizing the distribution,
 * \( k \in \{1, 2, \dots, \infty \} \), and
 * \( \zeta(s) \) is the Riemann zeta function.
 *
 * <p>\[ \sum_{k=1}^\infty \frac{1}{k^s} \]
 *
 * <p><strong>Implementation note</strong>
 *
 * <p>The zeta distribution is the large \( N \) limit of the {@link ZipfDistribution}.
 * Note that the zeta distribution has an upper limit of positive infinity. This
 * implementation is clipped to {@link Integer#MAX_VALUE} to implement the
 * {@link DiscreteDistribution} interface. As \( s \to 1 \) the survival probability
 * function ({@code sf}) at {@code x} = 2<sup>31</sup> - 1 will become significantly larger
 * than 0 as the distribution is truncated:
 *
 * <table border=1>
 * <caption><b>Survival function values</b></caption>
 * <tr><th>s</th><th>x</th><th>sf(x; s)</th>
 * <tr><td>1.20889...</td> <td>2147483647</td> <td>0.01</td>
 * <tr><td>1.10440...</td> <td>2147483647</td> <td>0.1</td>
 * <tr><td>1.03141...</td> <td>2147483647</td> <td>0.5</td>
 * <tr><td>1.00477...</td> <td>2147483647</td> <td>0.9</td>
 * </table>
 *
 * <p>The sum of the power series can be computed using the <a
 * href="https://en.wikipedia.org/wiki/Hurwitz_zeta_function">Hurwitz zeta function</a>:
 *
 * <p>\[ \zeta(s, a) = \sum_{n=0}^\infty \frac{1}{(n+a)^s} \]
 *
 * <p>The survival probability function is implemented using \( \zeta(s, a) \) with efficient
 * evaluation time and no loss of precision. The sum of the power series in cumulative probability
 * functions uses a direct sum of the probability mass function when the number of terms is
 * practical. In all other cases the implementation uses a difference of zeta functions:
 *
 * <p>\[ \begin{aligned}
 *       \sum_{k=a}^b \frac{1}{k^s} &amp;= \sum_{n=0}^\infty \frac{1}{(n+a)^s} - \sum_{n=0}^\infty \frac{1}{(n+b+1)^s} \\
 *                                  &amp;= \zeta(s, a) - \zeta(s, b+1) \end{aligned} \]
 *
 * <p>This may incur significant cancellation in the two zeta terms. The
 * {@link #cumulativeProbability(int) cdf(x)} method should be considered imprecise as the
 * value approaches 0. Precision loss is limited to {@code b}-bits for a value \( \gt 2^{-b+1} \).
 * The {@link #probability(int, int) probability(a, b)} method should be considered potentially
 * imprecise for any range {@code b - a >= 10}.
 *
 * <p>The following table shows the value of {@code x} for different {@code s} where the survival
 * function is 0.875. Computing {@code cdf(x) = 1 - sf(x)} is expected to lose 2-bits of precision.
 * A sum of the power series requires an increasingly large run-time cost for a similar precision
 * result.
 *
 * <table border=1>
 * <caption><b>Survival function values</b></caption>
 * <tr><th>s</th><th>x</th><th>sf(x; s)</th>
 * <tr><td>1.04565...</td> <td>10</td> <td>0.875</td>
 * <tr><td>1.02575...</td> <td>100</td> <td>0.875</td>
 * <tr><td>1.01784...</td> <td>1000</td> <td>0.875</td>
 * <tr><td>1.01364...</td> <td>10000</td> <td>0.875</td>
 * <tr><td>1.01104...</td> <td>100000</td> <td>0.875</td>
 * <tr><td>1.00927...</td> <td>1000000</td> <td>0.875</td>
 * <tr><td>1.00799...</td> <td>10000000</td> <td>0.875</td>
 * </table>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Zeta_distribution">Zeta distribution (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Riemann_zeta_function">Riemann zeta function (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Hurwitz_zeta_function">Hurwitz zeta function (Wikipedia)</a>
 * @since 1.4
 */
public final class ZetaDistribution extends AbstractDiscreteDistribution {
    /** Minimum number of terms required to use the Hurwitz zeta function for cumulative
     * probability functions. Below this level a regular sum of the terms is used.
     * Note the evaluation of the Hurwitz zeta function requires multiple calls to
     * {@link Math#pow(double, double)}. If the number of probability terms is low it is
     * more efficient to sum the terms directly. */
    private static final int MIN_TERMS = 10;
    /** 2 as a {@code double}. */
    private static final double TWO = 2;
    /** 3 as a {@code double}. */
    private static final double THREE = 3;

    /** Exponent parameter of the distribution. */
    private final double exponent;
    /** zeta(s, 1) where s is the exponent of the distribution. */
    private final double zeta1;
    /** Cached value of the log(zeta(s, 1)). */
    private final double logZeta1;
    /** Cached value of the mean. */
    private double mean = Double.NaN;
    /** Cached value of the variance. */
    private double variance = Double.NaN;

    /** Create an instance.
     * @param exponent Exponent (s).
     */
    private ZetaDistribution(double exponent) {
        this.exponent = exponent;
        this.zeta1 = HurwitzZeta.value(exponent, 1);
        logZeta1 = Math.log(zeta1);
    }

    /**
     * Creates a zeta distribution.
     *
     * @param exponent Exponent.
     * @return the distribution
     * @exception IllegalArgumentException if {@code exponent <= 1} or is {@code NaN}.
     */
    public static ZetaDistribution of(double exponent) {
        if (!(exponent > 1)) {
            // <= 1 or nan
            throw new DistributionException(DistributionException.NEGATIVE,
                                            exponent);
        }
        return new ZetaDistribution(exponent);
    }

    /**
     * Gets the exponent parameter of this distribution.
     *
     * @return the exponent.
     */
    public double getExponent() {
        return exponent;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final int x) {
        if (x <= 0) {
            return 0;
        }
        return Math.pow(x, -exponent) / zeta1;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation note</strong>
     *
     * <p>This is implemented using a direct sum of the probability mass function when the
     * size {@code x1 - x0} is practical. In all other cases the implementation uses the
     * survival probability function using the identity
     * {@code P(x0 < X <= x1) = P(X > x0) - P(X > x1)}. This will suffer loss of precision
     * when the two survival functions are of a similar magnitude.
     *
     * @see #survivalProbability(int)
     */
    @Override
    public double probability(int x0, int x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH, x0, x1);
        }
        if (x0 == x1 || x1 < 1) {
            // (x0, x1] does not overlap [1, infinity]
            return 0;
        }
        // If the lower range is outside the bounds use the cumulative probability.
        // Note: This cannot compare x1 to the unlimited upper bound to use the
        // survival probability.
        if (x0 < 1) {
            return cumulativeProbability(x1);
        }
        return genHarmonic(x0 + 1, x1) / zeta1;
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return -Math.log(x) * exponent - logZeta1;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation note</strong>
     *
     * <p>This is implemented using a direct sum of the probability mass function when the
     * size {@code x} is practical. In all other cases the implementation uses
     * {@code 1 - survivalProbability(x)}. This will suffer loss of precision when the
     * survival function is above 0.5.
     *
     * <p>When the exponent {@code s} of the distribution approaches 1 a significant part
     * of the probability mass is truncated at 2<sup>31</sup> - 1, the CDF is small for
     * any {@code x}, and this function loses many bits of precision.
     *
     * <p>A high-precision method to compute the CDF is to sum the power terms directly. This
     * can be done from small to large terms; the normalizing constant
     * \( 1 / \zeta(s) \) is provided using the probability mass function at {@code x=1}.
     *
     * <pre>{@code
     * double s = ...;
     * ZetaDistribution dist = ZetaDistribution.of(s);
     * double cdfX = IntStream.range(0, x)
     *                        .mapToDouble(y -> Math.pow(x - y, -s))
     *                        .sum() * dist.probability(1);
     * }</pre>
     *
     * <p>Direct summation may require a high run-time cost and this should only be used
     * when the argument {@code x} has been bounded to a reasonable range.
     *
     * @see #survivalProbability(int)
     * @see #probability(int)
     */
    @Override
    public double cumulativeProbability(int x) {
        if (x <= 0) {
            return 0;
        }
        return genHarmonic(1, x) / zeta1;
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x <= 0) {
            return 1;
        }
        // Add 1.0 to support integer max value without overflow
        return HurwitzZeta.value(exponent, x + 1.0) / zeta1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For \( s \gt 2 \) the mean is:
     *
     * <p>\[ \frac{\zeta(s - 1)}{\zeta(s)} \]
     *
     * <p>Otherwise the mean is infinity.
     */
    @Override
    public double getMean() {
        double m = mean;
        if (Double.isNaN(m)) {
            if (exponent <= TWO) {
                m = Double.POSITIVE_INFINITY;
            } else {
                m = HurwitzZeta.value(exponent - 1, 1) / zeta1;
            }
            mean = m;
        }
        return m;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For \( s \gt 3 \) the variance is:
     *
     * <p>\[ \frac{\zeta(s)\zeta(s-2) - \zeta(s-1)^2}{\zeta(s)^2} \]
     *
     * <p>Otherwise the variance is infinity.
     */
    @Override
    public double getVariance() {
        double v = variance;
        if (Double.isNaN(v)) {
            if (exponent <= THREE) {
                v = Double.POSITIVE_INFINITY;
            } else {
                final double s2 = HurwitzZeta.value(exponent - 2, 1);
                final double s1 = HurwitzZeta.value(exponent - 1, 1);
                final double s = zeta1;
                v = (s * s2 - s1 * s1) / (s * s);
            }
            variance = v;
        }
        return v;
    }

    /**
     * Calculates the sum of terms of the generalized
     * <a href="https://mathworld.wolfram.com/HarmonicSeries.html">Harmonic
     * Series</a>.
     *
     * <pre>
     *          1
     *   sum  -----  for k in [from, to]
     *         k^m
     * </pre>
     *
     * @param a First term in the series to calculate.
     * @param b Last term in the series to calculate.
     * @return the sum
     */
    private double genHarmonic(int a, int b) {
        final double s = exponent;
        // Entirely define the sum of the series using the zeta function
        // unless the number of terms is small.
        // Number of terms is b - a + 1 so use >= not >.
        if (b - a >= MIN_TERMS) {
            // a == 1 when used in the CDF
            final double z1 = a == 1 ? zeta1 : HurwitzZeta.value(s, a);
            // Add 1.0 to support integer max value without overflow
            final double z2 = HurwitzZeta.value(s, b + 1.0);
            return z1 - z2;
        }
        return ZipfDistribution.generalizedHarmonic(a, b, s);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 1.
     *
     * @return 1.
     */
    @Override
    public int getSupportLowerBound() {
        return 1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity.
     *
     * @return {@link Integer#MAX_VALUE}
     */
    @Override
    public int getSupportUpperBound() {
        return Integer.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        return new ZetaSampler(rng, exponent);
    }

    /**
     * Sample from a zeta distribution.
     * <ol>
     * <li>Devroye, L (2015)
     * Non-uniform random variate generation.
     * Springer New York, NY. pp 550-552.</li>
     * </ol>
     */
    private static final class ZetaSampler implements DiscreteDistribution.Sampler {
        /**
         * The threshold to bias the extreme sample to 1 or infinity. Change the
         * extreme sample of the zeta distribution using the midpoint of the support
         * domain, i.e. x = 2^31 / 2; cdf(x; a) = sf(x; a) ~ 0.5.
         */
        private static final double THRESHOLD = 1.0324376395045163;

        /** Source of randomness. */
        private final UniformRandomProvider rng;
        /** a - 1. */
        private final double am1;
        /** Reciprocal of (a - 1) = 1 / (a - 1). */
        private final double ram1;
        /** b = 2^(a-1). This constants is {@code (b-1) / b}. */
        private final double bm1Db;
        /** Function to compute u in [0, 1]. */
        private final ToDoubleFunction<UniformRandomProvider> nextU;

        /**
         * Create an instance.
         *
         * @param rng Source of randomness.
         * @param a Exponent of the zeta distribution ({@code a > 1}).
         */
        ZetaSampler(UniformRandomProvider rng, double a) {
            this.rng = rng;
            am1 = a - 1;
            ram1 = 1 / am1;
            final double b = Math.pow(2, am1);
            bm1Db = b == Double.POSITIVE_INFINITY ? 1 : (b - 1) / b;
            // Note:
            // u in [0, 1]
            // u == 0 : x == inf
            // u == 1 : x == 1
            // When a -> 1 then bias to infinity; otherwise bias to 1.
            nextU = a <= THRESHOLD ?
                // u in [0, 1)
                UniformRandomProvider::nextDouble :
                // u in (0, 1]
                g -> 1.0 - g.nextDouble();
        }

        @Override
        public int sample() {
            double u;
            double v;
            double x;
            double t;
            for (;;) {
                // Generate iid uniform [0, 1] random variate U, V.
                u = nextU.applyAsDouble(rng);
                v = rng.nextDouble();
                // X = floor ( U^{-1/(a-1)} ) , X in [1, inf]
                x = Math.floor(Math.pow(u, -ram1));
                t = Math.pow(1 + 1 / x, am1);

                // Until:
                //    T-1    T
                // VX --- <= -
                //    b-1    b

                // If (a-1) -> inf then t & b -> inf; b >= t
                // Avoid inf / inf = NaN and accept.
                // Large a will mostly sample X=1.

                // v * x * (t - 1) / (b - 1) <= t / b
                // Rearrange terms to ratios of similar magnitude and guard infinity:
                // v * x <= (t / (t - 1)) * ((b - 1) / b)
                final double tDtm1 = t == Double.POSITIVE_INFINITY ? 1 : t / (t - 1);
                if (v * x <= tDtm1 * bm1Db) {
                    // Truncates x >= 2^31 to integer max
                    return (int) x;
                }
            }
        }
    }
}
