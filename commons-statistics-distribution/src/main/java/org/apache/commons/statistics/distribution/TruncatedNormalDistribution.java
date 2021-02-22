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
public class TruncatedNormalDistribution extends AbstractContinuousDistribution {
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

    /** A standard normal distribution used for calculations. */
    private final NormalDistribution standardNormal;
    /** Stored value of @{code standardNormal.cumulativeProbability((lower - mean) / sd)} for faster computations. */
    private final double cdfAlpha;
    /**
     * Stored value of @{code standardNormal.cumulativeProbability((upper - mean) / sd) - cdfAlpha}
     * for faster computations.
     */
    private final double cdfDelta;

    /**
     * Creates a truncated normal distribution.
     * Note that the {@code mean} and {@code sd} is of the parent normal distribution,
     * and not the true mean and standard deviation of the truncated normal distribution.
     *
     * @param mean mean for this distribution.
     * @param sd standard deviation for this distribution.
     * @param lower lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     * @throws IllegalArgumentException if {@code sd <= 0} or if {@code upper <= lower}.
     */
    public TruncatedNormalDistribution(double mean, double sd, double lower, double upper) {
        if (sd <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, sd);
        }
        if (lower >= upper) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GTE_HIGH, lower, upper);
        }

        this.lower = lower;
        this.upper = upper;

        parentMean = mean;
        parentSd = sd;
        standardNormal = new NormalDistribution(0, 1);

        final double alpha = (lower - mean) / sd;
        final double beta = (upper - mean) / sd;

        final double cdfBeta = standardNormal.cumulativeProbability(beta);
        cdfAlpha = standardNormal.cumulativeProbability(alpha);
        cdfDelta = cdfBeta - cdfAlpha;

        // Calculation of variance and mean.
        final double pdfAlpha = standardNormal.density(alpha);
        final double pdfBeta = standardNormal.density(beta);
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

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x < lower || x > upper) {
            return 0;
        }
        return standardNormal.density((x - parentMean) / parentSd) / (parentSd * cdfDelta);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= lower) {
            return 0;
        } else if (x >= upper) {
            return 1;
        }
        return (standardNormal.cumulativeProbability((x - parentMean) / parentSd) - cdfAlpha) / cdfDelta;
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        if (p < 0 || p > 1) {
            throw new DistributionException(DistributionException.INVALID_PROBABILITY, p);
        }
        return standardNormal.inverseCumulativeProbability(cdfAlpha + p * cdfDelta) * parentSd + parentMean;
    }

    /**
     * {@inheritDoc}
     *
     * Represents the true mean of the truncated normal distribution rather
     * than the parent normal distribution mean.
     */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * Represents the true variance of the truncated normal distribution rather
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

    /** {@inheritDoc} */
    @Override
    public boolean isSupportConnected() {
        return true;
    }
}
