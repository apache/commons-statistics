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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.statistics.inference.UnconditionedExactTest.Candidates;
import org.apache.commons.statistics.inference.UnconditionedExactTest.Method;
import org.apache.commons.statistics.inference.UnconditionedExactTest.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link UnconditionedExactTest}.
 */
class UnconditionedExactTestTest {

    /**
     * Test the candidates data structure expands as expected. This test ensures the
     * structure can store multiple minima within tolerance of the best minima. It
     * hits edge case is not reached by current test data cases, such as more than two
     * minima and more minima than the initial capacity.
     */
    @ParameterizedTest
    @MethodSource
    void testCandidatesList(int max, double eps, double[][] points, double[][] expected) {
        final Candidates candidates = new Candidates(max, eps);
        Assertions.assertNull(candidates.getMinimum(), "min of empty");
        Arrays.stream(points).forEach(x -> candidates.add(x[0], x[1]));
        ArrayList<double[]> actual = new ArrayList<>();
        candidates.forEach(actual::add);
        Assertions.assertArrayEquals(expected, actual.toArray(new double[0][0]), "forEach candidates");
        // Check the min
        Optional<double[]> min = Arrays.stream(expected).reduce((x, y) -> x[1] > y[1] ? y : x);
        Assertions.assertArrayEquals(min.orElseGet(() -> null), candidates.getMinimum(), "min");
    }

    static Stream<Arguments> testCandidatesList() {
        final double nan = Double.NaN;
        final double inf = Double.POSITIVE_INFINITY;
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Add nothing
        builder.add(Arguments.of(1, 0.05,
            new double[0][0],
            new double[0][0]));
        // Single point
        builder.add(Arguments.of(1, 0.05,
            new double[][] {{0, 1}},
            new double[][] {{0, 1}}));
        // Multiple values
        builder.add(Arguments.of(10, 0.05,
            new double[][] {{0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}},
            new double[][] {{0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}}));
        // Only store 1 point. Keeps the last encountered (by replacement)
        builder.add(Arguments.of(1, 0.05,
            new double[][] {{0, 1}, {1, 1}},
            new double[][] {{1, 1}}));
        // Only store points within 10%
        builder.add(Arguments.of(10, 0.1,
            new double[][] {{0, 1}, {1, 1.1}, {2, 1.2}, {3, 1.05}},
            new double[][] {{0, 1}, {1, 1.1}, {3, 1.05}}));
        // Only store points within 5% of min encountered
        builder.add(Arguments.of(10, 0.05,
            new double[][] {{0, 2}, {1, 1}, {2, 1.2}, {3, 1.05}},
            new double[][] {{1, 1}, {3, 1.05}}));
        // Add only NaN - Stores only the first NaN
        builder.add(Arguments.of(10, 0.05,
            new double[][] {{0, nan}, {1, nan}, {2, nan}},
            new double[][] {{0, nan}}));
        // Add non-NaN and NaN. NaNs are removed as above the relative threshold
        builder.add(Arguments.of(10, 0.05,
            new double[][] {{0, nan}, {1, 1.05}, {2, 1.05}},
            new double[][] {{1, 1.05}, {2, 1.05}}));
        builder.add(Arguments.of(10, 0.05,
            new double[][] {{0, 1.05}, {1, nan}, {2, 1.05}},
            new double[][] {{0, 1.05}, {2, 1.05}}));
        // No tolerance
        builder.add(Arguments.of(10, 0,
            new double[][] {{0, 1}, {1, 1.1}, {2, 0.99}},
            new double[][] {{2, 0.99}}));
        // No tolerance. Handles +inf despite inf*0 = NaN
        builder.add(Arguments.of(10, 0,
            new double[][] {{0, inf}, {1, inf}},
            new double[][] {{0, inf}, {1, inf}}));
        builder.add(Arguments.of(10, 0,
            new double[][] {{0, inf}, {1, inf}, {2, 1.0e300}},
            new double[][] {{2, 1.0e300}}));
        return builder.build();
    }

    @ParameterizedTest
    @CsvSource({
        "10, 3, 0.05",
        "5, 1, 0.0",
        "10, 10, 0.1",
        "10, 10, 0.5",
    })
    void testCandidatesListRandom(int n, int max, double eps) {
        // Use a fixed seed known to avoid generating duplicates
        final SplittableRandom rng = new SplittableRandom(2374897234780L);
        double[] v = rng.doubles(n, -1, 0).toArray();
        double min = Arrays.stream(v).min().getAsDouble();
        double[][] actual = IntStream.range(0, n)
            .mapToObj(i -> new double[] {i, v[i]}).toArray(double[][]::new);
        double threshold = min + eps * Math.abs(min);
        double[][] expected = IntStream.range(0, n)
            .filter(i -> v[i] <= threshold)
            .mapToObj(i -> new double[] {i, v[i]}).limit(n).toArray(double[][]::new);
        testCandidatesList(max, eps, actual, expected);
    }

    @Test
    void testInvalidOptionsThrows() {
        final UnconditionedExactTest test = UnconditionedExactTest.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((AlternativeHypothesis) null));
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((Method) null));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 0, -1, Integer.MIN_VALUE})
    void testInvalidInitialPointsThrows(int points) {
        final UnconditionedExactTest test = UnconditionedExactTest.withDefaults();
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            test.withInitialPoints(points));
    }

    @Test
    void testUnconditionedExactTestInvalidTableThrows() {
        assertUnconditionedExactTestInvalidTableThrows(UnconditionedExactTest.withDefaults()::statistic);
        assertUnconditionedExactTestInvalidTableThrows(UnconditionedExactTest.withDefaults()::test);
    }

    private void assertUnconditionedExactTestInvalidTableThrows(Consumer<int[][]> action) {
        // Non 2-by-2 input
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(new int[3][3]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(new int[2][1]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(new int[1][2]));
        // Non-square input
        final int[][] v = {{1, 2}, {3}};
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(v));
        final int[][] w = {{1}, {2, 3}};
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(w));
        // Columns sum is zero
        final int[][] y = {{0, 1}, {0, 2}};
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(y), "column", "sum", "0", "zero");
        final int[][] z = {{1, 0}, {2, 0}};
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(z), "column", "sum", "1", "zero");
        // Maximum tables (m+1)*(n+1) > max array size
        final int x = 1 << 16;
        final int[][] big = {{0, 0}, {x, x}};
        final long product = (x + 1L) * (x + 1L);
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(big), Long.toString(product));
    }

    @ParameterizedTest
    @CsvSource({
        // Sum zero
        "0, 0, 0, 0",
        // Negative values
        "-1, 2, 3, 4",
        "1, -2, 3, 4",
        "1, 2, -3, 4",
        "1, 2, 3, -4",
        // Overflow (sum not an integer)
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
    void testUnconditionedExactTestThrows(int a, int b, int c, int d) {
        final int[][] table = {{a, b}, {c, d}};
        final UnconditionedExactTest test = UnconditionedExactTest.withDefaults();
        Assertions.assertThrows(IllegalArgumentException.class, () -> test.statistic(table), "statistic");
        Assertions.assertThrows(IllegalArgumentException.class, () -> test.test(table), "test");
    }

    /**
     * Test the p-value at the mode.
     * Reuses the test cases from the FisherExactTest to ensure the test statistic is
     * computed correctly.
     */
    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.inference.FisherExactTestTest#testMode"})
    void testMode(int n, int kk, int nn, int k) {
        final int[][] table = {
            {k, kk - k},
            {n - k, nn - (n + kk) + k}
        };
        final double pval = FisherExactTest.withDefaults().test(table).getPValue();
        Assertions.assertEquals(pval, UnconditionedExactTest.withDefaults().statistic(table), "statistic");
        Assertions.assertEquals(pval, UnconditionedExactTest.withDefaults().test(table).getStatistic(), "test statistic");
    }

    /**
     * Test the unconditioned exact test.
     *
     * <p>Note that this test involves an optional optimization to find the nuisance
     * parameter. As such there is some discrepancy between reference implementations in
     * SciPy and R. For the purpose of this test the number of initial search points has
     * been matched to the reference implementation where possible to allow a fair
     * comparison. SciPy uses by default a sobol sequence with a power of 2 number of points.
     * This generates n points uniformly in the half-open interval [0, 1) using an increment of
     * 1/n. The R Exact package uses n points uniformly in the range [1e-5, 1 - 1e-5] using
     * an increment of (1 - 2e-5) / (n-1) as it includes both end points.
     * <pre>
     *        lo     hi          inc
     * SciPy  0      1 - 1/n     1/n
     * R      1e-5   1 - 1e-5    (1-2e-5)/(n-1)
     * </pre>
     *
     * <p>Note that Barnard's original paper (1947) specifies that the nuisance parameter
     * is in the open interval (0, 1); thus SciPy's enumeration that includes 0 is
     * incorrect by this definition. Also note that the optimization routine used by SciPy uses
     * the closed interval [0, 1] for the bounds. The current Java implementation has been
     * matched to the R Exact package to enumerate almost the entire range of the open interval
     * (0, 1). Thus to closely match the 32 default points used by SciPy requires 33 points.
     *
     * <p>If the nuisance parameter approaches either 0 or 1 then this edge case table is likely
     * to generate a p-value close to 1. For significance testing the result of 1.0 or almost
     * 1.0 does not matter and a lower relative epsilon is used to allow the test to pass.
     */
    @ParameterizedTest
    @MethodSource(value = {"testZPooled", "testZUnpooled", "testBoschloo"})
    void testUnconditionedExactTest(int a, int b, int c, int d, Method method,
            int points, boolean optimize, double[] stat, double[] p, double statisticEps, double pEps) {
        final int[][] table = {{a, b}, {c, d}};
        UnconditionedExactTest test = UnconditionedExactTest.withDefaults()
            .with(method).withInitialPoints(points).withOptimize(optimize);
        double statistic = 0;
        if (stat.length == 1) {
            statistic = test.statistic(table);
            TestUtils.assertRelativelyEquals(stat[0], statistic, statisticEps, "statistic");
        }
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            final SignificanceResult r = test.test(table);
            if (stat.length != 1) {
                statistic = test.statistic(table);
                TestUtils.assertRelativelyEquals(stat[i], statistic, statisticEps, () -> h + " statistic");
                // Special case where Boschloo's test should use the Fisher exact test p-value
                if (method == Method.BOSCHLOO) {
                    Assertions.assertEquals(FisherExactTest.withDefaults().with(h).test(table).getPValue(), statistic,
                        () -> h + " Fisher p-value");
                }
            }
            Assertions.assertEquals(statistic, r.getStatistic(), () -> h + " statistic mismatch");
            TestUtils.assertProbability(p[i++], r.getPValue(), pEps, () -> h + " p-value");
        }
    }

    static Stream<Arguments> testZPooled() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        // statistic is in the same order; or an array of length 1 if the statistic is the
        // same.

        // Reference data:
        //
        // scipy.stats.barnard_exact (version 1.9.3)
        // barnard_exact([[7, 12], [8, 3]], alternative='greater')
        //
        // R: 3.6.1; Exact 3.2.0
        // require('Exact')
        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='z-pooled', alternative='g', npNumbers=33)[c(1,3)]

        // The implementation more closely matches SciPy than R for the z value; the p-value
        // is dependent on the z computation and also the sum to find the nuisance parameter.
        // This particularly effects the test without optimization using R as the reference.

        final Stream.Builder<Arguments> builder = Stream.builder();
        // SciPy
        builder.add(Arguments.of(7, 12, 8, 3,
            Method.Z_POOLED, 33, true,
            new double[] {-1.8943380760602064},
            new double[] {0.06815343273153582, 1, 0.034076716365767874}, 1e-15, 7e-15));
        // R without optimization
        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='z-pooled', alternative='t', npNumbers=15, ref.pvalue=FALSE)[c(1,3)]
        builder.add(Arguments.of(7, 12, 8, 3,
            Method.Z_POOLED, 15, false,
            new double[] {-1.8943380760599999},
            new double[] {0.067726937136699999, 1, 0.033863468568400001}, 2e-13, 3e-3));

        builder.add(Arguments.of(8, 2, 1, 5,
            Method.Z_POOLED, 33, true,
            new double[] {2.4722801848029503},
            new double[] {0.021270751953124997, 0.012192436131969697, 1}, 1e-15, 4e-15));

        // SciPy example for Boschloo's test
        builder.add(Arguments.of(74, 31, 43, 32,
            Method.Z_POOLED, 33, true,
            new double[] {1.8225863590257527},
            // R:         0.070148380863900003, 0.046915929572300001, 1
            new double[] {0.070148380862763000, 0.04691595404276874, 1}, 1e-15, 2e-13));

        // Larger tables.
        builder.add(Arguments.of(123, 92, 424, 113,
            Method.Z_POOLED, 33, true,
            new double[] {-6.051476769955081},
            new double[] {9.978753707498542e-09, 1, 9.978753707498542e-09}, 1e-15, 1e-12));

        // These cases have a p-value function with multiple maxima with similar values.
        // Enumerating the range may identify the wrong candidate to optimize when
        // the p-values are close. Using an implementation
        // that optimizes multiple candidates finds the global maximum p-value.
        // Note: The R implementation appears to only optimize the best candidate. The SciPy
        // implementation is more robust to multiple similar maxima.
        // The Java implementation picks the top candidates; all have to be within a tolerance
        // of the minimum candidate.
        builder.add(Arguments.of(123, 92, 424, 313,
            Method.Z_POOLED, 33, true,
            new double[] {-0.08382278919954203},
            // R:         1                 , 1, 0.47180327976999997 (33 points)
            //                                   0.47653831055000001 (100 points)
            new double[] {0.9480782554147217, 1, 0.4765383214549828}, 1e-15, 1e-12));
        builder.add(Arguments.of(67, 42, 23, 88,
            Method.Z_POOLED, 33, true,
            new double[] {6.145972185362486},
            // R:         4.8482474012700005e-10, 4.09507656074e-10 (33 points)
            new double[] {4.8482474013205090e-10, 4.095910401645987e-10, 1}, 1e-15, 1e-13));

        // Edge case: p0 == p1
        // SciPy allows the nuisance parameter to be zero so computes p = 1.0
        // R Exact does not and computes p = 0.99986000910299999
        // The Java implementation computes p = 0.9999999...
        // To pass the non-exact assertion with 1.0 we use nextDown
        builder.add(Arguments.of(7, 12, 7, 12,
            Method.Z_POOLED, 33, true,
            new double[] {0},
            new double[] {1, Math.nextDown(1.0), Math.nextDown(1.0)}, 1e-15, 3e-7));

        // Edge case minimum tables.
        builder.add(Arguments.of(1, 0, 0, 1,
            Method.Z_POOLED, 33, true,
            new double[] {1.414213562373095},
            new double[] {0.5, 0.25, 1}, 1e-15, 1e-15));
        builder.add(Arguments.of(0, 1, 1, 0,
            Method.Z_POOLED, 33, true,
            new double[] {-1.414213562373095},
            new double[] {0.5, 1, 0.25}, 1e-15, 1e-15));
        return builder.build();
    }

    static Stream<Arguments> testZUnpooled() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        // statistic is in the same order; or an array of length 1 if the statistic is the
        // same.

        // Reference data:
        //
        // scipy.stats.barnard_exact (version 1.9.3)
        // barnard_exact([[7, 12], [8, 3]], pooled=False, alternative='greater')
        //
        // R: 3.6.1; Exact 3.2.0
        // require('Exact')
        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='z-unpooled', alternative='g', npNumbers=33)[c(1,3)]

        // The implementation more closely matches SciPy than R for the z value; the p-value
        // is dependent on the z computation and also the sum to find the nuisance parameter.
        // This particularly effects the test without optimization using R as the reference.

        final Stream.Builder<Arguments> builder = Stream.builder();
        // SciPy
        builder.add(Arguments.of(7, 12, 8, 3,
            Method.Z_UNPOOLED, 33, true,
            new double[] {-2.018932132718121},
            new double[] {0.06815343273153582, 1, 0.034076716365767874}, 1e-15, 7e-15));
        // R without optimization
        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='z-unpooled', alternative='t', npNumbers=15, ref.pvalue=FALSE)[c(1,3)]
        builder.add(Arguments.of(7, 12, 8, 3,
            Method.Z_UNPOOLED, 15, false,
            new double[] {-2.0189321327199998},
            new double[] {0.067726937136699999, 1, 0.033863468568400001}, 1e-12, 3e-3));

        builder.add(Arguments.of(8, 2, 1, 5,
            Method.Z_UNPOOLED, 33, true,
            new double[] {3.011042066675127},
            new double[] {0.02511596679687499, 0.012721998585868003, 1}, 1e-15, 3e-15));

        // SciPy example for Boschloo's test
        builder.add(Arguments.of(74, 31, 43, 32,
            Method.Z_UNPOOLED, 33, true,
            new double[] {1.8197405688188515},
            // R:         0.087944196092599999, 0.076136257998199994, 1
            new double[] {0.087944212768358360, 0.07613626462554106, 1}, 1e-15, 2e-13));

        // Larger tables.
        builder.add(Arguments.of(123, 92, 424, 113,
            Method.Z_UNPOOLED, 33, true,
            new double[] {-5.733263012126067},
            new double[] {7.1612401340371655e-06, 1, 7.1612401184869755e-06}, 1e-15, 1e-12));

        builder.add(Arguments.of(123, 92, 424, 313,
            Method.Z_UNPOOLED, 33, true,
            new double[] {-0.0837781159838051},
            // R:         1                 , 1, 0.47180327976999997 (33 points)
            //                                   0.47653831055000001 (100 points)
            new double[] {0.9480782554147217, 1, 0.4765383214549828}, 1e-15, 1e-12));
        builder.add(Arguments.of(67, 42, 23, 88,
            Method.Z_UNPOOLED, 33, true,
            new double[] {6.838950390748158},
            // R:         8.7815106038399997e-10, 8.6790731909900004e-10 (33 points)
            new double[] {8.7815108621324530e-10, 8.679073193094083e-10, 1}, 1e-15, 1e-13));

        // Edge case: p0 == p1
        // SciPy allows the nuisance parameter to be zero so computes p = 1.0
        // R Exact does not and computes p = 0.99986000910299999
        // To pass the non-exact assertion with 1.0 we use nextDown
        builder.add(Arguments.of(7, 12, 7, 12,
            Method.Z_UNPOOLED, 33, true,
            new double[] {0},
            new double[] {1, Math.nextDown(1.0), Math.nextDown(1.0)}, 1e-15, 3e-7));

        // Edge case minimum tables.
        builder.add(Arguments.of(1, 0, 0, 1,
            Method.Z_UNPOOLED, 33, true,
            new double[] {Double.POSITIVE_INFINITY},
            new double[] {0.5, 0.25, 1}, 1e-15, 1e-15));
        builder.add(Arguments.of(0, 1, 1, 0,
            Method.Z_UNPOOLED, 33, true,
            new double[] {Double.NEGATIVE_INFINITY},
            new double[] {0.5, 1, 0.25}, 1e-15, 1e-15));
        return builder.build();
    }

    static Stream<Arguments> testBoschloo() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        // statistic is in the same order; or an array of length 1 if the statistic is the
        // same.

        // Reference data:
        //
        // scipy.stats.boschloo_exact (version 1.9.3)
        // barnard_exact([[7, 12], [8, 3]], pooled=False, alternative='greater')
        // Note: Only for 'less' or 'greater'. The 'two-sided' alternative uses double the
        // lowest one-sided result.
        //
        // R: 3.6.1; Exact 3.2.0
        // require('Exact')
        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='boschloo', alternative='g', npNumbers=33)[c(1,3)]
        // This uses tsmethod='square' by default for the two-sided alternative using the two-sided
        // Fisher p-value. The method 'central' matches the SciPy implementation by doubling the
        // one-sided result.

        // The p-value is dependent on the library implementation of the hypergeometric distribution.
        // Note that SciPy does not use p <= p0. It uses p <= p0*(1+1e-13) to allow some floating
        // point error when comparing the Fisher p-value's. See https://github.com/scipy/scipy/pull/14178
        // This has not been implemented in the Java code which uses an exact inequality.
        //
        // The following reference data uses R for the two-sided test statistic as this is not
        // supported by SciPy. The two-sided p-value can use the SciPy result if it closely matches
        // the R result (i.e. doubling the one-sided test is valid); the SciPy result is provided
        // below as BoschlooExactResult showing that the statistic can be different and the p-value
        // is often not within 2 significant figures.
        //
        // SciPy is used for the one-sided tests (as the values are closer to the Java implementation).
        // The relative epsilon values are typically limited by the R implementation.
        //
        // Note that the statistic is also checked for an exact match against the
        // FisherExactTest p-value to verify the two implementations are consistent.

        final Stream.Builder<Arguments> builder = Stream.builder();
        // BoschlooExactResult(statistic=0.06406796601699151, pvalue=0.06821830932319489)
        builder.add(Arguments.of(7, 12, 8, 3,
            Method.BOSCHLOO, 33, true,
            new double[] {0.12813593203400001, 0.9895302348825588, 0.06406796601699151},
            new double[] {0.06821830932319489, 0.9771215347573191, 0.034109154661597446}, 2e-13, 7e-15));
        // R without optimization
        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='boschloo', alternative='t', npNumbers=15, ref.pvalue=FALSE)[c(1,3)]
        builder.add(Arguments.of(7, 12, 8, 3,
            Method.BOSCHLOO, 15, false,
            new double[] {0.12813593203400001, 0.98953023488299996, 0.064067966017000003},
            new double[] {0.067726937136699999, 0.97712153475700003, 0.033863468568400001}, 5e-13, 2e-12));

        // BoschlooExactResult(statistic=0.024475524475524483, pvalue=0.024384872263939393)
        builder.add(Arguments.of(8, 2, 1, 5,
            Method.BOSCHLOO, 33, true,
            new double[] {0.034965034965000003, 0.024475524475524483, 0.9991258741258742},
            new double[] {0.019304891712500001, 0.012192436131969697, 0.9870565910170339}, 1e-12, 4e-10));

        // SciPy example for Boschloo's test
        // BoschlooExactResult(statistic=0.048312210086912236, pvalue=0.0711281286030935)
        builder.add(Arguments.of(74, 31, 43, 32,
            Method.BOSCHLOO, 33, true,
            new double[] {0.081794674954999994, 0.048312210086912236, 0.9759961281714722},
            new double[] {0.072078751323299994, 0.03556406430154675, 0.9665032229005569}, 2e-14, 3e-10));

        // Larger tables.
        // SciPy is slow here. It is fast for barnard_exact which shares the p-value optimization
        // code so the slow speed may be in the selection of the more extreme tables using the
        // hypergeom implementation.

        // BoschlooExactResult(statistic=2.826445350300923e-09, pvalue=3.618688125749579e-09)
        builder.add(Arguments.of(123, 92, 424, 113,
            Method.BOSCHLOO, 33, true,
            new double[] {4.4347446136499998e-09, 0.9999999990211362, 2.826445350300923e-09},
            new double[] {3.6182638424499998e-09, 0.9999999983112282, 1.8093440628747895e-09}, 6e-13, 7e-8));
        // BoschlooExactResult(statistic=0.4969777824620944, pvalue=0.9428559669698119)
        // Here the p-value function for greater/less is flat with many local minima.
        // SciPy does better for the 'less' function.
        // R does better for the 'greater' function.
        // Using 33 points matches SciPy for greater. This must be increased to 50 points for
        // less where we can match the higher result from R.
        // Note the true maxima for 'less' is located between two points (when sampled with 33
        // points) that are not local maxima. Thus the optimization must increase the number
        // of points to identify better start candidates, or use an optimization routine that
        // detects the flat function and optimizes over a broad range.
        builder.add(Arguments.of(123, 92, 424, 313,
            Method.BOSCHLOO, 50, true,
            new double[] {0.93767135630200005, 0.5652358165696375, 0.4969777824620944},
            // R:                              0.53928064412099996,0.47134187918800002 (33 points)
            //                                 0.53935860353700005,0.47142798346100001 (60 points)
            //                                 0.53935860234499999,0.47142798338899999 (100 point - worse than with 60 points)
            // SciPy:                          0.5392806447420694, 0.47142798348490594 (32 points)
            //                                 0.539358603564275,  0.4714279834851383  (128 points)
            new double[] {0.93370280995499999, 0.53935860353700005, 0.47142798348490594}, 2e-13, 5e-9));
        // BoschlooExactResult(statistic=5.169903676658518e-10, pvalue=5.215196886364725e-10)
        builder.add(Arguments.of(67, 42, 23, 88,
            Method.BOSCHLOO, 33, true,
            new double[] {8.7327872452500003e-10, 5.169903676658518e-10, 0.9999999999188225},
            new double[] {5.6543598496900003e-10, 2.6075984431823626e-10, 0.999999999768292}, 3e-13, 1e-9));

        // Edge case: p0 == p1
        builder.add(Arguments.of(7, 12, 7, 12,
            Method.BOSCHLOO, 33, true,
            new double[] {1, 0.6312858130655684, 0.6312858130655685},
            new double[] {1, 0.5160595470655773, 0.5160595470655791}, 1e-15, 1e-14));

        // Edge case minimum tables.
        builder.add(Arguments.of(1, 0, 0, 1,
            Method.BOSCHLOO, 33, true,
            new double[] {1, 0.5, 1},
            new double[] {1, 0.25, 1}, 1e-15, 1e-15));
        builder.add(Arguments.of(0, 1, 1, 0,
            Method.BOSCHLOO, 33, true,
            new double[] {1, 1, 0.5},
            new double[] {1, 1, 0.25}, 1e-15, 1e-15));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testNuisanceParameter(int a, int b, int c, int d,
        AlternativeHypothesis alternative, Method method, int points, boolean optimize,
        double[] expected, double[] eps) {
        final int[][] table = {{a, b}, {c, d}};
        final UnconditionedExactTest test = UnconditionedExactTest.withDefaults()
            .with(alternative).with(method).withInitialPoints(points).withOptimize(optimize);
        double statistic = test.statistic(table);
        TestUtils.assertRelativelyEquals(expected[0], statistic, eps[0], "statistic");
        final Result r = test.test(table);
        Assertions.assertEquals(statistic, r.getStatistic(), "statistic mismatch");
        TestUtils.assertProbability(expected[1], r.getPValue(), eps[1], "p-value");
        TestUtils.assertRelativelyEquals(expected[2], r.getNuisanceParameter(), eps[2], "nuisance parameter");
    }

    static Stream<Arguments> testNuisanceParameter() {
        // expected values are in the order: statistic, p-value, nuisance parameter with
        // relative eps tolerance for each
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Use the R implementation which provides a plot of the p-value verses the
        // nuisance parameter to aid in data selection. Data requires no symmetry
        // (i.e. no exact matching p-values) as the R implementation returns all maxima
        // and the Java implementation only 1.
        // The epsilons are relatively low as often the Java implementation finds a
        // better p-value, even with fewer initial points.

        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='z-unpooled', alternative='t', npNumbers=33, ref.pvalue=FALSE)[c(1, 3, 8)]
        builder.add(Arguments.of(7, 12, 8, 4,
            AlternativeHypothesis.LESS_THAN, Method.Z_UNPOOLED, 33, false,
            new double[] {-1.68390442589, 0.066796091742499994, 0.65624687500000001},
            new double[] {1e-12, 1e-10, 1e-10}));
        // exact.test(matrix(c(7, 12, 8, 3), 2, 2), method='z-unpooled', alternative='t', npNumbers=33, ref.pvalue=TRUE)[c(1, 3, 8)]
        builder.add(Arguments.of(7, 12, 8, 4,
            AlternativeHypothesis.LESS_THAN, Method.Z_UNPOOLED, 33, true,
            new double[] {-1.68390442589, 0.066810461623799999, 0.65134101325505922},
            new double[] {1e-12, 1e-10, 3e-6}));

        // Generated with npNumbers=100 but use the default of 33 for the Java implementation
        builder.add(Arguments.of(7, 12, 8, 7,
            AlternativeHypothesis.LESS_THAN, Method.Z_POOLED, 33, true,
            new double[] {-0.96159557564099996, 0.21902132934499999, 0.95694046624745288},
            new double[] {1e-12, 5e-8, 2e-5}));
        builder.add(Arguments.of(7, 8, 8, 6,
            AlternativeHypothesis.LESS_THAN, Method.Z_POOLED, 33, true,
            new double[] {-0.564160122652, 0.34711165127799998, 0.11654765288429031},
            new double[] {1e-12, 5e-8, 2e-4}));

        return builder.build();
    }
}
