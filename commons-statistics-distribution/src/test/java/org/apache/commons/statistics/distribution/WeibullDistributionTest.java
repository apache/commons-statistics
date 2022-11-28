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
import org.apache.commons.numbers.gamma.LogGamma;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link WeibullDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class WeibullDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double shape = (Double) parameters[0];
        final double scale = (Double) parameters[1];
        return WeibullDistribution.of(shape, scale);
    }


    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 2.0},
            {-0.1, 2.0},
            {1.0, 0.0},
            {1.0, -0.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Shape", "Scale"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 1e-14;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(double shape, double scale, double mean, double variance) {
        final WeibullDistribution dist = WeibullDistribution.of(shape, scale);
        testMoments(dist, mean, variance, createRelTolerance(1e-15));
    }

    static Stream<Arguments> testAdditionalMoments() {
        // In R: 3.5*gamma(1+(1/2.5)) (or empirically: mean(rweibull(10000, 2.5, 3.5)))
        double mu1 = 3.5 * Math.exp(LogGamma.value(1 + (1 / 2.5)));
        double mu2 = 2.222 * Math.exp(LogGamma.value(1 + (1 / 10.4)));
        return Stream.of(
            Arguments.of(2.5, 3.5, mu1,
                (3.5 * 3.5) *
                Math.exp(LogGamma.value(1 + (2 / 2.5))) -
                (mu1 * mu1)),
            Arguments.of(10.4, 2.222, mu2,
                (2.222 * 2.222) *
                Math.exp(LogGamma.value(1 + (2 / 10.4))) -
                (mu2 * mu2))
        );
    }

    @ParameterizedTest
    @CsvSource({
        "1, 2",
        "0.1, 2.34",
    })
    void testAdditionalParameterAccessors(double shape, double scale) {
        final WeibullDistribution dist = WeibullDistribution.of(shape, scale);
        Assertions.assertEquals(shape, dist.getShape());
        Assertions.assertEquals(scale, dist.getScale());
    }

    @Test
    void testInverseCumulativeProbabilitySmallPAccuracy() {
        final WeibullDistribution dist = WeibullDistribution.of(2, 3);
        final double t = dist.inverseCumulativeProbability(1e-17);
        // Analytically, answer is solution to 1e-17 = 1-exp(-(x/3)^2)
        // x = sqrt(-9*log(1-1e-17))
        // If we're not careful, answer will be 0. Answer below is computed with care in Octave:
        Assertions.assertEquals(9.48683298050514e-9, t, 1e-17);
    }
}
