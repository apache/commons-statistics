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

import org.apache.commons.numbers.gamma.RegularizedGamma;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.GaussianSampler;
import org.apache.commons.rng.sampling.distribution.PoissonSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateContinuousSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Implementation of the Poisson distribution.
 *
 * <p>The probability mass function of \( X \) is:
 *
 * <p>\[ f(k; \lambda) = \frac{\lambda^k e^{-k}}{k!} \]
 *
 * <p>for \( \lambda \in (0, \infty) \) the mean and
 * \( k \in \{0, 1, 2, \dots\} \) the number of events.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Poisson_distribution">Poisson distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution (MathWorld)</a>
 */
public final class PoissonDistribution extends AbstractDiscreteDistribution {
    /** 0.5 * ln(2 * pi). Computed to 25-digits precision. */
    private static final double HALF_LOG_TWO_PI = 0.9189385332046727417803297;
    /** Upper bound on the mean to use the PoissonSampler. */
    private static final double MAX_MEAN = 0.5 * Integer.MAX_VALUE;
    /** Mean of the distribution. */
    private final double mean;

    /**
     * @param mean Poisson mean.
     * probabilities.
     */
    private PoissonDistribution(double mean) {
        this.mean = mean;
    }

    /**
     * Creates a Poisson distribution.
     *
     * @param mean Poisson mean.
     * @return the distribution
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public static PoissonDistribution of(double mean) {
        if (mean <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, mean);
        }
        return new PoissonDistribution(mean);
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x) {
        return Math.exp(logProbability(x));
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x < 0) {
            return Double.NEGATIVE_INFINITY;
        } else if (x == 0) {
            return -mean;
        }
        return -SaddlePointExpansionUtils.getStirlingError(x) -
              SaddlePointExpansionUtils.getDeviancePart(x, mean) -
              HALF_LOG_TWO_PI - 0.5 * Math.log(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x < 0) {
            return 0;
        } else if (x == 0) {
            return Math.exp(-mean);
        }
        return RegularizedGamma.Q.value((double) x + 1, mean);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x < 0) {
            return 1;
        } else if (x == 0) {
            // 1 - exp(-mean)
            return -Math.expm1(-mean);
        }
        return RegularizedGamma.P.value((double) x + 1, mean);
    }

    /** {@inheritDoc} */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The variance is equal to the {@link #getMean() mean}.
     */
    @Override
    public double getVariance() {
        return getMean();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0.
     *
     * @return 0.
     */
    @Override
    public int getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity.
     *
     * @return {@code Integer.MAX_VALUE}
     */
    @Override
    public int getSupportUpperBound() {
        return Integer.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Poisson distribution sampler.
        // Large means are not supported.
        // See STATISTICS-35.
        final double mu = getMean();
        if (mu < MAX_MEAN) {
            return PoissonSampler.of(rng, mu)::sample;
        }
        // Switch to a Gaussian approximation.
        // Use a 0.5 shift to round samples to the correct integer.
        final SharedStateContinuousSampler s =
            GaussianSampler.of(ZigguratSampler.NormalizedGaussian.of(rng),
                               mu + 0.5, Math.sqrt(mu));
        return () -> {
            final double x = s.sample();
            return Math.max(0, (int) x);
        };
    }
}
