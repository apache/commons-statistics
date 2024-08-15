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
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Implementation of the exponential distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu) = \frac{1}{\mu} e^{-x / \mu} \]
 *
 * <p>for \( \mu &gt; 0 \) the mean and
 * \( x \in [0, \infty) \).
 *
 * <p>This implementation uses the scale parameter \( \mu \) which is the mean of the distribution.
 * A common alternative parameterization uses the rate parameter \( \lambda \) which is the reciprocal
 * of the mean. The distribution can be be created using \( \mu  = \frac{1}{\lambda} \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Exponential_distribution">Exponential distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/ExponentialDistribution.html">Exponential distribution (MathWorld)</a>
 */
public final class ExponentialDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = 0;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** The mean of this distribution. */
    private final double mean;
    /** The logarithm of the mean, stored to reduce computing time. */
    private final double logMean;

    /**
     * @param mean Mean of this distribution.
     */
    private ExponentialDistribution(double mean) {
        this.mean = mean;
        logMean = Math.log(mean);
    }

    /**
     * Creates an exponential distribution.
     *
     * @param mean Mean of this distribution. This is a scale parameter.
     * @return the distribution
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public static ExponentialDistribution of(double mean) {
        if (mean <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, mean);
        }
        return new ExponentialDistribution(mean);
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x < SUPPORT_LO) {
            return 0;
        }
        return Math.exp(-x / mean) / mean;
    }

    /** {@inheritDoc} **/
    @Override
    public double logDensity(double x) {
        if (x < SUPPORT_LO) {
            return Double.NEGATIVE_INFINITY;
        }
        return -x / mean - logMean;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x)  {
        if (x <= SUPPORT_LO) {
            return 0;
        }
        return -Math.expm1(-x / mean);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x)  {
        if (x <= SUPPORT_LO) {
            return 1;
        }
        return Math.exp(-x / mean);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code 0} when {@code p == 0} and
     * {@link Double#POSITIVE_INFINITY} when {@code p == 1}.
     */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return Double.POSITIVE_INFINITY;
        }
        // Subtract from zero to prevent returning -0.0 for p=-0.0
        return 0 - mean * Math.log1p(-p);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code 0} when {@code p == 1} and
     * {@link Double#POSITIVE_INFINITY} when {@code p == 0}.
     */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return Double.POSITIVE_INFINITY;
        }
        // Subtract from zero to prevent returning -0.0 for p=1
        return 0 - mean * Math.log(p);
    }

    /** {@inheritDoc} */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For mean \( \mu \), the variance is \( \mu^2 \).
     */
    @Override
    public double getVariance() {
        return mean * mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0.
     *
     * @return 0.
     */
    @Override
    public double getSupportLowerBound() {
        return SUPPORT_LO;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity.
     *
     * @return {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     */
    @Override
    public double getSupportUpperBound() {
        return SUPPORT_HI;
    }

    /** {@inheritDoc} */
    @Override
    double getMedian() {
        // Overridden for the probability(double, double) method.
        // This is intentionally not a public method.
        // ln(2) / rate = mean * ln(2)
        return mean * Constants.LN_TWO;
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Exponential distribution sampler.
        return ZigguratSampler.Exponential.of(rng, getMean())::sample;
    }
}
