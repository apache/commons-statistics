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
package org.apache.commons.statistics.inference;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import org.apache.commons.numbers.combinatorics.LogBinomialCoefficient;
import org.apache.commons.statistics.inference.BrentOptimizer.PointValuePair;

/**
 * Implements an unconditioned exact test for a contingency table.
 *
 * <p>Performs an exact test for the statistical significance of the association (contingency)
 * between two kinds of categorical classification. A 2x2 contingency table is:
 *
 * <p>\[ \left[ {\begin{array}{cc}
 *         a &amp; b \\
 *         c &amp; d \\
 *       \end{array} } \right] \]
 *
 * <p>This test applies to the case of a 2x2 contingency table with one margin fixed. Note that
 * if both margins are fixed (the row sums and column sums are not random)
 * then Fisher's exact test can be applied.
 *
 * <p>This implementation fixes the column sums \( m = a + c \) and \( n = b + d \).
 * All possible tables can be created using \( 0 \le a \le m \) and \( 0 \le b \le n \).
 * The random values \( a \) and \( b \) follow a binomial distribution with probabilities
 * \( p_0 \) and \( p_1 \) such that \( a \sim B(m, p_0) \) and \( b \sim B(n, p_1) \).
 * The p-value of the 2x2 table is the product of two binomials:
 *
 * <p>\[ \begin{aligned}
 *       p &amp;= Pr(a; m, p_0) \times Pr(b; n, p_1) \\
 *         &amp;= \binom{m}{a} p_0^a (1-p_0)^{m-a} \times \binom{n}{b} p_1^b (1-p_1)^{n-b} \end{aligned} \]
 *
 * <p>For the binomial model, the null hypothesis is the two nuisance parameters are equal
 * \( p_0 = p_1 = \pi\), with \( \pi \) the probability for equal proportions, and the probability
 * of any single table is:
 *
 * <p>\[ p = \binom{m}{a} \binom{n}{b} \pi^{a+b} (1-\pi)^{m+n-a-b} \]
 *
 * <p>The p-value of the observed table is calculated by maximising the sum of the as or more
 * extreme tables over the domain of the nuisance parameter \( 0 \lt \pi \lt 1 \):
 *
 * <p>\[ p(a, b) = \sum_{i,j} \binom{m}{i} \binom{n}{j} \pi^{i+j} (1-\pi)^{m+n-i-j} \]
 *
 * <p>where table \( (i,j) \) is as or more extreme than the observed table \( (a, b) \). The test
 * can be configured to select more extreme tables using various {@linkplain Method methods}.
 *
 * <p>Note that the sum of the joint binomial distribution is a univariate function for
 * the nuisance parameter \( \pi \). This function may have many local maxima and the
 * search enumerates the range with a configured {@linkplain #withInitialPoints(int)
 * number of points}. The best candidates are optionally used as the start point for an
 * {@linkplain #withOptimize(boolean) optimized} search for a local maxima.
 *
 * <p>References:
 * <ol>
 * <li>
 * Barnard, G.A. (1947).
 * <a href="https://doi.org/10.1093/biomet/34.1-2.123">Significance tests for 2x2 tables.</a>
 * Biometrika, 34, Issue 1-2, 123–138.
 * <li>
 * Boschloo, R.D. (1970).
 * <a href="https://doi.org/10.1111/j.1467-9574.1970.tb00104.x">Raised conditional level of
 * significance for the 2 × 2-table when testing the equality of two probabilities.</a>
 * Statistica neerlandica, 24(1), 1–9.
 * <li>
 * Suisaa, A and Shuster, J.J. (1985).
 * <a href="https://doi.org/10.2307/2981892">Exact Unconditional Sample Sizes
 * for the 2 × 2 Binomial Trial.</a>
 * Journal of the Royal Statistical Society. Series A (General), 148(4), 317-327.
 * </ol>
 *
 * @see FisherExactTest
 * @see <a href="https://en.wikipedia.org/wiki/Boschloo%27s_test">Boschloo&#39;s test (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Barnard%27s_test">Barnard&#39;s test (Wikipedia)</a>
 * @since 1.1
 */
public final class UnconditionedExactTest {
    /**
     * Default instance.
     *
     * <p>SciPy's boschloo_exact and barnard_exact tests use 32 points in the interval [0,
     * 1) The R Exact package uses 100 in the interval [1e-5, 1-1e-5]. Barnards 1947 paper
     * describes the nuisance parameter in the open interval {@code 0 < pi < 1}. Here we
     * respect the open-interval for the initial candidates and ignore 0 and 1. The
     * initial bounds used are the same as the R Exact package. We closely match the inner
     * 31 points from SciPy by using 33 points by default.
     */
    private static final UnconditionedExactTest DEFAULT = new UnconditionedExactTest(
        AlternativeHypothesis.TWO_SIDED, Method.BOSCHLOO, 33, true);
    /** Lower bound for the enumerated interval. The upper bound is {@code 1 - lower}. */
    private static final double LOWER_BOUND = 1e-5;
    /** Relative epsilon for the Brent solver. This is limited for a univariate function
     * to approximately sqrt(eps) with eps = 2^-52. */
    private static final double SOLVER_RELATIVE_EPS = 1.4901161193847656E-8;
    /** Fraction of the increment (interval between enumerated points) to initialise the bracket
     * for the minima. Note the minima should lie between x +/- increment. The bracket should
     * search within this range. Set to 1/8 and so the initial point of the bracket is
     * approximately 1.61 * 1/8 = 0.2 of the increment away from initial points a or b. */
    private static final double INC_FRACTION = 0.125;
    /** Maximum number of candidate to optimize. This is a safety limit to avoid excess
     * optimization. Only candidates within a relative tolerance of the best candidate are
     * stored. If the number of candidates exceeds this value then many candidates have a
     * very similar p-value and the top candidates will be optimized. Using a value of 3
     * allows at least one other candidate to be optimized when there is two-fold
     * symmetry in the energy function. */
    private static final int MAX_CANDIDATES = 3;
    /** Relative distance of candidate minima from the lowest candidate. Used to exclude
     * poor candidates from optimization. */
    private static final double MINIMA_EPS = 0.02;
    /** The maximum number of tables. This is limited by the maximum number of indices that
     * can be maintained in memory. Potentially up to this number of tables must be tracked
     * during computation of the p-value for as or more extreme tables. The limit is set
     * using the same limit for maximum capacity as java.util.ArrayList. In practice any
     * table anywhere near this limit can be computed using an alternative such as a chi-squared
     * or g test. */
    private static final int MAX_TABLES = Integer.MAX_VALUE - 8;
    /** Error message text for zero column sums. */
    private static final String COLUMN_SUM = "Column sum";

    /** Alternative hypothesis. */
    private final AlternativeHypothesis alternative;
    /** Method to identify more extreme tables. */
    private final Method method;
    /** Number of initial points. */
    private final int points;
    /** Option to optimize the best initial point(s). */
    private final boolean optimize;

    /**
     * Define the method to determine the more extreme tables.
     *
     * @since 1.1
     */
    public enum Method {
        /**
         * Uses the test statistic from a Z-test using a pooled variance.
         *
         * <p>\[ T(X) = \frac{\hat{p}_0 - \hat{p}_1}{\sqrt{\hat{p}(1 - \hat{p}) (\frac{1}{m} + \frac{1}{n})}} \]
         *
         * <p>where \( \hat{p}_0 = a / m \), \( \hat{p}_1 = b / n \), and
         * \( \hat{p} = (a+b) / (m+n) \) are the estimators of \( p_0 \), \( p_1 \) and the
         * pooled probability \( p \) assuming \( p_0 = p_1 \).
         *
         * <p>The more extreme tables are identified using the {@link AlternativeHypothesis}:
         * <ul>
         * <li>greater: \( T(X) \ge T(X_0) \)
         * <li>less: \( T(X) \le T(X_0) \)
         * <li>two-sided: \( | T(X) | \ge | T(X_0) | \)
         * </ul>
         *
         * <p>The use of the Z statistic was suggested by Suissa and Shuster (1985).
         * This method is uniformly more powerful than Fisher's test for balanced designs
         * (\( m = n \)).
         */
        Z_POOLED,

        /**
         * Uses the test statistic from a Z-test using an unpooled variance.
         *
         * <p>\[ T(X) = \frac{\hat{p}_0 - \hat{p}_1}
         * {\sqrt{ \frac{\hat{p}_0(1 - \hat{p}_0)}{m} + \frac{\hat{p}_1(1 - \hat{p}_1)}{n}} } \]
         *
         * <p>where \( \hat{p}_0 = a / m \) and \( \hat{p}_1 = b / n \).
         *
         * <p>The more extreme tables are identified using the {@link AlternativeHypothesis} as
         * per the {@link #Z_POOLED} method.
         */
        Z_UNPOOLED,

        /**
         * Uses the p-value from Fisher's exact test. This is also known as Boschloo's test.
         *
         * <p>The p-value for Fisher's test is computed using using the
         * {@link AlternativeHypothesis}. The more extreme tables are identified using
         * \( p(X) \le p(X_0) \).
         *
         * <p>This method is always uniformly more powerful than Fisher's test.
         *
         * @see FisherExactTest
         */
        BOSCHLOO;
    }

    /**
     * Result for the unconditioned exact test.
     *
     * <p>This class is immutable.
     *
     * @since 1.1
     */
    public static final class Result extends BaseSignificanceResult {
        /** Nuisance parameter. */
        private final double pi;

        /**
         * Create an instance where all tables are more extreme, i.e. the p-value
         * is 1.0.
         *
         * @param statistic Test statistic.
         */
        Result(double statistic) {
            super(statistic, 1);
            this.pi = 0.5;
        }

        /**
         * @param statistic Test statistic.
         * @param pi Nuisance parameter.
         * @param p Result p-value.
         */
        Result(double statistic, double pi, double p) {
            super(statistic, p);
            this.pi = pi;
        }

        /**
         * {@inheritDoc}
         *
         * <p>The value of the statistic is dependent on the {@linkplain Method method}
         * used to determine the more extreme tables.
         */
        @Override
        public double getStatistic() {
            // Note: This method is here for documentation
            return super.getStatistic();
        }

        /**
         * Gets the nuisance parameter that maximised the probability sum of the as or more
         * extreme tables.
         *
         * @return the nuisance parameter.
         */
        public double getNuisanceParameter() {
            return pi;
        }
    }

    /**
     * An expandable list of (x,y) values. This allows tracking 2D positions stored as
     * a single index.
     */
    private static class XYList {
        /** The maximum size of array to allocate. */
        private final int max;
        /** Width, or maximum x value (exclusive). */
        private final int width;

        /** The size of the list. */
        private int size;
        /** The list data. */
        private int[] data = new int[10];

        /**
         * Create an instance. It is assumed that (maxx+1)*(maxy+1) does not exceed the
         * capacity of an array.
         *
         * @param maxx Maximum x-value (inclusive).
         * @param maxy Maximum y-value (inclusive).
         */
        XYList(int maxx, int maxy) {
            this.width = maxx + 1;
            this.max = width * (maxy + 1);
        }

        /**
         * Gets the width.
         * (x, y) values are stored using y * width + x.
         *
         * @return the width
         */
        int getWidth() {
            return width;
        }

        /**
         * Gets the maximum X value (inclusive).
         *
         * @return the max X
         */
        int getMaxX() {
            return width - 1;
        }

        /**
         * Gets the maximum Y value (inclusive).
         *
         * @return the max Y
         */
        int getMaxY() {
            return max / width - 1;
        }

        /**
         * Adds the value to the list.
         *
         * @param x X value.
         * @param y Y value.
         */
        void add(int x, int y) {
            if (size == data.length) {
                // Overflow safe doubling of the current size.
                data = Arrays.copyOf(data, (int) Math.min(max, size * 2L));
            }
            data[size++] = width * y + x;
        }

        /**
         * Gets the 2D index at the specified {@code index}.
         * The index is y * width + x:
         * <pre>
         * x = index % width
         * y = index / width
         * </pre>
         *
         * @param index Element index.
         * @return the 2D index
         */
        int get(int index) {
            return data[index];
        }

        /**
         * Gets the number of elements in the list.
         *
         * @return the size
         */
        int size() {
            return size;
        }

        /**
         * Checks if the list size is zero.
         *
         * @return true if empty
         */
        boolean isEmpty() {
            return size == 0;
        }

        /**
         * Checks if the list is the maximum capacity.
         *
         * @return true if full
         */
        boolean isFull() {
            return size == max;
        }
    }

    /**
     * A container of (key,value) pairs to store candidate minima. Encapsulates the
     * logic of storing multiple initial search points for optimization.
     *
     * <p>Stores all pairs within a relative tolerance of the lowest minima up to a set
     * capacity. When at capacity the worst candidate is replaced by addition of a
     * better candidate.
     *
     * <p>Special handling is provided to store only a single NaN value if no non-NaN
     * values have been observed. This prevents storing a large number of NaN
     * candidates.
     */
    static class Candidates {
        /** The maximum size of array to allocate. */
        private final int max;
        /** Relative distance from lowest candidate. */
        private final double eps;
        /** Candidate (key,value) pairs. */
        private double[][] data;
        /** Current size of the list. */
        private int size;
        /** Current minimum. */
        private double min = Double.POSITIVE_INFINITY;
        /** Current threshold for inclusion. */
        private double threshold = Double.POSITIVE_INFINITY;

        /**
         * Create an instance.
         *
         * @param max Maximum number of allowed candidates (limited to at least 1).
         * @param eps Relative distance of candidate minima from the lowest candidate
         * (assumed to be positive and finite).
         */
        Candidates(int max, double eps) {
            this.max = Math.max(1, max);
            this.eps = eps;
            // Create the initial storage
            data = new double[Math.min(this.max, 4)][];
        }

        /**
         * Adds the (key, value) pair.
         *
         * @param k Key.
         * @param v Value.
         */
        void add(double k, double v) {
            // Store only a single NaN
            if (Double.isNaN(v)) {
                if (size == 0) {
                    // No requirement to check capacity
                    data[size++] = new double[] {k, v};
                }
                return;
            }
            // Here values are non-NaN.
            // If higher then do not store.
            if (v > threshold) {
                return;
            }
            // Check if lower than the current minima.
            if (v < min) {
                min = v;
                // Get new threshold
                threshold = v + Math.abs(v) * eps;
                // Remove existing entries above the threshold
                int s = 0;
                for (int i = 0; i < size; i++) {
                    // This will filter NaN values
                    if (data[i][1] <= threshold) {
                        data[s++] = data[i];
                    }
                }
                size = s;
                // Caution: This does not clear stale data
                // by setting all values in [newSize, oldSize) = null
            }
            addPair(k, v);
        }

        /**
         * Add the (key, value) pair to the data.
         * It is assumed the data satisfy the conditions for addition.
         *
         * @param k Key.
         * @param v Value.
         */
        private void addPair(double k, double v) {
            if (size == data.length) {
                if (size == max) {
                    // At capacity.
                    replaceWorst(k, v);
                    return;
                }
                // Expand
                data = Arrays.copyOfRange(data, 0, (int) Math.min(max, size * 2L));
            }
            data[size++] = new double[] {k, v};
        }

        /**
         * Replace the worst candidate.
         *
         * @param k Key.
         * @param v Value.
         */
        private void replaceWorst(double k, double v) {
            // Note: This only occurs when NaN values have been removed by addition
            // of non-NaN values.
            double[] worst = data[0];
            for (int i = 1; i < size; i++) {
                if (worst[1] < data[i][1]) {
                    worst = data[i];
                }
            }
            worst[0] = k;
            worst[1] = v;
        }

        /**
         * Return the minimum (key,value) pair.
         *
         * @return the minimum (or null)
         */
        double[] getMinimum() {
            // This will handle size=0 as data[0] will be null
            double[] best = data[0];
            for (int i = 1; i < size; i++) {
                if (best[1] > data[i][1]) {
                    best = data[i];
                }
            }
            return best;
        }

        /**
         * Perform the given action for each (key, value) pair.
         *
         * @param action Action.
         */
        void forEach(Consumer<double[]> action) {
            for (int i = 0; i < size; i++) {
                action.accept(data[i]);
            }
        }
    }

    /**
     * Compute the statistic for Boschloo's test.
     */
    @FunctionalInterface
    private interface BoschlooStatistic {
        /**
         * Compute Fisher's p-value for the 2x2 contingency table with the observed
         * value {@code x} in position [0][0]. Note that the table margins are fixed
         * and are defined by the population size, number of successes and sample
         * size of the specified hypergeometric distribution.
         *
         * @param dist Hypergeometric distribution.
         * @param x Value.
         * @return Fisher's p-value
         */
        double value(Hypergeom dist, int x);
    }

    /**
     * @param alternative Alternative hypothesis.
     * @param method Method to identify more extreme tables.
     * @param points Number of initial points.
     * @param optimize Option to optimize the best initial point(s).
     */
    private UnconditionedExactTest(AlternativeHypothesis alternative,
                                   Method method,
                                   int points,
                                   boolean optimize) {
        this.alternative = alternative;
        this.method = method;
        this.points = points;
        this.optimize = optimize;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     * <li>{@link AlternativeHypothesis#TWO_SIDED}
     * <li>{@link Method#BOSCHLOO}
     * <li>{@linkplain #withInitialPoints(int) points = 33}
     * <li>{@linkplain #withOptimize(boolean) optimize = true}
     * </ul>
     *
     * @return default instance
     */
    public static UnconditionedExactTest withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured alternative hypothesis.
     *
     * @param v Value.
     * @return an instance
     */
    public UnconditionedExactTest with(AlternativeHypothesis v) {
        return new UnconditionedExactTest(Objects.requireNonNull(v), method, points, optimize);
    }

    /**
     * Return an instance with the configured method.
     *
     * @param v Value.
     * @return an instance
     */
    public UnconditionedExactTest with(Method v) {
        return new UnconditionedExactTest(alternative, Objects.requireNonNull(v), points, optimize);
    }

    /**
     * Return an instance with the configured number of initial points.
     *
     * <p>The search for the nuisance parameter will use \( v \) points in the open interval
     * \( (0, 1) \). The interval is evaluated by including start and end points approximately
     * equal to 0 and 1. Additional internal points are enumerated using increments of
     * approximately \( \frac{1}{v-1} \). The minimum number of points is 2. Increasing the
     * number of points increases the precision of the search at the cost of performance.
     *
     * <p>To approximately double the number of points so that all existing points are included
     * and additional points half-way between them are sampled requires using {@code 2p - 1}
     * where {@code p} is the existing number of points.
     *
     * @param v Value.
     * @return an instance
     * @throws IllegalArgumentException if the value is {@code < 2}.
     */
    public UnconditionedExactTest withInitialPoints(int v) {
        if (v <= 1) {
            throw new InferenceException(InferenceException.X_LT_Y, v, 2);
        }
        return new UnconditionedExactTest(alternative, method, v, optimize);
    }

    /**
     * Return an instance with the configured optimization of initial search points.
     *
     * <p>If enabled then the initial point(s) with the highest probability is/are used as the start
     * for an optimization to find a local maxima.
     *
     * @param v Value.
     * @return an instance
     * @see #withInitialPoints(int)
     */
    public UnconditionedExactTest withOptimize(boolean v) {
        return new UnconditionedExactTest(alternative, method, points, v);
    }

    /**
     * Compute the statistic for the unconditioned exact test. The statistic returned
     * depends on the configured {@linkplain Method method}.
     *
     * @param table 2-by-2 contingency table.
     * @return test statistic
     * @throws IllegalArgumentException if the {@code table} is not a 2-by-2 table; any
     * table entry is negative; any column sum is zero; the table sum is zero or not an
     * integer; or the number of possible tables exceeds the maximum array capacity.
     * @see #with(Method)
     * @see #test(int[][])
     */
    public double statistic(int[][] table) {
        checkTable(table);
        final int a = table[0][0];
        final int b = table[0][1];
        final int c = table[1][0];
        final int d = table[1][1];
        final int m = a + c;
        final int n = b + d;
        // Exhaustive switch statement
        switch (method) {
        case Z_POOLED:
            return statisticZ(a, b, m, n, true);
        case Z_UNPOOLED:
            return statisticZ(a, b, m, n, false);
        case BOSCHLOO:
            return statisticBoschloo(a, b, m, n);
        }
        throw new IllegalStateException(String.valueOf(method));
    }

    /**
     * Performs an unconditioned exact test on the 2-by-2 contingency table. The statistic and
     * p-value returned depends on the configured {@linkplain Method method} and
     * {@linkplain AlternativeHypothesis alternative hypothesis}.
     *
     * <p>The search for the nuisance parameter that maximises the p-value can be configured to:
     * start with a number of {@linkplain #withInitialPoints(int) initial points}; and
     * {@linkplain #withOptimize(boolean) optimize} the best points.
     *
     * @param table 2-by-2 contingency table.
     * @return test result
     * @throws IllegalArgumentException if the {@code table} is not a 2-by-2 table; any
     * table entry is negative; any column sum is zero; the table sum is zero or not an
     * integer; or the number of possible tables exceeds the maximum array capacity.
     * @see #with(Method)
     * @see #with(AlternativeHypothesis)
     * @see #statistic(int[][])
     */
    public Result test(int[][] table) {
        checkTable(table);
        final int a = table[0][0];
        final int b = table[0][1];
        final int c = table[1][0];
        final int d = table[1][1];
        final int m = a + c;
        final int n = b + d;

        // Used to track more extreme tables
        final XYList tableList = new XYList(m, n);

        final double statistic = findExtremeTables(a, b, tableList);
        if (tableList.isEmpty() || tableList.isFull()) {
            // All possible tables are more extreme, e.g. a two-sided test where the
            // z-statistic is zero.
            return new Result(statistic);
        }
        final double[] opt = computePValue(tableList);

        return new Result(statistic, opt[0], opt[1]);
    }

    /**
     * Find all tables that are as or more extreme than the observed table.
     *
     * <p>If the list of tables is full then all tables are more extreme.
     * Some configurations can detect this without performing a search
     * and in this case the list of tables is returned as empty.
     *
     * @param a Observed value for a.
     * @param b Observed value for b.
     * @param tableList List to track more extreme tables.
     * @return the test statistic
     */
    private double findExtremeTables(int a, int b, XYList tableList) {
        final int m = tableList.getMaxX();
        final int n = tableList.getMaxY();
        // Exhaustive switch statement
        switch (method) {
        case Z_POOLED:
            return findExtremeTablesZ(a, b, m, n, true, tableList);
        case Z_UNPOOLED:
            return findExtremeTablesZ(a, b, m, n, false, tableList);
        case BOSCHLOO:
            return findExtremeTablesBoschloo(a, b, m, n, tableList);
        }
        throw new IllegalStateException(String.valueOf(method));
    }

    /**
     * Compute the statistic from a Z-test.
     *
     * @param a Observed value for a.
     * @param b Observed value for b.
     * @param m Column sum m.
     * @param n Column sum n.
     * @param pooled true to use a pooled variance.
     * @return z
     */
    private static double statisticZ(int a, int b, int m, int n, boolean pooled) {
        final double p0 = (double) a / m;
        final double p1 = (double) b / n;
        // Avoid NaN generation 0 / 0 when the variance is 0
        if (p0 != p1) {
            final double variance;
            if (pooled) {
                // Integer sums will not overflow
                final double p = (double) (a + b) / (m + n);
                variance = p * (1 - p) * (1.0 / m + 1.0 / n);
            } else {
                variance = p0 * (1 - p0) / m + p1 * (1 - p1) / n;
            }
            return (p0 - p1) / Math.sqrt(variance);
        }
        return 0;
    }

    /**
     * Find all tables that are as or more extreme than the observed table using the Z statistic.
     *
     * @param a Observed value for a.
     * @param b Observed value for b.
     * @param m Column sum m.
     * @param n Column sum n.
     * @param pooled true to use a pooled variance.
     * @param tableList List to track more extreme tables.
     * @return observed z
     */
    private double findExtremeTablesZ(int a, int b, int m, int n, boolean pooled, XYList tableList) {
        final double statistic = statisticZ(a, b, m, n, pooled);
        // Identify more extreme tables using the alternate hypothesis
        final DoublePredicate test;
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            test = z -> z >= statistic;
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            test = z -> z <= statistic;
        } else {
            // two-sided
            if (statistic == 0) {
                // Early exit: all tables are as extreme
                return 0;
            }
            final double za = Math.abs(statistic);
            test = z -> Math.abs(z) >= za;
        }
        // Precompute factors
        final double mn = (double) m + n;
        final double norm = 1.0 / m + 1.0 / n;
        double z;
        // Process all possible tables
        for (int i = 0; i <= m; i++) {
            final double p0 = (double) i / m;
            final double vp0 = p0 * (1 - p0) / m;
            for (int j = 0; j <= n; j++) {
                final double p1 = (double) j / n;
                // Avoid NaN generation 0 / 0 when the variance is 0
                if (p0 == p1) {
                    z = 0;
                } else {
                    final double variance;
                    if (pooled) {
                        // Integer sums will not overflow
                        final double p = (i + j) / mn;
                        variance = p * (1 - p) * norm;
                    } else {
                        variance = vp0 + p1 * (1 - p1) / n;
                    }
                    z = (p0 - p1) / Math.sqrt(variance);
                }
                if (test.test(z)) {
                    tableList.add(i, j);
                }
            }
        }
        return statistic;
    }

    /**
     * Compute the statistic using Fisher's p-value (also known as Boschloo's test).
     *
     * @param a Observed value for a.
     * @param b Observed value for b.
     * @param m Column sum m.
     * @param n Column sum n.
     * @return p-value
     */
    private double statisticBoschloo(int a, int b, int m, int n) {
        final int nn = m + n;
        final int k = a + b;
        // Re-use the cached Hypergeometric implementation to allow the value
        // to be identical for the statistic and test methods.
        final Hypergeom dist = new Hypergeom(nn, k, m);
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            return dist.sf(a - 1);
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            return dist.cdf(a);
        }
        // two-sided: Find all i where Pr(X = i) <= Pr(X = a) and sum them.
        return statisticBoschlooTwoSided(dist, a);
    }

    /**
     * Compute the two-sided statistic using Fisher's p-value (also known as Boschloo's test).
     *
     * @param distribution Hypergeometric distribution.
     * @param k Observed value.
     * @return p-value
     */
    private static double statisticBoschlooTwoSided(Hypergeom distribution, int k) {
        // two-sided: Find all i where Pr(X = i) <= Pr(X = k) and sum them.
        // Logic is the same as FisherExactTest but using the probability (PMF), which
        // is cached, rather than the logProbability.
        final double pk = distribution.pmf(k);

        final int m1 = distribution.getLowerMode();
        final int m2 = distribution.getUpperMode();
        if (k < m1) {
            // Lower half = cdf(k)
            // Find upper half. As k < lower mode i should never
            // reach the lower mode based on the probability alone.
            // Bracket with the upper mode.
            final int i = Searches.searchDescending(m2, distribution.getSupportUpperBound(), pk,
                distribution::pmf);
            return distribution.cdf(k) +
                   distribution.sf(i - 1);
        } else if (k > m2) {
            // Upper half = sf(k - 1)
            // Find lower half. As k > upper mode i should never
            // reach the upper mode based on the probability alone.
            // Bracket with the lower mode.
            final int i = Searches.searchAscending(distribution.getSupportLowerBound(), m1, pk,
                distribution::pmf);
            return distribution.cdf(i) +
                   distribution.sf(k - 1);
        }
        // k == mode
        // Edge case where the sum of probabilities will be either
        // 1 or 1 - Pr(X = mode) where mode != k
        final double pm = distribution.pmf(k == m1 ? m2 : m1);
        return pm > pk ? 1 - pm : 1;
    }

    /**
     * Find all tables that are as or more extreme than the observed table using the
     * Fisher's p-value as the statistic (also known as Boschloo's test).
     *
     * @param a Observed value for a.
     * @param b Observed value for b.
     * @param m Column sum m.
     * @param n Column sum n.
     * @param tableList List to track more extreme tables.
     * @return observed p-value
     */
    private double findExtremeTablesBoschloo(int a, int b, int m, int n, XYList tableList) {
        final double statistic = statisticBoschloo(a, b, m, n);

        // Function to compute the statistic
        final BoschlooStatistic func;
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            func = (dist, x) -> dist.sf(x - 1);
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            func = Hypergeom::cdf;
        } else {
            func = UnconditionedExactTest::statisticBoschlooTwoSided;
        }

        // All tables are: 0 <= i <= m  by  0 <= j <= n
        // Diagonal (upper-left to lower-right) strips of the possible
        // tables use the same hypergeometric distribution
        // (i.e. i+j == number of successes). To enumerate all requires
        // using the full range of all distributions: 0 <= i+j <= m+n.
        // Note the column sum m is fixed.
        final int mn = m + n;
        for (int k = 0; k <= mn; k++) {
            final Hypergeom dist = new Hypergeom(mn, k, m);
            final int lo = dist.getSupportLowerBound();
            final int hi = dist.getSupportUpperBound();
            for (int i = lo; i <= hi; i++) {
                if (func.value(dist, i) <= statistic) {
                    // j = k - i
                    tableList.add(i, k - i);
                }
            }
        }
        return statistic;
    }

    /**
     * Compute the nuisance parameter and p-value for the binomial model given the list
     * of possible tables.
     *
     * <p>The current method enumerates an initial set of points and stores local
     * extrema as candidates. Any candidate within 2% of the best is optionally
     * optimized; this is limited to the top 3 candidates. These settings
     * could be exposed as configurable options. Currently only the choice to optimize
     * or not is exposed.
     *
     * @param tableList List of tables.
     * @return [nuisance parameter, p-value]
     */
    private double[] computePValue(XYList tableList) {
        final DoubleUnaryOperator func = createBinomialModel(tableList);

        // Enumerate the range [LOWER, 1-LOWER] and save the best points for optimization
        final Candidates minima = new Candidates(MAX_CANDIDATES, MINIMA_EPS);
        final int n = points - 1;
        final double inc = (1.0 - 2 * LOWER_BOUND) / n;
        // Moving window of 3 values to identify minima.
        // px holds the position of the previous evaluated point.
        double v2 = 0;
        double v3 = func.applyAsDouble(LOWER_BOUND);
        double px = LOWER_BOUND;
        for (int i = 1; i < n; i++) {
            final double x = LOWER_BOUND + i * inc;
            final double v1 = v2;
            v2 = v3;
            v3 = func.applyAsDouble(x);
            addCandidate(minima, v1, v2, v3, px);
            px = x;
        }
        // Add the upper bound
        final double x = 1 - LOWER_BOUND;
        final double vn = func.applyAsDouble(x);
        addCandidate(minima, v2, v3, vn, px);
        addCandidate(minima, v3, vn, 0, x);

        final double[] min = minima.getMinimum();

        // Optionally optimize the best point(s) (if not already optimal)
        if (optimize && min[1] > -1) {
            final BrentOptimizer opt = new BrentOptimizer(SOLVER_RELATIVE_EPS, Double.MIN_VALUE);
            final BracketFinder bf = new BracketFinder();
            minima.forEach(candidate -> {
                double a = candidate[0];
                final double fa;
                // Attempt to bracket the minima. Use an initial second point placed relative to
                // the size of the interval: [x - increment, x + increment].
                // if a < 0.5 then add a small delta ; otherwise subtract the delta.
                final double b = a - Math.copySign(inc * INC_FRACTION, a - 0.5);
                if (bf.search(func, a, b, 0, 1)) {
                    // The bracket a < b < c must have f(b) < min(f(a), f(b))
                    final PointValuePair p = opt.optimize(func, bf.getLo(), bf.getHi(), bf.getMid(), bf.getFMid());
                    a = p.getPoint();
                    fa = p.getValue();
                } else {
                    // Mid-point is at one of the bounds (i.e. is 0 or 1)
                    a = bf.getMid();
                    fa = bf.getFMid();
                }
                if (fa < min[1]) {
                    min[0] = a;
                    min[1] = fa;
                }
            });
        }
        // Reverse the sign of the p-value to create a maximum.
        // Note that due to the summation the p-value can be above 1 so we clip the final result.
        // Note: Apply max then reverse sign. This will pass through spurious NaN values if
        // the p-value computation produced all NaNs.
        min[1] = -Math.max(-1, min[1]);
        return min;
    }

    /**
     * Creates the binomial model p-value function for the nuisance parameter.
     * Note: This function computes the negative p-value so is suitable for
     * optimization by a search for a minimum.
     *
     * @param tableList List of tables.
     * @return the function
     */
    private static DoubleUnaryOperator createBinomialModel(XYList tableList) {
        final int m = tableList.getMaxX();
        final int n = tableList.getMaxY();
        final int mn = m + n;
        // Compute the probability using logs
        final double[] c = new double[tableList.size()];
        final int[] ij = new int[tableList.size()];
        final int width = tableList.getWidth();

        // Compute the log binomial dynamically for a small number of values
        final IntToDoubleFunction binomM;
        final IntToDoubleFunction binomN;
        if (tableList.size() < mn) {
            binomM = k -> LogBinomialCoefficient.value(m, k);
            binomN = k -> LogBinomialCoefficient.value(n, k);
        } else {
            // Pre-compute all values
            binomM = createLogBinomialCoefficients(m);
            binomN = m == n ? binomM : createLogBinomialCoefficients(n);
        }

        // Handle special cases i+j == 0 and i+j == m+n.
        // These will occur only once, if at all. Mark if they occur.
        int flag = 0;
        int j = 0;
        for (int i = 0; i < c.length; i++) {
            final int index = tableList.get(i);
            final int x = index % width;
            final int y = index / width;
            final int xy = x + y;
            if (xy == 0) {
                flag |= 1;
            } else if (xy == mn) {
                flag |= 2;
            } else {
                ij[j] = xy;
                c[j] = binomM.applyAsDouble(x) + binomN.applyAsDouble(y);
                j++;
            }
        }

        final int size = j;
        final boolean ij0 = (flag & 1) != 0;
        final boolean ijmn = (flag & 2) != 0;
        return pi -> {
            final double logp = Math.log(pi);
            final double log1mp = Math.log1p(-pi);
            double sum = 0;
            for (int i = 0; i < size; i++) {
                // binom(m, i) * binom(n, j) * pi^(i+j) * (1-pi)^(m+n-i-j)
                sum += Math.exp(ij[i] * logp + (mn - ij[i]) * log1mp + c[i]);
            }
            // Add the simplified terms where the binomial is 1.0 and one power is x^0 == 1.0.
            // This avoids 0 * log(x) generating NaN when x is 0 in the case where pi was 0 or 1.
            // Reuse exp (not pow) to support pi approaching 0 or 1.
            if (ij0) {
                // pow(1-pi, mn)
                sum += Math.exp(mn * log1mp);
            }
            if (ijmn) {
                // pow(pi, mn)
                sum += Math.exp(mn * logp);
            }
            // The optimizer minimises the function so this returns -p.
            return -sum;
        };
    }

    /**
     * Create the natural logarithm of the binomial coefficient for all {@code k = [0, n]}.
     *
     * @param n Limit N.
     * @return ln binom(n, k)
     */
    private static IntToDoubleFunction createLogBinomialCoefficients(int n) {
        final double[] binom = new double[n + 1];
        // Exploit symmetry.
        // ignore: binom(n, 0) == binom(n, n) == 1
        int j = n - 1;
        for (int i = 1; i <= j; i++, j--) {
            binom[i] = LogBinomialCoefficient.value(n, i);
            binom[j] = binom[i];
        }
        return k -> binom[k];
    }

    /**
     * Add point 2 to the list of minima if neither neighbour value is lower.
     * <pre>
     * !(v1 < v2 || v3 < v2)
     * </pre>
     *
     * @param minima Candidate minima.
     * @param v1 First point function value.
     * @param v2 Second point function value.
     * @param v3 Third point function value.
     * @param x2 Second point.
     */
    private void addCandidate(Candidates minima, double v1, double v2, double v3, double x2) {
        final double min = v1 < v3 ? v1 : v3;
        if (min < v2) {
            // Lower neighbour(s)
            return;
        }
        // Add the candidate. This could be NaN but the candidate list handles this by storing
        // NaN only when no non-NaN values have been observed.
        minima.add(x2, v2);
    }

    /**
     * Check the input is a 2-by-2 contingency table.
     *
     * @param table Contingency table.
     * @throws IllegalArgumentException if the {@code table} is not a 2-by-2 table; any
     * table entry is negative; any column sum is zero; the table sum is zero or not an
     * integer; or the number of possible tables exceeds the maximum array capacity.
     */
    private static void checkTable(int[][] table) {
        Arguments.checkTable(table);
        // Must all be positive
        final int a = table[0][0];
        final int c = table[1][0];
        // checkTable has validated the total sum is < 2^31
        final int m = a + c;
        if (m == 0) {
            throw new InferenceException(InferenceException.ZERO_AT, COLUMN_SUM, 0);
        }
        final int b = table[0][1];
        final int d = table[1][1];
        final int n = b + d;
        if (n == 0) {
            throw new InferenceException(InferenceException.ZERO_AT, COLUMN_SUM, 1);
        }
        // Total possible tables must be a size we can track in an array (to compute the p-value)
        final long size = (m + 1L) * (n + 1L);
        if (size > MAX_TABLES) {
            throw new InferenceException(InferenceException.X_GT_Y, size, MAX_TABLES);
        }
    }
}
