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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntToDoubleFunction;
import org.apache.commons.numbers.arrays.Selection;

/**
 * Provides quantile computation.
 *
 * <p>For values of length {@code n}:
 * <ul>
 * <li>The result is {@code NaN} if {@code n = 0}.
 * <li>The result is {@code values[0]} if {@code n = 1}.
 * <li>Otherwise the result is computed using the {@link EstimationMethod}.
 * </ul>
 *
 * <p>Computation of multiple quantiles and will handle duplicate and unordered
 * probabilities. Passing ordered probabilities is recommended if the order is already
 * known as this can improve efficiency; for example using uniform spacing through the
 * array data, or to identify extreme values from the data such as {@code [0.001, 0.999]}.
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
 * @see <a href="https://en.wikipedia.org/wiki/Quantile">Quantile (Wikipedia)</a>
 * @since 1.1
 */
public final class Quantile {
    /** Message when the probability is not in the range {@code [0, 1]}. */
    private static final String INVALID_PROBABILITY = "Invalid probability: ";
    /** Message when no probabilities are provided for the varargs method. */
    private static final String NO_PROBABILITIES_SPECIFIED = "No probabilities specified";
    /** Message when the size is not valid. */
    private static final String INVALID_SIZE = "Invalid size: ";
    /** Message when the number of probabilities in a range is not valid. */
    private static final String INVALID_NUMBER_OF_PROBABILITIES = "Invalid number of probabilities: ";

    /** Default instance. Method 8 is recommended by Hyndman and Fan. */
    private static final Quantile DEFAULT = new Quantile(false, NaNPolicy.INCLUDE, EstimationMethod.HF8);

    /** Flag to indicate if the data should be copied. */
    private final boolean copy;
    /** NaN policy for floating point data. */
    private final NaNPolicy nanPolicy;
    /** Transformer for NaN data. */
    private final NaNTransformer nanTransformer;
    /** Estimation type used to determine the value from the quantile. */
    private final EstimationMethod estimationType;

    /**
     * @param copy Flag to indicate if the data should be copied.
     * @param nanPolicy NaN policy.
     * @param estimationType Estimation type used to determine the value from the quantile.
     */
    private Quantile(boolean copy, NaNPolicy nanPolicy, EstimationMethod estimationType) {
        this.copy = copy;
        this.nanPolicy = nanPolicy;
        this.estimationType = estimationType;
        nanTransformer = NaNTransformers.createNaNTransformer(nanPolicy, copy);
    }

    /**
     * Return a new instance with the default options.
     *
     * <ul>
     * <li>{@linkplain #withCopy(boolean) Copy = false}
     * <li>{@linkplain #with(NaNPolicy) NaN policy = include}
     * <li>{@linkplain #with(EstimationMethod) Estimation method = HF8}
     * </ul>
     *
     * <p>Note: The default options configure for processing in-place and including
     * {@code NaN} values in the data. This is the most efficient mode and has the
     * smallest memory consumption.
     *
     * @return the quantile implementation
     * @see #withCopy(boolean)
     * @see #with(NaNPolicy)
     * @see #with(EstimationMethod)
     */
    public static Quantile withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured copy behaviour. If {@code false} then
     * the input array will be modified by the call to evaluate the quantiles; otherwise
     * the computation uses a copy of the data.
     *
     * @param v Value.
     * @return an instance
     */
    public Quantile withCopy(boolean v) {
        return new Quantile(v, nanPolicy, estimationType);
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
     * the size of the data <em>includes</em> the {@code NaN} values and the quantile will be
     * {@code NaN} if any value used for quantile interpolation is {@code NaN}.
     * <li>{@link NaNPolicy#EXCLUDE}: {@code NaN} values are moved to the end of the data;
     * the size of the data <em>excludes</em> the {@code NaN} values and the quantile will
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
    public Quantile with(NaNPolicy v) {
        return new Quantile(copy, Objects.requireNonNull(v), estimationType);
    }

    /**
     * Return an instance with the configured {@link EstimationMethod}.
     *
     * @param v Value.
     * @return an instance
     */
    public Quantile with(EstimationMethod v) {
        return new Quantile(copy, nanPolicy, Objects.requireNonNull(v));
    }

    /**
     * Generate {@code n} evenly spaced probabilities in the range {@code [0, 1]}.
     *
     * <pre>
     * 1/(n + 1), 2/(n + 1), ..., n/(n + 1)
     * </pre>
     *
     * @param n Number of probabilities.
     * @return the probabilities
     * @throws IllegalArgumentException if {@code n < 1}
     */
    public static double[] probabilities(int n) {
        checkNumberOfProbabilities(n);
        final double c1 = n + 1.0;
        final double[] p = new double[n];
        for (int i = 0; i < n; i++) {
            p[i] = (i + 1.0) / c1;
        }
        return p;
    }

    /**
     * Generate {@code n} evenly spaced probabilities in the range {@code [p1, p2]}.
     *
     * <pre>
     * w = p2 - p1
     * p1 + w/(n + 1), p1 + 2w/(n + 1), ..., p1 + nw/(n + 1)
     * </pre>
     *
     * @param n Number of probabilities.
     * @param p1 Lower probability.
     * @param p2 Upper probability.
     * @return the probabilities
     * @throws IllegalArgumentException if {@code n < 1}; if the probabilities are not in the
     * range {@code [0, 1]}; or {@code p2 <= p1}.
     */
    public static double[] probabilities(int n, double p1, double p2) {
        checkProbability(p1);
        checkProbability(p2);
        if (p2 <= p1) {
            throw new IllegalArgumentException("Invalid range: [" + p1 + ", " + p2 + "]");
        }
        final double[] p = probabilities(n);
        for (int i = 0; i < n; i++) {
            p[i] = (1 - p[i]) * p1 + p[i] * p2;
        }
        return p;
    }

    /**
     * Evaluate the {@code p}-th quantile of the values.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * <p><strong>Performance</strong>
     *
     * <p>It is not recommended to use this method for repeat calls for different quantiles
     * within the same values. The {@link #evaluate(double[], double...)} method should be used
     * which provides better performance.
     *
     * @param values Values.
     * @param p Probability for the quantile to compute.
     * @return the quantile
     * @throws IllegalArgumentException if the probability {@code p} is not in the range {@code [0, 1]};
     * or if the values contain NaN and the configuration is {@link NaNPolicy#ERROR}
     * @see #evaluate(double[], double...)
     * @see #with(NaNPolicy)
     */
    public double evaluate(double[] values, double p) {
        return compute(values, 0, values.length, p);
    }

    /**
     * Evaluate the {@code p}-th quantile of the specified range of values.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * <p><strong>Performance</strong>
     *
     * <p>It is not recommended to use this method for repeat calls for different quantiles
     * within the same values. The {@link #evaluateRange(double[], int, int, double...)} method should be used
     * which provides better performance.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probability for the quantile to compute.
     * @return the quantile
     * @throws IllegalArgumentException if the probability {@code p} is not in the range {@code [0, 1]};
     * or if the values contain NaN and the configuration is {@link NaNPolicy#ERROR}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @see #evaluateRange(double[], int, int, double...)
     * @see #with(NaNPolicy)
     * @since 1.2
     */
    public double evaluateRange(double[] values, int from, int to, double p) {
        Statistics.checkFromToIndex(from, to, values.length);
        return compute(values, from, to, p);
    }

    /**
     * Compute the {@code p}-th quantile of the specified range of values.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probability for the quantile to compute.
     * @return the quantile
     * @throws IllegalArgumentException if the probability {@code p} is not in the range {@code [0, 1]}
     */
    private double compute(double[] values, int from, int to, double p) {
        checkProbability(p);
        // Floating-point data handling
        final int[] bounds = new int[2];
        final double[] x = nanTransformer.apply(values, from, to, bounds);
        final int start = bounds[0];
        final int end = bounds[1];
        final int n = end - start;
        // Special cases
        if (n <= 1) {
            return n == 0 ? Double.NaN : x[start];
        }

        final double pos = estimationType.index(p, n);
        final int ip = (int) pos;
        final int i = start + ip;

        // Partition and compute
        if (pos > ip) {
            Selection.select(x, start, end, new int[] {i, i + 1});
            return Interpolation.interpolate(x[i], x[i + 1], pos - ip);
        }
        Selection.select(x, start, end, i);
        return x[i];
    }

    /**
     * Evaluate the {@code p}-th quantiles of the values.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @param p Probabilities for the quantiles to compute.
     * @return the quantiles
     * @throws IllegalArgumentException if any probability {@code p} is not in the range {@code [0, 1]};
     * no probabilities are specified; or if the values contain NaN and the configuration is {@link NaNPolicy#ERROR}
     * @see #with(NaNPolicy)
     */
    public double[] evaluate(double[] values, double... p) {
        return compute(values, 0, values.length, p);
    }

    /**
     * Evaluate the {@code p}-th quantiles of the specified range of values.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probabilities for the quantiles to compute.
     * @return the quantiles
     * @throws IllegalArgumentException if any probability {@code p} is not in the range {@code [0, 1]};
     * no probabilities are specified; or if the values contain NaN and the configuration is {@link NaNPolicy#ERROR}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @see #with(NaNPolicy)
     * @since 1.2
     */
    public double[] evaluateRange(double[] values, int from, int to, double... p) {
        Statistics.checkFromToIndex(from, to, values.length);
        return compute(values, from, to, p);
    }

    /**
     * Compute the {@code p}-th quantiles of the specified range of values.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probabilities for the quantiles to compute.
     * @return the quantiles
     * @throws IllegalArgumentException if any probability {@code p} is not in the range {@code [0, 1]};
     * or no probabilities are specified.
     */
    private double[] compute(double[] values, int from, int to, double... p) {
        checkProbabilities(p);
        // Floating-point data handling
        final int[] bounds = new int[2];
        final double[] x = nanTransformer.apply(values, from, to, bounds);
        final int start = bounds[0];
        final int end = bounds[1];
        final int n = end - start;
        // Special cases
        final double[] q = new double[p.length];
        if (n <= 1) {
            Arrays.fill(q, n == 0 ? Double.NaN : x[start]);
            return q;
        }

        // Collect interpolation positions. We use the output q as storage.
        final int[] indices = computeIndices(n, p, q, start);

        // Partition
        Selection.select(x, start, end, indices);

        // Compute
        for (int k = 0; k < p.length; k++) {
            // ip in [0, n); i in [start, end)
            final int ip = (int) q[k];
            final int i = start + ip;
            if (q[k] > ip) {
                q[k] = Interpolation.interpolate(x[i], x[i + 1], q[k] - ip);
            } else {
                q[k] = x[i];
            }
        }
        return q;
    }

    /**
     * Evaluate the {@code p}-th quantile of the values.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * <p><strong>Performance</strong>
     *
     * <p>It is not recommended to use this method for repeat calls for different quantiles
     * within the same values. The {@link #evaluate(int[], double...)} method should be used
     * which provides better performance.
     *
     * @param values Values.
     * @param p Probability for the quantile to compute.
     * @return the quantile
     * @throws IllegalArgumentException if the probability {@code p} is not in the range {@code [0, 1]}
     * @see #evaluate(int[], double...)
     */
    public double evaluate(int[] values, double p) {
        return compute(values, 0, values.length, p);
    }

    /**
     * Evaluate the {@code p}-th quantile of the specified range of values.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * <p><strong>Performance</strong>
     *
     * <p>It is not recommended to use this method for repeat calls for different quantiles
     * within the same values. The {@link #evaluateRange(int[], int, int, double...)} method should be used
     * which provides better performance.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probability for the quantile to compute.
     * @return the quantile
     * @throws IllegalArgumentException if the probability {@code p} is not in the range {@code [0, 1]}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @see #evaluateRange(int[], int, int, double...)
     * @since 1.2
     */
    public double evaluateRange(int[] values, int from, int to, double p) {
        Statistics.checkFromToIndex(from, to, values.length);
        return compute(values, from, to, p);
    }

    /**
     * Compute the {@code p}-th quantile of the specified range of values.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probability for the quantile to compute.
     * @return the quantile
     * @throws IllegalArgumentException if the probability {@code p} is not in the range {@code [0, 1]}
     */
    private double compute(int[] values, int from, int to, double p) {
        checkProbability(p);
        final int n = to - from;
        // Special cases
        if (n <= 1) {
            return n == 0 ? Double.NaN : values[from];
        }

        // Create the range
        final int[] x;
        final int start;
        final int end;
        if (copy) {
            x = Statistics.copy(values, from, to);
            start = 0;
            end = n;
        } else {
            x = values;
            start = from;
            end = to;
        }

        final double pos = estimationType.index(p, n);
        final int ip = (int) pos;
        final int i = start + ip;

        // Partition and compute
        if (pos > ip) {
            Selection.select(x, start, end, new int[] {i, i + 1});
            return Interpolation.interpolate(x[i], x[i + 1], pos - ip);
        }
        Selection.select(x, start, end, i);
        return x[i];
    }

    /**
     * Evaluate the {@code p}-th quantiles of the values.
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @param p Probabilities for the quantiles to compute.
     * @return the quantiles
     * @throws IllegalArgumentException if any probability {@code p} is not in the range {@code [0, 1]};
     * or no probabilities are specified.
     */
    public double[] evaluate(int[] values, double... p) {
        return compute(values, 0, values.length, p);
    }

    /**
     * Evaluate the {@code p}-th quantiles of the specified range of values..
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probabilities for the quantiles to compute.
     * @return the quantiles
     * @throws IllegalArgumentException if any probability {@code p} is not in the range {@code [0, 1]};
     * or no probabilities are specified.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public double[] evaluateRange(int[] values, int from, int to, double... p) {
        Statistics.checkFromToIndex(from, to, values.length);
        return compute(values, from, to, p);
    }

    /**
     * Evaluate the {@code p}-th quantiles of the specified range of values..
     *
     * <p>Note: This method may partially sort the input values if not configured to
     * {@link #withCopy(boolean) copy} the input data.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param p Probabilities for the quantiles to compute.
     * @return the quantiles
     * @throws IllegalArgumentException if any probability {@code p} is not in the range {@code [0, 1]};
     * or no probabilities are specified.
     */
    private double[] compute(int[] values, int from, int to, double... p) {
        checkProbabilities(p);
        final int n = to - from;
        // Special cases
        final double[] q = new double[p.length];
        if (n <= 1) {
            Arrays.fill(q, n == 0 ? Double.NaN : values[from]);
            return q;
        }

        // Create the range
        final int[] x;
        final int start;
        final int end;
        if (copy) {
            x = Statistics.copy(values, from, to);
            start = 0;
            end = n;
        } else {
            x = values;
            start = from;
            end = to;
        }

        // Collect interpolation positions. We use the output q as storage.
        final int[] indices = computeIndices(n, p, q, start);

        // Partition
        Selection.select(x, start, end, indices);

        // Compute
        for (int k = 0; k < p.length; k++) {
            // ip in [0, n); i in [start, end)
            final int ip = (int) q[k];
            final int i = start + ip;
            if (q[k] > ip) {
                q[k] = Interpolation.interpolate(x[i], x[i + 1], q[k] - ip);
            } else {
                q[k] = x[i];
            }
        }
        return q;
    }

    /**
     * Evaluate the {@code p}-th quantile of the values.
     *
     * <p>This method can be used when the values of known size are already sorted.
     *
     * <pre>{@code
     * short[] x = ...
     * Arrays.sort(x);
     * double q = Quantile.withDefaults().evaluate(x.length, i -> x[i], 0.05);
     * }</pre>
     *
     * @param n Size of the values.
     * @param values Values function.
     * @param p Probability for the quantile to compute.
     * @return the quantile
     * @throws IllegalArgumentException if {@code size < 0}; or if the probability {@code p} is
     * not in the range {@code [0, 1]}.
     */
    public double evaluate(int n, IntToDoubleFunction values, double p) {
        checkSize(n);
        checkProbability(p);
        // Special case
        if (n <= 1) {
            return n == 0 ? Double.NaN : values.applyAsDouble(0);
        }
        final double pos = estimationType.index(p, n);
        final int i = (int) pos;
        final double v1 = values.applyAsDouble(i);
        if (pos > i) {
            final double v2 = values.applyAsDouble(i + 1);
            return Interpolation.interpolate(v1, v2, pos - i);
        }
        return v1;
    }

    /**
     * Evaluate the {@code p}-th quantiles of the values.
     *
     * <p>This method can be used when the values of known size are already sorted.
     *
     * <pre>{@code
     * short[] x = ...
     * Arrays.sort(x);
     * double[] q = Quantile.withDefaults().evaluate(x.length, i -> x[i], 0.25, 0.5, 0.75);
     * }</pre>
     *
     * @param n Size of the values.
     * @param values Values function.
     * @param p Probabilities for the quantiles to compute.
     * @return the quantiles
     * @throws IllegalArgumentException if {@code size < 0}; if any probability {@code p} is
     * not in the range {@code [0, 1]}; or no probabilities are specified.
     */
    public double[] evaluate(int n, IntToDoubleFunction values, double... p) {
        checkSize(n);
        checkProbabilities(p);
        // Special case
        final double[] q = new double[p.length];
        if (n <= 1) {
            Arrays.fill(q, n == 0 ? Double.NaN : values.applyAsDouble(0));
            return q;
        }
        for (int k = 0; k < p.length; k++) {
            final double pos = estimationType.index(p[k], n);
            final int i = (int) pos;
            final double v1 = values.applyAsDouble(i);
            if (pos > i) {
                final double v2 = values.applyAsDouble(i + 1);
                q[k] = Interpolation.interpolate(v1, v2, pos - i);
            } else {
                q[k] = v1;
            }
        }
        return q;
    }

    /**
     * Check the probability {@code p} is in the range {@code [0, 1]}.
     *
     * @param p Probability for the quantile to compute.
     * @throws IllegalArgumentException if the probability is not in the range {@code [0, 1]}
     */
    private static void checkProbability(double p) {
        // Logic negation will detect NaN
        if (!(p >= 0 && p <= 1)) {
            throw new IllegalArgumentException(INVALID_PROBABILITY + p);
        }
    }

    /**
     * Check the probabilities {@code p} are in the range {@code [0, 1]}.
     *
     * @param p Probabilities for the quantiles to compute.
     * @throws IllegalArgumentException if any probabilities {@code p} is not in the range {@code [0, 1]};
     * or no probabilities are specified.
     */
    private static void checkProbabilities(double... p) {
        if (p.length == 0) {
            throw new IllegalArgumentException(NO_PROBABILITIES_SPECIFIED);
        }
        for (final double pp : p) {
            checkProbability(pp);
        }
    }

    /**
     * Check the {@code size} is positive.
     *
     * @param n Size of the values.
     * @throws IllegalArgumentException if {@code size < 0}
     */
    private static void checkSize(int n) {
        if (n < 0) {
            throw new IllegalArgumentException(INVALID_SIZE + n);
        }
    }

    /**
     * Check the number of probabilities {@code n} is strictly positive.
     *
     * @param n Number of probabilities.
     * @throws IllegalArgumentException if {@code c < 1}
     */
    private static void checkNumberOfProbabilities(int n) {
        if (n < 1) {
            throw new IllegalArgumentException(INVALID_NUMBER_OF_PROBABILITIES + n);
        }
    }

    /**
     * Compute the indices required for quantile interpolation.
     *
     * <p>The zero-based interpolation index in {@code [0, n)} is
     * saved into the working array {@code q} for each {@code p}.
     *
     * <p>The indices are incremented by the provided {@code offset} to allow
     * addressing sub-ranges of a larger array.
     *
     * @param n Size of the data.
     * @param p Probabilities for the quantiles to compute.
     * @param q Working array for quantiles in {@code [0, n)}.
     * @param offset Array offset.
     * @return the indices in {@code [offset, offset + n)}
     */
    private int[] computeIndices(int n, double[] p, double[] q, int offset) {
        final int[] indices = new int[p.length << 1];
        int count = 0;
        for (int k = 0; k < p.length; k++) {
            final double pos = estimationType.index(p[k], n);
            q[k] = pos;
            final int i = (int) pos;
            indices[count++] = offset + i;
            if (pos > i) {
                // Require the next index for interpolation
                indices[count++] = offset + i + 1;
            }
        }
        if (count < indices.length) {
            return Arrays.copyOf(indices, count);
        }
        return indices;
    }

    /**
     * Estimation methods for a quantile. Provides the nine quantile algorithms
     * defined in Hyndman and Fan (1996)[1] as {@code HF1 - HF9}.
     *
     * <p>Samples quantiles are defined by:
     *
     * <p>\[ Q(p) = (1 - \gamma) x_j + \gamma x_{j+1} \]
     *
     * <p>where \( \frac{j-m}{n} \leq p \le \frac{j-m+1}{n} \), \( x_j \) is the \( j \)th
     * order statistic, \( n \) is the sample size, the value of \( \gamma \) is a function
     * of \( j = \lfloor np+m \rfloor \) and \( g = np + m - j \), and \( m \) is a constant
     * determined by the sample quantile type.
     *
     * <p>Note that the real-valued position \( np + m \) is a 1-based index and
     * \( j \in [1, n] \). If the real valued position is computed as beyond the lowest or
     * highest values in the sample, this implementation will return the minimum or maximum
     * observation respectively.
     *
     * <p>Types 1, 2, and 3 are discontinuous functions of \( p \); types 4 to 9 are continuous
     * functions of \( p \).
     *
     * <p>For the continuous functions, the probability \( p_k \) is provided for the \( k \)-th order
     * statistic in size \( n \). Samples quantiles are equivalently obtained to \( Q(p) \) by
     * linear interpolation between points \( (p_k, x_k) \) and \( (p_{k+1}, x_{k+1}) \) for
     * any \( p_k \leq p \leq p_{k+1} \).
     *
     * <ol>
     * <li>Hyndman and Fan (1996)
     *     <i>Sample Quantiles in Statistical Packages.</i>
     *     The American Statistician, 50, 361-365.
     *     <a href="https://www.jstor.org/stable/2684934">doi.org/10.2307/2684934</a>
     * <li><a href="https://en.wikipedia.org/wiki/Quantile">Quantile (Wikipedia)</a>
     * </ol>
     */
    public enum EstimationMethod {
        /**
         * Inverse of the empirical distribution function.
         *
         * <p>\( m = 0 \). \( \gamma = 0 \) if \( g = 0 \), and 1 otherwise.
         */
        HF1 {
            @Override
            double position0(double p, int n) {
                // position = np + 0. This is 1-based so adjust to 0-based.
                return Math.ceil(n * p) - 1;
            }
        },
        /**
         * Similar to {@link #HF1} with averaging at discontinuities.
         *
         * <p>\( m = 0 \). \( \gamma = 0.5 \) if \( g = 0 \), and 1 otherwise.
         */
        HF2 {
            @Override
            double position0(double p, int n) {
                final double pos = n * p;
                // Average at discontinuities
                final int j = (int) pos;
                final double g = pos - j;
                if (g == 0) {
                    return j - 0.5;
                }
                // As HF1 : ceil(j + g) - 1
                return j;
            }
        },
        /**
         * The observation closest to \( np \). Ties are resolved to the nearest even order statistic.
         *
         * <p>\( m = -1/2 \). \( \gamma = 0 \) if \( g = 0 \) and \( j \) is even, and 1 otherwise.
         */
        HF3 {
            @Override
            double position0(double p, int n) {
                // Let rint do the work for ties to even
                return Math.rint(n * p) - 1;
            }
        },
        /**
         * Linear interpolation of the inverse of the empirical CDF.
         *
         * <p>\( m = 0 \). \( p_k = \frac{k}{n} \).
         */
        HF4 {
            @Override
            double position0(double p, int n) {
                // np + 0 - 1
                return n * p - 1;
            }
        },
        /**
         * A piecewise linear function where the knots are the values midway through the steps of
         * the empirical CDF. Proposed by Hazen (1914) and popular amongst hydrologists.
         *
         * <p>\( m = 1/2 \). \( p_k = \frac{k - 1/2}{n} \).
         */
        HF5 {
            @Override
            double position0(double p, int n) {
                // np + 0.5 - 1
                return n * p - 0.5;
            }
        },
        /**
         * Linear interpolation of the expectations for the order statistics for the uniform
         * distribution on [0,1]. Proposed by Weibull (1939).
         *
         * <p>\( m = p \). \( p_k = \frac{k}{n + 1} \).
         *
         * <p>This method computes the quantile as per the Apache Commons Math Percentile
         * legacy implementation.
         */
        HF6 {
            @Override
            double position0(double p, int n) {
                // np + p - 1
                return (n + 1) * p - 1;
            }
        },
        /**
         * Linear interpolation of the modes for the order statistics for the uniform
         * distribution on [0,1]. Proposed by Gumbull (1939).
         *
         * <p>\( m = 1 - p \). \( p_k = \frac{k - 1}{n - 1} \).
         */
        HF7 {
            @Override
            double position0(double p, int n) {
                // np + 1-p - 1
                return (n - 1) * p;
            }
        },
        /**
         * Linear interpolation of the approximate medians for order statistics.
         *
         * <p>\( m = (p + 1)/3 \). \( p_k = \frac{k - 1/3}{n + 1/3} \).
         *
         * <p>As per Hyndman and Fan (1996) this approach is most recommended as it provides
         * an approximate median-unbiased estimate regardless of distribution.
         */
        HF8 {
            @Override
            double position0(double p, int n) {
                return n * p + (p + 1) / 3 - 1;
            }
        },
        /**
         * Quantile estimates are approximately unbiased for the expected order statistics if
         * \( x \) is normally distributed.
         *
         * <p>\( m = p/4 + 3/8 \). \( p_k = \frac{k - 3/8}{n + 1/4} \).
         */
        HF9 {
            @Override
            double position0(double p, int n) {
                // np + p/4 + 3/8 - 1
                return (n + 0.25) * p - 0.625;
            }
        };

        /**
         * Finds the real-valued position for calculation of the quantile.
         *
         * <p>Return {@code i + g} such that the quantile value from sorted data is:
         *
         * <p>value = data[i] + g * (data[i+1] - data[i])
         *
         * <p>Warning: Interpolation should not use {@code data[i+1]} unless {@code g != 0}.
         *
         * <p>Note: In contrast to the definition of Hyndman and Fan in the class header
         * which uses a 1-based position, this is a zero based index. This change is for
         * convenience when addressing array positions.
         *
         * @param p p<sup>th</sup> quantile.
         * @param n Size.
         * @return a real-valued position (0-based) into the range {@code [0, n)}
         */
        abstract double position0(double p, int n);

        /**
         * Finds the index {@code i} and fractional part {@code g} of a real-valued position
         * to interpolate the quantile.
         *
         * <p>Return {@code i + g} such that the quantile value from sorted data is:
         *
         * <p>value = data[i] + g * (data[i+1] - data[i])
         *
         * <p>Note: Interpolation should not use {@code data[i+1]} unless {@code g != 0}.
         *
         * @param p p<sup>th</sup> quantile.
         * @param n Size.
         * @return index (in [0, n-1])
         */
        final double index(double p, int n) {
            final double pos = position0(p, n);
            // Bounds check in [0, n-1]
            if (pos < 0) {
                return 0;
            }
            if (pos > n - 1.0) {
                return n - 1.0;
            }
            return pos;
        }
    }
}
