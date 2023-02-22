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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link Arguments}.
 */
class ArgumentsTest {

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.5000000000000001, 1, -1, -Double.MIN_VALUE, Double.NaN})
    void testCheckSignificanceThrows(double alpha) {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkSignificance(alpha));
        Assertions.assertTrue(ex.getMessage().contains(Double.toString(alpha)));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1})
    void testCheckNonNegativeIntThrows(double v) {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkNonNegative(v));
        Assertions.assertTrue(ex.getMessage().contains(Double.toString(v)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-Double.MIN_VALUE, -1, Double.NEGATIVE_INFINITY, Double.NaN})
    void testCheckNonNegativeDoubleThrows(double v) {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkNonNegative(v));
        Assertions.assertTrue(ex.getMessage().contains(Double.toString(v)));
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1})
    void testCheckNonNegativeLongArrayThrows(long v) {
        final long[] a = {v};
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkNonNegative(a));
        Assertions.assertTrue(ex.getMessage().contains(Long.toString(v)));
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1})
    void testCheckNonNegativeLongArrayArrayThrows(long v) {
        final long[][] a = {{v}};
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkNonNegative(a));
        Assertions.assertTrue(ex.getMessage().contains(Long.toString(v)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void testCheckStrictlyPositiveIntThrows(int v) {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkStrictlyPositive(v));
        Assertions.assertTrue(ex.getMessage().contains(Integer.toString(v)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NEGATIVE_INFINITY, Double.NaN})
    void testCheckStrictlyPositiveDoubleArrayThrows(double v) {
        final double[] a = {v};
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkStrictlyPositive(a));
        Assertions.assertTrue(ex.getMessage().contains(Double.toString(v)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY})
    void testCheckFiniteThrows(double v) {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkFinite(v));
        Assertions.assertTrue(ex.getMessage().contains(Double.toString(v)));
    }

    @Test
    void testCheckNonNanArrayThrows() {
        Assertions.assertDoesNotThrow(() -> Arguments.checkNonNaN(new double[0]));
        final double[] a = new double[3];
        Assertions.assertDoesNotThrow(() -> Arguments.checkNonNaN(a));
        for (int i = 0; i < a.length; i++) {
            a[i] = Double.NaN;
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Arguments.checkNonNaN(a));
            Assertions.assertTrue(ex.getMessage().contains("NaN"));
            a[i] = 0;
        }
    }

    @Test
    void testCheckRectangular() {
        // Input is assumed to be non-zero length: this test what happens
        Assertions.assertThrows(NullPointerException.class,
            () -> Arguments.checkRectangular(null));
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> Arguments.checkRectangular(new long[0][0]));
        Arguments.checkRectangular(new long[1][0]);
        Arguments.checkRectangular(new long[1][1]);
        Arguments.checkRectangular(new long[1][2]);
        Arguments.checkRectangular(new long[2][0]);
        Arguments.checkRectangular(new long[2][1]);
        Arguments.checkRectangular(new long[2][2]);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkRectangular(new long[][] {{0}, {}}));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkRectangular(new long[][] {{0, 0}, {}}));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkRectangular(new long[][] {{0, 0}, {0}}));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.checkRectangular(new long[][] {{0, 0}, {0, 0, 0}}));
    }

    @Test
    void testCheckValuesRequiredSize() {
        Arguments.checkValuesRequiredSize(1, 1);
        Arguments.checkValuesRequiredSize(10, 2);
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> Arguments.checkValuesRequiredSize(0, 1), "values", "0", "1");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> Arguments.checkValuesRequiredSize(1, 2), "values", "1", "2");
    }

    @Test
    void testCheckCategoriesRequiredSize() {
        Arguments.checkCategoriesRequiredSize(1, 1);
        Arguments.checkCategoriesRequiredSize(10, 2);
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> Arguments.checkCategoriesRequiredSize(0, 1), "categories", "0", "1");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> Arguments.checkCategoriesRequiredSize(1, 2), "categories", "1", "2");
    }

    @Test
    void testCheckValuesSizeMatch() {
        Arguments.checkValuesSizeMatch(1, 1);
        Arguments.checkValuesSizeMatch(10, 10);
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> Arguments.checkValuesSizeMatch(0, 1), "values", "mismatch", "0", "1");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> Arguments.checkValuesSizeMatch(3, 2), "values", "mismatch", "3", "2");
    }

    @Test
    void testCheckAllowed() {
        final EnumSet<PValueMethod> allowed = EnumSet.of(PValueMethod.ASYMPTOTIC, PValueMethod.EXACT);
        allowed.forEach(v -> Assertions.assertEquals(v, Arguments.checkOption(v, allowed)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Arguments.checkOption(null, allowed),
            "Do not raise a NPE for null. The EnumSet handles this as false in contains(null)");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> Arguments.checkOption(PValueMethod.ESTIMATE, allowed), "invalid", "option", PValueMethod.ESTIMATE.toString());
    }

    @Test
    void testCheckTableInvalid2x2Throws() {
        Assertions.assertThrows(NullPointerException.class, () -> Arguments.checkTable(null));
        // Non 2-by-2 input
        Assertions.assertThrows(IllegalArgumentException.class, () -> Arguments.checkTable(new int[3][3]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Arguments.checkTable(new int[2][1]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Arguments.checkTable(new int[1][2]));
        // Non-square input
        final int[][] x = {{1, 2}, {3}};
        Assertions.assertThrows(IllegalArgumentException.class, () -> Arguments.checkTable(x));
        final int[][] y = {{1}, {2, 3}};
        Assertions.assertThrows(IllegalArgumentException.class, () -> Arguments.checkTable(y));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 0",
        // Overflow
        "2147483647, 1, 0, 0",
        "2147483647, 0, 1, 0",
        "2147483647, 0, 0, 1",
        "2147483647, 2147483647, 0, 0",
        "2147483647, 0, 2147483647, 0",
        "2147483647, 0, 0, 2147483647",
        "2147483647, 0, 2147483647, 2147483647",
        "2147483647, 2147483647, 0, 2147483647",
        "2147483647, 2147483647, 2147483647, 2147483647",
    })
    void testCheckTableInvalidSumThrows(int a, int b, int c, int d) {
        final int[][] table = {{a, b}, {c, d}};
        Assertions.assertThrows(IllegalArgumentException.class, () -> Arguments.checkTable(table));
    }
}
