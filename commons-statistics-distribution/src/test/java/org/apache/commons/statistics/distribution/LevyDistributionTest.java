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

class LevyDistributionTest extends ContinuousDistributionAbstractTest {

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public LevyDistribution makeDistribution() {
        return new LevyDistribution(1.2, 0.4);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {
            1.2001, 1.21, 1.225, 1.25, 1.3, 1.9, 3.4, 5.6
        };
    }

    @Override
    public double[] makeCumulativeTestValues() {
        // values computed with R and function plevy from rmutil package
        return new double[] {
            0, 2.53962850749e-10, 6.33424836662e-05, 0.00467773498105,
            0.0455002638964, 0.449691797969, 0.669815357599, 0.763024600553
        };
    }

    @Override
    public double[] makeDensityTestValues() {
        // values computed with R and function dlevy from rmutil package
        return new double[] {
            0, 5.20056373765e-07, 0.0214128361224, 0.413339707082, 1.07981933026,
            0.323749319161, 0.0706032550094, 0.026122839884
        };
    }

    /**
     * Creates the default logarithmic probability density test expected values.
     * Reference values are from R, version 2.14.1.
     */
    @Override
    public double[] makeLogDensityTestValues() {
        return new double[] {
            -1987.561573341398d, -14.469328620160d, -3.843764717971d,
            -0.883485488811d, 0.076793740349d, -1.127785768948d,
            -2.650679030597d, -3.644945255983d};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testParameterAccessors() {
        final LevyDistribution d = makeDistribution();
        Assertions.assertEquals(1.2, d.getLocation());
        Assertions.assertEquals(0.4, d.getScale());
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
        final LevyDistribution d = makeDistribution();
        Assertions.assertEquals(d.getLocation(), d.getSupportLowerBound());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, d.getSupportUpperBound());
        Assertions.assertTrue(d.isSupportConnected());
    }
}
