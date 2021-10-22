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

    /** No instances. */
    private ExtendedPrecision() {}

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
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
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
}
