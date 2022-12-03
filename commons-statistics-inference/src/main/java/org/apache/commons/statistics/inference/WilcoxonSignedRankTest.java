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
import java.util.EnumSet;
import java.util.Objects;
import org.apache.commons.numbers.core.Sum;
import org.apache.commons.statistics.distribution.NormalDistribution;
import org.apache.commons.statistics.ranking.NaNStrategy;
import org.apache.commons.statistics.ranking.NaturalRanking;
import org.apache.commons.statistics.ranking.RankingAlgorithm;
import org.apache.commons.statistics.ranking.TiesStrategy;

/**
 * Implements the Wilcoxon signed-rank test.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Wilcoxon_signed-rank_test">Wilcoxon signed-rank test (Wikipedia)</a>
 * @since 1.1
 */
public final class WilcoxonSignedRankTest {
    /** Limit on sample size for the exact p-value computation. */
    private static final int EXACT_LIMIT = 1023;
    /** Limit on sample size for the exact p-value computation for the auto mode. */
    private static final int AUTO_LIMIT = 50;
    /** Ranking instance. */
    private static final RankingAlgorithm RANKING = new NaturalRanking(NaNStrategy.FAILED, TiesStrategy.AVERAGE);
    /** Default instance. */
    private static final WilcoxonSignedRankTest DEFAULT = new WilcoxonSignedRankTest(
        AlternativeHypothesis.TWO_SIDED, PValueMethod.AUTO, true, 0);

    /** Alternative hypothesis. */
    private final AlternativeHypothesis alternative;
    /** Method to compute the p-value. */
    private final PValueMethod pValueMethod;
    /** Perform continuity correction. */
    private final boolean continuityCorrection;
    /** Expected location shift. */
    private final double mu;

    /**
     * Result for the Wilcoxon signed-rank test.
     *
     * <p>This class is immutable.
     */
    public static final class Result extends BaseSignificanceResult {
        /** Flag indicating the data had tied values. */
        private final boolean tiedValues;
        /** Flag indicating the data had zero values. */
        private final boolean zeroValues;

        /**
         * Create an instance.
         *
         * @param statistic Test statistic.
         * @param tiedValues Flag indicating the data had tied values.
         * @param zeroValues Flag indicating the data had zero values.
         * @param p Result p-value.
         */
        Result(double statistic, boolean tiedValues, boolean zeroValues, double p) {
            super(statistic, p);
            this.tiedValues = tiedValues;
            this.zeroValues = zeroValues;
        }

        /**
         * Return {@code true} if the data had tied values.
         *
         * <p>Note: The exact computation cannot be used when there are tied values.
         *
         * @return {@code true} if there were tied values
         */
        public boolean hasTiedValues() {
            return tiedValues;
        }

        /**
         * Return {@code true} if the data had zero values.
         *
         * <p>Note: The exact computation cannot be used when there are zero values.
         *
         * @return {@code true} if there were zero values
         */
        public boolean hasZeroValues() {
            return zeroValues;
        }
    }

    /**
     * @param alternative Alternative hypothesis.
     * @param method P-value method.
     * @param continuityCorrection true to perform continuity correction.
     * @param mu Expected location shift.
     */
    private WilcoxonSignedRankTest(AlternativeHypothesis alternative, PValueMethod method,
        boolean continuityCorrection, double mu) {
        this.alternative = alternative;
        this.pValueMethod = method;
        this.continuityCorrection = continuityCorrection;
        this.mu = mu;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     * <li>{@link AlternativeHypothesis#TWO_SIDED}
     * <li>{@link PValueMethod#AUTO}
     * <li>{@link ContinuityCorrection#ENABLED}
     * <li>{@link #withMu(double) mu = 0}
     * </ul>
     *
     * @return default instance
     */
    public static WilcoxonSignedRankTest withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured alternative hypothesis.
     *
     * @param v Value.
     * @return an instance
     */
    public WilcoxonSignedRankTest with(AlternativeHypothesis v) {
        return new WilcoxonSignedRankTest(Objects.requireNonNull(v), pValueMethod, continuityCorrection, mu);
    }

    /**
     * Return an instance with the configured p-value method.
     *
     * @param v Value.
     * @return an instance
     * @throws IllegalArgumentException if the value is not in the allowed options or is null
     */
    public WilcoxonSignedRankTest with(PValueMethod v) {
        return new WilcoxonSignedRankTest(alternative,
            Arguments.checkOption(v, EnumSet.of(PValueMethod.AUTO, PValueMethod.EXACT, PValueMethod.ASYMPTOTIC)),
            continuityCorrection, mu);
    }

    /**
     * Return an instance with the configured continuity correction.
     *
     * <p>If {@code enabled}, adjust the Wilcoxon rank statistic by 0.5 towards the
     * mean value when computing the z-statistic if a normal approximation is used
     * to compute the p-value.
     *
     * @param v Value.
     * @return an instance
     */
    public WilcoxonSignedRankTest with(ContinuityCorrection v) {
        return new WilcoxonSignedRankTest(alternative, pValueMethod,
            Objects.requireNonNull(v) == ContinuityCorrection.ENABLED, mu);
    }

    /**
     * Return an instance with the configured expected difference {@code mu}.
     *
     * @param v Value.
     * @return an instance
     * @throws IllegalArgumentException if the value is not finite
     */
    public WilcoxonSignedRankTest withMu(double v) {
        return new WilcoxonSignedRankTest(alternative, pValueMethod, continuityCorrection, Arguments.checkFinite(v));
    }

    /**
     * Computes the Wilcoxon signed ranked statistic comparing the differences between
     * sample values {@code z = x - y} to {@code mu}.
     *
     * <p>This method handles matching samples {@code z[i] == mu} (no difference)
     * by including them in the ranking of samples but excludes them from the test statistic
     * (<i>signed-rank zero procedure</i>).
     *
     * @param z Signed differences between sample values.
     * @return Wilcoxon <i>positive-rank sum</i> statistic (W+)
     * @throws IllegalArgumentException if {@code z} is zero-length; contains NaN values;
     * or all differences are equal to the expected difference
     * @see #withMu(double)
     */
    public double statistic(double[] z) {
        return computeStatistic(z, mu);
    }

    /**
     * Computes the Wilcoxon signed ranked statistic comparing the differences between two related
     * samples or repeated measurements on a single sample.
     *
     * <p>This method handles matching samples {@code x[i] - mu == y[i]} (no difference)
     * by including them in the ranking of samples but excludes them from the test statistic
     * (<i>signed-rank zero procedure</i>).
     *
     * <p>This method is equivalent to creating an array of differences
     * {@code z = x - y} and calling {@link #statistic(double[]) statistic(z)}.
     *
     * @param x First sample values.
     * @param y Second sample values.
     * @return Wilcoxon <i>positive-rank sum</i> statistic (W+)
     * @throws IllegalArgumentException if {@code x} or {@code y} are zero-length; are not
     * the same length; contain NaN values; or {@code x[i] == y[i]} for all data
     * @see #withMu(double)
     */
    public double statistic(double[] x, double[] y) {
        checkSamples(x, y);
        // Apply mu before creation of differences
        final double[] z = calculateDifferences(mu, x, y);
        return computeStatistic(z, 0);
    }

    /**
     * Performs a Wilcoxon signed ranked statistic comparing the differences between
     * sample values {@code z = x - y} to {@code mu}.
     *
     * <p>This method handles matching samples {@code z[i] == mu} (no difference)
     * by including them in the ranking of samples but excludes them from the test statistic
     * (<i>signed-rank zero procedure</i>).
     *
     * <p>The {@link AlternativeHypothesis alternative hypothesis} is:
     * <ul>
     * <li>'two-sided': the distribution of the difference is not symmetric about {@code mu}.
     * <li>'greater': the distribution of the difference is stochastically greater than a
     * distribution symmetric about {@code mu}.
     * <li>'less': the distribution of the difference is stochastically less than a distribution
     * symmetric about {@code mu}.
     * </ul>
     *
     * <p>If the p-value method is {@link PValueMethod#AUTO auto} an exact p-value
     * is computed if the samples contain less than 50 values; otherwise a normal
     * approximation is used.
     *
     * <p>Computation of the exact p-value is only valid if there are no matching
     * samples {@code z[i] == mu} and no tied ranks in the data; otherwise the
     * p-value resorts to the asymptotic Cureton approximation using a tie
     * correction and an optional continuity correction.
     *
     * <p><strong>Note: </strong>
     * Computation of the exact p-value requires the
     * sample size {@code <= 1023}. Exact computation requires tabulation of values
     * not exceeding size {@code n(n+1)/2} and computes in order n^2/2.
     *
     * @param z Differences between sample values.
     * @return test result
     * @throws IllegalArgumentException if {@code z} is zero-length; contains NaN values;
     * or all differences are zero
     * @see #withMu(double)
     * @see #with(AlternativeHypothesis)
     * @see #with(ContinuityCorrection)
     */
    public Result test(double[] z) {
        return computeTest(z, mu);
    }

    /**
     * Performs a Wilcoxon signed ranked statistic comparing mean for two related
     * samples or repeated measurements on a single sample.
     *
     * <p>This method handles matching samples {@code x[i] - mu == y[i]} (no difference)
     * by including them in the ranking of samples but excludes them
     * from the test statistic (<i>signed-rank zero procedure</i>).
     *
     * <p>This method is equivalent to creating an array of differences
     * {@code z = x - y} and calling {@link #test(double[]) test(z)}.
     *
     * @param x First sample values.
     * @param y Second sample values.
     * @return test result
     * @throws IllegalArgumentException if {@code x} or {@code y} are zero-length; are not
     * the same length; contain NaN values; or {@code x[i] - mu == y[i]} for all data
     * @see #statistic(double[], double[])
     * @see #test(double[])
     */
    public Result test(double[] x, double[] y) {
        checkSamples(x, y);
        // Apply mu before creation of differences
        final double[] z = calculateDifferences(mu, x, y);
        return computeTest(z, 0);
    }

    /**
     * Computes the Wilcoxon signed ranked statistic comparing the differences between
     * sample values {@code z = x - y} to {@code mu}.
     *
     * @param z Signed differences between sample values.
     * @param mu Expected difference.
     * @return Wilcoxon <i>positive-rank sum</i> statistic (W+)
     * @throws IllegalArgumentException if {@code z} is zero-length; contains NaN values;
     * or all differences are equal to the expected difference
     * @see #withMu(double)
     */
    private static double computeStatistic(double[] z, double mu) {
        Arguments.checkValuesRequiredSize(z.length, 1);
        final double[] x = StatisticUtils.subtract(z, mu);
        // Raises an error if all zeros
        countZeros(x);
        final double[] zAbs = calculateAbsoluteDifferences(x);
        final double[] ranks = RANKING.apply(zAbs);
        return calculateW(x, ranks);
    }

    /**
     * Performs a Wilcoxon signed ranked statistic comparing the differences between
     * sample values {@code z = x - y} to {@code mu}.
     *
     * @param z Differences between sample values.
     * @param expectedMu Expected difference.
     * @return test result
     * @throws IllegalArgumentException if {@code z} is zero-length; contains NaN values;
     * or all differences are zero
     */
    private Result computeTest(double[] z, double expectedMu) {
        // Computation as above. The ranks are required for tie correction.
        Arguments.checkValuesRequiredSize(z.length, 1);
        final double[] x = StatisticUtils.subtract(z, expectedMu);
        // Raises an error if all zeros
        final int zeros = countZeros(x);
        final double[] zAbs = calculateAbsoluteDifferences(x);
        final double[] ranks = RANKING.apply(zAbs);
        final double wPlus = calculateW(x, ranks);

        // Exact p has strict requirements for no zeros, no ties
        final double c = calculateTieCorrection(ranks);
        final boolean tiedValues = c != 0;

        final int n = z.length;
        // Exact p requires no ties and no zeros
        double p;
        if (selectMethod(pValueMethod, n) == PValueMethod.EXACT && n <= EXACT_LIMIT && !tiedValues && zeros == 0) {
            p = calculateExactPValue((int) wPlus, n, alternative);
        } else {
            p = calculateAsymptoticPValue(wPlus, n, zeros, c, alternative, continuityCorrection);
        }
        return new Result(wPlus, tiedValues, zeros != 0, p);
    }

    /**
     * Ensures that the provided arrays fulfil the assumptions.
     *
     * @param x First sample.
     * @param y Second sample.
     * @throws IllegalArgumentException if {@code x} or {@code y} are zero-length; or do not
     * have the same length
     */
    private static void checkSamples(double[] x, double[] y) {
        Arguments.checkValuesRequiredSize(x.length, 1);
        Arguments.checkValuesRequiredSize(y.length, 1);
        Arguments.checkValuesSizeMatch(x.length, y.length);
    }

    /**
     * Calculates x[i] - mu - y[i] for all i.
     *
     * @param mu Expected difference.
     * @param x First sample.
     * @param y Second sample.
     * @return z = x - y
     */
    private static double[] calculateDifferences(double mu, double[] x, double[] y) {
        final double[] z = new double[x.length];
        for (int i = 0; i < x.length; ++i) {
            z[i] = x[i] - mu - y[i];
        }
        return z;
    }

    /**
     * Calculates |z[i]| for all i.
     *
     * @param z Sample.
     * @return |z|
     */
    private static double[] calculateAbsoluteDifferences(double[] z) {
        final double[] zAbs = new double[z.length];
        for (int i = 0; i < z.length; ++i) {
            zAbs[i] = Math.abs(z[i]);
        }
        return zAbs;
    }

    /**
     * Calculate the Wilcoxon <i>positive-rank sum</i> statistic.
     *
     * @param obs Observed signed value.
     * @param ranks Ranks (including averages for ties).
     * @return Wilcoxon <i>positive-rank sum</i> statistic (W+)
     */
    private static double calculateW(final double[] obs, final double[] ranks) {
        final Sum wPlus = Sum.create();
        for (int i = 0; i < obs.length; ++i) {
            // Must be positive (excludes zeros)
            if (obs[i] > 0) {
                wPlus.add(ranks[i]);
            }
        }
        return wPlus.getAsDouble();
    }

    /**
     * Count the number of zeros in the data.
     *
     * @param z Input data.
     * @return the zero count
     * @throws IllegalArgumentException if the data is all zeros
     */
    private static int countZeros(final double[] z) {
        int c = 0;
        for (final double v : z) {
            if (v == 0) {
                c++;
            }
        }
        if (c == z.length) {
            throw new InferenceException("All signed differences are zero");
        }
        return c;
    }

    /**
     * Calculate the tie correction.
     * Destructively modifies ranks (by sorting).
     * <pre>
     * c = sum(t^3 - t)
     * </pre>
     * <p>where t is the size of each group of tied observations.
     *
     * @param ranks Ranks
     * @return the tie correction
     */
    static double calculateTieCorrection(double[] ranks) {
        double c = 0;
        int ties = 1;
        Arrays.sort(ranks);
        double last = Double.NaN;
        for (final double rank : ranks) {
            // Deliberate use of equals
            if (last == rank) {
                // Extend the tied group
                ties++;
            } else {
                if (ties != 1) {
                    c += Math.pow(ties, 3) - ties;
                    ties = 1;
                }
                last = rank;
            }
        }
        // Final ties count
        c += Math.pow(ties, 3) - ties;
        return c;
    }

    /**
     * Select the method to compute the p-value.
     *
     * @param method P-value method.
     * @param n Size of the data.
     * @return p-value method.
     */
    private static PValueMethod selectMethod(PValueMethod method, int n) {
        return method == PValueMethod.AUTO && n <= AUTO_LIMIT ? PValueMethod.EXACT : method;
    }

    /**
     * Compute the asymptotic p-value using the Cureton normal approximation. This
     * corrects for zeros in the signed-rank zero procedure and/or ties corrected using
     * the average method.
     *
     * @param wPlus Wilcoxon signed rank value (W+).
     * @param n Number of subjects.
     * @param z Count of number of zeros
     * @param c Tie-correction
     * @param alternative Alternative hypothesis.
     * @param continuityCorrection true to use a continuity correction.
     * @return two-sided asymptotic p-value
     */
    private static double calculateAsymptoticPValue(double wPlus, int n, double z, double c,
            AlternativeHypothesis alternative, boolean continuityCorrection) {
        // E[W+] = n * (n + 1) / 4 - z * (z + 1) / 4
        final double e = (n * (n + 1.0) - z * (z + 1.0)) * 0.25;

        final double variance = ((n * (n + 1.0) * (2 * n + 1.0)) -
                                (z * (z + 1.0) * (2 * z + 1.0)) - c * 0.5) / 24;

        double x = wPlus - e;
        if (continuityCorrection) {
            // +/- 0.5 is a continuity correction towards the expected.
            if (alternative == AlternativeHypothesis.GREATER_THAN) {
                x -= 0.5;
            } else if (alternative == AlternativeHypothesis.LESS_THAN) {
                x += 0.5;
            } else {
                // two-sided. Shift towards the expected of zero.
                // Use of signum ignores x==0 (i.e. not copySign(0.5, z))
                x -= Math.signum(x) * 0.5;
            }
        }
        x /= Math.sqrt(variance);

        final NormalDistribution standardNormal = NormalDistribution.of(0, 1);
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            return standardNormal.survivalProbability(x);
        }
        if (alternative == AlternativeHypothesis.LESS_THAN) {
            return standardNormal.cumulativeProbability(x);
        }
        // two-sided
        return 2 * standardNormal.survivalProbability(Math.abs(x));
    }

    /**
     * Compute the exact p-value.
     *
     * <p>This computation requires that no zeros or ties are found in the data. The input
     * value n is limited to 1023.
     *
     * @param w1 Wilcoxon signed rank value (W+, or W-).
     * @param n Number of subjects.
     * @param alternative Alternative hypothesis.
     * @return exact p-value (two-sided, greater, or less using the options)
     */
    private static double calculateExactPValue(int w1, int n, AlternativeHypothesis alternative) {
        // T+ plus T- equals the sum of the ranks: n(n+1)/2
        // Compute using the lower half.
        // No overflow here if n <= 1023.
        final int sum = n * (n + 1) / 2;
        final int w2 = sum - w1;

        // Return the correct side:
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            // sf(w1 - 1)
            return sf(w1 - 1, w2 + 1, n);
        }
        if (alternative == AlternativeHypothesis.LESS_THAN) {
            // cdf(w1)
            return cdf(w1, w2, n);
        }
        // two-sided: 2 * sf(max(w1, w2) - 1) or 2 * cdf(min(w1, w2))
        final double p = 2 * computeCdf(Math.min(w1, w2), n);
        // Clip to range: [0, 1]
        return Math.min(1, p);
    }

    /**
     * Compute the cumulative density function of the Wilcoxon signed rank W+ statistic.
     * The W- statistic is passed for convenience to exploit symmetry in the distribution.
     *
     * @param w1 Wilcoxon W+ statistic
     * @param w2 Wilcoxon W- statistic
     * @param n Number of subjects.
     * @return {@code Pr(X <= k)}
     */
    private static double cdf(int w1, int w2, int n) {
        // Exploit symmetry. Note the distribution is discrete thus requiring (w2 - 1).
        return w2 > w1 ?
            computeCdf(w1, n) :
            1 - computeCdf(w2 - 1, n);
    }

    /**
     * Compute the survival function of the Wilcoxon signed rank W+ statistic.
     * The W- statistic is passed for convenience to exploit symmetry in the distribution.
     *
     * @param w1 Wilcoxon W+ statistic
     * @param w2 Wilcoxon W- statistic
     * @param n Number of subjects.
     * @return {@code Pr(X <= k)}
     */
    private static double sf(int w1, int w2, int n) {
        // Opposite of the CDF
        return w2 > w1 ?
            1 - computeCdf(w1, n) :
            computeCdf(w2 - 1, n);
    }

    /**
     * Compute the cumulative density function for the distribution of the Wilcoxon
     * signed rank statistic. This is a discrete distribution and is only valid
     * when no zeros or ties are found in the data.
     *
     * <p>This should be called with the lower of W+ or W- for computational efficiency.
     * The input value n is limited to 1023.
     *
     * <p>Uses recursion to compute the density for {@code X <= t} and sums the values.
     * See: https://en.wikipedia.org/wiki/Wilcoxon_signed-rank_test#Computing_the_null_distribution
     *
     * @param t Smallest Wilcoxon signed rank value (W+, or W-).
     * @param n Number of subjects.
     * @return {@code Pr(T <= t)}
     */
    private static double computeCdf(int t, int n) {
        // Currently limited to n=1023.
        // Note:
        // The limit for t is n(n+1)/2.
        // The highest possible sum is bounded by the normalisation factor 2^n.
        // n         t              sum          support
        // 31        [0, 496]       < 2^31       int
        // 63        [0, 2016]      < 2^63       long
        // 1023      [0, 523766]    < 2^1023     double

        if (t <= 0) {
            // No recursion required
            return t < 0 ? 0 : Math.scalb(1, -n);
        }

        // Define u_n(t) as the number of sign combinations for T = t
        // Pr(T == t) = u_n(t) / 2^n
        // Sum them to create the cumulative probability Pr(T <= t).
        //
        // Recursive formula:
        // u_n(t) = u_{n-1}(t) + u_{n-1}(t-n)
        // u_0(0) = 1
        // u_0(t) = 0 : t != 0
        // u_n(t) = 0 : t < 0 || t > n(n+1)/2

        // Compute all u_n(t) up to t.
        final double[] u = new double[t + 1];
        // Initialize u_1(t) using base cases for recursion
        u[0] = u[1] = 1;

        // Each u_n(t) is created using the current correct values for u_{n-1}(t)
        for (int nn = 2; nn < n + 1; nn++) {
            // u[t] holds the correct value for u_{n-1}(t)
            // u_n(t) = u_{n-1}(t) + u_{n-1}(t-n)
            for (int tt = t; tt >= nn; tt--) {
                u[tt] += u[tt - nn];
            }
        }
        final double sum = Arrays.stream(u).sum();

        // Finally divide by the number of possible sums: 2^n
        return Math.scalb(sum, -n);
    }
}
