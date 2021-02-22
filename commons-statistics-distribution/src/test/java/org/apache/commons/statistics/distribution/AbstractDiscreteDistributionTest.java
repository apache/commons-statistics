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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for AbstractDiscreteDistribution default implementations.
 */
class AbstractDiscreteDistributionTest {
    private final DiceDistribution diceDistribution = new DiceDistribution();
    private final double p = diceDistribution.probability(1);

    @Test
    void testInverseCumulativeProbabilityMethod() {
        final double precision = 0.000000000000001;
        Assertions.assertEquals(1, diceDistribution.inverseCumulativeProbability(0));
        Assertions.assertEquals(1, diceDistribution.inverseCumulativeProbability((1d - Double.MIN_VALUE) / 6d));
        Assertions.assertEquals(2, diceDistribution.inverseCumulativeProbability((1d + precision) / 6d));
        Assertions.assertEquals(2, diceDistribution.inverseCumulativeProbability((2d - Double.MIN_VALUE) / 6d));
        Assertions.assertEquals(3, diceDistribution.inverseCumulativeProbability((2d + precision) / 6d));
        Assertions.assertEquals(3, diceDistribution.inverseCumulativeProbability((3d - Double.MIN_VALUE) / 6d));
        Assertions.assertEquals(4, diceDistribution.inverseCumulativeProbability((3d + precision) / 6d));
        Assertions.assertEquals(4, diceDistribution.inverseCumulativeProbability((4d - Double.MIN_VALUE) / 6d));
        Assertions.assertEquals(5, diceDistribution.inverseCumulativeProbability((4d + precision) / 6d));
        Assertions.assertEquals(5, diceDistribution.inverseCumulativeProbability((5d - precision) / 6d)); //Can't use Double.MIN
        Assertions.assertEquals(6, diceDistribution.inverseCumulativeProbability((5d + precision) / 6d));
        Assertions.assertEquals(6, diceDistribution.inverseCumulativeProbability((6d - precision) / 6d)); //Can't use Double.MIN
        Assertions.assertEquals(6, diceDistribution.inverseCumulativeProbability(1d));
    }

    @Test
    void testCumulativeProbabilitiesSingleArguments() {
        for (int i = 1; i < 7; i++) {
            Assertions.assertEquals(p * i,
                    diceDistribution.cumulativeProbability(i), Double.MIN_VALUE);
        }
        Assertions.assertEquals(0.0,
                diceDistribution.cumulativeProbability(0), Double.MIN_VALUE);
        Assertions.assertEquals(1.0,
                diceDistribution.cumulativeProbability(7), Double.MIN_VALUE);
    }

    @Test
    void testProbabilitiesRangeArguments() {
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
        final DiscreteDistribution dist = new AbstractDiscreteDistribution() {
            @Override
            public double probability(int x) {
                return 0;
            }
            @Override
            public double cumulativeProbability(int x) {
                return x == Integer.MIN_VALUE ? 0.1 : 1.0;
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
                return 42;
            }
            @Override
            public boolean isSupportConnected() {
                return false;
            }
        };
        Assertions.assertEquals(Integer.MIN_VALUE, dist.inverseCumulativeProbability(0.05));
        Assertions.assertEquals(dist.getSupportUpperBound(), dist.inverseCumulativeProbability(1.0));
    }

    @Test
    void testInverseCumulativeProbabilityWithNaN() {
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
            @Override
            public boolean isSupportConnected() {
                return false;
            }
        };
        Assertions.assertThrows(IllegalStateException.class, () -> dist.inverseCumulativeProbability(0.5));
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
                return p * x;
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

        @Override
        public final boolean isSupportConnected() {
            return true;
        }
    }
}
