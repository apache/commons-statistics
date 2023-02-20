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
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.HypergeometricDistribution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link FisherExactTest}.
 */
class FisherExactTestTest {

    @Test
    void testInvalidOptionsThrows() {
        final FisherExactTest test = FisherExactTest.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((AlternativeHypothesis) null));
    }

    @Test
    void testFisherExactTestInvalidTableThrows() {
        assertFisherExactTestInvalidTableThrows(FisherExactTest.withDefaults()::statistic);
        assertFisherExactTestInvalidTableThrows(FisherExactTest.withDefaults()::test);
    }

    private void assertFisherExactTestInvalidTableThrows(Consumer<int[][]> action) {
        // Non 2-by-2 input
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(new int[3][3]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(new int[2][1]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(new int[1][2]));
        // Non-square input
        final int[][] x = {{1, 2}, {3}};
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(x));
        final int[][] y = {{1}, {2, 3}};
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.accept(y));
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
    void testFisherExactTestThrows(int a, int b, int c, int d) {
        final int[][] table = {{a, b}, {c, d}};
        final FisherExactTest test = FisherExactTest.withDefaults();
        Assertions.assertThrows(IllegalArgumentException.class, () -> test.statistic(table), "statistic");
        Assertions.assertThrows(IllegalArgumentException.class, () -> test.test(table), "test");
    }

    /**
     * Test the Fisher exact test for each alternative hypothesis and all possible k given
     * the input table.
     */
    @ParameterizedTest
    @CsvSource({
        // The epsilon here is due to a difference in summation of p-values. This test
        // sums all p-values in the same stream which uses an extended precision sum.
        // The FisherExactTest adds the CDF and SF which use standard precision summations.
        "0, 0, 1, 2e-16",
        "1, 0, 1, 2e-16",
        "0, 1, 1, 2e-16",
        "1, 1, 1, 2e-16",
        "8, 7, 13, 1.1e-15",
        "10, 12, 24, 7e-16",
        "20, 25, 43, 3e-16",
        // Create a contingency table where the hypergeometric mode is 1.5
        "4, 2, 8, 3e-16",
    })
    void testFisherExactTest(int n, int kk, int nn, double eps) {
        final HypergeometricDistribution dist = HypergeometricDistribution.of(nn, kk, n);
        final int low = dist.getSupportLowerBound();
        final int high = dist.getSupportUpperBound();
        final double[] pk = IntStream.rangeClosed(0, high).mapToDouble(dist::probability).toArray();

        // Note: TestUtils.assertProbability expects exact equality when p is 0 or 1.
        // We *could* set the maximum for the sum to below 1 to avoid this.
        final double maxP = 1.0;

        final FisherExactTest twoSided = FisherExactTest.withDefaults();
        final FisherExactTest less = FisherExactTest.withDefaults().with(AlternativeHypothesis.LESS_THAN);
        final FisherExactTest greater = FisherExactTest.withDefaults().with(AlternativeHypothesis.GREATER_THAN);

        IntStream.rangeClosed(low, high).forEach(k -> {
            final int[][] table = {
                {k, kk - k},
                {n - k, nn - (n + kk) + k}
            };
            double expected;

            // One-sided
            expected = k == high ? 1 :
                Math.min(maxP, IntStream.rangeClosed(low, k).mapToDouble(i -> pk[i]).sum());
            TestUtils.assertProbability(expected,
                less.test(table).getPValue(), eps,
                () -> "less than: k=" + k);

            expected = k == low ? 1 :
                Math.min(maxP, IntStream.rangeClosed(k, high).mapToDouble(i -> pk[i]).sum());
            TestUtils.assertProbability(expected,
                greater.test(table).getPValue(), eps,
                () -> "greater than: k=" + k);

            // Two-sided
            // Find all i where Pr(X = i) <= Pr(X = k) and sum them.
            // Create an exact sum of 1.0 when all Pr(X = i) <= Pr(X = k).
            expected = IntStream.rangeClosed(low, high).noneMatch(i -> pk[i] > pk[k]) ? 1 :
                Math.min(maxP, Arrays.stream(pk).filter(x -> x <= pk[k]).sum());
            TestUtils.assertProbability(expected,
                    twoSided.test(table).getPValue(), eps,
                () -> "two-sided: k=" + k);
        });
    }

    /**
     * Test the p-value at the mode.
     * See also Math-1644 which is relevant to the same situation in the BinomialTest
     */
    @ParameterizedTest
    @CsvSource({
        // k = mode = ceil((n+1)(K+1) / (N+2)) - 1, floor((n+1)(K+1) / (N+2))
        // Exact mode
        "2, 2, 7, 1",
        "4, 3, 8, 2",
        // Rounded
        "2, 2, 5, 1",
        "2, 2, 5, 2",
        "4, 3, 10, 1",
        "4, 3, 10, 2",
        // mode == 1.5
        "4, 2, 8, 1",
        "4, 2, 8, 2",
    })
    void testMode(int n, int kk, int nn, int k) {
        final int[][] table = {
            {k, kk - k},
            {n - k, nn - (n + kk) + k}
        };
        final double pval = FisherExactTest.withDefaults().test(table).getPValue();
        Assertions.assertTrue(pval <= 1, () -> "pval=" + pval);
    }

    @ParameterizedTest
    @MethodSource
    void testFisherExactTest(int a, int b, int c, int d, double ratio, double[] p) {
        final int[][] table = {{a, b}, {c, d}};
        final double statistic = FisherExactTest.withDefaults().statistic(table);
        TestUtils.assertRelativelyEquals(ratio, statistic, 2e-16, "statistic");
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            final SignificanceResult r = FisherExactTest.withDefaults().with(h).test(table);
            Assertions.assertEquals(statistic, r.getStatistic(), "statistic mismatch");
            TestUtils.assertProbability(p[i++], r.getPValue(), 8e-15, "p-value");
        }
    }

    static Stream<Arguments> testFisherExactTest() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        // scipy.stats.fisher_exact (version 1.9.3)
        final Stream.Builder<Arguments> builder = Stream.builder();
        // SciPy's examples
        builder.add(Arguments.of(6, 2, 1, 4, 12,
            new double[] {0.10256410256410256, 0.08624708624708625, 0.9953379953379954}));
        builder.add(Arguments.of(8, 2, 1, 5, 20,
            new double[] {0.034965034965034975, 0.024475524475524483, 0.9991258741258742}));
        // Wikipedia example
        builder.add(Arguments.of(1, 9, 11, 3, 0.030303030303030304,
            new double[] {0.0027594561852200836, 0.9999663480953022, 0.0013797280926100418}));
        // Larger tables
        builder.add(Arguments.of(123, 92, 424, 313, 0.986951394585726,
            new double[] {0.9376713563018861, 0.5652358165696375, 0.4969777824620944}));
        builder.add(Arguments.of(123, 92, 424, 113, 0.3563115258408532,
            new double[] {4.434744613652278e-09, 0.9999999990211362, 2.826445350300923e-09}));
        builder.add(Arguments.of(67, 42, 23, 88, 6.10351966873706,
            new double[] {8.732787245248246e-10, 5.169903676658518e-10, 0.9999999999188225}));
        // Edge case tables
        builder.add(Arguments.of(0, 0, 0, 1, Double.NaN,
            new double[] {1, 1, 1}));
        builder.add(Arguments.of(1, 0, 0, 1, Double.POSITIVE_INFINITY,
            new double[] {1, 0.5, 1}));
        builder.add(Arguments.of(0, 1, 1, 0, 0,
            new double[] {1, 1, 0.5}));
        return builder.build();
    }
}
