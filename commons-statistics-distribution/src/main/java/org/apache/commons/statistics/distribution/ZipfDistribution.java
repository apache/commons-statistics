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

package org.apache.commons.statistics.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.DiscreteUniformSampler;
import org.apache.commons.rng.sampling.distribution.RejectionInversionZipfSampler;

/**
 * Implementation of the <a href="https://en.wikipedia.org/wiki/Zipf's_law">Zipf distribution</a>.
 * <p>
 * <strong>Parameters:</strong>
 * For a random variable {@code X} whose values are distributed according to this
 * distribution, the probability mass function is given by:
 * <pre>
 *   P(X = k) = H(N,s) * 1 / k^s    for {@code k = 1,2,...,N}.
 * </pre>
 * <p>{@code H(N,s)} is the normalizing constant
 * which corresponds to the generalized harmonic number of order N of s.
 * <ul>
 * <li>{@code N} is the number of elements</li>
 * <li>{@code s} is the exponent</li>
 * </ul>
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

    /**
     * @param numberOfElements Number of elements.
     * @param exponent Exponent.
     */
    private ZipfDistribution(int numberOfElements,
                             double exponent) {
        this.numberOfElements = numberOfElements;
        this.exponent = exponent;
        this.nthHarmonic = generalizedHarmonic(numberOfElements, exponent);
        logNthHarmonic = Math.log(nthHarmonic);
    }

    /**
     * Creates a Zipf distribution.
     *
     * @param numberOfElements Number of elements.
     * @param exponent Exponent.
     * @return the distribution
     * @exception IllegalArgumentException if {@code numberOfElements <= 0}
     * or {@code exponent <= 0}.
     */
    public static ZipfDistribution of(int numberOfElements,
                                      double exponent) {
        if (numberOfElements <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            numberOfElements);
        }
        if (exponent < 0) {
            throw new DistributionException(DistributionException.NEGATIVE,
                                            exponent);
        }
        return new ZipfDistribution(numberOfElements, exponent);
    }

    /**
     * Get the number of elements (e.g. corpus size) for the distribution.
     *
     * @return the number of elements
     */
    public int getNumberOfElements() {
        return numberOfElements;
    }

    /**
     * Get the exponent characterizing the distribution.
     *
     * @return the exponent
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

        return generalizedHarmonic(x, exponent) / nthHarmonic;
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x <= 0) {
            return 1;
        } else if (x >= numberOfElements) {
            return 0;
        }

        // See http://www.math.wm.edu/~leemis/chart/UDR/PDFs/Zipf.pdf
        // S(x) = P(X > x) = ((x+1)^a Hn,a - (x+1)^a Hx+1,a + 1) / ((x+1)^a Hn,a)
        // where a = exponent and Hx,a is the generalized harmonic for x with exponent a.
        final double z = Math.pow(x + 1.0, exponent);
        // Compute generalizedHarmonic(x, exponent) and generalizedHarmonic(x+1, exponent)
        final double hx = generalizedHarmonic(x, exponent);
        final double hx1 = hx + Math.pow(x + 1.0, -exponent);
        // Compute the survival function
        final double p = (z * (nthHarmonic - hx1) + 1) / (z * nthHarmonic);
        // May overflow for large exponent so validate the probability.
        // If this occurs revert to 1 - CDF(x), reusing the generalized harmonic for x
        return p <= 1.0 ? p : 1.0 - hx / nthHarmonic;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For number of elements {@code N} and exponent {@code s}, the mean is
     * {@code Hs1 / Hs}, where
     * <ul>
     *  <li>{@code Hs1 = generalizedHarmonic(N, s - 1)},</li>
     *  <li>{@code Hs = generalizedHarmonic(N, s)}.</li>
     * </ul>
     */
    @Override
    public double getMean() {
        final int N = getNumberOfElements();
        final double s = getExponent();

        final double Hs1 = generalizedHarmonicAscendingSum(N, s - 1);

        return Hs1 / nthHarmonic;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For number of elements {@code N} and exponent {@code s}, the mean is
     * {@code (Hs2 / Hs) - (Hs1^2 / Hs^2)}, where
     * <ul>
     *  <li>{@code Hs2 = generalizedHarmonic(N, s - 2)},</li>
     *  <li>{@code Hs1 = generalizedHarmonic(N, s - 1)},</li>
     *  <li>{@code Hs = generalizedHarmonic(N, s)}.</li>
     * </ul>
     */
    @Override
    public double getVariance() {
        final int N = getNumberOfElements();
        final double s = getExponent();

        final double Hs2 = generalizedHarmonicAscendingSum(N, s - 2);
        final double Hs1 = generalizedHarmonicAscendingSum(N, s - 1);
        final double Hs = nthHarmonic;

        return (Hs2 / Hs) - ((Hs1 * Hs1) / (Hs * Hs));
    }

    /**
     * Calculates the Nth generalized harmonic number. See
     * <a href="http://mathworld.wolfram.com/HarmonicSeries.html">Harmonic
     * Series</a>.
     *
     * <p>Assumes {@code exponent > 0} to arrange the terms to sum from small to large.
     *
     * @param n Term in the series to calculate (must be larger than 1)
     * @param m Exponent (special case {@code m = 1} is the harmonic series).
     * @return the n<sup>th</sup> generalized harmonic number.
     */
    private static double generalizedHarmonic(final int n, final double m) {
        double value = 0;
        // Sum small to large
        for (int k = n; k >= 1; k--) {
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
     * <p>The lower bound of the support is always 1 no matter the parameters.
     *
     * @return lower bound of the support (always 1)
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
     * @return upper bound of the support
     */
    @Override
    public int getSupportUpperBound() {
        return getNumberOfElements();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The support of this distribution is connected.
     *
     * @return {@code true}
     */
    @Override
    public boolean isSupportConnected() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        if (exponent > 0) {
            // Zipf distribution sampler.
            return RejectionInversionZipfSampler.of(rng, numberOfElements, exponent)::sample;
        }
        // Special case when exponent is zero then the sample is uniform
        return DiscreteUniformSampler.of(rng, 1, numberOfElements)::sample;
    }
}
