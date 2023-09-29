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
import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
final class SumOfFourthDeviationsTest {
    private static final int ULP_ARRAY = 4;
    private static final int ULP_STREAM = 15;
    private static final int ULP_COMBINE_ACCEPT = 15;
    private static final int ULP_COMBINE_OF = 4;

    @Test
    void testEmpty() {
        Assertions.assertEquals(Double.NaN, SumOfFourthDeviations.of().getSumOfFourthDeviations());
    }

    @Test
    void testNaN() {
        SumOfFourthDeviations sq = new SumOfFourthDeviations();
        double[] testArray = {Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : testArray) {
            sq.accept(value);
        }
        Assertions.assertEquals(Double.NaN, sq.getSumOfFourthDeviations());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testSumOfFourthDeviations(double[] values) {
        final double expected = computeExpectedSQ(values);
        SumOfFourthDeviations sc = new SumOfFourthDeviations();
        for (double value : values) {
            sc.accept(value);
        }
        TestHelper.assertEqualsOrNonFinite(expected, sc.getSumOfFourthDeviations(), ULP_STREAM,
            () -> "sum-of-fourth deviations");
        TestHelper.assertEqualsOrNonFinite(expected, SumOfFourthDeviations.of(values).getSumOfFourthDeviations(), ULP_ARRAY,
            () -> "of (values)");

        SumOfFourthDeviations sc2 = SumOfFourthDeviations.of();
        for (double value : values) {
            sc2.accept(value);
        }
        Assertions.assertEquals(sc.getSumOfFourthDeviations(), sc2.getSumOfFourthDeviations(), "of() + values");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testParallelStream(double[] values) {
        final double expected = computeExpectedSQ(values);
        final double actual = Arrays.stream(values)
                .parallel()
                .collect(SumOfFourthDeviations::new, SumOfFourthDeviations::accept, SumOfFourthDeviations::combine)
                .getSumOfFourthDeviations();
        TestHelper.assertEqualsOrNonFinite(expected, actual, ULP_COMBINE_ACCEPT,
            () -> "sum-of-fourth deviations (in parallel)");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testSumOfFourthDeviationsRandomOrder(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testSumOfFourthDeviations(TestHelper.shuffle(rng, values));
            testParallelStream(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testSumOfFourthDeviationsNonFinite(double[] values) {
        final double expected = Double.NaN;
        SumOfFourthDeviations sq = new SumOfFourthDeviations();
        for (double value : values) {
            sq.accept(value);
        }
        Assertions.assertEquals(expected, sq.getSumOfFourthDeviations(), "sq non-finite");
        Assertions.assertEquals(expected, SumOfFourthDeviations.of(values).getSumOfFourthDeviations(), "of (values) non-finite");

        SumOfFourthDeviations sq2 = SumOfFourthDeviations.of();
        for (double value : values) {
            sq2.accept(value);
        }
        Assertions.assertEquals(sq.getSumOfFourthDeviations(), sq2.getSumOfFourthDeviations(), "of() + values non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testParallelStreamNonFinite(double[] values) {
        final double expected = Double.NaN;
        final double ans = Arrays.stream(values)
                .parallel()
                .collect(SumOfFourthDeviations::new, SumOfFourthDeviations::accept, SumOfFourthDeviations::combine)
                .getSumOfFourthDeviations();
        Assertions.assertEquals(expected, ans, "parallel stream non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testSumOfFourthDeviationsRandomOrderNonFinite(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testSumOfFourthDeviationsNonFinite(TestHelper.shuffle(rng, values));
            testParallelStreamNonFinite(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testCombine(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpectedSQ(combinedArray);
        SumOfFourthDeviations sq1 = new SumOfFourthDeviations();
        SumOfFourthDeviations sq2 = new SumOfFourthDeviations();
        Arrays.stream(array1).forEach(sq1);
        Arrays.stream(array2).forEach(sq2);
        final double sq1BeforeCombine = sq1.getSumOfFourthDeviations();
        final double sq2BeforeCombine = sq2.getSumOfFourthDeviations();
        sq1.combine(sq2);
        TestHelper.assertEqualsOrNonFinite(expected, sq1.getSumOfFourthDeviations(), ULP_COMBINE_ACCEPT, () -> "combine");
        Assertions.assertEquals(sq2BeforeCombine, sq2.getSumOfFourthDeviations());
        // Combine in reverse order
        SumOfFourthDeviations sq1b = new SumOfFourthDeviations();
        Arrays.stream(array1).forEach(sq1b);
        sq2.combine(sq1b);
        Assertions.assertEquals(sq1.getSumOfFourthDeviations(), sq2.getSumOfFourthDeviations(), () -> "combine reversed");
        Assertions.assertEquals(sq1BeforeCombine, sq1b.getSumOfFourthDeviations());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testCombineRandomOrder(double[] array1, double[] array2) {
        UniformRandomProvider rng = TestHelper.createRNG();
        double[] data = TestHelper.concatenate(array1, array2);
        final int n = array1.length;
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                TestHelper.shuffle(rng, array1);
                TestHelper.shuffle(rng, array2);
                testCombine(array1, array2);
            }
            TestHelper.shuffle(rng, data);
            System.arraycopy(data, 0, array1, 0, n);
            System.arraycopy(data, n, array2, 0, array2.length);
            testCombine(array1, array2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testArrayOfArrays(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpectedSQ(combinedArray);
        final double[][] values = {array1, array2};
        final double actual = Arrays.stream(values)
                .map(SumOfFourthDeviations::of)
                .reduce(SumOfFourthDeviations::combine)
                .map(SumOfFourthDeviations::getSumOfFourthDeviations)
                .orElseThrow(RuntimeException::new);
        TestHelper.assertEqualsOrNonFinite(expected, actual, ULP_COMBINE_OF, () -> "array of arrays combined");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testCombineNonFinite(double[][] values) {
        final double expected = Double.NaN;
        SumOfFourthDeviations sq1 = new SumOfFourthDeviations();
        SumOfFourthDeviations sq2 = new SumOfFourthDeviations();
        Arrays.stream(values[0]).forEach(sq1);
        Arrays.stream(values[1]).forEach(sq2);
        final double mean2BeforeCombine = sq2.getSumOfFourthDeviations();
        sq1.combine(sq2);
        Assertions.assertEquals(expected, sq1.getSumOfFourthDeviations(), "combine non-finite");
        Assertions.assertEquals(mean2BeforeCombine, sq2.getSumOfFourthDeviations());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testCombineRandomOrderNonFinite(double[][] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        final double[] data = TestHelper.concatenate(values[0], values[1]);
        final int n = values[0].length;
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                TestHelper.shuffle(rng, values[0]);
                TestHelper.shuffle(rng, values[1]);
                testCombineNonFinite(values);
            }
            TestHelper.shuffle(rng, data);
            System.arraycopy(data, 0, values[0], 0, n);
            System.arraycopy(data, n, values[1], 0, values[1].length);
            testCombineNonFinite(values);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testArrayOfArraysNonFinite(double[][] values) {
        final double expected = Double.NaN;
        final double actual = Arrays.stream(values)
                .map(SumOfFourthDeviations::of)
                .reduce(SumOfFourthDeviations::combine)
                .map(SumOfFourthDeviations::getSumOfFourthDeviations)
                .orElseThrow(RuntimeException::new);
        Assertions.assertEquals(expected, actual, "array of arrays combined non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testMultiCombine")
    void testMultiCombine(double[][] values) {
        final double[] combinedArray = TestHelper.concatenate(values);
        final double expected = computeExpectedSQ(combinedArray);
        final double actual = Arrays.stream(values)
                .map(SumOfFourthDeviations::of)
                .reduce(SumOfFourthDeviations::combine)
                .map(SumOfFourthDeviations::getSumOfFourthDeviations)
                .orElseThrow(RuntimeException::new);
        // Level to pass all test data
        final int ulp = 4;
        TestHelper.assertEqualsOrNonFinite(expected, actual, ulp, () -> "multi combine");
    }

    /**
     * Compute expected sum-of-fourth (quadruple) deviations.
     *
     * @param values Values.
     * @return sum-of-fourth deviations
     */
    private static double computeExpectedSQ(double[] values) {
        if (values.length <= 1) {
            return 0;
        }
        return computeExpectedSumOfFourthDeviations(values).doubleValue();
    }

    /**
     * Compute expected sum-of-fourth (quadruple) deviations using BigDecimal.
     *
     * @param values Values.
     * @return sum-of-fourth deviations
     */
    private static BigDecimal computeExpectedSumOfFourthDeviations(double[] values) {
        return computeExpectedSumOfFourthDeviations(values, null);
    }

    /**
     * Compute expected sum-of-fourth (quadruple) deviations using BigDecimal.
     *
     * <p>The mean can be provided as an optimisation when testing {@link Kurtosis}.
     *
     * @param values Values.
     * @return sum-of-fourth deviations
     */
    static BigDecimal computeExpectedSumOfFourthDeviations(double[] values, BigDecimal mean) {
        if (values.length <= 1) {
            return BigDecimal.ZERO;
        }
        if (mean == null) {
            mean = TestHelper.computeExpectedMean(values);
        }
        BigDecimal bd = BigDecimal.ZERO;
        for (double value : values) {
            BigDecimal bdDiff = new BigDecimal(value, MathContext.DECIMAL128);
            bdDiff = bdDiff.subtract(mean);
            bdDiff = bdDiff.pow(4);
            // Note: This is a sum of positive terms so summation with rounding is OK.
            bd = bd.add(bdDiff, MathContext.DECIMAL128);
        }
        return bd;
    }
}
