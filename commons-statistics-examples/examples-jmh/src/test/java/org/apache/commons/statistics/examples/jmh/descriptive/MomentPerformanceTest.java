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

import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.statistics.descriptive.Mean;
import org.apache.commons.statistics.examples.jmh.descriptive.MomentPerformance.ExtendedSumFirstMoment;
import org.apache.commons.statistics.examples.jmh.descriptive.MomentPerformance.RollingFirstMoment;
import org.apache.commons.statistics.examples.jmh.descriptive.MomentPerformance.SafeRollingFirstMoment;
import org.apache.commons.statistics.examples.jmh.descriptive.MomentPerformance.SumFirstMoment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Executes tests for {@link MomentPerformance}.
 */
class MomentPerformanceTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10})
    void testFirstMoment(int n) {
        final double[] values = ThreadLocalRandom.current().doubles(n).toArray();
        // Expected should be 0.5 as n -> inf
        final double expected = Mean.of(values).getAsDouble();
        final double tolerance = n <= 1 ? 0 : expected * 1e-14;
        Assertions.assertEquals(expected, MomentPerformance.forEach(new RollingFirstMoment(), values).getAsDouble(), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.forEach(new SafeRollingFirstMoment(), values).getAsDouble(), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.forEach(new SumFirstMoment(), values).getAsDouble(), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.forEach(new ExtendedSumFirstMoment(), values).getAsDouble(), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.arrayRollingFirstMoment(values), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.arraySafeRollingFirstMoment(values), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.arrayInlineRollingFirstMoment(values), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.arrayInlineSafeRollingFirstMoment(values), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.arrayInlineSafeRollingFirstMomentExt(values), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.arraySumMean(values), tolerance);
        Assertions.assertEquals(expected, MomentPerformance.arrayDDSumMean(values), tolerance);
    }
}
