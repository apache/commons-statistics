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

import java.util.Arrays;
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
        Assertions.assertArrayEquals(values, copy);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleMedian"})
    void testDoubleMedianExcludeNaN(double[] values, double expected) {
        // If NaN is present then the result will change from expected so ignore this
        Assumptions.assumeTrue(Arrays.stream(values).filter(Double::isNaN).count() == 0);
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
}
