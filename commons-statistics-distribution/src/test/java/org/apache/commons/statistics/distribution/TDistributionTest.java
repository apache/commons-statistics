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
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link TDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class TDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double df = (Double) parameters[0];
        return new TDistribution(df);
    }

    @Override
    protected double getAbsoluteTolerance() {
        return 1e-11;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0},
            {-0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"DegreesOfFreedom"};
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * @see <a href="https://issues.apache.orgg/bugzilla/show_bug.cgi?id=27243">
     *      Bug report that prompted this unit test.</a>
     */
    @Test
    void testCumulativeProbabilityAgainstStackOverflow() {
        final TDistribution td = new TDistribution(5.);
        Assertions.assertDoesNotThrow(() -> {
            td.cumulativeProbability(.1);
            td.cumulativeProbability(.01);
        });
    }

    @Test
    void testAdditionalMoments() {
        TDistribution dist;

        dist = new TDistribution(1.5);
        Assertions.assertEquals(0, dist.getMean());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getVariance());

        dist = new TDistribution(2.1);
        Assertions.assertEquals(0, dist.getMean());
        Assertions.assertEquals(2.1 / (2.1 - 2.0), dist.getVariance());

        dist = new TDistribution(12.1);
        Assertions.assertEquals(0, dist.getMean());
        Assertions.assertEquals(12.1 / (12.1 - 2.0), dist.getVariance());
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
        // Data points are not very exact so use a low tolerance.
        final DoubleTolerance tolerance = DoubleTolerances.absolute(1e-4);
        testSurvivalProbability(new TDistribution(2), args2, prob, tolerance);
        testSurvivalProbability(new TDistribution(10), args10, prob, tolerance);
        testSurvivalProbability(new TDistribution(30), args30, prob, tolerance);
        testSurvivalProbability(new TDistribution(100), args100, prob, tolerance);
    }

    // See https://issues.apache.org/jira/browse/STATISTICS-25
    @Test
    void testStatistics25() {
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
}
