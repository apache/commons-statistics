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

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ArraySampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Median}.
 */
class MedianTest {
    /** The number of random trials to perform. */
    private static final int RANDOM_TRIALS = 5;

    @Test
    void testNullPropertyThrows() {
        final Median m = Median.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () -> m.with((NaNPolicy) null));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleMedian"})
    void testDoubleMedian(double[] values, double expected) {
        final double[] copy = values.clone();
        Assertions.assertEquals(expected, Median.withDefaults().evaluate(values));
        // Test the result and data (modified in-place) match the quantile implementation.
        // Interpolation may be different by a single ulp.
        final double q = Quantile.withDefaults().evaluate(copy, 0.5);
        if (Double.isNaN(expected)) {
            Assertions.assertEquals(expected, q);
        } else {
            Assertions.assertEquals(expected, q, Math.ulp(expected));
        }
        // Note: This assertion is not strictly necessary. If the median uses a different
        // partial sort than the quantile the assertion can be removed.
        Assertions.assertArrayEquals(values, copy);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleMedian"})
    void testDoubleMedianExcludeNaN(double[] values, double expected) {
        // If NaN is present then the result will change from expected so ignore this
        Assumptions.assumeFalse(Arrays.stream(values).anyMatch(Double::isNaN));
        // Note: Use copy here. This checks that the copy of the data
        // (with excluded NaNs) is used for special cases.
        final Median m = Median.withDefaults().with(NaNPolicy.EXCLUDE).withCopy(true);
        // Insert some "random" NaN data.
        // Position can be in [0, n].
        for (final int pos : new int[] {0, values.length >>> 1, values.length,
                                        42 % (values.length + 1),
                                        1267836813 % (values.length + 1)}) {
            final double[] x = new double[values.length + 1];
            System.arraycopy(values, 0, x, 0, pos);
            x[pos] = Double.NaN;
            System.arraycopy(values, pos, x, pos + 1, values.length - pos);
            Assertions.assertEquals(expected, m.evaluate(x));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleMedian"})
    void testDoubleMedianErrorNaN(double[] values, double expected) {
        final Median m = Median.withDefaults().with(NaNPolicy.ERROR);
        final double[] y = values.clone();
        if (Arrays.stream(values).anyMatch(Double::isNaN)) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values));
            Assertions.assertArrayEquals(y, values, "Input was modified");
        } else {
            Assertions.assertEquals(expected, m.evaluate(values));
        }
    }

    static Stream<Arguments> testDoubleMedian() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final double[] x : new double[][] {
            {1},
            {1, 2},
            {2, 1},
            {1, Double.NaN},
            {Double.NaN, Double.NaN},
            {1, Double.NaN, Double.NaN},
            {1, 2, Double.NaN, Double.NaN},
            {Double.NaN, Double.NaN, 1, 2, 3, 4},
            {Double.MAX_VALUE, Double.MAX_VALUE},
            {-Double.MAX_VALUE, -Double.MAX_VALUE / 2},
            // Cases where quantile interpolation using p=0.5 is different.
            // Fail cases taken from adaption of InterpolationTest.testMeanVsInterpolate.
            {6.125850131710258E31, 2.11712251424532992E17},
            {3.550291387137841E117, 7.941355536862782E127},
            {9.026950581570208E-93, 1.5840864549779843E-77},
        }) {
            builder.add(Arguments.of(x, evaluate(x)));
        }
        // Cases where Commons Math Percentile returns NaN (because it always interpolates
        // x[i] + g * (x[i+1] - x[i]) even when g==0)
        builder.add(Arguments.of(new double[] {1, 2, Double.NaN}, 2));
        builder.add(Arguments.of(new double[] {Double.NaN, 1, 2, 3, Double.NaN}, 3));

        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        // Sizes above and below the threshold for partitioning
        double[] x;
        for (final int size : new int[] {5, 6, 50, 51}) {
            final double[] values = rng.doubles(size, -4.5, 1.5).toArray();
            final double expected = evaluate(values);
            for (int i = 0; i < 20; i++) {
                x = ArraySampler.shuffle(rng, values.clone());
                builder.add(Arguments.of(x, expected));
            }
            // Special values
            for (final double y : new double[] {-0.0, 0.0, 1, Double.MAX_VALUE,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN}) {
                x = new double[size];
                Arrays.fill(x, y);
                builder.add(Arguments.of(x, y));
            }
            // Odd: just over half -0.0
            // Even: half -0.0
            x = new double[size];
            Arrays.fill(x, 0, (size + 1) / 2, -0.0);
            ArraySampler.shuffle(rng, x);
            builder.add(Arguments.of(x.clone(), (size & 0x1) == 1 ? -0.0 : 0.0));
        }
        // Special cases
        builder.add(Arguments.of(new double[] {}, Double.NaN));
        builder.add(Arguments.of(new double[] {-Double.MAX_VALUE, Double.MAX_VALUE}, 0));
        builder.add(Arguments.of(new double[] {Double.MIN_VALUE, Double.MIN_VALUE}, Double.MIN_VALUE));
        return builder.build();
    }

    /**
     * Evaluate the median using a full sort on a copy of the data.
     *
     * @param values Value.
     * @return the median
     */
    private static double evaluate(double[] values) {
        final double[] x = values.clone();
        Arrays.sort(x);
        final int m = x.length >> 1;
        if ((x.length & 0x1) == 1) {
            // odd
            return x[m];
        }
        return Interpolation.mean(x[m - 1], x[m]);
    }

    @Test
    void testDoubleMedianWithCopy() {
        assertMedianWithCopy(new double[] {2, 1}, 1.5);
        assertMedianWithCopy(new double[] {3, 2, 1}, 2);
        assertMedianWithCopy(new double[] {4, 3, 2, 1}, 2.5);
        assertMedianWithCopy(new double[] {5, 4, 3, 2, 1}, 3);
        // Special case for 2 values with signed zeros (must be unordered)
        assertMedianWithCopy(new double[] {0.0, -0.0}, 0.0);
    }

    private static void assertMedianWithCopy(double[] values, double expected) {
        final double[] original = values.clone();
        Assertions.assertEquals(expected, Median.withDefaults().withCopy(true).evaluate(values));
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(expected, Median.withDefaults().withCopy(false).evaluate(values));
        Assertions.assertFalse(Arrays.equals(original, values));
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#arrayRangeTestData"})
    final void testDoubleMedianRangeThrows(int from, int to, int length) {
        final double[] values = new double[length];
        final Median m = Median.withDefaults();
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> m.evaluateRange(values, from, to),
            () -> String.format("range [%d, %d) in length %d", from, to, length));
    }

    /**
     * Test data with an internal region evaluates exactly the same when using
     * a copy of the internal region evaluated as a full length array,
     * or the range method on the full array.
     */
    @Test
    void testDoubleMedianRange() {
        // Empty range
        assertMedianRange(new double[] {1, 2, 3, 4, 5}, 2, 2);
        // Range range
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (int count = RANDOM_TRIALS; --count >= 0;) {
            final int n = 10 + count;
            final double[] x = rng.doubles(n).toArray();
            final int i = rng.nextInt(n);
            final int j = rng.nextInt(n);
            assertMedianRange(x, Math.min(i, j), Math.max(i, j));
        }
        // NaN in the range
        final double[] x = rng.doubles(10).toArray();
        x[5] = Double.NaN;
        assertMedianRange(x.clone(), 2, 8);
        assertMedianRange(x.clone(), 2, 9);
    }

    private static void assertMedianRange(double[] values, int from, int to) {
        // Test all NaN policies as these apply to double[] data
        for (final NaNPolicy p : NaNPolicy.values()) {
            assertMedianRange(values.clone(), from, to, p);
        }
    }

    private static void assertMedianRange(double[] values, int from, int to, NaNPolicy nanPolicy) {
        final Supplier<String> msg = () -> String.format("NaN=%s; range [%d, %d) in length %d",
            nanPolicy, from, to, values.length);
        final double[] original = values.clone();
        final double[] x = Arrays.copyOfRange(values, from, to);
        // Test with/without modification of the input
        final Median m = Median.withDefaults().with(nanPolicy).withCopy(false);
        final Median mCopy = Median.withDefaults().with(nanPolicy).withCopy(true);
        try {
            // Reference result operating in-place
            final double expected = m.evaluate(x);
            // With copy the input is unchanged
            Assertions.assertEquals(expected, mCopy.evaluateRange(values, from, to), msg);
            Assertions.assertArrayEquals(original, values, msg);
            // Without copy only the values inside the range should be modified.
            // Compose the expected result.
            System.arraycopy(x, 0, original, from, x.length);
            Assertions.assertEquals(expected, m.evaluateRange(values, from, to), msg);
            Assertions.assertArrayEquals(original, values, msg);
        } catch (IllegalArgumentException e) {
            // NaN input
            Assertions.assertThrows(e.getClass(), () -> mCopy.evaluateRange(values, from, to), msg);
            Assertions.assertThrows(e.getClass(), () -> m.evaluateRange(values, from, to), msg);
            Assertions.assertArrayEquals(original, values, msg);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntMedian"})
    void testIntMedian(int[] values, double expected) {
        final int[] copy = values.clone();
        Assertions.assertEquals(expected, Median.withDefaults().evaluate(values));
        // Test the result and data (modified in-place) match the quantile implementation.
        // Results are either integer or half-integer so this should be exact.
        Assertions.assertEquals(expected, Quantile.withDefaults().evaluate(copy, 0.5));
        Assertions.assertArrayEquals(values, copy);
    }

    static Stream<Arguments> testIntMedian() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final int[] x : new int[][] {
            {1},
            {1, 2},
            {2, 1},
            {1, 2, 3, 4},
            {Integer.MAX_VALUE, Integer.MAX_VALUE / 2},
            {Integer.MIN_VALUE, Integer.MIN_VALUE / 2},
        }) {
            builder.add(Arguments.of(x, evaluate(x)));
        }

        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        // Sizes above and below the threshold for partitioning
        int[] x;
        for (final int size : new int[] {5, 6, 50, 51}) {
            final int[] values = rng.ints(size, -4500, 1500).toArray();
            final double expected = evaluate(values);
            for (int i = 0; i < 20; i++) {
                x = ArraySampler.shuffle(rng, values.clone());
                builder.add(Arguments.of(x, expected));
            }
            // Special values
            for (final int y : new int[] {0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                x = new int[size];
                Arrays.fill(x, y);
                builder.add(Arguments.of(x, y));
            }
        }
        // Special cases
        builder.add(Arguments.of(new int[] {}, Double.NaN));
        builder.add(Arguments.of(new int[] {-Integer.MAX_VALUE, Integer.MAX_VALUE}, 0));
        return builder.build();
    }

    /**
     * Evaluate the median using a full sort on a copy of the data.
     *
     * @param values Value.
     * @return the median
     */
    private static double evaluate(int[] values) {
        final int[] x = values.clone();
        Arrays.sort(x);
        final int m = x.length >> 1;
        if ((x.length & 0x1) == 1) {
            // odd
            return x[m];
        }
        return Interpolation.mean(x[m - 1], x[m]);
    }

    @Test
    void testIntMedianWithCopy() {
        assertMedianWithCopy(new int[] {2, 1}, 1.5);
        assertMedianWithCopy(new int[] {3, 2, 1}, 2);
        assertMedianWithCopy(new int[] {4, 3, 2, 1}, 2.5);
        assertMedianWithCopy(new int[] {5, 4, 3, 2, 1}, 3);
    }

    private static void assertMedianWithCopy(int[] values, double expected) {
        final int[] original = values.clone();
        Assertions.assertEquals(expected, Median.withDefaults().withCopy(true).evaluate(values));
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(expected, Median.withDefaults().withCopy(false).evaluate(values));
        Assertions.assertFalse(Arrays.equals(original, values));
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#arrayRangeTestData"})
    final void testIntMedianRangeThrows(int from, int to, int length) {
        final int[] values = new int[length];
        final Median m = Median.withDefaults();
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> m.evaluateRange(values, from, to),
            () -> String.format("range [%d, %d) in length %d", from, to, length));
    }

    /**
     * Test data with an internal region evaluates exactly the same when using
     * a copy of the internal region evaluated as a full length array,
     * or the range method on the full array.
     */
    @Test
    void testIntMedianRange() {
        // Empty range
        assertMedianRange(new int[] {1, 2, 3, 4, 5}, 2, 2);
        // Range range
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (int count = RANDOM_TRIALS; --count >= 0;) {
            final int n = 10 + count;
            final int[] x = rng.ints(n).toArray();
            final int i = rng.nextInt(n);
            final int j = rng.nextInt(n);
            assertMedianRange(x, Math.min(i, j), Math.max(i, j));
        }
    }

    private static void assertMedianRange(int[] values, int from, int to) {
        final Supplier<String> msg = () -> String.format("range [%d, %d) in length %d",
            from, to, values.length);
        final int[] original = values.clone();
        final int[] x = Arrays.copyOfRange(values, from, to);
        // Test with/without modification of the input
        final Median m = Median.withDefaults().withCopy(false);
        final Median mCopy = Median.withDefaults().withCopy(true);
        // Reference result operating in-place
        final double expected = m.evaluate(x);
        // With copy the input is unchanged
        Assertions.assertEquals(expected, mCopy.evaluateRange(values, from, to), msg);
        Assertions.assertArrayEquals(original, values, msg);
        // Without copy only the values inside the range should be modified.
        // Compose the expected result.
        System.arraycopy(x, 0, original, from, x.length);
        Assertions.assertEquals(expected, m.evaluateRange(values, from, to), msg);
        Assertions.assertArrayEquals(original, values, msg);
    }
}
