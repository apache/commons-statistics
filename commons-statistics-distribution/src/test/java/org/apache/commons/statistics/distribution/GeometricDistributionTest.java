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

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link GeometricDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class GeometricDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final double p = (Double) parameters[0];
        return GeometricDistribution.of(p);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {-0.1},
            {0.0},
            {1.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"ProbabilityOfSuccess"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 2 * RELATIVE_EPS;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(double p, double mean, double variance) {
        final GeometricDistribution dist = GeometricDistribution.of(p);
        testMoments(dist, mean, variance, DoubleTolerances.ulps(1));
    }

    static Stream<Arguments> testAdditionalMoments() {
        return Stream.of(
            Arguments.of(0.5, (1.0 - 0.5) / 0.5, (1.0 - 0.5) / (0.5 * 0.5)),
            Arguments.of(0.3, (1.0 - 0.3) / 0.3, (1.0 - 0.3) / (0.3 * 0.3))
        );
    }

    /**
     * Test the PMF is computed using the power function when p is above 0.5.
     * <p>Note: The geometric distribution PMF is defined as:
     * <pre>
     *   pmf(x) = (1-p)^x * p
     * </pre>
     * <p>As {@code p -> 0} use of the power function should be avoided as it will
     * propagate the inexact computation of {@code 1 - p}. The implementation can
     * switch to using a rearrangement with the exponential function which avoid
     * computing {@code 1 - p}.
     * <p>See STATISTICS-34.
     *
     * @param p Probability of success
     */
    @ParameterizedTest
    @ValueSource(doubles = {0.5, 0.6658665, 0.75, 0.8125347, 0.9, 0.95, 0.99})
    void testPMF(double p) {
        final GeometricDistribution dist = GeometricDistribution.of(p);
        final int[] x = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40};
        final double[] values = Arrays.stream(x).mapToDouble(k -> p * Math.pow(1 - p, k)).toArray();
        // The PMF should be an exact match to the direct implementation with Math.pow.
        testProbability(dist, x, values, DoubleTolerances.equals());
    }

    /**
     * Test the inverse CDF returns the correct x from the CDF result.
     * Cases were identified using various probabilities to discover a mismatch
     * of x != icdf(cdf(x)). This occurs due to rounding errors on the inversion.
     */
    @ParameterizedTest
    @ValueSource(doubles = {
        0.2,
        0.8,
        // icdf(cdf(x)) requires rounding up
        0.07131208016887369,
        0.14441285445326058,
        0.272118157703929,
        0.424656239093432,
        0.00899452845634574,
        // icdf(cdf(x)) requires rounding down
        0.3441320118140774,
        0.5680886873083258,
        0.8738746761971425,
        0.17373328785967923,
        0.09252030895185881,
    })
    void testInverseCDF(double p) {
        final GeometricDistribution dist = GeometricDistribution.of(p);
        final int[] x = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        testCumulativeProbabilityInverseMapping(dist, x);
    }

    /**
     * Test the inverse SF returns the correct x from the SF result.
     * Cases were identified using various probabilities to discover a mismatch
     * of x != isf(sf(x)). This occurs due to rounding errors on the inversion.
     */
    @ParameterizedTest
    @ValueSource(doubles = {
        0.2,
        0.8,
        // isf(sf(x)) requires rounding up
        0.9625911263689207,
        0.2858964038911178,
        0.31872883511135996,
        0.46149078212832284,
        0.3701613946505057,
        // isf(sf(x)) requires rounding down
        0.3796493606864414,
        0.1113177920615187,
        0.2587259503484439,
        0.8996839434455458,
        0.450704136259792,
    })
    void testInverseSF(double p) {
        final GeometricDistribution dist = GeometricDistribution.of(p);
        final int[] x = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        testSurvivalProbabilityInverseMapping(dist, x);
    }

    /**
     * Test the most extreme parameters. Uses a small enough value of p that the distribution is
     * truncated by the maximum integer value. This creates a case where (x+1) will overflow.
     * This occurs in the cumulative and survival function computations.
     */
    @Test
    void testExtremeParameters() {
        final double p = Double.MIN_VALUE;
        final GeometricDistribution dist = GeometricDistribution.of(p);

        final int x = Integer.MAX_VALUE;
        // CDF = 1 - (1-p)^(x+1)
        // Compute with log for accuracy with small p
        final double cdf = -Math.expm1(Math.log1p(-p) * (x + 1.0));
        Assertions.assertNotEquals(1.0, cdf);
        Assertions.assertEquals(cdf, dist.cumulativeProbability(x));
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(x - i, dist.inverseCumulativeProbability(dist.cumulativeProbability(x - i)));
        }

        // CDF(x=0) = p
        Assertions.assertEquals(p, dist.cumulativeProbability(0));
        Assertions.assertEquals(0, dist.inverseCumulativeProbability(p));
        Assertions.assertEquals(1, dist.inverseCumulativeProbability(Math.nextUp(p)));
        for (int i = 1; i < 5; i++) {
            Assertions.assertEquals(i, dist.inverseCumulativeProbability(dist.cumulativeProbability(i)));
        }

        // SF = (1-p)^(x+1)
        // Compute with log for accuracy with small p
        final double sf = Math.exp(Math.log1p(-p) * (x + 1.0));
        Assertions.assertEquals(1.0 - cdf, sf);
        Assertions.assertEquals(sf, dist.survivalProbability(x));
        // SF is too close to 1 to be able to invert
        Assertions.assertEquals(1.0, sf);
        Assertions.assertEquals(x, dist.inverseSurvivalProbability(Math.nextDown(1.0)));
    }

    /**
     * Test the most extreme parameters. Uses a large enough value of p that the distribution is
     * compacted to x=0.
     *
     * <p>p is one ULP down from 1.0.
     */
    @Test
    void testExtremeParameters2() {
        final double p = Math.nextDown(1.0);
        final GeometricDistribution dist = GeometricDistribution.of(p);

        final int x = 0;
        // CDF = 1 - (1-p)^(x+1)
        // CDF(x=0) = p
        Assertions.assertEquals(p, dist.cumulativeProbability(0));
        Assertions.assertEquals(0, dist.inverseCumulativeProbability(p));
        // CDF is too close to 1 to be able to invert next value
        Assertions.assertEquals(Integer.MAX_VALUE, dist.inverseCumulativeProbability(Math.nextUp(p)));

        // SF = (1-p)^(x+1)
        final double sf = 1 - p;
        Assertions.assertNotEquals(0.0, sf);
        Assertions.assertEquals(sf, dist.survivalProbability(x));
        for (int i = 1; i < 5; i++) {
            Assertions.assertEquals(i, dist.inverseSurvivalProbability(dist.survivalProbability(i)));
        }
    }

    /**
     * Test the most extreme parameters. Uses a large enough value of p that the distribution is
     * compacted to x=0.
     *
     * <p>p is two ULP down from 1.0.
     */
    @Test
    void testExtremeParameters3() {
        final double p = Math.nextDown(Math.nextDown(1.0));
        final GeometricDistribution dist = GeometricDistribution.of(p);

        final int x = 0;
        // CDF = 1 - (1-p)^(x+1)
        // CDF(x=0) = p
        Assertions.assertEquals(p, dist.cumulativeProbability(0));
        Assertions.assertEquals(0, dist.inverseCumulativeProbability(p));
        Assertions.assertEquals(1, dist.inverseCumulativeProbability(Math.nextUp(p)));
        // CDF is too close to 1 to be able to invert next value
        Assertions.assertEquals(Integer.MAX_VALUE, dist.inverseCumulativeProbability(Math.nextUp(Math.nextUp(p))));

        // SF = (1-p)^(x+1)
        final double sf = 1 - p;
        Assertions.assertNotEquals(0.0, sf);
        Assertions.assertEquals(sf, dist.survivalProbability(x));
        for (int i = 1; i < 5; i++) {
            Assertions.assertEquals(i, dist.inverseSurvivalProbability(dist.survivalProbability(i)));
        }
    }
}
