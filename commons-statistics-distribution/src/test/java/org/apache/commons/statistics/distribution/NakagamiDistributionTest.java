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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for NakagamiDistribution.
 */
class NakagamiDistributionTest extends ContinuousDistributionAbstractTest {

    //-------------- Implementations for abstract methods ----------------------

    // Test values created using scipy.stats nakagami
    // The distribution is not defined for x=0.
    // Some implementations compute the formula and return the natural limit as x -> 0.
    // This implementation returns zero for any x outside the domain.

    @Override
    public NakagamiDistribution makeDistribution() {
        return new NakagamiDistribution(0.5, 1);
    }

    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] {
            0, 1e-3, 0.2, 0.4, 0.6, 0.8, 1, 1.2, 1.4, 1.6, 1.8, 2
        };
    }

    @Override
    public double[] makeDensityTestValues() {
        return new double[] {
            0.0,                 0.79788416186068489, 0.78208538795091187,
            0.73654028060664678, 0.66644920578359934, 0.57938310552296557,
            0.48394144903828679, 0.38837210996642596, 0.29945493127148981,
            0.22184166935891111, 0.15790031660178833, 0.10798193302637614,
        };
    }

    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {
            0.0,                     7.97884427822125389e-04,
            1.58519418878206031e-01, 3.10843483220648364e-01,
            4.51493764499852956e-01, 5.76289202833206615e-01,
            6.82689492137085852e-01, 7.69860659556583560e-01,
            8.38486681532458089e-01, 8.90401416600884343e-01,
            9.28139361774148575e-01, 9.54499736103641472e-01,
        };
    }

    @Override
    public double[] makeCumulativePrecisionTestPoints() {
        return new double[] {1e-16, 4e-17};
    }

    @Override
    public double[] makeCumulativePrecisionTestValues() {
        // These were created using WolframAlpha
        return new double[] {7.978845608028653e-17, 3.1915382432114614e-17};
    }

    @Override
    public double[] makeSurvivalPrecisionTestPoints() {
        return new double[] {9, 8.7};
    }

    @Override
    public double[] makeSurvivalPrecisionTestValues() {
        // These were created using WolframAlpha
        return new double[] {2.2571768119076845e-19, 3.318841739929575e-18};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testAdditionalDistribution1() {
        final NakagamiDistribution dist = new NakagamiDistribution(1.0 / 3, 1);
        setDistribution(dist);
        setCumulativeTestPoints(makeCumulativeTestPoints());
        // Computed using scipy.stats nakagami
        setCumulativeTestValues(new double[] {
            0.,                  0.00776458146673576, 0.26466318463713673,
            0.41599060641445568, 0.53633771818837206, 0.63551561797542433,
            0.71746556659624028, 0.7845448997061909,  0.83861986211366601,
            0.88141004735798412, 0.91458032800205946, 0.93973541101651015
        });
        setDensityTestValues(new double[] {
            0,                   5.17638635039373352, 0.8734262427029803,
            0.66605658341650675, 0.54432849968092045, 0.45048535438453824,
            0.3709044132031733,  0.30141976583757241, 0.24075672187548078,
            0.18853365020699897, 0.14451001716499515, 0.10829893529327907
        });
        setInverseCumulativeTestPoints(getCumulativeTestValues());
        setInverseCumulativeTestValues(getCumulativeTestPoints());
        verifyDensities();
        verifyLogDensities();
        verifyCumulativeProbabilities();
        verifySurvivalProbability();
        verifySurvivalAndCumulativeProbabilityComplement();
        verifyInverseCumulativeProbabilities();
    }

    @Test
    void testAdditionalDistribution2() {
        final NakagamiDistribution dist = new NakagamiDistribution(1.5, 2);
        setDistribution(dist);
        setCumulativeTestPoints(makeCumulativeTestPoints());
        // Computed using matlab (scipy.stats does not support the omega parameter)
        setCumulativeTestValues(new double[] {
            0,                 0.000000000488602,
            0.003839209349952, 0.029112642643164,
            0.089980307387723, 0.189070530913232,
            0.317729669663787, 0.460129965238200,
            0.599031192110653, 0.720732382881390,
            0.817659600745483, 0.888389774905287,
        });
        setDensityTestValues(new double[] {
            0,                 0.000001465806436,
            0.056899455042812, 0.208008745554258,
            0.402828269545621, 0.580491109555755,
            0.692398452624549, 0.716805620039994,
            0.660571957322857, 0.550137830087772,
            0.418105970486118, 0.291913039977849,
        });
        setInverseCumulativeTestPoints(getCumulativeTestValues());
        setInverseCumulativeTestValues(getCumulativeTestPoints());
        verifyDensities();
        verifyLogDensities();
        verifyCumulativeProbabilities();
        verifySurvivalProbability();
        verifySurvivalAndCumulativeProbabilityComplement();
        verifyInverseCumulativeProbabilities();
    }

    @Test
    void testExtremeLogDensity() {
        // XXX: Verify with more test data from a reference distribution
        final NakagamiDistribution dist = new NakagamiDistribution(0.5, 1);
        final double x = 50;
        Assertions.assertEquals(0.0, dist.density(x));
        Assertions.assertEquals(-1250.22579, dist.logDensity(x), 1e-4);
    }

    @ParameterizedTest
    @CsvSource({
        "1.2, 2.1",
        "0.5, 1",
    })
    void testParameterAccessors(double shape, double scale) {
        final NakagamiDistribution dist = new NakagamiDistribution(shape, scale);
        Assertions.assertEquals(shape, dist.getShape());
        Assertions.assertEquals(scale, dist.getScale());
    }

    @ParameterizedTest
    @CsvSource({
        "0.0, 1.0",
        "-0.1, 1.0",
        "0.5, 0.0",
        "0.5, -0.1",
    })
    void testConstructorPreconditions(double shape, double scale) {
        Assertions.assertThrows(DistributionException.class, () -> new NakagamiDistribution(shape, scale));
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

        dist = new NakagamiDistribution(1.0 / 3, 2.0);
        Assertions.assertEquals(1.032107387207478, dist.getMean(), eps);
        Assertions.assertEquals(0.934754341271753, dist.getVariance(), eps);
    }

    @Test
    void testSupport() {
        final NakagamiDistribution dist = makeDistribution();
        Assertions.assertEquals(0, dist.getSupportLowerBound());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.getSupportUpperBound());
        Assertions.assertTrue(dist.isSupportConnected());
    }
}
