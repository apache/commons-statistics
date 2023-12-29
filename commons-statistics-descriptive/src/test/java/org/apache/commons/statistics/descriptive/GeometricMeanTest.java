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
import java.math.MathContext;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.junit.jupiter.api.Assertions;

/**
 * Test for {@link GeometricMean}.
 */
final class GeometricMeanTest extends BaseDoubleStatisticTest<GeometricMean> {

    @Override
    protected GeometricMean create() {
        return GeometricMean.create();
    }

    @Override
    protected GeometricMean create(double... values) {
        return GeometricMean.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
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
        return computeExpectedGeometricMean(values);
    }

    /**
     * Compute the expected geometric mean.
     *
     * @param values Values.
     * @return the result
     */
    static double computeExpectedGeometricMean(double[] values) {
        final int n = values.length;
        if (n == 0) {
            Assertions.fail();
        }
        // High precision sum using BigDecimal
        final BigDecimal sumOfLogs = Arrays.stream(values)
            .map(Math::log)
            .mapToObj(BigDecimal::new)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Math.exp(sumOfLogs.divide(BigDecimal.valueOf(n), MathContext.DECIMAL128).doubleValue());
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Use the JDK as a reference implementation for non-finite values
        return Math.exp(Arrays.stream(values).map(Math::log).sum() / values.length);
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.ulps(1);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        // At least 1 zero; all other finite positive
        builder.accept(addReference(0.0, DoubleTolerances.equals(), 0, 1, 2, 3, 4, Double.MAX_VALUE));
        // At least 1 negative; all other finite positive
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), -1, 1, 2, 3, 4));
        // At least 1 zero and 1 infinity
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 1, Double.POSITIVE_INFINITY));
        // Python SciPy version 1.11: scipy.stats.gmean(x)
        builder.accept(addReference(2.213363839400643, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(11.331836486692287, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        // Matlab v2023a: geomean(x)
        builder.accept(addReference(4.3456634281443094, DoubleTolerances.ulps(3), 0.25, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        builder.accept(addReference(0.0022133638394006435, DoubleTolerances.ulps(3), 0.001, 0.002, 0.003, 0.004));
        return builder.build();
    }
}
