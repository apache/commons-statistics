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

import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link ZipfDistribution}.
 * Extends DiscreteDistributionAbstractTest.
 * See class javadoc for DiscreteDistributionAbstractTest for details.
 */
class ZipfDistributionTest extends DiscreteDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-12);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public ZipfDistribution makeDistribution() {
        return new ZipfDistribution(10, 1);
    }

    @Override
    public int[] makeProbabilityTestPoints() {
        return new int[] {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    }

    @Override
    public double[] makeProbabilityTestValues() {
        // Reference values are from R, version 2.15.3 (VGAM package 0.9-0).
        return new double[] {0d, 0d, 0.341417152147, 0.170708576074, 0.113805717382, 0.0853542880369, 0.0682834304295,
                             0.0569028586912, 0.0487738788782, 0.0426771440184, 0.0379352391275, 0.0341417152147, 0};
    }

    @Override
    public double[] makeLogProbabilityTestValues() {
        // Reference values are from R, version 2.14.1.
        return new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                             -1.07465022926458, -1.76779740982453, -2.17326251793269, -2.46094459038447,
                             -2.68408814169868, -2.86640969849264, -3.0205603783199, -3.15409177094442,
                             -3.2718748066008, -3.37723532225863, Double.NEGATIVE_INFINITY};
    }

    @Override
    public int[] makeCumulativeTestPoints() {
        return makeProbabilityTestPoints();
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0, 0, 0.341417152147, 0.512125728221, 0.625931445604, 0.71128573364,
                             0.77956916407, 0.836472022761, 0.885245901639, 0.927923045658, 0.965858284785, 1d, 1d};
    }

    @Override
    public double[] makeInverseCumulativeTestPoints() {
        return new double[] {0d, 0.001d, 0.010d, 0.025d, 0.050d, 0.3413d, 0.3415d, 0.999d,
                             0.990d, 0.975d, 0.950d, 0.900d, 1d};
    }

    @Override
    public int[] makeInverseCumulativeTestValues() {
        return new int[] {1, 1, 1, 1, 1, 1, 2, 10, 10, 10, 9, 8, 10};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testParameterAccessors() {
        final ZipfDistribution distribution = makeDistribution();
        Assertions.assertEquals(10, distribution.getNumberOfElements());
        Assertions.assertEquals(1.0, distribution.getExponent());
    }

    @Test
    void testConstructorPreconditions1() {
        Assertions.assertThrows(DistributionException.class, () -> new ZipfDistribution(0, 1));
    }

    @Test
    void testConstructorPreconditions2() {
        Assertions.assertThrows(DistributionException.class, () -> new ZipfDistribution(1, 0));
    }

    @Test
    void testMoments() {
        final double tol = 1e-9;
        ZipfDistribution dist;

        dist = new ZipfDistribution(2, 0.5);
        Assertions.assertEquals(Math.sqrt(2), dist.getMean(), tol);
        Assertions.assertEquals(0.24264068711928521, dist.getVariance(), tol);
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
                        RandomSource.create(RandomSource.WELL_19937_C, 6));

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
