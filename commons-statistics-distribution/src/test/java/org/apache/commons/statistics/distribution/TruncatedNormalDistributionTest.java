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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test class for {@link TruncatedNormalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 * All test values were computed using Python with SciPy v1.6.0.
 */
class TruncatedNormalDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mean = (Double) parameters[0];
        final double sd = (Double) parameters[1];
        final double upper = (Double) parameters[2];
        final double lower = (Double) parameters[3];
        return TruncatedNormalDistribution.of(mean, sd, upper, lower);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0, -1.0, 1.0},
            {0.0, -0.1, -1.0, 1.0},
            {0.0, 1.0, 1.0, -1.0},
        };
    }

    @Override
    String[] getParameterNames() {
        // Input mean and standard deviation refer to the underlying normal distribution.
        // The constructor arguments do not match the mean and SD of the truncated distribution.
        return new String[] {null, null, "SupportLowerBound", "SupportUpperBound"};
    }

    /**
     * Hit the edge cases where the lower and upper bound are not infinite but the
     * CDF of the parent distribution is either 0 or 1. This is effectively no truncation.
     * Big finite bounds should be handled as if infinite when computing the moments.
     *
     * @param mean Mean for the parent distribution.
     * @param sd Standard deviation for the parent distribution.
     * @param lower Lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper Upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     */
    @ParameterizedTest
    @CsvSource({
        "0.0, 1.0, -4, 6",
        "1.0, 2.0, -4, 6",
        "3.45, 6.78, -8, 10",
    })
    void testEffectivelyNoTruncation(double mean, double sd, double lower, double upper) {
        double inf = Double.POSITIVE_INFINITY;
        double max = Double.MAX_VALUE;
        TruncatedNormalDistribution dist1;
        TruncatedNormalDistribution dist2;
        // truncation of upper tail
        dist1 = TruncatedNormalDistribution.of(mean, sd, -inf, upper);
        dist2 = TruncatedNormalDistribution.of(mean, sd, -max, upper);
        Assertions.assertEquals(dist1.getMean(), dist2.getMean(), "Mean");
        Assertions.assertEquals(dist1.getVariance(), dist2.getVariance(), "Variance");
        // truncation of lower tail
        dist1 = TruncatedNormalDistribution.of(mean, sd, lower, inf);
        dist2 = TruncatedNormalDistribution.of(mean, sd, lower, max);
        Assertions.assertEquals(dist1.getMean(), dist2.getMean(), "Mean");
        Assertions.assertEquals(dist1.getVariance(), dist2.getVariance(), "Variance");
        // no truncation
        dist1 = TruncatedNormalDistribution.of(mean, sd, -inf, inf);
        dist2 = TruncatedNormalDistribution.of(mean, sd, -max, max);
        Assertions.assertEquals(dist1.getMean(), dist2.getMean(), "Mean");
        Assertions.assertEquals(dist1.getVariance(), dist2.getVariance(), "Variance");
    }
}
