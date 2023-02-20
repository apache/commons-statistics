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

package org.apache.commons.statistics.inference;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test code used in the inference section of the user guide.
 */
class UserGuideTest {
    @Test
    void testChiSquaredTest() {
        double[] expected = {0.25, 0.5, 0.25};
        long[] observed = {57, 123, 38};
        SignificanceResult result = ChiSquareTest.withDefaults()
                                                 .test(expected, observed);
        Assertions.assertEquals(0.0316148, result.getPValue(), 1e-4);
        Assertions.assertTrue(result.reject(0.05));
        Assertions.assertFalse(result.reject(0.01));
    }

    @Test
    void testTTest() {
        // Generated with:
        // from scipy.stats import norm, ttest_rel
        // x = norm(73, 10).rvs(10).astype(int)
        // y = norm(66, 12).rvs(10).astype(int)
        // ttest_rel(x, y, alternative='greater')
        // (repeated until the test was just above the 0.05 significance level)
        double[] math = {53, 69, 65, 65, 67, 79, 86, 65, 62, 69};
        double[] science = {75, 65, 68, 63, 55, 65, 73, 45, 51, 52};
        Assertions.assertEquals(68.0, Arrays.stream(math).average().getAsDouble());
        Assertions.assertEquals(61.2, Arrays.stream(science).average().getAsDouble());
        SignificanceResult result = TTest.withDefaults()
                                         .with(AlternativeHypothesis.GREATER_THAN)
                                         .pairedTest(math, science);
        Assertions.assertEquals(0.05764, result.getPValue(), 1e-5);
        Assertions.assertFalse(result.reject(0.05));
    }

    @Test
    void testGTestIntrinsic() {
        // See: http://www.biostathandbook.com/gtestgof.html

        // Allele frequencies: Mpi 90/90, Mpi 90/100, Mpi 100/100
        long[] observed = {1203, 2919, 1678};
        // Mpi 90 proportion
        double p = (2.0 * observed[0] + observed[1]) / (2 * Arrays.stream(observed).sum());
        Assertions.assertEquals(0.459, p, 1e-2);

        // Hardy-Weinberg proportions
        double[] expected = {p * p, 2 * p * (1 - p), (1 - p) * (1 - p)};
        Assertions.assertArrayEquals(new double[] {0.211, 0.497, 0.293}, expected, 5e-3);

        SignificanceResult result = GTest.withDefaults()
                                         .withDegreesOfFreedomAdjustment(1)
                                         .test(expected, observed);
        Assertions.assertEquals(1.03, result.getStatistic(), 5e-2);
        Assertions.assertEquals(0.309, result.getPValue(), 5e-3);
        Assertions.assertFalse(result.reject(0.05));
    }

    @Test
    void testAOV() {
        // See: http://www.biostathandbook.com/onewayanova.html

        double[] tillamook = {0.0571, 0.0813, 0.0831, 0.0976, 0.0817, 0.0859, 0.0735, 0.0659, 0.0923, 0.0836};
        double[] newport = {0.0873, 0.0662, 0.0672, 0.0819, 0.0749, 0.0649, 0.0835, 0.0725};
        double[] petersburg = {0.0974, 0.1352, 0.0817, 0.1016, 0.0968, 0.1064, 0.105};
        double[] magadan = {0.1033, 0.0915, 0.0781, 0.0685, 0.0677, 0.0697, 0.0764, 0.0689};
        double[] tvarminne = {0.0703, 0.1026, 0.0956, 0.0973, 0.1039, 0.1045};

        OneWayAnova.Result result = OneWayAnova.withDefaults()
                                               .test(Arrays.asList(tillamook, newport, petersburg, magadan, tvarminne));
        Assertions.assertEquals(4, result.getDFBG());
        Assertions.assertEquals(34, result.getDFWG());
        Assertions.assertEquals(0.001113, result.getMSBG(), 2e-5);
        Assertions.assertEquals(0.000159, result.getMSWG(), 1e-5);
        Assertions.assertEquals(7.12, result.getStatistic(), 1e-2);
        Assertions.assertEquals(2.8e-4, result.getPValue(), 1e-5);
        Assertions.assertTrue(result.reject(0.001));
    }
}
