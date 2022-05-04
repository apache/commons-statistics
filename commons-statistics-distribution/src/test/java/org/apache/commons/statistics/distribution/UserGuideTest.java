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

import java.util.stream.IntStream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test code used in the distributions section of the user guide.
 */
class UserGuideTest {
    @Test
    void testCDF() {
        TDistribution t = TDistribution.of(29);
        double lowerTail = t.cumulativeProbability(-2.656);   // P(T(29) &lt;= -2.656)
        double upperTail = t.survivalProbability(2.75);       // P(T(29) &gt; 2.75)

        Assertions.assertTrue(lowerTail > upperTail,
            () -> String.format("Since 2.75 > |-2.656|, expected %s > %s", lowerTail, upperTail));
    }

    @Test
    void testProbability() {
        PoissonDistribution pd = PoissonDistribution.of(1.23);
        double p1 = pd.probability(5);
        double p2 = pd.probability(5, 5);
        double p3 = pd.probability(4, 5);

        Assertions.assertEquals(0, p2);
        Assertions.assertEquals(p1, p3);
    }

    @Test
    void testInverseCDF() {
        NormalDistribution n = NormalDistribution.of(0, 1);
        double x1 = n.inverseCumulativeProbability(1e-300);
        double x2 = n.inverseSurvivalProbability(1e-300);

        Assertions.assertEquals(x1, -x2);
        Assertions.assertEquals(-37.0471, x1, 1e-3);
    }

    @Test
    void testProperties() {
        ChiSquaredDistribution chi2 = ChiSquaredDistribution.of(42);
        double df = chi2.getDegreesOfFreedom();    // 42
        double mean = chi2.getMean();              // 42
        double var = chi2.getVariance();           // 84

        CauchyDistribution cauchy = CauchyDistribution.of(1.23, 4.56);
        double location = cauchy.getLocation();    // 1.23
        double scale = cauchy.getScale();          // 4.56
        double undefined1 = cauchy.getMean();      // NaN
        double undefined2 = cauchy.getVariance();  // NaN

        Assertions.assertEquals(42, df);
        Assertions.assertEquals(42, mean);
        Assertions.assertEquals(84, var);
        Assertions.assertEquals(1.23, location);
        Assertions.assertEquals(4.56, scale);
        Assertions.assertEquals(Double.NaN, undefined1);
        Assertions.assertEquals(Double.NaN, undefined2);
    }

    @Test
    void testDomain() {
        BinomialDistribution b = BinomialDistribution.of(13, 0.15);
        int lower = b.getSupportLowerBound();  // 0
        int upper = b.getSupportUpperBound();  // 13

        Assertions.assertEquals(0, lower);
        Assertions.assertEquals(13, upper);
    }

    @Test
    void testSampling() {
        // From Commons RNG Simple
        UniformRandomProvider rng = RandomSource.KISS.create(123L);

        NormalDistribution n = NormalDistribution.of(0, 1);
        double x = n.createSampler(rng).sample();

        // Generate a number of samples
        GeometricDistribution g = GeometricDistribution.of(0.75);
        int[] k = IntStream.generate(g.createSampler(rng)::sample).limit(100).toArray();

        Assertions.assertTrue(-5 < x && x < 5, () -> Double.toString(x));
        Assertions.assertEquals(100, k.length);
    }

    @Test
    void testComplement() {
        ChiSquaredDistribution chi2 = ChiSquaredDistribution.of(42);
        double q1 = 1 - chi2.cumulativeProbability(168);
        double q2 = chi2.survivalProbability(168);

        Assertions.assertEquals(0, q1);
        Assertions.assertNotEquals(0, q2);

        // For the table
        final double eps = Math.pow(2, -53);
        Assertions.assertEquals(1.110223e-16, eps, 1e-3);
        Assertions.assertEquals(eps, 1 - chi2.cumulativeProbability(166));
        Assertions.assertEquals(eps, 1 - chi2.cumulativeProbability(167));
        Assertions.assertEquals(0, 1 - chi2.cumulativeProbability(168));
        Assertions.assertEquals(0, 1 - chi2.cumulativeProbability(200));
        Assertions.assertEquals(1.16583e-16, chi2.survivalProbability(166), 1e-3);
        Assertions.assertEquals(7.95907e-17, chi2.survivalProbability(167), 1e-3);
        Assertions.assertEquals(5.42987e-17, chi2.survivalProbability(168), 1e-3);
        Assertions.assertEquals(1.19056e-22, chi2.survivalProbability(200), 1e-3);
    }

    @Test
    void testInverseComplement() {
        ChiSquaredDistribution chi2 = ChiSquaredDistribution.of(42);
        double q = 5.43e-17;
        // Incorrect: p = 1 - q == 1.0 !!!
        double x1 = chi2.inverseCumulativeProbability(1 - q);
        // Correct: invert q
        double x2 = chi2.inverseSurvivalProbability(q);

        Assertions.assertEquals(Double.POSITIVE_INFINITY, x1);
        Assertions.assertEquals(168.0, x2, 0.1);
    }
}
