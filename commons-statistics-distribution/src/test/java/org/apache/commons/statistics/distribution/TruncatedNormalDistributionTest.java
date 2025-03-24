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

import org.apache.commons.numbers.gamma.Erf;
import org.apache.commons.numbers.gamma.Erfcx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test class for {@link TruncatedNormalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 * All test values were computed using Python with SciPy v1.6.0.
 */
class TruncatedNormalDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mean = (Double) parameters[0];
        final double sd = (Double) parameters[1];
        final double upper = (Double) parameters[2];
        final double lower = (Double) parameters[3];
        return TruncatedNormalDistribution.of(mean, sd, upper, lower);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0, -1.0, 1.0},
            {0.0, -0.1, -1.0, 1.0},
            {0.0, 1.0, 1.0, -1.0},
            // No usable probability range
            {0.0, 1.0, 100.0, 101.0},
        };
    }

    @Override
    String[] getParameterNames() {
        // Input mean and standard deviation refer to the underlying normal distribution.
        // The constructor arguments do not match the mean and SD of the truncated distribution.
        return new String[] {null, null, "SupportLowerBound", "SupportUpperBound"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 1e-14;
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Hit the edge cases where the lower and upper bound are not infinite but the
     * CDF of the parent distribution is either 0 or 1. This is effectively no truncation.
     * Big finite bounds should be handled as if infinite when computing the moments.
     *
     * @param mean Mean for the parent distribution.
     * @param sd Standard deviation for the parent distribution.
     * @param lower Lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper Upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     */
    @ParameterizedTest
    @CsvSource({
        "0.0, 1.0, -4, 6",
        "1.0, 2.0, -4, 6",
        "3.45, 6.78, -8, 10",
    })
    void testMomentsEffectivelyNoTruncation(double mean, double sd, double lower, double upper) {
        double inf = Double.POSITIVE_INFINITY;
        double max = Double.MAX_VALUE;
        TruncatedNormalDistribution dist1;
        TruncatedNormalDistribution dist2;
        // truncation of upper tail
        dist1 = TruncatedNormalDistribution.of(mean, sd, -inf, upper);
        dist2 = TruncatedNormalDistribution.of(mean, sd, -max, upper);
        Assertions.assertEquals(dist1.getMean(), dist2.getMean(), "Mean");
        Assertions.assertEquals(dist1.getVariance(), dist2.getVariance(), "Variance");
        // truncation of lower tail
        dist1 = TruncatedNormalDistribution.of(mean, sd, lower, inf);
        dist2 = TruncatedNormalDistribution.of(mean, sd, lower, max);
        Assertions.assertEquals(dist1.getMean(), dist2.getMean(), "Mean");
        Assertions.assertEquals(dist1.getVariance(), dist2.getVariance(), "Variance");
        // no truncation
        dist1 = TruncatedNormalDistribution.of(mean, sd, -inf, inf);
        dist2 = TruncatedNormalDistribution.of(mean, sd, -max, max);
        Assertions.assertEquals(dist1.getMean(), dist2.getMean(), "Mean");
        Assertions.assertEquals(dist1.getVariance(), dist2.getVariance(), "Variance");
    }

    /**
     * Test mean cases adapted from the source implementation for the truncated
     * normal moments.
     *
     * @see <a href="https://github.com/cossio/TruncatedNormal.jl/blob/master/test/tnmom1.jl">
     * cossio TruncatedNormal moment1 tests</a>
     */
    @Test
    void testMean() {
        assertMean(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0);
        assertMean(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0);
        assertMean(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0);
        assertMean(0, Double.POSITIVE_INFINITY, Math.sqrt(2 / Math.PI), 1e-15);
        assertMean(Double.NEGATIVE_INFINITY, 0, -Math.sqrt(2 / Math.PI), 1e-15);

        for (int x = -10; x <= 10; x++) {
            final double expected = Math.sqrt(2 / Math.PI) / Erfcx.value(x / Math.sqrt(2));
            assertMean(x, Double.POSITIVE_INFINITY, expected, 1e-15);
        }

        for (int i = -100; i <= 100; i++) {
            final double x = Math.exp(i);
            assertMean(-x, x, 0, 0);
            final double expected = -Math.sqrt(2 / Math.PI) * Math.expm1(-x * x / 2) / Erf.value(x / Math.sqrt(2));
            assertMean(0, x, expected, 1e-15);
        }

        assertMean(1e-44, 1e-43, 5.4999999999999999999999999999999999999999e-44, 1e-15);

        assertMean(100, 115, 100.00999800099926070518490239457545847490332879043, 1e-15);
        assertMean(-1e6, -999000, -999000.00000100100100099899498898098, 1e-15);
        assertMean(+1e6, Double.POSITIVE_INFINITY, +1.00000000000099999999999800000e6, 1e-15);
        assertMean(Double.NEGATIVE_INFINITY, -1e6, -1.00000000000099999999999800000e6, 1e-15);

        assertMean(-1e200, 1e200, 0, 1e-15);
        assertMean(0, +1e200, +0.797884560802865355879892119869, 1e-15);
        assertMean(-1e200, 0, -0.797884560802865355879892119869, 1e-15);

        assertMean(50, 70, -2, 3, 50.171943499898757645751683644632860837133138152489, 1e-15);
        assertMean(-100.0, 0.0, 0.0, 2.0986317998643735, -1.6744659119217125058885983754999713622460154892645, 1e-15);
        assertMean(0.0, 0.9, 0.0, 0.07132755843183151, 0.056911157632522598806524588414964004271754161737065, 1e-15);
        assertMean(-100.0, 100.0, 0.0, 17.185261847875548, 0, 1e-15);
        assertMean(-100.0, 0.5, 0.0, 0.47383322897860064, -0.1267981330521791493635176736743283314399, 1e-15);
        assertMean(-100.0, 100.0, 0.0, 17.185261847875548, 0, 1e-15);

        for (int i = -10; i <= 10; i++) {
            final double a = Math.exp(i);
            for (int j = -10; j <= 10; j++) {
                final double b = Math.exp(j);
                if (a <= b) {
                    final double mean = TruncatedNormalDistribution.moment1(a, b);
                    Assertions.assertTrue(a <= mean && mean <= b);
                }
            }
        }

        // https://github.com/JuliaStats/Distributions.jl/issues/827, 1e-15);
        assertMean(0, 1000, 1000000, 1, 999.99999899899899900100501101901899090472046236710608108591983, 6e-14);
    }

    /**
     * Test variance cases adapted from the source implementation for the truncated
     * normal moments.
     *
     * @see <a href="https://github.com/cossio/TruncatedNormal.jl/blob/master/test/tnvar.jl">
     * cossio TruncatedNormal variance tests</a>
     */
    @Test
    void testVariance() {
        assertVariance(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1, 0);
        assertVariance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0);
        assertVariance(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0);
        assertVariance(0, Double.POSITIVE_INFINITY, 1 - 2 / Math.PI, 1e-15);
        assertVariance(Double.NEGATIVE_INFINITY, 0, 1 - 2 / Math.PI, 1e-15);

        for (int x = -10; x <= 10; x++) {
            final double expected = 1 + Math.sqrt(2 / Math.PI) * x / Erfcx.value(x / Math.sqrt(2)) -
                (2 / Math.PI) / Math.pow(Erfcx.value(x / Math.sqrt(2)), 2);
            assertVariance(x, Double.POSITIVE_INFINITY, expected, 1e-11);
        }

        assertVariance(50, 70, 0.0003990431868038995479099272265360593305365, 1e-9);

        assertVariance(50, 70, -2, 3, 0.029373438107168350377591231295634273607812172191712, 1e-11);
        assertVariance(-100.0, 0.0, 0.0, 2.0986317998643735, 1.6004193412141677189841357987638847137391508803335, 1e-15);
        assertVariance(0.0, 0.9, 0.0, 0.07132755843183151, 0.0018487407287725028827020557707636415445504260892486, 1e-15);
        assertVariance(-100.0, 100.0, 0.0, 17.185261847875548, 295.333163899557735486302841237124507431445, 1e-15);
        assertVariance(-100.0, 0.5, 0.0, 0.47383322897860064, 0.145041095812679283837328561547251019229612, 1e-15);
        assertVariance(-100.0, 100.0, 0.0, 17.185261847875548, 295.333163899557735486302841237124507431445, 1e-15);
        assertVariance(-10000, 10000, 0, 1, 1, 1e-15);

        // https://github.com/JuliaStats/Distributions.jl/issues/827
        Assertions.assertTrue(TruncatedNormalDistribution.variance(999000, 1e6) >= 0);
        Assertions.assertTrue(TruncatedNormalDistribution.variance(-1000000, 1000 - 1000000) >= 0);

        // These tests are marked as broken in the reference implementation.
        // They present extreme deviations of the truncation bounds from the mean.
        //assertVariance(1e6, Double.POSITIVE_INFINITY, 9.99999999994000000000050000000e-13, 1e-15);
        //assertVariance(999000, 1e6, 1.00200300399898194688784897455e-12, 1e-15);
        //assertVariance(-1e6, -999000, 1.00200300399898194688784897455e-12, 1e-15);
    }

    /**
     * Test cases for computation of the moments. This hits edge cases including truncations
     * too extreme to have a probability range for the distribution.
     * The test ensures that the moments are computable for parameterisations
     * where the bounds fall within +/- 40 standard deviations from the mean.
     *
     * <p>Test data generated using a 128-bit implementation of the method using GCC lib quadmath
     * and Boost C++ Error function routines adapted to compute erfcx. Data verified using
     * the Julia implementation:
     * <pre>
     * import Pkg
     * Pkg.add(url="https://github.com/cossio/TruncatedNormal.jl")
     * using TruncatedNormal
     *
     * tnmean(1.23, 4.56)  # 1.7122093853640246
     * tnvar(1.23, 4.56)   # 0.1739856461219162
     *
     * # Using BigFloat does not work on hard cases of the variance
     * tnvar(BigFloat(1.0), BigFloat(1.0000000000000002))
     * </pre>
     *
     * <p>Computation of the mean is stable. Computation of the variance is not accurate as it
     * approaches machine epsilon (2^-52). Using Julia's BigFloat support does not allow computation
     * of the difficult cases listed below for the variance.
     *
     * @param lower Lower bound (inclusive) of the distribution, can be {@link Double#NEGATIVE_INFINITY}.
     * @param upper Upper bound (inclusive) of the distribution, can be {@link Double#POSITIVE_INFINITY}.
     * @param mean Expected mean
     * @param variance Expected variance
     * @param meanRelativeError Relative error tolerance for the mean
     * @param varianceRelativeError Relative error tolerance for the variance
     * (if set to negative the variance is allowed to be within 1.5 * epsilon of zero)
     */
    @ParameterizedTest
    @CsvSource({
        // Equal bounds
        "1.23, 1.23, 1.23, 0, 0, 0",
        "1.23, 4.56, 1.7122093853640246, 0.1739856461219162, 1e-15, 5e-15",

        // Effectively no truncation
        "-55, 60, 0, 1, 0, 0",

        // Long tail
        "-100, 101, 1.3443134677817230433408433600205167e-2172, 1, 1e-15, 1e-15",
        "-40, 101, 1.46327025083830317873709720033828097e-348, 1, 1e-15, 1e-15",
        "-30, 101, 1.47364613487854751904949326604507453e-196, 1, 1e-15, 1e-15",
        "-20, 101, 5.52094836215976318958273568278700042e-88, 1, 1e-15, 1e-15",
        "-10, 101, 7.69459862670641934633909221175249367e-23, 0.999999999999999999999230540137329438, 1e-15, 1e-15",
        "-5, 101, 1.48671994090490571244174411946057083e-06, 0.999992566398085139288753504945569711, 1e-15, 1e-15",
        "-1, 101, 0.287599970939178361228670127385217202, 0.629686285776605400861244494862843017, 1e-15, 1e-15",
        "0, 101, 0.797884560802865355879892119868763748, 0.363380227632418656924464946509942526, 1e-15, 1e-15",
        "1, 101, 1.52513527616098120908909053639057876, 0.199097665570348791553367979096726767, 1e-15, 1e-14",
        "5, 101, 5.18650396712584211561650896200523673, 0.032696434617112225345315807700917674, 1e-15, 1e-13",
        "10, 101, 10.0980932339625119628436416537120371, 0.00944537782565626116413681765035684208, 1e-15, 1e-11",
        "20, 101, 20.0497530685278505422140233087209891, 0.00246326161505216359968528619980015911, 1e-15, 1e-11",
        "30, 101, 30.033259667433677037071124100012257, 0.00110377151189009100113674138540728116, 1e-15, 1e-10",
        "40, 101, 40.0249688472072637232448709953697417, 0.000622668378591388773498879400697584317, 1e-15, 2e-9",
        "100, 101, 100.009998000999260705184902394575471, 9.99400499482634503612772420030347819e-05, 1e-15, 2e-8",

        // One-sided truncation
        "-5, Infinity, 1.4867199409049057124417441194605712e-06, 0.999992566398085139288753504945569711, 1e-14, 1e-14",
        "-3, Infinity, 0.00443783904212566379330210431090259846, 0.98666678845825919379095350748267984, 1e-15, 1e-15",
        "-1, Infinity, 0.287599970939178361228670127385217154, 0.629686285776605400861244494862843306, 1e-15, 1e-15",
        "0, Infinity, 0.797884560802865355879892119868763748, 0.363380227632418656924464946509942526, 1e-15, 1e-15",
        "1, Infinity, 1.52513527616098120908909053639057876, 0.199097665570348791553367979096726767, 1e-15, 1e-15",
        "3, Infinity, 3.28309865493043650692809222681220005, 0.0705591867852681168624020577420568271, 1e-15, 2e-14",
        "20, Infinity, 20.0497530685278505422140233087209891, 0.00246326161505216359968528619980015911, 1e-15, 1e-11",
        "100, Infinity, 100.009998000999260705184902394575471, 9.99400499482634503612772420030347819e-05, 1e-15, 4e-8",
        // The variance method is inaccurate at this extreme
        "1e4, Infinity, 10000.0000999999980000000999999925986, 9.99999940000005002391967510312099493e-09, 1e-15, 0.8",
        "1e6, Infinity, 1000000.00000099999999999800000000016, 9.99999999770471649802883928921316157e-13, 1e-15, 1.0",
        // XXX: The expected variance here is incorrect. It will be small but may be non zero.
        // The computation will return 0. This hits an edge case in the code that detects when the
        // variance computation fails.
        "1e100, Infinity, 1.00000000000000001590289110975991788e+100, 0, 1e-15, -1",

        // XXX: The expected variance here is incorrect. It will be small but may be non zero.
        // This hits an edge case where the computed variance (infinity) is above 1
        "1e290, 1e300, 1.00000000000000006172783352786715689e+290, 0, 1e-15, -1",

        // Small ranges.
        "1, 1.1000000000000001, 1.04912545221799091312759556239135752, 0.000832596851563726615564931035799390151, 1e-15, 2e-12",
        "5, 5.0999999999999996, 5.04581083165668427678725919870992629, 0.000822546087919772895415146023240560636, 1e-15, 2e-11",
        "35, 35.100000000000001, 35.025438801080858717764612789648226, 0.000494605845872597846399929727938197022, 1e-15, 2e-9",

        // (b-a) = 1 ULP
        // XXX: The expected variance here is incorrect.
        // It is upper limited to the variance of a uniform distribution.
        // The computation will return 0. This hits an edge case in the code that detects when the
        // variance computation fails.
        // Spans p=8.327e-17 of the parent normal distribution
        "1, 1.0000000000000002, 1.00000000000000011091535982917837267, 0, 1e-15, -1",
        // Spans p=1.626e-19 of the parent normal distribution
        "4, 4.0000000000000009, 4.00000000000000044406536771487238653, 0, 1e-15, -1",
        // Spans p=1.925e-37 of the parent normal distribution
        "10, 10.000000000000002, 10.0000000000000008883225369216741152, 0, 1e-15, -1",

        // Test for truncation close to zero.
        // At z <= ~1.5e-8, exp(-0.5 * z * z) / sqrt(2 pi) == 1 / sqrt(2 pi)
        // and the PDF is constant. It can be approximated as a uniform distribution.
        // Here the mean is computable but the variance computation -> 0.
        // The epsilons for the variance allow the test to pass if the second moment
        // uses a uniform distribution approximation: (b^3 - a^3) / (3b - 3a).
        // This is not done at present and the variance computes incorrectly and close to 0.
        // The largest span covers only 5.8242e-8 of the probability range of the parent normal
        // and these are not practical truncations.
        "-7.299454196351098e-8, 7.299454196351098e-8, 0, 1.77606771882092042827020676955306864e-15, 1e-15, -1e-15",
        "-7.299454196351098e-8, 3.649727098175549e-8, -1.82486354908777262111748030604612676e-08, 9.99038091836768051420202283759953002e-16, 1e-15, -1e-15",
        "-7.299454196351098e-8, 1.8248635490877744e-8, -2.7372953236316597672674778496667655e-08, 6.93776452664422342699175710737901419e-16, 1e-15, -2e-15",
        "-7.299454196351098e-8, 0, -3.64972709817554726791073610445021429e-08, 4.44016929705230343732389204118195096e-16, 1e-15, -2e-15",
        "-7.299454196351098e-8, -1.8248635490877744e-8, -4.56215887271943497112157190855901547e-08, 2.49759522957641055973442997155578316e-16, 3e-10, -5e-9",
        "-7.299454196351098e-8, -3.649727098175549e-8, -5.47459064726332272497430210977513379e-08, 1.11004232421306844799494326433537718e-16, 3e-10, -2e-8",
        "-3.649727098175549e-8, 3.649727098175549e-8, 0, 4.44016929705230343602092590994317462e-16, 1e-15, -1e-15",
        "-3.649727098175549e-8, 1.8248635490877744e-8, -9.12431774543886994224314381693928319e-09, 2.49759522959192087703300220816741702e-16, 1e-15, -1e-15",
        "-3.649727098175549e-8, 0, -1.82486354908777424165810069993271136e-08, 1.11004232426307600672725101668733634e-16, 1e-15, -2e-15",
        "-3.649727098175549e-8, -1.8248635490877744e-8, -2.73729532363166159037567578213044937e-08, 2.77510581119222125912321725734620803e-17, 3e-10, -2e-8",
        "-1.8248635490877744e-8, 1.8248635490877744e-8, 0, 1.11004232426307600757649604002272128e-16, 1e-15, -1e-15",
        "-1.8248635490877744e-8, 9.124317745438872e-9, -4.5621588727194358257035396943085424e-09, 6.24398807397980267185125584627689296e-17, 1e-15, -1e-15",
        "-1.8248635490877744e-8, 0, -9.12431774543887196791891930929818729e-09, 2.77510581065769011631256630479419225e-17, 1e-15, -1e-15",
        "-9.124317745438872e-9, 9.124317745438872e-9, 0, 2.77510581065769013145632047586655539e-17, 1e-15, -1e-15",
        "-9.124317745438872e-9, 4.562158872719436e-9, -2.28107943635971801967451582038414367e-09, 1.56099701849495071020338587207547036e-17, 1e-15, -1e-15",
        "-9.124317745438872e-9, 0, -4.5621588727194360789130116308534264e-09, 6.93776452664422554954584023114952882e-18, 1e-15, -1e-15",

        // The variance method is inaccurate at this extreme.
        // Spans p=8.858e-17 of the parent normal distribution
        "0, 2.220446049250313e-16, 1.11022302462515654042363166809081572e-16, 4.14074938043255708407035257655783112e-33, 1e-15, -1e-2",
    })
    void testAdditionalMoments(double lower, double upper,
                               double mean, double variance,
                               double meanRelativeError, double varianceRelativeError) {
        assertMean(lower, upper, mean, meanRelativeError);
        if (varianceRelativeError < 0) {
            // Known problem case.
            // Allow small absolute variances using an absolute threshold of
            // machine epsilon (2^-52) * 1.5. Any true variance approaching machine epsilon
            // is allowed to be computed as small or zero but cannot be too large.
            final double v = TruncatedNormalDistribution.variance(lower, upper);
            Assertions.assertTrue(v >= 0, () -> "Variance is not positive: " + v);
            Assertions.assertEquals(v, TruncatedNormalDistribution.variance(-upper, -lower));
            TestUtils.assertEquals(variance, v,
                    createAbsOrRelTolerance(1.5 * 0x1.0p-52, -varianceRelativeError),
                () -> String.format("variance(%s, %s)", lower, upper));
        } else {
            assertVariance(lower, upper, variance, varianceRelativeError);
        }
    }

    /**
     * Assert the mean of the truncated normal distribution is within the provided relative error.
     */
    private static void assertMean(double lower, double upper, double expected, double eps) {
        final double mean = TruncatedNormalDistribution.moment1(lower, upper);
        Assertions.assertEquals(0 - mean, TruncatedNormalDistribution.moment1(-upper, -lower));
        TestUtils.assertEquals(expected, mean, DoubleTolerances.relative(eps),
            () -> String.format("mean(%s, %s)", lower, upper));
    }

    /**
     * Assert the mean of the truncated normal distribution is within the provided relative error.
     * Helper method using range [lower, upper] of the parent normal distribution with the specified
     * mean and standard deviation.
     */
    private static void assertMean(double lower, double upper, double u, double s, double expected, double eps) {
        final double a = (lower - u) / s;
        final double b = (upper - u) / s;
        final double mean = u + TruncatedNormalDistribution.moment1(a, b) * s;
        TestUtils.assertEquals(expected, mean, DoubleTolerances.relative(eps),
            () -> String.format("mean(%s, %s, %s, %s)", lower, upper, u, s));
    }

    /**
     * Assert the variance of the truncated normal distribution is within the provided relative error.
     */
    private static void assertVariance(double lower, double upper, double expected, double eps) {
        final double variance = TruncatedNormalDistribution.variance(lower, upper);
        Assertions.assertEquals(variance, TruncatedNormalDistribution.variance(-upper, -lower));
        TestUtils.assertEquals(expected, variance, DoubleTolerances.relative(eps),
            () -> String.format("variance(%s, %s)", lower, upper));
    }

    /**
     * Assert the variance of the truncated normal distribution is within the provided relative error.
     * Helper method using range [lower, upper] of the parent normal distribution with the specified
     * mean and standard deviation.
     */
    private static void assertVariance(double lower, double upper, double u, double s, double expected, double eps) {
        final double a = (lower - u) / s;
        final double b = (upper - u) / s;
        final double variance = TruncatedNormalDistribution.variance(a, b) * s * s;
        TestUtils.assertEquals(expected, variance, DoubleTolerances.relative(eps),
            () -> String.format("variance(%s, %s, %s, %s)", lower, upper, u, s));
    }
}
