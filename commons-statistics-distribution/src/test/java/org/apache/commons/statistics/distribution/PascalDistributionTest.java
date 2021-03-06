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
 * Test cases for PascalDistribution.
 * Extends DiscreteDistributionAbstractTest.  See class javadoc for
 * DiscreteDistributionAbstractTest for details.
 */
class PascalDistributionTest extends DiscreteDistributionAbstractTest {

    private static final double DEFAULT_TOLERANCE = 1e-7;

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(DEFAULT_TOLERANCE);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public PascalDistribution makeDistribution() {
        return new PascalDistribution(10, 0.70);
    }

    @Override
    public int[] makeDensityTestPoints() {
        return new int[] {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {0, 0.0282475249, 0.0847425747, 0.139825248255, 0.167790297906, 0.163595540458,
                             0.137420253985, 0.103065190489, 0.070673273478, 0.0450542118422, 0.0270325271053,
                             0.0154085404500, 0.0084046584273};
    }

    @Override
    public int[] makeCumulativeTestPoints() {
        return makeDensityTestPoints();
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0, 0.0282475249, 0.1129900996, 0.252815347855, 0.420605645761, 0.584201186219,
                             0.721621440204, 0.824686630693, 0.895359904171, 0.940414116013, 0.967446643119,
                             0.982855183569, 0.991259841996};
    }

    @Override
    public double[] makeInverseCumulativeTestPoints() {
        return new double[] {0.0, 0.001, 0.010, 0.025, 0.050, 0.100, 0.999,
                             0.990, 0.975, 0.950, 0.900, 1.0};
    }

    @Override
    public int[] makeInverseCumulativeTestValues() {
        return new int[] {0, 0, 0, 0, 1, 1, 14, 11, 10, 9, 8, Integer.MAX_VALUE};
    }

    //-------------------- Additional test cases -------------------------------

    /** Test degenerate case p = 0   */
    @Test
    void testDegenerate0() {
        setDistribution(new PascalDistribution(5, 0.0d));
        setCumulativeTestPoints(new int[] {-1, 0, 1, 5, 10 });
        setCumulativeTestValues(new double[] {0d, 0d, 0d, 0d, 0d});
        setDensityTestPoints(new int[] {-1, 0, 1, 10, 11});
        setDensityTestValues(new double[] {0d, 0d, 0d, 0d, 0d});
        setInverseCumulativeTestPoints(new double[] {0.1d, 0.5d});
        setInverseCumulativeTestValues(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE});
        verifyDensities();
        verifyCumulativeProbabilities();
        verifyInverseCumulativeProbabilities();
    }

    /** Test degenerate case p = 1   */
    @Test
    void testDegenerate1() {
        setDistribution(new PascalDistribution(5, 1.0d));
        setCumulativeTestPoints(new int[] {-1, 0, 1, 2, 5, 10 });
        setCumulativeTestValues(new double[] {0d, 1d, 1d, 1d, 1d, 1d});
        setDensityTestPoints(new int[] {-1, 0, 1, 2, 5, 10});
        setDensityTestValues(new double[] {0d, 1d, 0d, 0d, 0d, 0d});
        setInverseCumulativeTestPoints(new double[] {0.1d, 0.5d});
        setInverseCumulativeTestValues(new int[] {0, 0});
        verifyDensities();
        verifyCumulativeProbabilities();
        verifyInverseCumulativeProbabilities();
    }

    @Test
    void testParameterAccessors() {
        final PascalDistribution distribution = makeDistribution();
        Assertions.assertEquals(10, distribution.getNumberOfSuccesses());
        Assertions.assertEquals(0.7, distribution.getProbabilityOfSuccess());
    }

    @Test
    void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new PascalDistribution(0, 0.5));
    }

    @Test
    void testConstructorPrecondition2() {
        Assertions.assertThrows(DistributionException.class, () -> new PascalDistribution(3, -0.1));
    }

    @Test
    void testConstructorPrecondition3() {
        Assertions.assertThrows(DistributionException.class, () -> new PascalDistribution(3, 1.1));
    }

    @Test
    void testMoments() {
        final double tol = 1e-9;
        PascalDistribution dist;

        dist = new PascalDistribution(10, 0.5);
        Assertions.assertEquals((10d * 0.5d) / 0.5d, dist.getMean(), tol);
        Assertions.assertEquals((10d * 0.5d) / (0.5d * 0.5d), dist.getVariance(), tol);

        dist = new PascalDistribution(25, 0.7);
        Assertions.assertEquals((25d * 0.3d) / 0.7d, dist.getMean(), tol);
        Assertions.assertEquals((25d * 0.3d) / (0.7d * 0.7d), dist.getVariance(), tol);
    }
}
