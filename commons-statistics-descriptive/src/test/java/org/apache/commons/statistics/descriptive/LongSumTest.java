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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link LongSum}.
 */
final class LongSumTest extends BaseLongStatisticTest<LongSum> {

    @Override
    protected ResultType getResultType() {
        return ResultType.BIG_INTEGER;
    }

    @Override
    protected LongSum create() {
        return LongSum.create();
    }

    @Override
    protected LongSum create(long... values) {
        return LongSum.of(values);
    }

    @Override
    protected LongSum create(long[] values, int from, int to) {
        return LongSum.ofRange(values, from, to);
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(long... values) {
        return Sum.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        // Floating-point sum may be inexact.
        // Currently the double sum matches on the standard test data.
        // It fails on large random data added in streamTestData().
        // 41-bits of precision ~ 4.5e-13
        return DoubleTolerances.relative(0x1.0p-41);
    }

    @Override
    protected StatisticResult getEmptyValue() {
        // It does not matter that this returns a IntStatisticResult
        // rather than a BigIntegerStatisticResult
        return createStatisticResult(0);
    }

    @Override
    protected StatisticResult getExpectedValue(long[] values) {
        final BigInteger x = Arrays.stream(values)
            .mapToObj(BigInteger::valueOf)
            .reduce(BigInteger.ZERO, BigInteger::add);
        return createStatisticResult(x);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final UniformRandomProvider rng = TestHelper.createRNG();
        return Stream.of(
            addCase(Long.MAX_VALUE, 1, 2, 3, 4, -20, Long.MAX_VALUE),
            addCase(Long.MIN_VALUE, -1, -2, -3, -4, 20, Long.MIN_VALUE),
            addCase(rng.longs(5).toArray()),
            addCase(rng.longs(10).toArray()),
            addCase(rng.longs(20).toArray()),
            addCase(rng.longs(40).toArray()),
            // Case with relative error of 2.75E-14 compared to a double sum
            addCase(-4487066808448153496L, 7787390584347681521L, -7858453033172463569L, 47145713150439093L,
                3633776039196843638L, -4341282612965864270L, -1485329079196125724L, 6233716229349935702L,
                7375800655291232789L, -6937385231548425316L),
            // Case with relative error of 3.72E-14 compared to a double sum
            addCase(1762148088465390728L, -7115931862782408920L, -5636555954314137385L, 6430559082344489638L,
                4114490813139260252L, 8579187161455135584L, 7739722798844245340L, 2891970534844301876L,
                -4237740878246353493L, -4070467815045385449L, -78616704962308788L, 4337904397494590309L,
                -5978347168335833324L, 516700626540252157L, 6376613030089653543L, -987353620073772000L,
                -860005478611364991L, -110486665566037318L, -7489804585071312763L, -6181238641752885618L)
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
    @MethodSource
    void testLongOverflow(long x, long y, int exp) {
        final LongSum s = LongSum.of(x, y);
        BigInteger sum = BigInteger.valueOf(x).add(BigInteger.valueOf(y));
        for (int i = 0; i < exp; i++) {
            // Assumes the sum as a long will overflow
            s.combine(s);
            sum = sum.shiftLeft(1);
            Assertions.assertEquals(sum, s.getAsBigInteger());
        }
    }

    static Stream<Arguments> testLongOverflow() {
        return Stream.of(
            Arguments.of(-1628367672438123811L, -97927322516725738L, 60),
            Arguments.of(3279208082627834682L, 4234564566706285432L, 61),
            Arguments.of(9223372036854775807L, 9223372036854775806L, 61),
            Arguments.of(-9223372036854775808L, -9223372036854775807L, 61));
    }
}
