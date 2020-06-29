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
 * Test cases for NakagamiDistribution.
 */
public class NakagamiDistributionTest extends ContinuousDistributionAbstractTest {

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public NakagamiDistribution makeDistribution() {
        return new NakagamiDistribution(0.5, 1);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {
            0, 0.2, 0.4, 0.6, 0.8, 1, 1.2, 1.4, 1.6, 1.8, 2
        };
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {
            0.0000000, 0.7820854, 0.7365403, 0.6664492, 0.5793831, 0.4839414,
            0.3883721, 0.2994549, 0.2218417, 0.1579003, 0.1079819
        };
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {
            0.0000000, 0.1585194, 0.3108435, 0.4514938, 0.5762892, 0.6826895,
            0.7698607, 0.8384867, 0.8904014, 0.9281394, 0.9544997
        };
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testParameterAccessors() {
        final NakagamiDistribution d = makeDistribution();
        Assertions.assertEquals(0.5, d.getShape());
        Assertions.assertEquals(1, d.getScale());
    }

    @Test
    void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new NakagamiDistribution(0.4999, 1.0));
    }

    @Test
    void testConstructorPrecondition2() {
        Assertions.assertThrows(DistributionException.class, () -> new NakagamiDistribution(0.5, 0.0));
    }

    @Test
    void testMoments() {
        // Values obtained using Matlab, e.g.
        // format long;
        // pd = makedist('Nakagami','mu',0.5,'omega',1.0);
        // disp([pd.mean, pd.var])
        NakagamiDistribution dist;
        final double eps = 1e-9;

        dist = new NakagamiDistribution(0.5, 1.0);
        Assertions.assertEquals(0.797884560802866, dist.getMean(), eps);
        Assertions.assertEquals(0.363380227632418, dist.getVariance(), eps);

        dist = new NakagamiDistribution(1.23, 2.5);
        Assertions.assertEquals(1.431786259006201, dist.getMean(), eps);
        Assertions.assertEquals(0.449988108521028, dist.getVariance(), eps);
    }

    @Test
    void testSupport() {
        final NakagamiDistribution d = makeDistribution();
        Assertions.assertEquals(0, d.getSupportLowerBound());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, d.getSupportUpperBound());
        Assertions.assertTrue(d.isSupportConnected());
    }
}
