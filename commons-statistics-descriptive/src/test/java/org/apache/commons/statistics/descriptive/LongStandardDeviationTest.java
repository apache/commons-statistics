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
 * Test for {@link LongStandardDeviation}.
 */
final class LongStandardDeviationTest extends BaseLongStatisticTest<LongStandardDeviation> {

    @Override
    protected LongStandardDeviation create() {
        return LongStandardDeviation.create();
    }

    @Override
    protected LongStandardDeviation create(long... values) {
        return LongStandardDeviation.of(values);
    }

    @Override
    protected LongStandardDeviation create(long[] values, int from, int to) {
        return LongStandardDeviation.ofRange(values, from, to);
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(long... values) {
        return StandardDeviation.of(Arrays.stream(values).asDoubleStream().toArray());
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
        return createStatisticResult(Math.sqrt(LongVarianceTest.computeExpectedVariance(values)));
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.equals();
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        builder.accept(addCase(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
        builder.accept(addCase(Integer.MIN_VALUE + 1, Integer.MIN_VALUE));

        // Same cases as for the DoubleStatistic StandardDeviation but the tolerance is exact
        final DoubleTolerance tol = DoubleTolerances.equals();

        // Python Numpy v1.25.1: numpy.std(x, ddof=1)
        builder.accept(addReference(1.2909944487358056, tol, 1, 2, 3, 4));
        builder.accept(addReference(2.73030134866931, tol,
            14, 8, 11, 10, 7, 9, 10, 11, 10, 15, 5, 10));
        // R v4.3.1: sd(x)
        builder.accept(addReference(3.0276503540974917, tol, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        builder.accept(addReference(13.369741957120938, tol, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("testAccept")
    void testConsistentWithVarianceAccept(long[] values) {
        assertConsistentWithVariance(Statistics.add(LongVariance.create(), values),
                                     Statistics.add(LongStandardDeviation.create(), values));
    }

    @ParameterizedTest
    @MethodSource("testArray")
    void testConsistentWithVarianceArray(long[] values) {
        assertConsistentWithVariance(LongVariance.of(values),
                                     LongStandardDeviation.of(values));
    }

    @ParameterizedTest
    @MethodSource("testAcceptAndCombine")
    void testConsistentWithVarianceCombine(long[][] values) {
        // Assume the sequential stream will combine in the same order.
        // Do not use a parallel stream which may be stochastic.
        final LongVariance variance = Arrays.stream(values)
            .map(LongVariance::of)
            .reduce(LongVariance::combine)
            .orElseGet(LongVariance::create);
        final LongStandardDeviation std = Arrays.stream(values)
            .map(LongStandardDeviation::of)
            .reduce(LongStandardDeviation::combine)
            .orElseGet(LongStandardDeviation::create);
        assertConsistentWithVariance(variance, std);
    }

    private static void assertConsistentWithVariance(LongVariance variance, LongStandardDeviation std) {
        Assertions.assertEquals(Math.sqrt(variance.getAsDouble()), std.getAsDouble(), "Unbiased");
        variance.setBiased(true);
        std.setBiased(true);
        Assertions.assertEquals(Math.sqrt(variance.getAsDouble()), std.getAsDouble(), "Biased");
    }

    @ParameterizedTest
    @MethodSource
    void testBiased(long[] values, double biased, double unbiased, DoubleTolerance tol) {
        final LongStandardDeviation stat = LongStandardDeviation.of(values);
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
        LongVarianceTest.testBiased().forEach(arg -> {
            final Object[] args = arg.get();
            final Object a = args[0];
            final double biased = ((Number) args[1]).doubleValue();
            final double unbiased = ((Number) args[2]).doubleValue();
            final Object d = args[3];
            builder.accept(Arguments.of(a, Math.sqrt(biased), Math.sqrt(unbiased), d));
        });
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
    void testLongOverflow(int x, int y, int exp) {
        final LongStandardDeviation s = LongStandardDeviation.of(x, y);
        // var = sum((x - mean)^2) / (n-1)
        //     = (n * sum(x^2) - sum(x)^2) / (n * (n-1))
        long n = 2;
        BigInteger term1 = BigInteger.valueOf((long) x * x).add(BigInteger.valueOf((long) y * y));
        BigInteger term2 = BigInteger.valueOf((long) x + y);
        final DoubleTolerance tol = DoubleTolerances.ulps(2);
        for (int i = 0; i < exp; i++) {
            // Assumes the sum as a long will overflow
            s.combine(s);
            n <<= 1;
            term1 = term1.add(term1);
            term2 = term2.add(term2);
            final double expected = Math.sqrt(new BigDecimal(
                    term1.multiply(BigInteger.valueOf(n)).subtract(term2.pow(2)))
                .divide(
                    new BigDecimal(BigInteger.valueOf(n).multiply(BigInteger.valueOf(n - 1))),
                    MathContext.DECIMAL128)
                .doubleValue());
            TestUtils.assertEquals(expected, s.getAsDouble(), tol);
        }
    }
}
