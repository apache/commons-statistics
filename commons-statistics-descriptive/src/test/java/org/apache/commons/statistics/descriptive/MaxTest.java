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
import java.util.function.DoubleSupplier;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Max}.
 */
final class MaxTest {

    @Test
    void testEmpty() {
        Max max = Max.create();
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, max.getAsDouble());
    }

    @Test
    void testIncrement() {
        // Test the max after each incremental update
        // First parameter of testArray is the value that would be added
        // Second parameter of testArray is the max we expect after adding the value
        double[][] testArray = {
            {1729.22, 1729.22},
            {2520.35, 2520.35},
            {2010.87, 2520.35},
            {100000000.1, 100000000.1},
            {+0.0, 100000000.1},
            {-0.0, 100000000.1},
            {Double.MAX_VALUE, Double.MAX_VALUE},
            {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
            {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}
        };

        Max stat = Max.create();
        for (final double[] valueAndExpected: testArray) {
            final double value = valueAndExpected[0];
            final double expected = valueAndExpected[1];
            stat.accept(value);
            Assertions.assertEquals(expected, stat.getAsDouble());
        }
    }

    @Test
    void testNaN() {
        // Test non-nan values cannot revert a NaN
        double[] testArray = {Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        Max stat = Max.create();
        for (final double x : testArray) {
            stat.accept(x);
            Assertions.assertEquals(Double.NaN, stat.getAsDouble());
        }
    }

    @ParameterizedTest
    @MethodSource
    void testMax(double[] values, double expected) {
        Max stat = Max.create();
        Arrays.stream(values).forEach(stat);
        double actual = stat.getAsDouble();
        Assertions.assertEquals(expected, actual, "max");
        Assertions.assertEquals(expected, Max.of(values).getAsDouble(), "of(values)");
    }

    static Stream<Arguments> testMax() {
        return Stream.of(
            Arguments.of(new double[] {}, Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {3.14}, 3.14),
            Arguments.of(new double[] {12.34, 56.78, -2.0}, 56.78),
            Arguments.of(new double[] {Double.NaN, 3.14, Double.NaN, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {-1d, 1d, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {Double.NaN, Double.NaN, Double.NaN}, Double.NaN),
            Arguments.of(new double[] {0.0d, Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}, Double.NaN),
            Arguments.of(new double[] {+0.0d, -0.0d, 1.0, 3.14}, 3.14),
            Arguments.of(new double[] {-0.0, +0.0}, 0.0),
            Arguments.of(new double[] {0.0, -0.0}, 0.0),
            Arguments.of(new double[] {0.0, +0.0}, 0.0),
            Arguments.of(new double[] {1.2, -34.56, 456.789, -5678.9012}, 456.789),
            Arguments.of(new double[] {-23467824, 23648, 2368, 23749, -23424, -23492, -92397747}, 23749),
            Arguments.of(new double[] {1.7976931348623157e+307, 1.7976931348623157e+306, 0.0, Double.MAX_VALUE}, Double.MAX_VALUE),
            Arguments.of(new double[] {1.7976931348623157e+307, 1.7976931348623157e+306, 0.0, -Double.MAX_VALUE}, 1.7976931348623157e+307),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, Double.MAX_VALUE, Double.POSITIVE_INFINITY}, Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -Double.MIN_VALUE}, -Double.MIN_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource(value = "testMax")
    void testParallelStream(double[] values, double expected) {
        double actual = Arrays.stream(values).parallel().collect(Max::create, Max::accept, Max::combine).getAsDouble();
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource(value = "testMax")
    void testMaxRandomOrder(double[] values, double expected) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 0; i < 10; i++) {
            testMax(TestHelper.shuffle(rng, values), expected);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testCombine(double[] first, double[] second, double expected) {
        Max firstMax = Max.create();
        Max secondMax = Max.create();

        Arrays.stream(first).forEach(firstMax);
        Arrays.stream(second).forEach(secondMax);

        double secondMaxBeforeCombine = secondMax.getAsDouble();
        firstMax.combine(secondMax);
        Assertions.assertEquals(expected, firstMax.getAsDouble());
        Assertions.assertEquals(secondMaxBeforeCombine, secondMax.getAsDouble());
    }

    static Stream<Arguments> testCombine() {
        return Stream.of(
            Arguments.of(new double[] {}, new double[] {}, Double.NEGATIVE_INFINITY),
            Arguments.of(new double[] {3.14}, new double[] {}, 3.14),
            Arguments.of(new double[] {}, new double[] {2.718}, 2.718),
            Arguments.of(new double[] {}, new double[] {Double.NaN}, Double.NaN),
            Arguments.of(new double[] {Double.NaN, Double.NaN}, new double[] {}, Double.NaN),
            Arguments.of(new double[] {3.14}, new double[] {2.718}, 3.14),
            Arguments.of(new double[] {-1, 0, 1}, new double[] {1.1, 2.2, 3.3}, 3.3),
            Arguments.of(new double[] {3.14, 1.1, 22.22}, new double[] {2.718, 1.1, 333.333}, 333.333),
            Arguments.of(new double[] {12.34, 56.78, -2.0}, new double[] {0.0, 23.45}, 56.78),
            Arguments.of(new double[] {-2023.79, 11.11, 333.333}, new double[] {1.1}, 333.333),
            Arguments.of(new double[] {1.1, +0.0, 3.14}, new double[] {22.22, 2.718, -0.0}, 22.22),
            Arguments.of(new double[] {0.0, -Double.MAX_VALUE, Double.POSITIVE_INFINITY},
                new double[] {Double.NEGATIVE_INFINITY, -0.0, Double.NEGATIVE_INFINITY, Double.MAX_VALUE},
                Double.POSITIVE_INFINITY),
            Arguments.of(new double[] {0.0, Double.NaN, -Double.MAX_VALUE, Double.POSITIVE_INFINITY},
                new double[] {Double.NaN, -0.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.MAX_VALUE},
                Double.NaN)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testArrayOfArrays(double[][] input, double expected) {
        double actual = Arrays.stream(input)
                .map(Max::of)
                .reduce(Max::combine)
                .map(DoubleSupplier::getAsDouble)
                .orElseThrow(RuntimeException::new);

        Assertions.assertEquals(expected, actual);
    }

    static Stream<Arguments> testArrayOfArrays() {
        return Stream.of(
                Arguments.of(new double[][] {{}, {}, {}}, Double.NEGATIVE_INFINITY),
                Arguments.of(new double[][] {{}, {Double.NaN}, {-1.7}}, Double.NaN),
                Arguments.of(new double[][] {{}, {Double.NaN}, {}}, Double.NaN),
                Arguments.of(new double[][] {{}, {1.1, 2}, {-1.7}}, 2),
                Arguments.of(new double[][] {{1, 2}, {3, 4}}, 4),
                Arguments.of(new double[][] {{+0.0, 2.0}, {1.0, -0.0, 3.14}}, 3.14),
                Arguments.of(new double[][] {{+0.0, Double.NEGATIVE_INFINITY}, {-0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}}, Double.POSITIVE_INFINITY),
                Arguments.of(new double[][] {{1.1, 22.22}, {34.56, -5678.9, 2.718}, {Double.NaN, 0}},
                        Double.NaN),
                Arguments.of(new double[][] {{Double.NaN, Double.NaN}, {Double.NaN}, {Double.NaN, Double.NaN, Double.NaN}}, Double.NaN)
        );
    }
}
