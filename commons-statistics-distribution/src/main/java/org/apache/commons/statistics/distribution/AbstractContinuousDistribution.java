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

import org.apache.commons.numbers.rootfinder.BrentSolver;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InverseTransformContinuousSampler;

/**
 * Base class for probability distributions on the reals.
 * Default implementations are provided for some of the methods
 * that do not vary from distribution to distribution.
 *
 * <p>This base class provides a default factory method for creating
 * a {@link ContinuousDistribution.Sampler sampler instance} that uses the
 * <a href="http://en.wikipedia.org/wiki/Inverse_transform_sampling">
 * inversion method</a> for generating random samples that follow the
 * distribution.
 *
 * <p>The class provides functionality to evaluate the probability in a range
 * using either the cumulative probability or the survival probability.
 * The survival probability is used if both arguments to
 * {@link #probability(double, double)} are above the median.
 * Child classes with a known median can override the default {@link #getMedian()}
 * method.
 */
abstract class AbstractContinuousDistribution
    implements ContinuousDistribution {
    // XXX Values copied from defaults in class
    // "o.a.c.math4.analysis.solvers.BaseAbstractUnivariateSolver"

    // Note:
    // Probability distributions may have very small CDF values.
    // This creates issues when using the Brent solver.
    //
    // 1. It cannot identify bracketing if the multiplication of the two
    // end points creates a signed zero since the condition uses:
    //   lower * upper < 0
    // It should be changed to Double.compare(lower * upper, 0.0) < 0.
    //
    // 2. Function value accuracy determines if the Brent solver performs a
    // search. Ideally set to zero to force a search (unless one of the the
    // initial bracket values is correct). This can result in functions
    // that evaluate very small p to raise a no-bracket exception due to [1].
    //
    // 3. Solver absolute accuracy is the minimum absolute difference between
    // bracketing points to continue the search. To invert very small probability
    // values (which map to very small x values) requires a very small absolute
    // accuracy otherwise the search stops too soon. Set to zero to force
    // stopping criteria based only on the relative difference between points.
    //
    // Note:
    // The Brent solver does not allow a stopping criteria for the proximity
    // to the root. It is hard coded to 1 ULP (CDF(x) - p == 0). Thus we
    // search until there is a small relative difference between the upper
    // and lower bracket of the root.

    // TODO:
    // Extract the Brent solver code into this class and fix it for the known
    // issues. These can be transferred back into the original class when the
    // best solution is known.
    //
    // Changes to this class affect many samplers. Altering the accuracy
    // thresholds can cause any test that uses the inverse CDF to fail. This
    // includes sampling tests for distributions with use the inverse
    // transform sampler which have been coded with fixed seeds that work.

    /** BrentSolver relative accuracy. */
    private static final double SOLVER_RELATIVE_ACCURACY = 1e-14;
    /** BrentSolver absolute accuracy. */
    private static final double SOLVER_ABSOLUTE_ACCURACY = 1e-9;
    /** BrentSolver function value accuracy. */
    private static final double SOLVER_FUNCTION_VALUE_ACCURACY = 1e-15;

    /** Cached value of the median. */
    private double median = Double.NaN;

    /**
     * Gets the median. This is used to determine if the arguments to the
     * {@link #probability(double, double)} function are in the upper or lower domain.
     *
     * <p>The default implementation calls {@link #inverseCumulativeProbability(double)}
     * with a value of 0.5.
     *
     * @return the median
     */
    protected double getMedian() {
        double m = median;
        if (Double.isNaN(m)) {
            median = m = inverseCumulativeProbability(0.5);
        }
        return m;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(double x0,
                              double x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH, x0, x1);
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
     * <li>the result of a search for a root between the lower and upper bound using
     *     {@link #cumulativeProbability(double) cdf(x) - p}. The bounds may be bracketed for
     *     efficiency.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
     */
    @Override
    public double inverseCumulativeProbability(final double p) {
        /* IMPLEMENTATION NOTES
         * --------------------
         * Where applicable, use is made of the one-sided Chebyshev inequality
         * to bracket the root. This inequality states that
         * P(X - mu >= k * sig) <= 1 / (1 + k^2),
         * mu: mean, sig: standard deviation. Equivalently
         * 1 - P(X < mu + k * sig) <= 1 / (1 + k^2),
         * F(mu + k * sig) >= k^2 / (1 + k^2).
         *
         * For k = sqrt(p / (1 - p)), we find
         * F(mu + k * sig) >= p,
         * and (mu + k * sig) is an upper-bound for the root.
         *
         * Then, introducing Y = -X, mean(Y) = -mu, sd(Y) = sig, and
         * P(Y >= -mu + k * sig) <= 1 / (1 + k^2),
         * P(-X >= -mu + k * sig) <= 1 / (1 + k^2),
         * P(X <= mu - k * sig) <= 1 / (1 + k^2),
         * F(mu - k * sig) <= 1 / (1 + k^2).
         *
         * For k = sqrt((1 - p) / p), we find
         * F(mu - k * sig) <= p,
         * and (mu - k * sig) is a lower-bound for the root.
         *
         * In cases where the Chebyshev inequality does not apply, geometric
         * progressions 1, 2, 4, ... and -1, -2, -4, ... are used to bracket
         * the root.
         */
        ArgumentUtils.checkProbability(p);

        double lowerBound = getSupportLowerBound();
        if (p == 0) {
            return lowerBound;
        }

        double upperBound = getSupportUpperBound();
        if (p == 1) {
            return upperBound;
        }

        final double mu = getMean();
        final double sig = Math.sqrt(getVariance());
        final boolean chebyshevApplies = Double.isFinite(mu) &&
                                         ArgumentUtils.isFiniteStrictlyPositive(sig);

        if (lowerBound == Double.NEGATIVE_INFINITY) {
            if (chebyshevApplies) {
                lowerBound = mu - sig * Math.sqrt((1 - p) / p);
            }
            // Bound may have been set as infinite
            if (lowerBound == Double.NEGATIVE_INFINITY) {
                lowerBound = Math.min(-1, upperBound);
                while (cumulativeProbability(lowerBound) >= p) {
                    lowerBound *= 2;
                }
            }
        }

        if (upperBound == Double.POSITIVE_INFINITY) {
            if (chebyshevApplies) {
                upperBound = mu + sig * Math.sqrt(p / (1 - p));
            }
            // Bound may have been set as infinite
            if (upperBound == Double.POSITIVE_INFINITY) {
                upperBound = Math.max(1, lowerBound);
                while (cumulativeProbability(upperBound) < p) {
                    upperBound *= 2;
                }
            }
        }

        final double x = new BrentSolver(SOLVER_RELATIVE_ACCURACY,
                                         SOLVER_ABSOLUTE_ACCURACY,
                                         SOLVER_FUNCTION_VALUE_ACCURACY)
            .findRoot(arg -> cumulativeProbability(arg) - p,
                      lowerBound,
                      0.5 * (lowerBound + upperBound),
                      upperBound);

        if (!isSupportConnected()) {
            /* Test for plateau. */
            final double dx = SOLVER_ABSOLUTE_ACCURACY;
            if (x - dx >= lowerBound) {
                final double px = cumulativeProbability(x);
                if (cumulativeProbability(x - dx) == px) {
                    upperBound = x;
                    while (upperBound - lowerBound > dx) {
                        final double midPoint = 0.5 * (lowerBound + upperBound);
                        if (cumulativeProbability(midPoint) < px) {
                            lowerBound = midPoint;
                        } else {
                            upperBound = midPoint;
                        }
                    }
                    return upperBound;
                }
            }
        }
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Inversion method distribution sampler.
        return InverseTransformContinuousSampler.of(rng, this::inverseCumulativeProbability)::sample;
    }
}
