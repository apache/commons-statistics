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

import org.apache.commons.numbers.gamma.Gamma;
import org.apache.commons.numbers.gamma.LogGamma;
import org.apache.commons.numbers.gamma.RegularizedGamma;

/**
 * This class implements the <a href="http://en.wikipedia.org/wiki/Nakagami_distribution">Nakagami distribution</a>.
 */
public final class NakagamiDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = 0;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** Natural logarithm of 2. */
    private static final double LN_2 = 0.6931471805599453094172321;

    /** The shape parameter. */
    private final double mu;
    /** The scale parameter. */
    private final double omega;
    /** Density prefactor. */
    private final double densityPrefactor;
    /** Log density prefactor. */
    private final double logDensityPrefactor;

    /**
     * @param mu Shape parameter.
     * @param omega Scale parameter (must be positive). Controls the spread of the distribution.
     */
    private NakagamiDistribution(double mu,
                                 double omega) {
        this.mu = mu;
        this.omega = omega;
        densityPrefactor = 2.0 * Math.pow(mu, mu) / (Gamma.value(mu) * Math.pow(omega, mu));
        logDensityPrefactor = LN_2 + Math.log(mu) * mu - LogGamma.value(mu) - Math.log(omega) * mu;
    }

    /**
     * Creates a Nakagami distribution.
     *
     * @param mu Shape parameter.
     * @param omega Scale parameter (must be positive). Controls the spread of the distribution.
     * @return the distribution
     * @throws IllegalArgumentException  if {@code mu < 0.5} or if
     * {@code omega <= 0}.
     */
    public static NakagamiDistribution of(double mu,
                                          double omega) {
        if (mu <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, mu);
        }
        if (omega <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, omega);
        }
        return new NakagamiDistribution(mu, omega);
    }

    /**
     * Access the shape parameter, {@code mu}.
     *
     * @return the shape parameter.
     */
    public double getShape() {
        return mu;
    }

    /**
     * Access the scale parameter, {@code omega}.
     *
     * @return the scale parameter.
     */
    public double getScale() {
        return omega;
    }

    /** {@inheritDoc} */
    @Override
    public double density(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            return 0;
        }

        return densityPrefactor * Math.pow(x, 2 * mu - 1) * Math.exp(-mu * x * x / omega);
    }

    /** {@inheritDoc} */
    @Override
    public double logDensity(double x) {
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            return Double.NEGATIVE_INFINITY;
        }

        return logDensityPrefactor + Math.log(x) * (2 * mu - 1) - (mu * x * x / omega);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 0;
        } else if (x >= SUPPORT_HI) {
            return 1;
        }

        return RegularizedGamma.P.value(mu, mu * x * x / omega);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 1;
        } else if (x >= SUPPORT_HI) {
            return 0;
        }

        return RegularizedGamma.Q.value(mu, mu * x * x / omega);
    }

    /** {@inheritDoc} */
    @Override
    public double getMean() {
        return Gamma.value(mu + 0.5) / Gamma.value(mu) * Math.sqrt(omega / mu);
    }

    /** {@inheritDoc} */
    @Override
    public double getVariance() {
        final double v = Gamma.value(mu + 0.5) / Gamma.value(mu);
        return omega * (1 - 1 / mu * v * v);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lower bound of the support is always 0 no matter parameters.
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
     * @return upper bound of the support (always
     * {@code Double.POSITIVE_INFINITY})
     */
    @Override
    public double getSupportUpperBound() {
        return SUPPORT_HI;
    }
}
