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
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link WilcoxonSignedRankTest}.
 */
class WilcoxonSignedRankTestTest {

    @Test
    void testInvalidOptionsThrows() {
        final WilcoxonSignedRankTest test = WilcoxonSignedRankTest.withDefaults();
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

    @ParameterizedTest
    @MethodSource
    void testTieCorrection(double[] data, double c) {
        TestUtils.assertRelativelyEquals(c, WilcoxonSignedRankTest.calculateTieCorrection(data), 1e-14, "tie correction");
    }

    static Stream<Arguments> testTieCorrection() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // scipy.stats.tiecorrect (version 1.9.3)
        // The tiecorrect value must be adjusted to match the computation used to create c
        builder.add(scipyTieCorrect(0.9, 1, 2.5, 2.5, 4));
        builder.add(scipyTieCorrect(0.9833333333333333, 1., 4., 2.5, 5.5, 7., 8., 2.5, 9., 5.5));
        builder.add(scipyTieCorrect(0.8, 1, 3, 3, 3, 5));
        builder.add(scipyTieCorrect(0.9523809523809523, 1, 3, 3, 3, 5, 6, 7, 8));
        builder.add(scipyTieCorrect(0.9696969696969697, 1, 3, 3, 3, 5, 6, 7.5, 7.5, 9, 10));
        builder.add(scipyTieCorrect(0.9892857142857143, 1, 3, 3, 3, 5, 6, 7.5, 7.5, 9, 10, 11, 12, 14, 14, 15));
        builder.add(scipyTieCorrect(0.9761904761904762, 4, 1, 3, 5, 2, 3, 1, 6));
        builder.add(scipyTieCorrect(0.95, 42, 13, 17, 500, 13));
        builder.add(scipyTieCorrect(0.8928571428571429, 10, 100, 1000, 10, 10, 100, 1000));
        return builder.build();
    }

    /**
     * Adjust the tie correct factor computed by scipy tiecorrect to the raw
     * count of tied lengths.
     * <pre>
     * c = sum(t^3 - t) ; t is the length of each tie
     *
     * tiecorrect = 1 - c / (n^3 - n)
     * </pre>
     *
     * @param tieCorrect Tie correct factor
     * @param ranks Ranks.
     * @return the test arguments ([ranks, c])
     */
    private static Arguments scipyTieCorrect(double tieCorrect, double... ranks) {
        final int n = ranks.length;
        final double c = (1 - tieCorrect) * (Math.pow(n, 3) - n);
        return Arguments.of(ranks, c);
    }

    @Test
    void testWilcoxonSignedRankDifferencesThrows() {
        assertWilcoxonSignedRankDifferencesThrows(WilcoxonSignedRankTest.withDefaults()::statistic);
        assertWilcoxonSignedRankDifferencesThrows(WilcoxonSignedRankTest.withDefaults()::test);
    }

    private static void assertWilcoxonSignedRankDifferencesThrows(Consumer<double[]> action) {
        // Samples must be present, i.e. length > 0
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {}), "values", "size");
        // z is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null));
        // z is all zeros
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[10]), "zero");
    }

    @ParameterizedTest
    @MethodSource
    void testWilcoxonSignedRankDifferences(double[] z, double statistic, double[] p,
                                           double mu, PValueMethod method, boolean correct, double eps) {
        WilcoxonSignedRankTest test = WilcoxonSignedRankTest.withDefaults().withMu(mu).with(method)
            .with(correct ? ContinuityCorrection.ENABLED : ContinuityCorrection.DISABLED);
        final double s = mu == 0 ?
            WilcoxonSignedRankTest.withDefaults().statistic(z) :
            test.statistic(z);
        Assertions.assertEquals(statistic, s, "statistic");
        final boolean hasTies = hasTies(z);
        final boolean hasZeros = hasZeros(z);
        // Note: Assertions are within a try-catch block to allow printing the R command
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final WilcoxonSignedRankTest.Result r =
                h == AlternativeHypothesis.TWO_SIDED &&
                mu == 0 &&
                method == PValueMethod.AUTO &&
                correct ?
                WilcoxonSignedRankTest.withDefaults().test(z) :
                test.test(z);
            Assertions.assertEquals(statistic, r.getStatistic(), () -> h + " statistic");
            TestUtils.assertProbability(p[i++], r.getPValue(), eps, () -> h + " p-value");
            Assertions.assertEquals(hasTies, r.hasTiedValues(), () -> " tied values");
            Assertions.assertEquals(hasZeros, r.hasZeroValues(), () -> " zero values");
        }
    }

    static Stream<Arguments> testWilcoxonSignedRankDifferences() {
        // Extract the differences from the paired samples
        return testWilcoxonSignedRank().map(a -> {
            final Object[] args = a.get();
            final double[] x = (double[]) args[0];
            final double[] y = (double[]) args[1];
            final double[] z = new double[x.length];
            for (int i = 0; i < x.length; i++) {
                z[i] = x[i] - y[i];
            }
            final Object[] args2 = new Object[args.length - 1];
            args2[0] = z;
            System.arraycopy(args, 2, args2, 1, args.length - 2);
            return Arguments.of(args2);
        });
    }

    @Test
    void testWilcoxonSignedRankThrows() {
        assertWilcoxonSignedRankThrows(WilcoxonSignedRankTest.withDefaults()::statistic);
        assertWilcoxonSignedRankThrows(WilcoxonSignedRankTest.withDefaults()::test);
    }

    private static void assertWilcoxonSignedRankThrows(BiConsumer<double[], double[]> action) {
        // Samples must be present, i.e. length > 0
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {}, new double[] {1.0}), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1.0}, new double[] {}), "values", "size");

        // Samples not same size, i.e. cannot be paired
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1.0, 2.0}, new double[] {3.0}), "values", "size", "mismatch");

        // x and y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, null));

        // x or y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, new double[] {1.0}));
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(new double[] {1.0}, null));

        // z = x - y is all zeros
        final double[] x = {1, 2, 3};
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(x, x), "zero");
    }

    @ParameterizedTest
    @MethodSource
    void testWilcoxonSignedRank(double[] x, double[] y, double statistic, double[] p,
                                double mu, PValueMethod method, boolean correct, double eps) {
        WilcoxonSignedRankTest test = WilcoxonSignedRankTest.withDefaults().withMu(mu).with(method)
            .with(correct ? ContinuityCorrection.ENABLED : ContinuityCorrection.DISABLED);
        final double s = mu == 0 ?
            WilcoxonSignedRankTest.withDefaults().statistic(x, y) :
            test.statistic(x, y);
        final boolean hasTies = hasTies(x, y);
        final int zeros = countZeros(x, y);
        // Note: Assertions are within a try-catch block to allow printing the R command
        int i = 0;
        try {
            for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
                test = test.with(h);
                // Test the default if possible
                final WilcoxonSignedRankTest.Result r =
                    h == AlternativeHypothesis.TWO_SIDED &&
                    mu == 0 &&
                    method == PValueMethod.AUTO &&
                    correct ?
                    WilcoxonSignedRankTest.withDefaults().test(x, y) :
                    test.test(x, y);
                Assertions.assertEquals(statistic, r.getStatistic(), () -> h + " statistic");
                TestUtils.assertProbability(p[i++], r.getPValue(), eps, () -> h + " p-value");
                Assertions.assertEquals(hasTies, r.hasTiedValues(), () -> " tied values");
                Assertions.assertEquals(zeros != 0, r.hasZeroValues(), () -> " zero values");
                // Check symmetry: must reverse sign of mu
                // W+ + W- = n*(n+1)/2 - nZeros
                final double other = (long) x.length * (x.length + 1) / 2 - statistic - zeros;
                Assertions.assertEquals(other, test.withMu(-mu).statistic(y, x), () -> h + " statistic (y, x)");
            }
        } catch (final AssertionError e) {
            // print failing cases
            TestUtils.printf("x=c%s;%ny=c%s;%nwilcox.test(x, y, paired=TRUE, exact=%s, correct=%s, alternative='%c')%n",
                Arrays.toString(x).replace('[', '(').replace(']', ')'),
                Arrays.toString(y).replace('[', '(').replace(']', ')'),
                PValueMethod.EXACT == method ? "TRUE" : "FALSE",
                correct ? "TRUE" : "FALSE",
                Character.toLowerCase(AlternativeHypothesis.values()[i - 1].toString().charAt(0)));
            throw e;
        }
    }

    static Stream<Arguments> testWilcoxonSignedRank() {
        // For relative error of p-value
        final double eps = Math.ulp(1.0);

        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less.
        // continuity correction is only required for the asymptotic p-value.
        final boolean ignoredContinuityCorrection = false;
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Target values computed using R version 3.4.0,
        // R command to compute the values can be printed to stdout in the test, e.g.
        // options(digits=20)
        // x <- c(1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30)
        // y <- c(0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29)
        // wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = TRUE, exact = TRUE, correct = TRUE)
        // V = 40, p-value = 0.0390625
        // Note: V here is directional (it corresponds to W+)

        // Exact cases
        builder.accept(Arguments.of(
            new double[] {1},
            new double[] {-1},
            1, new double[] {1, 0.5, 1},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0
        ));
        builder.accept(Arguments.of(
            new double[] {1, 2},
            new double[] {-1, 3},
            2, new double[] {1, 0.5, 0.75},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0
        ));
        builder.accept(Arguments.of(
            new double[] {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30},
            new double[] {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29},
            40, new double[] {0.0390625, 0.01953125, 0.986328125},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0
        ));
        // Edge case where statistic == 0
        builder.accept(Arguments.of(
            new double[] {1, 2, 3},
            new double[] {2, 4, 6},
            0, new double[] {0.25, 1, 0.125},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0
        ));
        // exact is chosen by AUTO
        builder.accept(Arguments.of(
            new double[] {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30},
            new double[] {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29},
            40, new double[] {0.0390625, 0.01953125, 0.986328125},
            0, PValueMethod.AUTO, ignoredContinuityCorrection, 0
        ));
        // expected difference
        builder.accept(Arguments.of(
            new double[] {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30},
            new double[] {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29},
            45, new double[] {0.00390625, 0.001953125, 1},
            -2.5, PValueMethod.AUTO, ignoredContinuityCorrection, 0
        ));
        builder.accept(Arguments.of(
            new double[] {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30},
            new double[] {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29},
            1, new double[] {0.0078125, 0.998046875, 0.00390625},
            1, PValueMethod.AUTO, ignoredContinuityCorrection, 0
        ));
        // R removes any z = x - y where z == 0, so we generate data with no zero deltas.
        // For the exact computation |z| must be unique (no ties). The following attempts to create
        // a unique y (although samples within y may be identical and |x - y| may occur).
        // R will output when there are ties and the exact computation is invalid.
        // In this case generate again (and optionally increase precision).
        // e.g. for 3 decimal places:
        // UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        // ZigguratSampler.NormalizedGaussian s = ZigguratSampler.NormalizedGaussian.of(rng);
        // for (int n = 50; n <= 200; n += 50) {
        //     double[] x = s.samples(n).map(i -> Precision.round(i, 3)).toArray();
        //     double[] y = s.samples().map(i -> Precision.round(i, 3))
        //                             .filter(i -> Arrays.stream(x).noneMatch(j -> i == j))
        //                             .limit(n).toArray();
        // CHECKSTYLE: stop regex
        //     System.out.println(Arrays.stream(x).mapToObj(Double::toString).collect(Collectors.joining(", ", "new double[] {", "},")));
        //     System.out.println(Arrays.stream(y).mapToObj(Double::toString).collect(Collectors.joining(", ", "new double[] {", "},")));
        // CHECKSTYLE: resume regex
        // }
        builder.accept(Arguments.of(
            new double[] {-2.35, 1.46, 0.88, 0.24, 0.52, -1.10, -0.36, -0.93, 0.60, 0.76},
            new double[] {1.31, -0.88, 0.24, -0.99, 0.79, 0.23, 1.09, -0.01, -0.02, 0.07},
            24, new double[] {0.76953125, 0.65234375, 0.384765625},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 0
        ));
        builder.accept(Arguments.of(
            new double[] {-0.25, -0.83, -0.43, 1.55, -0.03, -0.03, -0.41, -1.97, 0.59, 0.90, 2.52, 1.29, -1.20, -0.66, 1.02},
            new double[] {0.51, 0.36, 0.91, 1.22, -2.10, -1.16, 0.29, 0.07, 0.91, -0.73, 1.49, -2.08, 1.01, 1.40, 1.82},
            53, new double[] {0.71972656250000011, 0.660614013671875, 0.35986328125000006},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, eps
        ));
        builder.accept(Arguments.of(
            new double[] {-0.45, 0.75, 0.81, 0.24, -0.68, 1.05, -0.52, 1.12, -1.22, -1.59, 0.32, 1.18, 0.63, -1.21, 0.29, -0.51, -0.59, 1.10, -1.18, -1.08},
            new double[] {1.50, 0.82, -0.85, 0.17, 1.87, -0.56, 0.57, 0.04, -0.02, 0.38, 0.53, -0.97, -0.29, 0.48, 0.07, 0.01, 1.09, -0.37, 1.11, 2.49},
            69, new double[] {0.18934822082519531, 0.91157341003417969, 0.094674110412597656},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, eps
        ));
        builder.accept(Arguments.of(
            new double[] {-0.20, -0.81, 2.99, -1.14, 1.57, 0.95, -0.42, 0.87, -0.56, -0.43, 0.24, 0.42, 1.80, -0.29, -1.21, -1.34, -0.51, 0.82, 0.92, 0.77, -2.66, -0.77, 1.05, -1.88, 0.32},
            new double[] {-1.08, -0.93, 0.08, 0.17, -0.73, 0.00, -1.42, -0.20, -0.92, -1.06, 0.69, -1.42, -1.54, 1.14, -0.12, 2.45, 0.05, 0.21, 0.60, 2.57, 2.01, -0.61, -0.58, 0.25, 0.67},
            174, new double[] {0.77115941047668446, 0.38557970523834223, 0.62450352311134338},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, eps
        ));
        builder.accept(Arguments.of(
            new double[] {-0.874, 0.272, 0.732, 1.040, -1.244, -2.621, -0.750, 0.009, 0.212, 0.407, 2.200, 0.897, -1.378, -1.141, 0.403, -1.222, 1.135, -0.909, -0.211, 0.612, -0.349, -2.022, 0.515, -0.833, 0.724, 0.318, 0.665, 0.597, 0.008, -0.087},
            new double[] {0.997, -0.263, 1.310, -0.093, 0.180, -0.119, 0.013, 1.036, 0.391, -2.282, 1.865, -1.448, -0.181, 0.732, 0.551, 2.061, -1.883, -0.775, 1.004, -0.862, 1.943, -1.163, 0.128, -1.624, 1.197, 0.555, -1.168, 0.052, 0.874, 0.121},
            188, new double[] {0.37074060738086706, 0.8200610363855958, 0.18537030369043353},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, eps
        ));
        // Largest exact computation
        // x = 0:1022
        // y = 1023:1 + 0.5
        // wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = TRUE, exact = TRUE)
        // Note: R is summing count / 2^n as count * exp(-n * ln(2)) so a difference accumulates:
        // Math.scalb(1.0, -1023)        == 1.1125369292536007E-308
        // Math.exp(-1023 * Math.log(2)) == 1.112536929253566E-308
        builder.accept(Arguments.of(
            IntStream.range(0, 1023).asDoubleStream().toArray(),
            IntStream.range(0, 1023).mapToDouble(i -> 1023 - i + 0.5).toArray(),
            261121, new double[] {0.93539811751499313, 0.53234298976024264, 0.46769905875749657},
            0, PValueMethod.EXACT, ignoredContinuityCorrection, 5e-14
        ));

        // Exact with tied absolute differences -> asymptotic
        // wilcox.test(x, y, alternative = "t", paired = TRUE, exact = TRUE)
        builder.add(Arguments.of(
            new double[] {1, 2, 3, 4},
            new double[] {0, 3, 4, 6},
            2, new double[] {0.34470422200695772, 0.90706163381706195, 0.17235211100347886},
            0, PValueMethod.EXACT, true, 2 * eps));
        builder.add(Arguments.of(
            new double[] {1, 4, 3, 4},
            new double[] {0, 3, 4, 6},
            4, new double[] {0.85010673913852575, 0.71462480597091305, 0.42505336956926287},
            0, PValueMethod.EXACT, true, 2 * eps));

        // Inexact cases using continuity correction
        // wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = TRUE, exact = FALSE, correct = TRUE)
        builder.accept(Arguments.of(
            new double[] {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30},
            new double[] {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29},
            40, new double[] {0.044010984012951455, 0.022005492006475728, 0.98351530937788989},
            0, PValueMethod.ASYMPTOTIC, true, 2 * eps
        ));
        // Edge case where statistic == 0
        builder.accept(Arguments.of(
            new double[] {1, 2, 3},
            new double[] {2, 4, 6},
            0, new double[] {0.18144920772142031, 0.96931558543029894, 0.090724603860710157},
            0, PValueMethod.ASYMPTOTIC, true, 4 * eps
        ));
        // Target values computed using R version 3.4.0, including the continuity correction e.g.
        // R removes any z = x - y where z == 0, so we generate data with no zero deltas (see above)
        builder.accept(Arguments.of(
            new double[] {0.716, 0.113, 0.507, 0.506, -0.11, -1.063, -1.543, -0.735, -0.38, 0.751, 0.778, -1.923, 0.053, 0.342, -0.121, -0.045, -0.086, 0.153, 0.802, -0.402, 1.443, 1.355, -0.518, -0.884, 0.027, 0.129, -1.219, -1.461, -1.426, -0.975, 1.332, -0.472, -1.028, -1.029, -0.962, 0.402, 1.0, 0.147, -0.536, 0.947, 0.721, -0.154, 0.061, -2.281, -1.993, -0.283, 0.02, -0.41, -1.586, 0.129},
            new double[] {0.143, -0.741, -2.008, -0.96, 0.474, 1.527, 1.514, 0.551, -2.142, -0.127, -1.152, 1.531, 0.557, 1.256, 0.629, 1.265, 0.488, -1.981, -0.489, -1.21, 1.52, 0.362, -0.235, -0.207, 1.497, 0.211, 2.054, 0.243, -1.18, -0.817, -0.42, 0.166, 1.575, -1.642, -0.389, -0.482, 2.789, -1.452, 0.795, 0.472, -0.221, -1.589, 1.26, -0.168, 0.647, -0.582, -0.999, -0.217, 0.058, 0.77},
            530.5, new double[] {0.30391211935371887, 0.85030292827375042, 0.15195605967685943},
            0, PValueMethod.ASYMPTOTIC, true, 4 * eps
        ));
        builder.accept(Arguments.of(
            new double[] {0.198, 0.299, -0.327, 0.68, 1.914, 2.069, -0.767, 0.833, -1.337, -0.238, 0.037, 1.785, 1.488, 1.164, 0.404, 0.033, 1.667, -0.838, 0.092, -0.696, -0.41, 0.341, -0.398, -1.381, 0.535, -0.528, 1.496, -2.384, -1.739, 0.335, 0.249, -0.779, -0.821, 0.777, 1.455, 1.025, -0.217, 0.21, 0.009, -0.158, -0.664, -0.079, 0.119, 0.342, 0.523, -0.742, -0.757, 0.538, 0.591, -0.241, -0.904, -0.407, 1.43, 0.436, -0.223, -0.663, 1.094, -0.03, -2.054, 1.183, 0.178, 0.171, 0.231, -0.376, -1.431, -1.744, 0.527, 0.523, -0.775, -1.792, 1.096, 1.055, -0.15, -0.247, -0.067, -0.661, -0.207, 0.162, 0.355, -1.945, 0.866, -0.868, 0.774, -1.823, -0.708, -0.877, -0.683, 0.774, -2.174, -1.312, -0.678, -0.236, -0.7, -0.751, -0.638, -1.324, -0.345, 0.164, -0.999, 2.433},
            new double[] {0.095, -0.385, -0.659, -0.051, 0.227, 0.323, 0.227, -0.151, -1.293, 1.195, 0.799, -0.121, 2.589, -0.121, 0.665, -1.417, -2.043, -1.459, -0.097, -0.753, -0.81, -0.33, -0.069, 1.453, 0.197, 0.411, -0.581, 0.35, 0.143, -1.07, 0.725, -0.469, -1.673, -1.097, -0.415, 1.708, 0.52, 1.001, -1.052, -1.319, 1.723, -0.561, 2.333, -0.344, -1.013, 1.062, -1.4, 0.439, -0.92, 1.037, 0.824, -0.912, 1.639, -1.098, 0.838, -0.797, 1.733, 1.464, 0.098, -0.287, 0.481, 0.259, -1.697, 0.371, -0.239, -1.536, -0.448, 0.065, -0.763, -1.497, 0.082, 0.213, 0.353, -0.161, 0.571, 1.091, 1.506, -0.141, 0.23, 0.381, 0.861, -0.416, -0.824, -0.681, -1.105, -0.713, 1.727, -0.902, 1.825, 0.101, 0.917, 0.2, 1.245, 0.046, 0.242, 0.318, 1.293, -0.747, 0.316, -0.51},
            2294, new double[] {0.42804876442031159, 0.7869762452609379, 0.21402438221015579},
            0, PValueMethod.ASYMPTOTIC, true, 4 * eps
        ));
        builder.accept(Arguments.of(
            new double[] {0.444, 0.331, -1.225, -1.391, 0.896, 0.885, 1.239, -0.985, -0.84, -0.344, -1.522, -0.845, 1.244, -1.609, -0.565, -0.371, 0.575, -0.775, -0.351, 0.427, -1.294, -0.33, 1.07, 1.307, -1.777, -0.011, 1.452, -0.29, 0.509, -1.604, 2.316, 0.978, 0.075, 0.439, 1.941, 0.122, -1.051, 0.116, -0.466, -1.561, -0.978, 0.54, -1.097, 1.106, 0.835, -1.15, 1.015, 0.182, 0.426, 0.496, -0.484, -1.222, 0.21, -1.213, 1.382, 0.375, -0.282, -0.879, 1.297, -1.467, -0.646, -0.322, -0.996, 1.488, 2.242, 0.479, 0.386, 0.254, -0.394, 2.275, -2.256, 1.644, -0.508, -0.042, -0.108, -0.105, 1.369, 2.124, -0.59, -0.281, -0.165, -1.779, -0.04, 0.534, -0.703, -0.2, 1.031, 1.102, 0.163, -2.057, -0.125, -0.378, 0.839, 0.627, -0.93, -2.635, -0.495, 0.467, 1.288, 1.051, -1.192, -1.354, 0.452, -0.351, 1.508, -0.033, 1.185, -0.546, -0.222, -1.274, -0.886, 1.901, 1.041, 0.042, -0.338, 1.811, -0.8, 0.077, 1.516, 0.183, 1.012, -1.273, 0.224, 1.065, 0.289, -0.336, -2.554, 0.813, 1.038, 0.589, -2.023, -0.422, -0.619, 0.075, 0.829, -0.808, -0.129, 0.725, 0.534, -0.368, -1.1, 1.121, 0.187, 1.095, 1.607, 0.323, -0.724, -0.682, 2.623, 0.999},
            new double[] {0.431, -0.665, -0.504, 1.36, 0.556, -0.096, 0.275, 0.298, 1.291, -1.212, 0.129, 1.455, 0.184, 0.178, -1.645, -0.791, -0.64, 1.068, -1.007, 0.377, 0.371, 0.901, -1.036, 0.88, -0.7, -0.457, -0.822, -0.348, -0.345, -1.338, 0.995, -1.132, 0.023, 0.796, 0.44, -0.586, 0.114, 0.041, 0.304, 0.147, -0.906, 0.208, -2.017, -0.352, -1.511, 0.42, 0.901, -0.036, -1.817, 0.617, -0.109, -0.308, 3.17, 0.333, 1.757, -1.525, -0.076, 0.977, -0.244, -0.512, -0.126, -0.925, -1.291, -0.164, -0.11, -0.747, 0.36, -0.156, -0.019, 0.026, -0.035, -0.637, -0.856, -0.278, -2.123, -0.335, 0.674, -0.325, -0.306, 0.792, -0.19, -1.12, -0.494, -2.005, -0.672, 1.784, 1.667, -1.427, -0.193, 0.815, 0.584, -0.122, 0.634, -1.035, -0.761, 0.09, -0.633, 0.371, -0.807, 0.164, -0.255, 0.161, -0.343, 0.07, 0.398, -1.432, -0.317, -0.588, -1.844, -0.08, 0.081, -0.675, 1.609, -0.82, 0.402, -0.63, -0.736, -0.74, 0.781, -0.778, 0.617, -0.449, 0.917, 0.236, -0.158, -0.785, -1.667, -1.173, -0.628, -0.608, 0.176, 0.476, -0.897, -0.977, 0.142, -0.185, -0.288, 0.935, -0.086, -0.287, -0.483, 0.574, 0.443, 0.791, 0.674, 0.086, -0.734, 0.631, 0.903, -0.733},
            6671, new double[] {0.058590782908404818, 0.029295391454202409, 0.9708295564629108},
            0, PValueMethod.ASYMPTOTIC, true, 4 * eps
        ));
        builder.accept(Arguments.of(
            new double[] {-1.913, -0.100, 0.361, -0.726, -0.637, 0.845, -2.157, -1.606, -0.747, -0.684, -0.127, 0.313, 1.140, 0.545, -2.100, 0.177, -0.347, 1.098, -0.020, -0.920, 1.152, -1.057, -0.254, 0.565, -2.260, -0.433, -0.685, -0.614, -2.786, 2.641, 0.880, 0.620, 0.538, 0.260, -0.026, 0.146, 0.516, 0.000, 2.162, -1.050, 1.951, 0.744, 0.349, -0.071, -0.289, -0.916, -0.990, -0.043, -0.173, 0.579, -1.373, 1.781, 1.202, 0.098, 0.082, -0.731, 1.667, -0.874, 0.388, -0.185, 0.117, -0.770, 1.456, -2.188, 0.400, -0.598, 1.216, -0.910, 2.016, 0.206, 2.601, -0.206, -1.390, -0.742, -1.416, -1.261, -0.044, 0.108, -0.575, 0.038, -0.152, -0.432, 1.064, -0.324, 0.665, -0.310, 0.468, -0.186, 1.856, -0.354, 0.226, -1.795, -1.430, -1.213, 0.958, 0.035, -0.126, 0.867, 0.911, -0.208, -1.056, -0.158, -0.662, -1.578, -0.347, 0.338, 1.373, -0.664, -0.129, -0.706, 0.078, 0.034, -0.239, -0.559, 0.710, -0.827, 0.805, -0.416, 0.582, 0.320, 0.891, -0.718, -0.317, 0.373, 0.160, -2.045, 2.371, 0.421, -1.305, -2.235, -0.009, 0.520, 0.758, -0.929, 0.799, -1.217, -0.641, 0.025, 0.999, -1.656, 1.621, 0.297, -0.291, 2.127, 0.663, -0.582, 0.904, -1.033, 1.305, 0.023, 0.190, 0.754, 1.008, -1.927, 0.079, 0.284, 0.865, -0.240, 0.853, 0.237, -1.275, -1.619, -1.552, 0.892, 1.112, 0.829, 0.858, -0.223, 1.246, 1.297, 0.906, 0.606, -0.012, -1.186, 0.971, -0.613, 0.298, 0.351, 0.314, 2.514, 0.287, -1.313, -0.500, 0.955, 0.500, -1.193, 2.070, 0.514, 0.298, -1.035, -0.177, 0.384, -0.472, 1.806, -0.544, -0.882, -0.048, 0.113, -0.622, 1.267},
            new double[] {0.512, 0.830, 0.725, -1.156, -0.704, 1.700, -2.194, -1.254, -2.072, -0.031, 0.381, -0.448, 0.903, 0.972, 0.364, -0.072, -0.319, 0.449, -0.643, 1.391, -0.139, 1.203, -0.519, 1.239, 1.147, -0.205, 0.242, -0.420, 0.458, -1.876, -0.096, -1.781, 0.656, -0.989, 0.841, -0.870, 1.167, 1.076, -1.296, 0.202, 1.519, 0.898, -0.626, 1.120, -1.466, 0.883, -0.251, 0.348, 0.352, 1.500, -1.097, -0.459, 1.750, 0.272, 0.624, 0.286, 1.328, -0.879, 0.772, 0.158, -0.937, 1.465, 1.399, 0.925, -1.382, 0.430, 1.386, 0.457, 2.430, -0.687, 0.398, -0.487, 0.590, 1.982, 0.549, -1.685, 1.776, -0.697, 0.027, -0.495, 2.677, -0.369, 0.465, 0.369, 1.384, 2.614, -0.668, 1.002, -0.079, -0.908, 1.973, 0.424, 0.657, 1.511, 1.171, -0.848, 0.507, -0.425, 0.381, 0.143, -0.586, 0.310, -1.125, 1.904, -0.301, 1.091, -0.868, -0.346, -0.580, -1.310, 1.063, 0.766, 0.567, 2.055, -1.457, -0.501, -1.098, 0.625, -0.615, -0.930, -1.341, 1.012, 0.441, -0.661, -0.792, 0.419, 2.773, -0.892, -0.829, 0.662, -1.051, 1.000, 0.333, 2.639, -0.550, -1.491, 1.542, -0.377, 0.801, -0.549, -1.992, -0.231, -0.469, -1.584, -2.626, -0.450, -0.160, -1.361, -0.037, -1.585, -1.064, -0.330, -0.311, -0.995, -0.300, 0.737, -0.549, 1.656, -0.806, -0.375, 0.301, 0.064, -1.200, -0.153, -0.906, -1.569, -0.757, -0.141, 0.083, -0.388, 0.983, 1.286, 0.533, -0.449, -1.357, -0.407, 0.123, -0.335, -0.546, 1.106, 0.512, -0.843, -0.445, 0.058, -0.562, 0.158, 1.464, 1.075, -1.222, -0.589, 0.181, -2.208, -0.142, -1.844, -0.353, -2.242, -1.383, 0.185, 1.696, 1.528},
            9947, new double[] {0.90047012937135407, 0.55024788374638656, 0.45023506468567703},
            0, PValueMethod.ASYMPTOTIC, true, 4 * eps
        ));
        // Asymptotic is chosen by AUTO
        builder.accept(Arguments.of(
            new double[] {-1.913, -0.100, 0.361, -0.726, -0.637, 0.845, -2.157, -1.606, -0.747, -0.684, -0.127, 0.313, 1.140, 0.545, -2.100, 0.177, -0.347, 1.098, -0.020, -0.920, 1.152, -1.057, -0.254, 0.565, -2.260, -0.433, -0.685, -0.614, -2.786, 2.641, 0.880, 0.620, 0.538, 0.260, -0.026, 0.146, 0.516, 0.000, 2.162, -1.050, 1.951, 0.744, 0.349, -0.071, -0.289, -0.916, -0.990, -0.043, -0.173, 0.579, -1.373, 1.781, 1.202, 0.098, 0.082, -0.731, 1.667, -0.874, 0.388, -0.185, 0.117, -0.770, 1.456, -2.188, 0.400, -0.598, 1.216, -0.910, 2.016, 0.206, 2.601, -0.206, -1.390, -0.742, -1.416, -1.261, -0.044, 0.108, -0.575, 0.038, -0.152, -0.432, 1.064, -0.324, 0.665, -0.310, 0.468, -0.186, 1.856, -0.354, 0.226, -1.795, -1.430, -1.213, 0.958, 0.035, -0.126, 0.867, 0.911, -0.208, -1.056, -0.158, -0.662, -1.578, -0.347, 0.338, 1.373, -0.664, -0.129, -0.706, 0.078, 0.034, -0.239, -0.559, 0.710, -0.827, 0.805, -0.416, 0.582, 0.320, 0.891, -0.718, -0.317, 0.373, 0.160, -2.045, 2.371, 0.421, -1.305, -2.235, -0.009, 0.520, 0.758, -0.929, 0.799, -1.217, -0.641, 0.025, 0.999, -1.656, 1.621, 0.297, -0.291, 2.127, 0.663, -0.582, 0.904, -1.033, 1.305, 0.023, 0.190, 0.754, 1.008, -1.927, 0.079, 0.284, 0.865, -0.240, 0.853, 0.237, -1.275, -1.619, -1.552, 0.892, 1.112, 0.829, 0.858, -0.223, 1.246, 1.297, 0.906, 0.606, -0.012, -1.186, 0.971, -0.613, 0.298, 0.351, 0.314, 2.514, 0.287, -1.313, -0.500, 0.955, 0.500, -1.193, 2.070, 0.514, 0.298, -1.035, -0.177, 0.384, -0.472, 1.806, -0.544, -0.882, -0.048, 0.113, -0.622, 1.267},
            new double[] {0.512, 0.830, 0.725, -1.156, -0.704, 1.700, -2.194, -1.254, -2.072, -0.031, 0.381, -0.448, 0.903, 0.972, 0.364, -0.072, -0.319, 0.449, -0.643, 1.391, -0.139, 1.203, -0.519, 1.239, 1.147, -0.205, 0.242, -0.420, 0.458, -1.876, -0.096, -1.781, 0.656, -0.989, 0.841, -0.870, 1.167, 1.076, -1.296, 0.202, 1.519, 0.898, -0.626, 1.120, -1.466, 0.883, -0.251, 0.348, 0.352, 1.500, -1.097, -0.459, 1.750, 0.272, 0.624, 0.286, 1.328, -0.879, 0.772, 0.158, -0.937, 1.465, 1.399, 0.925, -1.382, 0.430, 1.386, 0.457, 2.430, -0.687, 0.398, -0.487, 0.590, 1.982, 0.549, -1.685, 1.776, -0.697, 0.027, -0.495, 2.677, -0.369, 0.465, 0.369, 1.384, 2.614, -0.668, 1.002, -0.079, -0.908, 1.973, 0.424, 0.657, 1.511, 1.171, -0.848, 0.507, -0.425, 0.381, 0.143, -0.586, 0.310, -1.125, 1.904, -0.301, 1.091, -0.868, -0.346, -0.580, -1.310, 1.063, 0.766, 0.567, 2.055, -1.457, -0.501, -1.098, 0.625, -0.615, -0.930, -1.341, 1.012, 0.441, -0.661, -0.792, 0.419, 2.773, -0.892, -0.829, 0.662, -1.051, 1.000, 0.333, 2.639, -0.550, -1.491, 1.542, -0.377, 0.801, -0.549, -1.992, -0.231, -0.469, -1.584, -2.626, -0.450, -0.160, -1.361, -0.037, -1.585, -1.064, -0.330, -0.311, -0.995, -0.300, 0.737, -0.549, 1.656, -0.806, -0.375, 0.301, 0.064, -1.200, -0.153, -0.906, -1.569, -0.757, -0.141, 0.083, -0.388, 0.983, 1.286, 0.533, -0.449, -1.357, -0.407, 0.123, -0.335, -0.546, 1.106, 0.512, -0.843, -0.445, 0.058, -0.562, 0.158, 1.464, 1.075, -1.222, -0.589, 0.181, -2.208, -0.142, -1.844, -0.353, -2.242, -1.383, 0.185, 1.696, 1.528},
            9947, new double[] {0.90047012937135407, 0.55024788374638656, 0.45023506468567703},
            0, PValueMethod.AUTO, true, 4 * eps
        ));
        // Largest exact computation as inexact
        // x = 0:1022
        // y = 1023:1 + 0.5
        // wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = TRUE, exact = FALSE)
        builder.accept(Arguments.of(
            IntStream.range(0, 1023).asDoubleStream().toArray(),
            IntStream.range(0, 1023).mapToDouble(i -> 1023 - i + 0.5).toArray(),
            261121, new double[] {0.9353698195565141, 0.53235715699599806, 0.46768490977825705},
            0, PValueMethod.ASYMPTOTIC, true, 4 * eps
        ));

        // expected difference
        builder.accept(Arguments.of(
            new double[] {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30},
            new double[] {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29},
            19, new double[] {0.72228296271619219, 0.68220693886061667, 0.36114148135809609},
            0.5, PValueMethod.ASYMPTOTIC, true, 2 * eps
        ));

        // No continuity correction
        builder.accept(Arguments.of(
            new double[] {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30},
            new double[] {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29},
            40, new double[] {0.038151710173415156, 0.019075855086707578, 0.98092414491329238},
            0, PValueMethod.ASYMPTOTIC, false, 2 * eps
        ));
        // Edge case where statistic == 0
        builder.accept(Arguments.of(
            new double[] {1, 2, 3},
            new double[] {2, 4, 6},
            0, new double[] {0.10880943004054568, 0.94559528497972711, 0.054404715020272838},
            0, PValueMethod.ASYMPTOTIC, false, 4 * eps
        ));
        // Target values computed using R version 3.4.0, including the continuity correction e.g.
        // R removes any z = x - y where z == 0, so we generate data with no zero deltas (see above)
        builder.accept(Arguments.of(
            new double[] {0.716, 0.113, 0.507, 0.506, -0.11, -1.063, -1.543, -0.735, -0.38, 0.751, 0.778, -1.923, 0.053, 0.342, -0.121, -0.045, -0.086, 0.153, 0.802, -0.402, 1.443, 1.355, -0.518, -0.884, 0.027, 0.129, -1.219, -1.461, -1.426, -0.975, 1.332, -0.472, -1.028, -1.029, -0.962, 0.402, 1.0, 0.147, -0.536, 0.947, 0.721, -0.154, 0.061, -2.281, -1.993, -0.283, 0.02, -0.41, -1.586, 0.129},
            new double[] {0.143, -0.741, -2.008, -0.96, 0.474, 1.527, 1.514, 0.551, -2.142, -0.127, -1.152, 1.531, 0.557, 1.256, 0.629, 1.265, 0.488, -1.981, -0.489, -1.21, 1.52, 0.362, -0.235, -0.207, 1.497, 0.211, 2.054, 0.243, -1.18, -0.817, -0.42, 0.166, 1.575, -1.642, -0.389, -0.482, 2.789, -1.452, 0.795, 0.472, -0.221, -1.589, 1.26, -0.168, 0.647, -0.582, -0.999, -0.217, 0.058, 0.77},
            530.5, new double[] {0.30164750033598381, 0.84917624983200812, 0.1508237501679919},
            0, PValueMethod.ASYMPTOTIC, false, 4 * eps
        ));

        // Exact computation requested but it is not currently supported. Compare to R inexact.
        // x = 0:1023
        // y = 1024:1 + 0.5
        // wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = TRUE, exact = FALSE)
        // Currently the code does not throw an exception
        builder.accept(Arguments.of(
            IntStream.range(0, 1024).asDoubleStream().toArray(),
            IntStream.range(0, 1024).mapToDouble(i -> 1024 - i + 0.5).toArray(),
            261632, new double[] {0.93538020545645328, 0.5323519025146467, 0.46769010272822664},
            0, PValueMethod.EXACT, true, 4 * eps
        ));
        builder.accept(Arguments.of(
            IntStream.range(0, 1024).asDoubleStream().toArray(),
            IntStream.range(0, 1024).mapToDouble(i -> 1024 - i + 0.5).toArray(),
            261632, new double[] {0.93538020545645328, 0.5323519025146467, 0.46769010272822664},
            0, PValueMethod.ASYMPTOTIC, true, 4 * eps
        ));

        // Edge cases with zeros (x == y) and/or tied absolute differences (|z|)
        // Cannot be tested in R as it removes zeros.
        // Use scipy with the non-standard 'pratt' option.
        // scipy.stats.wilcoxon(x, y, zero_method='pratt', correction=True, alternative='two-sided', method='exact')
        // Note: 'two-sided' returns min(W+, W-). We always use W+.

        // Base data: y = x - z
        final double[] x = {1, 3, 5, 6, 8, 9, 13, 17};
        final double[] z = {-7, -5, -3, -1, 2, 4, 6, 8};
        // Zeros
        builder.accept(Arguments.of(
            x,
            IntStream.range(0, x.length).mapToDouble(i -> i == 3 ? x[i] : x[i] - z[i]).toArray(),
            20, new double[] {0.7789060199126654, 0.3894530099563327, 0.6631653378127099},
            0, PValueMethod.EXACT, true, 2 * eps
        ));
        // Tied |z|.
        // scipy.stats 'exact' method ignores tied |z|, so explicitly use method='approx'.
        // "There is no clear consensus among references on which method most accurately
        // approximates the p-value for small samples in the presence of zeros and/or
        // ties. In any case, this is the behavior of wilcoxon when method='auto':
        // ``method='exact' is used when len(d) <= 50 and *there are no zeros*; otherwise,
        // method='approx' is used."
        // In the java code we use the approximation if there are zeros or ties.
        builder.accept(Arguments.of(
            x,
            IntStream.range(0, x.length).mapToDouble(i -> i == 5 ? x[i] - z[i - 1] : x[i] - z[i]).toArray(),
            19, new double[] {0.9441140955776399, 0.47205704778881996, 0.5832831898888379},
            0, PValueMethod.EXACT, true, 2 * eps
        ));
        // Zeros and tied |z|
        builder.accept(Arguments.of(
            x,
            IntStream.range(0, x.length).mapToDouble(i -> {
                if (i == 3) {
                    return x[i];
                }
                return i == 5 ? x[i] - z[i - 1] : x[i] - z[i];
            }).toArray(),
            19, new double[] {0.8882288681198502, 0.4441144340599251, 0.6106798313095702},
            0, PValueMethod.EXACT, true, 2 * eps
        ));

        return builder.build();
    }

    /**
     * Test with a large sample. This tests the {@code exact} parameter does not
     * trigger an exception when the sample is too large. If this behaviour changes
     * this test can be updated to assert the exception.
     */
    @ParameterizedTest
    @ValueSource(ints = {2000, 3000})
    void testBigDataSet(int n) {
        final double[] x = IntStream.range(0, n).asDoubleStream().toArray();
        final double[] y = IntStream.range(0, n).mapToDouble(i -> n - i + 0.5).toArray();
        final WilcoxonSignedRankTest test = WilcoxonSignedRankTest.withDefaults().with(PValueMethod.ASYMPTOTIC);
        final double p = test.test(x, y).getPValue();
        Assertions.assertNotEquals(0, p);
        Assertions.assertEquals(p, test.with(PValueMethod.AUTO).test(x, y).getPValue());
        Assertions.assertEquals(p, test.with(PValueMethod.EXACT).test(x, y).getPValue());
    }

    /**
     * Checks for ties in the absolute differences of the data
     * (i.e. a shared difference between the two samples
     * {@code |x[i] - y[i]| == |x[j] - y[j]|}).
     *
     * @param x Sample 1.
     * @param y Sample 2.
     * @return true if at least 1 absolute difference is shared
     */
    private static boolean hasTies(double[] x, double[] y) {
        final double[] z = x.clone();
        for (int i = 0; i < x.length; i++) {
            z[i] = Math.abs(x[i] - y[i]);
        }
        Arrays.sort(z);
        for (int i = 1; i < z.length; i++) {
            if (z[i - 1] == z[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for ties in the absolute data (i.e. {@code |z[i]| == |z[j]|}).
     *
     * @param z Differences.
     * @return true if at least 1 absolute difference is shared
     */
    private static boolean hasTies(double[] z) {
        z = Arrays.stream(z).map(Math::abs).toArray();
        Arrays.sort(z);
        for (int i = 1; i < z.length; i++) {
            if (z[i - 1] == z[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for zeros in the difference data (i.e. {@code x[i] == y[i]}.
     *
     * @param x Sample 1.
     * @param y Sample 2.
     * @return true if at least 1 difference is zero
     */
    private static int countZeros(double[] x, double[] y) {
        int c = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] == y[i]) {
                c++;
            }
        }
        return c;
    }

    /**
     * Checks for zeros in the difference data.
     *
     * @param z Differences.
     * @return true if at least 1 difference is zero
     */
    private static boolean hasZeros(double[] z) {
        for (final double d : z) {
            if (d == 0) {
                return true;
            }
        }
        return false;
    }
}
