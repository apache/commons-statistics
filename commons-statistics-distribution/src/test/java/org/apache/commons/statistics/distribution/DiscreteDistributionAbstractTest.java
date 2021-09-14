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
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for {@link DiscreteDistribution} tests.
 * <p>
 * To create a concrete test class for a continuous distribution
 * implementation, first implement makeDistribution() to return a distribution
 * instance to use in tests. Then implement each of the test data generation
 * methods below.  In each case, the test points and test values arrays
 * returned represent parallel arrays of inputs and expected values for the
 * distribution returned by makeDistribution().  Default implementations
 * are provided for the makeInverseXxx methods that just invert the mapping
 * defined by the arrays returned by the makeCumulativeXxx methods.
 * <ul>
 *  <li>makeProbabilityTestPoints() -- arguments used to test probability density calculation
 *  <li>makeProbabilityTestValues() -- expected probability densities
 *  <li>makeCumulativeTestPoints() -- arguments used to test cumulative probabilities
 *  <li>makeCumulativeTestValues() -- expected cumulative probabilities
 *  <li>makeInverseCumulativeTestPoints() -- arguments used to test inverse cdf evaluation
 *  <li>makeInverseCumulativeTestValues() -- expected inverse cdf values
 * </ul>
 * <p>
 * If the discrete distribution provides higher precision implementations of cumulativeProbability
 * and/or survivalProbability, the following methods should be implemented to provide testing.
 * To use these tests, calculate the cumulativeProbability and survivalProbability such that their naive
 * complement is exceptionally close to `1` and consequently could lose precision due to floating point
 * arithmetic.
 *
 * NOTE: The default high-precision threshold is 1e-22.
 * <ul>
 *  <li>makeCumulativePrecisionTestPoints() -- high precision test inputs
 *  <li>makeCumulativePrecisionTestValues() -- high precision expected results
 *  <li>makeSurvivalPrecisionTestPoints() -- high precision test inputs
 *  <li>makeSurvivalPrecisionTestValues() -- high precision expected results
 * </ul>
 * <p>
 * To implement additional test cases with different distribution instances and
 * test data, use the setXxx methods for the instance data in test cases and
 * call the verifyXxx methods to verify results.
 * <p>
 * Error tolerance can be overridden by implementing getTolerance().
 * <p>
 * Test data should be validated against reference tables or other packages
 * where possible, and the source of the reference data and/or validation
 * should be documented in the test cases.  A framework for validating
 * distribution data against R is included in the /src/test/R source tree.
 * <p>
 * See {@link PoissonDistributionTest} and {@link PascalDistributionTest}
 * for examples.
 */
abstract class DiscreteDistributionAbstractTest {
    /** A zero length int[] array. */
    private static final int[] EMPTY_INT_ARRAY = {};
    /** A zero length double[] array. */
    private static final double[] EMPTY_DOUBLE_ARRAY = {};

    //-------------------- Private test instance data -------------------------

    /** Discrete distribution instance used to perform tests. */
    private DiscreteDistribution distribution;

    /** Tolerance used in comparing expected and returned values. */
    private double tolerance = 1e-12;

    /** Tolerance used in high precision tests. */
    private double highPrecisionTolerance = 1e-22;

    /** Arguments used to test probability calculations. */
    private int[] probabilityTestPoints;

    /** Values used to test probability calculations. */
    private double[] probabilityTestValues;

    /** Values used to test logarithmic probability density calculations. */
    private double[] logProbabilityTestValues;

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

    /** Creates the default probability test input values. */
    public abstract int[] makeProbabilityTestPoints();

    /** Creates the default probability test expected values. */
    public abstract double[] makeProbabilityTestValues();

    /** Creates the default logarithmic probability density test expected values.
     *
     * <p>The default implementation simply computes the logarithm of all the values in
     * {@link #makeProbabilityTestValues()}.
     *
     * @return the default logarithmic probability density test expected values.
     */
    public double[] makeLogProbabilityTestValues() {
        return Arrays.stream(makeProbabilityTestValues()).map(Math::log).toArray();
    }

    /** Creates the default cumulative probability test input values. */
    public abstract int[] makeCumulativeTestPoints();

    /** Creates the default cumulative probability test expected values. */
    public abstract double[] makeCumulativeTestValues();

    /** Creates the default cumulative probability precision test input values. */
    public int[] makeCumulativePrecisionTestPoints() {
        return EMPTY_INT_ARRAY;
    }

    /**
     * Creates the default cumulative probability precision test expected values.
     * Note: The default threshold is 1e-22, any expected values with much higher precision may
     *       not test the desired results without increasing precision threshold.
     */
    public double[] makeCumulativePrecisionTestValues() {
        return EMPTY_DOUBLE_ARRAY;
    }

    /** Creates the default survival probability precision test input values. */
    public int[] makeSurvivalPrecisionTestPoints() {
        return EMPTY_INT_ARRAY;
    }

    /**
     * Creates the default survival probability precision test expected values.
     * Note: The default threshold is 1e-22, any expected values with much higher precision may
     *       not test the desired results without increasing precision threshold.
     */
    public double[] makeSurvivalPrecisionTestValues() {
        return EMPTY_DOUBLE_ARRAY;
    }

    //---- Default implementations of inverse test data generation methods ----

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
        probabilityTestPoints = makeProbabilityTestPoints();
        probabilityTestValues = makeProbabilityTestValues();
        logProbabilityTestValues = makeLogProbabilityTestValues();
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
        probabilityTestPoints = null;
        probabilityTestValues = null;
        logProbabilityTestValues = null;
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
     * Verifies that probability calculations match expected values
     * using current test instance data.
     */
    protected void verifyProbabilities() {
        for (int i = 0; i < probabilityTestPoints.length; i++) {
            final int x = probabilityTestPoints[i];
            Assertions.assertEquals(probabilityTestValues[i],
                distribution.probability(x), getTolerance(),
                () -> "Incorrect probability value returned for " + x);
        }
    }

    /**
     * Verifies that logarithmic probability calculations match expected values
     * using current test instance data.
     */
    protected void verifyLogProbabilities() {
        for (int i = 0; i < probabilityTestPoints.length; i++) {
            final int x = probabilityTestPoints[i];
            Assertions.assertEquals(logProbabilityTestValues[i],
                distribution.logProbability(x), getTolerance(),
                () -> "Incorrect log probability value returned for " + x);
        }
    }

    /**
     * Verifies that cumulative probability density calculations match expected values
     * using current test instance data.
     */
    protected void verifyCumulativeProbabilities() {
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            final int x = cumulativeTestPoints[i];
            Assertions.assertEquals(cumulativeTestValues[i],
                distribution.cumulativeProbability(x), getTolerance(),
                () -> "Incorrect cumulative probability value returned for " + x);
        }
        // verify probability(double, double)
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            for (int j = 0; j < cumulativeTestPoints.length; j++) {
                if (cumulativeTestPoints[i] <= cumulativeTestPoints[j]) {
                    Assertions.assertEquals(
                        cumulativeTestValues[j] - cumulativeTestValues[i],
                        distribution.probability(cumulativeTestPoints[i], cumulativeTestPoints[j]),
                        getTolerance());
                } else {
                    try {
                        distribution.probability(cumulativeTestPoints[i], cumulativeTestPoints[j]);
                    } catch (final IllegalArgumentException e) {
                        continue;
                    }
                    Assertions.fail("distribution.probability(double, double) should have thrown an exception that second argument is too large");
                }
            }
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

    @Test
    void testProbabilities() {
        verifyProbabilities();
    }

    @Test
    void testLogProbabilities() {
        verifyLogProbabilities();
    }

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

    @Test
    void testInverseCumulativeProbabilities() {
        verifyInverseCumulativeProbabilities();
    }

    /**
     * Verifies that probability computations are consistent.
     */
    @Test
    void testConsistency() {
        for (int i = 1; i < cumulativeTestPoints.length; i++) {

            // check that cdf(x, x) = 0
            Assertions.assertEquals(
                0.0,
                distribution.probability(cumulativeTestPoints[i], cumulativeTestPoints[i]),
                getTolerance());

            // check that P(a < X <= b) = P(X <= b) - P(X <= a)
            final int upper = Math.max(cumulativeTestPoints[i], cumulativeTestPoints[i - 1]);
            final int lower = Math.min(cumulativeTestPoints[i], cumulativeTestPoints[i - 1]);
            final double diff = distribution.cumulativeProbability(upper) -
                distribution.cumulativeProbability(lower);
            final double direct = distribution.probability(lower, upper);
            Assertions.assertEquals(diff, direct, getTolerance(),
                () -> "Inconsistent probability for (" + lower + "," + upper + ")");
        }
    }

    @Test
    void testOutsideSupport() {
        // Test various quantities when the variable is outside the support.
        final int lo = distribution.getSupportLowerBound();
        Assertions.assertEquals(distribution.probability(lo), distribution.cumulativeProbability(lo), getTolerance());
        Assertions.assertEquals(lo, distribution.inverseCumulativeProbability(0.0));

        if (lo != Integer.MIN_VALUE) {
            final int below = lo - 1;
            Assertions.assertEquals(0.0, distribution.probability(below));
            Assertions.assertEquals(Double.NEGATIVE_INFINITY, distribution.logProbability(below));
            Assertions.assertEquals(0.0, distribution.cumulativeProbability(below));
            Assertions.assertEquals(1.0, distribution.survivalProbability(below));
        }

        final int hi = distribution.getSupportUpperBound();
        Assertions.assertEquals(0.0, distribution.survivalProbability(hi));
        Assertions.assertEquals(distribution.probability(hi), distribution.survivalProbability(hi - 1), getTolerance());
        Assertions.assertEquals(hi, distribution.inverseCumulativeProbability(1.0));
        if (hi != Integer.MAX_VALUE) {
            final int above = hi + 1;
            Assertions.assertEquals(0.0, distribution.probability(above));
            Assertions.assertEquals(Double.NEGATIVE_INFINITY, distribution.logProbability(above));
            Assertions.assertEquals(1.0, distribution.cumulativeProbability(above));
            Assertions.assertEquals(0.0, distribution.survivalProbability(above));
        }

        // Test the logProbability at the support bound. This hits edge case coverage for logProbability.
        // It is assumed the log probability may support a value when the plain probability will be zero.
        // So do not test Math.log(distribution.probability(x)) == distribution.logProbability(x)
        Assertions.assertEquals(distribution.probability(lo), Math.exp(distribution.logProbability(lo)), getTolerance());
        Assertions.assertEquals(distribution.probability(hi), Math.exp(distribution.logProbability(hi)), getTolerance());
    }

    @Test
    void testProbabilityWithLowerBoundAboveUpperBound() {
        Assertions.assertThrows(DistributionException.class, () -> distribution.probability(1, 0));
    }

    @Test
    void testInverseCumulativeProbabilityWithProbabilityBelowZero() {
        Assertions.assertThrows(DistributionException.class, () -> distribution.inverseCumulativeProbability(-1));
    }

    @Test
    void testInverseCumulativeProbabilityWithProbabilityAboveOne() {
        Assertions.assertThrows(DistributionException.class, () -> distribution.inverseCumulativeProbability(2));
    }

    @Test
    void testSampling() {
        // This test uses the points that are used to test the distribution PMF.
        // The sum of the probability values does not have to be 1 (or very close to 1).
        // Any value generated by the sampler that is not an expected point will
        // be ignored. If the sum of probabilities is above 0.5 then at least half
        // of the samples should be counted and the test will verify these occur with
        // the expected relative frequencies. Note: The expected values are normalised
        // to 1 (i.e. relative frequencies) by the Chi-square test.
        final int[] probabilityPoints = makeProbabilityTestPoints();
        final double[] probabilityValues = makeProbabilityTestValues();
        final int length = TestUtils.eliminateZeroMassPoints(probabilityPoints, probabilityValues);
        final double[] expected = Arrays.copyOf(probabilityValues, length);

        // This test will not be valid if the points do not represent enough of the PMF.
        // Require at least 50%.
        final double sum = Arrays.stream(expected).sum();
        if (sum < 0.5) {
            Assertions.fail("Not enough of the PMF is tested during sampling: " + sum);
        }

        final int sampleSize = 1000;
        MathArrays.scaleInPlace(sampleSize, expected);

        // Use fixed seed.
        final DiscreteDistribution.Sampler sampler =
            getDistribution().createSampler(RandomSource.create(RandomSource.WELL_512_A, 1000));
        final int[] sample = TestUtils.sample(sampleSize, sampler);

        final long[] counts = new long[length];
        for (int i = 0; i < sampleSize; i++) {
            final int x = sample[i];
            for (int j = 0; j < length; j++) {
                if (x == probabilityPoints[j]) {
                    counts[j]++;
                    break;
                }
            }
        }

        TestUtils.assertChiSquareAccept(probabilityPoints, expected, counts, 0.001);
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
     * @return Returns the probabilityTestPoints.
     */
    protected int[] getProbabilityTestPoints() {
        return probabilityTestPoints;
    }

    /**
     * @param probabilityTestPoints The probabilityTestPoints to set.
     */
    protected void setProbabilityTestPoints(int[] probabilityTestPoints) {
        this.probabilityTestPoints = probabilityTestPoints;
    }

    /**
     * @return Returns the probabilityTestValues.
     */
    protected double[] getProbabilityTestValues() {
        return probabilityTestValues;
    }

    /**
     * Set the probability test values.
     * For convenience this recomputes the log density test values using {@link Math#log(double)}.
     *
     * @param probabilityTestValues The probabilityTestValues to set.
     */
    protected void setProbabilityTestValues(double[] probabilityTestValues) {
        this.probabilityTestValues = probabilityTestValues;
        logProbabilityTestValues = Arrays.stream(probabilityTestValues).map(Math::log).toArray();
    }

    /**
     * @return Returns the logProbabilityTestValues.
     */
    protected double[] getLogProbabilityTestValues() {
        return logProbabilityTestValues;
    }

    /**
     * @param logProbabilityTestValues The logProbabilityTestValues to set.
     */
    protected void setLogProbabilityTestValues(double[] logProbabilityTestValues) {
        this.logProbabilityTestValues = logProbabilityTestValues;
    }

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
