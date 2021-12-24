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
 * Test cases for {@link NakagamiDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class NakagamiDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mu = (Double) parameters[0];
        final double omega = (Double) parameters[1];
        return NakagamiDistribution.of(mu, omega);
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 1.0},
            {-0.1, 1.0},
            {0.5, 0.0},
            {0.5, -0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Shape", "Scale"};
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Test additional moments.
     * Includes cases where {@code gamma(mu + 0.5) / gamma(mu)} is not computable
     * directly due to overflow of the gamma function.
     */
    @ParameterizedTest
    @CsvSource({
        // Generated using matlab
        "175, 0.75, 0.86540703592357171, 0.0010706621739778321",
        "175, 1, 0.99928597029814059, 0.0014275495653037762",
        "175, 1.25, 1.1172356792742391, 0.0017844369566297202",
        "175, 3.75, 1.9351089605317091, 0.0053533108698891607",
        "205.25, 0.75, 0.86549814380218737, 0.00091296307496802065",
        "205.25, 1, 0.99939117261462862, 0.0012172840999573609",
        "205.25, 1.25, 1.1173532990397681, 0.0015216051249467011",
        "205.25, 3.75, 1.9353126839415795, 0.0045648153748401032",
        "305.25, 0.75, 0.865670838787722, 0.00061399887256183283",
        "305.25, 1.75, 1.32233404855355, 0.0014326640359776099",
        "305.25, 3.75, 1.9356988416686078, 0.0030699943628091642",
        "305.25, 12.75, 3.5692523053388152, 0.010437980833551158",
        "305.25, 25.25, 5.0228805186490098, 0.020671295376248372",
    })
    void testAdditionalMoments(double mu, double omega, double mean, double variance) {
        // Note:
        // The relative error of the variance is much greater than the mean.
        // Accuracy of Matlab data requires verification with another source.
        // Use a moderate threshold.
        final DoubleTolerance tolerance = DoubleTolerances.relative(2e-10);
        final NakagamiDistribution dist = NakagamiDistribution.of(mu, omega);
        testMoments(dist, mean, variance, tolerance);
    }

    @Test
    void testExtremeLogDensity() {
        // XXX: Verify with more test data from a reference distribution
        final NakagamiDistribution dist = NakagamiDistribution.of(0.5, 1);
        final double x = 50;
        Assertions.assertEquals(0.0, dist.density(x));
        Assertions.assertEquals(-1250.22579, dist.logDensity(x), 1e-4);
    }
}
