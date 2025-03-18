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
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link LongMean}.
 */
final class LongMeanTest extends BaseLongStatisticTest<LongMean> {

    @Override
    protected LongMean create() {
        return LongMean.create();
    }

    @Override
    protected LongMean create(long... values) {
        return LongMean.of(values);
    }

    @Override
    protected LongMean create(long[] values, int from, int to) {
        return LongMean.ofRange(values, from, to);
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(Double.NaN);
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(long... values) {
        return Mean.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        // Data with large shifts in the rolling mean is not computed very accurately
        return DoubleTolerances.relative(5e-8);
    }

    @Override
    protected StatisticResult getExpectedValue(long[] values) {
        final BigInteger sum = Arrays.stream(values)
            .mapToObj(BigInteger::valueOf)
            .reduce(BigInteger.ZERO, BigInteger::add);
        final double x = new BigDecimal(sum)
            .divide(BigDecimal.valueOf(values.length), MathContext.DECIMAL128)
            .doubleValue();
        return createStatisticResult(x);
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
        final long[] a = new long[2 * 512 * 512];
        Arrays.fill(a, 0, a.length / 2, 10);
        Arrays.fill(a, a.length / 2, a.length, 1);
        builder.accept(addReference(5.5, a));

        // Same cases as for the DoubleStatistic Variance but the tolerance is exact
        final DoubleTolerance tol = DoubleTolerances.equals();

        // Python Numpy v1.25.1: numpy.mean
        builder.accept(addReference(2.5, tol, 1, 2, 3, 4));
        builder.accept(addReference(12.0, tol, 5, 9, 13, 14, 10, 12, 11, 15, 19));
        // R v4.3.1: mean(x)
        builder.accept(addReference(5.5, tol, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        builder.accept(addReference(8.75, tol, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    /**
     * Test large integer sums that overflow a {@code long}.
     * Overflow is created by repeat addition.
     *
     * <p>Note: Currently no check is made for overflow in the
     * count of observations. If this overflows then the statistic
     * will be incorrect so the test is limited to {@code n < 2^63}.
     */
    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.IntSumTest#testLongOverflow")
    void testLongOverflow(long x, long y, int exp) {
        final LongMean s = LongMean.of(x, y);
        final double mean = BigInteger.valueOf(x)
            .add(BigInteger.valueOf(y)).doubleValue() * 0.5;
        for (int i = 0; i < exp; i++) {
            // Assumes the sum as a long will overflow
            s.combine(s);
            Assertions.assertEquals(mean, s.getAsDouble());
        }
    }
}
