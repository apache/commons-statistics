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
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.apache.commons.statistics.distribution.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link StandardDeviation}.
 * This test is based on the {@link VarianceTest}. The standard deviations is
 * tested to be consistent with the square root of the variance.
 */
final class StandardDeviationTest extends BaseDoubleStatisticTest<StandardDeviation> {

    @Override
    protected StandardDeviation create() {
        return StandardDeviation.create();
    }

    @Override
    protected StandardDeviation create(double... values) {
        return StandardDeviation.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedStandardDeviation(values);
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Not supported
        return Double.NaN;
    }

    // Re-use tolerances from the VarianceTest

    @Override
    protected DoubleTolerance getToleranceAccept() {
        return DoubleTolerances.ulps(15);
    }

    @Override
    protected DoubleTolerance getToleranceArray() {
        return DoubleTolerances.ulps(10);
    }

    @Override
    protected DoubleTolerance getToleranceAcceptAndCombine() {
        return DoubleTolerances.ulps(15);
    }

    @Override
    protected DoubleTolerance getToleranceArrayAndCombine() {
        return DoubleTolerances.ulps(10);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // Non-finite sum-of-squared deviations
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 0x1.0p1023));
        // Python Numpy v1.25.1: numpy.std(x, ddof=1)
        builder.accept(addReference(1.2909944487358056, DoubleTolerances.ulps(2), 1, 2, 3, 4));
        builder.accept(addReference(2.73030134866931, DoubleTolerances.ulps(10),
            14, 8, 11, 10, 7, 9, 10, 11, 10, 15, 5, 10));
        final double[] a = new double[2 * 512 * 512];
        Arrays.fill(a, 0, a.length / 2, 1.0);
        Arrays.fill(a, a.length / 2, a.length, 0.1);
        // Note: if ddof=0 the std.dev. is sqrt(((1-0.55)**2 + (0.1-0.55)**2)/2) = 0.45
        builder.accept(addReference(0.4500004291540563, createRelTolerance(1e-11), a));
        // R v4.3.1: sd(x)
        builder.accept(addReference(3.0276503540974917, DoubleTolerances.ulps(2), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        builder.accept(addReference(13.369741957120938, DoubleTolerances.ulps(2), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("testAccept")
    void testConsistentWithVarianceAccept(double[] values) {
        assertConsistentWithVariance(Statistics.add(Variance.create(), values),
                                     Statistics.add(StandardDeviation.create(), values));
    }

    @ParameterizedTest
    @MethodSource("testArray")
    void testConsistentWithVarianceArray(double[] values) {
        assertConsistentWithVariance(Variance.of(values),
                                     StandardDeviation.of(values));
    }

    @ParameterizedTest
    @MethodSource("testAcceptAndCombine")
    void testConsistentWithVarianceCombine(double[][] values) {
        // Assume the sequential stream will combine in the same order.
        // Do not use a parallel stream which may be stochastic.
        final Variance variance = Arrays.stream(values)
            .map(Variance::of)
            .reduce(Variance::combine)
            .orElseGet(Variance::create);
        final StandardDeviation std = Arrays.stream(values)
            .map(StandardDeviation::of)
            .reduce(StandardDeviation::combine)
            .orElseGet(StandardDeviation::create);
        assertConsistentWithVariance(variance, std);
    }

    private static void assertConsistentWithVariance(Variance variance, StandardDeviation std) {
        Assertions.assertEquals(Math.sqrt(variance.getAsDouble()), std.getAsDouble(), "Unbiased");
        variance.setBiased(true);
        std.setBiased(true);
        Assertions.assertEquals(Math.sqrt(variance.getAsDouble()), std.getAsDouble(), "Biased");
    }

    /**
     * Helper function to compute the expected standard deviation.
     *
     * @param values Values.
     * @return Standard deviation of values
     */
    private static double computeExpectedStandardDeviation(double[] values) {
        long n = values.length;
        if (n == 0) {
            return Double.NaN;
        }
        if (n == 1) {
            return 0;
        }
        return Math.sqrt(VarianceTest.computeExpectedVariance(values, null));
    }

    @ParameterizedTest
    @MethodSource
    void testBiased(double[] values, double biased, double unbiased, DoubleTolerance tol) {
        final StandardDeviation stat = StandardDeviation.of(values);
        // Default is unbiased
        final double actualUnbiased = stat.getAsDouble();
        TestUtils.assertEquals(unbiased, actualUnbiased, tol, () -> "Unbiased: " + format(values));
        Assertions.assertSame(stat, stat.setBiased(true));
        final double acutalBiased = stat.getAsDouble();
        TestUtils.assertEquals(biased, acutalBiased, tol, () -> "Biased: " + format(values));
        // The mutable state can be switched back and forth
        Assertions.assertSame(stat, stat.setBiased(false));
        Assertions.assertEquals(actualUnbiased, stat.getAsDouble(), () -> "Unbiased: " + format(values));
        Assertions.assertSame(stat, stat.setBiased(true));
        Assertions.assertEquals(acutalBiased, stat.getAsDouble(), () -> "Biased: " + format(values));
    }

    static Stream<Arguments> testBiased() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Repack the same cases from variance
        VarianceTest.testBiased().forEach(arg -> {
            final Object[] args = arg.get();
            final Object a = args[0];
            final double biased = ((Number) args[1]).doubleValue();
            final double unbiased = ((Number) args[2]).doubleValue();
            final Object d = args[3];
            builder.accept(Arguments.of(a, Math.sqrt(biased), Math.sqrt(unbiased), d));
        });
        return builder.build();
    }
}
