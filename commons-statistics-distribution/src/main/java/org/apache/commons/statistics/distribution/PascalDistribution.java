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
package org.apache.commons.statistics.distribution;

import org.apache.commons.numbers.combinatorics.BinomialCoefficientDouble;
import org.apache.commons.numbers.combinatorics.LogBinomialCoefficient;
import org.apache.commons.numbers.gamma.RegularizedBeta;

/**
 * Implementation of the Pascal distribution.
 *
 * <p>The Pascal distribution is a special case of the negative binomial distribution
 * where the number of successes parameter is an integer.
 *
 * <p>There are various ways to express the probability mass and distribution
 * functions for the Pascal distribution. The present implementation represents
 * the distribution of the number of failures before \( r \) successes occur.
 * This is the convention adopted in e.g.
 * <a href="https://mathworld.wolfram.com/NegativeBinomialDistribution.html">MathWorld</a>,
 * but <em>not</em> in
 * <a href="https://en.wikipedia.org/wiki/Negative_binomial_distribution">Wikipedia</a>.
 *
 * <p>The probability mass function of \( X \) is:
 *
 * <p>\[ f(k; r, p) = \binom{k+r-1}{r-1} p^r \, (1-p)^k \]
 *
 * <p>for \( r \in \{1, 2, \dots\} \) the number of successes,
 * \( p \in (0, 1] \) the probability of success,
 * \( k \in \{0, 1, 2, \dots\} \) the total number of failures, and
 *
 * <p>\[ \binom{k+r-1}{r-1} = \frac{(k+r-1)!}{(r-1)! \, k!} \]
 *
 * <p>is the binomial coefficient.
 *
 * <p>The mean and variance of \( X \) are:
 *
 * <p>\[ \begin{aligned} \mathbb{E}(X) &amp;= \frac {(1 - p) r}{p} \\ \mathrm{Var}(X) &amp;= \frac {(1 - p) r}{p^2} \end{aligned} \]
 *
 * <p>The cumulative distribution function of \( X \) is:
 *
 * <p>\[ P(X \leq k) = I(p, r, k + 1) \]
 *
 * <p>where \( I \) is the regularized incomplete beta function.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Negative_binomial_distribution">Negative binomial distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/NegativeBinomialDistribution.html">Negative binomial distribution (MathWorld)</a>
 */
public final class PascalDistribution extends AbstractDiscreteDistribution {
    /** The number of successes. */
    private final int numberOfSuccesses;
    /** The probability of success. */
    private final double probabilityOfSuccess;
    /** The value of {@code log(p) * n}, where {@code p} is the probability of success
     * and {@code n} is the number of successes, stored for faster computation. */
    private final double logProbabilityOfSuccessByNumOfSuccesses;
    /** The value of {@code log(1-p)}, where {@code p} is the probability of success,
     * stored for faster computation. */
    private final double log1mProbabilityOfSuccess;
    /** The value of {@code p^n}, where {@code p} is the probability of success
     * and {@code n} is the number of successes, stored for faster computation. */
    private final double probabilityOfSuccessPowNumOfSuccesses;

    /**
     * @param r Number of successes.
     * @param p Probability of success.
     */
    private PascalDistribution(int r,
                               double p) {
        numberOfSuccesses = r;
        probabilityOfSuccess = p;
        logProbabilityOfSuccessByNumOfSuccesses = Math.log(p) * numberOfSuccesses;
        log1mProbabilityOfSuccess = Math.log1p(-p);
        probabilityOfSuccessPowNumOfSuccesses = Math.pow(probabilityOfSuccess, numberOfSuccesses);
    }

    /**
     * Create a Pascal distribution.
     *
     * @param r Number of successes.
     * @param p Probability of success.
     * @return the distribution
     * @throws IllegalArgumentException if {@code r <= 0} or {@code p <= 0} or
     * {@code p > 1}.
     */
    public static PascalDistribution of(int r,
                                        double p) {
        if (r <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, r);
        }
        if (p <= 0 ||
            p > 1) {
            throw new DistributionException(DistributionException.INVALID_NON_ZERO_PROBABILITY, p);
        }
        return new PascalDistribution(r, p);
    }

    /**
     * Access the number of successes for this distribution.
     *
     * @return the number of successes.
     */
    public int getNumberOfSuccesses() {
        return numberOfSuccesses;
    }

    /**
     * Access the probability of success for this distribution.
     *
     * @return the probability of success.
     */
    public double getProbabilityOfSuccess() {
        return probabilityOfSuccess;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x) {
        if (x <= 0) {
            // Special case of x=0 exploiting cancellation.
            return x == 0 ? probabilityOfSuccessPowNumOfSuccesses : 0.0;
        }
        final int n = x + numberOfSuccesses - 1;
        if (n < 0) {
            // overflow
            return 0.0;
        }
        return BinomialCoefficientDouble.value(n, numberOfSuccesses - 1) *
              probabilityOfSuccessPowNumOfSuccesses *
              Math.pow(1.0 - probabilityOfSuccess, x);
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x <= 0) {
            // Special case of x=0 exploiting cancellation.
            return x == 0 ? logProbabilityOfSuccessByNumOfSuccesses : Double.NEGATIVE_INFINITY;
        }
        final int n = x + numberOfSuccesses - 1;
        if (n < 0) {
            // overflow
            return Double.NEGATIVE_INFINITY;
        }
        return LogBinomialCoefficient.value(x +
              numberOfSuccesses - 1, numberOfSuccesses - 1) +
              logProbabilityOfSuccessByNumOfSuccesses +
              log1mProbabilityOfSuccess * x;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x < 0) {
            return 0.0;
        }
        return RegularizedBeta.value(probabilityOfSuccess,
                                     numberOfSuccesses, x + 1.0);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x < 0) {
            return 1.0;
        }
        // Use a helper function to compute the complement of the cumulative probability
        return RegularizedBetaUtils.complement(probabilityOfSuccess,
                                               numberOfSuccesses, x + 1.0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For number of successes {@code r} and probability of success {@code p},
     * the mean is {@code r * (1 - p) / p}.
     */
    @Override
    public double getMean() {
        final double p = getProbabilityOfSuccess();
        final double r = getNumberOfSuccesses();
        return (r * (1 - p)) / p;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For number of successes {@code r} and probability of success {@code p},
     * the variance is {@code r * (1 - p) / p^2}.
     */
    @Override
    public double getVariance() {
        final double p = getProbabilityOfSuccess();
        final double r = getNumberOfSuccesses();
        return r * (1 - p) / (p * p);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0 no matter the parameters.
     *
     * @return lower bound of the support (always 0)
     */
    @Override
    public int getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is positive infinity except for the
     * probability parameter {@code p = 1.0}.
     *
     * @return upper bound of the support ({@code Integer.MAX_VALUE} or 0)
     */
    @Override
    public int getSupportUpperBound() {
        return probabilityOfSuccess < 1 ? Integer.MAX_VALUE : 0;
    }
}
