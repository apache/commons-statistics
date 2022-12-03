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
package org.apache.commons.statistics.inference;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;
import org.apache.commons.numbers.core.Precision;

/**
 * Utility computation methods.
 *
 * @since 1.1
 */
final class StatisticUtils {
    /** No instances. */
    private StatisticUtils() {}

    /**
     * Compute {@code x - y}.
     *
     * <p>If {@code y} is zero the original array is returned, else a new array is created
     * with the difference.
     *
     * @param x Array.
     * @param y Value.
     * @return x - y
     * @throws NullPointerException if {@code x} is null and {@code y} is non-zero
     */
    static double[] subtract(double[] x, double y) {
        return y == 0 ? x : Arrays.stream(x).map(v -> v - y).toArray();
    }

    /**
     * Compute the degrees of freedom as {@code n - 1 - m}.
     *
     * <p>This method is common functionality shared between the Chi-square test and
     * G-test. The pre-conditions for those tests are performed by this method.
     *
     * @param n Number of observations.
     * @param m Adjustment (assumed to be positive).
     * @return the degrees of freedom
     * @throws IllegalArgumentException if the degrees of freedom is not strictly positive
     */
    static int computeDegreesOfFreedom(int n, int m) {
        final int df = n - 1 - m;
        if (df <= 0) {
            throw new InferenceException("Invalid degrees of freedom: " + df);
        }
        return df;
    }

    /**
     * Gets the ratio between the sum of the observed and expected values.
     * The ratio can be used to scale the expected values to have the same sum
     * as the observed values:
     *
     * <pre>
     * sum(o) = sum(e * ratio)
     * </pre>
     *
     * <p>This method is common functionality shared between the Chi-square test and
     * G-test. The pre-conditions for those tests are performed by this method.
     *
     * @param expected Expected values.
     * @param observed Observed values.
     * @return the ratio
     * @throws IllegalArgumentException if the sample size is less than 2; the array
     * sizes do not match; {@code expected} has entries that are not strictly
     * positive; {@code observed} has negative entries; or all the the observations are zero.
     */
    static double computeRatio(double[] expected, long[] observed) {
        Arguments.checkValuesRequiredSize(expected.length, 2);
        Arguments.checkValuesSizeMatch(expected.length, observed.length);
        Arguments.checkStrictlyPositive(expected);
        Arguments.checkNonNegative(observed);
        final DD e = DD.create();
        final DD o = DD.create();
        for (int i = 0; i < observed.length; i++) {
            DD.fastAdd(e.hi(), e.lo(), expected[i], e);
            add(o, observed[i]);
        }
        if (o.doubleValue() == 0) {
            throw new InferenceException(InferenceException.NO_DATA);
        }
        // sum(o) / sum(e)
        final double ratio = DD.divide(o.hi(), o.lo(), e.hi(), e.lo(), e).doubleValue();
        // Allow a sum within 1 ulp of 1.0
        return Precision.equals(ratio, 1.0, 0) ? 1.0 : ratio;
    }

    /**
     * Adds the value to the sum.
     *
     * @param sum Sum.
     * @param v Value.
     */
    private static void add(DD sum, long v) {
        // Split into hi and lo parts so the high part has 53-bits
        final double hi = v;
        final long lo = v - (long) hi;
        // The condition here is a high probability branch if the sample is
        // frequency counts which are typically in the 32-bit integer range.
        if (lo == 0) {
            DD.fastAdd(sum.hi(), sum.lo(), hi, sum);
        } else {
            DD.fastAdd(sum.hi(), sum.lo(), hi, lo, sum);
        }
    }

    /**
     * Returns the arithmetic mean of the entries in the input array,
     * or {@code NaN} if the array is empty.
     *
     * @param x Values.
     * @return the mean of the values or NaN if length = 0
     */
    static double mean(long[] x) {
        final int n = x.length;
        if (n == 0) {
            return Double.NaN;
        }

        // Single pass high accuracy sum. The total cannot be more than 2^63 * 2^31 bits
        // so can be exactly represented in a double-double. Cumulative error in the sum
        // is (n-1) * 4eps with eps = 2^-106. The sum should be exact to double precision.
        final DD dd = DD.create();
        for (final long v : x) {
            add(dd, v);
        }

        return DD.divide(dd.hi(), dd.lo(), n, 0, dd).doubleValue();
    }

    /**
     * Returns the arithmetic mean of the entries in the input array,
     * or {@code NaN} if the array is empty.
     *
     * <p>A two-pass, corrected algorithm is used, starting with the definitional formula
     * computed using the array of stored values and then correcting this by adding the
     * mean deviation of the data values from the arithmetic mean. See, e.g. "Comparison
     * of Several Algorithms for Computing Sample Means and Variances," Robert F. Ling,
     * Journal of the American Statistical Association, Vol. 69, No. 348 (Dec., 1974), pp.
     * 859-866.
     *
     * @param x Values.
     * @return the mean of the values or NaN if length = 0
     */
    static double mean(double[] x) {
        final int n = x.length;
        // No check for n == 0 -> return NaN.
        // This internal method is only called with non-zero length arrays.
        // The divide by zero creates NaN anyway.

        // Adapted from org.apache.commons.math4.legacy.stat.descriptive.moment.Mean
        // Updated to use a stream to support high-precision summation as the stream maintains
        // a rounding-error term during the aggregation. This is important
        // when summing differences which can create cancellation: x + -x => 0.

        // Compute initial estimate using definitional formula
        final double mean = Arrays.stream(x).sum() / n;

        // Compute correction factor in second pass
        return mean + Arrays.stream(x).map(v -> v - mean).sum() / n;
    }

    /**
     * Returns the arithmetic mean of the entries in the input arrays,
     * or {@code NaN} if the combined length of the arrays is zero.
     *
     * <p>This is the equivalent of using {@link #mean(double[])} with all the samples
     * concatenated into a single array. Supports a combined length above the maximum array
     * size.
     *
     * @param samples Values.
     * @return the mean of the values or NaN if length = 0
     * @see #mean(double[])
     */
    static double mean(Collection<double[]> samples) {
        // See above for computation details
        final long n = samples.stream().mapToInt(x -> x.length).sum();
        final double mean = samples.stream().flatMapToDouble(Arrays::stream).sum() / n;
        return mean + samples.stream().flatMapToDouble(Arrays::stream).map(v -> v - mean).sum() / n;
    }

    /**
     * Returns the variance of the entries in the input array, or {@code NaN} if the array
     * is empty.
     *
     * <p>This method returns the bias-corrected sample variance (using {@code n - 1} in
     * the denominator).
     *
     * <p>Uses a two-pass algorithm. Specifically, these methods use the "corrected
     * two-pass algorithm" from Chan, Golub, Levesque, <i>Algorithms for Computing the
     * Sample Variance</i>, American Statistician, vol. 37, no. 3 (1983) pp.
     * 242-247.
     *
     * <p>Returns 0 for a single-value (i.e. length = 1) sample.
     *
     * @param x Values.
     * @param mean the mean of the input array
     * @return the variance of the values or NaN if the array is empty
     */
    static double variance(double[] x, double mean) {
        final int n = x.length;
        // No check for n == 0 -> return NaN.
        // This internal method is only called with non-zero length arrays.
        // The input mean of NaN for zero length creates NaN anyway.
        if (n == 1) {
            return 0;
        }

        // Adapted from org.apache.commons.math4.legacy.stat.descriptive.moment.Variance
        // Use a stream to accumulate the sum of deviations in high precision.
        // This compensation term for the sum of deviations from the mean -> 0.
        // We sum the squares in standard precision as there is no cancellation of summands.
        final double[] sumSq = {0};
        final double sum2 = Arrays.stream(x).map(v -> {
            final double dx = v - mean;
            sumSq[0] += dx * dx;
            return dx;
        }).sum();

        final double sum1 = sumSq[0];
        // Bias corrected
        // Note: variance ~ sum1 / (n-1) but with a correction term sum2
        return (sum1 - (sum2 * sum2 / n)) / (n - 1);
    }

    /**
     * Returns the mean of the (signed) differences between corresponding elements of the
     * input arrays.
     *
     * <pre>
     * sum(x[i] - y[i]) / x.length
     * </pre>
     *
     * <p>This computes the same result as creating an array {@code z = x - y}
     * and calling {@link #mean(double[]) mean(z)}, but without the intermediate array
     * allocation.
     *
     * @param x First array.
     * @param y Second array.
     * @return mean of paired differences
     * @throws IllegalArgumentException if the arrays do not have the same length.
     * @see #mean(double[])
     */
    static double meanDifference(double[] x, double[] y) {
        final int n = x.length;
        if (n != y.length) {
            throw new InferenceException(InferenceException.VALUES_MISMATCH, n, y.length);
        }
        // See mean(double[]) for details.
        final double mean = IntStream.range(0, n).mapToDouble(i -> x[i] - y[i]).sum() / n;
        return mean + IntStream.range(0, n).mapToDouble(i -> (x[i] - y[i]) - mean).sum() / n;
    }

    /**
     * Returns the variance of the (signed) differences between corresponding elements of
     * the input arrays.
     *
     * <pre>
     * var(x[i] - y[i])
     * </pre>
     *
     * <p>This computes the same result as creating an array {@code z = x - y}
     * and calling {@link #variance(double[], double) variance(z, mean(z))}, but without the
     * intermediate array allocation.
     *
     * @param x First array.
     * @param y Second array.
     * @param mean the mean difference between corresponding entries
     * @return variance of paired differences
     * @throws IllegalArgumentException if the arrays do not have the same length.
     * @see #meanDifference(double[], double[])
     * @see #variance(double[], double)
     */
    static double varianceDifference(double[] x, double[] y, double mean) {
        final int n = x.length;
        if (n != y.length) {
            throw new InferenceException(InferenceException.VALUES_MISMATCH, n, y.length);
        }
        // See variance(double[]) for details.
        if (n == 1) {
            return 0;
        }
        final double[] sumSq = {0};
        final double sum2 = IntStream.range(0, n).mapToDouble(i -> {
            final double dx = (x[i] - y[i]) - mean;
            sumSq[0] += dx * dx;
            return dx;
        }).sum();
        final double sum1 = sumSq[0];
        return (sum1 - (sum2 * sum2 / n)) / (n - 1);
    }
}
