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

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Hypergeometric_distribution">hypergeometric distribution</a>.
 */
public final class HypergeometricDistribution extends AbstractDiscreteDistribution {
    /** The number of successes in the population. */
    private final int numberOfSuccesses;
    /** The population size. */
    private final int populationSize;
    /** The sample size. */
    private final int sampleSize;
    /** The lower bound of the support (inclusive). */
    private final int lowerBound;
    /** The upper bound of the support (inclusive). */
    private final int upperBound;
    /** Binomial probability of success (sampleSize / populationSize). */
    private final double p;
    /** Binomial probability of failure ((populationSize - sampleSize) / populationSize). */
    private final double q;

    /**
     * @param populationSize Population size.
     * @param numberOfSuccesses Number of successes in the population.
     * @param sampleSize Sample size.
     */
    private HypergeometricDistribution(int populationSize,
                                       int numberOfSuccesses,
                                       int sampleSize) {
        this.numberOfSuccesses = numberOfSuccesses;
        this.populationSize = populationSize;
        this.sampleSize = sampleSize;
        lowerBound = getLowerDomain(populationSize, numberOfSuccesses, sampleSize);
        upperBound = getUpperDomain(numberOfSuccesses, sampleSize);
        p = (double) sampleSize / (double) populationSize;
        q = (double) (populationSize - sampleSize) / (double) populationSize;
    }

    /**
     * Creates a hypergeometric distribution.
     *
     * @param populationSize Population size.
     * @param numberOfSuccesses Number of successes in the population.
     * @param sampleSize Sample size.
     * @return the distribution
     * @throws IllegalArgumentException if {@code numberOfSuccesses < 0}, or
     * {@code populationSize <= 0} or {@code numberOfSuccesses > populationSize}, or
     * {@code sampleSize > populationSize}.
     */
    public static HypergeometricDistribution of(int populationSize,
                                                int numberOfSuccesses,
                                                int sampleSize) {
        if (populationSize <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            populationSize);
        }
        if (numberOfSuccesses < 0) {
            throw new DistributionException(DistributionException.NEGATIVE,
                                            numberOfSuccesses);
        }
        if (sampleSize < 0) {
            throw new DistributionException(DistributionException.NEGATIVE,
                                            sampleSize);
        }

        if (numberOfSuccesses > populationSize) {
            throw new DistributionException(DistributionException.TOO_LARGE,
                                            numberOfSuccesses, populationSize);
        }
        if (sampleSize > populationSize) {
            throw new DistributionException(DistributionException.TOO_LARGE,
                                            sampleSize, populationSize);
        }
        return new HypergeometricDistribution(populationSize, numberOfSuccesses, sampleSize);
    }

    /**
     * Return the lowest domain value for the given hypergeometric distribution
     * parameters.
     *
     * @param n Population size.
     * @param m Number of successes in the population.
     * @param k Sample size.
     * @return the lowest domain value of the hypergeometric distribution.
     */
    private static int getLowerDomain(int n, int m, int k) {
        return Math.max(0, m - (n - k));
    }

    /**
     * Return the highest domain value for the given hypergeometric distribution
     * parameters.
     *
     * @param m Number of successes in the population.
     * @param k Sample size.
     * @return the highest domain value of the hypergeometric distribution.
     */
    private static int getUpperDomain(int m, int k) {
        return Math.min(k, m);
    }

    /**
     * Access the population size.
     *
     * @return the population size.
     */
    public int getPopulationSize() {
        return populationSize;
    }

    /**
     * Access the number of successes.
     *
     * @return the number of successes.
     */
    public int getNumberOfSuccesses() {
        return numberOfSuccesses;
    }

    /**
     * Access the sample size.
     *
     * @return the sample size.
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(int x) {
        return Math.exp(logProbability(x));
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x < lowerBound || x > upperBound) {
            return Double.NEGATIVE_INFINITY;
        }
        return computeLogProbability(x);
    }

    /**
     * Compute the log probability.
     *
     * @param x Value.
     * @return log(P(X = x))
     */
    private double computeLogProbability(int x) {
        final double p1 =
                SaddlePointExpansionUtils.logBinomialProbability(x, numberOfSuccesses, p, q);
        final double p2 =
                SaddlePointExpansionUtils.logBinomialProbability(sampleSize - x,
                        populationSize - numberOfSuccesses, p, q);
        final double p3 =
                SaddlePointExpansionUtils.logBinomialProbability(sampleSize, populationSize, p, q);
        return p1 + p2 - p3;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x < lowerBound) {
            return 0.0;
        } else if (x >= upperBound) {
            return 1.0;
        }
        return innerCumulativeProbability(lowerBound, x);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x < lowerBound) {
            return 1.0;
        } else if (x >= upperBound) {
            return 0.0;
        }
        return innerCumulativeProbability(upperBound, x + 1);
    }

    /**
     * For this distribution, {@code X}, this method returns
     * {@code P(x0 <= X <= x1)}.
     * This probability is computed by summing the point probabilities for the
     * values {@code x0, x0 + dx, x0 + 2 * dx, ..., x1}; the direction {@code dx} is determined
     * using a comparison of the input bounds.
     * This should be called by using {@code x0} as the domain limit and {@code x1}
     * as the internal value. This will result in an initial sum of increasing larger magnitudes.
     *
     * @param x0 Inclusive domain bound.
     * @param x1 Inclusive internal bound.
     * @return {@code P(x0 <= X <= x1)}.
     */
    private double innerCumulativeProbability(int x0, int x1) {
        // Assume the range is within the domain.
        // Reuse the computation for probability(x) but avoid checking the domain for each call.
        int x = x0;
        double ret = Math.exp(computeLogProbability(x));
        if (x0 < x1) {
            while (x != x1) {
                x++;
                ret += Math.exp(computeLogProbability(x));
            }
        } else {
            while (x != x1) {
                x--;
                ret += Math.exp(computeLogProbability(x));
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the mean is {@code n * m / N}.
     */
    @Override
    public double getMean() {
        return getSampleSize() * (getNumberOfSuccesses() / (double) getPopulationSize());
    }

    /**
     * {@inheritDoc}
     *
     * <p>For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the variance is
     * {@code (n * m * (N - n) * (N - m)) / (N^2 * (N - 1))}.
     */
    @Override
    public double getVariance() {
        final double N = getPopulationSize();
        final double m = getNumberOfSuccesses();
        final double n = getSampleSize();
        return (n * m * (N - n) * (N - m)) / (N * N * (N - 1));
    }

    /**
     * {@inheritDoc}
     *
     * <p>For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the lower bound of the support is
     * {@code max(0, n + m - N)}.
     *
     * @return lower bound of the support
     */
    @Override
    public int getSupportLowerBound() {
        return lowerBound;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For number of successes {@code m} and sample size {@code n}, the upper
     * bound of the support is {@code min(m, n)}.
     *
     * @return upper bound of the support
     */
    @Override
    public int getSupportUpperBound() {
        return upperBound;
    }
}
