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
 * Test for {@link SumOfFourthDeviations}.
 *
 * <p>This class is used in the {@link Kurtosis} statistic. It is tested
 * separately to verify the limits of the algorithm on the
 * generic test data which include extreme finite values.
 * Unlike the {@link FirstMoment} and {@link SumOfSquaredDeviations}
 * the algorithm on an array is not high precision.
 *
 * <p>Note that the output value is always positive and so does not
 * suffer as much from cancellation effects observed in the sum of cubed
 * deviations. However the sum of cubed deviation is used during the
 * updating and combine methods so errors can propagate. The test
 * tolerances are higher than the first two moments, and lower than
 * the sum of cubed deviations.
 */
final class SumOfFourthDeviationsTest extends BaseDoubleStatisticTest<SumOfFourthDeviationsWrapper> {

    @Override
    protected String getStatisticName() {
        return "SumOfFourthDeviations";
    }

    @Override
    protected SumOfFourthDeviationsWrapper create() {
        return new SumOfFourthDeviationsWrapper(new SumOfFourthDeviations());
    }

    @Override
    protected SumOfFourthDeviationsWrapper create(double... values) {
        return new SumOfFourthDeviationsWrapper(SumOfFourthDeviations.of(values));
    }

    @Override
    protected double getEmptyValue() {
        return Double.NaN;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        return computeExpectedSumOfFourthDeviations(values, null).doubleValue();
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        // The non-finite behaviour is undefined.
        // This is handled using a special DoubleTolerance.
        return Double.NaN;
    }

    // Failures are observed across all methods at a similar precision

    @Override
    protected DoubleTolerance getToleranceAccept() {
        return TestHelper.equalsOrNonFinite(DoubleTolerances.ulps(20));
    }

    @Override
    protected DoubleTolerance getToleranceArray() {
        return TestHelper.equalsOrNonFinite(DoubleTolerances.ulps(15));
    }

    @Override
    protected DoubleTolerance getToleranceAcceptAndCombine() {
        return TestHelper.equalsOrNonFinite(DoubleTolerances.ulps(25));
    }

    @Override
    protected DoubleTolerance getToleranceArrayAndCombine() {
        return TestHelper.equalsOrNonFinite(DoubleTolerances.ulps(20));
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        TestData.momentTestData().forEach(x -> builder.accept(addCase(x)));
        // The value 2^1023 will overflow the sum of squared deviations
        builder.accept(addCase(0, 0, 0, 0x1.0p1023));
        // The value 2^500 will overflow the sum of cubed deviations but not
        // the sum of squared deviations
        builder.accept(addCase(0, 0, 0, 0x1.0p500));
        // The value 2^300 will overflow the sum of fourth deviations
        // but not the sum of cubed deviations
        builder.accept(addCase(0, 0, 0, 0x1.0p300));
        return builder.build();
    }

    /**
     * Compute expected sum-of-fourth (quadruple) deviations using BigDecimal.
     *
     * <p>The mean can be provided as an optimisation when testing {@link Kurtosis}.
     *
     * @param values Values.
     * @param mean Mean (can be null).
     * @return sum-of-fourth deviations
     */
    static BigDecimal computeExpectedSumOfFourthDeviations(double[] values, BigDecimal mean) {
        if (values.length <= 1) {
            return BigDecimal.ZERO;
        }
        if (mean == null) {
            mean = TestHelper.computeExpectedMean(values);
        }
        // Round mean to nearest double
        final BigDecimal mu = new BigDecimal(mean.doubleValue());
        BigDecimal bd = BigDecimal.ZERO;
        for (double value : values) {
            BigDecimal bdDiff = new BigDecimal(value);
            bdDiff = bdDiff.subtract(mu);
            bdDiff = bdDiff.pow(4);
            // Note: This is a sum of positive terms so summation with rounding is OK.
            bd = bd.add(bdDiff, MathContext.DECIMAL128);
        }
        return bd;
    }
}
