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
 * Test for {@link Product} using {@code long} values.
 */
final class LongProductTest extends BaseLongStatisticTest<DoubleAsLongStatistic> {

    @Override
    protected DoubleAsLongStatistic create() {
        return DoubleAsLongStatistic.from(Product.create());
    }

    @Override
    protected DoubleAsLongStatistic create(long... values) {
        return DoubleAsLongStatistic.from(Product.of(values));
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(long... values) {
        return Product.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        return DoubleTolerances.equals();
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(1);
    }

    @Override
    protected StatisticResult getExpectedValue(long[] values) {
        // The product is not high precision
        final double x = Arrays.stream(values).asDoubleStream().reduce(1, (a, b) -> a * b);
        return createStatisticResult(x);
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.ulps(20);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        // Same cases as for the DoubleStatistic Product
        // Python Numpy v1.25.1: numpy.product
        builder.accept(addReference(24.0, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(3081078000.0, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        // Case with ULP error of 11 and at least 17 observed with random permutation
        builder.accept(addCase(43, 40, 42, 41, 42, 35, 38, 39, 47, 38, 44, 51, 38, 45, 58, 45, 46, 49, 55, 49, 47, 43,
            45, 46, 43, 46, 42, 41, 51, 44, 45, 44, 49, 48, 50, 51, 52, 53, 50, 56, 55, 52, 42, 45, 48, 49, 51, 49, 47,
            50, 44, 59, 40, 43, 38, 46, 39, 46, 36, 41, 46, 48, 50, 42, 51, 70, 49, 43, 35, 43, 48, 52, 63, 45, 53, 39,
            52, 45, 41, 43, 49, 42, 32, 47, 37, 46, 35, 42, 47, 42, 57, 45, 55, 51, 40, 43, 45, 46, 53, 49));
        return builder.build();
    }
}
