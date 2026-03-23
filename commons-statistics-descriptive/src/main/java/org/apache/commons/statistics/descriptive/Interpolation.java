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
package org.apache.commons.statistics.descriptive;

import org.apache.commons.numbers.core.DD;

/**
 * Support class for interpolation.
 *
 * @since 1.1
 */
final class Interpolation {
    /** 0.5. */
    private static final double HALF = 0.5;
    /** The value 2^53 converted for comparison as an unsigned integer. */
    private static final long UNSIGNED_2_POW_53 = Long.MIN_VALUE + (1L << 53);
    /** 2^63. */
    private static final double TWO_POW_63 = 0x1.0p63;

    /** No instances. */
    private Interpolation() {}

    /**
     * Compute the arithmetic mean of the two values taking care to avoid overflow.
     *
     * @param x Value.
     * @param y Value.
     * @return the mean
     */
    static double mean(double x, double y) {
        final double v = x + y;
        if (Double.isFinite(v)) {
            return v * 0.5;
        }
        // Note: Using this by default can be incorrect on sub-normal numbers
        return x * 0.5 + y * 0.5;
    }

    /**
     * Compute the arithmetic mean of the two values.
     *
     * @param x Value.
     * @param y Value.
     * @return the mean
     */
    static double mean(int x, int y) {
        // long arithmetic handles a 32-bit signed integer
        return ((long) x + y) * 0.5;
    }

    /**
     * Compute the arithmetic mean of the two values as a {@code double}.
     *
     * @param x Value.
     * @param y Value.
     * @return the mean
     */
    static double meanAsDouble(long x, long y) {
        final long sum = x + y;
        // Overflow if both arguments have the opposite sign of the result.
        // Note that overflow is expected to be rare and in most cases
        // the result is half the sum.
        if (((x ^ sum) & (y ^ sum)) < 0) {
            // The sum is a 64-bit magnitude plus a sign.
            // Split the sum into two means which are representable doubles.
            // These can be added to ensure correct rounding for all cases.
            // The upper bits will be an even number so shift 1 for the mean.
            // The sign bit may have been lost to overflow so restore from one addend.
            // Note overflow only occurs if the signs match so use either x or y.
            final double hi = ((sum & 0xffff_ffff_0000_0000L) >>> 1) | (x & Long.MIN_VALUE);
            final double lo = (sum & 0xffff_ffffL) * 0.5;
            return hi + lo;
        }
        return sum * 0.5;
    }

    /**
     * Compute the arithmetic mean of the two values as a {@code long}.
     *
     * <p>The result value is the nearest whole number to the result, with ties
     * rounding towards positive infinity. This is equivalent to the ceiling average.
     *
     * @param x Value.
     * @param y Value.
     * @return the mean
     */
    static long meanAsLong(long x, long y) {
        // Hacker's Delight 2-5: Average of Two Integers
        // Ceiling average for signed integers
        return (x | y) - ((x ^ y) >> 1);
    }

    /**
     * Compute the arithmetic mean of the two values.
     *
     * <p>The result {@code long} value is the nearest whole number to the result, with ties
     * rounding towards positive infinity. This is equivalent to the ceiling average.
     *
     * @param x Value.
     * @param y Value.
     * @return the mean
     */
    static StatisticResult mean(long x, long y) {
        // Compute the result dynamically.
        // This saves computation in the case where only 1 result is required.
        return new LongStatisticResult() {
            @Override
            public double getAsDouble() {
                return meanAsDouble(x, y);
            }

            @Override
            public long getAsLong() {
                return meanAsLong(x, y);
            }
        };
    }

    /**
     * Linear interpolation between <strong>sorted</strong> values {@code a <= b} using the
     * interpolant {@code t} taking care to avoid overflow.
     *
     * <pre>
     * value = a + t * (b - a)
     * </pre>
     *
     * <p>Note
     *
     * <p>This function has the same properties of as the C++ function <a
     * href="https://en.cppreference.com/w/cpp/numeric/lerp">std::lerp</a> for
     * {@code t in (0, 1)} and {@code b >= a}. It is not a full implementation as it
     * removes explicit checks for {@code t==0} and {@code t==1} and does not support
     * extrapolation as the usage is intended for interpolation of sorted values.
     * The function is monotonic and avoids overflow for finite {@code a} and {@code b}.
     *
     * <p>Interpolation between equal signed infinity arguments will return {@code a}.
     * Alternative implementations may return {@code NaN} for this case. Thus this method
     * interprets infinity values as equivalent and avoids interpolation.
     *
     * @param a Min value.
     * @param b Max value.
     * @param t Interpolant in (0, 1).
     * @return the value
     */
    static double interpolate(double a, double b, double t) {
        // Linear interpolation adapted from:
        // P0811R2: Well-behaved interpolation for numbers and pointers
        // https://www.open-std.org/jtc1/sc22/wg21/docs/papers/2018/p0811r2.html
        // https://en.cppreference.com/w/cpp/numeric/lerp

        // Notes:
        // a+t*(b-a) does not in general reproduce b when t==1, and can overflow if a and b
        // have the largest exponent and opposite signs.
        // t*b+(1-t)*a is not monotonic in general (unless the product ab≤0).

        // Exact, monotonic, bounded, determinate, and (for a=b=0) consistent:
        // Removed check a >= 0 && b <= 0 as the arguments are assumed to be sorted.
        if (a <= 0 && b >= 0) {
            // Note: Does not return a for a=-0.0, b=0.0, t=0.0.
            // This is ignored as interpolation is only used when t != 0.0.
            return t * b + (1.0 - t) * a;
        }

        // Here a and b are on the same side of zero, and at least 1 is non-zero.
        // Since we assume t in (0, 1) remove: if t==1 return b.

        // P0811R2 assumes finite arguments so we add a case to detect same signed infinity
        // and avoid (b - a) == NaN. This is simply handled with floating-point equivalence.
        if (a == b) {
            return a;
        }

        // Exact at t=0, monotonic except near t=1,
        // bounded, determinate, and consistent:
        // Note: switching to 'b - (1.0 - t) * (b - a)' when t > 0.5 would
        // provide exact ends at t=0 and t=1.
        return a + t * (b - a);
    }

    /**
     * Linear interpolation between <strong>sorted</strong> values {@code a <= b} using the
     * interpolant {@code t}.
     *
     * <pre>
     * value = a + t * (b - a)
     * </pre>
     *
     * <p>The {@code long} value is the nearest whole number to the result, with ties
     * rounding towards positive infinity. This value will be in {@code [a, b]}.
     *
     * <p>The {@code double} value is computed within 1 ULP of the exact result.
     * In some cases this may be outside the range {@code [a, b]} due to rounding
     * to a 53-bit representation.
     *
     * <p>Note
     *
     * <p>This function does not support extrapolation as the usage is intended for
     * interpolation of sorted values.
     *
     * @param a Min value.
     * @param b Max value.
     * @param t Interpolant in [0, 1].
     * @return the value
     */
    static StatisticResult interpolate(long a, long b, double t) {
        final long diff = b - a;

        // The product t * (b - a) is exact as a double-double
        // if the factor (b - a) is a representable double.
        // Compare to 2^53 as unsigned to also detect overflow.
        if (diff + Long.MIN_VALUE < UNSIGNED_2_POW_53) {
            final DD addend = DD.ofProduct(diff, t);
            // result = a + addend
            // The positive addend has the integer component in the high part
            // if t in [0, 1]. Decompose to exact integer and fractional parts.
            // Note that if addend.lo() is negative then i is already
            // rounded towards positive infinity. Later rounding will not
            // double round up, e.g. addend=(2, -2^-53) -> i=2; f=(-2^53, 0).
            final long i = (long) addend.hi();
            final DD f = DD.ofSum(addend.hi() - i, addend.lo());

            // Integer part of the result
            final long v = a + i;

            // Round to nearest whole number, ties towards positive infinity.
            final long l = v + ((f.hi() > HALF || f.hi() == HALF && f.lo() >= 0) ? 1 : 0);

            // Addition includes f.lo() required for correct rounding when
            // |v| is close to 1 and t is small so addend has no integer part.
            final double d = DD.of(v).add(f).hi();

            return new LongStatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        // Create two exact products using the difference (b - a) split into
        // representable doubles.
        // Subtract the integer parts to leave two fractional parts. These are
        // combined to allow rounding.

        // Two exact addends
        final DD delta = DD.ofUnsigned(diff);
        DD addend1 = DD.ofProduct(delta.hi(), t);
        DD addend2 = DD.ofProduct(delta.lo(), t);

        // Subtract integer parts and add them to min value a
        long v = a;
        long i;
        // addend1 can be > 2^63
        if (addend1.hi() >= TWO_POW_63) {
            // The value is even so add half
            i = (long) (addend1.hi() * 0.5);
            final long j = (long) addend1.lo();
            v = v + i + i + j;
            addend1 = DD.ofSum((addend1.hi() * 0.5 - i) * 2, addend1.lo() - j);
        } else {
            i = (long) addend1.hi();
            final long j = (long) addend1.lo();
            v = v + i + j;
            addend1 = DD.ofSum(addend1.hi() - i, addend1.lo() - j);
        }
        // The low part of addend2 cannot have an integer component
        // since this was a [0, 11]-bit integer * [0, 1].
        i = (long) addend2.hi();
        v = v + i;
        addend2 = DD.ofSum(addend2.hi() - i, addend2.lo());

        // Collect addends.
        // The result addend has a magnitude in [0, 2) and
        // is 117-bits or less if integer parts were subtracted.
        // High part of addend2 overlaps with low part of addend1:
        // addend1 |----|----|
        // addend2      |----|----|
        // addend1 may be shifted right if integer component was
        // subtracted increasing the overlap.
        // DD addition should collect the low parts of addend2
        // but can potentially lose bits.
        // These should not effect rounding to integer.
        // Given the 64-bit unsigned multiplier the
        // largest separation of bits by zeros in the multiplier
        // (and thus the result) is 62 zeros. So a 0.5 cannot have
        // 105 zeros before the next 1 bit in the result. Rounding
        // of 0.5 only requires an accurate 64-bit result so the
        // precision of DD.add is sufficient.
        final DD addend = addend1.add(addend2);

        // double result is within 1 ULP
        final double d = DD.of(v).add(addend).hi();

        // Sum of fractions may generate an integer part
        double f = addend.hi();
        i = (long) f;
        v += i;
        f -= i;

        // Round v to nearest whole number using fraction f.
        // This comparison must use the parts in order. Each part is
        // smaller than the previous part.
        // Ties to infinity using f >= 0.5 and f < -0.5.
        if (f > HALF ||
            f == HALF && addend.lo() >= 0) {
            v++;
        } else if (f < -HALF) {
            // ---
            // Note: It is not possible to hit f == -0.5 && addend.lo() < 0
            // ---
            // delta (64-bits) * t (53-bits)
            // The exact result is 117-bits.
            // [1] If t has 1 significant bit (power of 2) the largest gap between
            // non-zero bits is 62 if delta = 1000...0001
            // [2] If t has multiple significant bits the largest gap between bits
            // is 51 if t = 1.000...0001 * 2^b and delta is a power of 2.
            // To generate f == -0.5 requires at least 52 zero bits after the first
            // significant bit.
            // If t is a power of 2 addend1 must be entirely positive and
            // the -0.5 is from addend2 with zero trailing bits.
            // If addend1 low bits are negative this can only result from the factor t
            // having more than 1 bit. Given [2] a -0.5 is entirely from addend1
            // and all trailing bits are zero (addend2 is zero).
            v--;
        }

        final long l = v;

        return new LongStatisticResult() {
            @Override
            public double getAsDouble() {
                return d;
            }

            @Override
            public long getAsLong() {
                return l;
            }
        };
    }
}
