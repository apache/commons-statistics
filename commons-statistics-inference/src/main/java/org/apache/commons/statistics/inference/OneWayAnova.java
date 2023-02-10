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

import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.numbers.core.Sum;
import org.apache.commons.statistics.distribution.FDistribution;

/**
 * Implements one-way ANOVA (analysis of variance) statistics.
 *
 * <p>Tests for differences between two or more categories of univariate data
 * (for example, the body mass index of accountants, lawyers, doctors and
 * computer programmers). When two categories are given, this is equivalent to
 * the {@link TTest}.
 *
 * <p>This implementation computes the F statistic using the definitional formula:
 *
 * <p>\[ F = \frac{\text{between-group variability}}{\text{within-group variability}} \]
 *
 * @see <a href="https://en.wikipedia.org/wiki/Analysis_of_variance">Analysis of variance (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/F-test#Multiple-comparison_ANOVA_problems">
 * Multiple-comparison ANOVA problems (Wikipedia)</a>
 * @see <a href="https://www.biostathandbook.com/onewayanova.html">
 * McDonald, J.H. 2014. Handbook of Biological Statistics (3rd ed.). Sparky House Publishing, Baltimore, Maryland.
 * One-way anova. pp 145-156.</a>
 * @since 1.1
 */
public final class OneWayAnova {
    /** Default instance. */
    private static final OneWayAnova DEFAULT = new OneWayAnova();

    /**
     * Result for the one-way ANOVA.
     *
     * <p>This class is immutable.
     */
    public static final class Result extends BaseSignificanceResult {
        /** Degrees of freedom in numerator (between groups). */
        private final int dfbg;
        /** Degrees of freedom in denominator (within groups). */
        private final long dfwg;
        /** Mean square between groups. */
        private final double msbg;
        /** Mean square within groups. */
        private final double mswg;
        /** nO value used to partition the variance. */
        private final double nO;

        /**
         * @param dfbg Degrees of freedom in numerator (between groups).
         * @param dfwg Degrees of freedom in denominator (within groups).
         * @param msbg Mean square between groups.
         * @param mswg Mean square within groups.
         * @param nO Factor for partitioning the variance.
         * @param f F statistic
         * @param p P-value.
         */
        Result(int dfbg, long dfwg, double msbg, double mswg, double nO, double f, double p) {
            super(f, p);
            this.dfbg = dfbg;
            this.dfwg = dfwg;
            this.msbg = msbg;
            this.mswg = mswg;
            this.nO = nO;
        }

        /**
         * Gets the degrees of freedom in the numerator (between groups).
         *
         * @return degrees of freedom between groups
         */
        int getDFBG() {
            return dfbg;
        }

        /**
         * Gets the degrees of freedom in the denominator (within groups).
         *
         * @return degrees of freedom within groups
         */
        long getDFWG() {
            return dfwg;
        }

        /**
         * Gets the mean square between groups.
         *
         * @return mean square between groups
         */
        public double getMSBG() {
            return msbg;
        }

        /**
         * Gets the mean square within groups.
         *
         * @return mean square within groups
         */
        public double getMSWG() {
            return mswg;
        }

        /**
         * Gets the variance component between groups.
         *
         * <p>The value is a partitioning of the variance.
         * It is the complement of {@link #getVCWG()}.
         *
         * <p>Partitioning the variance applies only to a model II
         * (random effects) one-way anova. This applies when the
         * groups are random samples from a larger set of groups;
         * partitioning the variance allows comparison of the
         * variation between groups to the variation within groups.
         *
         * <p>If the {@link #getMSBG() MSBG} is less than the
         * {@link #getMSWG() MSWG} this returns 0. Otherwise this
         * creates an estimate of the added variance component
         * between groups as:
         *
         * <p>\[ \text{between-group variance} = A = (\text{MS}_{\text{bg}} - \text{MS}_{\text{wg}}) / n_o \]
         *
         * <p>where \( n_o \) is a number close to, but usually less than,
         * the arithmetic mean of the sample size \(n_i\) of each
         * of the \( a \) groups:
         *
         * <p>\[ n_o = \frac{1}{a-1} \left( \sum_i{n_i} - \frac{\sum_i{n_i^2}}{\sum_i{n_i}} \right) \]
         *
         * <p>The added variance component among groups \( A \) is expressed
         * as a fraction of the total variance components \( A + B \) where
         * \( B \) is the {@link #getMSWG() MSWG}.
         *
         * @return variance component between groups (in [0, 1]).
         */
        public double getVCBG() {
            if (msbg <= mswg) {
                return 0;
            }
            // a is an estimate of the between-group variance
            final double a = (msbg - mswg) / nO;
            final double b = mswg;
            return a / (a + b);
        }

        /**
         * Gets the variance component within groups.
         *
         * <p>The value is a partitioning of the variance.
         * It is the complement of {@link #getVCBG()}. See
         * that method for details.
         *
         * @return variance component within groups (in [0, 1]).
         */
        public double getVCWG() {
            if (msbg <= mswg) {
                return 1;
            }
            final double a = (msbg - mswg) / nO;
            final double b = mswg;
            return b / (a + b);
        }
    }

    /** Private constructor. */
    private OneWayAnova() {
        // Do nothing
    }

    /**
     * Return an instance using the default options.
     *
     * @return default instance
     */
    public static OneWayAnova withDefaults() {
        return DEFAULT;
    }

    /**
     * Computes the F statistic for an ANOVA test for a collection of category data,
     * evaluating the null hypothesis that there is no difference among the means of
     * the data categories.
     *
     * <p>Special cases:
     * <ul>
     * <li>If the value in each category is the same (no variance within groups) but different
     * between groups, the f-value is {@link Double#POSITIVE_INFINITY infinity}.
     * <li>If the value in every group is the same (no variance within or between groups),
     * the f-value is {@link Double#NaN NaN}.
     * </ul>
     *
     * @param data Category summary data.
     * @return F statistic
     * @throws IllegalArgumentException if the number of categories is less than
     * two; a contained category does not have at least one value; or all
     * categories have only one value (zero degrees of freedom within groups)
     */
    public double statistic(Collection<double[]> data) {
        final double[] f = new double[1];
        aov(data, f);
        return f[0];
    }

    /**
     * Performs an ANOVA test for a collection of category data,
     * evaluating the null hypothesis that there is no difference among the means of
     * the data categories.
     *
     * <p>Special cases:
     * <ul>
     * <li>If the value in each category is the same (no variance within groups) but different
     * between groups, the f-value is {@link Double#POSITIVE_INFINITY infinity} and the p-value is zero.
     * <li>If the value in every group is the same (no variance within or between groups),
     * the f-value and p-value are {@link Double#NaN NaN}.
     * </ul>
     *
     * @param data Category summary data.
     * @return test result
     * @throws IllegalArgumentException if the number of categories is less than
     * two; a contained category does not have at least one value; or all
     * categories have only one value (zero degrees of freedom within groups)
     */
    public Result test(Collection<double[]> data) {
        return aov(data, null);
    }

    /**
     * Performs an ANOVA test for a collection of category data, evaluating the null
     * hypothesis that there is no difference among the means of the data categories.
     *
     * <p>This is a utility method to allow computation of the F statistic without
     * the p-value or partitioning of the variance. If the {@code statistic} is not null
     * the method will record the F statistic in the array and return null.
     *
     * @param data Category summary data.
     * @param statistic Result for the F statistic (or null).
     * @return test result (or null)
     * @throws IllegalArgumentException if the number of categories is less than two; a
     * contained category does not have at least one value; or all categories have only
     * one value (zero degrees of freedom within groups)
     */
    private static Result aov(Collection<double[]> data, double[] statistic) {
        Arguments.checkCategoriesRequiredSize(data.size(), 2);
        long n = 0;
        for (final double[] array : data) {
            n += array.length;
            Arguments.checkValuesRequiredSize(array.length, 1);
        }
        final long dfwg = n - data.size();
        if (dfwg == 0) {
            throw new InferenceException(InferenceException.ZERO, "Degrees of freedom within groups");
        }

        // wg = within group
        // bg = between group

        // F = Var(bg) / Var(wg)
        // Var = SS / df
        // SStotal = sum((x - u)^2) = sum(x^2) - sum(x)^2/n
        //         = SSwg + SSbg
        // Some cancellation of terms reduces the computation to 3 sums:
        // SSwg = [ sum(x^2) - sum(x)^2/n ] - [ sum_g { sum(sum(x^2) - sum(x)^2/n) } ]
        // SSbg = SStotal - SSwg
        //      = sum_g { sum(x)^2/n) } - sum(x)^2/n
        // SSwg = SStotal - SSbg
        //      = sum(x^2) - sum_g { sum(x)^2/n) }

        // Stabilize the computation by shifting all to a common mean of zero.
        // This minimise the magnitude of x^2 terms.
        // The terms sum(x)^2/n -> 0. Included them to capture the round-off.
        final double m = StatisticUtils.mean(data);
        final Sum sxx = Sum.create();
        final Sum sx = Sum.create();
        final Sum sg = Sum.create();
        // Track if each group had the same value
        boolean eachSame = true;
        for (final double[] array : data) {
            eachSame = eachSame && allMatch(array[0], array);
            final Sum s = Sum.create();
            for (final double v : array) {
                final double x = v - m;
                s.add(x);
                // sum(x)
                sx.add(x);
                // sum(x^2)
                sxx.add(x * x);
            }
            // Create the negative sum so we can subtract it via 'add'
            // -sum_g { sum(x)^2/n) }
            sg.add(-Math.pow(s.getAsDouble(), 2) / array.length);
        }

        // Note: SS terms should not be negative given:
        // SS = sum((x - u)^2)
        // This can happen due to floating-point error in sum(x^2) - sum(x)^2/n
        final double sswg = Math.max(0, sxx.add(sg).getAsDouble());
        // Flip the sign back
        final double ssbg = Math.max(0, -sg.add(Math.pow(sx.getAsDouble(), 2) / n).getAsDouble());
        final int dfbg = data.size() - 1;
        // Handle edge-cases:
        // Note: 0 / 0 -> NaN : x / 0 -> inf
        // These are documented results and should output p=NaN or 0.
        // This result will occur naturally.
        // However the SS totals may not be 0.0 so we correct these cases.
        final boolean allSame = eachSame && allMatch(data);
        final double msbg = allSame ? 0 : ssbg / dfbg;
        final double mswg = eachSame ? 0 : sswg / dfwg;
        final double f = msbg / mswg;
        if (statistic != null) {
            statistic[0] = f;
            return null;
        }
        final double p = FDistribution.of(dfbg, dfwg).survivalProbability(f);

        // Support partitioning the variance
        // ni = size of each of the groups
        // nO=(1/(a−1))*(sum(ni)−(sum(ni^2)/sum(ni))
        final double nO = (n - data.stream()
                .mapToDouble(x -> Math.pow(x.length, 2)).sum() / n) / dfbg;

        return new Result(dfbg, dfwg, msbg, mswg, nO, f, p);
    }

    /**
     * Return true if all values in the array match the specified value.
     *
     * @param v Value.
     * @param a Array.
     * @return true if all match
     */
    private static boolean allMatch(double v, double[] a) {
        for (final double w : a) {
            if (v != w) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if all values in the arrays match.
     *
     * <p>Assumes that there are at least two arrays and that each array has the same
     * value throughout. Thus only the first element in each array is checked.
     *
     * @param data Arrays.
     * @return true if all match
     */
    private static boolean allMatch(Collection<double[]> data) {
        final Iterator<double[]> iter = data.iterator();
        final double v = iter.next()[0];
        while (iter.hasNext()) {
            if (iter.next()[0] != v) {
                return false;
            }
        }
        return true;
    }
}
