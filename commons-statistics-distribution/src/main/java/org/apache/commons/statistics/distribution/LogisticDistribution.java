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
package org.apache.commons.statistics.distribution;

/**
 * Implementation of the logistic distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; \mu, s) = \frac{e^{-(x-\mu)/s}} {s\left(1+e^{-(x-\mu)/s}\right)^2} \]
 *
 * <p>for \( \mu \) the location,
 * \( s &gt; 0 \) the scale, and
 * \( x \in (-\infty, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Logistic_distribution">Logistic distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/LogisticDistribution.html">Logistic distribution (MathWorld)</a>
 */
public final class LogisticDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = Double.NEGATIVE_INFINITY;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** &pi;<sup>2</sup>/3. https://oeis.org/A195055. */
    private static final double PI_SQUARED_OVER_THREE = 3.289868133696452872944830;
    /** Location parameter. */
    private final double mu;
    /** Scale parameter. */
    private final double scale;
    /** Logarithm of "scale". */
    private final double logScale;

    /**
     * @param mu Location parameter.
     * @param scale Scale parameter (must be positive).
     */
    private LogisticDistribution(double mu,
                                 double scale) {
        this.mu = mu;
        this.scale = scale;
        this.logScale = Math.log(scale);
    }

    /**
     * Creates a logistic distribution.
     *
     * @param mu Location parameter.
     * @param scale Scale parameter (must be positive).
     * @return the distribution
     * @throws IllegalArgumentException if {@code scale <= 0}.
     */
    public static LogisticDistribution of(double mu,
                                          double scale) {
        if (scale <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            scale);
        }
        return new LogisticDistribution(mu, scale);
    }

    /**
     * Gets the location parameter of this distribution.
     *
     * @return the location parameter.
     */
    public double getLocation() {
        return mu;
    }

    /**
     * Gets the scale parameter of this distribution.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return scale;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            return 0;
        }

        // Ensure symmetry around location by using the absolute.
        // This also ensures exp(z) is between 1 and 0 and avoids
        // overflow for large negative values of (x - mu).
        // Exploits the reciprocal relation: exp(-x) == 1 / exp(x)
        //     exp(-z)                   1                exp(z)     exp(z)
        // --------------- = -------------------------- * ------ = --------------
        // (1 + exp(-z))^2    exp(z) (1 + 1 / exp(z))^2   exp(z)   (1 + exp(z))^2
        final double z = -Math.abs(x - mu) / scale;
        final double v = Math.exp(z);
        return v / ((1 + v) * (1 + v)) / scale;
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            return Double.NEGATIVE_INFINITY;
        }

        // Ensure symmetry around location by using the absolute
        final double z = -Math.abs(x - mu) / scale;
        final double v = Math.exp(z);
        return z - 2 * Math.log1p(v) - logScale;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        final double z = (x - mu) / scale;
        return 1 / (1 + Math.exp(-z));
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        final double z = (x - mu) / scale;
        return 1 / (1 + Math.exp(z));
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return SUPPORT_LO;
        } else if (p == 1) {
            return SUPPORT_HI;
        } else {
            return scale * Math.log(p / (1 - p)) + mu;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return SUPPORT_LO;
        } else if (p == 0) {
            return SUPPORT_HI;
        } else {
            return scale * -Math.log(p / (1 - p)) + mu;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mean is equal to the {@linkplain #getLocation() location}.
     */
    @Override
    public double getMean() {
        return getLocation();
    }

    /**
     * {@inheritDoc}
     *
     * <p>For scale parameter \( s \), the variance is:
     *
     * <p>\[ \frac{s^2 \pi^2}{3} \]
     */
    @Override
    public double getVariance() {
        return scale * scale * PI_SQUARED_OVER_THREE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always negative infinity.
     *
     * @return {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
     */
    @Override
    public double getSupportLowerBound() {
        return SUPPORT_LO;
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
        return SUPPORT_HI;
    }

    /** {@inheritDoc} */
    @Override
    double getMedian() {
        // Overridden for the probability(double, double) method.
        // This is intentionally not a public method.
        return mu;
    }
}
