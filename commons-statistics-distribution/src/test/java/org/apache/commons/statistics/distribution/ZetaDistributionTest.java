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

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.numbers.rootfinder.BrentSolver;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.DiscreteDistribution.Sampler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link ZetaDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class ZetaDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final double e = (Double) parameters[0];
        return ZetaDistribution.of(e);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {-0.1},
            {1.0},
            {Double.NaN},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Exponent"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 1e-14;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @CsvSource({
        // Generated using scipy 1.17.1 using scipy.stats.zipf.stats(s)
        "1.512, Infinity, Infinity, 1e-15",
        "2.512, 1.919627298627266, Infinity, 1e-15",
        "3.512, 1.1879319717598735, 0.8692042723730669, 1e-15",
    })
    void testAdditionalMoments(double s, double mean, double variance, double eps) {
        final DoubleTolerance tolerance = createRelTolerance(eps);
        final ZetaDistribution dist = ZetaDistribution.of(s);
        testMoments(dist, mean, variance, tolerance);
        // Run twice to check the cached values
        testMoments(dist, mean, variance, tolerance);
    }

    /**
     * Test the suggested code in the javadoc for computing the cumulative probability in
     * high precision.
     */
    @ParameterizedTest
    @MethodSource
    void testJavadocCumulativeProbabilityHighPrecision(double s, int x, double expected) {
        final DoubleTolerance tolerance = DoubleTolerances.ulps(1);
        final ZetaDistribution dist = ZetaDistribution.of(s);
        // Method suggested in the javadoc
        double cdfX = IntStream.range(0, x)
                               .mapToDouble(y -> Math.pow(x - y, -s))
                               .sum() * dist.probability(1);
        TestUtils.assertEquals(expected, cdfX, tolerance);
    }

    static Stream<Arguments> testJavadocCumulativeProbabilityHighPrecision() {
        return Stream.of(
            // Computed from scipy.stats (1.17.1) zipf(s)
            // Note: large x will have a long runtime so are avoided
            Arguments.of(2.1, 1, 0.6409366767538137),
            Arguments.of(2.1, 10, 0.9561735629717311),
            Arguments.of(2.1, 100, 0.9963437521110778),
            Arguments.of(1.1, 10, 0.2532163286767671),
            Arguments.of(1.1, 100, 0.40418015518943295),
            Arguments.of(1.01, 100, 0.05054241012268188),
            Arguments.of(1.00001, 100, 5.187242037932912e-05),
            Arguments.of(1.00000001, 100, 5.1873773506313715e-08),
            Arguments.of(1.0000000000000002, 100, 1.1518291915012786e-15),
            Arguments.of(1.0000000000000002, 1000, 1.6621084199087341e-15),
            Arguments.of(1.0000000000000002, 10000, 2.1732851154353238e-15),
            Arguments.of(1.0000000000000002, 10001, 2.1733073176755921e-15)
        );
    }

    /**
     * Suggested method to compute the CDF in [1, x].
     */
    @ParameterizedTest
    @ValueSource(doubles = {1.3, 2.4})
    void testCumulativeProbabilityHighPrecisionSinglePass(double s) {
        final int x = 5;
        final double zeta1 = HurwitzZeta.value(s, 1);
        final double[] expected = IntStream.rangeClosed(1, x)
            .mapToDouble(i -> ZipfDistribution.generalizedHarmonic(1, i, s) / zeta1)
            .toArray();
        final double[] cdf = new double[x];
        final double p1 = ZetaDistribution.of(s).probability(1);
        cdf[0] = p1;
        DD sum = DD.ONE;
        for (int k = 2; k <= x; k++) {
            sum = sum.add(Math.pow(k, -s));
            cdf[k - 1] = sum.doubleValue() * p1;
        }
        final DoubleTolerance tolerance = DoubleTolerances.ulps(1);
        for (int i = 0; i < x; i++) {
            TestUtils.assertEquals(expected[i], cdf[i], tolerance);
        }
    }

    /**
     * Test inversion correctly returns the lower or upper bound with high precision p.
     */
    @ParameterizedTest
    @CsvSource({
        // sf(1, 2, 3) = 8.673617380119933e-19, 2.3589825628243265e-29, 7.523175374682315e-37
        "60, 1e-16, 1",
        "60, 1e-20, 2",
        "60, 1e-28, 2",
        "60, 1e-29, 3",
        "60, 1e-38, 4",
        // sf(1, 2, 3) = 6.223015277861142e-61, 3.764861949599026e-96, 3.8725919148493183e-121
        "200, 1e-60, 1",
        "200, 1e-95, 2",
        "200, 1e-96, 3",
        "200, 1e-122, 4",
        // sf(2^31-3, 2^31-2, 2^31-1] = 2.830881171681671e-10, 2.830881170363439e-10, 2.8308811690452073e-10
        "2, 2.830881171e-10, 2147483646",
        "2, 2.830881170e-10, 2147483647",
        "2, 1e-10, 2147483647",
        "2, 1e-100, 2147483647",
        // sf(2^31-3, 2^31-2, 2^31-1] = 9.019557827587404e-20, 9.019557819187286e-20, 9.019557810787168e-20
        "3, 9.01955782e-20, 2147483646",
        "3, 9.01955781e-20, 2147483647",
        "3, 1e-20, 2147483647",
        "3, 1e-200, 2147483647",
    })
    void testAdditionalInverseSurvivalFunction(double s, double p, int x) {
        final ZetaDistribution dist = ZetaDistribution.of(s);
        Assertions.assertEquals(x, dist.inverseSurvivalProbability(p));
    }

    @ParameterizedTest
    @CsvSource({
        // Tiny s. cdf(2^31 - 1) == 4.8993649719501934e-15
        "1.0000000000000002, 2147483647",
        // Large s. sf(1) == 8.673617380119933e-19
        "60, 1",
        // Very large s. 2^(s-1) == Infinity
        "1025, 1",
    })
    void testSamplingExtremeS(double s, int x) {
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_256_PP.create(123456789L);
        final Sampler sampler = ZetaDistribution.of(s).createSampler(rng);
        final int n = 10;
        final int[] expected = new int[n];
        Arrays.fill(expected, x);
        final int[] sample = TestUtils.sample(n, sampler);
        Assertions.assertArrayEquals(expected, sample);
    }

    /**
     * This is added as the sampler can only be tested when the quantiles of the distribution
     * are spread within the range [1, 2^31). The test resources do not test the sampler
     * with many values of s.
     *
     * <p>The sensitivity of the quantiles to s can be observed using the pmf at x=1:
     * <pre>
     * s       pmf(1)
     * 1.0625  0.06030727407685079
     * 1.125   0.11646539687154238
     * 1.25    0.21762256021191748
     * 1.5     0.3827933839994266
     * 2.25    0.6848321282518275
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(doubles = {1.11, 1.12, 1.13, 1.14, 1.15, 1.2, 1.25, 1.3, 1.35})
    void testAdditionalSampling(double s) {
        testSampling(ZetaDistribution.of(s));
    }

    /**
     * Test the value of the exponent s for critical points in the distribution.
     * These points are used in the main ZetaDistribution either in the code or
     * the documentation. Some values are recorded in the STATISTICS-101 issue.
     *
     * @param p the desired survival function probability
     * @param x the value at which to evaluate the survival function
     * @param exponent the expected exponent
     */
    @ParameterizedTest
    @CsvSource({
        // Threshold to switch the sampler extreme value bias from x=1 to x=inf.
        // This value is used in the Commons RNG ZetaSampler (RNG-203).
        "0.5, 1073741824, 1.0324376395045163",
        // Used in the class javadoc to describe truncation of the distribution
        "0.01, 2147483647, 1.2088900037546617",
        "0.10, 2147483647, 1.1044011399570164",
        "0.50, 2147483647, 1.0314183630697709",
        "0.90, 2147483647, 1.0047751511822856",
        // Test threshold for significant cancellation in the cumulative probability.
        // Each p value is 1 - 2^-b where (b-1) is the number of bits of precision
        // lost by cancellation in the CDF.
        "0.5, 10, 1.238388603270506", // 0-bits
        "0.75, 10, 1.0985236087748393", // 1-bit
        "0.875, 10, 1.0456558205227693", // 2-bits
        "0.9375, 10, 1.0220499988943923", // 3-bits
        "0.96875, 10, 1.0108432918659283", // 4-bits
        // Number of terms to sum to avoid a 2-bit loss of precision for various s
        "0.875, 100, 1.0257535317691207",
        "0.875, 1000, 1.0178427184412626",
        "0.875, 10000, 1.0136446856290058",
        "0.875, 100000, 1.0110455910807687",
        "0.875, 1000000, 1.0092782593613394",
        "0.875, 10000000, 1.0079984961118713",
    })
    void testExponent(double p, int x, double exponent) {
        // Search survival function so that sf(x; s) ~ p
        final DoubleUnaryOperator fun =
            s -> HurwitzZeta.value(s, x + 1.0) / HurwitzZeta.value(s, 1) - p;
        final BrentSolver solver = new BrentSolver(0x1.0p-53, Double.MIN_VALUE, Double.MIN_VALUE);
        // Bracket using the expected value
        final double lower = exponent * 0.5;
        final double upper = exponent * 2;
        final double s = solver.findRoot(fun, lower, upper);
        final ZetaDistribution dist = ZetaDistribution.of(s);
        final double q = dist.survivalProbability(x);
        TestUtils.assertEquals(p, q, DoubleTolerances.relative(5e-15));
        TestUtils.assertEquals(exponent, s, DoubleTolerances.relative(1e-15));
    }
}
