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

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Utility class which provides the data for tests in {o.a.c.s.descriptive} module.
 */
final class TestData {

    /** Class contains only static methods. */
    private TestData() {}

    /**
     * A stream of test data for the moments. This is provided so all moment
     * statistics based on the mean (first raw moment) can use the same cases.
     *
     * @return the stream of test data
     */
    static Stream<double[]> momentTestData() {
        final double max = Double.MAX_VALUE;
        return Stream.of(
            // Large numbers
            new double[] {10E-50, 5E-100, 25E-200, 35.345E-50},
            // Small numbers
            new double[] {0.001, 0.0002, 0.00003, 10000.11, 0.000004},
            // Overflow of the sum which prevents using the sum for the mean}
            new double[] {max, max},
            new double[] {-max, -max},
            new double[] {max, max, max, max},
            new double[] {max, max / 2},
            new double[] {max, max, -max},
            new double[] {-max, -max / 2, -max / 4},
            // Extreme range
            new double[] {-max, 1, 1},
            // zeros
            new double[10]
        );
    }

    /**
     * Function which supplies test data for a statistic as a single array.
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
     * Function which supplies test data for a statistic as a single array.
     * Each case will contain at least one non-finite value.
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
     * Function which supplies test data for a statistic as a pair of double[] arrays.
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
     * Function which supplies test data for a statistic as a double[][] array.
     * Each case will contain at least one non-finite value.
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

    /**
     * Function which supplies test data for a statistic as a series of double[] arrays.
     * The data are samples from distributions that include uniform, normal, or skewed.
     * Since the data are random, each array in a series set should follow approximately
     * the same distribution. This reduces effects of limited floating-point precision
     * when combining statistics of sub-sets as the statistic values should be similar.
     *
     * @return Stream of a series of 1-d arrays.
     */
    static Stream<double[][]> testMultiCombine() {
        Stream.Builder<double[][]> builder = Stream.builder();
        final int[][] sizes = {
            {50, 50},
            {10, 90},
            {10, 20, 70},
            {10, 20, 50, 20},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
        };
        // Samples using SciPy: import scipy.stats
        // Allow sample data to be pasted directly from SciPy output
        // CHECKSTYLE: stop NoWhitespaceBefore
        // CHECKSTYLE: stop Indentation
        // Uniform samples: scipy.stats.randint.rvs(-15, 25, size=100)
        // https://en.wikipedia.org/wiki/Discrete_uniform_distribution
        final double[] v1 = {
            20,   8,   1,   2,  24, -13,  -1,  19,   0,  23,  14,   2,  -9,
            0,   7, -15,  -2,  -9,  -1,  12, -12,   9,   2,  12,   0,   5,
           18,  15, -10,   6,  14,   6,  24,  12,  -2,   1,  -9,  17,  10,
            7,   4,  -8,   7,   7,  -9,  13, -13,   0, -13,   6,  10,  -6,
           23,   2,   6, -10,  12,   6,  -6,   4,  -5,  -9,   5,  16, -14,
           -4,  22,  -4,  20,  12, -13,   6, -13,  12,  -3,  13,   1,  -6,
           13, -14,  -8,   4,  24,  10,  22,   8,  -3,  -5,   6,   9,   2,
           16,  20,  -8,  -5,  23,  19,  20, -10,  21};
        Arrays.stream(sizes).forEach(x -> builder.add(cut(v1, x)));
        // Poisson samples: scipy.stats.poisson.rvs(1.5, size=100)
        // https://en.wikipedia.org/wiki/Poisson_distribution
        final double[] v2 = {
            3, 0, 3, 4, 0, 1, 1, 3, 2, 1, 3, 1, 5, 0, 0, 0, 3, 3, 2, 0, 3, 1,
            2, 1, 1, 0, 0, 1, 0, 1, 3, 0, 0, 2, 2, 0, 2, 1, 2, 1, 3, 2, 1, 2,
            0, 2, 2, 1, 2, 0, 3, 2, 0, 1, 2, 2, 1, 2, 1, 0, 2, 0, 2, 2, 3, 2,
            1, 1, 0, 1, 2, 1, 0, 0, 6, 2, 0, 1, 1, 1, 1, 2, 2, 2, 2, 0, 2, 1,
            2, 4, 2, 4, 2, 0, 0, 1, 1, 2, 2, 3};
        Arrays.stream(sizes).forEach(x -> builder.add(cut(v2, x)));
        // Poisson samples: scipy.stats.poisson.rvs(4.5, size=100)
        final double[] v3 = {
            1,  5,  5,  2,  1,  7,  5,  5,  3,  5,  5,  2,  4,  4,  6,  2,  7,
            5,  3,  6,  7,  3,  5,  3,  7,  6,  4,  3,  3,  3,  3,  3,  5,  3,
            2,  8,  2,  4,  3,  1,  2,  5,  4,  3,  5,  4,  4,  8,  6,  2,  4,
            3,  5,  5,  6,  3,  1,  6,  8,  6,  3,  6, 10,  4,  5,  2,  4,  2,
            2,  8,  2,  2,  4,  1,  4,  2,  5,  3,  2,  4,  4,  6,  9,  4,  5,
            9,  9,  6,  3,  5,  3,  3,  5,  7,  5,  2,  3,  7,  4,  5};
        Arrays.stream(sizes).forEach(x -> builder.add(cut(v3, x)));
        // Poisson samples: scipy.stats.poisson.rvs(45, size=100)
        final double[] v4 = {
            42, 51, 38, 38, 49, 48, 42, 47, 51, 46, 45, 35, 39, 42, 49, 55, 53,
            46, 49, 56, 42, 46, 42, 53, 43, 55, 49, 52, 51, 45, 40, 49, 39, 40,
            46, 43, 46, 48, 36, 44, 40, 49, 49, 43, 45, 44, 41, 55, 52, 45, 57,
            41, 43, 44, 38, 52, 44, 45, 43, 42, 38, 37, 47, 42, 47, 45, 70, 45,
            50, 47, 46, 50, 47, 35, 43, 52, 51, 41, 45, 42, 45, 53, 46, 48, 51,
            43, 63, 48, 49, 41, 58, 51, 59, 43, 39, 32, 35, 46, 50, 50};
        Arrays.stream(sizes).forEach(x -> builder.add(cut(v4, x)));
        // Normal samples: scipy.stats.norm.rvs(loc=3.4, scale=2.25, size=100)
        // https://en.wikipedia.org/wiki/Normal_distribution
        final double[] v5 = {
            1.06356579, -1.52552007,  7.09739891, -0.41516549,  0.17131653,
            0.77923148,  2.90491862,  4.12648256,  5.04920689,  4.20053484,
            5.83485097,  4.33138009,  4.18795702,  3.269289  ,  2.2399589 ,
            4.16551591, -1.67192439,  1.44919254,  3.52270229, -1.49186865,
           -0.30794835,  5.82394621,  4.84755567,  4.79622486,  5.12461983,
            2.62561931,  5.12457788,  8.24460895,  4.91249002,  3.75550863,
            4.35440479,  4.17587334, -0.34934393,  2.98071452, -1.35620308,
            1.93956508,  7.57171999,  5.41976186,  2.8427556 ,  3.04101193,
            2.20374721,  4.65406057,  5.76961878,  3.14412957,  7.60322297,
            1.598286  ,  2.51552974,  0.67767289,  0.76514432,  3.65663671,
            0.53116457,  2.79439061,  7.58564809,  4.16735822,  2.95210392,
            6.37867376,  6.57010411,  0.11837698,  9.16270054,  3.80097588,
            5.48811672,  3.83378268,  2.03669252,  5.34865676,  3.11338528,
            4.70088345,  6.00069684,  0.16144587,  4.22654482,  2.2722623 ,
            5.39142224,  0.811471  ,  2.74523433,  6.32457234,  0.73033045,
            9.54402353,  0.4800466 ,  2.00806359,  6.06115109,  2.3072464 ,
            5.40974674,  2.05533169,  0.97160161,  8.06915145,  4.40792026,
            4.53139251,  3.32350119,  1.53645238,  3.49059212,  3.57904997,
            0.58634639,  5.87567911,  3.49424866,  5.72228178,  4.41403447,
            1.27815121,  7.13861948,  4.68209093,  6.4598438 ,  0.66270586};
        Arrays.stream(sizes).forEach(x -> builder.add(cut(v5, x)));
        // Gamma samples: scipy.stats.gamma.rvs(9, scale=0.5, size=100)
        // https://en.wikipedia.org/wiki/Gamma_distribution
        final double[] v6 = {
            3.46479451, 8.80950045, 3.91437318, 4.23327834, 2.6910161 ,
            4.51122052, 5.81939474, 3.9142699 , 7.75537607, 6.06693317,
            3.29388792, 3.90689471, 3.26357137, 3.6398822 , 5.60048428,
            3.68248997, 5.09297897, 4.6302593 , 7.01654777, 4.2244833 ,
            2.75326355, 5.36988549, 2.88392811, 3.50131464, 4.81183009,
            4.92155284, 4.37061644, 3.8064197 , 3.31941113, 5.01257676,
            3.48037207, 2.62777255, 6.2447332 , 6.18425783, 3.06915179,
            6.42851381, 3.8969583 , 3.48723372, 3.49516941, 2.90404439,
            2.25920041, 3.68515649, 5.09607663, 3.18984299, 2.49261713,
            3.9345895 , 6.01480539, 8.8065787 , 4.3464082 , 5.03522483,
            4.05315513, 6.07365399, 4.34804323, 5.16061656, 3.24706079,
            2.89888437, 6.24575902, 3.10893227, 3.74196045, 3.94099137,
            3.33951846, 4.9264514 , 5.21935748, 5.06904776, 2.77543623,
            3.72451685, 6.35546017, 4.42425655, 5.99568005, 4.2602446 ,
            3.75834066, 5.17730802, 3.60682583, 4.09703419, 5.59942582,
            3.49191032, 3.02164323, 8.85183547, 5.58631958, 3.24891648,
            4.03267796, 4.30984912, 3.78187375, 5.98969913, 1.68855026,
            8.43117397, 3.808258  , 4.82043745, 2.91020117, 4.12921107,
            3.54350667, 4.60545934, 5.63180941, 5.07331453, 5.04419517,
            3.78796082, 4.25143811, 6.3242129 , 4.25630677, 4.59158821};
        Arrays.stream(sizes).forEach(x -> builder.add(cut(v6, x)));
        return builder.build();
        // CHECKSTYLE: resume all
    }

    /**
     * Cut the data into the specified sizes.
     *
     * @param data Data.
     * @param sizes Sizes.
     * @return the set of arrays
     */
    private static double[][] cut(double[] data, int... sizes) {
        Assertions.assertEquals(data.length, Arrays.stream(sizes).sum(), "Invalid sizes");
        final double[][] set = new double[sizes.length][];
        int from = 0;
        for (int i = 0; i < sizes.length; i++) {
            final int to = from + sizes[i];
            set[i] = Arrays.copyOfRange(data, from, to);
            from = to;
        }
        return set;
    }
}
