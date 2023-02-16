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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.BinomialDistribution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link BinomialTest}.
 */
class BinomialTestTest {

    @Test
    void testInvalidOptionsThrows() {
        final BinomialTest test = BinomialTest.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((AlternativeHypothesis) null));
    }

    @ParameterizedTest
    @CsvSource({
        "10, 5, -1",
        "10, 5, 2",
        "10, -1, 0.5",
        "10, 11, 0.5",
        "-1, 5, 0.5",
        "1, 2, 0.5",
    })
    void testBinomialTestThrows(int n, int k, double p) {
        final BinomialTest test = BinomialTest.withDefaults();
        Assertions.assertThrows(IllegalArgumentException.class, () -> test.test(n, k, p));
    }

    /**
     * Test the binomial test for each alternative hypothesis and all possible k given
     * the number of trials (n).
     *
     * <p>Compute the p-values using a direct summation of the probability values.
     * See https://en.wikipedia.org/wiki/Binomial_test.
     *
     * <p>The summation is not as accurate as using the CDF / SF so the epsilon
     * is changed for larger number of trials (n). When n is very large summing the
     * individual p-values has too much error and is covered by
     * {@link #testBinomialTestLargeN(int, double)}.
     */
    @ParameterizedTest
    @CsvSource({
        "0, 0.25, 1e-15, 0",
        "1, 0.25, 1e-15, 0",
        "2, 0.25, 1e-15, 0",
        "0, 0.5, 1e-15, 0",
        "1, 0.5, 1e-15, 0",
        "2, 0.5, 1e-15, 0",
        "0, 0.75, 1e-15, 0",
        "1, 0.75, 1e-15, 0",
        "2, 0.75, 1e-15, 0",
        "10, 0.25, 2e-15, 0",
        "10, 0.49, 2e-15, 0",
        "10, 0.5, 2e-15, 0",
        "10, 0.51, 2e-15, 0",
        "10, 0.75, 2e-15, 0",
        "11, 0.25, 3e-15, 0",
        "11, 0.49, 2e-15, 0",
        "11, 0.5, 2e-15, 0",
        "11, 0.51, 2e-15, 0",
        "11, 0.75, 3e-15, 0",
        "5, 0.1, 2e-15, 0",
        "5, 0.7, 1e-15, 0",
        "20, 0.7, 3e-15, 0",
    })
    void testBinomialTest(int n, double p, double eps) {
        final BinomialDistribution dist = BinomialDistribution.of(n, p);
        final double[] pk = IntStream.rangeClosed(0, n).mapToDouble(dist::probability).toArray();

        // Note: TestUtils.assertProbability expects exact equality when p is 0 or 1.
        // Set the maximum for the sum to below 1 to avoid this.
        final double maxP = Math.nextDown(1.0);

        final BinomialTest twoSided = BinomialTest.withDefaults();
        final BinomialTest less = BinomialTest.withDefaults().with(AlternativeHypothesis.LESS_THAN);
        final BinomialTest greater = BinomialTest.withDefaults().with(AlternativeHypothesis.GREATER_THAN);

        IntStream.rangeClosed(0, n).forEach(k -> {
            double expected;

            // One-sided
            expected = Math.min(maxP, IntStream.rangeClosed(0, k).mapToDouble(i -> pk[i]).sum());
            TestUtils.assertProbability(expected,
                less.test(n, k, p).getPValue(), eps,
                () -> "less than: k=" + k);

            expected = Math.min(maxP, IntStream.rangeClosed(k, n).mapToDouble(i -> pk[i]).sum());
            TestUtils.assertProbability(expected,
                greater.test(n, k, p).getPValue(), eps,
                () -> "greater than: k=" + k);

            // Two-sided
            // Find all i where Pr(X = i) <= Pr(X = k) and sum them.
            expected = Math.min(maxP, Arrays.stream(pk).filter(x -> x <= pk[k]).sum());
            TestUtils.assertProbability(expected,
                    twoSided.test(n, k, p).getPValue(), eps,
                () -> "two-sided: k=" + k);
        });
    }

    /**
     * Test the binomial test for each alternative hypothesis and all possible k given
     * the number of trials (n).
     *
     * <p>Compute the p-values using a summation of the probability values.
     * See https://en.wikipedia.org/wiki/Binomial_test.
     *
     * <p>The summation is performed using the CDF / SF after look-up of the appropriate
     * value for k. The actual value must be an exact match to the expected. This test
     * verifies the binary search to locate indices in BinomialTest is correct.
     */
    @ParameterizedTest
    @CsvSource({
        "1234, 0.3",
        "1234, 0.55",
        "1234, 0.87",
        "12345, 0.3",
        "12345, 0.55",
        "12345, 0.87",
        // Case where the upper and lower mode have different values
        "10000, 0.49999",
        "10000, 0.50001",
    })
    void testBinomialTestLargeN(int n, double p) {
        final BinomialDistribution dist = BinomialDistribution.of(n, p);
        // Use the log probability here which has a larger range than probability and
        // can detect small p-values that are non-zero but less than Double.MIN_NORMAL.
        // It is also faster as it avoids a call to Math.exp.
        final double[] pk = IntStream.rangeClosed(0, n).mapToDouble(dist::logProbability).toArray();

        // Require an exact match.
        // This ensures the BinomialTest uses the CDF and SF.
        final double eps = 0;

        final BinomialTest twoSided = BinomialTest.withDefaults();
        final BinomialTest less = BinomialTest.withDefaults().with(AlternativeHypothesis.LESS_THAN);
        final BinomialTest greater = BinomialTest.withDefaults().with(AlternativeHypothesis.GREATER_THAN);

        IntStream.rangeClosed(0, n).forEach(k -> {
            double expected;

            // One-sided.
            expected = dist.cumulativeProbability(k);
            TestUtils.assertProbability(expected,
                less.test(n, k, p).getPValue(), eps,
                () -> "less than: k=" + k);

            expected = dist.survivalProbability(k - 1);
            TestUtils.assertProbability(expected,
                greater.test(n, k, p).getPValue(), eps,
                () -> "greater than: k=" + k);

            // Two-sided
            // Find all i where Pr(X = i) <= Pr(X = k) and sum them.
            int i = -1;
            while (i < n && pk[i + 1] <= pk[k]) {
                i++;
            }
            int j = n + 1;
            while (j > 0 && pk[j - 1] <= pk[k]) {
                j--;
            }
            expected = j <= i ? 1 : dist.cumulativeProbability(i) + dist.survivalProbability(j - 1);
            TestUtils.assertProbability(expected,
                twoSided.test(n, k, p).getPValue(), eps,
                () -> "two-sided: k=" + k);
        });
    }

    @Test
    void testBinomialTestPValues() {
        final int successes = 51;
        final int trials = 235;
        final double probability = 1.0 / 6.0;

        Assertions.assertEquals(0.04375, BinomialTest.withDefaults()
            .test(trials, successes, probability).getPValue(), 1e-4);
        Assertions.assertEquals(0.02654, BinomialTest.withDefaults().with(AlternativeHypothesis.GREATER_THAN)
            .test(trials, successes, probability).getPValue(), 1e-4);
        Assertions.assertEquals(0.982, BinomialTest.withDefaults().with(AlternativeHypothesis.LESS_THAN)
            .test(trials, successes, probability).getPValue(), 1e-4);

        // for special boundary conditions
        final BinomialTest twoSided = BinomialTest.withDefaults();
        Assertions.assertEquals(1, twoSided.test(3, 3, 1).getPValue(), 1e-4);
        Assertions.assertEquals(1, twoSided.test(3, 3, 0.9).getPValue(), 1e-4);
        Assertions.assertEquals(1, twoSided.test(3, 3, 0.8).getPValue(), 1e-4);
        Assertions.assertEquals(0.559, twoSided.test(3, 3, 0.7).getPValue(), 1e-4);
        Assertions.assertEquals(0.28, twoSided.test(3, 3, 0.6).getPValue(), 1e-4);
        Assertions.assertEquals(0.25, twoSided.test(3, 3, 0.5).getPValue(), 1e-4);
        Assertions.assertEquals(0.064, twoSided.test(3, 3, 0.4).getPValue(), 1e-4);
        Assertions.assertEquals(0.027, twoSided.test(3, 3, 0.3).getPValue(), 1e-4);
        Assertions.assertEquals(0.008, twoSided.test(3, 3, 0.2).getPValue(), 1e-4);
        Assertions.assertEquals(0.001, twoSided.test(3, 3, 0.1).getPValue(), 1e-4);
        Assertions.assertEquals(0, twoSided.test(3, 3, 0.0).getPValue(), 1e-4);

        Assertions.assertEquals(0, twoSided.test(3, 0, 1).getPValue(), 1e-4);
        Assertions.assertEquals(0.001, twoSided.test(3, 0, 0.9).getPValue(), 1e-4);
        Assertions.assertEquals(0.008, twoSided.test(3, 0, 0.8).getPValue(), 1e-4);
        Assertions.assertEquals(0.027, twoSided.test(3, 0, 0.7).getPValue(), 1e-4);
        Assertions.assertEquals(0.064, twoSided.test(3, 0, 0.6).getPValue(), 1e-4);
        Assertions.assertEquals(0.25, twoSided.test(3, 0, 0.5).getPValue(), 1e-4);
        Assertions.assertEquals(0.28, twoSided.test(3, 0, 0.4).getPValue(), 1e-4);
        Assertions.assertEquals(0.559, twoSided.test(3, 0, 0.3).getPValue(), 1e-4);
        Assertions.assertEquals(1, twoSided.test(3, 0, 0.2).getPValue(), 1e-4);
        Assertions.assertEquals(1, twoSided.test(3, 0, 0.1).getPValue(), 1e-4);
        Assertions.assertEquals(1, twoSided.test(3, 0, 0.0).getPValue(), 1e-4);
    }

    @ParameterizedTest
    @CsvSource({
        // numberOfSuccesses = numberOfTrials * probability (median)
        "10, 5, 0.5",
        "11, 5, 0.5",
        "11, 6, 0.5",
        "20, 5, 0.25",
        "21, 5, 0.25",
        "21, 6, 0.25",
        "20, 15, 0.75",
        "21, 15, 0.75",
        "21, 16, 0.75",
    })
    void testMath1644(int n, int k, double p) {
        final double pval = BinomialTest.withDefaults().test(n, k, p).getPValue();
        Assertions.assertTrue(pval <= 1, () -> "pval=" + pval);
    }

    @ParameterizedTest
    @MethodSource
    void testBinomTest(int n, int k, double probability, double[] p) {
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            final SignificanceResult r = BinomialTest.withDefaults().with(h).test(n, k, probability);
            Assertions.assertEquals((double) k / n, r.getStatistic(), "statistic");
            TestUtils.assertProbability(p[i++], r.getPValue(), 1e-14, "p-value");
        }
    }

    static Stream<Arguments> testBinomTest() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        // scipy.stats.binomtest (version 1.9.3)
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(15, 3, 0.1,
            new double[] {0.18406106910639106, 0.18406106910639106, 0.944444369992464}));
        builder.add(Arguments.of(150, 37, 0.25,
                new double[] {1.0, 0.5687513546881982, 0.5062937783866548}));
        builder.add(Arguments.of(150, 67, 0.25,
                new double[] {2.083753914662947e-07, 1.2964820621216238e-07, 0.9999999481384629}));
        builder.add(Arguments.of(150, 17, 0.25,
                new double[] {4.229481760264341e-05, 0.9999911956737946, 2.399451075709081e-05}));
        return builder.build();
    }
}
