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

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link MannWhitneyUTest}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MannWhitneyUTestTest {

    @Test
    void testInvalidOptionsThrows() {
        final MannWhitneyUTest test = MannWhitneyUTest.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((AlternativeHypothesis) null));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            test.with((PValueMethod) null));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            test.with(PValueMethod.ESTIMATE));
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((ContinuityCorrection) null));
        for (final double v : new double[] {Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> test.withMu(v));
        }
    }

    @Test
    void testMannWhitneyUTestThrows() {
        assertMannWhitneyUTestThrows(MannWhitneyUTest.withDefaults()::statistic);
        assertMannWhitneyUTestThrows(MannWhitneyUTest.withDefaults()::test);
    }

    private static void assertMannWhitneyUTestThrows(BiConsumer<double[], double[]> action) {
        // Samples must be present, i.e. length > 0
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {}, new double[] {1.0}), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1.0}, new double[] {}), "values", "size");

        // x and y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, null));

        // x or y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, new double[] {1.0}));
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(new double[] {1.0}, null));
    }

    @ParameterizedTest
    @MethodSource
    void testMannWhitneyU(double[] x, double[] y, double statistic, double[] p,
                          double mu, PValueMethod method, boolean correct, double eps) {
        MannWhitneyUTest test = MannWhitneyUTest.withDefaults().withMu(mu).with(method)
            .with(correct ? ContinuityCorrection.ENABLED : ContinuityCorrection.DISABLED);
        final double s = mu == 0 ?
            MannWhitneyUTest.withDefaults().statistic(x, y) :
            test.statistic(x, y);
        Assertions.assertEquals(statistic, s, "statistic");
        final boolean hasTies = hasTies(x, y);
        // Note: Assertions are within a try-catch block to allow printing the R command
        int i = 0;
        try {
            for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
                test = test.with(h);
                // Test the default if possible
                final MannWhitneyUTest.Result r =
                    h == AlternativeHypothesis.TWO_SIDED &&
                    mu == 0 &&
                    method == PValueMethod.AUTO &&
                    correct ?
                    MannWhitneyUTest.withDefaults().test(x, y) :
                    test.test(x, y);
                Assertions.assertEquals(statistic, r.getStatistic(), () -> h + " statistic");
                TestUtils.assertProbability(p[i++], r.getPValue(), eps, () -> h + " p-value");
                Assertions.assertEquals(hasTies, r.hasTiedValues(), () -> " tied values");
                // Check symmetry: must reverse sign of mu
                final double other = (long) x.length * y.length - statistic;
                Assertions.assertEquals(other, test.withMu(-mu).statistic(y, x), () -> h + " statistic (y, x)");
            }
        } catch (final AssertionError e) {
            // print failing cases
            TestUtils.printf("x=c%s;%ny=c%s;%nwilcox.test(x, y, exact=%s, correct=%s, alternative='%c', mu=%s)%n",
                Arrays.toString(x).replace('[', '(').replace(']', ')'),
                Arrays.toString(y).replace('[', '(').replace(']', ')'),
                PValueMethod.EXACT == method ? "TRUE" : "FALSE",
                correct ? "TRUE" : "FALSE",
                Character.toLowerCase(AlternativeHypothesis.values()[i - 1].toString().charAt(0)),
                mu);
            throw e;
        }
    }

    static Stream<Arguments> testMannWhitneyU() {
        // For relative error of p-value
        final double eps = Math.ulp(1.0);

        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less.
        // continuity correction is only required for the asymptotic p-value.
        final boolean ignoredContinuityCorrection = false;
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Target values computed using R version 3.4.0
        // R command to compute the values can be printed to stdout in the test.

        // Exact cases: Requires no ties
        // x <- c(19, 22, 16, 29, 24)
        // y <- c(20, 11, 17, 12)
        // wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = FALSE, exact = TRUE)
        builder.add(Arguments.of(
            new double[] {19, 22, 16, 29, 24},
            new double[] {20, 11, 17, 12},
            17, new double[] {0.1111111111111111, 0.055555555555555552, 0.96825396825396826},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 15, 16, 17, 18},
            new double[] {1, 3, 5, 7, 9, 11, 13, 19, 20},
            56, new double[] {0.65563229340319129, 0.32781614670159565, 0.69866039533222191},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            57, new double[] {0.046400658165364046, 0.023200329082682023, 0.98202385849444673},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0));
        // Auto selects the exact
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            57, new double[] {0.046400658165364046, 0.023200329082682023, 0.98202385849444673},
            0, PValueMethod.AUTO, ignoredContinuityCorrection, 0));
        // Extreme u value
        builder.add(Arguments.of(
            new double[] {1, 2, 3, 4},
            new double[] {5, 6, 7, 8},
            0, new double[] {0.028571428571428571, 1, 0.014285714285714285},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0));
        // u = n*m/2
        builder.add(Arguments.of(
            new double[] {1, 3, 5, 7},
            new double[] {2, 4, 6},
            6, new double[] {1, 0.5714285714285714, 0.5714285714285714},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0));

        // Location shift
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            66, new double[] {0.0024681201151789383, 0.0012340600575894691, 0.99921842863019328},
            -4, PValueMethod.EXACT, ignoredContinuityCorrection, eps));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 15, 16, 17, 18},
            new double[] {1, 3, 5, 7, 9, 11, 13, 19, 20},
            48, new double[] {0.94084305787092171, 0.55900809716599187, 0.47042152893546085},
            2.5, PValueMethod.EXACT, ignoredContinuityCorrection, eps));

        // Random data without ties
        // UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        // ZigguratSampler.NormalizedGaussian s = ZigguratSampler.NormalizedGaussian.of(rng);
        // for (int n = 50; n <= 150; n += 50) {
        //     double[] x = s.samples(n).map(i -> Precision.round(i, 3)).distinct().toArray();
        //     double[] y = s.samples().map(i -> Precision.round(i, 3))
        //                             .filter(i -> Arrays.stream(x).noneMatch(j -> i == j))
        //                             .limit(n+10).distinct().toArray();
        // CHECKSTYLE: stop regex
        //     System.out.println(Arrays.stream(x).mapToObj(Double::toString).collect(Collectors.joining(", ", "new double[] {", "},")));
        //     System.out.println(Arrays.stream(y).mapToObj(Double::toString).collect(Collectors.joining(", ", "new double[] {", "},")));
        // CHECKSTYLE: resume regex
        // }
        builder.add(Arguments.of(
            new double[] {1.208, -1.411, -0.507, -0.521, 0.325, 0.887, -0.543, -0.012, -2.185, 0.718, 0.659, -1.095, -0.41, 0.921, -0.442, 0.883, 2.817, 0.963, 0.452, -1.171, 1.32, -0.224, 1.88, -1.459, -0.955, 2.512, 1.147, -0.471, -1.124, 0.577, 0.362, 1.737, 0.407, 0.701, -0.302, -0.859, 0.648, 0.65, 1.869, -0.685, 0.317, -0.049, 0.155, 0.943, -1.516, -0.615, 0.663, 0.048, 1.386, -0.444},
            new double[] {1.042, -0.735, 3.151, 0.628, -1.442, 1.142, 0.834, 1.686, 0.37, 1.474, 0.975, 0.697, 1.552, 0.388, -0.408, 0.62, 0.032, 2.458, 1.723, 0.549, 1.055, 0.822, -0.549, -0.517, 2.322, 1.172, -1.63, -1.151, -1.065, -0.464, -1.188, 0.472, -2.228, -0.626, 0.521, -0.334, -0.687, -1.894, 1.217, 1.061, -0.393, 1.366, 2.217, -0.045, -0.552, 1.047, 0.138, 1.27, -0.838, 0.107, 0.555, 0.1, 0.276, -0.156, 1.11, -1.498, 0.26, -1.071},
            1380, new double[] {0.66972967418927165, 0.66736881602680143, 0.33486483709463583},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 32 * eps));
        builder.add(Arguments.of(
            new double[] {1.012, -1.187, 0.737, -0.465, -0.426, 0.373, -2.206, 0.102, 0.032, 1.171, 1.615, -0.167, 0.138, -0.043, -0.391, -0.318, -0.257, 0.053, 0.129, -1.385, 0.246, 0.189, 0.286, 0.26, 0.781, -1.124, -0.404, 1.364, -0.175, -0.567, 0.224, 0.075, 1.194, -0.549, 1.277, -0.337, 0.221, -0.29, -0.26, -0.904, 0.402, -0.645, 0.88, 0.497, 1.125, -0.803, -0.66, -0.082, -1.763, -0.631, -0.85, -1.661, -1.24, 2.018, -0.013, -0.272, 2.12, -0.913, 1.151, -0.759, 1.724, -0.021, -1.84, -0.417, 0.656, -0.814, -0.179, 1.282, 0.204, 1.122, 1.434, 1.293, 0.761, -0.668, -0.527, -0.712, -0.616, -2.102, -1.03, 1.138, 0.019, -1.038, -1.085, -0.579, 1.427, -1.184, 0.196, -0.145, -0.545, 0.876, -1.262, -1.833, -0.482, 0.209, -0.159, -0.163, -1.457, -0.339, -0.08, -0.459},
            new double[] {0.207, -0.381, 0.564, 1.116, 1.365, 0.417, -0.694, -1.301, 0.803, 0.238, 0.97, -1.597, 1.123, -1.296, -0.119, -0.176, -1.188, -1.303, 1.472, 0.212, 0.895, -1.919, -1.047, -0.419, 1.499, 0.033, 1.513, 0.762, 0.531, 1.071, -0.306, 0.896, 0.709, -0.363, -0.766, -0.61, 0.888, -0.435, 0.839, 1.192, -0.375, -0.861, -2.061, -0.834, -1.093, 2.483, -0.115, -2.47, 0.058, -0.187, -1.067, 0.2, 1.643, -2.492, -1.577, 0.086, 0.622, -0.658, -1.024, 2.55, 1.989, -0.767, 1.17, -0.866, 0.89, 0.979, 0.486, 1.089, 0.574, -1.013, 0.348, 0.851, 0.304, -0.711, 0.018, -0.5, 0.797, 0.017, 0.287, -0.935, 0.644, -0.857, -0.671, -0.797, -0.411, 1.048, -0.705, -1.383, -1.791, 0.142, -0.95, -0.263, -1.591, -1.36, 0.848, -0.716, -1.281, 0.659, 2.224, -0.737, -2.193, 0.79, -0.371, 0.46, -1.135, 0.733, -0.596, 0.303, 0.904},
            5348, new double[] {0.81661195397099406, 0.59258116744016509, 0.40830597698549703},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 16 * eps));
        // Larger samples are too slow in R

        // Exact with ties -> asymptotic
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 11, 11, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            57, new double[] {0.047988968229705727, 0.023994484114852863, 0.98095699468343656},
            0, PValueMethod.EXACT, true, 2 * eps));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 11, 11, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            57, new double[] {0.042795845837304496, 0.021397922918652248, 0.97860207708134772},
            0, PValueMethod.EXACT, false, 2 * eps));
        builder.add(Arguments.of(
            new double[] {1, 2, 3, 4, 10},
            new double[] {2, 4, 6, 4, 5},
            8.5, new double[] {0.45780739419094196, 0.83010851282209053, 0.22890369709547098},
            0, PValueMethod.EXACT, true, 2 * eps));
        builder.add(Arguments.of(
            new double[] {1, 2, 3, 4, 10},
            new double[] {2, 4, 6, 4, 5},
            8.5, new double[] {0.39614390915207404, 0.80192804542396301, 0.19807195457603702},
            0, PValueMethod.EXACT, false, 2 * eps));

        // Inexact cases using continuity correction
        // wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = FALSE, exact = FALSE, correct = TRUE)
        builder.add(Arguments.of(
            new double[] {19, 22, 16, 29, 24},
            new double[] {20, 11, 17, 12},
            17, new double[] {0.11134688653314045, 0.055673443266570227, 0.96690371013890331},
            0, PValueMethod.ASYMPTOTIC, true, 2 * eps));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 15, 16, 17, 18},
            new double[] {1, 3, 5, 7, 9, 11, 13, 19, 20},
            56, new double[] {0.648503379652976, 0.324251689826488, 0.7025732881058373},
            0, PValueMethod.ASYMPTOTIC, true, eps));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            57, new double[] {0.048539622897320618, 0.024269811448660309, 0.98071937620128768},
            0, PValueMethod.ASYMPTOTIC, true, eps));
        builder.add(Arguments.of(
            new double[] {1.208, -1.411, -0.507, -0.521, 0.325, 0.887, -0.543, -0.012, -2.185, 0.718, 0.659, -1.095, -0.41, 0.921, -0.442, 0.883, 2.817, 0.963, 0.452, -1.171, 1.32, -0.224, 1.88, -1.459, -0.955, 2.512, 1.147, -0.471, -1.124, 0.577, 0.362, 1.737, 0.407, 0.701, -0.302, -0.859, 0.648, 0.65, 1.869, -0.685, 0.317, -0.049, 0.155, 0.943, -1.516, -0.615, 0.663, 0.048, 1.386, -0.444},
            new double[] {1.042, -0.735, 3.151, 0.628, -1.442, 1.142, 0.834, 1.686, 0.37, 1.474, 0.975, 0.697, 1.552, 0.388, -0.408, 0.62, 0.032, 2.458, 1.723, 0.549, 1.055, 0.822, -0.549, -0.517, 2.322, 1.172, -1.63, -1.151, -1.065, -0.464, -1.188, 0.472, -2.228, -0.626, 0.521, -0.334, -0.687, -1.894, 1.217, 1.061, -0.393, 1.366, 2.217, -0.045, -0.552, 1.047, 0.138, 1.27, -0.838, 0.107, 0.555, 0.1, 0.276, -0.156, 1.11, -1.498, 0.26, -1.071},
            1380, new double[] {0.66849366084453488, 0.66799289503005865, 0.33424683042226744},
            0, PValueMethod.ASYMPTOTIC, true, eps));
        builder.add(Arguments.of(
            new double[] {1.012, -1.187, 0.737, -0.465, -0.426, 0.373, -2.206, 0.102, 0.032, 1.171, 1.615, -0.167, 0.138, -0.043, -0.391, -0.318, -0.257, 0.053, 0.129, -1.385, 0.246, 0.189, 0.286, 0.26, 0.781, -1.124, -0.404, 1.364, -0.175, -0.567, 0.224, 0.075, 1.194, -0.549, 1.277, -0.337, 0.221, -0.29, -0.26, -0.904, 0.402, -0.645, 0.88, 0.497, 1.125, -0.803, -0.66, -0.082, -1.763, -0.631, -0.85, -1.661, -1.24, 2.018, -0.013, -0.272, 2.12, -0.913, 1.151, -0.759, 1.724, -0.021, -1.84, -0.417, 0.656, -0.814, -0.179, 1.282, 0.204, 1.122, 1.434, 1.293, 0.761, -0.668, -0.527, -0.712, -0.616, -2.102, -1.03, 1.138, 0.019, -1.038, -1.085, -0.579, 1.427, -1.184, 0.196, -0.145, -0.545, 0.876, -1.262, -1.833, -0.482, 0.209, -0.159, -0.163, -1.457, -0.339, -0.08, -0.459},
            new double[] {0.207, -0.381, 0.564, 1.116, 1.365, 0.417, -0.694, -1.301, 0.803, 0.238, 0.97, -1.597, 1.123, -1.296, -0.119, -0.176, -1.188, -1.303, 1.472, 0.212, 0.895, -1.919, -1.047, -0.419, 1.499, 0.033, 1.513, 0.762, 0.531, 1.071, -0.306, 0.896, 0.709, -0.363, -0.766, -0.61, 0.888, -0.435, 0.839, 1.192, -0.375, -0.861, -2.061, -0.834, -1.093, 2.483, -0.115, -2.47, 0.058, -0.187, -1.067, 0.2, 1.643, -2.492, -1.577, 0.086, 0.622, -0.658, -1.024, 2.55, 1.989, -0.767, 1.17, -0.866, 0.89, 0.979, 0.486, 1.089, 0.574, -1.013, 0.348, 0.851, 0.304, -0.711, 0.018, -0.5, 0.797, 0.017, 0.287, -0.935, 0.644, -0.857, -0.671, -0.797, -0.411, 1.048, -0.705, -1.383, -1.791, 0.142, -0.95, -0.263, -1.591, -1.36, 0.848, -0.716, -1.281, 0.659, 2.224, -0.737, -2.193, 0.79, -0.371, 0.46, -1.135, 0.733, -0.596, 0.303, 0.904},
            5348, new double[] {0.8162283275427048, 0.59277469738715016, 0.4081141637713524},
            0, PValueMethod.ASYMPTOTIC, true, eps));
        builder.add(Arguments.of(
            new double[] {1.606, 0.442, -0.039, 0.837, -0.28, 0.826, 0.521, -1.523, 0.452, -0.252, 0.934, -0.607, -0.416, 0.211, -0.479, 0.298, -0.25, 0.249, 0.863, -0.964, 0.18, 1.183, -1.903, 0.536, 0.901, -0.759, -0.275, 0.432, -0.745, 1.235, -2.398, -0.946, 0.469, 0.235, -1.278, 0.024, -0.263, 0.382, -0.739, -0.369, 0.179, 0.595, -0.884, 0.499, 0.677, 1.014, -1.216, -0.49, 0.247, -0.192, -1.272, 0.824, -0.72, -0.876, -0.381, 1.232, -0.037, 1.96, -0.737, 1.485, 1.286, 0.256, -0.341, 0.419, -1.028, -0.34, 1.72, -0.802, 1.299, 0.087, 2.023, 0.584, 1.456, -0.873, 2.247, -0.496, 1.15, 1.569, -0.305, 0.941, 0.882, -0.505, 2.011, -2.787, -0.04, 0.652, 0.04, -0.935, -1.706, -0.772, -0.877, -0.64, 1.464, 0.054, 0.761, -1.241, 1.677, -0.024, 1.397, 0.322, 0.148, 0.698, -1.82, -1.785, 0.586, -0.021, -0.636, -0.257, -0.388, 1.163, 0.66, 1.552, -0.857, -0.987, -0.116, 0.244, -0.372, -0.256, -0.206, 1.504, 0.146, 1.347, -0.034, -0.044, -1.19, 0.21, -0.657, 2.021, 0.875, -1.304, -0.154, -0.574, 0.706, 0.724, 1.295, -0.307, -0.797, -0.627, 1.089, 0.38, -2.377, -1.209, -1.426, 0.263, 0.515, 0.013, 0.887},
            new double[] {-1.335, 0.377, -0.167, 0.137, 0.763, -0.98, -0.073, 2.204, -0.173, -0.886, 1.217, 0.855, 1.189, 1.121, 0.864, 0.528, -1.2, 0.716, -1.033, 0.142, 1.543, 1.101, -1.388, 0.326, -0.032, 0.083, 1.429, 0.274, 0.209, -1.115, 0.714, -0.516, -0.085, 1.004, -0.985, -0.897, -0.179, 0.556, -0.622, 0.843, -0.48, 2.202, -1.519, 1.478, -0.054, 0.954, -0.484, -0.778, 0.898, 0.389, -0.129, 0.358, -0.611, 0.487, 0.175, -0.816, -0.15, -0.397, 0.64, 0.375, 1.305, 2.19, -0.974, 0.131, 0.725, -1.746, -0.202, -0.194, 0.29, 0.756, -0.068, 1.261, -0.333, -0.486, 1.421, -0.653, -0.195, -0.88, 0.098, 0.701, 0.656, 1.288, -0.696, 0.694, -0.284, 2.131, 0.136, -0.115, 0.857, -0.243, -1.344, -1.555, -1.622, -0.148, 1.283, 1.075, -0.3, 1.568, -0.648, -1.755, 1.11, 1.178, 0.072, -0.342, 0.154, -0.466, 0.111, 0.189, 0.509, -0.438, 0.12, 1.687, 0.443, -1.87, -0.236, -1.223, 1.74, 0.866, 2.509, 1.773, 0.27, 0.893, 2.269, 0.15, 0.103, -0.841, -1.164, 1.061, 0.292, -0.337, 1.581, 2.529, 1.186, -0.017, -0.03, -0.087, -2.128, -0.635, 2.048, -0.049, -3.219, 0.58, 0.315, 0.047, -0.483, -0.639, 0.35, -2.385, 0.679, 0.665, 0.197, -0.851, -1.874, -0.523, -1.154},
            11005, new double[] {0.60989875841900276, 0.69551223629252534, 0.30494937920950138},
            0, PValueMethod.ASYMPTOTIC, true, eps));
        // Auto selects the asymptotic
        builder.add(Arguments.of(
            new double[] {1.606, 0.442, -0.039, 0.837, -0.28, 0.826, 0.521, -1.523, 0.452, -0.252, 0.934, -0.607, -0.416, 0.211, -0.479, 0.298, -0.25, 0.249, 0.863, -0.964, 0.18, 1.183, -1.903, 0.536, 0.901, -0.759, -0.275, 0.432, -0.745, 1.235, -2.398, -0.946, 0.469, 0.235, -1.278, 0.024, -0.263, 0.382, -0.739, -0.369, 0.179, 0.595, -0.884, 0.499, 0.677, 1.014, -1.216, -0.49, 0.247, -0.192, -1.272, 0.824, -0.72, -0.876, -0.381, 1.232, -0.037, 1.96, -0.737, 1.485, 1.286, 0.256, -0.341, 0.419, -1.028, -0.34, 1.72, -0.802, 1.299, 0.087, 2.023, 0.584, 1.456, -0.873, 2.247, -0.496, 1.15, 1.569, -0.305, 0.941, 0.882, -0.505, 2.011, -2.787, -0.04, 0.652, 0.04, -0.935, -1.706, -0.772, -0.877, -0.64, 1.464, 0.054, 0.761, -1.241, 1.677, -0.024, 1.397, 0.322, 0.148, 0.698, -1.82, -1.785, 0.586, -0.021, -0.636, -0.257, -0.388, 1.163, 0.66, 1.552, -0.857, -0.987, -0.116, 0.244, -0.372, -0.256, -0.206, 1.504, 0.146, 1.347, -0.034, -0.044, -1.19, 0.21, -0.657, 2.021, 0.875, -1.304, -0.154, -0.574, 0.706, 0.724, 1.295, -0.307, -0.797, -0.627, 1.089, 0.38, -2.377, -1.209, -1.426, 0.263, 0.515, 0.013, 0.887},
            new double[] {-1.335, 0.377, -0.167, 0.137, 0.763, -0.98, -0.073, 2.204, -0.173, -0.886, 1.217, 0.855, 1.189, 1.121, 0.864, 0.528, -1.2, 0.716, -1.033, 0.142, 1.543, 1.101, -1.388, 0.326, -0.032, 0.083, 1.429, 0.274, 0.209, -1.115, 0.714, -0.516, -0.085, 1.004, -0.985, -0.897, -0.179, 0.556, -0.622, 0.843, -0.48, 2.202, -1.519, 1.478, -0.054, 0.954, -0.484, -0.778, 0.898, 0.389, -0.129, 0.358, -0.611, 0.487, 0.175, -0.816, -0.15, -0.397, 0.64, 0.375, 1.305, 2.19, -0.974, 0.131, 0.725, -1.746, -0.202, -0.194, 0.29, 0.756, -0.068, 1.261, -0.333, -0.486, 1.421, -0.653, -0.195, -0.88, 0.098, 0.701, 0.656, 1.288, -0.696, 0.694, -0.284, 2.131, 0.136, -0.115, 0.857, -0.243, -1.344, -1.555, -1.622, -0.148, 1.283, 1.075, -0.3, 1.568, -0.648, -1.755, 1.11, 1.178, 0.072, -0.342, 0.154, -0.466, 0.111, 0.189, 0.509, -0.438, 0.12, 1.687, 0.443, -1.87, -0.236, -1.223, 1.74, 0.866, 2.509, 1.773, 0.27, 0.893, 2.269, 0.15, 0.103, -0.841, -1.164, 1.061, 0.292, -0.337, 1.581, 2.529, 1.186, -0.017, -0.03, -0.087, -2.128, -0.635, 2.048, -0.049, -3.219, 0.58, 0.315, 0.047, -0.483, -0.639, 0.35, -2.385, 0.679, 0.665, 0.197, -0.851, -1.874, -0.523, -1.154},
            11005, new double[] {0.60989875841900276, 0.69551223629252534, 0.30494937920950138},
            0, PValueMethod.AUTO, true, eps));

        // No continuity correction
        builder.add(Arguments.of(
            new double[] {19, 22, 16, 29, 24},
            new double[] {20, 11, 17, 12},
            17, new double[] {0.086410732973700027, 0.043205366486850014, 0.95679463351314997},
            0, PValueMethod.ASYMPTOTIC, false, 2 * eps));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            57, new double[] {0.043308142810791969, 0.021654071405395985, 0.97834592859460401},
            0, PValueMethod.ASYMPTOTIC, false, eps));
        // u = n*m/2 - no correction required for two-sided
        builder.add(Arguments.of(
            new double[] {1, 3, 5, 7},
            new double[] {2, 4, 6},
            6, new double[] {1, 0.57015810240066689, 0.57015810240066689},
            0, PValueMethod.ASYMPTOTIC, true, eps));
        builder.add(Arguments.of(
            new double[] {1, 3, 5, 7},
            new double[] {2, 4, 6},
            6, new double[] {1, 0.5, 0.5},
            0, PValueMethod.ASYMPTOTIC, false, eps));

        // Location shift
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 10, 12, 14, 16, 18},
            new double[] {-3, -1, 1, 3, 5, 7, 9, 11},
            66, new double[] {0.0045306406763359836, 0.0022653203381679918, 0.99833152974630091},
            -4, PValueMethod.ASYMPTOTIC, true, 5 * eps));

        // Exact computation requested but it is not currently supported. Compare to R inexact.
        // x = 0:515
        // y = 516:1 + 0.5
        // wilcox.test(x, y, alternative = "two.sided", exact = FALSE, correct = TRUE)
        // Currently the code does not throw an exception
        builder.accept(Arguments.of(
            IntStream.rangeClosed(0, 515).asDoubleStream().toArray(),
            IntStream.rangeClosed(0, 515).mapToDouble(i -> 516 - i + 0.5).toArray(),
            132355, new double[] {0.87181181107702632, 0.56417634519959059, 0.43590590553851316},
            0, PValueMethod.EXACT, true, eps));
        return builder.build();
    }

    @Test
    void testBigDataSet() {
        final double[] x = new double[1500];
        final double[] y = new double[1500];
        for (int i = 0; i < 1500; i++) {
            x[i] = 2 * i;
            y[i] = 2 * i + 1;
        }
        final MannWhitneyUTest test = MannWhitneyUTest.withDefaults().with(PValueMethod.ASYMPTOTIC);
        final double p = test.test(x, y).getPValue();
        // R: wilcox.test(0:1499 * 2, 0:1499 * 2 + 1)
        TestUtils.assertProbability(0.97479389112077031, p, 1e-14, "p-value");
        // These revert to the asymptotic computation.
        Assertions.assertEquals(p, test.with(PValueMethod.AUTO).test(x, y).getPValue());
        Assertions.assertEquals(p, test.with(PValueMethod.EXACT).test(x, y).getPValue());
    }

    @Test
    void testBigDataSetOverflow() {
        // MATH-1145: n*m > Integer.MAX_VALUE
        final double[] x = new double[110000];
        for (int i = 0; i < 110000; i++) {
            x[i] = i;
        }
        final double[] y = x.clone();
        final double u = MannWhitneyUTest.withDefaults().statistic(x, y);
        Assertions.assertEquals(6.05e+09, u);
        final double result = MannWhitneyUTest.withDefaults().test(x, y).getPValue();
        Assertions.assertEquals(1.0, result);
    }

    @Test
    void testCalculateExactPValueTooLarge() {
        // Hit the edge case where u is not an integer. This cannot be computed exactly.
        // Note: binom(2^31 - 1, 8) is finite
        final int m = 8;
        final int n = Integer.MAX_VALUE;
        // u in [0, m*n] can be above an integer
        Assertions.assertEquals(-1, MannWhitneyUTest.calculateExactPValue(1L << 32, m, n, AlternativeHypothesis.TWO_SIDED));
    }

    /**
     * Test the exact CDF computation.
     * This hits all edge cases for expanding the cache of f.
     *
     * <p>Note: The method must run first of all the tests.
     */
    @ParameterizedTest
    @MethodSource
    @Order(1)
    void testCDF(int u, int m, int n, double p) {
        // Use 'less than' to compute the wilcox distribution CDF(u)
        TestUtils.assertProbability(p, MannWhitneyUTest.calculateExactPValue(u, m, n, AlternativeHypothesis.LESS_THAN), 1e-14, "p-value");
    }

    static Stream<Arguments> testCDF() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Target values computed using R version 3.4.0
        // pwilcox(u, m, n)
        // Here we gradually increase the value of cdf(u, m, n) to
        // deliberately expand the cache.
        builder.add(Arguments.of(0, 1, 1, 0.5));
        builder.add(Arguments.of(1, 1, 1, 1));
        // Grow M
        builder.add(Arguments.of(0, 2, 1, 0.33333333333333331483));
        builder.add(Arguments.of(1, 2, 1, 0.66666666666666662966));
        builder.add(Arguments.of(2, 2, 1, 1));
        // Grow N
        builder.add(Arguments.of(0, 2, 2, 0.16666666666666665741));
        builder.add(Arguments.of(1, 2, 2, 0.33333333333333331483));
        builder.add(Arguments.of(2, 2, 2, 0.66666666666666662966));
        builder.add(Arguments.of(3, 2, 2, 0.83333333333333337034));
        builder.add(Arguments.of(4, 2, 2, 1));
        // Grow all and reuse
        builder.add(Arguments.of(5, 3, 3, 0.6500000000000000222));
        builder.add(Arguments.of(4, 3, 3, 0.5));
        builder.add(Arguments.of(3, 3, 3, 0.3499999999999999778));
        builder.add(Arguments.of(2, 3, 3, 0.2000000000000000111));
        builder.add(Arguments.of(1, 3, 3, 0.10000000000000000555));
        builder.add(Arguments.of(0, 3, 3, 0.050000000000000002776));
        // Large values. R is slow here.
        // The Java version takes approximately 1.6 seconds total.
        // library(tictoc)
        // tic(); pwilcox(4999, 100, 100); toc();
        // 107.975 sec
        builder.add(Arguments.of(4999, 100, 100, 0.49951371413271111743));
        // 27.247 sec
        builder.add(Arguments.of(1234, 100, 100, 1.0356236722237297086e-23));
        // 0.883 sec
        builder.add(Arguments.of(123, 100, 100, 2.669299519135844296e-49));
        // 0.015 sec
        builder.add(Arguments.of(12, 100, 100, 3.0039145427513413762e-57));
        // 83.244 sec
        builder.add(Arguments.of(6789, 100, 100, 0.99999525470611971834));
        // 52.777 sec
        builder.add(Arguments.of(7890, 100, 100, 0.99999999999990418775));
        // 22.447 sec
        builder.add(Arguments.of(8901, 100, 100, 1.0));
        return builder.build();
    }

    /**
     * Checks for ties in the data (i.e. a shared value between the two samples).
     *
     * @param x Sample 1.
     * @param y Sample 2.
     * @return true if at least 1 value is shared
     */
    private static boolean hasTies(double[] x, double[] y) {
        final double[] sx = x.clone();
        Arrays.sort(sx);
        for (final double v : y) {
            if (Arrays.binarySearch(sx, v) >= 0) {
                return true;
            }
        }
        return false;
    }
}
