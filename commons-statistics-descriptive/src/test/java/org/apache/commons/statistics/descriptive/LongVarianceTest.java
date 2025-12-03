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
import java.math.BigInteger;
import java.math.MathContext;
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
 * Test for {@link LongVariance}.
 */
final class LongVarianceTest extends BaseLongStatisticTest<LongVariance> {

    @Override
    protected LongVariance create() {
        return LongVariance.create();
    }

    @Override
    protected LongVariance create(long... values) {
        return LongVariance.of(values);
    }

    @Override
    protected LongVariance create(long[] values, int from, int to) {
        return LongVariance.ofRange(values, from, to);
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(long... values) {
        if (values.length == 0) {
            return Variance.create();
        }
        // Detect cases where all the values are the same double (no variance)
        // and map values with a shift.
        if (values.length > 1) {
            final double first = values[0];
            if (!Arrays.stream(values).asDoubleStream().filter(x -> x != first).findAny().isPresent()) {
                final long shift = Arrays.stream(values).min().orElse(0);
                values = Arrays.stream(values).map(x -> x - shift).toArray();
            }
        }
        return Variance.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        return DoubleTolerances.ulps(20);
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(Double.NaN);
    }

    @Override
    protected StatisticResult getExpectedValue(long[] values) {
        return createStatisticResult(computeExpectedVariance(values));
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.equals();
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        builder.accept(addCase(Long.MAX_VALUE - 1, Long.MAX_VALUE));
        builder.accept(addCase(Long.MIN_VALUE + 1, Long.MIN_VALUE));

        // Same cases as for the DoubleStatistic Variance but the tolerance is exact
        final DoubleTolerance tol = DoubleTolerances.equals();

        // Python Numpy v1.25.1: numpy.var(x, ddof=1)
        builder.accept(addReference(1.6666666666666667, tol, 1, 2, 3, 4));
        builder.accept(addReference(7.454545454545454, tol,
            14, 8, 11, 10, 7, 9, 10, 11, 10, 15, 5, 10));
        // R v4.3.1: var(x)
        builder.accept(addReference(9.166666666666666, tol, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        builder.accept(addReference(178.75, tol, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     *
     * @param values Values.
     * @return Variance of values
     */
    static double computeExpectedVariance(long[] values) {
        if (values.length == 1) {
            return 0;
        }
        final BigInteger s = Arrays.stream(values).mapToObj(BigInteger::valueOf)
            .reduce(BigInteger.ZERO, BigInteger::add);
        final BigInteger ss = Arrays.stream(values)
            .mapToObj(i -> BigInteger.valueOf(i).pow(2))
            .reduce(BigInteger.ZERO, BigInteger::add);
        final MathContext mc = MathContext.DECIMAL128;
        final int n = values.length;
        // var = (n * sum(x^2) - sum(x)^2) / (n * (n-1))
        // Exact numerator
        final BigInteger num = ss.multiply(BigInteger.valueOf(n)).subtract(s.pow(2));
        // Exact divide
        return new BigDecimal(num)
            .divide(BigDecimal.valueOf(n * (n - 1L)), mc)
            .doubleValue();
    }

    @ParameterizedTest
    @MethodSource
    void testBiased(long[] values, double biased, double unbiased, DoubleTolerance tol) {
        final LongVariance stat = LongVariance.of(values);
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
        // Same cases as for the DoubleStatistic Variance but the tolerance is exact
        final DoubleTolerance tol = DoubleTolerances.equals();

        // Note: Biased variance is ((10-5.5)**2 + (1-5.5)**2)/2 = 20.25
        // Scale by (2 * 512 * 512) / (2 * 512 * 512 - 1)
        // The variance is invariant to shift
        final long shift = -1379182644762676428L;
        final long[] a = new long[2 * 512 * 512];
        Arrays.fill(a, 0, a.length / 2, 10 + shift);
        Arrays.fill(a, a.length / 2, a.length, 1 + shift);
        builder.accept(Arguments.of(a, 20.25, 20.250038623883484, tol));

        // Python Numpy v1.25.1: numpy.var(x, ddof=0/1)
        // Note: Numpy allows other degrees of freedom adjustment than 0 or 1.
        builder.accept(Arguments.of(new long[] {1, 2, 3}, 0.6666666666666666, 1, tol));
        builder.accept(Arguments.of(new long[] {1, 2}, 0.25, 0.5, tol));
        // Matlab R2023s: var(x, 1/0)
        // Matlab only allows turning the biased option on (1) or off (0).
        // Note: Numpy will return NaN for ddof=1 when the array length is 1 (since 0 / 0 = NaN).
        // This implementation matches the behaviour of Matlab which returns zero.
        builder.accept(Arguments.of(new long[] {1}, 0, 0, tol));
        builder.accept(Arguments.of(new long[] {1, 2, 4, 8}, 7.1875, 9.583333333333334, tol));
        return builder.build();
    }

    /**
     * Test a large integer sums that overflow a {@code long}.
     * Overflow is created by repeat addition.
     *
     * <p>Note: Currently no check is made for overflow in the
     * count of observations. If this overflows then the statistic
     * will be incorrect so the test is limited to {@code n < 2^63}.
     */
    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.IntSumTest#testLongOverflow")
    void testLongOverflow(long x, long y, int exp) {
        final LongVariance s = LongVariance.of(x, y);
        // var = sum((x - mean)^2) / (n-1)
        //     = (n * sum(x^2) - sum(x)^2) / (n * (n-1))
        long n = 2;
        BigInteger term1 = BigInteger.valueOf(x).pow(2).add(BigInteger.valueOf(y).pow(2));
        BigInteger term2 = BigInteger.valueOf(x).add(BigInteger.valueOf(y));
        final DoubleTolerance tol = DoubleTolerances.ulps(2);
        for (int i = 0; i < exp; i++) {
            // Assumes the sum as a long will overflow
            s.combine(s);
            n <<= 1;
            term1 = term1.add(term1);
            term2 = term2.add(term2);
            final double expected = new BigDecimal(
                    term1.multiply(BigInteger.valueOf(n)).subtract(term2.pow(2)))
                .divide(
                    new BigDecimal(BigInteger.valueOf(n).multiply(BigInteger.valueOf(n - 1))),
                    MathContext.DECIMAL128)
                .doubleValue();
            TestUtils.assertEquals(expected, s.getAsDouble(), tol);
        }
    }
}
