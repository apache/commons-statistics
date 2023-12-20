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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.apache.commons.numbers.core.DD;

/**
 * A mutable 128-bit signed integer.
 *
 * <p>This is a specialised class to implement an accumulator of {@code long} values.
 *
 * <p>Note: This number uses a signed long integer representation of:
 *
 * <pre>value = 2<sup>64</sup> * hi64 + lo64</pre>
 *
 * <p>If the high value is zero then the low value is the long representation of the
 * number including the sign bit. Otherwise the low value corresponds to a correction
 * term for the scaled high value which contains the sign-bit of the number.
 *
 * @since 1.1
 */
final class Int128 {
    /** Mask for the lower 32-bits of a long. */
    private static final long MASK32 = 0xffff_ffffL;

    /** low 64-bits. */
    private long lo;
    /** high 64-bits. */
    private long hi;

    /**
     * Create an instance.
     */
    private Int128() {
        // No-op
    }

    /**
     * Create an instance.
     *
     * @param x Value.
     */
    private Int128(long x) {
        lo = x;
    }

    /**
     * Create an instance using a direct binary representation.
     * This is package-private for testing.
     *
     * @param hi High 64-bits.
     * @param lo Low 64-bits.
     */
    Int128(long hi, long lo) {
        this.lo = lo;
        this.hi = hi;
    }

    /**
     * Create an instance. The initial value is zero.
     *
     * @return the instance
     */
    static Int128 create() {
        return new Int128();
    }

    /**
     * Create an instance of the {@code long} value.
     *
     * @param x Value.
     * @return the instance
     */
    static Int128 of(long x) {
        return new Int128(x);
    }

    /**
     * Adds the value.
     *
     * @param x Value.
     */
    void add(long x) {
        final long y = lo;
        final long r = y + x;
        // Overflow if the result has the opposite sign of both arguments
        // (+,+) -> -
        // (-,-) -> +
        // Detect opposite sign:
        if (((y ^ r) & (x ^ r)) < 0) {
            // Carry overflow bit
            hi += x < 0 ? -1 : 1;
        }
        lo = r;
    }

    /**
     * Adds the value.
     *
     * @param x Value.
     */
    void add(Int128 x) {
        // Avoid issues adding to itself
        final long l = x.lo;
        final long h = x.hi;
        add(l);
        hi += h;
    }

    /**
     * Compute the square of the low 64-bits of this number.
     *
     * <p>Warning: This ignores the upper 64-bits. Use with caution.
     *
     * @return the square
     */
    UInt128 squareLow() {
        final long x = lo;
        final long upper = IntMath.squareHigh(x);
        return new UInt128(upper, x * x);
    }

    /**
     * Convert to a BigInteger.
     *
     * @return the value
     */
    BigInteger toBigInteger() {
        long h = hi;
        long l = lo;
        // Special cases
        if (h == 0) {
            return BigInteger.valueOf(l);
        }
        if (l == 0) {
            return BigInteger.valueOf(h).shiftLeft(64);
        }

        // The representation is 2^64 * hi64 + lo64.
        // Here we avoid evaluating the addition:
        // BigInteger.valueOf(l).add(BigInteger.valueOf(h).shiftLeft(64))
        // It is faster to create from bytes.
        // BigInteger bytes are an unsigned integer in BigEndian format, plus a sign.
        // If both values are positive we can use the values unchanged.
        // Otherwise selective negation is used to create a positive magnitude
        // and we track the sign.
        // Note: Negation of -2^63 is valid to create an unsigned 2^63.

        int sign = 1;
        if ((h ^ l) < 0) {
            // Opposite signs and lo64 is not zero.
            // The lo64 bits are an adjustment to the magnitude of hi64
            // to make it smaller.
            // Here we rearrange to [2^64 * (hi64-1)] + [2^64 - lo64].
            // The second term [2^64 - lo64] can use lo64 as an unsigned 64-bit integer.
            // The first term [2^64 * (hi64-1)] does not work if low is zero.
            // It would work if zero was detected and we carried the overflow
            // bit up to h to make it equal to: (h - 1) + 1 == h.
            // Instead lo64 == 0 is handled as a special case above.

            if (h >= 0) {
                // Treat (unchanged) low as an unsigned add
                h = h - 1;
            } else {
                // As above with negation
                h = ~h; // -h - 1
                l = -l;
                sign = -1;
            }
        } else if (h < 0) {
            // Invert negative values to create the equivalent positive magnitude.
            h = -h;
            l = -l;
            sign = -1;
        }

        return new BigInteger(sign,
            ByteBuffer.allocate(Long.BYTES * 2)
                .putLong(h).putLong(l).array());
    }

    /**
     * Convert to a double-double.
     *
     * @return the value
     */
    DD toDD() {
        // Don't combine two 64-bit DD numbers:
        // DD.of(hi).scalb(64).add(DD.of(lo))
        // It is more accurate to create a 96-bit number and add the final 32-bits.
        // Sum low to high.
        // Note: Masking a negative hi number will create a small positive magnitude
        // to add to a larger negative number:
        // e.g. x = (x & 0xff) + ((x >> 8) << 8)
        return DD.of(lo).add((hi & MASK32) * 0x1.0p64).add((hi >> Integer.SIZE) * 0x1.0p96);
    }

    /**
     * Return the lower 64-bits as a {@code long} value.
     *
     * <p>If the high value is zero then the low value is the long representation of the
     * number including the sign bit. Otherwise this value corresponds to a correction
     * term for the scaled high value which contains the sign-bit of the number
     * (see {@link Int128}).
     *
     * @return the low 64-bits
     */
    long lo64() {
        return lo;
    }

    /**
     * Return the higher 64-bits as a {@code long} value.
     *
     * @return the high 64-bits
     * @see #lo64()
     */
    long hi64() {
        return hi;
    }
}
