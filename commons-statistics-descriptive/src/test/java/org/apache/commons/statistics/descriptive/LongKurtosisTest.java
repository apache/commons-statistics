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
 * Test for {@link Kurtosis} using {@code long} values.
 */
final class LongKurtosisTest extends BaseLongStatisticTest<DoubleAsLongStatistic> {

    @Override
    protected DoubleAsLongStatistic create() {
        return DoubleAsLongStatistic.from(Kurtosis.create());
    }

    @Override
    protected DoubleAsLongStatistic create(long... values) {
        return DoubleAsLongStatistic.from(Kurtosis.of(values));
    }

    @Override
    protected DoubleAsLongStatistic create(long[] values, int from, int to) {
        return DoubleAsLongStatistic.from(Kurtosis.ofRange(values, from, to));
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(long... values) {
        return Kurtosis.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        // The double[] and int[] construction do not agree closely if the
        // compute mean is different. The integer version is more robust.
        return DoubleTolerances.ulps(100);
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(Double.NaN);
    }

    @Override
    protected int getEmptySize() {
        return 3;
    }

    @Override
    protected StatisticResult getExpectedValue(long[] values) {
        final double[] x = Arrays.stream(values).asDoubleStream().toArray();
        return createStatisticResult(
            KurtosisTest.computeExpectedKurtosis(x));
    }

    @Override
    protected DoubleTolerance getTolerance() {
        // Same as the DoubleStatistic Kurtosis
        return createAbsOrRelTolerance(0, 3e-13);
    }

    @Override
    protected boolean skipExtremeData() {
        // The array algorithm cannot compute on the [max, min, 1, 1, 1, ...] data.
        // The mean is exact but the double representation of (x - mean) suffers from
        // floating-point error. This accumulates to make the relative error ~6e-8.
        return true;
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();

        // Same cases as for the DoubleStatistic Kurtosis

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
}
