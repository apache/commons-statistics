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
 * Test for {@link IntMin}.
 */
final class IntMinTest extends BaseIntStatisticTest<IntMin> {

    @Override
    protected ResultType getResultType() {
        return ResultType.INT;
    }

    @Override
    protected IntMin create() {
        return IntMin.create();
    }

    @Override
    protected IntMin create(int... values) {
        return IntMin.of(values);
    }

    @Override
    protected IntMin create(int[] values, int from, int to) {
        return IntMin.ofRange(values, from, to);
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(int... values) {
        return Min.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        return DoubleTolerances.equals();
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(Integer.MAX_VALUE);
    }

    @Override
    protected StatisticResult getExpectedValue(int[] values) {
        // Use the JDK as a reference implementation
        final int x = Arrays.stream(values).min().orElse(Integer.MAX_VALUE);
        return createStatisticResult(x);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        return Stream.of(
            addCase(Integer.MAX_VALUE - 1),
            addCase(Integer.MIN_VALUE + 1)
        );
    }
}
