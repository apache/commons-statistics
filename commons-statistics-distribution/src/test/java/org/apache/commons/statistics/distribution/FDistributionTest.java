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
 * Test cases for {@link FDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class FDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double df1 = (Double) parameters[0];
        final double df2 = (Double) parameters[1];
        return FDistribution.of(df1, df2);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 1.0},
            {-0.1, 1.0},
            {1.0, 0.0},
            {1.0, -0.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"NumeratorDegreesOfFreedom", "DenominatorDegreesOfFreedom"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testAdditionalMoments() {
        FDistribution dist;

        dist = FDistribution.of(1, 2);
        Assertions.assertEquals(Double.NaN, dist.getMean());
        Assertions.assertEquals(Double.NaN, dist.getVariance());

        dist = FDistribution.of(1, 3);
        Assertions.assertEquals(3d / (3d - 2d), dist.getMean());
        Assertions.assertEquals(Double.NaN, dist.getVariance());

        dist = FDistribution.of(1, 5);
        Assertions.assertEquals(5d / (5d - 2d), dist.getMean());
        Assertions.assertEquals((2d * 5d * 5d * 4d) / 9d, dist.getVariance());
    }

    @Test
    void testLargeDegreesOfFreedom() {
        final double x0 = 0.999;
        final FDistribution fd = FDistribution.of(100000, 100000);
        final double p = fd.cumulativeProbability(x0);
        final double x = fd.inverseCumulativeProbability(p);
        Assertions.assertEquals(x0, x, 1.0e-5);
    }

    @Test
    void testSmallDegreesOfFreedom() {
        final double x0 = 0.975;
        FDistribution fd = FDistribution.of(1, 1);
        double p = fd.cumulativeProbability(x0);
        double x = fd.inverseCumulativeProbability(p);
        Assertions.assertEquals(x0, x, 1.0e-5);

        fd = FDistribution.of(1, 2);
        p = fd.cumulativeProbability(x0);
        x = fd.inverseCumulativeProbability(p);
        Assertions.assertEquals(x0, x, 1.0e-5);
    }

    @Test
    void testMath785() {
        // this test was failing due to inaccurate results from ContinuedFraction.
        final double prob = 0.01;
        final FDistribution f = FDistribution.of(200000, 200000);
        final double result = f.inverseCumulativeProbability(prob);
        Assertions.assertTrue(result < 1.0, "Failing to calculate inverse cumulative probability");
    }
}
