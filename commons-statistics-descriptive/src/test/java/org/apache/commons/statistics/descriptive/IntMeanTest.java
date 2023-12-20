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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link IntMean}.
 */
final class IntMeanTest extends BaseIntStatisticTest<IntMean> {

    @Override
    protected IntMean create() {
        return IntMean.create();
    }

    @Override
    protected IntMean create(int... values) {
        return IntMean.of(values);
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(int... values) {
        return Mean.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        // Large shifts in the rolling mean are not computed very accurately
        return DoubleTolerances.relative(5e-8);
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(Double.NaN);
    }

    @Override
    protected StatisticResult getExpectedValue(int[] values) {
        // Use the JDK as a reference implementation
        final double x = Arrays.stream(values).average().orElse(Double.NaN);
        return createStatisticResult(x);
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
        final int[] a = new int[2 * 512 * 512];
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
     * Test a large integer sums that overflow a {@code long}.
     * Overflow is created by repeat addition.
     *
     * <p>Note: Currently no check is made for overflow in the
     * count of observations. If this overflows then the statistic
     * will be incorrect so the test is limited to {@code n < 2^63}.
     */
    @ParameterizedTest
    @CsvSource({
        "-1628367811, -516725738, 60",
        "627834682, 456456670, 61",
        "2147483647, 2147483646, 61",
        "-2147483648, -2147483647, 61",
    })
    void testLongOverflow(int x, int y, int exp) {
        final IntMean s = IntMean.of(x, y);
        final double mean = ((long) x + y) * 0.5;
        for (int i = 0; i < exp; i++) {
            // Assumes the sum as a long will overflow
            s.combine(s);
            Assertions.assertEquals(mean, s.getAsDouble());
        }
    }
}
