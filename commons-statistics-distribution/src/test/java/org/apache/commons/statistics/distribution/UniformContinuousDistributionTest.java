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
 * Test cases for UniformContinuousDistribution. See class javadoc for
 * {@link ContinuousDistributionAbstractTest} for further details.
 */
class UniformContinuousDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-4);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public UniformContinuousDistribution makeDistribution() {
        return new UniformContinuousDistribution(-0.5, 1.25);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {-0.5001, -0.5, -0.4999, -0.25, -0.0001, 0.0,
                             0.0001, 0.25, 1.0, 1.2499, 1.25, 1.2501};
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0.0, 0.0, 0.0001, 0.25 / 1.75, 0.4999 / 1.75,
                             0.5 / 1.75, 0.5001 / 1.75, 0.75 / 1.75, 1.5 / 1.75,
                             1.7499 / 1.75, 1.0, 1.0};
    }

    @Override
    public double[] makeDensityTestValues() {
        final double d = 1 / 1.75;
        return new double[] {0, d, d, d, d, d, d, d, d, d, d, 0};
    }

    //-------------------- Additional test cases -------------------------------

    /** Test lower bound getter. */
    @Test
    void testGetLowerBound() {
        final UniformContinuousDistribution distribution = makeDistribution();
        Assertions.assertEquals(-0.5, distribution.getSupportLowerBound());
    }

    /** Test upper bound getter. */
    @Test
    void testGetUpperBound() {
        final UniformContinuousDistribution distribution = makeDistribution();
        Assertions.assertEquals(1.25, distribution.getSupportUpperBound());
    }

    /** Test pre-condition for equal lower/upper bound. */
    @Test
    void testConstructorPreconditions1() {
        Assertions.assertThrows(DistributionException.class, () -> new UniformContinuousDistribution(0, 0));
    }

    /** Test pre-condition for lower bound larger than upper bound. */
    @Test
    void testConstructorPreconditions2() {
        Assertions.assertThrows(DistributionException.class, () -> new UniformContinuousDistribution(1, 0));
    }

    @Test
    void testMoments() {
        UniformContinuousDistribution dist;

        dist = new UniformContinuousDistribution(0, 1);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(1 / 12.0, dist.getVariance());

        dist = new UniformContinuousDistribution(-1.5, 0.6);
        Assertions.assertEquals(-0.45, dist.getMean());
        Assertions.assertEquals(0.3675, dist.getVariance());

        dist = new UniformContinuousDistribution(-0.5, 1.25);
        Assertions.assertEquals(0.375, dist.getMean());
        Assertions.assertEquals(0.2552083333333333, dist.getVariance());
    }

    /**
     * Check accuracy of analytical inverse CDF. Fails if a solver is used
     * with the default accuracy.
     */
    @Test
    void testInverseCumulativeDistribution() {
        final UniformContinuousDistribution dist = new UniformContinuousDistribution(0, 1e-9);

        Assertions.assertEquals(2.5e-10, dist.inverseCumulativeProbability(0.25));
    }
}
