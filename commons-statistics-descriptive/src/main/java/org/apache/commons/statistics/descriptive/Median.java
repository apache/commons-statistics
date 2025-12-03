/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.statistics.descriptive;

import java.util.Objects;
import org.apache.commons.numbers.arrays.Selection;

/**
 * Returns the median of the available values.
 *
 * <p>For values of length {@code n}, let {@code k = n / 2}:
 * <ul>
 * <li>The result is {@code NaN} if {@code n = 0}.
 * <li>The result is {@code values[k]} if {@code n} is odd.
 * <li>The result is {@code (values[k - 1] + values[k]) / 2} if {@code n} is even.
 * </ul>
 *
 * <p>This implementation respects the ordering imposed by
 * {@link Double#compare(double, double)} for {@code NaN} values. If a {@code NaN} occurs
 * in the selected positions in the fully sorted values then the result is {@code NaN}.
 *
 * <p>The {@link NaNPolicy} can be used to change the behaviour on {@code NaN} values.
 *
 * <p>Instances of this class are immutable and thread-safe.
 *
 * @see #with(NaNPolicy)
 * @see <a href="https://en.wikipedia.org/wiki/Median">Median (Wikipedia)</a>
 * @since 1.1
 */
public final class Median {
    /** Default instance. */
    private static final Median DEFAULT = new Median(false, NaNPolicy.INCLUDE);

    /** Flag to indicate if the data should be copied. */
    private final boolean copy;
    /** NaN policy for floating point data. */
    private final NaNPolicy nanPolicy;
    /** Transformer for NaN data. */
    private final NaNTransformer nanTransformer;

    /**
     * @param copy Flag to indicate if the data should be copied.
     * @param nanPolicy NaN policy.
     */
    private Median(boolean copy, NaNPolicy nanPolicy) {
        this.copy = copy;
        this.nanPolicy = nanPolicy;
        nanTransformer = NaNTransformers.createNaNTransformer(nanPolicy, copy);
    }

    /**
     * Return a new instance with the default options.
     *
     * <ul>
     * <li>{@linkplain #withCopy(boolean) Copy = false}
     * <li>{@linkplain #with(NaNPolicy) NaN policy = include}
     * </ul>
     *
     * <p>Note: The default options configure for processing in-place and including
     * {@code NaN} values in the data. This is the most efficient mode and has the
     * smallest memory consumption.
     *
     * @return the median implementation
     * @see #withCopy(boolean)
     * @see #with(NaNPolicy)
     */
    public static Median withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured copy behaviour. If {@code false} then
     * the input array will be modified by the call to evaluate the median; otherwise
     * the computation uses a copy of the data.
     *
     * @param v Value.
     * @return an instance
     */
    public Median withCopy(boolean v) {
        return new Median(v, nanPolicy);
    }

    /**
     * Return an instance with the configured {@link NaNPolicy}.
     *
     * <p>Note: This implementation respects the ordering imposed by
     * {@link Double#compare(double, double)} for {@code NaN} values: {@code NaN} is
     * considered greater than all other values, and all {@code NaN} values are equal. The
     * {@link NaNPolicy} changes the computation of the statistic in the presence of
     * {@code NaN} values.
     *
     * <ul>
     * <li>{@link NaNPolicy#INCLUDE}: {@code NaN} values are moved to the end of the data;
     * the size of the data <em>includes</em> the {@code NaN} values and the median will be
     * {@code NaN} if any value used for median interpolation is {@code NaN}.
     * <li>{@link NaNPolicy#EXCLUDE}: {@code NaN} values are moved to the end of the data;
     * the size of the data <em>excludes</em> the {@code NaN} values and the median will
     * never be {@code NaN} for non-zero size. If all data are {@code NaN} then the size is zero
     * and the result is {@code NaN}.
     * <li>{@link NaNPolicy#ERROR}: An exception is raised if the data contains {@code NaN}
     * values.
     * </ul>
     *
     * <p>Note that the result is identical for all policies if no {@code NaN} values are present.
     *
     * @param v Value.
     * @return an instance
     */
    public Median with(NaNPolicy v) {
        return new Median(copy, Objects.requireNonNull(v));
    }

    /**
     * Evaluate the median.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @return the median
     * @throws IllegalArgumentException if the values contain NaN and the configuration is {@link NaNPolicy#ERROR}
     * @see #with(NaNPolicy)
     */
    public double evaluate(double[] values) {
        return compute(values, 0, values.length);
    }

    /**
     * Evaluate the median of the specified range.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return the median
     * @throws IllegalArgumentException if the values contain NaN and the configuration is {@link NaNPolicy#ERROR}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @see #with(NaNPolicy)
     * @since 1.2
     */
    public double evaluateRange(double[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return compute(values, from, to);
    }

    /**
     * Compute the median of the specified range.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return the median
     */
    private double compute(double[] values, int from, int to) {
        // Floating-point data handling
        final int[] bounds = new int[2];
        final double[] x = nanTransformer.apply(values, from, to, bounds);
        final int start = bounds[0];
        final int end = bounds[1];
        final int n = end - start;
        // Special cases
        if (n <= 2) {
            switch (n) {
            case 2:
                // Sorting the array matches the behaviour of Quantile for n==2
                // Handle NaN and signed zeros
                if (Double.compare(x[start + 1], x[start]) < 0) {
                    final double t = x[start];
                    x[start] = x[start + 1];
                    x[start + 1] = t;
                }
                return Interpolation.mean(x[start], x[start + 1]);
            case 1:
                return x[start];
            default:
                return Double.NaN;
            }
        }
        // Median index (including the offset)
        final int m = (start + end) >>> 1;
        // Odd
        if ((n & 0x1) == 1) {
            Selection.select(x, start, end, m);
            return x[m];
        }
        // Even: require (m-1, m)
        Selection.select(x, start, end, new int[] {m - 1, m});
        return Interpolation.mean(x[m - 1], x[m]);
    }

    /**
     * Evaluate the median.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @return the median
     */
    public double evaluate(int[] values) {
        return compute(values, 0, values.length);
    }

    /**
     * Evaluate the median of the specified range.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return the median
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public double evaluateRange(int[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return compute(values, from, to);
    }

    /**
     * Compute the median of the specified range.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return the median
     */
    private double compute(int[] values, int from, int to) {
        final int[] x;
        final int start;
        final int end;
        if (copy) {
            x = Statistics.copy(values, from, to);
            start = 0;
            end = x.length;
        } else {
            x = values;
            start = from;
            end = to;
        }
        final int n = end - start;
        // Special cases
        if (n <= 2) {
            switch (n) {
            case 2:
                // Sorting the array matches the behaviour of Quantile for n==2
                if (x[start + 1] < x[start]) {
                    final int t = x[start];
                    x[start] = x[start + 1];
                    x[start + 1] = t;
                }
                return Interpolation.mean(x[start], x[start + 1]);
            case 1:
                return x[start];
            default:
                return Double.NaN;
            }
        }
        // Median index (including the offset)
        final int m = (start + end) >>> 1;
        // Odd
        if ((n & 0x1) == 1) {
            Selection.select(x, start, end, m);
            return x[m];
        }
        // Even: require (m-1, m)
        Selection.select(x, start, end, new int[] {m - 1, m});
        return Interpolation.mean(x[m - 1], x[m]);
    }
}
