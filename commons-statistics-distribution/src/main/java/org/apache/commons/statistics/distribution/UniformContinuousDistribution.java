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
import org.apache.commons.rng.sampling.distribution.ContinuousUniformSampler;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Uniform_distribution_(continuous)">uniform distribution</a>.
 */
public final class UniformContinuousDistribution extends AbstractContinuousDistribution {
    /** Lower bound of this distribution (inclusive). */
    private final double lower;
    /** Upper bound of this distribution (exclusive). */
    private final double upper;
    /** Range between the upper and lower bound of this distribution (cached for computations). */
    private final double upperMinusLower;
    /** Cache of the density. */
    private final double pdf;
    /** Cache of the log density. */
    private final double logPdf;

    /**
     * @param lower Lower bound of this distribution (inclusive).
     * @param upper Upper bound of this distribution (inclusive).
     */
    private UniformContinuousDistribution(double lower,
                                          double upper) {
        this.lower = lower;
        this.upper = upper;
        upperMinusLower = upper - lower;
        pdf = 1.0 / upperMinusLower;
        logPdf = -Math.log(upperMinusLower);
    }

    /**
     * Creates a uniform distribution.
     *
     * @param lower Lower bound of this distribution (inclusive).
     * @param upper Upper bound of this distribution (inclusive).
     * @return the distribution
     * @throws IllegalArgumentException if {@code lower >= upper}.
     */
    public static UniformContinuousDistribution of(double lower,
                                                   double upper) {
        if (lower >= upper) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GTE_HIGH,
                                            lower, upper);
        }
        return new UniformContinuousDistribution(lower, upper);
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x < lower ||
            x > upper) {
            return 0;
        }
        return pdf;
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        if (x < lower ||
            x > upper) {
            return Double.NEGATIVE_INFINITY;
        }
        return logPdf;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x)  {
        if (x <= lower) {
            return 0;
        }
        if (x >= upper) {
            return 1;
        }
        return (x - lower) / upperMinusLower;
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= lower) {
            return 1;
        }
        if (x >= upper) {
            return 0;
        }
        return (upper - x) / upperMinusLower;
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(final double p) {
        ArgumentUtils.checkProbability(p);
        // Avoid floating-point error for lower + p * (upper - lower) when p == 1.
        return p == 1 ? upper : p * upperMinusLower + lower;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower bound {@code lower} and upper bound {@code upper}, the mean is
     * {@code 0.5 * (lower + upper)}.
     */
    @Override
    public double getMean() {
        return 0.5 * (lower + upper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower bound {@code lower} and upper bound {@code upper}, the
     * variance is {@code (upper - lower)^2 / 12}.
     */
    @Override
    public double getVariance() {
        return upperMinusLower * upperMinusLower / 12;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is equal to the lower bound parameter
     * of the distribution.
     *
     * @return lower bound of the support
     */
    @Override
    public double getSupportLowerBound() {
        return lower;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is equal to the upper bound parameter
     * of the distribution.
     *
     * @return upper bound of the support
     */
    @Override
    public double getSupportUpperBound() {
        return upper;
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
    protected double getMedian() {
        // Overridden for the probability(double, double) method.
        // This is intentionally not a public method.
        return getMean();
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Uniform distribution sampler.
        return ContinuousUniformSampler.of(rng, lower, upper)::sample;
    }
}
