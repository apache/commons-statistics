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
    @MethodSource(value = "testMean")
    void testMean(double[] values) {
        double expected = computeExpected(values);
        Mean mean = Mean.create();
        for (double value : values) {
            mean.accept(value);
        }
        TestHelper.assertEquals(expected, mean.getAsDouble(), ULP_STREAM, () -> "mean");
        TestHelper.assertEquals(expected, Mean.of(values).getAsDouble(), ULP_ARRAY, () -> "of (values)");
    }

    @ParameterizedTest
    @MethodSource(value = "testMean")
    void testParallelStream(double[] values) {
        double expected = computeExpected(values);
        double ans = Arrays.stream(values)
                .parallel()
                .collect(Mean::create, Mean::accept, Mean::combine)
                .getAsDouble();
        TestHelper.assertEquals(expected, ans, ULP_COMBINE, () -> "parallel stream");
    }

    @ParameterizedTest
    @MethodSource(value = "testMean")
    void testMeanRandomOrder(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testMean(TestHelper.shuffle(rng, values));
            testParallelStream(TestHelper.shuffle(rng, values));
        }
    }

    static Stream<Arguments> testMean() {
        return Stream.of(
            Arguments.of(new double[] {0.0}),
            Arguments.of(new double[] {10, 8, 13, 9, 11, 14, 6, 4, 12, 7, 5}),
            Arguments.of(new double[] {8.04, 6.95, 7.58, 8.81, 8.33, 9.96, 7.24, 4.26, 10.84, 4.82, 5.68}),
            Arguments.of(new double[] {9.14, 8.14, 8.74, 8.77, 9.26, 8.10, 6.13, 3.10, 9.13, 7.26, 4.74, 7.46, 6.77, 12.74, 7.11, 7.81, 8.84, 6.08, 5.39, 8.15, 6.42, 5.73}),
            Arguments.of(new double[] {8, 8, 8, 8, 8, 8, 8, 19, 8, 8, 8}),
            Arguments.of(new double[] {6.58, 5.76, 7.71, 8.84, 8.47, 7.04, 5.25, 12.50, 5.56, 7.91, 6.89}),
            Arguments.of(new double[] {0, 0, 0.0}),
            Arguments.of(new double[] {1, -7, 6}),
            Arguments.of(new double[] {1, 7, -15, 3}),
            Arguments.of(new double[] {2, 2, 2, 2}),
            Arguments.of(new double[] {2.3}),
            Arguments.of(new double[] {3.14, 2.718, 1.414}),
            Arguments.of(new double[] {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0, 8.2, 10.3, 11.3, 14.1, 9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0, 8.8, 9.0, 12.3}),
            Arguments.of(new double[] {-0.0, +0.0}),
            Arguments.of(new double[] {0.0, -0.0}),
            Arguments.of(new double[] {0.0, +0.0}),
            Arguments.of(new double[] {0.001, 0.0002, 0.00003, 10000.11, 0.000004}),
            Arguments.of(new double[] {10E-50, 5E-100, 25E-200, 35.345E-50}),
            // Overflow of the sum
            Arguments.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}),
            Arguments.of(new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE, 1}),
            Arguments.of(new double[] {-Double.MAX_VALUE, 1, 1}),
            Arguments.of(new double[] {-Double.MAX_VALUE, -1, 1}),
            Arguments.of(new double[] {Double.MAX_VALUE, -1}),
            Arguments.of(new double[] {Double.MAX_VALUE, -Double.MAX_VALUE}),
            Arguments.of(new double[] {1, -Double.MAX_VALUE}),
            Arguments.of(new double[] {1, 1, 1, -Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE / 2}),
            Arguments.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE})
        );
    }

    @ParameterizedTest
    @MethodSource(value = "testMeanNonFinite")
    void testMeanNonFinite(double[] values, double expected) {
        Mean mean = Mean.create();
        for (double value : values) {
            mean.accept(value);
        }
        Assertions.assertEquals(expected, mean.getAsDouble(), "mean non-finite");
        Assertions.assertEquals(expected, Mean.of(values).getAsDouble(), "of (values) non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "testMeanNonFinite")
    void testParallelStreamNonFinite(double[] values, double expected) {
        double ans = Arrays.stream(values)
                .parallel()
                .collect(Mean::create, Mean::accept, Mean::combine)
                .getAsDouble();
        Assertions.assertEquals(expected, ans, "parallel stream non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "testMeanNonFinite")
    void testMeanRandomOrderNonFinite(double[] values, double expected) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testMeanNonFinite(TestHelper.shuffle(rng, values), expected);
            testParallelStreamNonFinite(TestHelper.shuffle(rng, values), expected);
        }
    }

    static Stream<Arguments> testMeanNonFinite() {
        return Stream.of(
            Arguments.of(new double[] {}, Double.NaN),
            Arguments.of(new double[] {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
                Double.NaN),
            Arguments.of(new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
                Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
                Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {Double.POSITIVE_INFINITY, Double.MAX_VALUE},
                Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, -Double.MIN_VALUE},
                Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {Double.NaN, 34.56, 89.74}, Double.NaN),
            Arguments.of(new double[] {34.56, Double.NaN, 89.74}, Double.NaN),
            Arguments.of(new double[] {34.56, 89.74, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {Double.NaN, 3.14, Double.NaN, Double.NaN},
                Double.NaN),
            Arguments.of(new double[] {Double.NaN, Double.NaN, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, Double.MAX_VALUE},
                Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {-Double.MAX_VALUE, Double.POSITIVE_INFINITY},
                Double.POSITIVE_INFINITY)
        );
    }

    @ParameterizedTest
    @MethodSource(value = "testCombine")
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
    @MethodSource(value = "testCombine")
    void testCombineRandomOrder(double[] array1, double[] array2) {
        UniformRandomProvider rng = TestHelper.createRNG();
        double[] data = TestHelper.concatenate(array1, array2);
        int n = array1.length;
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
    @MethodSource(value = "testCombine")
    void testArrayOfArrays(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpected(combinedArray);
        final double[][] values = {array1, array2};
        double actual = Arrays.stream(values)
                .map(Mean::of)
                .reduce(Mean::combine)
                .map(Mean::getAsDouble)
                .orElseThrow(RuntimeException::new);
        TestHelper.assertEquals(expected, actual, ULP_COMBINE, () -> "array of arrays combined mean");
    }

    static Stream<Arguments> testCombine() {
        return Stream.of(
            Arguments.of(new double[] {}, new double[] {1}),
            Arguments.of(new double[] {1}, new double[] {}),
            Arguments.of(new double[] {}, new double[] {1, 7, -15, 3}),
            Arguments.of(new double[] {0}, new double[] {0, 0.0}),
            Arguments.of(new double[] {4, 8, -6, 3, 18}, new double[] {1, -7, 6}),
            Arguments.of(new double[] {10, 8, 13, 9, 11, 14, 6, 4, 12, 7, 5}, new double[] {8, 8, 8, 8, 8, 8, 8, 19, 8, 8, 8}),
            Arguments.of(new double[] {10, 8, 13, 9, 11, 14, 6, 4, 12, 7, 5}, new double[] {7.46, 6.77, 12.74, 7.11, 7.81, 8.84, 6.08, 5.39, 8.15, 6.42, 5.73}),
            Arguments.of(new double[] {6.0, -1.32, -5.78, 8.967, 13.32, -9.67, 0.14, 7.321, 11.456, -3.111}, new double[] {2, 2, 2, 2}),
            Arguments.of(new double[] {2.3}, new double[] {-42, 10, -88, 5, -17}),
            Arguments.of(new double[] {-20, 34.983, -12.745, 28.12, -8.34, 42, -4, 16}, new double[] {3.14, 2.718, 1.414}),
            Arguments.of(new double[] {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0, 8.2, 10.3, 11.3, 14.1, 9.9}, new double[] {12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0, 8.8, 9.0, 12.3}),
            Arguments.of(new double[] {-0.0}, new double[] {+0.0}),
            Arguments.of(new double[] {0.0}, new double[] {-0.0}),
            Arguments.of(new double[] {0.0}, new double[] {+0.0}),
            Arguments.of(new double[] {10E-50, 5E-100}, new double[] {25E-200, 35.345E-50}),
            Arguments.of(new double[] {Double.MAX_VALUE}, new double[] {Double.MAX_VALUE}),
            Arguments.of(new double[] {-Double.MAX_VALUE}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {-Double.MAX_VALUE, 1}, new double[] {1}),
            Arguments.of(new double[] {Double.MAX_VALUE, 3.1415E153}, new double[] {}),
            Arguments.of(new double[] {1}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {1, 1, 1}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE}, new double[] {1, 1E300}),
            Arguments.of(new double[] {Double.MAX_VALUE}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE}, new double[] {Double.MAX_VALUE, -Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}, new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}, new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE})
        );
    }

    @ParameterizedTest
    @MethodSource(value = "testCombineNonFinite")
    void testCombineNonFinite(double[][] values, double expected) {
        Mean mean1 = Mean.create();
        Mean mean2 = Mean.create();
        Arrays.stream(values[0]).forEach(mean1);
        Arrays.stream(values[1]).forEach(mean2);
        double mean2BeforeCombine = mean2.getAsDouble();
        mean1.combine(mean2);
        Assertions.assertEquals(expected, mean1.getAsDouble(), "combine non-finite");
        Assertions.assertEquals(mean2BeforeCombine, mean2.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = "testCombineNonFinite")
    void testCombineRandomOrderNonFinite(double[][] values, double expected) {
        UniformRandomProvider rng = TestHelper.createRNG();
        double[] data = TestHelper.concatenate(values[0], values[1]);
        int n = values[0].length;
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
    @MethodSource(value = "testCombineNonFinite")
    void testArrayOfArraysNonFinite(double[][] values, double expected) {
        double actual = Arrays.stream(values)
                .map(Mean::of)
                .reduce(Mean::combine)
                .map(Mean::getAsDouble)
                .orElseThrow(RuntimeException::new);
        Assertions.assertEquals(expected, actual, "array of arrays combined mean non-finite");
    }

    static Stream<Arguments> testCombineNonFinite() {
        return Stream.of(
            Arguments.of(new double[][] {{}, {}}, Double.NaN),
            Arguments.of(new double[][] {{Double.POSITIVE_INFINITY}, {Double.NEGATIVE_INFINITY}}, Double.NaN),
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

    // Helper function to compute the expected value of Mean using BigDecimal.
    private static double computeExpected(double[] values) {
        BigDecimal bd = BigDecimal.ZERO;
        for (double value : values) {
            bd = bd.add(new BigDecimal(value));
        }
        return bd.divide(BigDecimal.valueOf(values.length), MathContext.DECIMAL128).doubleValue();
    }
}
