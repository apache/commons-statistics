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
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Implementation of the Weibull distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x;k,\lambda) = \frac{k}{\lambda}\left(\frac{x}{\lambda}\right)^{k-1}e^{-(x/\lambda)^{k}} \]
 *
 * <p>for \( k &gt; 0 \) the shape,
 * \( \lambda &gt; 0 \) the scale, and
 * \( x \in (0, \infty) \).
 *
 * <p>Note the special cases:
 * <ul>
 * <li>\( k = 1 \) is the exponential distribution
 * <li>\( k = 2 \) is the Rayleigh distribution with scale \( \sigma = \frac {\lambda}{\sqrt{2}} \)
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Weibull_distribution">Weibull distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/WeibullDistribution.html">Weibull distribution (MathWorld)</a>
 */
public final class WeibullDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = 0;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** The shape parameter. */
    private final double shape;
    /** The scale parameter. */
    private final double scale;
    /** shape / scale. */
    private final double shapeOverScale;
    /** log(shape / scale). */
    private final double logShapeOverScale;

    /**
     * @param shape Shape parameter.
     * @param scale Scale parameter.
     */
    private WeibullDistribution(double shape,
                                double scale) {
        this.scale = scale;
        this.shape = shape;
        shapeOverScale = shape / scale;
        logShapeOverScale = Math.log(shapeOverScale);
    }

    /**
     * Creates a Weibull distribution.
     *
     * @param shape Shape parameter.
     * @param scale Scale parameter.
     * @return the distribution
     * @throws IllegalArgumentException if {@code shape <= 0} or {@code scale <= 0}.
     */
    public static WeibullDistribution of(double shape,
                                         double scale) {
        if (shape <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            shape);
        }
        if (scale <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            scale);
        }
        return new WeibullDistribution(shape, scale);
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
        if (x <= SUPPORT_LO || x >= SUPPORT_HI) {
            // Special case x=0
            if (x == SUPPORT_LO && shape <= 1) {
                return shape == 1 ?
                    // Exponential distribution
                    shapeOverScale :
                    Double.POSITIVE_INFINITY;
            }
            return 0;
        }

        final double xscale = x / scale;
        final double xscalepow = Math.pow(xscale, shape - 1);

        /*
         * Math.pow(x / scale, shape) =
         * Math.pow(xscale, shape) =
         * Math.pow(xscale, shape - 1) * xscale
         */
        final double xscalepowshape = xscalepow * xscale;

        return shapeOverScale * xscalepow * Math.exp(-xscalepowshape);
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code shape < 1}: Infinity
     * <li>{@code shape == 1}: log(1 / scale)
     * <li>{@code shape > 1}: -Infinity
     * </ul>
     */
    @Override
    public double logDensity(double x) {
        if (x <= SUPPORT_LO || x >= SUPPORT_HI) {
            // Special case x=0
            if (x == SUPPORT_LO && shape <= 1) {
                return shape == 1 ?
                    // Exponential distribution
                    logShapeOverScale :
                    Double.POSITIVE_INFINITY;
            }
            return Double.NEGATIVE_INFINITY;
        }

        final double xscale = x / scale;
        final double logxscalepow = Math.log(xscale) * (shape - 1);

        /*
         * Math.pow(x / scale, shape) =
         * Math.pow(xscale, shape) =
         * Math.pow(xscale, shape - 1) * xscale
         * Math.exp(log(xscale) * (shape - 1)) * xscale
         */
        final double xscalepowshape = Math.exp(logxscalepow) * xscale;

        return logShapeOverScale + logxscalepow - xscalepowshape;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 0;
        }

        return -Math.expm1(-Math.pow(x / scale, shape));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 1;
        }

        return Math.exp(-Math.pow(x / scale, shape));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code 0} when {@code p == 0} and
     * {@link Double#POSITIVE_INFINITY} when {@code p == 1}.
     */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return 0.0;
        } else  if (p == 1) {
            return Double.POSITIVE_INFINITY;
        }
        return scale * Math.pow(-Math.log1p(-p), 1.0 / shape);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code 0} when {@code p == 1} and
     * {@link Double#POSITIVE_INFINITY} when {@code p == 0}.
     */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return 0.0;
        } else  if (p == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return scale * Math.pow(-Math.log(p), 1.0 / shape);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter \( k \) and scale parameter \( \lambda \), the mean is:
     *
     * <p>\[ \lambda \, \Gamma(1+\frac{1}{k}) \]
     *
     * <p>where \( \Gamma \) is the Gamma-function.
     */
    @Override
    public double getMean() {
        final double sh = getShape();
        final double sc = getScale();

        // Special case of exponential when shape is 1
        return sh == 1 ? sc : sc * Math.exp(LogGamma.value(1 + (1 / sh)));
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter \( k \) and scale parameter \( \lambda \), the variance is:
     *
     * <p>\[ \lambda^2 \left[ \Gamma\left(1+\frac{2}{k}\right) -
     *                        \left(\Gamma\left(1+\frac{1}{k}\right)\right)^2 \right] \]
     *
     * <p>where \( \Gamma \) is the Gamma-function.
     */
    @Override
    public double getVariance() {
        final double sh = getShape();
        final double sc = getScale();
        final double mn = getMean();

        // Special case of exponential when shape is 1
        return sh == 1 ?
               sc * sc :
               (sc * sc) * Math.exp(LogGamma.value(1 + (2 / sh))) -
               (mn * mn);
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
     * @return {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     */
    @Override
    public double getSupportUpperBound() {
        return SUPPORT_HI;
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Special case: shape=1 is the exponential distribution
        if (shape == 1) {
            // Exponential distribution sampler.
            return ZigguratSampler.Exponential.of(rng, scale)::sample;
        }
        return super.createSampler(rng);
    }
}
