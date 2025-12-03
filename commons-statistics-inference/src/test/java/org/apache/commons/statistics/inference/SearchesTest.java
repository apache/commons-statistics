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

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link Searches}.
 */
class SearchesTest {

    @Test
    void testSearchDescendingSequential() {
        // Array should be small enough to not trigger the binary search.
        final double[] values = {4, 3, 2, 2, 1};
        final int a = 0;
        final int b = values.length - 1;
        final IntToDoubleFunction value = i -> i >= a && i <= b ? values[i] : -1;
        Assertions.assertEquals(a, Searches.searchDescending(a, b, 10, value));
        Assertions.assertEquals(0, Searches.searchDescending(a, b, 4, value));
        Assertions.assertEquals(1, Searches.searchDescending(a, b, 3, value));
        Assertions.assertEquals(2, Searches.searchDescending(a, b, 2, value));
        Assertions.assertEquals(4, Searches.searchDescending(a, b, 1, value));
        Assertions.assertEquals(b + 1, Searches.searchDescending(a, b, -5, value));
    }

    @Test
    void testSearchDescendingBinary() {
        // Array should be large enough to trigger the binary search.
        final double[] values = {11, 10, 9, 8, 8, 6, 5, 4, 2, 2, 1};
        final int a = 0;
        final int b = values.length - 1;
        final IntToDoubleFunction value = i -> i >= a && i <= b ? values[i] : -1;
        Assertions.assertEquals(a, Searches.searchDescending(a, b, 20, value));
        Assertions.assertEquals(0, Searches.searchDescending(a, b, 11, value));
        Assertions.assertEquals(1, Searches.searchDescending(a, b, 10, value));
        Assertions.assertEquals(2, Searches.searchDescending(a, b, 9, value));
        Assertions.assertEquals(3, Searches.searchDescending(a, b, 8, value));
        Assertions.assertEquals(5, Searches.searchDescending(a, b, 7, value));
        Assertions.assertEquals(5, Searches.searchDescending(a, b, 6, value));
        Assertions.assertEquals(6, Searches.searchDescending(a, b, 5, value));
        Assertions.assertEquals(7, Searches.searchDescending(a, b, 4, value));
        Assertions.assertEquals(8, Searches.searchDescending(a, b, 3, value));
        Assertions.assertEquals(8, Searches.searchDescending(a, b, 2, value));
        Assertions.assertEquals(10, Searches.searchDescending(a, b, 1, value));
        Assertions.assertEquals(b + 1, Searches.searchDescending(a, b, -5, value));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSearch"})
    void testSearchDescending(int length, int unique) {
        final double[] values = createValues(length, unique, true);
        final int a = 0;
        final int b = values.length - 1;
        final IntToDoubleFunction value = i -> i >= a && i <= b ? values[i] : -1;
        // Test the ends
        Assertions.assertEquals(a, Searches.searchDescending(a, b, values[0] + 10, value));
        Assertions.assertEquals(a, Searches.searchDescending(a, b, values[0] + 1, value));
        Assertions.assertEquals(a, Searches.searchDescending(a, b, values[0], value));
        Assertions.assertEquals(b + 1, Searches.searchDescending(a, b, values[b] - 1, value));
        Assertions.assertEquals(b + 1, Searches.searchDescending(a, b, values[b] - 10, value));
        // Test inside the range
        for (int i = 1; i <= b; i++) {
            // Test at each new smaller value
            // It should find the lowest index where value <= x
            if (values[i - 1] != values[i]) {
                Assertions.assertTrue(i > Searches.searchDescending(a, b, values[i - 1], value),
                        "Previous higher value should have lower index");
                Assertions.assertEquals(i, Searches.searchDescending(a, b, values[i], value),
                        "Lowest index for value");
            }
        }
    }

    @Test
    void testSearchAscendingSequential() {
        // Array should be small enough to not trigger the binary search.
        final double[] values = {0, 1, 2, 4, 4};
        final int a = 0;
        final int b = values.length - 1;
        final IntToDoubleFunction value = i -> i >= a && i <= b ? values[i] : -1;
        Assertions.assertEquals(a - 1, Searches.searchAscending(a, b, -5, value));
        Assertions.assertEquals(0, Searches.searchAscending(a, b, 0, value));
        Assertions.assertEquals(1, Searches.searchAscending(a, b, 1, value));
        Assertions.assertEquals(2, Searches.searchAscending(a, b, 2, value));
        Assertions.assertEquals(2, Searches.searchAscending(a, b, 3, value));
        Assertions.assertEquals(4, Searches.searchAscending(a, b, 4, value));
        Assertions.assertEquals(b, Searches.searchAscending(a, b, 10, value));
    }

    @Test
    void testSearchAscendingBinary() {
        // Array should be large enough to trigger the binary search.
        final double[] values = {0, 1, 2, 4, 4, 5, 6, 8, 8, 9, 11, 11};
        final int a = 0;
        final int b = values.length - 1;
        final IntToDoubleFunction value = i -> i >= a && i <= b ? values[i] : -1;
        Assertions.assertEquals(a - 1, Searches.searchAscending(a, b, -5, value));
        Assertions.assertEquals(0, Searches.searchAscending(a, b, 0, value));
        Assertions.assertEquals(1, Searches.searchAscending(a, b, 1, value));
        Assertions.assertEquals(2, Searches.searchAscending(a, b, 2, value));
        Assertions.assertEquals(2, Searches.searchAscending(a, b, 3, value));
        Assertions.assertEquals(4, Searches.searchAscending(a, b, 4, value));
        Assertions.assertEquals(5, Searches.searchAscending(a, b, 5, value));
        Assertions.assertEquals(6, Searches.searchAscending(a, b, 6, value));
        Assertions.assertEquals(6, Searches.searchAscending(a, b, 7, value));
        Assertions.assertEquals(8, Searches.searchAscending(a, b, 8, value));
        Assertions.assertEquals(9, Searches.searchAscending(a, b, 9, value));
        Assertions.assertEquals(9, Searches.searchAscending(a, b, 10, value));
        Assertions.assertEquals(11, Searches.searchAscending(a, b, 11, value));
        Assertions.assertEquals(b, Searches.searchAscending(a, b, 20, value));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSearch"})
    void testSearchAscending(int length, int unique) {
        final double[] values = createValues(length, unique, false);
        final int a = 0;
        final int b = values.length - 1;
        final IntToDoubleFunction value = i -> i >= a && i <= b ? values[i] : -1;
        // Test the ends
        Assertions.assertEquals(a - 1, Searches.searchAscending(a, b, values[0] - 10, value));
        Assertions.assertEquals(a - 1, Searches.searchAscending(a, b, values[0] - 1, value));
        Assertions.assertEquals(b, Searches.searchAscending(a, b, values[b], value));
        Assertions.assertEquals(b, Searches.searchAscending(a, b, values[b] + 1, value));
        Assertions.assertEquals(b, Searches.searchAscending(a, b, values[b] + 10, value));
        // Test inside the range
        for (int i = 0; i < b; i++) {
            // Test at each new larger value
            // It should find the highest index where value <= x
            if (values[i] != values[i + 1]) {
                Assertions.assertEquals(i, Searches.searchAscending(a, b, values[i], value),
                        "Highest index for value");
                Assertions.assertTrue(i < Searches.searchAscending(a, b, values[i + 1], value),
                        "Next higher value should have higher index");
            }
        }
    }

    static Stream<Arguments> testSearch() {
        return Stream.of(
            Arguments.of(4, 4),
            Arguments.of(4, 2),
            Arguments.of(4, 1),
            Arguments.of(20, 20),
            Arguments.of(20, 10),
            Arguments.of(20, 1),
            Arguments.of(1000, 1000),
            Arguments.of(1000, 123),
            Arguments.of(1000, 1)
        );
    }

    /**
     * Creates an array of ascending or descending values of the specified length
     * with a number of duplicates.
     *
     * @param length Length.
     * @param n Count of unique values.
     * @param descending Set to true for a descending set of values.
     * @return the values
     */
    private static double[] createValues(int length, int n, boolean descending) {
        final double[] values = new double[length];
        final double offset = ThreadLocalRandom.current().nextDouble(7, 13);
        for (int i = 0; i < n; i++) {
            values[i] = offset + i;
        }
        for (int i = n; i < length; i++) {
            values[i] = offset + ThreadLocalRandom.current().nextInt(n);
        }
        Arrays.sort(values);
        if (descending) {
            // Reverse the array
            for (int i = 0, j = length - 1; i < j; i++, j--) {
                final double x = values[j];
                values[j] = values[i];
                values[i] = x;
            }
        }
        return values;
    }
}
