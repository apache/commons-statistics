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
 * Test cases for LogisticsDistribution.
 */
public class LogisticsDistributionTest extends ContinuousDistributionAbstractTest {

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public LogisticDistribution makeDistribution() {
        return new LogisticDistribution(2, 5);
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
            0.03173698, 0.03557889, 0.03932239, 0.04278194, 0.04575685, 0.04805215,
            0.04950331, 0.05000000, 0.04950331, 0.04805215, 0.04575685
        };
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {
            0.1978161, 0.2314752, 0.2689414, 0.3100255, 0.3543437, 0.4013123,
            0.4501660, 0.5000000, 0.5498340, 0.5986877, 0.6456563
        };
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        setInverseCumulativeTestPoints(new double[] {0, 1});
        setInverseCumulativeTestValues(new double[] {0, Double.POSITIVE_INFINITY});
        verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testParametersAccessors() {
        final LogisticDistribution d = makeDistribution();
        Assertions.assertEquals(2, d.getLocation());
        Assertions.assertEquals(5, d.getScale());
    }

    @Test
    public void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new LogisticDistribution(1, 0));
    }

    @Test
    public void testMeanAndVariance() {
        final LogisticDistribution d = makeDistribution();
        // Constructor 'location' parameter = mean
        Assertions.assertEquals(2.0, d.getMean());
        // Variance = (s^2 * pi^2) / 3
        // Constructor 'scale' parameter = s
        Assertions.assertEquals(5 * 5 * Math.PI * Math.PI / 3, d.getVariance());
    }

    @Test
    public void testSupport() {
        final LogisticDistribution d = makeDistribution();
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, d.getSupportLowerBound());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, d.getSupportUpperBound());
        Assertions.assertTrue(d.isSupportConnected());
    }
}
