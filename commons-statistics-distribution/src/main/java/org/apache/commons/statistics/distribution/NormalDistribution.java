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

import org.apache.commons.numbers.gamma.Erfc;
import org.apache.commons.numbers.gamma.InverseErf;
import org.apache.commons.numbers.gamma.ErfDifference;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.GaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Normal_distribution">normal (Gaussian) distribution</a>.
 */
public class NormalDistribution extends AbstractContinuousDistribution {
    /** &radic;(2). */
    private static final double SQRT2 = Math.sqrt(2.0);
    /** Mean of this distribution. */
    private final double mean;
    /** Standard deviation of this distribution. */
    private final double standardDeviation;
    /** The value of {@code log(sd) + 0.5*log(2*pi)} stored for faster computation. */
    private final double logStandardDeviationPlusHalfLog2Pi;
    /** Standard deviation multiplied by sqrt(2). */
    private final double sdSqrt2;

    /**
     * Creates a distribution.
     *
     * @param mean Mean for this distribution.
     * @param sd Standard deviation for this distribution.
     * @throws IllegalArgumentException if {@code sd <= 0}.
     */
    public NormalDistribution(double mean,
                              double sd) {
        if (sd <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sd);
        }

        this.mean = mean;
        standardDeviation = sd;
        logStandardDeviationPlusHalfLog2Pi = Math.log(sd) + 0.5 * Math.log(2 * Math.PI);
        sdSqrt2 = sd * SQRT2;
    }

    /**
     * Access the standard deviation.
     *
     * @return the standard deviation for this distribution.
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        return Math.exp(logDensity(x));
    }

    /** {@inheritDoc} */
    @Override
    public double probability(double x0,
                              double x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH,
                                            x0, x1);
        }
        final double v0 = (x0 - mean) / sdSqrt2;
        final double v1 = (x1 - mean) / sdSqrt2;
        return 0.5 * ErfDifference.value(v0, v1);
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        final double x0 = x - mean;
        final double x1 = x0 / standardDeviation;
        return -0.5 * x1 * x1 - logStandardDeviationPlusHalfLog2Pi;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If {@code x} is more than 40 standard deviations from the mean, 0 or 1
     * is returned, as in these cases the actual value is within
     * {@code Double.MIN_VALUE} of 0 or 1.
     */
    @Override
    public double cumulativeProbability(double x)  {
        final double dev = x - mean;
        if (Math.abs(dev) > 40 * standardDeviation) {
            return dev < 0 ? 0.0d : 1.0d;
        }
        return 0.5 * Erfc.value(-dev / sdSqrt2);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        final double dev = x - mean;
        if (Math.abs(dev) > 40 * standardDeviation) {
            return dev > 0 ? 0.0d : 1.0d;
        }
        return 0.5 * Erfc.value(dev / sdSqrt2);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(final double p) {
        if (p < 0 ||
            p > 1) {
            throw new DistributionException(DistributionException.INVALID_PROBABILITY, p);
        }
        return mean + sdSqrt2 * InverseErf.value(2 * p - 1);
    }

    /** {@inheritDoc} */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For standard deviation parameter {@code s}, the variance is {@code s^2}.
     */
    @Override
    public double getVariance() {
        final double s = getStandardDeviation();
        return s * s;
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
        return Double.NEGATIVE_INFINITY;
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
        // Gaussian distribution sampler.
        return new GaussianSampler(new ZigguratNormalizedGaussianSampler(rng),
                                   mean, standardDeviation)::sample;
    }
}
