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

import org.apache.commons.numbers.gamma.Gamma;
import org.apache.commons.numbers.gamma.GammaRatio;
import org.apache.commons.numbers.gamma.LogGamma;
import org.apache.commons.numbers.gamma.RegularizedGamma;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.AhrensDieterMarsagliaTsangGammaSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateContinuousSampler;

/**
 * Implementation of the Nakagami distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, \Omega) = \frac{2\mu^\mu}{\Gamma(\mu)\Omega^\mu}x^{2\mu-1}\exp\left(-\frac{\mu}{\Omega}x^2\right) \]
 *
 * <p>for \( \mu &gt; 0 \) the shape,
 * \( \Omega &gt; 0 \) the scale, and
 * \( x \in (0, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Nakagami_distribution">Nakagami distribution (Wikipedia)</a>
 */
public final class NakagamiDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = 0;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;

    /** The shape parameter. */
    private final double mu;
    /** The scale parameter. */
    private final double omega;
    /** Density prefactor. */
    private final double densityPrefactor;
    /** Log density prefactor. */
    private final double logDensityPrefactor;
    /** Cached value for inverse probability function. */
    private final double mean;
    /** Cached value for inverse probability function. */
    private final double variance;

    /**
     * @param mu Shape parameter (must be positive).
     * @param omega Scale parameter (must be positive). Controls the spread of the distribution.
     */
    private NakagamiDistribution(double mu,
                                 double omega) {
        this.mu = mu;
        this.omega = omega;
        densityPrefactor = 2.0 * Math.pow(mu, mu) / (Gamma.value(mu) * Math.pow(omega, mu));
        logDensityPrefactor = Constants.LN_TWO + Math.log(mu) * mu - LogGamma.value(mu) - Math.log(omega) * mu;
        final double v = GammaRatio.delta(mu, 0.5);
        mean = Math.sqrt(omega / mu) / v;
        variance = omega - (omega / mu) / v / v;
    }

    /**
     * Creates a Nakagami distribution.
     *
     * @param mu Shape parameter (must be positive).
     * @param omega Scale parameter (must be positive). Controls the spread of the distribution.
     * @return the distribution
     * @throws IllegalArgumentException  if {@code mu <= 0} or if
     * {@code omega <= 0}.
     */
    public static NakagamiDistribution of(double mu,
                                          double omega) {
        if (mu <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, mu);
        }
        if (omega <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, omega);
        }
        return new NakagamiDistribution(mu, omega);
    }

    /**
     * Gets the shape parameter of this distribution.
     *
     * @return the shape parameter.
     */
    public double getShape() {
        return mu;
    }

    /**
     * Gets the scale parameter of this distribution.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return omega;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            return 0;
        }

        return densityPrefactor * Math.pow(x, 2 * mu - 1) * Math.exp(-mu * x * x / omega);
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            return Double.NEGATIVE_INFINITY;
        }

        return logDensityPrefactor + Math.log(x) * (2 * mu - 1) - (mu * x * x / omega);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 0;
        } else if (x >= SUPPORT_HI) {
            return 1;
        }

        return RegularizedGamma.P.value(mu, mu * x * x / omega);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 1;
        } else if (x >= SUPPORT_HI) {
            return 0;
        }

        return RegularizedGamma.Q.value(mu, mu * x * x / omega);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter \( \mu \) and scale parameter \( \Omega \), the mean is:
     *
     * <p>\[ \frac{\Gamma(m+\frac{1}{2})}{\Gamma(m)}\left(\frac{\Omega}{m}\right)^{1/2} \]
     */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter \( \mu \) and scale parameter \( \Omega \), the variance is:
     *
     * <p>\[ \Omega\left(1-\frac{1}{m}\left(\frac{\Gamma(m+\frac{1}{2})}{\Gamma(m)}\right)^2\right) \]
     */
    @Override
    public double getVariance() {
        return variance;
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

    @Override
    public Sampler createSampler(UniformRandomProvider rng) {
        // Generate using a related Gamma distribution
        // See https://en.wikipedia.org/wiki/Nakagami_distribution#Generation
        final double shape = mu;
        final double scale = omega / mu;
        final SharedStateContinuousSampler sampler =
            AhrensDieterMarsagliaTsangGammaSampler.of(rng, shape, scale);
        return () -> Math.sqrt(sampler.sample());
    }
}
