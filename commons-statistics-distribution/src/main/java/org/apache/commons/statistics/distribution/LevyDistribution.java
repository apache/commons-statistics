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

import org.apache.commons.numbers.gamma.Erfc;
import org.apache.commons.numbers.gamma.InverseErfc;

/**
 * This class implements the <a href="http://en.wikipedia.org/wiki/L%C3%A9vy_distribution">
 * L&eacute;vy distribution</a>.
 */
public class LevyDistribution extends AbstractContinuousDistribution {
    /** Location parameter. */
    private final double mu;
    /** Scale parameter. */
    private final double c;
    /** Half of c (for calculations). */
    private final double halfC;

    /**
     * Creates a distribution.
     *
     * @param mu location
     * @param c scale parameter
     */
    public LevyDistribution(final double mu,
                            final double c) {
        this.mu = mu;
        this.c = c;
        this.halfC = 0.5 * c;
    }

    /** {@inheritDoc}
    * <p>
    * From Wikipedia: The probability density function of the L&eacute;vy distribution
    * over the domain is
    * </p>
    * <div style="white-space: pre"><code>
    * f(x; &mu;, c) = &radic;(c / 2&pi;) * e<sup>-c / 2 (x - &mu;)</sup> / (x - &mu;)<sup>3/2</sup>
    * </code></div>
    * <p>
    * For this distribution, {@code X}, this method returns {@code P(X < x)}.
    * If {@code x} is less than location parameter &mu;, {@code Double.NaN} is
    * returned, as in these cases the distribution is not defined.
    * </p>
    */
    @Override
    public double density(final double x) {
        if (x < mu) {
            return Double.NaN;
        }

        final double delta = x - mu;
        final double f = halfC / delta;
        return Math.sqrt(f / Math.PI) * Math.exp(-f) / delta;
    }

    /** {@inheritDoc}
     *
     * See documentation of {@link #density(double)} for computation details.
     */
    @Override
    public double logDensity(double x) {
        if (x < mu) {
            return Double.NaN;
        }

        final double delta = x - mu;
        final double f     = halfC / delta;
        return 0.5 * Math.log(f / Math.PI) - f - Math.log(delta);
    }

    /** {@inheritDoc}
     * <p>
     * From Wikipedia: the cumulative distribution function is
     * </p>
     * <pre>
     * f(x; u, c) = erfc (&radic; (c / 2 (x - u )))
     * </pre>
     */
    @Override
    public double cumulativeProbability(final double x) {
        if (x < mu) {
            return Double.NaN;
        }
        return Erfc.value(Math.sqrt(halfC / (x - mu)));
    }

    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(final double p) {
        if (p < 0 ||
            p > 1) {
            throw new DistributionException(DistributionException.OUT_OF_RANGE, p, 0, 1);
        }
        final double t = InverseErfc.value(p);
        return mu + halfC / (t * t);
    }

    /**
     * Gets the scale parameter of the distribution.
     *
     * @return scale parameter of the distribution
     */
    public double getScale() {
        return c;
    }

    /**
     * Gets the location parameter of the distribution.
     *
     * @return location parameter of the distribution
     */
    public double getLocation() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public double getMean() {
        return Double.POSITIVE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public double getVariance() {
        return Double.POSITIVE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public double getSupportLowerBound() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupportConnected() {
        return true;
    }
}
