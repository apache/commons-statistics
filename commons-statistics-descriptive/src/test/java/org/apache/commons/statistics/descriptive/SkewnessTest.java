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
 * Test for {@link Skewness}.
 */
final class SkewnessTest extends BaseDoubleStatisticTest<Skewness> {

    @Override
    protected Skewness create() {
        return Skewness.create();
    }

    @Override
    protected Skewness create(double... values) {
        return Skewness.of(values);
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected int getEmptySize() {
        return 2;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedSkewness(values);
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // Not supported
        return Double.NaN;
    }

    @Override
    protected DoubleTolerance getTolerance() {
        // The skewness is not very precise.
        // Both the accept and array tests observe failures at over 100 ulp.
        return createAbsOrRelTolerance(0, 5e-14);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // The value 2^1023 will overflow the sum of squared deviations
        builder.accept(addReference(Double.NaN, 0, 0, 0x1.0p1023));
        // The value 2^500 will overflow the sum of cubed deviations but not
        // the sum of squared deviations
        builder.accept(addReference(Double.NaN, DoubleTolerances.equals(), 0, 0, 0x1.0p500));
        // SciPy v1.11.1: scipy.stats.skew(x, bias=False)
        // The accept and/or combine methods can drift away from zero so use an absolute tolerance
        builder.accept(addReference(0.0,
            createAbsTolerance(1e-15), DoubleTolerances.ulps(2),
            createAbsTolerance(1e-15), createAbsTolerance(1e-15),
            1, 2, 3, 4, 5));
        builder.accept(addReference(0.3305821804079746, DoubleTolerances.ulps(10), 2, 8, 0, 4, 1, 9, 9, 0));
        // Matlab v2023a: skewness(x, 0)   %% 0 is for bias correction
        builder.accept(addReference(3.1210230430100503, DoubleTolerances.ulps(5), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        return builder.build();
    }

    /**
     * Helper function to compute the expected variance using BigDecimal.
     * This is exposed for reuse in higher order moments.
     *
     * @param values Values.
     * @return Skewness of values
     */
    private static double computeExpectedSkewness(double[] values) {
        final long n = values.length;
        if (n < 3) {
            return Double.NaN;
        }
        // High precision deviations and variance.
        // These could be in BigDecimal but we forego this:
        // 1. The sum-of-cubed deviations returns the IEEE754 result in the case of overflow.
        // 2. The variance must be raised to the power 3/2, which cannot use BigDeicmal (JDK 8).
        // Capture the mean for reuse.
        final BigDecimal[] mean = new BigDecimal[1];
        final double variance = VarianceTest.computeExpectedVariance(values, mean);
        if (!Double.isFinite(variance)) {
            return Double.NaN;
        }
        // Here we add a check for a zero denominator.
        final double denom = variance * Math.sqrt(variance);
        if (denom == 0) {
            return 0;
        }
        final double m3 = SumOfCubedDeviationsTest.computeExpectedSC(values, mean[0]);
        if (!Double.isFinite(m3)) {
            return Double.NaN;
        }
        // Compute in BigDecimal so the order of operations does not change
        // the double precision result.
        final BigDecimal n0 = BigDecimal.valueOf(n);
        final BigDecimal nm1 = BigDecimal.valueOf(n - 1);
        final BigDecimal nm2 = BigDecimal.valueOf(n - 2);
        final BigDecimal a = n0.multiply(new BigDecimal(m3));
        final BigDecimal b = nm1.multiply(nm2).multiply(new BigDecimal(denom));
        return a.divide(b, MathContext.DECIMAL128).doubleValue();
    }
}
