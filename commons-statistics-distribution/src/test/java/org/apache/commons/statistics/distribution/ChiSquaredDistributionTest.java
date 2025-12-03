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
 * Test cases for {@link ChiSquaredDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class ChiSquaredDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double df = (Double) parameters[0];
        return ChiSquaredDistribution.of(df);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0},
            {-0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"DegreesOfFreedom"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalDensity(double df, double[] points, double[] values, DoubleTolerance tol) {
        testDensity(ChiSquaredDistribution.of(df), points, values, tol);
    }

    static Stream<Arguments> testAdditionalDensity() {
        // Values have many digits above the decimal point so use relative tolerance
        final DoubleTolerance tol = DoubleTolerances.relative(5e-14);

        // R 2.5:
        // x <- c(-0.1, 1e-6, 0.5, 1, 2, 5)
        final double[] x = new double[]{-0.1, 1e-6, 0.5, 1, 2, 5};
        return Stream.of(
            // print(dchisq(x, df=1), digits=17)
            Arguments.of(1, x, new double[] {
                0, 398.942080930342626743, 0.439391289467722435, 0.241970724519143365,
                0.103776874355148693, 0.014644982561926489}, tol),
            // print(dchisq(x, df=0.1), digits=17)
            Arguments.of(0.1, x, new double[] {
                0, 2.4864539972849805e+04, 7.4642387316120481e-02,
                3.0090777182393683e-02, 9.4472991589506262e-03, 8.8271993957607896e-04}, tol),
            // print(dchisq(x, df=2), digits=17)
            Arguments.of(2, x, new double[] {
                0, 0.49999975000006253, 0.38940039153570244,
                0.30326532985631671, 0.18393972058572117, 0.04104249931194940}, tol),
            // print(dchisq(x, df=10), digits=17)
            Arguments.of(10, x, new double[] {
                0, 1.3020826822918329e-27, 6.3378969976514082e-05,
                7.8975346316749191e-04, 7.6641550244050524e-03, 6.6800942890542614e-02}, tol),
            // print(dchisq(x, df=100), digits=17)
            Arguments.of(100, x, new double[] {
                0, 0.0000000000000000e+00, 2.0200026568141969e-93,
                8.8562141121618944e-79, 3.0239224849774644e-64, 2.1290671364111626e-45}, tol),

            // Progressively larger degrees of freedom (df) with values around the mean (df).
            // Note that the CDF tends towards a step function (0 to 1) around the mean, the
            // density is tiny and the computation has large cancellation leading to inaccuracy.
            // Note that R's dchisq and matlab's chi2pdf computations are close and have a
            // similar error to the scipy result as the current java code.

            // scipy.stats 1.9.1
            // chi2.pdf([250, 500, 1000, 1500, 2000, 2500], 1000)
            Arguments.of(1000,
                new double[] {250, 500, 1000, 1500, 2000, 2500},
                new double[] {
                    2.4144472784936886e-140, 2.0416219443308211e-044,
                    8.9191339347531283e-003, 1.7629519620803219e-023,
                    1.0400388688836408e-069, 6.3317766286480137e-130
                }, DoubleTolerances.relative(5e-13)),
            // chi2.pdf([7000, 8000, 9000, 10000, 11000, 12000, 14000], 10000)
            Arguments.of(10000,
                new double[] {7000, 8000, 9000, 10000, 11000, 12000, 14000},
                new double[] {
                    3.4451863344803051e-126, 1.9575583029092260e-053,
                    7.1768433769351565e-015, 2.8209009023369056e-003,
                    1.6794999068429000e-013, 9.6151246453108889e-042,
                    2.2671193242727422e-141
                }, DoubleTolerances.relative(5e-12)),
            // chi2.pdf([90000, 93000, 97000, 100000, 103000, 106000, 110000], 100000)
            Arguments.of(100000,
                new double[] {90000, 93000, 97000, 100000, 103000, 106000, 110000},
                new double[] {
                    3.9267505859463047e-120, 1.4455254881569810e-059,
                    9.8188082087821045e-014, 8.9206057128873189e-004,
                    2.2754616642908043e-013, 2.1622505071910235e-041,
                    1.1771994592645341e-105
                }, DoubleTolerances.relative(2e-10)),
            // chi2.pdf([970000, 980000, 990000, 1000000, 1010000, 1020000, 1030000], 1000000)
            Arguments.of(1000000,
                new double[] {970000, 980000, 990000, 1000000, 1010000, 1020000, 1030000},
                new double[] {
                    5.5973818150499247e-104, 2.7658855125007757e-048,
                    3.3455543700508753e-015, 2.8209474455352760e-004,
                    4.5767314194128692e-015, 3.8269849102866545e-047,
                    4.2922183625107689e-100
                }, DoubleTolerances.relative(1e-9)),
            // chi2.pdf([9889000, 9890000, 9900000, 10000000, 10100000, 10150000], 10000000)
            Arguments.of(10000000,
                new double[] {9889000, 9890000, 9900000, 10000000, 10100000, 10150000},
                new double[] {
                    1.5256172522921843e-139, 4.0708897874504053e-137,
                    4.4858296398343578e-114, 8.9206205026501391e-005,
                    1.2327974895005824e-112, 1.1722829145179983e-246
                }, DoubleTolerances.relative(2e-8)),
            // chi2.pdf([1e9-1e7, 1e9-1e6-1e5, 1e9-1e6-1e3, 1e9-1e6, 1e9, 1e9+1e6, 1e9+1e6+1e3, 1e9+1e6+1e5, 1e9+1e7], 1e9)
            Arguments.of(1e9,
                new double[] {1e9 - 1e7, 1e9 - 1e6 - 1e5, 1e9 - 1e6 - 1e3, 1e9 - 1e6, 1e9,
                    1e9 + 1e6, 1e9 + 1e6 + 1e3, 1e9 + 1e6 + 1e5, 1e9 + 1e7},
                new double[] {
                    0.0000000000000000e+000, 3.0225312085986285e-137,
                    1.2226420014038719e-114, 2.0173074570506826e-114,
                    8.9206087939511128e-006, 2.8097537346819163e-114,
                    1.7046276559389835e-114, 4.7000061695431404e-137,
                    0.0000000000000000e+000
                }, DoubleTolerances.relative(3e-6))
        );
    }
}
