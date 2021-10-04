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

import java.util.function.IntToDoubleFunction;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.GeometricSampler;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Geometric_distribution">geometric distribution</a>.
 */
public class GeometricDistribution extends AbstractDiscreteDistribution {
    /** 1/2. */
    private static final double HALF = 0.5;

    /** The probability of success. */
    private final double probabilityOfSuccess;
    /** {@code log(p)} where p is the probability of success. */
    private final double logProbabilityOfSuccess;
    /** {@code log(1 - p)} where p is the probability of success. */
    private final double log1mProbabilityOfSuccess;
    /** Implementation of PMF(x). Assumes that {@code x > 0}. */
    private final IntToDoubleFunction pmf;

    /**
     * Creates a geometric distribution.
     *
     * @param p Probability of success.
     * @throws IllegalArgumentException if {@code p <= 0} or {@code p > 1}.
     */
    public GeometricDistribution(double p) {
        if (p <= 0 || p > 1) {
            throw new DistributionException(DistributionException.INVALID_NON_ZERO_PROBABILITY, p);
        }

        probabilityOfSuccess = p;
        logProbabilityOfSuccess = Math.log(p);
        log1mProbabilityOfSuccess = Math.log1p(-p);

        // Choose the PMF implementation.
        // When p >= 0.5 then 1 - p is exact and using the power function
        // is consistently more accurate than the use of the exponential function.
        // When p -> 0 then the exponential function avoids large error propagation
        // of the power function used with an inexact 1 - p.
        if (p >= HALF) {
            final double q = 1 - p;
            pmf = x -> Math.pow(q, x) * probabilityOfSuccess;
        } else {
            pmf = x -> Math.exp(log1mProbabilityOfSuccess * x) * probabilityOfSuccess;
        }
    }

    /**
     * Access the probability of success for this distribution.
     *
     * @return the probability of success.
     */
    public double getProbabilityOfSuccess() {
        return probabilityOfSuccess;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x) {
        if (x <= 0) {
            // Special case of x=0 exploiting cancellation.
            return x == 0 ? probabilityOfSuccess : 0.0;
        }
        return pmf.applyAsDouble(x);
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x <= 0) {
            // Special case of x=0 exploiting cancellation.
            return x == 0 ? logProbabilityOfSuccess : Double.NEGATIVE_INFINITY;
        }
        return x * log1mProbabilityOfSuccess + logProbabilityOfSuccess;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x < 0) {
            return 0.0;
        }
        return -Math.expm1(log1mProbabilityOfSuccess * (x + 1));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x < 0) {
            return 1.0;
        } else if (x == Integer.MAX_VALUE) {
            return 0.0;
        }
        return Math.exp(log1mProbabilityOfSuccess * (x + 1));
    }

    /** {@inheritDoc} */
    @Override
    public int inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return getSupportUpperBound();
        }
        if (p == 0) {
            return 0;
        }
        final int x = (int) Math.ceil(Math.log1p(-p) / log1mProbabilityOfSuccess - 1);
        // Note: x may be too high due to floating-point error and rounding up with ceil.
        // Return the next value down if that is also above the input cumulative probability.
        // This ensures x == icdf(cdf(x))
        if (x <= 0) {
            return 0;
        }
        return cumulativeProbability(x - 1) >= p ? x - 1 : x;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For probability parameter {@code p}, the mean is {@code (1 - p) / p}.
     */
    @Override
    public double getMean() {
        return (1 - probabilityOfSuccess) / probabilityOfSuccess;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For probability parameter {@code p}, the variance is
     * {@code (1 - p) / (p * p)}.
     */
    @Override
    public double getVariance() {
        return (1 - probabilityOfSuccess) / (probabilityOfSuccess * probabilityOfSuccess);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0.
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
     * <p>The upper bound of the support is positive infinity except for the
     * probability parameter {@code p = 1.0}.
     *
     * @return upper bound of the support ({@code Integer.MAX_VALUE} or 0)
     */
    @Override
    public int getSupportUpperBound() {
        return probabilityOfSuccess < 1 ? Integer.MAX_VALUE : 0;
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
    public Sampler createSampler(UniformRandomProvider rng) {
        return GeometricSampler.of(rng, probabilityOfSuccess)::sample;
    }
}
