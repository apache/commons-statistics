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
import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link Int128}.
 */
class Int128Test {
    private static final BigInteger TWO_POW_128 = BigInteger.ONE.shiftLeft(128);
    private static final BigInteger TWO_POW_127 = BigInteger.ONE.shiftLeft(127);
    private static final BigInteger MINUS_TWO_POW_127 = BigInteger.ONE.shiftLeft(127).negate();

    @Test
    void testCreate() {
        final Int128 v = Int128.create();
        Assertions.assertEquals(BigInteger.ZERO, v.toBigInteger());
    }

    @ParameterizedTest
    @MethodSource(value = {"testAddLong"})
    void testToBigInteger(long a, long b) {
        final BigInteger expected = BigInteger.valueOf(a).shiftLeft(64).add(BigInteger.valueOf(b));
        final Int128 v = new Int128(a, b);
        Assertions.assertEquals(expected, v.toBigInteger());
    }

    @ParameterizedTest
    @MethodSource
    void testAddLong(long a, long b) {
        final BigInteger expected = BigInteger.valueOf(a).add(BigInteger.valueOf(b));
        final Int128 v = Int128.of(a);
        v.add(b);
        Assertions.assertEquals(expected, v.toBigInteger());
    }

    static Stream<Arguments> testAddLong() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final long[] x = {0, 1, 2, Long.MIN_VALUE, Long.MAX_VALUE, 612783421678L, 42};
        for (final long i : x) {
            for (final long j : x) {
                builder.accept(Arguments.of(i, j));
                builder.accept(Arguments.of(i, -j));
                builder.accept(Arguments.of(-i, j));
                builder.accept(Arguments.of(-i, -j));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testAddLongs(long[] a) {
        final BigInteger expected = Arrays.stream(a).mapToObj(BigInteger::valueOf)
            .reduce(BigInteger::add).orElse(BigInteger.ZERO);
        final Int128 v = Int128.create();
        for (final long x : a) {
            v.add(x);
        }
        Assertions.assertEquals(expected, v.toBigInteger());
        // Check floating-point representation
        Assertions.assertEquals(expected.doubleValue(), v.toDouble(), "double");
        TestHelper.assertEquals(new BigDecimal(expected), v.toDD(), 0x1.0p-106, "DD");
    }

    static Stream<Arguments> testAddLongs() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = TestHelper.createRNG();
        for (final int n : new int[] {50, 100}) {
            builder.accept(Arguments.of(rng.longs(n).toArray()));
            builder.accept(Arguments.of(rng.longs(n).map(x -> x >>> 2).toArray()));
            builder.accept(Arguments.of(rng.longs(n).map(x -> -(x >>> 2)).toArray()));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testAddInt128(long a, long b, long c, long d) {
        final Int128 x = new Int128(a, b);
        final Int128 y = new Int128(c, d);
        Assertions.assertEquals(a, x.hi64());
        Assertions.assertEquals(b, x.lo64());
        BigInteger expected = x.toBigInteger().add(y.toBigInteger());
        // The Int128 result is a signed 128-bit integer.
        // This is subject to integer overflow.
        // Clip the unlimited BigInteger result to the range [2^-127, 2^127).
        // Since the overflow will be at most 1-bit we can wrap the value
        // using +/- 2^128.
        if (expected.compareTo(TWO_POW_127) >= 0) {
            // too high
            expected = expected.subtract(TWO_POW_128);
        } else if (expected.compareTo(MINUS_TWO_POW_127) < 0) {
            // too low
            expected = expected.add(TWO_POW_128);
        }
        x.add(y);
        Assertions.assertEquals(expected, x.toBigInteger(),
            () -> String.format("(%d, %d) + (%d, %d)", a, b, c, d));
        // Check floating-point representation
        Assertions.assertEquals(expected.doubleValue(), x.toDouble(), "double");
        TestHelper.assertEquals(new BigDecimal(expected), x.toDD(), 0x1.0p-106, "DD");
        // Check self-addition
        expected = y.toBigInteger();
        expected = expected.add(expected);
        if (expected.compareTo(TWO_POW_127) >= 0) {
            // too high
            expected = expected.subtract(TWO_POW_128);
        } else if (expected.compareTo(MINUS_TWO_POW_127) < 0) {
            // too low
            expected = expected.add(TWO_POW_128);
        }
        y.add(y);
        Assertions.assertEquals(expected, y.toBigInteger(),
            () -> String.format("(%d, %d) self-addition", c, d));
    }

    static Stream<Arguments> testAddInt128() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 0; i < 50; i++) {
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong() >>> 2, rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextLong(), rng.nextLong() >>> 1, rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong() >>> 1, rng.nextLong(), rng.nextLong() >>> 2, rng.nextLong()));
            builder.accept(Arguments.of(rng.nextLong(), rng.nextLong(), rng.nextLong(), rng.nextLong()));
        }
        // Special case where hi is non-zero and lo is zero.
        // Hit edge case in toDouble()
        for (int i = 0; i < 5; i++) {
            builder.accept(Arguments.of(rng.nextLong() >>> 3, 0, rng.nextLong() >>> 3, 0));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testSquareLow(long a) {
        final BigInteger expected = BigInteger.valueOf(a).pow(2);
        final UInt128 v = Int128.of(a).squareLow();
        Assertions.assertEquals(expected, v.toBigInteger());
    }

    static LongStream testSquareLow() {
        final LongStream.Builder builder = LongStream.builder();
        final long[] x = {0, 1, Long.MIN_VALUE, Long.MAX_VALUE, 612783421678L, 42};
        for (final long i : x) {
            builder.accept(i);
            builder.accept(-i);
        }
        RandomSource.XO_RO_SHI_RO_128_PP.create().longs(20).forEach(builder);
        return builder.build();
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MAX_VALUE, Integer.MIN_VALUE})
    void testToIntExact(int x) {
        final Int128 v = Int128.of(x);
        Assertions.assertEquals(x, v.toIntExact());
        final int y = x < 0 ? -1 : 1;
        v.add(y);
        Assertions.assertThrows(ArithmeticException.class, v::toIntExact);
        v.add(-y);
        Assertions.assertEquals(x, v.toIntExact());
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MAX_VALUE, Long.MIN_VALUE})
    void testToLongExact(long x) {
        final Int128 v = Int128.of(x);
        Assertions.assertEquals(x, v.toLongExact());
        final int y = x < 0 ? -1 : 1;
        v.add(y);
        Assertions.assertThrows(ArithmeticException.class, v::toLongExact);
        v.add(-y);
        Assertions.assertEquals(x, v.toLongExact());
    }
}
