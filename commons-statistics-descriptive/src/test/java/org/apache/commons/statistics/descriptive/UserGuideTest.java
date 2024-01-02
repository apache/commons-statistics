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
import java.util.function.DoubleSupplier;
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
    void testVariance() {
        double[] values = {1, 1, 2, 3, 5, 8, 13, 21};

        double v = Variance.of(values).getAsDouble();

        double v2 = Stream.of("one", "two", "three", "four")
                          .mapToDouble(String::length)
                          .collect(Variance::create, Variance::accept, Variance::combine)
                          .getAsDouble();

        // import numpy as np
        // np.var([1, 1, 2, 3, 5, 8, 13, 21], ddof=1)
        Assertions.assertEquals(49.92857142857143, v, 1e-10);

        // np.var([3, 3, 5, 4], ddof=1)
        Assertions.assertEquals(0.9166666666666666, v2);
    }

    @Test
    void testDoubleStatistics1() {
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        DoubleStatistics stats = DoubleStatistics.builder(
            Statistic.MIN, Statistic.MAX, Statistic.VARIANCE)
            .build(data);
        Assertions.assertEquals(1, stats.getAsDouble(Statistic.MIN));
        Assertions.assertEquals(8, stats.getAsDouble(Statistic.MAX));
        // Python numpy 1.24.4
        // np.var(np.arange(1, 9), ddof=1)
        // np.std(np.arange(1, 9), ddof=1)
        Assertions.assertEquals(6.0, stats.getAsDouble(Statistic.VARIANCE), 1e-10);
        // Get other statistics supported by the underlying computations
        Assertions.assertEquals(2.449489742783178, stats.getAsDouble(Statistic.STANDARD_DEVIATION), 1e-10);
        Assertions.assertEquals(4.5, stats.getAsDouble(Statistic.MEAN), 1e-10);
    }

    @Test
    void testDoubleStatistics2() {
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
    void testDoubleStatistics3() {
        double[][] data = {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
        };
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
    void testDoubleStatistics4() {
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        DoubleStatistics varStats = DoubleStatistics.builder(Statistic.VARIANCE).build(data);
        DoubleStatistics meanStats = DoubleStatistics.builder(Statistic.MEAN).build(data);
        Assertions.assertThrows(IllegalArgumentException.class, () -> varStats.combine(meanStats));
        Assertions.assertDoesNotThrow(() -> meanStats.combine(varStats));
    }

    @Test
    void testDoubleStatistics5() {
        DoubleStatistics stats = DoubleStatistics.of(
            EnumSet.of(Statistic.MIN, Statistic.MAX),
            1, 1, 2, 3, 5, 8, 13);
        Assertions.assertEquals(1, stats.getAsDouble(Statistic.MIN));
        Assertions.assertEquals(13, stats.getAsDouble(Statistic.MAX));
    }

    @Test
    void testDoubleStatistics6() {
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
}
