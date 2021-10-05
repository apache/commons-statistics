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
 * Test cases for {@link PoissonDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class PoissonDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final double mean = (Double) parameters[0];
        return new PoissonDistribution(mean);
    }

    @Override
    protected double getTolerance() {
        return 1e-12;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0},
            {-0.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Mean"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testLargeMeanCumulativeProbability() {
        double mean = 1.0;
        while (mean <= 10000000.0) {
            final PoissonDistribution dist = new PoissonDistribution(mean);

            double x = mean * 2.0;
            final double dx = x / 10.0;
            double p = Double.NaN;
            final double sigma = Math.sqrt(mean);
            while (x >= 0) {
                try {
                    p = dist.cumulativeProbability((int) x);
                    Assertions.assertFalse(Double.isNaN(p), "NaN cumulative probability");
                    if (x > mean - 2 * sigma) {
                        Assertions.assertTrue(p > 0, "Zero cumulative probaility");
                    }
                } catch (final AssertionError ex) {
                    Assertions.fail("mean of " + mean + " and x of " + x + " caused " + ex.getMessage());
                }
                x -= dx;
            }

            mean *= 10.0;
        }
    }

    /**
     * JIRA: MATH-282
     */
    @Test
    void testCumulativeProbabilitySpecial() {
        PoissonDistribution dist;
        dist = new PoissonDistribution(9120);
        checkProbability(dist, 9075);
        checkProbability(dist, 9102);
        dist = new PoissonDistribution(5058);
        checkProbability(dist, 5044);
        dist = new PoissonDistribution(6986);
        checkProbability(dist, 6950);
    }

    private void checkProbability(PoissonDistribution dist, int x) {
        final double p = dist.cumulativeProbability(x);
        Assertions.assertFalse(Double.isNaN(p), () -> "NaN cumulative probability returned for mean = " +
                dist.getMean() + " x = " + x);
        Assertions.assertTrue(p > 0, () -> "Zero cum probability returned for mean = " +
                dist.getMean() + " x = " + x);
    }

    @Test
    void testLargeMeanInverseCumulativeProbability() {
        double mean = 1.0;
        while (mean <= 100000.0) { // Extended test value: 1E7.  Reduced to limit run time.
            final PoissonDistribution dist = new PoissonDistribution(mean);
            double p = 0.1;
            final double dp = p;
            while (p < .99) {
                try {
                    final int ret = dist.inverseCumulativeProbability(p);
                    // Verify that returned value satisties definition
                    Assertions.assertTrue(p <= dist.cumulativeProbability(ret));
                    Assertions.assertTrue(p > dist.cumulativeProbability(ret - 1));
                } catch (final AssertionError ex) {
                    Assertions.fail("mean of " + mean + " and p of " + p + " caused " + ex.getMessage());
                }
                p += dp;
            }
            mean *= 10.0;
        }
    }

    @Test
    void testLargeMeanHighPrecisionCumulativeProbabilities() {
        // computed using R version 3.4.4
        testCumulativeProbabilityHighPrecision(
                new PoissonDistribution(100),
                new int[] {28, 25},
                new double[] {1.6858675763053070496e-17, 3.184075559619425735e-19},
                createHighPrecisionTolerance());
    }
}
