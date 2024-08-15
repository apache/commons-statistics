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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link FoldedNormalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class FoldedNormalDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mu = (Double) parameters[0];
        final double sigma = (Double) parameters[1];
        return FoldedNormalDistribution.of(mu, sigma);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0},
            {0.0, -0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Mu", "Sigma"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Test the mean. This is performed using the folding together of two truncated
     * normal distributions, with the truncation at the origin.
     *
     * <p>This test cross-validates the mean computation.
     */
    @ParameterizedTest
    @MethodSource
    void testMean(double mu, double sigma) {
        // Expected mean is the weighted means of each truncated distribution;
        // the mean of the distribution below the origin must be negated
        final TruncatedNormalDistribution t1 = TruncatedNormalDistribution.of(mu, sigma, Double.NEGATIVE_INFINITY, 0);
        final TruncatedNormalDistribution t2 = TruncatedNormalDistribution.of(mu, sigma, 0, Double.POSITIVE_INFINITY);
        final NormalDistribution n = NormalDistribution.of(mu, sigma);
        final double p1 = n.cumulativeProbability(0);
        final double p2 = 1 - p1;
        final double expected = p2 * t2.getMean() - p1 * t1.getMean();
        TestUtils.assertEquals(expected, FoldedNormalDistribution.of(mu, sigma).getMean(),
            DoubleTolerances.relative(1e-14));
    }

    static Stream<Arguments> testMean() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final double mu : new double[] {-3, -2, -1, 0, 1, 2, 3}) {
            for (final double sigma : new double[] {0.75, 1, 1.5}) {
                builder.add(Arguments.of(mu, sigma));
            }
        }
        return builder.build();
    }

    @Test
    void testCumulativeProbabilityExtremes() {
        // Use a small shape parameter so that we can exceed 40 * shape
        testCumulativeProbability(FoldedNormalDistribution.of(1, 0.0001),
                                  new double[] {0, 10},
                                  new double[] {0, 1.0},
                                  DoubleTolerances.equals());
    }

    @Test
    void testSurvivalProbabilityExtremes() {
        // Use a small shape parameter so that we can exceed 40 * shape
        testSurvivalProbability(FoldedNormalDistribution.of(1, 0.0001),
                                new double[] {0, 10},
                                new double[] {1.0, 0.0},
                                DoubleTolerances.equals());
    }
}
