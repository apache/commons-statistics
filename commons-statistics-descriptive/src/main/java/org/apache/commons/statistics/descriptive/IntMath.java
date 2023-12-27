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
package org.apache.commons.statistics.descriptive;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.numbers.core.DD;

/**
 * Support class for integer math.
 *
 * @since 1.1
 */
final class IntMath {
    /** Mask for the lower 32-bits of a long. */
    private static final long MASK32 = 0xffff_ffffL;
    /** Mask for the lower 52-bits of a long. */
    private static final long MASK52 = 0xf_ffff_ffff_ffffL;
    /** Bias offset for the exponent of a double. */
    private static final int EXP_BIAS = 1023;
    /** Shift for the exponent of a double. */
    private static final int EXP_SHIFT = 52;
    /** 0.5. */
    private static final double HALF = 0.5;
    /** 2^53. */
    private static final long TWO_POW_53 = 1L << 53;

    /** No instances. */
    private IntMath() {}

    /**
     * Square the values as if an unsigned 64-bit long to produce the high 64-bits
     * of the 128-bit unsigned result.
     *a
     * <p>This method computes the equivalent of:
     * <pre>{@code
     * Math.multiplyHigh(x, x)
     * Math.unsignedMultiplyHigh(x, x) - (((x >> 63) & x) << 1)
     * }</pre>
     *
     * <p>Note: The method {@code Math.multiplyHigh} was added in JDK 9
     * and should be used as above when the source code targets Java 11
     * to exploit the intrinsic method.
     *
     * <p>Note: The method uses the unsigned multiplication. When the input is negative
     * it can be adjusted to the signed result by subtracting the argument twice from the
     * result.
     *
     * @param x Value
     * @return the high 64-bits of the 128-bit result
     */
    static long squareHigh(long x) {
        // Computation is based on the following observation about the upper (a and x)
        // and lower (b and y) bits of unsigned big-endian integers:
        //   ab * xy
        // =  b *  y
        // +  b * x0
        // + a0 *  y
        // + a0 * x0
        // = b * y
        // + b * x * 2^32
        // + a * y * 2^32
        // + a * x * 2^64
        //
        // Summation using a character for each byte:
        //
        //             byby byby
        // +      bxbx bxbx 0000
        // +      ayay ayay 0000
        // + axax axax 0000 0000
        //
        // The summation can be rearranged to ensure no overflow given
        // that the result of two unsigned 32-bit integers multiplied together
        // plus two full 32-bit integers cannot overflow 64 bits:
        // > long x = (1L << 32) - 1
        // > x * x + x + x == -1 (all bits set, no overflow)
        //
        // The carry is a composed intermediate which will never overflow:
        //
        //             byby byby
        // +           bxbx 0000
        // +      ayay ayay 0000
        //
        // +      bxbx 0000 0000
        // + axax axax 0000 0000

        final long a = x >>> 32;
        final long b = x & MASK32;

        final long aa = a * a;
        final long ab = a * b;
        final long bb = b * b;

        // Cannot overflow
        final long carry = (bb >>> 32) +
                           (ab & MASK32) +
                            ab;
        // Note:
        // low = (carry << 32) | (bb & MASK32)
        // Benchmarking shows outputting low to a long[] output argument
        // has no benefit over computing 'low = value * value' separately.

        final long hi = (ab >>> 32) + (carry >>> 32) + aa;
        // Adjust to the signed result:
        // if x < 0:
        //    hi - 2 * x
        return hi - (((x >> 63) & x) << 1);
    }

    /**
     * Multiply the two values as if unsigned 64-bit longs to produce the high 64-bits
     * of the 128-bit unsigned result.
     *
     * <p>This method computes the equivalent of:
     * <pre>{@code
     * Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a)
     * }</pre>
     *
     * <p>Note: The method {@code Math.multiplyHigh} was added in JDK 9
     * and should be used as above when the source code targets Java 11
     * to exploit the intrinsic method.
     *
     * <p>Note: The method {@code Math.unsignedMultiplyHigh} was added in JDK 18
     * and should be used when the source code target allows.
     *
     * <p>Taken from {@code o.a.c.rng.core.source64.LXMSupport}.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @return the high 64-bits of the 128-bit result
     */
    static long unsignedMultiplyHigh(long value1, long value2) {
        // Computation is based on the following observation about the upper (a and x)
        // and lower (b and y) bits of unsigned big-endian integers:
        //   ab * xy
        // =  b *  y
        // +  b * x0
        // + a0 *  y
        // + a0 * x0
        // = b * y
        // + b * x * 2^32
        // + a * y * 2^32
        // + a * x * 2^64
        //
        // Summation using a character for each byte:
        //
        //             byby byby
        // +      bxbx bxbx 0000
        // +      ayay ayay 0000
        // + axax axax 0000 0000
        //
        // The summation can be rearranged to ensure no overflow given
        // that the result of two unsigned 32-bit integers multiplied together
        // plus two full 32-bit integers cannot overflow 64 bits:
        // > long x = (1L << 32) - 1
        // > x * x + x + x == -1 (all bits set, no overflow)
        //
        // The carry is a composed intermediate which will never overflow:
        //
        //             byby byby
        // +           bxbx 0000
        // +      ayay ayay 0000
        //
        // +      bxbx 0000 0000
        // + axax axax 0000 0000

        final long a = value1 >>> 32;
        final long b = value1 & MASK32;
        final long x = value2 >>> 32;
        final long y = value2 & MASK32;

        final long by = b * y;
        final long bx = b * x;
        final long ay = a * y;
        final long ax = a * x;

        // Cannot overflow
        final long carry = (by >>> 32) +
                           (bx & MASK32) +
                            ay;
        // Note:
        // low = (carry << 32) | (by & INT_TO_UNSIGNED_BYTE_MASK)
        // Benchmarking shows outputting low to a long[] output argument
        // has no benefit over computing 'low = value1 * value2' separately.

        return (bx >>> 32) + (carry >>> 32) + ax;
    }

    /**
     * Multiply the arguments as if unsigned integers to a {@code double} result.
     *
     * @param x Value.
     * @param y Value.
     * @return the double
     */
    static double unsignedMultiplyToDouble(long x, long y) {
        final long lo = x * y;
        // Fast case: check the arguments cannot overflow a long.
        // This is true if neither has the upper 33-bits set.
        if (((x | y) >>> 31) == 0) {
            // Implicit conversion to a double.
            return lo;
        }
        return uint128ToDouble(unsignedMultiplyHigh(x, y), lo);
    }

    /**
     * Convert an unsigned 128-bit integer to a {@code double}.
     *
     * @param hi High 64-bits.
     * @param lo Low 64-bits.
     * @return the double
     */
    static double uint128ToDouble(long hi, long lo) {
        // Require the representation:
        // 2^exp * mantissa / 2^53
        // The mantissa has an implied leading 1-bit.

        // We have the mantissa final bit as xxx0 or xxx1.
        // To perform correct rounding we maintain the 54-th bit (a) and
        // a check bit (b) of remaining bits.
        // Cases:
        // xxx0 00 - round-down              [1]
        // xxx0 0b - round-down              [1]
        // xxx0 a0 - half-even, round-down   [4]
        // xxx0 ab - round-up                [2]
        // xxx1 00 - round-down              [1]
        // xxx1 0b - round-down              [1]
        // xxx1 a0 - half-even, round-up     [3]
        // xxx1 ab - round-up                [2]
        // [1] If the 54-th bit is 0 always round-down.
        // [2] Otherwise round-up if the check bit is set or
        // [3] the final bit is odd (half-even rounding up).
        // [4] half-even rounding down.

        if (hi == 0) {
            // If lo is a 63-bit result then we are done
            if (lo >= 0) {
                return lo;
            }
            // Create a 63-bit number with a sticky bit for rounding, rescale the result
            return 2 * (double) ((lo >>> 1) | (lo & 0x1));
        }

        // Initially we create the most significant 64-bits.
        final int shift = Long.numberOfLeadingZeros(hi);
        // Shift the high bits and add trailing low bits.
        // The mask is for the bits from low that are *not* used.
        // Flipping the mask obtains the bits we concatenate
        // after shifting (64 - shift).
        final long maskLow = -1L >>> shift;
        long bits64 = (hi << shift) | ((lo & ~maskLow) >>> -shift);
        // exponent for 2^exp is the index of the highest bit in the 128 bit integer
        final int exp = 127 - shift;
        // Some of the low bits are lost. If non-zero set
        // a sticky bit for rounding.
        bits64 |= (lo & maskLow) == 0 ? 0 : 1;

        // We have a 64-bit unsigned fraction magnitude and an exponent.
        // This must be converted to a IEEE double by mapping the fraction to a base of 2^53.

        // Create the 53-bit mantissa without the implicit 1-bit
        long bits = (bits64 >>> 11) & MASK52;
        // Extract 54-th bit and a sticky bit
        final long a = (bits64 >>> 10) & 0x1;
        final long b = (bits64 << 54) == 0 ? 0 : 1;
        // Perform half-even rounding.
        bits += a & (b | (bits & 0x1));

        // Add the exponent.
        // No worry about overflow to the sign bit as the max exponent is 127.
        bits += (long) (exp + EXP_BIAS) << EXP_SHIFT;

        return Double.longBitsToDouble(bits);
    }

    /**
     * Return the whole number that is nearest to the {@code double} argument {@code x}
     * as an {@code int}, with ties rounding towards positive infinity.
     *
     * <p>This will raise an {@link ArithmeticException} if the closest
     * integer result is not within the range {@code [-2^31, 2^31)},
     * i.e. it overflows an {@code int}; or the argument {@code x}
     * is not finite.
     *
     * <p>Note: This method is equivalent to:
     * <pre>
     * Math.toIntExact(Math.round(x))
     * </pre>
     *
     * <p>The behaviour has been re-implemented for consistent error handling
     * for {@code int}, {@code long} and {@code BigInteger} types.
     *
     * @param x Value.
     * @return rounded value
     * @throws ArithmeticException if the {@code result} overflows an {@code int},
     * or {@code x} is not finite
     * @see Math#round(double)
     * @see Math#toIntExact(long)
     */
    static int toIntExact(double x) {
        final double r = roundToInteger(x);
        if (r >= -0x1.0p31 && r < 0x1.0p31) {
            return (int) r;
        }
        throw new ArithmeticException("integer overflow: " + x);
    }

    /**
     * Return the whole number that is nearest to the {@code double} argument {@code x}
     * as an {@code long}, with ties rounding towards positive infinity.
     *
     * <p>This will raise an {@link ArithmeticException} if the closest
     * integer result is not within the range {@code [-2^63, 2^63)},
     * i.e. it overflows a {@code long}; or the argument {@code x}
     * is not finite.
     *
     * @param x Value.
     * @return rounded value
     * @throws ArithmeticException if the {@code result} overflows a {@code long},
     * or {@code x} is not finite
     */
    static long toLongExact(double x) {
        final double r = roundToInteger(x);
        if (r >= -0x1.0p63 && r < 0x1.0p63) {
            return (long) r;
        }
        throw new ArithmeticException("long integer overflow: " + x);
    }

    /**
     * Return the whole number that is nearest to the {@code double} argument {@code x}
     * as an {@code int}, with ties rounding towards positive infinity.
     *
     * <p>This will raise an {@link ArithmeticException} if the argument {@code x}
     * is not finite.
     *
     * @param x Value.
     * @return rounded value
     * @throws ArithmeticException if {@code x} is not finite
     */
    static BigInteger toBigIntegerExact(double x) {
        if (!Double.isFinite(x)) {
            throw new ArithmeticException("BigInteger overflow: " + x);
        }
        final double r = roundToInteger(x);
        if (r >= -0x1.0p63 && r < 0x1.0p63) {
            // Representable as a long
            return BigInteger.valueOf((long) r);
        }
        // Large result
        return new BigDecimal(r).toBigInteger();
    }

    /**
     * Get the whole number that is the nearest to x, with ties rounding towards positive infinity.
     *
     * <p>This method is intended to perform the equivalent of
     * {@link Math#round(double)} without converting to a {@code long} primitive type.
     * This allows the domain of the result to be checked against the range {@code [-2^63, 2^63)}.
     *
     * <p>Note: Adapted from {@code o.a.c.math4.AccurateMath.rint} and
     * modified to perform rounding towards positive infinity.
     *
     * @param x Number from which nearest whole number is requested.
     * @return a double number r such that r is an integer {@code r - 0.5 <= x < r + 0.5}
     */
    private static double roundToInteger(double x) {
        final double y = Math.floor(x);
        final double d = x - y;
        if (d >= HALF) {
            // Here we do not preserve the sign of the operand in the case
            // of -0.5 < x <= -0.0 since the rounded result is required as an integer.
            // if y == -1.0:
            //    return -0.0
            return y + 1.0;
        }
        return y;
    }

    /**
     * Divide value {@code x} by the count {@code n}.
     *
     * @param x Value.
     * @param n Count.
     * @return the quotient
     */
    static double divide(Int128 x, long n) {
        final DD a = x.toDD();
        if (n < TWO_POW_53) {
            // n is a representable double
            return a.divide(n).doubleValue();
        }
        // Extended precision divide when n > 2^53
        return a.divide(DD.of(n)).doubleValue();
    }
}
