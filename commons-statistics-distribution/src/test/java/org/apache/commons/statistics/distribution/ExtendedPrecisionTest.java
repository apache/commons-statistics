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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ExtendedPrecision}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExtendedPrecisionTest {
    /** sqrt(2). */
    private static final double ROOT2 = Math.sqrt(2.0);
    /** sqrt(2 pi) as a String. Computed to 64-digits. */
    private static final String SQRT_TWO_PI = "2.506628274631000502415765284811045253006986740609938316629923576";
    /** sqrt(2 pi) as a double. Note: This is 1 ULP different from Math.sqrt(2 * Math.PI). */
    private static final double ROOT2PI = Double.parseDouble(SQRT_TWO_PI);
    /** The sum of the squared ULP error for the first standard computation for sqrt(2 * x * x). */
    private static final RMS SQRT2XX_RMS1 = new RMS();
    /** The sum of the squared ULP error for the second standard computation for sqrt(2 * x * x). */
    private static final RMS SQRT2XX_RMS2 = new RMS();
    /** The sum of the squared ULP error for the first computation for x * sqrt(2 pi). */
    private static final RMS XSQRT2PI_RMS = new RMS();
    /** The sum of the squared ULP error for the first computation for exp(-0.5*x*x). */
    private static final RMS EXPMHXX_RMS1 = new RMS();
    /** The sum of the squared ULP error for the second computation for exp(-0.5*x*x). */
    private static final RMS EXPMHXX_RMS2 = new RMS();

    /**
     * Class to compute the root mean squared error (RMS).
     * @see <a href="https://en.wikipedia.org/wiki/Root_mean_square">Wikipedia: RMS</a>
     */
    private static class RMS {
        private double ss;
        private double max;
        private int n;

        /**
         * @param x Value (assumed to be positive)
         */
        void add(double x) {
            // Overflow is not supported.
            // Assume the expected and actual are quite close when measuring the RMS.
            ss += x * x;
            n++;
            // Absolute error when detecting the maximum
            x = Math.abs(x);
            max = max < x ? x : max;
        }

        /**
         * Gets the maximum error.
         *
         * <p>This is not used for assertions. It can be used to set maximum ULP thresholds
         * for test data if the TestUtils.assertEquals method is used with a large maxUlps
         * to measure the ulp (and effectively ignore failures) and the maximum reported
         * as the end of testing.
         *
         * @return maximum error
         */
        double getMax() {
            return max;
        }

        /**
         * Gets the root mean squared error (RMS).
         *
         * <p> Note: If no data has been added this will return 0/0 = nan.
         * This prevents using in assertions without adding data.
         *
         * @return root mean squared error (RMS)
         */
        double getRMS() {
            return Math.sqrt(ss / n);
        }
    }

    @Test
    void testSqrt2PiConstants() {
        final BigDecimal sqrt2pi = new BigDecimal(SQRT_TWO_PI);

        // Use a 106-bit number as:
        // (value, roundOff)
        final double value = sqrt2pi.doubleValue();
        final double roundOff = sqrt2pi.subtract(new BigDecimal(value)).doubleValue();
        // Adding the round-off does not change the value
        Assertions.assertEquals(value, value + roundOff, "value + round-off");
        // Check constants
        Assertions.assertEquals(value, ExtendedPrecision.SQRT2PI.hi(), "sqrt(2 pi)");
        Assertions.assertEquals(roundOff, ExtendedPrecision.SQRT2PI.lo(), "sqrt(2 pi) round-off");
        // Sanity check against JDK Math
        Assertions.assertEquals(value, Math.sqrt(2 * Math.PI), Math.ulp(value), "Math.sqrt(2 pi)");
    }

    @Test
    void testSqrt2xxUnderAndOverflow() {
        final double x = 1.5;
        final double e = 2.12132034355964257320253308631;
        Assertions.assertEquals(e, ExtendedPrecision.sqrt2xx(x));
        for (final int i : new int[] {-1000, -600, -200, 200, 600, 1000}) {
            final double scale = Math.scalb(1.0, i);
            final double x1 = x * scale;
            final double e1 = e * scale;
            Assertions.assertEquals(e1, ExtendedPrecision.sqrt2xx(x1), () -> Double.toString(x1));
        }
    }

    @Test
    void testSqrt2xxExtremes() {
        // Handle big numbers
        Assertions.assertEquals(Double.POSITIVE_INFINITY, ExtendedPrecision.sqrt2xx(Double.MAX_VALUE));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, ExtendedPrecision.sqrt2xx(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(0.0, ExtendedPrecision.sqrt2xx(0));
        Assertions.assertEquals(ROOT2, ExtendedPrecision.sqrt2xx(1));
        Assertions.assertEquals(Math.sqrt(8), ExtendedPrecision.sqrt2xx(2));
        // Handle sub-normal numbers
        for (int i = 2; i <= 10; i++) {
            Assertions.assertEquals(i * Double.MIN_VALUE * Math.sqrt(2), ExtendedPrecision.sqrt2xx(i * Double.MIN_VALUE));
        }
        // Currently the argument is assumed to be positive.
        Assertions.assertEquals(Double.NaN, ExtendedPrecision.sqrt2xx(Double.NaN));
        // Big negative numbers overflow the square and the extended precision computation generates the overflow result.
        Assertions.assertEquals(Double.POSITIVE_INFINITY, ExtendedPrecision.sqrt2xx(-1e300));
    }

    /**
     * Test the extended precision {@code sqrt(2 * x * x)}. The expected result
     * is an extended precision computation. For comparison ulp errors are collected for
     * two standard precision computations.
     *
     * @param x Value x
     * @param expected Expected result of {@code sqrt(2 * x * x)}.
     */
    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "sqrt2xx.csv")
    void testSqrt2xx(double x, BigDecimal expected) {
        final double e = expected.doubleValue();
        Assertions.assertEquals(e, ExtendedPrecision.sqrt2xx(x));
        // Compute error for the standard computations
        addError(Math.sqrt(2 * x * x), expected, e, SQRT2XX_RMS1);
        addError(x * ROOT2, expected, e, SQRT2XX_RMS2);
    }

    @Test
    void testSqrt2xxStandardPrecision1() {
        // Typical result:   max   0.7780  rms   0.2144
        assertPrecision(SQRT2XX_RMS1, 0.9, 0.3);
    }

    @Test
    void testSqrt2xxStandardPrecision2() {
        // Typical result:   max   1.0598  rms   0.4781
        assertPrecision(SQRT2XX_RMS2, 1.3, 0.6);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 1, Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN})
    void testXsqrt2piEdgeCases(double x) {
        final double expected = x * ROOT2PI;
        final double actual = ExtendedPrecision.xsqrt2pi(x);
        Assertions.assertEquals(expected, actual, 1e-15);
    }

    /**
     * Test the extended precision {@code x * sqrt(2 * pi)}. The expected result
     * is an extended precision computation. For comparison ulp errors are collected for
     * a standard computation.
     *
     * @param x Value x
     * @param expected Expected result of {@code x * sqrt(2 * pi)}.
     */
    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "xsqrt2pi.csv")
    void testXsqrt2pi(double x, BigDecimal expected) {
        final double e = expected.doubleValue();
        Assertions.assertEquals(e, ExtendedPrecision.xsqrt2pi(x));
        // Compute error for the standard computation
        addError(x * ROOT2PI, expected, e, XSQRT2PI_RMS);
    }

    @Test
    void testXsqrt2piPrecision() {
        // Typical result:   max   1.1397  rms   0.5368
        assertPrecision(XSQRT2PI_RMS, 1.2, 0.6);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.5, 1, 2, 3, 4, 5, 38.5, Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN})
    void testExpmhxxEdgeCases(double x) {
        final double expected = Math.exp(-0.5 * x * x);
        Assertions.assertEquals(expected, ExtendedPrecision.expmhxx(x));
        Assertions.assertEquals(expected, ExtendedPrecision.expmhxx(-x));
    }

    /**
     * Test the extended precision {@code exp(-0.5 * x * x)}. The expected result
     * is an extended precision computation. For comparison ulp errors are collected for
     * the standard precision computation.
     *
     * @param x Value x
     * @param expected Expected result of {@code exp(-0.5 * x * x)}.
     */
    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "expmhxx.csv")
    void testExpmhxx(double x, BigDecimal expected) {
        final double e = expected.doubleValue();
        final double actual = ExtendedPrecision.expmhxx(x);
        Assertions.assertEquals(e, actual, Math.ulp(e) * 2);
        // Compute errors
        addError(actual, expected, e, EXPMHXX_RMS1);
        addError(Math.exp(-0.5 * x * x), expected, e, EXPMHXX_RMS2);
    }

    @Test
    void testExpmhxxHighPrecision() {
        // Typical result:   max    0.9727  rms   0.3481
        assertPrecision(EXPMHXX_RMS1, 1.5, 0.5);
    }

    @Test
    void testExpmhxxStandardPrecision() {
        // Typical result:   max   385.7193  rms   50.7769
        assertPrecision(EXPMHXX_RMS2, 400, 60);
    }

    private static void assertPrecision(RMS rms, double maxError, double rmsError) {
        Assertions.assertTrue(rms.getMax() < maxError, () -> "max error: " + rms.getMax());
        Assertions.assertTrue(rms.getRMS() < rmsError, () -> "rms error: " + rms.getRMS());
    }

    private static void addError(double z, BigDecimal expected, double e, RMS rms) {
        double error;
        if (z == e) {
            error = 0;
        } else {
            // Compute ULP error
            error = expected.subtract(new BigDecimal(z))
                .divide(new BigDecimal(Math.ulp(e)), MathContext.DECIMAL64).doubleValue();
        }
        rms.add(error);
    }
}
