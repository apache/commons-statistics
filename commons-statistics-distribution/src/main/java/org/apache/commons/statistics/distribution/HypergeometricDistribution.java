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
public class HypergeometricDistribution extends AbstractDiscreteDistribution {
    /** The number of successes in the population. */
    private final int numberOfSuccesses;
    /** The population size. */
    private final int populationSize;
    /** The sample size. */
    private final int sampleSize;

    /**
     * Creates a new hypergeometric distribution.
     *
     * @param populationSize Population size.
     * @param numberOfSuccesses Number of successes in the population.
     * @param sampleSize Sample size.
     * @throws IllegalArgumentException if {@code numberOfSuccesses < 0}, or
     * {@code populationSize <= 0} or {@code numberOfSuccesses > populationSize},
     * or {@code sampleSize > populationSize}.
     */
    public HypergeometricDistribution(int populationSize,
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

        this.numberOfSuccesses = numberOfSuccesses;
        this.populationSize = populationSize;
        this.sampleSize = sampleSize;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        double ret;

        final int[] domain = getDomain(populationSize, numberOfSuccesses, sampleSize);
        if (x < domain[0]) {
            ret = 0.0;
        } else if (x >= domain[1]) {
            ret = 1.0;
        } else {
            ret = innerCumulativeProbability(domain[0], x);
        }

        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        double ret;

        final int[] domain = getDomain(populationSize, numberOfSuccesses, sampleSize);
        if (x < domain[0]) {
            ret = 1.0;
        } else if (x >= domain[1]) {
            ret = 0.0;
        } else {
            ret = innerCumulativeProbability(domain[1], x + 1);
        }

        return ret;
    }

    /**
     * Return the domain for the given hypergeometric distribution parameters.
     *
     * @param n Population size.
     * @param m Number of successes in the population.
     * @param k Sample size.
     * @return a two element array containing the lower and upper bounds of the
     * hypergeometric distribution.
     */
    private static int[] getDomain(int n, int m, int k) {
        return new int[] {getLowerDomain(n, m, k), getUpperDomain(m, k)};
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
     * Access the number of successes.
     *
     * @return the number of successes.
     */
    public int getNumberOfSuccesses() {
        return numberOfSuccesses;
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
     * Access the sample size.
     *
     * @return the sample size.
     */
    public int getSampleSize() {
        return sampleSize;
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

    /** {@inheritDoc} */
    @Override
    public double probability(int x) {
        final double logProbability = logProbability(x);
        return logProbability == Double.NEGATIVE_INFINITY ? 0 : Math.exp(logProbability);
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        double ret;

        final int[] domain = getDomain(populationSize, numberOfSuccesses, sampleSize);
        if (x < domain[0] || x > domain[1]) {
            ret = Double.NEGATIVE_INFINITY;
        } else {
            final double p = (double) sampleSize / (double) populationSize;
            final double q = (double) (populationSize - sampleSize) / (double) populationSize;
            ret = logProbability(x, p, q);
        }

        return ret;
    }

    /**
     * Compute the log probability.
     *
     * @param x Value.
     * @param p sample size / population size.
     * @param q (population size - sample size) / population size
     * @return log(P(X = x))
     */
    private double logProbability(int x, double p, double q) {
        final double p1 =
                SaddlePointExpansionUtils.logBinomialProbability(x, numberOfSuccesses, p, q);
        final double p2 =
                SaddlePointExpansionUtils.logBinomialProbability(sampleSize - x,
                        populationSize - numberOfSuccesses, p, q);
        final double p3 =
                SaddlePointExpansionUtils.logBinomialProbability(sampleSize, populationSize, p, q);
        return p1 + p2 - p3;
    }

    /**
     * For this distribution, {@code X}, this method returns {@code P(X >= x)}.
     *
     * <p>Note: This is not equals to {@link #survivalProbability(int)} which computes {@code P(X > x)}.
     *
     * @param x Value at which the CDF is evaluated.
     * @return the upper tail CDF for this distribution.
     */
    public double upperCumulativeProbability(int x) {
        double ret;

        final int[] domain = getDomain(populationSize, numberOfSuccesses, sampleSize);
        if (x <= domain[0]) {
            ret = 1.0;
        } else if (x > domain[1]) {
            ret = 0.0;
        } else {
            ret = innerCumulativeProbability(domain[1], x);
        }

        return ret;
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
        final double p = (double) sampleSize / (double) populationSize;
        final double q = (double) (populationSize - sampleSize) / (double) populationSize;
        int x = x0;
        double ret = Math.exp(logProbability(x, p, q));
        if (x0 < x1) {
            while (x != x1) {
                x++;
                ret += Math.exp(logProbability(x, p, q));
            }
        } else {
            while (x != x1) {
                x--;
                ret += Math.exp(logProbability(x, p, q));
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     *
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the mean is {@code n * m / N}.
     */
    @Override
    public double getMean() {
        return getSampleSize() * (getNumberOfSuccesses() / (double) getPopulationSize());
    }

    /**
     * {@inheritDoc}
     *
     * For population size {@code N}, number of successes {@code m}, and sample
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
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the lower bound of the support is
     * {@code max(0, n + m - N)}.
     *
     * @return lower bound of the support
     */
    @Override
    public int getSupportLowerBound() {
        return Math.max(0,
                        getSampleSize() + getNumberOfSuccesses() - getPopulationSize());
    }

    /**
     * {@inheritDoc}
     *
     * For number of successes {@code m} and sample size {@code n}, the upper
     * bound of the support is {@code min(m, n)}.
     *
     * @return upper bound of the support
     */
    @Override
    public int getSupportUpperBound() {
        return Math.min(getNumberOfSuccesses(), getSampleSize());
    }

    /**
     * {@inheritDoc}
     *
     * The support of this distribution is connected.
     *
     * @return {@code true}
     */
    @Override
    public boolean isSupportConnected() {
        return true;
    }
}
