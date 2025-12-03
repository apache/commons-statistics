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

import org.apache.commons.numbers.core.DD;

/**
 * Computes extended precision floating-point operations.
 *
 * <p>Extended precision computation is delegated to the {@link DD} class. The methods here
 * verify the arguments to the computations will not overflow.
 */
final class ExtendedPrecision {
    /** sqrt(2 pi) as a double-double number.
     * Divided into two parts from the value sqrt(2 pi) computed to 64 decimal digits. */
    static final DD SQRT2PI = DD.ofSum(2.5066282746310007, -1.8328579980459167e-16);

    /** Threshold for a big number that may overflow when squared. 2^500. */
    private static final double BIG = 0x1.0p500;
    /** Threshold for a small number that may underflow when squared. 2^-500. */
    private static final double SMALL = 0x1.0p-500;
    /** Scale up by 2^600. */
    private static final double SCALE_UP = 0x1.0p600;
    /** Scale down by 2^600. */
    private static final double SCALE_DOWN = 0x1.0p-600;
    /** X squared value where {@code exp(-0.5*x*x)} cannot increase accuracy using the round-off
     * from x squared. */
    private static final int EXP_M_HALF_XX_MIN_VALUE = 2;
    /** Approximate x squared value where {@code exp(-0.5*x*x) == 0}. This is above
     * {@code -2 * ln(2^-1074)} due to rounding performed within the exp function. */
    private static final int EXP_M_HALF_XX_MAX_VALUE = 1491;

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
        return SQRT2PI.multiply(a).hi();
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
        return DD.ofProduct(2 * a, a).sqrt().hi();
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
        final DD x2 = DD.ofSquare(x);
        return expxx(-0.5 * x2.hi(), -0.5 * x2.lo());
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
        // bits are not significant to change the following sum with exp(a)

        final double ea = Math.exp(a);
        // b ~ expm1(b)
        return ea * b + ea;
    }
}
