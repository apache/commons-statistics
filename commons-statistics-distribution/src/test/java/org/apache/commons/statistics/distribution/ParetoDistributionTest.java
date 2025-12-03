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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for {@link ParetoDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class ParetoDistributionTest extends BaseContinuousDistributionTest {
    /**
     * The difference each of the 2^53 dyadic rationals in [0, 1).
     * This is the smallest non-zero value for p to use when inverse transform sampling.
     * Equal to 2^-53.
     */
    private static final double U = 0x1.0p-53;

    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double scale = (Double) parameters[0];
        final double shape = (Double) parameters[1];
        return ParetoDistribution.of(scale, shape);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 1.0},
            {-0.1, 1.0},
            {1.0, 0.0},
            {1.0, -0.1},
            {Double.POSITIVE_INFINITY, 1.0},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Scale", "Shape"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 5e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @CsvSource({
        "1, 1, Infinity, Infinity",
        "2.2, 2.4, 3.771428571428, 14.816326530",
    })
    void testAdditionalMoments(double scale, double shape, double mean, double variance) {
        final ParetoDistribution dist = ParetoDistribution.of(scale, shape);
        testMoments(dist, mean, variance, createRelTolerance(1e-9));
    }

    @Test
    void testAdditionalCumulativeProbabilityHighPrecision() {
        final double scale = 2.1;
        // 2.1000000000000005, 2.100000000000001
        final double[] x = {Math.nextUp(scale), Math.nextUp(Math.nextUp(scale))};

        // R and Wolfram alpha do not match for high precision CDF at small x.
        // The answers were computed using BigDecimal with a math context precision of 100.
        // Note that the results using double are limited by intermediate rounding and the
        // CDF is not high precision as the number of bits of accuracy is low:
        //
        // x = Math.nextUp(scale)
        // 1.0 - pow(scale/x, 0.75)                    ==> 1.1102230246251565E-16
        // -expm1(shape * log(scale/x))                ==> 1.665334536937735E-16
        // -expm1(shape * log(scale) - shape * log(x)) ==> 2.2204460492503128E-16
        //
        // x = Math.nextUp(Math.nextUp(scale))
        // 1.0 - pow(scale/x, 0.75)                    ==> 3.3306690738754696E-16
        // -expm1(shape * log(scale/x))                ==> 3.33066907387547E-16
        // -expm1(shape * log(scale) - shape * log(x)) ==> 4.440892098500625E-16

        final ParetoDistribution dist = ParetoDistribution.of(scale, 0.75);
        // BigDecimal: 1 - (scale/x).pow(3).sqrt().sqrt()
        // MathContext mc = new MathContext(100)
        // BigDecimal.ONE.subtract(
        //   new BigDecimal(2.1).divide(new BigDecimal(Math.nextUp(Math.nextUp(2.1))), mc)
        //    .pow(3).sqrt(mc).sqrt(mc)).doubleValue()
        final double[] values = {1.5860328923216517E-16, 3.172065784643303E-16};
        testCumulativeProbabilityHighPrecision(dist, x, values, createRelTolerance(0.05));
    }

    @Test
    void testAdditionalCumulativeProbabilityHighPrecision2() {
        final double scale = 3;
        // 3.0000000000000004, 3.000000000000001
        final double[] x = {Math.nextUp(scale), Math.nextUp(Math.nextUp(scale))};

        // The current implementation is closer to the answer than either R or Wolfram but
        // the relative error is typically 0.25 (error in the first or second digit).
        // The absolute tolerance checks the result to a closer tolerance than
        // the answer computed using 1 - Math.pow(scale/x, shape), which is zero.

        final ParetoDistribution dist = ParetoDistribution.of(3, 0.25);
        // BigDecimal: 1 - (scale/x).sqrt().sqrt()
        final double[] values = {3.700743415417188E-17, 7.401486830834375E-17};
        testCumulativeProbabilityHighPrecision(dist, x, values, createAbsTolerance(1e-17));

        final ParetoDistribution dist2 = ParetoDistribution.of(3, 1.5);
        // BigDecimal: 1 - (scale/x).pow(3).sqrt()
        final double[] values2 = {2.2204460492503126E-16, 4.4408920985006247E-16};
        testCumulativeProbabilityHighPrecision(dist2, x, values2, createAbsTolerance(6e-17));
    }

    @Test
    void testAdditionalSurvivalProbabilityHighPrecision() {
        final ParetoDistribution dist = ParetoDistribution.of(2.1, 1.4);
        testSurvivalProbabilityHighPrecision(
            dist,
            new double[] {42e11, 64e11},
            new double[] {6.005622169907148e-18, 3.330082930386111e-18},
            DoubleTolerances.relative(5e-14));
    }

    /**
     * Check to make sure top-coding of extreme values works correctly.
     */
    @Test
    void testExtremeValues() {
        final ParetoDistribution dist = ParetoDistribution.of(1, 1);
        for (int i = 0; i < 10000; i++) { // make sure no convergence exception
            final double upperTail = dist.cumulativeProbability(i);
            if (i <= 1000) { // make sure not top-coded
                Assertions.assertTrue(upperTail < 1.0d);
            } else { // make sure top coding not reversed
                Assertions.assertTrue(upperTail > 0.999);
            }
        }

        Assertions.assertEquals(1, dist.cumulativeProbability(Double.MAX_VALUE));
        Assertions.assertEquals(0, dist.cumulativeProbability(-Double.MAX_VALUE));
        Assertions.assertEquals(1, dist.cumulativeProbability(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(0, dist.cumulativeProbability(Double.NEGATIVE_INFINITY));
    }

    /**
     * Test extreme parameters to the distribution. This uses the same computation to precompute
     * factors for the PMF and log PMF as performed by the distribution. When the factors are
     * not finite then the edges cases must be appropriately handled.
     */
    @Test
    void testExtremeParameters() {
        double scale;
        double shape;

        // Overflow of standard computation. Log computation OK.
        scale = 10;
        shape = 306;
        Assertions.assertEquals(Double.POSITIVE_INFINITY, shape * Math.pow(scale, shape));
        Assertions.assertTrue(Double.isFinite(Math.log(shape) + Math.log(scale) * shape));

        // ---

        // Overflow of standard computation. Overflow of Log computation.
        scale = 10;
        shape = Double.POSITIVE_INFINITY;
        Assertions.assertEquals(Double.POSITIVE_INFINITY, shape * Math.pow(scale, shape));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Math.log(shape) + Math.log(scale) * shape);

        // This case can compute as if shape is big (Dirac delta function)
        shape = 1e300;
        Assertions.assertEquals(Double.POSITIVE_INFINITY, shape * Math.pow(scale, shape));
        Assertions.assertTrue(Double.isFinite(Math.log(shape) + Math.log(scale) * shape));

        // ---

        // NaN of standard computation. NaN of Log computation.
        scale = 1;
        shape = Double.POSITIVE_INFINITY;
        // 1^inf == NaN
        Assertions.assertEquals(Double.NaN, shape * Math.pow(scale, shape));
        // 0 * inf == NaN
        Assertions.assertEquals(Double.NaN, Math.log(shape) + Math.log(scale) * shape);

        // This case can compute as if shape is big (Dirac delta function)
        shape = 1e300;
        Assertions.assertEquals(shape, shape * Math.pow(scale, shape));
        Assertions.assertTrue(Double.isFinite(Math.log(shape) + Math.log(scale) * shape));

        // ---

        // Underflow of standard computation. Log computation OK.
        scale = 0.1;
        shape = 324;
        Assertions.assertEquals(0.0, shape * Math.pow(scale, shape));
        Assertions.assertTrue(Double.isFinite(Math.log(shape) + Math.log(scale) * shape));

        // ---

        // Underflow of standard computation. Underflow of Log computation.
        scale = 0.1;
        shape = Double.MAX_VALUE;
        Assertions.assertEquals(0.0, shape * Math.pow(scale, shape));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Math.log(shape) + Math.log(scale) * shape);

        // This case can compute as if shape is big (Dirac delta function)

        // ---

        // Underflow of standard computation to NaN. NaN of Log computation.
        scale = 0.1;
        shape = Double.POSITIVE_INFINITY;
        Assertions.assertEquals(Double.NaN, shape * Math.pow(scale, shape));
        Assertions.assertEquals(Double.NaN, Math.log(shape) + Math.log(scale) * shape);

        // This case can compute as if shape is big (Dirac delta function)

        // ---

        // Smallest possible value of shape is OK.
        // The Math.pow function -> 1 as the exponent -> 0.
        shape = Double.MIN_VALUE;
        for (final double scale2 : new double[] {Double.MIN_VALUE, 0.1, 1, 10, 100}) {
            Assertions.assertEquals(shape, shape * Math.pow(scale2, shape));
            Assertions.assertTrue(Double.isFinite(Math.log(shape) + Math.log(scale2) * shape));
        }
    }

    /**
     * Test sampling with a large shape. As {@code shape -> inf} then the distribution
     * approaches a delta function with {@code CDF(x=scale)} = 0 and {@code CDF(x>scale) = 1}.
     * This test verifies that a large shape is effectively sampled from p in [0, 1) to avoid
     * spurious infinite samples when p=1.
     *
     * <p>Sampling Details
     *
     * <p>Note that sampling is using inverse transform sampling by inverting the CDF:
     * <pre>
     * CDF(x) = 1 - (scale / x)^shape
     * x = scale / (1 - p)^(1 / shape)
     *   = scale / exp(log(1 - p) / shape)
     * </pre>
     *
     * <p>The sampler in Commons RNG is inverting the CDF function using Math.pow:
     * <pre>
     * x = scale / Math.pow(1 - p, 1 / shape)
     * </pre>
     *
     * <p>The Pareto distribution uses log functions to achieve the same result:
     * <pre>
     * x = scale / Math.exp(Math.log1p(-p) / shape);
     * </pre>
     *
     * <p>Inversion will return the scale when Math.exp(X) == 1 where X (in [-inf, 0]) is:
     * <pre>
     * X = log(1 - p) / shape
     * </pre>
     *
     * <p>This occurs when {@code X > log(1.0 - epsilon)}, or larger (closer to zero) than
     * {@code Math.log(Math.nextDown(1.0))}; X is approximately -1.11e-16.
     * During sampling p is bounded to the 2^53 dyadic rationals in [0, 1). The largest
     * finite value for the logarithm is log(2^-53) thus the critical size for shape is around:
     * <pre>
     * shape = log(2^-53) / -1.1102230246251565e-16 = 3.3089568271276403e17
     * </pre>
     *
     * <p>Note that if the p-value is 1 then inverseCumulativeProbability(1.0) == inf.
     * However using the power function to invert this ignores this possibility when the shape
     * is infinite and will always return scale / x^0 = scale / 1 = scale. If the inversion
     * using logarithms is directly used then a log(0) / inf == -inf / inf == NaN occurs.
     */
    @ParameterizedTest
    @CsvSource({
        // Scale values match those from the test resource files where the sampling test is disabled
        "10, Infinity",
        "1, Infinity",
        "0.1, Infinity",
        // This behaviour occurs even when the shape is not infinite due to limited precision
        // of double values. Shape is set to twice the limit derived above to account for rounding:
        // double p = 0x1.0p-53
        // Math.pow(p, 1 / (Math.log(p) / -p))     ==> 0.9999999999999999
        // Math.pow(p, 1 / (2 * Math.log(p) / -p)) ==> 1.0
        // shape = (2 * Math.log(p) / -p)
        "10, 6.6179136542552806e17",
        "1, 6.6179136542552806e17",
        "0.1, 6.6179136542552806e17",
    })
    void testSamplingWithLargeShape(double scale, double shape) {
        final ParetoDistribution dist = ParetoDistribution.of(scale, shape);

        // Sampling should act as if inverting p in [0, 1)
        final double x0 = dist.inverseCumulativeProbability(0);
        final double x1 = dist.inverseCumulativeProbability(1 - U);
        Assertions.assertEquals(scale, x0);
        Assertions.assertEquals(x0, x1, "Test parameters did not create an extreme distribution");

        // Sampling for p in [0, 1): returns scale when shape is large
        assertSampler(dist, scale);
    }

    /**
     * Test sampling with a tiny shape. As {@code shape -> 0} then the distribution
     * approaches a function with {@code CDF(x=inf) = 1} and {@code CDF(x>=scale) = 0}.
     * This test verifies that a tiny shape is effectively sampled from p in (0, 1] to avoid
     * spurious NaN samples when p=0.
     *
     * <p>Sampling Details
     *
     * <p>The sampler in Commons RNG is inverting the CDF function using Math.pow:
     * <pre>
     * x = scale / Math.pow(1 - p, 1 / shape)
     * </pre>
     *
     * <p>However Math.pow(1, infinity) == NaN. This can be avoided if p=0 is not used.
     * For all other values Math.pow(1 - p, infinity) == 0 and the sample is infinite.
     */
    @ParameterizedTest
    @CsvSource({
        // 1 / shape is infinite
        // Scale values match those from the test resource files where the sampling test is disabled
        "10, 4.9e-324",
        "1, 4.9e-324",
        "0.1, 4.9e-324",
        // This behaviour occurs even when 1 / shape is not infinite due to limited precision
        // of double values. Shape provides the largest possible finite value from 1 / shape:
        // shape = (1.0 + Math.ulp(1.0)*2) / Double.MAX_VALUE
        // 1 / shape = 1.7976931348623143e308
        // 1 / Math.nextDown(shape) = Infinity
        "10, 5.56268464626801E-309",
        "1, 5.56268464626801E-309",
        "0.1, 5.56268464626801E-309",
        // Lower limit is where pow(1 - p, 1 / shape) < Double.MIN_VALUE:
        // shape < log(1 - p) / log(MIN_VALUE)
        // Shape is set to half this limit to account for rounding:
        // double p = 0x1.0p-53
        // Math.pow(1 - p, 1 / (Math.log(1 - p) / Math.log(Double.MIN_VALUE))) ==> 4.9e-324
        // Math.pow(1 - p, 2 / (Math.log(1 - p) / Math.log(Double.MIN_VALUE))) ==> 0.0
        // shape = 0.5 * Math.log(1 - p) / Math.log(Double.MIN_VALUE)
        "10, 7.456765604783329e-20",
        "1, 7.456765604783329e-20",
        // Use smallest possible scale: test will fail if shape is not half the limit
        "4.9e-324, 7.456765604783329e-20",
    })
    void testSamplingWithTinyShape(double scale, double shape) {
        final ParetoDistribution dist = ParetoDistribution.of(scale, shape);

        // Sampling should act as if inverting p in (0, 1]
        final double x0 = dist.inverseCumulativeProbability(U);
        final double x1 = dist.inverseCumulativeProbability(1);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, x1);
        Assertions.assertEquals(x1, x0, "Test parameters did not create an extreme distribution");

        // Sampling for p in [0, 1): returns infinity when shape is tiny
        assertSampler(dist, Double.POSITIVE_INFINITY);
    }

    /**
     * Assert the sampler produces the expected sample value irrespective of the values from the RNG.
     *
     * @param dist Distribution
     * @param expected Expected sample value
     */
    private static void assertSampler(ParetoDistribution dist, double expected) {
        // Extreme random numbers using no bits or all bits, then combinations
        // that may be used to generate a double from the lower or upper 53-bits
        final long[] values = {0, -1, 1, 1L << 11, -2, -2L << 11};
        final UniformRandomProvider rng = createRNG(values);
        ContinuousDistribution.Sampler s = dist.createSampler(rng);
        for (final long l : values) {
            Assertions.assertEquals(expected, s.sample(), () -> "long bits = " + l);
        }
        // Any random number
        final long seed = RandomSource.createLong();
        s = dist.createSampler(RandomSource.SPLIT_MIX_64.create(seed));
        for (int i = 0; i < 100; i++) {
            Assertions.assertEquals(expected, s.sample(), () -> "seed = " + seed);
        }
    }

    /**
     * Creates the RNG to return the given values from the nextLong() method.
     *
     * @param values Long values
     * @return the RNG
     */
    private static UniformRandomProvider createRNG(long... values) {
        return new UniformRandomProvider() {
            private int i;

            @Override
            public long nextLong() {
                return values[i++];
            }

            @Override
            public double nextDouble() {
                throw new IllegalStateException("nextDouble cannot be trusted to be in [0, 1) and should be ignored");
            }
        };
    }
}
