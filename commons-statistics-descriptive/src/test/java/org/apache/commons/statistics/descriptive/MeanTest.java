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
        return TestData.momentTestData().map(this::addCase);
    }
}
