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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link TDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class TDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double df = (Double) parameters[0];
        return TDistribution.of(df);
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
        return 1e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @MethodSource
    void testAdditionalMoments(double df, double mean, double variance) {
        final TDistribution dist = TDistribution.of(df);
        testMoments(dist, mean, variance, DoubleTolerances.equals());
    }

    static Stream<Arguments> testAdditionalMoments() {
        return Stream.of(
            Arguments.of(1.5, 0, Double.POSITIVE_INFINITY),
            Arguments.of(2.1, 0, 2.1 / (2.1 - 2.0)),
            Arguments.of(12.1, 0, 12.1 / (12.1 - 2.0))
        );
    }

    /**
     * @see <a href="https://issues.apache.orgg/bugzilla/show_bug.cgi?id=27243">
     *      Bug report that prompted this unit test.</a>
     */
    @Test
    void testCumulativeProbabilityAgainstStackOverflow() {
        final TDistribution td = TDistribution.of(5.);
        Assertions.assertDoesNotThrow(() -> {
            td.cumulativeProbability(.1);
            td.cumulativeProbability(.01);
        });
    }

    /*
     * Adding this test to benchmark against tables published by NIST
     * http://itl.nist.gov/div898/handbook/eda/section3/eda3672.htm
     * Have chosen tabulated results for degrees of freedom 2,10,30,100
     * Have chosen problevels from 0.10 to 0.001
     */
    @Test
    void nistData() {
        final double[] prob = new double[]{0.10, 0.05, 0.025, 0.01, 0.005, 0.001};
        final double[] args2 = new double[]{1.886, 2.920, 4.303, 6.965, 9.925, 22.327};
        final double[] args10 = new double[]{1.372, 1.812, 2.228, 2.764, 3.169, 4.143};
        final double[] args30 = new double[]{1.310, 1.697, 2.042, 2.457, 2.750, 3.385};
        final double[] args100 = new double[]{1.290, 1.660, 1.984, 2.364, 2.626, 3.174};
        // Data points are not very exact so use a low tolerance.
        final DoubleTolerance tolerance = DoubleTolerances.absolute(1e-4);
        testSurvivalProbability(TDistribution.of(2), args2, prob, tolerance);
        testSurvivalProbability(TDistribution.of(10), args10, prob, tolerance);
        testSurvivalProbability(TDistribution.of(30), args30, prob, tolerance);
        testSurvivalProbability(TDistribution.of(100), args100, prob, tolerance);
    }

    // See https://issues.apache.org/jira/browse/STATISTICS-25
    @ParameterizedTest
    @CsvSource({
        // Data from r stats TDist
        "1.00E+00, 0.025, 0.31811106676706130125, 0.50795608991202578775",
        "1.00E+01, 0.025, 0.38897465512398698984, 0.50972659510159001872",
        "1.00E+02, 0.025, 0.39782060538246560855, 0.50994760809308248284",
        "1.00E+03, 0.025, 0.39871781392704330749, 0.50997002433945715083",
        "1.00E+04, 0.025, 0.39880764764142323520, 0.50997226878155033081",
        "1.00E+05, 0.025, 0.39881663212763956983, 0.50997249325358851024",
        "1.00E+06, 0.025, 0.39881753058739516371, 0.50997251570107027252",
        "2.00E+06, 0.025, 0.39881758050188537146, 0.50997251694815404210",
        "2.98E+06, 0.025, 0.39881759691671903045, 0.50997251735826898411",
        "2.99E+06, 0.025, 0.39881759702875807516, 0.50997251736106818942",
        "3.00E+06, 0.025, 0.39881759714005016182, 0.50997251736384874299",
        "4.00E+06, 0.025, 0.39881760545913280680, 0.50997251757169603792",
        "1.00E+07, 0.025, 0.39881762043348206737, 0.50997251794582121320",
        "1.00E+08, 0.025, 0.39881762941809190126, 0.50997251817029631837",
        "1.00E+09, 0.025, 0.39881763031655281804, 0.50997251819274391771",
        "1.00E+10, 0.025, 0.39881763040639894857, 0.50997251819498856662",
        "1.00E+11, 0.025, 0.39881763041538353942, 0.50997251819521305372",
        "1.00E+12, 0.025, 0.39881763041628198740, 0.50997251819523548022",
        "1.00E+13, 0.025, 0.39881763041637185996, 0.50997251819523781169",
        "1.00E+14, 0.025, 0.39881763041638085276, 0.50997251819523803373",
        "1.00E+15, 0.025, 0.39881763041638174094, 0.50997251819523803373",
        "1.00E+16, 0.025, 0.39881763041638179645, 0.50997251819523803373",
        "1.00E+17, 0.025, 0.39881763041638179645, 0.50997251819523803373",
        "1.00E+18, 0.025, 0.39881763041638179645, 0.50997251819523803373",
    })
    void testStatistics25(double df, double x, double pdf, double cdf) {
        final TDistribution dist = TDistribution.of(df);

        final double density = dist.density(x);
        Assertions.assertEquals(pdf, density, 4 * Math.ulp(pdf),
            () -> "pdf error: " + (Double.doubleToLongBits(pdf) - Double.doubleToRawLongBits(density)));

        final double p = dist.cumulativeProbability(x);
        Assertions.assertEquals(cdf, p, 6 * Math.ulp(cdf),
            () -> "cdf error: " + (Double.doubleToLongBits(cdf) - Double.doubleToRawLongBits(p)));
    }
}
