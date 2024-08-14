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

import org.apache.commons.numbers.gamma.Erf;
import org.apache.commons.numbers.gamma.ErfDifference;
import org.apache.commons.numbers.gamma.Erfc;
import org.apache.commons.numbers.gamma.InverseErf;
import org.apache.commons.numbers.gamma.InverseErfc;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.GaussianSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateContinuousSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;

/**
 * Implementation of the folded normal distribution.
 *
 * <p>Given a normally distributed random variable \( X \) with mean \( \mu \) and variance
 * \( \sigma^2 \), the random variable \( Y = |X| \) has a folded normal distribution. This is
 * equivalent to not recording the sign from a normally distributed random variable.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, \sigma) = \frac 1 {\sigma\sqrt{2\pi}} e^{-{\frac 1 2}\left( \frac{x-\mu}{\sigma} \right)^2 } +
 *                           \frac 1 {\sigma\sqrt{2\pi}} e^{-{\frac 1 2}\left( \frac{x+\mu}{\sigma} \right)^2 }\]
 *
 * <p>for \( \mu \) the location,
 * \( \sigma &gt; 0 \) the scale, and
 * \( x \in [0, \infty) \).
 *
 * <p>If the location \( \mu \) is 0 this reduces to the half-normal distribution.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Folded_normal_distribution">Folded normal distribution (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Half-normal_distribution">Half-normal distribution (Wikipedia)</a>
 * @since 1.1
 */
public abstract class FoldedNormalDistribution extends AbstractContinuousDistribution {
    /** Normalisation constant sqrt(2 / pi). */
    private static final double ROOT_TWO_DIV_PI = 0.7978845608028654;

    /** The scale. */
    final double sigma;
    /**
     * The scale multiplied by sqrt(2).
     * This is used to avoid a double division when computing the value passed to the
     * error function:
     * <pre>
     *  ((x - u) / sd) / sqrt(2) == (x - u) / (sd * sqrt(2)).
     *  </pre>
     * <p>Note: Implementations may first normalise x and then divide by sqrt(2) resulting
     * in differences due to rounding error that show increasingly large relative
     * differences as the error function computes close to 0 in the extreme tail.
     */
    final double sigmaSqrt2;
    /**
     * The scale multiplied by sqrt(2 pi). Computed to high precision.
     */
    final double sigmaSqrt2pi;

    /**
     * Regular implementation of the folded normal distribution.
     */
    private static class RegularFoldedNormalDistribution extends FoldedNormalDistribution {
        /** The location. */
        private final double mu;
        /** Cached value for inverse probability function. */
        private final double mean;
        /** Cached value for inverse probability function. */
        private final double variance;

        /**
         * @param mu Location parameter.
         * @param sigma Scale parameter.
         */
        RegularFoldedNormalDistribution(double mu, double sigma) {
            super(sigma);
            this.mu = mu;

            final double a = mu / sigmaSqrt2;
            mean = sigma * ROOT_TWO_DIV_PI * Math.exp(-a * a) + mu * Erf.value(a);
            this.variance = mu * mu + sigma * sigma - mean * mean;
        }

        @Override
        public double getMu() {
            return mu;
        }

        @Override
        public double density(double x) {
            if (x < 0) {
                return 0;
            }
            final double vm = (x - mu) / sigma;
            final double vp = (x + mu) / sigma;
            return (ExtendedPrecision.expmhxx(vm) + ExtendedPrecision.expmhxx(vp)) / sigmaSqrt2pi;
        }

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
            final double v0m = (x0 - mu) / sigmaSqrt2;
            final double v1m = (x1 - mu) / sigmaSqrt2;
            final double v0p = (x0 + mu) / sigmaSqrt2;
            final double v1p = (x1 + mu) / sigmaSqrt2;
            return 0.5 * (ErfDifference.value(v0m, v1m) + ErfDifference.value(v0p, v1p));
        }

        @Override
        public double cumulativeProbability(double x) {
            if (x <= 0) {
                return 0;
            }
            return 0.5 * (Erf.value((x - mu) / sigmaSqrt2) + Erf.value((x + mu) / sigmaSqrt2));
        }

        @Override
        public double survivalProbability(double x) {
            if (x <= 0) {
                return 1;
            }
            return 0.5 * (Erfc.value((x - mu) / sigmaSqrt2) + Erfc.value((x + mu) / sigmaSqrt2));
        }

        @Override
        public double getMean() {
            return mean;
        }

        @Override
        public double getVariance() {
            return variance;
        }

        @Override
        public Sampler createSampler(UniformRandomProvider rng) {
            // Return the absolute of a Gaussian distribution sampler.
            final SharedStateContinuousSampler s =
                GaussianSampler.of(ZigguratSampler.NormalizedGaussian.of(rng), mu, sigma);
            return () -> Math.abs(s.sample());
        }
    }

    /**
     * Specialisation for the half-normal distribution.
     *
     * <p>Elimination of the {@code mu} location parameter simplifies the probability
     * functions and allows computation of the log density and inverse CDF/SF.
     */
    private static class HalfNormalDistribution extends FoldedNormalDistribution {
        /** Variance constant (1 - 2/pi). */
        private static final double VAR = 0.363380227632418617567;
        /** ln(2). */
        private static final double LN_2 = 0.6931471805599453094172;
        /** 0.5 * ln(2 * pi). Computed to 25-digits precision. */
        private static final double HALF_LOG_TWO_PI = 0.9189385332046727417803297;
        /** The value of {@code log(sigma) + 0.5 * log(2*PI)} stored for faster computation. */
        private final double logSigmaPlusHalfLog2Pi;

        /**
         * @param sigma Scale parameter.
         */
        HalfNormalDistribution(double sigma) {
            super(sigma);
            logSigmaPlusHalfLog2Pi = Math.log(sigma) + HALF_LOG_TWO_PI;
        }

        @Override
        public double getMu() {
            return 0;
        }

        @Override
        public double density(double x) {
            if (x < 0) {
                return 0;
            }
            return 2 * ExtendedPrecision.expmhxx(x / sigma) / sigmaSqrt2pi;
        }

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
            return ErfDifference.value(x0 / sigmaSqrt2, x1 / sigmaSqrt2);
        }

        @Override
        public double logDensity(double x) {
            if (x < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            final double z = x / sigma;
            return LN_2 - 0.5 * z * z - logSigmaPlusHalfLog2Pi;
        }

        @Override
        public double cumulativeProbability(double x) {
            if (x <= 0) {
                return 0;
            }
            return Erf.value(x / sigmaSqrt2);
        }

        @Override
        public double survivalProbability(double x) {
            if (x <= 0) {
                return 1;
            }
            return Erfc.value(x / sigmaSqrt2);
        }

        @Override
        public double inverseCumulativeProbability(double p) {
            ArgumentUtils.checkProbability(p);
            // Addition of 0.0 ensures 0.0 is returned for p=-0.0
            return 0.0 + sigmaSqrt2 * InverseErf.value(p);
        }

        /** {@inheritDoc} */
        @Override
        public double inverseSurvivalProbability(double p) {
            ArgumentUtils.checkProbability(p);
            return sigmaSqrt2 * InverseErfc.value(p);
        }

        @Override
        public double getMean() {
            return sigma * ROOT_TWO_DIV_PI;
        }

        @Override
        public double getVariance() {
            // sigma^2 - mean^2
            // sigma^2 - (sigma^2 * 2/pi)
            return sigma * sigma * VAR;
        }

        @Override
        public Sampler createSampler(UniformRandomProvider rng) {
            // Return the absolute of a Gaussian distribution sampler.
            final SharedStateContinuousSampler s = ZigguratSampler.NormalizedGaussian.of(rng);
            return () -> Math.abs(s.sample() * sigma);
        }
    }

    /**
     * @param sigma Scale parameter.
     */
    FoldedNormalDistribution(double sigma) {
        this.sigma = sigma;
        // Minimise rounding error by computing sqrt(2 * sigma * sigma) exactly.
        // Compute using extended precision with care to avoid over/underflow.
        sigmaSqrt2 = ExtendedPrecision.sqrt2xx(sigma);
        // Compute sigma * sqrt(2 * pi)
        sigmaSqrt2pi = ExtendedPrecision.xsqrt2pi(sigma);
    }

    /**
     * Creates a folded normal distribution. If the location {@code mu} is zero this is
     * the half-normal distribution.
     *
     * @param mu Location parameter.
     * @param sigma Scale parameter.
     * @return the distribution
     * @throws IllegalArgumentException if {@code sigma <= 0}.
     */
    public static FoldedNormalDistribution of(double mu,
                                              double sigma) {
        if (sigma > 0) {
            if (mu == 0) {
                return new HalfNormalDistribution(sigma);
            }
            return new RegularFoldedNormalDistribution(mu, sigma);
        }
        // scale is zero, negative or nan
        throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sigma);
    }

    /**
     * Gets the location parameter \( \mu \) of this distribution.
     *
     * @return the mu parameter.
     */
    public abstract double getMu();

    /**
     * Gets the scale parameter \( \sigma \) of this distribution.
     *
     * @return the sigma parameter.
     */
    public double getSigma() {
        return sigma;
    }

    /**
     * {@inheritDoc}
     *
     *
     * <p>For location parameter \( \mu \) and scale parameter \( \sigma \), the mean is:
     *
     * <p>\[ \sigma \sqrt{ \frac 2 \pi } \exp \left( \frac{-u^2}{2\sigma^2} \right) +
     *       \mu \operatorname{erf} \left( \frac \mu {\sqrt{2\sigma^2}} \right) \]
     *
     * <p>where \( \operatorname{erf} \) is the error function.
     */
    @Override
    public abstract double getMean();

    /**
     * {@inheritDoc}
     *
     * <p>For location parameter \( \mu \), scale parameter \( \sigma \) and a distribution
     * mean \( \mu_Y \), the variance is:
     *
     * <p>\[ \mu^2 + \sigma^2 - \mu_{Y}^2 \]
     */
    @Override
    public abstract double getVariance();

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0.
     *
     * @return 0.
     */
    @Override
    public double getSupportLowerBound() {
        return 0.0;
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
}
