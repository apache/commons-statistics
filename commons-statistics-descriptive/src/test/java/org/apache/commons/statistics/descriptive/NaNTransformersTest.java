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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link NaNTransformers}.
 */
class NaNTransformersTest {
    @ParameterizedTest
    @MethodSource(value = {"nanData"})
    void testNaNErrorWithNaN(double[] a) {
        final int[] bounds = new int[2];
        for (final boolean copy : new boolean[] {false, true}) {
            final NaNTransformer t = NaNTransformers.createNaNTransformer(NaNPolicy.ERROR, copy);
            Assertions.assertThrows(IllegalArgumentException.class, () -> t.apply(a, 0, a.length, bounds));
            // Partial ranges.
            // Note: Checks on the contents of the returned array range without NaN are
            // done in a separate test on the nonNanData.
            // First half
            final int half = a.length >> 1;
            if (Arrays.stream(a, 0, half).anyMatch(Double::isNaN)) {
                Assertions.assertThrows(IllegalArgumentException.class, () -> t.apply(a, 0, half, bounds));
            } else {
                Assertions.assertDoesNotThrow(() -> t.apply(a, 0, half, bounds));
            }
            // Second half
            if (Arrays.stream(a, half, a.length).anyMatch(Double::isNaN)) {
                Assertions.assertThrows(IllegalArgumentException.class, () -> t.apply(a, half, a.length, bounds));
            } else {
                Assertions.assertDoesNotThrow(() -> t.apply(a, half, a.length, bounds));
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"nonNanData"})
    void testNaNError(double[] a) {
        assertNaNTransformer(a, NaNTransformers.createNaNTransformer(NaNPolicy.ERROR, false), true, false);
        assertNaNTransformer(a, NaNTransformers.createNaNTransformer(NaNPolicy.ERROR, true), true, true);
    }

    @ParameterizedTest
    @MethodSource(value = {"nanData", "nonNanData"})
    void testNaNInclude(double[] a) {
        assertNaNTransformer(a, NaNTransformers.createNaNTransformer(NaNPolicy.INCLUDE, false), true, false);
        assertNaNTransformer(a, NaNTransformers.createNaNTransformer(NaNPolicy.INCLUDE, true), true, true);
    }

    @ParameterizedTest
    @MethodSource(value = {"nanData", "nonNanData"})
    void testNaNExclude(double[] a) {
        assertNaNTransformer(a, NaNTransformers.createNaNTransformer(NaNPolicy.EXCLUDE, false), false, false);
        assertNaNTransformer(a, NaNTransformers.createNaNTransformer(NaNPolicy.EXCLUDE, true), false, true);
    }

    /**
     * Assert the NaN transformer allows including or excluding NaN.
     *
     * @param a Data.
     * @param t Transformer.
     * @param includeNaN True if the size should include NaN.
     * @param copy True if the pre-processed data should be a copy.
     */
    private static void assertNaNTransformer(double[] a, NaNTransformer t,
            boolean includeNaN, boolean copy) {
        final int n = a.length;
        assertNaNTransformer(a.clone(), 0, n, t, includeNaN, copy);
        assertNaNTransformer(a.clone(), 0, n >> 1, t, includeNaN, copy);
        assertNaNTransformer(a.clone(), n >> 1, n, t, includeNaN, copy);
    }

    /**
     * Assert the NaN transformer allows including or excluding NaN.
     *
     * @param a Data.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param t Transformer.
     * @param includeNaN True if the size should include NaN.
     * @param copy True if the pre-processed data should be a copy.
     */
    private static void assertNaNTransformer(double[] a, int from, int to, NaNTransformer t,
            boolean includeNaN, boolean copy) {
        // Count NaN
        final int nanCount = (int) Arrays.stream(a, from, to).filter(Double::isNaN).count();
        final int length = to - from - (includeNaN ? 0 : nanCount);

        final int[] bounds = {-1, -1};
        final double[] original = a.clone();
        final double[] b = t.apply(a, from, to, bounds);

        // Sort the original and the returned range to allow equality comparison
        double[] s1 = Arrays.copyOfRange(original, from, to);
        Arrays.sort(s1);
        final double[] s2 = Arrays.copyOfRange(b, bounds[0], bounds[1]);
        Arrays.sort(s2);

        if (copy) {
            Assertions.assertNotSame(a, b, "copy returned the original");
            Assertions.assertArrayEquals(original, a, "original array was modified");
            // b is a new array, it can be any length but the returned range
            // should contains the same values as a, optionally excluding NaN
            if (!includeNaN) {
                s1 = Arrays.copyOf(s1, length);
            }
            Assertions.assertArrayEquals(s1, s2);
        } else {
            Assertions.assertSame(a, b, "no copy returned a new array");
            // Unchanged outside of [from, to)
            for (int i = 0; i < from; i++) {
                Assertions.assertEquals(original[i], a[i]);
            }
            for (int i = to; i < a.length; i++) {
                Assertions.assertEquals(original[i], a[i]);
            }
            // Same values inside the range (including NaN)
            final double[] s3 = Arrays.copyOfRange(b, from, to);
            Arrays.sort(s3);
            Assertions.assertArrayEquals(s1, s3);
            // The returned range has the same values optionally excluding NaN
            if (!includeNaN) {
                s1 = Arrays.copyOf(s1, length);
            }
            Assertions.assertArrayEquals(s1, s2);
        }
    }

    static Stream<double[]> nanData() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final double nan = Double.NaN;
        builder.add(new double[] {nan});
        builder.add(new double[] {1, 2, nan});
        builder.add(new double[] {nan, 2, 3});
        builder.add(new double[] {1, nan, 3});
        builder.add(new double[] {nan, nan});
        builder.add(new double[] {nan, 2, nan});
        builder.add(new double[] {nan, nan, nan});
        builder.add(new double[] {1, 0.0, 0.0, nan, -1});
        builder.add(new double[] {1, 0.0, -0.0, nan, -1});
        builder.add(new double[] {1, -0.0, 0.0, nan, -1});
        builder.add(new double[] {1, -0.0, -0.0, nan, -1});
        builder.add(new double[] {1, 0.0, 0.0, nan, -1, 0.0, 0.0});
        builder.add(new double[] {1, 0.0, -0.0, nan, -1, 0.0, 0.0});
        builder.add(new double[] {1, 0.0, -0.0, nan, -1, 0.0, -0.0});
        builder.add(new double[] {nan, -0.0, 0.0, nan, -1, -0.0, -0.0});
        builder.add(new double[] {nan, -0.0, -0.0, nan, -1, -0.0, -0.0});
        return builder.build();
    }

    static Stream<double[]> nonNanData() {
        final Stream.Builder<double[]> builder = Stream.builder();
        builder.add(new double[] {});
        builder.add(new double[] {3});
        builder.add(new double[] {3, 2, 1});
        builder.add(new double[] {1, 0.0, 0.0, 3, -1});
        builder.add(new double[] {1, 0.0, -0.0, 3, -1});
        builder.add(new double[] {1, -0.0, 0.0, 3, -1});
        builder.add(new double[] {1, -0.0, -0.0, 3, -1});
        builder.add(new double[] {1, 0.0, 0.0, 3, -1, 0.0, 0.0});
        builder.add(new double[] {1, 0.0, -0.0, 3, -1, 0.0, 0.0});
        builder.add(new double[] {1, 0.0, -0.0, 3, -1, 0.0, -0.0});
        builder.add(new double[] {1, -0.0, 0.0, 3, -1, -0.0, -0.0});
        builder.add(new double[] {1, -0.0, -0.0, 3, -1, -0.0, -0.0});
        return builder.build();
    }
}
