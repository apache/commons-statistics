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
import org.apache.commons.numbers.gamma.InverseErfc;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.LogNormalSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Implementation of the log-normal distribution.
 *
 * <p>\( X \) is log-normally distributed if its natural logarithm \( \ln(x) \)
 * is normally distributed. The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, \sigma) = \frac 1 {x\sigma\sqrt{2\pi\,}} e^{-{\frac 1 2}\left( \frac{\ln x-\mu}{\sigma} \right)^2 } \]
 *
 * <p>for \( \mu \) the mean of the normally distributed natural logarithm of this distribution,
 * \( \sigma &gt; 0 \) the standard deviation of the normally distributed natural logarithm of this
 * distribution, and
 * \( x \in (0, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Log-normal_distribution">Log-normal distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/LogNormalDistribution.html">Log-normal distribution (MathWorld)</a>
 */
public final class LogNormalDistribution extends AbstractContinuousDistribution {
    /** &radic;(2 &pi;). */
    private static final double SQRT2PI = Math.sqrt(2 * Math.PI);
    /** The mu parameter of this distribution. */
    private final double mu;
    /** The sigma parameter of this distribution. */
    private final double sigma;
    /** The value of {@code log(sigma) + 0.5 * log(2*PI)} stored for faster computation. */
    private final double logSigmaPlusHalfLog2Pi;
    /** Sigma multiplied by sqrt(2). */
    private final double sigmaSqrt2;
    /** Sigma multiplied by sqrt(2 * pi). */
    private final double sigmaSqrt2Pi;

    /**
     * @param mu Mean of the natural logarithm of the distribution values.
     * @param sigma Standard deviation of the natural logarithm of the distribution values.
     */
    private LogNormalDistribution(double mu,
                                  double sigma) {
        this.mu = mu;
        this.sigma = sigma;
        logSigmaPlusHalfLog2Pi = Math.log(sigma) + Constants.HALF_LOG_TWO_PI;
        sigmaSqrt2 = ExtendedPrecision.sqrt2xx(sigma);
        sigmaSqrt2Pi = sigma * SQRT2PI;
    }

    /**
     * Creates a log-normal distribution.
     *
     * @param mu Mean of the natural logarithm of the distribution values.
     * @param sigma Standard deviation of the natural logarithm of the distribution values.
     * @return the distribution
     * @throws IllegalArgumentException if {@code sigma <= 0}.
     */
    public static LogNormalDistribution of(double mu,
                                           double sigma) {
        if (sigma <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sigma);
        }
        return new LogNormalDistribution(mu, sigma);
    }

    /**
     * Gets the {@code mu} parameter of this distribution.
     * This is the mean of the natural logarithm of the distribution values,
     * not the mean of distribution.
     *
     * @return the mu parameter.
     */
    public double getMu() {
        return mu;
    }

    /**
     * Gets the {@code sigma} parameter of this distribution.
     * This is the standard deviation of the natural logarithm of the distribution values,
     * not the standard deviation of distribution.
     *
     * @return the sigma parameter.
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
        return Math.exp(-0.5 * x1 * x1) / (sigmaSqrt2Pi * x);
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
            return cumulativeProbability(x1);
        }
        // Assumes x1 >= x0 && x0 > 0
        final double v0 = (Math.log(x0) - mu) / sigmaSqrt2;
        final double v1 = (Math.log(x1) - mu) / sigmaSqrt2;
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
        return -0.5 * x1 * x1 - (logSigmaPlusHalfLog2Pi + logX);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@code mu}, and sigma {@code s} of this distribution, the CDF
     * is given by
     * <ul>
     * <li>{@code 0} if {@code x <= 0},</li>
     * <li>{@code 0} if {@code ln(x) - mu < 0} and {@code mu - ln(x) > 40 * s}, as
     * in these cases the actual value is within {@link Double#MIN_VALUE} of 0,
     * <li>{@code 1} if {@code ln(x) - mu >= 0} and {@code ln(x) - mu > 40 * s},
     * as in these cases the actual value is within {@link Double#MIN_VALUE} of
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
        return 0.5 * Erfc.value(-dev / sigmaSqrt2);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x)  {
        if (x <= 0) {
            return 1;
        }
        final double dev = Math.log(x) - mu;
        return 0.5 * Erfc.value(dev / sigmaSqrt2);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return Math.exp(mu - sigmaSqrt2 * InverseErfc.value(2 * p));
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return Math.exp(mu + sigmaSqrt2 * InverseErfc.value(2 * p));
    }

    /**
     * {@inheritDoc}
     *
     * <p>For \( \mu \) the mean of the normally distributed natural logarithm of
     * this distribution, \( \sigma &gt; 0 \) the standard deviation of the normally
     * distributed natural logarithm of this distribution, the mean is:
     *
     * <p>\[ \exp(\mu + \frac{\sigma^2}{2}) \]
     */
    @Override
    public double getMean() {
        final double s = sigma;
        return Math.exp(mu + (s * s / 2));
    }

    /**
     * {@inheritDoc}
     *
     * <p>For \( \mu \) the mean of the normally distributed natural logarithm of
     * this distribution, \( \sigma &gt; 0 \) the standard deviation of the normally
     * distributed natural logarithm of this distribution, the variance is:
     *
     * <p>\[ [\exp(\sigma^2) - 1)] \exp(2 \mu + \sigma^2) \]
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
     * @return {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Log normal distribution sampler.
        final ZigguratSampler.NormalizedGaussian gaussian = ZigguratSampler.NormalizedGaussian.of(rng);
        return LogNormalSampler.of(gaussian, mu, sigma)::sample;
    }
}
