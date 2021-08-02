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

import org.apache.commons.numbers.gamma.ErfDifference;
import org.apache.commons.numbers.gamma.Erfc;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.LogNormalSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Log-normal_distribution">log-normal distribution</a>.
 *
 * <p>
 * <strong>Parameters:</strong>
 * {@code X} is log-normally distributed if its natural logarithm {@code log(X)}
 * is normally distributed. The probability distribution function of {@code X}
 * is given by (for {@code x > 0})
 * </p>
 * <p>
 * {@code exp(-0.5 * ((ln(x) - mu) / s)^2) / (s * sqrt(2 * pi) * x)}
 * </p>
 * <ul>
 * <li>{@code mu} is the mean of the normally distributed natural logarithm of this distribution,</li>
 * <li>{@code s} is standard deviation of the normally distributed natural logarithm of this
 * distribution.
 * </ul>
 */
public class LogNormalDistribution extends AbstractContinuousDistribution {
    /** &radic;(2 &pi;). */
    private static final double SQRT2PI = Math.sqrt(2 * Math.PI);
    /** &radic;(2). */
    private static final double SQRT2 = Math.sqrt(2);
    /** The mu parameter of this distribution. */
    private final double mu;
    /** The sigma parameter of this distribution. */
    private final double sigma;
    /** The value of {@code log(sigma) + 0.5 * log(2*PI)} stored for faster computation. */
    private final double logShapePlusHalfLog2Pi;

    /**
     * Creates a log-normal distribution.
     *
     * @param mu Mean of the natural logarithm of the distribution values.
     * @param sigma Standard deviation of the natural logarithm of the distribution values.
     * @throws IllegalArgumentException if {@code sigma <= 0}.
     */
    public LogNormalDistribution(double mu,
                                 double sigma) {
        if (sigma <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sigma);
        }

        this.mu = mu;
        this.sigma = sigma;
        this.logShapePlusHalfLog2Pi = Math.log(sigma) + 0.5 * Math.log(2 * Math.PI);
    }

    /**
     * Returns the mu parameter of this distribution.
     * This is the mean of the natural logarithm of the distribution values,
     * not the mean of distribution.
     *
     * @return the mu parameter
     */
    public double getMu() {
        return mu;
    }

    /**
     * Returns the sigma parameter of this distribution.
     * This is the standard deviation of the natural logarithm of the distribution values,
     * not the standard deviation of distribution.
     *
     * @return the sigma parameter
     */
    public double getSigma() {
        return sigma;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code mu}, and sigma {@code s} of this distribution, the PDF
     * is given by
     * <ul>
     * <li>{@code 0} if {@code x <= 0},</li>
     * <li>{@code exp(-0.5 * ((ln(x) - mu) / s)^2) / (s * sqrt(2 * pi) * x)}
     * otherwise.</li>
     * </ul>
     */
    @Override
    public double density(double x) {
        if (x <= 0) {
            return 0;
        }
        final double x0 = Math.log(x) - mu;
        final double x1 = x0 / sigma;
        return Math.exp(-0.5 * x1 * x1) / (sigma * SQRT2PI * x);
    }

    /** {@inheritDoc} */
    @Override
    public double probability(double x0,
                              double x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH,
                                            x0, x1);
        }
        if (x0 <= 0) {
            return super.probability(x0, x1);
        }
        // Assumes x1 >= x0 && x0 > 0
        final double denom = sigma * SQRT2;
        final double v0 = (Math.log(x0) - mu) / denom;
        final double v1 = (Math.log(x1) - mu) / denom;
        return 0.5 * ErfDifference.value(v0, v1);
    }

    /** {@inheritDoc}
     *
     * <p>See documentation of {@link #density(double)} for computation details.
     */
    @Override
    public double logDensity(double x) {
        if (x <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        final double logX = Math.log(x);
        final double x0 = logX - mu;
        final double x1 = x0 / sigma;
        return -0.5 * x1 * x1 - (logShapePlusHalfLog2Pi + logX);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code mu}, and sigma {@code s} of this distribution, the CDF
     * is given by
     * <ul>
     * <li>{@code 0} if {@code x <= 0},</li>
     * <li>{@code 0} if {@code ln(x) - mu < 0} and {@code mu - ln(x) > 40 * s}, as
     * in these cases the actual value is within {@code Double.MIN_VALUE} of 0,
     * <li>{@code 1} if {@code ln(x) - mu >= 0} and {@code ln(x) - mu > 40 * s},
     * as in these cases the actual value is within {@code Double.MIN_VALUE} of
     * 1,</li>
     * <li>{@code 0.5 + 0.5 * erf((ln(x) - mu) / (s * sqrt(2))} otherwise.</li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(double x)  {
        if (x <= 0) {
            return 0;
        }
        final double dev = Math.log(x) - mu;
        if (Math.abs(dev) > 40 * sigma) {
            return dev < 0 ? 0.0d : 1.0d;
        }
        return 0.5 * Erfc.value(-dev / (sigma * SQRT2));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x)  {
        if (x <= 0) {
            return 1;
        }
        final double dev = Math.log(x) - mu;
        if (Math.abs(dev) > 40 * sigma) {
            return dev > 0 ? 0.0d : 1.0d;
        }
        return 0.5 * Erfc.value(dev / (sigma * SQRT2));
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code mu} and sigma {@code s}, the mean is
     * {@code exp(m + s^2 / 2)}.
     */
    @Override
    public double getMean() {
        final double s = sigma;
        return Math.exp(mu + (s * s / 2));
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code mu} and sigma {@code s}, the variance is
     * {@code (exp(s^2) - 1) * exp(2 * m + s^2)}.
     */
    @Override
    public double getVariance() {
        final double s = sigma;
        final double ss = s * s;
        return Math.expm1(ss) * Math.exp(2 * mu + ss);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0 no matter the parameters.
     *
     * @return lower bound of the support (always 0)
     */
    @Override
    public double getSupportLowerBound() {
        return 0;
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
        // Log normal distribution sampler.
        return LogNormalSampler.of(ZigguratNormalizedGaussianSampler.of(rng), mu, sigma)::sample;
    }
}
