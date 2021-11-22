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

import org.apache.commons.numbers.gamma.LogBeta;
import org.apache.commons.numbers.gamma.RegularizedBeta;

/**
 * Implementation of the F-distribution.
 *
 * @see <a href="http://en.wikipedia.org/wiki/F-distribution">F-distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/F-Distribution.html">F-distribution (MathWorld)</a>
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
    /** n/2 * log(n) with n = numerator DF. */
    private final double nhalfLogn;
    /** m/2 * log(m) with m = denominator DF. */
    private final double mhalfLogm;
    /** LogBeta(n/2, n/2) with n = numerator DF. */
    private final double logBetaNhalfNhalf;

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
        nhalfLogn = nhalf * Math.log(numeratorDegreesOfFreedom);
        mhalfLogm = mhalf * Math.log(denominatorDegreesOfFreedom);
        logBetaNhalfNhalf = LogBeta.value(nhalf, mhalf);
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
     * Access the numerator degrees of freedom.
     *
     * @return the numerator degrees of freedom.
     */
    public double getNumeratorDegreesOfFreedom() {
        return numeratorDegreesOfFreedom;
    }

    /**
     * Access the denominator degrees of freedom.
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
        return Math.exp(logDensity(x));
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

        final double nhalf = numeratorDegreesOfFreedom / 2;
        final double mhalf = denominatorDegreesOfFreedom / 2;
        final double logx = Math.log(x);
        final double lognxm = Math.log(numeratorDegreesOfFreedom * x +
                denominatorDegreesOfFreedom);
        return nhalfLogn + nhalf * logx - logx +
               mhalfLogm - nhalf * lognxm - mhalf * lognxm -
               logBetaNhalfNhalf;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The implementation of this method is based on
     * <ul>
     *  <li>
     *   <a href="http://mathworld.wolfram.com/F-Distribution.html">
     *   F-Distribution</a>, equation (4).
     *  </li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(double x)  {
        if (x <= SUPPORT_LO) {
            return 0;
        } else if (x >= SUPPORT_HI) {
            return 1;
        }

        final double n = numeratorDegreesOfFreedom;
        final double m = denominatorDegreesOfFreedom;

        return RegularizedBeta.value((n * x) / (m + n * x),
                                     0.5 * n,
                                     0.5 * m);
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

        // Comute the complement of the regularized beta function
        // with direct computation of 1 - z:
        // 1 - I(z, a, b) = I(1 - z, b, a)
        return RegularizedBeta.value(m / (m + n * x),
                                     0.5 * m,
                                     0.5 * n);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For denominator degrees of freedom parameter {@code b}, the mean is
     * <ul>
     *  <li>if {@code b > 2} then {@code b / (b - 2)},</li>
     *  <li>else undefined ({@code Double.NaN}).
     * </ul>
     */
    @Override
    public double getMean() {
        final double denominatorDF = getDenominatorDegreesOfFreedom();

        if (denominatorDF > MIN_DENOMINATOR_DF_FOR_MEAN) {
            return denominatorDF / (denominatorDF - 2);
        }

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For numerator degrees of freedom parameter {@code a} and denominator
     * degrees of freedom parameter {@code b}, the variance is
     * <ul>
     *  <li>
     *    if {@code b > 4} then
     *    {@code [2 * b^2 * (a + b - 2)] / [a * (b - 2)^2 * (b - 4)]},
     *  </li>
     *  <li>else undefined ({@code Double.NaN}).
     * </ul>
     */
    @Override
    public double getVariance() {
        final double denominatorDF = getDenominatorDegreesOfFreedom();

        if (denominatorDF > MIN_DENOMINATOR_DF_FOR_VARIANCE) {
            final double numeratorDF = getNumeratorDegreesOfFreedom();
            final double denomDFMinusTwo = denominatorDF - 2;

            return (2 * (denominatorDF * denominatorDF) * (numeratorDF + denominatorDF - 2)) /
                   (numeratorDF * (denomDFMinusTwo * denomDFMinusTwo) * (denominatorDF - 4));
        }

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0 no matter the parameters.
     *
     * @return lower bound of the support (always 0)
     */
    @Override
    public double getSupportLowerBound() {
        return SUPPORT_LO;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The upper bound of the support is always positive infinity
     * no matter the parameters.
     *
     * @return upper bound of the support (always Double.POSITIVE_INFINITY)
     */
    @Override
    public double getSupportUpperBound() {
        return SUPPORT_HI;
    }
}
