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

/**
 * Test for {@link Variance}.
 */
final class VarianceTest extends BaseDoubleStatisticTest<Variance> {

    @Override
    protected Variance create() {
        return Variance.create();
    }

    @Override
    protected Variance create(double... values) {
        return Variance.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedVariance(values, null);
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Not supported
        return Double.NaN;
    }

    // The full-array method should be more accurate on average;
    // however the tolerance is for the max error which is similar

    @Override
    protected DoubleTolerance getToleranceAccept() {
        return DoubleTolerances.ulps(8);
    }

    @Override
    protected DoubleTolerance getToleranceArray() {
        return DoubleTolerances.ulps(8);
    }

    @Override
    protected DoubleTolerance getToleranceAcceptAndCombine() {
        return DoubleTolerances.ulps(10);
    }

    @Override
    protected DoubleTolerance getToleranceArrayAndCombine() {
        return DoubleTolerances.ulps(10);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // Non-finite sum-of-squared deviations
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 0x1.0p1023));
        // Python Numpy v1.25.1: numpy.var(x, ddof=1)
        builder.accept(addReference(1.6666666666666667, DoubleTolerances.ulps(2), 1, 2, 3, 4));
        builder.accept(addReference(7.454545454545454, DoubleTolerances.ulps(4),
            14, 8, 11, 10, 7, 9, 10, 11, 10, 15, 5, 10));
        final double[] a = new double[2 * 512 * 512];
        Arrays.fill(a, 0, a.length / 2, 1.0);
        Arrays.fill(a, a.length / 2, a.length, 0.1);
        // Note: if ddof=0 the variance is ((1-0.55)**2 + (0.1-0.55)**2)/2 = 0.2025
        builder.accept(addReference(0.20250038623883485, createRelTolerance(1e-11), a));
        // R v4.3.1: var(x)
        builder.accept(addReference(9.166666666666666, DoubleTolerances.ulps(1), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        builder.accept(addReference(178.75, DoubleTolerances.ulps(1), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     * This is exposed for reuse in higher order moments.
     *
     * @param values Values.
     * @param mean Mean (result). Only computed if {@code length > 1}.
     * @return Variance of values
     */
    static double computeExpectedVariance(double[] values, BigDecimal[] mean) {
        long n = values.length;
        if (n == 0) {
            return Double.NaN;
        }
        if (n == 1) {
            return 0;
        }
        final BigDecimal s2 = TestHelper.computeExpectedSumOfSquaredDeviations(values, mean);
        if (!Double.isFinite(s2.doubleValue())) {
            return Double.NaN;
        }
        return s2.divide(BigDecimal.valueOf(n - 1), MathContext.DECIMAL128).doubleValue();
    }
}
