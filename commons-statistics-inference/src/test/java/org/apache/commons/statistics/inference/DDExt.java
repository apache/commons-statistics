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

package org.apache.commons.statistics.inference;

/**
 * Computes double-double floating-point operations.
 *
 * <p>This class contains extension methods to supplement the functionality in {@link DD}.
 * The methods are tested in {@link DDTest} to ensure validity and compare performance.
 *
 * @since 1.1
 */
final class DDExt {
    /** Threshold for large n where the Taylor series for (1+z)^n is not applicable. */
    private static final int LARGE_N = 100000000;
    /** Threshold for (x, xx)^n where x=0.5 and low-part will not be sub-normal.
     * Note x has an exponent of -1; xx of -54 (if normalised); the min normal exponent is -1022;
     * add 10-bits headroom in case xx is below epsilon * x: 1022 - 54 - 10. 0.5^958 = 4.1e-289. */
    private static final int SAFE_EXPONENT_F = 958;
    /** Threshold for (x, xx)^n where n ~ 2^31 and low-part will not be sub-normal.
     * x ~ exp(log(2^-958) / 2^31).
     * Note: floor(-958 * ln(2) / ln(nextDown(SAFE_F))) < 2^31. */
    private static final double SAFE_F = 0.9999996907846553;
    /** Threshold for (x, xx)^n where x=2 and high-part is finite.
     * For consistency we use 10-bits headroom down from max exponent 1023. 0.5^1013 = 8.78e304. */
    private static final int SAFE_EXPONENT_2F = 1013;
    /** Threshold for (x, xx)^n where n ~ 2^31 and high-part is finite.
     * x ~ exp(log(2^1013) / 2^31)
     * Note: floor(1013 * ln(2) / ln(nextUp(SAFE_2F))) < 2^31. */
    private static final double SAFE_2F = 1.0000003269678954;
    /** log(2) (20-digits). */
    private static final double LN2 = 0.69314718055994530941;
    /** sqrt(0.5) == 1 / sqrt(2). */
    private static final double ROOT_HALF = 0.707106781186547524400;
    /** The limit for safe multiplication of {@code x*y}, assuming values above 1.
     * Used to maintain positive values during the power computation. */
    private static final double SAFE_MULTIPLY = 0x1.0p500;
    /** Used to downscale values before multiplication. Downscaling of any value
     * strictly above SAFE_MULTIPLY will be above 1 even including a double-double
     * roundoff that lowers the magnitude. */
    private static final double SAFE_MULTIPLY_DOWNSCALE = 0x1.0p-500;
    /** Error message when the input is not a normalized double: {@code x != x + xx}. */
    private static final String NOT_NOMALIZED = "Input is not a normalized double-double";

    /**
     * No instances.
     */
    private DDExt() {}

    /**
     * Compute the number {@code x} raised to the power {@code n}.
     *
     * <p>This uses the powDSimple algorithm of van Mulbregt [1] which applies a Taylor series
     * adjustment to the result of {@code x^n}:
     * <pre>
     * (x+xx)^n = x^n * (1 + xx/x)^n
     *          = x^n + x^n * (exp(n log(1 + xx/x)) - 1)
     * </pre>
     *
     * <ol>
     * <li>
     * van Mulbregt, P. (2018).
     * <a href="https://doi.org/10.48550/arxiv.1802.06966">Computing the Cumulative Distribution
     * Function and Quantiles of the One-sided Kolmogorov-Smirnov Statistic</a>
     * arxiv:1802.06966.
     * </ol>
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power.
     * @param r Result.
     * @return the result
     * @see Math#pow(double, double)
     */
    static DD simplePow(double x, double xx, int n, DD r) {
        // Edge cases. These ignore (x, xx) = (+/-1, 0). The result is Math.pow(x, n).
        if (n == 0) {
            return DD.set(1, r);
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x) || x == 0) {
            return DD.set(Math.pow(x, n), r);
        }
        // Here the number is non-zero finite
        assert x == x + xx : NOT_NOMALIZED;
        if (n < 0) {
            computeSimplePow(x, xx, -1L * n, r);
            // Safe inversion of small values. Reuse the existing multiply scaling factors.
            // 1 / x = b * 1 / bx
            if (Math.abs(r.hi()) < SAFE_MULTIPLY_DOWNSCALE) {
                DD.inverse(r.hi() * SAFE_MULTIPLY, r.lo() * SAFE_MULTIPLY, r);
                final double hi = r.hi() * SAFE_MULTIPLY;
                // Return signed zero by multiplication for infinite
                final double lo = r.lo() * (Double.isInfinite(hi) ? 0 : SAFE_MULTIPLY);
                return DD.set(hi, lo, r);
            }
            return DD.inverse(r.hi(), r.lo(), r);
        }
        return computeSimplePow(x, xx, n, r);
    }

    /**
     * Compute the number {@code x} raised to the power {@code n} (must be strictly positive).
     *
     * <p>This method exists to allow negation of the power when it is {@link Integer#MIN_VALUE}
     * by casting to a long. It is called directly by simplePow and computeSimplePowScaled
     * when the arguments have already been validated.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power (must be positive).
     * @param r Result.
     * @return the result
     */
    private static DD computeSimplePow(double x, double xx, long n, DD r) {
        final double y = Math.pow(x, n);
        final double z = xx / x;
        // Taylor series: (1 + z)^n = n*z * (1 + ((n-1)*z/2))
        // Applicable when n*z is small.
        // Assume xx < epsilon * x.
        // n > 1e8 => n * xx/x > 1e8 * xx/x == n*z > 1e8 * 1e-16 > 1e-8
        double w;
        if (n > LARGE_N) {
            w = Math.expm1(n * Math.log1p(z));
        } else {
            w = n * z * (1 + (n - 1) * z * 0.5);
        }
        // w ~ (1+z)^n : z ~ 2^-53
        // Math.pow(1 + 2 * Math.ulp(1.0), 2^31) ~ 1.0000000000000129
        // Math.pow(1 - 2 * Math.ulp(1.0), 2^31) ~ 0.9999999999999871
        // If (x, xx) is normalised a fast-two-sum can be used.
        // fast-two-sum propagates sign changes for input of (+/-1.0, +/-0.0) (two-sum does not).
        return DD.fastTwoSum(y, y * w, r);
    }

    /**
     * Compute the number {@code x} raised to the power {@code n}.
     *
     * <p>The value is returned as fractional {@code f} and integral
     * {@code 2^exp} components.
     * <pre>
     * (x+xx)^n = (f+ff) * 2^exp
     * </pre>
     *
     * <p>The combined fractional part (f, ff) is in the range {@code [0.5, 1)}.
     *
     * <p>Special cases:
     *
     * <ul>
     * <li>If {@code (x, xx)} is zero the high part of the fractional part is
     * computed using {@link Math#pow(double, double) Math.pow(x, n)} and the exponent is 0.
     * <li>If {@code n = 0} the fractional part is 0.5 and the exponent is 1.
     * <li>If {@code (x, xx)} is an exact power of 2 the fractional part is 0.5 and the exponent
     * is the power of 2 minus 1.
     * <li>If the result high-part is an exact power of 2 and the low-part has an opposite
     * signed non-zero magnitude then the fraction high-part {@code f} will be {@code +/-1} such that
     * the double-double number is in the range {@code [0.5, 1)}.
     * <p>If the argument is not finite then a fractional representation is not possible.
     * In this case the fraction and the scale factor is undefined.
     * </ul>
     *
     * <p>Note: This method returns the exponent to avoid using an {@code long[] exp} argument
     * to save the result.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power.
     * @param f Fraction part.
     * @return Power of two scale factor (integral exponent).
     * @see #simplePow(double, double, int, DD)
     * @see DD#frexp(double, double, DD)
     */
    static long simplePowScaled(double x, double xx, int n, DD f) {
        // Edge cases.
        if (n == 0) {
            DD.set(0.5, f);
            return 1;
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x) || x == 0) {
            DD.set(Math.pow(x, n), f);
            return 0;
        }
        // Here the number is non-zero finite
        assert x == x + xx : NOT_NOMALIZED;
        long b = DD.frexp(x, xx, f);
        if (n < 0) {
            b = computeSimplePowScaled(b, f.hi(), f.lo(), -1L * n, f);
            // Result is a non-zero fraction part so inversion is safe
            DD.inverse(f.hi(), f.lo(), f);
            // Rescale to [0.5, 1.0]
            return -b + DD.frexp(f.hi(), f.lo(), f);
        }
        return computeSimplePowScaled(b, f.hi(), f.lo(), n, f);
    }

    /**
     * Compute the number {@code x} (non-zero finite) raised to the power {@code n} (must be strictly positive).
     *
     * <p>This method exists to allow negation of the power when it is {@link Integer#MIN_VALUE}
     * by casting to a long. By using a fractional representation for the argument
     * the recursive calls avoid a step to normalise the input.
     *
     * @param exp Integral component 2^exp of x.
     * @param x Fractional high part of x.
     * @param xx Fractional low part of x.
     * @param n Power (in [1, 2^31]).
     * @param f Fraction part.
     * @return Power of two scale factor (integral exponent).
     */
    private static long computeSimplePowScaled(long exp, double x, double xx, long n, DD f) {
        // By normalising x we can break apart the power to avoid over/underflow:
        // x^n = (f * 2^b)^n = 2^bn * f^n
        long b = exp;
        double f0 = x;
        double f1 = xx;

        // Minimise the amount we have to decompose the power. This is done
        // using either f (<=1) or 2f (>=1) as the fractional representation,
        // based on which can use a larger exponent without over/underflow.
        // We approximate the power as 2^b and require a result with the
        // smallest absolute b. An additional consideration is the low-part ff
        // which sets a more conservative underflow limit:
        // f^n              = 2^(-b+53)  => b = -n log2(f) - 53
        // (2f)^n = 2^n*f^n = 2^b        => b =  n log2(f) + n
        // Switch-over point for f is at:
        // -n log2(f) - 53 = n log2(f) + n
        // 2n log2(f) = -53 - n
        // f = 2^(-53/2n) * 2^(-1/2)
        // Avoid a power computation to find the threshold by dropping the first term:
        // f = 2^(-1/2) = 1/sqrt(2) = sqrt(0.5) = 0.707
        // This will bias towards choosing f even when (2f)^n would not overflow.
        // It allows the same safe exponent to be used for both cases.

        // Safe maximum for exponentiation.
        long m;
        double af = Math.abs(f0);
        if (af < ROOT_HALF) {
            // Choose 2f.
            // This case will handle (x, xx) = (1, 0) in a single power operation
            f0 *= 2;
            f1 *= 2;
            af *= 2;
            b -= 1;
            if (n <= SAFE_EXPONENT_2F || af <= SAFE_2F) {
                m = n;
            } else {
                // f^m < 2^1013
                // m ~ 1013 / log2(f)
                m = Math.max(SAFE_EXPONENT_2F, (long) (SAFE_EXPONENT_2F * LN2 / Math.log(af)));
            }
        } else {
            // Choose f
            if (n <= SAFE_EXPONENT_F || af >= SAFE_F) {
                m = n;
            } else {
                // f^m > 2^-958
                // m ~ -958 / log2(f)
                m = Math.max(SAFE_EXPONENT_F, (long) (-SAFE_EXPONENT_F * LN2 / Math.log(af)));
            }
        }

        if (n <= m) {
            computeSimplePow(f0, f1, n, f);
            return b * n + DD.frexp(f.hi(), f.lo(), f);
        }

        // Decompose the power function.
        // quotient q = n / m
        // remainder r = n % m
        // f^n = (f^m)^(n/m) * f^(n%m)

        final long q = n / m;
        final long r = n % m;
        // (f^m)
        // m is safe and > 1
        computeSimplePow(f0, f1, m, f);
        long qb = DD.frexp(f.hi(), f.lo(), f);
        // (f^m)^(n/m)
        // q is non-zero but may be 1
        if (q > 1) {
            // full fast-pow to ensure safe exponentiation
            qb = computeSimplePowScaled(qb, f.hi(), f.lo(), q, f);
        }
        // f^(n%m)
        // r may be zero or one which do not require another power
        if (r == 0) {
            return b * n + qb + DD.frexp(f.hi(), f.lo(), f);
        }
        if (r == 1) {
            DD.multiply(f.hi(), f.lo(), f0, f1, f);
            return b * n + qb + DD.frexp(f.hi(), f.lo(), f);
        }
        // Here r is safe
        final double q0 = f.hi();
        final double q1 = f.lo();
        computeSimplePow(f0, f1, r, f);
        final long rb = DD.frexp(f.hi(), f.lo(), f);
        // (f^m)^(n/m) * f^(n%m)
        DD.multiply(q0, q1, f.hi(), f.lo(), f);
        // 2^bn * (f^m)^(n/m) * f^(n%m)
        return b * n + qb + rb + DD.frexp(f.hi(), f.lo(), f);
    }
}
