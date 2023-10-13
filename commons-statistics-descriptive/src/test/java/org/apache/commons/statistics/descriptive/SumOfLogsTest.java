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
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;

/**
 * Test for {@link SumOfLogs}.
 */
final class SumOfLogsTest extends BaseDoubleStatisticTest<SumOfLogs> {

    @Override
    protected SumOfLogs create() {
        return SumOfLogs.create();
    }

    @Override
    protected SumOfLogs create(double... values) {
        return SumOfLogs.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return 0;
    }

    @Override
    protected double mapValue(double value) {
        // Do not allow negative finite values.
        // Ignore zeros (since log(1) == 0).
        if (value == 0) {
            return 1;
        }
        return Math.abs(value);
    }

    @Override
    protected double getExpectedValue(double[] values) {
        // High precision sum using BigDecimal
        return Arrays.stream(values)
                     .map(Math::log)
                     .mapToObj(BigDecimal::new)
                     .reduce(BigDecimal.ZERO, BigDecimal::add)
                     .doubleValue();
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Use the JDK as a reference implementation for non-finite values
        return Arrays.stream(values).map(Math::log).sum();
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.ulps(1);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        // At least 1 zero; all other finite positive
        builder.accept(addReference(Double.NEGATIVE_INFINITY, DoubleTolerances.equals(), 0, 1, 2, 3, 4, Double.MAX_VALUE));
        // At least 1 negative; all other finite positive
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), -1, 1, 2, 3, 4));
        // At least 1 zero and 1 infinity
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 1, Double.POSITIVE_INFINITY));
        // Large cancellation (failed by a standard precision sum)
        builder.accept(addCase(0.25, 0.4, 0.5, 2, 2.5, 4));
        // Extreme range
        builder.accept(addCase(Double.MAX_VALUE, Double.MAX_VALUE / 2, Double.MAX_VALUE / 4, 42));
        // Python Numpy v1.25.1: numpy.log(x).sum()
        builder.accept(addReference(3.1780538303479453, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(21.848545372696186, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        return builder.build();
    }
}
