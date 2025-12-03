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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link PascalDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class PascalDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int r = (Integer) parameters[0];
        final double p = (Double) parameters[1];
        return PascalDistribution.of(r, p);
    }


    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0, 0.5},
            {-1, 0.5},
            {3, -0.1},
            {3, 0.0},
            {3, 1.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"NumberOfSuccesses", "ProbabilityOfSuccess"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(int r, double p, double mean, double variance) {
        final PascalDistribution dist = PascalDistribution.of(r, p);
        testMoments(dist, mean, variance, DoubleTolerances.ulps(1));
    }

    static Stream<Arguments> testAdditionalMoments() {
        return Stream.of(
            Arguments.of(10, 0.5, (10d * 0.5d) / 0.5, (10d * 0.5d) / (0.5d * 0.5d)),
            Arguments.of(25, 0.7, (25d * 0.3d) / 0.7, (25d * 0.3d) / (0.7d * 0.7d))
        );
    }
}
