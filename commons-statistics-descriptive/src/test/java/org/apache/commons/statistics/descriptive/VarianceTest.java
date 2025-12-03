/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.apache.commons.statistics.distribution.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Variance}.
 */
final class VarianceTest extends BaseDoubleStatisticTest<Variance> {

    @Override
    protected Variance create() {
        return Variance.create();
    }

    @Override
    protected Variance create(double... values) {
        return Variance.of(values);
    }

    @Override
    protected Variance create(double[] values, int from, int to) {
        return Variance.ofRange(values, from, to);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedVariance(values, null);
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Not supported
        return Double.NaN;
    }

    // The full-array method should be more accurate on average.
    // However the tolerance is for the max error which is similar

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
        // Python Numpy v1.25.1: numpy.var(x, ddof=1)
        builder.accept(addReference(1.6666666666666667, DoubleTolerances.ulps(2), 1, 2, 3, 4));
        builder.accept(addReference(7.454545454545454, DoubleTolerances.ulps(10),
            14, 8, 11, 10, 7, 9, 10, 11, 10, 15, 5, 10));
        final double[] a = new double[2 * 512 * 512];
        Arrays.fill(a, 0, a.length / 2, 1.0);
        Arrays.fill(a, a.length / 2, a.length, 0.1);
        // Note: if ddof=0 the variance is ((1-0.55)**2 + (0.1-0.55)**2)/2 = 0.2025
        builder.accept(addReference(0.20250038623883485, createRelTolerance(1e-11), a));
        // R v4.3.1: var(x)
        builder.accept(addReference(9.166666666666666, DoubleTolerances.ulps(2), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        builder.accept(addReference(178.75, DoubleTolerances.ulps(2), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     * This is exposed for reuse in higher order moments.
     *
     * @param values Values.
     * @param mean Mean (result). Only computed if {@code length > 1}.
     * @return Variance of values
     */
    static double computeExpectedVariance(double[] values, BigDecimal[] mean) {
        final long n = values.length;
        if (n == 0) {
            return Double.NaN;
        }
        if (n == 1) {
            return 0;
        }
        final BigDecimal s2 = TestHelper.computeExpectedSumOfSquaredDeviations(values, mean);
        if (!Double.isFinite(s2.doubleValue())) {
            return Double.NaN;
        }
        return s2.divide(BigDecimal.valueOf(n - 1), MathContext.DECIMAL128).doubleValue();
    }

    @ParameterizedTest
    @MethodSource
    void testBiased(double[] values, double biased, double unbiased, DoubleTolerance tol) {
        final Variance stat = Variance.of(values);
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
        final DoubleTolerance tol = DoubleTolerances.ulps(1);
        // Python Numpy v1.25.1: numpy.var(x, ddof=0/1)
        // Note: Numpy allows other degrees of freedom adjustment than 0 or 1.
        builder.accept(Arguments.of(new double[] {1, 2, 3}, 0.6666666666666666, 1, tol));
        builder.accept(Arguments.of(new double[] {1, 2}, 0.25, 0.5, tol));
        // Matlab R2023s: var(x, 1/0)
        // Matlab only allows turning the biased option on (1) or off (0).
        // Note: Numpy will return NaN for ddof=1 when the array length is 1 (since 0 / 0 = NaN).
        // This implementation matches the behaviour of Matlab which returns zero.
        builder.accept(Arguments.of(new double[] {1}, 0, 0, tol));
        builder.accept(Arguments.of(new double[] {1, 2, 4, 8}, 7.1875, 9.583333333333334, tol));
        return builder.build();
    }

    /**
     * Test the variance of data with an extreme condition number.
     * The condition number is defined in Chan, Golub and Levesque (1983)
     * (American Statistician, 37, 242-247) as:
     * <pre>
     * k = sqrt(1 + mean^2 * N/S) = sqrt(1 + mean^2 / var)   (Eq 2.1)
     * </pre>
     * <p>where N is the number of samples, S is the sum-of-squared deviations from
     * the mean and var is the biased variance.
     *
     * <p>This test exists to demonstrate that the dual-pass with correction is more
     * accurate that the dual-pass algorithm. This can generate a different variance
     * for extreme data to other libraries that do not use the algorithm.
     */
    @Test
    void testBadlyConditionedData() {
        // This data has a variance of 1.4273452003842682e-29 in: Python numpy; Matlab; R.
        // The 'unlimited' precision result is 1.4262886902433472E-29.
        // This is different by a relative error of ~7.4e-4.
        // Condition number k ~ 3e14. This is very badly conditioned data.
        // 1.00000000000001 is 45 ULP above 1.0.
        final double[] values = {1, 1, 1, 1, 1, 1, 1.00000000000001};
        final double dualpassResult = 1.4273452003842682e-29;
        final double correctedDualpassResult = 1.4262886902433472E-29;
        final double v = Variance.of(values).getAsDouble();

        Assertions.assertNotEquals(dualpassResult, v, "Dual-pass result");
        Assertions.assertEquals(correctedDualpassResult, v, "Corrected dual-pass result");

        // Check using the helper function
        Assertions.assertEquals(computeExpectedVariance(values, null), v, "Reference expected result");

        // Using 256 decimal digits of precision
        final MathContext mc = new MathContext(256);
        final BigDecimal mean = Arrays.stream(values)
            .mapToObj(BigDecimal::new)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.length), mc);
        final double variance = Arrays.stream(values)
            .mapToObj(BigDecimal::new)
            .map(x -> x.subtract(mean))
            .map(x -> x.pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.length - 1), mc).doubleValue();
        Assertions.assertEquals(variance, v, "256-digit precision result");
    }
}
