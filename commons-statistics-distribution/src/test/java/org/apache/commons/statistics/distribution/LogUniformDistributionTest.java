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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link LogUniformDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class LogUniformDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double a = (Double) parameters[0];
        final double b = (Double) parameters[1];
        return LogUniformDistribution.of(a, b);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            // lower >= upper
            {0.0, 0.0},
            {1.0, 0.5},
            // Range not finite
            {Double.NaN, 1.0},
            {0.5, Double.NaN},
            // lower <= 0
            {-1.0, 1.0},
            {0.0, 1.0},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"SupportLowerBound", "SupportUpperBound"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Test the moments using the canonical formulas (from the javadoc).
     */
    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(double a, double b) {
        final double diff = b - a;
        final double denom = Math.log(b / a);
        final double mean = diff / denom;
        // Note: b^2 - a^2 = (b-a)*(b+a)
        final double variance = mean * (b + a) / 2 - mean * mean;
        TestUtils.assertEquals(mean, LogUniformDistribution.of(a, b).getMean(),
            DoubleTolerances.relative(1e-14), "Mean");
        TestUtils.assertEquals(variance, LogUniformDistribution.of(a, b).getVariance(),
            DoubleTolerances.relative(1e-10), "Variance");
    }

    static Stream<Arguments> testAdditionalMoments() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final double a : new double[] {1, 10, 100}) {
            for (final double x : new double[] {10, 20, 40}) {
                builder.add(Arguments.of(a, a + x));
            }
        }
        return builder.build();
    }

    /**
     * Test the survival function for extreme values is computed using high precision,
     * i.e. is not the default of 1 - CDF.
     */
    @ParameterizedTest
    @CsvSource({
        "1e-100, 1e100",
        "1e-10, 1e10",
    })
    void testExtremeSurvivalFunction(double a, double b) {
        final LogUniformDistribution d = LogUniformDistribution.of(a, b);
        // Find a small non-zero survival probability
        final double u = Math.ulp(b);
        int i = 1;
        double p;
        do {
            p = d.survivalProbability(b - i * u);
            i *= 2;
        } while (p == 0);
        Assertions.assertTrue(p < 0x1.0p-53, "sf is not small enough for high precision");
        Assertions.assertNotEquals(1 - d.cumulativeProbability(b - i * u), p, "sf is not high precision: sf == 1 - cdf");
    }
}
