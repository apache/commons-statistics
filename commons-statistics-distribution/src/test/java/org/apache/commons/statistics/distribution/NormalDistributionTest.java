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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * Test cases for {@link NormalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class NormalDistributionTest extends BaseContinuousDistributionTest {
    /** A standard normal distribution used for calculations.
     * This is immutable and thread-safe and can be used across instances. */
    private static final NormalDistribution STANDARD_NORMAL = NormalDistribution.of(0, 1);

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

    @Override
    protected double getRelativeTolerance() {
        // Tests are limited by the inverse survival probability
        // Tolerance is 4.440892098500626E-15.
        return 20 * RELATIVE_EPS;
    }

    @Override
    protected double getHighPrecisionRelativeTolerance() {
        // Tests are limited by the survival probability.
        // Tolerance is 1.6653345369377348E-14.
        // This is the lowest achieved with various implementations of the
        // survival function against high precision reference data.
        // It requires computing the factor sqrt(2 * sd * sd) exactly.
        return 75 * RELATIVE_EPS;
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
     * Verifies fixes for JIRA MATH-167, MATH-414.
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
     * Verifies fixes for JIRA MATH-167, MATH-414.
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
        // Tolerance limited by precision of p close to 1.
        // Lower the tolerance as the p value approaches 1.
        double result;
        result = dist.inverseCumulativeProbability(0.841344746068543);
        TestUtils.assertEquals(1.0, result, createRelTolerance(1e-15));
        result = dist.inverseCumulativeProbability(0.9772498680518209);
        TestUtils.assertEquals(2.0, result, createRelTolerance(1e-14));
        result = dist.inverseCumulativeProbability(0.9986501019683698);
        TestUtils.assertEquals(3.0, result, createRelTolerance(1e-13));
        result = dist.inverseCumulativeProbability(0.9999683287581673);
        TestUtils.assertEquals(4.0, result, createRelTolerance(1e-12));
    }

    /**
     * Test the inverse CDF is supported through the entire range of small values
     * that can be computed by the CDF. Approximate limit is x down to -38
     * (CDF around 2.8854e-316).
     * Verifies fix for STATISTICS-37.
     */
    @Test
    void testInverseCDF() {
        final NormalDistribution dist = NormalDistribution.of(0, 1);
        Assertions.assertEquals(0.0, dist.inverseCumulativeProbability(0.5));
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
            Assertions.assertEquals(x, x0, Math.abs(x) * 1e-11, () -> "CDF = " + cdf);
        }
    }

    /**
     * Test the PDF using high-accuracy uniform x data.
     *
     * <p>This dataset uses uniformly spaced machine representable x values that have no
     * round-off component when squared. If the density is implemented using
     * {@code exp(logDensity(x))} the test will fail. Using the log density requires a
     * tolerance of approximately 53 ULP to pass the test of larger x values.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "normpdf.csv")
    void testPDF(double x, BigDecimal expected) {
        assertPDF(x, expected, 2);
    }

    /**
     * Test the PDF using high-accuracy random x data.
     *
     * <p>This dataset uses random x values with full usage of the 52-bit mantissa to ensure
     * that there is a round-off component when squared. It requires a high precision exponential
     * function using the round-off to compute {@code exp(-0.5*x*x)} accurately.
     * Using a standard precision computation requires a tolerance of approximately 383 ULP
     * to pass the test of larger x values.
     *
     * <p>See STATISTICS-52.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "normpdf2.csv")
    void testPDF2(double x, BigDecimal expected) {
        assertPDF(x, expected, 3);
    }

    private static void assertPDF(double x, BigDecimal expected, int ulpTolerance) {
        final double e = expected.doubleValue();
        final double a = STANDARD_NORMAL.density(x);
        Assertions.assertEquals(e, a, Math.ulp(e) * ulpTolerance,
            () -> "ULP error: " + expected.subtract(new BigDecimal(a)).doubleValue() / Math.ulp(e));
    }
}
