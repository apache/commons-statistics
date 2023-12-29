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
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.apache.commons.statistics.distribution.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Kurtosis}.
 */
final class KurtosisTest extends BaseDoubleStatisticTest<Kurtosis> {

    @Override
    protected Kurtosis create() {
        return Kurtosis.create();
    }

    @Override
    protected Kurtosis create(double... values) {
        return Kurtosis.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected int getEmptySize() {
        return 3;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedKurtosis(values);
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Not supported
        return Double.NaN;
    }

    @Override
    protected DoubleTolerance getTolerance() {
        // The kurtosis is not very precise.
        // Both the accept and array tests observe failures at well over 100 ulp.
        return createAbsOrRelTolerance(0, 3e-13);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // The value 2^1023 will overflow the sum of squared deviations
        builder.accept(addReference(Double.NaN, 0, 0, 0, 0x1.0p1023));
        // The value 2^500 will overflow the sum of cubed deviations but not
        // the sum of squared deviations
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 0, 0, 0x1.0p500));
        // The value 2^300 will overflow the sum of fourth deviations
        // but not the sum of cubed deviations
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 0, 0, 0x1.0p300));
        // Note: Many versions of bias-corrected kurtosis exist.
        // This uses libraries with the same "standard unbiased estimator".
        // SciPy v1.11.1: scipy.stats.kurtosis(x, bias=False, fisher=True)
        builder.accept(addReference(-1.2000000000000004, DoubleTolerances.ulps(30),
            1, 2, 3, 4, 5));
        builder.accept(addReference(-2.098602258096087, DoubleTolerances.ulps(10), 2, 8, 0, 4, 1, 9, 9, 0));
        // Excel v16.78 23100802: KURT(x)
        builder.accept(addReference(10.3116694214876, DoubleTolerances.ulps(15), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     * This is exposed for reuse in higher order moments.
     *
     * @param values Values.
     * @return Kurtosis of values
     */
    static double computeExpectedKurtosis(double[] values) {
        final long n = values.length;
        if (n < 4) {
            return Double.NaN;
        }
        // High precision deviations.
        // Capture the mean for reuse.
        final BigDecimal[] mean = new BigDecimal[1];
        final BigDecimal s2 = TestHelper.computeExpectedSumOfSquaredDeviations(values, mean);
        // Check for a zero denominator.
        if (s2.compareTo(BigDecimal.ZERO) == 0) {
            // Return 0 / 0
            return Double.NaN;
        }
        if (!Double.isFinite(s2.doubleValue())) {
            return Double.NaN;
        }
        final BigDecimal s4 = SumOfFourthDeviationsTest.computeExpectedSumOfFourthDeviations(values, mean[0]);
        if (!Double.isFinite(s2.doubleValue())) {
            return Double.NaN;
        }
        // Compute in BigDecimal so the order of operations does not change
        // the double precision result.
        final BigDecimal np1 = BigDecimal.valueOf(n + 1);
        final BigDecimal n0 = BigDecimal.valueOf(n);
        final BigDecimal nm1 = BigDecimal.valueOf(n - 1);
        final BigDecimal nm2 = BigDecimal.valueOf(n - 2);
        final BigDecimal nm3 = BigDecimal.valueOf(n - 3);
        final BigDecimal a = np1.multiply(n0).multiply(nm1).multiply(s4);
        final BigDecimal b = nm2.multiply(nm3).multiply(s2.pow(2));
        final BigDecimal c = BigDecimal.valueOf(3).multiply(nm1.pow(2));
        final BigDecimal d = nm2.multiply(nm3);
        return a.divide(b, MathContext.DECIMAL128).subtract(
            c.divide(d, MathContext.DECIMAL128)).doubleValue();
    }

    @ParameterizedTest
    @MethodSource
    void testBiased(double[] values, double biased, double unbiased, DoubleTolerance tol) {
        final Kurtosis stat = Kurtosis.of(values);
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
        final double nan = Double.NaN;
        // Python scipy v1.11.1: scipy.stats.kurtosis(x, bias=True/False)
        builder.accept(Arguments.of(new double[] {1, 2, 4, 8}, -1.0989792060491494, 0.7576559546313799, tol));
        builder.accept(Arguments.of(new double[] {1}, nan, nan, tol));
        builder.accept(Arguments.of(new double[] {1, 3, 9, 13, 15}, -1.625216788067985, -2.5008671522719403,
            DoubleTolerances.ulps(5)));
        // Matlab R2023s: kurtosis(x, 1/0) - 3
        // Note: the -3 is required to convert the Pearson kurtosis to the Fisher-Pearson kurtosis.
        // Scipy only allows bias correction when n>3. When n<=3 it returns the biased value.
        // Matlab will return NaN for bias correction when n<=3.
        // This implementation matches the behaviour of Matlab.
        builder.accept(Arguments.of(new double[] {1, 2}, -2, nan, tol));
        builder.accept(Arguments.of(new double[] {1, 2, 3}, -1.5, nan, tol));
        builder.accept(Arguments.of(new double[] {1, 3, 7, 9, 11}, -1.4908058409951321, -1.9632233639805285, tol));
        return builder.build();
    }
}
