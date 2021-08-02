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
import org.apache.commons.rng.sampling.distribution.InverseTransformParetoSampler;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Pareto_distribution">Pareto distribution</a>.
 *
 * <p>
 * <strong>Parameters:</strong>
 * The probability distribution function of {@code X} is given by (for {@code x >= k}):
 * <pre>
 *  α * k^α / x^(α + 1)
 * </pre>
 * <ul>
 * <li>{@code k} is the <em>scale</em> parameter: this is the minimum possible value of {@code X},</li>
 * <li>{@code α} is the <em>shape</em> parameter: this is the Pareto index</li>
 * </ul>
 */
public class ParetoDistribution extends AbstractContinuousDistribution {
    /** The minimum value for the shape parameter when computing when computing the variance. */
    private static final double MIN_SHAPE_FOR_VARIANCE = 2.0;

    /** The scale parameter of this distribution. */
    private final double scale;
    /** The shape parameter of this distribution. */
    private final double shape;
    /** shape * scale^shape. */
    private final double shapeByScalePowShape;
    /** log(shape) + shape * log(scale). */
    private final double logShapePlusShapeByLogScale;

    /**
     * Creates a Pareto distribution.
     *
     * @param scale Scale parameter of this distribution.
     * @param shape Shape parameter of this distribution.
     * @throws IllegalArgumentException if {@code scale <= 0} or {@code shape <= 0}.
     */
    public ParetoDistribution(double scale,
                              double shape) {
        if (scale <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, scale);
        }

        if (shape <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, shape);
        }

        this.scale = scale;
        this.shape = shape;
        shapeByScalePowShape = shape * Math.pow(scale, shape);
        logShapePlusShapeByLogScale = Math.log(shape) + Math.log(scale) * shape;
    }

    /**
     * Returns the scale parameter of this distribution.
     *
     * @return the scale parameter
     */
    public double getScale() {
        return scale;
    }

    /**
     * Returns the shape parameter of this distribution.
     *
     * @return the shape parameter
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
        return shapeByScalePowShape / Math.pow(x, shape + 1);
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
        return logShapePlusShapeByLogScale - Math.log(x) * (shape + 1);
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
        // Can be improved by improving log calculation
        return -Math.expm1(shape * Math.log(scale / x));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x)  {
        if (x <= scale) {
            return 1;
        }
        return Math.pow(scale / x, shape);
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
        return shape * scale / (shape - 1);
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
        final double s = shape - 1;
        return scale * scale * shape / (s * s) / (shape - 2);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The lower bound of the support is equal to the scale parameter {@code k}.
     *
     * @return lower bound of the support
     */
    @Override
    public double getSupportLowerBound() {
        return getScale();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The upper bound of the support is always positive infinity no matter the parameters.
     *
     * @return upper bound of the support (always {@code Double.POSITIVE_INFINITY})
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
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

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Pareto distribution sampler.
        return new InverseTransformParetoSampler(rng, scale, shape)::sample;
    }
}
