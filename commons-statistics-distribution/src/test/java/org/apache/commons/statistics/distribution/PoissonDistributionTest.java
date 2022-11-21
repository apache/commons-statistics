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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for {@link PoissonDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class PoissonDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final double mean = (Double) parameters[0];
        return PoissonDistribution.of(mean);
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

    @Override
    protected double getRelativeTolerance() {
        return 1e-14;
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testLargeMeanCumulativeProbability() {
        double mean = 1.0;
        while (mean <= 10000000.0) {
            final PoissonDistribution dist = PoissonDistribution.of(mean);

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
    @ParameterizedTest
    @CsvSource({
        "9120, 9075",
        "9120, 9102",
        "5058, 5044",
        "6986, 6950",
    })
    void testCumulativeProbabilitySpecial(double mean, int x) {
        final PoissonDistribution dist = PoissonDistribution.of(mean);
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
            final PoissonDistribution dist = PoissonDistribution.of(mean);
            double p = 0.1;
            final double dp = p;
            while (p < .99) {
                try {
                    final int ret = dist.inverseCumulativeProbability(p);
                    // Verify that returned value satisfies definition
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
    void testAdditionalCumulativeProbabilityHighPrecision() {
        // computed using R version 3.4.4
        testCumulativeProbabilityHighPrecision(
                PoissonDistribution.of(100),
                new int[] {28, 25},
                new double[] {1.6858675763053070496e-17, 3.184075559619425735e-19},
                createHighPrecisionTolerance());
    }

    /**
     * Test creation of a sampler with a large mean that computes valid probabilities.
     */
    @Test
    void testCreateSamplerWithLargeMean() {
        final int mean = Integer.MAX_VALUE;
        final PoissonDistribution dist = PoissonDistribution.of(mean);
        // The mean is roughly the median for large mean
        Assertions.assertEquals(0.5, dist.cumulativeProbability(mean), 0.05);
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        dist.createSampler(rng)
            .samples(50)
            .forEach(i -> Assertions.assertTrue(i >= 0, () -> "Bad sample: " + i));
    }
}
