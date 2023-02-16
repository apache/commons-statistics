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
import org.apache.commons.statistics.distribution.TDistribution;

/**
 * Implements Student's t-test statistics.
 *
 * <p>Tests can be:
 * <ul>
 * <li>One-sample or two-sample
 * <li>One-sided or two-sided
 * <li>Paired or unpaired (for two-sample tests)
 * <li>Homoscedastic (equal variance assumption) or heteroscedastic (for two sample tests)
 * </ul>
 *
 * <p>Input to tests can be either {@code double[]} arrays or the mean, variance, and size
 * of the sample.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Student%27s_t-test">Student&#39;s t-test (Wikipedia)</a>
 * @since 1.1
 */
public final class TTest {
    /** Default instance. */
    private static final TTest DEFAULT = new TTest(AlternativeHypothesis.TWO_SIDED, false, 0);

    /** Alternative hypothesis. */
    private final AlternativeHypothesis alternative;
    /** Assume the two samples have the same population variance. */
    private final boolean equalVariances;
    /** The true value of the mean (or difference in means for a two sample test). */
    private final double mu;

    /**
     * Result for the t-test.
     *
     * <p>This class is immutable.
     */
    public static final class Result extends BaseSignificanceResult {
        /** Degrees of freedom. */
        private final double degreesOfFreedom;

        /**
         * Create an instance.
         *
         * @param statistic Test statistic.
         * @param degreesOfFreedom Degrees of freedom.
         * @param p Result p-value.
         */
        Result(double statistic, double degreesOfFreedom, double p) {
            super(statistic, p);
            this.degreesOfFreedom = degreesOfFreedom;
        }

        /**
         * Gets the degrees of freedom.
         *
         * @return the degrees of freedom
         */
        public double getDegreesOfFreedom() {
            return degreesOfFreedom;
        }
    }

    /**
     * @param alternative Alternative hypothesis.
     * @param equalVariances Assume the two samples have the same population variance.
     * @param mu true value of the mean (or difference in means for a two sample test).
     */
    private TTest(AlternativeHypothesis alternative, boolean equalVariances, double mu) {
        this.alternative = alternative;
        this.equalVariances = equalVariances;
        this.mu = mu;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     * <li>{@link AlternativeHypothesis#TWO_SIDED}
     * <li>{@link DataDispersion#HETEROSCEDASTIC}
     * <li>{@linkplain #withMu(double) mu = 0}
     * </ul>
     *
     * @return default instance
     */
    public static TTest withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured alternative hypothesis.
     *
     * @param v Value.
     * @return an instance
     */
    public TTest with(AlternativeHypothesis v) {
        return new TTest(Objects.requireNonNull(v), equalVariances, mu);
    }

    /**
     * Return an instance with the configured assumption on the data dispersion.
     *
     * <p>Applies to the two-sample independent t-test.
     * The statistic can compare the means without the assumption of equal
     * sub-population variances (heteroscedastic); otherwise the means are compared
     * under the assumption of equal sub-population variances (homoscedastic).
     *
     * @param v Value.
     * @return an instance
     * @see #test(double[], double[])
     * @see #test(double, double, long, double, double, long)
     */
    public TTest with(DataDispersion v) {
        return new TTest(alternative, Objects.requireNonNull(v) == DataDispersion.HOMOSCEDASTIC, mu);
    }

    /**
     * Return an instance with the configured {@code mu}.
     *
     * <p>For the one-sample test this is the expected mean.
     *
     * <p>For the two-sample test this is the expected difference between the means.
     *
     * @param v Value.
     * @return an instance
     * @throws IllegalArgumentException if the value is not finite
     */
    public TTest withMu(double v) {
        return new TTest(alternative, equalVariances, Arguments.checkFinite(v));
    }

    /**
     * Computes a one-sample t statistic comparing the mean of the dataset to {@code mu}.
     *
     * <p>The returned t-statistic is:
     *
     * <p>\[ t = \frac{m - \mu}{ \sqrt{ \frac{v}{n} } } \]
     *
     * @param m Sample mean.
     * @param v Sample variance.
     * @param n Sample size.
     * @return t statistic
     * @throws IllegalArgumentException if the number of samples is {@code < 2}; or the
     * variance is negative
     * @see #withMu(double)
     */
    public double statistic(double m, double v, long n) {
        Arguments.checkNonNegative(v);
        checkSampleSize(n);
        return computeT(m - mu, v, n);
    }

    /**
     * Computes a one-sample t statistic comparing the mean of the sample to {@code mu}.
     *
     * @param x Sample values.
     * @return t statistic
     * @throws IllegalArgumentException if the number of samples is {@code < 2}
     * @see #statistic(double, double, long)
     * @see #withMu(double)
     */
    public double statistic(double[] x) {
        final long n = checkSampleSize(x.length);
        final double m = StatisticUtils.mean(x);
        final double v = StatisticUtils.variance(x, m);
        return computeT(m - mu, v, n);
    }

    /**
     * Computes a paired two-sample t-statistic on related samples comparing the mean difference
     * between the samples to {@code mu}.
     *
     * <p>The t-statistic returned is functionally equivalent to what would be returned by computing
     * the one-sample t-statistic {@link #statistic(double[])}, with
     * the sample array consisting of the (signed) differences between corresponding
     * entries in {@code x} and {@code y}.
     *
     * @param x First sample values.
     * @param y Second sample values.
     * @return t statistic
     * @throws IllegalArgumentException if the number of samples is {@code < 2}; or the
     * the size of the samples is not equal
     * @see #withMu(double)
     */
    public double pairedStatistic(double[] x, double[] y) {
        final long n = checkSampleSize(x.length);
        final double m = StatisticUtils.meanDifference(x, y);
        final double v = StatisticUtils.varianceDifference(x, y, m);
        return computeT(m - mu, v, n);
    }

    /**
     * Computes a two-sample t statistic on independent samples comparing the difference in means
     * of the datasets to {@code mu}.
     *
     * <p>Use the {@link DataDispersion} to control the computation of the variance.
     *
     * <p>The heteroscedastic t-statistic is:
     *
     * <p>\[ t = \frac{m1 - m2 - \mu}{ \sqrt{ \frac{v_1}{n_1} + \frac{v_2}{n_2} } } \]
     *
     * <p>The homoscedastic t-statistic is:
     *
     * <p>\[ t = \frac{m1 - m2 - \mu}{ \sqrt{ v (\frac{1}{n_1} + \frac{1}{n_2}) } } \]
     *
     * <p>where \( v \) is the pooled variance estimate:
     *
     * <p>\[ v = \frac{(n_1-1)v_1 + (n_2-1)v_2}{n_1 + n_2 - 2} \]
     *
     * @param m1 First sample mean.
     * @param v1 First sample variance.
     * @param n1 First sample size.
     * @param m2 Second sample mean.
     * @param v2 Second sample variance.
     * @param n2 Second sample size.
     * @return t statistic
     * @throws IllegalArgumentException if the number of samples in either dataset is
     * {@code < 2}; or the variances are negative.
     * @see #withMu(double)
     * @see #with(DataDispersion)
     */
    public double statistic(double m1, double v1, long n1,
                            double m2, double v2, long n2) {
        Arguments.checkNonNegative(v1);
        Arguments.checkNonNegative(v2);
        checkSampleSize(n1);
        checkSampleSize(n2);
        return equalVariances ?
            computeHomoscedasticT(mu, m1, v1, n1, m2, v2, n2) :
            computeT(mu, m1, v1, n1, m2, v2, n2);
    }

    /**
     * Computes a two-sample t statistic on independent samples comparing the difference
     * in means of the samples to {@code mu}.
     *
     * <p>Use the {@link DataDispersion} to control the computation of the variance.
     *
     * @param x First sample values.
     * @param y Second sample values.
     * @return t statistic
     * @throws IllegalArgumentException if the number of samples in either dataset is {@code < 2}
     * @see #withMu(double)
     * @see #with(DataDispersion)
     */
    public double statistic(double[] x, double[] y) {
        final long n1 = checkSampleSize(x.length);
        final long n2 = checkSampleSize(y.length);
        final double m1 = StatisticUtils.mean(x);
        final double m2 = StatisticUtils.mean(y);
        final double v1 = StatisticUtils.variance(x, m1);
        final double v2 = StatisticUtils.variance(y, m2);
        return equalVariances ?
            computeHomoscedasticT(mu, m1, v1, n1, m2, v2, n2) :
            computeT(mu, m1, v1, n1, m2, v2, n2);
    }

    /**
     * Perform a one-sample t-test comparing the mean of the dataset to {@code mu}.
     *
     * <p>Degrees of freedom are \( v = n - 1 \).
     *
     * @param m Sample mean.
     * @param v Sample variance.
     * @param n Sample size.
     * @return test result
     * @throws IllegalArgumentException if the number of samples is {@code < 2}; or the
     * variance is negative
     * @see #statistic(double, double, long)
     */
    public Result test(double m, double v, long n) {
        final double t = statistic(m, v, n);
        final double df = n - 1.0;
        final double p = computeP(t, df);
        return new Result(t, df, p);
    }

    /**
     * Performs a one-sample t-test comparing the mean of the sample to {@code mu}.
     *
     * <p>Degrees of freedom are \( v = n - 1 \).
     *
     * @param sample Sample values.
     * @return the test result
     * @throws IllegalArgumentException if the number of samples is {@code < 2}; or the
     * the size of the samples is not equal
     * @see #statistic(double[])
     */
    public Result test(double[] sample) {
        final double t = statistic(sample);
        final double df = sample.length - 1.0;
        final double p = computeP(t, df);
        return new Result(t, df, p);
    }

    /**
     * Performs a paired two-sample t-test on related samples comparing the mean difference between
     * the samples to {@code mu}.
     *
     * <p>The test is functionally equivalent to what would be returned by computing
     * the one-sample t-test {@link #test(double[])}, with
     * the sample array consisting of the (signed) differences between corresponding
     * entries in {@code x} and {@code y}.
     *
     * @param x First sample values.
     * @param y Second sample values.
     * @return the test result
     * @throws IllegalArgumentException if the number of samples is {@code < 2}; or the
     * the size of the samples is not equal
     * @see #pairedStatistic(double[], double[])
     */
    public Result pairedTest(double[] x, double[] y) {
        final double t = pairedStatistic(x, y);
        final double df = x.length - 1.0;
        final double p = computeP(t, df);
        return new Result(t, df, p);
    }

    /**
     * Performs a two-sample t-test on independent samples comparing the difference in means of the
     * datasets to {@code mu}.
     *
     * <p>Use the {@link DataDispersion} to control the computation of the variance.
     *
     * <p>The heteroscedastic degrees of freedom are estimated using the
     * Welch-Satterthwaite approximation:
     *
     * <p>\[ v = \frac{ (\frac{v_1}{n_1} + \frac{v_2}{n_2})^2 }
     *                { \frac{(v_1/n_1)^2}{n_1-1} + \frac{(v_2/n_2)^2}{n_2-1} } \]
     *
     * <p>The homoscedastic degrees of freedom are \( v = n_1 + n_2 - 2 \).
     *
     * @param m1 First sample mean.
     * @param v1 First sample variance.
     * @param n1 First sample size.
     * @param m2 Second sample mean.
     * @param v2 Second sample variance.
     * @param n2 Second sample size.
     * @return test result
     * @throws IllegalArgumentException if the number of samples in either dataset is
     * {@code < 2}; or the variances are negative.
     * @see #statistic(double, double, long, double, double, long)
     */
    public Result test(double m1, double v1, long n1,
                       double m2, double v2, long n2) {
        final double t = statistic(m1, v1, n1, m2, v2, n2);
        final double df = equalVariances ?
                -2.0 + n1 + n2 :
                computeDf(v1, n1, v2, n2);
        final double p = computeP(t, df);
        return new Result(t, df, p);
    }

    /**
     * Performs a two-sample t-test on independent samples comparing the difference in means of
     * the samples to {@code mu}.
     *
     * <p>Use the {@link DataDispersion} to control the computation of the variance.
     *
     * @param x First sample values.
     * @param y Second sample values.
     * @return the test result
     * @throws IllegalArgumentException if the number of samples in either dataset
     * is {@code < 2}
     * @see #statistic(double[], double[])
     * @see #test(double, double, long, double, double, long)
     */
    public Result test(double[] x, double[] y) {
        // Here we do not call statistic(double[], double[]) because the degreesOfFreedom
        // requires the variance. So repeat the computation and compute p.
        final long n1 = checkSampleSize(x.length);
        final long n2 = checkSampleSize(y.length);
        final double m1 = StatisticUtils.mean(x);
        final double m2 = StatisticUtils.mean(y);
        final double v1 = StatisticUtils.variance(x, m1);
        final double v2 = StatisticUtils.variance(y, m2);
        double t;
        double df;
        if (equalVariances) {
            t = computeHomoscedasticT(mu, m1, v1, n1, m2, v2, n2);
            df = -2.0 + n1 + n2;
        } else {
            t = computeT(mu, m1, v1, n1, m2, v2, n2);
            df = computeDf(v1, n1, v2, n2);
        }
        final double p = computeP(t, df);
        return new Result(t, df, p);
    }

    /**
     * Computes t statistic for one-sample t-test.
     *
     * @param m Sample mean.
     * @param v Sample variance.
     * @param n Sample size.
     * @return t test statistic
     */
    private static double computeT(double m, double v, long n) {
        return m / Math.sqrt(v / n);
    }

    /**
     * Computes t statistic for two-sample t-test without the assumption of equal
     * samples sizes or sub-population variances.
     *
     * @param mu Expected difference between means.
     * @param m1 First sample mean.
     * @param v1 First sample variance.
     * @param n1 First sample size.
     * @param m2 Second sample mean.
     * @param v2 Second sample variance.
     * @param n2 Second sample size.
     * @return t test statistic
     */
    private static double computeT(double mu,
                                   double m1, double v1, long n1,
                                   double m2, double v2, long n2)  {
        return (m1 - m2 - mu) / Math.sqrt((v1 / n1) + (v2 / n2));
    }

    /**
     * Computes approximate degrees of freedom for two-sample t-test without the
     * assumption of equal samples sizes or sub-population variances.
     *
     * @param v1 First sample variance.
     * @param n1 First sample size.
     * @param v2 Second sample variance.
     * @param n2 Second sample size.
     * @return approximate degrees of freedom
     */
    private static double computeDf(double v1, long n1,
                                    double v2, long n2) {
        // Sample sizes are specified as a double to avoid integer overflow
        final double d1 = n1;
        final double d2 = n2;
        return (((v1 / d1) + (v2 / d2)) * ((v1 / d1) + (v2 / d2))) /
            ((v1 * v1) / (d1 * d1 * (n1 - 1)) + (v2 * v2) / (d2 * d2 * (n2 - 1)));
    }

    /**
     * Computes t statistic for two-sample t-test under the hypothesis of equal
     * sub-population variances.
     *
     * @param mu Expected difference between means.
     * @param m1 First sample mean.
     * @param v1 First sample variance.
     * @param n1 First sample size.
     * @param m2 Second sample mean.
     * @param v2 Second sample variance.
     * @param n2 Second sample size.
     * @return t test statistic
     */
    private static double computeHomoscedasticT(double mu,
                                                double m1, double v1, long n1,
                                                double m2, double v2, long n2)  {
        final double pooledVariance = ((n1 - 1) * v1 + (n2 - 1) * v2) / (-2.0 + n1 + n2);
        return (m1 - m2 - mu) / Math.sqrt(pooledVariance * (1.0 / n1 + 1.0 / n2));
    }

    /**
     * Computes p-value for the specified t statistic.
     *
     * @param t T statistic.
     * @param degreesOfFreedom Degrees of freedom.
     * @return p-value for t-test
     */
    private double computeP(double t, double degreesOfFreedom) {
        if (alternative == AlternativeHypothesis.LESS_THAN) {
            return TDistribution.of(degreesOfFreedom).cumulativeProbability(t);
        }
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            return TDistribution.of(degreesOfFreedom).survivalProbability(t);
        }
        // two-sided
        return 2.0 * TDistribution.of(degreesOfFreedom).survivalProbability(Math.abs(t));
    }

    /**
     * Check sample data size.
     *
     * @param n Data size.
     * @return the sample size
     * @throws IllegalArgumentException if the number of samples {@code < 2}
     */
    private static long checkSampleSize(long n) {
        if (n <= 1) {
            throw new InferenceException(InferenceException.TWO_VALUES_REQUIRED, n);
        }
        return n;
    }
}
