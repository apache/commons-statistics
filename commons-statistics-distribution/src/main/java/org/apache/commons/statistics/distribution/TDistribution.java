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

import org.apache.commons.numbers.gamma.RegularizedBeta;
import org.apache.commons.numbers.gamma.LogGamma;

/**
 * Implementation of <a href='http://en.wikipedia.org/wiki/Student&apos;s_t-distribution'>Student's t-distribution</a>.
 */
public class TDistribution extends AbstractContinuousDistribution {
    /** The degrees of freedom. */
    private final double degreesOfFreedom;
    /** degreesOfFreedom / 2 */
    private final double dofOver2;
    /** Cached value. */
    private final double factor;

    /**
     * Creates a distribution.
     *
     * @param degreesOfFreedom Degrees of freedom.
     * @throws IllegalArgumentException if {@code degreesOfFreedom <= 0}
     */
    public TDistribution(double degreesOfFreedom) {
        if (degreesOfFreedom <= 0) {
            throw new DistributionException(DistributionException.NEGATIVE,
                                            degreesOfFreedom);
        }
        this.degreesOfFreedom = degreesOfFreedom;

        dofOver2 = 0.5 * degreesOfFreedom;
        factor = LogGamma.value(dofOver2 + 0.5) -
                 0.5 * (Math.log(Math.PI) + Math.log(degreesOfFreedom)) -
                 LogGamma.value(dofOver2);
    }

    /**
     * Access the degrees of freedom.
     *
     * @return the degrees of freedom.
     */
    public double getDegreesOfFreedom() {
        return degreesOfFreedom;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        return Math.exp(logDensity(x));
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        final double nPlus1Over2 = dofOver2 + 0.5;
        return factor - nPlus1Over2 * Math.log(1 + x * x / degreesOfFreedom);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        double ret;
        if (x == 0) {
            ret = 0.5;
        } else {
            final double t =
                RegularizedBeta.value(degreesOfFreedom / (degreesOfFreedom + (x * x)),
                                      dofOver2,
                                      0.5);
            if (x < 0) {
                ret = 0.5 * t;
            } else {
                ret = 1 - 0.5 * t;
            }
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     *
     * For degrees of freedom parameter {@code df}, the mean is
     * <ul>
     *  <li>if {@code df > 1} then {@code 0},</li>
     * <li>else undefined ({@code Double.NaN}).</li>
     * </ul>
     */
    @Override
    public double getMean() {
        if (degreesOfFreedom > 1) {
            return 0;
        }

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     *
     * For degrees of freedom parameter {@code df}, the variance is
     * <ul>
     *  <li>if {@code df > 2} then {@code df / (df - 2)},</li>
     *  <li>if {@code 1 < df <= 2} then positive infinity
     *  ({@code Double.POSITIVE_INFINITY}),</li>
     *  <li>else undefined ({@code Double.NaN}).</li>
     * </ul>
     */
    @Override
    public double getVariance() {
        if (degreesOfFreedom > 2) {
            return degreesOfFreedom / (degreesOfFreedom - 2);
        }

        if (degreesOfFreedom > 1 &&
            degreesOfFreedom <= 2) {
            return Double.POSITIVE_INFINITY;
        }

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     *
     * The lower bound of the support is always negative infinity no matter the
     * parameters.
     *
     * @return lower bound of the support (always
     * {@code Double.NEGATIVE_INFINITY})
     */
    @Override
    public double getSupportLowerBound() {
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * The upper bound of the support is always positive infinity no matter the
     * parameters.
     *
     * @return upper bound of the support (always
     * {@code Double.POSITIVE_INFINITY})
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * The support of this distribution is connected.
     *
     * @return {@code true}
     */
    @Override
    public boolean isSupportConnected() {
        return true;
    }
}
