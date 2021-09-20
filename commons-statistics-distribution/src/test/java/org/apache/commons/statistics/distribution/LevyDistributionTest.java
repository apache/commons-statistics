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

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for LevyDistribution.
 */
class LevyDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-6);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public LevyDistribution makeDistribution() {
        return new LevyDistribution(1.2, 0.4);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {
            1.2, 1.2001, 1.21, 1.225, 1.25, 1.3, 1.9, 3.4, 5.6
        };
    }

    @Override
    public double[] makeCumulativeTestValues() {
        // values computed with R and function plevy from rmutil package
        // 0 has been supplemented as R raises an error for x=mu;
        // 'scipy.stats import levy' returns 0.
        // Mathematica returns 0.
        return new double[] {
            0, 0,
            2.5396285074918978353e-10, 6.3342483666239957074e-05,
            4.6777349810471768876e-03, 4.5500263896358417171e-02,
            4.4969179796889102718e-01, 6.6981535759941657204e-01,
            7.6302460055299503594e-01,
        };
    }

    @Override
    public double[] makeDensityTestValues() {
        // values computed with R and function plevy from rmutil package
        // 0 has been supplemented for x=mu.
        return new double[] {
            0, 0,
            5.2005637376544783034e-07, 2.1412836122382383069e-02,
            4.1333970708184164522e-01, 1.0798193302637617563e+00,
            3.2374931916108729002e-01, 7.0603255009363707906e-02,
            2.6122839883975741693e-02,
        };
    }

    @Override
    public double[] makeLogDensityTestValues() {
        // Reference values are from R, version 2.14.1.
        // -infinity has been supplemented for x=mu.
        return new double[] {
            Double.NEGATIVE_INFINITY,   -1.9875615733413976614e+03,
            -1.4469328620159595644e+01, -3.8437647179708118728e+00,
            -8.8348548881076238715e-01,  7.6793740349318850846e-02,
            -1.1277857689479373615e+00, -2.6506790305972467436e+00,
            -3.6449452559826185372e+00
        };
    }

    @Override
    public double[] makeCumulativePrecisionTestPoints() {
        return new double[] {1.205, 1.2049};
    }

    @Override
    public double[] makeCumulativePrecisionTestValues() {
        // These were created using WolframAlpha
        return new double[] {3.7440973842063723e-19, 1.6388396909072308e-19};
    }

    @Override
    public double[] makeSurvivalPrecisionTestPoints() {
        return new double[] {1e39, 42e37};
    }

    @Override
    public double[] makeSurvivalPrecisionTestValues() {
        // These were created using WolframAlpha
        return new double[] {1.5957691216057308e-20, 2.4623252122982907e-20};
    }

    @Override
    public double[] makeInverseCumulativeTestPoints() {
        // Eliminate redundancy as two cdf values are zero.
        final double[] x = makeCumulativeTestValues();
        return Arrays.copyOfRange(x, 1, x.length);
    }

    @Override
    public double[] makeInverseCumulativeTestValues() {
        // Remove the second test point which evaluates cdf(x) == 0.
        double[] x = makeCumulativeTestPoints();
        final double[] y = Arrays.copyOfRange(x, 1, x.length);
        // Copy back the lowest value of x (this is the support lower bound)
        y[0] = x[0];
        return y;
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testDensityAtSupportBounds() {
        final LevyDistribution distribution = makeDistribution();
        // Below the location
        Assertions.assertEquals(0.0, distribution.density(distribution.getLocation() - 1));
    }

    @Test
    void testParameterAccessors() {
        final LevyDistribution dist = makeDistribution();
        Assertions.assertEquals(1.2, dist.getLocation());
        Assertions.assertEquals(0.4, dist.getScale());
    }

    @Test
    void testMoments() {
        LevyDistribution dist;

        dist = new LevyDistribution(0, 0.5);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getMean());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getVariance());

        dist = new LevyDistribution(0, 1);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getMean());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getVariance());

        dist = new LevyDistribution(-3, 2);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getMean());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getVariance());
    }

    @Test
    void testSupport() {
        final LevyDistribution dist = makeDistribution();
        Assertions.assertEquals(dist.getLocation(), dist.getSupportLowerBound());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getSupportUpperBound());
        Assertions.assertTrue(dist.isSupportConnected());
    }
}
