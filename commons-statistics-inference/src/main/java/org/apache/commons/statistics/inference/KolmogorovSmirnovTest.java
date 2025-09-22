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
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import org.apache.commons.numbers.combinatorics.BinomialCoefficientDouble;
import org.apache.commons.numbers.combinatorics.Factorial;
import org.apache.commons.numbers.core.ArithmeticUtils;
import org.apache.commons.numbers.core.Sum;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Implements the Kolmogorov-Smirnov (K-S) test for equality of continuous distributions.
 *
 * <p>The one-sample test uses a D statistic based on the maximum deviation of the empirical
 * distribution of sample data points from the distribution expected under the null hypothesis.
 *
 * <p>The two-sample test uses a D statistic based on the maximum deviation of the two empirical
 * distributions of sample data points. The two-sample tests evaluate the null hypothesis that
 * the two samples {@code x} and {@code y} come from the same underlying distribution.
 *
 * <p>References:
 * <ol>
 * <li>
 * Marsaglia, G., Tsang, W. W., &amp; Wang, J. (2003).
 * <a href="https://doi.org/10.18637/jss.v008.i18">Evaluating Kolmogorov's Distribution.</a>
 * Journal of Statistical Software, 8(18), 1–4.
 * <li>Simard, R., &amp; L’Ecuyer, P. (2011).
 * <a href="https://doi.org/10.18637/jss.v039.i11">Computing the Two-Sided Kolmogorov-Smirnov Distribution.</a>
 * Journal of Statistical Software, 39(11), 1–18.
 * <li>Sekhon, J. S. (2011).
 * <a href="https://doi.org/10.18637/jss.v042.i07">
 * Multivariate and Propensity Score Matching Software with Automated Balance Optimization:
 * The Matching package for R.</a>
 * Journal of Statistical Software, 42(7), 1–52.
 * <li>Viehmann, T (2021).
 * <a href="https://doi.org/10.48550/arXiv.2102.08037">
 * Numerically more stable computation of the p-values for the two-sample Kolmogorov-Smirnov test.</a>
 * arXiv:2102.08037
 * <li>Hodges, J. L. (1958).
 * <a href="https://doi.org/10.1007/BF02589501">
 * The significance probability of the smirnov two-sample test.</a>
 * Arkiv for Matematik, 3(5), 469-486.
 * </ol>
 *
 * <p>Note that [1] contains an error in computing h, refer to <a
 * href="https://issues.apache.org/jira/browse/MATH-437">MATH-437</a> for details.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Kolmogorov-Smirnov_test">
 * Kolmogorov-Smirnov (K-S) test (Wikipedia)</a>
 * @since 1.1
 */
public final class KolmogorovSmirnovTest {
    /** Name for sample 1. */
    private static final String SAMPLE_1_NAME = "Sample 1";
    /** Name for sample 2. */
    private static final String SAMPLE_2_NAME = "Sample 2";
    /** When the largest sample size exceeds this value, 2-sample test AUTO p-value
     * uses an asymptotic distribution to compute the p-value. */
    private static final int LARGE_SAMPLE = 10000;
    /** Maximum finite factorial. */
    private static final int MAX_FACTORIAL = 170;
    /** Maximum length of an array. This is used to determine if two arrays can be concatenated
     * to create a sampler from the joint distribution. The limit is copied from the limit
     * of java.util.ArrayList. */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    /** The maximum least common multiple (lcm) to attempt the exact p-value computation.
     * The integral d value is in [0, n*m] in steps of the greatest common denominator (gcd),
     * thus lcm = n*m/gcd is the number of possible different p-values.
     * Some methods have a lower limit due to computation limits. This should be larger
     * than LARGE_SAMPLE^2 so all AUTO p-values attempt an exact computation, i.e.
     * at least 10000^2 ~ 2^26.56. */
    private static final long MAX_LCM_TWO_SAMPLE_EXACT_P = 1L << 31;
    /** Placeholder to use for the two-sample sign array when the value can be ignored. */
    private static final int[] IGNORED_SIGN = new int[1];
    /** Placeholder to use for the two-sample ties D array when the value can be ignored. */
    private static final long[] IGNORED_D = new long[2];
    /** Default instance. */
    private static final KolmogorovSmirnovTest DEFAULT = new KolmogorovSmirnovTest(
        AlternativeHypothesis.TWO_SIDED, PValueMethod.AUTO, false, null, 1000);

    /** Alternative hypothesis. */
    private final AlternativeHypothesis alternative;
    /** Method to compute the p-value. */
    private final PValueMethod pValueMethod;
    /** Use a strict inequality for the two-sample exact p-value. */
    private final boolean strictInequality;
    /** Source of randomness. */
    private final UniformRandomProvider rng;
    /** Number of iterations . */
    private final int iterations;

    /**
     * Result for the one-sample Kolmogorov-Smirnov test.
     *
     * <p>This class is immutable.
     *
     * @since 1.1
     */
    public static class OneResult extends BaseSignificanceResult {
        /** Sign of the statistic. */
        private final int sign;

        /**
         * Create an instance.
         *
         * @param statistic Test statistic.
         * @param sign Sign of the statistic.
         * @param p Result p-value.
         */
        OneResult(double statistic, int sign, double p) {
            super(statistic, p);
            this.sign = sign;
        }

        /**
         * Gets the sign of the statistic. This is 1 for \(D^+\) and -1 for \(D^-\).
         * For a two sided-test this is zero if the magnitude of \(D^+\) and \(D^-\) was equal;
         * otherwise the sign indicates the larger of \(D^+\) or \(D^-\).
         *
         * @return the sign
         */
        public int getSign() {
            return sign;
        }
    }

    /**
     * Result for the two-sample Kolmogorov-Smirnov test.
     *
     * <p>This class is immutable.
     *
     * @since 1.1
     */
    public static final class TwoResult extends OneResult {
        /** Flag to indicate there were significant ties.
         * Note that in extreme cases there may be significant ties despite {@code upperD == D}
         * due to rounding when converting the integral statistic to a double. For this
         * reason the presence of ties is stored as a flag. */
        private final boolean significantTies;
        /** Upper bound of the D statistic from all possible paths through regions with ties. */
        private final double upperD;
        /** The p-value of the upper D value. */
        private final double upperP;

        /**
         * Create an instance.
         *
         * @param statistic Test statistic.
         * @param sign Sign of the statistic.
         * @param p Result p-value.
         * @param significantTies Flag to indicate there were significant ties.
         * @param upperD Upper bound of the D statistic from all possible paths through
         * regions with ties.
         * @param upperP The p-value of the upper D value.
         */
        TwoResult(double statistic, int sign, double p, boolean significantTies, double upperD, double upperP) {
            super(statistic, sign, p);
            this.significantTies = significantTies;
            this.upperD = upperD;
            this.upperP = upperP;
        }

        /**
         * {@inheritDoc}
         *
         * <p><strong>Ties</strong>
         *
         * <p>The presence of ties in the data creates a distribution for the D values generated
         * by all possible orderings of the tied regions. This statistic is computed using the
         * path with the <em>minimum</em> effect on the D statistic.
         *
         * <p>For a one-sided statistic \(D^+\) or \(D^-\), this is the lower bound of the D statistic.
         *
         * <p>For a two-sided statistic D, this may be <em>below</em> the lower bound of the
         * distribution of all possible D values. This case occurs when the number of ties
         * is very high and is identified by a {@link #getPValue() p-value} of 1.
         *
         * <p>If the two-sided statistic is zero this only occurs in the presence of ties:
         * either the two arrays are identical, are 'identical' data of a single value
         * (sample sizes may be different), or have a sequence of ties of 'identical' data
         * with a net zero effect on the D statistic, e.g.
         * <pre>
         *  [1,2,3]           vs [1,2,3]
         *  [0,0,0,0]         vs [0,0,0]
         *  [0,0,0,0,1,1,1,1] vs [0,0,0,1,1,1]
         * </pre>
         */
        @Override
        public double getStatistic() {
            // Note: This method is here for documentation
            return super.getStatistic();
        }

        /**
         * Returns {@code true} if there were ties between samples that occurred
         * in a region which could change the D statistic if the ties were resolved to
         * a defined order.
         *
         * <p>Ties between the data can be interpreted as if the values were different
         * but within machine epsilon. In this case the order within the tie region is not known.
         * If the most extreme ordering of any tied regions (e.g. all tied values of {@code x}
         * before all tied values of {@code y}) could create a larger D statistic this
         * method will return {@code true}.
         *
         * <p>If there were no ties, or all possible orderings of tied regions create the same
         * D statistic, this method returns {@code false}.
         *
         * <p>Note it is possible that this method returns {@code true} when {@code D == upperD}
         * due to rounding when converting the computed D statistic to a double. This will
         * only occur for large sample sizes {@code n} and {@code m} where the product
         * {@code n*m >= 2^53}.
         *
         * @return true if the D statistic could be changed by resolution of ties
         * @see #getUpperD()
         */
        public boolean hasSignificantTies() {
            return significantTies;
        }

        /**
         * Return the upper bound of the D statistic from all possible paths through regions with ties.
         *
         * <p>This will return a value equal to or greater than {@link #getStatistic()}.
         *
         * @return the upper bound of D
         * @see #hasSignificantTies()
         */
        public double getUpperD() {
            return upperD;
        }

        /**
         * Return the p-value of the upper bound of the D statistic.
         *
         * <p>If computed, this will return a value equal to or less than
         * {@link #getPValue() getPValue}. It may be orders of magnitude smaller.
         *
         * <p>Note: This p-value corresponds to the most extreme p-value from all possible
         * orderings of tied regions. It is <strong>not</strong> recommended to use this to
         * reject the null hypothesis. The upper bound of D and the corresponding p-value
         * provide information that must be interpreted in the context of the sample data and
         * used to inform a decision on the suitability of the test to the data.
         *
         * <p>This value is set to {@link Double#NaN NaN} if the {@link #getPValue() p-value} was
         * {@linkplain PValueMethod#ESTIMATE estimated}. The estimated p-value will have been created
         * using a distribution of possible D values given the underlying joint distribution of
         * the sample data. Comparison of the p-value to the upper p-value is not applicable.
         *
         * @return the p-value of the upper bound of D (or NaN)
         * @see #getUpperD()
         */
        public double getUpperPValue() {
            return upperP;
        }
    }

    /**
     * @param alternative Alternative hypothesis.
     * @param method P-value method.
     * @param strict true to use a strict inequality.
     * @param rng Source of randomness.
     * @param iterations Number of iterations.
     */
    private KolmogorovSmirnovTest(AlternativeHypothesis alternative, PValueMethod method, boolean strict,
        UniformRandomProvider rng, int iterations) {
        this.alternative = alternative;
        this.pValueMethod = method;
        this.strictInequality = strict;
        this.rng = rng;
        this.iterations = iterations;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     * <li>{@link AlternativeHypothesis#TWO_SIDED}
     * <li>{@link PValueMethod#AUTO}
     * <li>{@link Inequality#NON_STRICT}
     * <li>{@linkplain #with(UniformRandomProvider) RNG = none}
     * <li>{@linkplain #withIterations(int) Iterations = 1000}
     * </ul>
     *
     * @return default instance
     */
    public static KolmogorovSmirnovTest withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured alternative hypothesis.
     *
     * @param v Value.
     * @return an instance
     */
    public KolmogorovSmirnovTest with(AlternativeHypothesis v) {
        return new KolmogorovSmirnovTest(Objects.requireNonNull(v), pValueMethod, strictInequality, rng, iterations);
    }

    /**
     * Return an instance with the configured p-value method.
     *
     * <p>For the one-sample two-sided test Kolmogorov's asymptotic approximation can be used;
     * otherwise the p-value uses the distribution of the D statistic.
     *
     * <p>For the two-sample test the exact p-value can be computed for small sample sizes;
     * otherwise the p-value resorts to the asymptotic approximation. Alternatively a p-value
     * can be estimated from the combined distribution of the samples. This requires a source
     * of randomness.
     *
     * @param v Value.
     * @return an instance
     * @see #with(UniformRandomProvider)
     */
    public KolmogorovSmirnovTest with(PValueMethod v) {
        return new KolmogorovSmirnovTest(alternative, Objects.requireNonNull(v), strictInequality, rng, iterations);
    }

    /**
     * Return an instance with the configured inequality.
     *
     * <p>Computes the p-value for the two-sample test as \(P(D_{n,m} &gt; d)\) if strict;
     * otherwise \(P(D_{n,m} \ge d)\), where \(D_{n,m}\) is the 2-sample
     * Kolmogorov-Smirnov statistic, either the two-sided \(D_{n,m}\) or one-sided
     * \(D_{n,m}^+\) or \(D_{n,m}^-\).
     *
     * @param v Value.
     * @return an instance
     */
    public KolmogorovSmirnovTest with(Inequality v) {
        return new KolmogorovSmirnovTest(alternative, pValueMethod,
            Objects.requireNonNull(v) == Inequality.STRICT, rng, iterations);
    }

    /**
     * Return an instance with the configured source of randomness.
     *
     * <p>Applies to the two-sample test when the p-value method is set to
     * {@link PValueMethod#ESTIMATE ESTIMATE}. The randomness
     * is used for sampling of the combined distribution.
     *
     * <p>There is no default source of randomness. If the randomness is not
     * set then estimation is not possible and an {@link IllegalStateException} will be
     * raised in the two-sample test.
     *
     * @param v Value.
     * @return an instance
     * @see #with(PValueMethod)
     */
    public KolmogorovSmirnovTest with(UniformRandomProvider v) {
        return new KolmogorovSmirnovTest(alternative, pValueMethod, strictInequality,
            Objects.requireNonNull(v), iterations);
    }

    /**
     * Return an instance with the configured number of iterations.
     *
     * <p>Applies to the two-sample test when the p-value method is set to
     * {@link PValueMethod#ESTIMATE ESTIMATE}. This is the number of sampling iterations
     * used to estimate the p-value. The p-value is a fraction using the {@code iterations}
     * as the denominator. The number of significant digits in the p-value is
     * upper bounded by log<sub>10</sub>(iterations); small p-values have fewer significant
     * digits. A large number of iterations is recommended when using a small critical
     * value to reject the null hypothesis.
     *
     * @param v Value.
     * @return an instance
     * @throws IllegalArgumentException if the number of iterations is not strictly positive
     */
    public KolmogorovSmirnovTest withIterations(int v) {
        return new KolmogorovSmirnovTest(alternative, pValueMethod, strictInequality, rng,
            Arguments.checkStrictlyPositive(v));
    }

    /**
     * Computes the one-sample Kolmogorov-Smirnov test statistic.
     *
     * <ul>
     * <li>two-sided: \(D_n=\sup_x |F_n(x)-F(x)|\)
     * <li>greater: \(D_n^+=\sup_x (F_n(x)-F(x))\)
     * <li>less: \(D_n^-=\sup_x (F(x)-F_n(x))\)
     * </ul>
     *
     * <p>where \(F\) is the distribution cumulative density function ({@code cdf}),
     * \(n\) is the length of {@code x} and \(F_n\) is the empirical distribution that puts
     * mass \(1/n\) at each of the values in {@code x}.
     *
     * <p>The cumulative distribution function should map a real value {@code x} to a probability
     * in [0, 1]. To use a reference distribution the CDF can be passed using a method reference:
     * <pre>
     * UniformContinuousDistribution dist = UniformContinuousDistribution.of(0, 1);
     * UniformRandomProvider rng = RandomSource.KISS.create(123);
     * double[] x = dist.sampler(rng).samples(100);
     * double d = KolmogorovSmirnovTest.withDefaults().statistic(x, dist::cumulativeProbability);
     * </pre>
     *
     * @param cdf Reference cumulative distribution function.
     * @param x Sample being evaluated.
     * @return Kolmogorov-Smirnov statistic
     * @throws IllegalArgumentException if {@code data} does not have length at least 2; or contains NaN values.
     * @see #test(double[], DoubleUnaryOperator)
     */
    public double statistic(double[] x, DoubleUnaryOperator cdf) {
        return computeStatistic(x, cdf, IGNORED_SIGN);
    }

    /**
     * Computes the two-sample Kolmogorov-Smirnov test statistic.
     *
     * <ul>
     * <li>two-sided: \(D_{n,m}=\sup_x |F_n(x)-F_m(x)|\)
     * <li>greater: \(D_{n,m}^+=\sup_x (F_n(x)-F_m(x))\)
     * <li>less: \(D_{n,m}^-=\sup_x (F_m(x)-F_n(x))\)
     * </ul>
     *
     * <p>where \(n\) is the length of {@code x}, \(m\) is the length of {@code y}, \(F_n\) is the
     * empirical distribution that puts mass \(1/n\) at each of the values in {@code x} and \(F_m\)
     * is the empirical distribution that puts mass \(1/m\) at each of the values in {@code y}.
     *
     * @param x First sample.
     * @param y Second sample.
     * @return Kolmogorov-Smirnov statistic
     * @throws IllegalArgumentException if either {@code x} or {@code y} does not
     * have length at least 2; or contain NaN values.
     * @see #test(double[], double[])
     */
    public double statistic(double[] x, double[] y) {
        final int n = checkArrayLength(x);
        final int m = checkArrayLength(y);
        // Clone to avoid destructive modification of input
        final long dnm = computeIntegralKolmogorovSmirnovStatistic(x.clone(), y.clone(),
                IGNORED_SIGN, IGNORED_D);
        // Re-use the method to compute D in [0, 1] for consistency
        return computeD(dnm, n, m, ArithmeticUtils.gcd(n, m));
    }

    /**
     * Performs a one-sample Kolmogorov-Smirnov test evaluating the null hypothesis
     * that {@code x} conforms to the distribution cumulative density function ({@code cdf}).
     *
     * <p>The test is defined by the {@link AlternativeHypothesis}:
     * <ul>
     * <li>Two-sided evaluates the null hypothesis that the two distributions are
     * identical, \(F_n(i) = F(i)\) for all \( i \); the alternative is that the are not
     * identical. The statistic is \( max(D_n^+, D_n^-) \) and the sign of \( D \) is provided.
     * <li>Greater evaluates the null hypothesis that the \(F_n(i) &lt;= F(i)\) for all \( i \);
     * the alternative is \(F_n(i) &gt; F(i)\) for at least one \( i \). The statistic is \( D_n^+ \).
     * <li>Less evaluates the null hypothesis that the \(F_n(i) &gt;= F(i)\) for all \( i \);
     * the alternative is \(F_n(i) &lt; F(i)\) for at least one \( i \). The statistic is \( D_n^- \).
     * </ul>
     *
     * <p>The p-value method defaults to exact. The one-sided p-value uses Smirnov's stable formula:
     *
     * <p>\[ P(D_n^+ \ge x) = x \sum_{j=0}^{\lfloor n(1-x) \rfloor} \binom{n}{j} \left(\frac{j}{n} + x\right)^{j-1} \left(1-x-\frac{j}{n} \right)^{n-j} \]
     *
     * <p>The two-sided p-value is computed using methods described in
     * Simard &amp; L’Ecuyer (2011). The two-sided test supports an asymptotic p-value
     * using Kolmogorov's formula:
     *
     * <p>\[ \lim_{n\to\infty} P(\sqrt{n}D_n &gt; z) = 2 \sum_{i=1}^\infty (-1)^{i-1} e^{-2 i^2 z^2} \]
     *
     * @param x Sample being being evaluated.
     * @param cdf Reference cumulative distribution function.
     * @return test result
     * @throws IllegalArgumentException if {@code data} does not have length at least 2; or contains NaN values.
     * @see #statistic(double[], DoubleUnaryOperator)
     */
    public OneResult test(double[] x, DoubleUnaryOperator cdf) {
        final int[] sign = {0};
        final double d = computeStatistic(x, cdf, sign);
        final double p;
        if (alternative == AlternativeHypothesis.TWO_SIDED) {
            PValueMethod method = pValueMethod;
            if (method == PValueMethod.AUTO) {
                // No switch to the asymptotic for large n
                method = PValueMethod.EXACT;
            }
            if (method == PValueMethod.ASYMPTOTIC) {
                // Kolmogorov's asymptotic formula using z = sqrt(n) * d
                p = KolmogorovSmirnovDistribution.ksSum(Math.sqrt(x.length) * d);
            } else {
                // exact
                p = KolmogorovSmirnovDistribution.Two.sf(d, x.length);
            }
        } else {
            // one-sided: always use exact
            p = KolmogorovSmirnovDistribution.One.sf(d, x.length);
        }
        return new OneResult(d, sign[0], p);
    }

    /**
     * Performs a two-sample Kolmogorov-Smirnov test on samples {@code x} and {@code y}.
     * Test the empirical distributions \(F_n\) and \(F_m\) where \(n\) is the length
     * of {@code x}, \(m\) is the length of {@code y}, \(F_n\) is the empirical distribution
     * that puts mass \(1/n\) at each of the values in {@code x} and \(F_m\) is the empirical
     * distribution that puts mass \(1/m\) of the {@code y} values.
     *
     * <p>The test is defined by the {@link AlternativeHypothesis}:
     * <ul>
     * <li>Two-sided evaluates the null hypothesis that the two distributions are
     * identical, \(F_n(i) = F_m(i)\) for all \( i \); the alternative is that they are not
     * identical. The statistic is \( max(D_n^+, D_n^-) \) and the sign of \( D \) is provided.
     * <li>Greater evaluates the null hypothesis that the \(F_n(i) &lt;= F_m(i)\) for all \( i \);
     * the alternative is \(F_n(i) &gt; F_m(i)\) for at least one \( i \). The statistic is \( D_n^+ \).
     * <li>Less evaluates the null hypothesis that the \(F_n(i) &gt;= F_m(i)\) for all \( i \);
     * the alternative is \(F_n(i) &lt; F_m(i)\) for at least one \( i \). The statistic is \( D_n^- \).
     * </ul>
     *
     * <p>If the {@linkplain PValueMethod p-value method} is auto, then an exact p computation
     * is attempted if both sample sizes are less than 10000 using the methods presented in
     * Viehmann (2021) and Hodges (1958); otherwise an asymptotic p-value is returned.
     * The two-sided p-value is \(\overline{F}(d, \sqrt{mn / (m + n)})\) where \(\overline{F}\)
     * is the complementary cumulative density function of the two-sided one-sample
     * Kolmogorov-Smirnov distribution. The one-sided p-value uses an approximation from
     * Hodges (1958) Eq 5.3.
     *
     * <p>\(D_{n,m}\) has a discrete distribution. This makes the p-value associated with the
     * null hypothesis \(H_0 : D_{n,m} \gt d \) differ from \(H_0 : D_{n,m} \ge d \)
     * by the mass of the observed value \(d\). These can be distinguished using an
     * {@link Inequality} parameter. This is ignored for large samples.
     *
     * <p>If the data contains ties there is no defined ordering in the tied region to use
     * for the difference between the two empirical distributions. Each ordering of the
     * tied region <em>may</em> create a different D statistic. All possible orderings
     * generate a distribution for the D value. In this case the tied region is traversed
     * entirely and the effect on the D value evaluated at the end of the tied region.
     * This is the path with the least change on the D statistic. The path with the
     * greatest change on the D statistic is also computed as the upper bound on D.
     * If these two values are different then the tied region is known to generate a
     * distribution for the D statistic and the p-value is an over estimate for the cases
     * with a larger D statistic. The presence of any significant tied regions is returned
     * in the result.
     *
     * <p>If the p-value method is {@link PValueMethod#ESTIMATE ESTIMATE} then the p-value is
     * estimated by repeat sampling of the joint distribution of {@code x} and {@code y}.
     * The p-value is the frequency that a sample creates a D statistic
     * greater than or equal to (or greater than for strict inequality) the observed value.
     * In this case a source of randomness must be configured or an {@link IllegalStateException}
     * will be raised. The p-value for the upper bound on D will not be estimated and is set to
     * {@link Double#NaN NaN}. This estimation procedure is not affected by ties in the data
     * and is increasingly robust for larger datasets. The method is modeled after
     * <a href="https://sekhon.berkeley.edu/matching/ks.boot.html">ks.boot</a>
     * in the R {@code Matching} package (Sekhon (2011)).
     *
     * @param x First sample.
     * @param y Second sample.
     * @return test result
     * @throws IllegalArgumentException if either {@code x} or {@code y} does not
     * have length at least 2; or contain NaN values.
     * @throws IllegalStateException if the p-value method is {@link PValueMethod#ESTIMATE ESTIMATE}
     * and there is no source of randomness.
     * @see #statistic(double[], double[])
     */
    public TwoResult test(double[] x, double[] y) {
        final int n = checkArrayLength(x);
        final int m = checkArrayLength(y);
        PValueMethod method = pValueMethod;
        final int[] sign = {0};
        final long[] tiesD = {0, 0};

        final double[] sx = x.clone();
        final double[] sy = y.clone();
        final long dnm = computeIntegralKolmogorovSmirnovStatistic(sx, sy, sign, tiesD);

        // Compute p-value. Note that the p-value is not invalidated by ties; it is the
        // D statistic that could be invalidated by resolution of the ties. So compute
        // the exact p even if ties are present.
        if (method == PValueMethod.AUTO) {
            // Use exact for small samples
            method = Math.max(n, m) < LARGE_SAMPLE ?
                PValueMethod.EXACT :
                PValueMethod.ASYMPTOTIC;
        }
        final int gcd = ArithmeticUtils.gcd(n, m);
        final double d = computeD(dnm, n, m, gcd);
        final boolean significantTies = tiesD[1] > dnm;
        final double d2 = significantTies ? computeD(tiesD[1], n, m, gcd) : d;

        final double p;
        final double p2;

        // Allow bootstrap estimation of the p-value
        if (method == PValueMethod.ESTIMATE) {
            p = estimateP(sx, sy, dnm);
            p2 = Double.NaN;
        } else {
            final boolean exact = method == PValueMethod.EXACT;
            p = twoSampleP(dnm, n, m, gcd, d, exact);
            if (significantTies) {
                // Compute the upper bound on D.
                // The p-value is also computed. The alternative is to save the options
                // in the result with (upper dnm, n, m) and compute it on-demand.
                // Note detection of whether the exact P computation is possible is based on
                // n and m, thus this will use the same computation.
                p2 = twoSampleP(tiesD[1], n, m, gcd, d2, exact);
            } else {
                p2 = p;
            }
        }
        return new TwoResult(d, sign[0], p, significantTies, d2, p2);
    }

    /**
     * Estimates the <i>p-value</i> of a two-sample Kolmogorov-Smirnov test evaluating the
     * null hypothesis that {@code x} and {@code y} are samples drawn from the same
     * probability distribution.
     *
     * <p>This method will destructively modify the input arrays (via a sort).
     *
     * <p>This method estimates the p-value by repeatedly sampling sets of size
     * {@code x.length} and {@code y.length} from the empirical distribution
     * of the combined sample. The memory requirement is an array copy of each of
     * the input arguments.
     *
     * <p>When using strict inequality, this is equivalent to the algorithm implemented in
     * the R function {@code ks.boot} as described in Sekhon (2011) Journal of Statistical
     * Software, 42(7), 1–52 [3].
     *
     * @param x First sample.
     * @param y Second sample.
     * @param dnm Integral D statistic.
     * @return p-value
     * @throws IllegalStateException if the source of randomness is null.
     */
    private double estimateP(double[] x, double[] y, long dnm) {
        if (rng == null) {
            throw new IllegalStateException("No source of randomness");
        }

        // Test if the random statistic is greater (strict), or greater or equal to d
        final long d = strictInequality ? dnm : dnm - 1;

        final long plus;
        final long minus;
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            plus = d;
            minus = Long.MIN_VALUE;
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            plus = Long.MAX_VALUE;
            minus = -d;
        } else {
            // two-sided
            plus = d;
            minus = -d;
        }

        // Test dnm=0. This occurs for example when x == y.
        if (0 < minus || 0 > plus) {
            // Edge case where all possible d will be outside the inclusive bounds
            return 1;
        }

        // Sample randomly with replacement from the combined distribution.
        final DoubleSupplier gen = createSampler(x, y, rng);
        int count = 0;
        for (int i = iterations; i > 0; i--) {
            for (int j = 0; j < x.length; j++) {
                x[j] = gen.getAsDouble();
            }
            for (int j = 0; j < y.length; j++) {
                y[j] = gen.getAsDouble();
            }
            if (testIntegralKolmogorovSmirnovStatistic(x, y, plus, minus)) {
                count++;
            }
        }
        return count / (double) iterations;
    }

    /**
     * Computes the magnitude of the one-sample Kolmogorov-Smirnov test statistic.
     * The sign of the statistic is optionally returned in {@code sign}. For the two-sided case
     * the sign is 0 if the magnitude of D+ and D- was equal; otherwise it indicates which D
     * had the larger magnitude.
     *
     * @param x Sample being evaluated.
     * @param cdf Reference cumulative distribution function.
     * @param sign Sign of the statistic (non-zero length).
     * @return Kolmogorov-Smirnov statistic
     * @throws IllegalArgumentException if {@code data} does not have length at least 2;
     * or contains NaN values.
     */
    private double computeStatistic(double[] x, DoubleUnaryOperator cdf, int[] sign) {
        final int n = checkArrayLength(x);
        final double nd = n;
        final double[] sx = sort(x.clone(), "Sample");
        // Note: ties in the data do not matter as we compare the empirical CDF
        // immediately before the value (i/n) and at the value (i+1)/n. For ties
        // of length m this would be (i-m+1)/n and (i+1)/n and the result is the same.
        double d = 0;
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            // Compute D+
            for (int i = 0; i < n; i++) {
                final double yi = cdf.applyAsDouble(sx[i]);
                final double dp = (i + 1) / nd - yi;
                d = dp > d ? dp : d;
            }
            sign[0] = 1;
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            // Compute D-
            for (int i = 0; i < n; i++) {
                final double yi = cdf.applyAsDouble(sx[i]);
                final double dn = yi - i / nd;
                d = dn > d ? dn : d;
            }
            sign[0] = -1;
        } else {
            // Two sided.
            // Compute both (as unsigned) and return the sign indicating the largest result.
            double plus = 0;
            double minus = 0;
            for (int i = 0; i < n; i++) {
                final double yi = cdf.applyAsDouble(sx[i]);
                final double dn = yi - i / nd;
                final double dp = (i + 1) / nd - yi;
                minus = dn > minus ? dn : minus;
                plus = dp > plus ? dp : plus;
            }
            sign[0] = Double.compare(plus, minus);
            d = Math.max(plus, minus);
        }
        return d;
    }

    /**
     * Computes the two-sample Kolmogorov-Smirnov test statistic. The statistic is integral
     * and has a value in {@code [0, n*m]}. Not all values are possible; the smallest
     * increment is the greatest common divisor of {@code n} and {@code m}.
     *
     * <p>This method will destructively modify the input arrays (via a sort).
     *
     * <p>The sign of the statistic is returned in {@code sign}. For the two-sided case
     * the sign is 0 if the magnitude of D+ and D- was equal; otherwise it indicates which D
     * had the larger magnitude. If the two-sided statistic is zero the two arrays are
     * identical, or are 'identical' data of a single value (sample sizes may be different),
     * or have a sequence of ties of 'identical' data with a net zero effect on the D statistic
     * e.g.
     * <pre>
     *  [1,2,3]           vs [1,2,3]
     *  [0,0,0,0]         vs [0,0,0]
     *  [0,0,0,0,1,1,1,1] vs [0,0,0,1,1,1]
     * </pre>
     *
     * <p>This method detects ties in the input data. Not all ties will invalidate the returned
     * statistic. Ties between the data can be interpreted as if the values were different
     * but within machine epsilon. In this case the path through the tie region is not known.
     * All paths through the tie region end at the same point. This method will compute the
     * most extreme path through each tie region and the D statistic for these paths. If the
     * ties D statistic is a larger magnitude than the returned D statistic then at least
     * one tie region lies at a point on the full path that could result in a different
     * statistic in the absence of ties. This signals the P-value computed using the returned
     * D statistic is one of many possible p-values given the data and how ties are resolved.
     * Note: The tiesD value may be smaller than the returned D statistic as it is not set
     * to the maximum of D or tiesD. The only result of interest is when {@code tiesD > D}
     * due to a tie region that can change the output D. On output {@code tiesD[0] != 0}
     * if there were ties between samples and {@code tiesD[1] = D} of the most extreme path
     * through the ties.
     *
     * @param x First sample (destructively modified by sort).
     * @param y Second sample  (destructively modified by sort).
     * @param sign Sign of the statistic (non-zero length).
     * @param tiesD Integral statistic for the most extreme path through any ties (length at least 2).
     * @return integral Kolmogorov-Smirnov statistic
     * @throws IllegalArgumentException if either {@code x} or {@code y} contain NaN values.
     */
    private long computeIntegralKolmogorovSmirnovStatistic(double[] x, double[] y, int[] sign, long[] tiesD) {
        // Sort the sample arrays
        sort(x, SAMPLE_1_NAME);
        sort(y, SAMPLE_2_NAME);
        final int n = x.length;
        final int m = y.length;

        // CDFs range from 0 to 1 using increments of 1/n and 1/m for x and y respectively.
        // Scale by n*m to use increments of m and n for x and y.
        // Find the max difference between cdf_x and cdf_y.
        int i = 0;
        int j = 0;
        long d = 0;
        long plus = 0;
        long minus = 0;
        // Ties: store the D+,D- for most extreme path though tie region(s)
        long tplus = 0;
        long tminus = 0;
        do {
            // No NaNs so compare using < and >
            if (x[i] < y[j]) {
                final double z = x[i];
                do {
                    i++;
                    d += m;
                } while (i < n && x[i] == z);
                plus = d > plus ? d : plus;
            } else if (x[i] > y[j]) {
                final double z = y[j];
                do {
                    j++;
                    d -= n;
                } while (j < m && y[j] == z);
                minus = d < minus ? d : minus;
            } else {
                // Traverse to the end of the tied section for d.
                // Also compute the most extreme path through the tied region.
                final double z = x[i];
                final long dd = d;
                int k = i;
                do {
                    i++;
                } while (i < n && x[i] == z);
                k = i - k;
                d += k * (long) m;
                // Extreme D+ path
                tplus = d > tplus ? d : tplus;
                k = j;
                do {
                    j++;
                } while (j < m && y[j] == z);
                k = j - k;
                d -= k * (long) n;
                // Extreme D- path must start at the original d
                tminus = Math.min(tminus, dd - k * (long) n);
                // End of tied section
                if (d > plus) {
                    plus = d;
                } else if (d < minus) {
                    minus = d;
                }
            }
        } while (i < n && j < m);
        // The presence of any ties is flagged by a non-zero value for D+ or D-.
        // Note we cannot use the selected tiesD value as in the one-sided case it may be zero
        // and the non-selected D value will be non-zero.
        tiesD[0] = tplus | tminus;
        // For simplicity the correct tiesD is not returned (correct value is commented).
        // The only case that matters is tiesD > D which is evaluated by the caller.
        // Note however that the distance of tiesD < D is a measure of how little the
        // tied region matters.
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            sign[0] = 1;
            // correct = max(tplus, plus)
            tiesD[1] = tplus;
            return plus;
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            sign[0] = -1;
            // correct = -min(tminus, minus)
            tiesD[1] = -tminus;
            return -minus;
        } else {
            // Two sided.
            sign[0] = Double.compare(plus, -minus);
            d = Math.max(plus, -minus);
            // correct = max(d, max(tplus, -tminus))
            tiesD[1] = Math.max(tplus, -tminus);
            return d;
        }
    }

    /**
     * Test if the two-sample integral Kolmogorov-Smirnov statistic is strictly greater
     * than the specified values for D+ and D-. Note that D- should have a negative sign
     * to impose an inclusive lower bound.
     *
     * <p>This method will destructively modify the input arrays (via a sort).
     *
     * <p>For a two-sided alternative hypothesis {@code plus} and {@code minus} should have the
     * same magnitude with opposite signs.
     *
     * <p>For a one-sided alternative hypothesis the value of {@code plus} or {@code minus}
     * should have the expected value of the statistic, and the opposite D should have the maximum
     * or minimum long value. The {@code minus} should be negatively signed:
     *
     * <ul>
     * <li>greater: {@code plus} = D, {@code minus} = {@link Long#MIN_VALUE}
     * <li>greater: {@code minus} = -D, {@code plus} = {@link Long#MAX_VALUE}
     * </ul>
     *
     * <p>Note: This method has not been specialized for the one-sided case. Specialization
     * would eliminate a conditional branch for {@code d > Long.MAX_VALUE} or
     * {@code d < Long.MIN_VALUE}. Since these branches are never possible in the one-sided case
     * this should be efficiently chosen by branch prediction in a processor pipeline.
     *
     * @param x First sample (destructively modified by sort; must not contain NaN).
     * @param y Second sample (destructively modified by sort; must not contain NaN).
     * @param plus Limit on D+ (inclusive upper bound).
     * @param minus Limit on D- (inclusive lower bound).
     * @return true if the D value exceeds the provided limits
     */
    private static boolean testIntegralKolmogorovSmirnovStatistic(double[] x, double[] y, long plus, long minus) {
        // Sort the sample arrays
        Arrays.sort(x);
        Arrays.sort(y);
        final int n = x.length;
        final int m = y.length;

        // CDFs range from 0 to 1 using increments of 1/n and 1/m for x and y respectively.
        // Scale by n*m to use increments of m and n for x and y.
        // Find the any difference that exceeds the specified bounds.
        int i = 0;
        int j = 0;
        long d = 0;
        do {
            // No NaNs so compare using < and >
            if (x[i] < y[j]) {
                final double z = x[i];
                do {
                    i++;
                    d += m;
                } while (i < n && x[i] == z);
                if (d > plus) {
                    return true;
                }
            } else if (x[i] > y[j]) {
                final double z = y[j];
                do {
                    j++;
                    d -= n;
                } while (j < m && y[j] == z);
                if (d < minus) {
                    return true;
                }
            } else {
                // Traverse to the end of the tied section for d.
                final double z = x[i];
                do {
                    i++;
                    d += m;
                } while (i < n && x[i] == z);
                do {
                    j++;
                    d -= n;
                } while (j < m && y[j] == z);
                // End of tied section
                if (d > plus || d < minus) {
                    return true;
                }
            }
        } while (i < n && j < m);
        // Note: Here d requires returning to zero. This is applicable to the one-sided
        // cases since d may have always been above zero (favours D+) or always below zero
        // (favours D-). This is ignored as the method is not called when dnm=0 is
        // outside the inclusive bounds.
        return false;
    }

    /**
     * Creates a sampler to sample randomly from the combined distribution of the two samples.
     *
     * @param x First sample.
     * @param y Second sample.
     * @param rng Source of randomness.
     * @return the sampler
     */
    private static DoubleSupplier createSampler(double[] x, double[] y,
                                                UniformRandomProvider rng) {
        return createSampler(x, y, rng, MAX_ARRAY_SIZE);
    }

    /**
     * Creates a sampler to sample randomly from the combined distribution of the two
     * samples. This will copy the input data for the sampler.
     *
     * @param x First sample.
     * @param y Second sample.
     * @param rng Source of randomness.
     * @param maxArraySize Maximum size of a single array.
     * @return the sampler
     */
    static DoubleSupplier createSampler(double[] x, double[] y,
                                        UniformRandomProvider rng,
                                        int maxArraySize) {
        final int n = x.length;
        final int m = y.length;
        final int len = n + m;
        // Overflow safe: len > maxArraySize
        if (len - maxArraySize > 0) {
            // Support sampling with maximum length arrays
            // (where a concatenated array is not possible)
            // by choosing one or the other.
            // - generate i in [-n, m)
            // - return i < 0 ? x[n + i] : y[i]
            // The sign condition is a 50-50 branch.
            // Perform branchless by extracting the sign bit to pick the array.
            // Copy the source data.
            final double[] xx = x.clone();
            final double[] yy = y.clone();
            final IntToDoubleFunction nextX = i -> xx[n + i];
            final IntToDoubleFunction nextY = i -> yy[i];
            // Arrange function which accepts the negative index at position [1]
            final IntToDoubleFunction[] next = {nextY, nextX};
            return () -> {
                final int i = rng.nextInt(-n, m);
                return next[i >>> 31].applyAsDouble(i);
            };
        }
        // Concatenate arrays
        final double[] z = new double[len];
        System.arraycopy(x, 0, z, 0, n);
        System.arraycopy(y, 0, z, n, m);
        return () -> z[rng.nextInt(len)];
    }

    /**
     * Compute the D statistic from the integral D value.
     *
     * @param dnm Integral D-statistic value (in [0, n*m]).
     * @param n First sample size.
     * @param m Second sample size.
     * @param gcd Greatest common divisor of n and m.
     * @return D-statistic value (in [0, 1]).
     */
    private static double computeD(long dnm, int n, int m, int gcd) {
        // Note: Integer division using the gcd is intentional
        final long a = dnm / gcd;
        final int b = m / gcd;
        return a / ((double) n * b);
    }

    /**
     * Computes \(P(D_{n,m} &gt; d)\) for the 2-sample Kolmogorov-Smirnov statistic.
     *
     * @param dnm Integral D-statistic value (in [0, n*m]).
     * @param n First sample size.
     * @param m Second sample size.
     * @param gcd Greatest common divisor of n and m.
     * @param d D-statistic value (in [0, 1]).
     * @param exact whether to compute the exact probability; otherwise approximate.
     * @return probability
     * @see #twoSampleExactP(long, int, int, int, boolean, boolean)
     * @see #twoSampleApproximateP(double, int, int, boolean)
     */
    private double twoSampleP(long dnm, int n, int m, int gcd, double d, boolean exact) {
        // Exact computation returns -1 if it cannot compute.
        double p = -1;
        if (exact) {
            p = twoSampleExactP(dnm, n, m, gcd, strictInequality, alternative == AlternativeHypothesis.TWO_SIDED);
        }
        if (p < 0) {
            p = twoSampleApproximateP(d, n, m, alternative == AlternativeHypothesis.TWO_SIDED);
        }
        return p;
    }

    /**
     * Computes \(P(D_{n,m} &gt; d)\) if {@code strict} is {@code true}; otherwise \(P(D_{n,m} \ge
     * d)\), where \(D_{n,m}\) is the 2-sample Kolmogorov-Smirnov statistic, either the two-sided
     * \(D_{n,m}\) or one-sided \(D_{n,m}^+\}. See
     * {@link #statistic(double[], double[])} for the definition of \(D_{n,m}\).
     *
     * <p>The returned probability is exact. If the value cannot be computed this returns -1.
     *
     * <p>Note: This requires the greatest common divisor of n and m. The integral D statistic
     * in the range [0, n*m] is separated by increments of the gcd. The method will only
     * compute p-values for valid values of D by calculating for D/gcd.
     * Strict inquality is performed using the next valid value for D.
     *
     * @param dnm Integral D-statistic value (in [0, n*m]).
     * @param n First sample size.
     * @param m Second sample size.
     * @param gcd Greatest common divisor of n and m.
     * @param strict whether or not the probability to compute is expressed as a strict inequality.
     * @param twoSided whether D refers to D or D+.
     * @return probability that a randomly selected m-n partition of m + n generates D
     *         greater than (resp. greater than or equal to) {@code d} (or -1)
     */
    static double twoSampleExactP(long dnm, int n, int m, int gcd, boolean strict, boolean twoSided) {
        // Create the statistic in [0, lcm]
        // For strict inequality D > d the result is the same if we compute for D >= (d+1)
        final long d = dnm / gcd + (strict ? 1 : 0);

        // P-value methods compute for d <= lcm (least common multiple)
        final long lcm = (long) n * (m / gcd);
        if (d > lcm) {
            return 0;
        }

        // Note: Some methods require m >= n, others n >= m
        final int a = Math.min(n, m);
        final int b = Math.max(n, m);

        if (twoSided) {
            // Any two-sided statistic dnm cannot be less than min(n, m) in the absence of ties.
            if (d * gcd <= a) {
                return 1;
            }
            // Here d in [2, lcm]
            if (n == m) {
                return twoSampleTwoSidedPOutsideSquare(d, n);
            }
            return twoSampleTwoSidedPStabilizedInner(d, b, a, gcd);
        }
        // Any one-sided statistic cannot be less than 0
        if (d <= 0) {
            return 1;
        }
        // Here d in [1, lcm]
        if (n == m) {
            return twoSampleOneSidedPOutsideSquare(d, n);
        }
        return twoSampleOneSidedPOutside(d, a, b, gcd);
    }

    /**
     * Computes \(P(D_{n,m} \ge d)\), where \(D_{n,m}\) is the 2-sample Kolmogorov-Smirnov statistic.
     *
     * <p>The returned probability is exact, implemented using the stabilized inner method
     * presented in Viehmann (2021).
     *
     * <p>This is optimized for {@code m <= n}. If {@code m > n} and index-out-of-bounds
     * exception can occur.
     *
     * @param d Integral D-statistic value (in [2, lcm])
     * @param n Larger sample size.
     * @param m Smaller sample size.
     * @param gcd Greatest common divisor of n and m.
     * @return probability that a randomly selected m-n partition of m + n generates \(D_{n,m}\)
     *         greater than or equal to {@code d}
     */
    private static double twoSampleTwoSidedPStabilizedInner(long d, int n, int m, int gcd) {
        // Check the computation is possible.
        // Note that the possible paths is binom(m+n, n).
        // However the computation is stable above this limit.
        // Possible d values (requiring a unique p-value) = max(dnm) / gcd = lcm(n, m).
        // Possible terms to compute <= n * size(cij)
        // Simple limit based on the number of possible different p-values
        if ((long) n * (m / gcd) > MAX_LCM_TWO_SAMPLE_EXACT_P) {
            return -1;
        }

        // This could be updated to use d in [1, lcm].
        // Currently it uses d in [gcd, n*m].
        // Largest intermediate value is (dnm + im + n) which is within 2^63
        // if n and m are 2^31-1, i = n, dnm = n*m: (2^31-1)^2 + (2^31-1)^2 + 2^31-1 < 2^63
        final long dnm = d * gcd;

        // Viehmann (2021): Updated for i in [0, n], j in [0, m]
        // C_i,j = 1                                      if |i/n - j/m| >= d
        //       = 0                                      if |i/n - j/m| < d and (i=0 or j=0)
        //       = C_i-1,j * i/(i+j) + C_i,j-1 * j/(i+j)  otherwise
        // P2 = C_n,m
        // Note: The python listing in Viehmann used d in [0, 1]. This uses dnm in [0, nm]
        // so updates the scaling to compute the ranges. Also note that the listing uses
        // dist > d or dist < -d where this uses |dist| >= d to compute P(D >= d) (non-strict inequality).
        // The provided listing is explicit in the values for each j in the range.
        // It can be optimized given the known start and end j for each iteration as only
        // j where |i/n - j/m| < d must be processed:
        // startJ where: im - jn < dnm : jn > im - dnm
        // j = floor((im - dnm) / n) + 1      in [0, m]
        // endJ where: jn - im >= dnm
        // j = ceil((dnm + im) / n)           in [0, m+1]

        // First iteration with i = 0
        // j = ceil(dnm / n)
        int endJ = Math.min(m + 1, (int) ((dnm + n - 1) / n));

        // Only require 1 array to store C_i-1,j as the startJ only ever increases
        // and we update lower indices using higher ones.
        // The maximum value *written* is j=m or less using j/m <= 2*d : j = ceil(2*d*m)
        // Viehmann uses: size = int(2*m*d + 2) == ceil(2*d*m) + 1
        // The maximum value *read* is j/m <= 2*d. This may be above m. This occurs when
        // j - lastStartJ > m and C_i-1,j = 1. This can be avoided if (startJ - lastStartJ) <= 1
        // which occurs if m <= n, i.e. the window only slides 0 or 1 in j for each increment i
        // and we can maintain Cij as 1 larger than ceil(2*d*m) + 1.
        final double[] cij = new double[Math.min(m + 1, 2 * endJ + 2)];

        // Each iteration fills C_i,j with values and the remaining values are
        // kept as 1 for |i/n - j/m| >= d
        //assert (endJ - 1) * (long) n < dnm : "jn >= dnm for j < endJ";
        for (int j = endJ; j < cij.length; j++) {
            //assert j * (long) n >= dnm : "jn < dnm for j >= endJ";
            cij[j] = 1;
        }

        int startJ = 0;
        int length = endJ;
        double val = -1;
        long im = 0;
        for (int i = 1; i <= n; i++) {
            im += m;
            final int lastStartJ = startJ;

            // Compute C_i,j for startJ <= j < endJ
            // startJ = floor((im - dnm) / n) + 1      in [0, m]
            // endJ   = ceil((dnm + im) / n)           in [0, m+1]
            startJ = im < dnm ? 0 : Math.min(m, (int) ((im - dnm) / n) + 1);
            endJ = Math.min(m + 1, (int) ((dnm + im + n - 1) / n));

            if (startJ >= endJ) {
                // No possible paths inside the boundary
                return 1;
            }

            //assert startJ - lastStartJ <= 1 : "startJ - lastStartJ > 1";

            // Initialize previous value C_i,j-1
            val = startJ == 0 ? 0 : 1;

            //assert startJ == 0 || Math.abs(im - (startJ - 1) * (long) n) >= dnm : "|im - jn| < dnm for j < startJ";
            //assert endJ > m || Math.abs(im - endJ * (long) n) >= dnm : "|im - jn| < dnm for j >= endJ";
            for (int j = startJ; j < endJ; j++) {
                //assert j == 0 || Math.abs(im - j * (long) n) < dnm : "|im - jn| >= dnm for startJ <= j < endJ";
                // C_i,j = C_i-1,j * i/(i+j) + C_i,j-1 * j/(i+j)
                // Note: if (j - lastStartJ) >= cij.length this creates an IOOB exception.
                // In this case cij[j - lastStartJ] == 1. Only happens when m >= n.
                // Fixed using c_i-1,j = (j - lastStartJ >= cij.length ? 1 : cij[j - lastStartJ]
                val = (cij[j - lastStartJ] * i + val * j) / ((double) i + j);
                cij[j - startJ] = val;
            }

            // Must keep the remaining values in C_i,j as 1 to allow
            // cij[j - lastStartJ] * i == i when (j - lastStartJ) > lastLength
            final int lastLength = length;
            length = endJ - startJ;
            for (int j = lastLength - length - 1; j >= 0; j--) {
                cij[length + j] = 1;
            }
        }
        // Return the most recently written value: C_n,m
        return val;
    }

    /**
     * Computes \(P(D_{n,m}^+ \ge d)\), where \(D_{n,m}^+\) is the 2-sample one-sided
     * Kolmogorov-Smirnov statistic.
     *
     * <p>The returned probability is exact, implemented using the outer method
     * presented in Hodges (1958).
     *
     * <p>This method will fail-fast and return -1 if the computation of the
     * numbers of paths overflows.
     *
     * @param d Integral D-statistic value (in [0, lcm])
     * @param n First sample size.
     * @param m Second sample size.
     * @param gcd Greatest common divisor of n and m.
     * @return probability that a randomly selected m-n partition of m + n generates \(D_{n,m}\)
     *         greater than or equal to {@code d}
     */
    private static double twoSampleOneSidedPOutside(long d, int n, int m, int gcd) {
        // Hodges, Fig.2
        // Lower boundary: (nx - my)/nm >= d : (nx - my) >= dnm
        // B(x, y) is the number of ways from (0, 0) to (x, y) without previously
        // reaching the boundary.
        // B(x, y) = binom(x+y, y) - [number of ways which previously reached the boundary]
        // Total paths:
        // sum_y { B(x, y) binom(m+n-x-y, n-y) }

        // Normalized by binom(m+n, n). Check this is possible.
        final long lm = m;
        if (n + lm > Integer.MAX_VALUE) {
            return -1;
        }
        final double binom = binom(m + n, n);
        if (binom == Double.POSITIVE_INFINITY) {
            return -1;
        }

        // This could be updated to use d in [1, lcm].
        // Currently it uses d in [gcd, n*m].
        final long dnm = d * gcd;

        // Visit all x in [0, m] where (nx - my) >= d for each increasing y in [0, n].
        // x = ceil( (d + my) / n ) = (d + my + n - 1) / n
        // y = ceil( (nx - d) / m ) = (nx - d + m - 1) / m
        // Note: n m integer, d in [0, nm], the intermediate cannot overflow a long.
        // x | y=0 = (d + n - 1) / n
        final int x0 = (int) ((dnm + n - 1) / n);
        if (x0 >= m) {
            return 1 / binom;
        }
        // The y above is the y *on* the boundary. Set the limit as the next y above:
        // y | x=m = 1 + floor( (nx - d) / m ) = 1 + (nm - d) / m
        final int maxy = (int) ((n * lm - dnm + m) / m);
        // Compute x and B(x, y) for visited B(x,y)
        final int[] xy = new int[maxy];
        final double[] bxy = new double[maxy];
        xy[0] = x0;
        bxy[0] = 1;
        for (int y = 1; y < maxy; y++) {
            final int x = (int) ((dnm + lm * y + n - 1) / n);
            // B(x, y) = binom(x+y, y) - [number of ways which previously reached the boundary]
            // Add the terms to subtract as a negative sum.
            final Sum b = Sum.create();
            for (int yy = 0; yy < y; yy++) {
                // Here: previousX = x - xy[yy] : previousY = y - yy
                // bxy[yy] is the paths to (previousX, previousY)
                // binom represent the paths from (previousX, previousY) to (x, y)
                b.addProduct(bxy[yy], -binom(x - xy[yy] + y - yy, y - yy));
            }
            b.add(binom(x + y, y));
            xy[y] = x;
            bxy[y] = b.getAsDouble();
        }
        // sum_y { B(x, y) binom(m+n-x-y, n-y) }
        final Sum sum = Sum.create();
        for (int y = 0; y < maxy; y++) {
            sum.addProduct(bxy[y], binom(m + n - xy[y] - y, n - y));
        }
        // No individual term should have overflowed since binom is finite.
        // Any sum above 1 is floating-point error.
        return KolmogorovSmirnovDistribution.clipProbability(sum.getAsDouble() / binom);
    }

    /**
     * Computes \(P(D_{n,n}^+ \ge d)\), where \(D_{n,n}^+\) is the 2-sample one-sided
     * Kolmogorov-Smirnov statistic.
     *
     * <p>The returned probability is exact, implemented using the outer method
     * presented in Hodges (1958).
     *
     * @param d Integral D-statistic value (in [1, lcm])
     * @param n Sample size.
     * @return probability that a randomly selected m-n partition of m + n generates \(D_{n,m}\)
     *         greater than or equal to {@code d}
     */
    private static double twoSampleOneSidedPOutsideSquare(long d, int n) {
        // Hodges (1958) Eq. 2.3:
        // p = binom(2n, n-a) / binom(2n, n)
        // a in [1, n] == d * n == dnm / n
        final int a = (int) d;

        // Rearrange:
        // p = ( 2n! / ((n-a)! (n+a)!) ) / ( 2n! / (n! n!) )
        //   = n! n! / ( (n-a)! (n+a)! )
        // Perform using pre-computed factorials if possible.
        if (n + a <= MAX_FACTORIAL) {
            final double x = Factorial.doubleValue(n);
            final double y = Factorial.doubleValue(n - a);
            final double z = Factorial.doubleValue(n + a);
            return (x / y) * (x / z);
        }
        // p = n! / (n-a)!  *  n! / (n+a)!
        //       n * (n-1) * ... * (n-a+1)
        //   = -----------------------------
        //     (n+a) * (n+a-1) * ... * (n+1)

        double p = 1;
        for (int i = 0; i < a && p != 0; i++) {
            p *= (n - i) / (1.0 + n + i);
        }
        return p;
    }

    /**
     * Computes \(P(D_{n,n}^+ \ge d)\), where \(D_{n,n}^+\) is the 2-sample two-sided
     * Kolmogorov-Smirnov statistic.
     *
     * <p>The returned probability is exact, implemented using the outer method
     * presented in Hodges (1958).
     *
     * @param d Integral D-statistic value (in [1, n])
     * @param n Sample size.
     * @return probability that a randomly selected m-n partition of n + n generates \(D_{n,n}\)
     *         greater than or equal to {@code d}
     */
    private static double twoSampleTwoSidedPOutsideSquare(long d, int n) {
        // Hodges (1958) Eq. 2.4:
        // p = 2 [ binom(2n, n-a) - binom(2n, n-2a) + binom(2n, n-3a) - ... ] / binom(2n, n)
        // a in [1, n] == d * n == dnm / n

        // As per twoSampleOneSidedPOutsideSquare, divide by binom(2n, n) and each term
        // can be expressed as a product:
        //         (             n - i                    n - i                   n - i         )
        // p = 2 * ( prod_i=0^a --------- - prod_i=0^2a --------- + prod_i=0^3a --------- + ... )
        //         (           1 + n + i                1 + n + i               1 + n + i       )
        // for ja in [1, ..., n/a]
        // Avoid repeat computation of terms by extracting common products:
        // p = 2 * ( p0a * (1 - p1a * (1 - p2a * (1 - ... ))) )
        // where each term pja is prod_i={ja}^{ja+a} for all j in [1, n / a]

        // The first term is the one-sided p.
        final double p0a = twoSampleOneSidedPOutsideSquare(d, n);
        if (p0a == 0) {
            // Underflow - nothing more to do
            return 0;
        }
        // Compute the inner-terms from small to big.
        // j = n / (d/n) ~ n*n / d
        // j is a measure of how extreme the d value is (small j is extreme d).
        // When j is above 0 a path may traverse from the lower boundary to the upper boundary.
        final int a = (int) d;
        double p = 0;
        for (int j = n / a; j > 0; j--) {
            double pja = 1;
            final int jaa = j * a + a;
            // Since p0a did not underflow we avoid the check for pj != 0
            for (int i = j * a; i < jaa; i++) {
                pja *= (n - i) / (1.0 + n + i);
            }
            p = pja * (1 - p);
        }
        p = p0a * (1 - p);
        return Math.min(1, 2 * p);
    }

    /**
     * Compute the binomial coefficient binom(n, k).
     *
     * @param n N.
     * @param k K.
     * @return binom(n, k)
     */
    private static double binom(int n, int k) {
        return BinomialCoefficientDouble.value(n, k);
    }

    /**
     * Uses the Kolmogorov-Smirnov distribution to approximate \(P(D_{n,m} &gt; d)\) where \(D_{n,m}\)
     * is the 2-sample Kolmogorov-Smirnov statistic. See
     * {@link #statistic(double[], double[])} for the definition of \(D_{n,m}\).
     *
     * <p>Specifically, what is returned is \(1 - CDF(d, \sqrt{mn / (m + n)})\) where CDF
     * is the cumulative density function of the two-sided one-sample Kolmogorov-Smirnov
     * distribution.
     *
     * @param d D-statistic value.
     * @param n First sample size.
     * @param m Second sample size.
     * @param twoSided True to compute the two-sided p-value; else one-sided.
     * @return approximate probability that a randomly selected m-n partition of m + n generates
     *         \(D_{n,m}\) greater than {@code d}
     */
    static double twoSampleApproximateP(double d, int n, int m, boolean twoSided) {
        final double nn = Math.min(n, m);
        final double mm = Math.max(n, m);
        if (twoSided) {
            // Smirnov's asymptotic formula:
            // P(sqrt(N) D_n > x)
            // N = m*n/(m+n)
            return KolmogorovSmirnovDistribution.Two.sf(d, (int) Math.round(mm * nn / (mm + nn)));
        }
        // one-sided
        // Use Hodges Eq 5.3. Requires m >= n
        // Correct for m=n, m an integral multiple of n, and 'on the average' for m nearly equal to n
        final double z = d * Math.sqrt(nn * mm / (nn + mm));
        return Math.exp(-2 * z * z - 2 * z * (mm + 2 * nn) / Math.sqrt(mm * nn * (mm + nn)) / 3);
    }

    /**
     * Verifies that {@code array} has length at least 2.
     *
     * @param array Array to test.
     * @return the length
     * @throws IllegalArgumentException if array is too short
     */
    private static int checkArrayLength(double[] array) {
        final int n = array.length;
        if (n <= 1) {
            throw new InferenceException(InferenceException.TWO_VALUES_REQUIRED, n);
        }
        return n;
    }

    /**
     * Sort the input array. Throws an exception if NaN values are
     * present. It is assumed the array is non-zero length.
     *
     * @param x Input array.
     * @param name Name of the array.
     * @return a reference to the input (sorted) array
     * @throws IllegalArgumentException if {@code x} contains NaN values.
     */
    private static double[] sort(double[] x, String name) {
        Arrays.sort(x);
        // NaN will be at the end
        if (Double.isNaN(x[x.length - 1])) {
            throw new InferenceException(name + " contains NaN");
        }
        return x;
    }
}
