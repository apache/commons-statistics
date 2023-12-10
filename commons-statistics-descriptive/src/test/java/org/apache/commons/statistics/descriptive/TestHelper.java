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
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.DoubleTolerance;

/**
 * Helper class for tests in {@code o.a.c.s.descriptive} module.
 */
final class TestHelper {
    /**
     * Cached seed. Using the same seed ensures all statistics use the same shuffled
     * data for tests executed in the same JVM.
     * Native seed for XO_RO_SHI_RO_128_PP is long[] (size 2).
     */
    private static final long[] SEED = RandomSource.createLongArray(2);

    /**
     * A {@link DoubleTolerance} that considers finite values equal using the {@code ==} operator,
     * and all non-finite values equal.
     */
    private static final class EqualsNonFinite implements DoubleTolerance, Supplier<String> {
        /** An instance. */
        static final EqualsNonFinite INSTANCE = new EqualsNonFinite();

        @Override
        public boolean test(double a, double b) {
            return Double.isFinite(a) ?
                a == b :
                !Double.isFinite(b);
        }

        @Override
        public String get() {
            // Note: This method provides an assertion message for the tolerance
            return "(== or non-finite match)";
        }
    }

    /** Class contains only static methods. */
    private TestHelper() {}

    /**
     * Helper function to concatenate arrays.
     *
     * @param arrays Arrays to be concatenated.
     * @return A new array containing elements from all input arrays in the order they appear.
     */
    static double[] concatenate(double[]... arrays) {
        return Arrays.stream(arrays)
                .flatMapToDouble(Arrays::stream)
                .toArray();
    }

    /**
     * Helper function to reverse the concatenation of arrays. The array is copied
     * sequentially into the destination arrays.
     *
     * <p>Warning: No lengths checks are performed. It is assume the combine length
     * of the destination arrays is equal to the length of the data.
     *
     * @param data Array to be unconcatenated.
     * @param arrays Destination arrays.
     */
    static void unconcatenate(double[] data, double[]... arrays) {
        int from = 0;
        for (final double[] a : arrays) {
            System.arraycopy(data, from, a, 0, a.length);
            from += a.length;
        }
    }

    /**
     * Helper function to compute the expected value of Mean using BigDecimal.
     * @param values Values.
     * @return Mean of values rounded to <a href = "https://en.wikipedia.org/wiki/Decimal128_floating-point_format"> DECIMAL128 precision</a>.
     */
    static BigDecimal computeExpectedMean(double[] values) {
        BigDecimal bd = BigDecimal.ZERO;
        for (double value : values) {
            bd = bd.add(new BigDecimal(value));
        }
        return bd.divide(BigDecimal.valueOf(values.length), MathContext.DECIMAL128);
    }

    /**
     * Compute expected sum-of-square deviations using BigDecimal.
     * Uses the mean from {@link #computeExpectedMean(double[])}.
     * This is optionally returned.
     *
     * <p>Note: The values and result are limited to {@link MathContext#DECIMAL128} precision.
     *
     * @param values Values.
     * @param mean Mean (result). Only computed if {@code length > 1}.
     * @return sum-of-square deviations
     */
    static BigDecimal computeExpectedSumOfSquaredDeviations(double[] values, BigDecimal[] mean) {
        long n = values.length;
        if (n <= 1) {
            return BigDecimal.ZERO;
        }
        final BigDecimal m = computeExpectedMean(values);
        if (mean != null) {
            mean[0] = m;
        }
        BigDecimal ss = BigDecimal.ZERO;
        for (double value : values) {
            // Note: The mean is returned in DECIMAL128 precision.
            // Truncate the double value to the same precision.
            // This avoids round-off issues with extreme values such as Double.MAX_VALUE
            // that have a very large BigInteger representation,
            // i.e. the entire computation must use the same precision.
            BigDecimal bdDiff = new BigDecimal(value, MathContext.DECIMAL128);
            bdDiff = bdDiff.subtract(m);
            bdDiff = bdDiff.pow(2);
            // Note: This is a sum of positive terms so summation with rounding is OK.
            ss = ss.add(bdDiff, MathContext.DECIMAL128);
        }
        return ss;
    }

    /**
     * Create a tolerance using the provided tolerance for finite values; all non-finite
     * values are considered equal.
     *
     * @param tol Finite value tolerance.
     * @return the tolerance
     */
    static DoubleTolerance equalsOrNonFinite(DoubleTolerance tol) {
        return tol.or(EqualsNonFinite.INSTANCE);
    }

    /**
     * Creates a seed for the RNG.
     *
     * @return the seed
     * @see #createRNG(long[])
     */
    static long[] createRNGSeed() {
        // This could be generated dynamically.
        // However using the same seed across all tests ensures:
        // 1. Related statistics use the same shuffled data, e.g. Mean and Variance.
        // 2. Different tests of the same statistic use the same shuffle data.
        // This allows settings test tolerances appropriately across the test suite.
        return SEED.clone();
    }

    /**
     * Creates a RNG instance.
     *
     * @param seed Seed.
     * @return A new RNG instance.
     * @see #createRNGSeed()
     */
    static UniformRandomProvider createRNG(long[] seed) {
        return RandomSource.XO_RO_SHI_RO_128_PP.create(seed);
    }

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Uses Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * <p>TODO: This can be removed when {@code commons-rng-sampling 1.6} is released.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static double[] shuffle(UniformRandomProvider rng, double[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Uses Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * <p>TODO: This can be removed when {@code commons-rng-sampling 1.6} is released.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static <T> T[] shuffle(UniformRandomProvider rng, T[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static <T> void swap(T[] array, int i, int j) {
        final T tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
}
