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
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Log-Cauchy_distribution">Log Cauchy distribution</a>.
 */
public class LogCauchyDistribution extends AbstractContinuousDistribution {
    /** The location of this distribution. */
    private final double location;
    /** The scale of this distribution. */
    private final double scale;

    /**
     * Creates a distribution.
     *
     * @param location Median for this distribution.
     * @param scale Scale parameter for this distribution.
     * @throws IllegalArgumentException if {@code scale <= 0}.
     */
    public LogCauchyDistribution(double location,
                              double scale) {
        if (scale <= 0) {
            throw new DistributionException(DistributionException.NEGATIVE, scale);
        }
        this.scale = scale;
        this.location = location;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= 0) {
            return 0;
        }
        return 0.5 + (Math.atan((Math.log(x) - location) / scale) / Math.PI);
    }

    /**
     * Access the median.
     *
     * @return the median for this distribution.
     */
    public double getMedian() {
        return Math.pow (Math.E,location);
    }

    /**
     * Access the scale parameter.
     *
     * @return the scale parameter for this distribution.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Access the location parameter.
     *
     * @return the location parameter for this distribution.
     */
    public double getLocation() {
        return location;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x <= 0) {
            return 0;
        }
        final double dev = Math.log(x) - location;
        return (1 / Math.PI) * (1 / x) * (scale / (dev * dev + scale * scale));
    }

    /**
     * {@inheritDoc}
     *
     * The mean is always infinite no matter the parameters.
     *
     * @return mean (always Double.POSITIVE_INFINITY)
     */
    @Override
    public double getMean() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * The variance is always infinite no matter the parameters.
     *
     * @return variance (always Double.POSITIVE_INFINITY)
     */
    @Override
    public double getVariance() {
        return Double.POSITIVE_INFINITY;
    }

    /* The distribution is not defined for x less than or qual to zero */

    /**
     * {@inheritDoc}
     *
     * The lower bound of the support is zero no matter the parameters.
     * The lower bound is open ended for the Support
     *
     * @return lower bound of the support (always Double.NEGATIVE_INFINITY)
     */
    @Override
    public double getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * The upper bound of the support is always positive infinity no matter
     * the parameters.
     *
     * @return upper bound of the support (always Double.POSITIVE_INFINITY)
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
