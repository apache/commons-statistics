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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.function.DoubleSupplier;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * Test for {@link Min}.
 */
final class MinTest {

    @Test
    public void testEmpty() {
        Min min = Min.create();
        Min immutable = Min.of();

        Assertions.assertEquals(Double.POSITIVE_INFINITY, min.getAsDouble());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, immutable.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource
    public void testMin(double[] values, double expectedMin) {
        Min stat = Min.create();
        for (final double value: values) {
            stat.accept(value);
        }
        double actualMin = stat.getAsDouble();
        Assertions.assertEquals(expectedMin, actualMin, "min");
        Assertions.assertEquals(expectedMin, Min.of(values).getAsDouble(), "min");
    }

    static Stream<Arguments> testMin() {
        return Stream.of(
                Arguments.of(new double[] {3.14}, 3.14),
                Arguments.of(new double[] {12.34, 56.78, -2.0}, -2.0),
                Arguments.of(new double[] {Double.NaN, 3.14, Double.NaN, Double.NaN}, Double.NaN),
                Arguments.of(new double[] {-0.0, +0.0}, -0.0),
                Arguments.of(new double[] {0.0, -0.0}, -0.0),
                Arguments.of(new double[] {1.2, -34.56, 456.789, -5678.9012}, -5678.9012),
                Arguments.of(new double[] {-23467824, 23648, 2368, 23749, -23424, -23492, -92397747}, -92397747)
        );
    }

    @Test
    public void testMinImmutable() {
        Min stat = Min.of(1.0, 2.0, -1.0, 4.0);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> stat.accept(3.14),
                "cannot update an immutable instance");
    }

    @ParameterizedTest
    @MethodSource
    public void testCombine(double[] first, double[] second, double expectedMin) {
        Min firstMin = Min.create();
        Min secondMin = Min.create();

        Arrays.stream(first).forEach(firstMin);
        Arrays.stream(second).forEach(secondMin);

        firstMin.combine(secondMin);
        Assertions.assertEquals(expectedMin, firstMin.getAsDouble());

        secondMin.combine(firstMin);
        Assertions.assertEquals(expectedMin, secondMin.getAsDouble());
    }

    static Stream<Arguments> testCombine() {
        return Stream.of(
                Arguments.of(new double[] {3.14}, new double[] {2.718}, 2.718),
                Arguments.of(new double[] {3.14, 1.1, 22.22}, new double[] {2.718, 1.1, 333.333}, 1.1),
                Arguments.of(new double[] {12.34, 56.78, -2.0}, new double[] {0.0, 23.45}, -2.0),
                Arguments.of(new double[] {-2023.79, 11.11, 333.333}, new double[] {1.1}, -2023.79),
                Arguments.of(new double[] {1.1, +0.0, 3.14}, new double[] {22.22, 2.718, -0.0}, -0.0),
                Arguments.of(new double[] {0.0, -Double.MIN_VALUE, Double.POSITIVE_INFINITY},
                        new double[] {Double.NEGATIVE_INFINITY, -0.0, Double.NEGATIVE_INFINITY, Double.MIN_VALUE},
                        Double.NEGATIVE_INFINITY),
                Arguments.of(new double[] {0.0, Double.NaN, -Double.MIN_VALUE, Double.POSITIVE_INFINITY},
                        new double[] {Double.NaN, -0.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.MIN_VALUE},
                        Double.NaN)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testParallelStream(double[] values, double expectedMin) {
        double actualMin = Arrays.stream(values).parallel().collect(Min::create, Min::accept, Min::combine).getAsDouble();
        Assertions.assertEquals(expectedMin, actualMin);

        DoubleStream nanStream = DoubleStream.of(Double.NaN, Double.NaN, Double.NaN);
        Assertions.assertTrue(Double.isNaN(nanStream.parallel().collect(Min::create, Min::accept, Min::combine).getAsDouble()));
    }

    static Stream<Arguments> testParallelStream() {
        return Stream.of(
                Arguments.of(new double[] {3.14}, 3.14),
                Arguments.of(new double[] {12.34, 56.78, -2.0}, -2.0),
                Arguments.of(new double[] {1.2, -34.56, 456.789, -5678.9012}, -5678.9012),
                Arguments.of(new double[] {-23467824, 23648, 2368, 23749, -23424, -23492, -92397747}, -92397747),
                Arguments.of(new double[] {-1d, 1d, Double.NaN}, Double.NaN),
                Arguments.of(new double[] {+0.0d, -0.0d, 1.0, 3.14}, -0.0d),
                Arguments.of(new double[] {0.0d, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}, Double.NEGATIVE_INFINITY),
                Arguments.of(new double[] {0.0d, Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}, Double.NaN)
        );
    }

    @Test
    public void testCombineImmutable() {
        Min immutable = Min.of(1.1, 22.22, -333.333);
        Min mutable = Min.create();
        mutable.combine(immutable);
        Assertions.assertEquals(-333.333, mutable.getAsDouble());
        Assertions.assertEquals(-333.333, immutable.getAsDouble());

        Min mutable2 = Min.create();
        mutable2.accept(-4444.4444);
        mutable2.combine(immutable);
        Assertions.assertEquals(-4444.4444, mutable2.getAsDouble());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> immutable.combine(mutable),
                "cannot update an immutable instance");
    }

    @Test
    public void testSpecialValues() {
        double[] testArray = {0.0d, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        Min stat = Min.create();

        stat.accept(testArray[0]);
        Assertions.assertEquals(0.0d, stat.getAsDouble());

        stat.accept(testArray[1]);
        Assertions.assertEquals(0.0d, stat.getAsDouble());

        stat.accept(testArray[2]);
        Assertions.assertEquals(-0.0d, stat.getAsDouble());

        stat.accept(testArray[3]);
        Assertions.assertEquals(-0.0d, stat.getAsDouble());

        stat.accept(testArray[4]);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, stat.getAsDouble());

        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Min.of(testArray).getAsDouble());
    }

    @ParameterizedTest
    @MethodSource
    public void testNaNs(double[] values, double expectedMin) {
        Min stat = Min.create();
        for (final double value: values) {
            stat.accept(value);
        }
        Assertions.assertEquals(expectedMin, stat.getAsDouble());

        Assertions.assertTrue(Double.isNaN(Min.of(Double.NaN, Double.NaN, Double.NaN).getAsDouble()));
    }

    static Stream<Arguments> testNaNs() {
        return Stream.of(
                Arguments.of(new double[] {Double.NaN, 2d, 3d}, Double.NaN),
                Arguments.of(new double[] {1d, Double.NaN, 3d}, Double.NaN),
                Arguments.of(new double[] {-1d, 1d, Double.NaN}, Double.NaN)
        );
    }

    @Test
    public void testNaN() {
        double[] testArray = {0.0d, Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        Min stat = Min.create();

        stat.accept(testArray[0]);
        Assertions.assertEquals(0.0d, stat.getAsDouble());

        stat.accept(testArray[1]);
        Assertions.assertEquals(Double.NaN, stat.getAsDouble());

        stat.accept(testArray[2]);
        Assertions.assertEquals(Double.NaN, stat.getAsDouble());

        stat.accept(testArray[3]);
        Assertions.assertEquals(Double.NaN, stat.getAsDouble());

        stat.accept(testArray[4]);
        Assertions.assertEquals(Double.NaN, stat.getAsDouble());

        stat.accept(testArray[5]);
        Assertions.assertEquals(Double.NaN, stat.getAsDouble());

        Assertions.assertEquals(Double.NaN, Min.of(testArray).getAsDouble());
    }

    @ParameterizedTest
    @MethodSource
    public void testArrayOfArrays(double[][] input, double expectedMin) {

        double actualMin = Arrays.stream(input)
                .map(Min::of)
                .reduce(Min::combine)
                .map(DoubleSupplier::getAsDouble)
                .orElseThrow(RuntimeException::new);

        Assertions.assertEquals(expectedMin, actualMin);
    }
    static Stream<Arguments> testArrayOfArrays() {
        return Stream.of(
                Arguments.of(new double[][] {{1, 2}, {3, 4}}, 1),
                Arguments.of(new double[][] {{+0.0, 2.0}, {1.0, -0.0, 3.14}}, -0.0),
                Arguments.of(new double[][] {{+0.0, Double.NEGATIVE_INFINITY},
                        {-0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}}, Double.NEGATIVE_INFINITY),
                Arguments.of(new double[][] {{1.1, 22.22}, {34.56, -5678.9, 2.718}, {Double.NaN, 0}},
                        Double.NaN),
                Arguments.of(new double[][] {{Double.NaN, Double.NaN}, {Double.NaN},
                        {Double.NaN, Double.NaN, Double.NaN}}, Double.NaN)
        );
    }

}
