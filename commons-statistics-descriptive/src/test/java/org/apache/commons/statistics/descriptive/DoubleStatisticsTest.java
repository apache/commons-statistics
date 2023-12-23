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
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
 */
class DoubleStatisticsTest {
    /** Empty statistic array. */
    private static final Statistic[] EMPTY_STATISTIC_ARRAY = {};

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
                .reduce(StatisticAccumulator::combine)
                .orElseThrow(IllegalStateException::new)
                .getAsDouble();
            // Create from array
            final double e2 = d.stream()
                .map(arrayConstructor)
                .reduce(StatisticAccumulator::combine)
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
        final EnumSet<Statistic> computed = EnumSet.copyOf(stats);
        stats.forEach(s -> computed.addAll(coComputed.get(s)));

        // Test if the statistics are correctly identified as supported
        EnumSet.allOf(Statistic.class).forEach(s ->
            Assertions.assertEquals(computed.contains(s), statistics.isSupported(s),
                () -> stats + " isSupported -> " + s.toString()));

        // Test the values
        computed.forEach(s ->
            Assertions.assertEquals(expected.applyAsDouble(expectedResult.get(s).get(id)), statistics.get(s),
                () -> stats + " value -> " + s.toString()));
    }

    /**
     * Add all the {@code values} to an aggregator of the {@code statistics}.
     *
     * <p>This method verifies that the {@link DoubleStatistics#get(Statistic)} and
     * {@link DoubleStatistics#getSupplier(Statistic)} methods return the same
     * result as values are added.
     *
     * @param statistic Statistics.
     * @param values Values.
     * @return the statistics
     */
    private static DoubleStatistics acceptAll(Statistic[] statistics, double[] values) {
        final DoubleStatistics stats = DoubleStatistics.of(statistics);
        final DoubleSupplier[] f = getSuppliers(statistics, stats);
        for (final double x : values) {
            stats.accept(x);
            for (int i = 0; i < statistics.length; i++) {
                final Statistic s = statistics[i];
                Assertions.assertEquals(stats.get(s), f[i].getAsDouble(),
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
    private static DoubleSupplier[] getSuppliers(Statistic[] statistics, final DoubleStatistics stats) {
        final DoubleSupplier[] f = new DoubleSupplier[statistics.length];
        for (int i = 0; i < statistics.length; i++) {
            final DoubleSupplier supplier = stats.getSupplier(statistics[i]);
            Assertions.assertFalse(supplier instanceof DoubleStatistic,
                () -> "DoubleStatistic instance: " + supplier.getClass().getSimpleName());
            f[i] = supplier;
        }
        return f;
    }

    /**
     * Combine the two statistic aggregators.
     *
     * <p>This method verifies that the {@link DoubleStatistics#get(Statistic)} and
     * {@link DoubleStatistics#getSupplier(Statistic)} methods return the same
     * result after the {@link DoubleStatistics#combine(DoubleStatistics)}.
     *
     * @param statistics Statistics to compute.
     * @param s1 Statistic aggregator.
     * @param s2 Statistic aggregator.
     * @return the double statistics
     */
    private static DoubleStatistics combine(Statistic[] statistics,
        DoubleStatistics s1, DoubleStatistics s2) {
        final DoubleSupplier[] f = getSuppliers(statistics, s1);
        s1.combine(s2);
        for (int i = 0; i < statistics.length; i++) {
            final Statistic s = statistics[i];
            Assertions.assertEquals(s1.get(s), f[i].getAsDouble(),
                () -> "Supplier(" + s + ") after combine");
        }
        return s1;
    }

    @Test
    void testNoOpConsumer() {
        final DoubleConsumer c = DoubleStatistics.NOOP;
        // Hit coverage
        c.accept(0);
        final double[] value = {0};
        final DoubleConsumer other = x -> value[0] = x;
        final DoubleConsumer combined = c.andThen(other);
        Assertions.assertSame(combined, other);
        final double y = 42;
        combined.accept(y);
        Assertions.assertEquals(y, value[0]);
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
    void testBuilderThrows() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.builder());
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStatistics.builder(EMPTY_STATISTIC_ARRAY));
        Assertions.assertThrows(NullPointerException.class, () -> DoubleStatistics.builder(new Statistic[1]));
    }

    @Test
    void testIsSupportedWithNull() {
        DoubleStatistics s = DoubleStatistics.of(Statistic.MIN);
        Assertions.assertThrows(NullPointerException.class, () -> s.isSupported(null));
    }

    @ParameterizedTest
    @MethodSource
    void testNotSupported(Statistic stat) {
        DoubleStatistics statistics = DoubleStatistics.of(stat);
        for (final Statistic s : Statistic.values()) {
            Assertions.assertEquals(s == stat, statistics.isSupported(s),
                () -> stat + " isSupported -> " + s.toString());
            if (s == stat) {
                Assertions.assertDoesNotThrow(() -> statistics.get(s),
                    () -> stat + " get -> " + s.toString());
                Assertions.assertNotNull(statistics.getSupplier(s),
                    () -> stat + " getSupplier -> " + s.toString());
            } else {
                Assertions.assertThrows(IllegalArgumentException.class, () -> statistics.get(s),
                    () -> stat + " get -> " + s.toString());
                Assertions.assertThrows(IllegalArgumentException.class, () -> statistics.getSupplier(s),
                    () -> stat + " getSupplier -> " + s.toString());
            }
        }
    }

    static Statistic[] testNotSupported() {
        return new Statistic[] {Statistic.MIN, Statistic.PRODUCT};
    }

    @ParameterizedTest
    @MethodSource
    void testIncompatibleCombineThrows(EnumSet<Statistic> stat1, EnumSet<Statistic> stat2) {
        final double[] v1 = {1, 2, 3.5, 6};
        final double[] v2 = {3, 4, 5};
        DoubleStatistics statistics = DoubleStatistics.of(stat1, v1);
        DoubleStatistics other = DoubleStatistics.of(stat2, v2);
        // Store values
        final double[] values = stat1.stream().mapToDouble(statistics::get).toArray();
        Assertions.assertThrows(IllegalArgumentException.class, () -> statistics.combine(other),
            () -> stat1 + " " + stat2);
        // Values should be unchanged
        final int[] i = {0};
        stat1.stream().forEach(
            s -> Assertions.assertEquals(values[i[0]++], statistics.get(s), () -> s + " changed"));
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
            final double expected = statistics1.get(s);
            assertFinite(expected, s);
            Assertions.assertEquals(expected, statistics2.get(s), () -> s.toString());
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
        DoubleSupplier s = null;
        // Note the circular loop to check setting back to the start option
        for (int index = 0; index <= options.length; index++) {
            final int i = index % options.length;
            final boolean value = options[i];
            c = c.withBiased(value);
            Assertions.assertSame(statistics1, statistics1.setConfiguration(c));

            Assertions.assertEquals(results[i], statistics1.get(stat),
                () -> options[i] + " get: " + BaseDoubleStatisticTest.format(values));
            final DoubleSupplier s1 = statistics1.getSupplier(stat);
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
            Assertions.assertEquals(results[i], statistics2.get(stat),
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
    private static <T extends DoubleStatistic & StatisticAccumulator<T>> int addBooleanOptionCase(
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
