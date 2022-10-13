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

import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test default implementations in the {@link ContinuousDistribution} interface.
 */
class ContinuousDistributionTest {
    /**
     * Test the default interface methods.
     */
    @Test
    void testDefaultMethods() {
        final double high = 0.54;
        final double low = 0.313;

        final ContinuousDistribution dist = new ContinuousDistribution() {
            @Override
            public double inverseCumulativeProbability(double p) {
                // For the default inverseSurvivalProbability(double) method
                return 10 * p;
            }
            @Override
            public double getVariance() {
                return 0;
            }
            @Override
            public double getSupportUpperBound() {
                return 0;
            }
            @Override
            public double getSupportLowerBound() {
                return 0;
            }
            @Override
            public double getMean() {
                return 0;
            }
            @Override
            public double density(double x) {
                // Return input value for testing
                return x;
            }
            @Override
            public double cumulativeProbability(double x) {
                // For the default probability(double, double) method
                return x > 1 ? high : low;
            }
            @Override
            public Sampler createSampler(UniformRandomProvider rng) {
                return null;
            }
        };

        for (final double x : new double[] {Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 0, 1, 0.123}) {
            // Return the log of the density
            Assertions.assertEquals(Math.log(x), dist.logDensity(x));
        }

        // Should throw for bad range
        Assertions.assertThrows(DistributionException.class, () -> dist.probability(0.5, 0.4));
        Assertions.assertEquals(high - low, dist.probability(0.5, 1.5));
        Assertions.assertEquals(high - low, dist.probability(0.5, 1.5));
        for (final double p : new double[] {0.2, 0.5, 0.7}) {
            Assertions.assertEquals(dist.inverseCumulativeProbability(1 - p),
                                    dist.inverseSurvivalProbability(p));
        }
    }

    /**
     * Test the {@link ContinuousDistribution.Sampler} default stream methods.
     *
     * @param streamSize Number of values to generate.
     */
    @ParameterizedTest
    @ValueSource(longs = {0, 1, 13})
    void testSamplerStreamMethods(long streamSize) {
        final double seed = ThreadLocalRandom.current().nextDouble();
        final ContinuousDistribution.Sampler s1 = createIncrementSampler(seed);
        final ContinuousDistribution.Sampler s2 = createIncrementSampler(seed);
        final ContinuousDistribution.Sampler s3 = createIncrementSampler(seed);
        // Get the reference output from the sample() method
        final double[] x = new double[(int) streamSize];
        for (int i = 0; i < x.length; i++) {
            x[i] = s1.sample();
        }
        // Test default stream methods
        Assertions.assertArrayEquals(x, s2.samples().limit(streamSize).toArray(), "samples()");
        Assertions.assertArrayEquals(x, s3.samples(streamSize).toArray(), "samples(long)");
    }

    /**
     * Test the {@link ContinuousDistribution.Sampler} default stream method with a bad stream size.
     *
     * @param streamSize Number of values to generate.
     */
    @ParameterizedTest
    @ValueSource(longs = {-1, -6576237846822L})
    void testSamplerStreamMethodsThrow(long streamSize) {
        final ContinuousDistribution.Sampler s = createIncrementSampler(42);
        Assertions.assertThrows(IllegalArgumentException.class, () -> s.samples(streamSize));
    }

    /**
     * Creates the sampler with a given seed value.
     * Each successive output sample will increment this value by 1.
     *
     * @param seed Seed value.
     * @return the sampler
     */
    private static ContinuousDistribution.Sampler createIncrementSampler(double seed) {
        return new ContinuousDistribution.Sampler() {
            private double x = seed;

            @Override
            public double sample() {
                return x += 1;
            }
        };
    }
}
