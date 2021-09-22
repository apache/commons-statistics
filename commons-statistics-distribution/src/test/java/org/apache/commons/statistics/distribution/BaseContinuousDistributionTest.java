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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract base class for {@link ContinuousDistribution} tests.
 *
 * <p>This class uses parameterized tests that are repeated for instances of a
 * distribution. The distribution, test input and expected values are generated
 * dynamically.
 *
 * <p>Abstract methods return arrays that are dynamically transformed into arguments for
 * each test. Each abstract method creates an array of arrays. The inner array provides
 * arguments for the test. The size of the outer array corresponds to the number of
 * parameterizations of the distribution to test.
 *
 * <p>To create a concrete test class for a continuous distribution implementation, first
 * implement {@link #makeDistribution(Object...)} to return a distribution instance to use
 * in tests from an array of paremeters. Then implement each of the test data generation
 * methods below. In each case, the test points and test values arrays returned represent
 * parallel arrays of inputs and expected values for the distribution.
 *
 * <ul>
 *  <li>makeParameters() -- parameters of the distribution to test
 *  <li>makeCumulativeTestPoints() -- arguments used to test cumulative probabilities
 *  <li>makeCumulativeTestValues() -- expected cumulative probabilities
 *  <li>makeDensityTestValues() -- expected density values at density test points
 *  <li>makeSupportBounds -- expected support bounds
 *  <li>makeMoments -- expected mean and variance values
 *  <li>makeInvalidParameters -- array of parameters containing values that should raise an exception
 * </ul>
 *
 * <p>Default implementations are provided for convenience to test other functions in the
 * distribution. These may be overridden if required.
 *
 * <ul>
 *  <li>makeDensityTestPoints() -- arguments used to test density (defaults to cumulative test points)
 *  <li>makeLogDensityTestValues() -- expected log density values at density test points (defaults to log(density test values))
 *  <li>makeInverseCumulativeTestPoints() -- arguments used to test inverse cdf evaluation (default to cumulative test values)
 *  <li>makeInverseCumulativeTestValues() -- expected inverse cdf values (default to cumulative test points)
 * </ul>
 *
 * <p>The tolerance for the test is specified by {@link #getTolerance()}. The default is 1e-4 and
 * it is recommended to override this for stricter testing.
 *
 * <p>If the continuous distribution provides higher precision implementations of
 * cumulativeProbability and/or survivalProbability, the following methods should be
 * implemented to provide testing. To use these tests, calculate the cumulativeProbability
 * and survivalProbability such that their naive complement is exceptionally close to `1`
 * and consequently could lose precision due to floating point arithmetic.
 *
 * <ul>
 *  <li>makeCumulativePrecisionTestPoints() -- high precision test inputs
 *  <li>makeCumulativePrecisionTestValues() -- high precision expected results
 *  <li>makeSurvivalPrecisionTestPoints() -- high precision test inputs
 *  <li>makeSurvivalPrecisionTestValues() -- high precision expected results
 * </ul>
 *
 * <p>The tolerance for the test is specified by {@link #getHighPrecisionTolerance()}. The
 * default is 1e-22.
 *
 * <p>Note: The high-precision values may not be applicable to all parameterizations of the
 * distribution. Returning null for the inner parameter array will cause the
 * test to skip that parameterization.
 *
 * <p>If the distribution provides parameter accessors these should be tested in the child test class.
 *
 * <p>To implement additional test cases with a specific distribution instance and test
 * data, create a test in the child class and call the relevant test case to verify
 * results.
 *
 * <p>Test data should be validated against reference tables or other packages where
 * possible, and the source of the reference data and/or validation should be documented
 * in the test cases.
 *
 * <p>See {@link NakagamiDistributionTest} for an example.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class BaseContinuousDistributionTest {
    /**
     * Create a new distribution instance from the parameters.
     * It is assumed the parameters match the order of the parameter constructor.
     *
     * @param parameters Parameters of the distribution.
     * @return the distribution
     */
    abstract ContinuousDistribution makeDistribution(Object... parameters);

    /** Tolerance used in comparing expected and returned values.
     * The default is 1e-4.
     * @return Returns the tolerance.
     */
    double getTolerance() {
        return 1e-4;
    }

    /** High precision tolerance used in comparing expected and returned values.
     * The default is 1e-22.
     * @return Returns the high precision tolerance.
     */
    double getHighPrecisionTolerance() {
        return 1e-22;
    }

    /** Creates invalid parameters that are expected to throw an exception when passed to
     * the {@link #makeDistribution(Object...)} method.
     *
     * <p>This may return as many inner parameter arrays as is required to test all permutations
     * of invalid parameters to the distribution.
     * @return Array of invalid parameter arrays
     */
    abstract Object[][] makeInvalidParameters();

    /** Creates the parameters for the distributions.
     * @return Array of the parameter arrays for each distribution to test
     */
    abstract Object[][] makeParameters();

    /**
     * Gets the parameter names.
     * The names will be used with reflection to identify a parameter accessor in the distribution
     * with the name {@code getX()} where {@code X} is the parameter name.
     * The names should use the same order as {@link #makeDistribution(Object...)}.
     *
     * <p>Return {@code null} to ignore this test. Return {@code null} for an element of the
     * returned array to ignore that parameter.
     *
     * @return the parameter names
     */
    abstract String[] getParameterNames();

    /** Creates the cumulative probability test input points.
     *
     * @return Array of the cumulative probability test input points for each distribution
     */
    abstract double[][] makeCumulativeTestPoints();

    /** Creates the cumulative probability test expected values.
     *
     * @return Array of the cumulative probability expected values for each distribution
     */
    abstract double[][] makeCumulativeTestValues();

    /** Creates the density test input points.
     *
     * <p>The default implementation uses {@link #makeCumulativeTestPoints()}.
     *
     * @return Array of the density test input points for each distribution
     */
    double[][] makeDensityTestPoints() {
        return makeCumulativeTestPoints();
    }

    /** Creates the density test expected values.
     *
     * @return Array of the density expected values for each distribution
     */
    abstract double[][] makeDensityTestValues();

    /**
     * Creates the logarithmic probability density test expected values.
     *
     * <p>The default implementation simply computes the logarithm of all the values in
     * {@link #makeDensityTestValues()}.
     *
     * @return Array of the logarithmic probability density expected values for each distribution
     */
    double[][] makeLogDensityTestValues() {
        return Arrays.stream(makeDensityTestValues()).map(a -> {
            final double[] b = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = Math.log(a[i]);
            }
            return b;
        }).toArray(double[][]::new);
    }

    /** Creates the inverse cumulative probability test input points.
     *
     * <p>The default implementation uses {@link #makeCumulativeTestValues()}.
     *
     * @return Array of the inverse cumulative probability test input points for each distribution
     */
    double[][] makeInverseCumulativeTestPoints() {
        return makeCumulativeTestValues();
    }

    /** Creates the inverse cumulative probability density test expected values.
     *
     * <p>The default implementation uses {@link #makeCumulativeTestPoints()}.
     *
     * @return Array of the inverse cumulative probability expected values for each distribution
     */
    double[][] makeInverseCumulativeTestValues() {
        return makeCumulativeTestPoints();
    }

    /** Creates the cumulative probability high precision test input points.
     *
     * @return Array of the cumulative probability high precision test input points for each distribution
     */
    double[][] makeCumulativePrecisionTestPoints() {
        return null;
    }

    /** Creates the cumulative probability high precision test expected values.
     *
     * <p>Note: The default threshold is 1e-22, any expected values with much higher
     * precision may not test the desired results without increasing precision threshold.
     *
     * @return Array of the cumulative probability high precision expected values for each
     * distribution
     */
    double[][] makeCumulativePrecisionTestValues() {
        return null;
    }

    /** Creates the survival probability high precision test input points.
     *
     * @return Array of the survival probability test input points for each distribution
     */
    double[][] makeSurvivalPrecisionTestPoints() {
        return null;
    }

    /** Creates the survival probability high precision test expected values.
     *
     * <p>Note: The default threshold is 1e-22, any expected values with much higher
     * precision may not test the desired results without increasing precision threshold.
     *
     * @return Array of the survival probability high precision expected values for each
     * distribution
     */
    double[][] makeSurvivalPrecisionTestValues() {
        return null;
    }

    /** Creates the support bounds (lower (double), upper (double), connected (boolean))
     * expected values.
     *
     * @return Array of the support [lower, upper, connected] for each distribution
     */
    abstract Object[][] makeSupportBounds();

    /** Creates the moments (mean, variance, [tolerance]) expected values. The tolerance is
     * optional and defaults to the global test tolerance.
     *
     * @return Array of the moments [mean, variance, [tolerance]] for each distribution
     */
    abstract double[][] makeMoments();

    //------------------------ Methods to stream the test data -----------------------------

    // The @MethodSource annotation will default to a no arguments method of the same name
    // as the @ParameterizedTest method. These can be overridden by child classes to
    // stream different arguments to the test case.

    /**
     * Create a named argument for the distribution from the parameters.
     *
     * @param parameters Parameters of the distribution.
     * @return the distribution argument
     */
    Named<ContinuousDistribution> named(Object... parameters) {
        final ContinuousDistribution dist = makeDistribution(parameters);
        final String name = dist.getClass().getSimpleName() + " " + Arrays.toString(parameters);
        return Named.of(name, dist);
    }

    /**
     * Create a named argument for the array.
     * This is a convenience method to present arrays with a short name in a test report.
     *
     * @param name Name
     * @param array Array
     * @return the named argument
     */
    static Named<double[]> named(String name, double[] array) {
        // Create the name using the first 3 elements
        final StringBuilder sb = new StringBuilder(75);
        sb.append(name);
        // Assume length is non-zero length
        int i = 0;
        sb.append(" [");
        sb.append(array[i++]);
        while (i < Math.min(3, array.length)) {
            sb.append(", ");
            sb.append(array[i++]);
        }
        if (i < array.length) {
            sb.append(", ... ");
        }
        sb.append(']');
        return Named.of(sb.toString(), array);
    }

    Stream<Arguments> stream(Object[][] params, double[][] points, double[][] values, double tolerance) {
        Assertions.assertEquals(params.length, points.length, "Points length mismatch");
        Assertions.assertEquals(params.length, values.length, "Values length mismatch");
        final Builder<Arguments> b = Stream.builder();
        for (int i = 0; i < params.length; i++) {
            if (getLength(points[i]) == 0 || getLength(values[i]) == 0) {
                continue;
            }
            b.accept(Arguments.of(named(params[i]),
                                  named("points", points[i]),
                                  named("values", values[i]),
                                  tolerance));
        }
        return b.build();
    }

    Stream<Arguments> sourceCumulativeTestPoints() {
        Object[][] params = makeParameters();
        double[][] points = makeCumulativeTestPoints();
        double tolerance = getTolerance();
        Assertions.assertEquals(params.length, points.length, "Points length mismatch");
        final Builder<Arguments> b = Stream.builder();
        for (int i = 0; i < params.length; i++) {
            if (getLength(points[i]) == 0) {
                continue;
            }
            b.accept(Arguments.of(named(params[i]),
                                  named("points", points[i]),
                                  tolerance));
        }
        return b.build();
    }

    Object[][] testInvalidParameters() {
        final Object[][] params = makeInvalidParameters();
        Assumptions.assumeTrue(params != null, "Distribution has no invalid parameters");
        return params;
    }

    Stream<Arguments> testDensity() {
        return stream(makeParameters(),
                      makeCumulativeTestPoints(), makeDensityTestValues(),
                      getTolerance());
    }

    Stream<Arguments> testLogDensity() {
        return stream(makeParameters(),
                      makeCumulativeTestPoints(), makeLogDensityTestValues(),
                      getTolerance());
    }

    Stream<Arguments> testCumulativeProbability() {
        return stream(makeParameters(),
                      makeCumulativeTestPoints(), makeCumulativeTestValues(),
                      getTolerance());
    }

    Stream<Arguments> testSurvivalProbability() {
        return stream(makeParameters(),
                      makeCumulativeTestPoints(), makeCumulativeTestValues(),
                      getTolerance());
    }

    Stream<Arguments> testCumulativeProbabilityHighPrecision() {
        final double[][] points = makeCumulativePrecisionTestPoints();
        Assumptions.assumeTrue(points != null, "Distribution has no high-precision cumulative probability function");
        return stream(makeParameters(),
                      points, makeCumulativePrecisionTestValues(),
                      getHighPrecisionTolerance());
    }

    Stream<Arguments> testSurvivalProbabilityHighPrecision() {
        final double[][] points = makeSurvivalPrecisionTestPoints();
        Assumptions.assumeTrue(points != null, "Distribution has no high-precision survival probability function");
        return stream(makeParameters(),
                      points, makeSurvivalPrecisionTestValues(),
                      getHighPrecisionTolerance());
    }

    Stream<Arguments> testInverseCumulativeProbability() {
        return stream(makeParameters(),
                      makeInverseCumulativeTestPoints(), makeInverseCumulativeTestValues(),
                      getTolerance());
    }

    Stream<Arguments> testSurvivalAndCumulativeProbabilityComplement() {
        return sourceCumulativeTestPoints();
    }

    Stream<Arguments> testConsistency() {
        return sourceCumulativeTestPoints();
    }

    /**
     * Stream the arguments to test the density integrals. The test
     * integrates the density function between consecutive test points for the cumulative
     * density function. The default tolerance is 1e-9. Override this method to change
     * the tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testDensityIntegrals() {
        // Use a high tolerance for the integrals
        return stream(makeParameters(),
                      makeCumulativeTestPoints(), makeCumulativeTestValues(),
                      1e-9);
    }

    Stream<Arguments> testSupport() {
        final Object[][] params = makeParameters();
        final Object[][] bounds = makeSupportBounds();
        Assertions.assertEquals(params.length, bounds.length, "Bounds length mismatch");
        final Builder<Arguments> b = Stream.builder();
        for (int i = 0; i < params.length; i++) {
            b.accept(Arguments.of(named(params[i]), bounds[i][0], bounds[i][1], bounds[i][2]));
        }
        return b.build();
    }

    Stream<Arguments> testMoments() {
        final Object[][] params = makeParameters();
        final double[][] moments = makeMoments();
        Assertions.assertEquals(params.length, moments.length, "Moments length mismatch");
        final Builder<Arguments> b = Stream.builder();
        for (int i = 0; i < params.length; i++) {
            b.accept(Arguments.of(named(params[i]), moments[i][0], moments[i][1],
                     moments[i].length > 2 ? moments[i][2] : getTolerance()));
        }
        return b.build();
    }

    /**
     * Gets the length of the array.
     *
     * @param array Array
     * @return the length (or 0 for null array)
     */
    private static int getLength(double[] array) {
        return array == null ? 0 : array.length;
    }

    //------------------------ Tests -----------------------------
    // Tests are final. It is expected that the test can be modified by overridding
    // the method used to stream the arguments, for example to use a specific tolerance
    // for a test in preference to the global tolerance.

    @ParameterizedTest
    @MethodSource
    final void testInvalidParameters(@AggregateWith(value = ArrayAggregator.class) Object[] parameters) {
        Assertions.assertThrows(DistributionException.class, () -> makeDistribution(parameters));
    }

    /**
     * Test the parameter accessors using the reflection API.
     */
    @ParameterizedTest
    @MethodSource(value = "makeParameters")
    final void testParameterAccessors(@AggregateWith(value = ArrayAggregator.class) Object[] parameters) {
        final String[] names = getParameterNames();
        Assumptions.assumeTrue(names != null, "No parameter accessors");
        Assertions.assertEquals(parameters.length, names.length, "Parameter <-> names length mismatch");

        final ContinuousDistribution dist = makeDistribution(parameters);
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            if (name == null) {
                continue;
            }
            try {
                final Method method = dist.getClass().getMethod("get" + name);
                final Object o = method.invoke(dist);
                Assertions.assertEquals(parameters[i], o, () -> "Invalid parameter for " + name);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                     IllegalArgumentException | InvocationTargetException e) {
                Assertions.fail("Failed to find method accessor: " + name, e);
            }
        }
    }

    /**
     * Verifies that density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testDensity(ContinuousDistribution distribution,
                           double[] points,
                           double[] values,
                           double tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(values[i],
                distribution.density(x), tolerance,
                () -> "Incorrect probability density value returned for " + x);
        }
    }

    /**
     * Verifies that logarithmic density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testLogDensity(ContinuousDistribution distribution,
                              double[] points,
                              double[] values,
                              double tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(values[i],
                distribution.logDensity(x), tolerance,
                () -> "Incorrect probability density value returned for " + x);
        }
    }

    /**
     * Verifies that cumulative probability density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbability(ContinuousDistribution distribution,
                                         double[] points,
                                         double[] values,
                                         double tolerance) {
        // verify cumulativeProbability(double)
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(values[i],
                distribution.cumulativeProbability(x),
                tolerance,
                () -> "Incorrect cumulative probability value returned for " + x);
        }
        // verify probability(double, double)
        for (int i = 0; i < points.length; i++) {
            final double x0 = points[i];
            for (int j = 0; j < points.length; j++) {
                final double x1 = points[j];
                if (x0 <= x1) {
                    Assertions.assertEquals(
                        values[j] - values[i],
                        distribution.probability(x0, x1),
                        tolerance);
                } else {
                    Assertions.assertThrows(IllegalArgumentException.class,
                        () -> distribution.probability(x0, x1),
                        "distribution.probability(double, double) should have thrown an exception that first argument is too large");
                }
            }
        }
    }

    /**
     * Verifies that survival probability density calculations match expected values.
     * The expected values are computed using 1 - CDF(x) from the cumulative test values.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbability(ContinuousDistribution distribution,
                                       double[] points,
                                       double[] values,
                                       double tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(
                1 - values[i],
                distribution.survivalProbability(points[i]),
                tolerance,
                () -> "Incorrect survival probability value returned for " + x);
        }
    }

    /**
     * Verifies that CDF is simply not 1-survival function by testing values that would result
     * with inaccurate results if simply calculating 1-survival function.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityHighPrecision(ContinuousDistribution distribution,
                                                      double[] points,
                                                      double[] values,
                                                      double tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(
                values[i],
                distribution.cumulativeProbability(x),
                tolerance,
                () -> "cumulative probability is not precise for value " + x);
        }
    }

    /**
     * Verifies that survival is simply not 1-cdf by testing calculations that would underflow
     * that calculation and result in an inaccurate answer.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbabilityHighPrecision(ContinuousDistribution distribution,
                                                    double[] points,
                                                    double[] values,
                                                    double tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(
                values[i],
                distribution.survivalProbability(x),
                tolerance,
                () -> "survival probability is not precise for value " + x);
        }
    }

    /**
     * Verifies that inverse cumulative probability density calculations match expected values
     */
    @ParameterizedTest
    @MethodSource
    final void testInverseCumulativeProbability(ContinuousDistribution distribution,
                                                double[] points,
                                                double[] values,
                                                double tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(
                values[i],
                distribution.inverseCumulativeProbability(points[i]),
                tolerance,
                () -> "Incorrect inverse cumulative probability value returned for " + x);
        }
    }

    /**
     * Verifies that cumulative probability density and survival probability calculations
     * sum to approximately 1.0.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalAndCumulativeProbabilityComplement(ContinuousDistribution distribution,
                                                              double[] points,
                                                              double tolerance) {
        for (final double x : points) {
            Assertions.assertEquals(
                1.0,
                distribution.survivalProbability(x) + distribution.cumulativeProbability(x),
                tolerance,
                () -> "survival + cumulative probability were not close to 1.0 for " + x);
        }
    }

    /**
     * Verifies that probability computations are consistent.
     */
    @ParameterizedTest
    @MethodSource
    final void testConsistency(ContinuousDistribution distribution,
                               double[] points,
                               double tolerance) {
        for (int i = 1; i < points.length; i++) {

            // check that cdf(x, x) = 0
            Assertions.assertEquals(
                0.0,
                distribution.probability(points[i], points[i]),
                getTolerance());

            // check that P(a < X <= b) = P(X <= b) - P(X <= a)
            final double upper = Math.max(points[i], points[i - 1]);
            final double lower = Math.min(points[i], points[i - 1]);
            final double diff = distribution.cumulativeProbability(upper) -
                                distribution.cumulativeProbability(lower);
            final double direct = distribution.probability(lower, upper);
            Assertions.assertEquals(diff, direct, tolerance,
                () -> "Inconsistent probability for (" + lower + "," + upper + ")");
        }
    }

    @ParameterizedTest
    @MethodSource(value = "makeParameters")
    final void testOutsideSupport(@AggregateWith(value = ArrayAggregator.class) Object[] parameters) {
        final ContinuousDistribution dist = makeDistribution(parameters);

        // Test various quantities when the variable is outside the support.
        final double lo = dist.getSupportLowerBound();
        Assertions.assertEquals(lo, dist.inverseCumulativeProbability(0.0));

        final double below = Math.nextDown(lo);
        Assertions.assertEquals(0.0, dist.density(below));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logDensity(below));
        Assertions.assertEquals(0.0, dist.cumulativeProbability(below));
        Assertions.assertEquals(1.0, dist.survivalProbability(below));

        final double hi = dist.getSupportUpperBound();
        Assertions.assertEquals(0.0, dist.survivalProbability(hi));
        Assertions.assertEquals(hi, dist.inverseCumulativeProbability(1.0));

        final double above = Math.nextUp(hi);
        Assertions.assertEquals(0.0, dist.density(above));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logDensity(above));
        Assertions.assertEquals(1.0, dist.cumulativeProbability(above));
        Assertions.assertEquals(0.0, dist.survivalProbability(above));
    }

    @ParameterizedTest
    @MethodSource(value = "makeParameters")
    final void testInvalidProbailities(@AggregateWith(value = ArrayAggregator.class) Object[] parameters) {
        final ContinuousDistribution dist = makeDistribution(parameters);
        Assertions.assertThrows(DistributionException.class, () -> dist.probability(1, 0));
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(-1));
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(2));
    }

    @ParameterizedTest
    @MethodSource(value = "makeParameters")
    final void testSampling(@AggregateWith(value = ArrayAggregator.class) Object[] parameters) {
        final ContinuousDistribution dist = makeDistribution(parameters);
        final double[] quartiles = TestUtils.getDistributionQuartiles(dist);
        final double[] expected = {0.25, 0.25, 0.25, 0.25};

        final int sampleSize = 1000;
        MathArrays.scaleInPlace(sampleSize, expected);

        // Use fixed seed.
        final ContinuousDistribution.Sampler sampler =
            dist.createSampler(RandomSource.XO_SHI_RO_256_PP.create(123456789L));
        final double[] sample = TestUtils.sample(sampleSize, sampler);

        final long[] counts = new long[4];
        for (int i = 0; i < sampleSize; i++) {
            TestUtils.updateCounts(sample[i], counts, quartiles);
        }

        TestUtils.assertChiSquareAccept(expected, counts, 0.001);
    }

    /**
     * Verify that density integrals match the distribution.
     * The (filtered, sorted) points array is used to source
     * integration limits. The integral of the density (estimated using a
     * Legendre-Gauss integrator) is compared with the cdf over the same
     * interval. Test points outside of the domain of the density function
     * are discarded.
     */
    @ParameterizedTest
    @MethodSource
    final void testDensityIntegrals(ContinuousDistribution distribution,
                                    double[] points,
                                    double[] values,
                                    double tolerance) {
        final BaseAbstractUnivariateIntegrator integrator =
            new IterativeLegendreGaussIntegrator(5, 1e-12, 1e-10);
        final UnivariateFunction d = distribution::density;
        final ArrayList<Double> integrationTestPoints = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            if (Double.isNaN(values[i]) ||
                values[i] < 1e-5 ||
                values[i] > 1 - 1e-5) {
                continue; // exclude integrals outside domain.
            }
            integrationTestPoints.add(points[i]);
        }
        Collections.sort(integrationTestPoints);
        for (int i = 1; i < integrationTestPoints.size(); i++) {
            final double x0 = integrationTestPoints.get(i - 1);
            final double x1 = integrationTestPoints.get(i);
            Assertions.assertEquals(
                distribution.probability(x0, x1),
                integrator.integrate(1000000, // Triangle integrals are very slow to converge
                                     d, x0, x1), tolerance);
        }
    }

    @ParameterizedTest
    @MethodSource
    final void testSupport(ContinuousDistribution dist, double lower, double upper, boolean connected) {
        Assertions.assertEquals(lower, dist.getSupportLowerBound());
        Assertions.assertEquals(upper, dist.getSupportUpperBound());
        Assertions.assertEquals(connected, dist.isSupportConnected());
    }

    @ParameterizedTest
    @MethodSource
    final void testMoments(ContinuousDistribution dist, double mean, double variance, double tolerance) {
        Assertions.assertEquals(mean, dist.getMean(), tolerance);
        Assertions.assertEquals(variance, dist.getVariance(), tolerance);
    }

    /**
     * Aggregate all arguments as a single {@code Object[]} array.
     *
     * <p>Note: The default JUnit 5 behaviour for an Argument containing an {@code Object[]} is
     * to uses each element of the Object array as an indexed argument. This aggregator changes
     * the behaviour to pass the Object[] as argument index 0.
     */
    static class ArrayAggregator implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context)
            throws ArgumentsAggregationException {
            return accessor.toArray();
        }
    }
}
