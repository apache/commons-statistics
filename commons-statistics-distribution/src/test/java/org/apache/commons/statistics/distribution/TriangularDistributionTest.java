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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for {@link TriangularDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class TriangularDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double lower = (Double) parameters[0];
        final double mode = (Double) parameters[1];
        final double upper = (Double) parameters[2];
        return TriangularDistribution.of(lower, mode, upper);
    }


    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0, 0.0},
            // 1.0, 2.0, 3 is OK - move points to incorrect locations
            {4.0, 2.0, 3.0},
            {3.0, 2.0, 3.0},
            {2.5, 2.0, 3.0},
            {1.0, 0.0, 3.0},
            {1.0, 4.0, 3.0},
            {1.0, 2.0, -1.0},
            {1.0, 2.0, 1.5},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"SupportLowerBound", "Mode", "SupportUpperBound"};
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @CsvSource({
        "1, 2, 3",
        "0.12, 3.45, 12.56",
    })
    void testAdditionalParameterAccessors(double lower, double mode, double upper) {
        final TriangularDistribution dist = TriangularDistribution.of(lower, mode, upper);
        Assertions.assertEquals(lower, dist.getSupportLowerBound());
        Assertions.assertEquals(mode, dist.getMode());
        Assertions.assertEquals(upper, dist.getSupportUpperBound());
    }

    @Test
    void testAdditionalMoments() {
        TriangularDistribution dist;

        dist = TriangularDistribution.of(0, 0.5, 1.0);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(1 / 24.0, dist.getVariance());

        dist = TriangularDistribution.of(0, 1, 1);
        Assertions.assertEquals(2 / 3.0, dist.getMean());
        Assertions.assertEquals(1 / 18.0, dist.getVariance());

        dist = TriangularDistribution.of(-3, 2, 12);
        Assertions.assertEquals(3 + (2 / 3.0), dist.getMean());
        Assertions.assertEquals(175 / 18.0, dist.getVariance());
    }
}
