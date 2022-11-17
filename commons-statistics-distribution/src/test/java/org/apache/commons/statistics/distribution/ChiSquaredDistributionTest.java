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
    void testAdditionalDensity(double df, double[] points, double[] values) {
        // Values have many digits above the decimal point so use relative tolerance
        final DoubleTolerance tol = createRelTolerance(5e-14);
        testDensity(ChiSquaredDistribution.of(df), points, values, tol);
    }

    static Stream<Arguments> testAdditionalDensity() {
        // R 2.5:
        // x <- c(-0.1, 1e-6, 0.5, 1, 2, 5)
        final double[] x = new double[]{-0.1, 1e-6, 0.5, 1, 2, 5};
        return Stream.of(
            // print(dchisq(x, df=1), digits=17)
            Arguments.of(1, x, new double[] {
                0, 398.942080930342626743, 0.439391289467722435, 0.241970724519143365,
                0.103776874355148693, 0.014644982561926489}),
            // print(dchisq(x, df=0.1), digits=17)
            Arguments.of(0.1, x, new double[] {
                0, 2.4864539972849805e+04, 7.4642387316120481e-02,
                3.0090777182393683e-02, 9.4472991589506262e-03, 8.8271993957607896e-04}),
            // print(dchisq(x, df=2), digits=17)
            Arguments.of(2, x, new double[] {
                0, 0.49999975000006253, 0.38940039153570244,
                0.30326532985631671, 0.18393972058572117, 0.04104249931194940}),
            // print(dchisq(x, df=10), digits=17)
            Arguments.of(10, x, new double[] {
                0, 1.3020826822918329e-27, 6.3378969976514082e-05,
                7.8975346316749191e-04, 7.6641550244050524e-03, 6.6800942890542614e-02}),
            // print(dchisq(x, df=100), digits=17)
            Arguments.of(100, x, new double[] {
                0, 0.0000000000000000e+00, 2.0200026568141969e-93,
                8.8562141121618944e-79, 3.0239224849774644e-64, 2.1290671364111626e-45})

            // TODO:
            // Add more density checks with large DF and x points around the mean
            // and into overflow for the underlying Gamma distribution.
        );
    }
}
