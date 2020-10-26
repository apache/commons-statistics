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
 * Test cases for TDistribution.
 * Extends ContinuousDistributionAbstractTest.  See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 */
class TDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-9);
    }

    //-------------- Implementations for abstract methods ----------------------

    /** Creates the default continuous distribution instance to use in tests. */
    @Override
    public TDistribution makeDistribution() {
        return new TDistribution(5.0);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R version 2.9.2
        return new double[] {-5.89342953136, -3.36492999891, -2.57058183564, -2.01504837333, -1.47588404882,
                             5.89342953136, 3.36492999891, 2.57058183564, 2.01504837333, 1.47588404882};
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0.001, 0.01, 0.025, 0.05, 0.1, 0.999,
                             0.990, 0.975, 0.950, 0.900};
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] {0.000756494565517, 0.0109109752919, 0.0303377878006, 0.0637967988952, 0.128289492005,
                             0.000756494565517, 0.0109109752919, 0.0303377878006, 0.0637967988952, 0.128289492005};
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * @see <a href="https://issues.apache.orgg/bugzilla/show_bug.cgi?id=27243">
     *      Bug report that prompted this unit test.</a>
     */
    @Test
    void testCumulativeProbabilityAgainstStackOverflow() {
        Assertions.assertDoesNotThrow(() -> {
            final TDistribution td = new TDistribution(5.);
            td.cumulativeProbability(.1);
            td.cumulativeProbability(.01);
        });
    }

    @Test
    void testSmallDf() {
        setDistribution(new TDistribution(1d));
        // quantiles computed using R version 2.9.2
        setCumulativeTestPoints(new double[] {-318.308838986, -31.8205159538, -12.7062047362,
                                              -6.31375151468, -3.07768353718, 318.308838986,
                                              31.8205159538, 12.7062047362, 6.31375151468,
                                              3.07768353718});
        setDensityTestValues(new double[] {3.14158231817e-06, 0.000314055924703, 0.00195946145194,
                                           0.00778959736375, 0.0303958893917, 3.14158231817e-06,
                                           0.000314055924703, 0.00195946145194, 0.00778959736375,
                                           0.0303958893917});
        setInverseCumulativeTestValues(getCumulativeTestPoints());
        verifyCumulativeProbabilities();
        verifyInverseCumulativeProbabilities();
        verifyDensities();
    }

    @Test
    void testInverseCumulativeProbabilityExtremes() {
        setInverseCumulativeTestPoints(new double[] {0, 1});
        setInverseCumulativeTestValues(new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY});
        verifyInverseCumulativeProbabilities();
    }

    @Test
    void testCumulativeProbablilityExtremes() {
        TDistribution dist;
        for (int i = 1; i < 11; i++) {
            dist = new TDistribution(i * 5);
            Assertions.assertEquals(1,
                                dist.cumulativeProbability(Double.POSITIVE_INFINITY), Double.MIN_VALUE);
            Assertions.assertEquals(0,
                                dist.cumulativeProbability(Double.NEGATIVE_INFINITY), Double.MIN_VALUE);
        }
    }

    @Test
    void testParameterAccessors() {
        final TDistribution dist = makeDistribution();
        Assertions.assertEquals(5d, dist.getDegreesOfFreedom());
    }

    @Test
    void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new TDistribution(0));
    }

    @Test
    void testMoments() {
        final double tol = 1e-9;
        TDistribution dist;

        dist = new TDistribution(1);
        Assertions.assertTrue(Double.isNaN(dist.getMean()));
        Assertions.assertTrue(Double.isNaN(dist.getVariance()));

        dist = new TDistribution(1.5);
        Assertions.assertEquals(0, dist.getMean(), tol);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getVariance());

        dist = new TDistribution(5);
        Assertions.assertEquals(0, dist.getMean(), tol);
        Assertions.assertEquals(5d / (5d - 2d), dist.getVariance(), tol);
    }

    /*
     * Adding this test to benchmark against tables published by NIST
     * http://itl.nist.gov/div898/handbook/eda/section3/eda3672.htm
     * Have chosen tabulated results for degrees of freedom 2,10,30,100
     * Have chosen problevels from 0.10 to 0.001
     */
    @Test
    void nistData() {
        final double[] prob = new double[]{0.10, 0.05, 0.025, 0.01, 0.005, 0.001};
        final double[] args2 = new double[]{1.886, 2.920, 4.303, 6.965, 9.925, 22.327};
        final double[] args10 = new double[]{1.372, 1.812, 2.228, 2.764, 3.169, 4.143};
        final double[] args30 = new double[]{1.310, 1.697, 2.042, 2.457, 2.750, 3.385};
        final double[] args100 = new double[]{1.290, 1.660, 1.984, 2.364, 2.626, 3.174};
        TestUtils.assertEquals(prob, makeNistResults(args2, 2), 1.0e-4);
        TestUtils.assertEquals(prob, makeNistResults(args10, 10), 1.0e-4);
        TestUtils.assertEquals(prob, makeNistResults(args30, 30), 1.0e-4);
        TestUtils.assertEquals(prob, makeNistResults(args100, 100), 1.0e-4);
        return;
    }

    // See https://issues.apache.org/jira/browse/STATISTICS-25
    @Test
    public void testStatistics25() {
        final double[] df = {1, 10, 1e2, 1e3, 1e4, 1e5,
                             1e6, 2e6, 2.98e6, 2.99e6, 3e6, 4e6,
                             1e7, 1e8, 1e9, 1e10};
        // Generated from Python.
        final double[] expected = {0.507956089912,
                                   0.509726595102,
                                   0.509947608093,
                                   0.509970024339,
                                   0.509972268782,
                                   0.509972493254,
                                   0.509972515701,
                                   0.509972516948,
                                   0.509972517358,
                                   0.509972517361,
                                   0.509972517364,
                                   0.509972517572,
                                   0.509972517946,
                                   0.50997251817,
                                   0.509972518193,
                                   0.509972518195};

        final double c = 0.025;
        final double tol = 1e-9;
        for (int i = 0; i < df.length; i++) {
            final TDistribution dist = new TDistribution(df[i]);
            final double x = dist.cumulativeProbability(c);
            Assertions.assertEquals(expected[i], x, tol);
        }
    }

    private double[] makeNistResults(double[] args, int df) {
        final TDistribution td =  new TDistribution(df);
        final double[] res  = new double[args.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = 1.0 - td.cumulativeProbability(args[i]);
        }
        return res;
    }
}
