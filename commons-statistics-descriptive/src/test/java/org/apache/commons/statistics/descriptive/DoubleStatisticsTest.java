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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link DoubleStatistics}.
 *
 * <p>This class verifies that the statistics computed using the summary
 * class are an exact match to the statistics computed individually.
 *
 * <p>For simplicity some tests use only the {@code double} result of the statistic.
 * The full {@link StatisticResult} interface is asserted in the test of the array or
 * stream of input data.
 */
class DoubleStatisticsTest {
    /** Empty statistic array. */
    private static final Statistic[] EMPTY_STATISTIC_ARRAY = {};
    /** The number of random permutations to perform. */
    private static final int RANDOM_PERMUTATIONS = 5;

    /** The test data. */
    private static List<TestData> testData;

    /** The expected result for each statistic on the test data. */
    private static EnumMap<Statistic, List<ExpectedResult>> expectedResult;

    /** The statistics that are co-computed. */
    private static EnumMap<Statistic, EnumSet<Statistic>> coComputed;

    /**
     * Container for test data.
     */
    private static class TestData {
        /** Identifier. */
        private final int id;
        /** The sample values. */
        private final double[][] values;
        /** The number of values. */
        private final long size;

        /**
         * @param id Identifier.
         * @param values Sample values.
         */
        TestData(int id, double[]... values) {
            this.id = id;
            this.values = values;
            size = Arrays.stream(values).mapToLong(x -> x.length).sum();
        }

        /**
         * @return the identifier
         */
        int getId() {
            return id;
        }

        /**
         * @return the values as a stream
         */
        Stream<double[]> stream() {
            return Arrays.stream(values);
        }

        /**
         * @return the number of values
         */
        long size() {
            return size;
        }

        /**
         * @return the values as a single array
         */
        double[] toArray() {
            return TestHelper.concatenate(values);
        }
    }

    /**
     * Container for expected results.
     */
    private static class ExpectedResult {
        /** The expected result for a stream of values. */
        private final double stream;
        /** The expected result for an array of values. */
        private final double array;

        /**
         * @param stream Stream result.
         * @param array Array result.
         */
        ExpectedResult(double stream, double array) {
            this.stream = stream;
            this.array = array;
        }

        /**
         * @return the expected result for a stream of values.
         */
        double getStream() {
            return stream;
        }

        /**
         * @return the expected result for an array of values.
         */
        double getArray() {
            return array;
        }
    }

    @BeforeAll
    static void setup() {
        // Random double[] of different lengths
        final double[][] arrays = IntStream.of(4, 5, 7)
            .mapToObj(i -> ThreadLocalRandom.current().doubles(i, 2.25, 3.75).toArray())
            .toArray(double[][]::new);
        // Create test data. IDs must be created in order of the data.
        testData = new ArrayList<>();
        testData.add(new TestData(testData.size(), new double[0]));
        testData.add(new TestData(testData.size(), arrays[0]));
        testData.add(new TestData(testData.size(), arrays[1]));
        testData.add(new TestData(testData.size(), arrays[2]));
        testData.add(new TestData(testData.size(), arrays[0], arrays[1]));
        testData.add(new TestData(testData.size(), arrays[1], arrays[2]));
        // Create reference expected results
        expectedResult = new EnumMap<>(Statistic.class);
        addExpected(Statistic.MIN, Min::create, Min::of);
        addExpected(Statistic.MAX, Max::create, Max::of);
        addExpected(Statistic.MEAN, Mean::create, Mean::of);
        addExpected(Statistic.STANDARD_DEVIATION, StandardDeviation::create, StandardDeviation::of);
        addExpected(Statistic.VARIANCE, Variance::create, Variance::of);
        addExpected(Statistic.SKEWNESS, Skewness::create, Skewness::of);
        addExpected(Statistic.KURTOSIS, Kurtosis::create, Kurtosis::of);
        addExpected(Statistic.PRODUCT, Product::create, Product::of);
        addExpected(Statistic.SUM, Sum::create, Sum::of);
        addExpected(Statistic.SUM_OF_LOGS, SumOfLogs::create, SumOfLogs::of);
        addExpected(Statistic.SUM_OF_SQUARES, SumOfSquares::create, SumOfSquares::of);
        addExpected(Statistic.GEOMETRIC_MEAN, GeometricMean::create, GeometricMean::of);
        // Create co-computed statistics
        coComputed = new EnumMap<>(Statistic.class);
        Arrays.stream(Statistic.values()).forEach(s -> coComputed.put(s, EnumSet.of(s)));
        addCoComputed(Statistic.GEOMETRIC_MEAN, Statistic.SUM_OF_LOGS);
        addCoComputed(Statistic.VARIANCE, Statistic.STANDARD_DEVIATION);
        // Cascade moments up
        EnumSet<Statistic> m = coComputed.get(Statistic.MEAN);
        coComputed.get(Statistic.STANDARD_DEVIATION).addAll(m);
        coComputed.get(Statistic.VARIANCE).addAll(m);
        m = coComputed.get(Statistic.VARIANCE);
        coComputed.get(Statistic.SKEWNESS).addAll(m);
        m = coComputed.get(Statistic.SKEWNESS);
        coComputed.get(Statistic.KURTOSIS).addAll(m);
    }

    /**
     * Adds the expected result for the specified {@code statistic}.
     *
     * @param <T> {@link DoubleStatistic} being computed.
     * @param statistic Statistic.
     * @param constructor Constructor for an empty object.
     * @param arrayConstructor Constructor using an array of values.
     */
    private static <T extends DoubleStatistic & StatisticAccumulator<T>> void addExpected(Statistic statistic,
            Supplier<T> constructor, Function<double[], T> arrayConstructor) {
        final List<ExpectedResult> results = new ArrayList<>();
        for (final TestData d : testData) {
            // Stream values
            final double e1 = d.stream()
                .map(values -> Statistics.add(constructor.get(), values))
                .reduce(StatisticAccumulator<T>::combine)
                .orElseThrow(IllegalStateException::new)
                .getAsDouble();
            // Create from array
            final double e2 = d.stream()
                .map(arrayConstructor)
                .reduce(StatisticAccumulator<T>::combine)
                .orElseThrow(IllegalStateException::new)
                .getAsDouble();
            // Check that there is a finite value to compute during testing
            if (d.size() != 0) {
                assertFinite(e1, statistic);
                assertFinite(e2, statistic);
            }
            results.add(new ExpectedResult(e1, e2));
        }
        expectedResult.put(statistic, results);
    }

    /**
     * Adds the co-computed statistics to the co-computed mapping.
     * The statistics must be co-computed (computing either one will compute the other)
     * and not a one-way relationship (a computes b but b does not compute a).
     *
     * @param s1 First statistic.
     * @param s2 Second statistic.
     */
    private static void addCoComputed(Statistic s1, Statistic s2) {
        coComputed.get(s1).add(s2);
        coComputed.get(s2).add(s1);
    }

    @AfterAll
    static void teardown() {
        // Free memory
        testData = null;
        expectedResult = null;
        coComputed = null;
    }

    static Stream<Arguments> streamTestData() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final Statistic[] statistics = Statistic.values();
        for (int i = 0; i < statistics.length; i++) {
            // Single statistics
            final EnumSet<Statistic> s1 = EnumSet.of(statistics[i]);
            testData.stream().forEach(d -> builder.add(Arguments.of(s1, d)));
            // Paired statistics
            for (int j = i + 1; j < statistics.length; j++) {
                final EnumSet<Statistic> s2 = EnumSet.of(statistics[i], statistics[j]);
                testData.stream().forEach(d -> builder.add(Arguments.of(s2, d)));
            }
        }
        return builder.build();
    }

    /**
     * Test the {@link DoubleStatistics} when all data is passed as a stream of single values.
     */
    @ParameterizedTest
    @MethodSource(value = {"streamTestData"})
    void testStream(EnumSet<Statistic> stats, TestData data) {
        // Test creation from specified statistics
        final Statistic[] statistics = stats.toArray(EMPTY_STATISTIC_ARRAY);
        assertStatistics(stats, data, x -> acceptAll(statistics, x), ExpectedResult::getStream);
    }

    /**
     * Test the {@link DoubleStatistics} when data is passed as a {@code double[]} of values.
     */
    @ParameterizedTest
    @MethodSource(value = {"streamTestData"})
    void testArray(EnumSet<Statistic> stats, TestData data) {
        // Test creation from a builder
        final DoubleStatistics.Builder builder = DoubleStatistics.builder(stats.toArray(EMPTY_STATISTIC_ARRAY));
        assertStatistics(stats, data, builder::build, ExpectedResult::getArray);
    }

    /**
     * Test the {@link DoubleStatistics} when data is passed as a range of {@code double[]} of values.
     */
    @ParameterizedTest
    @MethodSource(value = {"streamTestData"})
    final void testArrayRange(EnumSet<Statistic> stats, TestData data) {
        // Test full range
        final double[] values = data.toArray();
        assertStatistics(stats, values, 0, values.length);
        if (values.length == 0) {
            // Nothing more to do
            return;
        }
        // Test half-range
        assertStatistics(stats, values, 0, values.length >> 1);
        assertStatistics(stats, values, values.length >> 1, values.length);
        // Random range
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        for (int repeat = RANDOM_PERMUTATIONS; --repeat >= 0;) {
            final int i = rng.nextInt(values.length);
            final int j = rng.nextInt(values.length);
            assertStatistics(stats, values, Math.min(i, j), Math.max(i, j));
        }
    }

    /**
     * Assert the computed statistics match the expected result.
     *
     * @param stats Statistics that are computed.
     * @param data Test data.
     * @param constructor Constructor to create the {@link IntStatistics}.
     * @param expected Function to obtain the expected result.
     */
    private static void assertStatistics(EnumSet<Statistic> stats, TestData data,
            Function<double[], DoubleStatistics> constructor,
            ToDoubleFunction<ExpectedResult> expected) {
        final Statistic[] statsArray = stats.toArray(EMPTY_STATISTIC_ARRAY);
        final DoubleStatistics statistics = data.stream()
            .map(constructor)
            .reduce((a, b) -> combine(statsArray, a, b))
            .orElseThrow(IllegalStateException::new);
        final int id = data.getId();
        Assertions.assertEquals(data.size(), statistics.getCount(), "Count");
        final EnumSet<Statistic> computed = buildComputedStatistics(stats);

        // Test if the statistics are correctly identified as supported
        EnumSet.allOf(Statistic.class).forEach(s -> {
            final boolean isSupported = computed.contains(s);
            Assertions.assertEquals(isSupported, statistics.isSupported(s),
                () -> stats + " isSupported -> " + s.toString());
            if (isSupported) {
                final double doubleResult = expected.applyAsDouble(expectedResult.get(s).get(id));
                // Test individual values
                Assertions.assertEquals(doubleResult, statistics.getAsDouble(s),
                    () -> stats + " getAsDouble -> " + s.toString());
                // Test the values from the result
                final StatisticResult result = () -> doubleResult;
                TestHelper.assertEquals(result, statistics.getResult(s),
                    DoubleTolerances.equals(),
                    () -> stats + " getResult -> " + s.toString());
            } else {
                Assertions.assertThrows(IllegalArgumentException.class, () -> statistics.getAsDouble(s),
                    () -> stats + " getAsDouble -> " + s.toString());
                Assertions.assertThrows(IllegalArgumentException.class, () -> statistics.getResult(s),
                    () -> stats + " getResult -> " + s.toString());
            }
        });
    }

    /**
     * Assert the computation of the statistics from the specified range of values
     * matches the computation using a copy of the range.
     *
     * @param stats Statistics that are computed.
     * @param data Test data.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     */
    private static void assertStatistics(EnumSet<Statistic> stats, double[] data, int from, int to) {
        final DoubleStatistics expected = DoubleStatistics.of(stats, Arrays.copyOfRange(data, from, to));
        final DoubleStatistics statistics = DoubleStatistics.ofRange(stats, data, from, to);
        Assertions.assertEquals(expected.getCount(), statistics.getCount(), "Count");
        final EnumSet<Statistic> computed = buildComputedStatistics(stats);

        // Test if the statistics are correctly identified as supported
        EnumSet.allOf(Statistic.class).forEach(s -> {
            final boolean isSupported = computed.contains(s);
            Assertions.assertEquals(isSupported, statistics.isSupported(s),
                () -> stats + " isSupported -> " + s.toString());
            if (isSupported) {
                // Test individual values
                Assertions.assertEquals(expected.getAsDouble(s), statistics.getAsDouble(s),
                    () -> stats + " getAsDouble -> " + s.toString());
            }
        });
    }

    /**
     * Builds the complete set of supported statistics from the specified statistics to compute.
     * This method expands the input statistics with co-computed statistics.
     *
     * @param stats Statistics.
     * @return the statistics
     */
    private static EnumSet<Statistic> buildComputedStatistics(EnumSet<Statistic> stats) {
        final EnumSet<Statistic> computed = EnumSet.copyOf(stats);
        stats.forEach(s -> computed.addAll(coComputed.get(s)));
        return computed;
    }

    /**
     * Add all the {@code values} to an aggregator of the {@code statistics}.
     *
     * <p>This method verifies that the {@link DoubleStatistics#getAsDouble(Statistic)} and
     * {@link DoubleStatistics#getResult(Statistic)} methods return the same
     * result as values are added.
     *
     * @param statistics Statistics.
     * @param values Values.
     * @return the statistics
     */
    private static DoubleStatistics acceptAll(Statistic[] statistics, double[] values) {
        final DoubleStatistics stats = DoubleStatistics.of(statistics);
        final StatisticResult[] f = getResults(statistics, stats);
        for (final double x : values) {
            stats.accept(x);
            for (int i = 0; i < statistics.length; i++) {
                final Statistic s = statistics[i];
                Assertions.assertEquals(stats.getAsDouble(s), f[i].getAsDouble(),
                    () -> "Supplier(" + s + ") after value " + x);
            }
        }
        return stats;
    }

    /**
     * Gets the suppliers for the {@code statistics}.
     *
     * @param statistics Statistics to compute.
     * @param stats Statistic aggregator.
     * @return the suppliers
     */
    private static StatisticResult[] getResults(Statistic[] statistics, final DoubleStatistics stats) {
        final StatisticResult[] f = new StatisticResult[statistics.length];
        for (int i = 0; i < statistics.length; i++) {
            final StatisticResult supplier = stats.getResult(statistics[i]);
            Assertions.assertFalse(supplier instanceof DoubleStatistic,
                () -> "DoubleStatistic instance: " + supplier.getClass().getSimpleName());
            f[i] = supplier;
        }
        return f;
    }

    /**
     * Combine the two statistic aggregators.
     *
     * <p>This method verifies that the {@link DoubleStatistics#getAsDouble(Statistic)} and
     * {@link DoubleStatistics#getResult(Statistic)} methods return the same
     * result after the {@link DoubleStatistics#combine(DoubleStatistics)}.
     *
     * @param statistics Statistics to compute.
     * @param s1 Statistic aggregator.
     * @param s2 Statistic aggregator.
     * @return the double statistics
     */
    private static DoubleStatistics combine(Statistic[] statistics,
        DoubleStatistics s1, DoubleStatistics s2) {
        final StatisticResult[] f = getResults(statistics, s1);
        s1.combine(s2);
        for (int i = 0; i < statistics.length; i++) {
            final Statistic s = statistics[i];
            Assertions.assertEquals(s1.getAsDouble(s), f[i].getAsDouble(),
                () -> "Supplier(" + s + ") after combine");
        }
        return s1;
    }

    @Test
    void testOfThrows() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.of());
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.of(EMPTY_STATISTIC_ARRAY));
        Assertions.assertThrows(NullPointerException.class, () -> DoubleStatistics.of(new Statistic[1]));
    }

    @Test
    void testOfSetThrows() {
        final EnumSet<Statistic> s1 = EnumSet.noneOf(Statistic.class);
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.of(s1));
        final EnumSet<Statistic> s2 = null;
        Assertions.assertThrows(NullPointerException.class, () -> DoubleStatistics.of(s2));
        final EnumSet<Statistic> s3 = EnumSet.of(Statistic.MIN);
        Assertions.assertThrows(NullPointerException.class, () -> DoubleStatistics.of(s3, null));
    }

    @Test
    void testOfSetRangeThrows() {
        final double[] data = {};
        final EnumSet<Statistic> s1 = EnumSet.noneOf(Statistic.class);
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.ofRange(s1, data, 0, data.length));
        final EnumSet<Statistic> s2 = null;
        Assertions.assertThrows(NullPointerException.class, () -> DoubleStatistics.ofRange(s2, data, 0, data.length));
        final EnumSet<Statistic> s3 = EnumSet.of(Statistic.MIN);
        Assertions.assertThrows(NullPointerException.class, () -> DoubleStatistics.ofRange(s3, null, 0, 0));
    }

    /**
     * Test the {@link DoubleStatistics#ofRange(Set, double[], int, int)} method throws with an invalid range.
     */
    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#arrayRangeTestData"})
    final void testArrayRangeThrows(int from, int to, int length) {
        final EnumSet<Statistic> statistics = EnumSet.of(Statistic.MIN);
        final double[] values = new double[length];
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> DoubleStatistics.ofRange(statistics, values, from, to),
            () -> String.format("range [%d, %d) in length %d", from, to, length));
    }

    @Test
    void testBuilderThrows() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.builder());
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.builder(EMPTY_STATISTIC_ARRAY));
        Assertions.assertThrows(NullPointerException.class, () -> DoubleStatistics.builder(new Statistic[1]));
    }

    @Test
    void testIsSupportedWithNull() {
        final DoubleStatistics s = DoubleStatistics.of(Statistic.MIN);
        Assertions.assertThrows(NullPointerException.class, () -> s.isSupported(null));
    }

    @ParameterizedTest
    @MethodSource
    void testIncompatibleCombineThrows(EnumSet<Statistic> stat1, EnumSet<Statistic> stat2) {
        final double[] v1 = {1, 2, 3.5, 6};
        final double[] v2 = {3, 4, 5};
        final DoubleStatistics statistics = DoubleStatistics.of(stat1, v1);
        final DoubleStatistics other = DoubleStatistics.of(stat2, v2);
        // Store values
        final double[] values = stat1.stream().mapToDouble(statistics::getAsDouble).toArray();
        Assertions.assertThrows(IllegalArgumentException.class, () -> statistics.combine(other),
            () -> stat1 + " " + stat2);
        // Values should be unchanged
        final int[] i = {0};
        stat1.stream().forEach(
            s -> Assertions.assertEquals(values[i[0]++], statistics.getAsDouble(s), () -> s + " changed"));
    }

    static Stream<Arguments> testIncompatibleCombineThrows() {
        return Stream.of(
            Arguments.of(EnumSet.of(Statistic.MIN), EnumSet.of(Statistic.PRODUCT)),
            Arguments.of(EnumSet.of(Statistic.VARIANCE), EnumSet.of(Statistic.MIN)),
            // Note: MEAN is compatible with VARIANCE. The combine is not symmetric.
            Arguments.of(EnumSet.of(Statistic.VARIANCE), EnumSet.of(Statistic.MEAN))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCompatibleCombine(EnumSet<Statistic> stat1, EnumSet<Statistic> stat2) {
        final double[] v1 = {1, 2, 3.5, 6};
        final double[] v2 = {3, 4, 5};
        final DoubleStatistics statistics1 = DoubleStatistics.of(stat1, v1);
        final DoubleStatistics statistics2 = DoubleStatistics.of(stat1, v1);
        // Note: other1 is intentionally using the same flags as statistics1
        final DoubleStatistics other1 = DoubleStatistics.of(stat1, v2);
        final DoubleStatistics other2 = DoubleStatistics.of(stat2, v2);
        // This should work
        statistics1.combine(other1);
        // This should be compatible
        statistics2.combine(other2);
        // The stats should be the same
        for (final Statistic s : stat1) {
            final double expected = statistics1.getAsDouble(s);
            assertFinite(expected, s);
            Assertions.assertEquals(expected, statistics2.getAsDouble(s), s::toString);
        }
    }

    static Stream<Arguments> testCompatibleCombine() {
        return Stream.of(
            Arguments.of(EnumSet.of(Statistic.MEAN), EnumSet.of(Statistic.VARIANCE)),
            Arguments.of(EnumSet.of(Statistic.VARIANCE), EnumSet.of(Statistic.SKEWNESS)),
            Arguments.of(EnumSet.of(Statistic.VARIANCE), EnumSet.of(Statistic.KURTOSIS)),
            Arguments.of(EnumSet.of(Statistic.GEOMETRIC_MEAN), EnumSet.of(Statistic.SUM_OF_LOGS)),
            // Compatible combinations
            Arguments.of(EnumSet.of(Statistic.VARIANCE, Statistic.MIN, Statistic.SKEWNESS),
                         EnumSet.of(Statistic.KURTOSIS, Statistic.MEAN, Statistic.MIN))
        );
    }

    private static void assertFinite(double value, Statistic s) {
        Assertions.assertTrue(Double.isFinite(value), () -> s.toString() + " isFinite");
    }

    @ParameterizedTest
    @MethodSource
    void testBiased(Statistic stat, double[] values, boolean[] options, double[] results) {
        final DoubleStatistics statistics1 = DoubleStatistics.builder(stat).build(values);

        StatisticsConfiguration c = StatisticsConfiguration.withDefaults();
        StatisticResult s = null;
        // Note the circular loop to check setting back to the start option
        for (int index = 0; index <= options.length; index++) {
            final int i = index % options.length;
            final boolean value = options[i];
            c = c.withBiased(value);
            Assertions.assertSame(statistics1, statistics1.setConfiguration(c));

            Assertions.assertEquals(results[i], statistics1.getAsDouble(stat),
                () -> options[i] + " get: " + BaseDoubleStatisticTest.format(values));
            final StatisticResult s1 = statistics1.getResult(stat);
            Assertions.assertEquals(results[i], s1.getAsDouble(),
                () -> options[i] + " supplier: " + BaseDoubleStatisticTest.format(values));

            // Config change does not propagate to previous supplier
            if (s != null) {
                final int j = (i - 1 + options.length) % options.length;
                Assertions.assertEquals(results[j], s.getAsDouble(),
                    () -> options[j] + " previous supplier: " + BaseDoubleStatisticTest.format(values));
            }
            s = s1;

            // Set through the builder
            final DoubleStatistics statistics2 = DoubleStatistics.builder(stat)
                .setConfiguration(c).build(values);
            Assertions.assertEquals(results[i], statistics2.getAsDouble(stat),
                () -> options[i] + " get via builder: " + BaseDoubleStatisticTest.format(values));
        }
    }

    static Stream<Arguments> testBiased() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Data must generate different results for different options
        final double[][] data = {
            {1},
            {1, 2},
            {1, 2, 4},
            {1, 2, 4, 8},
            {1, 3, 6, 7, 19},
        };
        final Statistic[] stats = {Statistic.STANDARD_DEVIATION, Statistic.VARIANCE,
            Statistic.SKEWNESS, Statistic.KURTOSIS};
        final int[] diff = new int[stats.length];
        for (final double[] d : data) {
            diff[0] += addBooleanOptionCase(builder, d, stats[0], StandardDeviation::of, StandardDeviation::setBiased);
            diff[1] += addBooleanOptionCase(builder, d, stats[1], Variance::of, Variance::setBiased);
            diff[2] += addBooleanOptionCase(builder, d, stats[2], Skewness::of, Skewness::setBiased);
            diff[3] += addBooleanOptionCase(builder, d, stats[3], Kurtosis::of, Kurtosis::setBiased);
        }
        // Check the option generated some different results for each statistic
        for (int i = 0; i < stats.length; i++) {
            final Statistic s = stats[i];
            Assertions.assertTrue(diff[i] > data.length, () -> "Option never changes the result: " + s);
        }
        return builder.build();
    }

    /**
     * Adds the expected results for the boolean option of the specified {@code statistic}
     * computed using the provided {@code values}.
     *
     * <p>This method returns the number of unique results. It does not enforce that the
     * results are different.
     *
     * @param <T> {@link DoubleStatistic} being computed.
     * @param builder Argument builder.
     * @param values Values.
     * @param statistic Statistic.
     * @param arrayConstructor Constructor using an array of values.
     * @param setter Option setter.
     * @return the number of unique results
     */
    static <T extends DoubleStatistic & StatisticAccumulator<T>> int addBooleanOptionCase(
            Stream.Builder<Arguments> builder, double[] values, Statistic statistic,
            Function<double[], T> arrayConstructor, BiConsumer<T, Boolean> setter) {
        final T stat = arrayConstructor.apply(values);
        final boolean[] options = {true, false};
        final double[] results = new double[2];
        final Set<Double> all = new HashSet<>();
        for (int i = 0; i < options.length; i++) {
            setter.accept(stat, options[i]);
            results[i] = stat.getAsDouble();
            all.add(results[i]);
        }
        builder.accept(Arguments.of(statistic, values, options, results));
        return all.size();
    }
}
