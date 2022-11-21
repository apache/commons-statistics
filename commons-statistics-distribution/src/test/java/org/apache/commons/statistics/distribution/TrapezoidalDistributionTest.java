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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link TrapezoidalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class TrapezoidalDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double a = (Double) parameters[0];
        final double b = (Double) parameters[1];
        final double c = (Double) parameters[2];
        final double d = (Double) parameters[3];
        return TrapezoidalDistribution.of(a, b, c, d);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0, 0.0, 0.0},
            // 1.0, 2.0, 3.0, 4.0 is OK - move points to incorrect locations
            {5.0, 2.0, 3.0, 4.0}, // a > d
            {1.0, 5.0, 3.0, 4.0}, // b > d
            {1.0, 2.0, 5.0, 4.0}, // c > d
            {3.5, 2.0, 3.0, 4.0}, // a > c
            {1.0, 3.5, 3.0, 4.0}, // b > c
            {2.5, 2.0, 3.0, 4.0}, // a > b
            {1.0, 2.0, 3.0, 0.0}, // d < a
            {1.0, 2.0, 0.0, 4.0}, // c < a
            {1.0, 0.0, 3.0, 4.0}, // b < a
            {1.0, 2.0, 3.0, 1.5}, // d < b
            {1.0, 2.0, 1.5, 4.0}, // c < b
            {1.0, 2.0, 3.0, 2.5}, // d < c
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"SupportLowerBound", "B", "C", "SupportUpperBound"};
    }

    @Override
    protected double getRelativeTolerance() {
        // Tolerance is 4.440892098500626E-15.
        return 20 * RELATIVE_EPS;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(double a, double b, double c, double d, double mean, double variance) {
        final TrapezoidalDistribution dist = TrapezoidalDistribution.of(a, b, c, d);
        testMoments(dist, mean, variance, DoubleTolerances.ulps(8));
    }

    static Stream<Arguments> testAdditionalMoments() {
        return Stream.of(
            // Computed using scipy.stats.trapezoid
            // Up slope, then flat
            Arguments.of(0, 0.1,   1, 1, 0.5245614035087719, 0.07562480763311791),
            Arguments.of(0, 1e-3,  1, 1, 0.5002499583124894, 0.08325006249999839),
            Arguments.of(0, 1e-6,  1, 1, 0.5000002499999582, 0.08333325000006259),
            Arguments.of(0, 1e-9,  1, 1, 0.50000000025,      0.08333333324999997),
            Arguments.of(0, 1e-12, 1, 1, 0.50000000000025,   0.08333333333324999),
            Arguments.of(0, 1e-15, 1, 1, 0.5000000000000003, 0.0833333333333332),
            Arguments.of(0, 0,     1, 1, 0.5,                0.08333333333333331),
            // Flat, then down slope
            Arguments.of(0, 0, 0.9,               1, 0.47543859649122816, 0.07562480763311777),
            Arguments.of(0, 0, 0.999,             1, 0.49975004168751025, 0.08325006249999842),
            Arguments.of(0, 0, 0.999999,          1, 0.4999997500000417,  0.08333325000006248),
            Arguments.of(0, 0, 0.999999999,       1, 0.49999999975000003, 0.08333333325000003),
            Arguments.of(0, 0, 0.999999999999,    1, 0.49999999999975003, 0.08333333333324999),
            Arguments.of(0, 0, 0.999999999999999, 1, 0.4999999999999998,  0.08333333333333326),
            Arguments.of(0, 0, 1,                 1, 0.5,                 0.08333333333333331)
        );
    }

    /**
     * Create a trapezoid with a very long upper tail to explicitly test the survival
     * probability is high precision.
     */
    @Test
    void testAdditionalSurvivalProbabilityHighPrecision() {
        final double a = 0;
        final double b = 0;
        final double c = 1;
        final double d = 1 << 14;
        final double x1 = Math.nextDown(d);
        final double x2 = Math.nextDown(x1);
        final double p1 = survivalProbability(a, b, c, d, x1);
        final double p2 = survivalProbability(a, b, c, d, x2);
        final TrapezoidalDistribution dist = TrapezoidalDistribution.of(a, b, c, d);
        final double[] points = {x1, x2};
        final double[] probabilities = {p1, p2};

        // This fails if the sf(x) = 1 - cdf(x)
        testSurvivalProbabilityHighPrecision(
            dist,
            points,
            probabilities,
            DoubleTolerances.relative(1e-15));

        // This fails if the isf(p) = icdf(1 - p)
        testInverseSurvivalProbability(
            dist,
            probabilities,
            points,
            DoubleTolerances.ulps(0));
    }

    /**
     * Compute the trapezoid distribution survival probability for the value {@code x}
     * in the region {@code [c, d]}.
     *
     * @param a Lower limit of the distribution (inclusive).
     * @param b Start of the trapezoid constant density.
     * @param c End of the trapezoid constant density.
     * @param d Upper limit of the distribution (inclusive).
     * @param x Value in [c, d].
     * @return the probability
     */
    private static double survivalProbability(double a, double b, double c, double d, double x) {
        Assertions.assertTrue(c <= x && x <= d, "Domain error");
        final BigDecimal aa = new BigDecimal(a);
        final BigDecimal bb = new BigDecimal(b);
        final BigDecimal cc = new BigDecimal(c);
        final BigDecimal dd = new BigDecimal(d);
        final BigDecimal divisor = dd.add(cc).subtract(aa).subtract(bb).multiply(dd.subtract(cc));
        return dd.subtract(new BigDecimal(x)).pow(2)
                .divide(divisor, MathContext.DECIMAL128).doubleValue();
    }
}
