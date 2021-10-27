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

/**
 * Implementation of the truncated normal distribution.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Truncated_normal_distribution">
 * Truncated normal distribution (Wikipedia)</a>
 */
public final class TruncatedNormalDistribution extends AbstractContinuousDistribution {
    /** A standard normal distribution used for calculations.
     * This is immutable and thread-safe and can be used across instances. */
    private static final NormalDistribution STANDARD_NORMAL = NormalDistribution.of(0, 1);

    /** Parent normal distribution. */
    private final NormalDistribution parentNormal;
    /** Mean of this distribution. */
    private final double mean;
    /** Variance of this distribution. */
    private final double variance;
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

    /**
     * @param mean Mean for the parent distribution.
     * @param sd Standard deviation for the parent distribution.
     * @param lower Lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper Upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     */
    private TruncatedNormalDistribution(double mean, double sd, double lower, double upper) {
        this.lower = lower;
        this.upper = upper;

        // Use an instance for the parent normal distribution to maximise accuracy
        // in range computations using the error function
        parentNormal = NormalDistribution.of(mean, sd);

        cdfDelta = parentNormal.probability(lower, upper);
        logCdfDelta = Math.log(cdfDelta);
        // Used to map the inverseCumulativeProbability
        cdfAlpha = parentNormal.cumulativeProbability(lower);

        // Calculation of variance and mean.
        //
        // Use the equations provided on Wikipedia:
        // https://en.wikipedia.org/wiki/Truncated_normal_distribution#Moments

        final double alpha = (lower - mean) / sd;
        final double beta = (upper - mean) / sd;
        final double pdfAlpha = STANDARD_NORMAL.density(alpha);
        final double pdfBeta = STANDARD_NORMAL.density(beta);

        // lower or upper may be infinite or the density is zero.

        double mu;
        double v;

        if (lower == Double.NEGATIVE_INFINITY || pdfAlpha == 0) {
            if (upper == Double.POSITIVE_INFINITY || pdfBeta == 0) {
                // No truncation
                mu = mean;
                v = sd * sd;
            } else {
                // One sided truncation (of upper tail)
                final double betaRatio = pdfBeta / cdfDelta;
                mu = mean - sd * betaRatio;
                v = sd * sd * (1 - beta * betaRatio - betaRatio * betaRatio);
            }
        } else {
            if (upper == Double.POSITIVE_INFINITY || pdfBeta == 0) {
                // One sided truncation (of lower tail)
                final double alphaRatio = pdfAlpha / cdfDelta;
                mu = mean + sd * alphaRatio;
                v = sd * sd * (1 + alpha * alphaRatio - alphaRatio * alphaRatio);
            } else {
                // Two-sided truncation
                // Note:
                // This computation is numerically unstable and requires improvement.

                // Do not use z = cdfDelta which can create cancellation.
                final double cdfBeta = parentNormal.cumulativeProbability(upper);
                final double z = cdfBeta - cdfAlpha;
                final double pdfCdfDelta = (pdfAlpha - pdfBeta) / z;
                final double alphaBetaDelta = (alpha * pdfAlpha - beta * pdfBeta) / z;
                mu = mean + pdfCdfDelta * sd;
                v = sd * sd * (1 + alphaBetaDelta - pdfCdfDelta * pdfCdfDelta);
            }
        }

        // The mean should be clipped to the range [lower, upper].
        // The variance should be less than the variance of the parent normal distribution.
        this.mean = clipToRange(mu);
        variance = Math.min(v, sd * sd);
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
     * @throws IllegalArgumentException if {@code sd <= 0} or if {@code upper <= lower}.
     */
    public static TruncatedNormalDistribution of(double mean, double sd, double lower, double upper) {
        if (sd <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sd);
        }
        if (lower >= upper) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GTE_HIGH, lower, upper);
        }
        return new TruncatedNormalDistribution(mean, sd, lower, upper);

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

    /**
     * {@inheritDoc}
     *
     * <p>Represents the true mean of the truncated normal distribution rather
     * than the parent normal distribution mean.
     */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Represents the true variance of the truncated normal distribution rather
     * than the parent normal distribution variance.
     */
    @Override
    public double getVariance() {
        return variance;
    }

    /** {@inheritDoc} */
    @Override
    public double getSupportLowerBound() {
        return lower;
    }

    /** {@inheritDoc} */
    @Override
    public double getSupportUpperBound() {
        return upper;
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

    /**
     * Clip to the value to the range [lower, upper].
     * This is used to handle floating-point error at the support bound.
     *
     * @param x the x
     * @return x clipped to the range
     */
    private double clipToRange(double x) {
        if (x <= lower) {
            return lower;
        }
        return x < upper ? x : upper;
    }
}
