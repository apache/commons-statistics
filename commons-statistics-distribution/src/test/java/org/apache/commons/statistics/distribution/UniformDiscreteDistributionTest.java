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

import java.util.stream.Stream;
import org.apache.commons.math3.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link UniformDiscreteDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class UniformDiscreteDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int lower = (Integer) parameters[0];
        final int upper = (Integer) parameters[1];
        return UniformDiscreteDistribution.of(lower, upper);
    }


    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            // MATH-1141
            {1, 0},
            {3, 2},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"SupportLowerBound", "SupportUpperBound"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 1e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(int lower, int upper, double mean, double variance) {
        final UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(lower, upper);
        testMoments(dist, mean, variance, DoubleTolerances.equals());
    }

    static Stream<Arguments> testAdditionalMoments() {
        return Stream.of(
            Arguments.of(0, 5, 2.5, 35 / 12.0),
            Arguments.of(0, 1, 0.5, 3 / 12.0)
        );
    }

    // MATH-1396
    @Test
    void testLargeRangeSubtractionOverflow() {
        final int hi = Integer.MAX_VALUE / 2 + 10;
        final int lower = -hi;
        final int upper = hi - 1;

        // range = upper - lower + 1 would overflow
        Assertions.assertTrue(upper - lower < 0);

        final UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(lower, upper);

        Assertions.assertEquals(0.5 / hi, dist.probability(123456));
        Assertions.assertEquals(0.5, dist.cumulativeProbability(-1));

        Assertions.assertEquals((Math.pow(2d * hi, 2) - 1) / 12, dist.getVariance());
    }

    // MATH-1396
    @Test
    void testLargeRangeAdditionOverflow() {
        final int hi = Integer.MAX_VALUE / 2 + 10;
        final int lower = hi - 1;
        final int upper = hi + 1;

        // mean = (lower + upper) / 2 would overflow
        Assertions.assertTrue(lower + upper < 0);

        final UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(lower, upper);

        Assertions.assertEquals(1d / 3d, dist.probability(hi));
        Assertions.assertEquals(2d / 3d, dist.cumulativeProbability(hi));

        Assertions.assertEquals(hi, dist.getMean());
    }

    /**
     * Test the inverse CDF returns the correct x from the CDF result.
     * Test cases created to generate rounding errors on the inversion.
     */
    @ParameterizedTest
    @CsvSource(value = {
        // Extreme bounds
        "-2147483648, -2147483648",
        "-2147483648, -2147483647",
        "-2147483648, -2147483646",
        "-2147483648, -2147483638",
        "2147483647, 2147483647",
        "2147483646, 2147483647",
        "2147483645, 2147483647",
        "2147483637, 2147483647",
        // icdf(cdf(x)) requires rounding up
        "3, 40",
        "71, 201",
        "223, 267",
        "45, 125",
        "53, 81",
        // icdf(cdf(x)) requires rounding down
        "48, 247",
        "141, 222",
        "106, 223",
        "156, 201",
        "86, 265",
    })
    void testInverseCDF(int lower, int upper) {
        final UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(lower, upper);
        final int[] x = MathArrays.sequence(upper - lower, lower, 1);
        testCumulativeProbabilityInverseMapping(dist, x);
    }

    /**
     * Test the inverse SF returns the correct x from the SF result.
     * Test cases created to generate rounding errors on the inversion.
     */
    @ParameterizedTest
    @CsvSource(value = {
        // Extreme bounds
        "-2147483648, -2147483648",
        "-2147483648, -2147483647",
        "-2147483648, -2147483646",
        "-2147483648, -2147483638",
        "2147483647, 2147483647",
        "2147483646, 2147483647",
        "2147483645, 2147483647",
        "2147483637, 2147483647",
        // isf(sf(x)) requires rounding up
        "52, 91",
        "81, 106",
        "79, 268",
        "54, 249",
        "189, 267",
        // isf(sf(x)) requires rounding down
        "105, 279",
        "42, 261",
        "37, 133",
        "59, 214",
        "33, 118",
    })
    void testInverseSF(int lower, int upper) {
        final UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(lower, upper);
        final int[] x = MathArrays.sequence(upper - lower, lower, 1);
        testSurvivalProbabilityInverseMapping(dist, x);
    }

    /**
     * Test the probability in a range uses the exact computation of
     * {@code (x1 - x0) / (upper - lower + 1)} assuming x0 and x1 are within [lower, upper].
     * This test will fail if the distribution uses the default implementation in
     * {@link AbstractDiscreteDistribution}.
     */
    @ParameterizedTest
    @CsvSource(value = {
        // Extreme bounds
        "-2147483648, -2147483648",
        "-2147483648, -2147483647",
        "-2147483648, -2147483646",
        "-2147483648, -2147483638",
        "2147483647, 2147483647",
        "2147483646, 2147483647",
        "2147483645, 2147483647",
        "2147483637, 2147483647",
        // Range is a prime number
        "-10, 2", // 13
        "10, 16",  // 7
        "-20, -10", // 11
        // Range is even
        "-10, 3", // 14
        "10, 17",  // 8
        "-20, -9", // 12
        // Large range
        "-2147483648, 2147483647",
        "-2147483648, 1263781682",
        "-2147483648, 1781682",
        "-2147483648, -231781682",
        "-1324234584, 2147483647",
        "-324234584, 2147483647",
        "6234584, 2147483647",
        "-1256362376, 125637",
        "-62378468, 1325657374",
    })
    void testProbabilityRange(int lower, int upper) {
        final UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(lower, upper);
        final double r = (double) upper - lower + 1;
        final long stride = r < 20 ? 1 : (long) (r / 20);
        for (long x0 = lower; x0 <= upper; x0 += stride) {
            for (long x1 = x0; x1 <= upper; x1 += stride) {
                final double p = (x1 - x0) / r;
                Assertions.assertEquals(p, dist.probability((int) x0, (int) x1));
            }
        }
    }

    @Test
    void testProbabilityRangeEdgeCases() {
        final UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(3, 5);

        Assertions.assertThrows(DistributionException.class, () -> dist.probability(4, 3));

        // x0 >= upper
        Assertions.assertEquals(0, dist.probability(5, 6));
        Assertions.assertEquals(0, dist.probability(15, 16));
        // x1 < lower
        Assertions.assertEquals(0, dist.probability(-3, 1));

        // x0 == x1
        Assertions.assertEquals(0, dist.probability(3, 3));
        Assertions.assertEquals(0, dist.probability(4, 4));
        Assertions.assertEquals(0, dist.probability(5, 5));
        Assertions.assertEquals(0, dist.probability(6, 6));

        // x0+1 == x1
        Assertions.assertEquals(1.0 / 3, dist.probability(3, 4));
        Assertions.assertEquals(1.0 / 3, dist.probability(4, 5));

        // x1 > upper
        Assertions.assertEquals(1, dist.probability(2, 6));
        Assertions.assertEquals(2.0 / 3, dist.probability(3, 6));
        Assertions.assertEquals(1.0 / 3, dist.probability(4, 6));
        Assertions.assertEquals(0, dist.probability(5, 6));

        // x0 < lower
        Assertions.assertEquals(0, dist.probability(-2, 2));
        Assertions.assertEquals(1.0 / 3, dist.probability(-2, 3));
        Assertions.assertEquals(2.0 / 3, dist.probability(-2, 4));
        Assertions.assertEquals(1.0, dist.probability(-2, 5));

        // x1 > upper && x0 < lower
        Assertions.assertEquals(1, dist.probability(-2, 6));
    }
}
