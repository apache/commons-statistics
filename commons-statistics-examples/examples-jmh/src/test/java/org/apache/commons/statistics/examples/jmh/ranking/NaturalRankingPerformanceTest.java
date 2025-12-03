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

package org.apache.commons.statistics.examples.jmh.ranking;

import java.util.function.UnaryOperator;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.examples.jmh.ranking.NaturalRankingPerformance.DataSource;
import org.apache.commons.statistics.examples.jmh.ranking.NaturalRankingPerformance.RankingSource;
import org.apache.commons.statistics.ranking.NaturalRanking;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Executes tests for {@link NaturalRankingPerformance}.
 */
class NaturalRankingPerformanceTest {
    /**
     * Test the ranking of data from the data source.
     *
     * <p>Each known method in the benchmark is tested to: create the same ranking;
     * and not destructively modify the data. Violation of these assumptions will
     * invalidate the performance benchmark.
     */
    @ParameterizedTest
    @CsvSource({
        "100, 0, 0, 1",
        "100, 0.5, 5, 1",
        "100, 1.0, 5, 0.5",
        "100, 0.25, 5, 2",
    })
    void testDataSource(int length,  double tieFraction, int ties, double alpha) {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final double[] data = DataSource.createData(rng, length, tieFraction, ties, alpha);

        final double[] original = data.clone();
        double[] ranking = null;
        for (final String name : RankingSource.getFunctionNames()) {
            final double[] r = RankingSource.createFunction(name).apply(data);
            Assertions.assertArrayEquals(original, data, () -> name + " destroyed the data");
            if (ranking == null) {
                ranking = r;
            } else {
                Assertions.assertArrayEquals(ranking, r, () -> name + " has a different ranking");
            }
        }
    }

    /**
     * Demonstrate the Commons Math3 bug that has been fixed in statistics.
     */
    @Test
    void testNoData() {
        final double[] empty = {};
        final UnaryOperator<double[]> fun1 = new org.apache.commons.math3.stat.ranking.NaturalRanking()::rank;
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> fun1.apply(empty));
        final UnaryOperator<double[]> fun2 = new NaturalRanking();
        Assertions.assertArrayEquals(empty, fun2.apply(empty));
    }
}
