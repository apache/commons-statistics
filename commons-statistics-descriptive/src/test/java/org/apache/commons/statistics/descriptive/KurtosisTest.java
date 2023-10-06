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
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;

/**
 * Test for {@link Kurtosis}.
 */
final class KurtosisTest extends BaseDoubleStatisticTest<Kurtosis> {

    @Override
    protected Kurtosis create() {
        return Kurtosis.create();
    }

    @Override
    protected Kurtosis create(double... values) {
        return Kurtosis.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected int getEmptySize() {
        return 3;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedKurtosis(values);
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Not supported
        return Double.NaN;
    }

    @Override
    protected DoubleTolerance getTolerance() {
        // The kurtosis is not very precise.
        // Both the accept and array tests observe failures at well over 100 ulp.
        return createAbsOrRelTolerance(0, 3e-13);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // The value 2^1023 will overflow the sum of squared deviations
        builder.accept(addReference(Double.NaN, 0, 0, 0, 0x1.0p1023));
        // The value 2^500 will overflow the sum of cubed deviations but not
        // the sum of squared deviations
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 0, 0, 0x1.0p500));
        // The value 2^300 will overflow the sum of fourth deviations
        // but not the sum of cubed deviations
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 0, 0, 0x1.0p300));
        return builder.build();
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     * This is exposed for reuse in higher order moments.
     *
     * @param values Values.
     * @return Kurtosis of values
     */
    private static double computeExpectedKurtosis(double[] values) {
        final long n = values.length;
        if (n < 4) {
            return Double.NaN;
        }
        // High precision deviations.
        // Capture the mean for reuse.
        final BigDecimal[] mean = new BigDecimal[1];
        final BigDecimal s2 = TestHelper.computeExpectedSumOfSquaredDeviations(values, mean);
        // Check for a zero denominator.
        if (s2.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        if (!Double.isFinite(s2.doubleValue())) {
            return Double.NaN;
        }
        final BigDecimal s4 = SumOfFourthDeviationsTest.computeExpectedSumOfFourthDeviations(values, mean[0]);
        if (!Double.isFinite(s2.doubleValue())) {
            return Double.NaN;
        }
        // Compute in BigDecimal so the order of operations does not change
        // the double precision result.
        final BigDecimal np1 = BigDecimal.valueOf(n + 1);
        final BigDecimal n0 = BigDecimal.valueOf(n);
        final BigDecimal nm1 = BigDecimal.valueOf(n - 1);
        final BigDecimal nm2 = BigDecimal.valueOf(n - 2);
        final BigDecimal nm3 = BigDecimal.valueOf(n - 3);
        final BigDecimal a = np1.multiply(n0).multiply(nm1).multiply(s4);
        final BigDecimal b = nm2.multiply(nm3).multiply(s2.pow(2));
        final BigDecimal c = BigDecimal.valueOf(3).multiply(nm1.pow(2));
        final BigDecimal d = nm2.multiply(nm3);
        return a.divide(b, MathContext.DECIMAL128).subtract(
            c.divide(d, MathContext.DECIMAL128)).doubleValue();
    }
}
