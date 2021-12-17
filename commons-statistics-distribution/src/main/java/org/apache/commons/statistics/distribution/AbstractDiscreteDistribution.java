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

import java.util.function.IntUnaryOperator;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InverseTransformDiscreteSampler;

/**
 * Base class for integer-valued discrete distributions.  Default
 * implementations are provided for some of the methods that do not vary
 * from distribution to distribution.
 *
 * <p>This base class provides a default factory method for creating
 * a {@link DiscreteDistribution.Sampler sampler instance} that uses the
 * <a href="https://en.wikipedia.org/wiki/Inverse_transform_sampling">
 * inversion method</a> for generating random samples that follow the
 * distribution.
 *
 * <p>The class provides functionality to evaluate the probability in a range
 * using either the cumulative probability or the survival probability.
 * The survival probability is used if both arguments to
 * {@link #probability(int, int)} are above the median.
 * Child classes with a known median can override the default {@link #getMedian()}
 * method.
 */
abstract class AbstractDiscreteDistribution
    implements DiscreteDistribution {
    /** Marker value for no median.
     * This is a long to be outside the value of any possible int valued median. */
    private static final long NO_MEDIAN = Long.MIN_VALUE;

    /** Cached value of the median. */
    private long median = NO_MEDIAN;

    /**
     * Gets the median. This is used to determine if the arguments to the
     * {@link #probability(int, int)} function are in the upper or lower domain.
     *
     * <p>The default implementation calls {@link #inverseCumulativeProbability(double)}
     * with a value of 0.5.
     *
     * @return the median
     */
    protected int getMedian() {
        long m = median;
        if (m == NO_MEDIAN) {
            median = m = inverseCumulativeProbability(0.5);
        }
        return (int) m;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x0,
                              int x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH, x0, x1);
        }
        // As per the default interface method handle special cases:
        // x0     = x1 : return 0
        // x0 + 1 = x1 : return probability(x1)
        // Long addition avoids overflow
        if (x0 + 1L >= x1) {
            return x0 == x1 ? 0.0 : probability(x1);
        }

        // Use the survival probability when in the upper domain [3]:
        //
        //  lower          median         upper
        //    |              |              |
        // 1.     |------|
        //        x0     x1
        // 2.         |----------|
        //            x0         x1
        // 3.                  |--------|
        //                     x0       x1

        final double m = getMedian();
        if (x0 >= m) {
            return survivalProbability(x0) - survivalProbability(x1);
        }
        return cumulativeProbability(x1) - cumulativeProbability(x0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns:
     * <ul>
     * <li>{@link #getSupportLowerBound()} for {@code p = 0},</li>
     * <li>{@link #getSupportUpperBound()} for {@code p = 1}, or</li>
     * <li>the result of a binary search between the lower and upper bound using
     *     {@link #cumulativeProbability(int)}. The bounds may be bracketed for
     *     efficiency.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
     */
    @Override
    public int inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return inverseProbability(p, 1 - p, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns:
     * <ul>
     * <li>{@link #getSupportLowerBound()} for {@code p = 1},</li>
     * <li>{@link #getSupportUpperBound()} for {@code p = 0}, or</li>
     * <li>the result of a search for a root between the lower and upper bound using
     *     {@link #survivalProbability(int) survivalProbability(x) - p}.
     *     The bounds may be bracketed for efficiency.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
     */
    @Override
    public int inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return inverseProbability(1 - p, p, true);
    }

    /**
     * Implementation for the inverse cumulative or survival probability.
     *
     * @param p Cumulative probability.
     * @param q Survival probability.
     * @param complement Set to true to compute the inverse survival probability
     * @return the value
     */
    private int inverseProbability(double p, double q, boolean complement) {

        int lower = getSupportLowerBound();
        if (p == 0) {
            return lower;
        }
        int upper = getSupportUpperBound();
        if (q == 0) {
            return upper;
        }

        // The binary search sets the upper value to the mid-point
        // based on fun(x) >= 0. The upper value is returned.
        //
        // Create a function to search for x where the upper bound can be
        // lowered if:
        // cdf(x) >= p
        // sf(x)  <= q
        final IntUnaryOperator fun = complement ?
            x -> Double.compare(q, checkedProbability(survivalProbability(x))) :
            x -> Double.compare(checkedProbability(cumulativeProbability(x)), p);

        if (lower == Integer.MIN_VALUE) {
            if (fun.applyAsInt(lower) >= 0) {
                return lower;
            }
        } else {
            // this ensures:
            // cumulativeProbability(lower) < p
            // survivalProbability(lower) > q
            // which is important for the solving step
            lower -= 1;
        }

        // use the one-sided Chebyshev inequality to narrow the bracket
        // cf. AbstractContinuousDistribution.inverseCumulativeProbability(double)
        final double mu = getMean();
        final double sig = Math.sqrt(getVariance());
        final boolean chebyshevApplies = Double.isFinite(mu) &&
                                         ArgumentUtils.isFiniteStrictlyPositive(sig);

        if (chebyshevApplies) {
            double tmp = mu - sig * Math.sqrt(q / p);
            if (tmp > lower) {
                lower = ((int) Math.ceil(tmp)) - 1;
            }
            tmp = mu + sig * Math.sqrt(p / q);
            if (tmp < upper) {
                upper = ((int) Math.ceil(tmp)) - 1;
            }
        }

        // TODO
        // Improve the simple bisection to use a faster search,
        // e.g. a BrentSolver.

        return solveInverseProbability(fun, lower, upper);
    }

    /**
     * This is a utility function used by {@link
     * #inverseProbability(double, double, boolean)}. It assumes
     * that the inverse probability lies in the bracket {@code
     * (lower, upper]}. The implementation does simple bisection to find the
     * smallest {@code x} such that {@code fun(x) >= 0}.
     *
     * @param fun Probability function.
     * @param lowerBound Value satisfying {@code fun(lower) < 0}.
     * @param upperBound Value satisfying {@code fun(upper) >= 0}.
     * @return the smallest x
     */
    private static int solveInverseProbability(IntUnaryOperator fun,
                                               int lowerBound,
                                               int upperBound) {
        // Use long to prevent overflow during computation of the middle
        long lower = lowerBound;
        long upper = upperBound;
        while (lower + 1 < upper) {
            // Cannot replace division by 2 with a right shift because (lower + upper)
            // can be negative. This can be optimized when we know that both
            // lower and upper arguments of this method are positive, for
            // example, for PoissonDistribution.
            final long middle = (lower + upper) / 2;
            final double pm = fun.applyAsInt((int) middle);
            if (pm < 0) {
                lower = middle;
            } else {
                upper = middle;
            }
        }
        return (int) upper;
    }

    /**
     * Checks the probability function result for {@code NaN}.
     * Throws {@code IllegalStateException} if the cumulative
     * probability function returns {@code NaN}.
     *
     * <p>TODO: Q. Is this required? The bisection search will
     * eventually return if NaN is computed. Check the origin
     * of this in Commons Math. Its origins may be for debugging.
     *
     * @param result Probability
     * @return the probability.
     * @throws IllegalStateException if the probability is {@code NaN}.
     */
    private static double checkedProbability(double result) {
        if (Double.isNaN(result)) {
            throw new IllegalStateException("Internal error");
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Inversion method distribution sampler.
        return InverseTransformDiscreteSampler.of(rng, this::inverseCumulativeProbability)::sample;
    }
}
