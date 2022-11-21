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
        //   variance = omega - mean^2; omega > 0; x > 0; mean > 0
        // This computation is subject to cancellation due to subtraction of two large
        // values to approach a result of zero.
        // Use a moderate threshold.
        final DoubleTolerance tolerance = createRelTolerance(2e-10);
        final NakagamiDistribution dist = NakagamiDistribution.of(mu, omega);
        testMoments(dist, mean, variance, tolerance);
    }

    /**
     * Repeat test of additional moments with alternative source for the expected result.
     */
    @ParameterizedTest
    @CsvSource({
        // Generated using 128-bit quad precision implementation using Boost C++:
        // #include <boost/multiprecision/float128.hpp>
        // #include <boost/math/special_functions/gamma.hpp>
        // #define quad boost::multiprecision::float128
        // T v = boost::math::tgamma_delta_ratio(mu, T(0.5));
        // T mean = sqrt(omega / mu) / v;
        // T var = omega - (omega / mu) / v / v;
        "175, 0.75, 0.865407035923572335404337637742305354, 0.00107066217397678136642741884083229635",
        "175, 1, 0.999285970298141244170512691211913862, 0.0014275495653023751552365584544430618",
        "175, 1.25, 1.11723567927423980521693795242933784, 0.00178443695662796894404569806805382725",
        "175, 3.75, 1.93510896053171023839534780723184735, 0.00535331086988390683213709420416109656",
        "205.25, 0.75, 0.865498143802251959479795150977083271, 0.000912963074856388060643895128688537674",
        "205.25, 1, 0.999391172614703197622376095323984551, 0.0012172840998085174141918601715848132",
        "205.25, 1.25, 1.11735329903985129515900415713529348, 0.00152160512476064676773982521448079983",
        "205.25, 3.75, 1.93531268394172368161190235734322469, 0.00456481537428194030321947564344316985",
        "305.25, 0.75, 0.865670838787713729127832304174216151, 0.000613998872576147383115881187594898943",
        "305.25, 1.75, 1.32233404855353739372707758901129787, 0.00143266403601101056060372277105460371",
        "305.25, 3.75, 1.93569884166858953645398412102636382, 0.00306999436288073691557940593797382064",
        "305.25, 12.75, 3.56925230533878138370667203279492999, 0.010437980833794505512969980189112608",
        "305.25, 25.25, 5.02288051864896241877391197174369638, 0.0206712953767302952315679999823609879",
    })
    void testAdditionalMoments2(double mu, double omega, double mean, double variance) {
        // The mean is within 2 ULP.
        // The variance is closer than the matlab result but the effect of cancellation
        // prevents high accuracy.
        final DoubleTolerance tolerance = createRelTolerance(1e-12);
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
