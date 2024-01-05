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
package org.apache.commons.statistics.inference;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.Mean;
import org.apache.commons.statistics.descriptive.Variance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link StatisticUtils}.
 */
class StatisticUtilsTest {

    @ParameterizedTest
    @ValueSource(doubles = {-1, 0, 1})
    void testSubtract(double y) {
        final double[] x = {-99, -7, 0, 1, 1.23, 4, 42, 10234};
        final double[] xmy = StatisticUtils.subtract(x, y);
        if (y == 0) {
            Assertions.assertSame(x, xmy);
            // Test null array
            Assertions.assertNull(StatisticUtils.subtract(null, y),
                "null array should be returned unchanged");
        } else {
            for (int i = 0; i < x.length; i++) {
                Assertions.assertEquals(x[i] - y, xmy[i]);
            }
            // Test null array
            Assertions.assertThrows(NullPointerException.class, () -> StatisticUtils.subtract(null, y),
                "cannot change a null array");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "1, 0",
        "1, 1",
        "2, 0",
        "2, 1",
        "10, 0",
        "10, 1",
        "10, 8",
        "10, 9",
        "10, 10",
        "10, 100",
    })
    void testComputeDegreesOfFreedom(int n, int m) {
        final int df = n - 1 - m;
        if (df <= 0) {
            TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> StatisticUtils.computeDegreesOfFreedom(n, m), "degrees", "of", "freedom", Integer.toString(df));
        } else {
            Assertions.assertEquals(df, StatisticUtils.computeDegreesOfFreedom(n, m));
        }
    }

    @Test
    void testComputeRatioThrows() {
        final BiConsumer<double[], long[]> action = StatisticUtils::computeRatio;
        // Samples must be present, i.e. length > 1
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1}, new long[] {1, 2}), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1, 1}, new long[] {1}), "values", "size");

        // Observed is zero
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1, 1}, new long[] {0, 0}), "no", "data");

        // Samples not same size, i.e. cannot be paired
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1, 1}, new long[] {1, 2, 3}), "values", "size", "mismatch");

        // not strictly positive expected
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {0, 1}, new long[] {1, 1}), "0.0");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {-0.5, 1}, new long[] {1, 1}), "-0.5");
        // negative observed
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1, 1}, new long[] {1, -1}), "negative", "-1");

        // x and y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, null));

        // x or y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, new long[] {1, 2}));
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(new double[] {1, 1}, null));
    }

    @ParameterizedTest
    @MethodSource
    void testComputeRatio(double[] e, long[] o) {
        final double ratio = StatisticUtils.computeRatio(e, o);
        // Sum as an exact double
        final double sum1 = Arrays.stream(o).mapToObj(BigInteger::valueOf)
            .reduce(BigInteger.ZERO, BigInteger::add).doubleValue();
        final double sum2 = Arrays.stream(e).map(x -> x * ratio).sum();
        TestUtils.assertRelativelyEquals(sum1, sum2, Math.ulp(sum1), "rescaled sum(e) != sum(o)");
    }

    static Stream<Arguments> testComputeRatio() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final long[] o = {1, 2, 3};
        final double[] e = Arrays.stream(o).mapToDouble(i -> i).toArray();
        // Equal
        builder.add(Arguments.of(e, o, 1.0));
        // Not equal
        builder.add(Arguments.of(Arrays.stream(e).map(i -> 2 * i).toArray(), o));
        builder.add(Arguments.of(Arrays.stream(e).map(i -> 0.5 * i).toArray(), o));
        // Almost equal
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] + Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] + 2 * Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] + 3 * Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] + 4 * Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] + 5 * Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] - Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] - 2 * Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] - 3 * Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] - 4 * Math.ulp(e[2])}, o));
        builder.add(Arguments.of(new double[] {e[0], e[1], e[2] - 5 * Math.ulp(e[2])}, o));
        // Random
        builder.add(Arguments.of(new double[] {1.23, 3.45, 6.78}, new long[] {4, 18, 9}));
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create(123);
        for (int n = 2; n < 1024; n *= 2) {
            builder.add(Arguments.of(rng.doubles(n).toArray(), rng.longs(n, 0, Integer.MAX_VALUE).toArray()));
            // values that are not integers
            builder.add(Arguments.of(rng.doubles(n).toArray(), rng.longs(n).map(x -> x >>> 1).toArray()));
        }
        return builder.build();
    }

    @Test
    void testMeanAndVarianceSize0() {
        final double[] data = {};
        Assertions.assertEquals(Double.NaN, StatisticUtils.mean(Arrays.asList()), "mean (empty)");
        Assertions.assertEquals(Double.NaN, StatisticUtils.mean(Arrays.asList(data)), "mean (single array)");
        Assertions.assertEquals(Double.NaN, StatisticUtils.mean(Arrays.asList(data, data, data)), "mean (multi array)");
        Assertions.assertEquals(Double.NaN, StatisticUtils.meanDifference(data, data), "meanDifference");
        Assertions.assertEquals(Double.NaN, StatisticUtils.varianceDifference(data, data, 0), "varianceDifference");
    }

    @Test
    void testMeanAndVarianceDiffereneSizeMimatch() {
        final double[] x = {1};
        final double[] y = {1, 2};
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> StatisticUtils.meanDifference(x, y), "meanDifference");
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> StatisticUtils.varianceDifference(x, y, 0), "varianceDifference");
    }

    @ParameterizedTest
    @MethodSource
    void testMeanAndVariance(double[] data, double mean, double variance) {
        final double deltaMean = Math.abs(mean) * 1e-15;

        // For the multiple array mean, split the array
        final double[] x = Arrays.copyOf(data, data.length / 2);
        final double[] y = Arrays.copyOfRange(data, data.length / 2, data.length);
        Assertions.assertEquals(mean, StatisticUtils.mean(Arrays.asList(x, y)), deltaMean, "mean(x, y)");

        // For the difference computation construct input so that data = data1 - data2.
        // This is simple if data1 = data and data2 is all zero but here
        // we offset by a constant to check data2 is used.
        final int c = 3;
        final double[] data1 = IntStream.range(0, data.length).mapToDouble(i -> data[i] + i + c).toArray();
        final double[] data2 = IntStream.range(0, data.length).mapToDouble(i -> i + c).toArray();
        final double m2 = StatisticUtils.meanDifference(data1, data2);
        final double v2 = StatisticUtils.varianceDifference(data1, data2, m2);
        Assertions.assertEquals(mean, m2, deltaMean, "meanDifference");
        Assertions.assertEquals(variance, v2, variance * 1e-15, "varianceDifference");

        // Compare to o.a.c.statistics.descriptive
        // This should match exactly
        Assertions.assertEquals(Mean.of(data).getAsDouble(), m2, "meanDifference");
        Assertions.assertEquals(Variance.of(data).getAsDouble(), v2, "varianceDifference");
    }

    static Stream<Arguments> testMeanAndVariance() {
        return Stream.of(
            // Bias corrected variance
            Arguments.of(new double[] {1.5}, 1.5, 0),
            // Python numpy 1.20.3: numpy.mean(x); numpy.var(x, ddof=1)
            Arguments.of(new double[] {4, -2, 1}, 1, 9),
            Arguments.of(new double[] {3214, 234, 234234, 2, 3244, 234, 234, 234, 234}, 26873.777777777777, 6048361810.444446),
            Arguments.of(new double[] {-23467824, 23648, 2368, 23749, -23424, -23492, -92397747}, -16551817.42857143, 1195057670342971.2)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMeanAndVariance2(double[] data1) {
        // Create dummy data for the difference
        final double[] data2 = new double[data1.length];
        final double m2 = StatisticUtils.meanDifference(data1, data2);
        final double v2 = StatisticUtils.varianceDifference(data1, data2, m2);
        // Compare to o.a.c.statistics.descriptive
        // This should match exactly
        Assertions.assertEquals(Mean.of(data1).getAsDouble(), m2, "meanDifference");
        Assertions.assertEquals(Variance.of(data1).getAsDouble(), v2, "varianceDifference");
    }

    static Stream<double[]> testMeanAndVariance2() {
        Stream.Builder<double[]> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create(123);
        for (int n = 2; n < 1024; n *= 2) {
            builder.add(rng.doubles(n).toArray());
        }
        return builder.build();
    }
}
