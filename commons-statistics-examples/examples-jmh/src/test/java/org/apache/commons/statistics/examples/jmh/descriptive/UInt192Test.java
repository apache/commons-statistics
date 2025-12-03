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
 * Test for {@link UInt192}.
 */
class UInt192Test {
    @Test
    void testCreate() {
        final UInt192 v = UInt192.create();
        Assertions.assertEquals(BigInteger.ZERO, v.toBigInteger());
    }

    @ParameterizedTest
    @MethodSource
    void testAddSquareLong(long a, long b) {
        final BigInteger expected = BigInteger.valueOf(a).pow(2)
            .add(BigInteger.valueOf(b).pow(2));
        final UInt192 v = UInt192.create();
        v.addSquare(a);
        v.addSquare(b);
        Assertions.assertEquals(expected, v.toBigInteger());
    }

    static Stream<Arguments> testAddSquareLong() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final long[] x = {0, 1, Long.MAX_VALUE, 61278342166787978L, 42, 8652939272947492397L};
        for (final long i : x) {
            for (final long j : x) {
                builder.accept(Arguments.of(i, j));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testAddSquareLongs(long[] a) {
        final BigInteger expected = Arrays.stream(a).mapToObj(BigInteger::valueOf)
            .map(x -> x.pow(2))
            .reduce(BigInteger::add).orElse(BigInteger.ZERO);
        final UInt192 v = UInt192.create();
        for (final long x : a) {
            v.addSquare(x);
        }
        Assertions.assertEquals(expected, v.toBigInteger());
        // Check floating-point representation
        TestUtils.assertEquals(new BigDecimal(expected), v.toDD(), 0x1.0p-106, "DD");
        Assertions.assertEquals(expected.doubleValue(), v.toDouble(), "double");
    }

    static Stream<Arguments> testAddSquareLongs() {
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
    void testAddInt192(long a, long b, long c, long d, long e, long f) {
        final UInt192 x = new UInt192(a, b, c);
        final UInt192 y = new UInt192(d, e, f);
        BigInteger expected = x.toBigInteger().add(y.toBigInteger());
        // The result is an unsigned 192-bit integer.
        // This is subject to integer overflow.
        // Clip the unlimited BigInteger result to the range [0, 2^192).
        if (expected.testBit(192)) {
            expected = expected.flipBit(192);
        }
        x.add(y);
        Assertions.assertEquals(expected, x.toBigInteger(),
            () -> String.format("(%d, %d, %d) + (%d, %d, %d)", a, b, c, d, e, f));
        // Check floating-point representation
        TestUtils.assertEquals(new BigDecimal(expected), x.toDD(), 0x1.0p-106, "DD");
        Assertions.assertEquals(expected.doubleValue(), x.toDouble(), "double");
        // Check self-addition
        expected = y.toBigInteger();
        expected = expected.add(expected);
        if (expected.testBit(192)) {
            expected = expected.flipBit(192);
        }
        y.add(y);
        Assertions.assertEquals(expected, y.toBigInteger(),
            () -> String.format("(%d, %d, %d) self-addition", d, e, f));
    }

    static Stream<Arguments> testAddInt192() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int i = 0; i < 50; i++) {
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong(),
                                        rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong(),
                                        rng.nextLong() >>> 1, rng.nextLong(), rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong() >>> 1, rng.nextLong(), rng.nextLong(),
                                        rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong(), rng.nextLong(), rng.nextLong(),
                                        rng.nextLong(), rng.nextLong(), rng.nextLong()));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testMultiplyInt(long a, long b, long c, int n) {
        assertMultiplyInt(a, b, c, n);
        assertMultiplyInt(a >>> 32, b, c, n);
        assertMultiplyInt(0, b, c, n);
    }

    private static void assertMultiplyInt(long a, long b, long c, int n) {
        final UInt192 v = new UInt192(a, b, c);
        BigInteger expected = v.toBigInteger().multiply(BigInteger.valueOf(n & 0xffff_ffffL));
        // Clip to 192-bits. Only required if the upper 32-bits are non-zero.
        final int len = expected.bitLength();
        if (len > 192 && v.hi32() != 0) {
            expected = expected.subtract(expected.shiftRight(192).shiftLeft(192));
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
            final long c = rng.nextLong();
            for (final int n : x) {
                builder.accept(Arguments.of(a, b, c, n));
            }
            for (int j = 0; j < 5; j++) {
                builder.accept(Arguments.of(a, b, c, rng.nextInt()));
            }
        }
        builder.accept(Arguments.of(-1L >>> 32, -1L, -1L, -1));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testSubtract(long a, long b, long c, long d, long e) {
        assertSubtract(a, b, c, d, e);
    }

    private static void assertSubtract(long a, long b, long c, long d, long e) {
        final UInt192 x = new UInt192(a, b, c);
        final UInt128 y = new UInt128(d, e);
        BigInteger expected = x.toBigInteger().subtract(y.toBigInteger());
        if (expected.signum() < 0) {
            expected = expected.add(BigInteger.ONE.shiftLeft(192));
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
            final long e = rng.nextLong();
            builder.accept(Arguments.of(a, b, c, d, e));
            builder.accept(Arguments.of(0, 0, 0, d, e));
            builder.accept(Arguments.of(-1L, -1L, -1L, d, e));
        }
        builder.accept(Arguments.of(-1L, -1L, -1L, -1L, -1L));
        return builder.build();
    }
}
