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
package org.apache.commons.statistics.inference;

import java.util.function.IntToDoubleFunction;

/**
 * Search utility methods.
 *
 * @since 1.1
 */
final class Searches {
    /** Range threshold to use a binary search.
     * The binary search takes O(log(n)) so is used when n is large and a sequential
     * search is slower. */
    private static final int BINARY_SEARCH = 8;

    /** No instances. */
    private Searches() {}

    /**
     * Conduct a search between {@code a} inclusive and {@code b} inclusive
     * to find the lowest index where {@code value <= x}. The values must be
     * in <em>descending</em> order. The method is functionally equivalent to:
     * <pre>
     * {@code
     * i = b + 1
     * while (i > a AND value(i - 1) <= x)
     *    i = i - 1
     * return i
     * }</pre>
     *
     * <p>The function is only evaluated between the closed interval {@code [a, b]}.
     * Special cases:
     * <ul>
     * <li>If {@code value(a) <= x} the returned index is {@code a}.
     * <li>If {@code value(b) > x} the returned index is {@code b + 1}.
     * </ul>
     *
     * @param a Lower limit (inclusive).
     * @param b Upper limit (inclusive).
     * @param x Target value.
     * @param value Function to evaluate the value at an index.
     * @return the minimum index where {@code value(i) <= x}.
     */
    static int searchDescending(int a, int b, double x, IntToDoubleFunction value) {
        // Re-use the search for ascending order.
        // Invert the index to find the lowest for the descending order.
        final int offset = a + b;
        return offset - searchAscending(a, b, x, i -> value.applyAsDouble(offset - i));
    }

    /**
     * Conduct a search between {@code a} inclusive and {@code b} inclusive
     * to find the highest index where {@code value <= x}. The values must be
     * in <em>ascending</em> order. The method is functionally equivalent to:
     * <pre>
     * {@code
     * i = a - 1
     * while (i < b AND value(i + 1) <= x)
     *    i = i + 1
     * return i
     * }</pre>
     *
     * <p>The function is only evaluated between the closed interval {@code [a, b]}.
     * Special cases:
     * <ul>
     * <li>If {@code value(a) > x} the returned index is {@code a - 1}.
     * <li>If {@code value(b) <= x} the returned index is {@code b}.
     * </ul>
     *
     * @param a Lower limit (inclusive).
     * @param b Upper limit (inclusive).
     * @param x Target value.
     * @param value Function to evaluate the value at an index.
     * @return the maximum index where {@code value(i) <= x}.
     */
    static int searchAscending(int a, int b, double x, IntToDoubleFunction value) {
        // Use a binary search for a large range.
        if (b - a > BINARY_SEARCH) {
            // Edge case as the search never evaluates the end points.
            if (value.applyAsDouble(a) > x) {
                return a - 1;
            }
            if (value.applyAsDouble(b) <= x) {
                return b;
            }

            // value(lo) is always <= x
            // value(hi) is always > x
            int lo = a;
            int hi = b;
            while (lo + 1 < hi) {
                final int mid = (lo + hi) >>> 1;
                if (value.applyAsDouble(mid) <= x) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            return lo;
        }

        // Sequential search
        int i = a - 1;
        // Evaluate between [a, b]
        while (i < b && value.applyAsDouble(i + 1) <= x) {
            i++;
        }
        return i;
    }
}
