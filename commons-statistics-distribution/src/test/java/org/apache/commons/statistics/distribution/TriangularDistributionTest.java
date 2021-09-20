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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for {@link TriangularDistribution}. See class javadoc for
 * {@link ContinuousDistributionAbstractTest} for further details.
 */
class TriangularDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-4);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public TriangularDistribution makeDistribution() {
        // Left side 5 wide, right side 10 wide.
        return new TriangularDistribution(-3, 2, 12);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {
            -3.0001,                 // below lower limit
            -3.0,                    // at lower limit
            -2.0, -1.0, 0.0, 1.0,    // on lower side
            2.0,                     // at mode
            3.0, 4.0, 10.0, 11.0,    // on upper side
            12.0,                    // at upper limit
            12.0001                  // above upper limit
        };
    }

    @Override
    public double[] makeCumulativeTestValues() {
        // Top at 2 / (b - a) = 2 / (12 - -3) = 2 / 15 = 7.5
        // Area left  = 7.5 * 5  * 0.5 = 18.75 (1/3 of the total area)
        // Area right = 7.5 * 10 * 0.5 = 37.5  (2/3 of the total area)
        // Area total = 18.75 + 37.5 = 56.25
        // Derivative left side = 7.5 / 5 = 1.5
        // Derivative right side = -7.5 / 10 = -0.75
        final double third = 1 / 3.0;
        final double left = 18.75;
        final double area = 56.25;
        return new double[] {0.0,
                             0.0,
                             0.75 / area, 3 / area, 6.75 / area, 12 / area,
                             third,
                             (left + 7.125) / area, (left + 13.5) / area,
                             (left + 36) / area, (left + 37.125) / area,
                             1.0,
                             1.0};
    }

    @Override
    public double[] makeInverseCumulativeTestPoints() {
        // Exclude the points outside the limits, as they have cumulative
        // probability of zero and one, meaning the inverse returns the
        // limits and not the points outside the limits.
        final double[] points = makeCumulativeTestValues();
        final double[] points2 = new double[points.length - 2];
        System.arraycopy(points, 1, points2, 0, points2.length);
        return points2;
        //return Arrays.copyOfRange(points, 1, points.length - 1);
    }

    @Override
    public double[] makeInverseCumulativeTestValues() {
        // Exclude the points outside the limits, as they have cumulative
        // probability of zero and one, meaning the inverse returns the
        // limits and not the points outside the limits.
        final double[] points = makeCumulativeTestPoints();
        final double[] points2 = new double[points.length - 2];
        System.arraycopy(points, 1, points2, 0, points2.length);
        return points2;
        //return Arrays.copyOfRange(points, 1, points.length - 1);
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {0,
                             0,
                             2 / 75.0, 4 / 75.0, 6 / 75.0, 8 / 75.0,
                             10 / 75.0,
                             9 / 75.0, 8 / 75.0, 2 / 75.0, 1 / 75.0,
                             0,
                             0};
    }

    //-------------------- Additional test cases -------------------------------

    @ParameterizedTest
    @CsvSource({
        "1, 2, 3",
        "0.12, 3.45, 12.56",
    })
    void testParameterAccessors(double lower, double mode, double upper) {
        final TriangularDistribution dist = new TriangularDistribution(lower, mode, upper);
        Assertions.assertEquals(lower, dist.getSupportLowerBound());
        Assertions.assertEquals(mode, dist.getMode());
        Assertions.assertEquals(upper, dist.getSupportUpperBound());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",
        // 1, 2, 3 is OK - move points to incorrect locations
        "4, 2, 3",
        "3, 2, 3",
        "2.5, 2, 3",
        "1, 0, 3",
        "1, 4, 3",
        "1, 2, -1",
        "1, 2, 1.5",
    })
    void testConstructorPreconditions(double lower, double mode, double upper) {
        Assertions.assertThrows(DistributionException.class, () -> new TriangularDistribution(lower, mode, upper));
    }

    @Test
    void testMoments() {
        TriangularDistribution dist;

        dist = new TriangularDistribution(0, 0.5, 1.0);
        Assertions.assertEquals(0.5, dist.getMean());
        Assertions.assertEquals(1 / 24.0, dist.getVariance());

        dist = new TriangularDistribution(0, 1, 1);
        Assertions.assertEquals(2 / 3.0, dist.getMean());
        Assertions.assertEquals(1 / 18.0, dist.getVariance());

        dist = new TriangularDistribution(-3, 2, 12);
        Assertions.assertEquals(3 + (2 / 3.0), dist.getMean());
        Assertions.assertEquals(175 / 18.0, dist.getVariance());
    }
}
