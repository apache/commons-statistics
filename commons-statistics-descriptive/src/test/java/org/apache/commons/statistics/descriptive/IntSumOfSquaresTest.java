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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link IntSumOfSquares}.
 */
final class IntSumOfSquaresTest extends BaseIntStatisticTest<IntSumOfSquares> {

    @Override
    protected ResultType getResultType() {
        return ResultType.BIG_INTEGER;
    }

    @Override
    protected IntSumOfSquares create() {
        return IntSumOfSquares.create();
    }

    @Override
    protected IntSumOfSquares create(int... values) {
        return IntSumOfSquares.of(values);
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(int... values) {
        return SumOfSquares.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        // Floating-point sum may be inexact.
        // Currently the double sum matches on the standard test data.
        // It fails on large random data added in streamTestData().
        return DoubleTolerances.ulps(5);
    }

    @Override
    protected StatisticResult getEmptyValue() {
        // It does not matter that this returns a IntStatisticResult
        // rather than a BigIntegerStatisticResult
        return createStatisticResult(0);
    }

    @Override
    protected StatisticResult getExpectedValue(int[] values) {
        final BigInteger x = Arrays.stream(values)
            .mapToObj(i -> BigInteger.valueOf((long) i * i))
            .reduce(BigInteger.ZERO, BigInteger::add);
        return createStatisticResult(x);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        // A null seed will create a different RNG each time
        final UniformRandomProvider rng = TestHelper.createRNG(null);
        return Stream.of(
            addCase(Integer.MAX_VALUE, 1, 2, 3, 4, Integer.MAX_VALUE),
            addCase(Integer.MIN_VALUE, -1, -2, -3, -4, Integer.MIN_VALUE),
            addCase(rng.ints(5).toArray()),
            addCase(rng.ints(10).toArray()),
            addCase(rng.ints(20).toArray()),
            addCase(rng.ints(40).toArray())
        );
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
    @CsvSource({
        "-1628367811, -516725738, 60",
        "627834682, 456456670, 61",
        "2147483647, 2147483646, 61",
        "-2147483648, -2147483647, 61",
    })
    void testLongOverflow(int x, int y, int exp) {
        final IntSumOfSquares s = IntSumOfSquares.of(x, y);
        BigInteger sum = BigInteger.valueOf((long) x * x).add(BigInteger.valueOf((long) y * y));
        for (int i = 0; i < exp; i++) {
            s.combine(s);
            sum = sum.shiftLeft(1);
            Assertions.assertEquals(sum, s.getAsBigInteger());
        }
    }
}
