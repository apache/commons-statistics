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
public class UniformContinuousDistributionTest extends ContinuousDistributionAbstractTest {

    // --------------------- Override tolerance  --------------

    @BeforeEach
    public void customSetUp() {
        setTolerance(1e-4);
    }

    //-------------- Implementations for abstract methods -----------------------

    /** Creates the default uniform real distribution instance to use in tests. */
    @Override
    public UniformContinuousDistribution makeDistribution() {
        return new UniformContinuousDistribution(-0.5, 1.25);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {-0.5001, -0.5, -0.4999, -0.25, -0.0001, 0.0,
                             0.0001, 0.25, 1.0, 1.2499, 1.25, 1.2501};
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0.0, 0.0, 0.0001, 0.25 / 1.75, 0.4999 / 1.75,
                             0.5 / 1.75, 0.5001 / 1.75, 0.75 / 1.75, 1.5 / 1.75,
                             1.7499 / 1.75, 1.0, 1.0};
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        double d = 1 / 1.75;
        return new double[] {0, d, d, d, d, d, d, d, d, d, d, 0};
    }

    //---------------------------- Additional test cases -------------------------

    /** Test lower bound getter. */
    @Test
    public void testGetLowerBound() {
        UniformContinuousDistribution distribution = makeDistribution();
        Assertions.assertEquals(-0.5, distribution.getSupportLowerBound(), 0);
    }

    /** Test upper bound getter. */
    @Test
    public void testGetUpperBound() {
        UniformContinuousDistribution distribution = makeDistribution();
        Assertions.assertEquals(1.25, distribution.getSupportUpperBound(), 0);
    }

    /** Test pre-condition for equal lower/upper bound. */
    @Test
    public void testConstructorPreconditions1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UniformContinuousDistribution(0, 0));
    }

    /** Test pre-condition for lower bound larger than upper bound. */
    @Test
    public void testConstructorPreconditions2() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UniformContinuousDistribution(1, 0));
    }

    @Test
    public void testMoments() {
        UniformContinuousDistribution dist;

        dist = new UniformContinuousDistribution(0, 1);
        Assertions.assertEquals(0.5, dist.getMean(), 0);
        Assertions.assertEquals(1 / 12.0, dist.getVariance(), 0);

        dist = new UniformContinuousDistribution(-1.5, 0.6);
        Assertions.assertEquals(-0.45, dist.getMean(), 0);
        Assertions.assertEquals(0.3675, dist.getVariance(), 0);

        dist = new UniformContinuousDistribution(-0.5, 1.25);
        Assertions.assertEquals(0.375, dist.getMean(), 0);
        Assertions.assertEquals(0.2552083333333333, dist.getVariance(), 0);
    }

    /**
     * Check accuracy of analytical inverse CDF. Fails if a solver is used
     * with the default accuracy.
     */
    @Test
    public void testInverseCumulativeDistribution() {
        UniformContinuousDistribution dist = new UniformContinuousDistribution(0, 1e-9);

        Assertions.assertEquals(2.5e-10, dist.inverseCumulativeProbability(0.25), 0);
    }
}
