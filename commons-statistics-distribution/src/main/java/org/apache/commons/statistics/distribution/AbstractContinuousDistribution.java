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

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
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
    double getMedian() {
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
    public double inverseCumulativeProbability(double p) {
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
     *     {@link #survivalProbability(double) survivalProbability(x) - p}.
     *     The bounds may be bracketed for efficiency.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
     */
    @Override
    public double inverseSurvivalProbability(double p) {
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
    private double inverseProbability(final double p, final double q, boolean complement) {
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
         *
         * In the case of the survival probability the bracket can be set using the same
         * bound given that the argument p = 1 - q, with q the survival probability.
         */

        double lowerBound = getSupportLowerBound();
        if (p == 0) {
            return lowerBound;
        }
        double upperBound = getSupportUpperBound();
        if (q == 0) {
            return upperBound;
        }

        final double mu = getMean();
        final double sig = Math.sqrt(getVariance());
        final boolean chebyshevApplies = Double.isFinite(mu) &&
                                         ArgumentUtils.isFiniteStrictlyPositive(sig);

        if (lowerBound == Double.NEGATIVE_INFINITY) {
            lowerBound = createFiniteLowerBound(p, q, complement, upperBound, mu, sig, chebyshevApplies);
        }

        if (upperBound == Double.POSITIVE_INFINITY) {
            upperBound = createFiniteUpperBound(p, q, complement, lowerBound, mu, sig, chebyshevApplies);
        }

        // Here the bracket [lower, upper] uses finite values. If the support
        // is infinite the bracket can truncate the distribution and the target
        // probability can be outside the range of [lower, upper].
        if (upperBound == Double.MAX_VALUE) {
            if (complement) {
                if (survivalProbability(upperBound) > q) {
                    return getSupportUpperBound();
                }
            } else if (cumulativeProbability(upperBound) < p) {
                return getSupportUpperBound();
            }
        }
        if (lowerBound == -Double.MAX_VALUE) {
            if (complement) {
                if (survivalProbability(lowerBound) < q) {
                    return getSupportLowerBound();
                }
            } else if (cumulativeProbability(lowerBound) > p) {
                return getSupportLowerBound();
            }
        }

        final DoubleUnaryOperator fun = complement ?
            arg -> survivalProbability(arg) - q :
            arg -> cumulativeProbability(arg) - p;
        // Note the initial value is robust to overflow.
        // Do not use 0.5 * (lowerBound + upperBound).
        final double x = new BrentSolver(SOLVER_RELATIVE_ACCURACY,
                                         SOLVER_ABSOLUTE_ACCURACY,
                                         SOLVER_FUNCTION_VALUE_ACCURACY)
            .findRoot(fun,
                      lowerBound,
                      lowerBound + 0.5 * (upperBound - lowerBound),
                      upperBound);

        if (!isSupportConnected()) {
            return searchPlateau(complement, lowerBound, x);
        }
        return x;
    }

    /**
     * Create a finite lower bound. Assumes the current lower bound is negative infinity.
     *
     * @param p Cumulative probability.
     * @param q Survival probability.
     * @param complement Set to true to compute the inverse survival probability
     * @param upperBound Current upper bound
     * @param mu Mean
     * @param sig Standard deviation
     * @param chebyshevApplies True if the Chebyshev inequality applies (mean is finite and {@code sig > 0}}
     * @return the finite lower bound
     */
    private double createFiniteLowerBound(final double p, final double q, boolean complement,
        double upperBound, final double mu, final double sig, final boolean chebyshevApplies) {
        double lowerBound;
        if (chebyshevApplies) {
            lowerBound = mu - sig * Math.sqrt(q / p);
        } else {
            lowerBound = Double.NEGATIVE_INFINITY;
        }
        // Bound may have been set as infinite
        if (lowerBound == Double.NEGATIVE_INFINITY) {
            lowerBound = Math.min(-1, upperBound);
            if (complement) {
                while (survivalProbability(lowerBound) < q) {
                    lowerBound *= 2;
                }
            } else {
                while (cumulativeProbability(lowerBound) >= p) {
                    lowerBound *= 2;
                }
            }
            // Ensure finite
            lowerBound = Math.max(lowerBound, -Double.MAX_VALUE);
        }
        return lowerBound;
    }

    /**
     * Create a finite upper bound. Assumes the current upper bound is positive infinity.
     *
     * @param p Cumulative probability.
     * @param q Survival probability.
     * @param complement Set to true to compute the inverse survival probability
     * @param lowerBound Current lower bound
     * @param mu Mean
     * @param sig Standard deviation
     * @param chebyshevApplies True if the Chebyshev inequality applies (mean is finite and {@code sig > 0}}
     * @return the finite lower bound
     */
    private double createFiniteUpperBound(final double p, final double q, boolean complement,
        double lowerBound, final double mu, final double sig, final boolean chebyshevApplies) {
        double upperBound;
        if (chebyshevApplies) {
            upperBound = mu + sig * Math.sqrt(p / q);
        } else {
            upperBound = Double.POSITIVE_INFINITY;
        }
        // Bound may have been set as infinite
        if (upperBound == Double.POSITIVE_INFINITY) {
            upperBound = Math.max(1, lowerBound);
            if (complement) {
                while (survivalProbability(upperBound) >= q) {
                    upperBound *= 2;
                }
            } else {
                while (cumulativeProbability(upperBound) < p) {
                    upperBound *= 2;
                }
            }
            // Ensure finite
            upperBound = Math.min(upperBound, Double.MAX_VALUE);
        }
        return upperBound;
    }

    /**
     * Indicates whether the support is connected, i.e. whether all values between the
     * lower and upper bound of the support are included in the support.
     *
     * <p>This method is used in the default implementation of the inverse cumulative and
     * survival probability functions.
     *
     * <p>The default value is true which assumes the cdf and sf have no plateau regions
     * where the same probability value is returned for a large range of x.
     * Override this method if there are gaps in the support of the cdf and sf.
     *
     * <p>If false then the inverse will perform an additional step to ensure that the
     * lower-bound of the interval on which the cdf is constant should be returned. This
     * will search from the initial point x downwards if a smaller value also has the same
     * cumulative (survival) probability.
     *
     * <p>Any plateau with a width in x smaller than the inverse absolute accuracy will
     * not be searched.
     *
     * <p>Note: This method was public in commons math. It has been reduced to protected
     * in commons statistics as it is an implementation detail.
     *
     * @return whether the support is connected.
     * @see <a href="https://issues.apache.org/jira/browse/MATH-699">MATH-699</a>
     */
    boolean isSupportConnected() {
        return true;
    }

    /**
     * Test the probability function for a plateau at the point x. If detected
     * search the plateau for the lowest point y such that
     * {@code inf{y in R | P(y) == P(x)}}.
     *
     * <p>This function is used when the distribution support is not connected
     * to satisfy the inverse probability requirements of {@link ContinuousDistribution}
     * on the returned value.
     *
     * @param complement Set to true to search the survival probability.
     * @param lower Lower bound used to limit the search downwards.
     * @param x Current value.
     * @return the infimum y
     */
    private double searchPlateau(boolean complement, double lower, final double x) {
        // Test for plateau. Lower the value x if the probability is the same.
        // Ensure the step is robust to the solver accuracy being less
        // than 1 ulp of x (e.g. dx=0 will infinite loop)
        final double dx = Math.max(SOLVER_ABSOLUTE_ACCURACY, Math.ulp(x));
        if (x - dx >= lower) {
            final DoubleUnaryOperator fun = complement ?
                this::survivalProbability :
                this::cumulativeProbability;
            final double px = fun.applyAsDouble(x);
            if (fun.applyAsDouble(x - dx) == px) {
                double upperBound = x;
                double lowerBound = lower;
                // Bisection search
                // Require cdf(x) < px and sf(x) > px to move the lower bound
                // to the midpoint.
                final DoubleBinaryOperator cmp = complement ?
                    (a, b) -> a > b ? -1 : 1 :
                    (a, b) -> a < b ? -1 : 1;
                while (upperBound - lowerBound > dx) {
                    final double midPoint = 0.5 * (lowerBound + upperBound);
                    if (cmp.applyAsDouble(fun.applyAsDouble(midPoint), px) < 0) {
                        lowerBound = midPoint;
                    } else {
                        upperBound = midPoint;
                    }
                }
                return upperBound;
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
