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

import org.apache.commons.numbers.gamma.Erf;
import org.apache.commons.numbers.gamma.Erfc;
import org.apache.commons.numbers.gamma.InverseErf;
import org.apache.commons.numbers.gamma.InverseErfc;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.LevySampler;

/**
 * This class implements the <a href="http://en.wikipedia.org/wiki/L%C3%A9vy_distribution">
 * L&eacute;vy distribution</a>.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, c) = \sqrt{\frac{c}{2\pi}}~~\frac{e^{ -\frac{c}{2(x-\mu)}}} {(x-\mu)^{3/2}} \]
 *
 * <p>for \( \mu \) the location,
 * \( c &gt; 0 \) the scale, and
 * \( x \in [\mu, \infty) \).
 */
public final class LevyDistribution extends AbstractContinuousDistribution {
    /** 1 / 2(erfc^-1 (0.5))^2. Computed using Matlab's VPA to 30 digits. */
    private static final double HALF_OVER_ERFCINV_HALF_SQUARED = 2.1981093383177324039996779530797;
    /** Location parameter. */
    private final double mu;
    /** Scale parameter. */
    private final double c;
    /** Half of c (for calculations). */
    private final double halfC;

    /**
     * @param mu Location parameter.
     * @param c Scale parameter.
     */
    private LevyDistribution(double mu,
                             double c) {
        this.mu = mu;
        this.c = c;
        this.halfC = 0.5 * c;
    }

    /**
     * Creates a Levy distribution.
     *
     * @param mu Location parameter.
     * @param c Scale parameter.
     * @return the distribution
     * @throws IllegalArgumentException if {@code c <= 0}.
     */
    public static LevyDistribution of(double mu,
                                      double c) {
        if (c <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            c);
        }
        return new LevyDistribution(mu, c);
    }

    /**
     * Gets the location parameter of the distribution.
     *
     * @return location parameter of the distribution
     */
    public double getLocation() {
        return mu;
    }

    /**
     * Gets the scale parameter of the distribution.
     *
     * @return scale parameter of the distribution
     */
    public double getScale() {
        return c;
    }

    /** {@inheritDoc}
    * <p>
    * From Wikipedia: The probability density function of the L&eacute;vy distribution
    * over the domain is
    * </p>
    * <div style="white-space: pre"><code>
    * f(x; &mu;, c) = &radic;(c / 2&pi;) * e<sup>-c / 2 (x - &mu;)</sup> / (x - &mu;)<sup>3/2</sup>
    * </code></div>
    * <p>
    * For this distribution, {@code X}, this method returns {@code P(X < x)}.
    * If {@code x} is less than location parameter &mu;, {@code 0} is
    * returned, as in these cases the distribution is not defined.
    * </p>
    */
    @Override
    public double density(final double x) {
        if (x <= mu) {
            // x=mu creates NaN:
            // sqrt(c / 2pi) * exp(-c / 2(x-mu)) / (x-mu)^1.5
            // = F * exp(-inf) * (x-mu)^-1.5 = F * 0 * inf
            // Return 0 for this case.
            return 0;
        }

        final double delta = x - mu;
        final double f = halfC / delta;
        return Math.sqrt(f / Math.PI) * Math.exp(-f) / delta;
    }

    /** {@inheritDoc}
     *
     * <p>See documentation of {@link #density(double)} for computation details.
     */
    @Override
    public double logDensity(double x) {
        if (x <= mu) {
            return Double.NEGATIVE_INFINITY;
        }

        final double delta = x - mu;
        final double f     = halfC / delta;
        return 0.5 * Math.log(f / Math.PI) - f - Math.log(delta);
    }

    /** {@inheritDoc}
     * <p>
     * From Wikipedia: the cumulative distribution function is
     * </p>
     * <pre>
     * f(x; u, c) = erfc (&radic; (c / 2 (x - u )))
     * </pre>
     */
    @Override
    public double cumulativeProbability(final double x) {
        if (x <= mu) {
            return 0;
        }
        return Erfc.value(Math.sqrt(halfC / (x - mu)));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(final double x) {
        if (x <= mu) {
            return 1;
        }
        return Erf.value(Math.sqrt(halfC / (x - mu)));
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        final double t = InverseErfc.value(p);
        return mu + halfC / (t * t);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        final double t = InverseErf.value(p);
        return mu + halfC / (t * t);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mean is equal to positive infinity.
     */
    @Override
    public double getMean() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The variance is equal to positive infinity.
     */
    @Override
    public double getVariance() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is the {@link #getLocation() location}.
     *
     * @return lower bound of the support
     */
    @Override
    public double getSupportLowerBound() {
        return getLocation();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity
     * no matter the parameters.
     *
     * @return upper bound of the support (always
     * {@code Double.POSITIVE_INFINITY})
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
        // u + c / 2(erfc^-1 (0.5))^2
        return mu + c * HALF_OVER_ERFCINV_HALF_SQUARED;
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Levy distribution sampler.
        return LevySampler.of(rng, getLocation(), getScale())::sample;
    }
}
