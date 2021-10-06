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
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link UniformContinuousDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class UniformContinuousDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double lower = (Double) parameters[0];
        final double upper = (Double) parameters[1];
        return new UniformContinuousDistribution(lower, upper);
    }

    @Override
    protected double getRelativeTolerance() {
        return 1e-15;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0},
            {1.0, 0.0},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"SupportLowerBound", "SupportUpperBound"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testAdditionalMoments() {
        UniformContinuousDistribution dist;

        dist = new UniformContinuousDistribution(0, 1);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(1 / 12.0, dist.getVariance());

        dist = new UniformContinuousDistribution(-1.5, 0.6);
        Assertions.assertEquals(-0.45, dist.getMean());
        Assertions.assertEquals(0.3675, dist.getVariance());
    }

    /**
     * Check accuracy of analytical inverse CDF. Fails if a solver is used
     * with the default accuracy.
     */
    @Test
    void testInverseCumulativeDistribution() {
        final double upper = 1e-9;
        final double tiny = 0x1.0p-100;

        final UniformContinuousDistribution dist = new UniformContinuousDistribution(0, upper);
        Assertions.assertEquals(2.5e-10, dist.inverseCumulativeProbability(0.25));
        Assertions.assertEquals(tiny * upper, dist.inverseCumulativeProbability(tiny));

        final UniformContinuousDistribution dist2 = new UniformContinuousDistribution(-upper, 0);
        // This is inexact
        Assertions.assertEquals(-7.5e-10, dist2.inverseCumulativeProbability(0.25), Math.ulp(-7.5e-10));
        Assertions.assertEquals(-upper + tiny * upper, dist2.inverseCumulativeProbability(tiny));
    }
}
