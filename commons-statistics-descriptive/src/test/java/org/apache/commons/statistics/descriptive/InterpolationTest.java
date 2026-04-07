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

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Interpolation}.
 */
class InterpolationTest {
    @ParameterizedTest
    @MethodSource
    void testMean(double x, double y, double expected) {
        Assertions.assertEquals(expected, Interpolation.mean(x, y), "x, y");
        Assertions.assertEquals(expected, Interpolation.mean(y, x), "y, x");
    }

    static Stream<Arguments> testMean() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(2, 3, 2.5));
        builder.add(Arguments.of(-4, 3, -0.5));
        builder.add(Arguments.of(-4, 4, 0));
        builder.add(Arguments.of(-4, 5, 0.5));
        builder.add(Arguments.of(-0.0, -0.0, -0.0));
        builder.add(Arguments.of(-Double.MAX_VALUE, Double.MAX_VALUE, 0));
        builder.add(Arguments.of(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE));
        builder.add(Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
        builder.add(Arguments.of(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        builder.add(Arguments.of(-Double.MAX_VALUE, Double.MAX_VALUE, 0));
        builder.add(Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        builder.add(Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        builder.add(Arguments.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN));
        builder.add(Arguments.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testIntMean(int x, int y, double expected) {
        Assertions.assertEquals(expected, Interpolation.mean(x, y), "x, y");
        Assertions.assertEquals(expected, Interpolation.mean(y, x), "y, x");
    }

    static Stream<Arguments> testIntMean() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(2, 3, 2.5));
        builder.add(Arguments.of(-4, 3, -0.5));
        builder.add(Arguments.of(-4, 4, 0));
        builder.add(Arguments.of(-4, 5, 0.5));
        builder.add(Arguments.of(0, 0, 0));
        builder.add(Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        builder.add(Arguments.of(-Integer.MAX_VALUE, Integer.MAX_VALUE, 0));
        builder.add(Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE));
        builder.add(Arguments.of(Integer.MIN_VALUE, Integer.MAX_VALUE, -0.5));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testLongMeanAsDouble(long x, long y, double expected) {
        if (Double.isNaN(expected)) {
            // Compute expected
            expected = BigDecimal.valueOf(x)
                .add(BigDecimal.valueOf(y))
                .divide(BigDecimal.valueOf(2)).doubleValue();
        }
        Assertions.assertEquals(expected, Interpolation.meanAsDouble(x, y), "x, y");
        Assertions.assertEquals(expected, Interpolation.meanAsDouble(y, x), "y, x");
        if (Math.min(x, y) != Long.MIN_VALUE) {
            // Expect closest double result following IEEE rounding.
            // This is just the negation. Subtract from zero to remove the sign on -0.0.
            Assertions.assertEquals(0.0 - expected, Interpolation.meanAsDouble(-x, -y), "-x, -y");
            Assertions.assertEquals(0.0 - expected, Interpolation.meanAsDouble(-y, -x), "-y, -x");
        }
    }

    static Stream<Arguments> testLongMeanAsDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(2, 3, 2.5));
        builder.add(Arguments.of(-4, 3, -0.5));
        builder.add(Arguments.of(-4, 4, 0));
        builder.add(Arguments.of(-4, 5, 0.5));
        builder.add(Arguments.of(0, 0, 0));
        builder.add(Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE));
        builder.add(Arguments.of(-Long.MAX_VALUE, Long.MAX_VALUE, 0));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MAX_VALUE, -0.5));
        builder.add(Arguments.of(-Long.MAX_VALUE, 0, -0x1.0p62));

        // Large values
        // Compute expected by using NaN
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int j = 0; j < 50; j++) {
            // This aims to create a sum that will have 54 bits of significant digits.
            // The result must then be computed with correct rounding to 53-bits.
            // The rounding can be different if +/-1 is added to one of the numbers.
            // In addition we use two values that overflow when added to avoid the
            // simple result of (a+b) * 0.5
            final long a = rng.nextLong() << 10; // 54-bits
            final long b = rng.nextLong() << 11; // 53-bits
            for (int i = -1; i <= 1; i += 2) {
                // Force overflow of (a+b) by matching signs
                builder.add(Arguments.of(i + (a | Long.MIN_VALUE), b | Long.MIN_VALUE, Double.NaN));
                builder.add(Arguments.of(i + (a & Long.MAX_VALUE), b & Long.MAX_VALUE, Double.NaN));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testLongMeanAsLong(long x, long y, long expected) {
        Assertions.assertEquals(expected, Interpolation.meanAsLong(x, y), "x, y");
        Assertions.assertEquals(expected, Interpolation.meanAsLong(y, x), "y, x");
    }

    static Stream<Arguments> testLongMeanAsLong() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(2, 3, 3));
        builder.add(Arguments.of(-2, -3, -2));
        builder.add(Arguments.of(-4, 3, 0));
        builder.add(Arguments.of(-4, 4, 0));
        builder.add(Arguments.of(-4, 5, 1));
        builder.add(Arguments.of(0, 0, 0));
        builder.add(Arguments.of(Long.MAX_VALUE - 0, Long.MAX_VALUE, Long.MAX_VALUE - 0));
        builder.add(Arguments.of(Long.MAX_VALUE - 1, Long.MAX_VALUE, Long.MAX_VALUE - 0));
        builder.add(Arguments.of(Long.MAX_VALUE - 2, Long.MAX_VALUE, Long.MAX_VALUE - 1));
        builder.add(Arguments.of(Long.MAX_VALUE - 3, Long.MAX_VALUE, Long.MAX_VALUE - 1));
        builder.add(Arguments.of(Long.MAX_VALUE - 4, Long.MAX_VALUE, Long.MAX_VALUE - 2));
        builder.add(Arguments.of(-Long.MAX_VALUE, Long.MAX_VALUE, 0));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 0, Long.MIN_VALUE + 0));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 1));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 2, Long.MIN_VALUE + 1));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 3, Long.MIN_VALUE + 2));
        builder.add(Arguments.of(Long.MIN_VALUE + 0, Long.MAX_VALUE, 0));
        builder.add(Arguments.of(Long.MIN_VALUE + 1, Long.MAX_VALUE, 0));
        builder.add(Arguments.of(Long.MIN_VALUE + 2, Long.MAX_VALUE, 1));
        builder.add(Arguments.of(Long.MAX_VALUE - 0, 0, 1 + (Long.MAX_VALUE >> 1)));
        builder.add(Arguments.of(Long.MAX_VALUE - 1, 0, 0 + (Long.MAX_VALUE >> 1)));
        builder.add(Arguments.of(Long.MAX_VALUE - 2, 0, 0 + (Long.MAX_VALUE >> 1)));
        builder.add(Arguments.of(Long.MAX_VALUE - 3, 0, -1 + (Long.MAX_VALUE >> 1)));
        builder.add(Arguments.of(Long.MIN_VALUE + 0, 0, 0 + Long.MIN_VALUE >> 1));
        builder.add(Arguments.of(Long.MIN_VALUE + 1, 0, 1 + (Long.MIN_VALUE >> 1)));
        builder.add(Arguments.of(Long.MIN_VALUE + 2, 0, 1 + (Long.MIN_VALUE >> 1)));
        builder.add(Arguments.of(Long.MIN_VALUE + 3, 0, 2 + (Long.MIN_VALUE >> 1)));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testLongMean(long x, long y) {
        // Use the StatisticResult to test the int and BigInteger values
        final StatisticResult expected = TestHelper.interpolate(x, y, 0.5);
        TestHelper.assertEquals(expected, Interpolation.mean(x, y), null, () -> "x, y");
        TestHelper.assertEquals(expected, Interpolation.mean(y, x), null, () -> "y, x");
    }

    static Stream<Arguments> testLongMean() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(2, 3));
        builder.add(Arguments.of(-4, -1));
        // Not rounded to positive infinity as a double
        final long big = 1L << 53;
        for (int i = 1; i <= 4; i++) {
            builder.add(Arguments.of(big, big + i));
            builder.add(Arguments.of(-big, -big - i));
        }
        builder.add(Arguments.of(Long.MAX_VALUE - 5, Long.MAX_VALUE));
        builder.add(Arguments.of(Long.MAX_VALUE - 4, Long.MAX_VALUE));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 3));
        builder.add(Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 4));
        builder.add(Arguments.of(Long.MIN_VALUE + 2, 0));
        builder.add(Arguments.of(Long.MIN_VALUE + 3, 0));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testInterpolate(double a, double b, double t, double expected) {
        Assertions.assertEquals(expected, Interpolation.interpolate(a, b, t));
    }

    static Stream<Arguments> testInterpolate() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Same cases as the mean
        builder.add(Arguments.of(2, 3, 0.5, 2.5));
        builder.add(Arguments.of(-4, 3, 0.5, -0.5));
        builder.add(Arguments.of(-4, 4, 0.5, 0));
        builder.add(Arguments.of(-0.0, -0.0, 0.5, -0.0));
        builder.add(Arguments.of(-Double.MAX_VALUE, Double.MAX_VALUE, 0.5, 0));
        builder.add(Arguments.of(Double.MIN_VALUE, Double.MIN_VALUE, 0.5, Double.MIN_VALUE));
        builder.add(Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE, 0.5, Double.MAX_VALUE));
        builder.add(Arguments.of(-Double.MAX_VALUE, -Double.MAX_VALUE, 0.5, -Double.MAX_VALUE));
        builder.add(Arguments.of(-Double.MAX_VALUE, Double.MAX_VALUE, 0.5, 0));
        builder.add(Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.5, Double.POSITIVE_INFINITY));
        builder.add(Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.5, Double.NEGATIVE_INFINITY));
        builder.add(Arguments.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.5, Double.NaN));
        // Interpolation
        builder.add(Arguments.of(1, 11, 0, 1));
        builder.add(Arguments.of(1, 11, 0.1, 2));
        builder.add(Arguments.of(1, 11, 0.2, 3));
        builder.add(Arguments.of(1, 11, 0.7, 8));
        builder.add(Arguments.of(1, 11, 1, 11));
        // Edge case from:
        // https://stackoverflow.com/questions/4353525/floating-point-linear-interpolation
        // https://stackoverflow.com/a/23716956
        builder.add(Arguments.of(0.45, 0.45, 0.81965185546875, 0.45));
        // https://fgiesen.wordpress.com/2012/08/15/linear-interpolation-past-present-and-future/
        builder.add(Arguments.of(127, 127, 5.0586262771736122e-15, 127));
        builder.add(Arguments.of(1.7, 1.7, 0.4, 1.7));
        // t=0 for zero. This captures the current interpolation behaviour where argument 'a'
        // is not always returned if t==0.
        builder.add(Arguments.of(-0.0, -0.0, 0.0, -0.0));
        builder.add(Arguments.of(-0.0, -0.0, 1.0, -0.0));
        builder.add(Arguments.of(-0.0, 0.0, 0.0, 0.0));
        builder.add(Arguments.of(-0.0, 0.0, 1.0, 0.0));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testInterpolateProperties(double a, double b, double t1, double t2) {
        // The interpolate function assumes t in (0, 1) and a <= b.
        // These assumptions allow the implementation to be simplified.
        if (b < a) {
            testInterpolateProperties(b, a, t1, t2);
            return;
        }

        // Test properties given arguments are finite.
        // exactness: lerp(a,b,0)==a && lerp(a,b,1)==b
        // monotonicity: cmp(lerp(a,b,t2),lerp(a,b,t1)) * cmp(t2,t1) * cmp(b,a) >= 0, where cmp
        //               is an arithmetic three-way comparison function
        // determinacy: result of NaN only for lerp(a,a,INFINITY)
        // boundedness: t<0 || t>1 || isfinite(lerp(a,b,t))
        // consistency: lerp(a,a,t)==a
        // See: https://www.open-std.org/jtc1/sc22/wg21/docs/papers/2018/p0811r2.html

        // Defined as x = a + t(b-a) but implementation may vary

        // Interpolation between a==-0.0 and b>=0.0 is 0.0.
        // This is known behaviour and ignored as usage has t in (0, 1).
        if (Double.compare(a, -0.0) == 0 && Double.compare(b, 0.0) >= 0) {
            Assertions.assertEquals(a, Interpolation.interpolate(a, b, 0), 0.0, "exactness");
        } else {
            // result is -0.0 if a=b=-0.0
            Assertions.assertEquals(a, Interpolation.interpolate(a, b, 0), "exactness");
        }
        // Not supported
        //Assertions.assertEquals(b, DoubleMath.interpolate(a, b, 1));
        Assertions.assertTrue(
            Double.compare(Interpolation.interpolate(a, b, t2), Interpolation.interpolate(a, b, t1)) *
                Double.compare(t2, t1) * Double.compare(b, a) >= 0, "monotonicity");
        final double m = Double.MAX_VALUE;
        Assertions.assertTrue(Double.isFinite(Interpolation.interpolate(a * m, b * m, t1)), "boundedness");
        Assertions.assertTrue(Double.isFinite(Interpolation.interpolate(a * m, b * m, t2)), "boundedness");
        Assertions.assertEquals(a, Interpolation.interpolate(a, a, t1), "consistency");
        Assertions.assertEquals(b, Interpolation.interpolate(b, b, t1), "consistency");
    }

    static Stream<Arguments> testInterpolateProperties() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int i = 0; i < 50; i++) {
            final double a = signedDouble(rng);
            final double b = signedDouble(rng);
            final double t1 = rng.nextDouble();
            final double t2 = rng.nextDouble();
            builder.add(Arguments.of(a, b, t1, t2));
            builder.add(Arguments.of(a * 0.0, b, t1, t2));
            builder.add(Arguments.of(a, b * 0.0, t1, t2));
            builder.add(Arguments.of(a * 0.0, b * 0.0, t1, t2));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"anyFiniteDouble"})
    void testInterpolateEdge(double a, double b) {
        if (b < a) {
            testInterpolateEdge(b, a);
            return;
        }
        // Test the extremes of t in (0, 1)
        Assertions.assertTrue(Interpolation.interpolate(a, b, 0.0) >= a, () -> "t=0 a=" + a + " b=" + b);
        Assertions.assertTrue(Interpolation.interpolate(a, b, Double.MIN_VALUE) >= a, () -> "t=min a=" + a + " b=" + b);
        Assertions.assertTrue(Interpolation.interpolate(a, b, Math.nextDown(1.0)) <= b, () -> "t=0.999... a=" + a + " b=" + b);
        // This fails with the current implementation as it assumes interpolation never uses t=1
        //Assertions.assertTrue(DoubleMath.interpolate(a, b, 1.0) <= b, () -> "t=1 a=" + a + " b=" + b);
    }

    @ParameterizedTest
    @MethodSource(value = {"anyFiniteDouble"})
    void testMeanVsInterpolate(double a, double b) {
        if (b < a) {
            testMeanVsInterpolate(b, a);
            return;
        }
        // The mean should be the exact double
        // but the interpolation has more float additions.
        // The multiplication by the interpolant 0.5 is exact for normal numbers.
        double expected = a + b;
        if (!Double.isFinite(expected) || Math.abs(expected) < Double.MIN_NORMAL) {
            return;
        }
        expected *= 0.5;
        Assertions.assertEquals(expected, Interpolation.mean(a, b));
        Assertions.assertEquals(expected, Interpolation.interpolate(a, b, 0.5), Math.ulp(expected),
            () -> a + ", " + b);
    }

    static Stream<Arguments> anyFiniteDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final long infBits = Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
        final long signBit = Long.MIN_VALUE;
        for (int i = 0; i < 50; i++) {
            // doubles in [-1, 1)
            builder.add(Arguments.of(signedDouble(rng), signedDouble(rng)));
            // Any finite double
            final long m = rng.nextLong(infBits);
            final long n = rng.nextLong(infBits);
            builder.add(Arguments.of(Double.longBitsToDouble(m), Double.longBitsToDouble(n)));
            builder.add(Arguments.of(Double.longBitsToDouble(m), Double.longBitsToDouble(n | signBit)));
            builder.add(Arguments.of(Double.longBitsToDouble(m | signBit), Double.longBitsToDouble(n)));
            builder.add(Arguments.of(Double.longBitsToDouble(m | signBit), Double.longBitsToDouble(n | signBit)));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {
        "testLongInterpolate",
        "testLongInterpolateCoverage"
    })
    void testLongInterpolate(long a, long b, double t) {
        if (b < a) {
            testLongInterpolate(b, a, t);
            return;
        }
        final StatisticResult expected = TestHelper.interpolate(a, b, t);
        final StatisticResult actual = Interpolation.interpolate(a, b, t);
        // Note: This tests exact equality of the double value.
        // DD arithmetic can be incorrect by 1 ULP. Current test cases
        // do not hit this condition.
        final DoubleTolerance tol = DoubleTolerances.equals();
        TestHelper.assertEquals(expected, actual, tol, () -> String.format("(%d, %d) * %s", a, b, t));
        // Check caching
        TestHelper.assertEquals(expected, actual, tol, () -> String.format("(%d, %d) * %s (repeated)", a, b, t));
    }

    static Stream<Arguments> testLongInterpolate() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Interpolation
        builder.add(Arguments.of(1, 11, 0.0));
        builder.add(Arguments.of(1, 11, 0.1));
        builder.add(Arguments.of(1, 11, 0.2));
        builder.add(Arguments.of(1, 11, 1.0 / 3));
        builder.add(Arguments.of(1, 11, 0.7));
        builder.add(Arguments.of(1, 11, 0.73));
        builder.add(Arguments.of(1, 11, 0.77));
        builder.add(Arguments.of(1, 11, 1.0));
        // Rounding of ties
        builder.add(Arguments.of(10, 20, 0.25));
        builder.add(Arguments.of(-5, 5, 0.25));
        builder.add(Arguments.of(-25, -15, 0.25));
        builder.add(Arguments.of(10, 20, 0.75));
        builder.add(Arguments.of(-5, 5, 0.75));
        builder.add(Arguments.of(-25, -15, 0.75));
        // Rounding of negative
        builder.add(Arguments.of(-25, -15, 0.73));
        builder.add(Arguments.of(-25, -15, 0.77));
        // Random values
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        // Small values
        for (int i = 0; i < 10; i++) {
            builder.add(Arguments.of(rng.nextLong() >>> 12, rng.nextLong() >> 12, rng.nextDouble()));
        }
        // Large values
        for (int i = 0; i < 50; i++) {
            builder.add(Arguments.of(rng.nextLong(), rng.nextLong(), rng.nextDouble()));
        }
        // Extreme interpolant
        for (int i = 0; i < 10; i++) {
            long a = rng.nextLong();
            long b = rng.nextLong();
            builder.add(Arguments.of(a, b, 0));
            builder.add(Arguments.of(a, b, 1.0));
            builder.add(Arguments.of(a, b, Math.nextDown(1.0)));
            // Biggest difference should be 2^64 - 1. Test factors that will generate
            // small integer gaps using |b - a|
            builder.add(Arguments.of(a, b, 0x1.0p-61));
            builder.add(Arguments.of(a, b, 0x1.0p-62));
            builder.add(Arguments.of(a, b, 0x1.0p-63));
            // Close to 0.5
            builder.add(Arguments.of(a, b, Math.nextDown(0.5)));
            builder.add(Arguments.of(a, b, Math.nextUp(0.5)));
            // Very large odd difference.
            // Moving min/max by a 13-bit number makes the difference have random
            // lower bits in a double-double representation and should create
            // cases with 64-bits * 53-bits for (b - a) * t where the fraction
            // part of the result is 0.5 +/- tiny.
            a = (Long.MIN_VALUE + (a >>> 51)) & ~0x1L;
            b = (Long.MAX_VALUE - (b >>> 51)) | 0x1L;
            builder.add(Arguments.of(a, b, Math.nextDown(0.5)));
            builder.add(Arguments.of(a, b, Math.nextUp(0.5)));
        }
        // Failure cases from alternative implementations.
        // Cases where use of Commons Numbers DD class to compute
        // a + t * (b - a) has incorrect rounding as the fractional part is
        // within 1 ULP of 0.5.
        for (int i = -3; i <= 3; i += 1) {
            builder.add(Arguments.of(i + (1L << 61), 1L << 62, Math.nextDown(0x1.0p-62)));
            builder.add(Arguments.of(i + (1L << 62) - 1, Long.MAX_VALUE, Math.nextDown(0x1.0p-63)));
            // Same difference but a + t * (b - a) == t * (b - a) as double
            builder.add(Arguments.of(i, 1L << 61, Math.nextDown(0x1.0p-62)));
            builder.add(Arguments.of(i - 1, Long.MAX_VALUE - (1L << 62), Math.nextDown(0x1.0p-63)));
            // Smaller delta (b - a) but a is large
            long a = rng.nextLong();
            builder.add(Arguments.of(a + i, a + (1L << 53), 0x1.0p-54));
            builder.add(Arguments.of(a + i, a + (1L << 53), Math.nextUp(0x1.0p-54)));
            builder.add(Arguments.of(a + i, a + (1L << 53), Math.nextDown(0x1.0p-54)));
            // Hits case of rounding of exact product
            // t * (b - a) == (0.5, +/-x)
            a = (1L << 62) + (a >>> 60);
            builder.add(Arguments.of(i, a, 0.5));
            builder.add(Arguments.of(i, a, Math.nextUp(0.5)));
            builder.add(Arguments.of(i, a, Math.nextDown(0.5)));
            final long shift = Long.MIN_VALUE + 10;
            builder.add(Arguments.of(shift + i, shift + a, 0.5));
            builder.add(Arguments.of(shift + i, shift + a, Math.nextUp(0.5)));
            builder.add(Arguments.of(shift + i, shift + a, Math.nextDown(0.5)));
        }
        // delta (b - a) ~ 2^64
        for (int i = 0; i <= 3; i++) {
            builder.add(Arguments.of(Long.MIN_VALUE + i, Long.MAX_VALUE, 0x1.0p-65));
            builder.add(Arguments.of(Long.MIN_VALUE + i, Long.MAX_VALUE, Math.nextUp(0x1.0p-65)));
            builder.add(Arguments.of(Long.MIN_VALUE + i, Long.MAX_VALUE, Math.nextDown(0x1.0p-65)));
        }
        // Fail some implementations due to rounding ties at 0.5.
        builder.add(Arguments.of(-9223372036854768386L, 9223372036854768115L, 0.5000000000000001));
        builder.add(Arguments.of(-9223372036854773642L, 9223372036854769547L, 0.5000000000000001));
        // Large difference exactly representable as a double
        builder.add(Arguments.of(-4478619208235831223L, -3752646238795058871L, 0.28196227437773047));
        return builder.build();
    }

    /**
     * Cases that hit all code paths in the long interpolate method.
     */
    static Stream<Arguments> testLongInterpolateCoverage() {
        final Stream.Builder<Arguments> builder = Stream.builder();

        // Extremes of t in [0, 1]
        builder.add(Arguments.of(-123, 42, 0.0));
        builder.add(Arguments.of(-123, 42, 1.0));
        builder.add(Arguments.of(-9223372036854775797L, -4611686018427387894L, 0.0));
        builder.add(Arguments.of(-9223372036854775797L, -4611686018427387894L, 1.0));

        // Note: f is fractional part after subtraction of integer component of t * (b - a)

        // ----
        // t * (b - a) is exact as a double-double;
        // ----
        // f > 0.5
        builder.add(Arguments.of(-25, -15, 0.77));
        builder.add(Arguments.of(15, 25, 0.77));
        // f < 0.5
        builder.add(Arguments.of(-25, -15, 0.73));
        builder.add(Arguments.of(15, 25, 0.77));
        // f = (0.5, +x)
        // (b - a) = (1L << 53) - 1
        builder.add(Arguments.of(-4320143753338034118L, -4311136554083293127L, 0.5000000000000001 * 0x1p-53));
        // f = (0.5, 0)
        builder.add(Arguments.of(-25L, -15L, 0.25));
        // f = (0.5, -x)
        // (b - a) = (1L << 53) - 2
        builder.add(Arguments.of(8177289726244351876L, 8186296925499092866L, 0.5000000000000001 * 0x1p-53));

        // Require low part of t * (b - a) to generate the correctly rounded double value
        builder.add(Arguments.of(-787880950476038L, 1648063999686614L, 0.6428698833722182));
        builder.add(Arguments.of(-2027750983326380L, 1393002285320925L, 0.7441679727226498));
        builder.add(Arguments.of(-1739674140143992L, 3278644961678849L, 0.925732984914108));
        builder.add(Arguments.of(-975431565402466L, 2448477137132956L, 0.8612711685317348));
        // t is small so that there is no integer part to t * (b - a).
        builder.add(Arguments.of(1, 97450645279000L, 1.063632902700862E-16));
        builder.add(Arguments.of(1, 4883862211873020L, 2.7408630060971736E-17));
        builder.add(Arguments.of(1, 4544522888524833L, 7.85943528660425E-17));

        // Rounding to integer fails if using double arithmetic.
        // These are randomly generated cases that occur with frequency
        // around 9/100 when using case:
        // builder.add(Arguments.of(rng.nextLong() >>> 12, rng.nextLong() >> 12, rng.nextDouble()));
        builder.add(Arguments.of(1872072617636825L, 3729351242345288L, 0.908880938830305));
        builder.add(Arguments.of(646040713007248L, 2076283366558024L, 0.6532575075121976));
        builder.add(Arguments.of(-1492953179628870L, 1292106777261102L, 0.5817060461218366));
        builder.add(Arguments.of(-2231514673530436L, 4020168327742576L, 0.3038722322856059));

        // ----
        // t * (b - a) is up to 117-bits
        // ----
        // f > 0.5
        builder.add(Arguments.of(-4317983120538845730L, -3620797767309099417L, 0.49999999999999994));
        builder.add(Arguments.of(-4317983120538845730L, -3620797767309099417L, 0.5000000000000001));
        // f < 0.5
        builder.add(Arguments.of(-9223372036854767628L, 9223372036854774965L, 0.5000000000000001));
        builder.add(Arguments.of(-24505126620578412L, 1899819214393636406L, 0.49999999999999994));
        // t * (b - a) > 2^63
        builder.add(Arguments.of(-6809932153038592084L, 7721749020011090769L, 0.9999999999999999));
        builder.add(Arguments.of(-9223372036854772776L, 9223372036854774501L, 0.5000000000000001));
        // t * (b - a) < 2^63
        builder.add(Arguments.of(-6809932153038592084L, 7721749020011090769L, 0.01));
        // f = (0.5, +x)
        builder.add(Arguments.of(-2760372990263914563L, -2751365791009173570L, 5.551115123125783E-17));
        // f = (0.5, 0)
        builder.add(Arguments.of(-1L, 4611686018427387904L, 0.5));
        builder.add(Arguments.of(6988866235401677194L, 6997873434656418186L, 5.551115123125783E-17));
        // f = (0.5, -x)
        builder.add(Arguments.of(-9223372036854775805L, 9223372036854775807L, 2.710505431213761E-20));
        // f = (-0.5, 0)
        builder.add(Arguments.of(-9223372036854775797L, -4611686018427387894L, 0.5));
        // f = (-0.5, +x) // Not possible
        // f = (-0.5, -x) // Not possible

        // double value of 5.499999999999999 requires low part of fraction for rounding
        builder.add(Arguments.of(-36028797018963968L, 36028797018963963L, 0.5000000000000001));

        return builder.build();
    }

    /**
     * Test that long interpolation is consistent with all the test cases for the mean.
     */
    @ParameterizedTest
    @MethodSource(value = {"testLongMeanAsDouble", "testLongMeanAsLong", "testLongMean"})
    void testLongMeanVsInterpolate(long x, long y) {
        // Values must be sorted
        if (y < x) {
            testLongMeanVsInterpolate(y, x);
            return;
        }
        final StatisticResult r1 = Interpolation.mean(x, y);
        final StatisticResult r2 = Interpolation.interpolate(x, y, 0.5);
        TestHelper.assertEquals(r1, r2, null, () -> String.format("(%d, %d) * 0.5", x, y));
        // Check caching
        TestHelper.assertEquals(r1, r2, null, () -> String.format("(%d, %d) * 0.5 (repeated)", x, y));
    }

    @Test
    void testSignedDouble() {
        Assertions.assertEquals(-1.0, signedDouble(() -> Long.MIN_VALUE));
        Assertions.assertEquals(Math.nextDown(1.0), signedDouble(() -> Long.MAX_VALUE));
        Assertions.assertEquals(0.0, signedDouble(() -> 0));
        Assertions.assertEquals(-Math.ulp(0.5), signedDouble(() -> -1L));
        Assertions.assertEquals(Math.ulp(0.5), signedDouble(() -> 1L << 10));
    }

    /**
     * Create a uniform signed double in [-1, 1). Spacing is 2^-53, or Math.ulp(0.5).
     *
     * @param rng Source of randomness.
     * @return the double
     */
    private static double signedDouble(UniformRandomProvider rng) {
        // As per UniformRandomProvider nextDouble but use a signed shift.
        return 0x1.0p-53 * (rng.nextLong() >> 10);
    }
}
