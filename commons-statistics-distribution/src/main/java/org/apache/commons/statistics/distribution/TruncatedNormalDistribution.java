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
import org.apache.commons.numbers.gamma.Erfcx;

/**
 * Implementation of the truncated normal distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x;\mu,\sigma,a,b) = \frac{1}{\sigma}\,\frac{\phi(\frac{x - \mu}{\sigma})}{\Phi(\frac{b - \mu}{\sigma}) - \Phi(\frac{a - \mu}{\sigma}) } \]
 *
 * <p>for \( \mu \) mean of the parent normal distribution,
 * \( \sigma \) standard deviation of the parent normal distribution,
 * \( -\infty \le a \lt b \le \infty \) the truncation interval, and
 * \( x \in [a, b] \), where \( \phi \) is the probability
 * density function of the standard normal distribution and \( \Phi \)
 * is its cumulative distribution function.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Truncated_normal_distribution">
 * Truncated normal distribution (Wikipedia)</a>
 */
public final class TruncatedNormalDistribution extends AbstractContinuousDistribution {

    /** The max allowed value for x where (x*x) will not overflow.
     * This is a limit on computation of the moments of the truncated normal
     * as some calculations assume x*x is finite. Value is sqrt(MAX_VALUE). */
    private static final double MAX_X = 0x1.fffffffffffffp511;

    /** The min allowed probability range of the parent normal distribution.
     * Set to 0.0. This may be too low for accurate usage. It is a signal that
     * the truncation is invalid. */
    private static final double MIN_P = 0.0;

    /** sqrt(2). */
    private static final double ROOT2 = 1.414213562373095048801688724209698078;
    /** Normalisation constant 2 / sqrt(2 pi) = sqrt(2 / pi). */
    private static final double ROOT_2_PI = 0.797884560802865405726436165423365309;
    /** Normalisation constant sqrt(2 pi) / 2 = sqrt(pi / 2). */
    private static final double ROOT_PI_2 = 1.253314137315500251207882642405522626;

    /** Parent normal distribution. */
    private final NormalDistribution parentNormal;
    /** Lower bound of this distribution. */
    private final double lower;
    /** Upper bound of this distribution. */
    private final double upper;

    /** Stored value of {@code parentNormal.probability(lower, upper)}. This is used to
     * normalise the probability computations. */
    private final double cdfDelta;
    /** log(cdfDelta). */
    private final double logCdfDelta;
    /** Stored value of {@code parentNormal.cumulativeProbability(lower)}. Used to map
     * a probability into the range of the parent normal distribution. */
    private final double cdfAlpha;
    /** Stored value of {@code parentNormal.survivalProbability(upper)}. Used to map
     * a probability into the range of the parent normal distribution. */
    private final double sfBeta;

    /**
     * @param parent Parent distribution.
     * @param z Probability of the parent distribution for {@code [lower, upper]}.
     * @param lower Lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper Upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     */
    private TruncatedNormalDistribution(NormalDistribution parent, double z, double lower, double upper) {
        this.parentNormal = parent;
        this.lower = lower;
        this.upper = upper;

        cdfDelta = z;
        logCdfDelta = Math.log(cdfDelta);
        // Used to map the inverse probability.
        cdfAlpha = parentNormal.cumulativeProbability(lower);
        sfBeta = parentNormal.survivalProbability(upper);
    }

    /**
     * Creates a truncated normal distribution.
     *
     * <p>Note that the {@code mean} and {@code sd} is of the parent normal distribution,
     * and not the true mean and standard deviation of the truncated normal distribution.
     *
     * @param mean Mean for the parent distribution.
     * @param sd Standard deviation for the parent distribution.
     * @param lower Lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper Upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     * @return the distribution
     * @throws IllegalArgumentException if {@code sd <= 0}; if {@code lower >= upper}; or if
     * the truncation covers no probability range in the parent distribution.
     */
    public static TruncatedNormalDistribution of(double mean, double sd, double lower, double upper) {
        if (sd <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sd);
        }
        if (lower >= upper) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GTE_HIGH, lower, upper);
        }

        // Use an instance for the parent normal distribution to maximise accuracy
        // in range computations using the error function
        final NormalDistribution parent = NormalDistribution.of(mean, sd);

        // If there is no computable range then raise an exception.
        final double z = parent.probability(lower, upper);
        if (z <= MIN_P) {
            // Map the bounds to a standard normal distribution for the message
            final double a = (lower - mean) / sd;
            final double b = (upper - mean) / sd;
            throw new DistributionException(
               "Excess truncation of standard normal : CDF(%s, %s) = %s", a, b, z);
        }

        // Here we have a meaningful truncation. Note that excess truncation may not be optimal.
        // For example truncation close to zero where the PDF is constant can be approximated
        // using a uniform distribution.

        return new TruncatedNormalDistribution(parent, z, lower, upper);
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x < lower || x > upper) {
            return 0;
        }
        return parentNormal.density(x) / cdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(double x0, double x1) {
        if (x0 > x1) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GT_HIGH,
                                            x0, x1);
        }
        return parentNormal.probability(clipToRange(x0), clipToRange(x1)) / cdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        if (x < lower || x > upper) {
            return Double.NEGATIVE_INFINITY;
        }
        return parentNormal.logDensity(x) - logCdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= lower) {
            return 0;
        } else if (x >= upper) {
            return 1;
        }
        return parentNormal.probability(lower, x) / cdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= lower) {
            return 1;
        } else if (x >= upper) {
            return 0;
        }
        return parentNormal.probability(x, upper) / cdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        // Exact bound
        if (p == 0) {
            return lower;
        } else if (p == 1) {
            return upper;
        }
        // Linearly map p to the range [lower, upper]
        final double x = parentNormal.inverseCumulativeProbability(cdfAlpha + p * cdfDelta);
        return clipToRange(x);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        // Exact bound
        if (p == 1) {
            return lower;
        } else if (p == 0) {
            return upper;
        }
        // Linearly map p to the range [lower, upper]
        final double x = parentNormal.inverseSurvivalProbability(sfBeta + p * cdfDelta);
        return clipToRange(x);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Represents the true mean of the truncated normal distribution rather
     * than the parent normal distribution mean.
     */
    @Override
    public double getMean() {
        final double u = parentNormal.getMean();
        final double s = parentNormal.getStandardDeviation();
        final double a = (lower - u) / s;
        final double b = (upper - u) / s;
        return u + moment1(a, b) * s;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Represents the true variance of the truncated normal distribution rather
     * than the parent normal distribution variance.
     */
    @Override
    public double getVariance() {
        final double u = parentNormal.getMean();
        final double s = parentNormal.getStandardDeviation();
        final double a = (lower - u) / s;
        final double b = (upper - u) / s;
        return variance(a, b) * s * s;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is equal to the lower bound parameter
     * of the distribution.
     */
    @Override
    public double getSupportLowerBound() {
        return lower;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is equal to the upper bound parameter
     * of the distribution.
     */
    @Override
    public double getSupportUpperBound() {
        return upper;
    }

    /**
     * Clip the value to the range [lower, upper].
     * This is used to handle floating-point error at the support bound.
     *
     * @param x Value x
     * @return x clipped to the range
     */
    private double clipToRange(double x) {
        return clip(x, lower, upper);
    }

    /**
     * Clip the value to the range [lower, upper].
     *
     * @param x Value x
     * @param lower Lower bound (inclusive)
     * @param upper Upper bound (inclusive)
     * @return x clipped to the range
     */
    private static double clip(double x, double lower, double upper) {
        if (x <= lower) {
            return lower;
        }
        return x < upper ? x : upper;
    }

    // Calculation of variance and mean can suffer from cancellation.
    //
    // Use formulas from Jorge Fernandez-de-Cossio-Diaz adapted under the
    // terms of the MIT "Expat" License (see NOTICE and LICENSE).
    //
    // These formulas use the complementary error function
    //   erfcx(z) = erfc(z) * exp(z^2)
    // This avoids computation of exp terms for the Gaussian PDF and then
    // dividing by the error functions erf or erfc:
    //   exp(-0.5*x*x) / erfc(x / sqrt(2)) == 1 / erfcx(x / sqrt(2))
    // At large z the erfcx function is computable but exp(-0.5*z*z) and
    // erfc(z) are zero. Use of these formulas allows computation of the
    // mean and variance for the usable range of the truncated distribution
    // (cdf(a, b) != 0). The variance is not accurate when it approaches
    // machine epsilon (2^-52) at extremely narrow truncations and the
    // computation -> 0.
    //
    // See: https://github.com/cossio/TruncatedNormal.jl

    /**
     * Compute the first moment (mean) of the truncated standard normal distribution.
     *
     * <p>Assumes {@code a <= b}.
     *
     * @param a Lower bound
     * @param b Upper bound
     * @return the first moment
     */
    static double moment1(double a, double b) {
        // Assume a <= b
        if (a == b) {
            return a;
        }
        if (Math.abs(a) > Math.abs(b)) {
            // Subtract from zero to avoid generating -0.0
            return 0 - moment1(-b, -a);
        }

        // Here:
        // |a| <= |b|
        // a < b
        // 0 < b

        if (a <= -MAX_X) {
            // No truncation
            return 0;
        }
        if (b >= MAX_X) {
            // One-sided truncation
            return ROOT_2_PI / Erfcx.value(a / ROOT2);
        }

        // pdf = exp(-0.5*x*x) / sqrt(2*pi)
        // cdf = erfc(-x/sqrt(2)) / 2
        // Compute:
        // -(pdf(b) - pdf(a)) / cdf(b, a)
        // Note:
        // exp(-0.5*b*b) - exp(-0.5*a*a)
        // Use cancellation of powers:
        // exp(-0.5*(b*b-a*a)) * exp(-0.5*a*a) - exp(-0.5*a*a)
        // expm1(-0.5*(b*b-a*a)) * exp(-0.5*a*a)

        // dx = -0.5*(b*b-a*a)
        final double dx = 0.5 * (b + a) * (b - a);
        double m;
        if (a <= 0) {
            // Opposite signs
            m = ROOT_2_PI * -Math.expm1(-dx) * Math.exp(-0.5 * a * a) / ErfDifference.value(a / ROOT2, b / ROOT2);
        } else {
            final double z = Math.exp(-dx) * Erfcx.value(b / ROOT2) - Erfcx.value(a / ROOT2);
            if (z == 0) {
                // Occurs when a and b have large magnitudes and are very close
                return (a + b) * 0.5;
            }
            m = ROOT_2_PI * Math.expm1(-dx) / z;
        }

        // Clip to the range
        return clip(m, a, b);
    }

    /**
     * Compute the second moment of the truncated standard normal distribution.
     *
     * <p>Assumes {@code a <= b}.
     *
     * @param a Lower bound
     * @param b Upper bound
     * @return the first moment
     */
    private static double moment2(double a, double b) {
        // Assume a < b.
        // a == b is handled in the variance method
        if (Math.abs(a) > Math.abs(b)) {
            return moment2(-b, -a);
        }

        // Here:
        // |a| <= |b|
        // a < b
        // 0 < b

        if (a <= -MAX_X) {
            // No truncation
            return 1;
        }
        if (b >= MAX_X) {
            // One-sided truncation.
            // For a -> inf : moment2 -> a*a
            // This occurs when erfcx(z) is approximated by (1/sqrt(pi)) / z and terms
            // cancel. z > 6.71e7, a > 9.49e7
            return 1 + ROOT_2_PI * a / Erfcx.value(a / ROOT2);
        }

        // pdf = exp(-0.5*x*x) / sqrt(2*pi)
        // cdf = erfc(-x/sqrt(2)) / 2
        // Compute:
        // 1 - (b*pdf(b) - a*pdf(a)) / cdf(b, a)
        // = (cdf(b, a) - b*pdf(b) -a*pdf(a)) / cdf(b, a)

        // Note:
        // For z -> 0:
        //   sqrt(pi / 2) * erf(z / sqrt(2)) -> z
        //   z * Math.exp(-0.5 * z * z) -> z
        // Both computations below have cancellation as b -> 0 and the
        // second moment is not computable as the fraction P/Q
        // since P < ulp(Q). This always occurs when b < MIN_X
        // if MIN_X is set at the point where
        //   exp(-0.5 * z * z) / sqrt(2 pi) == 1 / sqrt(2 pi).
        // This is JDK dependent due to variations in Math.exp.
        // For b < MIN_X the second moment can be approximated using
        // a uniform distribution: (b^3 - a^3) / (3b - 3a).
        // In practice it also occurs when b > MIN_X since any a < MIN_X
        // is effectively zero for part of the computation. A
        // threshold to transition to a uniform distribution
        // approximation is a compromise. Also note it will not
        // correct computation when (b-a) is small and is far from 0.
        // Thus the second moment is left to be inaccurate for
        // small ranges (b-a) and the variance -> 0 when the true
        // variance is close to or below machine epsilon.

        double m;

        if (a <= 0) {
            // Opposite signs
            final double ea = ROOT_PI_2 * Erf.value(a / ROOT2);
            final double eb = ROOT_PI_2 * Erf.value(b / ROOT2);
            final double fa = ea - a * Math.exp(-0.5 * a * a);
            final double fb = eb - b * Math.exp(-0.5 * b * b);
            // Assume fb >= fa && eb >= ea
            // If fb <= fa this is a tiny range around 0
            m = (fb - fa) / (eb - ea);
            // Clip to the range
            m = clip(m, 0, 1);
        } else {
            final double dx = 0.5 * (b + a) * (b - a);
            final double ex = Math.exp(-dx);
            final double ea = ROOT_PI_2 * Erfcx.value(a / ROOT2);
            final double eb = ROOT_PI_2 * Erfcx.value(b / ROOT2);
            final double fa = ea + a;
            final double fb = eb + b;
            m = (fa - fb * ex) / (ea - eb * ex);
            // Clip to the range
            m = clip(m, a * a, b * b);
        }
        return m;
    }

    /**
     * Compute the variance of the truncated standard normal distribution.
     *
     * <p>Assumes {@code a <= b}.
     *
     * @param a Lower bound
     * @param b Upper bound
     * @return the first moment
     */
    static double variance(double a, double b) {
        if (a == b) {
            return 0;
        }

        final double m1 = moment1(a, b);
        double m2 = moment2(a, b);
        // variance = m2 - m1*m1
        // rearrange x^2 - y^2 as (x-y)(x+y)
        m2 = Math.sqrt(m2);
        final double variance = (m2 - m1) * (m2 + m1);

        // Detect floating-point error.
        if (variance >= 1) {
            // Note:
            // Extreme truncations in the tails can compute a variance above 1,
            // for example if m2 is infinite: m2 - m1*m1 > 1
            // Detect no truncation as the terms a and b lie far either side of zero;
            // otherwise return 0 to indicate very small unknown variance.
            return a < -1 && b > 1 ? 1 : 0;
        } else if (variance <= 0) {
            // Floating-point error can create negative variance so return 0.
            return 0;
        }

        return variance;
    }
}
