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
import org.apache.commons.rng.sampling.distribution.StableSampler;

/**
 * Implementation of the Cauchy distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; x_0, \gamma) = { 1 \over \pi \gamma } \left[ { \gamma^2 \over (x - x_0)^2 + \gamma^2  } \right] \]
 *
 * <p>for \( x_0 \) the location,
 * \( \gamma &gt; 0 \) the scale, and
 * \( x \in (-\infty, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cauchy_distribution">Cauchy distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/CauchyDistribution.html">Cauchy distribution (MathWorld)</a>
 */
public final class CauchyDistribution extends AbstractContinuousDistribution {
    /** The location of this distribution. */
    private final double location;
    /** The scale of this distribution. */
    private final double scale;
    /** Density factor (scale / pi). */
    private final double scaleOverPi;
    /** Density factor (scale^2). */
    private final double scale2;

    /**
     * @param location Location parameter.
     * @param scale Scale parameter.
     */
    private CauchyDistribution(double location,
                               double scale) {
        this.scale = scale;
        this.location = location;
        scaleOverPi = scale / Math.PI;
        scale2 = scale * scale;
    }

    /**
     * Creates a Cauchy distribution.
     *
     * @param location Location parameter.
     * @param scale Scale parameter.
     * @return the distribution
     * @throws IllegalArgumentException if {@code scale <= 0}.
     */
    public static CauchyDistribution of(double location,
                                        double scale) {
        if (scale <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, scale);
        }
        return new CauchyDistribution(location, scale);
    }

    /**
     * Gets the location parameter of this distribution.
     *
     * @return the location parameter.
     */
    public double getLocation() {
        return location;
    }

    /**
     * Gets the scale parameter of this distribution.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return scale;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        final double dev = x - location;
        return scaleOverPi / (dev * dev + scale2);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        return cdf((x - location) / scale);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        return cdf(-(x - location) / scale);
    }

    /**
     * Compute the CDF of the Cauchy distribution with location 0 and scale 1.
     * @param x Point at which the CDF is evaluated
     * @return CDF(x)
     */
    private static double cdf(double x) {
        return 0.5 + (Math.atan(x) / Math.PI);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link Double#NEGATIVE_INFINITY} when {@code p == 0}
     * and {@link Double#POSITIVE_INFINITY} when {@code p == 1}.
     */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return Double.NEGATIVE_INFINITY;
        } else  if (p == 1) {
            return Double.POSITIVE_INFINITY;
        }
        return location + scale * Math.tan(Math.PI * (p - 0.5));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link Double#NEGATIVE_INFINITY} when {@code p == 1}
     * and {@link Double#POSITIVE_INFINITY} when {@code p == 0}.
     */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return Double.NEGATIVE_INFINITY;
        } else  if (p == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return location - scale * Math.tan(Math.PI * (p - 0.5));
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mean is always undefined.
     *
     * @return {@link Double#NaN NaN}.
     */
    @Override
    public double getMean() {
        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The variance is always undefined.
     *
     * @return {@link Double#NaN NaN}.
     */
    @Override
    public double getVariance() {
        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always negative infinity.
     *
     * @return {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
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
     * @return {@linkplain Double#POSITIVE_INFINITY positive infinity}.
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
        return location;
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Cauchy distribution =
        // Stable distribution with alpha=1, beta=0, gamma=scale, delta=location
        return StableSampler.of(rng, 1, 0, getScale(), getLocation())::sample;
    }
}
