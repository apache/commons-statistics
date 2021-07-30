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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

/**
 * Abstract base class for {@link ContinuousDistribution} tests.
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
 *  <li>makeCumulativeTestPoints() -- arguments used to test cumulative probabilities
 *  <li>makeCumulativeTestValues() -- expected cumulative probabilities
 *  <li>makeDensityTestValues() -- expected density values at cumulativeTestPoints
 *  <li>makeInverseCumulativeTestPoints() -- arguments used to test inverse cdf evaluation
 *  <li>makeInverseCumulativeTestValues() -- expected inverse cdf values
 * </ul>
 * <p>
 * If the continuous distribution provides higher precision implementations of cumulativeProbability
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
 * See {@link NormalDistributionTest} and {@link ChiSquaredDistributionTest}
 * for examples.
 */
abstract class ContinuousDistributionAbstractTest {
    /** A zero length double[] array. */
    private static final double[] EMPTY_DOUBLE_ARRAY = {};

    //-------------------- Private test instance data -------------------------

    /**  Distribution instance used to perform tests. */
    private ContinuousDistribution distribution;

    /** Tolerance used in comparing expected and returned values. */
    private double tolerance = 1e-4;

    /** Tolerance used in high precision tests. */
    private double highPrecisionTolerance = 1e-22;

    /** Arguments used to test probability calculations. */
    private double[] probabilityTestPoints;

    /** Values used to test probability calculations. */
    private double[] probabilityTestValues;

    // No log probability in the ContinuousDistribution interface

    /** Values used to test density calculations.
     *
     * <p>Note: The ContinuousDistribution interface defines the density as the gradient
     * of the CDF. It is evaluated using the cumulativeTestPoints.
     */
    private double[] densityTestValues;

    /** Values used to test logarithmic density calculations. */
    private double[] logDensityTestValues;

    /** Arguments used to test cumulative probability density calculations. */
    private double[] cumulativeTestPoints;

    /** Values used to test cumulative probability density calculations. */
    private double[] cumulativeTestValues;

    /** Arguments used to test cumulative probability precision, effectively any x where 1-cdf(x) would result in 1. */
    private double[] cumulativePrecisionTestPoints;

    /** Values used to test cumulative probability precision, usually exceptionally tiny values. */
    private double[] cumulativePrecisionTestValues;

    /** Arguments used to test survival probability precision, effectively any x where 1-sf(x) would result in 1. */
    private double[] survivalPrecisionTestPoints;

    /** Values used to test survival probability precision, usually exceptionally tiny values. */
    private double[] survivalPrecisionTestValues;

    /** Arguments used to test inverse cumulative probability density calculations. */
    private double[] inverseCumulativeTestPoints;

    /** Values used to test inverse cumulative probability density calculations. */
    private double[] inverseCumulativeTestValues;

    //-------------------- Abstract methods -----------------------------------

    /** Creates the default continuous distribution instance to use in tests. */
    public abstract ContinuousDistribution makeDistribution();

    /** Creates the default probability test input values.
     *
     * <p>Distributions that evaluate a non-zero probability (i.e. override the
     * interface default method {@link ContinuousDistribution#probability(double)} should
     * define points to test.
     */
    public double[] makeProbabilityTestPoints() {
        return EMPTY_DOUBLE_ARRAY;
    }

    /** Creates the default probability test expected values. */
    public double[] makeProbabilityTestValues() {
        return EMPTY_DOUBLE_ARRAY;
    }

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

    /** Creates the default density test expected values. */
    public abstract double[] makeDensityTestValues();

    /** Creates the default logarithmic probability density test expected values.
     *
     * <p>The default implementation simply computes the logarithm of all the values in
     * {@link #makeDensityTestValues()}.
     *
     * @return the default logarithmic probability density test expected values.
     */
    public double[] makeLogDensityTestValues() {
        return Arrays.stream(makeDensityTestValues()).map(Math::log).toArray();
    }

    /** Creates the default cumulative probability test input values. */
    public abstract double[] makeCumulativeTestPoints();

    /** Creates the default cumulative probability test expected values. */
    public abstract double[] makeCumulativeTestValues();

    /** Creates the default cumulative probability precision test input values. */
    public double[] makeCumulativePrecisionTestPoints() {
        return EMPTY_DOUBLE_ARRAY;
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
    public double[] makeSurvivalPrecisionTestPoints() {
        return EMPTY_DOUBLE_ARRAY;
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
    public double[] makeInverseCumulativeTestPoints() {
        return makeCumulativeTestValues();
    }

    /** Creates the default inverse cumulative probability density test expected values. */
    public double[] makeInverseCumulativeTestValues() {
        return makeCumulativeTestPoints();
    }

    //-------------------- Setup / tear down ----------------------------------

    /**
     * Setup sets all test instance data to default values.
     * <p>
     * This method is @BeforeEach (created for each test) as certain test methods may wish
     * to alter the defaults.
     */
    @BeforeEach
    void setUp() {
        distribution = makeDistribution();
        probabilityTestPoints = makeProbabilityTestPoints();
        probabilityTestValues = makeProbabilityTestValues();
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
     * Cleans up test instance data.
     */
    @AfterEach
    void tearDown() {
        distribution = null;
        probabilityTestPoints = null;
        probabilityTestValues = null;
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
     * Verifies that probability calculations match expected values
     * using current test instance data.
     */
    protected void verifyProbabilities() {
        for (int i = 0; i < probabilityTestPoints.length; i++) {
            final double x = probabilityTestPoints[i];
            Assertions.assertEquals(probabilityTestValues[i],
                distribution.probability(x), getTolerance(),
                () -> "Incorrect probability value returned for " + x);
        }
    }

    /**
     * Verifies that density calculations match expected values
     * using current test instance data.
     */
    protected void verifyDensities() {
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            final double x = cumulativeTestPoints[i];
            Assertions.assertEquals(densityTestValues[i],
                distribution.density(x), getTolerance(),
                () -> "Incorrect probability density value returned for " + x);
        }
    }

    /**
     * Verifies that logarithmic density calculations match expected values
     * using current test instance data.
     */
    protected void verifyLogDensities() {
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            final double x = cumulativeTestPoints[i];
            Assertions.assertEquals(logDensityTestValues[i],
                distribution.logDensity(x), getTolerance(),
                () -> "Incorrect probability density value returned for " + x);
        }
    }

    /**
     * Verifies that cumulative probability density calculations match expected values
     * using current test instance data.
     */
    protected void verifyCumulativeProbabilities() {
        // verify cumulativeProbability(double)
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            final double x = cumulativeTestPoints[i];
            Assertions.assertEquals(cumulativeTestValues[i],
                distribution.cumulativeProbability(x),
                getTolerance(),
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
            final double x = cumulativeTestPoints[i];
            Assertions.assertEquals(
                1 - cumulativeTestValues[i],
                distribution.survivalProbability(cumulativeTestPoints[i]),
                getTolerance(),
                () -> "Incorrect survival probability value returned for " + x);
        }
    }

    protected void verifySurvivalAndCumulativeProbabilityComplement() {
        for (final double x : cumulativeTestPoints) {
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
            final double x = survivalPrecisionTestPoints[i];
            Assertions.assertEquals(
                survivalPrecisionTestValues[i],
                distribution.survivalProbability(x),
                getHighPrecisionTolerance(),
                () -> "survival probability is not precise for value " + x);
        }
    }

    /**
     * Verifies that CDF is simply not 1-survival function by testing values that would result with inaccurate results
     * if simply calculating 1-survival function.
     */
    protected void verifyCumulativeProbabilityPrecision() {
        for (int i = 0; i < cumulativePrecisionTestPoints.length; i++) {
            final double x = cumulativePrecisionTestPoints[i];
            Assertions.assertEquals(
                cumulativePrecisionTestValues[i],
                distribution.cumulativeProbability(x),
                getHighPrecisionTolerance(),
                () -> "cumulative probability is not precise for value " + x);
        }
    }

    /**
     * Verifies that inverse cumulative probability density calculations match expected values
     * using current test instance data.
     */
    protected void verifyInverseCumulativeProbabilities() {
        for (int i = 0; i < inverseCumulativeTestPoints.length; i++) {
            final double x = inverseCumulativeTestPoints[i];
            Assertions.assertEquals(
                inverseCumulativeTestValues[i],
                distribution.inverseCumulativeProbability(inverseCumulativeTestPoints[i]),
                getTolerance(),
                () -> "Incorrect inverse cumulative probability value returned for " + x);
        }
    }

    //------------------------ Default test cases -----------------------------

    @Test
    void testProbabilities() {
        verifyProbabilities();
    }

    @Test
    void testDensities() {
        verifyDensities();
    }

    @Test
    void testLogDensities() {
        verifyLogDensities();
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
            final double upper = Math.max(cumulativeTestPoints[i], cumulativeTestPoints[i - 1]);
            final double lower = Math.min(cumulativeTestPoints[i], cumulativeTestPoints[i - 1]);
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
        final double lo = distribution.getSupportLowerBound();
        Assertions.assertEquals(lo, distribution.inverseCumulativeProbability(0.0));

        final double below = Math.nextDown(lo);
        Assertions.assertEquals(0.0, distribution.density(below));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, distribution.logDensity(below));
        Assertions.assertEquals(0.0, distribution.cumulativeProbability(below));
        Assertions.assertEquals(1.0, distribution.survivalProbability(below));

        final double hi = distribution.getSupportUpperBound();
        Assertions.assertEquals(0.0, distribution.survivalProbability(hi));
        Assertions.assertEquals(hi, distribution.inverseCumulativeProbability(1.0));

        final double above = Math.nextUp(hi);
        Assertions.assertEquals(0.0, distribution.density(above));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, distribution.logDensity(above));
        Assertions.assertEquals(1.0, distribution.cumulativeProbability(above));
        Assertions.assertEquals(0.0, distribution.survivalProbability(above));
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
        final double[] quartiles = TestUtils.getDistributionQuartiles(getDistribution());
        final double[] expected = {0.25, 0.25, 0.25, 0.25};

        // Use fixed seed.
        final int sampleSize = 1000;
        final ContinuousDistribution.Sampler sampler =
            getDistribution().createSampler(RandomSource.create(RandomSource.WELL_19937_C, 123456789L));
        final double[] sample = TestUtils.sample(sampleSize, sampler);

        final long[] counts = new long[4];
        for (int i = 0; i < sampleSize; i++) {
            TestUtils.updateCounts(sample[i], counts, quartiles);
        }

        TestUtils.assertChiSquareAccept(expected, counts, 0.001);
    }

    /**
     * Verify that density integrals match the distribution.
     * The (filtered, sorted) cumulativeTestPoints array is used to source
     * integration limits. The integral of the density (estimated using a
     * Legendre-Gauss integrator) is compared with the cdf over the same
     * interval. Test points outside of the domain of the density function
     * are discarded.
     */
    @Test
    void testDensityIntegrals() {
        final double tol = 1e-9;
        final BaseAbstractUnivariateIntegrator integrator =
            new IterativeLegendreGaussIntegrator(5, 1e-12, 1e-10);
        final UnivariateFunction d = new UnivariateFunction() {
                @Override
                public double value(double x) {
                    return distribution.density(x);
                }
            };
        final ArrayList<Double> integrationTestPoints = new ArrayList<>();
        for (int i = 0; i < cumulativeTestPoints.length; i++) {
            if (Double.isNaN(cumulativeTestValues[i]) ||
                cumulativeTestValues[i] < 1e-5 ||
                cumulativeTestValues[i] > 1 - 1e-5) {
                continue; // exclude integrals outside domain.
            }
            integrationTestPoints.add(cumulativeTestPoints[i]);
        }
        Collections.sort(integrationTestPoints);
        for (int i = 1; i < integrationTestPoints.size(); i++) {
            Assertions.assertEquals(
                distribution.probability(integrationTestPoints.get(0),
                integrationTestPoints.get(i)),
                integrator.integrate(1000000, // Triangle integrals are very slow to converge
                                     d, integrationTestPoints.get(0),
                                     integrationTestPoints.get(i)), tol);
        }
    }

    /**
     * Test if the distribution is support connected. This test exists to ensure the support
     * connected property is tested. This may be evaluated in the default implementation
     * of {@link AbstractContinuousDistribution#inverseCumulativeProbability(double)}
     * depending on the data points used to test the distribution (see
     * {@link #makeInverseCumulativeTestPoints()}). If this default method has been overridden
     * then the support connected property is not used elsewhere in the standard tests.
     */
    @Test
    void testIsSupportConnected() {
        Assertions.assertEquals(isSupportConnected(), distribution.isSupportConnected());
    }

    //------------------ Getters / Setters for test instance data -----------

    /**
     * @return Returns the distribution.
     */
    protected ContinuousDistribution getDistribution() {
        return distribution;
    }

    /**
     * @param distribution The distribution to set.
     */
    protected void setDistribution(ContinuousDistribution distribution) {
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
    protected double[] getProbabilityTestPoints() {
        return probabilityTestPoints;
    }

    /**
     * @param probabilityTestPoints The probabilityTestPoints to set.
     */
    protected void setProbabilityTestPoints(double[] probabilityTestPoints) {
        this.probabilityTestPoints = probabilityTestPoints;
    }

    /**
     * @return Returns the probabilityTestValues.
     */
    protected double[] getProbabilityTestValues() {
        return probabilityTestValues;
    }

    /**
     * @param probabilityTestValues The probabilityTestValues to set.
     */
    protected void setProbabilityTestValues(double[] probabilityTestValues) {
        this.probabilityTestValues = probabilityTestValues;
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
     * @return Returns the cumulativeTestPoints.
     */
    protected double[] getCumulativeTestPoints() {
        return cumulativeTestPoints;
    }

    /**
     * @param cumulativeTestPoints The cumulativeTestPoints to set.
     */
    protected void setCumulativeTestPoints(double[] cumulativeTestPoints) {
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
    protected double[] getCumulativePrecisionTestPoints() {
        return cumulativePrecisionTestPoints;
    }

    /**
     * @param cumulativePrecisionTestPoints The cumulativePrecisionTestPoints to set.
     */
    protected void setCumulativePrecisionTestPoints(double[] cumulativePrecisionTestPoints) {
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
    protected double[] getSurvivalPrecisionTestPoints() {
        return survivalPrecisionTestPoints;
    }

    /**
     * @param survivalPrecisionTestPoints The survivalPrecisionTestPoints to set.
     */
    protected void setSurvivalPrecisionTestPoints(double[] survivalPrecisionTestPoints) {
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
    protected double[] getInverseCumulativeTestValues() {
        return inverseCumulativeTestValues;
    }

    /**
     * @param inverseCumulativeTestValues The inverseCumulativeTestValues to set.
     */
    protected void setInverseCumulativeTestValues(double[] inverseCumulativeTestValues) {
        this.inverseCumulativeTestValues = inverseCumulativeTestValues;
    }

    /**
     * The expected value for {@link ContinuousDistribution#isSupportConnected()}.
     * The default is {@code true}. Test class should override this when the distribution
     * is not support connected.
     *
     * @return Returns true if the distribution is support connected.
     */
    protected boolean isSupportConnected() {
        return true;
    }
}
