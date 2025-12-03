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

import org.apache.commons.numbers.gamma.LogBeta;
import org.apache.commons.numbers.gamma.RegularizedBeta;

/**
 * Implementation of the F-distribution.
 *
 * <p>The probability density function of \( X \) is:
 *
 * <p>\[ \begin{aligned}
 *       f(x; n, m) &amp;= \frac{1}{\operatorname{B}\left(\frac{n}{2},\frac{m}{2}\right)} \left(\frac{n}{m}\right)^{n/2} x^{n/2 - 1} \left(1+\frac{n}{m} \, x \right)^{-(n+m)/2} \\
 *                  &amp;= \frac{n^{\frac n 2} m^{\frac m 2} x^{\frac{n}{2}-1} }{ (nx+m)^{\frac{(n+m)}{2}} \operatorname{B}\left(\frac{n}{2},\frac{m}{2}\right)}   \end{aligned} \]
 *
 * <p>for \( n, m &gt; 0 \) the degrees of freedom, \( \operatorname{B}(a, b) \) is the beta function,
 * and \( x \in [0, \infty) \).
 *
 * @see <a href="https://en.wikipedia.org/wiki/F-distribution">F-distribution (Wikipedia)</a>
 * @see <a href="https://mathworld.wolfram.com/F-Distribution.html">F-distribution (MathWorld)</a>
 */
public final class FDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = 0;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** The minimum degrees of freedom for the denominator when computing the mean. */
    private static final double MIN_DENOMINATOR_DF_FOR_MEAN = 2.0;
    /** The minimum degrees of freedom for the denominator when computing the variance. */
    private static final double MIN_DENOMINATOR_DF_FOR_VARIANCE = 4.0;

    /** The numerator degrees of freedom. */
    private final double numeratorDegreesOfFreedom;
    /** The denominator degrees of freedom. */
    private final double denominatorDegreesOfFreedom;
    /** n/2 * log(n) + m/2 * log(m) with n = numerator DF and m = denominator DF. */
    private final double nHalfLogNmHalfLogM;
    /** LogBeta(n/2, n/2) with n = numerator DF. */
    private final double logBetaNhalfMhalf;
    /** Cached value for inverse probability function. */
    private final double mean;
    /** Cached value for inverse probability function. */
    private final double variance;

    /**
     * @param numeratorDegreesOfFreedom Numerator degrees of freedom.
     * @param denominatorDegreesOfFreedom Denominator degrees of freedom.
     */
    private FDistribution(double numeratorDegreesOfFreedom,
                          double denominatorDegreesOfFreedom) {
        this.numeratorDegreesOfFreedom = numeratorDegreesOfFreedom;
        this.denominatorDegreesOfFreedom = denominatorDegreesOfFreedom;
        final double nhalf = numeratorDegreesOfFreedom / 2;
        final double mhalf = denominatorDegreesOfFreedom / 2;
        nHalfLogNmHalfLogM = nhalf * Math.log(numeratorDegreesOfFreedom) +
                             mhalf * Math.log(denominatorDegreesOfFreedom);
        logBetaNhalfMhalf = LogBeta.value(nhalf, mhalf);

        if (denominatorDegreesOfFreedom > MIN_DENOMINATOR_DF_FOR_MEAN) {
            mean = denominatorDegreesOfFreedom / (denominatorDegreesOfFreedom - 2);
        } else {
            mean = Double.NaN;
        }
        if (denominatorDegreesOfFreedom > MIN_DENOMINATOR_DF_FOR_VARIANCE) {
            final double denomDFMinusTwo = denominatorDegreesOfFreedom - 2;
            variance = (2 * (denominatorDegreesOfFreedom * denominatorDegreesOfFreedom) *
                            (numeratorDegreesOfFreedom + denominatorDegreesOfFreedom - 2)) /
                       (numeratorDegreesOfFreedom * (denomDFMinusTwo * denomDFMinusTwo) *
                            (denominatorDegreesOfFreedom - 4));
        } else {
            variance = Double.NaN;
        }
    }

    /**
     * Creates an F-distribution.
     *
     * @param numeratorDegreesOfFreedom Numerator degrees of freedom.
     * @param denominatorDegreesOfFreedom Denominator degrees of freedom.
     * @return the distribution
     * @throws IllegalArgumentException if {@code numeratorDegreesOfFreedom <= 0} or
     * {@code denominatorDegreesOfFreedom <= 0}.
     */
    public static FDistribution of(double numeratorDegreesOfFreedom,
                                   double denominatorDegreesOfFreedom) {
        if (numeratorDegreesOfFreedom <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            numeratorDegreesOfFreedom);
        }
        if (denominatorDegreesOfFreedom <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE,
                                            denominatorDegreesOfFreedom);
        }
        return new FDistribution(numeratorDegreesOfFreedom, denominatorDegreesOfFreedom);
    }

    /**
     * Gets the numerator degrees of freedom parameter of this distribution.
     *
     * @return the numerator degrees of freedom.
     */
    public double getNumeratorDegreesOfFreedom() {
        return numeratorDegreesOfFreedom;
    }

    /**
     * Gets the denominator degrees of freedom parameter of this distribution.
     *
     * @return the denominator degrees of freedom.
     */
    public double getDenominatorDegreesOfFreedom() {
        return denominatorDegreesOfFreedom;
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code df1 < 2}: Infinity
     * <li>{@code df1 == 2}: 1
     * <li>{@code df1 > 2}: 0
     * </ul>
     * <p>Where {@code df1} is the numerator degrees of freedom.
     */
    @Override
    public double density(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            // Special case x=0:
            // PDF reduces to the term x^(df1 / 2 - 1) multiplied by a scaling factor
            if (x == SUPPORT_LO && numeratorDegreesOfFreedom <= 2) {
                return numeratorDegreesOfFreedom == 2 ?
                    1 :
                    Double.POSITIVE_INFINITY;
            }
            return 0;
        }
        return computeDensity(x, false);
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code df1 < 2}: Infinity
     * <li>{@code df1 == 2}: 0
     * <li>{@code df1 > 2}: -Infinity
     * </ul>
     * <p>Where {@code df1} is the numerator degrees of freedom.
     */
    @Override
    public double logDensity(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            // Special case x=0:
            // PDF reduces to the term x^(df1 / 2 - 1) multiplied by a scaling factor
            if (x == SUPPORT_LO && numeratorDegreesOfFreedom <= 2) {
                return numeratorDegreesOfFreedom == 2 ?
                    0 :
                    Double.POSITIVE_INFINITY;
            }
            return Double.NEGATIVE_INFINITY;
        }
        return computeDensity(x, true);
    }

    /**
     * Compute the density at point x. Assumes x is within the support bound.
     *
     * @param x Value
     * @param log true to compute the log density
     * @return pdf(x) or logpdf(x)
     */
    private double computeDensity(double x, boolean log) {
        // The log computation may suffer cancellation effects due to summation of large
        // opposing terms. Use it when the standard PDF result is not normal.

        // Keep the z argument to the regularized beta well away from 1 to avoid rounding error.
        // See: https://www.boost.org/doc/libs/1_78_0/libs/math/doc/html/math_toolkit/dist_ref/dists/f_dist.html

        final double n = numeratorDegreesOfFreedom;
        final double m = denominatorDegreesOfFreedom;
        final double nx = n * x;
        final double z = m + nx;
        final double y = n * m / (z * z);
        double p;
        if (nx > m) {
            p = y * RegularizedBeta.derivative(m / z,
                                               m / 2, n / 2);
        } else {
            p = y * RegularizedBeta.derivative(nx / z,
                                               n / 2, m / 2);
        }
        // RegularizedBeta.derivative can have intermediate overflow before normalisation
        // with small y. Check the result for a normal finite number.
        if (p <= Double.MAX_VALUE && p >= Double.MIN_NORMAL) {
            return log ? Math.log(p) : p;
        }
        // Fall back to the log computation
        p = nHalfLogNmHalfLogM + (n / 2 - 1) * Math.log(x) - logBetaNhalfMhalf -
                ((n + m) / 2) * Math.log(z);
        return log ? p : Math.exp(p);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x)  {
        if (x <= SUPPORT_LO) {
            return 0;
        } else if (x >= SUPPORT_HI) {
            return 1;
        }

        final double n = numeratorDegreesOfFreedom;
        final double m = denominatorDegreesOfFreedom;

        // Keep the z argument to the regularized beta well away from 1 to avoid rounding error.
        // See: https://www.boost.org/doc/libs/1_78_0/libs/math/doc/html/math_toolkit/dist_ref/dists/f_dist.html
        // Note the logic in the Boost documentation for pdf and cdf is contradictory.
        // This order will keep the argument far from 1.

        final double nx = n * x;
        if (nx > m) {
            return RegularizedBeta.complement(m / (m + nx),
                                              m / 2, n / 2);
        }
        return RegularizedBeta.value(nx / (m + nx),
                                     n / 2, m / 2);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x)  {
        if (x <= SUPPORT_LO) {
            return 1;
        } else if (x >= SUPPORT_HI) {
            return 0;
        }

        final double n = numeratorDegreesOfFreedom;
        final double m = denominatorDegreesOfFreedom;

        // Do the opposite of the CDF

        final double nx = n * x;
        if (nx > m) {
            return RegularizedBeta.value(m / (m + nx),
                                         m / 2, n / 2);
        }
        return RegularizedBeta.complement(nx / (m + nx),
                                          n / 2, m / 2);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For denominator degrees of freedom parameter \( m \), the mean is:
     *
     * <p>\[ \mathbb{E}[X] = \begin{cases}
     *       \frac{m}{m-2}    &amp; \text{for } m \gt 2 \\
     *       \text{undefined} &amp; \text{otherwise}
     *       \end{cases} \]
     *
     * @return the mean, or {@link Double#NaN NaN} if it is not defined.
     */
    @Override
    public double getMean() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For numerator degrees of freedom parameter \( n \) and denominator
     * degrees of freedom parameter \( m \), the variance is:
     *
     * <p>\[ \operatorname{var}[X] = \begin{cases}
     *       \frac{2m^2 (n+m-2)}{n (m-2)^2 (m-4)} &amp; \text{for } m \gt 4 \\
     *       \text{undefined}                     &amp; \text{otherwise}
     *       \end{cases} \]
     *
     * @return the variance, or {@link Double#NaN NaN} if it is not defined.
     */
    @Override
    public double getVariance() {
        return variance;
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
}
