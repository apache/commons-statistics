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

/**
 * Test cases for {@link ParetoDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class ParetoDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double scale = (Double) parameters[0];
        final double shape = (Double) parameters[1];
        return new ParetoDistribution(scale, shape);
    }

    @Override
    protected double getAbsoluteTolerance() {
        // Limited by CDF inverse mapping test
        return 1e-9;
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

    //-------------------- Additional test cases -------------------------------

    @Test
    void testHighPrecision() {
        final ParetoDistribution dist = new ParetoDistribution(2.1, 1.4);
        testCumulativeProbabilityHighPrecision(
            dist,
            new double[] {2.100000000000001, 2.100000000000005},
            new double[] {6.217248937900875e-16, 3.2640556923979585e-15},
            createHighPrecisionTolerance());
        testSurvivalProbabilityHighPrecision(
            dist,
            new double[] {42e11, 64e11},
            new double[] {6.005622169907148e-18, 3.330082930386111e-18},
            createHighPrecisionTolerance());
    }

    @Test
    void testAdditionalHighPrecision() {
        final double[] x = {3.000000000000001, 3.000000000000005};

        // R and Wolfram alpha do not match for high precision CDF at small x.
        // The answers were computed using BigDecimal with a math context precision of 100.
        // The current implementation is closer to the answer than either R or Wolfram but
        // the tolerance is quite high as the error is typically in the second significant digit.

        final ParetoDistribution dist = new ParetoDistribution(3, 0.5);
        // BigDecimal: 1 - (scale/x).sqrt()
        final double[] values = {1.480297366166875E-16, 8.141635513917804E-16};
        testCumulativeProbabilityHighPrecision(dist, x, values, DoubleTolerances.absolute(2e-17));

        final ParetoDistribution dist2 = new ParetoDistribution(3, 2);
        // BigDecimal: 1 - (scale/x).pow(2)
        final double[] values2 = {5.921189464667499E-16, 3.256654205567118E-15};
        testCumulativeProbabilityHighPrecision(dist2, x, values2, DoubleTolerances.absolute(8e-17));
    }

    @Test
    void testAdditionalMoments() {
        final double tol = 1e-9;
        ParetoDistribution dist;

        dist = new ParetoDistribution(1, 1);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getMean(), tol);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getVariance(), tol);

        dist = new ParetoDistribution(2.2, 2.4);
        Assertions.assertEquals(3.771428571428, dist.getMean(), tol);
        Assertions.assertEquals(14.816326530, dist.getVariance(), tol);
    }

    /**
     * Check to make sure top-coding of extreme values works correctly.
     */
    @Test
    void testExtremeValues() {
        final ParetoDistribution dist = new ParetoDistribution(1, 1);
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
}
