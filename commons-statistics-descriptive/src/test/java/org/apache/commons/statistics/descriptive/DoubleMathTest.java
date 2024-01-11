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

import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link DoubleMath}.
 */
class DoubleMathTest {
    @ParameterizedTest
    @MethodSource
    void testMean(double x, double y, double expected) {
        Assertions.assertEquals(expected, DoubleMath.mean(x, y));
    }

    static Stream<Arguments> testMean() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(2, 3, 2.5));
        builder.add(Arguments.of(-4, 3, -0.5));
        builder.add(Arguments.of(-4, 4, 0));
        builder.add(Arguments.of(-0.0, -0.0, -0.0));
        builder.add(Arguments.of(-Double.MAX_VALUE, Double.MAX_VALUE, 0));
        builder.add(Arguments.of(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE));
        builder.add(Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
        builder.add(Arguments.of(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        builder.add(Arguments.of(-Double.MAX_VALUE, Double.MAX_VALUE, 0));
        builder.add(Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        builder.add(Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        builder.add(Arguments.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testInterpolate(double a, double b, double t, double expected) {
        Assertions.assertEquals(expected, DoubleMath.interpolate(a, b, t));
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
            Assertions.assertEquals(a, DoubleMath.interpolate(a, b, 0), 0.0, "exactness");
        } else {
            // result is -0.0 if a=b=-0.0
            Assertions.assertEquals(a, DoubleMath.interpolate(a, b, 0), "exactness");
        }
        // Not supported
        //Assertions.assertEquals(b, DoubleMath.interpolate(a, b, 1));
        Assertions.assertTrue(
            Double.compare(DoubleMath.interpolate(a, b, t2), DoubleMath.interpolate(a, b, t1)) *
                Double.compare(t2, t1) * Double.compare(b, a) >= 0, "monotonicity");
        final double m = Double.MAX_VALUE;
        Assertions.assertTrue(Double.isFinite(DoubleMath.interpolate(a * m, b * m, t1)), "boundedness");
        Assertions.assertTrue(Double.isFinite(DoubleMath.interpolate(a * m, b * m, t2)), "boundedness");
        Assertions.assertEquals(a, DoubleMath.interpolate(a, a, t1), "consistency");
        Assertions.assertEquals(b, DoubleMath.interpolate(b, b, t1), "consistency");
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
    @MethodSource
    void testInterpolateEdge(double a, double b) {
        if (b < a) {
            testInterpolateEdge(b, a);
            return;
        }
        // Test the extremes of t in (0, 1)
        Assertions.assertTrue(DoubleMath.interpolate(a, b, 0.0) >= a, () -> "t=0 a=" + a + " b=" + b);
        Assertions.assertTrue(DoubleMath.interpolate(a, b, Double.MIN_VALUE) >= a, () -> "t=min a=" + a + " b=" + b);
        Assertions.assertTrue(DoubleMath.interpolate(a, b, Math.nextDown(1.0)) <= b, () -> "t=0.999... a=" + a + " b=" + b);
        // This fails with the current implementation as it assumes interpolation never uses t=1
        //Assertions.assertTrue(DoubleMath.interpolate(a, b, 1.0) <= b, () -> "t=1 a=" + a + " b=" + b);
    }

    static Stream<Arguments> testInterpolateEdge() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final long infBits = Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
        final long signBit = Long.MIN_VALUE;
        for (int i = 0; i < 50; i++) {
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
