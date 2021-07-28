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
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for {@link DiscreteDistribution} tests.
 * <p>
 * To create a concrete test class for an integer distribution implementation,
 *  implement makeDistribution() to return a distribution instance to use in
 *  tests and each of the test data generation methods below.  In each case, the
 *  test points and test values arrays returned represent parallel arrays of
 *  inputs and expected values for the distribution returned by makeDistribution().
 *  <p>
 *  makeDensityTestPoints() -- arguments used to test probability density calculation
 *  makeDensityTestValues() -- expected probability densities
 *  makeCumulativeTestPoints() -- arguments used to test cumulative probabilities
 *  makeCumulativeTestValues() -- expected cumulative probabilites
 *  makeInverseCumulativeTestPoints() -- arguments used to test inverse cdf evaluation
 *  makeInverseCumulativeTestValues() -- expected inverse cdf values
 * <p>
 * If the discrete distribution provides higher precision implementations of cumulativeProbability
 * and/or survivalProbability, the following methods should be implemented to provide testing.
 * To use these tests, calculate the cumulativeProbability and survivalProbability such that their naive
 * complement is exceptionally close to `1` and consequently could lose precision due to floating point
 * arithmetic.
 *
 * NOTE: The default high-precision threshold is 1e-22.
 * <pre>
 * makeCumulativePrecisionTestPoints() -- high precision test inputs
 * makeCumulativePrecisionTestValues() -- high precision expected results
 * makeSurvivalPrecisionTestPoints() -- high precision test inputs
 * makeSurvivalPrecisionTestValues() -- high precision expected results
 * </pre>
 * <p>
 *  To implement additional test cases with different distribution instances and test data,
 *  use the setXxx methods for the instance data in test cases and call the verifyXxx methods
 *  to verify results.
 */
abstract class DiscreteDistributionAbstractTest {

//-------------------- Private test instance data -------------------------
    /** Discrete distribution instance used to perform tests. */
    private DiscreteDistribution distribution;

    /** Tolerance used in comparing expected and returned values. */
    private double tolerance = 1e-12;

    /** Tolerance used in high precision tests. */
    private double highPrecisionTolerance = 1e-22;

    /** Arguments used to test probability density calculations. */
    private int[] densityTestPoints;

    /** Values used to test probability density calculations. */
    private double[] densityTestValues;

    /** Values used to test logarithmic probability density calculations. */
    private double[] logDensityTestValues;

    /** Arguments used to test cumulative probability density calculations. */
    private int[] cumulativeTestPoints;

    /** Values used to test cumulative probability density calculations. */
    private double[] cumulativeTestValues;

    /** Arguments used to test cumulative probability precision, effectively any x where 1-cdf(x) would result in 1. */
    private int[] cumulativePrecisionTestPoints;

    /** Values used to test cumulative probability precision, usually exceptionally tiny values. */
    private double[] cumulativePrecisionTestValues;

    /** Arguments used to test survival probability precision, effectively any x where 1-sf(x) would result in 1. */
    private int[] survivalPrecisionTestPoints;

    /** Values used to test survival probability precision, usually exceptionally tiny values. */
    private double[] survivalPrecisionTestValues;

    /** Arguments used to test inverse cumulative probability density calculations. */
    private double[] inverseCumulativeTestPoints;

    /** Values used to test inverse cumulative probability density calculations. */
    private int[] inverseCumulativeTestValues;

    //-------------------- Abstract methods -----------------------------------

    /** Creates the default discrete distribution instance to use in tests. */
    public abstract DiscreteDistribution makeDistribution();

    /** Creates the default probability density test input values. */
    public abstract int[] makeDensityTestPoints();

    /** Creates the default probability density test expected values. */
    public abstract double[] makeDensityTestValues();

    /** Creates the default logarithmic probability density test expected values.
     *
     * The default implementation simply computes the logarithm of all the values in
     * {@link #makeDensityTestValues()}.
     *
     * @return double[] the default logarithmic probability density test expected values.
     */
    public double[] makeLogDensityTestValues() {
        return Arrays.stream(makeDensityTestValues()).map(Math::log).toArray();
    }

    /** Creates the default cumulative probability density test input values. */
    public abstract int[] makeCumulativeTestPoints();

    /** Creates the default cumulative probability density test expected values. */
    public abstract double[] makeCumulativeTestValues();

    /** Creates the default cumulative probability precision test input values. */
    public int[] makeCumulativePrecisionTestPoints() {
        return new int[0];
    }

    /**
     * Creates the default cumulative probability precision test expected values.
     * Note: The default threshold is 1e-22, any expected values with much higher precision may
     *       not test the desired results without increasing precision threshold.
     */
    public double[] makeCumulativePrecisionTestValues() {
        return new double[0];
    }

    /** Creates the default survival probability precision test input values. */
    public int[] makeSurvivalPrecisionTestPoints() {
        return new int[0];
    }

    /**
     * Creates the default survival probability precision test expected values.
     * Note: The default threshold is 1e-22, any expected values with much higher precision may
     *       not test the desired results without increasing precision threshold.
     */
    public double[] makeSurvivalPrecisionTestValues() {
        return new double[0];
    }

    /** Creates the default inverse cumulative probability test input values. */
    public abstract double[] makeInverseCumulativeTestPoints();

    /** Creates the default inverse cumulative probability density test expected values. */
    public abstract int[] makeInverseCumulativeTestValues();

    //-------------------- Setup / tear down ----------------------------------

    /**
     * Setup sets all test instance data to default values.
     */
    @BeforeEach
    void setUp() {
        distribution = makeDistribution();
        densityTestPoints = makeDensityTestPoints();
        densityTestValues = makeDensityTestValues();
        logDensityTestValues = makeLogDensityTestValues();
        cumulativeTestPoints = makeCumulativeTestPoints();
        cumulativeTestValues = makeCumulativeTestValues();
        cumulativePrecisionTestPoints = makeCumulativePrecisionTestPoints();
        cumulativePrecisionTestValues = makeCumulativePrecisionTestValues();
        survivalPrecisionTestPoints = makeSurvivalPrecisionTestPoints();
        survivalPrecisionTestValues = makeSurvivalPrecisionTestValues();
        inverseCumulativeTestPoints = makeInverseCumulativeTestPoints();
        inverseCumulativeTestValues = makeInverseCumulativeTestValues();
    }

    /**
     * Cleans up test instance data
     */
    @AfterEach
    void tearDown() {
        distribution = null;
        densityTestPoints = null;
        densityTestValues = null;
        logDensityTestValues = null;
        cumulativeTestPoints = null;
        cumulativeTestValues = null;
        cumulativePrecisionTestPoints = null;
        cumulativePrecisionTestValues = null;
        survivalPrecisionTestPoints = null;
        survivalPrecisionTestValues = null;
        inverseCumulativeTestPoints = null;
        inverseCumulativeTestValues = null;
    }

    //-------------------- Verification methods -------------------------------

    /**
     * Verifies that probability density calculations match expected values
     * using current test instance data.
     */
    protected void verifyDensities() {
        for (int i = 0; i < densityTestPoints.length; i++) {
            final int testPoint = densityTestPoints[i];
            Assertions.assertEquals(densityTestValues[i],
                distribution.probability(testPoint), getTolerance(),
                () -> "Incorrect density value returned for " + testPoint);
        }
    }

    /**
     * Verifies that logarithmic probability density calculations match expected values
     * using current test instance data.
     */
    protected void verifyLogDensities() {
        for (int i = 0; i < densityTestPoints.length; i++) {
            final int testPoint = densityTestPoints[i];
            Assertions.assertEquals(logDensityTestValues[i],
                distribution.logProbability(testPoint), tolerance,
                () -> "Incorrect log density value returned for " + testPoint);
        }
    }

    /**
     * Verifies that cumulative probability density calculations match expected values
     * using current test instance data.
     */
    protected void verifyCumulativeProbabilities() {
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            final int testPoint = cumulativeTestPoints[i];
            Assertions.assertEquals(cumulativeTestValues[i],
                distribution.cumulativeProbability(testPoint), getTolerance(),
                () -> "Incorrect cumulative probability value returned for " + testPoint);
        }
    }

    protected void verifySurvivalProbability() {
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            final int x = cumulativeTestPoints[i];
            Assertions.assertEquals(
                1 - cumulativeTestValues[i],
                distribution.survivalProbability(cumulativeTestPoints[i]),
                getTolerance(),
                () -> "Incorrect survival probability value returned for " + x);
        }
    }

    protected void verifySurvivalAndCumulativeProbabilityComplement() {
        for (final int x : cumulativeTestPoints) {
            Assertions.assertEquals(
                1.0,
                distribution.survivalProbability(x) + distribution.cumulativeProbability(x),
                getTolerance(),
                () -> "survival + cumulative probability were not close to 1.0 for " + x);
        }
    }

    /**
     * Verifies that survival is simply not 1-cdf by testing calculations that would underflow that calculation and
     * result in an inaccurate answer.
     */
    protected void verifySurvivalProbabilityPrecision() {
        for (int i = 0; i < survivalPrecisionTestPoints.length; i++) {
            final int x = survivalPrecisionTestPoints[i];
            Assertions.assertEquals(
                survivalPrecisionTestValues[i],
                distribution.survivalProbability(x),
                getHighPrecisionTolerance(),
                () -> "survival probability is not precise for " + x);
        }
    }

    /**
     * Verifies that CDF is simply not 1-survival function by testing values that would result with inaccurate results
     * if simply calculating 1-survival function.
     */
    protected void verifyCumulativeProbabilityPrecision() {
        for (int i = 0; i < cumulativePrecisionTestPoints.length; i++) {
            final int x = cumulativePrecisionTestPoints[i];
            Assertions.assertEquals(
                cumulativePrecisionTestValues[i],
                distribution.cumulativeProbability(x),
                getHighPrecisionTolerance(),
                () -> "cumulative probability is not precise for " + x);
        }
    }

    /**
     * Verifies that inverse cumulative probability density calculations match expected values
     * using current test instance data.
     */
    protected void verifyInverseCumulativeProbabilities() {
        for (int i = 0; i < inverseCumulativeTestPoints.length; i++) {
            final double testPoint = inverseCumulativeTestPoints[i];
            Assertions.assertEquals(inverseCumulativeTestValues[i],
                distribution.inverseCumulativeProbability(testPoint),
                () -> "Incorrect inverse cumulative probability value returned for " + testPoint);
        }
    }

    //------------------------ Default test cases -----------------------------

    /**
     * Verifies that probability density calculations match expected values
     * using default test instance data.
     */
    @Test
    void testDensities() {
        verifyDensities();
    }

    /**
     * Verifies that logarithmic probability density calculations match expected values
     * using default test instance data.
     */
    @Test
    void testLogDensities() {
        verifyLogDensities();
    }

    /**
     * Verifies that cumulative probability density calculations match expected values
     * using default test instance data.
     */
    @Test
    void testCumulativeProbabilities() {
        verifyCumulativeProbabilities();
    }

    @Test
    void testSurvivalProbability() {
        verifySurvivalProbability();
    }

    @Test
    void testSurvivalAndCumulativeProbabilitiesAreComplementary() {
        verifySurvivalAndCumulativeProbabilityComplement();
    }

    @Test
    void testCumulativeProbabilityPrecision() {
        verifyCumulativeProbabilityPrecision();
    }

    @Test
    void testSurvivalProbabilityPrecision() {
        verifySurvivalProbabilityPrecision();
    }

    /**
     * Verifies that inverse cumulative probability density calculations match expected values
     * using default test instance data.
     */
    @Test
    void testInverseCumulativeProbabilities() {
        verifyInverseCumulativeProbabilities();
    }

    @Test
    void testConsistencyAtSupportBounds() {
        final int lower = distribution.getSupportLowerBound();
        Assertions.assertEquals(0.0, distribution.cumulativeProbability(lower - 1), 0.0,
                "Cumulative probability must be 0 below support lower bound.");
        Assertions.assertEquals(distribution.probability(lower), distribution.cumulativeProbability(lower), getTolerance(),
                "Cumulative probability of support lower bound must be equal to probability mass at this point.");
        Assertions.assertEquals(1.0, distribution.survivalProbability(lower - 1), 0.0,
            "Survival probability must be 1.0 below support lower bound.");
        Assertions.assertEquals(lower, distribution.inverseCumulativeProbability(0.0),
                "Inverse cumulative probability of 0 must be equal to support lower bound.");

        final int upper = distribution.getSupportUpperBound();
        if (upper != Integer.MAX_VALUE) {
            Assertions.assertEquals(1.0, distribution.cumulativeProbability(upper), 0.0,
                    "Cumulative probability of support upper bound must be equal to 1.");
            Assertions.assertEquals(0.0, distribution.survivalProbability(upper), 0.0,
                    "Survival probability of support upper bound must be equal to 0.");
        }
        Assertions.assertEquals(upper, distribution.inverseCumulativeProbability(1.0),
                "Inverse cumulative probability of 1 must be equal to support upper bound.");
    }

    @Test
    void testPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> distribution.probability(1, 0));
    }
    @Test
    void testPrecondition2() {
        Assertions.assertThrows(DistributionException.class, () -> distribution.inverseCumulativeProbability(-1));
    }
    @Test
    void testPrecondition3() {
        Assertions.assertThrows(DistributionException.class, () -> distribution.inverseCumulativeProbability(2));
    }

    /**
     * Test sampling.
     */
    @Test
    void testSampling() {
        final int[] densityPoints = makeDensityTestPoints();
        final double[] densityValues = makeDensityTestValues();
        final int sampleSize = 1000;
        final int length = TestUtils.eliminateZeroMassPoints(densityPoints, densityValues);
        final AbstractDiscreteDistribution dist = (AbstractDiscreteDistribution) makeDistribution();
        final double[] expectedCounts = new double[length];
        final long[] observedCounts = new long[length];
        for (int i = 0; i < length; i++) {
            expectedCounts[i] = sampleSize * densityValues[i];
        }
        // Use fixed seed.
        final DiscreteDistribution.Sampler sampler =
            dist.createSampler(RandomSource.create(RandomSource.WELL_512_A, 1000));
        final int[] sample = TestUtils.sample(sampleSize, sampler);
        for (int i = 0; i < sampleSize; i++) {
            for (int j = 0; j < length; j++) {
                if (sample[i] == densityPoints[j]) {
                    observedCounts[j]++;
                }
            }
        }
        TestUtils.assertChiSquareAccept(densityPoints, expectedCounts, observedCounts, 0.001);
    }

    /**
     * Test if the distribution is support connected. This test exists to ensure the support
     * connected property is tested.
     */
    @Test
    void testIsSupportConnected() {
        Assertions.assertEquals(isSupportConnected(), distribution.isSupportConnected());
    }

    //------------------ Getters / Setters for test instance data -----------
    /**
     * @return Returns the cumulativeTestPoints.
     */
    protected int[] getCumulativeTestPoints() {
        return cumulativeTestPoints;
    }

    /**
     * @param cumulativeTestPoints The cumulativeTestPoints to set.
     */
    protected void setCumulativeTestPoints(int[] cumulativeTestPoints) {
        this.cumulativeTestPoints = cumulativeTestPoints;
    }

    /**
     * @return Returns the cumulativeTestValues.
     */
    protected double[] getCumulativeTestValues() {
        return cumulativeTestValues;
    }

    /**
     * @param cumulativeTestValues The cumulativeTestValues to set.
     */
    protected void setCumulativeTestValues(double[] cumulativeTestValues) {
        this.cumulativeTestValues = cumulativeTestValues;
    }

    /**
     * @return Returns the densityTestPoints.
     */
    protected int[] getDensityTestPoints() {
        return densityTestPoints;
    }

    /**
     * @param densityTestPoints The densityTestPoints to set.
     */
    protected void setDensityTestPoints(int[] densityTestPoints) {
        this.densityTestPoints = densityTestPoints;
    }

    /**
     * @return Returns the densityTestValues.
     */
    protected double[] getDensityTestValues() {
        return densityTestValues;
    }

    /**
     * Set the density test values.
     * For convenience this recomputes the log density test values using {@link Math#log(double)}.
     *
     * @param densityTestValues The densityTestValues to set.
     */
    protected void setDensityTestValues(double[] densityTestValues) {
        this.densityTestValues = densityTestValues;
        logDensityTestValues = Arrays.stream(densityTestValues).map(Math::log).toArray();
    }

    /**
     * @return Returns the logDensityTestValues.
     */
    protected double[] getLogDensityTestValues() {
        return logDensityTestValues;
    }

    /**
     * @param logDensityTestValues The logDensityTestValues to set.
     */
    protected void setLogDensityTestValues(double[] logDensityTestValues) {
        this.logDensityTestValues = logDensityTestValues;
    }

    /**
     * @return Returns the cumulativePrecisionTestPoints.
     */
    protected int[] getCumulativePrecisionTestPoints() {
        return cumulativePrecisionTestPoints;
    }

    /**
     * @param cumulativePrecisionTestPoints The cumulativePrecisionTestPoints to set.
     */
    protected void setCumulativePrecisionTestPoints(int[] cumulativePrecisionTestPoints) {
        this.cumulativePrecisionTestPoints = cumulativePrecisionTestPoints;
    }

    /**
     * @return Returns the cumulativePrecisionTestValues.
     */
    protected double[] getCumulativePrecisionTestValues() {
        return cumulativePrecisionTestValues;
    }

    /**
     * @param cumulativePrecisionTestValues The cumulativePrecisionTestValues to set.
     */
    protected void setCumulativePrecisionTestValues(double[] cumulativePrecisionTestValues) {
        this.cumulativePrecisionTestValues = cumulativePrecisionTestValues;
    }

    /**
     * @return Returns the survivalPrecisionTestPoints.
     */
    protected int[] getSurvivalPrecisionTestPoints() {
        return survivalPrecisionTestPoints;
    }

    /**
     * @param survivalPrecisionTestPoints The survivalPrecisionTestPoints to set.
     */
    protected void setSurvivalPrecisionTestPoints(int[] survivalPrecisionTestPoints) {
        this.survivalPrecisionTestPoints = survivalPrecisionTestPoints;
    }

    /**
     * @return Returns the survivalPrecisionTestValues.
     */
    protected double[] getSurvivalPrecisionTestValues() {
        return survivalPrecisionTestValues;
    }

    /**
     * @param survivalPrecisionTestValues The survivalPrecisionTestValues to set.
     */
    protected void setSurvivalPrecisionTestValues(double[] survivalPrecisionTestValues) {
        this.survivalPrecisionTestValues = survivalPrecisionTestValues;
    }

    /**
     * @return Returns the distribution.
     */
    protected DiscreteDistribution getDistribution() {
        return distribution;
    }

    /**
     * @param distribution The distribution to set.
     */
    protected void setDistribution(DiscreteDistribution distribution) {
        this.distribution = distribution;
    }

    /**
     * @return Returns the inverseCumulativeTestPoints.
     */
    protected double[] getInverseCumulativeTestPoints() {
        return inverseCumulativeTestPoints;
    }

    /**
     * @param inverseCumulativeTestPoints The inverseCumulativeTestPoints to set.
     */
    protected void setInverseCumulativeTestPoints(double[] inverseCumulativeTestPoints) {
        this.inverseCumulativeTestPoints = inverseCumulativeTestPoints;
    }

    /**
     * @return Returns the inverseCumulativeTestValues.
     */
    protected int[] getInverseCumulativeTestValues() {
        return inverseCumulativeTestValues;
    }

    /**
     * @param inverseCumulativeTestValues The inverseCumulativeTestValues to set.
     */
    protected void setInverseCumulativeTestValues(int[] inverseCumulativeTestValues) {
        this.inverseCumulativeTestValues = inverseCumulativeTestValues;
    }

    /**
     * @return Returns the tolerance.
     */
    protected double getTolerance() {
        return tolerance;
    }

    /**
     * @param tolerance The tolerance to set.
     */
    protected void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    /**
     * @return Returns the high precision tolerance.
     */
    protected double getHighPrecisionTolerance() {
        return highPrecisionTolerance;
    }

    /**
     * @param highPrecisionTolerance The high precision highPrecisionTolerance to set.
     */
    protected void setHighPrecisionTolerance(double highPrecisionTolerance) {
        this.highPrecisionTolerance = highPrecisionTolerance;
    }

    /**
     * The expected value for {@link DiscreteDistribution#isSupportConnected()}.
     * The default is {@code true}. Test class should override this when the distribution
     * is not support connected.
     *
     * @return Returns true if the distribution is support connected.
     */
    protected boolean isSupportConnected() {
        return true;
    }
}
