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

/**
 * Support class for interpolation.
 *
 * @since 1.1
 */
final class Interpolation {
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
     * Linear interpolation between sorted values {@code a <= b} using the
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
}
