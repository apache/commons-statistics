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
import java.util.SplittableRandom;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test code used in the ranking section of the user guide.
 */
class UserGuideTest {
    @Test
    void testRanking1() {
        final NaturalRanking ranking = new NaturalRanking();
        Assertions.assertArrayEquals(new double[] {1, 2, 3, 4}, ranking.apply(new double[] {5, 6, 7, 8}));
        Assertions.assertArrayEquals(new double[] {4, 1, 3, 2}, ranking.apply(new double[] {8, 5, 7, 6}));
    }

    @Test
    void testRanking2() {
        final double[] data = {8, 5, Double.NaN, 6};
        final NaturalRanking ranking1 = new NaturalRanking();
        Assertions.assertThrows(IllegalArgumentException.class, () -> ranking1.apply(data));
        Assertions.assertArrayEquals(new double[] {4, 2, 1, 3}, new NaturalRanking(NaNStrategy.MINIMAL).apply(data));
        Assertions.assertArrayEquals(new double[] {3, 1, 4, 2}, new NaturalRanking(NaNStrategy.MAXIMAL).apply(data));
        Assertions.assertArrayEquals(new double[] {3, 1, 2}, new NaturalRanking(NaNStrategy.REMOVED).apply(data));
        Assertions.assertArrayEquals(new double[] {3, 1, Double.NaN, 2}, new NaturalRanking(NaNStrategy.FIXED).apply(data));
        final NaturalRanking ranking2 = new NaturalRanking(NaNStrategy.FAILED);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ranking2.apply(data));
    }

    @Test
    void testRanking3() {
        final double[] data = {7, 5, 7, 6};
        Assertions.assertArrayEquals(new double[] {3.5, 1, 3.5, 2}, new NaturalRanking().apply(data));
        Assertions.assertArrayEquals(new double[] {3, 1, 4, 2}, new NaturalRanking(TiesStrategy.SEQUENTIAL).apply(data));
        Assertions.assertArrayEquals(new double[] {3, 1, 3, 2}, new NaturalRanking(TiesStrategy.MINIMUM).apply(data));
        Assertions.assertArrayEquals(new double[] {4, 1, 4, 2}, new NaturalRanking(TiesStrategy.MAXIMUM).apply(data));
        Assertions.assertArrayEquals(new double[] {3.5, 1, 3.5, 2}, new NaturalRanking(TiesStrategy.AVERAGE).apply(data));
        final double[] r = new NaturalRanking(TiesStrategy.RANDOM).apply(data);
        Assertions.assertTrue(Arrays.equals(new double[] {3, 1, 4, 2}, r) || Arrays.equals(new double[] {4, 1, 3, 2}, r));
    }

    @Test
    void testRanking4() {
        final double[] data = {7, 5, 7, 6};
        final double[] r1 = new NaturalRanking(TiesStrategy.RANDOM).apply(data);
        final double[] r2 = new NaturalRanking(new SplittableRandom()::nextLong).apply(data);
        final UniformRandomProvider rng = RandomSource.KISS.create();
        final double[] r3 = new NaturalRanking(rng::nextLong).apply(data);
        final double[] expected1 = {3, 1, 4, 2};
        final double[] expected2 = {4, 1, 3, 2};
        Assertions.assertTrue(Arrays.equals(expected1, r1) || Arrays.equals(expected2, r1));
        Assertions.assertTrue(Arrays.equals(expected1, r2) || Arrays.equals(expected2, r2));
        Assertions.assertTrue(Arrays.equals(expected1, r3) || Arrays.equals(expected2, r3));
    }
}
