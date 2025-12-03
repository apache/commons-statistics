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
package org.apache.commons.statistics.distribution;

/**
 * Implementation of the Gumbel distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, \beta) =  \frac{1}{\beta} e^{-(z+e^{-z})} \]
 *
 * <p>where \[ z = \frac{x - \mu}{\beta} \]
 *
 * <p>for \( \mu \) the location,
 * \( \beta &gt; 0 \) the scale, and
 * \( x \in (-\infty, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Gumbel_distribution">Gumbel distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/GumbelDistribution.html">Gumbel distribution (MathWorld)</a>
 */
public final class GumbelDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = Double.NEGATIVE_INFINITY;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** &pi;<sup>2</sup>/6. https://oeis.org/A013661. */
    private static final double PI_SQUARED_OVER_SIX = 1.644934066848226436472415166646;
    /**
     * <a href="https://en.wikipedia.org/wiki/Euler%27s_constant">
     * Approximation of Euler's constant</a>.
     * https://oeis.org/A001620.
     */
    private static final double EULER = 0.5772156649015328606065;
    /** ln(ln(2)). https://oeis.org/A074785. */
    private static final double LN_LN_2 = -0.3665129205816643270124;
    /** Location parameter. */
    private final double mu;
    /** Scale parameter. */
    private final double beta;

    /**
     * @param mu Location parameter.
     * @param beta Scale parameter (must be positive).
     */
    private GumbelDistribution(double mu,
                               double beta) {
        this.beta = beta;
        this.mu = mu;
    }

    /**
     * Creates a Gumbel distribution.
     *
     * @param mu Location parameter.
     * @param beta Scale parameter (must be positive).
     * @return the distribution
     * @throws IllegalArgumentException if {@code beta <= 0}
     */
    public static GumbelDistribution of(double mu,
                                        double beta) {
        if (beta <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, beta);
        }
        return new GumbelDistribution(mu, beta);
    }

    /**
     * Gets the location parameter of this distribution.
     *
     * @return the location parameter.
     */
    public double getLocation() {
        return mu;
    }

    /**
     * Gets the scale parameter of this distribution.
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

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return Double.NEGATIVE_INFINITY;
        } else if (p == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return mu - Math.log(-Math.log1p(-p)) * beta;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For location parameter \( \mu \) and scale parameter \( \beta \), the mean is:
     *
     * <p>\[ \mu + \beta \gamma \]
     *
     * <p>where \( \gamma \) is the
     * <a href="https://mathworld.wolfram.com/Euler-MascheroniConstantApproximations.html">
     * Euler-Mascheroni constant</a>.
     */
    @Override
    public double getMean() {
        return mu + EULER * beta;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For scale parameter \( \beta \), the variance is:
     *
     * <p>\[ \frac{\pi^2}{6} \beta^2 \]
     */
    @Override
    public double getVariance() {
        return PI_SQUARED_OVER_SIX * beta * beta;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always negative infinity.
     *
     * @return {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
     */
    @Override
    public double getSupportLowerBound() {
        return SUPPORT_LO;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity.
     *
     * @return {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     */
    @Override
    public double getSupportUpperBound() {
        return SUPPORT_HI;
    }

    /** {@inheritDoc} */
    @Override
    double getMedian() {
        // Overridden for the probability(double, double) method.
        // This is intentionally not a public method.
        // u - beta * ln(ln(2))
        return mu - beta * LN_LN_2;
    }
}
