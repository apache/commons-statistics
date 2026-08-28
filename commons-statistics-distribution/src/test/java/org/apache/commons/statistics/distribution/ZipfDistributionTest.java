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

package org.apache.commons.statistics.distribution;

import java.util.stream.Stream;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link ZipfDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class ZipfDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int n = (Integer) parameters[0];
        final double e = (Double) parameters[1];
        return ZipfDistribution.of(n, e);
    }


    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0, 1.0},
            {-1, 1.0},
            {1, -0.1},
            {1, Double.NaN},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"NumberOfElements", "Exponent"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 1e-14;
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Test additional moments.
     */
    @ParameterizedTest
    @CsvSource({
        // Generated using scipy 1.16.3 using scipy.stats.zipfian.stats(exp, n)
        "150, 0.512, 52.707637767916495, 1966.9356468021338",
        "73, 1.67, 4.937625767687036, 87.76033876340095",
        "999, 2.1, 3.5725516349635846, 343.7153292773371",
    })
    void testAdditionalMoments(int n, double exp, double mean, double variance) {
        final DoubleTolerance tolerance = createRelTolerance(1e-14);
        final ZipfDistribution dist = ZipfDistribution.of(n, exp);
        testMoments(dist, mean, variance, tolerance);
        // Run twice to check the cached N-th harmonic numbers
        testMoments(dist, mean, variance, tolerance);
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionlSurvivalProbabilityHighPrecision(int n, double e, int[] x, double[] expected, DoubleTolerance tol) {
        testSurvivalProbabilityHighPrecision(ZipfDistribution.of(n, e), x, expected, tol);
    }

    static Stream<Arguments> testAdditionlSurvivalProbabilityHighPrecision() {
        // Computed from the generalized harmonic number and the upper
        // series of terms using Matlab R2023a VPA, e.g.:
        // vpa(symsum(1/k^10, k, 1, 60), 30)
        // vpa(symsum(1/k^10, k, 58, 60), 30)
        // vpa(symsum(1/k^10, k, 60, 60), 30)
        // Generalized harmonic numbers, the upper summations are inlined below
        final double k60e10 = 1.00099457512781807511565108861;
        final double k60e505 = 1.00000000000000062803698427773;
        final double k60e1005 = 1.0;
        return Stream.of(
            Arguments.of(60, 10,
                new int[] {57, 59},
                new double[] {
                    0.00000000000000000593155740928262062795573723513 / k60e10,
                    0.0000000000000000016538171687920201866246676489 / k60e10},
                DoubleTolerances.relative(1e-14)),
            Arguments.of(60, 50.5,
                new int[] {57, 59},
                new double[] {
                    1.41783221158702775324465028711e-89 / k60e505,
                    1.59720939322646230873883366414e-90 / k60e505},
                DoubleTolerances.relative(1e-14)),
            Arguments.of(60, 100.5,
                new int[] {57, 59},
                new double[] {
                    7.23087851232732627244617172568e-178 / k60e1005,
                    1.9760564023408181841715991846e-179 / k60e1005},
                DoubleTolerances.relative(1e-14))
        );
    }

    /**
     * Test the high precision survival probability computation when the exponent creates
     * an overflow in the intermediate. The result should not be infinite or NaN and
     * it should be a complement to the CDF value.
     */
    @Test
    void testAdditionalSurvivalAndCumulativeProbabilityComplement() {
        // Requires (x+1)^a to overflow
        final int n = 60;
        final double a = 200.5;
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Math.pow(n, a));
        ZipfDistribution dist = ZipfDistribution.of(n, a);
        final int[] points = MathArrays.natural(n);
        testSurvivalAndCumulativeProbabilityComplement(dist, points, createTolerance());
    }

    /**
     * Test sampling for various number of points and exponents.
     */
    @Test
    void testSamplingExtended() {
        final int sampleSize = 1000;

        final int[] numPointsValues = {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100
        };
        final double[] exponentValues = {
            1e-10, 1e-9, 1e-8, 1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1, 2e-1, 5e-1,
            1. - 1e-9, 1.0, 1. + 1e-9, 1.1, 1.2, 1.3, 1.5, 1.6, 1.7, 1.8, 2.0,
            2.5, 3.0, 4., 5., 6., 7., 8., 9., 10., 20., 30., 100., 150.
        };

        for (final int numPoints : numPointsValues) {
            for (final double exponent : exponentValues) {
                double weightSum = 0.;
                final double[] weights = new double[numPoints];
                for (int i = numPoints; i >= 1; i -= 1) {
                    weights[i - 1] = Math.pow(i, -exponent);
                    weightSum += weights[i - 1];
                }

                // Use fixed seed, the test is expected to fail for more than 50% of all
                // seeds because each test case can fail with probability 0.001, the chance
                // that all test cases do not fail is 0.999^(32*22) = 0.49442874426
                final DiscreteDistribution.Sampler distribution =
                    ZipfDistribution.of(numPoints, exponent).createSampler(
                        RandomSource.XO_SHI_RO_256_PP.create(1));

                final double[] expectedCounts = new double[numPoints];
                final long[] observedCounts = new long[numPoints];
                for (int i = 0; i < numPoints; i++) {
                    expectedCounts[i] = sampleSize * (weights[i] / weightSum);
                }
                final int[] sample = TestUtils.sample(sampleSize, distribution);
                for (final int s : sample) {
                    observedCounts[s - 1]++;
                }
                TestUtils.assertChiSquareAccept(expectedCounts, observedCounts, 0.001);
            }
        }
    }
}
