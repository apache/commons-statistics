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

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link GeometricDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class GeometricDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final double p = (Double) parameters[0];
        return new GeometricDistribution(p);
    }

    @Override
    protected double getTolerance() {
        return 1e-12;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {-0.1},
            {0.0},
            {1.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"ProbabilityOfSuccess"};
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Test the PMF is computed using the power function when p is above 0.5.
     * <p>Note: The geometric distribution PMF is defined as:
     * <pre>
     *   pmf(x) = (1-p)^x * p
     * </pre>
     * <p>As {@code p -> 0} use of the power function should be avoided as it will
     * propagate the inexact computation of {@code 1 - p}. The implementation can
     * switch to using a rearrangement with the exponential function which avoid
     * computing {@code 1 - p}.
     * <p>See STATISTICS-34.
     *
     * @param p Probability of success
     */
    @ParameterizedTest
    @ValueSource(doubles = {0.5, 0.6658665, 0.75, 0.8125347, 0.9, 0.95, 0.99})
    void testPMF(double p) {
        final GeometricDistribution dist = new GeometricDistribution(p);
        final int[] x = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40};
        final double[] values = Arrays.stream(x).mapToDouble(k -> p * Math.pow(1 - p, k)).toArray();
        // The PMF should be an exact match to the direct implementation with Math.pow.
        testProbability(dist, x, values, 0.0);
    }

    /**
     * Test the inverse CDF returns the correct x from the CDF result.
     * This case was identified using various probabilities to discover a mismatch
     * of x != icdf(cdf(x)). This occurs due to rounding errors on the inversion.
     *
     * @param p Probability of success
     */
    @ParameterizedTest
    @ValueSource(doubles = {0.2, 0.8})
    void testInverseCDF(double p) {
        final GeometricDistribution dist = new GeometricDistribution(p);
        final int[] x = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        testCumulativeProbabilityInverseMapping(dist, x);
    }

    @Test
    void testAdditionalMoments() {
        GeometricDistribution dist;

        dist = new GeometricDistribution(0.5);
        Assertions.assertEquals((1.0d - 0.5d) / 0.5d, dist.getMean(), getTolerance());
        Assertions.assertEquals((1.0d - 0.5d) / (0.5d * 0.5d), dist.getVariance(), getTolerance());

        dist = new GeometricDistribution(0.3);
        Assertions.assertEquals((1.0d - 0.3d) / 0.3d, dist.getMean(), getTolerance());
        Assertions.assertEquals((1.0d - 0.3d) / (0.3d * 0.3d), dist.getVariance(), getTolerance());
    }
}
