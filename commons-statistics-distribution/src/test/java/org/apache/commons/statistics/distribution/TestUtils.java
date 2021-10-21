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

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.function.Supplier;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.jupiter.api.Assertions;

/**
 * Test utilities.
 */
final class TestUtils {
    /**
     * The relative error threshold below which absolute error is reported in ULP.
     */
    private static final double ULP_THRESHOLD = 100 * Math.ulp(1.0);
    /**
     * The prefix for the formatted expected value.
     *
     * <p>This should be followed by the expected value then '>'.
     */
    private static final String EXPECTED_FORMAT = "expected: <";
    /**
     * The prefix for the formatted actual value.
     *
     * <p>It is assumed this will be following the expected value.
     *
     * <p>This should be followed by the actual value then '>'.
     */
    private static final String ACTUAL_FORMAT = ">, actual: <";
    /**
     * The prefix for the formatted relative error value.
     *
     * <p>It is assumed this will be following the actual value.
     *
     * <p>This should be followed by the relative error value then '>'.
     */
    private static final String RELATIVE_ERROR_FORMAT = ">, rel.error: <";
    /**
     * The prefix for the formatted absolute error value.
     *
     * <p>It is assumed this will be following the relative value.
     *
     * <p>This should be followed by the absolute error value then '>'.
     */
    private static final String ABSOLUTE_ERROR_FORMAT = ">, abs.error: <";
    /**
     * The prefix for the formatted ULP error value.
     *
     * <p>It is assumed this will be following the relative value.
     *
     * <p>This should be followed by the ULP error value then '>'.
     */
    private static final String ULP_ERROR_FORMAT = ">, ulp error: <";

    /**
     * Collection of static methods used in math unit tests.
     */
    private TestUtils() {}

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Custom assertions using a DoubleTolerance
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <em>Asserts</em> {@code expected} and {@code actual} are considered equal with the
     * provided tolerance.
     *
     * @param expected The expected value.
     * @param actual The value to tolerance against {@code expected}.
     * @param tolerance The tolerance.
     * @throws AssertionError If the values are not considered equal
     */
    static void assertEquals(double expected, double actual, DoubleTolerance tolerance) {
        assertEquals(expected, actual, tolerance, (String) null);
    }

    /**
     * <em>Asserts</em> {@code expected} and {@code actual} are considered equal with the
     * provided tolerance.
     *
     * <p>Fails with the supplied failure {@code message}.
     *
     * @param expected The expected value.
     * @param actual The value to tolerance against {@code expected}.
     * @param tolerance The tolerance.
     * @param message The message.
     * @throws AssertionError If the values are not considered equal
     */
    static void assertEquals(double expected, double actual, DoubleTolerance tolerance, String message) {
        if (!tolerance.test(expected, actual)) {
            throw new AssertionError(format(expected, actual, tolerance, message));
        }
    }

    /**
     * <em>Asserts</em> {@code expected} and {@code actual} are considered equal with the
     * provided tolerance.
     *
     * <p>If necessary, the failure message will be retrieved lazily from the supplied
     * {@code messageSupplier}.
     *
     * @param expected The expected value.
     * @param actual The value to tolerance against {@code expected}.
     * @param tolerance The tolerance.
     * @param messageSupplier The message supplier.
     * @throws AssertionError If the values are not considered equal
     */
    static void assertEquals(double expected, double actual, DoubleTolerance tolerance,
        Supplier<String> messageSupplier) {
        if (!tolerance.test(expected, actual)) {
            throw new AssertionError(
                format(expected, actual, tolerance, messageSupplier == null ? null : messageSupplier.get()));
        }
    }

    /**
     * Format the message.
     *
     * @param expected The expected value.
     * @param actual The value to check against <code>expected</code>.
     * @param tolerance The tolerance.
     * @param message The message.
     * @return the formatted message
     */
    private static String format(double expected, double actual, DoubleTolerance tolerance, String message) {
        return buildPrefix(message) + formatValues(expected, actual, tolerance);
    }

    /**
     * Builds the fail message prefix.
     *
     * @param message the message
     * @return the prefix
     */
    private static String buildPrefix(String message) {
        return StringUtils.isNotEmpty(message) ? message + " ==> " : "";
    }

    /**
     * Format the values.
     *
     * @param expected The expected value.
     * @param actual The value to check against <code>expected</code>.
     * @param tolerance The tolerance.
     * @return the formatted values
     */
    private static String formatValues(double expected, double actual, DoubleTolerance tolerance) {
        // Add error
        final double diff = Math.abs(expected - actual);
        final double rel = diff / Math.max(Math.abs(expected), Math.abs(actual));
        final StringBuilder msg = new StringBuilder(EXPECTED_FORMAT).append(expected).append(ACTUAL_FORMAT)
            .append(actual).append(RELATIVE_ERROR_FORMAT).append(rel);
        if (rel < ULP_THRESHOLD) {
            final long ulp = Math.abs(Double.doubleToRawLongBits(expected) - Double.doubleToRawLongBits(actual));
            msg.append(ULP_ERROR_FORMAT).append(ulp);
        } else {
            msg.append(ABSOLUTE_ERROR_FORMAT).append(diff);
        }
        msg.append('>');
        appendTolerance(msg, tolerance);
        return msg.toString();
    }

    /**
     * Append the tolerance to the message.
     *
     * @param msg The message
     * @param tolerance the tolerance
     */
    private static void appendTolerance(final StringBuilder msg, final Object tolerance) {
        final String description = StringUtils.toString(tolerance);
        if (StringUtils.isNotEmpty(description)) {
            msg.append(", tolerance: ").append(description);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////

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
    static void assertRelativelyEquals(Supplier<String> msg,
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

    /**
     * Asserts the null hypothesis for a ChiSquare test.  Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     *
     * @param valueLabels labels for the values of the discrete distribution under test
     * @param expected expected counts
     * @param observed observed counts
     * @param alpha significance level of the test
     */
    private static void assertChiSquare(int[] valueLabels,
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
                msgBuffer.append('\t');
                msgBuffer.append(df.format(expected[i]));
                msgBuffer.append("\t\t");
                msgBuffer.append(observed[i]);
                msgBuffer.append('\n');
            }
            msgBuffer.append("This test can fail randomly due to sampling error with probability ");
            msgBuffer.append(alpha);
            msgBuffer.append('.');
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
    static void assertChiSquareAccept(int[] values,
                                      double[] expected,
                                      long[] observed,
                                      double alpha) {
        assertChiSquare(values, expected, observed, alpha);
    }

    /**
     * Asserts the null hypothesis for a ChiSquare test.  Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     *
     * @param expected expected counts
     * @param observed observed counts
     * @param alpha significance level of the test
     */
    static void assertChiSquareAccept(double[] expected,
                                      long[] observed,
                                      double alpha) {
        final int[] values = new int[expected.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = i + 1;
        }
        assertChiSquare(values, expected, observed, alpha);
    }

    /**
     * Computes the 25th, 50th and 75th percentiles of the given distribution and returns
     * these values in an array.
     *
     * @param distribution Distribution.
     * @return the quartiles
     */
    static double[] getDistributionQuartiles(ContinuousDistribution distribution) {
        final double[] quantiles = new double[3];
        quantiles[0] = distribution.inverseCumulativeProbability(0.25d);
        quantiles[1] = distribution.inverseCumulativeProbability(0.5d);
        quantiles[2] = distribution.inverseCumulativeProbability(0.75d);
        return quantiles;
    }

    /**
     * Updates observed counts of values in quartiles.
     * counts[0] <-> 1st quartile ... counts[3] <-> top quartile
     *
     * @param value Observed value.
     * @param counts Counts for each quartile.
     * @param quartiles Quartiles.
     */
    static void updateCounts(double value, long[] counts, double[] quartiles) {
        if (value > quartiles[1]) {
            counts[value <= quartiles[2] ? 2 : 3]++;
        } else {
            counts[value <= quartiles[0] ? 0 : 1]++;
        }
    }

    /**
     * Eliminates points with zero mass from densityPoints and densityValues parallel
     * arrays. Returns the number of positive mass points and collapses the arrays so that
     * the first <returned value> elements of the input arrays represent the positive mass
     * points.
     *
     * @param densityPoints Density points.
     * @param densityValues Density values.
     * @return number of positive mass points
     */
    static int eliminateZeroMassPoints(int[] densityPoints, double[] densityValues) {
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

    /**
     * Utility function for allocating an array and filling it with {@code n}
     * samples generated by the given {@code sampler}.
     *
     * @param n Number of samples.b
     * @param sampler Sampler.
     * @return an array of size {@code n}.
     */
    static double[] sample(int n,
                           ContinuousDistribution.Sampler sampler) {
        final double[] samples = new double[n];
        for (int i = 0; i < n; i++) {
            samples[i] = sampler.sample();
        }
        return samples;
    }

    /**
     * Utility function for allocating an array and filling it with {@code n}
     * samples generated by the given {@code sampler}.
     *
     * @param n Number of samples.
     * @param sampler Sampler.
     * @return an array of size {@code n}.
     */
    static int[] sample(int n,
                        DiscreteDistribution.Sampler sampler) {
        final int[] samples = new int[n];
        for (int i = 0; i < n; i++) {
            samples[i] = sampler.sample();
        }
        return samples;
    }

    /**
     * Gets the length of the array.
     *
     * @param array Array
     * @return the length (or 0 for null array)
     */
    static int getLength(double[] array) {
        return array == null ? 0 : array.length;
    }

    /**
     * Gets the length of the array.
     *
     * @param array Array
     * @return the length (or 0 for null array)
     */
    static int getLength(int[] array) {
        return array == null ? 0 : array.length;
    }

    /**
     * Gets the length of the array.
     *
     * @param array Array
     * @return the length (or 0 for null array)
     * @throws IllegalArgumentException if the object is not an array
     */
    static int getLength(Object array) {
        return array == null ? 0 : Array.getLength(array);
    }
}
