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
package org.apache.commons.statistics.descriptive;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.TestUtils;
import org.junit.jupiter.api.Assertions;

/**
 * Helper class for tests in {@code o.a.c.s.descriptive} module.
 */
final class TestHelper {
    /**
     * Cached seed. Using the same seed ensures all statistics use the same shuffled
     * data for tests executed in the same JVM.
     * Native seed for XO_RO_SHI_RO_128_PP is long[] (size 2).
     */
    private static final long[] SEED = RandomSource.createLongArray(2);

    /**
     * A {@link DoubleTolerance} that considers finite values equal using the {@code ==} operator,
     * and all non-finite values equal.
     */
    private static final class EqualsNonFinite implements DoubleTolerance, Supplier<String> {
        /** An instance. */
        static final EqualsNonFinite INSTANCE = new EqualsNonFinite();

        @Override
        public boolean test(double a, double b) {
            return Double.isFinite(a) ?
                a == b :
                !Double.isFinite(b);
        }

        @Override
        public String get() {
            // Note: This method provides an assertion message for the tolerance
            return "(== or non-finite match)";
        }
    }

    /** Class contains only static methods. */
    private TestHelper() {}

    /**
     * Helper function to concatenate arrays.
     *
     * @param arrays Arrays to be concatenated.
     * @return A new array containing elements from all input arrays in the order they appear.
     */
    static double[] concatenate(double[]... arrays) {
        return Arrays.stream(arrays)
                .flatMapToDouble(Arrays::stream)
                .toArray();
    }

    /**
     * Helper function to concatenate arrays.
     *
     * @param arrays Arrays to be concatenated.
     * @return A new array containing elements from all input arrays in the order they appear.
     */
    static int[] concatenate(int[]... arrays) {
        return Arrays.stream(arrays)
                .flatMapToInt(Arrays::stream)
                .toArray();
    }

    /**
     * Helper function to concatenate arrays.
     *
     * @param arrays Arrays to be concatenated.
     * @return A new array containing elements from all input arrays in the order they appear.
     */
    static long[] concatenate(long[]... arrays) {
        return Arrays.stream(arrays)
                .flatMapToLong(Arrays::stream)
                .toArray();
    }

    /**
     * Helper function to reverse the concatenation of arrays. The array is copied
     * sequentially into the destination arrays.
     *
     * <p>Warning: No lengths checks are performed. It is assume the combine length
     * of the destination arrays is equal to the length of the data.
     *
     * @param data Array to be unconcatenated.
     * @param arrays Destination arrays.
     */
    static void unconcatenate(double[] data, double[]... arrays) {
        int from = 0;
        for (final double[] a : arrays) {
            System.arraycopy(data, from, a, 0, a.length);
            from += a.length;
        }
    }

    /**
     * Helper function to reverse the concatenation of arrays. The array is copied
     * sequentially into the destination arrays.
     *
     * <p>Warning: No lengths checks are performed. It is assume the combine length
     * of the destination arrays is equal to the length of the data.
     *
     * @param data Array to be unconcatenated.
     * @param arrays Destination arrays.
     */
    static void unconcatenate(int[] data, int[]... arrays) {
        int from = 0;
        for (final int[] a : arrays) {
            System.arraycopy(data, from, a, 0, a.length);
            from += a.length;
        }
    }

    /**
     * Helper function to reverse the concatenation of arrays. The array is copied
     * sequentially into the destination arrays.
     *
     * <p>Warning: No lengths checks are performed. It is assume the combine length
     * of the destination arrays is equal to the length of the data.
     *
     * @param data Array to be unconcatenated.
     * @param arrays Destination arrays.
     */
    static void unconcatenate(long[] data, long[]... arrays) {
        int from = 0;
        for (final long[] a : arrays) {
            System.arraycopy(data, from, a, 0, a.length);
            from += a.length;
        }
    }

    /**
     * Helper function to compute the expected value of Mean using BigDecimal.
     * @param values Values.
     * @return Mean of values rounded to <a href = "https://en.wikipedia.org/wiki/Decimal128_floating-point_format"> DECIMAL128 precision</a>.
     */
    static BigDecimal computeExpectedMean(double[] values) {
        BigDecimal bd = BigDecimal.ZERO;
        for (double value : values) {
            bd = bd.add(new BigDecimal(value));
        }
        return bd.divide(BigDecimal.valueOf(values.length), MathContext.DECIMAL128);
    }

    /**
     * Compute expected sum-of-square deviations using BigDecimal.
     * Uses the mean from {@link #computeExpectedMean(double[])}.
     * This is optionally returned.
     *
     * <p>Note: The values and result are limited to {@link MathContext#DECIMAL128} precision.
     *
     * @param values Values.
     * @param mean Mean (result). Only computed if {@code length > 1}.
     * @return sum-of-square deviations
     */
    static BigDecimal computeExpectedSumOfSquaredDeviations(double[] values, BigDecimal[] mean) {
        long n = values.length;
        if (n <= 1) {
            return BigDecimal.ZERO;
        }
        final BigDecimal m = computeExpectedMean(values);
        if (mean != null) {
            mean[0] = m;
        }
        BigDecimal ss = BigDecimal.ZERO;
        for (double value : values) {
            // Note: The mean is returned in DECIMAL128 precision.
            // Truncate the double value to the same precision.
            // This avoids round-off issues with extreme values such as Double.MAX_VALUE
            // that have a very large BigInteger representation,
            // i.e. the entire computation must use the same precision.
            BigDecimal bdDiff = new BigDecimal(value, MathContext.DECIMAL128);
            bdDiff = bdDiff.subtract(m);
            bdDiff = bdDiff.pow(2);
            // Note: This is a sum of positive terms so summation with rounding is OK.
            ss = ss.add(bdDiff, MathContext.DECIMAL128);
        }
        return ss;
    }

    /**
     * Create a tolerance using the provided tolerance for finite values; all non-finite
     * values are considered equal.
     *
     * @param tol Finite value tolerance.
     * @return the tolerance
     */
    static DoubleTolerance equalsOrNonFinite(DoubleTolerance tol) {
        return tol.or(EqualsNonFinite.INSTANCE);
    }

    /**
     * Creates a seed for the RNG.
     *
     * @return the seed
     * @see #createRNG(long[])
     */
    static long[] createRNGSeed() {
        // This could be generated dynamically.
        // However using the same seed across all tests ensures:
        // 1. Related statistics use the same shuffled data, e.g. Mean and Variance.
        // 2. Different tests of the same statistic use the same shuffle data.
        // This allows settings test tolerances appropriately across the test suite.
        return SEED.clone();
    }

    /**
     * Creates a RNG instance.
     *
     * @param seed Seed.
     * @return A new RNG instance.
     * @see #createRNGSeed()
     */
    static UniformRandomProvider createRNG(long[] seed) {
        return RandomSource.XO_RO_SHI_RO_128_PP.create(seed);
    }

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Uses Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * <p>TODO: This can be removed when {@code commons-rng-sampling 1.6} is released.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static double[] shuffle(UniformRandomProvider rng, double[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Uses Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * <p>TODO: This can be removed when {@code commons-rng-sampling 1.6} is released.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static int[] shuffle(UniformRandomProvider rng, int[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Uses Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * <p>TODO: This can be removed when {@code commons-rng-sampling 1.6} is released.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static long[] shuffle(UniformRandomProvider rng, long[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Uses Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * <p>TODO: This can be removed when {@code commons-rng-sampling 1.6} is released.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static <T> T[] shuffle(UniformRandomProvider rng, T[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(int[] array, int i, int j) {
        final int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(long[] array, int i, int j) {
        final long tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static <T> void swap(T[] array, int i, int j) {
        final T tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Assert the results are equal. All result types are compared.
     *
     * <p>If the tolerance is null then it is assumed the result is an exact integer type;
     * the double value is compared using binary equality.
     *
     * <p>If the tolerance is not null then it is assumed the result is a {@code double} type;
     * the BigInteger value is compared using its {@code double} representation if the
     * actual BigInteger result is not equal.
     *
     * <p>If the {@code expected} throws an exception, then the {@code actual} is asserted
     * to throw an exception of the same class.
     *
     * @param expected Expected.
     * @param actual Actual.
     * @param tol Tolerance for double equality.
     * @param msg Message used for failure.
     */
    static void assertEquals(StatisticResult expected, StatisticResult actual, DoubleTolerance tol,
            Supplier<String> msg) {
        final Supplier<String> intMsg = () -> prefix(msg) + " int value";
        Integer i = null;
        try {
            i = expected.getAsInt();
        } catch (Throwable t) {
            Assertions.assertThrowsExactly(t.getClass(), () -> actual.getAsInt(), intMsg);
        }
        if (i != null) {
            Assertions.assertEquals(i.intValue(), actual.getAsInt(), intMsg);
        }

        final Supplier<String> longMsg = () -> prefix(msg) + " long value";
        Long l = null;
        try {
            l = expected.getAsLong();
        } catch (Throwable t) {
            Assertions.assertThrowsExactly(t.getClass(), () -> actual.getAsLong(), longMsg);
        }
        if (l != null) {
            Assertions.assertEquals(l.longValue(), actual.getAsLong(), longMsg);
        }

        final Supplier<String> doubleMsg = () -> prefix(msg) + " double value";
        Double d = null;
        try {
            d = expected.getAsDouble();
        } catch (Throwable t) {
            Assertions.assertThrowsExactly(t.getClass(), () -> actual.getAsDouble(), doubleMsg);
        }
        if (d != null) {
            if (tol == null) {
                Assertions.assertEquals(d, actual.getAsDouble(), doubleMsg);
            } else {
                TestUtils.assertEquals(d, actual.getAsDouble(), tol, doubleMsg);
            }
        }

        final Supplier<String> bigMsg = () -> prefix(msg) + " BigInteger value";
        BigInteger b = null;
        try {
            b = expected.getAsBigInteger();
        } catch (Throwable t) {
            Assertions.assertThrowsExactly(t.getClass(), () -> actual.getAsBigInteger(), bigMsg);
        }
        if (b != null) {
            if (tol == null) {
                // Assume exact
                Assertions.assertEquals(b, actual.getAsBigInteger(), bigMsg);
            } else {
                // Double computation may not be exact so check within tolerance
                final BigInteger bb = actual.getAsBigInteger();
                if (!b.equals(bb)) {
                    TestUtils.assertEquals(b.doubleValue(), bb.doubleValue(), tol, bigMsg);
                }
            }
        }
    }

    // DD equality checks adapted from o.a.c.numbers.core.TestUtils

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
        return assertEquals(expected, actual, eps, () -> msg);
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
        // actual - expected
        final BigDecimal delta = new BigDecimal(actual.hi())
            .add(new BigDecimal(actual.lo()))
            .subtract(expected);
        boolean equal;
        if (expected.compareTo(BigDecimal.ZERO) == 0) {
            // Edge case. Currently an absolute tolerance is not supported as summation
            // to zero cases generated in testing all pass.
            equal = actual.doubleValue() == 0;

            // DEBUG:
            if (eps < 0) {
                if (!equal) {
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

        // DEBUG:
        if (eps < 0) {
            if (!equal) {
                printf("%sexpected <%s> != actual <%s + %s> (rel.error=%s (%.3f x tol))%n",
                    prefix(msg), expected.round(MathContext.DECIMAL128), actual.hi(), actual.lo(),
                    rel, Math.abs(rel) / eps);
            }
        } else if (!equal) {
            Assertions.fail(String.format("%sexpected <%s> != actual <%s + %s> (rel.error=%s (%.3f x tol))",
                prefix(msg), expected.round(MathContext.DECIMAL128), actual.hi(), actual.lo(),
                rel, Math.abs(rel) / eps));
        }

        return rel;
    }

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
     * Get the prefix for the message.
     *
     * @param msg Message supplier
     * @return the prefix
     */
    static String prefix(Supplier<String> msg) {
        return msg == null ? "" : msg.get() + ": ";
    }
}
