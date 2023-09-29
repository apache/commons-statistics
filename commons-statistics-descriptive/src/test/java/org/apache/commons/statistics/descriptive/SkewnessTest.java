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
 * Test for {@link Skewness}.
 */
final class SkewnessTest {
    private static final int ULP_ARRAY = 35;
    private static final int ULP_STREAM = 60;
    private static final int ULP_COMBINE_ACCEPT = 100;
    private static final int ULP_COMBINE_OF = 35;

    @Test
    void testEmpty() {
        final Skewness skew = Skewness.create();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(Double.NaN, skew.getAsDouble());
            skew.accept(i);
        }
        Assertions.assertNotEquals(Double.NaN, skew.getAsDouble());
    }

    @Test
    void testNaN() {
        Skewness skewness = Skewness.create();
        double[] testArray = {Double.NaN, +0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : testArray) {
            skewness.accept(value);
        }
        Assertions.assertEquals(Double.NaN, skewness.getAsDouble());
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testSkewness(double[] values) {
        final double expected = computeExpectedSkewness(values);
        Skewness skew = Skewness.create();
        for (double value : values) {
            skew.accept(value);
        }
        assertStreaming(values.length, expected, skew.getAsDouble(), ULP_STREAM, () -> "skewness");
        TestHelper.assertEquals(expected, Skewness.of(values).getAsDouble(), ULP_ARRAY, () -> "of (values)");

        Skewness skew2 = Skewness.of();
        for (double value : values) {
            skew2.accept(value);
        }
        Assertions.assertEquals(skew.getAsDouble(), skew2.getAsDouble(), "of() + values");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testParallelStream(double[] values) {
        final double expected = computeExpectedSkewness(values);
        final double actual = Arrays.stream(values)
                .parallel()
                .collect(Skewness::create, Skewness::accept, Skewness::combine)
                .getAsDouble();
        assertStreaming(values.length, expected, actual, ULP_COMBINE_ACCEPT, () -> "parallel stream");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValues")
    void testSkewnessRandomOrder(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testSkewness(TestHelper.shuffle(rng, values));
            testParallelStream(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testSkewnessNonFinite(double[] values) {
        final double expected = Double.NaN;
        Skewness skew = Skewness.create();
        for (double value : values) {
            skew.accept(value);
        }
        Assertions.assertEquals(expected, skew.getAsDouble(), "skewness non-finite");
        Assertions.assertEquals(expected, Skewness.of(values).getAsDouble(), "of (values) non-finite");

        Skewness skew2 = Skewness.of();
        for (double value : values) {
            skew2.accept(value);
        }
        Assertions.assertEquals(skew.getAsDouble(), skew2.getAsDouble(), "of() + values non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testParallelStreamNonFinite(double[] values) {
        final double expected = Double.NaN;
        final double ans = Arrays.stream(values)
                .parallel()
                .collect(Skewness::create, Skewness::accept, Skewness::combine)
                .getAsDouble();
        Assertions.assertEquals(expected, ans, "parallel stream non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testValuesNonFinite")
    void testSkewnessRandomOrderNonFinite(double[] values) {
        UniformRandomProvider rng = TestHelper.createRNG();
        for (int i = 1; i <= 10; i++) {
            testSkewnessNonFinite(TestHelper.shuffle(rng, values));
            testParallelStreamNonFinite(TestHelper.shuffle(rng, values));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombine")
    void testCombine(double[] array1, double[] array2) {
        final double[] combinedArray = TestHelper.concatenate(array1, array2);
        final double expected = computeExpectedSkewness(combinedArray);
        Skewness skew1 = Skewness.create();
        Skewness skew2 = Skewness.create();
        Arrays.stream(array1).forEach(skew1);
        Arrays.stream(array2).forEach(skew2);
        final double skew1BeforeCombine = skew1.getAsDouble();
        final double skew2BeforeCombine = skew2.getAsDouble();
        skew1.combine(skew2);
        TestHelper.assertEquals(expected, skew1.getAsDouble(), ULP_COMBINE_ACCEPT, () -> "combine");
        Assertions.assertEquals(skew2BeforeCombine, skew2.getAsDouble());
        // Combine in reverse order
        Skewness skew1b = Skewness.create();
        Arrays.stream(array1).forEach(skew1b);
        skew2.combine(skew1b);
        Assertions.assertEquals(skew1.getAsDouble(), skew2.getAsDouble(), () -> "combine reversed");
        Assertions.assertEquals(skew1BeforeCombine, skew1b.getAsDouble());
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
        final double expected = computeExpectedSkewness(combinedArray);
        final double[][] values = {array1, array2};
        final double actual = Arrays.stream(values)
                .map(Skewness::of)
                .reduce(Skewness::combine)
                .map(Skewness::getAsDouble)
                .orElseThrow(RuntimeException::new);
        TestHelper.assertEquals(expected, actual, ULP_COMBINE_OF, () -> "array of arrays combined skewness");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testCombineNonFinite")
    void testCombineNonFinite(double[][] values) {
        final double expected = Double.NaN;
        Skewness skew1 = Skewness.create();
        Skewness skew2 = Skewness.create();
        Arrays.stream(values[0]).forEach(skew1);
        Arrays.stream(values[1]).forEach(skew2);
        final double mean2BeforeCombine = skew2.getAsDouble();
        skew1.combine(skew2);
        Assertions.assertEquals(expected, skew1.getAsDouble(), "combine non-finite");
        Assertions.assertEquals(mean2BeforeCombine, skew2.getAsDouble());
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
                .map(Skewness::of)
                .reduce(Skewness::combine)
                .map(Skewness::getAsDouble)
                .orElseThrow(RuntimeException::new);
        Assertions.assertEquals(expected, actual, "array of arrays combined skewness non-finite");
    }

    @ParameterizedTest
    @MethodSource(value = "org.apache.commons.statistics.descriptive.TestData#testMultiCombine")
    void testMultiCombine(double[][] values) {
        final double[] combinedArray = TestHelper.concatenate(values);
        final double expected = computeExpectedSkewness(combinedArray);
        final double actual = Arrays.stream(values)
                .map(Skewness::of)
                .reduce(Skewness::combine)
                .map(Skewness::getAsDouble)
                .orElseThrow(RuntimeException::new);
        // Level to pass all test data
        final int ulp = 79;
        TestHelper.assertEquals(expected, actual, ulp, () -> "multi combine");
    }

    @Test
    void testNonFiniteSumOfCubedDeviations() {
        // The value 2^500 will overflow the sum of cubed deviations but not
        // the sum of squared deviations
        final Skewness skew = Skewness.of(0, 0, 0x1.0p500);
        Assertions.assertEquals(Double.NaN, skew.getAsDouble());
    }

    /**
     * Compute expected skewness.
     *
     * @param values Values.
     * @return skewness
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
            TestHelper.assertEquals(expected, actual, ULP_COMBINE_ACCEPT, msg);
        }
    }
}
