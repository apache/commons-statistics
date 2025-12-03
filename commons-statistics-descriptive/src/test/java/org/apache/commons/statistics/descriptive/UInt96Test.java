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

package org.apache.commons.statistics.descriptive;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link UInt96}.
 */
class UInt96Test {
    @Test
    void testCreate() {
        final UInt96 v = UInt96.create();
        Assertions.assertEquals(BigInteger.ZERO, v.toBigInteger());
    }

    @Test
    void testAddLongMinValue() {
        final UInt96 v = UInt96.of(5675757768682342956L);
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
        final UInt96 v = UInt96.of(a);
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
        final UInt96 v = UInt96.create();
        for (final long x : a) {
            Assertions.assertFalse(x < 0, "Value must be positive");
            v.addPositive(x);
        }
        Assertions.assertEquals(expected, v.toBigInteger());
    }

    static Stream<Arguments> testAddLongs() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = TestHelper.createRNG();
        for (final int n : new int[] {50, 100}) {
            builder.accept(Arguments.of(rng.longs(n).map(x -> x >>> 1).toArray()));
            builder.accept(Arguments.of(rng.longs(n).map(x -> x >>> 2).toArray()));
            builder.accept(Arguments.of(rng.longs(n).map(x -> x >>> 4).toArray()));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testAddInt128(long a, int b, long c, int d) {
        final UInt96 x = new UInt96(a, b);
        final UInt96 y = new UInt96(c, d);
        Assertions.assertEquals(a, x.hi64());
        Assertions.assertEquals(b, x.lo32());
        BigInteger expected = x.toBigInteger().add(y.toBigInteger());
        // The result is an unsigned 96-bit integer.
        // This is subject to integer overflow.
        // Clip the unlimited BigInteger result to the range [0, 2^96).
        if (expected.testBit(96)) {
            expected = expected.flipBit(96);
        }
        x.add(y);
        Assertions.assertEquals(expected, x.toBigInteger(),
            () -> String.format("(%d, %d) + (%d, %d)", a, b, c, d));
        // Check self-addition
        expected = y.toBigInteger();
        expected = expected.add(expected);
        if (expected.testBit(96)) {
            expected = expected.flipBit(96);
        }
        y.add(y);
        Assertions.assertEquals(expected, y.toBigInteger(),
            () -> String.format("(%d, %d) self-addition", c, d));
    }

    static Stream<Arguments> testAddInt128() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 0; i < 50; i++) {
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextInt(), rng.nextLong() >>> 2, rng.nextInt()));
            builder.accept(Arguments.of(rng.nextLong() >>> 2, rng.nextInt(), rng.nextLong() >>> 1, rng.nextInt()));
            builder.accept(Arguments.of(rng.nextLong() >>> 1, rng.nextInt(), rng.nextLong() >>> 2, rng.nextInt()));
            builder.accept(Arguments.of(rng.nextLong(), rng.nextInt(), rng.nextLong(), rng.nextInt()));
        }
        return builder.build();
    }
}
