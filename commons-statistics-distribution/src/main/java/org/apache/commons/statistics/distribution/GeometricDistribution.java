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
 * Implementation of the geometric distribution.
 *
 * <p>The probability mass function of \( X \) is:
 *
 * <p>\[ f(k; p) = (1-p)^k \, p \]
 *
 * <p>for \( p \in (0, 1] \) the probability of success and
 * \( k \in \{0, 1, 2, \dots\} \) the number of failures.
 *
 * <p>This parameterization is used to model the number of failures until
 * the first success.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Geometric_distribution">Geometric distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/GeometricDistribution.html">Geometric distribution (MathWorld)</a>
 */
public final class GeometricDistribution extends AbstractDiscreteDistribution {
    /** 1/2. */
    private static final double HALF = 0.5;

    /** The probability of success. */
    private final double probabilityOfSuccess;
    /** {@code log(p)} where p is the probability of success. */
    private final double logProbabilityOfSuccess;
    /** {@code log(1 - p)} where p is the probability of success. */
    private final double log1mProbabilityOfSuccess;
    /** Value of survival probability for x=0.
     * Used in the survival functions. Equal to (1 - probability of success). */
    private final double sf0;
    /** Implementation of PMF(x). Assumes that {@code x > 0}. */
    private final IntToDoubleFunction pmf;

    /**
     * @param p Probability of success.
     */
    private GeometricDistribution(double p) {
        probabilityOfSuccess = p;
        logProbabilityOfSuccess = Math.log(p);
        log1mProbabilityOfSuccess = Math.log1p(-p);
        sf0 = 1 - p;

        // Choose the PMF implementation.
        // When p >= 0.5 then 1 - p is exact and using the power function
        // is consistently more accurate than the use of the exponential function.
        // When p -> 0 then the exponential function avoids large error propagation
        // of the power function used with an inexact 1 - p.
        // Also compute the survival probability for use when x=0.
        if (p >= HALF) {
            pmf = x -> Math.pow(sf0, x) * probabilityOfSuccess;
        } else {
            pmf = x -> Math.exp(log1mProbabilityOfSuccess * x) * probabilityOfSuccess;
        }
    }

    /**
     * Creates a geometric distribution.
     *
     * @param p Probability of success.
     * @return the geometric distribution
     * @throws IllegalArgumentException if {@code p <= 0} or {@code p > 1}.
     */
    public static GeometricDistribution of(double p) {
        if (p <= 0 || p > 1) {
            throw new DistributionException(DistributionException.INVALID_NON_ZERO_PROBABILITY, p);
        }
        return new GeometricDistribution(p);
    }

    /**
     * Gets the probability of success parameter of this distribution.
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
            return x == 0 ? probabilityOfSuccess : 0;
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
        if (x <= 0) {
            // Note: CDF(x=0) = PDF(x=0) = probabilityOfSuccess
            return x == 0 ? probabilityOfSuccess : 0;
        }
        // Note: Double addition avoids overflow. This may compute a value less than 1.0
        // for the max integer value when p is very small.
        return -Math.expm1(log1mProbabilityOfSuccess * (x + 1.0));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x <= 0) {
            // Note: SF(x=0) = 1 - PDF(x=0) = 1 - probabilityOfSuccess
            // Use a pre-computed value to avoid cancellation when probabilityOfSuccess -> 0
            return x == 0 ? sf0 : 1;
        }
        // Note: Double addition avoids overflow. This may compute a value greater than 0.0
        // for the max integer value when p is very small.
        return Math.exp(log1mProbabilityOfSuccess * (x + 1.0));
    }

    /** {@inheritDoc} */
    @Override
    public int inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return getSupportUpperBound();
        }
        if (p <= probabilityOfSuccess) {
            return 0;
        }
        // p > probabilityOfSuccess
        // => log(1-p) < log(1-probabilityOfSuccess);
        // Both terms are negative as probabilityOfSuccess > 0.
        // This should be lower bounded to (2 - 1) = 1
        int x = (int) (Math.ceil(Math.log1p(-p) / log1mProbabilityOfSuccess) - 1);

        // Correct rounding errors.
        // This ensures x == icdf(cdf(x))

        if (cumulativeProbability(x - 1) >= p) {
            // No checks for x=0.
            // If x=0; cdf(-1) = 0 and the condition is false as p>0 at this point.
            x--;
        } else if (cumulativeProbability(x) < p && x < Integer.MAX_VALUE) {
            // The supported upper bound is max_value here as probabilityOfSuccess != 1
            x++;
        }

        return x;
    }

    /** {@inheritDoc} */
    @Override
    public int inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return getSupportUpperBound();
        }
        if (p >= sf0) {
            return 0;
        }

        // p < 1 - probabilityOfSuccess
        // Inversion as for icdf using log(p) in place of log1p(-p)
        int x = (int) (Math.ceil(Math.log(p) / log1mProbabilityOfSuccess) - 1);

        // Correct rounding errors.
        // This ensures x == isf(sf(x))

        if (survivalProbability(x - 1) <= p) {
            // No checks for x=0
            // If x=0; sf(-1) = 1 and the condition is false as p<1 at this point.
            x--;
        } else if (survivalProbability(x) > p && x < Integer.MAX_VALUE) {
            // The supported upper bound is max_value here as probabilityOfSuccess != 1
            x++;
        }

        return x;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For probability parameter \( p \), the mean is:
     *
     * <p>\[ \frac{1 - p}{p} \]
     */
    @Override
    public double getMean() {
        return (1 - probabilityOfSuccess) / probabilityOfSuccess;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For probability parameter \( p \), the variance is:
     *
     * <p>\[ \frac{1 - p}{p^2} \]
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
     * @return 0.
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
     * @return {@link Integer#MAX_VALUE} or 0.
     */
    @Override
    public int getSupportUpperBound() {
        return probabilityOfSuccess < 1 ? Integer.MAX_VALUE : 0;
    }

    /** {@inheritDoc} */
    @Override
    public Sampler createSampler(UniformRandomProvider rng) {
        return GeometricSampler.of(rng, probabilityOfSuccess)::sample;
    }
}
