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

import org.apache.commons.numbers.gamma.LogGamma;
import org.apache.commons.numbers.gamma.RegularizedGamma;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.AhrensDieterMarsagliaTsangGammaSampler;

/**
 * Implementation of the gamma distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x;k,\theta) = \frac{x^{k-1}e^{-x/\theta}}{\theta^k\Gamma(k)} \]
 *
 * <p>for \( k &gt; 0 \) the shape, \( \theta &gt; 0 \) the scale, \( \Gamma(k) \) is the gamma function
 * and \( x \in (0, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Gamma_distribution">Gamma distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/GammaDistribution.html">Gamma distribution (MathWorld)</a>
 */
public final class GammaDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = 0;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;

    /** The shape parameter. */
    private final double shape;
    /** The scale parameter. */
    private final double scale;
    /** Precomputed term for the log density: {@code -log(gamma(shape)) - log(scale)}. */
    private final double minusLogGammaShapeMinusLogScale;
    /** Cached value for inverse probability function. */
    private final double mean;
    /** Cached value for inverse probability function. */
    private final double variance;

    /**
     * @param shape Shape parameter.
     * @param scale Scale parameter.
     */
    private GammaDistribution(double shape,
                              double scale) {
        this.shape = shape;
        this.scale = scale;
        this.minusLogGammaShapeMinusLogScale = -LogGamma.value(shape) - Math.log(scale);
        mean = shape * scale;
        variance = shape * scale * scale;
    }

    /**
     * Creates a gamma distribution.
     *
     * @param shape Shape parameter.
     * @param scale Scale parameter.
     * @return the distribution
     * @throws IllegalArgumentException if {@code shape <= 0} or {@code scale <= 0}.
     */
    public static GammaDistribution of(double shape,
                                       double scale) {
        if (shape <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, shape);
        }
        if (scale <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, scale);
        }
        return new GammaDistribution(shape, scale);
    }

    /**
     * Gets the shape parameter of this distribution.
     *
     * @return the shape parameter.
     */
    public double getShape() {
        return shape;
    }

    /**
     * Gets the scale parameter of this distribution.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return scale;
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code shape < 1}: Infinity
     * <li>{@code shape == 1}: 1 / scale
     * <li>{@code shape > 1}: 0
     * </ul>
     */
    @Override
    public double density(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            // Special case x=0
            if (x == SUPPORT_LO && shape <= 1) {
                return shape == 1 ?
                    1 / scale :
                    Double.POSITIVE_INFINITY;
            }
            return 0;
        }

        return RegularizedGamma.P.derivative(shape, x / scale) / scale;
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code shape < 1}: Infinity
     * <li>{@code shape == 1}: -log(scale)
     * <li>{@code shape > 1}: -Infinity
     * </ul>
     */
    @Override
    public double logDensity(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            // Special case x=0
            if (x == SUPPORT_LO && shape <= 1) {
                return shape == 1 ?
                    -Math.log(scale) :
                    Double.POSITIVE_INFINITY;
            }
            return Double.NEGATIVE_INFINITY;
        }

        final double y = x / scale;

        // More accurate to log the density when it is finite.
        // See NUMBERS-174: 'Log of the Gamma P Derivative'
        final double p = RegularizedGamma.P.derivative(shape, y) / scale;
        if (p <= Double.MAX_VALUE && p >= Double.MIN_NORMAL) {
            return Math.log(p);
        }
        // Use the log computation
        return minusLogGammaShapeMinusLogScale - y + Math.log(y) * (shape - 1);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The implementation of this method is based on:
     * <ul>
     *  <li>
     *   <a href="https://mathworld.wolfram.com/Chi-SquaredDistribution.html">
     *    Chi-Squared Distribution</a>, equation (9).
     *  </li>
     *  <li>Casella, G., &amp; Berger, R. (1990). <i>Statistical Inference</i>.
     *    Belmont, CA: Duxbury Press.
     *  </li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 0;
        } else if (x >= SUPPORT_HI) {
            return 1;
        }
        return RegularizedGamma.P.value(shape, x / scale);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 1;
        } else if (x >= SUPPORT_HI) {
            return 0;
        }
        return RegularizedGamma.Q.value(shape, x / scale);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter {@code alpha} and scale parameter {@code beta}, the
     * mean is {@code alpha * beta}.
     */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter {@code alpha} and scale parameter {@code beta}, the
     * variance is {@code alpha * beta^2}.
     */
    @Override
    public double getVariance() {
        return variance;
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
        return SUPPORT_LO;
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
        return SUPPORT_HI;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Sampling algorithms:
     * <ul>
     *  <li>
     *   For {@code 0 < shape < 1}:
     *   <blockquote>
     *    Ahrens, J. H. and Dieter, U.,
     *    <i>Computer methods for sampling from gamma, beta, Poisson and binomial distributions,</i>
     *    Computing, 12, 223-246, 1974.
     *   </blockquote>
     *  </li>
     *  <li>
     *  For {@code shape >= 1}:
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
        // Gamma distribution sampler.
        return AhrensDieterMarsagliaTsangGammaSampler.of(rng, shape, scale)::sample;
    }
}
