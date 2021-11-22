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

import org.apache.commons.numbers.gamma.LanczosApproximation;
import org.apache.commons.numbers.gamma.RegularizedGamma;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.AhrensDieterMarsagliaTsangGammaSampler;

/**
 * Implementation of the <a href="http://en.wikipedia.org/wiki/Gamma_distribution">gamma distribution</a>.
 */
public final class GammaDistribution extends AbstractContinuousDistribution {
    /** Support lower bound. */
    private static final double SUPPORT_LO = 0;
    /** Support upper bound. */
    private static final double SUPPORT_HI = Double.POSITIVE_INFINITY;
    /** Lanczos constant. */
    private static final double LANCZOS_G = LanczosApproximation.g();
    /** The shape parameter. */
    private final double shape;
    /** The scale parameter. */
    private final double scale;
    /**
     * The constant value of {@code shape + g + 0.5}, where {@code g} is the
     * Lanczos constant {@link LanczosApproximation#g()}.
     */
    private final double shiftedShape;
    /**
     * The constant value of
     * {@code shape / scale * sqrt(e / (2 * pi * (shape + g + 0.5))) / L(shape)},
     * where {@code L(shape)} is the Lanczos approximation returned by
     * {@link LanczosApproximation#value(double)}. This prefactor is used in
     * {@link #density(double)}, when no overflow occurs with the natural
     * calculation.
     */
    private final double densityPrefactor1;
    /**
     * The constant value of
     * {@code log(shape / scale * sqrt(e / (2 * pi * (shape + g + 0.5))) / L(shape))},
     * where {@code L(shape)} is the Lanczos approximation returned by
     * {@link LanczosApproximation#value(double)}. This prefactor is used in
     * {@link #logDensity(double)}, when no overflow occurs with the natural
     * calculation.
     */
    private final double logDensityPrefactor1;
    /**
     * The constant value of
     * {@code shape * sqrt(e / (2 * pi * (shape + g + 0.5))) / L(shape)},
     * where {@code L(shape)} is the Lanczos approximation returned by
     * {@link LanczosApproximation#value(double)}. This prefactor is used in
     * {@link #density(double)}, when overflow occurs with the natural
     * calculation.
     */
    private final double densityPrefactor2;
    /**
     * The constant value of
     * {@code log(shape * sqrt(e / (2 * pi * (shape + g + 0.5))) / L(shape))},
     * where {@code L(shape)} is the Lanczos approximation returned by
     * {@link LanczosApproximation#value(double)}. This prefactor is used in
     * {@link #logDensity(double)}, when overflow occurs with the natural
     * calculation.
     */
    private final double logDensityPrefactor2;
    /**
     * Lower bound on {@code y = x / scale} for the selection of the computation
     * method in {@link #density(double)}. For {@code y <= minY}, the natural
     * calculation overflows.
     */
    private final double minY;
    /**
     * Upper bound on {@code log(y)} ({@code y = x / scale}) for the selection
     * of the computation method in {@link #density(double)}. For
     * {@code log(y) >= maxLogY}, the natural calculation overflows.
     */
    private final double maxLogY;

    /**
     * @param shape Shape parameter.
     * @param scale Scale parameter.
     */
    private GammaDistribution(double shape,
                              double scale) {
        this.shape = shape;
        this.scale = scale;
        this.shiftedShape = shape + LANCZOS_G + 0.5;
        final double aux = Math.E / (2.0 * Math.PI * shiftedShape);
        this.densityPrefactor2 = shape * Math.sqrt(aux) / LanczosApproximation.value(shape);
        this.logDensityPrefactor2 = Math.log(shape) + 0.5 * Math.log(aux) -
            Math.log(LanczosApproximation.value(shape));
        this.densityPrefactor1 = this.densityPrefactor2 / scale *
            Math.pow(shiftedShape, -shape) *
            Math.exp(shape + LANCZOS_G);
        this.logDensityPrefactor1 = this.logDensityPrefactor2 - Math.log(scale) -
            Math.log(shiftedShape) * shape +
            shape + LANCZOS_G;
        this.minY = shape + LANCZOS_G - Math.log(Double.MAX_VALUE);
        this.maxLogY = Math.log(Double.MAX_VALUE) / (shape - 1.0);
    }

    /**
     * Creates a gamma distribution.
     *
     * @param shape Shape parameter.
     * @param scale Scale parameter.
     * @return the distribution
     * @throws IllegalArgumentException if {@code shape <= 0} or {@code scale <= 0}.
     */
    public static GammaDistribution of(double shape,
                                       double scale) {
        if (shape <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, shape);
        }
        if (scale <= 0) {
            throw new DistributionException(DistributionException.NOT_STRICTLY_POSITIVE, scale);
        }
        return new GammaDistribution(shape, scale);
    }

    /**
     * Returns the shape parameter of {@code this} distribution.
     *
     * @return the shape parameter
     */
    public double getShape() {
        return shape;
    }

    /**
     * Returns the scale parameter of {@code this} distribution.
     *
     * @return the scale parameter
     */
    public double getScale() {
        return scale;
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code shape < 1}: Infinity
     * <li>{@code shape == 1}: 1 / scale
     * <li>{@code shape > 1}: 0
     * </ul>
     */
    @Override
    public double density(double x) {
       /* The present method must return the value of
        *
        *     1          a-1     - x           1       x a     - x
        * ------------  x    exp(---)  =   ---------- (-)  exp(---)
        * Gamma(a) b^a            b        x Gamma(a)  b        b
        *
        * where a is the shape parameter, and b the scale parameter.
        * Substituting the Lanczos approximation of Gamma(a) leads to the
        * following expression of the density
        *
        * a              e            1         y      a
        * - sqrt(------------------) ---- (-----------)  exp(a - y + g),
        * x      2 pi (a + g + 0.5)  L(a)  a + g + 0.5
        *
        * where y = x / b. The above formula is the "natural" computation, which
        * is implemented when no overflow is likely to occur. If overflow occurs
        * with the natural computation, the following identity is used. It is
        * based on the BOOST library
        * http://www.boost.org/doc/libs/1_35_0/libs/math/doc/sf_and_dist/html/math_toolkit/special/sf_gamma/igamma.html
        * Formula (15) needs adaptations, which are detailed below.
        *
        *       y      a
        * (-----------)  exp(a - y + g)
        *  a + g + 0.5
        *                              y - a - g - 0.5    y (g + 0.5)
        *               = exp(a log1pm(---------------) - ----------- + g),
        *                                a + g + 0.5      a + g + 0.5
        *
        *  where log1pm(z) = log(1 + z) - z. Therefore, the value to be
        *  returned is
        *
        * a              e            1
        * - sqrt(------------------) ----
        * x      2 pi (a + g + 0.5)  L(a)
        *                              y - a - g - 0.5    y (g + 0.5)
        *               * exp(a log1pm(---------------) - ----------- + g).
        *                                a + g + 0.5      a + g + 0.5
        */
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            // Special case x=0
            if (x == SUPPORT_LO && shape <= 1) {
                return shape == 1 ?
                    1 / scale :
                    Double.POSITIVE_INFINITY;
            }
            return 0;
        }

        final double y = x / scale;
        if ((y <= minY) || (Math.log(y) >= maxLogY)) {
            /*
             * Overflow.
             */
            final double aux1 = (y - shiftedShape) / shiftedShape;
            final double aux2 = shape * (Math.log1p(aux1) - aux1);
            final double aux3 = -y * (LANCZOS_G + 0.5) / shiftedShape + LANCZOS_G + aux2;
            return densityPrefactor2 / x * Math.exp(aux3);
        }
        /*
         * Natural calculation.
         */
        return densityPrefactor1 * Math.exp(-y) * Math.pow(y, shape - 1);
    }

    /** {@inheritDoc}
     *
     * <p>Returns the limit when {@code x = 0}:
     * <ul>
     * <li>{@code shape < 1}: Infinity
     * <li>{@code shape == 1}: -log(scale)
     * <li>{@code shape > 1}: -Infinity
     * </ul>
     */
    @Override
    public double logDensity(double x) {
        /*
         * see the comment in {@link #density(double)} for computation details
         */
        if (x <= SUPPORT_LO ||
            x >= SUPPORT_HI) {
            // Special case x=0
            if (x == SUPPORT_LO && shape <= 1) {
                return shape == 1 ?
                    -Math.log(scale) :
                    Double.POSITIVE_INFINITY;
            }
            return Double.NEGATIVE_INFINITY;
        }

        final double y = x / scale;
        if ((y <= minY) || (Math.log(y) >= maxLogY)) {
            /*
             * Overflow.
             */
            final double aux1 = (y - shiftedShape) / shiftedShape;
            final double aux2 = shape * (Math.log1p(aux1) - aux1);
            final double aux3 = -y * (LANCZOS_G + 0.5) / shiftedShape + LANCZOS_G + aux2;
            return logDensityPrefactor2 - Math.log(x) + aux3;
        }
        /*
         * Natural calculation.
         */
        return logDensityPrefactor1 - y + Math.log(y) * (shape - 1);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The implementation of this method is based on:
     * <ul>
     *  <li>
     *   <a href="http://mathworld.wolfram.com/Chi-SquaredDistribution.html">
     *    Chi-Squared Distribution</a>, equation (9).
     *  </li>
     *  <li>Casella, G., &amp; Berger, R. (1990). <i>Statistical Inference</i>.
     *    Belmont, CA: Duxbury Press.
     *  </li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 0;
        } else if (x >= SUPPORT_HI) {
            return 1;
        }

        return RegularizedGamma.P.value(shape, x / scale);
    }

    /** {@inheritDoc} */
    @Override
    public double survivalProbability(double x) {
        if (x <= SUPPORT_LO) {
            return 1;
        } else if (x >= SUPPORT_HI) {
            return 0;
        }
        return RegularizedGamma.Q.value(shape, x / scale);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter {@code alpha} and scale parameter {@code beta}, the
     * mean is {@code alpha * beta}.
     */
    @Override
    public double getMean() {
        return shape * scale;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For shape parameter {@code alpha} and scale parameter {@code beta}, the
     * variance is {@code alpha * beta^2}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public double getVariance() {
        return shape * scale * scale;
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * Sampling algorithms:
     * <ul>
     *  <li>
     *   For {@code 0 < shape < 1}:
     *   <blockquote>
     *    Ahrens, J. H. and Dieter, U.,
     *    <i>Computer methods for sampling from gamma, beta, Poisson and binomial distributions,</i>
     *    Computing, 12, 223-246, 1974.
     *   </blockquote>
     *  </li>
     *  <li>
     *  For {@code shape >= 1}:
     *   <blockquote>
     *   Marsaglia and Tsang, <i>A Simple Method for Generating
     *   Gamma Variables.</i> ACM Transactions on Mathematical Software,
     *   Volume 26 Issue 3, September, 2000.
     *   </blockquote>
     *  </li>
     * </ul>
     */
    @Override
    public ContinuousDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Gamma distribution sampler.
        return AhrensDieterMarsagliaTsangGammaSampler.of(rng, shape, scale)::sample;
    }
}
