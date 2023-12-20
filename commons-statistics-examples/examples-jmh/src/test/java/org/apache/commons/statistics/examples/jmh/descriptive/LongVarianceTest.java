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

package org.apache.commons.statistics.examples.jmh.descriptive;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.LongVariance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link LongVariance2}. Tested against {@link LongVariance}.
 */
class LongVarianceTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 13, 1000})
    void testVariance(int n) {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final long[] values = rng.longs(n).toArray();
        final LongVariance v1 = LongVariance.of(values);
        final LongVariance2 v2 = LongVariance2.of(values);
        double variance = v1.getAsDouble();
        final double actual = v1.getAsDouble();
        Assertions.assertEquals(variance, actual, "Variance");

        if (n > 1) {
            variance = v1.setBiased(true).getAsDouble();
            Assertions.assertEquals(variance, v2.setBiased(true).getAsDouble(), "Variance biased");
        }
    }
}
