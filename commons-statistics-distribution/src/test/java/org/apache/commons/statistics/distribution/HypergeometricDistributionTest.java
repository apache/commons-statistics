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
import java.util.stream.Stream;
import org.apache.commons.numbers.core.Precision;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link HypergeometricDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class HypergeometricDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int populationSize = (Integer) parameters[0];
        final int numberOfSuccesses = (Integer) parameters[1];
        final int sampleSize = (Integer) parameters[2];
        return HypergeometricDistribution.of(populationSize, numberOfSuccesses, sampleSize);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0, 3, 5},
            {-1, 3, 5},
            {5, -1, 5},
            {5, 3, -1},
            {5, 6, 5},
            {5, 3, 6},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"PopulationSize", "NumberOfSuccesses", "SampleSize"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(int populationSize,
                               int numberOfSuccesses,
                               int sampleSize,
                               double mean, double variance) {
        final HypergeometricDistribution dist = HypergeometricDistribution.of(populationSize, numberOfSuccesses, sampleSize);
        testMoments(dist, mean, variance, DoubleTolerances.ulps(1));
    }

    static Stream<Arguments> testAdditionalMoments() {
        return Stream.of(
            Arguments.of(1500, 40, 100, 40d * 100d / 1500d, (100d * 40d * (1500d - 100d) * (1500d - 40d)) / (1500d * 1500d * 1499d)),
            Arguments.of(3000, 55, 200, 55d * 200d / 3000d, (200d * 55d * (3000d - 200d) * (3000d - 55d)) / (3000d * 3000d * 2999d))
        );
    }

    @Test
    void testLargeValues() {
        final int populationSize = 3456;
        final int sampleSize = 789;
        final int numberOfSucceses = 101;
        // data[i][3] contains P(x >= x).
        // It is tested using survivalProbability(x - 1)
        final double[][] data = {
            {0.0, 2.75646034603961e-12, 2.75646034603961e-12, 1.0},
            {1.0, 8.55705370142386e-11, 8.83269973602783e-11, 0.999999999997244},
            {2.0, 1.31288129219665e-9, 1.40120828955693e-9, 0.999999999911673},
            {3.0, 1.32724172984193e-8, 1.46736255879763e-8, 0.999999998598792},
            {4.0, 9.94501711734089e-8, 1.14123796761385e-7, 0.999999985326375},
            {5.0, 5.89080768883643e-7, 7.03204565645028e-7, 0.999999885876203},
            {20.0, 0.0760051397707708, 0.27349758476299, 0.802507555007781},
            {21.0, 0.087144222047629, 0.360641806810619, 0.72650241523701},
            {22.0, 0.0940378846881819, 0.454679691498801, 0.639358193189381},
            {23.0, 0.0956897500614809, 0.550369441560282, 0.545320308501199},
            {24.0, 0.0919766921922999, 0.642346133752582, 0.449630558439718},
            {25.0, 0.083641637261095, 0.725987771013677, 0.357653866247418},
            {96.0, 5.93849188852098e-57, 1.0, 6.01900244560712e-57},
            {97.0, 7.96593036832547e-59, 1.0, 8.05105570861321e-59},
            {98.0, 8.44582921934367e-61, 1.0, 8.5125340287733e-61},
            {99.0, 6.63604297068222e-63, 1.0, 6.670480942963e-63},
            {100.0, 3.43501099007557e-65, 1.0, 3.4437972280786e-65},
            {101.0, 8.78623800302957e-68, 1.0, 8.78623800302957e-68},
            // Out of domain
            {sampleSize + 1, 0, 1.0, 0},
        };

        testHypergeometricDistributionProbabilities(populationSize, sampleSize, numberOfSucceses, data);
    }

    private static void testHypergeometricDistributionProbabilities(int populationSize, int sampleSize,
        int numberOfSuccesses, double[][] data) {
        final HypergeometricDistribution dist = HypergeometricDistribution.of(populationSize, numberOfSuccesses, sampleSize);
        for (int i = 0; i < data.length; ++i) {
            final int x = (int)data[i][0];
            final double pmf = data[i][1];
            final double actualPmf = dist.probability(x);
            TestUtils.assertRelativelyEquals(() -> "Expected equals for <" + x + "> pmf", pmf, actualPmf, 1.0e-9);

            final double cdf = data[i][2];
            final double actualCdf = dist.cumulativeProbability(x);
            TestUtils.assertRelativelyEquals(() -> "Expected equals for <" + x + "> cdf", cdf, actualCdf, 1.0e-9);

            final double cdf1 = data[i][3];
            final double actualCdf1 = dist.survivalProbability(x - 1);
            TestUtils.assertRelativelyEquals(() -> "Expected equals for <" + x + "> cdf1", cdf1, actualCdf1, 1.0e-9);
        }
    }

    @Test
    void testMoreLargeValues() {
        final int populationSize = 26896;
        final int sampleSize = 895;
        final int numberOfSucceses = 55;
        final double[][] data = {
            {0.0, 0.155168304750504, 0.155168304750504, 1.0},
            {1.0, 0.29437545000746, 0.449543754757964, 0.844831695249496},
            {2.0, 0.273841321577003, 0.723385076334967, 0.550456245242036},
            {3.0, 0.166488572570786, 0.889873648905753, 0.276614923665033},
            {4.0, 0.0743969744713231, 0.964270623377076, 0.110126351094247},
            {5.0, 0.0260542785784855, 0.990324901955562, 0.0357293766229237},
            {20.0, 3.57101101678792e-16, 1.0, 3.78252101622096e-16},
            {21.0, 2.00551638598312e-17, 1.0, 2.11509999433041e-17},
            {22.0, 1.04317070180562e-18, 1.0, 1.09583608347287e-18},
            {23.0, 5.03153504903308e-20, 1.0, 5.266538166725e-20},
            {24.0, 2.2525984149695e-21, 1.0, 2.35003117691919e-21},
            {25.0, 9.3677424515947e-23, 1.0, 9.74327619496943e-23},
            {50.0, 9.83633962945521e-69, 1.0, 9.8677629437617e-69},
            {51.0, 3.13448949497553e-71, 1.0, 3.14233143064882e-71},
            {52.0, 7.82755221928122e-74, 1.0, 7.84193567329055e-74},
            {53.0, 1.43662126065532e-76, 1.0, 1.43834540093295e-76},
            {54.0, 1.72312692517348e-79, 1.0, 1.7241402776278e-79},
            {55.0, 1.01335245432581e-82, 1.0, 1.01335245432581e-82},
        };
        testHypergeometricDistributionProbabilities(populationSize, sampleSize, numberOfSucceses, data);
    }

    /**
     * Test Math-644 is ported from Commons Math 3 where the distribution had the function
     * upperCumulativeProbability(x) to compute P(X >= x). This has been replaced
     * in Commons Statistics with survivalProbability(x) which computes P(X > x). To
     * create the equivalent use survivalProbability(x - 1).
     */
    @Test
    void testMath644() {
        final int N = 14761461;  // population
        final int m = 1035;      // successes in population
        final int n = 1841;      // number of trials

        final int k = 0;
        final HypergeometricDistribution dist = HypergeometricDistribution.of(N, m, n);

        // Compute upper cumulative probability using the survival probability
        Assertions.assertEquals(0, Precision.compareTo(1.0, dist.survivalProbability(k - 1), 1));
        Assertions.assertTrue(Precision.compareTo(dist.cumulativeProbability(k), 0.0, 1) > 0);

        // another way to calculate the upper cumulative probability
        final double upper = 1.0 - dist.cumulativeProbability(k) + dist.probability(k);
        Assertions.assertEquals(0, Precision.compareTo(1.0, upper, 1));
    }

    @Test
    void testZeroTrials() {
        final int n = 11; // population
        final int m = 4;  // successes in population
        final int s = 0;  // number of trials

        final HypergeometricDistribution dist = HypergeometricDistribution.of(n, m, s);

        for (int i = 1; i <= n; i++) {
            final double p = dist.probability(i);
            Assertions.assertEquals(0, p, () -> "p=" + p);
        }
    }

    @Test
    void testMath1356() {
        final int n = 11;  // population
        final int m = 11;  // successes in population

        for (int s = 0; s <= n; s++) {
            final HypergeometricDistribution dist = HypergeometricDistribution.of(n, m, s);
            final double p = dist.probability(s);
            Assertions.assertEquals(1, p, () -> "p=" + p);
        }
    }

    @Test
    void testMath1021() {
        final int N = 43130568;
        final int m = 42976365;
        final int n = 50;
        final DiscreteDistribution.Sampler dist =
            HypergeometricDistribution.of(N, m, n).createSampler(RandomSource.XO_SHI_RO_256_PP.create());

        for (int i = 0; i < 100; i++) {
            final int sample = dist.sample();
            Assertions.assertTrue(0 <= sample, () -> "sample=" + sample);
            Assertions.assertTrue(sample <= n, () -> "sample=" + sample);
        }
    }

    @Test
    void testAdditionalCumulativeProbabilityHighPrecision() {
        // computed using R version 3.4.4
        testCumulativeProbabilityHighPrecision(
            HypergeometricDistribution.of(500, 70, 300),
            new int[] {10, 8},
            new double[] {2.4055720603264525e-17, 1.2848174992266236e-19},
            DoubleTolerances.relative(5e-14));
    }

    @Test
    void testAdditionalSurvivalProbabilityHighPrecision() {
        // computed using R version 3.4.4
        testSurvivalProbabilityHighPrecision(
            HypergeometricDistribution.of(500, 70, 300),
            new int[] {68, 69},
            new double[] {4.570379934029859e-16, 7.4187180434325268e-18},
            DoubleTolerances.relative(5e-14));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 0, 0",
        "1, 1, 0",
        "1, 0, 1",
        "1, 1, 1",
        "2, 1, 1",
        "2, 1, 2",
        "2, 2, 1",
        "2, 2, 2",
        "3, 1, 1",
        "3, 1, 2",
        "3, 1, 3",
        "3, 2, 1",
        "3, 2, 2",
        "3, 2, 3",
        "3, 3, 1",
        "3, 3, 2",
        "3, 3, 3",
        // Mean = n * K / N
        "15, 9, 7", // 4.2
        "23, 13, 11", // 6.22
        "200, 130, 70", // 45.5
    })
    void testAdditionalInverseMapping(int populationSize, int numberOfSuccesses, int sampleSize) {
        final HypergeometricDistribution dist = HypergeometricDistribution.of(populationSize, numberOfSuccesses, sampleSize);
        final int[] points = IntStream.rangeClosed(dist.getSupportLowerBound(), dist.getSupportUpperBound()).toArray();
        testCumulativeProbabilityInverseMapping(dist, points);
        testSurvivalProbabilityInverseMapping(dist, points);
    }
}
