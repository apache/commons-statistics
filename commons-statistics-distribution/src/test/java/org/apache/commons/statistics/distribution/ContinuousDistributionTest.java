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
            public boolean isSupportConnected() {
                return false;
            }
            @Override
            public double inverseCumulativeProbability(double p) {
                return 0;
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
            // Always zero
            Assertions.assertEquals(0, dist.probability(x));

            // Return the log of the density
            Assertions.assertEquals(Math.log(x), dist.logDensity(x));
        }

        // Should throw for bad range
        Assertions.assertThrows(DistributionException.class, () -> dist.probability(0.5, 0.4));
        Assertions.assertEquals(high - low, dist.probability(0.5, 1.5));
    }
}
