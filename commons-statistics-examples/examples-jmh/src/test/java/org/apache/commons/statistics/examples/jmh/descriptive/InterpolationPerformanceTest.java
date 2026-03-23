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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.StatisticResult;
import org.apache.commons.statistics.examples.jmh.descriptive.InterpolationPerformance.InterpolationFunction;
import org.apache.commons.statistics.examples.jmh.descriptive.InterpolationPerformance.InterpolationSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Executes tests for {@link InterpolationPerformance}.
 *
 * <p>Test cases adapted from o.a.c.statistics.descriptive.InterpolationTest.
 * The interpolation methods are tested with a tolerance to allow non-exact implementations
 * to be checked as close to the exact result.
 */
class InterpolationPerformanceTest {
    private static List<Arguments> cases;
    private static List<Arguments> casesHard;

    /**
     * Setup the test data. This uses an exact computation to generate
     * the expected results which are reused for each interpolation method.
     */
    @BeforeAll
    static void setup() {
        // Use a seed for reproducible builds given the tolerance on the
        // inexact methods may not be correct.
        final long seed = -7286619705323203154L;
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create(seed);

        cases = new ArrayList<>();
        casesHard = new ArrayList<>();

        // Cases from o.a.c.statistics.descriptive.InterpolationTest

        // Interpolation
        addCase(cases, 1, 11, 0.0);
        addCase(cases, 1, 11, 0.1);
        addCase(cases, 1, 11, 0.2);
        addCase(cases, 1, 11, 1.0 / 3);
        addCase(cases, 1, 11, 0.7);
        addCase(cases, 1, 11, 1.0);
        // Rounding of ties
        addCase(cases, 10, 20, 0.25);
        addCase(cases, -5, 5, 0.25);
        addCase(cases, -25, -15, 0.25);
        addCase(cases, 10, 20, 0.75);
        addCase(cases, -5, 5, 0.75);
        addCase(cases, -25, -15, 0.75);
        // Large values
        for (int i = 0; i < 50; i++) {
            addCase(cases, rng.nextLong(), rng.nextLong(), rng.nextDouble());
        }
        for (int i = 0; i < 10; i++) {
            long a = rng.nextLong();
            long b = rng.nextLong();
            addCase(cases, a, b, 0);
            addCase(cases, a, b, 1.0);
            addCase(cases, a, b, Math.nextDown(1.0));
            // Biggest difference should be 2^64 - 1. Test factors that will generate
            // small integer gaps using |b - a|
            addCase(cases, a, b, 0x1.0p-61);
            addCase(cases, a, b, 0x1.0p-62);
            addCase(cases, a, b, 0x1.0p-63);
            // Close to 0.5
            addCase(cases, a, b, Math.nextDown(0.5));
            addCase(cases, a, b, Math.nextUp(0.5));
            // Very large odd difference.
            // Moving min/max by a 13-bit number makes the difference have random
            // lower bits in a double-double representation and should create
            // cases with 64-bits * 53-bits for (b - a) * t where the fraction
            // part of the result is 0.5 +/- tiny.
            a = (Long.MIN_VALUE + (a >>> 51)) & ~0x1L;
            b = (Long.MAX_VALUE - (b >>> 51)) | 0x1L;
            addCase(cases, a, b, Math.nextDown(0.5));
            addCase(cases, a, b, Math.nextUp(0.5));
        }

        // Failure cases from alternative implementations.
        // These are added to hard cases and used with a test
        // tolerance for inexact methods.

        // Cases where use of Commons Numbers DD class to compute
        // a + t * (b - a) has incorrect rounding as the fractional part is
        // within 1 ULP of 0.5.
        for (int i = -3; i <= 3; i += 1) {
            addCase(casesHard, i + (1L << 61), 1L << 62, Math.nextDown(0x1.0p-62));
            addCase(casesHard, i + (1L << 62) - 1, Long.MAX_VALUE, Math.nextDown(0x1.0p-63));
            // Same difference but a + (b - a) * t == (b - a) * t as double
            addCase(cases, i, 1L << 61, Math.nextDown(0x1.0p-62));
            addCase(cases, i - 1, Long.MAX_VALUE - (1L << 62), Math.nextDown(0x1.0p-63));
            // Smaller delta (b - a) but a is large
            final long a = rng.nextLong();
            addCase(casesHard, a + i, a + (1L << 53), 0x1.0p-54);
            addCase(casesHard, a + i, a + (1L << 53), Math.nextUp(0x1.0p-54));
            addCase(casesHard, a + i, a + (1L << 53), Math.nextDown(0x1.0p-54));
        }
        // delta (b - a) ~ 2^64
        for (int i = 0; i <= 3; i++) {
            addCase(casesHard, Long.MIN_VALUE + i, Long.MAX_VALUE, 0x1.0p-65);
            addCase(casesHard, Long.MIN_VALUE + i, Long.MAX_VALUE, Math.nextUp(0x1.0p-65));
            addCase(casesHard, Long.MIN_VALUE + i, Long.MAX_VALUE, Math.nextDown(0x1.0p-65));
        }
        // Fail a pure DD implementation due to rounding ties at 0.5.
        addCase(casesHard, -9223372036854768386L, 9223372036854768115L, 0.5000000000000001);
        addCase(casesHard, -9223372036854773642L, 9223372036854769547L, 0.5000000000000001);
    }

    @AfterAll
    static void teardown() {
        cases = null;
        casesHard = null;
    }

    private static void addCase(List<Arguments> list, long a, long b, double t) {
        // Sort arguments
        if (b < a) {
            addCase(list, b, a, t);
            return;
        }
        final BigDecimal aa = BigDecimal.valueOf(a);
        final BigDecimal bb = BigDecimal.valueOf(b);
        final BigDecimal tt = new BigDecimal(t);
        final BigDecimal expected = bb.subtract(aa).multiply(tt).add(aa);

        final double d = expected.doubleValue();
        // Round to closest, ties to positive infinity
        final RoundingMode m = d > 0 ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
        final long l = expected.setScale(0, m).longValue();

        list.add(Arguments.of(a, b, t, d, l));
    }

    static Stream<Arguments> testLongInterpolate() {
        return cases.stream();
    }

    static Stream<Arguments> testLongInterpolateHard() {
        return casesHard.stream();
    }

    @ParameterizedTest
    @MethodSource(value = {"testLongInterpolate", "testLongInterpolateHard"})
    void testInterpolateBigDecimal(long a, long b, double t, double doubleValue, long longValue) {
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateBigDecimal, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "testLongInterpolate")
    void testInterpolateDD(long a, long b, double t, double doubleValue, long longValue) {
        // Can have double ulp error
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateDD, 2, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "testLongInterpolate")
    void testInterpolateDD2(long a, long b, double t, double doubleValue, long longValue) {
        // Can have double ulp error
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateDD2, 2, 0);
    }

    @ParameterizedTest
    @MethodSource(value = {"testLongInterpolate", "testLongInterpolateHard"})
    void testInterpolateHybrid(long a, long b, double t, double doubleValue, long longValue) {
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateHybrid, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = {"testLongInterpolate", "testLongInterpolateHard"})
    void testInterpolatePartial(long a, long b, double t, double doubleValue, long longValue) {
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolatePartial, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = {"testLongInterpolate", "testLongInterpolateHard"})
    void testInterpolateDD3(long a, long b, double t, double doubleValue, long longValue) {
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateDD3, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = {"testLongInterpolate", "testLongInterpolateHard"})
    void testInterpolateDD4(long a, long b, double t, double doubleValue, long longValue) {
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateDD4, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = {"testLongInterpolate", "testLongInterpolateHard"})
    void testInterpolateDD5(long a, long b, double t, double doubleValue, long longValue) {
        // Use double arithmetic for (b - a) < 2^53 which
        // can have rounding errors in long and double
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateDD5, 1, 1);
    }

    // Hard cases have a different test with a higher tolerance.
    // The ULP tolerance is a reasonable level to ensure the double is close to expected.

    @ParameterizedTest
    @MethodSource(value = "testLongInterpolateHard")
    void testInterpolateDDHard(long a, long b, double t, double doubleValue, long longValue) {
        // Can have double ulp error
        // Can have long rounding error
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateDD, 42, 1);
    }

    @ParameterizedTest
    @MethodSource(value = "testLongInterpolateHard")
    void testInterpolateDD2Hard(long a, long b, double t, double doubleValue, long longValue) {
        // Can have double ulp error
        // Can have long rounding error
        assertLongInterpolate(a, b, t, doubleValue, longValue, InterpolationSource::interpolateDD2, 42, 1);
    }

    private static void assertLongInterpolate(long a, long b, double t,
        double doubleValue, long longValue,
        InterpolationFunction fun, int ulps, int longDelta) {
        final StatisticResult actual = fun.interpolate(a, b, t);
        final Supplier<String> msg = () -> String.format("(%d, %d) * %s", a, b, t);
        if (ulps > 0) {
            final double x = actual.getAsDouble();
            if (doubleValue != x) {
                // Ulp difference.
                // No need to handle signed zeros as the values are not equal.
                final long u = Double.doubleToRawLongBits(doubleValue) - Double.doubleToRawLongBits(x);
                Assertions.assertTrue(Math.abs(u) <= ulps,
                    () -> String.format("(%d, %d) * %s : %s != %s (%d ulp)", a, b, t, doubleValue, x, u));
            }
        } else {
            Assertions.assertEquals(doubleValue, actual.getAsDouble(), msg);
        }
        if (longDelta > 0) {
            final long x = actual.getAsLong();
            if (longValue != x) {
                final long u = longValue - x;
                Assertions.assertTrue(Math.abs(u) <= longDelta,
                    () -> String.format("(%d, %d) * %s : %s != %s (%d delta)", a, b, t, doubleValue, x, u));
            }
        } else {
            Assertions.assertEquals(longValue, actual.getAsLong(), msg);
        }
    }
}
