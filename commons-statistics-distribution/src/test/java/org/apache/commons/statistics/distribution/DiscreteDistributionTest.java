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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test default implementations in the {@link DiscreteDistribution} interface.
 */
class DiscreteDistributionTest {
    /**
     * Test the default interface methods.
     */
    @Test
    void testDefaultMethods() {
        // Implement methods called by the defaults
        final DiscreteDistribution dist = new InvalidDiscreteDistribution() {
            @Override
            public double probability(int x) {
                return x;
            }

            @Override
            public double cumulativeProbability(int x) {
                // Return some different values to allow the survival probability to be
                // tested
                if (x < 0) {
                    return x < -5 ? 0.25 : 0.5;
                }
                return x > 5 ? 1.0 : 0.75;
            }
        };

        for (final int x : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, Integer.MAX_VALUE}) {
            // Return the log of the density
            Assertions.assertEquals(Math.log(x), dist.logProbability(x));
            // Must return 1 - CDF(x)
            Assertions.assertEquals(1.0 - dist.cumulativeProbability(x), dist.survivalProbability(x));
        }
    }

    /**
     * Test the default implementation of probability in a range.
     */
    @Test
    void testDefaultProbabilityRange() {
        // Return a marker probability. This should be unique for each input.
        final DiscreteDistribution dist = new InvalidDiscreteDistribution() {
            @Override
            public double probability(int x) {
                // Value >= 1
                return x + (1L << 31) + 1;
            }

            @Override
            public double cumulativeProbability(int x) {
                // Value in [-1, 1)
                return x * 0x1.0p-31;
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

        // Test default implementation
        final int[] values = {
            Integer.MIN_VALUE, Integer.MIN_VALUE + 1,
            -3, -2, -1, 0, 1, 2, 3,
            Integer.MAX_VALUE - 1, Integer.MAX_VALUE
        };
        for (final int x0 : values) {
            // probability(x, x) == 0.0
            Assertions.assertEquals(0.0, dist.probability(x0, x0));
            if (x0 < dist.getSupportUpperBound()) {
                // probability(x, x + 1) == probability(x + 1)
                Assertions.assertEquals(dist.probability(x0 + 1), dist.probability(x0, x0 + 1));
                for (final int x1 : values) {
                    if (x1 > x0 + 1) {
                        // probability(x0, x1) == cdf(x1) - cdf(x0)
                        Assertions.assertEquals(dist.cumulativeProbability(x1) - dist.cumulativeProbability(x0),
                                                dist.probability(x0, x1));
                    } else if (x1 < x0) {
                        Assertions.assertThrows(IllegalArgumentException.class, () -> dist.probability(x0, x1));
                    }
                }
            }
        }
    }

    /**
     * Test the default implementation of probability in a range calls the probability function
     * when x+1 or x-1 would overflow.
     */
    @Test
    void testDefaultProbabilityRangeOverflow() {
        // Return only the probability.
        // The CDF should not be called.
        final DiscreteDistribution dist = new InvalidDiscreteDistribution() {
            @Override
            public double probability(int x) {
                return x;
            }
        };

        // Extreme x at the integer limits
        final int min = Integer.MIN_VALUE;
        final int max = Integer.MAX_VALUE;
        Assertions.assertEquals(0.0, dist.probability(min, min));
        Assertions.assertEquals(0.0, dist.probability(max, max));
        Assertions.assertEquals(min + 1, dist.probability(min, min + 1));
        Assertions.assertEquals(max, dist.probability(max - 1, max));
    }

    /**
     * Invalid implementation of DiscreteDistribution that raise an exception for all methods.
     * Ensures the methods are not called.
     */
    private abstract static class InvalidDiscreteDistribution implements DiscreteDistribution {
        @Override
        public double probability(int x) {
            throw new AssertionError();
        }
        @Override
        public double cumulativeProbability(int x) {
            throw new AssertionError();
        }
        @Override
        public int inverseCumulativeProbability(double p) {
            throw new AssertionError();
        }
        @Override
        public double getMean() {
            throw new AssertionError();
        }
        @Override
        public double getVariance() {
            throw new AssertionError();
        }
        @Override
        public int getSupportLowerBound() {
            throw new AssertionError();
        }
        @Override
        public int getSupportUpperBound() {
            throw new AssertionError();
        }
        @Override
        public boolean isSupportConnected() {
            throw new AssertionError();
        }
        @Override
        public Sampler createSampler(UniformRandomProvider rng) {
            throw new AssertionError();
        }
    }
}
