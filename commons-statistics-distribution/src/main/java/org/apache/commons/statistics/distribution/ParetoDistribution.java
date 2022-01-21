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

import java.util.function.DoubleUnaryOperator;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InverseTransformParetoSampler;

/**
 * Implementation of the Pareto (Type I) distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; k, \alpha) = \frac{\alpha  k^\alpha}{x^{\alpha + 1}} \]
 *
 * <p>for \( k &gt; 0 \),
 * \( \alpha &gt; 0 \), and
 * \( x \in [k, \infty) \).
 *
 * <p>\( k \) is a <em>scale</em> parameter: this is the minimum possible value of \( X \).
 * <br>\( \alpha \) is a <em>shape</em> parameter: this is the Pareto index.
 *
 * @see  <a href="https://en.wikipedia.org/wiki/Pareto_distribution">Pareto distribution (Wikipedia)</a>
 * @see  <a href="https://mathworld.wolfram.com/ParetoDistribution.html">Pareto distribution (MathWorld)</a>
 */
public final class ParetoDistribution extends AbstractContinuousDistribution {
    /** The minimum value for the shape parameter when computing when computing the variance. */
    private static final double MIN_SHAPE_FOR_VARIANCE = 2.0;

    /** The scale parameter of this distribution. Also known as {@code k};
     * the minimum possible value for the random variable {@code X}. */
    private final double scale;
    /** The shape parameter of this distribution. */
    private final double shape;
    /** Implementation of PDF(x). Assumes that {@code x >= scale}. */
    private final DoubleUnaryOperator pdf;
    /** Implementation of log PDF(x). Assumes that {@code x >= scale}. */
    private final DoubleUnaryOperator logpdf;

    /**
     * @param scale Scale parameter (minimum possible value of X).
     * @param shape Shape parameter (Pareto index).
     */
    private ParetoDistribution(double scale,
                               double shape) {
        this.scale = scale;
        this.shape = shape;

        // The Pareto distribution approaches a Dirac delta function when shape -> inf.
        // Parameterisations can also lead to underflow in the standard computation.
        // Extract the PDF and CDF to specialized implementations to handle edge cases.

        // Pre-compute factors for the standard computation
        final double shapeByScalePowShape = shape * Math.pow(scale, shape);
        final double logShapePlusShapeByLogScale = Math.log(shape) + Math.log(scale) * shape;

        if (shapeByScalePowShape < Double.POSITIVE_INFINITY &&
            shapeByScalePowShape >= Double.MIN_NORMAL) {
            // Standard computation
            pdf = x -> shapeByScalePowShape / Math.pow(x, shape + 1);
            logpdf = x -> logShapePlusShapeByLogScale - Math.log(x) * (shape + 1);
        } else {
            // Standard computation overflow; underflow to sub-normal or zero; or nan (pow(1.0, inf))
            if (Double.isFinite(logShapePlusShapeByLogScale)) {
                // Log computation is valid
                logpdf = x -> logShapePlusShapeByLogScale - Math.log(x) * (shape + 1);
                pdf = x -> Math.exp(logpdf.applyAsDouble(x));
            } else  {
                // Assume Dirac function
                logpdf = x -> x > scale ? -Double.POSITIVE_INFINITY : Double.POSITIVE_INFINITY;
                // PDF has infinite value at lower bound
                pdf = x -> x > scale ? 0 : Double.POSITIVE_INFINITY;
            }
        }
    }

    /**
     * Creates a Pareto distribution.
     *
     * @param scale Scale parameter (minimum possible value of X).
     * @param shape Shape parameter (Pareto index).
     * @return the distribution
     * @throws IllegalArgumentException if {@code scale <= 0}, {@code scale} is
     * infinite, or {@code shape <= 0}.
     */
    public static ParetoDistribution of(double scale,
                                        double shape) {
        if (scale <= 0 || scale == Double.POSITIVE_INFINITY) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE_FINITE, scale);
        }

        if (shape <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, shape);
        }
        return new ParetoDistribution(scale, shape);
    }

    /**
     * Gets the scale parameter of this distribution.
     * This is the minimum possible value of X.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Gets the shape parameter of this distribution.
     * This is the Pareto index.
     *
     * @return the shape parameter.
     */
    public double getShape() {
        return shape;
    }

    /**
     * {@inheritDoc}
     * <p>
     * For scale {@code k}, and shape {@code α} of this distribution, the PDF
     * is given by
     * <ul>
     * <li>{@code 0} if {@code x < k},</li>
     * <li>{@code α * k^α / x^(α + 1)} otherwise.</li>
     * </ul>
     */
    @Override
    public double density(double x) {
        if (x < scale) {
            return 0;
        }
        return pdf.applyAsDouble(x);
    }

    /** {@inheritDoc}
     *
     * <p>See documentation of {@link #density(double)} for computation details.
     */
    @Override
    public double logDensity(double x) {
        if (x < scale) {
            return Double.NEGATIVE_INFINITY;
        }
        return logpdf.applyAsDouble(x);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For scale {@code k}, and shape {@code α} of this distribution, the CDF is given by
     * <ul>
     * <li>{@code 0} if {@code x < k},</li>
     * <li>{@code 1 - (k / x)^α} otherwise.</li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(double x)  {
        if (x <= scale) {
            return 0;
        }
        // Increase accuracy for CDF close to 0 by using a log calculation:
        // 1 - exp(α * ln(k / x)) == -(exp(α * ln(k / x)) - 1)
        return -Math.expm1(shape * Math.log(scale / x));
    }

    /**
     * {@inheritDoc}
     * <p>
     * For scale {@code k}, and shape {@code α} of this distribution, the survival function is given by
     * <ul>
     * <li>{@code 1} if {@code x < k},</li>
     * <li>{@code (k / x)^α} otherwise.</li>
     * </ul>
     */
    @Override
    public double survivalProbability(double x)  {
        if (x <= scale) {
            return 1;
        }
        return Math.pow(scale / x, shape);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return getSupportLowerBound();
        }
        if (p == 1) {
            return getSupportUpperBound();
        }
        return scale / Math.exp(Math.log1p(-p) / shape);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return getSupportLowerBound();
        }
        if (p == 0) {
            return getSupportUpperBound();
        }
        return scale / Math.pow(p, 1 / shape);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For scale {@code k} and shape {@code α}, the mean is given by
     * <ul>
     * <li>{@code ∞} if {@code α <= 1},</li>
     * <li>{@code α * k / (α - 1)} otherwise.</li>
     * </ul>
     */
    @Override
    public double getMean() {
        if (shape <= 1) {
            return Double.POSITIVE_INFINITY;
        }
        if (shape == Double.POSITIVE_INFINITY) {
            return scale;
        }
        return scale * (shape / (shape - 1));
    }

    /**
     * {@inheritDoc}
     * <p>
     * For scale {@code k} and shape {@code α}, the variance is given by
     * <ul>
     * <li>{@code ∞} if {@code 1 < α <= 2},</li>
     * <li>{@code k^2 * α / ((α - 1)^2 * (α - 2))} otherwise.</li>
     * </ul>
     */
    @Override
    public double getVariance() {
        if (shape <= MIN_SHAPE_FOR_VARIANCE) {
            return Double.POSITIVE_INFINITY;
        }
        if (shape == Double.POSITIVE_INFINITY) {
            return 0;
        }
        final double s = shape - 1;
        final double z = shape / s / s / (shape - 2);
        // Avoid intermediate overflow of scale^2 if z is small
        return z < 1 ? z * scale * scale : scale * scale * z;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The lower bound of the support is equal to the scale parameter {@code k}.
     *
     * @return scale.
     */
    @Override
    public double getSupportLowerBound() {
        return getScale();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The upper bound of the support is always positive infinity.
     *
     * @return positive infinity.
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Pareto distribution sampler.
        return InverseTransformParetoSampler.of(rng, scale, shape)::sample;
    }
}
