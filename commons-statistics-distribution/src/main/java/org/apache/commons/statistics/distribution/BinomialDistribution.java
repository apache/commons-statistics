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

import org.apache.commons.numbers.gamma.RegularizedBeta;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Binomial_distribution">binomial distribution</a>.
 */
public final class BinomialDistribution extends AbstractDiscreteDistribution {
    /** The number of trials. */
    private final int numberOfTrials;
    /** The probability of success. */
    private final double probabilityOfSuccess;

    /**
     * @param trials Number of trials.
     * @param p Probability of success.
     */
    private BinomialDistribution(int trials,
                                 double p) {
        probabilityOfSuccess = p;
        numberOfTrials = trials;
    }

    /**
     * Creates a binomial distribution.
     *
     * @param trials Number of trials.
     * @param p Probability of success.
     * @return the distribution
     * @throws IllegalArgumentException if {@code trials < 0}, or if {@code p < 0}
     * or {@code p > 1}.
     */
    public static BinomialDistribution of(int trials,
                                          double p) {
        if (trials < 0) {
            throw new DistributionException(DistributionException.NEGATIVE,
                                            trials);
        }
        ArgumentUtils.checkProbability(p);
        return new BinomialDistribution(trials, p);
    }

    /**
     * Access the number of trials for this distribution.
     *
     * @return the number of trials.
     */
    public int getNumberOfTrials() {
        return numberOfTrials;
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
        return Math.exp(logProbability(x));
    }

    /** {@inheritDoc} **/
    @Override
    public double logProbability(int x) {
        if (numberOfTrials == 0) {
            return (x == 0) ? 0.0 : Double.NEGATIVE_INFINITY;
        } else if (x < 0 || x > numberOfTrials) {
            return Double.NEGATIVE_INFINITY;
        }
        return SaddlePointExpansionUtils.logBinomialProbability(x,
                numberOfTrials, probabilityOfSuccess,
                1.0 - probabilityOfSuccess);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x < 0) {
            return 0.0;
        } else if (x >= numberOfTrials) {
            return 1.0;
        }
        // Use a helper function to compute the complement of the survival probability
        return RegularizedBetaUtils.complement(probabilityOfSuccess,
                                               x + 1.0, (double) numberOfTrials - x);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x < 0) {
            return 1.0;
        } else if (x >= numberOfTrials) {
            return 0.0;
        }
        return RegularizedBeta.value(probabilityOfSuccess,
                                     x + 1.0, (double) numberOfTrials - x);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code n} trials and probability parameter {@code p}, the mean is
     * {@code n * p}.
     */
    @Override
    public double getMean() {
        return numberOfTrials * probabilityOfSuccess;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code n} trials and probability parameter {@code p}, the variance is
     * {@code n * p * (1 - p)}.
     */
    @Override
    public double getVariance() {
        final double p = probabilityOfSuccess;
        return numberOfTrials * p * (1 - p);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0 except for the probability
     * parameter {@code p = 1}.
     *
     * @return lower bound of the support (0 or the number of trials)
     */
    @Override
    public int getSupportLowerBound() {
        return probabilityOfSuccess < 1.0 ? 0 : numberOfTrials;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is the number of trials except for the
     * probability parameter {@code p = 0}.
     *
     * @return upper bound of the support (number of trials or 0)
     */
    @Override
    public int getSupportUpperBound() {
        return probabilityOfSuccess > 0.0 ? numberOfTrials : 0;
    }

    /** {@inheritDoc} */
    @Override
    protected int getMedian() {
        // Overridden for the probability(double, double) method.
        // This is intentionally not a public method.
        // Can be floor or ceiling of np. For the probability in a range use the floor
        // as this only used for values >= median+1.
        return (int) (numberOfTrials * probabilityOfSuccess);
    }
}
