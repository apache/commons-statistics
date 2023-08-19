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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Mean}.
 */
final class MeanTest {
    private static final int ULP_ARRAY = 2;

    private static final int ULP_STREAM = 5;

    private static final int ULP_COMBINE = 5;

    @Test
    void testEmpty() {
        Mean mean = Mean.create();
        Assertions.assertEquals(Double.NaN, mean.getAsDouble());
    }

    @Test
    void testNan() {
        Mean mean = Mean.create();
        double[] testArray = {Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : testArray) {
            mean.accept(value);
        }
        Assertions.assertEquals(Double.NaN, mean.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testMean(double[] values) {
        final double expected = computeExpected(values);
        Mean mean = Mean.create();
        for (double value : values) {
            mean.accept(value);
        }
        TestHelper.assertEquals(expected, mean.getAsDouble(), ULP_STREAM, () -> "mean");
        TestHelper.assertEquals(expected, Mean.of(values).getAsDouble(), ULP_ARRAY, () -> "of (values)");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testParallelStream(double[] values) {
        final double expected = computeExpected(values);
        final double ans = Arrays.stream(values)
                .parallel()
                .collect(Mean::create, Mean::accept, Mean::combine)
                .getAsDouble();
        TestHelper.assertEquals(expected, ans, ULP_COMBINE, () -> "parallel stream");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testMeanRandomOrder(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testMean(TestHelper.shuffle(rng, values));
            testParallelStream(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testMeanNonFinite(double[] values, double expected) {
        Mean mean = Mean.create();
        for (double value : values) {
            mean.accept(value);
        }
        Assertions.assertEquals(expected, mean.getAsDouble(), "mean non-finite");
        Assertions.assertEquals(expected, Mean.of(values).getAsDouble(), "of (values) non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testParallelStreamNonFinite(double[] values, double expected) {
        final double ans = Arrays.stream(values)
                .parallel()
                .collect(Mean::create, Mean::accept, Mean::combine)
                .getAsDouble();
        Assertions.assertEquals(expected, ans, "parallel stream non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite", "testMeanNonFinite"})
    void testMeanRandomOrderNonFinite(double[] values, double expected) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testMeanNonFinite(TestHelper.shuffle(rng, values), expected);
            testParallelStreamNonFinite(TestHelper.shuffle(rng, values), expected);
        }
    }

    static Stream<Arguments> testMeanNonFinite() {
        return Stream.of(
            Arguments.of(new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[]{Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}, Double.NEGATIVE_INFINITY),
            Arguments.of(new double[]{Double.POSITIVE_INFINITY, Double.MAX_VALUE}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[]{Double.NEGATIVE_INFINITY, -Double.MIN_VALUE}, Double.NEGATIVE_INFINITY),
            Arguments.of(new double[]{Double.NEGATIVE_INFINITY, Double.MAX_VALUE}, Double.NEGATIVE_INFINITY),
            Arguments.of(new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[]{-Double.MAX_VALUE, Double.POSITIVE_INFINITY}, Double.POSITIVE_INFINITY)
        );
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testCombine(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpected(combinedArray);
        Mean mean1 = Mean.create();
        Mean mean2 = Mean.create();
        Arrays.stream(array1).forEach(mean1);
        Arrays.stream(array2).forEach(mean2);
        final double mean1BeforeCombine = mean1.getAsDouble();
        final double mean2BeforeCombine = mean2.getAsDouble();
        mean1.combine(mean2);
        TestHelper.assertEquals(expected, mean1.getAsDouble(), ULP_COMBINE, () -> "combine");
        Assertions.assertEquals(mean2BeforeCombine, mean2.getAsDouble());
        // Combine in reversed order
        Mean mean1b = Mean.create();
        Arrays.stream(array1).forEach(mean1b);
        mean2.combine(mean1b);
        TestHelper.assertEquals(expected, mean2.getAsDouble(), ULP_COMBINE, () -> "combine");
        Assertions.assertEquals(mean1BeforeCombine, mean1b.getAsDouble());
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
        final double expected = computeExpected(combinedArray);
        final double[][] values = {array1, array2};
        final double actual = Arrays.stream(values)
                .map(Mean::of)
                .reduce(Mean::combine)
                .map(Mean::getAsDouble)
                .orElseThrow(RuntimeException::new);
        TestHelper.assertEquals(expected, actual, ULP_COMBINE, () -> "array of arrays combined mean");
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite", "testCombineMeanNonFinite"})
    void testCombineNonFinite(double[][] values, double expected) {
        Mean mean1 = Mean.create();
        Mean mean2 = Mean.create();
        Arrays.stream(values[0]).forEach(mean1);
        Arrays.stream(values[1]).forEach(mean2);
        final double mean2BeforeCombine = mean2.getAsDouble();
        mean1.combine(mean2);
        Assertions.assertEquals(expected, mean1.getAsDouble(), "combine non-finite");
        Assertions.assertEquals(mean2BeforeCombine, mean2.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite", "testCombineMeanNonFinite"})
    void testCombineRandomOrderNonFinite(double[][] values, double expected) {
        UniformRandomProvider rng = TestHelper.createRNG();
        final double[] data = TestHelper.concatenate(values[0], values[1]);
        final int n = values[0].length;
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                TestHelper.shuffle(rng, values[0]);
                TestHelper.shuffle(rng, values[1]);
                testCombineNonFinite(values, expected);
            }
            TestHelper.shuffle(rng, data);
            System.arraycopy(data, 0, values[0], 0, n);
            System.arraycopy(data, n, values[1], 0, values[1].length);
            testCombineNonFinite(values, expected);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite", "testCombineMeanNonFinite"})
    void testArrayOfArraysNonFinite(double[][] values, double expected) {
        final double actual = Arrays.stream(values)
                .map(Mean::of)
                .reduce(Mean::combine)
                .map(Mean::getAsDouble)
                .orElseThrow(RuntimeException::new);
        Assertions.assertEquals(expected, actual, "array of arrays combined mean non-finite");
    }

    static Stream<Arguments> testCombineMeanNonFinite() {
        return Stream.of(
            Arguments.of(new double[][] {{Double.POSITIVE_INFINITY}, {Double.POSITIVE_INFINITY}}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[][] {{Double.NEGATIVE_INFINITY}, {Double.NEGATIVE_INFINITY}}, Double.NEGATIVE_INFINITY),
            Arguments.of(new double[][] {{Double.POSITIVE_INFINITY}, {Double.MAX_VALUE}}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[][] {{-Double.MAX_VALUE}, {Double.POSITIVE_INFINITY}}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[][] {{Double.NEGATIVE_INFINITY}, {-Double.MIN_VALUE}}, Double.NEGATIVE_INFINITY),
            Arguments.of(new double[][] {{Double.NaN, 34.56, 89.74}, {Double.NaN}}, Double.NaN),
            Arguments.of(new double[][] {{34.56}, {Double.NaN, 89.74}}, Double.NaN),
            Arguments.of(new double[][] {{34.56, 89.74}, {Double.NaN, Double.NaN}}, Double.NaN),
            Arguments.of(new double[][] {{Double.NaN, 3.14, Double.NaN, Double.NaN}, {}}, Double.NaN),
            Arguments.of(new double[][] {{Double.NaN, Double.NaN, Double.NaN}, {Double.NaN, Double.NaN, Double.NaN}}, Double.NaN),
            Arguments.of(new double[][] {{Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -Double.MIN_VALUE}, {Double.MAX_VALUE, Double.MIN_VALUE}}, Double.NEGATIVE_INFINITY)
        );
    }

    // Helper function which converts the mean of BigDecimal type to a double type.
    private static double computeExpected(double[] values) {
        return TestHelper.computeExpectedMean(values).doubleValue();
    }
}
