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

import java.util.Objects;
import org.apache.commons.statistics.distribution.BinomialDistribution;

/**
 * Implements binomial test statistics.
 *
 * <p>Performs an exact test for the statistical significance of deviations from a
 * theoretically expected distribution of observations into two categories.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Binomial_test">Binomial test (Wikipedia)</a>
 * @since 1.1
 */
public final class BinomialTest {
    /** Default instance. */
    private static final BinomialTest DEFAULT = new BinomialTest(AlternativeHypothesis.TWO_SIDED);

    /** Alternative hypothesis. */
    private final AlternativeHypothesis alternative;

    /**
     * @param alternative Alternative hypothesis.
     */
    private BinomialTest(AlternativeHypothesis alternative) {
        this.alternative = alternative;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     * <li>{@link AlternativeHypothesis#TWO_SIDED}</li>
     * </ul>
     *
     * @return default instance
     */
    public static BinomialTest withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured alternative hypothesis.
     *
     * @param v Value.
     * @return an instance
     */
    public BinomialTest with(AlternativeHypothesis v) {
        return new BinomialTest(Objects.requireNonNull(v));
    }

    /**
     * Performs a binomial test about the probability of success \( \pi \).
     *
     * <p>The null hypothesis is \( H_0:\pi=\pi_0 \) where \( \pi_0 \) is between 0 and 1.
     *
     * <p>The probability of observing \( k \) successes from \( n \) trials with a given
     * probability of success \( p \) is:
     *
     * <p>\[ \Pr(X=k)=\binom{n}{k}p^k(1-p)^{n-k} \]
     *
     * <p>The test is defined by the {@link AlternativeHypothesis}.
     *
     * <p>To test \( \pi &lt; \pi_0 \) (less than):
     *
     * <p>\[ p = \sum_{i=0}^k\Pr(X=i)=\sum_{i=0}^k\binom{n}{i}\pi_0^i(1-\pi_0)^{n-i} \]
     *
     * <p>To test \( \pi &gt; \pi_0 \) (greater than):
     *
     * <p>\[ p = \sum_{i=0}^k\Pr(X=i)=\sum_{i=k}^n\binom{n}{i}\pi_0^i(1-\pi_0)^{n-i} \]
     *
     * <p>To test \( \pi \ne \pi_0 \) (two-sided) requires finding all \( i \) such that
     * \( \mathcal{I}=\{i:\Pr(X=i)\leq \Pr(X=k)\} \) and compute the sum:
     *
     * <p>\[ p = \sum_{i\in\mathcal{I}}\Pr(X=i)=\sum_{i\in\mathcal{I}}\binom{n}{i}\pi_0^i(1-\pi_0)^{n-i} \]
     *
     * <p>The two-sided p-value represents the likelihood of getting a result at least as
     * extreme as the sample, given the provided {@code probability} of success on a
     * single trial.
     *
     * <p>The test statistic is equal to the estimated proportion \( \frac{k}{n} \).
     *
     * @param numberOfTrials Number of trials performed.
     * @param numberOfSuccesses Number of successes observed.
     * @param probability Assumed probability of a single trial under the null
     * hypothesis.
     * @return test result
     * @throws IllegalArgumentException if {@code numberOfTrials} or
     * {@code numberOfSuccesses} is negative; {@code probability} is not between 0
     * and 1; or if {@code numberOfTrials < numberOfSuccesses}
     * @see #with(AlternativeHypothesis)
     */
    public SignificanceResult test(int numberOfTrials, int numberOfSuccesses, double probability) {
        // Note: The distribution validates number of trials and probability.
        // Here we only have to validate the number of successes.
        Arguments.checkNonNegative(numberOfSuccesses);
        if (numberOfTrials < numberOfSuccesses) {
            throw new InferenceException(
                "must have n >= k for binomial coefficient (n, k), got n = %d, k = %d",
                numberOfSuccesses, numberOfTrials);
        }

        final BinomialDistribution distribution = BinomialDistribution.of(numberOfTrials, probability);
        final double p;
        if (alternative == AlternativeHypothesis.GREATER_THAN) {
            p = distribution.survivalProbability(numberOfSuccesses - 1);
        } else if (alternative == AlternativeHypothesis.LESS_THAN) {
            p = distribution.cumulativeProbability(numberOfSuccesses);
        } else {
            p = twoSidedBinomialTest(numberOfTrials, numberOfSuccesses, probability, distribution);
        }
        return new BaseSignificanceResult((double) numberOfSuccesses / numberOfTrials, p);
    }

    /**
     * Returns the <i>observed significance level</i>, or p-value, associated with a
     * two-sided binomial test about the probability of success \( \pi \).
     *
     * @param n Number of trials performed.
     * @param k Number of successes observed.
     * @param probability Assumed probability of a single trial under the null
     * hypothesis.
     * @param distribution Binomial distribution.
     * @return p-value
     */
    private static double twoSidedBinomialTest(int n, int k, double probability,
                                               BinomialDistribution distribution) {
        // Find all i where Pr(X = i) <= Pr(X = k) and sum them.
        // Exploit the known unimodal distribution to increase the
        // search speed. Note the search depends only on magnitude differences.
        // The current BinomialDistribution is faster using log probability
        // as it omits a call to Math.exp.

        // Use the mode as the point of largest probability.
        // The lower or upper mode is important for the search below.
        final int m1 = (int) Math.ceil((n + 1.0) * probability) - 1;
        final int m2 = (int) Math.floor((n + 1.0) * probability);
        if (k < m1) {
            final double pk = distribution.logProbability(k);
            // Lower half = cdf(k)
            // Find upper half. As k < lower mode i should never
            // reach the lower mode based on the probability alone.
            // Bracket with the upper mode.
            final int i = Searches.searchDescending(m2, n, pk, distribution::logProbability);
            return distribution.cumulativeProbability(k) +
                   distribution.survivalProbability(i - 1);
        } else if (k > m2) {
            final double pk = distribution.logProbability(k);
            // Upper half = sf(k - 1)
            // Find lower half. As k > upper mode i should never
            // reach the upper mode based on the probability alone.
            // Bracket with the lower mode.
            final int i = Searches.searchAscending(0, m1, pk, distribution::logProbability);
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
}
