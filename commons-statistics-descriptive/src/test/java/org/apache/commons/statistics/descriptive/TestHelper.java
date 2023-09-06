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
import java.math.MathContext;
import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.commons.numbers.core.Precision;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;

/**
 * Helper class for tests in {@code o.a.c.s.descriptive} module.
 */
final class TestHelper {
    /** Positive zero bits. */
    private static final long POSITIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(+0.0);
    /** Negative zero bits. */
    private static final long NEGATIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(-0.0);

    /** Class contains only static methods. */
    private TestHelper() {}

    /**
     * Helper function to concatenate arrays.
     * @param arrays Arrays to be concatenated.
     * @return A new array containing elements from all input arrays in the order they appear.
     */
    static double[] concatenate(double[]... arrays) {
        return Arrays.stream(arrays)
                .flatMapToDouble(Arrays::stream)
                .toArray();
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
     * Helper function to assert that {@code actual} is equal to {@code expected} as defined
     * by {@link org.apache.commons.numbers.core.Precision#equals(double, double, int)
     * Precision.equals(x, y, maxUlps)}.
     *
     * <p> Note: This uses {@code ULP} tolerance only if both {@code actual} and {@code expected} are finite.
     * Otherwise, it uses a binary equality through {@link Assertions#assertEquals(double, double, String)
     * Assertions.assertEquals(expected, actual, message)}.
     *
     * @param expected expected value.
     * @param actual actual value.
     * @param ulps {@code (ulps - 1)} is the number of floating point
     * values between {@code actual} and {@code expected}.
     * @param msg additional debug message to log when the assertion fails.
     */
    static void assertEquals(double expected, double actual, int ulps, Supplier<String> msg) {
        // Require strict equivalence of non-finite values
        if (Double.isFinite(expected) && Double.isFinite(actual)) {
            if (!Precision.equals(expected, actual, ulps)) {
                Assertions.fail(() -> msg.get() + ": " + expected + " != " + actual +
                    " within " + ulps + " ulp(s); difference = " + formatUlpDifference(expected, actual));
            }
        } else {
            Assertions.assertEquals(expected, actual, msg);
        }
    }

    /**
     * Format the difference in ULP between two arguments. This will return "0" for values
     * that are binary equal, or for the difference between zeros of opposite signs.
     *
     * @param a first argument
     * @param b second argument
     * @return Signed ULP difference between the arguments as a string
     */
    private static String formatUlpDifference(double expected, double actual) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);

        // Code adapted from Precision#equals(double, double, int).
        // Compute the absolute delta; the sign is maintained separately
        // to allow reporting errors above Long.MAX_VALUE.

        if (e == a) {
            // Binary equal
            return "0";
        }
        int sign;
        long delta;
        if ((a ^ e) < 0L) {
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
        } else {
            if (a < e) {
                sign = -1;
                delta = e - a;
            } else {
                sign = 1;
                delta = a - e;
            }
        }
        return sign < 0 ?
            "-" + Long.toUnsignedString(delta) :
            Long.toUnsignedString(delta);
    }

    /**
     * Creates a RNG instance.
     *
     * @return A new RNG instance.
     */
    static UniformRandomProvider createRNG() {
        return RandomSource.SPLIT_MIX_64.create();
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
}
