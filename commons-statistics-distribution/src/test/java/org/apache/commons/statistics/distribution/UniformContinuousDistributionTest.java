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

import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousUniformSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link UniformContinuousDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class UniformContinuousDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double lower = (Double) parameters[0];
        final double upper = (Double) parameters[1];
        return UniformContinuousDistribution.of(lower, upper);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0},
            {1.0, 0.0},
            // Range not finite
            {-Double.MAX_VALUE, Double.MAX_VALUE},
            {Double.NaN, 1.0},
            {0.0, Double.NaN},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"SupportLowerBound", "SupportUpperBound"};
    }

    @Override
    protected double getRelativeTolerance() {
        // Tolerance is 4.440892098500626E-16
        return 2 * RELATIVE_EPS;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(double lower, double upper, double mean, double variance) {
        final UniformContinuousDistribution dist = UniformContinuousDistribution.of(lower, upper);
        testMoments(dist, mean, variance, DoubleTolerances.equals());
    }

    static Stream<Arguments> testAdditionalMoments() {
        return Stream.of(
            Arguments.of(0, 1, 0.5, 1 / 12.0),
            Arguments.of(-1.5, 0.6, -0.45, 0.3675),
            Arguments.of(Double.MAX_VALUE / 2, Double.MAX_VALUE, Double.MAX_VALUE - Double.MAX_VALUE / 4, Double.POSITIVE_INFINITY)
        );
    }

    /**
     * Check accuracy of analytical inverse CDF. Fails if a solver is used
     * with the default accuracy.
     */
    @Test
    void testInverseCumulativeDistribution() {
        final double upper = 1e-9;
        final double tiny = 0x1.0p-100;

        final UniformContinuousDistribution dist = UniformContinuousDistribution.of(0, upper);
        Assertions.assertEquals(2.5e-10, dist.inverseCumulativeProbability(0.25));
        Assertions.assertEquals(tiny * upper, dist.inverseCumulativeProbability(tiny));

        final UniformContinuousDistribution dist2 = UniformContinuousDistribution.of(-upper, 0);
        // This is inexact
        Assertions.assertEquals(-7.5e-10, dist2.inverseCumulativeProbability(0.25), Math.ulp(-7.5e-10));
        Assertions.assertEquals(-upper + tiny * upper, dist2.inverseCumulativeProbability(tiny));
    }

    /**
     * Test the probability in a range uses the exact computation of
     * {@code (x1 - x0) / (upper - lower)} assuming x0 and x1 are within [lower, upper].
     * This test will fail if the distribution uses the default implementation in
     * {@link AbstractContinuousDistribution}.
     */
    @ParameterizedTest
    @CsvSource(value = {
        "-1.6358421681, -0.566237287234",
        "-10.23678, 234.234",
        "234.2342, 54322342.13",
    })
    void testProbabilityRange(double lower, double upper) {
        final UniformContinuousDistribution dist = UniformContinuousDistribution.of(lower, upper);
        final double r = upper - lower;
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final ContinuousSampler sampler = ContinuousUniformSampler.of(rng, lower, upper);
        for (int i = 0; i < 100; i++) {
            double x0 = sampler.sample();
            double x1 = sampler.sample();
            if (x1 < x0) {
                final double tmp = x0;
                x1 = x0;
                x0 = tmp;
            }
            Assertions.assertEquals((x1 - x0) / r, dist.probability(x0, x1));
        }
    }

    @Test
    void testProbabilityRangeEdgeCases() {
        final UniformContinuousDistribution dist = UniformContinuousDistribution.of(0, 11);

        Assertions.assertThrows(DistributionException.class, () -> dist.probability(4, 3));

        // x0 >= upper
        Assertions.assertEquals(0, dist.probability(11, 16));
        Assertions.assertEquals(0, dist.probability(15, 16));
        // x1 < lower
        Assertions.assertEquals(0, dist.probability(-3, -1));

        // x0 == x1
        Assertions.assertEquals(0, dist.probability(4.12, 4.12));
        Assertions.assertEquals(0, dist.probability(5.68, 5.68));

        // x1 > upper
        Assertions.assertEquals(1, dist.probability(0, 16));
        Assertions.assertEquals((11 - 3.45) / 11, dist.probability(3.45, 16));
        Assertions.assertEquals((11 - 4.89) / 11, dist.probability(4.89, 16));
        Assertions.assertEquals(0, dist.probability(11, 16));

        // x0 < lower
        Assertions.assertEquals(2.0 / 11, dist.probability(-2, 2));
        Assertions.assertEquals(3.0 / 11, dist.probability(-2, 3));
        Assertions.assertEquals(4.0 / 11, dist.probability(-2, 4));
        Assertions.assertEquals(1.0, dist.probability(-2, 11));

        // x1 > upper && x0 < lower
        Assertions.assertEquals(1, dist.probability(-2, 16));
    }
}
