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

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Uniform_distribution_(discrete)">
 * uniform integer distribution</a>.
 */
public class UniformDiscreteDistribution extends AbstractDiscreteDistribution {
    /** Lower bound (inclusive) of this distribution. */
    private final int lower;
    /** Upper bound (inclusive) of this distribution. */
    private final int upper;
    /** "upper" - "lower" + 1 (as a double to avoid overflow). */
    private final double upperMinusLowerPlus1;
    /** Cache of the probability. */
    private final double pmf;
    /** Cache of the log probability. */
    private final double logPmf;

    /**
     * Creates a new uniform integer distribution using the given lower and
     * upper bounds (both inclusive).
     *
     * @param lower Lower bound (inclusive) of this distribution.
     * @param upper Upper bound (inclusive) of this distribution.
     * @throws IllegalArgumentException if {@code lower > upper}.
     */
    public UniformDiscreteDistribution(int lower,
                                       int upper) {
        if (lower > upper) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH,
                                            lower, upper);
        }
        this.lower = lower;
        this.upper = upper;
        upperMinusLowerPlus1 = (double) upper - (double) lower + 1;
        pmf = 1.0 / upperMinusLowerPlus1;
        logPmf = -Math.log(upperMinusLowerPlus1);
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x) {
        if (x < lower || x > upper) {
            return 0;
        }
        return pmf;
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x < lower || x > upper) {
            return Double.NEGATIVE_INFINITY;
        }
        return logPmf;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x < lower) {
            return 0;
        }
        if (x > upper) {
            return 1;
        }
        return (x - lower + 1) / upperMinusLowerPlus1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower bound {@code lower} and upper bound {@code upper}, the mean is
     * {@code 0.5 * (lower + upper)}.
     */
    @Override
    public double getMean() {
        // Avoid overflow
        return 0.5 * ((double) upper + (double) lower);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower bound {@code lower} and upper bound {@code upper}, and
     * {@code n = upper - lower + 1}, the variance is {@code (n^2 - 1) / 12}.
     */
    @Override
    public double getVariance() {
        return (upperMinusLowerPlus1 * upperMinusLowerPlus1 - 1) / 12;
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
    public int getSupportLowerBound() {
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
    public int getSupportUpperBound() {
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

    /**{@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Discrete uniform distribution sampler.
        return DiscreteUniformSampler.of(rng, lower, upper)::sample;
    }
}
