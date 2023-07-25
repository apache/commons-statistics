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
 * Test for {@link Sum}.
 */
final class SumTest {

    @Test
    void testEmpty() {
        Sum sum = Sum.create();
        Assertions.assertEquals(0.0, sum.getAsDouble());
    }

    @Test
    void testNan() {
        Sum sum = Sum.create();
        double[] testArray = {Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : testArray) {
            sum.accept(value);
        }
        Assertions.assertEquals(Double.NaN, sum.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = "testSum")
    void testSum(double[] values, double expected) {
        Sum sum = Sum.create();
        for (double value : values) {
            sum.accept(value);
        }
        TestHelper.assertEquals(expected, sum.getAsDouble(), 1, () -> "sum");
        TestHelper.assertEquals(expected, Sum.of(values).getAsDouble(), 1, () -> "of (values)");
    }

    static Stream<Arguments> testSum() {
        return Stream.of(
            Arguments.of(new double[] {}, 0.0),
            Arguments.of(new double[] {0, 0, 0.0}, 0.0),
            Arguments.of(new double[] {1, -7, 6}, 0),
            Arguments.of(new double[] {1, 7, -15, 3}, -4),
            Arguments.of(new double[] {2, 2, 2, 2}, 8.0),
            Arguments.of(new double[] {2.3}, 2.3),
            Arguments.of(new double[] {3.14, 2.718, 1.414}, 7.272),
            Arguments.of(new double[] {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0,
                8.2, 10.3, 11.3, 14.1, 9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0, 8.8,
                9.0, 12.3}, 272.9),
            Arguments.of(new double[] {-0.0, +0.0}, 0.0),
            Arguments.of(new double[] {0.0, -0.0}, 0.0),
            Arguments.of(new double[] {0.0, +0.0}, 0.0),
            Arguments.of(new double[] {0.001, 0.0002, 0.00003, 10000.11, 0.000004}, 10000.111234),
            Arguments.of(new double[] {10E-50, 5E-100, 25E-200, 35.345E-50}, 45.345E-50),
            Arguments.of(new double[] {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
                Double.NaN),
            Arguments.of(new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
                Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
                Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {Double.POSITIVE_INFINITY, Double.MAX_VALUE},
                Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE},
                Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {-Double.MAX_VALUE, Double.POSITIVE_INFINITY},
                Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE},
                Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, -Double.MIN_VALUE},
                Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {Double.NaN, 34.56, 89.74}, Double.NaN),
            Arguments.of(new double[] {34.56, Double.NaN, 89.74}, Double.NaN),
            Arguments.of(new double[] {34.56, 89.74, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {Double.NaN, 3.14, Double.NaN, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {Double.NaN, Double.NaN, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, Double.MAX_VALUE},
                Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {Double.MAX_VALUE, 1}, Double.MAX_VALUE),
            Arguments.of(new double[] {-Double.MAX_VALUE, 1, 1}, -Double.MAX_VALUE),
            Arguments.of(new double[] {-Double.MAX_VALUE, -1, 1}, -Double.MAX_VALUE),
            Arguments.of(new double[] {Double.MAX_VALUE, -1}, Double.MAX_VALUE),
            Arguments.of(new double[] {1, 2, 3, Double.MAX_VALUE, -Double.MAX_VALUE, -7}, -1),
            Arguments.of(new double[] {1, 10E100, -10E100, 1}, 2)
        );
    }

    @ParameterizedTest
    @MethodSource(value = "testSum")
    void testParallelStream(double[] values, double expected) {
        double ans = Arrays.stream(values)
                .parallel()
                .collect(Sum::create, Sum::accept, Sum::combine)
                .getAsDouble();
        TestHelper.assertEquals(expected, ans, 1, () -> "parallel stream sum");
    }

    @ParameterizedTest
    @MethodSource(value = "testSum")
    void testSumRandomOrder(double[] values, double expected) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 0; i < 10; i++) {
            testSum(TestHelper.shuffle(rng, values), expected);
            testParallelStream(TestHelper.shuffle(rng, values), expected);
            testCombine(TestHelper.shuffle(rng, values), expected);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "testSum")
    void testCombine(double[] values, double expected) {
        Sum sum1 = Sum.create();
        Sum sum2 = Sum.create();
        Arrays.stream(values).forEach(sum1);
        Arrays.stream(values).forEach(sum2);
        double sum2BeforeCombine = sum2.getAsDouble();
        sum1.combine(sum2);
        TestHelper.assertEquals(expected * 2, sum1.getAsDouble(), 1, () -> "combined sum");
        Assertions.assertEquals(sum2BeforeCombine, sum2.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource
    void testArrayOfArrays(double[][] arr, double expected) {
        double actual = Arrays.stream(arr)
                .map(Sum::of)
                .reduce(Sum::combine)
                .map(Sum::getAsDouble)
                .orElseThrow(RuntimeException::new);
        TestHelper.assertEquals(expected, actual, 1, () -> "array of arrays combined sum");
    }

    static Stream<Arguments> testArrayOfArrays() {
        return Stream.of(
                Arguments.of(new double[][] {{}, {}, {}}, 0.0),
                Arguments.of(new double[][] {{1.0}, {2.0}}, 3.0),
                Arguments.of(new double[][] {{}, {1.1, 2}, {-1.7}}, 1.4),
                Arguments.of(new double[][] {{1, 2}, {3, 4}}, 10.0),
                Arguments.of(new double[][] {{+0.0, 2.0}, {1.0, -0.0, 3.14}}, 6.14),
                Arguments.of(new double[][] {{1, Double.MAX_VALUE, -7},
                    {-Double.MAX_VALUE, 2, 3}}, -1),
                Arguments.of(new double[][] {{+0.0, Double.NEGATIVE_INFINITY},
                    {-0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}}, Double.NaN),
                Arguments.of(new double[][] {{Double.POSITIVE_INFINITY, Double.MAX_VALUE},
                    {Double.MIN_VALUE, 2.345e+307}}, Double.POSITIVE_INFINITY),
                Arguments.of(new double[][] {{Double.NEGATIVE_INFINITY, -Double.MIN_VALUE},
                    {Double.MAX_VALUE, 0.0}}, Double.NEGATIVE_INFINITY),
                Arguments.of(new double[][] {{}, {Double.NaN}, {-1.7}}, Double.NaN),
                Arguments.of(new double[][] {{}, {Double.NaN}, {}}, Double.NaN),
                Arguments.of(new double[][] {{1.1, 22.22}, {34.56, -5678.9, 2.718}, {Double.NaN, 0}},
                    Double.NaN),
                Arguments.of(new double[][] {{Double.NaN, Double.NaN}, {Double.NaN}, {Double.NaN, Double.NaN, Double.NaN}}, Double.NaN),
                Arguments.of(new double[][] {{1, 10E100}, {-10E100, 1}}, 2)
        );
    }
}
