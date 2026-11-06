/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * Test cases for {@link CauchyDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class CauchyDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double location = (Double) parameters[0];
        final double scale = (Double) parameters[1];
        return CauchyDistribution.of(location, scale);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0},
            {0.0, -0.1},
            {Double.NaN, 1.0},
            {0.0, Double.NaN}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Location", "Scale"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Test the quantile function in the tails of the distribution.
     *
     * <p>The standard computation \( x_0 + \gamma \tan(\pi (p - 1/2)) \) loses every
     * significant digit of a probability close to 0 or 1: the tangent argument is
     * rounded to \( \pm \pi / 2 \) and the result saturates at
     * \( \pm 1.633 \times 10^{16} \gamma \). The expected values are computed using
     * {@code scipy.stats.cauchy(loc=x0, scale=gamma)}.
     */
    @Test
    void testInverseProbabilityTails() {
        // Position and scale with location 0.5 and scale 2
        final CauchyDistribution dist = CauchyDistribution.of(0.5, 2);
        final double[] p = {
            0.5, 1e-1, 1e-5, 1e-10, 1e-20, 1e-100, 1e-300,
            1 - 1e-7, 1 - 1e-12, 1 - 1e-16, 1 - 2 * 0x1.0p-53, 0x1.0p-53, 0x1.0p-60
        };
        final double[] icdf = {
            0.5, -5.655367074350508, -63661.47721581417, -6366197723.175813,
            -6.366197723675814e19, -6.366197723675814e99, -6.366197723675814e299,
            6366198.22702649, 636633855804.0593, 5734161139222660.0, 2867080569611330.0,
            -5734161139222658.0, -7.339726258205004e17
        };
        for (int i = 0; i < p.length; i++) {
            final double pi = p[i];
            final double expected = icdf[i];
            final double actual = dist.inverseCumulativeProbability(pi);
            final double tolerance = Math.abs(expected) * 4e-16 + 1e-15;
            Assertions.assertEquals(expected, actual, tolerance, () -> "icdf(" + pi + ")");
            // The survival function is the reflection of the cumulative value in the location
            Assertions.assertEquals(2 * dist.getMedian() - expected, dist.inverseSurvivalProbability(pi),
                    tolerance, () -> "isf(" + pi + ")");
        }
    }

    /**
     * Test the quantile function in the tails is monotonic and antisymmetric.
     *
     * <p>The tail values can not be checked with a round trip through the cumulative
     * probability: \( 1/2 + \arctan(x) / \pi \) cancels by \( \pi / 2 \) so the relative
     * accuracy of {@code cdf(icdf(p))} is \( \mathrm{ulp}(\pi/2) / p \) and is zero for
     * the smallest probabilities.
     */
    @Test
    void testInverseProbabilityTailsAreMonotonic() {
        // The quantile function of the standard distribution is cot(pi p)
        final CauchyDistribution dist = CauchyDistribution.of(0, 1);
        double previous = dist.inverseCumulativeProbability(0.5);
        Assertions.assertEquals(0, previous, "icdf(0.5)");
        for (int i = -1; i >= -300; i--) {
            final double p = Math.pow(10, i);
            final double x = dist.inverseCumulativeProbability(p);
            // Strictly decreasing towards the lower tail and finite
            Assertions.assertTrue(x < previous, () -> "icdf(" + p + ")");
            Assertions.assertTrue(x < 0, () -> "icdf(" + p + ")");
            // The survival value is the reflection in the support centre
            Assertions.assertEquals(-x, dist.inverseSurvivalProbability(p), Math.abs(x) * 1e-15, () -> "isf(" + p + ")");
            previous = x;
        }
        // And towards the upper tail
        previous = dist.inverseCumulativeProbability(0.5);
        for (int i = 1; i <= 16; i++) {
            final double p = 1 - Math.pow(10, -i);
            final double x = dist.inverseCumulativeProbability(p);
            Assertions.assertTrue(x > 0 && x > previous, () -> "icdf(" + p + ")");
            previous = x;
        }
    }
}
