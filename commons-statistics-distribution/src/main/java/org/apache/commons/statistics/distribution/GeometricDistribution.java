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
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Geometric_distribution">geometric distribution</a>.
 */
public class GeometricDistribution extends AbstractDiscreteDistribution {
    /** The probability of success. */
    private final double probabilityOfSuccess;
    /** {@code log(p)} where p is the probability of success. */
    private final double logProbabilityOfSuccess;
    /** {@code log(1 - p)} where p is the probability of success. */
    private final double log1mProbabilityOfSuccess;

    /**
     * Creates a geometric distribution.
     *
     * @param p Probability of success.
     * @throws IllegalArgumentException if {@code p <= 0} or {@code p > 1}.
     */
    public GeometricDistribution(double p) {
        if (p <= 0 || p > 1) {
            throw new DistributionException(DistributionException.OUT_OF_RANGE, p, 0, 1);
        }

        probabilityOfSuccess = p;
        logProbabilityOfSuccess = Math.log(p);
        log1mProbabilityOfSuccess = Math.log1p(-p);
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
        if (x < 0) {
            return 0.0;
        } else {
            return Math.exp(log1mProbabilityOfSuccess * x) * probabilityOfSuccess;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double logProbability(int x) {
        if (x < 0) {
            return Double.NEGATIVE_INFINITY;
        } else {
            return x * log1mProbabilityOfSuccess + logProbabilityOfSuccess;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(int x) {
        if (x < 0) {
            return 0.0;
        } else {
            return -Math.expm1(log1mProbabilityOfSuccess * (x + 1));
        }
    }

    /**
     * {@inheritDoc}
     *
     * For probability parameter {@code p}, the mean is {@code (1 - p) / p}.
     */
    @Override
    public double getMean() {
        return (1 - probabilityOfSuccess) / probabilityOfSuccess;
    }

    /**
     * {@inheritDoc}
     *
     * For probability parameter {@code p}, the variance is
     * {@code (1 - p) / (p * p)}.
     */
    @Override
    public double getVariance() {
        return (1 - probabilityOfSuccess) / (probabilityOfSuccess * probabilityOfSuccess);
    }

    /**
     * {@inheritDoc}
     *
     * The lower bound of the support is always 0.
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
     * The upper bound of the support is infinite (which we approximate as
     * {@code Integer.MAX_VALUE}).
     *
     * @return upper bound of the support (always Integer.MAX_VALUE)
     */
    @Override
    public int getSupportUpperBound() {
        return Integer.MAX_VALUE;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int inverseCumulativeProbability(double p) {
        if (p < 0 ||
            p > 1) {
            throw new DistributionException(DistributionException.OUT_OF_RANGE, p, 0, 1);
        }
        if (p == 1) {
            return Integer.MAX_VALUE;
        }
        if (p == 0) {
            return 0;
        }
        return Math.max(0, (int) Math.ceil(Math.log1p(-p) / log1mProbabilityOfSuccess - 1));
    }
}
