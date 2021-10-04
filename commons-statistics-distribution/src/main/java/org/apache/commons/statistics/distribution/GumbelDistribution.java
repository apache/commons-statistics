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
 * This class implements the <a href="http://en.wikipedia.org/wiki/Gumbel_distribution">Gumbel distribution</a>.
 */
public class GumbelDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = Double.NEGATIVE_INFINITY;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** &pi;<sup>2</sup>/6. */
    private static final double PI_SQUARED_OVER_SIX = Math.PI * Math.PI / 6;
    /**
     * <a href="https://en.wikipedia.org/wiki/Euler%27s_constant">
     * Approximation of Euler's constant</a>.
     */
    private static final double EULER = 0.57721566490153286060;
    /** Location parameter. */
    private final double mu;
    /** Scale parameter. */
    private final double beta;

    /**
     * Creates a distribution.
     *
     * @param mu Location parameter.
     * @param beta Scale parameter (must be positive).
     * @throws IllegalArgumentException if {@code beta <= 0}
     */
    public GumbelDistribution(double mu,
                              double beta) {
        if (beta <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, beta);
        }

        this.beta = beta;
        this.mu = mu;
    }

    /**
     * Gets the location parameter.
     *
     * @return the location parameter.
     */
    public double getLocation() {
        return mu;
    }

    /**
     * Gets the scale parameter.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return beta;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x <= SUPPORT_LO) {
            return 0;
        }

        final double z = (x - mu) / beta;
        final double t = Math.exp(-z);
        return Math.exp(-z - t) / beta;
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        if (x <= SUPPORT_LO) {
            return Double.NEGATIVE_INFINITY;
        }

        final double z = (x - mu) / beta;
        final double t = Math.exp(-z);
        return -z - t - Math.log(beta);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        final double z = (x - mu) / beta;
        return Math.exp(-Math.exp(-z));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        final double z = (x - mu) / beta;
        return -Math.expm1(-Math.exp(-z));
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return Double.NEGATIVE_INFINITY;
        } else if (p == 1) {
            return Double.POSITIVE_INFINITY;
        }
        return mu - Math.log(-Math.log(p)) * beta;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mean is {@code mu + gamma * beta}, where {@code gamma} is
     * <a href="http://mathworld.wolfram.com/Euler-MascheroniConstantApproximations.html">
     * Euler's constant</a>
     */
    @Override
    public double getMean() {
        return mu + EULER * beta;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The variance is {@code pi^2 * beta^2 / 6}.
     */
    @Override
    public double getVariance() {
        return PI_SQUARED_OVER_SIX * beta * beta;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always negative infinity
     * no matter the parameters.
     *
     * @return lower bound of the support (always
     * {@code Double.NEGATIVE_INFINITY})
     */
    @Override
    public double getSupportLowerBound() {
        return SUPPORT_LO;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity
     * no matter the parameters.
     *
     * @return upper bound of the support (always
     * {@code Double.POSITIVE_INFINITY})
     */
    @Override
    public double getSupportUpperBound() {
        return SUPPORT_HI;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The support of this distribution is connected.
     *
     * @return {@code true}
     */
    @Override
    public boolean isSupportConnected() {
        return true;
    }
}
