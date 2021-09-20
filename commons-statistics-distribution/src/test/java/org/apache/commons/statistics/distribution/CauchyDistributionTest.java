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
 * Test cases for CauchyDistribution.
 * Extends ContinuousDistributionAbstractTest.  See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 */
class CauchyDistributionTest extends ContinuousDistributionAbstractTest {

    private static final double DEFAULT_TOLERANCE = 1e-8;

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(DEFAULT_TOLERANCE);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public CauchyDistribution makeDistribution() {
        return new CauchyDistribution(1.2, 2.1);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R 2.9.2
        return new double[] {-667.24856187, -65.6230835029, -25.4830299460, -12.0588781808,
                             -5.26313542807, 669.64856187, 68.0230835029, 27.8830299460, 14.4588781808, 7.66313542807};
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0.001, 0.01, 0.025, 0.05, 0.1, 0.999,
                             0.990, 0.975, 0.950, 0.900};
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {1.49599158008e-06, 0.000149550440335, 0.000933076881878, 0.00370933207799, 0.0144742330437,
                             1.49599158008e-06, 0.000149550440335, 0.000933076881878, 0.00370933207799, 0.0144742330437};
    }

    @Override
    public double[] makeCumulativePrecisionTestPoints() {
        return new double[] {-1e16};
    }

    @Override
    public double[] makeCumulativePrecisionTestValues() {
        // These were created using WolframAlpha
        return new double[] {5.551115123125783e-17};
    }

    @Override
    public double[] makeSurvivalPrecisionTestPoints() {
        return new double[] {1e16};
    }

    @Override
    public double[] makeSurvivalPrecisionTestValues() {
        // These were created using WolframAlpha
        return makeCumulativePrecisionTestValues();
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testInverseCumulativeProbabilityExtremes() {
        setInverseCumulativeTestPoints(new double[] {0.0, 1.0});
        setInverseCumulativeTestValues(new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY});
        verifyInverseCumulativeProbabilities();
    }

    @ParameterizedTest
    @CsvSource({
        "1.2, 2.1",
        "0, 1",
        "-3, 2",
    })
    void testParameterAccessors(double location, double scale) {
        final CauchyDistribution dist = new CauchyDistribution(location, scale);
        Assertions.assertEquals(location, dist.getLocation());
        Assertions.assertEquals(scale, dist.getScale());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "0, -1",
    })
    void testConstructorPreconditions(double location, double scale) {
        Assertions.assertThrows(DistributionException.class, () -> new CauchyDistribution(location, scale));
    }

    @ParameterizedTest
    @CsvSource({
        "10.2, 0.15",
        "23.12, 2.12",
    })
    void testMoments(double location, double scale) {
        final CauchyDistribution dist = new CauchyDistribution(location, scale);
        Assertions.assertTrue(Double.isNaN(dist.getMean()));
        Assertions.assertTrue(Double.isNaN(dist.getVariance()));
    }
}
