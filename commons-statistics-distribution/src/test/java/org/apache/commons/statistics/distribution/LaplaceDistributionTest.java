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
 * Test cases for LaplaceDistribution.
 */
public class LaplaceDistributionTest extends ContinuousDistributionAbstractTest {

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public LaplaceDistribution makeDistribution() {
        return new LaplaceDistribution(0, 1);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {
            -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5
        };
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {
            0.003368973, 0.009157819, 0.024893534, 0.067667642, 0.183939721,
            0.500000000, 0.183939721, 0.067667642, 0.024893534, 0.009157819, 0.003368973
        };
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {
            0.003368973, 0.009157819, 0.024893534, 0.067667642, 0.183939721,
            0.500000000, 0.816060279, 0.932332358, 0.975106466, 0.990842181, 0.996631027
        };
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        setInverseCumulativeTestPoints(new double[] {0.0, 1.0});
        setInverseCumulativeTestValues(new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY});
        verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testParameterAccessors() {
        final LaplaceDistribution d = makeDistribution();
        Assertions.assertEquals(0, d.getLocation());
        Assertions.assertEquals(1, d.getScale());
    }

    @Test
    public void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new LaplaceDistribution(0, -0.1));
    }

    @Test
    public void testMoments() {
        LaplaceDistribution dist;

        dist = new LaplaceDistribution(0.5, 1.0);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(2.0 * 1.0 * 1.0, dist.getVariance());

        dist = new LaplaceDistribution(-0.3, 2.5);
        Assertions.assertEquals(-0.3, dist.getMean());
        Assertions.assertEquals(2.0 * 2.5 * 2.5, dist.getVariance());
    }

    @Test
    public void testSupport() {
        final LaplaceDistribution d = makeDistribution();
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, d.getSupportLowerBound());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, d.getSupportUpperBound());
        Assertions.assertTrue(d.isSupportConnected());
    }
}
