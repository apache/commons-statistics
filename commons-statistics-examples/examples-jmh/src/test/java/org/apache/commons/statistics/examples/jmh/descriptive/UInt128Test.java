/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.statistics.examples.jmh.descriptive;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link UInt128}.
 */
class UInt128Test {
    @Test
    void testCreate() {
        final UInt128 v = UInt128.create();
        Assertions.assertEquals(BigInteger.ZERO, v.toBigInteger());
    }

    @Test
    void testAddLongMinValue() {
        final UInt128 v = UInt128.of(1268361283468345237L);
        final BigInteger x = BigInteger.ONE.shiftLeft(63);
        BigInteger expected = v.toBigInteger();
        for (int i = 1; i <= 5; i++) {
            // Accepts a negative value without exception. This is
            // computed correctly if the current low 32 bits
            // added to the argument do not overflow. This is always
            // true for min value as all lower 32-bits are zero.
            v.addPositive(Long.MIN_VALUE);
            expected = expected.add(x);
            Assertions.assertEquals(expected, v.toBigInteger());
        }
    }

    @ParameterizedTest
    @MethodSource
    void testAddLong(long a, long b) {
        final BigInteger expected = BigInteger.valueOf(a).add(BigInteger.valueOf(b));
        final UInt128 v = UInt128.of(a);
        v.addPositive(b);
        Assertions.assertEquals(expected, v.toBigInteger());
    }

    static Stream<Arguments> testAddLong() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final long[] x = {0, 1, Long.MAX_VALUE, 612783421678L, 42};
        for (final long i : x) {
            for (final long j : x) {
                builder.accept(Arguments.of(i, j));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testAddLongs(long[] a) {
        final BigInteger expected = Arrays.stream(a).mapToObj(BigInteger::valueOf)
            .reduce(BigInteger::add).orElse(BigInteger.ZERO);
        final UInt128 v = UInt128.create();
        for (final long x : a) {
            Assertions.assertFalse(x < 0, "Value must be positive");
            v.addPositive(x);
        }
        Assertions.assertEquals(expected, v.toBigInteger());
        // Check floating-point representation
        TestUtils.assertEquals(new BigDecimal(expected), v.toDD(), 0x1.0p-106, "DD");
        Assertions.assertEquals(expected.doubleValue(), v.toDouble(), "double");
    }

    static Stream<Arguments> testAddLongs() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (final int n : new int[] {50, 100}) {
            builder.accept(Arguments.of(rng.longs(n).map(x -> x >>> 1).toArray()));
            builder.accept(Arguments.of(rng.longs(n).map(x -> x >>> 2).toArray()));
            builder.accept(Arguments.of(rng.longs(n).map(x -> x >>> 4).toArray()));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testAddInt128(long a, long b, long c, long d) {
        final UInt128 x = new UInt128(a, b);
        final UInt128 y = new UInt128(c, d);
        Assertions.assertEquals(a, x.hi64());
        Assertions.assertEquals(b, x.lo64());
        BigInteger expected = x.toBigInteger().add(y.toBigInteger());
        // The result is an unsigned 128-bit integer.
        // This is subject to integer overflow.
        // Clip the unlimited BigInteger result to the range [0, 2^128).
        if (expected.testBit(128)) {
            expected = expected.flipBit(128);
        }
        x.add(y);
        Assertions.assertEquals(expected, x.toBigInteger(),
            () -> String.format("(%d, %d) + (%d, %d)", a, b, c, d));
        // Check floating-point representation
        TestUtils.assertEquals(new BigDecimal(expected), x.toDD(), 0x1.0p-106, "DD");
        Assertions.assertEquals(expected.doubleValue(), x.toDouble(), "double");
        // Check self-addition
        expected = y.toBigInteger();
        expected = expected.add(expected);
        if (expected.testBit(128)) {
            expected = expected.flipBit(128);
        }
        y.add(y);
        Assertions.assertEquals(expected, y.toBigInteger(),
            () -> String.format("(%d, %d) self-addition", c, d));
    }

    static Stream<Arguments> testAddInt128() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int i = 0; i < 50; i++) {
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong() >>> 2, rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong() >>> 1, rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong() >>> 1, rng.nextLong(), rng.nextLong() >>> 2, rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong(), rng.nextLong(), rng.nextLong(), rng.nextLong()));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testOfInt96(long a, int b) {
        final UInt96 x = new UInt96(a, b);
        final UInt128 y = UInt128.of(x);
        Assertions.assertEquals(x.toBigInteger(), y.toBigInteger());
    }

    static Stream<Arguments> testOfInt96() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int i = 0; i < 50; i++) {
            final long a = rng.nextLong();
            final int b = rng.nextInt();
            builder.accept(Arguments.of(a, b));
            builder.accept(Arguments.of(0, b));
            builder.accept(Arguments.of(a, 0));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testMultiplyInt(long a, long b, int n) {
        assertMultiplyInt(a, b, n);
        assertMultiplyInt(a >>> 32, b, n);
        assertMultiplyInt(0, b, n);
    }

    private static void assertMultiplyInt(long a, long b, int n) {
        final UInt128 v = new UInt128(a, b);
        BigInteger expected = v.toBigInteger().multiply(BigInteger.valueOf(n & 0xffff_ffffL));
        // Clip to 128-bits. Only required if the upper 32-bits are non-zero.
        final int len = expected.bitLength();
        if (len > 128 && v.hi32() != 0) {
            expected = expected.subtract(expected.shiftRight(128).shiftLeft(128));
        }
        Assertions.assertEquals(expected, v.unsignedMultiply(n).toBigInteger());
    }

    static Stream<Arguments> testMultiplyInt() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final int[] x = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int i = 0; i < 50; i++) {
            final long a = rng.nextLong();
            final long b = rng.nextLong();
            for (final int n : x) {
                builder.accept(Arguments.of(a, b, n));
            }
            for (int j = 0; j < 5; j++) {
                builder.accept(Arguments.of(a, b, rng.nextInt()));
            }
        }
        builder.accept(Arguments.of(-1L >>> 32, -1L, -1));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testSubtract(long a, long b, long c, long d) {
        assertSubtract(a, b, c, d);
        assertSubtract(c, d, a, b);
    }

    private static void assertSubtract(long a, long b, long c, long d) {
        final UInt128 x = new UInt128(a, b);
        final UInt128 y = new UInt128(c, d);
        BigInteger expected = x.toBigInteger().subtract(y.toBigInteger());
        if (expected.signum() < 0) {
            expected = expected.add(BigInteger.ONE.shiftLeft(128));
        }
        Assertions.assertEquals(expected, x.subtract(y).toBigInteger());
    }

    static Stream<Arguments> testSubtract() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int i = 0; i < 50; i++) {
            final long a = rng.nextLong();
            final long b = rng.nextLong();
            final long c = rng.nextLong();
            final long d = rng.nextLong();
            builder.accept(Arguments.of(a, b, c, d));
            builder.accept(Arguments.of(0, 0, c, d));
            builder.accept(Arguments.of(-1L, -1L, c, d));
        }
        builder.accept(Arguments.of(-1L, -1L, -1L, -1L));
        return builder.build();
    }
}
