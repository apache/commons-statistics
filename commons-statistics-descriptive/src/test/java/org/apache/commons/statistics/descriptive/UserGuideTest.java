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
import java.util.EnumSet;
import java.util.SplittableRandom;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test code used in the descriptive section of the user guide.
 */
class UserGuideTest {
    @Test
    void testSingleStatistic() {
        int[] values = {1, 1, 2, 3, 5, 8, 13, 21};

        double v = IntVariance.of(values).getAsDouble();

        double m = Stream.of("one", "two", "three", "four")
                         .mapToInt(String::length)
                         .collect(IntMean::create, IntMean::accept, IntMean::combine)
                         .getAsDouble();

        // import numpy as np
        // np.var([1, 1, 2, 3, 5, 8, 13, 21], ddof=1)
        Assertions.assertEquals(49.92857142857143, v, 1e-10);

        // mean = sum([3, 3, 5, 4]) / 4
        Assertions.assertEquals(15.0 / 4.0, m);
    }

    @Test
    void testMultipleStatistics() {
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        // EnumSet and input array data
        DoubleStatistics stats = DoubleStatistics.of(
            EnumSet.of(Statistic.MIN, Statistic.MAX, Statistic.VARIANCE),
            data);
        Assertions.assertEquals(1, stats.getAsDouble(Statistic.MIN));
        Assertions.assertEquals(8, stats.getAsDouble(Statistic.MAX));
        // Python numpy 1.24.4
        // np.var(np.arange(1, 9), ddof=1)
        // np.std(np.arange(1, 9), ddof=1)
        Assertions.assertEquals(6.0, stats.getAsDouble(Statistic.VARIANCE), 1e-10);
        // Get other statistics supported by the underlying computations
        Assertions.assertTrue(stats.isSupported(Statistic.STANDARD_DEVIATION));
        Assertions.assertTrue(stats.isSupported(Statistic.MEAN));
        Assertions.assertEquals(2.449489742783178, stats.getAsDouble(Statistic.STANDARD_DEVIATION), 1e-10);
        Assertions.assertEquals(4.5, stats.getAsDouble(Statistic.MEAN), 1e-10);
    }

    @Test
    void testMultipleStatisticsIndividualValues() {
        IntStatistics stats = IntStatistics.of(
            Statistic.MIN, Statistic.MAX, Statistic.MEAN);
        Stream.of("one", "two", "three", "four")
            .mapToInt(String::length)
            .forEach(stats::accept);

        Assertions.assertEquals(3, stats.getAsInt(Statistic.MIN));
        Assertions.assertEquals(5, stats.getAsInt(Statistic.MAX));
        Assertions.assertEquals(15.0 / 4.0, stats.getAsDouble(Statistic.MEAN), 1e-10);
    }

    @Test
    void testMultipleStatisticsParallelStream() {
        IntStatistics.Builder builder = IntStatistics.builder(
            Statistic.MIN, Statistic.MAX, Statistic.MEAN);
        IntStatistics stats =
            Stream.of("one", "two", "three", "four")
            .parallel()
            .mapToInt(String::length)
            .collect(builder::build, IntConsumer::accept, IntStatistics::combine);

        Assertions.assertEquals(3, stats.getAsInt(Statistic.MIN));
        Assertions.assertEquals(5, stats.getAsInt(Statistic.MAX));
        Assertions.assertEquals(15.0 / 4.0, stats.getAsDouble(Statistic.MEAN), 1e-10);
    }

    @Test
    void testMultipleStatisticsMultipleArrays() {
        double[][] data = {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
        };
        DoubleStatistics.Builder builder = DoubleStatistics.builder(
            Statistic.MIN, Statistic.MAX, Statistic.VARIANCE);
        DoubleStatistics stats = Arrays.stream(data)
            .map(builder::build)
            .reduce(DoubleStatistics::combine)
            .get();
        Assertions.assertEquals(1, stats.getAsDouble(Statistic.MIN));
        Assertions.assertEquals(8, stats.getAsDouble(Statistic.MAX));
        Assertions.assertEquals(6.0, stats.getAsDouble(Statistic.VARIANCE), 1e-10);
        // Get other statistics supported by the underlying computations
        Assertions.assertEquals(2.449489742783178, stats.getAsDouble(Statistic.STANDARD_DEVIATION), 1e-10);
        Assertions.assertEquals(4.5, stats.getAsDouble(Statistic.MEAN), 1e-10);
    }

    @Test
    void testMultipleStatisticsCollector() {
        double[][] data = {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
        };
        // A re-usable Collector
        DoubleStatistics.Builder builder = DoubleStatistics.builder(
            Statistic.MIN, Statistic.MAX, Statistic.VARIANCE);
        Collector<double[], DoubleStatistics, DoubleStatistics> collector =
            Collector.of(builder::build, (s, d) -> s.combine(builder.build(d)), DoubleStatistics::combine);
        DoubleStatistics stats = Arrays.stream(data).collect(collector);
        Assertions.assertEquals(1, stats.getAsDouble(Statistic.MIN));
        Assertions.assertEquals(8, stats.getAsDouble(Statistic.MAX));
        Assertions.assertEquals(6.0, stats.getAsDouble(Statistic.VARIANCE), 1e-10);
        // Get other statistics supported by the underlying computations
        Assertions.assertEquals(2.449489742783178, stats.getAsDouble(Statistic.STANDARD_DEVIATION), 1e-10);
        Assertions.assertEquals(4.5, stats.getAsDouble(Statistic.MEAN), 1e-10);
    }

    @Test
    void testStatisticsCombineCompatibility() {
        double[] data1 = {1, 2, 3, 4};
        double[] data2 = {5, 6, 7, 8};
        DoubleStatistics varStats = DoubleStatistics.builder(Statistic.VARIANCE).build(data1);
        DoubleStatistics meanStats = DoubleStatistics.builder(Statistic.MEAN).build(data2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> varStats.combine(meanStats));
        meanStats.combine(varStats);
        Assertions.assertEquals(4.5, meanStats.getAsDouble(Statistic.MEAN), 1e-10);
    }

    @Test
    void testStatisticsUpdating() {
        DoubleStatistics stats = DoubleStatistics.of(Statistic.MEAN, Statistic.MAX);
        DoubleSupplier mean = stats.getResult(Statistic.MEAN);
        DoubleSupplier max = stats.getResult(Statistic.MAX);
        IntStream.rangeClosed(1, 5).forEach(x -> {
            stats.accept(x);
            Assertions.assertEquals((x + 1.0) / 2, mean.getAsDouble(), "mean");
            Assertions.assertEquals(x, max.getAsDouble(), "max");
            // Example print
            // printf("[1 .. %d] mean=%.1f, max=%s%n", x, mean.getAsDouble(), max.getAsDouble());
        });
    }

    @Test
    void testMedian() {
        double[] data = {8, 7, 6, 5, 4, 3, 2, 1};
        double m = Median.withDefaults()
                         .withCopy(true)
                         .with(NaNPolicy.ERROR)
                         .evaluate(data);
        Assertions.assertEquals(4.5, m);
    }

    @Test
    void testQuantile() {
        int size = 10000;
        double origin = 0;
        double bound = 100;
        double[] data =
            new SplittableRandom(123)
            .doubles(size, origin, bound)
            .toArray();
        double[] q = Quantile.withDefaults()
                             .evaluate(data, 0.25, 0.5, 0.75);
        Assertions.assertEquals(25.0, q[0], 0.5);
        Assertions.assertEquals(50.0, q[1], 0.5);
        Assertions.assertEquals(75.0, q[2], 0.5);
    }
}
