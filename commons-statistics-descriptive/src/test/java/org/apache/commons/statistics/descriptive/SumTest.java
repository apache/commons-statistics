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
 * Test for {@link Sum}.
 */
final class SumTest extends BaseDoubleStatisticTest<Sum> {

    @Override
    protected Sum create() {
        return Sum.create();
    }

    @Override
    protected Sum create(double... values) {
        return Sum.of(values);
    }

    @Override
    protected Sum create(double[] values, int from, int to) {
        return Sum.ofRange(values, from, to);
    }

    @Override
    protected double getEmptyValue() {
        return 0;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        // High precision sum using BigDecimal
        return Arrays.stream(values)
                     .mapToObj(BigDecimal::new)
                     .reduce(BigDecimal.ZERO, BigDecimal::add)
                     .doubleValue();
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Use the JDK as a reference implementation for non-finite values
        return Arrays.stream(values).sum();
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.ulps(1);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        // Large numbers
        builder.accept(addCase(10E-50, 5E-100, 25E-200, 35.345E-50));
        // Small numbers
        builder.accept(addCase(0.001, 0.0002, 0.00003, 10000.11, 0.000004));
        // Overflow
        builder.accept(addCase(Double.MAX_VALUE, Double.MAX_VALUE));
        builder.accept(addCase(-Double.MAX_VALUE, -Double.MAX_VALUE));
        // Large cancellation (failed by a standard precision sum)
        builder.accept(addCase(1, Double.MAX_VALUE, -7, -Double.MAX_VALUE, 2, 3));
        builder.accept(addCase(1, 10E100, -10E100, 1));
        // Extreme range
        builder.accept(addCase(-Double.MAX_VALUE, 1, 1));
        // Python Numpy v1.25.1: numpy.sum
        builder.accept(addReference(10.0, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(108.0, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        final double[] a = new double[2 * 512 * 512];
        Arrays.fill(a, 0, a.length / 2, 1.0);
        Arrays.fill(a, a.length / 2, a.length, 0.1);
        builder.accept(addReference(288358.4000000004, DoubleTolerances.ulps(10), a));
        return builder.build();
    }
}
