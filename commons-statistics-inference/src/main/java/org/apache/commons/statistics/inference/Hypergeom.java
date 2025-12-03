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

import org.apache.commons.statistics.distribution.HypergeometricDistribution;

/**
 * Provide a wrapper around the {@link HypergeometricDistribution} that caches
 * all probability mass values.
 *
 * <p>This class extracts the logic from the HypergeometricDistribution implementation
 * used for the cumulative probability functions. It allows fast computation of
 * the CDF and SF for the entire supported domain.
 *
 * @since 1.1
 */
class Hypergeom {
    /** 1/2. */
    private static final double HALF = 0.5;
    /** The lower bound of the support (inclusive). */
    private final int lowerBound;
    /** The upper bound of the support (inclusive). */
    private final int upperBound;
    /** Cached probability values. This holds values from x=0 even though the supported
     * lower bound may be above x=0. This allows x to be used as an index without offsetting
     * using the lower bound. */
    private final double[] prob;
    /** Cached midpoint, m, of the CDF/SF. This is not the true median. It is the value where
     * the CDF is closest to 0.5; as such the CDF(m) may be below 0.5 if the next value
     * CDF(m+1) is further from 0.5. Used for the cumulative probability functions. */
    private final int m;
    /** Cached CDF of the midpoint.
     * Used for the cumulative probability functions. */
    private final double midCDF;
    /** Lower mode. */
    private final int m1;
    /** Upper mode. */
    private final int m2;

    /**
     * @param populationSize Population size.
     * @param numberOfSuccesses Number of successes in the population.
     * @param sampleSize Sample size.
     */
    Hypergeom(int populationSize,
              int numberOfSuccesses,
              int sampleSize) {
        final HypergeometricDistribution dist =
            HypergeometricDistribution.of(populationSize, numberOfSuccesses, sampleSize);

        // Cache all values required to compute the cumulative probability functions

        // Bounds
        lowerBound = dist.getSupportLowerBound();
        upperBound = dist.getSupportUpperBound();

        // PMF values
        prob = new double[upperBound + 1];
        for (int x = lowerBound; x <= upperBound; x++) {
            prob[x] = dist.probability(x);
        }

        // Compute mid-point for CDF/SF computation
        // Find the closest sum(PDF) to 0.5.
        int x = lowerBound;
        double p0 = 0;
        double p1 = prob[x];
        // No check of the upper bound required here as the CDF should sum to 1 and 0.5
        // is exceeded before a bounds error.
        while (p1 < HALF) {
            x++;
            p0 = p1;
            p1 += prob[x];
        }
        // p1 >= 0.5 > p0
        // Pick closet
        if (p1 - HALF >= HALF - p0) {
            x--;
            p1 = p0;
        }
        m = x;
        midCDF = p1;

        // Compute the mode (lower != upper in the case where v is integer).
        // This value is used by the UnconditionedExactTest and is cached here for convenience.
        final double v = ((double) numberOfSuccesses + 1) * ((double) sampleSize + 1) / (populationSize + 2.0);
        m1 = (int) Math.ceil(v) - 1;
        m2 = (int) Math.floor(v);
    }

    /**
     * Get the lower bound of the support.
     *
     * @return lower bound
     */
    int getSupportLowerBound() {
        return lowerBound;
    }

    /**
     * Get the upper bound of the support.
     *
     * @return upper bound
     */
    int getSupportUpperBound() {
        return upperBound;
    }

    /**
     * Get the lower mode of the distribution.
     *
     * @return lower mode
     */
    int getLowerMode() {
        return m1;
    }

    /**
     * Get the upper mode of the distribution.
     *
     * @return upper mode
     */
    int getUpperMode() {
        return m2;
    }

    /**
     * Compute the probability mass function (PMF) at the specified value.
     *
     * @param x Value.
     * @return P(X = x)
     * @throws IndexOutOfBoundsException if the value {@code x} is not in the supported domain.
     */
    double pmf(int x) {
        return prob[x];
    }

    /**
     * Compute the cumulative distribution function (CDF) at the specified value.
     *
     * @param x Value.
     * @return P(X <= x)
     */
    double cdf(int x) {
        if (x < lowerBound) {
            return 0.0;
        } else if (x >= upperBound) {
            return 1.0;
        }
        if (x < m) {
            return innerCumulativeProbability(lowerBound, x);
        } else if (x > m) {
            return 1 - innerCumulativeProbability(upperBound, x + 1);
        }
        // cdf(x)
        return midCDF;
    }

    /**
     * Compute the survival function (SF) at the specified value. This is the complementary
     * cumulative distribution function.
     *
     * @param x Value.
     * @return P(X > x)
     */
    double sf(int x) {
        if (x < lowerBound) {
            return 1.0;
        } else if (x >= upperBound) {
            return 0.0;
        }
        if (x < m) {
            return 1 - innerCumulativeProbability(lowerBound, x);
        } else if (x > m) {
            return innerCumulativeProbability(upperBound, x + 1);
        }
        // 1 - cdf(x)
        return 1 - midCDF;
    }

    /**
     * For this distribution, {@code X}, this method returns
     * {@code P(x0 <= X <= x1)}.
     * This probability is computed by summing the point probabilities for the
     * values {@code x0, x0 + dx, x0 + 2 * dx, ..., x1}; the direction {@code dx} is determined
     * using a comparison of the input bounds.
     * This should be called by using {@code x0} as the domain limit and {@code x1}
     * as the internal value. This will result in a sum of increasingly larger magnitudes.
     *
     * @param x0 Inclusive domain bound.
     * @param x1 Inclusive internal bound.
     * @return {@code P(x0 <= X <= x1)}.
     */
    private double innerCumulativeProbability(int x0, int x1) {
        // Assume the range is within the domain.
        int x = x0;
        double ret = prob[x];
        if (x0 < x1) {
            while (x != x1) {
                x++;
                ret += prob[x];
            }
        } else {
            while (x != x1) {
                x--;
                ret += prob[x];
            }
        }
        return ret;
    }
}
