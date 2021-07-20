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
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Chi-squared_distribution">chi-squared distribution</a>.
 */
public class ChiSquaredDistribution extends AbstractContinuousDistribution {
    /** Internal Gamma distribution. */
    private final GammaDistribution gamma;

    /**
     * Creates a distribution.
     *
     * @param degreesOfFreedom Degrees of freedom.
     */
    public ChiSquaredDistribution(double degreesOfFreedom) {
        gamma = new GammaDistribution(degreesOfFreedom / 2, 2);
    }

    /**
     * Access the number of degrees of freedom.
     *
     * @return the degrees of freedom.
     */
    public double getDegreesOfFreedom() {
        return gamma.getShape() * 2;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        return gamma.density(x);
    }

    /** {@inheritDoc} **/
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

    /**
     * {@inheritDoc}
     *
     * For {@code k} degrees of freedom, the mean is {@code k}.
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
     * The lower bound of the support is always 0 no matter the
     * degrees of freedom.
     *
     * @return zero.
     */
    @Override
    public double getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * The upper bound of the support is always positive infinity no matter the
     * degrees of freedom.
     *
     * @return {@code Double.POSITIVE_INFINITY}.
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
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
