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
 * Test for {@link Mean}.
 */
final class MeanTest extends BaseDoubleStatisticTest<Mean> {

    @Override
    protected Mean create() {
        return Mean.create();
    }

    @Override
    protected Mean create(double... values) {
        return Mean.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        // High precision mean
        return TestHelper.computeExpectedMean(values).doubleValue();
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Use the JDK as a reference implementation for non-finite values
        return Arrays.stream(values).average().orElse(getEmptyValue());
    }

    // Different precision for full-array method

    @Override
    protected DoubleTolerance getToleranceAccept() {
        return DoubleTolerances.ulps(6);
    }

    @Override
    protected DoubleTolerance getToleranceArray() {
        return DoubleTolerances.ulps(3);
    }

    @Override
    protected DoubleTolerance getToleranceAcceptAndCombine() {
        return DoubleTolerances.ulps(5);
    }

    @Override
    protected DoubleTolerance getToleranceArrayAndCombine() {
        return DoubleTolerances.ulps(5);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // Python Numpy v1.25.1: numpy.mean
        builder.accept(addReference(2.5, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(12.0, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        final double[] a = new double[2 * 512 * 512];
        Arrays.fill(a, 0, a.length / 2, 1.0);
        Arrays.fill(a, a.length / 2, a.length, 0.1);
        // Actual mean is 1.1 / 2 = 0.55
        builder.accept(addReference(0.5500000000000007, createRelTolerance(1e-13), a));
        // R v4.3.1: mean(x)
        builder.accept(addReference(5.5, DoubleTolerances.ulps(1), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        builder.accept(addReference(8.75, DoubleTolerances.ulps(1), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }
}
