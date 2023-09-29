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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Variance}.
 */
final class VarianceTest {
    private static final int ULP_ARRAY = 4;

    private static final int ULP_STREAM = 8;

    private static final int ULP_COMBINE_ACCEPT = 8;

    private static final int ULP_COMBINE_OF = 2;

    @Test
    void testEmpty() {
        Variance var = Variance.create();
        Assertions.assertEquals(Double.NaN, var.getAsDouble());
    }

    @Test
    void testNaN() {
        Variance variance = Variance.create();
        double[] testArray = {Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : testArray) {
            variance.accept(value);
        }
        Assertions.assertEquals(Double.NaN, variance.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testVariance(double[] values) {
        final double expected = computeExpectedVariance(values);
        Variance var = Variance.create();
        for (double value : values) {
            var.accept(value);
        }
        TestHelper.assertEquals(expected, var.getAsDouble(), ULP_STREAM, () -> "variance");
        TestHelper.assertEquals(expected, Variance.of(values).getAsDouble(), ULP_ARRAY, () -> "of (values)");

        Variance var2 = Variance.of();
        for (double value : values) {
            var2.accept(value);
        }
        Assertions.assertEquals(var.getAsDouble(), var2.getAsDouble(), "of() + values");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testParallelStream(double[] values) {
        final double expected = computeExpectedVariance(values);
        final double actual = Arrays.stream(values)
                .parallel()
                .collect(Variance::create, Variance::accept, Variance::combine)
                .getAsDouble();
        TestHelper.assertEquals(expected, actual, ULP_COMBINE_ACCEPT, () -> "parallel stream");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testVarianceRandomOrder(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testVariance(TestHelper.shuffle(rng, values));
            testParallelStream(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testVarianceNonFinite(double[] values) {
        final double expected = Double.NaN;
        Variance var = Variance.create();
        for (double value : values) {
            var.accept(value);
        }
        Assertions.assertEquals(expected, var.getAsDouble(), "variance non-finite");
        Assertions.assertEquals(expected, Variance.of(values).getAsDouble(), "of (values) non-finite");

        Variance var2 = Variance.of();
        for (double value : values) {
            var2.accept(value);
        }
        Assertions.assertEquals(var.getAsDouble(), var2.getAsDouble(), "of() + values non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testParallelStreamNonFinite(double[] values) {
        final double expected = Double.NaN;
        final double ans = Arrays.stream(values)
                .parallel()
                .collect(Variance::create, Variance::accept, Variance::combine)
                .getAsDouble();
        Assertions.assertEquals(expected, ans, "parallel stream non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testVarianceRandomOrderNonFinite(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testVarianceNonFinite(TestHelper.shuffle(rng, values));
            testParallelStreamNonFinite(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testCombine(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpectedVariance(combinedArray);
        Variance var1 = Variance.create();
        Variance var2 = Variance.create();
        Arrays.stream(array1).forEach(var1);
        Arrays.stream(array2).forEach(var2);
        final double var1BeforeCombine = var1.getAsDouble();
        final double var2BeforeCombine = var2.getAsDouble();
        var1.combine(var2);
        TestHelper.assertEquals(expected, var1.getAsDouble(), ULP_COMBINE_ACCEPT, () -> "combine");
        Assertions.assertEquals(var2BeforeCombine, var2.getAsDouble());
        // Combine in reverse order
        Variance var1b = Variance.create();
        Arrays.stream(array1).forEach(var1b);
        var2.combine(var1b);
        Assertions.assertEquals(var1.getAsDouble(), var2.getAsDouble(), () -> "combine reversed");
        Assertions.assertEquals(var1BeforeCombine, var1b.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testCombineRandomOrder(double[] array1, double[] array2) {
        UniformRandomProvider rng = TestHelper.createRNG();
        double[] data = TestHelper.concatenate(array1, array2);
        final int n = array1.length;
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                TestHelper.shuffle(rng, array1);
                TestHelper.shuffle(rng, array2);
                testCombine(array1, array2);
            }
            TestHelper.shuffle(rng, data);
            System.arraycopy(data, 0, array1, 0, n);
            System.arraycopy(data, n, array2, 0, array2.length);
            testCombine(array1, array2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testArrayOfArrays(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpectedVariance(combinedArray);
        final double[][] values = {array1, array2};
        final double actual = Arrays.stream(values)
                .map(Variance::of)
                .reduce(Variance::combine)
                .map(Variance::getAsDouble)
                .orElseThrow(RuntimeException::new);
        TestHelper.assertEquals(expected, actual, ULP_COMBINE_OF, () -> "array of arrays combined variance");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testCombineNonFinite(double[][] values) {
        final double expected = Double.NaN;
        Variance var1 = Variance.create();
        Variance var2 = Variance.create();
        Arrays.stream(values[0]).forEach(var1);
        Arrays.stream(values[1]).forEach(var2);
        final double mean2BeforeCombine = var2.getAsDouble();
        var1.combine(var2);
        Assertions.assertEquals(expected, var1.getAsDouble(), "combine non-finite");
        Assertions.assertEquals(mean2BeforeCombine, var2.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testCombineRandomOrderNonFinite(double[][] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        final double[] data = TestHelper.concatenate(values[0], values[1]);
        final int n = values[0].length;
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                TestHelper.shuffle(rng, values[0]);
                TestHelper.shuffle(rng, values[1]);
                testCombineNonFinite(values);
            }
            TestHelper.shuffle(rng, data);
            System.arraycopy(data, 0, values[0], 0, n);
            System.arraycopy(data, n, values[1], 0, values[1].length);
            testCombineNonFinite(values);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testArrayOfArraysNonFinite(double[][] values) {
        final double expected = Double.NaN;
        final double actual = Arrays.stream(values)
                .map(Variance::of)
                .reduce(Variance::combine)
                .map(Variance::getAsDouble)
                .orElseThrow(RuntimeException::new);
        Assertions.assertEquals(expected, actual, "array of arrays combined variance non-finite");
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     * @param values Values.
     * @return Variance of values
     */
    private static double computeExpectedVariance(double[] values) {
        return computeExpectedVariance(values, null);
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     * @param values Values.
     * @param mean Mean (result). Only computed if {@code length > 1}.
     * @return Variance of values
     */
    static double computeExpectedVariance(double[] values, BigDecimal[] mean) {
        long n = values.length;
        if (n == 1) {
            return 0;
        }
        return TestHelper.computeExpectedSumOfSquaredDeviations(values, mean)
            .divide(BigDecimal.valueOf(n - 1), MathContext.DECIMAL128).doubleValue();
    }
}
