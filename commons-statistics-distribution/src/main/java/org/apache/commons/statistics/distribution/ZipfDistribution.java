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
 * <p><strong>Note:</strong> The generalized harmonic number \( H_{N,s} \) is computed
 * by direct summation of \( N \) terms. Construction of the distribution, and the first
 * call to {@link #getMean()} or {@link #getVariance()}, is \( O(N) \); each call to
 * {@link #cumulativeProbability(int) cumulativeProbability(x)} or
 * {@link #survivalProbability(int) survivalProbability(x)} is \( O(x) \) (the
 * partial harmonic sum is not cached between calls); the inverse probability
 * functions perform a search using \( O(\log N) \) cumulative probability
 * evaluations. A number of elements of order 2<sup>31</sup> requires billions of
 * {@code Math.pow} evaluations for construction alone. Take this run-time cost into
 * account when the parameters are derived from untrusted input, and bound the number
 * of elements accordingly. Sampling (see {@link #createSampler(UniformRandomProvider)
 * createSampler}) uses a rejection method with a cost per sample that does not depend
 * on the number of elements.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Zipf's_law">Zipf distribution (Wikipedia)</a>
 */
public final class ZipfDistribution extends AbstractDiscreteDistribution {
    /** Number of elements. */
    private final int numberOfElements;
    /** Exponent parameter of the distribution. */
    private final double exponent;
    /** Cached value of the nth generalized harmonic. */
    private final double nthHarmonic;
    /** Cached value of the log of the nth generalized harmonic. */
    private final double logNthHarmonic;
    /** Cached value of the nth generalized harmonic using (exponent - 1). */
    private double nthHarmonicM1 = Double.NaN;
    /** Cached value of the nth generalized harmonic using (exponent - 2). */
    private double nthHarmonicM2 = Double.NaN;

    /** Create an instance.
     * @param numberOfElements Number of elements.
     * @param exponent Exponent.
     */
    private ZipfDistribution(int numberOfElements,
                             double exponent) {
        this.numberOfElements = numberOfElements;
        this.exponent = exponent;
        this.nthHarmonic = generalizedHarmonic(1, numberOfElements, exponent);
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
        return new ZipfDistribution(numberOfElements, exponent);
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
    public double logProbability(int x) {
        if (x <= 0 || x > numberOfElements) {
            return Double.NEGATIVE_INFINITY;
        }

        return -Math.log(x) * exponent - logNthHarmonic;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final int x) {
        if (x <= 0) {
            return 0;
        } else if (x >= numberOfElements) {
            return 1;
        }

        return generalizedHarmonic(1, x, exponent) / nthHarmonic;
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
        return generalizedHarmonic(x + 1, numberOfElements, exponent) / nthHarmonic;
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
    private static double generalizedHarmonic(final int from, final int to, final double m) {
        double value = 0;
        // Sum small to large
        for (int k = to; k >= from; k--) {
            value += Math.pow(k, -m);
        }
        return value;
    }

    /**
     * Calculates the Nth generalized harmonic number.
     *
     * <p>Checks the value of the {@code exponent} to arrange the terms to sum from from small to large.
     *
     * @param n Term in the series to calculate (must be larger than 1)
     * @param m Exponent (special case {@code m = 1} is the harmonic series).
     * @return the n<sup>th</sup> generalized harmonic number.
     */
    private static double generalizedHarmonicAscendingSum(final int n, final double m) {
        double value = 0;
        // Sum small to large
        // If m < 0 then sum ascending, otherwise descending
        if (m < 0) {
            for (int k = 1; k <= n; k++) {
                value += Math.pow(k, -m);
            }
        } else {
            for (int k = n; k >= 1; k--) {
                value += Math.pow(k, -m);
            }
        }
        return value;
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
