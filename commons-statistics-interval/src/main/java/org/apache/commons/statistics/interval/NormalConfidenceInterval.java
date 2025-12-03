/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.interval;

import org.apache.commons.statistics.distribution.ChiSquaredDistribution;
import org.apache.commons.statistics.distribution.TDistribution;

/**
 * Generate confidence intervals for a normally distributed population.
 *
 * @see <a
 * href="https://en.wikipedia.org/wiki/Normal_distribution#Confidence_intervals">Normal
 * distribution confidence interval (Wikipedia)</a>
 *
 * @since 1.2
 */
public enum NormalConfidenceInterval {
    /**
     * Create a confidence interval for the true mean of an unknown normally distributed population.
     */
    MEAN {
        @Override
        Interval create(double mean, double variance, long n, double alpha) {
            final double c = TDistribution.of(n - 1).inverseSurvivalProbability(alpha * 0.5);
            final double distance = c * Math.sqrt(variance / n);
            return new BaseInterval(mean - distance, mean + distance);
        }
    },
    /**
     * Create a confidence interval for the true variance of an unknown normally distributed population.
     */
    VARIANCE {
        @Override
        Interval create(double mean, double variance, long n, double alpha) {
            final ChiSquaredDistribution d = ChiSquaredDistribution.of(n - 1);
            final double f = variance * (n - 1.0);
            final double lower = f / d.inverseSurvivalProbability(alpha * 0.5);
            final double upper = f / d.inverseCumulativeProbability(alpha * 0.5);
            return new BaseInterval(lower, upper);
        }
    };

    /**
     * Create a confidence interval from an independent sample from an unknown normally
     * distributed population with the given error rate.
     *
     * <p>The error rate {@code alpha} is related to the confidence level that the
     * interval contains the true value as
     * {@code alpha = 1 - confidence}, where {@code confidence} is the confidence level
     * in {@code [0, 1]}. For example a 95% confidence level is an {@code alpha} of 0.05.
     *
     * <p>The unbiased variance is the sum of the squared deviations from the mean divided
     * by {@code n - 1}.
     *
     * @param mean Sample mean.
     * @param variance Unbiased sample variance.
     * @param n Sample size.
     * @param alpha Desired error rate that the true value falls
     * <em>outside</em> the returned interval.
     * @return Confidence interval containing the target with error rate {@code alpha}
     * @throws IllegalArgumentException if {@code n <= 1}, or if {@code alpha} is not in
     * the open interval {@code (0, 1)}.
     */
    public Interval fromErrorRate(double mean, double variance, long n, double alpha) {
        if (n <= 1) {
            throw new IllegalArgumentException("Sample size is not above one: " + n);
        }
        ArgumentUtils.checkErrorRate(alpha);
        return create(mean, variance, n, alpha);
    }

    /**
     * Create a confidence interval from an independent sample from an unknown normally
     * distributed population with the given error rate.
     *
     * @param mean Sample mean.
     * @param variance Unbiased sample variance.
     * @param n Sample size.
     * @param alpha Desired error rate.
     * @return Confidence interval
     */
    abstract Interval create(double mean, double variance, long n, double alpha);
}
