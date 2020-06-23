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
public class DiscreteDistributionTest {
    /**
     * Test the default interface methods
     */
    @Test
    public void testDefaultMethods() {
        final DiscreteDistribution dist = new DiscreteDistribution() {
            @Override
            public double probability(int x) {
                return x;
            }
            @Override
            public double probability(int x0, int x1) {
                return 0;
            }
            @Override
            public double cumulativeProbability(int x) {
                return 0;
            }
            @Override
            public int inverseCumulativeProbability(double p) {
                return 0;
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
                return 0;
            }
            @Override
            public int getSupportUpperBound() {
                return 0;
            }
            @Override
            public boolean isSupportConnected() {
                return false;
            }
            @Override
            public Sampler createSampler(UniformRandomProvider rng) {
                return null;
            }
        };

        for (final int x : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, Integer.MAX_VALUE}) {
            // Return the log of the density
            Assertions.assertEquals(Math.log(x), dist.logProbability(x));
        }
    }
}
