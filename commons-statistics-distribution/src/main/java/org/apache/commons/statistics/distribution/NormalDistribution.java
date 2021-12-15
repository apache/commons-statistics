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
import org.apache.commons.numbers.gamma.InverseErfc;
import org.apache.commons.numbers.gamma.ErfDifference;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.GaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Normal_distribution">normal (Gaussian) distribution</a>.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, \sigma) = \frac 1 {\sigma\sqrt{2\pi}} e^{-{\frac 1 2}\left( \frac{x-\mu}{\sigma} \right)^2 } \]
 *
 * <p>for \( \mu \) the mean,
 * \( \sigma &gt; 0 \) the standard deviation, and
 * \( x \in (-\infty, \infty) \).
 */
public final class NormalDistribution extends AbstractContinuousDistribution {
    /** 0.5 * ln(2 * pi). Computed to 25-digits precision. */
    private static final double HALF_LOG_TWO_PI = 0.9189385332046727417803297;

    /** Mean of this distribution. */
    private final double mean;
    /** Standard deviation of this distribution. */
    private final double standardDeviation;
    /** The value of {@code log(sd) + 0.5*log(2*pi)} stored for faster computation. */
    private final double logStandardDeviationPlusHalfLog2Pi;
    /**
     * Standard deviation multiplied by sqrt(2).
     * This is used to avoid a double division when computing the value passed to the
     * error function:
     * <pre>
     *  ((x - u) / sd) / sqrt(2) == (x - u) / (sd * sqrt(2)).
     *  </pre>
     * <p>Note: Implementations may first normalise x and then divide by sqrt(2) resulting
     * in differences due to rounding error that show increasingly large relative
     * differences as the error function computes close to 0 in the extreme tail.
     */
    private final double sdSqrt2;
    /**
     * Standard deviation multiplied by sqrt(2 pi). Computed to high precision.
     */
    private final double sdSqrt2pi;

    /**
     * @param mean Mean for this distribution.
     * @param sd Standard deviation for this distribution.
     */
    private NormalDistribution(double mean,
                               double sd) {
        this.mean = mean;
        standardDeviation = sd;
        logStandardDeviationPlusHalfLog2Pi = Math.log(sd) + HALF_LOG_TWO_PI;
        // Minimise rounding error by computing sqrt(2 * sd * sd) exactly.
        // Compute using extended precision with care to avoid over/underflow.
        sdSqrt2 = ExtendedPrecision.sqrt2xx(sd);
        // Compute sd * sqrt(2 * pi)
        sdSqrt2pi = ExtendedPrecision.xsqrt2pi(sd);
    }

    /**
     * Creates a normal distribution.
     *
     * @param mean Mean for this distribution.
     * @param sd Standard deviation for this distribution.
     * @return the distribution
     * @throws IllegalArgumentException if {@code sd <= 0}.
     */
    public static NormalDistribution of(double mean,
                                        double sd) {
        if (sd > 0) {
            return new NormalDistribution(mean, sd);
        }
        // zero, negative or nan
        throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sd);
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
        final double x0 = x - mean;
        final double x1 = x0 / standardDeviation;
        return Math.exp(-0.5 * x1 * x1) / sdSqrt2pi;
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

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x)  {
        final double dev = x - mean;
        return 0.5 * Erfc.value(-dev / sdSqrt2);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        final double dev = x - mean;
        return 0.5 * Erfc.value(dev / sdSqrt2);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return mean - sdSqrt2 * InverseErfc.value(2 * p);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        return mean + sdSqrt2 * InverseErfc.value(2 * p);
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

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Gaussian distribution sampler.
        return GaussianSampler.of(ZigguratSampler.NormalizedGaussian.of(rng),
                                  mean, standardDeviation)::sample;
    }
}
