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
package org.apache.commons.statistics.inference;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link DD}.
 */
class DDTest {
    /** Down scale factors to apply to argument y of f(x, y). */
    private static final double[] DOWN_SCALES = {
        1.0, 0x1.0p-1, 0x1.0p-2, 0x1.0p-5, 0x1.0p-10, 0x1.0p-25, 0x1.0p-51, 0x1.0p-52, 0x1.0p-53, 0x1.0p-100
    };
    /** Scale factors to apply to argument y of f(x, y). */
    private static final double[] SCALES = {
        1.0, 0x1.0p-1, 0x1.0p-2, 0x1.0p-5, 0x1.0p-10, 0x1.0p-25, 0x1.0p-51, 0x1.0p-52, 0x1.0p-53, 0x1.0p-100,
        0x1.0p1, 0x1.0p2, 0x1.0p5, 0x1.0p10, 0x1.0p25, 0x1.0p51, 0x1.0p52, 0x1.0p53, 0x1.0p100
    };
    /** MathContext for division. A double-double has approximately 34 digits of precision so
     * use twice this to allow computation of relative error of the results to a useful precision. */
    private static final MathContext MC_DIVIDE = new MathContext(MathContext.DECIMAL128.getPrecision() * 2);

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds"})
    void testFastTwoSum(double xa, double ya) {
        // |x| > |y|
        double x;
        double y;
        if (Math.abs(xa) < Math.abs(ya)) {
            x = ya;
            y = xa;
        } else {
            x = xa;
            y = ya;
        }
        final DD z = DD.create();
        final BigDecimal bx = bd(x);
        final Supplier<String> msg = () -> String.format("%s+%s", x, y);
        for (final double scale : DOWN_SCALES) {
            final double sy = scale * y;
            DD.fastTwoSum(x, sy, z);
            final double hi = x + sy;
            final double lo = bx.add(bd(sy)).subtract(bd(hi)).doubleValue();
            // fast-two-sum should be exact
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " lo: scale=" + scale);
            Assertions.assertEquals(hi, hi + lo, () -> msg.get() + " hi+lo: scale=" + scale);
            Assertions.assertEquals(hi, z.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);
            // Same as fastTwoDiff
            DD.fastTwoDiff(x, -sy, z);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " fastTwoDiff hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " fastTwoDiff lo: scale=" + scale);
            DD.fastTwoSum(x, -sy, z);
            final double z0 = z.hi();
            final double z1 = z.lo();
            DD.fastTwoDiff(x, sy, z);
            Assertions.assertEquals(z0, z.hi(), () -> msg.get() + " fastTwoSum hi: scale=" + scale);
            Assertions.assertEquals(z1, z.lo(), () -> msg.get() + " fastTwoSum lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds"})
    void testTwoSum(double x, double y) {
        final DD z = DD.create();
        final BigDecimal bx = bd(x);
        final Supplier<String> msg = () -> String.format("%s+%s", x, y);
        for (final double scale : DOWN_SCALES) {
            final double sy = scale * y;
            DD.twoSum(x, sy, z);
            final double hi = x + sy;
            final double lo = bx.add(bd(sy)).subtract(bd(hi)).doubleValue();
            // two-sum should be exact
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " lo: scale=" + scale);
            Assertions.assertEquals(hi, hi + lo, () -> msg.get() + " hi+lo: scale=" + scale);
            Assertions.assertEquals(hi, z.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);
            DD.twoSum(sy, x, z);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " reversed lo: scale=" + scale);
            // Same as twoDiff
            DD.twoDiff(x, -sy, z);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " twoDiff hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " twoDiff lo: scale=" + scale);
            DD.twoSum(x, -sy, z);
            final double z0 = z.hi();
            final double z1 = z.lo();
            DD.twoDiff(x, sy, z);
            Assertions.assertEquals(z0, z.hi(), () -> msg.get() + " twoSum hi: scale=" + scale);
            Assertions.assertEquals(z1, z.lo(), () -> msg.get() + " twoSum lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds"})
    void testCreateAndSet(double x, double y) {
        final DD z = DD.create();
        DD dd = DD.create(x);
        Assertions.assertEquals(x, dd.hi(), "x hi");
        Assertions.assertEquals(0, dd.lo(), "x lo");
        DD.set(y, dd);
        Assertions.assertEquals(y, dd.hi(), "y hi");
        Assertions.assertEquals(0, dd.lo(), "y lo");
        final Supplier<String> msg = () -> String.format("%s+%s", x, y);
        for (final double scale : DOWN_SCALES) {
            final double sy = scale * y;
            DD.twoSum(x, sy, z);
            dd = DD.create(x, sy);
            Assertions.assertEquals(z.hi(), dd.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(z.lo(), dd.lo(), () -> msg.get() + " lo: scale=" + scale);
            dd = DD.create(sy, x);
            Assertions.assertEquals(z.hi(), dd.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(z.lo(), dd.lo(), () -> msg.get() + " lo: scale=" + scale);

            Assertions.assertSame(dd, DD.set(0, 0, dd));
            Assertions.assertEquals(0, dd.doubleValue(), "set(0, 0) doubleValue");
            Assertions.assertSame(dd, DD.set(z.hi(), z.lo(), dd));
            Assertions.assertEquals(z.hi(), dd.hi(), () -> "set(x, xx) hi");
            Assertions.assertEquals(z.lo(), dd.lo(), () -> "set(x, xx) lo");
            Assertions.assertSame(dd, DD.set(sy, dd));
            Assertions.assertEquals(sy, dd.hi(), () -> "set(sy, 0) hi");
            Assertions.assertEquals(0, dd.lo(), () -> "set(sy, 0) lo");
        }
    }

    static Stream<Arguments> twoSumAddeds() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 100; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng), signedNormalDouble(rng)));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testCopy(double x, double xx) {
        final DD s = DD.create(x, xx);
        final DD t = s.copy();
        Assertions.assertNotSame(s, t);
        Assertions.assertEquals(s.hi(), t.hi(), "hi");
        Assertions.assertEquals(s.lo(), t.lo(), "lo");
    }

    static Stream<Arguments> testCopy() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double[] v = {0, 1, 1.23, Double.POSITIVE_INFINITY, Double.NaN};
        for (final double x : v) {
            for (final double xx : v) {
                builder.add(Arguments.of(x, xx * 0x1.0p-53));
            }
        }
        return builder.build();
    }

    /**
     * Test {@link DD#twoProd(double, double, DD)} computes the same result as JDK 9
     * Math.fma(x, y, -x * y) for edge cases.
     */
    @Test
    void testTwoProdSpecialCases() {
        assertProductLow(0.0, 1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProductLow(0.0, -1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProductLow(Double.NaN, 1.0, Double.POSITIVE_INFINITY);
        assertProductLow(Double.NaN, 1.0, Double.NEGATIVE_INFINITY);
        assertProductLow(Double.NaN, 1.0, Double.NaN);
        assertProductLow(0.0, 1.0, Double.MAX_VALUE);
        assertProductLow(Double.NaN, 2.0, Double.MAX_VALUE);
    }

    private static void assertProductLow(double expected, double x, double y) {
        final DD z = DD.twoProd(x, y, DD.create());
        Assertions.assertEquals(x * y, z.hi(), "hi");
        // Requires a delta of 0.0 to assert -0.0 == 0.0
        Assertions.assertEquals(expected, z.lo(), 0.0, "lo");
    }

    @ParameterizedTest
    @MethodSource
    void testTwoProd(double x, double y) {
        final DD z = DD.create();
        final BigDecimal bx = bd(x);
        final Supplier<String> msg = () -> String.format("%s*%s", x, y);
        for (final double scale : DOWN_SCALES) {
            final double sy = scale * y;
            DD.twoProd(x, sy, z);
            final double hi = x * sy;
            final double lo = bx.multiply(bd(sy)).subtract(bd(hi)).doubleValue();
            // two-prod should be exact
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " lo: scale=" + scale);
            Assertions.assertEquals(hi, hi + lo, () -> msg.get() + " hi+lo: scale=" + scale);
            Assertions.assertEquals(hi, z.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);
            DD.twoProd(sy, x, z);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " reversed lo: scale=" + scale);
        }
    }

    static Stream<Arguments> testTwoProd() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 100; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng), signedNormalDouble(rng)));
        }
        // Test scaling when Dekker's split method using multiplication by ~2^27 will
        // overflow
        for (int i = 0; i < 10; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng) * 0x1.0p996, signedNormalDouble(rng)));
            builder.add(Arguments.of(signedNormalDouble(rng) * 0x1.0p1000, signedNormalDouble(rng)));
            builder.add(Arguments.of(signedNormalDouble(rng) * 0x1.0p1022, signedNormalDouble(rng)));
            builder.add(Arguments.of(signedNormalDouble(rng) * 0x1.0p500, signedNormalDouble(rng) * 0x1.0p500));
            builder.add(Arguments.of(signedNormalDouble(rng) * 0x1.0p511, signedNormalDouble(rng) * 0x1.0p511));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"addDouble"})
    void testFastAddDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD s = DD.create();
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+%s", x, xx, y);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            DD.fastAdd(x, xx, sy, s);
            // Check normalised
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(sy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53.
            // A single addition is 2 eps^2.
            // Passes at just over eps^2.
            TestUtils.assertEquals(e, s, 1.0625 * 0x1.0p-106, () -> msg.get() + " scale=" + scale);

            // Same as if low-part of y is zero
            DD.fastAdd(x, xx, sy, 0, s);
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"addDouble"})
    void testAddDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD s = DD.create();
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+%s", x, xx, y);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            DD.add(x, xx, sy, s);
            // Check normalised
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(sy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53
            // A single addition is 2 eps^2. Note that the extra computation in add vs fastAdd
            // does not improve the maximum relative error of the double-double.
            // Note:
            // It may be sporadically failed by add as data is random. The test could be updated
            // to assert the RMS relative error of add is lower than fastAdd.
            TestUtils.assertEquals(e, s, 0x1.0p-106, () -> msg.get() + " scale=" + scale);

            // Additional checks for full add:
            // (Note: These are failed by fastAdd for cases of large cancellation, or
            // non-overlapping addends. For reasonable cases the lo-part is within 4 ulp.)
            // e = full expansion series of m numbers, low suffix is smallest
            // |e - e_m| <= ulp(e_m) -> hi is a 1 ULP approximation to the IEEE double result
            TestUtils.assertEquals(e.doubleValue(), hi, 1, () -> msg.get() + " hi: scale=" + scale);
            // |sum_i^{m-1} (e_i)| <= ulp(e - e_m)
            final double esem = e.subtract(bd(hi)).doubleValue();
            TestUtils.assertEquals(esem, lo, 1, () -> msg.get() + " lo: scale=" + scale);

            // Same as if low-part of y is zero
            DD.add(x, xx, sy, 0, s);
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo: scale=" + scale);
        }
    }

    static Stream<Arguments> addDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        for (int i = 0; i < 100; i++) {
            signedNormalDoubleDouble(rng, s);
            builder.add(Arguments.of(s.hi(), s.lo(), signedNormalDouble(rng)));
        }
        // Cases of large cancellation
        for (int i = 0; i < 10; i++) {
            signedNormalDoubleDouble(rng, s);
            final double x = s.hi();
            final double xx = s.lo();
            final int dir = rng.nextBoolean() ? 1 : -1;
            final double ulp = Math.ulp(x);
            builder.add(Arguments.of(x, xx, -x));
            builder.add(Arguments.of(x, xx, -(x + dir * ulp)));
            builder.add(Arguments.of(x, xx, -(x + dir * ulp * 2)));
            builder.add(Arguments.of(x, xx, -(x + dir * ulp * 3)));
        }
        // Cases requiring correct rounding of low
        for (int i = 0; i < 10; i++) {
            signedNormalDoubleDouble(rng, s);
            final double x = s.hi();
            final double xx = s.lo();
            final double hUlpXX = Math.ulp(xx) / 2;
            final double hUlpXXu = Math.nextUp(hUlpXX);
            final double hUlpXXd = Math.nextDown(hUlpXX);
            builder.add(Arguments.of(x, xx, hUlpXX));
            builder.add(Arguments.of(x, xx, -hUlpXX));
            builder.add(Arguments.of(x, xx, hUlpXXu));
            builder.add(Arguments.of(x, xx, -hUlpXXu));
            builder.add(Arguments.of(x, xx, hUlpXXd));
            builder.add(Arguments.of(x, xx, -hUlpXXd));
        }
        // Create a summation of non-overlapping parts
        for (int i = 0; i < 10; i++) {
            final double x = signedNormalDouble(rng);
            final double xx = Math.ulp(x) / 2;
            final double y = Math.ulp(xx) / 2;
            final double y1 = Math.nextUp(y);
            final double y2 = Math.nextDown(y);
            DD.twoSum(x, xx, s);
            builder.add(Arguments.of(s.hi(), s.lo(), y));
            builder.add(Arguments.of(s.hi(), s.lo(), -y));
            builder.add(Arguments.of(s.hi(), s.lo(), y1));
            builder.add(Arguments.of(s.hi(), s.lo(), -y1));
            builder.add(Arguments.of(s.hi(), s.lo(), y2));
            builder.add(Arguments.of(s.hi(), s.lo(), -y2));
            DD.twoSum(x, Math.nextUp(xx), s);
            builder.add(Arguments.of(s.hi(), s.lo(), y));
            builder.add(Arguments.of(s.hi(), s.lo(), -y));
            builder.add(Arguments.of(s.hi(), s.lo(), y1));
            builder.add(Arguments.of(s.hi(), s.lo(), -y1));
            builder.add(Arguments.of(s.hi(), s.lo(), y2));
            builder.add(Arguments.of(s.hi(), s.lo(), -y2));
            DD.twoSum(x, Math.nextDown(xx), s);
            builder.add(Arguments.of(s.hi(), s.lo(), y));
            builder.add(Arguments.of(s.hi(), s.lo(), -y));
            builder.add(Arguments.of(s.hi(), s.lo(), y1));
            builder.add(Arguments.of(s.hi(), s.lo(), -y1));
            builder.add(Arguments.of(s.hi(), s.lo(), y2));
            builder.add(Arguments.of(s.hi(), s.lo(), -y2));
        }

        // Cases that fail exact summation to a single double if performed incorrectly.
        // These require correct propagation of the round-off to the high-part.
        // The double-double high-part may not be exact but summed with the low-part
        // it should be <= 0.5 ulp of the IEEE double result.
        builder.add(Arguments.of(-1.8903599998005227, 1.2825149462328469E-17, 1.8903599998005232, 1.2807862928011876E-17));
        builder.add(Arguments.of(1.8709715815417154, 2.542250988259237E-17, -1.8709715815417152, 1.982876215341407E-17));
        builder.add(Arguments.of(-1.8246677074340567, 2.158144877411339E-17, 1.8246677074340565, 2.043199561107511E-17));

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"addDoubleDouble"})
    void testFastAddDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD s = DD.create();
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+(%s,%s)", x, xx, y, yy);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            final double syy = scale * yy;
            DD.fastAdd(x, xx, sy, syy, s);
            // Check normalised
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(sy)).add(bd(syy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53.
            // Passes at 2 eps^2.
            TestUtils.assertEquals(e, s, 2 * 0x1.0p-106, () -> msg.get() + " scale=" + scale);

            // Same if reversed
            DD.fastAdd(sy, syy, x, xx, s);
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + "reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + "reversed lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"addDoubleDouble"})
    void testAddDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD s = DD.create();
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+(%s,%s)", x, xx, y, yy);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            final double syy = scale * yy;
            DD.add(x, xx, sy, syy, s);
            // Check normalised
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(sy)).add(bd(syy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53.
            // Note that the extra computation in add vs fastAdd
            // lowers the tolerance to eps^2. This tolerance is consistently failed by fastAdd.
            TestUtils.assertEquals(e, s, 0x1.0p-106, () -> msg.get() + " scale=" + scale);

            // Additional checks for full add.
            // (Note: These are failed by fastAdd for cases of large cancellation, or
            // non-overlapping addends. For reasonable cases the lo-part is within 4 ulp.
            // Thus add creates a double-double that is a better estimate of the first two
            // terms of the full expansion of e.)
            // e = full expansion series of m numbers, low suffix is smallest
            // |e - e_m| <= ulp(e_m) -> hi is a 1 ULP approximation to the IEEE double result
            TestUtils.assertEquals(e.doubleValue(), hi, 1, () -> msg.get() + " hi: scale=" + scale);
            // |sum_i^{m-1} (e_i)| <= ulp(e - e_m)
            final double esem = e.subtract(bd(hi)).doubleValue();
            TestUtils.assertEquals(esem, lo, 1, () -> msg.get() + " lo: scale=" + scale);

            // Same if reversed
            DD.add(sy, syy, x, xx, s);
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + "reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + "reversed lo: scale=" + scale);
        }
    }

    static Stream<Arguments> addDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        final DD t = DD.create();
        for (int i = 0; i < 100; i++) {
            signedNormalDoubleDouble(rng, s);
            signedNormalDoubleDouble(rng, t);
            builder.add(Arguments.of(s.hi(), s.lo(), t.hi(), t.lo()));
        }
        // Cases of large cancellation
        for (int i = 0; i < 10; i++) {
            signedNormalDoubleDouble(rng, s);
            final double x = s.hi();
            final double xx = s.lo();
            final int dir = rng.nextBoolean() ? 1 : -1;
            final double ulp = Math.ulp(x);
            double yy = signedNormalDouble(rng);
            yy = Math.scalb(yy, Math.getExponent(xx));
            add(builder, s, -x, -xx, t);
            add(builder, s, -x, -(xx + dir * ulp), t);
            add(builder, s, -x, -(xx + dir * ulp * 2), t);
            add(builder, s, -x, -(xx + dir * ulp * 3), t);
            add(builder, s, -x, yy, t);
            add(builder, s, -x, yy + ulp, t);
            add(builder, s, -(x + dir * ulp), -xx, t);
            add(builder, s, -(x + dir * ulp), -(xx + dir * ulp), t);
            add(builder, s, -(x + dir * ulp), -(xx + dir * ulp * 2), t);
            add(builder, s, -(x + dir * ulp), -(xx + dir * ulp * 3), t);
            add(builder, s, -(x + dir * ulp), yy, t);
            add(builder, s, -(x + dir * ulp), yy + ulp, t);
        }
        // Cases requiring correct rounding of low
        for (int i = 0; i < 10; i++) {
            signedNormalDoubleDouble(rng, s);
            final double x = s.hi();
            final double xx = s.lo();
            final int dir = rng.nextBoolean() ? 1 : -1;
            final double ulpX = Math.ulp(x);
            final double hUlpXX = Math.ulp(xx) / 2;
            final double hUlpXXu = Math.nextUp(hUlpXX);
            final double hUlpXXd = Math.nextDown(hUlpXX);
            add(builder, s, -x,  hUlpXX, t);
            add(builder, s, -x, -hUlpXX, t);
            add(builder, s, -x, hUlpXXu, t);
            add(builder, s, -x, -hUlpXXu, t);
            add(builder, s, -x, hUlpXXd, t);
            add(builder, s, -x, -hUlpXXd, t);
            add(builder, s, -(x + dir * ulpX),  hUlpXX, t);
            add(builder, s, -(x + dir * ulpX), -hUlpXX, t);
            add(builder, s, -(x + dir * ulpX), hUlpXXu, t);
            add(builder, s, -(x + dir * ulpX), -hUlpXXu, t);
            add(builder, s, -(x + dir * ulpX), hUlpXXd, t);
            add(builder, s, -(x + dir * ulpX), -hUlpXXd, t);
        }
        // Create a summation of non-overlapping parts
        for (int i = 0; i < 10; i++) {
            final double x = signedNormalDouble(rng);
            final double xx = Math.ulp(x) / 2;
            final double y = Math.ulp(xx) / 2;
            final double yy = Math.ulp(y) / 2;
            final double yy1 = Math.nextUp(yy);
            final double yy2 = Math.nextDown(yy);
            DD.twoSum(x, xx, s);
            add(builder, s, y, yy, t);
            add(builder, s, y, -yy, t);
            add(builder, s, y, yy1, t);
            add(builder, s, y, -yy1, t);
            add(builder, s, y, yy2, t);
            add(builder, s, y, -yy2, t);
            add(builder, s, -y, yy, t);
            add(builder, s, -y, -yy, t);
            add(builder, s, -y, yy1, t);
            add(builder, s, -y, -yy1, t);
            add(builder, s, -y, yy2, t);
            add(builder, s, -y, -yy2, t);
            DD.twoSum(x, Math.nextUp(xx), s);
            add(builder, s, y, yy, t);
            add(builder, s, y, -yy, t);
            add(builder, s, y, yy1, t);
            add(builder, s, y, -yy1, t);
            add(builder, s, y, yy2, t);
            add(builder, s, y, -yy2, t);
            add(builder, s, -y, yy, t);
            add(builder, s, -y, -yy, t);
            add(builder, s, -y, yy1, t);
            add(builder, s, -y, -yy1, t);
            add(builder, s, -y, yy2, t);
            add(builder, s, -y, -yy2, t);
            DD.twoSum(x, Math.nextDown(xx), s);
            add(builder, s, y, yy, t);
            add(builder, s, y, -yy, t);
            add(builder, s, y, yy1, t);
            add(builder, s, y, -yy1, t);
            add(builder, s, y, yy2, t);
            add(builder, s, y, -yy2, t);
            add(builder, s, -y, yy, t);
            add(builder, s, -y, -yy, t);
            add(builder, s, -y, yy1, t);
            add(builder, s, -y, -yy1, t);
            add(builder, s, -y, yy2, t);
            add(builder, s, -y, -yy2, t);
        }
        return builder.build();
    }

    /**
     * Adds the two double-double numbers as arguments. Ensured the (x,yy) values is normalised.
     * The argument {@code t} is used for working.
     */
    private static Stream.Builder<Arguments> add(Stream.Builder<Arguments> builder,
            DD x, double y, double yy, DD t) {
        DD.fastTwoSum(y, yy, t);
        builder.add(Arguments.of(x.hi(), x.lo(), t.hi(), t.lo()));
        return builder;
    }

    @ParameterizedTest
    @MethodSource
    void testMultiplyDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD s = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)*%s", x, xx, y);

        DD.multiply(x, xx, y, s);
        // Check normalised
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).multiply(bd(y));
        // double-double multiplication should be within 16 eps^2 with eps = 2^-53.
        // a single multiply is 4 eps^2
        TestUtils.assertEquals(e, s, 4 * 0x1.0p-106, () -> msg.get());

        // Same as if low-part of y is zero
        DD.multiply(x, xx, y, 0, s);
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo");
    }

    static Stream<Arguments> testMultiplyDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        for (int i = 0; i < 300; i++) {
            signedNormalDoubleDouble(rng, s);
            builder.add(Arguments.of(s.hi(), s.lo(), signedNormalDouble(rng)));
        }
        // Multiply by zero
        for (int i = 0; i < 3; i++) {
            signedNormalDoubleDouble(rng, s);
            builder.add(Arguments.of(s.hi(), s.lo(), 0));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testMultiplyDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD s = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)*(%s,%s)", x, xx, y, yy);

        DD.multiply(x, xx, y, yy, s);
        // Check normalised
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).multiply(bd(y).add(bd(yy)));
        // double-double multiplication should be within 16 eps^2 with eps = 2^-53.
        // This passes at 4 eps^2
        TestUtils.assertEquals(e, s, 4 * 0x1.0p-106, () -> msg.get());

        // Same if reversed
        DD.multiply(y, yy, x, xx, s);
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " reversed hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " reversed lo");

        // Fast multiply is the same (except signed zeros so use a delta of 0.0)
        DD.uncheckedMultiply(x, xx, y, yy, s);
        Assertions.assertEquals(hi, s.hi(), 0.0, () -> msg.get() + " uncheckedMultiply hi");
        Assertions.assertEquals(lo, s.lo(), 0.0, () -> msg.get() + " uncheckedMultiply lo");
    }

    static Stream<Arguments> testMultiplyDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        final DD t = DD.create();
        for (int i = 0; i < 300; i++) {
            signedNormalDoubleDouble(rng, s);
            signedNormalDoubleDouble(rng, t);
            builder.add(Arguments.of(s.hi(), s.lo(), t.hi(), t.lo()));
        }
        // Multiply by zero
        for (int i = 0; i < 5; i++) {
            signedNormalDoubleDouble(rng, s);
            builder.add(Arguments.of(s.hi(), s.lo(), 0.0, 0.0));
            builder.add(Arguments.of(s.hi(), s.lo(), -0.0, 0.0));
            builder.add(Arguments.of(s.hi(), s.lo(), 0.0, -0.0));
            builder.add(Arguments.of(s.hi(), s.lo(), -0.0, -0.0));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testDivide(double x, double y) {
        final DD s = DD.create();
        final Supplier<String> msg = () -> String.format("%s/%s", x, y);

        DD.divide(x, y, s);
        // Check normalised
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).divide(bd(y), MC_DIVIDE);
        // double-double has 106-bits precision.
        // This passes with a relative error of 2^-107.
        TestUtils.assertEquals(e, s, 0x1.0p-107, () -> msg.get());

        // Same as if low-part of x and y is zero
        DD.divide(x, 0, y, 0, s);
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " xx=yy=0 hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " xx=yy=0 lo");

        // Same as uncheckedDivide
        DD.uncheckedDivide(x, y, s);
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " uncheckedDivide hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " uncheckedDivide lo");
    }

    static Stream<Arguments> testDivide() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 300; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng), signedNormalDouble(rng)));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testDivideDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD s = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)/(%s,%s)", x, xx, y, yy);

        DD.divide(x, xx, y, yy, s);
        // Check normalised
        final double hi = s.hi();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).divide(bd(y).add(bd(yy)), MC_DIVIDE);
        // double-double multiplication should be within 16 eps^2 with eps = 2^-53.
        // Division is similar. This passes at 4 eps^2
        TestUtils.assertEquals(e, s, 4 * 0x1.0p-106, () -> msg.get());
    }

    static Stream<Arguments> testDivideDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        final DD t = DD.create();
        for (int i = 0; i < 300; i++) {
            signedNormalDoubleDouble(rng, s);
            signedNormalDoubleDouble(rng, t);
            builder.add(Arguments.of(s.hi(), s.lo(), t.hi(), t.lo()));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testInverseDoubleDouble(double y, double yy) {
        assertNormalized(y, yy, "y");
        final DD s = DD.create();
        final Supplier<String> msg = () -> String.format("1/(%s,%s)", y, yy);

        DD.inverse(y, yy, s);
        // Check normalised
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = BigDecimal.ONE.divide(bd(y).add(bd(yy)), MC_DIVIDE);
        // double-double has 106-bits precision.
        // This passes with a relative error of 2^-105.
        TestUtils.assertEquals(e, s, 2 * 0x1.0p-106, () -> msg.get());

        // Same as if using divide
        DD.divide(1, 0, y, yy, s);
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " (x,xx)=(1,0) hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " (x,xx)=(1,0) lo");
    }

    static Stream<Arguments> testInverseDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        for (int i = 0; i < 100; i++) {
            signedNormalDoubleDouble(rng, s);
            builder.add(Arguments.of(s.hi(), s.lo()));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testLdexp(double x, double xx) {
        final DD s = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)", x, xx);
        // Scales around powers of 2 up to the limit of 2^12 = 4096
        for (int p = 0; p <= 12; p++) {
            final int b = 1 << p;
            for (int i = -1; i <= 1; i++) {
                final int n = b + i;
                DD.ldexp(x, xx, n, s);
                Assertions.assertEquals(Math.scalb(x, n), s.hi(), () -> msg.get() + " hi: scale=" + n);
                Assertions.assertEquals(Math.scalb(xx, n), s.lo(), () -> msg.get() + " lo: scale=" + n);
                DD.ldexp(x, xx, -n, s);
                Assertions.assertEquals(Math.scalb(x, -n), s.hi(), () -> msg.get() + " hi: scale=" + -n);
                Assertions.assertEquals(Math.scalb(xx, -n), s.lo(), () -> msg.get() + " lo: scale=" + -n);
            }
        }
        // Extreme scaling
        for (final int n : new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) {
            DD.ldexp(x, xx, n, s);
            Assertions.assertEquals(Math.scalb(x, n), s.hi(), () -> msg.get() + " hi: scale=" + n);
            Assertions.assertEquals(Math.scalb(xx, n), s.lo(), () -> msg.get() + " lo: scale=" + n);
        }
    }

    static Stream<Arguments> testLdexp() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        for (int i = 0; i < 10; i++) {
            signedNormalDoubleDouble(rng, s);
            builder.add(Arguments.of(s.hi(), s.lo()));
        }
        final double[] v = {1, 0, Double.MAX_VALUE, Double.MIN_NORMAL, Double.MIN_VALUE, Math.PI,
            Math.E, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (final double x : v) {
            for (final double xx : v) {
                // Here we do not care if the value is normalised: |x| > |xx|
                // The test is to check scaling is performed on both numbers independently.
                builder.add(Arguments.of(x, xx));
            }
        }
        return builder.build();
    }


    @ParameterizedTest
    @CsvSource({
        // Non-scalable numbers:
        // exponent is always zero, (x,xx) is unchanged
        "0.0, 0.0, 0, 0.0, 0.0",
        "0.0, -0.0, 0, 0.0, -0.0",
        "NaN, 0.0, 0, NaN, 0.0",
        "NaN, NaN, 0, NaN, NaN",
        "Infinity, 0.0, 0, Infinity, 0.0",
        "Infinity, NaN, 0, Infinity, NaN",
        // Normalisation of (1, 0)
        "1.0, 0, 1, 0.5, 0",
        "-1.0, 0, 1, -0.5, 0",
        // Power of 2 with round-off to reduce the magnitude
        "0.5, -5.551115123125783E-17, -1, 1.0, -1.1102230246251565E-16",
        "1.0, -1.1102230246251565E-16, 0, 1.0, -1.1102230246251565E-16",
        "2.0, -2.220446049250313E-16, 1, 1.0, -1.1102230246251565E-16",
        "0.5, 5.551115123125783E-17, 0, 0.5, 5.551115123125783E-17",
        "1.0, 1.1102230246251565E-16, 1, 0.5, 5.551115123125783E-17",
        "2.0, 2.220446049250313E-16, 2, 0.5, 5.551115123125783E-17",
    })
    void testFrexpEdgeCases(double x, double xx, int exp, double fx, double fxx) {
        final DD f = DD.create();
        int e = DD.frexp(x, xx, f);
        Assertions.assertEquals(exp, e, "exp");
        Assertions.assertEquals(fx, f.hi(), "hi");
        Assertions.assertEquals(fxx, f.lo(), "lo");
        e = DD.frexp(-x, -xx, f);
        Assertions.assertEquals(exp, e, "exp");
        Assertions.assertEquals(-fx, f.hi(), "hi");
        Assertions.assertEquals(-fxx, f.lo(), "lo");
    }

    @ParameterizedTest
    @MethodSource
    void testFrexp(double x, double xx) {
        Assertions.assertTrue(Double.isFinite(x) && x != 0, "Invalid x: " + x);
        assertNormalized(x, xx, "x");

        final DD f = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)", x, xx);
        final int e = DD.frexp(x, xx, f);

        final double hi = f.hi();
        final double lo = f.lo();
        final double ahi = Math.abs(hi);
        Assertions.assertTrue(0.5 <= ahi && ahi <= 1, () -> msg.get() + " hi");

        // Get the exponent handling sub-normal numbers
        int exp = Math.abs(x) < 0x1.0p-900 ?
            Math.getExponent(x * 0x1.0p200) - 200 :
            Math.getExponent(x);

        // Math.getExponent returns the value for a fractional part in [1, 2) not [0.5, 1)
        exp += 1;

        // Edge case where the exponent is smaller
        if (Math.abs(ahi) == 1) {
            if (hi == 1) {
                Assertions.assertTrue(lo < 0, () -> msg.get() + " (f,ff) is not < 1");
            } else {
                Assertions.assertTrue(lo > 0, () -> msg.get() + " (f,ff) is not > -1");
            }
            exp -= 1;
        }

        Assertions.assertEquals(exp, e, () -> msg.get() + " exponent");

        // Check the bits are the same.
        Assertions.assertEquals(x, Math.scalb(hi, exp), () -> msg.get() + " scaled f hi");
        Assertions.assertEquals(xx, Math.scalb(lo, exp), () -> msg.get() + " scaled f lo");

        // Check round-trip
        DD.ldexp(hi, lo, exp, f);
        Assertions.assertEquals(x, f.hi(), () -> msg.get() + " ldexp f hi");
        Assertions.assertEquals(xx, f.lo(), () -> msg.get() + " ldexp f lo");
    }

    static Stream<Arguments> testFrexp() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        for (int i = 0; i < 10; i++) {
            signedNormalDoubleDouble(rng, s);
            for (final double scale : SCALES) {
                builder.add(Arguments.of(s.hi() * scale, s.lo() * scale));
            }
        }
        // Sub-normal numbers
        final double[] scales = IntStream.of(-1000, -1022, -1023, -1024, -1050, -1074)
            .mapToDouble(n -> Math.scalb(1.0, n)).toArray();
        for (int i = 0; i < 5; i++) {
            signedNormalDoubleDouble(rng, s);
            for (final double scale : scales) {
                builder.add(Arguments.of(s.hi() * scale, s.lo() * scale));
            }
        }
        // x is power of 2
        for (int i = 0; i < 3; i++) {
            for (final double scale : SCALES) {
                builder.add(Arguments.of(scale, signedNormalDouble(rng) * scale * 0x1.0p-55));
                builder.add(Arguments.of(scale, 0));
            }
        }
        // Extreme case should change x to 1.0 when xx is opposite sign
        builder.add(Arguments.of(0.5, Double.MIN_VALUE));
        builder.add(Arguments.of(0.5, -Double.MIN_VALUE));
        builder.add(Arguments.of(-0.5, Double.MIN_VALUE));
        builder.add(Arguments.of(-0.5, -Double.MIN_VALUE));
        return builder.build();
    }

    @ParameterizedTest
    @CsvSource({
        // Math.pow(x, 0) == 1, even for non-finite values
        "0.0, 0.0, 0, 1.0, 0.0",
        "1.23, 0.0, 0, 1.0, 0.0",
        "1.0, 0.0, 0, 1.0, 0.0",
        "Infinity, 0.0, 0, 1.0, 0.0",
        "NaN, 0.0, 0, 1.0, 0.0",
        // Math.pow(0.0, n) == +/- 0.0
        "0.0, 0.0, 1, 0.0, 0.0",
        "0.0, 0.0, 2, 0.0, 0.0",
        "-0.0, 0.0, 1, -0.0, 0.0",
        "-0.0, 0.0, 2, 0.0, 0.0",
        // Math.pow(1, n) == 1
        "1.0, 0.0, 1, 1.0, 0.0",
        "1.0, 0.0, 2, 1.0, 0.0",
        // Math.pow(-1, n) == +/-1 - requires round-off sign propagation
        "-1.0, 0.0, 1, -1.0, 0.0",
        "-1.0, 0.0, 2, 1.0, -0.0",
        "-1.0, -0.0, 1, -1.0, -0.0",
        "-1.0, -0.0, 2, 1.0, 0.0",
        // Math.pow(0.0, -n)
        "0.0, 0.0, -1, Infinity, 0.0",
        "0.0, 0.0, -2, Infinity, 0.0",
        "-0.0, 0.0, -1, -Infinity, 0.0",
        "-0.0, 0.0, -2, Infinity, 0.0",
        // NaN / Infinite is IEEE pow result for x
        "Infinity, 0.0, 1, Infinity, 0.0, 0",
        "-Infinity, 0.0, 1, -Infinity, 0.0, 0",
        "-Infinity, 0.0, 2, Infinity, 0.0, 0",
        "Infinity, 0.0, -1, 0.0, 0.0, 0",
        "-Infinity, 0.0, -1, -0.0, 0.0, 0",
        "-Infinity, 0.0, -2, 0.0, 0.0, 0",
        "NaN, 0.0, 1, NaN, 0.0, 0",
        // Inversion creates infinity (sub-normal x^-n < 2.22e-308)
        // Signed zeros should match inversion when the result is large and finite.
        "1e-312, 0.0, -1, Infinity, -0.0",
        "1e-312, -0.0, -1, Infinity, -0.0",
        "-1e-312, 0.0, -1, -Infinity, 0.0",
        "-1e-312, -0.0, -1, -Infinity, 0.0",
        "1e-156, 0.0, -2, Infinity, -0.0",
        "1e-156, -0.0, -2, Infinity, -0.0",
        "-1e-156, 0.0, -2, Infinity, -0.0",
        "-1e-156, -0.0, -2, Infinity, -0.0",
        "1e-106, 0.0, -3, Infinity, -0.0",
        "1e-106, -0.0, -3, Infinity, -0.0",
        "-1e-106, 0.0, -3, -Infinity, 0.0",
        "-1e-106, -0.0, -3, -Infinity, 0.0",
    })
    void testSimplePowEdgeCases(double x, double xx, int n, double z, double zz) {
        final DD f = DD.create();
        DDExt.simplePow(x, xx, n, f);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
    }

    @ParameterizedTest
    @MethodSource
    void testSimplePow(double x, double xx, int n, double eps) {
        assertNormalized(x, xx, "x");
        final DD s = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        DDExt.simplePow(x, xx, n, s);
        // Check normalised
        final double hi = s.hi();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        TestUtils.assertEquals(e, s, eps, () -> msg.get());
    }

    static Stream<Arguments> testSimplePow() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();

        // Note the negative power is essentially just the same result as x^n combined with
        // the error of the inverse operation. This is far more accurate than simplePow
        // and we can use the same relative error for both.

        // Small powers are around the precision of a double 2^-53
        // No overflow when n < 10
        for (int i = 0; i < 100; i++) {
            signedNormalDoubleDouble(rng, s);
            final int n = rng.nextInt(2, 10);
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-53));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-53));
        }

        // Trigger use of the Taylor series (1+z)^n >> 1
        // z = xx/x so is <= 2^-53 for a normalised double-double.
        // n * log1p(z) > log(1 + y)
        // n ~ log1p(y) / log1p(eps)
        // y       n
        // 2^-45   256
        // 2^-44   512
        // 2^-43   1024
        // 2^-40   8192
        // 2^-35   262144
        // 2^-b    2^(53-b)

        // Medium powers where the value of a normalised double will not overflow.
        // Here Math.pow(x, n) alone can be 3 orders of magnitude worse (~10 bits less precision).
        for (int i = 0; i < 100; i++) {
            signedNormalDoubleDouble(rng, s);
            final int n = rng.nextInt(512, 1024);
            // Some random failures at 2^-53
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-52));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-52));
        }

        // Large powers to trigger the case for n > 1e8 (100000000), or n * z > 1e-8.
        // Here Math.pow(x, n) alone can be 9 orders of magnitude worse (~30 bits less precision).
        // Only applicable when the value will not over/underflow.
        // Note that we wish to use n > 1e8 to trigger the condition more frequently.
        // The limit for BigDecimal is 1e9 - 1 so use half of that.
        // Here we use a value in [0.5, 1) and avoid underflow for the double-double
        // which occurs when the high part for the result is close to 2^-958.
        // x^n = 2^-958
        // x ~ exp(log(2^-958) / n) ~ 0.99999867...
        final int n = (int) 5e8;
        final double x = Math.exp(Math.log(0x1.0p-958) / n);
        for (int i = 0; i < 100; i++) {
            final double hi = rng.nextDouble(x, 1);
            // hi will have an exponent of -1 as it is in [0.5, 1).
            // Scale some random bits to add on to it.
            final double lo = signedNormalDouble(rng) * 0x1.0p-53;
            DD.fastTwoSum(hi, lo, s);
            // Some random failures at 2^-53
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-52));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-52));
        }

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledEdgeCases"})
    void testSimplePowScaledEdgeCases(double x, double xx, int n, double z, double zz, long exp) {
        final DD f = DD.create();
        final long e = DDExt.simplePowScaled(x, xx, n, f);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
        Assertions.assertEquals(exp, e, "exp");
    }

    /**
     * Test cases of {@link DDExt#simplePowScaled(double, double, int, DD)} where no scaling is
     * required. It should be the same as {@link DDExt#simplePow(double, double, int, DD)}.
     */
    @ParameterizedTest
    @CsvSource({
        "1.23, 0.0, 3",
        "1.23, 0.0, -3",
        "1.23, 1e-16, 2",
        "1.23, 1e-16, -2",
        // No underflow - Do not get close to underflowing the low part
        "0.5, 1e-17, 900",
        // x > sqrt(0.5)
        "0.75, 1e-17, 2000",  // 1.33e-250
        "0.9, 1e-17, 5000",   // 1.63e-229
        "0.99, 1e-17, 50000", // 5.75e-219
        "0.75, 1e-17, 100",   // (safe n)
        "0.9999999999999999, 1e-17, 2147483647", // (safe x)
        // No overflow
        "2.0, 1e-16, 1000",
        // 2x < sqrt(0.5)
        "1.5, 1e-16, 1500",   // 1.37e264
        "1.1, 1e-16, 6000",   // 2.27e248
        "1.01, 1e-16, 60000", // 1.92e259
        "2.0, 1e-16, 100",   // (safe n)
        "1.0000000000000002, 1e-17, 2147483647", // (safe x)
    })
    void testSimplePowScaledSafe(double x, double xx, int n) {
        final DD f = DD.create();
        final DD z = DD.create();
        final long exp = DDExt.simplePowScaled(x, xx, n, f);
        // Same
        DDExt.simplePow(x, xx, n, z);
        final long ez = DD.frexp(z.hi(), z.lo(), z);
        Assertions.assertEquals(z.hi(), f.hi(), "hi");
        Assertions.assertEquals(z.lo(), f.lo(), "lo");
        Assertions.assertEquals(ez, exp, "exp");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaled"})
    void testSimplePowScaled(double x, double xx, int n, double eps, double ignored) {
        assertNormalized(x, xx, "x");
        final DD f = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final long exp = DDExt.simplePowScaled(x, xx, n, f);

        // Check normalised
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        TestUtils.assertScaledEquals(e, f, exp, eps, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledLargeN"})
    void testSimplePowScaledLargeN(double x, double xx, int n, long e, BigDecimal expected, double eps, double ignored) {
        final DD f = DD.create();
        final long exp = DDExt.simplePowScaled(x, xx, n, f);
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        // Check normalised
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        Assertions.assertEquals(e, exp, () -> msg.get() + " exponent");
        TestUtils.assertEquals(expected, f, eps, () -> msg.get());
    }

    /**
     * Test computing the square of a double (no low part).
     * This effectively tests squareLowUnscaled via the public power function.
     */
    @ParameterizedTest
    @MethodSource(value = {"testPowScaledSquare"})
    void testFastPowScaledSquare(double x) {
        final DD z = DD.create();
        final DD x2 = DD.create();
        final Supplier<String> msg = () -> String.format("%s^2", x);

        // Two product is exact
        DD.twoProd(x, x, z);

        final long e = DD.fastPowScaled(x, 0, 2, x2);
        final double hi = Math.scalb(x2.hi(), (int) e);
        final double lo = Math.scalb(x2.lo(), (int) e);

        // Should be exact
        Assertions.assertEquals(z.hi(), hi, () -> msg.get() + " hi");
        Assertions.assertEquals(z.lo(), lo, () -> msg.get() + " lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledEdgeCases"})
    void testFastPowScaledEdgeCases(double x, double xx, int n, double z, double zz, long exp) {
        final DD f = DD.create();
        final long e = DD.fastPowScaled(x, xx, n, f);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
        Assertions.assertEquals(exp, e, "exp");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledSmall", "testPowScaled"})
    void testFastPowScaled(double x, double xx, int n, double ignored, double eps) {
        assertNormalized(x, xx, "x");
        final DD f = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final long exp = DD.fastPowScaled(x, xx, n, f);

        // Check normalised
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        TestUtils.assertScaledEquals(e, f, exp, eps, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledLargeN"})
    void testFastPowScaledLargeN(double x, double xx, int n, long e, BigDecimal expected, double ignored, double eps) {
        final DD f = DD.create();
        final long exp = DD.fastPowScaled(x, xx, n, f);
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        // Check normalised
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        Assertions.assertEquals(e, exp, () -> msg.get() + " exponent");
        TestUtils.assertEquals(expected, f, eps, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledEdgeCases"})
    void testPowScaledEdgeCases(double x, double xx, int n, double z, double zz, long exp) {
        final DD f = DD.create();
        final long e = DD.powScaled(x, xx, n, f);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
        Assertions.assertEquals(exp, e, "exp");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledSmall", "testPowScaled"})
    void testPowScaled(double x, double xx, int n, double ignored, double ignored2) {
        assertNormalized(x, xx, "x");
        final DD f = DD.create();
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final long exp = DD.powScaled(x, xx, n, f);

        // Check normalised
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        // Javadoc for the method states accuracy is 1 ULP to be conservative.
        // If correctly performed in triple-double precision it should be exact except
        // for cases of final rounding error when converted to double-double.
        // Test typically passes at: 0.5 * eps with eps = 2^-106.
        // Higher powers may have lower accuracy but are not tested.
        // Update tolerance to 1.0625 * eps as 1 case of rounding error has been observed.
        TestUtils.assertScaledEquals(e, f, exp, 0x1.1p-107, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledLargeN"})
    void testPowScaledLargeN(double x, double xx, int n, long e, BigDecimal expected, double ignored, double ignored2) {
        final DD f = DD.create();
        final long exp = DD.powScaled(x, xx, n, f);
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        // Check normalised
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        Assertions.assertEquals(e, exp, () -> msg.get() + " exponent");
        // Accuracy is that of a double-double number: 0.5 * eps with eps = 2^-106
        TestUtils.assertEquals(expected, f, 0x1.0p-107, () -> msg.get());
    }

    static Stream<Arguments> testPowScaledEdgeCases() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double inf = Double.POSITIVE_INFINITY;
        final double nan = Double.NaN;
        // Math.pow(x, 0) == 1, even for non-finite values (fractional representation)
        builder.add(Arguments.of(0.0, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(1.23, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(1.0, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(inf, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(nan, 0.0, 0, 0.5, 0.0, 1));
        // Math.pow(0.0, n) == +/- 0.0 (no fractional representation)
        builder.add(Arguments.of(0.0, 0.0, 1, 0.0, 0.0, 0));
        builder.add(Arguments.of(0.0, 0.0, 2, 0.0, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, 1, -0.0, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, 2, 0.0, 0.0, 0));
        // Math.pow(1, n) == 1 (fractional representation)
        builder.add(Arguments.of(1.0, 0.0, 1, 0.5, 0.0, 1));
        builder.add(Arguments.of(1.0, 0.0, 2, 0.5, 0.0, 1));
        // Math.pow(-1, n) == +/-1 (fractional representation) - requires round-off sign propagation
        builder.add(Arguments.of(-1.0, 0.0, 1, -0.5, 0.0, 1));
        builder.add(Arguments.of(-1.0, 0.0, 2, 0.5, -0.0, 1));
        builder.add(Arguments.of(-1.0, -0.0, 1, -0.5, -0.0, 1));
        builder.add(Arguments.of(-1.0, -0.0, 2, 0.5, 0.0, 1));
        // Math.pow(0.0, -n) - No fractional representation
        builder.add(Arguments.of(0.0, 0.0, -1, inf, 0.0, 0));
        builder.add(Arguments.of(0.0, 0.0, -2, inf, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, -1, -inf, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, -2, inf, 0.0, 0));
        // NaN / Infinite is IEEE pow result for x
        builder.add(Arguments.of(inf, 0.0, 1, inf, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, 1, -inf, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, 2, inf, 0.0, 0));
        builder.add(Arguments.of(inf, 0.0, -1, 0.0, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, -1, -0.0, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, -2, 0.0, 0.0, 0));
        builder.add(Arguments.of(nan, 0.0, 1, nan, 0.0, 0));
        // Hit edge case of zero low part
        builder.add(Arguments.of(0.5, 0.0, -1, 0.5, 0.0, 2));
        builder.add(Arguments.of(1.0, 0.0, -1, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, -1, 0.5, 0.0, 0));
        builder.add(Arguments.of(4.0, 0.0, -1, 0.5, 0.0, -1));
        builder.add(Arguments.of(0.5, 0.0, 2, 0.5, 0.0, -1));
        builder.add(Arguments.of(1.0, 0.0, 2, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, 2, 0.5, 0.0, 3));
        builder.add(Arguments.of(4.0, 0.0, 2, 0.5, 0.0, 5));
        // Exact power of two (representable)
        // Math.pow(0.5, 123) == 0.5 * Math.scalb(1.0, -122)
        // Math.pow(2.0, 123) == 0.5 * Math.scalb(1.0, 124)
        builder.add(Arguments.of(0.5, 0.0, 123, 0.5, 0.0, -122));
        builder.add(Arguments.of(1.0, 0.0, 123, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, 123, 0.5, 0.0, 124));
        builder.add(Arguments.of(0.5, 0.0, -123, 0.5, 0.0, 124));
        builder.add(Arguments.of(1.0, 0.0, -123, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, -123, 0.5, 0.0, -122));
        // Exact power of two (not representable)
        builder.add(Arguments.of(0.5, 0.0, 12345, 0.5, 0.0, -12344));
        builder.add(Arguments.of(1.0, 0.0, 12345, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, 12345, 0.5, 0.0, 12346));
        builder.add(Arguments.of(0.5, 0.0, -12345, 0.5, 0.0, 12346));
        builder.add(Arguments.of(1.0, 0.0, -12345, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, -12345, 0.5, 0.0, -12344));
        return builder.build();
    }

    static Stream<Arguments> testPowScaled() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();

        // Note the negative power is essentially just the same result as x^n combined with
        // the error of the inverse operation. This is far more accurate than simplePow
        // and we can use the same relative error for both.

        // This method uses two epsilon values.
        // The first is for simplePowScaled, the second for fastPowScaled.
        // van Mulbregt (2018) pp 22: Error of a compensated pow is ~ 16(n-1) eps^2.
        // The limit is:
        // Math.log(16.0 * (Integer.MAX_VALUE-1) * 0x1.0p-106) / Math.log(2) = -71.0
        // For this test the thresholds are slightly above this limit.
        // Note: powScaled does not require an epsilon as it is ~ eps^2.

        // Powers that approach and do overflow.
        // Here the fractional representation does not overflow.
        // min = 1.5^1700 = 2.26e299
        // max ~ 2^1799
        for (int i = 0; i < 100; i++) {
            final double x = 1 + rng.nextDouble() / 2;
            final double xx = signedNormalDouble(rng) * 0x1.0p-52;
            DD.fastTwoSum(x, xx, s);
            final int n = rng.nextInt(1700, 1800);
            // Math.log(16.0 * (1799) * 0x1.0p-106) / Math.log(2) = -91.2
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-52, 0x1.0p-94));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-52, 0x1.0p-94));
        }

        // Powers that approach and do underflow
        // Here the fractional representation does not overflow.
        // max = 0.75^2400 = 1.41e-300
        // min ~ 2^-2501
        for (int i = 0; i < 100; i++) {
            final double x = 0.5 + rng.nextDouble() / 4;
            final double xx = signedNormalDouble(rng) * 0x1.0p-53;
            DD.fastTwoSum(x, xx, s);
            final int n = rng.nextInt(2400, 2500);
            // Math.log(16.0 * (2499) * 0x1.0p-106) / Math.log(2) = -90.7
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-51, 0x1.0p-93));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-51, 0x1.0p-93));
        }

        // Powers where the fractional representation overflow/underflow
        // x close to sqrt(2) in range [1.4, 1.42). Smallest power:
        // 1.4^5000 = 4.37e730
        // 0.71^5000 = 1.96e-744
        for (int i = 0; i < 100; i++) {
            final double x = 1.4 + rng.nextDouble() / 50;
            final double xx = signedNormalDouble(rng) * 0x1.0p-52;
            DD.fastTwoSum(x, xx, s);
            final int n = rng.nextInt(5000, 6000);
            // Math.log(16.0 * (5999) * 0x1.0p-106) / Math.log(2) = -89.4
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-50, 0x1.0p-90));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-50, 0x1.0p-90));
        }

        // Large powers
        // These lose approximately 10-bits of precision in the double result
        for (int i = 0; i < 20; i++) {
            final double x = 1.4 + rng.nextDouble() / 50;
            final double xx = signedNormalDouble(rng) * 0x1.0p-52;
            DD.fastTwoSum(x, xx, s);
            final int n = rng.nextInt(500000, 600000);
            // Math.log(16.0 * (599999) * 0x1.0p-106) / Math.log(2) = -82.8
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-43, 0x1.0p-85));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-43, 0x1.0p-85));
        }

        // Powers approaching the limit of BigDecimal.pow (n=1e9)
        // These lose approximately 15-bits of precision in the double result.
        // Uncomment this to output test cases for testPowScaledLargeN.
        //for (int i = 0; i < 5; i++) {
        //    double x = 1.4 + rng.nextDouble() / 50;
        //    double xx = signedNormalDouble(rng) * 0x1.0p-52;
        //    DD.fastTwoSum(x, xx, s);
        //    int n = rng.nextInt(50000000, 60000000);
        //    builder.add(Arguments.of(s.hi(), s.lo(), n, -0x1.0p-43, -0x1.0p-85));
        //    builder.add(Arguments.of(s.hi(), s.lo(), -n, -0x1.0p-43, -0x1.0p-85));
        //}

        // Spot cases

        // Ensure simplePowScaled coverage with cases where:
        // q = 1
        builder.add(Arguments.of(0.6726201869238487, -1.260696696499313E-17, 2476, 0x1.0p-52, 0x1.0p-94));
        builder.add(Arguments.of(0.7373299007207562, 4.2392599349515834E-17, 2474, 0x1.0p-52, 0x1.0p-94));
        builder.add(Arguments.of(0.7253422761833876, -9.319060725056201E-18, 2477, 0x1.0p-52, 0x1.0p-94));
        // q > 1
        builder.add(Arguments.of(1.4057406814073525, 8.718218123265588E-17, 5172, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4123612475347687, -1.8461805152858888E-17, 5318, 0x1.0p-51, 0x1.0p-94));
        // q = 1, r = 0 (i.e. n = m)
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 1935, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 1917, 0x1.0p-51, 0x1.0p-94));
        // q = 1, r == 1
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 1936, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 1918, 0x1.0p-51, 0x1.0p-94));
        // q = 2, r = 0
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 3870, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 3834, 0x1.0p-51, 0x1.0p-94));
        // q = 2, r == 1
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 3871, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 3835, 0x1.0p-51, 0x1.0p-94));

        // Ensure powScaled coverage with high part a power of 2 and non-zero low part
        builder.add(Arguments.of(0.5, Math.ulp(0.5) / 2, 7, 0x1.0p-52, 0x1.0p-100));
        builder.add(Arguments.of(0.5, -Math.ulp(0.5) / 4, 7, 0x1.0p-52, 0x1.0p-100));
        builder.add(Arguments.of(2, Math.ulp(2.0) / 2, 13, 0x1.0p-52, 0x1.0p-100));
        builder.add(Arguments.of(2, -Math.ulp(2.0) / 4, 13, 0x1.0p-52, 0x1.0p-100));

        // Verify that if at any point the power x^p is down-scaled to ~ 1 then the
        // next squaring will create a value above 1 (hence the very small eps for powScaled).
        // This ensures the value x^e * x^p will always multiply as larger than 1.
        // pow(1.0 + 2^-53, 2) = 1.000000000000000222044604925031320
        // pow(1.0 + 2^-106, 2) = 1.000000000000000000000000000000025
        // pow(Math.nextUp(1.0) - 2^-53 + 2^-54, 2) = 1.000000000000000333066907387546990
        builder.add(Arguments.of(1.0, 0x1.0p-53, 2, 0x1.0p-52, 0x1.0p-106));
        builder.add(Arguments.of(1.0, 0x1.0p-106, 2, 0x1.0p-52, 0x1.0p-106));
        Assertions.assertNotEquals(Math.nextUp(1.0), Math.nextUp(1.0) - 0x1.0p-53, "not normalized double-double");
        builder.add(Arguments.of(Math.nextUp(1.0), -0x1.0p-53 + 0x1.0p-54, 2, 0x1.0p-52, 0x1.0p-106));

        // Misc failure cases
        builder.add(Arguments.of(0.991455078125, 0.0, 64, 0x1.0p-53, 0x1.0p-100));
        builder.add(Arguments.of(0.9530029296875, 0.0, 379, 0x1.0p-53, 0x1.0p-100));
        builder.add(Arguments.of(0.9774169921875, 0.0, 179, 0x1.0p-53, 0x1.0p-100));
        // Fails powScaled at 2^-107 due to a rounding error. Requires eps = 1.0047 * 2^-107.
        // This is a possible error in intermediate triple-double multiplication or
        // rounding of the triple-double to a double-double. A final rounding error could
        // be fixed as the power function norm3 normalises intermediates back to
        // a triple-double from a quad-double result. This discards rounding information
        // that could be used to correctly round the triple-double to a double-double.
        builder.add(Arguments.of(0.5319568842468022, -3.190137112420756E-17, 2473, 0x1.0p-51, 0x1.0p-94));

        // Fails fastPow at 2^-94
        builder.add(Arguments.of(0.5014627401015759, 4.9149107900633496E-17, 2424, 0x1.0p-52, 0x1.0p-93));
        builder.add(Arguments.of(0.5014627401015759, 4.9149107900633496E-17, -2424, 0x1.0p-52, 0x1.0p-93));

        // Observed to fail simplePow at 2^-52 (requires ~ 1.01 * 2^-52)
        // This is platform dependent due to the use of java.lang.Math functions.
        builder.add(Arguments.of(0.7409802960884472, -2.4773863758919158E-17, 2416, 0x1.0p-51, 0x1.0p-93));
        builder.add(Arguments.of(0.7409802960884472, -2.4773863758919158E-17, -2416, 0x1.0p-51, 0x1.0p-93));

        return builder.build();
    }

    static Stream<Arguments> testPowScaledLargeN() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // The scaled BigDecimal power is pre-computed as it takes >10 seconds per case.
        // Results are obtained from the debugging assertion
        // message in TestUtils and thus the BigDecimal is rounded to DECIMAL128 format.
        // simplePowScaled loses ~ 67-bits from a double-double (14-bits from a double).
        // fastPowScaled   loses ~ 26-bits from a double-double.
        // powScaled       loses ~ 1-bit from a double-double.
        builder.add(Arguments.of(1.402774996679172, 4.203934137477261E-17, 58162209, 28399655, "0.5069511623667528687158515355802548", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4024304626662112, -1.4084179645855846E-17, 55066019, 26868321, "0.8324073012126417513056910315887745", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4125582593027008, -3.545476880711939E-17, 50869441, 25348771, "0.5062665858255789519032946906819150", 0x1.0p-38, 0x1.0p-80));
        builder.add(Arguments.of(1.4119649130236207, -6.64913621578422E-17, 57868054, 28801176, "0.8386830789932243373181320367289536", 0x1.0p-41, 0x1.0p-80));
        builder.add(Arguments.of(1.4138979166089836, 1.9810424188649008E-17, 57796577, 28879676, "0.8521759805456274150644862351758441", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4145051107021165, 6.919285583856237E-17, -58047003, -29040764, "0.9609529369187483264098384290609811", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4146512942500389, 5.809007274041755E-17, -52177565, -26112078, "0.6333625587966193592039026704846324", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4145748596525067, -1.7347735766459908E-17, -58513216, -29278171, "0.6273407549603278011188148414634989", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4120799563428865, -5.594285001190042E-17, -52544350, -26157721, "0.5406504832406102336189856859270558", 0x1.0p-38, 0x1.0p-80));
        builder.add(Arguments.of(1.4092258370859025, -8.549761437095368E-17, -51083370, -25281304, "0.7447168954354128135078570760787011", 0x1.0p-39, 0x1.0p-80));
        return builder.build();
    }

    static Stream<Arguments> testPowScaledSquare() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 100; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng)));
        }
        return builder.build();
    }

    static Stream<Arguments> testPowScaledSmall() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final DD s = DD.create();
        final double ignored = Double.NaN;

        // Small powers
        for (int i = 0; i < 100; i++) {
            signedNormalDoubleDouble(rng, s);
            final int n = rng.nextInt(2, 10);
            builder.add(Arguments.of(s.hi(), s.lo(), n, ignored, -0x1.0p-100));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, ignored, -0x1.0p-100));
        }

        return builder.build();
    }

    /**
     * Hit assert errors when the method requires the input double-double to be
     * normalized: {@code x == x+xx}.
     */
    @Test
    void testNotNormalizedDoubleDoubleThrows() {
        final double x = 1.0;
        final double xx = Math.nextUp(1.0);
        Assertions.assertNotEquals(x, x + xx);
        final DD z = DD.create();
        // Currently DD is used internally and assert statements validate assumptions
        // so we expect AssertionError. A public API could use IllegalArgumentException
        // but it would not allow run-time removal of the assertions in non-debug code.
        Assertions.assertThrows(AssertionError.class, () -> DD.set(x, xx, z), "set");
        Assertions.assertThrows(AssertionError.class, () -> DDExt.simplePow(x, xx, 2, z), "simplePow");
        Assertions.assertThrows(AssertionError.class, () -> DDExt.simplePowScaled(x, xx, 2, z), "simplePowScaled");
        Assertions.assertThrows(AssertionError.class, () -> DD.fastPowScaled(x, xx, 2, z), "fastPowScaled");
        Assertions.assertThrows(AssertionError.class, () -> DD.powScaled(x, xx, 2, z), "powScaled");
    }

    /**
     * Creates a normalised double in the range {@code [1, 2)} with a random sign. The
     * magnitude is sampled evenly from the 2<sup>52</sup> dyadic rationals in the range.
     *
     * @param bits Random bits.
     * @return the double
     */
    private static double makeSignedNormalDouble(long bits) {
        // Combine an unbiased exponent of 0 with the 52 bit mantissa and a random sign
        // bit
        return Double.longBitsToDouble((1023L << 52) | (bits >>> 12) | (bits << 63));
    }

    /**
     * Creates a normalised double in the range {@code [1, 2)} with a random sign. The
     * magnitude is sampled evenly from the 2<sup>52</sup> dyadic rationals in the range.
     *
     * @param rng Source of randomness.
     * @return the double
     */
    private static double signedNormalDouble(UniformRandomProvider rng) {
        return makeSignedNormalDouble(rng.nextLong());
    }

    /**
     * Creates a normalised double-double in the range {@code [1, 2)} with a random sign.
     *
     * @param rng Source of randomness.
     * @return the double-double
     */
    private static DD signedNormalDoubleDouble(UniformRandomProvider rng, DD z) {
        final double x = signedNormalDouble(rng);
        // scale by 2^-52 to create an overlap
        final double xx = 0x1.0p-52 * signedNormalDouble(rng);
        // Ensure |lo| < 2^-53 * |hi|
        return DD.fastTwoSum(x, xx, z);
    }

    /**
     * Create a BigDecimal for the given value.
     *
     * @param v Value
     * @return the BigDecimal
     */
    private static BigDecimal bd(double v) {
        return new BigDecimal(v);
    }

    /**
     * Assert the number is normalized such that {@code |xx| <= eps * |x|}.
     *
     * @param x High part.
     * @param xx Low part.
     * @param name Name of the number.
     */
    private void assertNormalized(double x, double xx, String name) {
        // Use delta of 0 to allow addition of signed zeros (which may change the sign)
        Assertions.assertEquals(x, x + xx, 0.0, () -> name + " not a normalized double-double");
    }
}
