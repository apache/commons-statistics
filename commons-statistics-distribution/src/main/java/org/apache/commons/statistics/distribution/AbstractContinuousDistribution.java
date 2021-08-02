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
 * This base class provides a default factory method for creating
 * a {@link ContinuousDistribution.Sampler sampler instance} that uses the
 * <a href="http://en.wikipedia.org/wiki/Inverse_transform_sampling">
 * inversion method</a> for generating random samples that follow the
 * distribution.
 */
abstract class AbstractContinuousDistribution
    implements ContinuousDistribution {
    // XXX Values copied from defaults in class
    // "o.a.c.math4.analysis.solvers.BaseAbstractUnivariateSolver"

    /** BrentSolver relative accuracy. */
    private static final double SOLVER_RELATIVE_ACCURACY = 1e-14;
    /** BrentSolver absolute accuracy. */
    private static final double SOLVER_ABSOLUTE_ACCURACY = 1e-9;
    /** BrentSolver function value accuracy. */
    private static final double SOLVER_FUNCTION_VALUE_ACCURACY = 1e-15;

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
     */
    @Override
    public double inverseCumulativeProbability(final double p) {
        /*
         * IMPLEMENTATION NOTES
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
        if (p < 0 ||
            p > 1) {
            throw new DistributionException(DistributionException.INVALID_PROBABILITY, p);
        }

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
                                         Double.isFinite(sig);

        if (lowerBound == Double.NEGATIVE_INFINITY) {
            if (chebyshevApplies) {
                lowerBound = mu - sig * Math.sqrt((1 - p) / p);
            } else {
                lowerBound = -1;
                while (cumulativeProbability(lowerBound) >= p) {
                    lowerBound *= 2;
                }
            }
        }

        if (upperBound == Double.POSITIVE_INFINITY) {
            if (chebyshevApplies) {
                upperBound = mu + sig * Math.sqrt(p / (1 - p));
            } else {
                upperBound = 1;
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
            if (x - dx >= getSupportLowerBound()) {
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
