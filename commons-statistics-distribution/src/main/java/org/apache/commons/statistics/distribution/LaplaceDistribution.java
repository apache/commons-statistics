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

/**
 * Implementation of the Laplace distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, b) = \frac{1}{2b} \exp \left( -\frac{|x-\mu|}{b} \right) \]
 *
 * <p>for \( \mu \) the location,
 * \( b &gt; 0 \) the scale, and
 * \( x \in (-\infty, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Laplace_distribution">Laplace distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/LaplaceDistribution.html">Laplace distribution (MathWorld)</a>
 */
public final class LaplaceDistribution extends AbstractContinuousDistribution {
    /** The location parameter. */
    private final double mu;
    /** The scale parameter. */
    private final double beta;
    /** log(2 * beta). */
    private final double log2beta;

    /**
     * @param mu Location parameter.
     * @param beta Scale parameter (must be positive).
     */
    private LaplaceDistribution(double mu,
                                double beta) {
        this.mu = mu;
        this.beta = beta;
        log2beta = Math.log(2.0 * beta);
    }

    /**
     * Creates a Laplace distribution.
     *
     * @param mu Location parameter.
     * @param beta Scale parameter (must be positive).
     * @return the distribution
     * @throws IllegalArgumentException if {@code beta <= 0}
     */
    public static LaplaceDistribution of(double mu,
                                         double beta) {
        if (beta <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, beta);
        }
        return new LaplaceDistribution(mu, beta);
    }

    /**
     * Gets the location parameter of this distribution.
     *
     * @return the location parameter.
     */
    public double getLocation() {
        return mu;
    }

    /**
     * Gets the scale parameter of this distribution.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return beta;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        return Math.exp(-Math.abs(x - mu) / beta) / (2.0 * beta);
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        return -Math.abs(x - mu) / beta - log2beta;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= mu) {
            return 0.5 * Math.exp((x - mu) / beta);
        }
        return 1.0 - 0.5 * Math.exp((mu - x) / beta);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= mu) {
            return 1.0 - 0.5 * Math.exp((x - mu) / beta);
        }
        return 0.5 * Math.exp((mu - x) / beta);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return Double.NEGATIVE_INFINITY;
        } else if (p == 1) {
            return Double.POSITIVE_INFINITY;
        }
        final double x = (p > 0.5) ? -Math.log(2.0 * (1.0 - p)) : Math.log(2.0 * p);
        return mu + beta * x;
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return Double.NEGATIVE_INFINITY;
        } else if (p == 0) {
            return Double.POSITIVE_INFINITY;
        }
        // By symmetry: x = -icdf(p); then transform back by the scale and location
        final double x = (p > 0.5) ? Math.log(2.0 * (1.0 - p)) : -Math.log(2.0 * p);
        return mu + beta * x;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mean is equal to the {@link #getLocation() location}.
     */
    @Override
    public double getMean() {
        return getLocation();
    }

    /**
     * {@inheritDoc}
     *
     * <p>For scale parameter \( b \), the variance is \( 2 b^2 \).
     */
    @Override
    public double getVariance() {
        return 2.0 * beta * beta;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always negative infinity.
     *
     * @return negative infinity.
     */
    @Override
    public double getSupportLowerBound() {
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity.
     *
     * @return positive infinity.
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    double getMedian() {
        // Overridden for the probability(double, double) method.
        // This is intentionally not a public method.
        return mu;
    }
}
