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
package org.apache.commons.statistics.inference;

import org.apache.commons.statistics.distribution.HypergeometricDistribution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for {@link Hypergeom}.
 *
 * <p>This verifies the cache of the hypergeometric distribution is used to compute the same
 * probability values as the underlying {@link HypergeometricDistribution}.
 */
class HypergeomTest {

    @ParameterizedTest
    @CsvSource({
        "10, 5, 5",
        "10, 3, 5",
        "12, 5, 3",
    })
    void testDistribution(int n, int k, int m) {
        final HypergeometricDistribution d1 = HypergeometricDistribution.of(n, k, m);
        final Hypergeom d2 = new Hypergeom(n, k, m);
        final int lo = d1.getSupportLowerBound();
        final int hi = d2.getSupportUpperBound();
        Assertions.assertEquals(lo, d2.getSupportLowerBound(), "lower bound");
        Assertions.assertEquals(hi, d2.getSupportUpperBound(), "upper bound");
        // Out of bounds will throw
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> d2.pmf(-1), "pmf(-1)");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> d2.pmf(hi + 1), "pmf(hi + 1)");
        // Out of bounds for the cumulative functions is supported
        Assertions.assertEquals(d1.cumulativeProbability(lo - 1), d2.cdf(lo - 1), "cdf(lo - 1)");
        Assertions.assertEquals(d1.cumulativeProbability(hi + 1), d2.cdf(hi + 1), "cdf(hi + 1)");
        Assertions.assertEquals(d1.survivalProbability(lo - 1), d2.sf(lo - 1), "sf(lo - 1)");
        Assertions.assertEquals(d1.survivalProbability(hi + 1), d2.sf(hi + 1), "sf(hi + 1)");
        for (int x = lo; x <= hi; x++) {
            Assertions.assertEquals(d1.probability(x), d2.pmf(x), "pmf");
            Assertions.assertEquals(d1.cumulativeProbability(x), d2.cdf(x), "cdf");
            Assertions.assertEquals(d1.survivalProbability(x), d2.sf(x), "sf");
        }
        // Test the mode is the highest pmf
        double p = d2.pmf(lo);
        for (int x = lo + 1;; x++) {
            final double p2 = d2.pmf(x);
            if (p2 > p) {
                p = p2;
            } else {
                Assertions.assertEquals(d2.getLowerMode(), x - 1, "lower mode");
                break;
            }
        }
        p = d2.pmf(hi);
        for (int x = hi - 1;; x--) {
            final double p2 = d2.pmf(x);
            if (p2 > p) {
                p = p2;
            } else {
                Assertions.assertEquals(d2.getUpperMode(), x + 1, "upper mode");
                break;
            }
        }
    }
}
