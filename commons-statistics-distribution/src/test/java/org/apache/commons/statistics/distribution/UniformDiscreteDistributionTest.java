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
import org.apache.commons.numbers.core.Precision;

/**
 * Test cases for UniformDiscreteDistribution. See class javadoc for
 * {@link DiscreteDistributionAbstractTest} for further details.
 */
public class UniformDiscreteDistributionTest extends DiscreteDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    public void customSetUp() {
        setTolerance(1e-9);
    }

    //-------------- Implementations for abstract methods ----------------------

    /** Creates the default discrete distribution instance to use in tests. */
    @Override
    public DiscreteDistribution makeDistribution() {
        return new UniformDiscreteDistribution(-3, 5);
    }

    /** Creates the default probability density test input values. */
    @Override
    public int[] makeDensityTestPoints() {
        return new int[] {-4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6};
    }

    /** Creates the default probability density test expected values. */
    @Override
    public double[] makeDensityTestValues() {
        final double d = 1.0 / (5 - -3 + 1);
        return new double[] {0, d, d, d, d, d, d, d, d, d, 0};
    }

    /** Creates the default cumulative probability density test input values. */
    @Override
    public int[] makeCumulativeTestPoints() {
        return makeDensityTestPoints();
    }

    /** Creates the default cumulative probability density test expected values. */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0, 1 / 9.0, 2 / 9.0, 3 / 9.0, 4 / 9.0, 5 / 9.0,
                             6 / 9.0, 7 / 9.0, 8 / 9.0, 1, 1};
    }

    /** Creates the default inverse cumulative probability test input values */
    @Override
    public double[] makeInverseCumulativeTestPoints() {
        return new double[] {0, 0.001, 0.010, 0.025, 0.050, 0.100, 0.200,
                             0.5, 0.999, 0.990, 0.975, 0.950, 0.900, 1};
    }

    /** Creates the default inverse cumulative probability density test expected values */
    @Override
    public int[] makeInverseCumulativeTestValues() {
        return new int[] {-3, -3, -3, -3, -3, -3, -2, 1, 5, 5, 5, 5, 5, 5};
    }

    //-------------------- Additional test cases -------------------------------

    /** Test mean/variance. */
    @Test
    void testMoments() {
        UniformDiscreteDistribution dist;

        dist = new UniformDiscreteDistribution(0, 5);
        Assertions.assertEquals(2.5, dist.getMean());
        Assertions.assertEquals(35 / 12.0, dist.getVariance());

        dist = new UniformDiscreteDistribution(0, 1);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(3 / 12.0, dist.getVariance());
    }

    // MATH-1141
    @Test
    void testPreconditionUpperBoundInclusive1() {
        Assertions.assertThrows(DistributionException.class, () -> new UniformDiscreteDistribution(1, 0));
    }

    // MATH-1141
    @Test
    void testPreconditionUpperBoundInclusive2() {
        // Degenerate case is allowed.
        new UniformDiscreteDistribution(0, 0);
    }

    // MATH-1396
    @Test
    void testLargeRangeSubtractionOverflow() {
        final int hi = Integer.MAX_VALUE / 2 + 10;
        final UniformDiscreteDistribution dist = new UniformDiscreteDistribution(-hi, hi - 1);

        final double tol = Math.ulp(1d);
        Assertions.assertEquals(0.5 / hi, dist.probability(123456), tol);
        Assertions.assertEquals(0.5, dist.cumulativeProbability(-1), tol);

        Assertions.assertTrue(Precision.equals((Math.pow(2d * hi, 2) - 1) / 12, dist.getVariance(), 1));
    }

    // MATH-1396
    @Test
    void testLargeRangeAdditionOverflow() {
        final int hi = Integer.MAX_VALUE / 2 + 10;
        final UniformDiscreteDistribution dist = new UniformDiscreteDistribution(hi - 1, hi + 1);

        final double tol = Math.ulp(1d);
        Assertions.assertEquals(1d / 3d, dist.probability(hi), tol);
        Assertions.assertEquals(2d / 3d, dist.cumulativeProbability(hi), tol);

        Assertions.assertTrue(Precision.equals(hi, dist.getMean(), 1));
    }
}
