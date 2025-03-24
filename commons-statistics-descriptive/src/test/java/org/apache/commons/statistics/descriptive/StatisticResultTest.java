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
import java.math.RoundingMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link StatisticResult}.
 *
 * <p>Note: Rounding
 *
 * <p>Use of {@link Math#rint(double)} will round ties to the nearest even number.
 * This may not be desirable. {@link Math#round(double)} will round ties to positive infinity
 * which leads to different rounding for positive and negatives; it also returns a {@code long}.
 * Casting will discard the fractional part and may not return the closest integer.
 * Using a rounding mode of half-up is consistent across signs and ties. However it does not
 * create a result invariant to integer shift.
 * <pre>
 * value     rint     round      cast     RoundingMode.HALF_UP
 * -3.5      4        -3         -3       -4
 * -2.5      2        -2         -2       -3
 * -1.5      2        -1         -1       -2
 * -0.5      0        0          0        -1
 * 0.0       0        0          0        0
 * 0.5       0        1          0        1
 * 1.5       2        2          1        2
 * 2.5       2        3          2        3
 * 3.5       4        4          3        4
 * </pre>
 *
 * <p>Rounding uses the equivalent of {@link Math#round(double)}. A test asserts that the result
 * is invariant to shift.
 */
final class StatisticResultTest {
    private static final int[] SIGNS = {-1, 1};

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void testNonFinite(double x) {
        final StatisticResult r = () -> x;
        Assertions.assertThrows(ArithmeticException.class, r::getAsInt, "int result: " + x);
        Assertions.assertThrows(ArithmeticException.class, r::getAsLong, "long result: " + x);
        Assertions.assertThrows(ArithmeticException.class, r::getAsBigInteger, "BigInteger result: " + x);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.25, 2.5, 2.75, 3,
        Integer.MAX_VALUE - 0.75, Integer.MAX_VALUE - 0.5, Integer.MAX_VALUE - 0.25,
        Integer.MAX_VALUE, Integer.MAX_VALUE + 0.25,
        Integer.MIN_VALUE - 0.5, Integer.MIN_VALUE - 0.25, Integer.MIN_VALUE,
        Integer.MIN_VALUE + 0.25, Integer.MIN_VALUE + 0.5, Integer.MIN_VALUE + 0.75 })
    void testRepresentableInt(double x) {
        // Do not test both signs for the large negative integers
        final int[] signs = x < 0 ? new int[] {1} : SIGNS;
        for (final int sign : signs) {
            final double y = x * sign;
            final StatisticResult r = () -> y;
            final BigDecimal expected = round(y);
            Assertions.assertEquals(expected.intValue(), r.getAsInt(), () -> "int result: " + y);
            Assertions.assertEquals(expected.longValue(), r.getAsLong(), () -> "long result: " + y);
            Assertions.assertEquals(expected.toBigInteger(), r.getAsBigInteger(), () -> "BigInteger result: " + y);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {
        Integer.MAX_VALUE + 0.5, Integer.MAX_VALUE + 1.25,
        Integer.MIN_VALUE - 0.75, Integer.MIN_VALUE - 1.75,
        Long.MIN_VALUE,
        // Cannot represent Long.MAX_VALUE. Use the next value down:
        // Math.nextDown(0x1.0p63)
        9.223372036854775E18})
    void testNonRepresentableInt(double x) {
        final StatisticResult r = () -> x;
        final BigDecimal expected = round(x);
        Assertions.assertThrows(ArithmeticException.class, r::getAsInt, () -> "int result: " + x);
        Assertions.assertEquals(expected.longValue(), r.getAsLong(), () -> "long result: " + x);
        Assertions.assertEquals(expected.toBigInteger(), r.getAsBigInteger(), () -> "BigInteger result: " + x);
    }

    @ParameterizedTest
    @ValueSource(doubles = {
        Long.MAX_VALUE,
        // Math.nextDown((double) Long.MIN_VALUE)
        -9.223372036854778E18,
        Double.MAX_VALUE,
        -Double.MAX_VALUE,
        })
    void testNonRepresentableLong(double x) {
        final StatisticResult r = () -> x;
        final BigDecimal expected = round(x);
        Assertions.assertThrows(ArithmeticException.class, r::getAsInt, () -> "int result: " + x);
        Assertions.assertThrows(ArithmeticException.class, r::getAsLong, () -> "long result: " + x);
        Assertions.assertEquals(expected.toBigInteger(), r.getAsBigInteger(), () -> "BigInteger result: " + x);
    }

    /**
     * Round the value to the nearest integer, with ties rounding towards positive infinity.
     * This matches the rounding of {@link Math#round(double)} but returns a floating-point result.
     *
     * @param x Value.
     * @return the rounded result
     */
    private static BigDecimal round(double x) {
        return x < 0 ?
            new BigDecimal(x).setScale(0, RoundingMode.HALF_DOWN) :
            new BigDecimal(x).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Test the rounding of the double is invariant to shift. This is true if the rounding of
     * ties is in a consistent direction.
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1.75, -1.5, -1.25, -1, -0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75})
    void testShift(double x) {
        final StatisticResult expected = () -> x;
        for (final int shift : new int[] {-566, 42}) {
            final StatisticResult r = () -> x + shift;
            Assertions.assertEquals(expected.getAsInt() + shift, r.getAsInt(), () -> "int result: " + x + " + " + shift);
            Assertions.assertEquals(expected.getAsLong() + shift, r.getAsLong(), () -> "long result: " + x + " + " + shift);
            Assertions.assertEquals(expected.getAsBigInteger().add(BigInteger.valueOf(shift)),
                r.getAsBigInteger(), () -> "BigInteger result: " + x + " + " + shift);
        }
    }

    /**
     * Test the rounding of a double is equivalent to {@link Math#round(double)}.
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1.75, -1.5, -1.25, -1, -0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75})
    void testMatchesMathRound(double x) {
        final StatisticResult r = () -> x;
        final long expected = Math.round(x);
        Assertions.assertEquals((int) expected, r.getAsInt(), () -> "int result: " + x);
        Assertions.assertEquals(expected, r.getAsLong(), () -> "long result: " + x);
        Assertions.assertEquals(BigInteger.valueOf(expected), r.getAsBigInteger(), () -> "BigInteger result: " + x);
    }
}
