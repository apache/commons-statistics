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

/**
 * Test for {@link SumOfLogs} using {@code int} values.
 */
final class IntSumOfLogsTest extends BaseIntStatisticTest<DoubleAsIntStatistic> {

    @Override
    protected DoubleAsIntStatistic create() {
        return DoubleAsIntStatistic.from(SumOfLogs.create());
    }

    @Override
    protected DoubleAsIntStatistic create(int... values) {
        return DoubleAsIntStatistic.from(SumOfLogs.of(values));
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(int... values) {
        return SumOfLogs.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        return DoubleTolerances.equals();
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(0);
    }

    @Override
    protected int mapValue(int value) {
        // Do not allow negative values.
        if (value < 0) {
            value = ~value;
        }
        if (value == 0) {
            // Ignore zeros (since log(1) == 0).
            return 1;
        }
        return value;
    }

    @Override
    protected StatisticResult getExpectedValue(int[] values) {
        return createStatisticResult(
            SumOfLogsTest.computeExpectedSumOfLogs(Arrays.stream(values).asDoubleStream().toArray()));
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.ulps(1);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        // Same cases as for the DoubleStatistic SumOfLogs
        // At least 1 zero; all other finite positive
        builder.accept(addReference(Double.NEGATIVE_INFINITY, DoubleTolerances.equals(), 0, 1, 2, 3, 4, Integer.MAX_VALUE));
        // At least 1 negative; all other finite positive
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), -1, 1, 2, 3, 4));
        // Extreme range
        builder.accept(addCase(Integer.MAX_VALUE, Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 4, 42));
        // Python Numpy v1.25.1: numpy.log(x).sum()
        builder.accept(addReference(3.1780538303479453, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(21.848545372696186, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        return builder.build();
    }
}
