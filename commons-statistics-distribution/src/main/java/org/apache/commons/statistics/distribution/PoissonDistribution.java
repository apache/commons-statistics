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
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Poisson_distribution">Poisson distribution</a>.
 */
public final class PoissonDistribution extends AbstractDiscreteDistribution {
    /** 0.5 * ln(2 * pi). Computed to 25-digits precision. */
    private static final double HALF_LOG_TWO_PI = 0.9189385332046727417803297;
    /** Default maximum number of iterations. */
    private static final int DEFAULT_MAX_ITERATIONS = 10000000;
    /** Default convergence criterion. */
    private static final double DEFAULT_EPSILON = 1e-12;
    /** Upper bound on the mean to use the PoissonSampler. */
    private static final double MAX_MEAN = 0.5 * Integer.MAX_VALUE;
    /** Mean of the distribution. */
    private final double mean;
    /** Maximum number of iterations for cumulative probability. */
    private final int maxIterations;
    /** Convergence criterion for cumulative probability. */
    private final double epsilon;

    /**
     * @param mean Poisson mean.
     * @param epsilon Convergence criterion for cumulative probabilities.
     * @param maxIterations Maximum number of iterations for cumulative
     * probabilities.
     */
    private PoissonDistribution(double mean,
                                double epsilon,
                                int maxIterations) {
        this.mean = mean;
        this.epsilon = epsilon;
        this.maxIterations = maxIterations;
    }

    /**
     * Creates a Poisson distribution.
     *
     * @param mean Poisson mean.
     * @return the distribution
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public static PoissonDistribution of(double mean) {
        return of(mean, DEFAULT_EPSILON, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Creates a Poisson distribution with the specified convergence
     * criterion and maximum number of iterations.
     *
     * @param mean Poisson mean.
     * @param epsilon Convergence criterion for cumulative probabilities.
     * @param maxIterations Maximum number of iterations for cumulative
     * probabilities.
     * @return the distribution
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public static PoissonDistribution of(double mean,
                                         double epsilon,
                                         int maxIterations) {
        if (mean <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, mean);
        }
        return new PoissonDistribution(mean, epsilon, maxIterations);
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
        return RegularizedGamma.Q.value((double) x + 1, mean, epsilon,
                                        maxIterations);
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
        return RegularizedGamma.P.value((double) x + 1, mean, epsilon,
                                        maxIterations);
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
     * <p>The lower bound of the support is always 0 no matter the mean parameter.
     *
     * @return lower bound of the support (always 0)
     */
    @Override
    public int getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is positive infinity,
     * regardless of the parameter values. There is no integer infinity,
     * so this method returns {@code Integer.MAX_VALUE}.
     *
     * @return upper bound of the support (always {@code Integer.MAX_VALUE} for
     * positive infinity)
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
