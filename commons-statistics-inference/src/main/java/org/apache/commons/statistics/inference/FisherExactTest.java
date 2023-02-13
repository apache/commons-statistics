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

import java.util.Objects;
import org.apache.commons.statistics.distribution.HypergeometricDistribution;

/**
 * Implements Fisher's exact test.
 *
 * <p>Performs an exact test for the statistical significance of the association (contingency)
 * between two kinds of categorical classification.
 *
 * <p>Fisher's test applies in the case that the row sums and column sums are fixed in advance
 * and not random.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Fisher%27s_exact_test">Fisher's exact test (Wikipedia)</a>
 * @since 1.1
 */
public final class FisherExactTest {
    /** Two. */
    private static final int TWO = 2;
    /** Default instance. */
    private static final FisherExactTest DEFAULT = new FisherExactTest(AlternativeHypothesis.TWO_SIDED);

    /** Alternative hypothesis. */
    private final AlternativeHypothesis alternative;

    /**
     * @param alternative Alternative hypothesis.
     */
    private FisherExactTest(AlternativeHypothesis alternative) {
        this.alternative = alternative;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     * <li>{@link AlternativeHypothesis#TWO_SIDED}
     * </ul>
     *
     * @return default instance
     */
    public static FisherExactTest withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured alternative hypothesis.
     *
     * @param v Value.
     * @return an instance
     */
    public FisherExactTest with(AlternativeHypothesis v) {
        return new FisherExactTest(Objects.requireNonNull(v));
    }

    /**
     * Compute the prior odds ratio for the 2-by-2 contingency table. This is the
     * "sample" or "unconditional" maximum likelihood estimate. For a table of:
     *
     * <p>\[ \left[ {\begin{array}{cc}
     *         a &amp; b \\
     *         c &amp; d \\
     *       \end{array} } \right] \]
     *
     * <p>this is:
     *
     * <p>\[ r = \frac{a d}{b c} \]
     *
     * <p>Special cases:
     * <ul>
     * <li>If the denominator is zero, the value is {@link Double#POSITIVE_INFINITY infinity}.
     * <li>If a row or column sum is zero, the value is {@link Double#NaN NaN}.
     * </ul>
     *
     * <p>Note: This statistic is equal to the statistic computed by the SciPy function
     * {@code scipy.stats.fisher_exact}. It is different to the conditional maximum
     * likelihood estimate computed by R function {@code fisher.test}.
     *
     * @param table 2-by-2 contingency table.
     * @return odds ratio
     * @throws IllegalArgumentException if the {@code table} is not a 2-by-2 table; any
     * table entry is negative; or the sum of the table is 0 or larger than a 32-bit signed integer.
     * @see #with(AlternativeHypothesis)
     * @see #test(int[][])
     */
    public double statistic(int[][] table) {
        checkTable(table);
        final double a = table[0][0];
        final double b = table[0][1];
        final double c = table[1][0];
        final double d = table[1][1];
        return (a * d) / (b * c);
    }

    /**
     * Performs Fisher's exact test on the 2-by-2 contingency table.
     *
     * <p>The test statistic is equal to the prior odds ratio. This is the
     * "sample" or "unconditional" maximum likelihood estimate.
     *
     * <p>The test is defined by the {@link AlternativeHypothesis}.
     *
     * <p>For a table of [[a, b], [c, d]] the possible values of any table are conditioned
     * with the same marginals (row and column totals). In this case the possible values {@code x}
     * of the upper-left element {@code a} are {@code min(0, a - d) <= x <= a + min(b, c)}.
     * <ul>
     * <li>'two-sided': the odds ratio of the underlying population is not one; the p-value
     * is the probability that a random table has probability equal to or less than the input table.
     * <li>'greater': the odds ratio of the underlying population is greater than one; the p-value
     * is the probability that a random table has {@code x >= a}.
     * <li>'less': the odds ratio of the underlying population is less than one; the p-value
     * is the probability that a random table has {@code x <= a}.
     * </ul>
     *
     * @param table 2-by-2 contingency table.
     * @return test result
     * @throws IllegalArgumentException if the {@code table} is not a 2-by-2 table; any
     * table entry is negative; or the sum of the table is 0 or larger than a 32-bit signed integer.
     * @see #with(AlternativeHypothesis)
     * @see #statistic(int[][])
     */
    public SignificanceResult test(int[][] table) {
        checkTable(table);
        final int a = table[0][0];
        final int b = table[0][1];
        final int c = table[1][0];
        final int d = table[1][1];

        // Odd-ratio.
        final double statistic = ((double) a * d) / ((double) b * c);

        final int nn = a + b + c + d;
        final int k = a + b;
        final int n = a + c;

        // Note: The distribution validates the population size is > 0
        final HypergeometricDistribution distribution = HypergeometricDistribution.of(nn, k, n);
        double p;
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            p = distribution.survivalProbability(a - 1);
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            p = distribution.cumulativeProbability(a);
        } else {
            p = twoSidedTest(a, distribution);
        }
        return new BaseSignificanceResult(statistic, p);
    }

    /**
     * Returns the <i>observed significance level</i>, or p-value, associated with a
     * two-sided test about the observed value.
     *
     * @param k Observed value.
     * @param distribution Hypergeometric distribution.
     * @return p-value
     */
    private static double twoSidedTest(int k, HypergeometricDistribution distribution) {
        // Find all i where Pr(X = i) <= Pr(X = k) and sum them.
        // Exploit the known unimodal distribution to increase the
        // search speed. Note the search depends only on magnitude differences.
        // The current HypergeometricDistribution is faster using log probability
        // as it omits a call to Math.exp.

        // Use the mode as the point of largest probability.
        // The lower or upper mode is important for the search below.
        final int nn = distribution.getPopulationSize();
        final int kk = distribution.getNumberOfSuccesses();
        final int n = distribution.getSampleSize();
        final double v = ((double) n + 1) * ((double) kk + 1) / (nn + 2.0);
        final int m1 = (int) Math.ceil(v) - 1;
        final int m2 = (int) Math.floor(v);
        if (k < m1) {
            final double pk = distribution.logProbability(k);
            // Lower half = cdf(k)
            // Find upper half. As k < lower mode i should never
            // reach the lower mode based on the probability alone.
            // Bracket with the upper mode.
            final int i = Searches.searchDescending(m2, distribution.getSupportUpperBound(), pk,
                distribution::logProbability);
            return distribution.cumulativeProbability(k) +
                   distribution.survivalProbability(i - 1);
        } else if (k > m2) {
            final double pk = distribution.logProbability(k);
            // Upper half = sf(k - 1)
            // Find lower half. As k > upper mode i should never
            // reach the upper mode based on the probability alone.
            // Bracket with the lower mode.
            final int i = Searches.searchAscending(distribution.getSupportLowerBound(), m1, pk,
                distribution::logProbability);
            return distribution.cumulativeProbability(i) +
                   distribution.survivalProbability(k - 1);
        }
        // k == mode
        // Edge case where the sum of probabilities will be either
        // 1 or 1 - Pr(X = mode) where mode != k
        final double pk = distribution.probability(k);
        final double pm = distribution.probability(k == m1 ? m2 : m1);
        return pm > pk ? 1 - pm : 1;
    }

    /**
     * Check the input is a 2-by-2 contingency table.
     *
     * @param table Table.
     * @throws IllegalArgumentException if the {@code table} is not a 2-by-2 table; any
     * table entry is negative; or the sum is zero or is not an integer
     */
    private static void checkTable(int[][] table) {
        if (table.length != TWO || table[0].length != TWO || table[1].length != TWO) {
            throw new InferenceException("Require a 2-by-2 contingency table");
        }
        // Must all be positive
        final int a = table[0][0];
        final int b = table[0][1];
        final int c = table[1][0];
        final int d = table[1][1];
        // Bitwise OR combines the sign bit from all values
        Arguments.checkNonNegative(a | b | c | d);
        // Sum must be an integer
        final long sum = (long) a + b + c + d;
        if (sum > Integer.MAX_VALUE) {
            throw new InferenceException(InferenceException.X_GT_Y, sum, Integer.MAX_VALUE);
        }
        Arguments.checkStrictlyPositive((int) sum);
    }
}
