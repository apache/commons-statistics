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
 * Implementation of the triangular distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ f(x; a, b, c) = \begin{cases}
 *       \frac{2(x-a)}{(b-a)(c-a)} &amp; \text{for } a \le x \lt c  \\
 *       \frac{2}{b-a}             &amp; \text{for } x = c \\
 *       \frac{2(b-x)}{(b-a)(b-c)} &amp; \text{for } c \lt x \le b \\
 *       \end{cases} \]
 *
 * <p>for \( -\infty \lt a \le c \le b \lt \infty \) and
 * \( x \in [a, b] \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Triangular_distribution">Triangular distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/TriangularDistribution.html">Triangular distribution (MathWorld)</a>
 */
public final class TriangularDistribution extends AbstractContinuousDistribution {
    /** Lower limit of this distribution (inclusive). */
    private final double a;
    /** Upper limit of this distribution (inclusive). */
    private final double b;
    /** Mode of this distribution. */
    private final double c;
    /** Cached value ((b - a) * (c - a). */
    private final double divisor1;
    /** Cached value ((b - a) * (b - c)). */
    private final double divisor2;
    /** Cumulative probability at the mode. */
    private final double cdfMode;
    /** Survival probability at the mode. */
    private final double sfMode;

    /**
     * @param a Lower limit of this distribution (inclusive).
     * @param c Mode of this distribution.
     * @param b Upper limit of this distribution (inclusive).
     */
    private TriangularDistribution(double a,
                                   double c,
                                   double b) {
        this.a = a;
        this.c = c;
        this.b = b;
        divisor1 = (b - a) * (c - a);
        divisor2 = (b - a) * (b - c);
        cdfMode = (c - a) / (b - a);
        sfMode = (b - c) / (b - a);
    }

    /**
     * Creates a triangular distribution.
     *
     * @param a Lower limit of this distribution (inclusive).
     * @param c Mode of this distribution.
     * @param b Upper limit of this distribution (inclusive).
     * @return the distribution
     * @throws IllegalArgumentException if {@code a >= b}, if {@code c > b} or if
     * {@code c < a}.
     */
    public static TriangularDistribution of(double a,
                                            double c,
                                            double b) {
        if (a >= b) {
            throw new DistributionException(DistributionException.INVALID_RANGE_LOW_GTE_HIGH,
                                            a, b);
        }
        if (c < a) {
            throw new DistributionException(DistributionException.TOO_SMALL,
                                            c, a);
        }
        if (c > b) {
            throw new DistributionException(DistributionException.TOO_LARGE,
                                            c, b);
        }
        return new TriangularDistribution(a, c, b);
    }

    /**
     * Gets the mode.
     *
     * @return the mode of the distribution.
     */
    public double getMode() {
        return c;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower limit {@code a}, upper limit {@code b} and mode {@code c}, the
     * PDF is given by
     * <ul>
     * <li>{@code 2 * (x - a) / [(b - a) * (c - a)]} if {@code a <= x < c},</li>
     * <li>{@code 2 / (b - a)} if {@code x = c},</li>
     * <li>{@code 2 * (b - x) / [(b - a) * (b - c)]} if {@code c < x <= b},</li>
     * <li>{@code 0} otherwise.
     * </ul>
     */
    @Override
    public double density(double x) {
        if (x < a) {
            return 0;
        }
        if (x < c) {
            final double divident = 2 * (x - a);
            return divident / divisor1;
        }
        if (x == c) {
            return 2 / (b - a);
        }
        if (x <= b) {
            final double divident = 2 * (b - x);
            return divident / divisor2;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower limit {@code a}, upper limit {@code b} and mode {@code c}, the
     * CDF is given by
     * <ul>
     * <li>{@code 0} if {@code x < a},</li>
     * <li>{@code (x - a)^2 / [(b - a) * (c - a)]} if {@code a <= x < c},</li>
     * <li>{@code (c - a) / (b - a)} if {@code x = c},</li>
     * <li>{@code 1 - (b - x)^2 / [(b - a) * (b - c)]} if {@code c < x <= b},</li>
     * <li>{@code 1} if {@code x > b}.</li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(double x)  {
        if (x <= a) {
            return 0;
        }
        if (x < c) {
            final double divident = (x - a) * (x - a);
            return divident / divisor1;
        }
        if (x == c) {
            return cdfMode;
        }
        if (x < b) {
            final double divident = (b - x) * (b - x);
            return 1 - (divident / divisor2);
        }
        return 1;
    }


    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x)  {
        // By symmetry:
        if (x <= a) {
            return 1;
        }
        if (x < c) {
            final double divident = (x - a) * (x - a);
            return 1 - (divident / divisor1);
        }
        if (x == c) {
            return sfMode;
        }
        if (x < b) {
            final double divident = (b - x) * (b - x);
            return divident / divisor2;
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(double p) {
        ArgumentUtils.checkProbability(p);
        if (p == 0) {
            return a;
        }
        if (p == 1) {
            return b;
        }
        if (p < cdfMode) {
            return a + Math.sqrt(p * divisor1);
        }
        return b - Math.sqrt((1 - p) * divisor2);
    }

    /** {@inheritDoc} */
    @Override
    public double inverseSurvivalProbability(double p) {
        // By symmetry:
        ArgumentUtils.checkProbability(p);
        if (p == 1) {
            return a;
        }
        if (p == 0) {
            return b;
        }
        if (p >= sfMode) {
            return a + Math.sqrt((1 - p) * divisor1);
        }
        return b - Math.sqrt(p * divisor2);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower limit {@code a}, upper limit {@code b}, and mode {@code c},
     * the mean is {@code (a + b + c) / 3}.
     */
    @Override
    public double getMean() {
        return (a + b + c) / 3;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For lower limit {@code a}, upper limit {@code b}, and mode {@code c},
     * the variance is {@code (a^2 + b^2 + c^2 - a * b - a * c - b * c) / 18}.
     */
    @Override
    public double getVariance() {
        return (a * a + b * b + c * c - a * b - a * c - b * c) / 18;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is equal to the lower limit parameter
     * {@code a} of the distribution.
     *
     * @return lower bound of the support
     */
    @Override
    public double getSupportLowerBound() {
        return a;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is equal to the upper limit parameter
     * {@code b} of the distribution.
     *
     * @return upper bound of the support
     */
    @Override
    public double getSupportUpperBound() {
        return b;
    }
}
