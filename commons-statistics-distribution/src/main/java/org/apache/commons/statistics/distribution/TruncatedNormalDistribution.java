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
 * @see <a href="http://en.wikipedia.org/wiki/Truncated_normal_distribution">
 * Truncated normal distribution (Wikipedia)</a>
 */
public final class TruncatedNormalDistribution extends AbstractContinuousDistribution {
    /** A standard normal distribution used for calculations.
     * This is immutable and thread-safe and can be used across instances. */
    private static final NormalDistribution STANDARD_NORMAL = NormalDistribution.of(0, 1);

    /** Mean of parent normal distribution. */
    private final double parentMean;
    /** Standard deviation of parent normal distribution. */
    private final double parentSd;
    /** Mean of this distribution. */
    private final double mean;
    /** Variance of this distribution. */
    private final double variance;
    /** Lower bound of this distribution. */
    private final double lower;
    /** Upper bound of this distribution. */
    private final double upper;

    /** Stored value of @{code standardNormal.cumulativeProbability((lower - mean) / sd)} for faster computations. */
    private final double cdfAlpha;
    /**
     * Stored value of @{code standardNormal.cumulativeProbability((upper - mean) / sd) - cdfAlpha}
     * for faster computations.
     */
    private final double cdfDelta;
    /** parentSd * cdfDelta. */
    private final double parentSdByCdfDelta;
    /** log(parentSd * cdfDelta). */
    private final double logParentSdByCdfDelta;

    /**
     * @param mean Mean for the parent distribution.
     * @param sd Standard deviation for the parent distribution.
     * @param lower Lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper Upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     */
    private TruncatedNormalDistribution(double mean, double sd, double lower, double upper) {
        this.lower = lower;
        this.upper = upper;

        parentMean = mean;
        parentSd = sd;

        final double alpha = (lower - mean) / sd;
        final double beta = (upper - mean) / sd;

        final double cdfBeta = STANDARD_NORMAL.cumulativeProbability(beta);
        cdfAlpha = STANDARD_NORMAL.cumulativeProbability(alpha);
        cdfDelta = cdfBeta - cdfAlpha;

        parentSdByCdfDelta = parentSd * cdfDelta;
        logParentSdByCdfDelta = Math.log(parentSdByCdfDelta);

        // Calculation of variance and mean.
        final double pdfAlpha = STANDARD_NORMAL.density(alpha);
        final double pdfBeta = STANDARD_NORMAL.density(beta);
        final double pdfCdfDelta = (pdfAlpha - pdfBeta) / cdfDelta;
        final double alphaBetaDelta = (alpha * pdfAlpha - beta * pdfBeta) / cdfDelta;

        if (lower == Double.NEGATIVE_INFINITY) {
            if (upper == Double.POSITIVE_INFINITY) {
                // No truncation
                this.mean = mean;
                variance = sd * sd;
            } else {
                // One-sided lower tail truncation
                final double betaRatio = pdfBeta / cdfBeta;
                this.mean = mean - sd * betaRatio;
                variance = sd * sd * (1 - beta * betaRatio - betaRatio * betaRatio);
            }
        } else {
            if (upper == Double.POSITIVE_INFINITY) {
                // One-sided upper tail truncation
                final double alphaRatio = pdfAlpha / cdfDelta;
                this.mean = mean + sd * alphaRatio;
                variance = sd * sd * (1 + alpha * alphaRatio - alphaRatio * alphaRatio);
            } else {
                // Two-sided truncation
                this.mean = mean + pdfCdfDelta * parentSd;
                variance = sd * sd * (1 + alphaBetaDelta - pdfCdfDelta * pdfCdfDelta);
            }
        }
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
        return STANDARD_NORMAL.density((x - parentMean) / parentSd) / parentSdByCdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        if (x < lower || x > upper) {
            return Double.NEGATIVE_INFINITY;
        }
        return STANDARD_NORMAL.logDensity((x - parentMean) / parentSd) - logParentSdByCdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= lower) {
            return 0;
        } else if (x >= upper) {
            return 1;
        }
        return (STANDARD_NORMAL.cumulativeProbability((x - parentMean) / parentSd) - cdfAlpha) / cdfDelta;
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
        final double x = STANDARD_NORMAL.inverseCumulativeProbability(cdfAlpha + p * cdfDelta) * parentSd + parentMean;
        // Clip to support to handle floating-point error at the support bound
        if (x <= lower) {
            return lower;
        }
        return x < upper ? x : upper;
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
}
