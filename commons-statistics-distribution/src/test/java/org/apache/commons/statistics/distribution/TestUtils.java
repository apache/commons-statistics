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

import java.text.DecimalFormat;
import java.util.function.Supplier;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.numbers.core.Precision;
import org.junit.jupiter.api.Assertions;

/**
 * Test utilities.
 */
public final class TestUtils {
    /**
     * Collection of static methods used in math unit tests.
     */
    private TestUtils() {}

    /**
     * Verifies that expected and actual are within delta, or are both NaN or
     * infinities of the same sign.
     */
    public static void assertEquals(double expected,
                                    double actual,
                                    double delta) {
        Assertions.assertEquals(expected, actual, delta);
    }

    /**
     * Verifies that expected and actual are within delta, or are both NaN or
     * infinities of the same sign.
     */
    public static void assertEquals(Supplier<String> msg,
                                    double expected,
                                    double actual,
                                    double delta) {
        // check for NaN
        if (Double.isNaN(expected)) {
            Assertions.assertTrue(Double.isNaN(actual), () -> actual + " is not NaN.");
        } else {
            Assertions.assertEquals(expected, actual, delta, msg);
        }
    }

    /**
     * Verifies that two double arrays have equal entries, up to tolerance
     */
    public static void assertEquals(double[] expected,
                                    double[] observed,
                                    double tolerance) {
        assertEquals(() -> "Array comparison failure", expected, observed, tolerance);
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or
     * equal to relativeError.  If expected is infinite or NaN, actual must be
     * the same (NaN or infinity of the same sign).
     *
     * @param expected expected value
     * @param actual  observed value
     * @param relativeError  maximum allowable relative error
     */
    public static void assertRelativelyEquals(double expected,
                                              double actual,
                                              double relativeError) {
        assertRelativelyEquals(null, expected, actual, relativeError);
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or
     * equal to relativeError.  If expected is infinite or NaN, actual must be
     * the same (NaN or infinity of the same sign).
     *
     * @param msg  message to return with failure
     * @param expected expected value
     * @param actual  observed value
     * @param relativeError  maximum allowable relative error
     */
    public static void assertRelativelyEquals(Supplier<String> msg,
                                              double expected,
                                              double actual,
                                              double relativeError) {
        if (Double.isNaN(expected)) {
            Assertions.assertTrue(Double.isNaN(actual), msg);
        } else if (Double.isNaN(actual)) {
            Assertions.assertTrue(Double.isNaN(expected), msg);
        } else if (Double.isInfinite(actual) || Double.isInfinite(expected)) {
            Assertions.assertEquals(expected, actual, relativeError);
        } else if (expected == 0.0) {
            Assertions.assertEquals(actual, expected, relativeError, msg);
        } else {
            final double absError = Math.abs(expected) * relativeError;
            Assertions.assertEquals(expected, actual, absError, msg);
        }
    }

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(Supplier<String> msg,
                                    double[] expected,
                                    double[] observed,
                                    double tolerance) {
        if (expected.length != observed.length) {
            final StringBuilder out = new StringBuilder(msg.get());
            out.append("\n Arrays not same length. \n");
            out.append("expected has length ");
            out.append(expected.length);
            out.append(" observed length = ");
            out.append(observed.length);
            Assertions.fail(out.toString());
        }
        boolean failure = false;
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < expected.length; i++) {
            if (!Precision.equalsIncludingNaN(expected[i], observed[i], tolerance)) {
                if (!failure) {
                    out.append(msg.get());
                    failure = true;
                }
                out.append("\n Elements at index ");
                out.append(i);
                out.append(" differ. ");
                out.append(" expected = ");
                out.append(expected[i]);
                out.append(" observed = ");
                out.append(observed[i]);
            }
        }
        if (failure) {
            Assertions.fail(out.toString());
        }
    }

    /**
     * Asserts the null hypothesis for a ChiSquare test.  Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     *
     * @param valueLabels labels for the values of the discrete distribution under test
     * @param expected expected counts
     * @param observed observed counts
     * @param alpha significance level of the test
     */
    public static void assertChiSquareAccept(String[] valueLabels,
                                             double[] expected,
                                             long[] observed,
                                             double alpha) {
        final ChiSquareTest chiSquareTest = new ChiSquareTest();

        // Fail if we can reject null hypothesis that distributions are the same
        if (chiSquareTest.chiSquareTest(expected, observed, alpha)) {
            final StringBuilder msgBuffer = new StringBuilder();
            final DecimalFormat df = new DecimalFormat("#.##");
            msgBuffer.append("Chisquare test failed");
            msgBuffer.append(" p-value = ");
            msgBuffer.append(chiSquareTest.chiSquareTest(expected, observed));
            msgBuffer.append(" chisquare statistic = ");
            msgBuffer.append(chiSquareTest.chiSquare(expected, observed));
            msgBuffer.append(". \n");
            msgBuffer.append("value\texpected\tobserved\n");
            for (int i = 0; i < expected.length; i++) {
                msgBuffer.append(valueLabels[i]);
                msgBuffer.append("\t");
                msgBuffer.append(df.format(expected[i]));
                msgBuffer.append("\t\t");
                msgBuffer.append(observed[i]);
                msgBuffer.append("\n");
            }
            msgBuffer.append("This test can fail randomly due to sampling error with probability ");
            msgBuffer.append(alpha);
            msgBuffer.append(".");
            Assertions.fail(msgBuffer.toString());
        }
    }

    /**
     * Asserts the null hypothesis for a ChiSquare test.  Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     *
     * @param values integer values whose observed and expected counts are being compared
     * @param expected expected counts
     * @param observed observed counts
     * @param alpha significance level of the test
     */
    public static void assertChiSquareAccept(int[] values,
                                             double[] expected,
                                             long[] observed,
                                             double alpha) {
        final String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = Integer.toString(values[i]);
        }
        assertChiSquareAccept(labels, expected, observed, alpha);
    }

    /**
     * Asserts the null hypothesis for a ChiSquare test.  Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     *
     * @param expected expected counts
     * @param observed observed counts
     * @param alpha significance level of the test
     */
    public static void assertChiSquareAccept(double[] expected,
                                             long[] observed,
                                             double alpha) {
        final String[] labels = new String[expected.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Integer.toString(i + 1);
        }
        assertChiSquareAccept(labels, expected, observed, alpha);
    }

    /**
     * Computes the 25th, 50th and 75th percentiles of the given distribution and returns
     * these values in an array.
     */
    public static double[] getDistributionQuartiles(ContinuousDistribution distribution) {
        final double[] quantiles = new double[3];
        quantiles[0] = distribution.inverseCumulativeProbability(0.25d);
        quantiles[1] = distribution.inverseCumulativeProbability(0.5d);
        quantiles[2] = distribution.inverseCumulativeProbability(0.75d);
        return quantiles;
    }

    /**
     * Updates observed counts of values in quartiles.
     * counts[0] <-> 1st quartile ... counts[3] <-> top quartile
     */
    public static void updateCounts(double value, long[] counts, double[] quartiles) {
        if (value < quartiles[0]) {
            counts[0]++;
        } else if (value > quartiles[2]) {
            counts[3]++;
        } else if (value > quartiles[1]) {
            counts[2]++;
        } else {
            counts[1]++;
        }
    }

    /**
     * Eliminates points with zero mass from densityPoints and densityValues parallel
     * arrays.  Returns the number of positive mass points and collapses the arrays so
     * that the first <returned value> elements of the input arrays represent the positive
     * mass points.
     */
    public static int eliminateZeroMassPoints(int[] densityPoints, double[] densityValues) {
        int positiveMassCount = 0;
        for (int i = 0; i < densityValues.length; i++) {
            if (densityValues[i] > 0) {
                positiveMassCount++;
            }
        }
        if (positiveMassCount < densityValues.length) {
            final int[] newPoints = new int[positiveMassCount];
            final double[] newValues = new double[positiveMassCount];
            int j = 0;
            for (int i = 0; i < densityValues.length; i++) {
                if (densityValues[i] > 0) {
                    newPoints[j] = densityPoints[i];
                    newValues[j] = densityValues[i];
                    j++;
                }
            }
            System.arraycopy(newPoints, 0, densityPoints, 0, positiveMassCount);
            System.arraycopy(newValues, 0, densityValues, 0, positiveMassCount);
        }
        return positiveMassCount;
    }
}
