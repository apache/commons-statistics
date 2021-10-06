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
 * Test cases for {@link UniformDiscreteDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class UniformDiscreteDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int lower = (Integer) parameters[0];
        final int upper = (Integer) parameters[1];
        return new UniformDiscreteDistribution(lower, upper);
    }

    @Override
    protected double getAbsoluteTolerance() {
        return 1e-12;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            // MATH-1141
            {1, 0},
            {3, 2},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"SupportLowerBound", "SupportUpperBound"};
    }

    //-------------------- Additional test cases -------------------------------

    /** Test mean/variance. */
    @Test
    void testAdditionalMoments() {
        UniformDiscreteDistribution dist;

        dist = new UniformDiscreteDistribution(0, 5);
        Assertions.assertEquals(2.5, dist.getMean());
        Assertions.assertEquals(35 / 12.0, dist.getVariance());

        dist = new UniformDiscreteDistribution(0, 1);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(3 / 12.0, dist.getVariance());
    }

    // MATH-1396
    @Test
    void testLargeRangeSubtractionOverflow() {
        final int hi = Integer.MAX_VALUE / 2 + 10;
        final int lower = -hi;
        final int upper = hi - 1;

        // range = upper - lower + 1 would overflow
        Assertions.assertTrue(upper - lower < 0);

        final UniformDiscreteDistribution dist = new UniformDiscreteDistribution(lower, upper);

        Assertions.assertEquals(0.5 / hi, dist.probability(123456));
        Assertions.assertEquals(0.5, dist.cumulativeProbability(-1));

        Assertions.assertEquals((Math.pow(2d * hi, 2) - 1) / 12, dist.getVariance());
    }

    // MATH-1396
    @Test
    void testLargeRangeAdditionOverflow() {
        final int hi = Integer.MAX_VALUE / 2 + 10;
        final int lower = hi - 1;
        final int upper = hi + 1;

        // mean = (lower + upper) / 2 would overflow
        Assertions.assertTrue(lower + upper < 0);

        final UniformDiscreteDistribution dist = new UniformDiscreteDistribution(lower, upper);

        Assertions.assertEquals(1d / 3d, dist.probability(hi));
        Assertions.assertEquals(2d / 3d, dist.cumulativeProbability(hi));

        Assertions.assertEquals(hi, dist.getMean());
    }
}
