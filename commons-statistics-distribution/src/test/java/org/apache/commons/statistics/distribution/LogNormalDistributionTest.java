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
 * Test cases for {@link LogNormalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class LogNormalDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mu = (Double) parameters[0];
        final double sigma = (Double) parameters[1];
        return LogNormalDistribution.of(mu, sigma);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0},
            {0.0, -0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Mu", "Sigma"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testCumulativeProbabilityExtremes() {
        // Use a small shape parameter so that we can exceed 40 * shape
        testCumulativeProbability(LogNormalDistribution.of(1, 0.0001),
                                  new double[] {0.5, 10},
                                  new double[] {0, 1.0},
                                  DoubleTolerances.equals());
    }

    @Test
    void testSurvivalProbabilityExtremes() {
        // Use a small shape parameter so that we can exceed 40 * shape
        testSurvivalProbability(LogNormalDistribution.of(1, 0.0001),
                                new double[] {0.5, 10},
                                new double[] {1.0, 0.0},
                                DoubleTolerances.equals());
    }

    /**
     * Check to make sure top-coding of extreme values works correctly.
     * Verifies fixes for JIRA MATH-167, MATH-414
     */
    @Test
    void testExtremeValues() {
        final LogNormalDistribution dist = LogNormalDistribution.of(0, 1);
        for (int i = 0; i < 1e5; i++) { // make sure no convergence exception
            final double upperTail = dist.cumulativeProbability(i);
            if (i <= 72) { // make sure not top-coded
                Assertions.assertTrue(upperTail < 1.0d);
            } else { // make sure top coding not reversed
                Assertions.assertTrue(upperTail > 0.99999);
            }
        }

        Assertions.assertEquals(1, dist.cumulativeProbability(Double.MAX_VALUE));
        Assertions.assertEquals(0, dist.cumulativeProbability(-Double.MAX_VALUE));
        Assertions.assertEquals(1, dist.cumulativeProbability(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(0, dist.cumulativeProbability(Double.NEGATIVE_INFINITY));
    }

    @Test
    void testTinyVariance() {
        final LogNormalDistribution dist = LogNormalDistribution.of(0, 1e-9);
        final double t = dist.getVariance();
        Assertions.assertEquals(1e-18, t, 1e-20);
    }
}
