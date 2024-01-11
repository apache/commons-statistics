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
        final int[] bound = new int[1];
        final NaNTransformer t1 = NaNTransformers.createNaNTransformer(NaNPolicy.ERROR, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> t1.apply(a, bound));
        final NaNTransformer t2 = NaNTransformers.createNaNTransformer(NaNPolicy.ERROR, true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> t2.apply(a, bound));
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
        final int[] bounds = new int[1];
        final double[] b = t.apply(a, bounds);
        if (copy) {
            Assertions.assertNotSame(a, b);
        } else {
            Assertions.assertSame(a, b);
        }
        // Count NaN
        final int nanCount = (int) Arrays.stream(a).filter(Double::isNaN).count();
        final int size = a.length - (includeNaN ? 0 : nanCount);
        Assertions.assertEquals(size, bounds[0], "Size of data");
        if (!includeNaN) {
            for (int i = 0; i < size; i++) {
                Assertions.assertNotEquals(Double.NaN, b[i], "NaN in unsorted range");
            }
            for (int i = size; i < b.length; i++) {
                Assertions.assertEquals(Double.NaN, b[i], "non-NaN in upper range");
            }
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
