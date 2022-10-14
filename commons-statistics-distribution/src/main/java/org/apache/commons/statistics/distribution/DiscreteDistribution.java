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

import java.util.stream.IntStream;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Interface for distributions on the integers.
 */
public interface DiscreteDistribution {

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(X = x)}.
     * In other words, this method represents the probability mass function (PMF)
     * for the distribution.
     *
     * @param x Point at which the PMF is evaluated.
     * @return the value of the probability mass function at {@code x}.
     */
    double probability(int x);

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(x0 < X <= x1)}.
     * The default implementation uses the identity
     * {@code P(x0 < X <= x1) = P(X <= x1) - P(X <= x0)}
     *
     * <p>Special cases:
     * <ul>
     * <li>returns {@code 0.0} if {@code x0 == x1};
     * <li>returns {@code probability(x1)} if {@code x0 + 1 == x1};
     * </ul>
     *
     * @param x0 Lower bound (exclusive).
     * @param x1 Upper bound (inclusive).
     * @return the probability that a random variable with this distribution
     * takes a value between {@code x0} and {@code x1},  excluding the lower
     * and including the upper endpoint.
     * @throws IllegalArgumentException if {@code x0 > x1}.
     */
    default double probability(int x0,
                               int x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH, x0, x1);
        }
        // Long addition avoids overflow
        if (x0 + 1L >= x1) {
            return x0 == x1 ? 0.0 : probability(x1);
        }
        return cumulativeProbability(x1) - cumulativeProbability(x0);
    }

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code log(P(X = x))}, where
     * {@code log} is the natural logarithm.
     *
     * @param x Point at which the PMF is evaluated.
     * @return the logarithm of the value of the probability mass function at
     * {@code x}.
     */
    default double logProbability(int x) {
        return Math.log(probability(x));
    }

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(X <= x)}.
     * In other, words, this method represents the (cumulative) distribution
     * function (CDF) for this distribution.
     *
     * @param x Point at which the CDF is evaluated.
     * @return the probability that a random variable with this distribution
     * takes a value less than or equal to {@code x}.
     */
    double cumulativeProbability(int x);

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(X > x)}.
     * In other words, this method represents the complementary cumulative
     * distribution function.
     *
     * <p>By default, this is defined as {@code 1 - cumulativeProbability(x)}, but
     * the specific implementation may be more accurate.
     *
     * @param x Point at which the survival function is evaluated.
     * @return the probability that a random variable with this
     * distribution takes a value greater than {@code x}.
     */
    default double survivalProbability(int x) {
        return 1.0 - cumulativeProbability(x);
    }

    /**
     * Computes the quantile function of this distribution.
     * For a random variable {@code X} distributed according to this distribution,
     * the returned value is
     * <ul>
     * <li>{@code inf{x in Z | P(X<=x) >= p}} for {@code 0 < p <= 1},</li>
     * <li>{@code inf{x in Z | P(X<=x) > 0}} for {@code p = 0}.</li>
     * </ul>
     * <p>If the result exceeds the range of the data type {@code int},
     * then {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned.
     * In this case the result of {@link #cumulativeProbability(int)} called
     * using the returned {@code p}-quantile may not compute the original {@code p}.
     *
     * @param p Cumulative probability.
     * @return the smallest {@code p}-quantile of this distribution
     * (largest 0-quantile for {@code p = 0}).
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}.
     */
    int inverseCumulativeProbability(double p);

    /**
     * Computes the inverse survival probability function of this distribution.
     * For a random variable {@code X} distributed according to this distribution,
     * the returned value is
     * <ul>
     * <li>{@code inf{x in R | P(X>=x) <= p}} for {@code 0 <= p < 1},</li>
     * <li>{@code inf{x in R | P(X>=x) < 1}} for {@code p = 1}.</li>
     * </ul>
     * <p>If the result exceeds the range of the data type {@code int},
     * then {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned.
     * In this case the result of {@link #survivalProbability(int)} called
     * using the returned {@code (1-p)}-quantile may not compute the original {@code p}.
     *
     * <p>By default, this is defined as {@code inverseCumulativeProbability(1 - p)}, but
     * the specific implementation may be more accurate.
     *
     * @param p Cumulative probability.
     * @return the smallest {@code (1-p)}-quantile of this distribution
     * (largest 0-quantile for {@code p = 1}).
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}.
     */
    default int inverseSurvivalProbability(double p) {
        return inverseCumulativeProbability(1 - p);
    }

    /**
     * Gets the mean of this distribution.
     *
     * @return the mean.
     */
    double getMean();

    /**
     * Gets the variance of this distribution.
     *
     * @return the variance.
     */
    double getVariance();

    /**
     * Gets the lower bound of the support.
     * This method must return the same value as
     * {@code inverseCumulativeProbability(0)}, i.e.
     * {@code inf {x in Z | P(X <= x) > 0}}.
     * By convention, {@code Integer.MIN_VALUE} should be substituted
     * for negative infinity.
     *
     * @return the lower bound of the support.
     */
    int getSupportLowerBound();

    /**
     * Gets the upper bound of the support.
     * This method must return the same value as
     * {@code inverseCumulativeProbability(1)}, i.e.
     * {@code inf {x in Z | P(X <= x) = 1}}.
     * By convention, {@code Integer.MAX_VALUE} should be substituted
     * for positive infinity.
     *
     * @return the upper bound of the support.
     */
    int getSupportUpperBound();

    /**
     * Creates a sampler.
     *
     * @param rng Generator of uniformly distributed numbers.
     * @return a sampler that produces random numbers according this
     * distribution.
     */
    Sampler createSampler(UniformRandomProvider rng);

    /**
     * Sampling functionality.
     */
    @FunctionalInterface
    interface Sampler {
        /**
         * Generates a random value sampled from this distribution.
         *
         * @return a random value.
         */
        int sample();

        /**
         * Returns an effectively unlimited stream of {@code int} sample values.
         *
         * <p>The default implementation produces a sequential stream that repeatedly
         * calls {@link #sample sample}().
         *
         * @return a stream of {@code int} values.
         */
        default IntStream samples() {
            return IntStream.generate(this::sample).sequential();
        }

        /**
         * Returns a stream producing the given {@code streamSize} number of {@code int}
         * sample values.
         *
         * <p>The default implementation produces a sequential stream that repeatedly
         * calls {@link #sample sample}(); the stream is limited to the given {@code streamSize}.
         *
         * @param streamSize Number of values to generate.
         * @return a stream of {@code int} values.
         */
        default IntStream samples(long streamSize) {
            return samples().limit(streamSize);
        }
    }
}
