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
package org.apache.commons.statistics.ranking;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ArraySampler;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link NaturalRanking}.
 */
class NaturalRankingTest {

    /** Examples data in the {@link NaturalRanking} class javadoc. */
    private static final double[] EXAMPLE_DATA = {20, 17, 30, 42.3, 17, 50, Double.NaN, Double.NEGATIVE_INFINITY, 17};
    private static final double[] TIES_FIRST = {0, 0, 2, 1, 4};
    private static final double[] TIES_LAST = {4, 4, 1, 0};
    private static final double[] MULTIPLE_NANS = {0, 1, Double.NaN, Double.NaN};
    private static final double[] MULTIPLE_TIES = {3, 2, 5, 5, 6, 6, 1};
    private static final double[] ALL_SAME = {0, 0, 0, 0};

    /**
     * Test the strategies are correctly configured using the various constructors.
     */
    @Test
    void testProperties() {
        final TiesStrategy defaultTs = TiesStrategy.AVERAGE;
        final NaNStrategy defaultNs = NaNStrategy.FAILED;
        final IntUnaryOperator randomSource = x -> x;
        NaturalRanking ranking;

        ranking = new NaturalRanking();
        Assertions.assertEquals(defaultTs, ranking.getTiesStrategy());
        Assertions.assertEquals(defaultNs, ranking.getNanStrategy());
        ranking = new NaturalRanking(randomSource);
        Assertions.assertEquals(TiesStrategy.RANDOM, ranking.getTiesStrategy());
        Assertions.assertEquals(defaultNs, ranking.getNanStrategy());

        final TiesStrategy[] ts = TiesStrategy.values();
        final NaNStrategy[] ns = NaNStrategy.values();
        for (final NaNStrategy n : ns) {
            ranking = new NaturalRanking(n);
            Assertions.assertEquals(defaultTs, ranking.getTiesStrategy());
            Assertions.assertEquals(n, ranking.getNanStrategy());
            ranking = new NaturalRanking(n, randomSource);
            Assertions.assertEquals(TiesStrategy.RANDOM, ranking.getTiesStrategy());
            Assertions.assertEquals(n, ranking.getNanStrategy());
            for (final TiesStrategy t : ts) {
                ranking = new NaturalRanking(n, t);
                Assertions.assertEquals(t, ranking.getTiesStrategy());
                Assertions.assertEquals(n, ranking.getNanStrategy());
            }
        }
        for (final TiesStrategy t : ts) {
            ranking = new NaturalRanking(t);
            Assertions.assertEquals(t, ranking.getTiesStrategy());
            Assertions.assertEquals(defaultNs, ranking.getNanStrategy());
        }
    }

    @Test
    void testNullArguments() {
        final TiesStrategy nullTiesStrategy = null;
        final TiesStrategy tiesStrategy = TiesStrategy.AVERAGE;
        final NaNStrategy nullNaNStrategy = null;
        final NaNStrategy nanStrategy = NaNStrategy.FIXED;
        final IntUnaryOperator nullRandomness = null;
        final IntUnaryOperator randomness = x -> x;
        assertThrowsNPEWithKeywords(() -> new NaturalRanking(nullTiesStrategy), "ties", "strategy");
        assertThrowsNPEWithKeywords(() -> new NaturalRanking(nullNaNStrategy), "nan", "strategy");
        assertThrowsNPEWithKeywords(
            () -> new NaturalRanking(nullNaNStrategy, tiesStrategy), "nan", "strategy");
        assertThrowsNPEWithKeywords(
            () -> new NaturalRanking(nanStrategy, nullTiesStrategy), "ties", "strategy");
        assertThrowsNPEWithKeywords(() -> new NaturalRanking(nullRandomness), "random");
        assertThrowsNPEWithKeywords(
            () -> new NaturalRanking(nullNaNStrategy, randomness), "nan", "strategy");
        assertThrowsNPEWithKeywords(
            () -> new NaturalRanking(nanStrategy, nullRandomness), "random");
    }

    private static void assertThrowsNPEWithKeywords(Executable executable, String... keywords) {
        final NullPointerException e = Assertions.assertThrows(NullPointerException.class, executable);
        final String msg = e.getMessage().toLowerCase(Locale.ROOT);
        for (final String s : keywords) {
            Assertions.assertTrue(msg.contains(s), () -> "Missing keyword: " + s);
        }
    }

    /**
     * Test the ranks on the standard test cases.
     *
     * <p>If the expected result is null then the algorithm is expected to throw an
     * {@link IllegalArgumentException}.
     *
     * @param ranking Ranking algorithm
     * @param example Ranks for Ranks for the example data
     * @param tiesFirst Ranks for the ties first data
     * @param tiesLast Ranks for the ties last data
     * @param multipleNaNs Ranks for the multiple NaNs data
     * @param multipleTies Ranks for the multiple ties data
     * @param allSame Ranks for the all same data
     */
    @ParameterizedTest
    @MethodSource
    void testRanks(RankingAlgorithm ranking, double[] example, double[] tiesFirst, double[] tiesLast,
            double[] multipleNaNs, double[] multipleTies, double[] allSame) {
        assertRanks(ranking, EXAMPLE_DATA, example, "Example data");
        assertRanks(ranking, TIES_FIRST, tiesFirst, "Ties first");
        assertRanks(ranking, TIES_LAST, tiesLast, "Ties last");
        assertRanks(ranking, MULTIPLE_NANS, multipleNaNs, "Multiple NaNs");
        assertRanks(ranking, MULTIPLE_TIES, multipleTies, "Multiple ties");
        assertRanks(ranking, ALL_SAME, allSame, "All same");
    }

    /**
     * Provide expected results for the standard test cases using different algorithms.
     *
     * @return the arguments
     */
    static Stream<Arguments> testRanks() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(
            // Default: Ties averaged, NaNs failed
            new NaturalRanking(),
            null,
            new double[] {1.5, 1.5, 4, 3, 5},
            new double[] {3.5, 3.5, 2, 1},
            null,
            new double[] {3, 2, 4.5, 4.5, 6.5, 6.5, 1},
            new double[] {2.5, 2.5, 2.5, 2.5}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.FAILED),
            null,
            new double[] {1.5, 1.5, 4, 3, 5},
            new double[] {3.5, 3.5, 2, 1},
            null,
            new double[] {3, 2, 4.5, 4.5, 6.5, 6.5, 1},
            new double[] {2.5, 2.5, 2.5, 2.5}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.FAILED, TiesStrategy.AVERAGE),
            null,
            new double[] {1.5, 1.5, 4, 3, 5},
            new double[] {3.5, 3.5, 2, 1},
            null,
            new double[] {3, 2, 4.5, 4.5, 6.5, 6.5, 1},
            new double[] {2.5, 2.5, 2.5, 2.5}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(TiesStrategy.AVERAGE),
            null,
            new double[] {1.5, 1.5, 4, 3, 5},
            new double[] {3.5, 3.5, 2, 1},
            null,
            new double[] {3, 2, 4.5, 4.5, 6.5, 6.5, 1},
            new double[] {2.5, 2.5, 2.5, 2.5}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.MAXIMAL, TiesStrategy.MINIMUM),
            new double[] {5, 2, 6, 7, 2, 8, 9, 1, 2},
            new double[] {1, 1, 4, 3, 5},
            new double[] {3, 3, 2, 1},
            new double[] {1, 2, 3, 3},
            new double[] {3, 2, 4, 4, 6, 6, 1},
            new double[] {1, 1, 1, 1}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL),
            new double[] {5, 2, 6, 7, 3, 8, 1, 4},
            new double[] {1, 2, 4, 3, 5},
            new double[] {3, 4, 2, 1},
            new double[] {1, 2},
            new double[] {3, 2, 4, 5, 6, 7, 1},
            new double[] {1, 2, 3, 4}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.MINIMAL, TiesStrategy.MAXIMUM),
            new double[] {6, 5, 7, 8, 5, 9, 2, 2, 5},
            new double[] {2, 2, 4, 3, 5},
            new double[] {4, 4, 2, 1},
            new double[] {3, 4, 2, 2},
            new double[] {3, 2, 5, 5, 7, 7, 1},
            new double[] {4, 4, 4, 4}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.MINIMAL),
            new double[] {6, 4, 7, 8, 4, 9, 1.5, 1.5, 4},
            new double[] {1.5, 1.5, 4, 3, 5},
            new double[] {3.5, 3.5, 2, 1},
            new double[] {3, 4, 1.5, 1.5},
            new double[] {3, 2, 4.5, 4.5, 6.5, 6.5, 1},
            new double[] {2.5, 2.5, 2.5, 2.5}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.FAILED),
            null,
            new double[] {1.5, 1.5, 4, 3, 5},
            new double[] {3.5, 3.5, 2, 1},
            null,
            new double[] {3, 2, 4.5, 4.5, 6.5, 6.5, 1},
            new double[] {2.5, 2.5, 2.5, 2.5}
        ));
        return builder.build();
    }

    /**
     * Test the ranks on the standard test cases. This method requires the output ranking
     * to have unique indices using a natural sequence from one. The order within groups
     * is arbitrary. All elements of each successive group must be ranked after the previous
     * group.
     *
     * <p>If the expected result is null then the algorithm is expected to throw an
     * {@link IllegalArgumentException}.
     *
     * @param ranking Ranking algorithm
     * @param example Rank groups for Rank groups for the example data
     * @param tiesFirst Rank groups for the ties first data
     * @param tiesLast Rank groups for the ties last data
     * @param multipleNaNs Rank groups for the multiple NaNs data
     * @param multipleTies Rank groups for the multiple ties data
     * @param allSame Rank groups for the all same data
     */
    @ParameterizedTest
    @MethodSource
    void testRanksTiesRandom(RankingAlgorithm ranking, int[] example, int[] tiesFirst, int[] tiesLast,
            int[] multipleNaNs, int[] multipleTies, int[] allSame) {
        assertRanks(ranking, EXAMPLE_DATA, example, "Example data");
        assertRanks(ranking, TIES_FIRST, tiesFirst, "Ties first");
        assertRanks(ranking, TIES_LAST, tiesLast, "Ties last");
        assertRanks(ranking, MULTIPLE_NANS, multipleNaNs, "Multiple NaNs");
        assertRanks(ranking, MULTIPLE_TIES, multipleTies, "Multiple ties");
        assertRanks(ranking, ALL_SAME, allSame, "All same");
    }

    /**
     * Provide expected results for the standard test cases using different algorithms.
     *
     * @return the arguments
     */
    static Stream<Arguments> testRanksTiesRandom() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(
            new NaturalRanking(rng::nextInt),
            null,
            new int[] {1, 1, 3, 2, 4},
            new int[] {3, 3, 2, 1},
            null,
            new int[] {3, 2, 4, 4, 5, 5, 1},
            new int[] {1, 1, 1, 1}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.FIXED, rng::nextInt),
            new int[] {3, 2, 4, 5, 2, 6, 0, 1, 2},
            new int[] {1, 1, 3, 2, 4},
            new int[] {3, 3, 2, 1},
            new int[] {1, 2, 0, 0},
            new int[] {3, 2, 4, 4, 5, 5, 1},
            new int[] {1, 1, 1, 1}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.REMOVED, rng::nextInt),
            new int[] {3, 2, 4, 5, 2, 6, -1, 1, 2},
            new int[] {1, 1, 3, 2, 4},
            new int[] {3, 3, 2, 1},
            new int[] {1, 2, -1, -1},
            new int[] {3, 2, 4, 4, 5, 5, 1},
            new int[] {1, 1, 1, 1}
        ));
        // The test method works even when not using TiesStrategy.RANDOM but the
        // ties strategy must output unique indices so use SEQUENTIAL.
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.MAXIMAL, TiesStrategy.SEQUENTIAL),
            new int[] {5, 2, 6, 7, 3, 8, 9, 1, 4},
            new int[] {1, 2, 4, 3, 5},
            new int[] {3, 4, 2, 1},
            new int[] {1, 2, 3, 4},
            new int[] {3, 2, 4, 5, 6, 7, 1},
            new int[] {1, 2, 3, 4}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL),
            new int[] {5, 2, 6, 7, 3, 8, -1, 1, 4},
            new int[] {1, 2, 4, 3, 5},
            new int[] {3, 4, 2, 1},
            new int[] {1, 2, -1, -1},
            new int[] {3, 2, 4, 5, 6, 7, 1},
            new int[] {1, 2, 3, 4}
        ));
        builder.add(Arguments.of(
            new NaturalRanking(NaNStrategy.MINIMAL, TiesStrategy.SEQUENTIAL),
            new int[] {5, 2, 6, 7, 3, 8, 1, 1, 4},
            new int[] {1, 2, 4, 3, 5},
            new int[] {3, 4, 2, 1},
            new int[] {2, 3, 1, 1},
            new int[] {3, 2, 4, 5, 6, 7, 1},
            new int[] {1, 2, 3, 4}
        ));
        return builder.build();
    }

    /**
     * Test ranking of data with no ties. This should work for any ties strategy.
     */
    @ParameterizedTest
    @EnumSource(value = TiesStrategy.class)
    void testNoTies(TiesStrategy tiesStrategy) {
        // Ordered values required for the test. These are randomized below.
        final double[] values = {-13, -6, 1, 13.5, 66.9};
        final int[] indices = PermutationSampler.natural(values.length);
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final double[] data = new double[values.length];
        final double[] expected = new double[values.length];
        final NaturalRanking ranking = new NaturalRanking(tiesStrategy);
        for (int i = 0; i < 3; i++) {
            ArraySampler.shuffle(rng, indices);
            for (int j = 0; j < values.length; j++) {
                data[j] = values[indices[j]];
                expected[j] = indices[j] + 1;
            }
            assertRanks(ranking, data, expected, tiesStrategy.toString());
        }
    }

    /**
     * Test ranking of data that contains NaN and positive and negative infinities.
     */
    @ParameterizedTest
    @MethodSource
    void testNaNsAndInfinities(NaNStrategy nanStrategy, double[] expected) {
        final double[] data = {0, Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY};
        assertRanks(new NaturalRanking(nanStrategy), data, expected, nanStrategy.toString());
    }

    static Stream<Arguments> testNaNsAndInfinities() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(NaNStrategy.MAXIMAL, new double[] {2, 3.5, 3.5, 1}));
        builder.add(Arguments.of(NaNStrategy.MINIMAL, new double[] {3, 4, 1.5, 1.5}));
        builder.add(Arguments.of(NaNStrategy.REMOVED, new double[] {2, 3, 1}));
        builder.add(Arguments.of(NaNStrategy.FIXED, new double[] {2, 3, Double.NaN, 1}));
        builder.add(Arguments.of(NaNStrategy.FAILED, null));
        return builder.build();
    }

    /**
     * Test ranking of no data. This is enumerated for all NaN strategies to ensure
     * all methods searching for NaN handle no data.
     */
    @ParameterizedTest
    @EnumSource(value = NaNStrategy.class)
    void testEmpty(NaNStrategy nanStrategy) {
        final double[] data = {};
        assertRanks(new NaturalRanking(nanStrategy), data, data, nanStrategy.toString());
    }

    /**
     * Test ranking of only NaN data.
     */
    @ParameterizedTest
    @MethodSource
    void testNaN(NaNStrategy nanStrategy, double[] expected) {
        final double[] data = {Double.NaN, Double.NaN};
        assertRanks(new NaturalRanking(nanStrategy), data, expected, nanStrategy.toString());
    }

    static Stream<Arguments> testNaN() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(NaNStrategy.MAXIMAL, new double[] {1.5, 1.5}));
        builder.add(Arguments.of(NaNStrategy.MINIMAL, new double[] {1.5, 1.5}));
        builder.add(Arguments.of(NaNStrategy.REMOVED, new double[0]));
        builder.add(Arguments.of(NaNStrategy.FIXED, new double[] {Double.NaN, Double.NaN}));
        builder.add(Arguments.of(NaNStrategy.FAILED, null));
        return builder.build();
    }

    /**
     * Test the random allocation of ties is uniform for each tied-position.
     *
     * @param before Number of values before the length of ties
     * @param ties Number of tied values
     * @param after Number of values after the length of ties
     * @param seed Random seed (ensures test does not fail the build due to randomness)
     */
    @ParameterizedTest
    @CsvSource({
        "0, 10, 0, 23657426436",
        "1, 8, 0, 21637427438",
        "0, 6, 3, -9879847797",
        "1, 12, 1, -253672793297",
    })
    void testRandom(int before, int ties, int after, long seed) {
        Assertions.assertTrue(ties > 0);
        final int n = 1000;
        final DoubleStream.Builder builder = DoubleStream.builder();
        final double[] value = {0};
        IntStream.range(0, before).forEach(i -> builder.add(value[0]++));
        IntStream.range(0, ties).forEach(i -> builder.add(value[0]));
        IntStream.range(0, after).forEach(i -> builder.add(++value[0]));
        final double[] data = builder.build().toArray();
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(seed);
        final NaturalRanking ranking = new NaturalRanking(rng::nextInt);
        final int k = before + 1;
        final int m = before + ties;
        // Frequency of ranks for each tied position in the data
        final long[][] counts = new long[ties][ties];
        for (int i = 0; i < n; i++) {
            final double[] ranks = ranking.apply(data);
            int j = 0;
            for (; j < before; j++) {
                Assertions.assertEquals(j + 1, ranks[j]);
            }
            for (; j < m; j++) {
                counts[j - before][(int)ranks[j] - k]++;
            }
            for (; j < ranks.length; j++) {
                Assertions.assertEquals(j + 1, ranks[j]);
            }
        }
        final double p = new ChiSquareTest().chiSquareTest(counts);
        Assertions.assertFalse(p < 1e-3, () -> "p-value too small: " + p);
    }

    /**
     * Test random allocation of ties works without a supplied source of randomness.
     */
    @Test
    void testDefaultRandom() {
        // This is big enough the test should never fail to create 2 different results
        final double[] data = new double[1000];
        Arrays.fill(data, 1.23);
        final NaturalRanking ranking = new NaturalRanking(TiesStrategy.RANDOM);
        final double[] ranks1 = ranking.apply(data);
        final double[] ranks2 = ranking.apply(data);
        Assertions.assertFalse(Arrays.equals(ranks1, ranks2));
        final double[] expected = IntStream.rangeClosed(1, data.length).asDoubleStream().toArray();
        Arrays.sort(ranks1);
        Arrays.sort(ranks2);
        Assertions.assertArrayEquals(expected, ranks1);
        Assertions.assertArrayEquals(expected, ranks2);
    }

    /**
     * Assert the data ranks created by the algorithm are equal to the expected
     * ranks. The input data is tested to ensure it is unchanged (i.e. the ranking
     * does not destructively modify the input data). The expected ranks are passed
     * through the algorithm and the result must be unchanged thus ensuring the
     * algorithm is stable.
     *
     * @param ranking Ranking algorithm
     * @param data Input data
     * @param expected Expected ranks (if null the algorithm is expected to raise an {@link IllegalArgumentException})
     * @param msg Prefix for any assertion failure message
     */
    private static void assertRanks(RankingAlgorithm ranking, double[] data, double[] expected, String msg) {
        if (expected == null) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> ranking.apply(data));
        } else {
            final double[] original = data.clone();
            final double[] ranks = ranking.apply(data);
            Assertions.assertArrayEquals(original, data, () -> msg + ": Data was destructively modified");
            Assertions.assertArrayEquals(expected, ranks, () -> msg + ": Ranking failed");
            final double[] ranks2 = ranking.apply(ranks);
            Assertions.assertArrayEquals(ranks, ranks2, () -> msg + ": Repeat ranking changed the result");
        }
    }

    /**
     * Assert the data ranks created by the algorithm are assigned to the expected
     * groups. The input data is tested to ensure it is unchanged (i.e. the ranking
     * does not destructively modify the input data). The expected ranks are passed
     * through the algorithm and the result must be unchanged thus ensuring the
     * algorithm is stable.
     *
     * <p>Groups must use a sequential ordering from 1.
     * Any negative expected group marks data to be removed (e.g. NaNs).
     * Any group of zero marks data to be left unchanged (e.g. NaNs).
     * Note that the current test assumes the removed and unchanged options are mutually exclusive.
     * Groups are mapped to an expected rank using a sequential ordering from 1.
     * The order within a group can be random.
     *
     * For example:
     * <pre>
     * groups: [1, 2, 2, 2, 3]
     *
     * [1, 2, 3, 4, 5] : pass
     * [1, 4, 3, 2, 5] : pass
     * [1, 2, 4, 3, 5] : pass
     * [1, 3, 3, 3, 5] : fail
     * [1, 2, 2, 4, 5] : fail
     * [1, 2, 3, 4, 4] : fail
     * [1, 2, 3, 4, 6] : fail
     * </pre>
     *
     * @param ranking Ranking algorithm
     * @param data Input data
     * @param expectedGroups Expected groups (if null the algorithm is expected to raise an {@link IllegalArgumentException})
     * @param msg Prefix for any assertion failure message
     */
    private static void assertRanks(RankingAlgorithm ranking, double[] data, int[] expectedGroups, String msg) {
        if (expectedGroups == null) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> ranking.apply(data));
        } else {
            Assertions.assertEquals(data.length, expectedGroups.length, "Groups must be assigned to all data");

            final double[] original = data.clone();
            final double[] ranks = ranking.apply(data);
            Assertions.assertArrayEquals(original, data, () -> msg + ": Data was destructively modified");
            final double[] ranks2 = ranking.apply(ranks);
            Assertions.assertArrayEquals(ranks, ranks2, () -> msg + ": Repeat ranking changed the result");

            // TODO: Simplify the collation of groups into sets.

            // Check groups
            int max = 0;
            int numberOfElements = 0;
            int unchanged = 0;
            int removed = 0;
            for (int i = 0; i < expectedGroups.length; i++) {
                if (expectedGroups[i] > 0) {
                    max = Math.max(max, expectedGroups[i]);
                    // Reduce to only the expected elements.
                    // This filters unchanged/removed elements.
                    expectedGroups[numberOfElements] = expectedGroups[i];
                    numberOfElements++;
                } else if (expectedGroups[i] == 0) {
                    Assertions.assertEquals(data[i], ranks[i], "Element was changed");
                    // Flag for removal
                    ranks[i] = -1;
                    unchanged++;
                } else {
                    removed++;
                }
            }
            Assertions.assertTrue(removed == 0 || unchanged == 0, "removed and unchanged options are mutually exclusive");
            Assertions.assertEquals(ranks.length, data.length - removed, "Did not remove expected number of elements");
            Assertions.assertTrue(max <= numberOfElements, "Maximum group above the number of valid elements");

            if (unchanged != 0) {
                // Unchanged elements are not removed by the ranking algorithm.
                // These have already been asserted as unchanged so we remove them
                // so only the grouped elements remain.
                int j = 0;
                for (int i = 0; i < ranks.length; i++) {
                    if (ranks[i] < 0) {
                        continue;
                    }
                    ranks[j++] = ranks[i];
                }
                Assertions.assertEquals(numberOfElements, j, "Error removing unchanged elements");
            }

            // Count groups sizes
            final int[] sizes = new int[max + 1];
            for (int i = 0; i < numberOfElements; i++) {
                sizes[expectedGroups[i]]++;
            }
            // Each must be non-zero
            for (int i = 1; i <= max; i++) {
                final int index = i;
                Assertions.assertNotEquals(0, sizes[i], () -> "Empty group: " + index);
            }

            // Here we have a number of groups with non-zero sizes.
            // The expected ranks must be sequential from 1.
            // Create a BitSet for each group filled with bits enabled for each rank
            // within the group. This is typically a single value or a range: [1], [2, 3], [4], etc.
            // Store the set in an array using the rank as the index to allow look-up of
            // the set from the rank.
            final int[] rankIndex = {1};
            final BitSet[] rankToGroup = new BitSet[data.length + 1];
            final List<BitSet> groups =
                IntStream.rangeClosed(1, max)
                         .mapToObj(i -> {
                             final BitSet bs = new BitSet();
                             for (int j = 0; j < sizes[i]; j++) {
                                 bs.set(rankIndex[0]);
                                 rankToGroup[rankIndex[0]] = bs;
                                 rankIndex[0]++;
                             }
                             return bs;
                         }).collect(Collectors.toList());

            // For the actual rank, map back to the group and check it is allowed.
            for (int i = 0; i < numberOfElements; i++) {
                final double r = ranks[i];
                Assertions.assertEquals(r, (int) r, () -> "Non-integer rank: " + r);
                final int rank = (int) r;
                final BitSet groupSet = rankToGroup[rank];
                final int group = expectedGroups[i];
                Assertions.assertTrue(groupSet.get(rank),
                    () -> String.format("Unexpected rank %d in group %d", rank, group));
                groupSet.clear(rank);
            }

            // Check all groups should now be empty
            groups.forEach(g -> Assertions.assertEquals(0, g.cardinality(), "Non-empty group"));
        }
    }
}
