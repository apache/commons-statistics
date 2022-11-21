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

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link BinomialDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class BinomialDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int n = (Integer) parameters[0];
        final double p = (Double) parameters[1];
        return BinomialDistribution.of(n, p);
    }


    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {-1, 0.1},
            {10, -0.1},
            {10, 1.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"NumberOfTrials", "ProbabilityOfSuccess"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 1e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testMath718() {
        // For large trials the evaluation of ContinuedFraction was inaccurate.
        // Do a sweep over several large trials to test if the current implementation is
        // numerically stable.

        for (int trials = 500000; trials < 20000000; trials += 100000) {
            final BinomialDistribution dist = BinomialDistribution.of(trials, 0.5);
            final int p = dist.inverseCumulativeProbability(0.5);
            Assertions.assertEquals(trials / 2, p);
        }
    }

    /**
     * Test special case of probability of success 0.0.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10})
    void testProbabilityOfSuccess0(int n) {
        // The sign of p should not matter.
        // Exact equality checks no -0.0 values are generated.
        for (final double p : new double[] {-0.0, 0.0}) {
            final BinomialDistribution dist = BinomialDistribution.of(n, p);
            for (int k = -1; k <= n + 1; k++) {
                Assertions.assertEquals(k == 0 ? 1.0 : 0.0, dist.probability(k));
                Assertions.assertEquals(k == 0 ? 0.0 : Double.NEGATIVE_INFINITY, dist.logProbability(k));
                Assertions.assertEquals(k >= 0 ? 1.0 : 0.0, dist.cumulativeProbability(k));
                Assertions.assertEquals(k >= 0 ? 0.0 : 1.0, dist.survivalProbability(k));
            }
        }
    }

    /**
     * Test special case of probability of success 1.0.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10})
    void testProbabilityOfSuccess1(int n) {
        final BinomialDistribution dist = BinomialDistribution.of(n, 1);
        // Exact equality checks no -0.0 values are generated.
        for (int k = -1; k <= n + 1; k++) {
            Assertions.assertEquals(k == n ? 1.0 : 0.0, dist.probability(k));
            Assertions.assertEquals(k == n ? 0.0 : Double.NEGATIVE_INFINITY, dist.logProbability(k));
            Assertions.assertEquals(k >= n ? 1.0 : 0.0, dist.cumulativeProbability(k));
            Assertions.assertEquals(k >= n ? 0.0 : 1.0, dist.survivalProbability(k));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 0.01, 0.99, 1e-17, 0.3645257e-8, 0.123415276368128, 0.67834532657232434})
    void testNumberOfTrials0(double p) {
        final BinomialDistribution dist = BinomialDistribution.of(0, p);
        // Edge case where the probability is ignored when computing the result
        for (int k = -1; k <= 2; k++) {
            Assertions.assertEquals(k == 0 ? 1.0 : 0.0, dist.probability(k));
            Assertions.assertEquals(k == 0 ? 0.0 : Double.NEGATIVE_INFINITY, dist.logProbability(k));
            Assertions.assertEquals(k >= 0 ? 1.0 : 0.0, dist.cumulativeProbability(k));
            Assertions.assertEquals(k >= 0 ? 0.0 : 1.0, dist.survivalProbability(k));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 0.01, 0.99, 1e-17, 0.3645257e-8, 0.123415276368128, 0.67834532657232434})
    void testNumberOfTrials1(double p) {
        final BinomialDistribution dist = BinomialDistribution.of(1, p);
        // Edge case where the probability should be exact
        Assertions.assertEquals(0.0, dist.probability(-1));
        Assertions.assertEquals(1 - p, dist.probability(0));
        Assertions.assertEquals(p, dist.probability(1));
        Assertions.assertEquals(0.0, dist.probability(2));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logProbability(-1));
        // Current implementation does not use log1p so allow an error tolerance
        TestUtils.assertEquals(Math.log1p(-p), dist.logProbability(0), DoubleTolerances.ulps(1));
        Assertions.assertEquals(Math.log(p), dist.logProbability(1));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logProbability(2));
        Assertions.assertEquals(0.0, dist.cumulativeProbability(-1));
        Assertions.assertEquals(1 - p, dist.cumulativeProbability(0));
        Assertions.assertEquals(1.0, dist.cumulativeProbability(1));
        Assertions.assertEquals(1.0, dist.cumulativeProbability(2));
        Assertions.assertEquals(1.0, dist.survivalProbability(-1));
        Assertions.assertEquals(p, dist.survivalProbability(0));
        Assertions.assertEquals(0.0, dist.survivalProbability(1));
        Assertions.assertEquals(0.0, dist.survivalProbability(2));
    }

    /**
     * Special case for x=0.
     * This hits cases where the SaddlePointExpansionUtils are not used for
     * probability functions. It ensures the edge case handling in BinomialDistribution
     * matches the original logic in the saddle point expansion. This x=0 logic is used
     * by the related hypergeometric distribution and covered by test cases for that
     * distribution ensuring it is correct.
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 0.01, 0.99, 1e-17, 0.3645257e-8, 0.123415276368128, 0.67834532657232434})
    void testX0(double p) {
        for (final int n : new int[] {0, 1, 10}) {
            final BinomialDistribution dist = BinomialDistribution.of(n, p);
            final double expected = SaddlePointExpansionUtils.logBinomialProbability(0, n, p, 1 - p);
            Assertions.assertEquals(expected, dist.logProbability(0));
        }
    }

    /**
     * Test the probability functions at the lower and upper bounds when the p-values
     * are very small. The expected results should be within 1 ULP of an exact
     * computation for pmf(x=0) and pmf(x=n). These values can be computed using
     * java.lang.Math functions.
     *
     * <p>The next value, e.g. pmf(x=1) and cdf(x=1), is asserted to the specified
     * relative error tolerance. These values require computations using p and 1-p
     * and are less exact.
     *
     * @param n Number of trials
     * @param p Probability of success
     * @param eps1 Relative error tolerance for pmf(x=1)
     * @param epsn1 Relative error tolerance for pmf(x=n-1)
     */
    @ParameterizedTest
    @CsvSource({
        // Min p-value is shown for reference.
        "100, 0.50, 8e-15, 8e-15", // 7.888609052210118E-31
        "100, 0.01, 1e-15, 3e-14", // 1.000000000000002E-200
        "100, 0.99, 4e-15, 1e-15", // 1.0000000000000887E-200
        "140, 0.01, 1e-15, 2e-13", // 1.0000000000000029E-280
        "140, 0.99, 2e-13, 1e-15", // 1.0000000000001244E-280
        "50, 0.001, 1e-15, 5e-14", // 1.0000000000000011E-150
        "50, 0.999, 3e-14, 1e-15", // 1.0000000000000444E-150
    })
    void testBounds(int n, double p, double eps1, double epsn1) {
        final BinomialDistribution dist = BinomialDistribution.of(n, p);
        final BigDecimal prob0 = binomialProbability(n, p, 0);
        final BigDecimal probn = binomialProbability(n, p, n);
        final double p0 = prob0.doubleValue();
        final double pn = probn.doubleValue();

        // Require very small non-zero probabilities to make the test difficult.
        // Check using 2^-53 so that at least one p-value satisfies 1 - p == 1.
        final double minp = Math.min(p0, pn);
        Assertions.assertTrue(minp < 0x1.0p-53, () -> "Test should target small p-values: " + minp);
        Assertions.assertTrue(minp > Double.MIN_NORMAL, () -> "Minimum P-value should not be sub-normal: " + minp);

        // Almost exact at the bounds
        final DoubleTolerance tol1 = DoubleTolerances.ulps(1);
        TestUtils.assertEquals(p0, dist.probability(0), tol1, "pmf(0)");
        TestUtils.assertEquals(pn, dist.probability(n), tol1, "pmf(n)");
        // Consistent at the bounds
        Assertions.assertEquals(dist.probability(0), dist.cumulativeProbability(0), "pmf(0) != cdf(0)");
        Assertions.assertEquals(dist.probability(n), dist.survivalProbability(n - 1), "pmf(n) != sf(n-1)");

        // Test probability and log probability are consistent.
        // Avoid log when p-value is close to 1.
        if (p0 < 0.9) {
            TestUtils.assertEquals(Math.log(p0), dist.logProbability(0), tol1, "log(pmf(0)) != logpmf(0)");
        } else {
            TestUtils.assertEquals(p0, Math.exp(dist.logProbability(0)), tol1, "pmf(0) != exp(logpmf(0))");
        }
        if (pn < 0.9) {
            TestUtils.assertEquals(Math.log(pn), dist.logProbability(n), tol1, "log(pmf(n)) != logpmf(n)");
        } else {
            TestUtils.assertEquals(pn, Math.exp(dist.logProbability(n)), tol1, "pmf(n) != exp(logpmf(n))");
        }

        // The next probability is accurate to the specified tolerance.
        final BigDecimal prob1 = binomialProbability(n, p, 1);
        final BigDecimal probn1 = binomialProbability(n, p, n - 1);
        TestUtils.assertEquals(prob1.doubleValue(), dist.probability(1), createRelTolerance(eps1), "pmf(1)");
        TestUtils.assertEquals(probn1.doubleValue(), dist.probability(n - 1), createRelTolerance(epsn1), "pmf(n-1)");

        // Check the cumulative functions
        final double cdf1 = prob0.add(prob1).doubleValue();
        final double sfn2 = probn.add(probn1).doubleValue();
        TestUtils.assertEquals(cdf1, dist.cumulativeProbability(1), createRelTolerance(eps1), "cmf(1)");
        TestUtils.assertEquals(sfn2, dist.survivalProbability(n - 2), createRelTolerance(epsn1), "sf(n-2)");
    }

    /**
     * Compute the binomial distribution probability mass function using exact
     * arithmetic.
     *
     * <p>This has no error handling for invalid arguments.
     *
     * <p>Warning: BigDecimal has a limit on the size of the exponent for the power
     * function. This method has not been extensively tested with very small
     * p-values, large n or large k. Use of a MathContext to round intermediates may be
     * required to reduce memory consumption. The binomial coefficient may not
     * compute for large n and k ~ n/2.
     *
     * @param n Number of trials (must be positive)
     * @param p Probability of success (in [0, 1])
     * @param k Number of successes (must be positive)
     * @return pmf(X=k)
     */
    private static BigDecimal binomialProbability(int n, double p, int k) {
        final int nmk = n - k;
        final int m = Math.min(k, nmk);
        // Probability component: p^k * (1-p)^(n-k)
        final BigDecimal bp = new BigDecimal(p);
        final BigDecimal result = bp.pow(k).multiply(
                 BigDecimal.ONE.subtract(bp).pow(nmk));
        // Compute the binomial coefficient
        // Simple edge cases first.
        if (m == 0) {
            return result;
        } else if (m == 1) {
            return result.multiply(new BigDecimal(n));
        }
        // See org.apache.commons.numbers.combinatorics.BinomialCoefficient
        BigInteger nCk = BigInteger.ONE;
        int i = n - m + 1;
        for (int j = 1; j <= m; j++) {
            nCk = nCk.multiply(BigInteger.valueOf(i)).divide(BigInteger.valueOf(j));
            i++;
        }
        return new BigDecimal(nCk).multiply(result);
    }
}
