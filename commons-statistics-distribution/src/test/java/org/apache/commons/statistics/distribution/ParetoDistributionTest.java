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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link ParetoDistribution}.
 * Extends {@link ContinuousDistributionAbstractTest}. See class javadoc of that class for details.
 */
class ParetoDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-9);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public ParetoDistribution makeDistribution() {
        return new ParetoDistribution(2.1, 1.4);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R
        return new double[] {-2.226325228634938, -1.156887023657177, -0.643949578356075, -0.2027950777320613, 0.305827808237559,
                             +6.42632522863494, 5.35688702365718, 4.843949578356074, 4.40279507773206, 3.89417219176244};
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0, 0, 0, 0, 0, 0.791089998892, 0.730456085931, 0.689667290488, 0.645278794701, 0.578763688757};
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {0, 0, 0, 0, 0, 0.0455118580441, 0.070444173646, 0.0896924681582, 0.112794186114, 0.151439332084};
    }

    @Override
    public double[] makeInverseCumulativeTestPoints() {
        // Exclude the test points less than zero, as they have cumulative
        // probability of zero, meaning the inverse returns zero, and not the
        // points less than zero.
        final double[] points = makeCumulativeTestValues();
        final double[] points2 = new double[points.length - 5];
        System.arraycopy(points, 5, points2, 0, points.length - 5);
        return points2;
    }

    @Override
    public double[] makeInverseCumulativeTestValues() {
        // Exclude the test points less than zero, as they have cumulative
        // probability of zero, meaning the inverse returns zero, and not the
        // points less than zero.
        final double[] points = makeCumulativeTestPoints();
        final double[] points2 = new double[points.length - 5];
        System.arraycopy(points, 5, points2, 0, points.length - 5);
        return points2;
    }

    @Override
    public double[] makeCumulativePrecisionTestPoints() {
        return new double[] {2.100000000000001, 2.100000000000005};
    }

    @Override
    public double[] makeCumulativePrecisionTestValues() {
        // These were not created using WolframAlpha, the calculation for Math.log underflows in java
        return new double[] {6.217248937900875e-16, 3.2640556923979585e-15};
    }

    @Override
    public double[] makeSurvivalPrecisionTestPoints() {
        return new double[] {42e11, 64e11};
    }

    @Override
    public double[] makeSurvivalPrecisionTestValues() {
        // These were created using WolframAlpha
        return new double[] {6.005622169907148e-18, 3.330082930386111e-18};
    }

    //-------------------- Additional test cases -------------------------------

    private void verifyQuantiles() {
        final ParetoDistribution distribution = (ParetoDistribution)getDistribution();
        final double mu = distribution.getScale();
        final double sigma = distribution.getShape();
        setCumulativeTestPoints(new double[] {mu - 2 * sigma, mu - sigma,
                                              mu,             mu + sigma,
                                              mu + 2 * sigma, mu + 3 * sigma,
                                              mu + 4 * sigma, mu + 5 * sigma});
        verifyCumulativeProbabilities();
    }

    @Test
    void testQuantiles() {
        setCumulativeTestValues(new double[] {0, 0, 0, 0.510884134236, 0.694625688662, 0.785201995008, 0.837811522357, 0.871634279326});
        setDensityTestValues(new double[] {0, 0, 0.666666666, 0.195646346305, 0.0872498032394, 0.0477328899983, 0.0294888141169, 0.0197485724114});
        verifyQuantiles();
        verifyDensities();

        setDistribution(new ParetoDistribution(1, 1));
        setCumulativeTestValues(new double[] {0, 0, 0, 0.5, 0.666666666667, 0.75, 0.8, 0.833333333333});
        setDensityTestValues(new double[] {0, 0, 1.0, 0.25, 0.111111111111, 0.0625, 0.04, 0.0277777777778});
        verifyQuantiles();
        verifyDensities();

        setDistribution(new ParetoDistribution(0.1, 0.1));
        setCumulativeTestValues(new double[] {0, 0, 0, 0.0669670084632, 0.104041540159, 0.129449436704, 0.148660077479, 0.164041197922});
        setDensityTestValues(new double[] {0, 0, 1.0, 0.466516495768, 0.298652819947, 0.217637640824, 0.170267984504, 0.139326467013});
        verifyQuantiles();
        verifyDensities();
    }

    @Test
    void testInverseCumulativeProbabilityExtremes() {
        setInverseCumulativeTestPoints(new double[] {0, 1});
        setInverseCumulativeTestValues(new double[] {2.1, Double.POSITIVE_INFINITY});
        verifyInverseCumulativeProbabilities();
    }

    @Test
    void testParameterAccessors() {
        final ParetoDistribution distribution = (ParetoDistribution)getDistribution();
        Assertions.assertEquals(2.1, distribution.getScale());
        Assertions.assertEquals(1.4, distribution.getShape());
    }

    @Test
    void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new ParetoDistribution(1, 0));
    }

    @Test
    void testConstructorPrecondition2() {
        Assertions.assertThrows(DistributionException.class, () -> new ParetoDistribution(0, 1));
    }

    @Test
    void testMoments() {
        final double tol = 1e-9;
        ParetoDistribution dist;

        dist = new ParetoDistribution(1, 1);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getMean(), tol);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getVariance(), tol);

        dist = new ParetoDistribution(2.2, 2.4);
        Assertions.assertEquals(3.771428571428, dist.getMean(), tol);
        Assertions.assertEquals(14.816326530, dist.getVariance(), tol);
    }

    @Test
    void testDensity() {
        final double[] x = new double[] {-2, -1, 0, 1, 2};
        // R 2.14: print(dpareto(c(-2,-1,0,1,2), scale=1, shape=1), digits=10)
        checkDensity(1, 1, x, new double[] {0.00, 0.00, 0.00, 1.00, 0.25});
        // R 2.14: print(dpareto(c(-2,-1,0,1,2), scale=1.1, shape=1), digits=10)
        checkDensity(1.1, 1, x, new double[] {0.000, 0.000, 0.000, 0.000, 0.275});
    }

    private void checkDensity(double scale, double shape, double[] x,
        double[] expected) {
        final ParetoDistribution d = new ParetoDistribution(scale, shape);
        for (int i = 0; i < x.length; i++) {
            Assertions.assertEquals(expected[i], d.density(x[i]), 1e-9);
        }
    }

    /**
     * Check to make sure top-coding of extreme values works correctly.
     */
    @Test
    void testExtremeValues() {
        final ParetoDistribution d = new ParetoDistribution(1, 1);
        for (int i = 0; i < 1e5; i++) { // make sure no convergence exception
            final double upperTail = d.cumulativeProbability(i);
            if (i <= 1000) { // make sure not top-coded
                Assertions.assertTrue(upperTail < 1.0d);
            } else { // make sure top coding not reversed
                Assertions.assertTrue(upperTail > 0.999);
            }
        }

        Assertions.assertEquals(1, d.cumulativeProbability(Double.MAX_VALUE));
        Assertions.assertEquals(0, d.cumulativeProbability(-Double.MAX_VALUE));
        Assertions.assertEquals(1, d.cumulativeProbability(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(0, d.cumulativeProbability(Double.NEGATIVE_INFINITY));
    }
}
