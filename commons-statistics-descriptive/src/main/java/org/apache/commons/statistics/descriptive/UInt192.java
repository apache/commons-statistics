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

/**
 * A mutable 192-bit unsigned integer.
 *
 * <p>This is a specialised class to implement an accumulator of squared {@code long} values.
 *
 * @since 1.1
 */
final class UInt192 {
    /** Mask for the lower 32-bits of a long. */
    private static final long MASK32 = 0xffff_ffffL;

    // Data is stored using integers to allow efficient sum-with-carry addition

    /** bits 32-1 (low 32-bits). */
    private int f;
    /** bits 64-33. */
    private int e;
    /** bits 96-65. */
    private int d;
    /** bits 128-97. */
    private int c;
    /** bits 192-129 (high 64-bits). */
    private long ab;

    /**
     * Create an instance.
     */
    private UInt192() {
        // No-op
    }

    /**
     * Create an instance using a direct binary representation.
     * This is package-private for testing.
     *
     * @param hi High 64-bits.
     * @param mid Middle 64-bits.
     * @param lo Low 64-bits.
     */
    UInt192(long hi, long mid, long lo) {
        this.f = (int) lo;
        this.e = (int) (lo >>> Integer.SIZE);
        this.d = (int) mid;
        this.c = (int) (mid >>> Integer.SIZE);
        this.ab = hi;
    }

    /**
     * Create an instance using a direct binary representation.
     *
     * @param ab bits 192-129 (high 64-bits).
     * @param c bits 128-97.
     * @param d bits 96-65.
     * @param e bits 64-33.
     * @param f bits 32-1.
     */
    private UInt192(long ab, int c, int d, int e, int f) {
        this.ab = ab;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
    }

    /**
     * Create an instance. The initial value is zero.
     *
     * @return the instance
     */
    static UInt192 create() {
        return new UInt192();
    }

    /**
     * Adds the squared value {@code x * x}.
     *
     * @param x Value.
     */
    void addSquare(long x) {
        final long lo = x * x;
        final long hi = IntMath.squareHigh(x);

        // Sum with carry.
        long s = (lo & MASK32) + (f & MASK32);
        f = (int) s;
        s = (s >>> Integer.SIZE) + (lo >>> Integer.SIZE) + (e & MASK32);
        e = (int) s;
        s = (s >>> Integer.SIZE) + (hi & MASK32) + (d & MASK32);
        d = (int) s;
        s = (s >>> Integer.SIZE) + (hi >>> Integer.SIZE) + (c & MASK32);
        c = (int) s;
        ab += s >>> Integer.SIZE;
    }

    /**
     * Adds the value.
     *
     * @param x Value.
     */
    void add(UInt192 x) {
        // Avoid issues adding to itself
        final int ff = x.f;
        final int ee = x.e;
        final int dd = x.d;
        final int cc = x.c;
        final long aabb = x.ab;
        // Sum with carry.
        long s = (ff & MASK32) + (f & MASK32);
        f = (int) s;
        s = (s >>> Integer.SIZE) + (ee & MASK32) + (e & MASK32);
        e = (int) s;
        s = (s >>> Integer.SIZE) + (dd & MASK32) + (d & MASK32);
        d = (int) s;
        s = (s >>> Integer.SIZE) + (cc & MASK32) + (c & MASK32);
        c = (int) s;
        ab += (s >>> Integer.SIZE) + aabb;
    }


    /**
     * Multiply by the unsigned value.
     * Any overflow bits are lost.
     *
     * @param x Value.
     * @return the product
     */
    UInt192 unsignedMultiply(int x) {
        final long xx = x & MASK32;
        // Multiply with carry.
        long product = xx * (f & MASK32);
        final int ff = (int) product;
        product = (product >>> Integer.SIZE) + xx * (e & MASK32);
        final int ee = (int) product;
        product = (product >>> Integer.SIZE) + xx * (d & MASK32);
        final int dd = (int) product;
        product = (product >>> Integer.SIZE) + xx * (c & MASK32);
        final int cc = (int) product;
        // Possible overflow here and bits are lost
        final long aabb = (product >>> Integer.SIZE) + xx * ab;
        return new UInt192(aabb, cc, dd, ee, ff);
    }

    /**
     * Subtracts the value.
     * Any overflow bits (negative result) are lost.
     *
     * @param x Value.
     * @return the difference
     */
    UInt192 subtract(UInt128 x) {
        // Difference with carry.
        // Subtract common part.
        long diff = (f & MASK32) - (x.lo32()  & MASK32);
        final int ff = (int) diff;
        diff = (diff >> Integer.SIZE) + (e & MASK32) - (x.mid32() & MASK32);
        final int ee = (int) diff;
        diff = (diff >> Integer.SIZE) + (d & MASK32) - (x.hi64() & MASK32);
        final int dd = (int) diff;
        diff = (diff >> Integer.SIZE) + (c & MASK32) - (x.hi64() >>> Integer.SIZE);
        final int cc = (int) diff;
        // Possible overflow here and bits are lost containing info on the
        // magnitude of the true negative value
        final long aabb = (diff >> Integer.SIZE) + ab;
        return new UInt192(aabb, cc, dd, ee, ff);
    }

    /**
     * Convert to a BigInteger.
     *
     * @return the value
     */
    BigInteger toBigInteger() {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES * 6)
            .putLong(ab)
            .putInt(c)
            .putInt(d)
            .putInt(e)
            .putInt(f);
        // Sign is always positive. This works for zero.
        return new BigInteger(1, bb.array());
    }

    /**
     * Convert to a double.
     *
     * @return the value
     */
    double toDouble() {
        final long h = hi64();
        final long m = mid64();
        final long l = lo64();
        if (h == 0) {
            return IntMath.uint128ToDouble(m, l);
        }
        // For correct rounding we use a sticky bit to represent magnitude
        // lost from the low 64-bits. The result is scaled by 2^64.
        return IntMath.uint128ToDouble(h, m | ((l == 0) ? 0 : 1)) * 0x1.0p64;
    }

    /**
     * Convert to an {@code int}; throwing an exception if the value overflows an {@code int}.
     *
     * @return the value
     * @throws ArithmeticException if the value overflows an {@code int}.
     * @see Math#toIntExact(long)
     */
    int toIntExact() {
        return Math.toIntExact(toLongExact());
    }

    /**
     * Convert to a {@code long}; throwing an exception if the value overflows a {@code long}.
     *
     * @return the value
     * @throws ArithmeticException if the value overflows a {@code long}.
     */
    long toLongExact() {
        // Test if we have more than 63-bits
        if ((ab | c | d) != 0 || e < 0) {
            throw new ArithmeticException("long integer overflow");
        }
        return lo64();
    }

    /**
     * Return the lower 64-bits as a {@code long} value.
     *
     * @return the low 64-bits
     */
    long lo64() {
        return (f & MASK32) | ((e & MASK32) << Integer.SIZE);
    }

    /**
     * Return the middle 64-bits as a {@code long} value.
     *
     * @return bits 128-65
     */
    long mid64() {
        return (d & MASK32) | ((c & MASK32) << Integer.SIZE);
    }

    /**
     * Return the higher 64-bits as a {@code long} value.
     *
     * @return bits 192-129
     */
    long hi64() {
        return ab;
    }
}
