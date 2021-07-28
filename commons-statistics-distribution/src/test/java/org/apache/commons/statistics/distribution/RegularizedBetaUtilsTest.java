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
package org.apache.commons.statistics.distribution;

import org.apache.commons.numbers.gamma.RegularizedBeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RegularizedBetaUtils}.
 */
class RegularizedBetaUtilsTest {
    @Test
    void testComplement() {
        final double[] xs = {0, 0.1, 0.2, 0.25, 0.3, 1.0 / 3, 0.4, 0.5, 0.6, 2.0 / 3, 0.7, 0.75, 0.8, 0.9, 1};
        // Called in PascalDistribution with a >= 1; b >= 1
        // Called in BinomialDistribution with a >= 1; b >= 1
        final double[] as = {1, 2, 3, 4, 5, 10, 20, 100, 1000};
        final double[] bs = {1, 2, 3, 4, 5, 10, 20, 100, 1000};
        for (final double x : xs) {
            for (final double a : as) {
                for (final double b : bs) {
                    assertComplement(x, a, b);
                }
            }
        }
    }

    private static void assertComplement(double x, double a, double b) {
        final double expected1 = 1.0 - RegularizedBeta.value(x, a, b);
        final double expected2 = RegularizedBeta.value(1 - x, b, a);
        final double actual = RegularizedBetaUtils.complement(x, a, b);
        // Expect binary equality with 1 result
        Assertions.assertTrue(expected1 == actual || expected2 == actual,
            () -> String.format("I(%s, %s, %s) Expected %s or %s: Actual %s", x, a, b, expected1, expected2, actual));
    }
}
