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
    /** 1 / 12. **/
    private static final double ONE_TWELFTH = 1 / 12d;
    /** Lower bound (inclusive) of this distribution. */
    private final int lower;
    /** Upper bound (inclusive) of this distribution. */
    private final int upper;
    /** "upper" + "lower" (to avoid overflow). */
    private final double upperPlusLower;
    /** "upper" - "lower" (to avoid overflow). */
    private final double upperMinusLower;

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
            throw new DistributionException(DistributionException.TOO_LARGE,
                                            lower, upper);
        }
        this.lower = lower;
        this.upper = upper;
        upperPlusLower = (double) upper + (double) lower;
        upperMinusLower = (double) upper - (double) lower;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x) {
        if (x < lower || x > upper) {
            return 0;
        }
        return 1 / (upperMinusLower + 1);
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
        return (x - lower + 1) / (upperMinusLower + 1);
    }

    /**
     * {@inheritDoc}
     *
     * For lower bound {@code lower} and upper bound {@code upper}, the mean is
     * {@code 0.5 * (lower + upper)}.
     */
    @Override
    public double getMean() {
        return 0.5 * upperPlusLower;
    }

    /**
     * {@inheritDoc}
     *
     * For lower bound {@code lower} and upper bound {@code upper}, and
     * {@code n = upper - lower + 1}, the variance is {@code (n^2 - 1) / 12}.
     */
    @Override
    public double getVariance() {
        final double n = upperMinusLower + 1;
        return ONE_TWELFTH * (n * n - 1);
    }

    /**
     * {@inheritDoc}
     *
     * The lower bound of the support is equal to the lower bound parameter
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
     * The upper bound of the support is equal to the upper bound parameter
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
     * The support of this distribution is connected.
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
        return new DiscreteUniformSampler(rng, lower, upper)::sample;
    }
}
