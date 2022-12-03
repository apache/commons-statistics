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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
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
    /** Positive zero bits. */
    private static final long POSITIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(+0.0);
    /** Negative zero bits. */
    private static final long NEGATIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(-0.0);
    /** Set this to true to report all deviations to System out when the maximum ULPs is negative. */
    private static boolean reportAllDeviations = false;
    /** 2 as a BigDecimal. Used for scaling. */
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    /** 1/2 as a BigDecimal. Used for scaling. */
    private static final BigDecimal HALF = new BigDecimal(0.5);
    /** MathContext used for BigDecimal scaling. */
    private static final MathContext MC_SCALING = new MathContext(2 * MathContext.DECIMAL128.getPrecision());

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
     * Assert the two numbers are equal within the provided relative error.
     *
     * <p>The provided error is relative to the exact result in expected: (e - a) / e.
     * If expected is zero this division is undefined. In this case the actual must be zero
     * (no absolute tolerance is supported). The reporting of the error uses the absolute
     * error and the return value of the relative error is 0. Cases of complete cancellation
     * should be avoided for benchmarking relative accuracy.
     *
     * <p>Note that the actual double-double result is not validated using the high and low
     * parts individually. These are summed and compared to the expected.
     *
     * <p>Set {@code eps} to negative to report the relative error to the stdout and
     * ignore failures.
     *
     * <p>The relative error is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param eps maximum relative error between the two values
     * @param msg failure message
     * @return relative error difference between the values (signed)
     * @throws NumberFormatException if {@code actual} contains non-finite values
     */
    static double assertEquals(BigDecimal expected, DD actual, double eps, String msg) {
        return assertEquals(expected, actual, eps, null, () -> msg);
    }

    /**
     * Assert the two numbers are equal within the provided relative error.
     *
     * <p>The provided error is relative to the exact result in expected: (e - a) / e.
     * If expected is zero this division is undefined. In this case the actual must be zero
     * (no absolute tolerance is supported). The reporting of the error uses the absolute
     * error and the return value of the relative error is 0. Cases of complete cancellation
     * should be avoided for benchmarking relative accuracy.
     *
     * <p>Note that the actual double-double result is not validated using the high and low
     * parts individually. These are summed and compared to the expected.
     *
     * <p>Set {@code eps} to negative to report the relative error to the stdout and
     * ignore failures.
     *
     * <p>The relative error is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param eps maximum relative error between the two values
     * @param msg failure message
     * @return relative error difference between the values (signed)
     * @throws NumberFormatException if {@code actual} contains non-finite values
     */
    static double assertEquals(BigDecimal expected, DD actual, double eps, Supplier<String> msg) {
        return assertEquals(expected, actual, eps, null, msg);
    }

    /**
     * Assert the two numbers are equal within the provided relative error.
     *
     * <p>The provided error is relative to the exact result in expected: (e - a) / e.
     * If expected is zero this division is undefined. In this case the actual must be zero
     * (no absolute tolerance is supported). The reporting of the error uses the absolute
     * error and the return value of the relative error is 0. Cases of complete cancellation
     * should be avoided for benchmarking relative accuracy.
     *
     * <p>Note that the actual double-double result is not validated using the high and low
     * parts individually. These are summed and compared to the expected.
     *
     * <p>Set {@code eps} to negative to report the relative error to the stdout and
     * ignore failures.
     *
     * <p>The relative error is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param eps maximum relative error between the two values
     * @param error Consumer for the relative error difference between the values (signed)
     * @param msg failure message
     * @return relative error difference between the values (signed)
     * @throws NumberFormatException if {@code actual} contains non-finite values
     */
    static double assertEquals(BigDecimal expected, DD actual, double eps,
            DoubleConsumer error, Supplier<String> msg) {
        // actual - expected
        final BigDecimal delta = new BigDecimal(actual.hi())
            .add(new BigDecimal(actual.lo()))
            .subtract(expected);
        boolean equal;
        if (expected.compareTo(BigDecimal.ZERO) == 0) {
            // Edge case. Current an absolute tolerance is not supported as summation
            // to zero cases generated in testing all pass.
            equal = actual.doubleValue() == 0;

            // DEBUG:
            if (eps < 0) {
                if (!equal || reportAllDeviations) {
                    printf("%sexpected 0 != actual <%s + %s> (abs.error=%s)%n",
                        prefix(msg), actual.hi(), actual.lo(), delta.doubleValue());
                }
            } else if (!equal) {
                Assertions.fail(String.format("%sexpected 0 != actual <%s + %s> (abs.error=%s)",
                    prefix(msg), actual.hi(), actual.lo(), delta.doubleValue()));
            }

            return 0;
        }

        final double rel = delta.divide(expected, MathContext.DECIMAL128).doubleValue();
        // Allow input of a negative maximum ULPs
        equal = Math.abs(rel) <= Math.abs(eps);

        if (error != null) {
            error.accept(rel);
        }

        // DEBUG:
        if (eps < 0) {
            if (!equal || reportAllDeviations) {
                printf("%sexpected <%s> != actual <%s + %s> (rel.error=%s (%.2f x tol))%n",
                    prefix(msg), expected.round(MathContext.DECIMAL128), actual.hi(), actual.lo(),
                    rel, Math.abs(rel) / eps);
            }
        } else if (!equal) {
            Assertions.fail(String.format("%sexpected <%s> != actual <%s + %s> (rel.error=%s (%.2f x tol))",
                prefix(msg), expected.round(MathContext.DECIMAL128), actual.hi(), actual.lo(),
                rel, Math.abs(rel) / eps));
        }

        return rel;
    }

    /**
     * Assert the two numbers are equal within the provided relative error.
     *
     * <p>Scales the BigDecimal result by the provided exponent and then uses
     * {@link TestUtils#assertEquals(BigDecimal, DD, double, Supplier)}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param exp the scale factor of the actual value (2^exp)
     * @param eps maximum relative error between the two values
     * @param msg failure message
     * @return relative error difference between the values (signed)
     * @throws NumberFormatException if {@code actual} contains non-finite values
     */
    static double assertScaledEquals(BigDecimal expected, DD actual, long exp, double eps, Supplier<String> msg) {
        Assertions.assertEquals(exp, (int) exp, () -> prefix(msg) + "Cannot scale the result");
        BigDecimal e = expected;
        if (exp < 0) {
            e = e.multiply(TWO.pow((int) -exp), MC_SCALING);
        } else {
            e = e.multiply(HALF.pow((int) exp), MC_SCALING);
        }
        return TestUtils.assertEquals(e, actual, eps, () -> prefix(msg) + "scale=2^" + exp);
    }

    // ULP assertions copied from o.a.c.numbers.gamma.TestUtils

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * <p>The values -0.0 and 0.0 are considered equal.
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and ignore
     * failures.
     *
     * <p>The ulp difference is signed. It may be truncated to +/-Long.MAX_VALUE. Use of
     * {@link Math#abs(long)} on the value will always be positive. The sign of the error
     * is the same as that returned from Double.compare(actual, expected).
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @return ulp difference between the values (signed; may be truncated to +/-Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps) {
        return assertEquals(expected, actual, maxUlps, null, (Supplier<String>) null);
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * <p>The values -0.0 and 0.0 are considered equal.
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and ignore
     * failures.
     *
     * <p>The ulp difference is signed. It may be truncated to +/-Long.MAX_VALUE. Use of
     * {@link Math#abs(long)} on the value will always be positive. The sign of the error
     * is the same as that returned from Double.compare(actual, expected).
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param msg failure message
     * @return ulp difference between the values (signed; may be truncated to +/-Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps, String msg) {
        return assertEquals(expected, actual, maxUlps, null, () -> msg);
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * <p>The values -0.0 and 0.0 are considered equal.
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and ignore
     * failures.
     *
     * <p>The ulp difference is signed. It may be truncated to +/-Long.MAX_VALUE. Use of
     * {@link Math#abs(long)} on the value will always be positive. The sign of the error
     * is the same as that returned from Double.compare(actual, expected).
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param msg failure message
     * @return ulp difference between the values (signed; may be truncated to +/-Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps, Supplier<String> msg) {
        return assertEquals(expected, actual, maxUlps, null, msg);
    }

    /**
     * Assert the two numbers are equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * <p>The values -0.0 and 0.0 are considered equal.
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and
     * ignore failures.
     *
     * <p>The ulp difference is signed. It may be truncated to +/-Long.MAX_VALUE. Use of
     * {@link Math#abs(long)} on the value will always be positive. The sign of the error
     * is the same as that returned from Double.compare(actual, expected).
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param error Consumer for the ulp difference between the values (signed)
     * @param msg failure message
     * @return ulp difference between the values (signed; may be truncated to +/-Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps,
            LongConsumer error, Supplier<String> msg) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);

        // Code adapted from Precision#equals(double, double, int) so we maintain the delta
        // for the message and return it for reporting. The sign is maintained separately
        // to allow reporting errors above Long.MAX_VALUE.

        int sign;
        long delta;
        boolean equal;
        if (e == a) {
            // Binary equal
            equal = true;
            sign = 0;
            delta = 0;
        } else if ((a ^ e) < 0L) {
            // The difference is the count of numbers between each and zero.
            // This makes -0.0 and 0.0 equal.
            long d1;
            long d2;
            if (a < e) {
                sign = -1;
                d1 = e - POSITIVE_ZERO_DOUBLE_BITS;
                d2 = a - NEGATIVE_ZERO_DOUBLE_BITS;
            } else {
                sign = 1;
                d1 = a - POSITIVE_ZERO_DOUBLE_BITS;
                d2 = e - NEGATIVE_ZERO_DOUBLE_BITS;
            }
            // This may overflow but we report it using an unsigned formatter.
            delta = d1 + d2;
            if (delta < 0) {
                // Overflow
                equal = false;
            } else {
                // Allow input of a negative maximum ULPs
                equal = delta <= ((maxUlps < 0) ? (-maxUlps - 1) : maxUlps);
            }
        } else {
            if (a < e) {
                sign = -1;
                delta = e - a;
            } else {
                sign = 1;
                delta = a - e;
            }
            // The sign must be negated for negative doubles since the magnitude
            // comparison (a < e) included the sign bit.
            sign = a < 0 ? -sign : sign;

            // Allow input of a negative maximum ULPs
            equal = delta <= ((maxUlps < 0) ? (-maxUlps - 1) : maxUlps);
        }

        assert sign == Double.compare(actual, expected);

        // DEBUG:
        if (maxUlps < 0) {
            if (!equal || reportAllDeviations) {
                printf("%sexpected <%s> != actual <%s> (ulps=%c%s)%n",
                    prefix(msg), expected, actual, sign < 0 ? '-' : ' ', Long.toUnsignedString(delta));
            }
        } else if (!equal) {
            Assertions.fail(String.format("%sexpected <%s> != actual <%s> (ulps=%c%s)",
                prefix(msg), expected, actual, sign < 0 ? '-' : ' ', Long.toUnsignedString(delta)));
        }

        // This may have overflowed.
        delta = delta < 0 ? Long.MAX_VALUE : delta;
        delta *= sign;
        if (error != null) {
            error.accept(delta);
        }
        return delta;
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
