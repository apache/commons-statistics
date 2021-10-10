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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link NormalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class NormalDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mean = (Double) parameters[0];
        final double sd = (Double) parameters[1];
        return NormalDistribution.of(mean, sd);
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
        return new String[] {"Mean", "StandardDeviation"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testCumulativeProbabilityExtremes() {
        final NormalDistribution dist = NormalDistribution.of(0, 1);
        testCumulativeProbability(dist,
                                  new double[] {-Double.MAX_VALUE, Double.MAX_VALUE,
                                                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY},
                                  new double[] {0, 1, 0, 1},
                                  DoubleTolerances.equals());
    }

    /**
     * Check to make sure top-coding of extreme values works correctly.
     * Verifies fixes for JIRA MATH-167, MATH-414
     */
    @Test
    void testLowerTail() {
        final NormalDistribution dist = NormalDistribution.of(0, 1);
        for (int i = 0; i < 100; i++) { // make sure no convergence exception
            final double cdf = dist.cumulativeProbability(-i);
            if (i < 39) { // make sure not top-coded
                Assertions.assertTrue(cdf > 0);
            } else { // make sure top coding not reversed
                Assertions.assertEquals(0, cdf);
            }
            final double sf = dist.survivalProbability(-i);
            if (i < 9) { // make sure not top-coded
                Assertions.assertTrue(sf < 1);
            } else { // make sure top coding not reversed
                Assertions.assertEquals(1, sf);
            }
        }
    }

    /**
     * Check to make sure top-coding of extreme values works correctly.
     * Verifies fixes for JIRA MATH-167, MATH-414
     */
    @Test
    void testUpperTail() {
        final NormalDistribution dist = NormalDistribution.of(0, 1);
        for (int i = 0; i < 100; i++) { // make sure no convergence exception
            final double cdf = dist.cumulativeProbability(i);
            if (i < 9) { // make sure not top-coded
                Assertions.assertTrue(cdf < 1);
            } else { // make sure top coding not reversed
                Assertions.assertEquals(1, cdf);
            }
            // Test survival probability
            final double sf = dist.survivalProbability(i);
            if (i < 39) { // make sure not top-coded
                Assertions.assertTrue(sf > 0);
            } else { // make sure top coding not reversed
                Assertions.assertEquals(0, sf);
            }
        }
    }

    @Test
    void testMath1257() {
        final ContinuousDistribution dist = NormalDistribution.of(0, 1);
        final double x = -10;
        final double expected = 7.61985e-24;
        final double v = dist.cumulativeProbability(x);
        Assertions.assertEquals(1.0, v / expected, 1e-5);
    }

    @Test
    void testMath280() {
        final NormalDistribution dist = NormalDistribution.of(0, 1);
        final DoubleTolerance tol = createTolerance();
        double result = dist.inverseCumulativeProbability(0.9986501019683698);
        TestUtils.assertEquals(3.0, result, tol);
        result = dist.inverseCumulativeProbability(0.841344746068543);
        TestUtils.assertEquals(1.0, result, tol);
        result = dist.inverseCumulativeProbability(0.9999683287581673);
        TestUtils.assertEquals(4.0, result, tol);
        result = dist.inverseCumulativeProbability(0.9772498680518209);
        TestUtils.assertEquals(2.0, result, tol);
    }

    /**
     * Test the inverse CDF. This is currently limited by the accuracy
     * of {@code InverseErfc}. Although the CDF can be computed
     * to x down to -38 (CDF around 2.8854e-316) the inverse of the probability
     * fails when x is around 9 and the CDF is approximately 1.12e-19.
     */
    @Test
    @Disabled("Limited by accuracy of InverseErfc")
    void testInverseCDF() {
        final NormalDistribution dist = NormalDistribution.of(0, 1);
        // Get smaller and the CDF should reduce.
        double x = 0;
        for (;;) {
            x -= 1;
            final double cdf = dist.cumulativeProbability(x);
            if (cdf == 0) {
                break;
            }
            final double x0 = dist.inverseCumulativeProbability(cdf);
            // Must be close
            Assertions.assertEquals(x, x0, 1.0, () -> "CDF = " + cdf);
        }
    }
}
