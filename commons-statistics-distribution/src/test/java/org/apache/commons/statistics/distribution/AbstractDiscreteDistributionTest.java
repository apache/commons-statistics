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

import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for AbstractDiscreteDistribution default implementations.
 */
class AbstractDiscreteDistributionTest {
    private final DiceDistribution diceDistribution = new DiceDistribution();

    @Test
    void testInverseCumulativeProbabilityMethod() {
        // This must be consistent with its own cumulative probability
        final double[] p = IntStream.rangeClosed(1, 6).mapToDouble(diceDistribution::cumulativeProbability).toArray();
        Assertions.assertEquals(1.0, p[5], "Incorrect cumulative probability at upper bound");
        Assertions.assertEquals(1, diceDistribution.inverseCumulativeProbability(0));
        for (int i = 0; i < 6; i++) {
            final int x = i + 1;
            Assertions.assertEquals(x, diceDistribution.inverseCumulativeProbability(Math.nextDown(p[i])));
            Assertions.assertEquals(x, diceDistribution.inverseCumulativeProbability(p[i]));
            if (x < 6) {
                Assertions.assertEquals(x + 1, diceDistribution.inverseCumulativeProbability(Math.nextUp(p[i])));
            }
        }
    }

    @Test
    void testInverseSurvivalProbabilityMethod() {
        // This must be consistent with its own survival probability
        final double[] p = IntStream.rangeClosed(1, 6).mapToDouble(diceDistribution::survivalProbability).toArray();
        Assertions.assertEquals(0.0, p[5], "Incorrect survival probability at upper bound");
        Assertions.assertEquals(1, diceDistribution.inverseSurvivalProbability(1));
        for (int i = 0; i < 6; i++) {
            final int x = i + 1;
            Assertions.assertEquals(x, diceDistribution.inverseSurvivalProbability(Math.nextUp(p[i])));
            Assertions.assertEquals(x, diceDistribution.inverseSurvivalProbability(p[i]));
            if (x < 6) {
                Assertions.assertEquals(x + 1, diceDistribution.inverseSurvivalProbability(Math.nextDown(p[i])));
            }
        }
    }

    @Test
    void testCumulativeProbabilitiesSingleArguments() {
        final double p = diceDistribution.probability(1);
        for (int i = 1; i <= 6; i++) {
            Assertions.assertEquals(p * i,
                    diceDistribution.cumulativeProbability(i), Math.ulp(p * i));
        }
        Assertions.assertEquals(0.0, diceDistribution.cumulativeProbability(0));
        Assertions.assertEquals(1.0, diceDistribution.cumulativeProbability(7));
    }

    @Test
    void testProbabilitiesRangeArguments() {
        final double p = diceDistribution.probability(1);
        int lower = 0;
        int upper = 6;
        for (int i = 0; i < 2; i++) {
            // cum(0,6) = p(0 < X <= 6) = 1, cum(1,5) = 4/6, cum(2,4) = 2/6
            Assertions.assertEquals(1 - p * 2 * i,
                    diceDistribution.probability(lower, upper), 1E-12);
            lower++;
            upper--;
        }
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(p, diceDistribution.probability(i, i + 1), 1E-12);
        }
    }

    @Test
    void testInverseCumulativeProbabilityExtremes() {
        // Require a lower bound of MIN_VALUE and the cumulative probability
        // at that bound to be lower/higher than the argument cumulative probability.
        // Use a uniform distribution so that the default search is supported
        // for the inverse.
        final DiscreteDistribution dist = new AbstractDiscreteDistribution() {
            @Override
            public double probability(int x) {
                throw new AssertionError();
            }
            @Override
            public double cumulativeProbability(int x) {
                final int y = x - Integer.MIN_VALUE;
                if (y == 0) {
                    return 0.25;
                } else if (y == 1) {
                    return 0.5;
                } else if (y == 2) {
                    return 0.75;
                } else {
                    return 1.0;
                }
            }
            @Override
            public double getMean() {
                return 1.5 + Integer.MIN_VALUE;
            }
            @Override
            public double getVariance() {
                // n = upper - lower + 1, the variance is (n^2 - 1) / 12
                return 15.0 / 12;
            }
            @Override
            public int getSupportLowerBound() {
                return Integer.MIN_VALUE;
            }
            @Override
            public int getSupportUpperBound() {
                return Integer.MIN_VALUE + 3;
            }
        };
        Assertions.assertEquals(dist.getSupportLowerBound(), dist.inverseCumulativeProbability(0.0));
        Assertions.assertEquals(Integer.MIN_VALUE, dist.inverseCumulativeProbability(0.05));
        Assertions.assertEquals(Integer.MIN_VALUE + 1, dist.inverseCumulativeProbability(0.35));
        Assertions.assertEquals(Integer.MIN_VALUE + 2, dist.inverseCumulativeProbability(0.55));
        Assertions.assertEquals(dist.getSupportUpperBound(), dist.inverseCumulativeProbability(1.0));

        Assertions.assertEquals(dist.getSupportLowerBound(), dist.inverseSurvivalProbability(1.0));
        Assertions.assertEquals(Integer.MIN_VALUE, dist.inverseSurvivalProbability(0.95));
        Assertions.assertEquals(Integer.MIN_VALUE + 1, dist.inverseSurvivalProbability(0.65));
        Assertions.assertEquals(Integer.MIN_VALUE + 2, dist.inverseSurvivalProbability(0.45));
        Assertions.assertEquals(dist.getSupportUpperBound(), dist.inverseSurvivalProbability(0.0));
    }

    @Test
    void testInverseCumulativeProbabilityWithNoMean() {
        // A NaN mean will invalidate the Chebyshev inequality
        // to prevent bracketing
        final DiscreteDistribution dist = new AbstractDiscreteDistribution() {
            @Override
            public double probability(int x) {
                throw new AssertionError();
            }
            @Override
            public double cumulativeProbability(int x) {
                if (x == 0) {
                    return 0.25;
                } else if (x == 1) {
                    return 0.5;
                } else if (x == 2) {
                    return 0.75;
                } else {
                    return 1.0;
                }
            }
            @Override
            public double getMean() {
                return Double.NaN;
            }
            @Override
            public double getVariance() {
                return Double.NaN;
            }
            @Override
            public int getSupportLowerBound() {
                return 0;
            }
            @Override
            public int getSupportUpperBound() {
                return 3;
            }
        };
        Assertions.assertEquals(dist.getSupportLowerBound(), dist.inverseCumulativeProbability(0.0));
        Assertions.assertEquals(0, dist.inverseCumulativeProbability(0.05));
        Assertions.assertEquals(1, dist.inverseCumulativeProbability(0.35));
        Assertions.assertEquals(2, dist.inverseCumulativeProbability(0.55));
        Assertions.assertEquals(dist.getSupportUpperBound(), dist.inverseCumulativeProbability(1.0));

        Assertions.assertEquals(dist.getSupportLowerBound(), dist.inverseSurvivalProbability(1.0));
        Assertions.assertEquals(0, dist.inverseSurvivalProbability(0.95));
        Assertions.assertEquals(1, dist.inverseSurvivalProbability(0.65));
        Assertions.assertEquals(2, dist.inverseSurvivalProbability(0.45));
        Assertions.assertEquals(dist.getSupportUpperBound(), dist.inverseSurvivalProbability(0.0));
    }

    @Test
    void testInverseProbabilityWithNaN() {
        final DiscreteDistribution dist = new AbstractDiscreteDistribution() {
            @Override
            public double probability(int x) {
                return 0;
            }
            @Override
            public double cumulativeProbability(int x) {
                // NaN is not allowed
                return Double.NaN;
            }
            @Override
            public double getMean() {
                return 0;
            }
            @Override
            public double getVariance() {
                return 0;
            }
            @Override
            public int getSupportLowerBound() {
                return Integer.MIN_VALUE;
            }
            @Override
            public int getSupportUpperBound() {
                return Integer.MAX_VALUE;
            }
        };
        Assertions.assertThrows(IllegalStateException.class, () -> dist.inverseCumulativeProbability(0.5));
        Assertions.assertThrows(IllegalStateException.class, () -> dist.inverseSurvivalProbability(0.5));
    }

    /**
     * Simple distribution modeling a 6-sided die
     */
    class DiceDistribution extends AbstractDiscreteDistribution {
        private final double p = 1d / 6d;

        @Override
        public double probability(int x) {
            if (x < 1 || x > 6) {
                return 0;
            } else {
                return p;
            }
        }

        @Override
        public double cumulativeProbability(int x) {
            if (x < 1) {
                return 0;
            } else if (x >= 6) {
                return 1;
            } else {
                return x / 6d;
            }
        }

        @Override
        public double getMean() {
            return 3.5;
        }

        @Override
        public double getVariance() {
            return 70 / 24;  // E(X^2) - E(X)^2
        }

        @Override
        public int getSupportLowerBound() {
            return 1;
        }

        @Override
        public int getSupportUpperBound() {
            return 6;
        }
    }
}
