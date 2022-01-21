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

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Implementation of the chi-squared distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; k) = \frac{1}{2^{k/2} \Gamma(k/2)} x^{k/2 -1} e^{-x/2} \]
 *
 * <p>for \( k &gt; 0 \) the degrees of freedom,
 * \( \Gamma(k/2) \) is the gamma function, and
 * \( x \in [0, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Chi-squared_distribution">Chi-squared distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/Chi-SquaredDistribution.html">Chi-squared distribution (MathWorld)</a>
 */
public final class ChiSquaredDistribution extends AbstractContinuousDistribution {
    /** Internal Gamma distribution. */
    private final GammaDistribution gamma;

    /**
     * @param degreesOfFreedom Degrees of freedom.
     */
    private ChiSquaredDistribution(double degreesOfFreedom) {
        gamma = GammaDistribution.of(degreesOfFreedom / 2, 2);
    }

    /**
     * Creates a chi-squared distribution.
     *
     * @param degreesOfFreedom Degrees of freedom.
     * @return the distribution
     * @throws IllegalArgumentException if {@code degreesOfFreedom <= 0}.
     */
    public static ChiSquaredDistribution of(double degreesOfFreedom) {
        return new ChiSquaredDistribution(degreesOfFreedom);
    }

    /**
     * Gets the degrees of freedom parameter of this distribution.
     *
     * @return the degrees of freedom.
     */
    public double getDegreesOfFreedom() {
        return gamma.getShape() * 2;
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code df < 2}: Infinity
     * <li>{@code df == 2}: 1 / 2
     * <li>{@code df > 2}: 0
     * </ul>
     */
    @Override
    public double density(double x) {
        return gamma.density(x);
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code df < 2}: Infinity
     * <li>{@code df == 2}: log(1 / 2)
     * <li>{@code df > 2}: -Infinity
     * </ul>
     */
    @Override
    public double logDensity(double x) {
        return gamma.logDensity(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x)  {
        return gamma.cumulativeProbability(x);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        return gamma.survivalProbability(x);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        return gamma.inverseCumulativeProbability(p);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        return gamma.inverseSurvivalProbability(p);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code k} degrees of freedom, the mean is {@code k}.
     */
    @Override
    public double getMean() {
        return getDegreesOfFreedom();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code 2 * k}, where {@code k} is the number of degrees of freedom.
     */
    @Override
    public double getVariance() {
        return 2 * getDegreesOfFreedom();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0.
     *
     * @return 0.
     */
    @Override
    public double getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity.
     *
     * @return positive infinity.
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Sampling algorithms:
     * <ul>
     *  <li>
     *   For {@code 0 < degreesOfFreedom < 2}:
     *   <blockquote>
     *    Ahrens, J. H. and Dieter, U.,
     *    <i>Computer methods for sampling from gamma, beta, Poisson and binomial distributions,</i>
     *    Computing, 12, 223-246, 1974.
     *   </blockquote>
     *  </li>
     *  <li>
     *  For {@code degreesOfFreedom >= 2}:
     *   <blockquote>
     *   Marsaglia and Tsang, <i>A Simple Method for Generating
     *   Gamma Variables.</i> ACM Transactions on Mathematical Software,
     *   Volume 26 Issue 3, September, 2000.
     *   </blockquote>
     *  </li>
     * </ul>
     */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        return gamma.createSampler(rng);
    }
}
