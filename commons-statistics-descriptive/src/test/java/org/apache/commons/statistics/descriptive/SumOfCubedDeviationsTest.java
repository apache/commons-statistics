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
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;

/**
 * Test for {@link SumOfCubedDeviations}.
 *
 * <p>This class is used in the {@link Skewness} statistic. It is tested
 * separately to verify the limits of the algorithm on the
 * generic test data which include extreme finite values.
 * Unlike the {@link FirstMoment} and {@link SumOfSquaredDeviations}
 * the algorithm on an array is not high precision, and the
 * updating algorithm can suffer from large relative error when
 * the final statistic is close to zero.
 */
final class SumOfCubedDeviationsTest extends BaseDoubleStatisticTest<SumOfCubedDeviationsWrapper> {

    @Override
    protected String getStatisticName() {
        return "SumOfCubedDeviations";
    }

    @Override
    protected SumOfCubedDeviationsWrapper create() {
        return new SumOfCubedDeviationsWrapper(new SumOfCubedDeviations());
    }

    @Override
    protected SumOfCubedDeviationsWrapper create(double... values) {
        return new SumOfCubedDeviationsWrapper(SumOfCubedDeviations.of(values));
    }

    @Override
    protected SumOfCubedDeviationsWrapper create(double[] values, int from, int to) {
        // Add range checks here to pass the range validation tests
        Statistics.checkFromToIndex(from, to, values.length);
        return new SumOfCubedDeviationsWrapper(SumOfCubedDeviations.ofRange(values, from, to));
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedSC(values, null);
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // The non-finite behaviour is undefined.
        // This is handled using a special DoubleTolerance.
        return Double.NaN;
    }

    @Override
    protected DoubleTolerance getTolerance() {
        // The sum-of-cubed deviations is not very precise due to cancellation in the sum.
        // Both the accept and array tests observe failures at over 200 ulp.
        return TestHelper.equalsOrNonFinite(createAbsOrRelTolerance(0, 5e-14));
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // The value 2^1023 will overflow the sum of squared deviations
        builder.accept(addCase(0, 0, 0x1.0p1023));
        // The value 2^500 will overflow the sum of cubed deviations but not
        // the sum of squared deviations
        builder.accept(addCase(0, 0, 0x1.0p500));
        return builder.build();
    }

    /**
     * Compute expected sum-of-cubed deviations using BigDecimal.
     *
     * <p>The mean can be provided as an optimisation when testing {@link Skewness}.
     *
     * @param values Values.
     * @param mean Mean (can be null).
     * @return sum-of-cubed deviations
     */
    static double computeExpectedSC(double[] values, BigDecimal mean) {
        if (values.length <= 2) {
            // Note: When n==2 the difference from the mean is equal magnitude
            // and opposite sign. So the sum-of-cubed deviations is zero.
            return 0;
        }
        if (mean == null) {
            mean = TestHelper.computeExpectedMean(values);
        }
        BigDecimal bd = BigDecimal.ZERO;
        // Compute using double to get the IEEE result of any overflow
        final double xbar = mean.doubleValue();
        // Round mean to nearest double
        final BigDecimal mu = new BigDecimal(xbar);
        double sum = 0;
        for (final double value : values) {
            BigDecimal bdDiff = new BigDecimal(value);
            bdDiff = bdDiff.subtract(mu);
            bdDiff = bdDiff.pow(3);
            // Note: This sum has cancellation so we compute without rounding.
            bd = bd.add(bdDiff);
            sum += Math.pow(value - xbar, 3);
        }
        final double sc = bd.doubleValue();
        // Use the IEEE result for an overflow to +/- infinity
        // XXX: Possibly change this to return both the high-precision result
        // and the IEEE result, which contains information on whether there is
        // intermediate overflow in the evaluation. For example the
        // array [-x, 0, x] where x^3 overflows cannot be computed with either
        // the updating or array based algorithm.
        return Double.isFinite(sc) ? sc : sum;
    }
}
