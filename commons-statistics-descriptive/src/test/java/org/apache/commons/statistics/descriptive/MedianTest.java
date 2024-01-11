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
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.rng.UniformRandomProvider;
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
    @MethodSource(value = {"testMedian"})
    void testMedian(double[] values, double expected) {
        final double[] copy = values.clone();
        Assertions.assertEquals(expected, Median.withDefaults().evaluate(values));
        // Test the result and data (modified in-place) match the quantile implementation
        Assertions.assertEquals(expected, Quantile.withDefaults().evaluate(copy, 0.5));
        Assertions.assertArrayEquals(values, copy);
    }

    @ParameterizedTest
    @MethodSource(value = {"testMedian"})
    void testMedianExcludeNaN(double[] values, double expected) {
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

    static Stream<Arguments> testMedian() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final Percentile p = new Percentile(50).withNaNStrategy(NaNStrategy.FIXED);
        // Note: Cannot use CM when NaN is adjacent to the middle of an odd length
        // as it always interpolates pairs and uses: low + 0.0 * (NaN - low)
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
        }) {
            builder.add(Arguments.of(x, p.evaluate(x)));
        }
        // Cases where CM Percentile returns NaN
        builder.add(Arguments.of(new double[]{1, 2, Double.NaN}, 2));
        builder.add(Arguments.of(new double[]{Double.NaN, 1, 2, 3, Double.NaN}, 3));

        // Test against the percentile can fail at 1 ULP so used a fixed seed
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(26378461823L);
        // Sizes above and below the threshold for partitioning
        double[] x;
        for (final int size : new int[] {5, 6, 50, 51}) {
            final double[] values = rng.doubles(size, -4.5, 1.5).toArray();
            final double expected = p.evaluate(values);
            for (int i = 0; i < 20; i++) {
                x = TestHelper.shuffle(rng, values.clone());
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
            TestHelper.shuffle(rng, x);
            builder.add(Arguments.of(x.clone(), (size & 0x1) == 1 ? -0.0 : 0.0));
        }
        // Special cases
        builder.add(Arguments.of(new double[] {}, Double.NaN));
        builder.add(Arguments.of(new double[] {-Double.MAX_VALUE, Double.MAX_VALUE}, 0));
        builder.add(Arguments.of(new double[] {Double.MIN_VALUE, Double.MIN_VALUE}, Double.MIN_VALUE));
        return builder.build();
    }

    @Test
    void testMedianWithCopy() {
        final double[] values = {3, 4, 2, 1, 0};
        final double[] original = values.clone();
        Assertions.assertEquals(2, Median.withDefaults().withCopy(true).evaluate(values));
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(2, Median.withDefaults().withCopy(false).evaluate(values));
        Assertions.assertFalse(Arrays.equals(original, values));
    }
}
