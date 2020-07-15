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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for ConstantContinuousDistribution.
 */
class ConstantContinuousDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(0);
    }

    //-------------- Implementations for abstract methods ----------------------

    /** Creates the default uniform real distribution instance to use in tests. */
    @Override
    public ConstantContinuousDistribution makeDistribution() {
        return new ConstantContinuousDistribution(1);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {0, 0.5, 1};
    }

    /** Creates the default cumulative probability distribution test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0, 0, 1};
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] {0, 0, 1};
    }

    /** Override default test, verifying that inverse cum is constant */
    @Override
    @Test
    void testInverseCumulativeProbabilities() {
        final ContinuousDistribution dist = getDistribution();
        for (final double x : getCumulativeTestValues()) {
            Assertions.assertEquals(1, dist.inverseCumulativeProbability(x));
        }
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testMoments() {
        ConstantContinuousDistribution dist;

        dist = new ConstantContinuousDistribution(-1);
        Assertions.assertEquals(-1, dist.getMean());
        Assertions.assertEquals(0, dist.getVariance());
    }

    @Test
    @Override
    void testSampler() {
        final double value = 12.345;
        final ContinuousDistribution.Sampler sampler = new ConstantContinuousDistribution(value).createSampler(null);
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(value, sampler.sample());
        }
    }
}
