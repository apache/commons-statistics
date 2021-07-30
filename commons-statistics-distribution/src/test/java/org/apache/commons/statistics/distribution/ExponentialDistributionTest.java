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

import org.apache.commons.numbers.core.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for ExponentialDistribution.
 * Extends ContinuousDistributionAbstractTest.  See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 */
class ExponentialDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-9);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public ExponentialDistribution makeDistribution() {
        return new ExponentialDistribution(5.0);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R version 2.9.2
        return new double[] {0.00500250166792, 0.0502516792675, 0.126589039921, 0.256466471938,
                             0.526802578289, 34.5387763949, 23.0258509299, 18.4443972706, 14.9786613678, 11.5129254650};
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0.001, 0.01, 0.025, 0.05, 0.1, 0.999,
                             0.990, 0.975, 0.950, 0.900};
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {0.1998, 0.198, 0.195, 0.19, 0.18, 0.000200000000000,
                             0.00200000000002, 0.00499999999997, 0.00999999999994, 0.0199999999999};
    }

    @Override
    public double[] makeCumulativePrecisionTestPoints() {
        return new double[] {1e-15, 4e-16, 9e-16};
    }

    @Override
    public double[] makeCumulativePrecisionTestValues() {
        // calculated via scipy, specifically expon.cdf(x/5).
        // WolframAlpha provided either too accurate or inaccurate values.
        return new double[] {2.0000000000000002e-16, 7.999999999999999e-17, 1.8000000000000002e-16};
    }

    @Override
    public double[] makeSurvivalPrecisionTestPoints() {
        return new double[] {183, 197};
    }

    @Override
    public double[] makeSurvivalPrecisionTestValues() {
        // These were created using WolframAlpha. NOTE lambda parameter is 1/mean
        return new double[] {1.2729811194234181e-16, 7.741006159285781e-18};
    }

    //------------ Additional tests -------------------------------------------

    @Test
    void testCumulativeProbabilityExtremes() {
        setCumulativeTestPoints(new double[] {-2, 0});
        setCumulativeTestValues(new double[] {0, 0});
        verifyCumulativeProbabilities();
    }

    @Test
    void testInverseCumulativeProbabilityExtremes() {
        setInverseCumulativeTestPoints(new double[] {0, 1});
        setInverseCumulativeTestValues(new double[] {0, Double.POSITIVE_INFINITY});
        verifyInverseCumulativeProbabilities();
    }

    @Test
    void testCumulativeProbability2() {
        final double actual = getDistribution().probability(0.25, 0.75);
        Assertions.assertEquals(0.0905214, actual, 10e-4);
    }

    @Test
    void testDensity() {
        final ExponentialDistribution d1 = new ExponentialDistribution(1);
        Assertions.assertTrue(Precision.equals(0.0, d1.density(-1e-9), 1));
        Assertions.assertTrue(Precision.equals(1.0, d1.density(0.0), 1));
        Assertions.assertTrue(Precision.equals(0.0, d1.density(1000.0), 1));
        Assertions.assertTrue(Precision.equals(Math.exp(-1), d1.density(1.0), 1));
        Assertions.assertTrue(Precision.equals(Math.exp(-2), d1.density(2.0), 1));

        final ExponentialDistribution d2 = new ExponentialDistribution(3);
        Assertions.assertTrue(Precision.equals(1 / 3.0, d2.density(0.0), 1));
        // computed using  print(dexp(1, rate=1/3), digits=10) in R 2.5
        Assertions.assertEquals(0.2388437702, d2.density(1.0), 1e-8);

        // computed using  print(dexp(2, rate=1/3), digits=10) in R 2.5
        Assertions.assertEquals(0.1711390397, d2.density(2.0), 1e-8);
    }

    @Test
    void testMeanAccessors() {
        final ExponentialDistribution dist = makeDistribution();
        Assertions.assertEquals(5d, dist.getMean());
    }

    @Test
    void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new ExponentialDistribution(0));
    }

    @Test
    void testMoments() {
        final double tol = 1e-9;
        ExponentialDistribution dist;

        dist = new ExponentialDistribution(11d);
        Assertions.assertEquals(11d, dist.getMean(), tol);
        Assertions.assertEquals(11d * 11d, dist.getVariance(), tol);

        dist = new ExponentialDistribution(10.5d);
        Assertions.assertEquals(10.5d, dist.getMean(), tol);
        Assertions.assertEquals(10.5d * 10.5d, dist.getVariance(), tol);
    }
}
