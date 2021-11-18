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

import org.apache.commons.math3.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    //-------------------- Additional test cases -------------------------------

    /** Test mean/variance. */
    @Test
    void testAdditionalMoments() {
        UniformDiscreteDistribution dist;

        dist = UniformDiscreteDistribution.of(0, 5);
        Assertions.assertEquals(2.5, dist.getMean());
        Assertions.assertEquals(35 / 12.0, dist.getVariance());

        dist = UniformDiscreteDistribution.of(0, 1);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(3 / 12.0, dist.getVariance());
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
}
