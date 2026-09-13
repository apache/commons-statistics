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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.RejectionInversionZipfSampler;

/**
 * Implementation of the Zipf distribution.
 *
 * <p>The probability mass function of \( X \) is:
 *
 * <p>\[ f(k; N, s) = \frac{1/k^s}{H_{N,s}} \]
 *
 * <p>for \( N \in \{1, 2, 3, \dots\} \) the number of elements,
 * \( s \gt 0 \) the exponent characterizing the distribution,
 * \( k \in \{1, 2, \dots, N\} \) the element rank, and
 * \( H_{N,s} \) is the normalizing constant which corresponds to the
 * <a href="https://en.wikipedia.org/wiki/Harmonic_number#Generalized_harmonic_numbers">
 * generalized harmonic number</a> of order N of s.
 *
 * <p>\[ \sum_{k=1}^N \frac{1}{k^s} \]
 *
 * <p><strong>Implementation note</strong>
 *
 * <p>The sum of the power series in harmonic numbers or cumulative
 * probability functions may be computed
 * using the <a href="https://en.wikipedia.org/wiki/Hurwitz_zeta_function">Hurwitz zeta function</a>
 * when \( s \gt 1 \):
 *
 * <p>\[ \begin{aligned}
 *       \sum_{k=a}^b \frac{1}{k^s} &amp;= \sum_{n=0}^\infty \frac{1}{(n+a)^s} - \sum_{n=0}^\infty \frac{1}{(n+b+1)^s} \\
 *                                  &amp;= \zeta(s, a) - \zeta(s, b+1) \end{aligned} \]
 *
 * <p>This is performed unless there is significant cancellation in the two zeta terms.
 * In all other cases the sum is computed by direct summation of terms with performance
 * implications for large \( N \). Construction of the distribution, and the first
 * call to {@link #getMean()} or {@link #getVariance()}, is \( O(N) \); each call to
 * {@link #cumulativeProbability(int) cumulativeProbability(x)} or
 * {@link #survivalProbability(int) survivalProbability(x)} is \( O(x) \); the inverse
 * probability functions perform a search using \( O(\log N) \) cumulative probability
 * evaluations. A number of elements of order 2<sup>31</sup> requires billions of
 * {@code Math.pow} evaluations for construction alone. Take this run-time cost into
 * account when the parameters are derived from untrusted input, and bound the number
 * of elements accordingly. Sampling (see {@link #createSampler(UniformRandomProvider)
 * createSampler}) uses a rejection method with a cost per sample that does not depend
 * on the number of elements.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Zipf's_law">Zipf distribution (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Hurwitz_zeta_function">Hurwitz zeta function (Wikipedia)</a>
 */
public final class ZipfDistribution extends AbstractDiscreteDistribution {
    /** Minimum number of terms required to use the Hurwitz zeta function for cumulative
     * probability functions. Below this level a regular sum of the terms is used.
     * Note the evaluation of the Hurwitz zeta function requires multiple calls to
     * {@link Math#pow(double, double)}. If the number of probability terms is low it is
     * more efficient to sum the terms directly. */
    private static final int MIN_TERMS = 10;
    /** Maximum ratio between the small term and large term to avoid significant cancellation
     * in the sum {@code large - small}, i.e. {@code small / large <= ratio}.
     * Note that the ratio {@code 1 - 2^-b} will lose {@code b - 1} bits in the result.
     * This value allows a loss of 2-bits. */
    private static final double MAX_RATIO = 0.875;

    /** Number of elements. */
    private final int numberOfElements;
    /** Exponent parameter of the distribution. */
    private final double exponent;
    /** Cached value of the N-th generalized harmonic. */
    private final double nthHarmonic;
    /** Cached value of the log of the N-th generalized harmonic. */
    private final double logNthHarmonic;
    /** Cached value of the N-th generalized harmonic using (exponent - 1). */
    private double nthHarmonicM1 = Double.NaN;
    /** Cached value of the N-th generalized harmonic using (exponent - 2). */
    private double nthHarmonicM2 = Double.NaN;
    /** Function to compute the generalised harmonic series. */
    private final HarmonicSeries genHarmonic;

    /**
     * Compute the value of the generalised harmonic series.
     * <pre>
     *    b      1
     * sum      ---
     *    k=a     s
     *           k
     * </pre>
     */
    @FunctionalInterface
    private interface HarmonicSeries {
        /**
         * Compute the value.
         * <pre>
         *    b      1
         * sum      ---
         *    k=a     s
         *           k
         * </pre>
         * <p>The exponent is assumed to be known.
         * @param a Lower bound.
         * @param b Upper bound.
         * @return value
         */
        double value(int a, int b);
    }

    /**
     * Compute the value of the generalised harmonic series using a difference of Hurwitz
     * zeta functions.
     * <pre>
     *    b      1
     * sum      ---   = zeta(s, a) - zeta(s, b + 1)
     *    k=a     s
     *           k
     *
     *                 oo    1
     * zeta(s, a) = sum    ------
     *                 k=0      s
     *                     (k+a)
     * </pre>
     *
     * <p>If subtraction of terms results in significant loss of bits then the summation
     * uses (b - a + 1) terms of the power series.
     *
     * <p>Note: The Hurwitz zeta function is defined for {@code s > 1} where it is absolutely
     * convergent.
     */
    private static class ZetaHarmonicSeries implements HarmonicSeries {
        /** Number of elements parameter. */
        private final int n;
        /** Exponent parameter. */
        private final double s;
        /** zeta(s, 1) where s is the exponent of the distribution. */
        private final double zeta1;
        /** zeta(s, 1 + n) where s is the exponent of the distribution; n is the number of elements. */
        private final double zeta1pN;
        /** Cached value of the N-th generalized harmonic. */
        private final double nthHarmonic;

        /** Create an instance.
         * @param n Maximum number of elements (n).
         * @param exponent Exponent (s).
         * @param zeta1 zeta(s, 1)
         * @param zeta1pN zeta(s, 1 + n)
         */
        ZetaHarmonicSeries(int n, double exponent,
                           double zeta1, double zeta1pN) {
            this.n = n;
            this.s = exponent;
            this.zeta1 = zeta1;
            this.zeta1pN = zeta1pN;
            this.nthHarmonic = zeta1 - zeta1pN;
        }

        @Override
        public double value(int a, int b) {
            if (b - a >= MIN_TERMS) {
                final double z1 = a == 1 ? zeta1 : HurwitzZeta.value(s, a);
                final double z2 = b == n ? zeta1pN : HurwitzZeta.value(s, b + 1.0);
                if (allowedDifference(z1, z2)) {
                    return applyBounds(z1 - z2);
                }
            }
            return applyBounds(generalizedHarmonic(a, b, s));
        }

        /**
         * Ensure the value is within the bound {@code [0, N-th harmonic]}. Ensures
         * probability normalisation by N-th harmonic is in the range [0, 1]. In practice
         * this may not be required.
         *
         * <p>Note: It should not be possible for the series summation to exceed the N-th
         * harmonic. This has been computed using a difference of zeta terms:
         *
         * <pre>
         *   zeta(s, a) - zeta(s, b+1) <= zeta(s, 1) - zeta(s, N+1)  where a >= 1 and b <= N
         * </pre>
         *
         * <p>The zeta difference will only exceed the normalizing constant due to
         * floating-point error in the zeta function.
         *
         * <p>The series sum may exceed the N-th harmonic if there is an error in the zeta
         * function. This may occur as the power terms approach zero (sub-normal
         * summation) when s or n are both large. Since n is bounded to an integer the
         * value is always suitable for evaluation of k^-s with k in integer [1, n], i.e.
         * we never see 1 + n == n as n < 2^53. The only errors are expected when s is
         * very large.
         *
         * @param x Value
         * @return bounded value
         */
        private double applyBounds(double x) {
            return x < nthHarmonic ? x : nthHarmonic;
        }
    }

    /** Create an instance.
     * @param numberOfElements Number of elements.
     * @param exponent Exponent.
     * @param nthHarmonic N-th generalized harmonic number
     * @param genHarmonic Function to compute the generalised harmonic series.
     */
    private ZipfDistribution(int numberOfElements,
                             double exponent,
                             double nthHarmonic,
                             HarmonicSeries genHarmonic) {
        this.numberOfElements = numberOfElements;
        this.exponent = exponent;
        this.nthHarmonic = nthHarmonic;
        this.genHarmonic = genHarmonic;
        logNthHarmonic = Math.log(nthHarmonic);
    }

    /**
     * Creates a Zipf distribution.
     *
     * <p><strong>Note:</strong> Construction computes the normalizing constant
     * \( H_{N,s} \) by direct summation of {@code numberOfElements} terms and is
     * {@code O(numberOfElements)}. See the {@linkplain ZipfDistribution class-level}
     * documentation for details.
     *
     * @param numberOfElements Number of elements.
     * @param exponent Exponent.
     * @return the distribution
     * @exception IllegalArgumentException if {@code numberOfElements <= 0};
     * or {@code exponent <= 0} or is {@code NaN}.
     */
    public static ZipfDistribution of(int numberOfElements,
                                      double exponent) {
        if (numberOfElements <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            numberOfElements);
        }
        if (!(exponent >= 0)) {
            // negative or nan
            throw new DistributionException(DistributionException.NEGATIVE,
                                            exponent);
        }

        // If s > 1 and the size is non-trivial then use the Hurwitz zeta function
        // to compute the harmonic series. Note if size is close to MIN_TERMS then
        // the implementation may repeatedly call the zeta function and reject using
        // it so the threshold is 4 * MIN_TERMS.
        if (exponent > 1 && (numberOfElements >>> 2) > MIN_TERMS) {
            final double zeta1 = HurwitzZeta.value(exponent, 1);
            final double zeta1pN = HurwitzZeta.value(exponent, numberOfElements + 1.0);
            if (allowedDifference(zeta1, zeta1pN)) {
                return new ZipfDistribution(numberOfElements, exponent, zeta1 - zeta1pN,
                    new ZetaHarmonicSeries(numberOfElements, exponent, zeta1, zeta1pN));
            }
        }

        final double nthHarmonic = generalizedHarmonic(1, numberOfElements, exponent);
        return new ZipfDistribution(numberOfElements, exponent, nthHarmonic,
            (a, b) -> generalizedHarmonic(a, b, exponent));
    }

    /**
     * Gets the number of elements parameter of this distribution.
     *
     * @return the number of elements.
     */
    public int getNumberOfElements() {
        return numberOfElements;
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
        if (x <= 0 || x > numberOfElements) {
            return 0;
        }

        return Math.pow(x, -exponent) / nthHarmonic;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x0,
                              int x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH, x0, x1);
        }
        if (x0 == x1 || x1 < 1) {
            // (x0, x1] does not overlap [1, n]
            return 0;
        }
        // If the range is outside the bounds use the appropriate cumulative probability
        if (x0 < 1) {
            return cumulativeProbability(x1);
        }
        if (x1 >= numberOfElements) {
            return survivalProbability(x0);
        }
        // Here: 1 <= x0 < x1 < n:
        // sum(pdf(x)) for x in (x0, x1]
        return genHarmonic.value(x0 + 1, x1) / nthHarmonic;
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x <= 0 || x > numberOfElements) {
            return Double.NEGATIVE_INFINITY;
        }

        return -Math.log(x) * exponent - logNthHarmonic;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x <= 0) {
            return 0;
        } else if (x >= numberOfElements) {
            return 1;
        }

        return genHarmonic.value(1, x) / nthHarmonic;
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x <= 0) {
            return 1;
        } else if (x >= numberOfElements) {
            return 0;
        }

        // Compute summation of terms omitted in the CDF.
        // The raw sums in CDF(x) + SF(x) = N-th harmonic
        return genHarmonic.value(x + 1, numberOfElements) / nthHarmonic;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For number of elements \( N \) and exponent \( s \), the mean is:
     *
     * <p>\[ \frac{H_{N,s-1}}{H_{N,s}} \]
     *
     * <p>where \( H_{N,k} \) is the
     * <a href="https://en.wikipedia.org/wiki/Harmonic_number#Generalized_harmonic_numbers">
     * generalized harmonic number</a> of order \( N \) of \( k \).
     */
    @Override
    public double getMean() {
        final double Hs1 = nthHarmonicExpMinus1();
        return Hs1 / nthHarmonic;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For number of elements \( N \) and exponent \( s \), the variance is:
     *
     * <p>\[ \frac{H_{N,s-2}}{H_{N,s}} - \frac{H_{N,s-1}^2}{H_{N,s}^2} \]
     *
     * <p>where \( H_{N,k} \) is the
     * <a href="https://en.wikipedia.org/wiki/Harmonic_number#Generalized_harmonic_numbers">
     * generalized harmonic number</a> of order \( N \) of \( k \).
     */
    @Override
    public double getVariance() {
        final double Hs2 = nthHarmonicExpMinus2();
        final double Hs1 = nthHarmonicExpMinus1();
        final double Hs = nthHarmonic;
        // (Hs2 / Hs) - ((Hs1 * Hs1) / (Hs * Hs))
        // Values are ascending magnitude: Hs < Hs1 < Hs2.
        // (small * large) - (mid * mid) with a common denominator:
        return (Hs2 * Hs - Hs1 * Hs1) / (Hs * Hs);
    }

    /**
     * Compute the N-th harmonic number using the {@code exponent - 1}.
     * This is cached to avoid repeat expensive computation across all N
     * for the mean and variance.
     *
     * @return the number
     */
    private double nthHarmonicExpMinus1() {
        double h = nthHarmonicM1;
        if (Double.isNaN(h)) {
            h = generalizedHarmonicAscendingSum(getNumberOfElements(), getExponent() - 1);
            nthHarmonicM1 = h;
        }
        return h;
    }

    /**
     * Compute the N-th harmonic number using the {@code exponent - 2}.
     * This is cached to avoid repeat expensive computation across all N
     * for the variance.
     *
     * @return the number
     */
    private double nthHarmonicExpMinus2() {
        double h = nthHarmonicM2;
        if (Double.isNaN(h)) {
            h = generalizedHarmonicAscendingSum(getNumberOfElements(), getExponent() - 2);
            nthHarmonicM2 = h;
        }
        return h;
    }

    /**
     * Calculates the sum of terms of the
     * <a href="https://mathworld.wolfram.com/HarmonicSeries.html">Harmonic
     * Series</a>.
     *
     * <pre>
     *          1
     *   sum  -----  for k in [from, to]
     *         k^m
     * </pre>
     *
     * <p>When {@code from = 1} the result is the N-th harmonic number where {@code N = to}.
     *
     * <p>Assumes {@code exponent > 0} to arrange the terms to sum from small to large.
     *
     * @param from First term in the series to calculate.
     * @param to Last term in the series to calculate.
     * @param m Exponent (special case {@code m = 1} is the harmonic series).
     * @return the sum
     */
    static double generalizedHarmonic(int from, int to, double m) {
        double value = 0;
        // Sum small to large
        for (int k = to; k >= from; k--) {
            value += Math.pow(k, -m);
        }
        return value;
    }

    /**
     * Calculates the {@code n}-th generalized harmonic number.
     *
     * <p>Checks the value of the {@code exponent} to arrange the terms to sum from from small to large.
     *
     * @param n Term in the series to calculate (must be larger than 1)
     * @param m Exponent (special case {@code m = 1} is the harmonic series).
     * @return the n<sup>th</sup> generalized harmonic number.
     */
    private static double generalizedHarmonicAscendingSum(int n, double m) {
        // Sum small to large.
        // If m < 0 then sum ascending, otherwise descending.
        // Note: 1^-m = 1
        if (m < 0) {
            double value = 1.0;
            for (int k = 2; k <= n; k++) {
                value += Math.pow(k, -m);
            }
            return value;
        }

        // If s > 1 and the size is non-trivial then use the Hurwitz zeta function
        // to compute the harmonic series.
        if (m > 1 && (n >>> 2) > MIN_TERMS) {
            final double z1 = HurwitzZeta.value(m, 1);
            final double z2 = HurwitzZeta.value(m, n + 1.0);
            if (allowedDifference(z1, z2)) {
                return z1 - z2;
            }
        }

        return generalizedHarmonic(1, n, m);
    }

    /**
     * Check the difference {@code large - small} does not result in cancellation and
     * potential loss of significant bits in the result.
     *
     * <p>This method is used to test if the difference of zeta functions is suitable
     * to avoid the summation of the power series.
     *
     * @param large the large value
     * @param small the small value
     * @return true if {@code large - small} is allowed
     */
    static boolean allowedDifference(double large, double small) {
        return small <= large * MAX_RATIO;
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
     * <p>The upper bound of the support is the number of elements.
     *
     * @return number of elements.
     */
    @Override
    public int getSupportUpperBound() {
        return getNumberOfElements();
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Zipf distribution sampler.
        return RejectionInversionZipfSampler.of(rng, numberOfElements, exponent)::sample;
    }
}
