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
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.apache.commons.statistics.distribution.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link IntMath}.
 */
class IntMathTest {
    /** 2^63. */
    private static final BigInteger TWO_POW_63 = BigInteger.ONE.shiftLeft(63);

    @ParameterizedTest
    @MethodSource
    void testSquareHigh(long a) {
        final long actual = IntMath.squareHigh(a);
        final long expected = BigInteger.valueOf(a).pow(2).shiftRight(64).longValue();
        Assertions.assertEquals(expected, actual);
    }

    static LongStream testSquareHigh() {
        final UniformRandomProvider rng = TestHelper.createRNG();
        final Builder builder = LongStream.builder();
        builder.accept(0);
        builder.accept(Long.MAX_VALUE);
        builder.accept(Long.MIN_VALUE);
        rng.ints(5).forEach(builder::accept);
        rng.longs(50).forEach(builder::accept);
        rng.longs(10).map(x -> x >>> 1).forEach(builder::accept);
        rng.longs(10).map(x -> x >>> 2).forEach(builder::accept);
        rng.longs(10).map(x -> x >>> 5).forEach(builder::accept);
        rng.longs(10).map(x -> x >>> 13).forEach(builder::accept);
        rng.longs(10).map(x -> x >>> 35).forEach(builder::accept);
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testUnsignedMultiplyHigh(long a, long b) {
        final long actual = IntMath.unsignedMultiplyHigh(a, b);
        final BigInteger bi1 = toUnsignedBigInteger(a);
        final BigInteger bi2 = toUnsignedBigInteger(b);
        final BigInteger expected = bi1.multiply(bi2);
        Assertions.assertEquals(expected.shiftRight(Long.SIZE).longValue(), actual,
            () -> String.format("%s * %s", bi1, bi2));
        final double x = expected.doubleValue();
        Assertions.assertEquals(x, IntMath.unsignedMultiplyToDouble(a, b),
            () -> String.format("double %s * %s", bi1, bi2));
    }

    static Stream<Arguments> testUnsignedMultiplyHigh() {
        final UniformRandomProvider rng = TestHelper.createRNG();
        final Stream.Builder<Arguments> builder = Stream.builder();
        final long[] values = {
            -1, 0, 1, Long.MAX_VALUE, Long.MIN_VALUE,
            0xffL, 0xff00L, 0xff0000L, 0xff000000L,
            0xff00000000L, 0xff0000000000L, 0xff000000000000L, 0xff000000000000L,
            0xffffL, 0xffff0000L, 0xffff00000000L, 0xffff000000000000L,
            0xffffffffL, 0xffffffff00000000L
        };
        for (final long v1 : values) {
            for (final long v2 : values) {
                builder.accept(Arguments.of(v1, v2));
                builder.accept(Arguments.of(v1 >>> 15, v2 >>> 18));
            }
        }
        for (int i = 0; i < 200; i++) {
            builder.accept(Arguments.of(rng.nextLong(), rng.nextLong()));
        }
        return builder.build();
    }

    /**
     * Create a BigInteger treating the value as unsigned.
     *
     * @param v Value
     * @return the BigInteger
     */
    static BigInteger toUnsignedBigInteger(long v) {
        return v < 0 ?
            TWO_POW_63.or(BigInteger.valueOf(v & Long.MAX_VALUE)) :
            BigInteger.valueOf(v);
    }

    @ParameterizedTest
    @MethodSource
    void testUint128ToDouble(long a, long b) {
        final BigInteger bi1 = toUnsignedBigInteger(a).shiftLeft(Long.SIZE);
        final BigInteger bi2 = toUnsignedBigInteger(b);
        final double x = bi1.add(bi2).doubleValue();
        Assertions.assertEquals(x, IntMath.uint128ToDouble(a, b),
            () -> String.format("%s + %s", a, b));
    }

    static Stream<Arguments> testUint128ToDouble() {
        final UniformRandomProvider rng = TestHelper.createRNG();
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < 100; i++) {
            long a = rng.nextLong();
            long b = rng.nextLong();
            builder.accept(Arguments.of(a, b));
            builder.accept(Arguments.of(0, b));
            // Edge cases where trailing bits are required for rounding.
            // Create a 55-bit number. Ensure the highest bit is set.
            a = (a << 9) | Long.MIN_VALUE;
            // Shift right and carry bits down.
            int shift = rng.nextInt(1, 64);
            long c = a >>> shift;
            long d = a << -shift;
            // Check
            Assertions.assertEquals(Long.bitCount(a), Long.bitCount(c) + Long.bitCount(d));
            builder.accept(Arguments.of(c, d));
            // Add a trailing bit that may change rounding
            builder.accept(Arguments.of(c, d | 1));
            // Repeat for special case of a 64-bit unsigned integer
            builder.accept(Arguments.of(0, a | 1));
        }
        // At least one case where the trailing bit does effect rounding
        // 54-bits all set is an odd number + 0.5
        builder.accept(Arguments.of(1, (1L << 11)));
        builder.accept(Arguments.of(1, (1L << 11) | 1));
        // Unset the second to last bit and repeat above is an even number + 0.5
        builder.accept(Arguments.of(1, ((1L & ~0x2) << 11)));
        builder.accept(Arguments.of(1, ((1L & ~0x2) << 11) | 1));
        return builder.build();
    }

    /**
     * Test round-to-int exact matches the result generated using JDK Math functions.
     * The only exception is NaN which will round to zero (see {@link #testToIntExactNaN()}.
     *
     * <p>Note: Rounding for all types is tested in StatisticResultTest.
     */
    @ParameterizedTest
    @MethodSource
    @ValueSource(doubles = {-1.265384, 67.346578, 72893.5, -42.678,
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void testToIntExact(double x) {
        for (final int sign : new int[] {-1, 1}) {
            final double y = x * sign;
            final Supplier<String> intMsg = () -> String.valueOf(y);
            Integer i = null;
            try {
                i = Math.toIntExact(Math.round(y));
            } catch (Throwable t) {
                Assertions.assertThrowsExactly(t.getClass(), () -> IntMath.toIntExact(y), intMsg);
            }
            if (i != null) {
                Assertions.assertEquals(i.intValue(), IntMath.toIntExact(y), intMsg);
            }
        }
    }

    static DoubleStream testToIntExact() {
        DoubleStream.Builder builder = DoubleStream.builder();
        for (double x = 0; x < 3.5; x += 0.25) {
            builder.accept(x);
        }
        for (double x = -1.5; x <= 1.5; x += 0.25) {
            builder.accept(x + Integer.MAX_VALUE);
            builder.accept(x + Integer.MIN_VALUE);
        }
        return builder.build();
    }

    /**
     * Test round-to-int exact does not match the result generated using JDK Math functions
     * for NaN.
     *
     * <p>Note: Rounding for all types is tested in StatisticResultTest.
     */
    @Test
    void testToIntExactNaN() {
        final double x = Double.NaN;
        Assertions.assertEquals(0, Math.toIntExact(Math.round(x)));
        Assertions.assertThrowsExactly(ArithmeticException.class, () -> IntMath.toIntExact(x));
    }

    @ParameterizedTest
    @MethodSource
    void testInt128Divide(long a, long b, long n) {
        final Int128 x = new Int128(a, b);
        final BigInteger bi = BigInteger.valueOf(a).shiftLeft(Long.SIZE).add(BigInteger.valueOf(b));
        final double expected = new BigDecimal(bi).divide(
            new BigDecimal(n), MathContext.DECIMAL128).doubleValue();
        // This may require a 1 ulp tolerance; log the seed to allow repeats
        TestUtils.assertEquals(expected, IntMath.divide(x, n), DoubleTolerances.equals(),
            () -> String.format("(2^64 * %d + %d) / %d; Seed=%s",
                a, b, n, Arrays.toString(TestHelper.createRNGSeed())));
    }

    static Stream<Arguments> testInt128Divide() {
        final UniformRandomProvider rng = TestHelper.createRNG(TestHelper.createRNGSeed());
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < 100; i++) {
            final long a = rng.nextLong();
            final long b = rng.nextLong();
            final long n = rng.nextLong();
            builder.accept(Arguments.of(a, b, n >>> 1));
            builder.accept(Arguments.of(a, b, n >>> 43));
        }
        return builder.build();
    }
}
