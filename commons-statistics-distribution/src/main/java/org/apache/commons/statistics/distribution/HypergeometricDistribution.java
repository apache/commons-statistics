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

import java.util.function.DoublePredicate;

/**
 * Implementation of the hypergeometric distribution.
 *
 * <p>The probability mass function of \( X \) is:
 *
 * <p>\[ f(k; N, K, n) = \frac{\binom{K}{k} \binom{N - K}{n-k}}{\binom{N}{n}} \]
 *
 * <p>for \( N \in \{0, 1, 2, \dots\} \) the population size,
 * \( K \in \{0, 1, \dots, N\} \) the number of success states,
 * \( n \in \{0, 1, \dots, N\} \) the number of samples,
 * \( k \in \{\max(0, n+K-N), \dots, \min(n, K)\} \) the number of successes, and
 *
 * <p>\[ \binom{a}{b} = \frac{a!}{b! \, (a-b)!} \]
 *
 * <p>is the binomial coefficient.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Hypergeometric_distribution">Hypergeometric distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/HypergeometricDistribution.html">Hypergeometric distribution (MathWorld)</a>
 */
public final class HypergeometricDistribution extends AbstractDiscreteDistribution {
    /** 1/2. */
    private static final double HALF = 0.5;
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
    private final double bp;
    /** Binomial probability of failure ((populationSize - sampleSize) / populationSize). */
    private final double bq;
    /** Cached midpoint of the CDF/SF. The array holds [x, cdf(x)] for the midpoint x.
     * Used for the cumulative probability functions. */
    private double[] midpoint;

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
        bp = (double) sampleSize / (double) populationSize;
        bq = (double) (populationSize - sampleSize) / (double) populationSize;
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
     * @param nn Population size.
     * @param k Number of successes in the population.
     * @param n Sample size.
     * @return the lowest domain value of the hypergeometric distribution.
     */
    private static int getLowerDomain(int nn, int k, int n) {
        // Avoid overflow given N > n:
        // n + K - N == K - (N - n)
        return Math.max(0, k - (nn - n));
    }

    /**
     * Return the highest domain value for the given hypergeometric distribution
     * parameters.
     *
     * @param k Number of successes in the population.
     * @param n Sample size.
     * @return the highest domain value of the hypergeometric distribution.
     */
    private static int getUpperDomain(int k, int n) {
        return Math.min(n, k);
    }

    /**
     * Gets the population size parameter of this distribution.
     *
     * @return the population size.
     */
    public int getPopulationSize() {
        return populationSize;
    }

    /**
     * Gets the number of successes parameter of this distribution.
     *
     * @return the number of successes.
     */
    public int getNumberOfSuccesses() {
        return numberOfSuccesses;
    }

    /**
     * Gets the sample size parameter of this distribution.
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
    public double probability(int x0, int x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH, x0, x1);
        }
        if (x0 == x1 || x1 < lowerBound) {
            return 0;
        }
        // If the range is outside the bounds use the appropriate cumulative probability
        if (x0 < lowerBound) {
            return cumulativeProbability(x1);
        }
        if (x1 >= upperBound) {
            // 1 - cdf(x0)
            return survivalProbability(x0);
        }
        // Here: lower <= x0 < x1 < upper:
        // sum(pdf(x)) for x in (x0, x1]
        final int lo = x0 + 1;
        // Sum small values first by starting at the point the greatest distance from the mode.
        final int mode = (int) Math.floor((sampleSize + 1.0) * (numberOfSuccesses + 1.0) / (populationSize + 2.0));
        return Math.abs(mode - lo) > Math.abs(mode - x1) ?
            innerCumulativeProbability(lo, x1) :
            innerCumulativeProbability(x1, lo);
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
                SaddlePointExpansionUtils.logBinomialProbability(x, numberOfSuccesses, bp, bq);
        final double p2 =
                SaddlePointExpansionUtils.logBinomialProbability(sampleSize - x,
                        populationSize - numberOfSuccesses, bp, bq);
        final double p3 =
                SaddlePointExpansionUtils.logBinomialProbability(sampleSize, populationSize, bp, bq);
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
        final double[] mid = getMidPoint();
        final int m = (int) mid[0];
        if (x < m) {
            return innerCumulativeProbability(lowerBound, x);
        } else if (x > m) {
            return 1 - innerCumulativeProbability(upperBound, x + 1);
        }
        // cdf(x)
        return mid[1];
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(int x) {
        if (x < lowerBound) {
            return 1.0;
        } else if (x >= upperBound) {
            return 0.0;
        }
        final double[] mid = getMidPoint();
        final int m = (int) mid[0];
        if (x < m) {
            return 1 - innerCumulativeProbability(lowerBound, x);
        } else if (x > m) {
            return innerCumulativeProbability(upperBound, x + 1);
        }
        // 1 - cdf(x)
        return 1 - mid[1];
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

    @Override
    public int inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return computeInverseProbability(p, 1 - p, false);
    }

    @Override
    public int inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return computeInverseProbability(1 - p, p, true);
    }

    /**
     * Implementation for the inverse cumulative or survival probability.
     *
     * @param p Cumulative probability.
     * @param q Survival probability.
     * @param complement Set to true to compute the inverse survival probability.
     * @return the value
     */
    private int computeInverseProbability(double p, double q, boolean complement) {
        if (p == 0) {
            return lowerBound;
        }
        if (q == 0) {
            return upperBound;
        }

        // Sum the PDF(x) until the appropriate p-value is obtained
        // CDF: require smallest x where P(X<=x) >= p
        // SF:  require smallest x where P(X>x) <= q
        // The choice of summation uses the mid-point.
        // The test on the CDF or SF is based on the appropriate input p-value.

        final double[] mid = getMidPoint();
        final int m = (int) mid[0];
        final double mp = mid[1];

        final int midPointComparison = complement ?
            Double.compare(1 - mp, q) :
            Double.compare(p, mp);

        if (midPointComparison < 0) {
            return inverseLower(p, q, complement);
        } else if (midPointComparison > 0) {
            // Avoid floating-point summation error when the mid-point computed using the
            // lower sum is different to the midpoint computed using the upper sum.
            // Here we know the result must be above the midpoint so we can clip the result.
            return Math.max(m + 1, inverseUpper(p, q, complement));
        }
        // Exact mid-point
        return m;
    }

    /**
     * Compute the inverse cumulative or survival probability using the lower sum.
     *
     * @param p Cumulative probability.
     * @param q Survival probability.
     * @param complement Set to true to compute the inverse survival probability.
     * @return the value
     */
    private int inverseLower(double p, double q, boolean complement) {
        // Sum from the lower bound (computing the cdf)
        int x = lowerBound;
        final DoublePredicate test = complement ?
            i -> 1 - i > q :
            i -> i < p;
        double cdf = Math.exp(computeLogProbability(x));
        while (test.test(cdf)) {
            x++;
            cdf += Math.exp(computeLogProbability(x));
        }
        return x;
    }

    /**
     * Compute the inverse cumulative or survival probability using the upper sum.
     *
     * @param p Cumulative probability.
     * @param q Survival probability.
     * @param complement Set to true to compute the inverse survival probability.
     * @return the value
     */
    private int inverseUpper(double p, double q, boolean complement) {
        // Sum from the upper bound (computing the sf)
        int x = upperBound;
        final DoublePredicate test = complement ?
            i -> i < q :
            i -> 1 - i > p;
        double sf = 0;
        while (test.test(sf)) {
            sf += Math.exp(computeLogProbability(x));
            x--;
        }
        // Here either sf(x) >= q, or cdf(x) <= p
        // Ensure sf(x) <= q, or cdf(x) >= p
        if (complement && sf > q ||
            !complement && 1 - sf < p) {
            x++;
        }
        return x;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For population size \( N \), number of successes \( K \), and sample
     * size \( n \), the mean is:
     *
     * <p>\[ n \frac{K}{N} \]
     */
    @Override
    public double getMean() {
        return getSampleSize() * (getNumberOfSuccesses() / (double) getPopulationSize());
    }

    /**
     * {@inheritDoc}
     *
     * <p>For population size \( N \), number of successes \( K \), and sample
     * size \( n \), the variance is:
     *
     * <p>\[ n \frac{K}{N} \frac{N-K}{N} \frac{N-n}{N-1} \]
     */
    @Override
    public double getVariance() {
        final double N = getPopulationSize();
        final double K = getNumberOfSuccesses();
        final double n = getSampleSize();
        return (n * K * (N - K) * (N - n)) / (N * N * (N - 1));
    }

    /**
     * {@inheritDoc}
     *
     * <p>For population size \( N \), number of successes \( K \), and sample
     * size \( n \), the lower bound of the support is \( \max \{ 0, n + K - N \} \).
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
     * <p>For number of successes \( K \), and sample
     * size \( n \), the upper bound of the support is \( \min \{ n, K \} \).
     *
     * @return upper bound of the support
     */
    @Override
    public int getSupportUpperBound() {
        return upperBound;
    }

    /**
     * Return the mid-point {@code x} of the distribution, and the cdf(x).
     *
     * <p>This is not the true median. It is the value where the CDF(x) is closest to 0.5;
     * as such the CDF may be below 0.5 if the next value of x is further from 0.5.
     *
     * @return the mid-point ([x, cdf(x)])
     */
    private double[] getMidPoint() {
        double[] v = midpoint;
        if (v == null) {
            // Find the closest sum(PDF) to 0.5
            int x = lowerBound;
            double p0 = 0;
            double p1 = Math.exp(computeLogProbability(x));
            // No check of the upper bound required here as the CDF should sum to 1 and 0.5
            // is exceeded before a bounds error.
            while (p1 < HALF) {
                x++;
                p0 = p1;
                p1 += Math.exp(computeLogProbability(x));
            }
            // p1 >= 0.5 > p0
            // Pick closet
            if (p1 - HALF >= HALF - p0) {
                x--;
                p1 = p0;
            }
            midpoint = v = new double[] {x, p1};
        }
        return v;
    }
}
