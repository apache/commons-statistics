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

import java.util.EnumSet;

/**
 * Argument validation methods.
 *
 * @since 1.1
 */
final class Arguments {
    /** Two. */
    private static final int TWO = 2;

    /** No instances. */
    private Arguments() {}

    /**
     * Check the significance level is in the correct range.
     *
     * @param alpha Significance level of the test.
     * @throws IllegalArgumentException if {@code alpha} is not in the range
     * {@code (0, 0.5]}
     */
    static void checkSignificance(double alpha) {
        if (alpha > 0 && alpha <= 0.5) {
            return;
        }
        // Not in (0, 0.5], or NaN
        throw new InferenceException(InferenceException.INVALID_SIGNIFICANCE, alpha);
    }

    /**
     * Check that the value is {@code >= 0}.
     *
     * @param v Value to be tested.
     * @return the value
     * @throws IllegalArgumentException if the value is less than 0.
     */
    static int checkNonNegative(int v) {
        if (v < 0) {
            throw new InferenceException(InferenceException.NEGATIVE, v);
        }
        return v;
    }

    /**
     * Check that the value is {@code >= 0}.
     *
     * @param v Value to be tested.
     * @throws IllegalArgumentException if the value is less than 0.
     */
    static void checkNonNegative(double v) {
        if (v >= 0) {
            return;
        }
        // Negative, or NaN
        throw new InferenceException(InferenceException.NEGATIVE, v);
    }

    /**
     * Check that all values are {@code >= 0}.
     *
     * @param values Values to be tested.
     * @throws IllegalArgumentException if any values are less than 0.
     */
    static void checkNonNegative(long[] values) {
        for (final long v : values) {
            if (v < 0) {
                throw new InferenceException(InferenceException.NEGATIVE, v);
            }
        }
    }

    /**
     * Check that all values are {@code >= 0}.
     *
     * @param values Values to be tested.
     * @throws IllegalArgumentException if any values are less than 0.
     */
    static void checkNonNegative(long[][] values) {
        for (final long[] v : values) {
            checkNonNegative(v);
        }
    }

    /**
     * Check that value is {@code > 0}.
     *
     * @param v Value to be tested.
     * @return the value
     * @throws IllegalArgumentException if the value is not strictly positive.
     */
    static int checkStrictlyPositive(int v) {
        if (v <= 0) {
            throw new InferenceException(InferenceException.NOT_STRICTLY_POSITIVE, v);
        }
        return v;
    }

    /**
     * Check that value is {@code > 0}.
     *
     * @param v Value to be tested.
     * @return the value
     * @throws IllegalArgumentException if the value is not strictly positive.
     */
    static double checkStrictlyPositive(double v) {
        if (v > 0) {
            return v;
        }
        // not positive or NaN
        throw new InferenceException(InferenceException.NOT_STRICTLY_POSITIVE, v);
    }

    /**
     * Check that all values are {@code > 0}.
     *
     * @param values Values to be tested.
     * @throws IllegalArgumentException if any values are not strictly positive.
     */
    static void checkStrictlyPositive(double[] values) {
        for (final double v : values) {
            // Logic negation detects NaN
            if (!(v > 0)) {
                throw new InferenceException(InferenceException.NOT_STRICTLY_POSITIVE, v);
            }
        }
    }

    /**
     * Check that the value is finite.
     *
     * @param v Value to be tested.
     * @return the value
     * @throws IllegalArgumentException if the value is not finite.
     */
    static double checkFinite(double v) {
        if (!Double.isFinite(v)) {
            throw new InferenceException("Non-finite input value: " + v);
        }
        return v;
    }

    /**
     * Check that all values are not {@link Double#NaN}.
     *
     * @param values Values to be tested.
     * @throws IllegalArgumentException if any values are NaN.
     */
    static void checkNonNaN(double[] values) {
        for (final double v : values) {
            if (Double.isNaN(v)) {
                throw new InferenceException("NaN input value");
            }
        }
    }

    /**
     * Checks if the input array is rectangular. It is assumed the array is non-null
     * and has a non-zero length.
     *
     * @param array Array to be tested.
     * @throws NullPointerException if input array is null
     * @throws IndexOutOfBoundsException if input array is zero length
     * @throws IllegalArgumentException if input array is not rectangular
     */
    static void checkRectangular(long[][] array) {
        final int first = array[0].length;
        for (int i = 1; i < array.length; i++) {
            if (array[i].length != first) {
                throw new InferenceException(InferenceException.NOT_RECTANGULAR, array[i].length, first);
            }
        }
    }

    /**
     * Check the values size is the minimum required, {@code size >= required}.
     *
     * @param size Values size.
     * @param required Required size.
     * @throws IllegalArgumentException if {@code size < required}
     */
    static void checkValuesRequiredSize(int size, int required) {
        if (size < required) {
            throw new InferenceException(InferenceException.VALUES_REQUIRED, size, required);
        }
    }

    /**
     * Check the categories size is the minimum required, {@code size >= required}.
     *
     * @param size Values size.
     * @param required Required size.
     * @throws IllegalArgumentException if {@code size < required}
     */
    static void checkCategoriesRequiredSize(int size, int required) {
        if (size < required) {
            throw new InferenceException(InferenceException.CATEGORIES_REQUIRED, size, required);
        }
    }

    /**
     * Check the values sizes are equal, {@code size1 == size2}.
     *
     * @param size1 First size.
     * @param size2 Second size.
     * @throws IllegalArgumentException if {@code size1 != size2}
     */
    static void checkValuesSizeMatch(int size1, int size2) {
        if (size1 != size2) {
            throw new InferenceException(InferenceException.VALUES_MISMATCH, size1, size2);
        }
    }

    /**
     * Check the option is allowed.
     *
     * @param <E> Option type.
     * @param v Option value.
     * @param allowed Allowed options.
     * @return the value
     * @throws IllegalArgumentException if the value is not in the allowed options or is null
     */
    static <E extends Enum<E>> E checkOption(E v, EnumSet<E> allowed) {
        if (!allowed.contains(v)) {
            throw new InferenceException("Invalid option: " + v);
        }
        return v;
    }

    /**
     * Check the input is a 2-by-2 contingency table.
     *
     * @param table Table.
     * @throws IllegalArgumentException if the {@code table} is not a 2-by-2 table; any
     * table entry is negative; or the sum is zero or is not an integer
     */
    static void checkTable(int[][] table) {
        if (table.length != TWO || table[0].length != TWO || table[1].length != TWO) {
            throw new InferenceException("Require a 2-by-2 contingency table");
        }
        // Must all be positive
        final int a = table[0][0];
        final int b = table[0][1];
        final int c = table[1][0];
        final int d = table[1][1];
        // Bitwise OR combines the sign bit from all values
        checkNonNegative(a | b | c | d);
        // Sum must be an integer
        final long sum = (long) a + b + c + d;
        if (sum > Integer.MAX_VALUE) {
            throw new InferenceException(InferenceException.X_GT_Y, sum, Integer.MAX_VALUE);
        }
        checkStrictlyPositive((int) sum);
    }
}
