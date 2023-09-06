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

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Utility class which provides the data for tests in {o.a.c.s.descriptive} module.
 */
final class TestData {

    /** Class contains only static methods. */
    private TestData() {}

    /**
     * Function which supplies data to test the <code>accept()</code> and <code>of()</code> methods.
     * @return Stream of 1-d arrays.
     */
    static Stream<double[]> testValues() {
        return Stream.of(
            new double[] {0.0},
            new double[] {10, 8, 13, 9, 11, 14, 6, 4, 12, 7, 5},
            new double[] {8.04, 6.95, 7.58, 8.81, 8.33, 9.96, 7.24, 4.26, 10.84, 4.82, 5.68},
            new double[] {9.14, 8.14, 8.74, 8.77, 9.26, 8.10, 6.13, 3.10, 9.13, 7.26, 4.74, 7.46, 6.77, 12.74, 7.11, 7.81, 8.84, 6.08, 5.39, 8.15, 6.42, 5.73},
            new double[] {8, 8, 8, 8, 8, 8, 8, 19, 8, 8, 8},
            new double[] {6.58, 5.76, 7.71, 8.84, 8.47, 7.04, 5.25, 12.50, 5.56, 7.91, 6.89},
            new double[] {0, 0, 0.0},
            new double[] {1, -7, 6},
            new double[] {1, 7, -15, 3},
            new double[] {2, 2, 2, 2},
            new double[] {2.3},
            new double[] {3.14, 2.718, 1.414},
            new double[] {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0, 8.2, 10.3, 11.3, 14.1, 9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0, 8.8, 9.0, 12.3},
            new double[] {-0.0, +0.0},
            new double[] {0.0, -0.0},
            new double[] {0.0, +0.0},
            new double[] {0.001, 0.0002, 0.00003, 10000.11, 0.000004},
            new double[] {10E-50, 5E-100, 25E-200, 35.345E-50},
            // Overflow of the sum
            new double[] {Double.MAX_VALUE, Double.MAX_VALUE},
            new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE},
            new double[] {Double.MAX_VALUE, 1},
            new double[] {-Double.MAX_VALUE, 1, 1},
            new double[] {-Double.MAX_VALUE, -1, 1},
            new double[] {Double.MAX_VALUE, -1},
            new double[] {Double.MAX_VALUE, -Double.MAX_VALUE},
            new double[] {1, -Double.MAX_VALUE},
            new double[] {1, 1, 1, -Double.MAX_VALUE},
            new double[] {Double.MAX_VALUE, Double.MAX_VALUE / 2},
            new double[] {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE},
            new double[] {Double.MAX_VALUE, -Double.MAX_VALUE}
        );
    }

    /**
     * Function which supplies data with non-finite values to test the <code>accept()</code> and <code>of()</code> methods.
     * @return Stream of 1-d arrays.
     */
    static Stream<double[]> testValuesNonFinite() {
        return Stream.of(
            new double[]{},
            new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
            new double[]{Double.NaN, 34.56, 89.74},
            new double[]{34.56, Double.NaN, 89.74},
            new double[]{34.56, 89.74, Double.NaN},
            new double[]{Double.NaN, 3.14, Double.NaN, Double.NaN},
            new double[]{Double.NaN, Double.NaN, Double.NaN},
            new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
            new double[]{Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
            new double[]{Double.POSITIVE_INFINITY, Double.MAX_VALUE},
            new double[]{Double.NEGATIVE_INFINITY, -Double.MIN_VALUE},
            new double[]{Double.NEGATIVE_INFINITY, Double.MAX_VALUE},
            new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
            new double[]{-Double.MAX_VALUE, Double.POSITIVE_INFINITY}
        );
    }

    /**
     * Function which supplies data to test the <code>combine()</code> method.
     * @return Stream of 1-d arrays.
     */
    static Stream<Arguments> testCombine() {
        return Stream.of(
            Arguments.of(new double[] {}, new double[] {1}),
            Arguments.of(new double[] {1}, new double[] {}),
            Arguments.of(new double[] {}, new double[] {1, 7, -15, 3}),
            Arguments.of(new double[] {0}, new double[] {0, 0.0}),
            Arguments.of(new double[] {4, 8, -6, 3, 18}, new double[] {1, -7, 6}),
            Arguments.of(new double[] {10, 8, 13, 9, 11, 14, 6, 4, 12, 7, 5}, new double[] {8, 8, 8, 8, 8, 8, 8, 19, 8, 8, 8}),
            Arguments.of(new double[] {10, 8, 13, 9, 11, 14, 6, 4, 12, 7, 5}, new double[] {7.46, 6.77, 12.74, 7.11, 7.81, 8.84, 6.08, 5.39, 8.15, 6.42, 5.73}),
            Arguments.of(new double[] {6.0, -1.32, -5.78, 8.967, 13.32, -9.67, 0.14, 7.321, 11.456, -3.111}, new double[] {2, 2, 2, 2}),
            Arguments.of(new double[] {2.3}, new double[] {-42, 10, -88, 5, -17}),
            Arguments.of(new double[] {-20, 34.983, -12.745, 28.12, -8.34, 42, -4, 16}, new double[] {3.14, 2.718, 1.414}),
            Arguments.of(new double[] {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0, 8.2, 10.3, 11.3, 14.1, 9.9}, new double[] {12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0, 8.8, 9.0, 12.3}),
            Arguments.of(new double[] {-0.0}, new double[] {+0.0}),
            Arguments.of(new double[] {0.0}, new double[] {-0.0}),
            Arguments.of(new double[] {0.0}, new double[] {+0.0}),
            Arguments.of(new double[] {10E-50, 5E-100}, new double[] {25E-200, 35.345E-50}),
            Arguments.of(new double[] {Double.MAX_VALUE}, new double[] {Double.MAX_VALUE}),
            Arguments.of(new double[] {-Double.MAX_VALUE}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {-Double.MAX_VALUE, 1}, new double[] {1}),
            Arguments.of(new double[] {Double.MAX_VALUE, 3.1415E153}, new double[] {}),
            Arguments.of(new double[] {Double.MAX_VALUE}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {1}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {1, 1, 1}, new double[] {-Double.MAX_VALUE}),
            Arguments.of(new double[] {Double.MAX_VALUE}, new double[] {1, 1E300})
        );
    }

    /**
     * Function which supplies data with non-finite values to test the <code>combine()</code> method.
     * @return Stream of 2-d arrays.
     */
    static Stream<double[][]> testCombineNonFinite() {
        return Stream.of(
            new double[][] {{}, {}},
            new double[][] {{Double.POSITIVE_INFINITY}, {Double.NEGATIVE_INFINITY}},
            new double[][] {{Double.NaN, 34.56, 89.74}, {Double.NaN}},
            new double[][] {{34.56}, {Double.NaN, 89.74}},
            new double[][] {{34.56, 89.74}, {Double.NaN, Double.NaN}},
            new double[][] {{Double.NaN, 3.14, Double.NaN, Double.NaN}, {}},
            new double[][] {{Double.NaN, Double.NaN, Double.NaN}, {Double.NaN, Double.NaN, Double.NaN}},
            new double[][] {{Double.POSITIVE_INFINITY}, {Double.POSITIVE_INFINITY}},
            new double[][] {{Double.NEGATIVE_INFINITY}, {Double.NEGATIVE_INFINITY}},
            new double[][] {{Double.POSITIVE_INFINITY}, {Double.MAX_VALUE}},
            new double[][] {{-Double.MAX_VALUE}, {Double.POSITIVE_INFINITY}},
            new double[][] {{Double.NEGATIVE_INFINITY}, {-Double.MIN_VALUE}},
            new double[][] {{Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -Double.MIN_VALUE},
                {Double.MAX_VALUE, Double.MIN_VALUE}}
        );
    }
}
