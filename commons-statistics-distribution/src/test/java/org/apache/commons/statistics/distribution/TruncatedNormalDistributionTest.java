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
 * Test class for {@link TruncatedNormalDistribution}.
 * All test values were computed using Python with SciPy v1.6.0.
 */
class TruncatedNormalDistributionTest extends ContinuousDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-7);
    }

    //-------------- Implementations for abstract methods ----------------------

    /** {@inheritDoc} */
    @Override
    public ContinuousDistribution makeDistribution() {
        return new TruncatedNormalDistribution(1.9, 1.3, -1.1, 3.4);
    }

    /** {@inheritDoc} */
    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[]{-1.1, -1.09597275767544, -1.0609616183922, -0.79283350106842,
                            -0.505331829887808, -0.192170173599874, 0.21173317261645,
                            0.925791281910463, 1.71399518338879, 2.43413009451536, 2.94473113856785,
                            3.15310057075828, 3.27036798398733, 3.34641874981679, 3.39452729074341,
                            3.39945153287941, 3.4};
    }

    /** {@inheritDoc} */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[]{0, 0.0001, 0.001, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 0.75, 0.9, 0.95,
                            0.975, 0.99, 0.999, 0.9999, 1};
    }

    /** {@inheritDoc} */
    @Override
    public double[] makeDensityTestValues() {
        return new double[]{0.0247422752302618, 0.0249196707321102, 0.0265057408263321,
                            0.0415071096500185, 0.0640403254340905, 0.0971457789636,
                            0.152622492901864, 0.267853863255995, 0.35107475879338, 0.325977522502844,
                            0.25680502248913, 0.222886115806507, 0.203494915087054, 0.190997946666992,
                            0.183167918885238, 0.182370706542209, 0.182281965373914};
    }

    //-------------------- Additional test cases -------------------------------

    /**
     * Configures new test values and runs relevant tests in this class and {@link ContinuousDistributionAbstractTest}.
     *
     * @param distribution distribution to test with.
     * @param cumulativeTestPoints test points for the cumulative probability and density.
     * @param cumulativeTestValues expected values for the cumulative probability.
     * @param densityTestValues expected values for the density.
     * @param mean expected mean.
     * @param variance expected variance.
     */
    private void testAdditionalDistribution(
            TruncatedNormalDistribution distribution,
            double[] cumulativeTestPoints,
            double[] cumulativeTestValues,
            double[] densityTestValues,
            double mean,
            double variance) {
        setDistribution(distribution);
        setCumulativeTestPoints(cumulativeTestPoints);
        setCumulativeTestValues(cumulativeTestValues);
        setDensityTestValues(densityTestValues);
        // Use reverse mapping
        setInverseCumulativeTestPoints(cumulativeTestValues);
        setInverseCumulativeTestValues(cumulativeTestPoints);
        // Use the log(density)
        setLogDensityTestValues(Arrays.stream(densityTestValues).map(Math::log).toArray());

        testMoments(distribution, mean, variance);

        testConsistency();
        testSampler();
        testOutsideSupport();
        testDensities();
        testLogDensities();
        testOutsideSupport();
        testInverseCumulativeProbabilities();
        testDensityIntegrals();
        testCumulativeProbabilities();
        testIsSupportConnected();
        testPrecondition1();
        testPrecondition2();
        testPrecondition3();
    }

    /** Test a one-sided truncation with a lower tail. */
    @Test
    void testOneSidedLowerTail() {
        testAdditionalDistribution(
                new TruncatedNormalDistribution(12, 2.4, Double.NEGATIVE_INFINITY, 7.1),
                new double[]{Double.NEGATIVE_INFINITY, 2.20249292901062, 3.00511196424565, 3.80773099948069,
                             4.61035003471573, 5.41296906995077, 6.21558810518581, 7.01820714042084, 7.1},
                new double[]{0, 0.00108276414971883, 0.00433032247708514, 0.0155754809421998, 0.0504271331622245,
                        0.147106879016387, 0.387159643321778, 0.920668099879139, 1},
                new double[]{0, 0.00194181137319567, 0.00719165311538403, 0.0238165586714952, 0.0705273999981105,
                             0.186752027463317, 0.442182309739316, 0.936194292830215, 1.00423817618302},
                6.21558810518581,
                0.644197315721623);
    }

    /** Test a one-sided truncation with an upper tail. */
    @Test
    void testOneSidedUpperTail() {
        testAdditionalDistribution(
                new TruncatedNormalDistribution(-9.6, 17, -15, Double.POSITIVE_INFINITY),
                new double[]{-15, -10.5314720401464, 0.723583450712814, 11.978638941572, 23.2336944324312,
                             34.4887499232902, 45.7438054141485, 56.9988609050074, Double.POSITIVE_INFINITY},
                new double[]{0, 0.164539974698729, 0.564800349576255, 0.836443289017693, 0.957226746540945,
                             0.992394081771774, 0.999093968560336, 0.999928403010774, 1},
                new double[]{0.035721742043989, 0.0375137766818179, 0.0312438063187719, 0.0167870518464031,
                             0.00581865051705663, 0.00130109036611494, 0.000187685186297558, 1.74658560715427e-05, 0},
                0.723583450712812,
                126.676274102319);
    }

    /** Test no truncation. */
    @Test
    void testNoTruncation() {
        testAdditionalDistribution(
                new TruncatedNormalDistribution(3, 1.1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                new double[]{Double.NEGATIVE_INFINITY, -2.5, -1.4, -0.300000000000001, 0.799999999999999, 1.9, 3,
                             4.1, 5.2, 6.3, 7.4, 8.49999999996719, Double.POSITIVE_INFINITY},
                new double[]{0, 2.86651571879193e-07, 3.16712418331199e-05, 0.00134989803163009, 0.0227501319481792,
                             0.158655253931457, 0.5, 0.841344746068543, 0.977249868051821, 0.99865010196837,
                             0.999968328758167, 0.999999713348428, 1},
                new double[]{0, 1.35156319521299e-06, 0.000121663841604441, 0.00402895310176182, 0.0490826968301709,
                             0.219973385926494, 0.362674800364939, 0.219973385926494, 0.0490826968301709,
                             0.00402895310176184, 0.000121663841604441, 1.35156319541454e-06, 0},
                3,
                1.21);
    }

    /** Test a truncation range that is completely below the mean. */
    @Test
    void testLowerTailOnly() {
        testAdditionalDistribution(
                new TruncatedNormalDistribution(0, 1, Double.NEGATIVE_INFINITY, -5),
                new double[]{Double.NEGATIVE_INFINITY, -6.09061174025149, -5.90979018562636, -5.72896863100123,
                             -5.54814707637611, -5.36732552175098, -5.18650396712585, -5.00568241250073, -5},
                new double[]{0, 0.00196196451357246, 0.00597491488512203, 0.0176247203066899, 0.0503595643590926,
                             0.139390045971621, 0.373761183487683, 0.970943041215359, 1},
                new double[]{0, 0.0122562922051934, 0.0362705138555484, 0.103883943928261, 0.287967362544455,
                             0.772570689127439, 2.00601097433085, 5.04113700754108, 5.18650396712585},
                -5.18650396712585,
                0.0326964346170475);
    }

    /** Test a truncation range that is completely above the mean. */
    @Test
    void testUpperTailOnly() {
        testAdditionalDistribution(
                new TruncatedNormalDistribution(0, 1, 5, Double.POSITIVE_INFINITY),
                new double[]{5, 5.00568241254803, 5.18650396728068, 5.36732552203467, 5.54814707752324,
                             5.72896863159791, 5.90979018980065, 6.09061174555624, Double.POSITIVE_INFINITY},
                new double[]{0, 0.0290569590230917, 0.626238816822898, 0.860609954247549, 0.949640435971243,
                             0.982375279755296, 0.994025085266282, 0.998038035551444, 1},
                new double[]{5.18650396712585, 5.04113700634745, 2.00601097272001, 0.772570687951075,
                             0.287967360711704, 0.103883943573147, 0.0362705129607846, 0.0122562918092027, 0},
                5.18650396712585,
                0.0326964346170475);
    }

    /** Test a narrow truncation range. */
    @Test
    void testNarrowTruncatedRange() {
        testAdditionalDistribution(
                new TruncatedNormalDistribution(7.1, 9.9, 7.0999999, 7.1000001),
                new double[]{7.0999999, 7.1, 7.1000001},
                new double[]{0, 0.5, 1},
                new double[]{5000000.00238838, 5000000.00238838, 5000000.00238838},
                7.1,
                1.13584123966337e-07);
    }

    /** Test mean and variance moments. */
    @Test
    void testMoments() {
        final double mean = 1.63375792365723;
        final double variance = 1.03158703914439;
        testMoments(makeDistribution(), mean, variance);
    }

    private void testMoments(ContinuousDistribution distribution, double mean, double variance) {
        Assertions.assertEquals(mean, distribution.getMean(), getTolerance());
        Assertions.assertEquals(variance, distribution.getVariance(), getTolerance());
    }

    /** Test constructor precondition when the standard deviation is less than or equal to 0. */
    @Test
    void testConstructorSdPrecondition() {
        Assertions.assertThrows(DistributionException.class, () -> new TruncatedNormalDistribution(1, 0, -1, 1));
    }

    /** Test constructor precondition when the lower bound is greater than the upper bound. */
    @Test
    void testConstructorBoundsPrecondition() {
        Assertions.assertThrows(DistributionException.class, () -> new TruncatedNormalDistribution(1, 1, 1, -1));
    }
}
