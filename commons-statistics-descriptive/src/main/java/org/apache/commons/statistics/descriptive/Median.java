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
     */
    public double evaluate(double[] values) {
        // Floating-point data handling
        final int[] bounds = new int[1];
        final double[] x = nanTransformer.apply(values, bounds);
        final int n = bounds[0];
        // Special cases
        if (n <= 2) {
            switch (n) {
            case 2:
                // Sort data handling NaN and signed zeros.
                // Matches the default behaviour of Quantile using p=0.5.
                if (Double.compare(x[1], x[0]) < 0) {
                    final double t = x[0];
                    x[0] = x[1];
                    x[1] = t;
                }
                return DoubleMath.mean(x[0], x[1]);
            case 1:
                return x[0];
            default:
                return Double.NaN;
            }
        }
        // Median index
        final int m = n >>> 1;
        // Odd
        if ((n & 0x1) == 1) {
            Selection.select(x, 0, n, m);
            return x[m];
        }
        // Even: require (m-1, m)
        Selection.select(x, 0, n, new int[] {m - 1, m});
        return DoubleMath.mean(x[m - 1], x[m]);
    }
}
