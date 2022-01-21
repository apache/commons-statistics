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

import java.math.BigDecimal;

/**
 * Computes extended precision floating-point operations.
 *
 * <p>It is based on the 1971 paper
 * <a href="https://doi.org/10.1007/BF01397083">
 * Dekker (1971) A floating-point technique for extending the available precision</a>
 * Numer. Math. 18, 224-242.
 *
 * <p>Adapted from {@code org.apache.commons.numbers.core.ExtendedPrecion}.
 */
final class ExtendedPrecision {
    /** sqrt(2 pi). Computed to 64-digits. */
    private static final String SQRT_TWO_PI = "2.506628274631000502415765284811045253006986740609938316629923576";
    /**
     * The multiplier used to split the double value into high and low parts. From
     * Dekker (1971): "The constant should be chosen equal to 2^(p - p/2) + 1,
     * where p is the number of binary digits in the mantissa". Here p is 53
     * and the multiplier is {@code 2^27 + 1}.
     */
    private static final double MULTIPLIER = 1.0 + 0x1.0p27;
    /** Threshold for a big number that may overflow when squared. 2^500. */
    private static final double BIG = 0x1.0p500;
    /** Threshold for a small number that may underflow when squared. 2^-500. */
    private static final double SMALL = 0x1.0p-500;
    /** Scale up by 2^600. */
    private static final double SCALE_UP = 0x1.0p600;
    /** Scale down by 2^600. */
    private static final double SCALE_DOWN = 0x1.0p-600;
    /** sqrt(2 pi) as a double. */
    private static final double SQRT2PI;
    /** Upper bits of sqrt(2 pi). */
    private static final double SQRT2PI_H;
    /** Lower bits of sqrt(2 pi). */
    private static final double SQRT2PI_L;
    /** Round-off from sqrt(2 pi) as a double. */
    private static final double SQRT2PI_R;
    /** X-value where {@code exp(-0.5*x*x)} cannot increase accuracy using the round-off
     * from x squared. */
    private static final int EXP_M_HALF_XX_MIN_VALUE = 2;
    /** Approximate x-value where {@code exp(-0.5*x*x) == 0}. This is above
     * {@code -2 * ln(2^-1074)} due to rounding performed within the exp function. */
    private static final int EXP_M_HALF_XX_MAX_VALUE = 1491;

    static {
        // Initialise constants
        final BigDecimal sqrt2pi = new BigDecimal(SQRT_TWO_PI);

        // Use a 106-bit number as:
        // (SQRT2PI, SQRT2PI_R)
        SQRT2PI = sqrt2pi.doubleValue();
        SQRT2PI_R = sqrt2pi.subtract(new BigDecimal(SQRT2PI)).doubleValue();

        // Split the upper 53-bits for extended precision multiplication
        SQRT2PI_H = highPartUnscaled(SQRT2PI);
        SQRT2PI_L = SQRT2PI - SQRT2PI_H;
    }

    /** No instances. */
    private ExtendedPrecision() {}

    /**
     * Multiply the term by sqrt(2 pi).
     *
     * @param x Value (assumed to be positive)
     * @return x * sqrt(2 pi)
     */
    static double xsqrt2pi(double x) {
        // Note: Do not convert x to absolute for this use case
        if (x > BIG) {
            if (x == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY;
            }
            return computeXsqrt2pi(x * SCALE_DOWN) * SCALE_UP;
        } else if (x < SMALL) {
            // Note: Ignore possible zero for this use case
            return computeXsqrt2pi(x * SCALE_UP) * SCALE_DOWN;
        } else {
            return computeXsqrt2pi(x);
        }
    }

    /**
     * Compute {@code a * sqrt(2 * pi)}.
     *
     * @param a Value
     * @return the result
     */
    private static double computeXsqrt2pi(double a) {
        // Split the number
        final double ha = highPartUnscaled(a);
        final double la = a - ha;

        // Extended precision product with sqrt(2 * pi)
        final double x = a * SQRT2PI;
        final double xx = productLow(ha, la, SQRT2PI_H, SQRT2PI_L, x);

        // Add the term a multiplied by the round-off from sqrt(2 * pi)
        // result = a * (SQRT2PI + SQRT2PI_R)
        // Sum from small to high
        return a * SQRT2PI_R + xx + x;
    }

    /**
     * Compute {@code sqrt(2 * x * x)}.
     *
     * <p>The result is computed using a high precision computation of
     * {@code sqrt(2 * x * x)} avoiding underflow or overflow of {@code x}
     * squared.
     *
     * @param x Value (assumed to be positive)
     * @return {@code sqrt(2 * x * x)}
     */
    static double sqrt2xx(double x) {
        // Note: Do not convert x to absolute for this use case
        if (x > BIG) {
            if (x == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY;
            }
            return computeSqrt2aa(x * SCALE_DOWN) * SCALE_UP;
        } else if (x < SMALL) {
            // Note: Ignore possible zero for this use case
            return computeSqrt2aa(x * SCALE_UP) * SCALE_DOWN;
        } else {
            return computeSqrt2aa(x);
        }
    }

    /**
     * Compute {@code sqrt(2 * a * a)}.
     *
     * @param a Value
     * @return the result
     */
    private static double computeSqrt2aa(double a) {
        // Split the number
        final double ha = highPartUnscaled(a);
        final double la = a - ha;

        // Extended precision product
        final double x = 2 * a * a;
        final double xx = productLow(ha, la, 2 * ha, 2 * la, x);

        // Standard sqrt
        final double c = Math.sqrt(x);

        // Edge case.
        // Occurs if a has limited precision in the mantissa including
        // the special cases of 0 and 1.
        if (xx == 0) {
            return c;
        }

        // Dekker's double precision sqrt2 algorithm.
        // See Dekker, 1971, pp 242.
        final double hc = highPartUnscaled(c);
        final double lc = c - hc;
        final double u = c * c;
        final double uu = productLow(hc, lc, hc, lc, u);
        final double cc = (x - u - uu + xx) * 0.5 / c;

        // Extended precision result:
        // y = c + cc
        // yy = c - y + cc
        // Return only y
        return c + cc;
    }

    /**
     * Compute {@code exp(-0.5*x*x)} with high accuracy. This is performed using information in the
     * round-off from {@code x*x}.
     *
     * <p>This is accurate at large x to 1 ulp until exp(-0.5*x*x) is close to sub-normal. For very
     * small exp(-0.5*x*x) the adjustment is sub-normal and bits can be lost in the adjustment for a
     * max observed error of {@code < 2} ulp.
     *
     * <p>At small x the accuracy cannot be improved over using exp(-0.5*x*x). This occurs at
     * {@code x <= sqrt(2)}.
     *
     * @param x Value
     * @return exp(-0.5*x*x)
     * @see <a href="https://issues.apache.org/jira/browse/STATISTICS-52">STATISTICS-52</a>
     */
    static double expmhxx(double x) {
        final double z = x * x;
        if (z <= EXP_M_HALF_XX_MIN_VALUE) {
            return Math.exp(-0.5 * z);
        } else if (z >= EXP_M_HALF_XX_MAX_VALUE) {
            // exp(-745.5) == 0
            return 0;
        }
        // Split the number
        final double hx = highPartUnscaled(x);
        final double lx = x - hx;
        // Compute the round-off
        final double zz = squareLow(hx, lx, z);
        return expxx(-0.5 * z, -0.5 * zz);
    }

    /**
     * Compute {@code exp(a+b)} with high accuracy assuming {@code a+b = a}.
     *
     * <p>This is accurate at large positive a to 1 ulp. If a is negative and exp(a) is close to
     * sub-normal a bit of precision may be lost when adjusting result as the adjustment is sub-normal
     * (max observed error {@code < 2} ulp). For the use case of multiplication of a number less than
     * 1 by exp(-x*x), a = -x*x, the result will be sub-normal and the rounding error is lost.
     *
     * <p>At small |a| the accuracy cannot be improved over using exp(a) as the round-off is too small
     * to create terms that can adjust the standard result by more than 0.5 ulp. This occurs at
     * {@code |a| <= 1}.
     *
     * @param a High bits of a split number
     * @param b Low bits of a split number
     * @return exp(a+b)
     * @see <a href="https://issues.apache.org/jira/projects/NUMBERS/issues/NUMBERS-177">
     * Numbers-177: Accurate scaling by exp(z*z)</a>
     */
    private static double expxx(double a, double b) {
        // exp(a+b) = exp(a) * exp(b)
        // = exp(a) * (exp(b) - 1) + exp(a)
        // Assuming:
        // 1. -746 < a < 710 for no under/overflow of exp(a)
        // 2. a+b = a
        // As b -> 0 then exp(b) -> 1; expm1(b) -> b
        // The round-off b is limited to ~ 0.5 * ulp(746) ~ 5.68e-14
        // and we can use an approximation for expm1 (x/1! + x^2/2! + ...)
        // The second term is required for the expm1 result but the
        // bits are not significant to change the product with exp(a)

        final double ea = Math.exp(a);
        // b ~ expm1(b)
        return ea * b + ea;
    }

    /**
     * Implement Dekker's method to split a value into two parts. Multiplying by (2^s + 1) creates
     * a big value from which to derive the two split parts.
     * <pre>
     * c = (2^s + 1) * a
     * a_big = c - a
     * a_hi = c - a_big
     * a_lo = a - a_hi
     * a = a_hi + a_lo
     * </pre>
     *
     * <p>The multiplicand allows a p-bit value to be split into
     * (p-s)-bit value {@code a_hi} and a non-overlapping (s-1)-bit value {@code a_lo}.
     * Combined they have (p-1) bits of significand but the sign bit of {@code a_lo}
     * contains a bit of information. The constant is chosen so that s is ceil(p/2) where
     * the precision p for a double is 53-bits (1-bit of the mantissa is assumed to be
     * 1 for a non sub-normal number) and s is 27.
     *
     * <p>This conversion does not use scaling and the result of overflow is NaN. Overflow
     * may occur when the exponent of the input value is above 996.
     *
     * <p>Splitting a NaN or infinite value will return NaN.
     *
     * @param value Value.
     * @return the high part of the value.
     * @see Math#getExponent(double)
     */
    private static double highPartUnscaled(double value) {
        final double c = MULTIPLIER * value;
        return c - (c - value);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y} using Dekker's mult12 algorithm. The standard
     * precision product {@code x*y} must be provided. The numbers {@code x} and {@code y}
     * should already be split into low and high parts.
     *
     * <p>Note: This uses the high part of the result {@code (z,zz)} as {@code x * y} and not
     * {@code hx * hy + hx * ty + tx * hy} as specified in Dekker's original paper.
     * See Shewchuk (1997) for working examples.
     *
     * @param hx High part of first factor.
     * @param lx Low part of first factor.
     * @param hy High part of second factor.
     * @param ly Low part of second factor.
     * @param xy Product of the factors.
     * @return <code>lx * ly - (((xy - hx * hy) - lx * hy) - hx * ly)</code>
     * @see <a href="https://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 18</a>
     */
    private static double productLow(double hx, double lx, double hy, double ly, double xy) {
        // Compute the multiply low part:
        // err1 = xy - hx * hy
        // err2 = err1 - lx * hy
        // err3 = err2 - hx * ly
        // low = lx * ly - err3
        return lx * ly - (((xy - hx * hy) - lx * hy) - hx * ly);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * square of {@code x} using Dekker's mult12 algorithm. The standard precision product
     * {@code x*x} must be provided. The number {@code x} should already be split into low
     * and high parts.
     *
     * <p>Note: This is a specialisation of
     * {@link #productLow(double, double, double, double, double)}.
     *
     * @param hx High part of factor.
     * @param lx Low part of factor.
     * @param xx Square of the factor.
     * @return <code>lx * lx - (((xx - hx * hx) - lx * hx) - hx * lx)</code>
     */
    private static double squareLow(double hx, double lx, double xx) {
        return lx * lx - ((xx - hx * hx) - 2 * lx * hx);
    }
}
