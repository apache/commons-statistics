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
package org.apache.commons.statistics.inference;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

/**
 * Test utilities.
 */
final class TestUtils {
    private static final double[] INVALID_ALPHA = {
        0, 0.5000000000000001, 0.95, 0.99, 1, -1, -Double.MIN_VALUE, Double.MAX_VALUE,
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN
    };

    /** No instances. */
    private TestUtils() {}

    /**
     * Print a formatted message to stdout.
     * Provides a single point to disable checkstyle warnings on print statements and
     * enable/disable all print debugging.
     *
     * @param format Format string.
     * @param args Arguments.
     */
    static void printf(String format, Object... args) {
        // CHECKSTYLE: stop regex
        System.out.printf(format, args);
        // CHECKSTYLE: resume regex
    }

    /**
     * Assert that execution of the supplied executable throws an exception of the
     * expected type. The message of the exception is checked to contain
     * the provided values (case insensitive).
     *
     * @param <T> Exception type.
     * @param expectedType Expected exception type.
     * @param executable Executable.
     * @param values Values to observe in the exception message.
     * @return the exception
     */
    static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType,
            Executable executable, String... values) {
        final T t = Assertions.assertThrows(expectedType, executable);
        final String msg = t.getMessage();
        if (msg == null || msg.isEmpty()) {
            Assertions.assertEquals(0, values.length,
                "Exception message is empty but values were expected");
            return t;
        }
        final String m = msg.toLowerCase(Locale.ROOT);
        for (final String v : values) {
            Assertions.assertTrue(m.contains(v.toLowerCase(Locale.ROOT)),
                () -> "Exception message <" + msg + "> missing value '" + v + "'");
        }
        return t;
    }

    /**
     * Assert the provided function throws an {@link IllegalArgumentException} for an invalid
     * significance level; the alpha should be in the range {@code (0, 0.5]}.
     *
     * <p>Note: The function will be invoked many times with valid and invalid significance levels
     * so it is advised that the operation is low cost.
     *
     * @param fun Function
     * @param msg Message to return with failure
     */
    static void assertSignificanceLevel(DoubleConsumer fun, String msg) {
        assertSignificanceLevel(fun, () -> msg);
    }

    /**
     * Assert the provided function throws an {@link IllegalArgumentException} for an invalid
     * significance level; the alpha should be in the range {@code (0, 0.5]}.
     *
     * <p>Note: The function will be invoked many times with valid and invalid significance levels
     * so it is advised that the operation is low cost.
     *
     * @param fun Function
     * @param msg Message to return with failure
     */
    static void assertSignificanceLevel(DoubleConsumer fun, Supplier<String> msg) {
        fun.accept(Double.MIN_VALUE);
        fun.accept(0.5);
        for (final double alpha : INVALID_ALPHA) {
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> fun.accept(alpha),
                () -> String.format("Alpha %s", prefix(msg), alpha));
            final String m = ex.getMessage().toLowerCase(Locale.ROOT);
            Assertions.assertTrue(m.contains("significance"),
                    () -> "Exception message <" + ex.getMessage() + "> missing value 'significance'");
        }
    }

    /**
     * Assert the relative error in {@code expected} vs. {@code actual} is less than
     * or equal to relativeError. Values must be in the range {@code [0, 1]}.
     *
     * <p>The error is relative to the magnitude of the {@code expected} value.
     *
     * <p>Note: This method expects exact equality at the bound, i.e. {@code expected} is 0 or 1.
     * This can be avoiding by clipping {@code expected} to [Double.MIN_VALUE, Math.nextDown(1.0)].
     *
     * @param expected Expected value
     * @param actual Observed value
     * @param relativeError Maximum allowable relative error
     * @param msg Message to return with failure
     */
    static void assertProbability(double expected,
                                  double actual,
                                  double relativeError,
                                  String msg) {
        assertProbability(expected, actual, relativeError, () -> msg);
    }

    /**
     * Assert the relative error in {@code expected} vs. {@code actual} is less than
     * or equal to relativeError. Values must be in the range {@code [0, 1]}.
     *
     * <p>The error is relative to the magnitude of the {@code expected} value.
     *
     * <p>Note: This method expects exact equality at the bound, i.e. {@code expected} is 0 or 1.
     * This can be avoiding by clipping {@code expected} to [Double.MIN_VALUE, Math.nextDown(1.0)].
     *
     * @param expected Expected value
     * @param actual Observed value
     * @param relativeError Maximum allowable relative error
     * @param msg Message to return with failure
     */
    static void assertProbability(double expected,
                                  double actual,
                                  double relativeError,
                                  Supplier<String> msg) {
        Assertions.assertTrue(0 <= expected && expected <= 1,
            () -> String.format("%sInvalid expected %s", prefix(msg), expected));
        Assertions.assertTrue(0 <= actual && actual <= 1,
            () -> String.format("%sInvalid actual %s", prefix(msg), actual));
        // Must be exact at the bound 0 or 1, or if eps is zero
        if ((int) expected == expected || relativeError == 0) {
            Assertions.assertEquals(expected, actual, msg);
        } else {
            final double absError = Math.abs(expected) * relativeError;
            Assertions.assertEquals(expected, actual, absError,
                () -> String.format("%sError %s", prefix(msg), Math.abs(expected - actual) / expected));
        }
    }

    /**
     * Assert the relative error in {@code expected} vs. {@code actual} is less than
     * or equal to relativeError. If {@code expected} is infinite or NaN, actual
     * must be the same (NaN or infinity of the same sign).
     *
     * <p>The error is relative to the magnitude of the {@code expected} value.
     *
     * @param expected Expected value
     * @param actual Observed value
     * @param relativeError Maximum allowable relative error
     * @param msg Message to return with failure
     */
    static void assertRelativelyEquals(double expected,
                                       double actual,
                                       double relativeError,
                                       String msg) {
        assertRelativelyEquals(expected, actual, relativeError, () -> msg);
    }

    /**
     * Assert the relative error in {@code expected} vs. {@code actual} is less than
     * or equal to relativeError. If {@code expected} is infinite, NaN, or zero, {@code actual}
     * must be the same.
     *
     * <p>The error is relative to the magnitude of the {@code expected} value.
     *
     * @param expected Expected value
     * @param actual Observed value
     * @param relativeError Maximum allowable relative error
     * @param msg Message to return with failure
     */
    static void assertRelativelyEquals(double expected,
                                       double actual,
                                       double relativeError,
                                       Supplier<String> msg) {
        if (Double.isFinite(expected) && expected != 0 && relativeError != 0) {
            final double absError = Math.abs(expected) * relativeError;
            Assertions.assertEquals(expected, actual, absError,
                () -> String.format("%sError %s", prefix(msg), Math.abs(expected - actual) / Math.abs(expected)));
        } else {
            Assertions.assertEquals(expected, actual, msg);
        }
    }

    /**
     * Get the prefix for the message.
     *
     * @param msg Message supplier
     * @return the prefix
     */
    private static String prefix(Supplier<String> msg) {
        return msg == null ? "" : msg.get() + ": ";
    }
}
