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

import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link ZipfDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class ZipfDistributionTest  extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int n = (Integer) parameters[0];
        final double e = (Double) parameters[1];
        return new ZipfDistribution(n, e);
    }

    @Override
    protected double getAbsoluteTolerance() {
        return 1e-12;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0, 1.0},
            {-1, 1.0},
            {1, -0.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"NumberOfElements", "Exponent"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testHighPrecisionSurvivalProbabilities() {
        // computed using scipy.stats (1.7.1) zipfian
        testSurvivalProbabilityHighPrecision(new ZipfDistribution(60, 10),
            new int[] {57, 59},
            new double[] {2.3189337454689757e-18, 1.6521739576668957e-18},
            DoubleTolerances.absolute(1e-25));
        testSurvivalProbabilityHighPrecision(new ZipfDistribution(60, 50.5),
            new int[] {57, 59},
            new double[] {8.8488396450491320e-90, 1.5972093932264611e-90},
            DoubleTolerances.absolute(1e-95));
        testSurvivalProbabilityHighPrecision(new ZipfDistribution(60, 100.5),
            new int[] {57, 59},
            new double[] {5.9632998443758656e-178, 1.9760564023408183e-179},
            DoubleTolerances.absolute(1e-185));
    }

    /**
     * Test the high precision survival probability computation when the exponent creates
     * an overflow in the intermediate. The result should not be infinite or NaN and
     * it should be a complement to the CDF value.
     */
    @Test
    void testHighPrecisionSurvivalProbabilitiesWithOverflow() {
        // Requires (x+1)^a to overflow
        final int n = 60;
        final double a = 200.5;
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Math.pow(n, a));
        ZipfDistribution dist = new ZipfDistribution(n, a);
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
                    new ZipfDistribution(numPoints, exponent).createSampler(
                        RandomSource.XO_SHI_RO_256_PP.create(6));

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
