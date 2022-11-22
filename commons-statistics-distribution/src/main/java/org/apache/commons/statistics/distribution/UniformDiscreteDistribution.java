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
 * Implementation of the uniform discrete distribution.
 *
 * <p>The probability mass function of \( X \) is:
 *
 * <p>\[ f(k; a, b) = \frac{1}{b-a+1} \]
 *
 * <p>for integer \( a, b \) and \( a \le b \) and
 * \( k \in [a, b] \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Uniform_distribution_(discrete)">
 * Uniform distribution (discrete) (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/DiscreteUniformDistribution.html">
 * Discrete uniform distribution (MathWorld)</a>
 */
public final class UniformDiscreteDistribution extends AbstractDiscreteDistribution {
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
    /** Value of survival probability for x=0. Used in the inverse survival function. */
    private final double sf0;

    /**
     * @param lower Lower bound (inclusive) of this distribution.
     * @param upper Upper bound (inclusive) of this distribution.
     */
    private UniformDiscreteDistribution(int lower,
                                        int upper) {
        this.lower = lower;
        this.upper = upper;
        upperMinusLowerPlus1 = (double) upper - (double) lower + 1;
        pmf = 1.0 / upperMinusLowerPlus1;
        logPmf = -Math.log(upperMinusLowerPlus1);
        sf0 = (upperMinusLowerPlus1 - 1) / upperMinusLowerPlus1;
    }

    /**
     * Creates a new uniform integer distribution using the given lower and upper
     * bounds (both inclusive).
     *
     * @param lower Lower bound (inclusive) of this distribution.
     * @param upper Upper bound (inclusive) of this distribution.
     * @return the distribution
     * @throws IllegalArgumentException if {@code lower > upper}.
     */
    public static UniformDiscreteDistribution of(int lower,
                                                 int upper) {
        if (lower > upper) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH,
                                            lower, upper);
        }
        return new UniformDiscreteDistribution(lower, upper);
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
    public double probability(int x0,
                              int x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH, x0, x1);
        }
        if (x0 >= upper || x1 < lower) {
            // (x0, x1] does not overlap [lower, upper]
            return 0;
        }

        // x0 < upper
        // x1 >= lower

        // Find the range between x0 (exclusive) and x1 (inclusive) within [lower, upper].
        // In the case of x0 < lower set l so that u - l == (u - lower) + 1
        // long arithmetic prevents overflow
        final long l = Math.max(lower - 1L, x0);
        final long u = Math.min(upper, x1);

        return (u - l) / upperMinusLowerPlus1;
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
        if (x <= lower) {
            // Note: CDF(x=0) = PDF(x=0)
            return x == lower ? pmf : 0;
        }
        if (x >= upper) {
            return 1;
        }
        return ((double) x - lower + 1) / upperMinusLowerPlus1;
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x <= lower) {
            // Note: SF(x=0) = 1 - PDF(x=0)
            // Use a pre-computed value to avoid cancellation when probabilityOfSuccess -> 0
            return x == lower ? sf0 : 1;
        }
        if (x >= upper) {
            return 0;
        }
        return ((double) upper - x) / upperMinusLowerPlus1;
    }

    /** {@inheritDoc} */
    @Override
    public int inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p > sf0) {
            return upper;
        }
        if (p <= pmf) {
            return lower;
        }
        // p in ( pmf         , sf0             ]
        // p in ( 1 / {u-l+1} , {u-l} / {u-l+1} ]
        // x in ( l           , u-1             ]
        int x = (int) (lower + Math.ceil(p * upperMinusLowerPlus1) - 1);

        // Correct rounding errors.
        // This ensures x == icdf(cdf(x))
        // Note: Directly computing the CDF(x-1) avoids integer overflow if x=min_value

        if (((double) x - lower) / upperMinusLowerPlus1 >= p) {
            // No check for x > lower: cdf(x=lower) = 0 and thus is below p
            // cdf(x-1) >= p
            x--;
        } else if (((double) x - lower + 1) / upperMinusLowerPlus1 < p) {
            // No check for x < upper: cdf(x=upper) = 1 and thus is above p
            // cdf(x) < p
            x++;
        }

        return x;
    }

    /** {@inheritDoc} */
    @Override
    public int inverseSurvivalProbability(final double p) {
        ArgumentUtils.checkProbability(p);
        if (p < pmf) {
            return upper;
        }
        if (p >= sf0) {
            return lower;
        }
        // p in [ pmf         , sf0             )
        // p in [ 1 / {u-l+1} , {u-l} / {u-l+1} )
        // x in [ u-1         , l               )
        int x = (int) (upper - Math.floor(p * upperMinusLowerPlus1));

        // Correct rounding errors.
        // This ensures x == isf(sf(x))
        // Note: Directly computing the SF(x-1) avoids integer overflow if x=min_value

        if (((double) upper - x + 1) / upperMinusLowerPlus1 <= p) {
            // No check for x > lower: sf(x=lower) = 1 and thus is above p
            // sf(x-1) <= p
            x--;
        } else if (((double) upper - x) / upperMinusLowerPlus1 > p) {
            // No check for x < upper: sf(x=upper) = 0 and thus is below p
            // sf(x) > p
            x++;
        }

        return x;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower bound \( a \) and upper bound \( b \), the mean is \( \frac{a + b}{2} \).
     */
    @Override
    public double getMean() {
        // Avoid overflow
        return 0.5 * ((double) upper + (double) lower);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower bound \( a \) and upper bound \( b \), the variance is:
     *
     * <p>\[ \frac{n^2 - 1}{12} \]
     *
     * <p>where \( n = b - a + 1 \).
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
     */
    @Override
    public int getSupportUpperBound() {
        return upper;
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Discrete uniform distribution sampler.
        return DiscreteUniformSampler.of(rng, lower, upper)::sample;
    }
}
