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
import java.util.function.Supplier;
import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
final class SumOfCubedDeviationsTest {
    private static final int ULP_ARRAY = 20;
    private static final int ULP_STREAM = 60;
    private static final int ULP_COMBINE_ACCEPT = 110;
    private static final int ULP_COMBINE_OF = 20;

    @Test
    void testEmpty() {
        Assertions.assertEquals(Double.NaN, SumOfCubedDeviations.of().getSumOfCubedDeviations());
    }

    @Test
    void testNaN() {
        SumOfCubedDeviations sc = new SumOfCubedDeviations();
        double[] testArray = {Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : testArray) {
            sc.accept(value);
        }
        Assertions.assertEquals(Double.NaN, sc.getSumOfCubedDeviations());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testSumOfCubedDeviations(double[] values) {
        final double expected = computeExpectedSC(values);
        SumOfCubedDeviations sc = new SumOfCubedDeviations();
        for (double value : values) {
            sc.accept(value);
        }
        assertStreaming(values.length, expected, sc.getSumOfCubedDeviations(), ULP_STREAM,
            () -> "sum-of-cubed deviations");
        TestHelper.assertEqualsOrNonFinite(expected, SumOfCubedDeviations.of(values).getSumOfCubedDeviations(), ULP_ARRAY,
            () -> "of (values)");

        SumOfCubedDeviations sc2 = SumOfCubedDeviations.of();
        for (double value : values) {
            sc2.accept(value);
        }
        Assertions.assertEquals(sc.getSumOfCubedDeviations(), sc2.getSumOfCubedDeviations(), "of() + values");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testParallelStream(double[] values) {
        final double expected = computeExpectedSC(values);
        final double actual = Arrays.stream(values)
                .parallel()
                .collect(SumOfCubedDeviations::new, SumOfCubedDeviations::accept, SumOfCubedDeviations::combine)
                .getSumOfCubedDeviations();
        assertStreaming(values.length, expected, actual, ULP_COMBINE_ACCEPT,
            () -> "sum-of-cubed deviations (in parallel)");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testSumOfCubedDeviationsRandomOrder(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testSumOfCubedDeviations(TestHelper.shuffle(rng, values));
            testParallelStream(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testSumOfCubedDeviationsNonFinite(double[] values) {
        final double expected = Double.NaN;
        SumOfCubedDeviations sc = new SumOfCubedDeviations();
        for (double value : values) {
            sc.accept(value);
        }
        Assertions.assertEquals(expected, sc.getSumOfCubedDeviations(), "sc non-finite");
        Assertions.assertEquals(expected, SumOfCubedDeviations.of(values).getSumOfCubedDeviations(), "of (values) non-finite");

        SumOfCubedDeviations sc2 = SumOfCubedDeviations.of();
        for (double value : values) {
            sc2.accept(value);
        }
        Assertions.assertEquals(sc.getSumOfCubedDeviations(), sc2.getSumOfCubedDeviations(), "of() + values non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testParallelStreamNonFinite(double[] values) {
        final double expected = Double.NaN;
        final double ans = Arrays.stream(values)
                .parallel()
                .collect(SumOfCubedDeviations::new, SumOfCubedDeviations::accept, SumOfCubedDeviations::combine)
                .getSumOfCubedDeviations();
        Assertions.assertEquals(expected, ans, "parallel stream non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testSumOfCubedDeviationsRandomOrderNonFinite(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testSumOfCubedDeviationsNonFinite(TestHelper.shuffle(rng, values));
            testParallelStreamNonFinite(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testCombine(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpectedSC(combinedArray);
        SumOfCubedDeviations sc1 = new SumOfCubedDeviations();
        SumOfCubedDeviations sc2 = new SumOfCubedDeviations();
        Arrays.stream(array1).forEach(sc1);
        Arrays.stream(array2).forEach(sc2);
        final double sc1BeforeCombine = sc1.getSumOfCubedDeviations();
        final double sc2BeforeCombine = sc2.getSumOfCubedDeviations();
        sc1.combine(sc2);
        TestHelper.assertEqualsOrNonFinite(expected, sc1.getSumOfCubedDeviations(), ULP_COMBINE_ACCEPT, () -> "combine");
        Assertions.assertEquals(sc2BeforeCombine, sc2.getSumOfCubedDeviations());
        // Combine in reverse order
        SumOfCubedDeviations sc1b = new SumOfCubedDeviations();
        Arrays.stream(array1).forEach(sc1b);
        sc2.combine(sc1b);
        Assertions.assertEquals(sc1.getSumOfCubedDeviations(), sc2.getSumOfCubedDeviations(), () -> "combine reversed");
        Assertions.assertEquals(sc1BeforeCombine, sc1b.getSumOfCubedDeviations());
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
        final double expected = computeExpectedSC(combinedArray);
        final double[][] values = {array1, array2};
        final double actual = Arrays.stream(values)
                .map(SumOfCubedDeviations::of)
                .reduce(SumOfCubedDeviations::combine)
                .map(SumOfCubedDeviations::getSumOfCubedDeviations)
                .orElseThrow(RuntimeException::new);
        TestHelper.assertEqualsOrNonFinite(expected, actual, ULP_COMBINE_OF, () -> "array of arrays combined");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testCombineNonFinite(double[][] values) {
        final double expected = Double.NaN;
        SumOfCubedDeviations sc1 = new SumOfCubedDeviations();
        SumOfCubedDeviations sc2 = new SumOfCubedDeviations();
        Arrays.stream(values[0]).forEach(sc1);
        Arrays.stream(values[1]).forEach(sc2);
        final double mean2BeforeCombine = sc2.getSumOfCubedDeviations();
        sc1.combine(sc2);
        Assertions.assertEquals(expected, sc1.getSumOfCubedDeviations(), "combine non-finite");
        Assertions.assertEquals(mean2BeforeCombine, sc2.getSumOfCubedDeviations());
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
                .map(SumOfCubedDeviations::of)
                .reduce(SumOfCubedDeviations::combine)
                .map(SumOfCubedDeviations::getSumOfCubedDeviations)
                .orElseThrow(RuntimeException::new);
        Assertions.assertEquals(expected, actual, "array of arrays combined non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testMultiCombine")
    void testMultiCombine(double[][] values) {
        final double[] combinedArray = TestHelper.concatenate(values);
        final double expected = computeExpectedSC(combinedArray);
        final double actual = Arrays.stream(values)
                .map(SumOfCubedDeviations::of)
                .reduce(SumOfCubedDeviations::combine)
                .map(SumOfCubedDeviations::getSumOfCubedDeviations)
                .orElseThrow(RuntimeException::new);
        // Level to pass all test data
        final int ulp = 79;
        TestHelper.assertEqualsOrNonFinite(expected, actual, ulp, () -> "multi combine");
    }

    /**
     * Compute expected sum-of-cubed deviations using BigDecimal.
     *
     * @param values Values.
     * @return sum-of-cubed deviations
     */
    private static double computeExpectedSC(double[] values) {
        return computeExpectedSC(values, null);
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
        double sum = 0;
        for (double value : values) {
            BigDecimal bdDiff = new BigDecimal(value, MathContext.DECIMAL128);
            bdDiff = bdDiff.subtract(mean);
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

    /**
     * Assert the actual result from the streaming algorithm.
     *
     * <p>Note: Close to zero the streaming method is very dependent on the input order
     * and can be very far from the expected result in ULPs.
     * This simply checks the value is small.
     *
     * @param size Size of the data.
     * @param expected Expected value.
     * @param actual Actual value.
     * @param ulp ULP tolerance.
     * @param msg Failure message.
     */
    private static void assertStreaming(int size, double expected, double actual, int ulp,
        Supplier<String> msg) {
        if (size > 2 && Math.abs(expected) < 1e-10) {
            // Close to zero the streaming method is very dependent on the input order.
            // This simply checks the value is small.
            Assertions.assertEquals(expected, actual, 4e-13, msg);
        } else {
            // Non-finite values are not maintained by the implementation
            TestHelper.assertEqualsOrNonFinite(expected, actual, ULP_COMBINE_ACCEPT,
                msg);
        }
    }
}
