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

import org.apache.commons.numbers.core.Sum;
import org.apache.commons.statistics.distribution.ChiSquaredDistribution;

/**
 * Implements G-test (Generalized Log-Likelihood Ratio Test) statistics.
 *
 * <p>This is known in statistical genetics as the McDonald-Kreitman test.
 * The implementation handles both known and unknown distributions.
 *
 * <p>Two samples tests can be used when the distribution is unknown <i>a priori</i>
 * but provided by one sample, or when the hypothesis under test is that the two
 * samples come from the same underlying distribution.
 *
 * @see <a href="https://en.wikipedia.org/wiki/G-test">G-test (Wikipedia)</a>
 * @since 1.1
 */
public final class GTest {
    // Note:
    // The g-test statistic is a summation of terms with positive and negative sign
    // and thus the sum may exhibit cancellation. This class uses separate high precision
    // sums of the positive and negative terms which are then combined.
    // Total cancellation for a large number of terms will not impact
    // p-values of interest around critical alpha values as the Chi^2
    // distribution exhibits strong concentration around its mean (degrees of freedom, k).
    // The summation only need maintain enough bits in the final sum to distinguish
    // g values around critical alpha values where 0 << chisq.sf(g, k) << 0.5: g > k,
    // with k = number of terms - 1.

    /** Default instance. */
    private static final GTest DEFAULT = new GTest(0);

    /** Degrees of freedom adjustment. */
    private final int degreesOfFreedomAdjustment;

    /**
     * @param degreesOfFreedomAdjustment Degrees of freedom adjustment.
     */
    private GTest(int degreesOfFreedomAdjustment) {
        this.degreesOfFreedomAdjustment = degreesOfFreedomAdjustment;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     * <li>{@linkplain #withDegreesOfFreedomAdjustment(int) Degrees of freedom adjustment = 0}
     * </ul>
     *
     * @return default instance
     */
    public static GTest withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured degrees of freedom adjustment.
     *
     * <p>The default degrees of freedom for a sample of length {@code n} are
     * {@code n - 1}. An intrinsic null hypothesis is one where you estimate one or
     * more parameters from the data in order to get the numbers for your null
     * hypothesis. For a distribution with {@code p} parameters where up to
     * {@code p} parameters have been estimated from the data the degrees of freedom
     * is in the range {@code [n - 1 - p, n - 1]}.
     *
     * @param v Value.
     * @return an instance
     * @throws IllegalArgumentException if the value is negative
     */
    public GTest withDegreesOfFreedomAdjustment(int v) {
        return new GTest(Arguments.checkNonNegative(v));
    }

    /**
     * Computes the G-test goodness-of-fit statistic comparing the {@code observed} counts to
     * a uniform expected value (each category is equally likely).
     *
     * <p>Note: This is a specialized version of a comparison of {@code observed}
     * with an {@code expected} array of uniform values. The result is faster than
     * calling {@link #statistic(double[], long[])} and the statistic is the same,
     * with an allowance for accumulated floating-point error due to the optimized
     * routine.
     *
     * @param observed Observed frequency counts.
     * @return G-test statistic
     * @throws IllegalArgumentException if the sample size is less than 2;
     * {@code observed} has negative entries; or all the the observations are zero.
     * @see #test(long[])
     */
    public double statistic(long[] observed) {
        Arguments.checkValuesRequiredSize(observed.length, 2);
        Arguments.checkNonNegative(observed);
        final double e = StatisticUtils.mean(observed);
        if (e == 0) {
            throw new InferenceException(InferenceException.NO_DATA);
        }
        // g = 2 * sum{o * ln(o/e)}
        //   = 2 * [ sum{o * ln(o)} - sum(o) * ln(e) ]
        // The second form has more cancellation as the sums are larger.
        // Separate sum for positive and negative terms.
        final Sum sum = Sum.create();
        final Sum sum2 = Sum.create();
        for (final double o : observed) {
            if (o > e) {
                // Positive term
                sum.add(o * Math.log(o / e));
            } else if (o > 0) {
                // Negative term
                // Process non-zero counts to avoid 0 * -inf = NaN
                sum2.add(o * Math.log(o / e));
            }
        }
        return sum.add(sum2).getAsDouble() * 2;
    }

    /**
     * Computes the G-test goodness-of-fit statistic comparing {@code observed} and {@code expected}
     * frequency counts.
     *
     * <p><strong>Note:</strong>This implementation rescales the values
     * if necessary to ensure that the sum of the expected and observed counts
     * are equal.
     *
     * @param expected Expected frequency counts.
     * @param observed Observed frequency counts.
     * @return G-test statistic
     * @throws IllegalArgumentException if the sample size is less than 2; the array
     * sizes do not match; {@code expected} has entries that are not strictly
     * positive; {@code observed} has negative entries; or all the the observations are zero.
     * @see #test(double[], long[])
     */
    public double statistic(double[] expected, long[] observed) {
        // g = 2 * sum{o * ln(o/e)}
        // The sum of o and e must be the same.
        final double ratio = StatisticUtils.computeRatio(expected, observed);
        // High precision sum to reduce cancellation.
        // Separate sum for positive and negative terms.
        final Sum sum = Sum.create();
        final Sum sum2 = Sum.create();
        for (int i = 0; i < observed.length; i++) {
            final long o = observed[i];
            // Process non-zero counts to avoid 0 * -inf = NaN
            if (o != 0) {
                final double term = o * Math.log(o / (ratio * expected[i]));
                if (term < 0) {
                    sum2.add(term);
                } else {
                    sum.add(term);
                }
            }
        }
        return sum.add(sum2).getAsDouble() * 2;
    }

    /**
     * Computes a G-test statistic associated with a G-test of
     * independence based on the input {@code counts} array, viewed as a two-way
     * table. The formula used to compute the test statistic is:
     *
     * <p>\[ G = 2 \cdot \sum_{ij}{O_{ij}} \cdot \left[ H(r) + H(c) - H(r,c) \right] \]
     *
     * <p>and \( H \) is the <a
     * href="https://en.wikipedia.org/wiki/Entropy_%28information_theory%29">
     * Shannon Entropy</a> of the random variable formed by viewing the elements of
     * the argument array as incidence counts:
     *
     * <p>\[ H(X) = - {\sum_{x \in \text{Supp}(X)} p(x) \ln p(x)} \]
     *
     * @param counts 2-way table.
     * @return G-test statistic
     * @throws IllegalArgumentException if the number of rows or columns is less
     * than 2; the array is non-rectangular; the array has negative entries; or the
     * sum of a row or column is zero.
     * @see ChiSquareTest#test(long[][])
     */
    public double statistic(long[][] counts) {
        Arguments.checkCategoriesRequiredSize(counts.length, 2);
        Arguments.checkValuesRequiredSize(counts[0].length, 2);
        Arguments.checkRectangular(counts);
        Arguments.checkNonNegative(counts);

        final int ni = counts.length;
        final int nj = counts[0].length;

        // Compute row, column and total sums
        final double[] sumi = new double[ni];
        final double[] sumj = new double[nj];
        double n = 0;
        // We can sum data on the first pass. See below for computation details.
        final Sum sum = Sum.create();
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                final long c = counts[i][j];
                sumi[i] += c;
                sumj[j] += c;
                if (c > 1) {
                    sum.add(c * Math.log(c));
                }
            }
            checkNonZero(sumi[i], "Row", i);
            n += sumi[i];
        }

        for (int j = 0; j < nj; j++) {
            checkNonZero(sumj[j], "Column", j);
        }

        // This computes a modified form of the Shannon entropy H without requiring
        // normalisation of observations to probabilities and without negation,
        // i.e. we compute n * [ H(r) + H(c) - H(r,c) ] as [ H'(r,c) - H'(r) - H'(c) ].

        // H  = -sum (p * log(p))
        // H' = n * sum (p * log(p))
        //    = n * sum (o/n * log(o/n))
        //    = n * [ sum(o/n * log(o)) - sum(o/n * log(n)) ]
        //    = sum(o * log(o)) - n log(n)

        // After 3 modified entropy sums H'(r,c) - H'(r) - H'(c) compensation is (-1 + 2) * n log(n)
        sum.addProduct(n, Math.log(n));
        // Negative terms
        final Sum sum2 = Sum.create();
        // All these counts are above zero so no check for zeros
        for (final double c : sumi) {
            sum2.add(c * -Math.log(c));
        }
        for (final double c : sumj) {
            sum2.add(c * -Math.log(c));
        }

        return sum.add(sum2).getAsDouble() * 2;
    }

    /**
     * Perform a G-test for goodness-of-fit evaluating the null hypothesis that the {@code observed}
     * counts conform to a uniform distribution (each category is equally likely).
     *
     * @param observed Observed frequency counts.
     * @return test result
     * @throws IllegalArgumentException if the sample size is less than 2;
     * {@code observed} has negative entries; or all the the observations are zero
     * @see #statistic(long[])
     */
    public SignificanceResult test(long[] observed) {
        final int df = observed.length - 1;
        final double g = statistic(observed);
        final double p = computeP(g, df);
        return new BaseSignificanceResult(g, p);
    }

    /**
     * Perform a G-test for goodness-of-fit evaluating the null hypothesis that the {@code observed}
     * counts conform to the {@code expected} counts.
     *
     * <p>The test can be configured to apply an adjustment to the degrees of freedom
     * if the observed data has been used to create the expected counts.
     *
     * @param expected Expected frequency counts.
     * @param observed Observed frequency counts.
     * @return test result
     * @throws IllegalArgumentException if the sample size is less than 2; the array
     * sizes do not match; {@code expected} has entries that are not strictly
     * positive; {@code observed} has negative entries; all the the observations are zero; or
     * the adjusted degrees of freedom are not strictly positive
     * @see #withDegreesOfFreedomAdjustment(int)
     * @see #statistic(double[], long[])
     */
    public SignificanceResult test(double[] expected, long[] observed) {
        final int df = StatisticUtils.computeDegreesOfFreedom(observed.length, degreesOfFreedomAdjustment);
        final double g = statistic(expected, observed);
        final double p = computeP(g, df);
        return new BaseSignificanceResult(g, p);
    }

    /**
     * Perform a G-test of independence based on the input
     * {@code counts} array, viewed as a two-way table.
     *
     * @param counts 2-way table.
     * @return test result
     * @throws IllegalArgumentException if the number of rows or columns is less
     * than 2; the array is non-rectangular; the array has negative entries; or the
     * sum of a row or column is zero.
     * @see #statistic(long[][])
     */
    public SignificanceResult test(long[][] counts) {
        final double g = statistic(counts);
        final double df = (counts.length - 1.0) * (counts[0].length - 1.0);
        final double p = computeP(g, df);
        return new BaseSignificanceResult(g, p);
    }

    /**
     * Compute the G-test p-value.
     *
     * @param g G-test statistic.
     * @param degreesOfFreedom Degrees of freedom.
     * @return p-value
     */
    private static double computeP(double g, double degreesOfFreedom) {
        return ChiSquaredDistribution.of(degreesOfFreedom).survivalProbability(g);
    }

    /**
     * Check the array value is non-zero.
     *
     * @param value Value
     * @param name Name of the array
     * @param index Index in the array
     * @throws IllegalArgumentException if the value is zero
     */
    private static void checkNonZero(double value, String name, int index) {
        if (value == 0) {
            throw new InferenceException(InferenceException.ZERO_AT, name, index);
        }
    }
}
