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

/**
 * Test cases for BinomialDistribution. Extends DiscreteDistributionAbstractTest.
 * See class javadoc for DiscreteDistributionAbstractTest for details.
 */
class BinomialDistributionTest extends DiscreteDistributionAbstractTest {

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    void customSetUp() {
        setTolerance(1e-12);
    }

    //-------------- Implementations for abstract methods ----------------------

    @Override
    public DiscreteDistribution makeDistribution() {
        return new BinomialDistribution(10, 0.70);
    }

    @Override
    public int[] makeProbabilityTestPoints() {
        return new int[] {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    }

    @Override
    public double[] makeProbabilityTestValues() {
        // Reference values are from R, version 2.15.3.
        return new double[] {0d, 0.0000059049d, 0.000137781d, 0.0014467005,
                             0.009001692, 0.036756909, 0.1029193452, 0.200120949, 0.266827932,
                             0.2334744405, 0.121060821, 0.0282475249, 0d};
    }

    @Override
    public int[] makeCumulativeTestPoints() {
        return makeProbabilityTestPoints();
    }

    @Override
    public double[] makeCumulativeTestValues() {
        // Reference values are from R, version 2.15.3.
        return new double[] {0d, 5.9049e-06, 0.0001436859, 0.0015903864, 0.0105920784,  0.0473489874,
                             0.1502683326, 0.3503892816, 0.6172172136, 0.8506916541, 0.9717524751, 1d, 1d};
    }

    @Override
    public double[] makeInverseCumulativeTestPoints() {
        return new double[] {0, 0.001d, 0.010d, 0.025d, 0.050d, 0.100d,
                             0.999d, 0.990d, 0.975d, 0.950d, 0.900d, 1d};
    }

    @Override
    public int[] makeInverseCumulativeTestValues() {
        return new int[] {0, 2, 3, 4, 5, 5, 10, 10, 10, 9, 9, 10};
    }

    //-------------------- Additional test cases -------------------------------

    /** Test case n = 10, p = 0.3. */
    @Test
    void testSmallPValue() {
        final BinomialDistribution dist = new BinomialDistribution(10, 0.3);
        setDistribution(dist);
        setCumulativeTestPoints(makeCumulativeTestPoints());
        // computed using R version 3.4.4
        setCumulativeTestValues(new double[] {0.00000000000000000000, 0.02824752489999998728, 0.14930834590000002793,
            0.38278278639999974153, 0.64961071840000017552, 0.84973166740000016794, 0.95265101260000006889,
            0.98940792160000001765, 0.99840961360000002323, 0.99985631409999997654, 0.99999409509999992451,
            1.00000000000000000000, 1.00000000000000000000});
        setProbabilityTestPoints(makeProbabilityTestPoints());
        setProbabilityTestValues(new double[] {0.0000000000000000000e+00, 2.8247524899999980341e-02,
            1.2106082099999991575e-01, 2.3347444049999999116e-01, 2.6682793199999993439e-01, 2.0012094900000007569e-01,
            1.0291934520000002584e-01, 3.6756909000000004273e-02, 9.0016919999999864960e-03, 1.4467005000000008035e-03,
            1.3778099999999990615e-04, 5.9048999999999949131e-06, 0.0000000000000000000e+00});
        setInverseCumulativeTestPoints(makeInverseCumulativeTestPoints());
        setInverseCumulativeTestValues(new int[] {0, 0, 0, 0, 1, 1, 8, 7, 6, 5, 5, 10});
        verifyProbabilities();
        verifyLogProbabilities();
        verifyCumulativeProbabilities();
        verifySurvivalProbability();
        verifySurvivalAndCumulativeProbabilityComplement();
        verifyInverseCumulativeProbabilities();
    }

    /** Test degenerate case p = 0 */
    @Test
    void testDegenerate0() {
        final BinomialDistribution dist = new BinomialDistribution(5, 0.0d);
        setDistribution(dist);
        setCumulativeTestPoints(new int[] {-1, 0, 1, 5, 10});
        setCumulativeTestValues(new double[] {0d, 1d, 1d, 1d, 1d});
        setProbabilityTestPoints(new int[] {-1, 0, 1, 10, 11});
        setProbabilityTestValues(new double[] {0d, 1d, 0d, 0d, 0d});
        setInverseCumulativeTestPoints(new double[] {0.1d, 0.5d});
        setInverseCumulativeTestValues(new int[] {0, 0});
        verifyProbabilities();
        verifyLogProbabilities();
        verifyCumulativeProbabilities();
        verifySurvivalProbability();
        verifySurvivalAndCumulativeProbabilityComplement();
        verifyInverseCumulativeProbabilities();
        Assertions.assertEquals(0, dist.getSupportLowerBound());
        Assertions.assertEquals(0, dist.getSupportUpperBound());
    }

    /** Test degenerate case p = 1 */
    @Test
    void testDegenerate1() {
        final BinomialDistribution dist = new BinomialDistribution(5, 1.0d);
        setDistribution(dist);
        setCumulativeTestPoints(new int[] {-1, 0, 1, 2, 5, 10});
        setCumulativeTestValues(new double[] {0d, 0d, 0d, 0d, 1d, 1d});
        setProbabilityTestPoints(new int[] {-1, 0, 1, 2, 5, 10});
        setProbabilityTestValues(new double[] {0d, 0d, 0d, 0d, 1d, 0d});
        setInverseCumulativeTestPoints(new double[] {0.1d, 0.5d});
        setInverseCumulativeTestValues(new int[] {5, 5});
        verifyProbabilities();
        verifyLogProbabilities();
        verifyCumulativeProbabilities();
        verifySurvivalProbability();
        verifySurvivalAndCumulativeProbabilityComplement();
        verifyInverseCumulativeProbabilities();
        Assertions.assertEquals(5, dist.getSupportLowerBound());
        Assertions.assertEquals(5, dist.getSupportUpperBound());
    }

    /** Test degenerate case n = 0 */
    @Test
    void testDegenerate2() {
        final BinomialDistribution dist = new BinomialDistribution(0, 0.01d);
        setDistribution(dist);
        setCumulativeTestPoints(new int[] {-1, 0, 1, 2, 5, 10});
        setCumulativeTestValues(new double[] {0d, 1d, 1d, 1d, 1d, 1d});
        setProbabilityTestPoints(new int[] {-1, 0, 1, 2, 5, 10});
        setProbabilityTestValues(new double[] {0d, 1d, 0d, 0d, 0d, 0d});
        setInverseCumulativeTestPoints(new double[] {0.1d, 0.5d});
        setInverseCumulativeTestValues(new int[] {0, 0});
        verifyProbabilities();
        verifyLogProbabilities();
        verifyCumulativeProbabilities();
        verifySurvivalProbability();
        verifySurvivalAndCumulativeProbabilityComplement();
        verifyInverseCumulativeProbabilities();
        Assertions.assertEquals(0, dist.getSupportLowerBound());
        Assertions.assertEquals(0, dist.getSupportUpperBound());
    }

    @Test
    void testParameterAccessors() {
        for (final int n : new int[] {11, 42, 999}) {
            for (final double p : new double[] {0.1, 0.456, 0.999}) {
                final BinomialDistribution dist = new BinomialDistribution(n, p);
                Assertions.assertEquals(n, dist.getNumberOfTrials());
                Assertions.assertEquals(p, dist.getProbabilityOfSuccess());
            }
        }
    }

    @Test
    void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new BinomialDistribution(-1, 0.1));
    }

    @Test
    void testConstructorPrecondition2() {
        Assertions.assertThrows(DistributionException.class, () -> new BinomialDistribution(10, -0.1));
    }

    @Test
    void testConstructorPrecondition3() {
        Assertions.assertThrows(DistributionException.class, () -> new BinomialDistribution(10, 1.1));
    }

    @Test
    void testMoments() {
        final double tol = 1e-9;
        BinomialDistribution dist;

        dist = new BinomialDistribution(10, 0.5);
        Assertions.assertEquals(10d * 0.5d, dist.getMean(), tol);
        Assertions.assertEquals(10d * 0.5d * 0.5d, dist.getVariance(), tol);

        dist = new BinomialDistribution(30, 0.3);
        Assertions.assertEquals(30d * 0.3d, dist.getMean(), tol);
        Assertions.assertEquals(30d * 0.3d * (1d - 0.3d), dist.getVariance(), tol);
    }

    @Test
    void testMath718() {
        // for large trials the evaluation of ContinuedFraction was inaccurate
        // do a sweep over several large trials to test if the current implementation is
        // numerically stable.

        for (int trials = 500000; trials < 20000000; trials += 100000) {
            final BinomialDistribution dist = new BinomialDistribution(trials, 0.5);
            final int p = dist.inverseCumulativeProbability(0.5);
            Assertions.assertEquals(trials / 2, p);
        }
    }

    @Test
    void testHighPrecisionCumulativeProbabilities() {
        // computed using R version 3.4.4
        setDistribution(new BinomialDistribution(100, 0.99));
        setCumulativePrecisionTestPoints(new int[] {82, 81});
        setCumulativePrecisionTestValues(new double[] {1.4061271955993513664e-17, 6.1128083336354843707e-19});
        verifyCumulativeProbabilityPrecision();
    }

    @Test
    void testHighPrecisionSurvivalProbabilities() {
        // computed using R version 3.4.4
        setDistribution(new BinomialDistribution(100, 0.01));
        setSurvivalPrecisionTestPoints(new int[] {18, 19});
        setSurvivalPrecisionTestValues(new double[] {6.1128083336353977038e-19, 2.4944165604029235392e-20});
        verifySurvivalProbabilityPrecision();
    }
}
