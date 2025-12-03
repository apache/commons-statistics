/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.statistics.examples.jmh.distribution;

import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleUnaryOperator;
import org.apache.commons.numbers.rootfinder.BrentSolver;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.ChiSquaredDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.FDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;
import org.apache.commons.statistics.distribution.NakagamiDistribution;
import org.apache.commons.statistics.distribution.TDistribution;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Executes a benchmark of inverse probability function operations
 * (inverse cumulative distribution function (CDF) and inverse survival function (SF)).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class InverseProbabilityPerformance {
    /** No-operation for baseline. */
    private static final String NOOP = "Noop";
    /** Message prefix for an unknown function. */
    private static final String UNKNOWN_FUNCTION = "unknown function: ";
    /** Message prefix for an unknown distribution. */
    private static final String UNKNOWN_DISTRIBUTION = "unknown distrbution: ";

    /**
     * The seed for random number generation. Ensures the same numbers are generated
     * for each implementation of the function.
     */
    private static final long SEED = ThreadLocalRandom.current().nextLong();

    /**
     * Contains the inverse function to benchmark.
     */
    @State(Scope.Benchmark)
    public static class InverseData {
        /** The implementation of the function. */
        @Param({NOOP,
            // Worst accuracy cases from STATISTICS-36
            "Beta:4:0.1",
            "ChiSquared:0.1",
            "F:5:6",
            "Gamma:4:2",
            "Nakagami:0.33333333333:1",
            "T:5",
        })
        private String implementation;

        /** The inversion relative accuracy. */
        @Param({
            // Default from o.a.c.math4.analysis.solvers.BaseAbstractUnivariateSolver
            "1e-14",
            // Lowest value so that 2 * eps * x is 1 ULP. Equal to 2^-53.
            "1.1102230246251565E-16"})
        private double relEps;

        /** The inversion absolute accuracy. */
        @Param({
            // Default from o.a.c.math4.analysis.solvers.BaseAbstractUnivariateSolver
            "1e-9",
            // Lowest non-zero value. Equal to Double.MIN_VALUE.
            "4.9e-324"})
        private double absEps;

        /** The function to invert. */
        @Param({"cdf", "sf"})
        private String invert;

        /** Source of randomness for probabilities in the range [0, 1]. */
        private SplittableRandom rng;

        /** The inverse probability function. */
        private DoubleUnaryOperator function;

        /**
         * Create the next inversion of a probability.
         *
         * @return the result
         */
        public double next() {
            return function.applyAsDouble(rng.nextDouble());
        }

        /**
         * Create the source of random probability values and the inverse probability function.
         */
        @Setup
        public void setup() {
            // Creation with a seed ensures the increment uses the golden ratio
            // with its known robust statistical properties. Creating with no
            // seed will use a random increment.
            rng = new SplittableRandom(SEED);
            function = createFunction(implementation, relEps, absEps, invert);
        }

        /**
         * Creates the inverse probability function.
         *
         * @param implementation Function implementation
         * @param relativeAccuracy Inversion relative accuracy
         * @param absoluteAccuracy Inversion absolute accuracy
         * @param invert Function to invert
         * @return the function
         */
        private static DoubleUnaryOperator createFunction(String implementation,
                                                          double relativeAccuracy,
                                                          double absoluteAccuracy,
                                                          String invert) {
            if (implementation.startsWith(NOOP)) {
                return x -> x;
            }

            // Create the distribution
            final ContinuousDistribution dist = createDistribution(implementation);

            // Get the function inverter
            final ContinuousDistributionInverter inverter =
                new ContinuousDistributionInverter(dist, relativeAccuracy, absoluteAccuracy);
            // Support CDF and SF
            if ("cdf".equals(invert)) {
                return inverter::inverseCumulativeProbability;
            } else if ("sf".equals(invert)) {
                return inverter::inverseSurvivalProbability;
            }
            throw new IllegalStateException(UNKNOWN_FUNCTION + invert);
        }

        /**
         * Creates the distribution.
         *
         * @param implementation Function implementation
         * @return the continuous distribution
         */
        private static ContinuousDistribution createDistribution(String implementation) {
            // Implementation is:
            // distribution:param1:param2:...
            final String[] parts = implementation.split(":");
            if ("Beta".equals(parts[0])) {
                return BetaDistribution.of(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            } else if ("ChiSquared".equals(parts[0])) {
                return ChiSquaredDistribution.of(Double.parseDouble(parts[1]));
            } else if ("F".equals(parts[0])) {
                return FDistribution.of(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            } else if ("Gamma".equals(parts[0])) {
                return GammaDistribution.of(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            } else if ("Nakagami".equals(parts[0])) {
                return NakagamiDistribution.of(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            } else if ("T".equals(parts[0])) {
                return TDistribution.of(Double.parseDouble(parts[1]));
            }
            throw new IllegalStateException(UNKNOWN_DISTRIBUTION + implementation);
        }

        /**
         * Class to invert the cumulative or survival probability.
         * This is based on the implementation in the AbstractContinuousDistribution class
         * from Commons Statistics version 1.0.
         */
        static class ContinuousDistributionInverter {
            /** BrentSolver function value accuracy.
             * Set to a very low value to search using Brent's method unless
             * the starting point is correct. */
            private static final double SOLVER_FUNCTION_VALUE_ACCURACY = Double.MIN_VALUE;

            /** BrentSolver relative accuracy. This is used with {@code 2 * eps * abs(b)}
             * so the minimum non-zero value with an effect is half of machine epsilon (2^-53). */
            private final double relativeAccuracy;
            /** BrentSolver absolute accuracy. */
            private final double absoluteAccuracy;
            /** The distribution. */
            private final ContinuousDistribution dist;

            /**
             * @param dist The distribution to invert
             * @param relativeAccuracy Solver relative accuracy
             * @param absoluteAccuracy Solver absolute accuracy
             */
            ContinuousDistributionInverter(ContinuousDistribution dist,
                                           double relativeAccuracy,
                                           double absoluteAccuracy) {
                this.dist = dist;
                this.relativeAccuracy = relativeAccuracy;
                this.absoluteAccuracy = absoluteAccuracy;
            }

            /**
             * Checks if the value {@code x} is finite and strictly positive.
             *
             * @param x Value
             * @return true if {@code x > 0} and is finite
             */
            private static boolean isFiniteStrictlyPositive(double x) {
                return x > 0 && x < Double.POSITIVE_INFINITY;
            }

            /**
             * Check the probability {@code p} is in the interval {@code [0, 1]}.
             *
             * @param p Probability
             * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
             */
            private static void checkProbability(double p) {
                if (p >= 0 && p <= 1) {
                    return;
                }
                // Out-of-range or NaN
                throw new IllegalArgumentException("Invalid p: " + p);
            }

            /**
             * Compute the inverse cumulative probability.
             *
             * @param p Probability
             * @return the value
             * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
             */
            public double inverseCumulativeProbability(double p) {
                checkProbability(p);
                return inverseProbability(p, 1 - p, false);
            }

            /**
             * Compute the inverse survival probability.
             *
             * @param p Probability
             * @return the value
             * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
             */
            public double inverseSurvivalProbability(double p) {
                checkProbability(p);
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

                double lowerBound = dist.getSupportLowerBound();
                if (p == 0) {
                    return lowerBound;
                }
                double upperBound = dist.getSupportUpperBound();
                if (q == 0) {
                    return upperBound;
                }

                final double mu = dist.getMean();
                final double sig = Math.sqrt(dist.getVariance());
                final boolean chebyshevApplies = Double.isFinite(mu) &&
                                                 isFiniteStrictlyPositive(sig);

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
                        if (dist.survivalProbability(upperBound) > q) {
                            return dist.getSupportUpperBound();
                        }
                    } else if (dist.cumulativeProbability(upperBound) < p) {
                        return dist.getSupportUpperBound();
                    }
                }
                if (lowerBound == -Double.MAX_VALUE) {
                    if (complement) {
                        if (dist.survivalProbability(lowerBound) < q) {
                            return dist.getSupportLowerBound();
                        }
                    } else if (dist.cumulativeProbability(lowerBound) > p) {
                        return dist.getSupportLowerBound();
                    }
                }

                final DoubleUnaryOperator fun = complement ?
                    arg -> dist.survivalProbability(arg) - q :
                    arg -> dist.cumulativeProbability(arg) - p;
                // Note the initial value is robust to overflow.
                // Do not use 0.5 * (lowerBound + upperBound).
                final double x = new BrentSolver(relativeAccuracy,
                                                 absoluteAccuracy,
                                                 SOLVER_FUNCTION_VALUE_ACCURACY)
                    .findRoot(fun,
                              lowerBound,
                              lowerBound + 0.5 * (upperBound - lowerBound),
                              upperBound);

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
                        while (dist.survivalProbability(lowerBound) < q) {
                            lowerBound *= 2;
                        }
                    } else {
                        while (dist.cumulativeProbability(lowerBound) >= p) {
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
                        while (dist.survivalProbability(upperBound) >= q) {
                            upperBound *= 2;
                        }
                    } else {
                        while (dist.cumulativeProbability(upperBound) < p) {
                            upperBound *= 2;
                        }
                    }
                    // Ensure finite
                    upperBound = Math.min(upperBound, Double.MAX_VALUE);
                }
                return upperBound;
            }
        }
    }

    /**
     * Benchmark the inverse function.
     *
     * @param data Test data.
     * @return the inverse function value
     */
    @Benchmark
    public double inverse(InverseData data) {
        return data.next();
    }
}
