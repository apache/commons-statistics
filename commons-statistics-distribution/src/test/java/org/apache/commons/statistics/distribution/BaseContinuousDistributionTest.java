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
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.DistributionTestData.ContinuousDistributionTestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract base class for {@link ContinuousDistribution} tests.
 *
 * <p>This class uses parameterized tests that are repeated for instances of a
 * distribution. The distribution, test input and expected values are generated
 * dynamically from properties files loaded from resources.
 *
 * <p>The class has a single instance (see {@link Lifecycle#PER_CLASS}) that loads properties
 * files from resources on creation. Resource files are assumed to be in the corresponding package
 * for the class and named sequentially from 1:
 * <pre>
 * test.distname.1.properties
 * test.distname.2.properties
 * </pre>
 * <p>Where {@code distname} is the name of the distribution. The name is dynamically
 * created in {@link #getDistributionName()} and can be overridden by implementing classes.
 * A single parameterization of a distribution is tested using a single properties file.
 *
 * <p>To test a distribution create a sub-class and override the following methods:
 * <ul>
 * <li>{@link #makeDistribution(Object...) makeDistribution(Object...)} - Creates the distribution from the parameters
 * <li>{@link #makeInvalidParameters()} - Generate invalid parameters for the distribution
 * <li>{@link #getParameterNames()} - Return the names of parameter accessors
 * </ul>
 *
 * <p>The distribution is created using
 * {@link #makeDistribution(Object...) makeDistribution(Object...)}. This should
 * create an instance of the distribution using parameters defined in the properties file.
 * The parameters are parsed from String values to the appropriate parameter object. Currently
 * this supports Double and Integer; numbers can be unboxed and used to create the distribution.
 *
 * <p>Illegal arguments for the distribution are tested from parameters provided by
 * {@link #makeInvalidParameters()}. If there are no illegal arguments this method may return
 * null to skip the test. Primitive parameters are boxed to objects so ensure the canonical form
 * is used, e.g. {@code 1.0} not {@code 1} for a {@code double} argument.
 *
 * <p>If the distribution provides parameter accessors then the child test class can return
 * the accessor names using {@link #getParameterNames()}. The distribution method accessors
 * will be detected and invoked using reflection. The accessor must provide the same value
 * as that passed to the {@link #makeDistribution(Object...)} method to create the distribution.
 * This method may return null to skip the test, or null for the name to skip the test for a
 * single parameter accessor.
 *
 * <p>The properties file must contain parameters for the distribution, properties of the
 * distribution (moments and bounds) and points to test the CDF and PDF with the expected values.
 * This information can be used to evaluate the distribution CDF and PDF but also the survival
 * function, consistency of the probability computations and random sampling.
 *
 * <p>Optionally:
 * <ul>
 * <li>Points for the PDF (and log PDF) can be specified. The default will use the CDF points.
 * Note: It is not expected that evaluation of the PDF will require different points to the CDF.
 * <li>Points and expected values for the inverse CDF can be specified. These are used in
 * addition to a test of the inverse mapping of the CDF values to the CDF test points. The
 * inverse mapping test can be disabled.
 * <li>Expected values for the log PDF can be specified. The default will use
 * {@link Math#log(double)} on the PDF values.
 * <li>Points and expected values for the survival function can be specified. The default will use
 * the expected CDF values (SF = 1 - CDF).
 * <li>A tolerance for equality assertions. The default is set by {@link #getAbsoluteTolerance()}
 * and {@link #getRelativeTolerance()}.
 * <li>A flag to indicate the returned value for {@link ContinuousDistribution#isSupportConnected()}.
 * The default is set by {@link #isSupportConnected()}.
 * </ul>
 *
 * <p>If the distribution provides higher precision implementations of
 * cumulative probability and/or survival probability as the values approach zero, then test
 * points and expected values can be provided with a tolerance for equality assertions of
 * high-precision computations. The default is set by {@link #getHighPrecisionAbsoluteTolerance()}
 * and {@link #getHighPrecisionRelativeTolerance()}.
 *
 * <p>Note: All properties files are read during test initialization. Any errors in a single
 * property file will throw an exception, invalidating the initialization and no tests
 * will be executed.
 *
 * <p>The parameterized tests in this class are inherited. The tests are final and cannot be
 * changed. This ensures each instance of a distribution is tested for all functionality in
 * the {@link ContinuousDistribution} interface. Arguments to the parameterized tests are
 * generated dynamically using methods of the same name. These can be over-ridden in child
 * classes to alter parameters. Throwing a
 * {@link org.opentest4j.TestAbortedException TestAbortedException} in this method will
 * skip the test as the arguments will not be generated.
 *
 * <p>Each parameterized test is effectively static; it uses no instance data.
 * To implement additional test cases with a specific distribution instance and test
 * data, create a test in the child class and call the relevant test case to verify
 * results. Note that it is recommended to use the properties file as this ensures the
 * entire functionality of the distribution is tested for that parameterization.
 *
 * <p>Tests using floating-point equivalence comparisons are asserted using a {@link DoubleTolerance}.
 * This interface computes true or false for the comparison of two {@code double} types.
 * This allows the flexibility to use absolute, relative or ULP thresholds in combinations
 * built using And or Or conditions to compare numbers. The default uses an Or combination
 * of the absolute and relative thresholds. See {@link DoubleTolerances} to construct
 * custom instances for additional tests.
 *
 * <p>Test data should be validated against reference tables or other packages where
 * possible, and the source of the reference data and/or validation should be documented
 * in the properties file or additional test cases as appropriate.
 *
 * <p>The properties file uses {@code key=value} pairs loaded using a
 * {@link java.util.Properties} object. Values will be read as String and then parsed to
 * numeric data, and data arrays. Multi-line values can use a {@code \} character.
 * Data in the properties file will be converted to numbers using standard parsing
 * functions appropriate to the primitive type, e.g. {@link Double#parseDouble(String)}.
 * Special double values should use NaN, Infinity and -Infinity. As a convenience
 * for creating test data parsing of doubles supports the following notations from
 * other languages ('inf', 'Inf').
 *
 * <p>The following is a complete properties file for a distribution:
 * <pre>
 * parameters = 0.5 1.0
 * # Computed using XYZ
 * mean = 1.0
 * variance = NaN
 * # optional (default -Infinity)
 * lower = 0
 * # optional (default Infinity)
 * upper = Infinity
 * # optional (default true or over-ridden in isSupportConnected())
 * connected = false
 * # optional (default 1e-12 or over-ridden in getRelativeTolerance())
 * tolerance.relative = 1e-9
 * # optional (default 0.0 or over-ridden in getAbsoluteTolerance())
 * tolerance.absolute = 0.0
 * # optional (default 1e-12 or over-ridden in getHighPrecisionRelativeTolerance())
 * tolerance.relative.hp = 1e-10
 * # optional (default 0.0 or over-ridden in getHighPrecisionAbsoluteTolerance())
 * tolerance.absolute.hp = 1e-30
 * cdf.points = 0, 0.2
 * cdf.values = 0.0, 0.5
 * # optional (default uses cdf.points)
 * pdf.points = 0, 40000
 * pdf.values = 0.0,\
 *  0.0
 * # optional (default uses log pdf.values)
 * logpdf.values = -1900.123, -Infinity
 * # optional (default uses cdf.points and 1 - cdf.values)
 * sf.points = 400
 * sf.values = 0.0
 * # optional high-precision CDF test
 * cdf.hp.points = 1e-16
 * cdf.hp.values = 1.23e-17
 * # optional high-precision survival function test
 * sf.hp.points = 9
 * sf.hp.values = 2.34e-18
 * # optional inverse CDF test (defaults to ignore)
 * icdf.values = 0.0, 0.5
 * ipdf.values = 0.0, 0.2
 * # CDF inverse mapping test (default false)
 * disable.cdf.inverse = false
 * # Sampling test (default false)
 * disable.sample = false
 * # PDF values test (default false)
 * disable.pdf = false
 * # Log PDF values test (default false)
 * disable.logpdf = false
 * # CDF values test (default false)
 * disable.cdf = false
 * # Survival function values test (default false)
 * disable.sf = false
 * </pre>
 *
 * <p>See {@link NakagamiDistributionTest} for an example and the resource file {@code test.nakagami.1.properties}.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class BaseContinuousDistributionTest
    extends BaseDistributionTest<ContinuousDistribution, ContinuousDistributionTestData> {

    /** Relative accuracy of the integrator result. */
    private static final double INTEGRATOR_ABS_ACCURACY = 1e-10;
    /** Absolute accuracy of the integrator result. */
    private static final double INTEGRATOR_REL_ACCURACY = 1e-12;

    @Override
    ContinuousDistributionTestData makeDistributionData(Properties properties) {
        return new ContinuousDistributionTestData(properties);
    }

    //------------------------ Methods to stream the test data -----------------------------

    // The @MethodSource annotation will default to a no arguments method of the same name
    // as the @ParameterizedTest method. These can be overridden by child classes to
    // stream different arguments to the test case.

    /**
     * Create a stream of arguments containing the distribution to test, the CDF
     * test points and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> streamCdfTestPoints() {
        return streamCdfTestPoints(d -> false);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the CDF
     * test points and the test tolerance.
     *
     * @param filter Filter applied on the test data. If true the data is ignored.
     * @return the stream
     */
    Stream<Arguments> streamCdfTestPoints(Predicate<ContinuousDistributionTestData> filter) {
        final Builder<Arguments> b = Stream.builder();
        final int[] size = {0};
        data.forEach(d -> {
            final double[] p = d.getCdfPoints();
            if (filter.test(d) || TestUtils.getLength(p) == 0) {
                return;
            }
            size[0]++;
            b.accept(Arguments.of(namedDistribution(d.getParameters()),
                     namedArray("points", p),
                     createTestTolerance(d)));
        });
        Assumptions.assumeTrue(size[0] != 0, () -> "Distribution has no data for cdf test points");
        return b.build();
    }

    /**
     * Create a stream of arguments containing the distribution to test, the PDF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testDensity() {
        return stream(ContinuousDistributionTestData::isDisablePdf,
                      ContinuousDistributionTestData::getPdfPoints,
                      ContinuousDistributionTestData::getPdfValues,
                      this::createTestTolerance, "pdf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the log PDF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testLogDensity() {
        return stream(ContinuousDistributionTestData::isDisableLogPdf,
                      ContinuousDistributionTestData::getPdfPoints,
                      ContinuousDistributionTestData::getLogPdfValues,
                      this::createTestTolerance, "logpdf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbability() {
        return stream(ContinuousDistributionTestData::isDisableCdf,
                      ContinuousDistributionTestData::getCdfPoints,
                      ContinuousDistributionTestData::getCdfValues,
                      this::createTestTolerance, "cdf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the survival function
     * test points and values, and the test tolerance.
     *
     * <p>This defaults to using the CDF points. The survival function is tested as 1 - CDF.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalProbability() {
        return stream(ContinuousDistributionTestData::isDisableSf,
                      ContinuousDistributionTestData::getSfPoints,
                      ContinuousDistributionTestData::getSfValues,
                      this::createTestTolerance, "sf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test points
     * and values, and the test tolerance for high-precision computations.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbabilityHighPrecision() {
        return stream(ContinuousDistributionTestData::getCdfHpPoints,
                      ContinuousDistributionTestData::getCdfHpValues,
                      this::createTestHighPrecisionTolerance, "cdf.hp");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the survival function
     * test points and values, and the test tolerance for high-precision computations.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalProbabilityHighPrecision() {
        return stream(ContinuousDistributionTestData::getSfHpPoints,
                      ContinuousDistributionTestData::getSfHpValues,
                      this::createTestHighPrecisionTolerance, "sf.hp");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the inverse CDF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testInverseCumulativeProbability() {
        return stream(ContinuousDistributionTestData::getIcdfPoints,
                      ContinuousDistributionTestData::getIcdfValues,
                      this::createTestTolerance, "icdf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test points
     * to evaluate the CDF, and the test tolerance. The equality
     * {@code cdf(x) = cdf(icdf(cdf(x)))} must be true within the tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbabilityInverseMapping() {
        return streamCdfTestPoints(ContinuousDistributionTestData::isDisableCdfInverse);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test points
     * to evaluate the CDF and survival function, and the test tolerance. CDF + SF must equal 1.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalAndCumulativeProbabilityComplement() {
        // This is not disabled based on isDisableCdf && isDisableSf.
        // Those flags are intended to ignore tests against reference values.
        return streamCdfTestPoints();
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test points
     * to evaluate the CDF and probability in a range, and the test tolerance.
     * Used to test CDF(x1) - CDF(x0) = probability(x0, x1).
     *
     * @return the stream
     */
    Stream<Arguments> testConsistency() {
        // This is not disabled based on isDisableCdf.
        // That flags is intended to ignore tests against reference values.
        return streamCdfTestPoints();
    }

    /**
     * Create a stream of arguments containing the distribution to test sampling.
     *
     * @return the stream
     */
    Stream<Arguments> testSampling() {
        return streamDistrbution(ContinuousDistributionTestData::isDisableSample, "sampling");
    }

    /**
     * Stream the arguments to test the density integrals. The test
     * integrates the density function between consecutive test points for the cumulative
     * density function. The default tolerance is based on the convergence tolerance of
     * the underlying integrator (abs=1e-10, rel=1e-12).
     * Override this method to change the tolerance.
     *
     * <p>This is disabled by {@link ContinuousDistributionTestData#isDisablePdf()}. If
     * the distribution cannot compute the density to match reference values then it
     * is assumed an integral of the PDF will fail to match reference CDF values.
     *
     * @return the stream
     */
    Stream<Arguments> testDensityIntegrals() {
        // Create a tolerance suitable for the same thresholds used by the intergator.
        final Function<ContinuousDistributionTestData, DoubleTolerance> tolerance =
            d -> createAbsOrRelTolerance(INTEGRATOR_ABS_ACCURACY * 10, INTEGRATOR_REL_ACCURACY * 10);
        return stream(ContinuousDistributionTestData::isDisablePdf,
                      ContinuousDistributionTestData::getCdfPoints,
                      ContinuousDistributionTestData::getCdfValues,
                      tolerance, "pdf integrals");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the support
     * lower and upper bound, and the support connect flag.
     *
     * @return the stream
     */
    Stream<Arguments> testSupport() {
        return data.stream().map(d -> {
            return Arguments.of(namedDistribution(d.getParameters()), d.getLower(), d.getUpper(), d.isConnected());
        });
    }

    /**
     * Create a stream of arguments containing the distribution to test, the mean
     * and variance, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testMoments() {
        return data.stream().map(d -> {
            return Arguments.of(namedDistribution(d.getParameters()), d.getMean(), d.getVariance(), createTestTolerance(d));
        });
    }

    //------------------------ Tests -----------------------------

    // Tests are final. It is expected that the test can be modified by overriding
    // the method used to stream the arguments, for example to use a specific tolerance
    // for a test in preference to the tolerance defined in the properties file.

    /**
     * Test that density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testDensity(ContinuousDistribution dist,
                           double[] points,
                           double[] values,
                           DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            TestUtils.assertEquals(values[i],
                dist.density(x), tolerance,
                () -> "Incorrect probability density value returned for " + x);
        }
    }

    /**
     * Test that logarithmic density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testLogDensity(ContinuousDistribution dist,
                              double[] points,
                              double[] values,
                              DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            TestUtils.assertEquals(values[i],
                dist.logDensity(x), tolerance,
                () -> "Incorrect probability density value returned for " + x);
        }
    }

    /**
     * Test that cumulative probability density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbability(ContinuousDistribution dist,
                                         double[] points,
                                         double[] values,
                                         DoubleTolerance tolerance) {
        // verify cumulativeProbability(double)
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            TestUtils.assertEquals(values[i],
                dist.cumulativeProbability(x),
                tolerance,
                () -> "Incorrect cumulative probability value returned for " + x);
        }
    }

    /**
     * Test that survival probability density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbability(ContinuousDistribution dist,
                                       double[] points,
                                       double[] values,
                                       DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            TestUtils.assertEquals(
                values[i],
                dist.survivalProbability(points[i]),
                tolerance,
                () -> "Incorrect survival probability value returned for " + x);
        }
    }

    /**
     * Test that CDF is simply not 1-survival function by testing values that would result
     * with inaccurate results if simply calculating 1-survival function.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityHighPrecision(ContinuousDistribution dist,
                                                      double[] points,
                                                      double[] values,
                                                      DoubleTolerance tolerance) {
        testCumulativeProbability(dist, points, values, tolerance);
    }

    /**
     * Test that survival is simply not 1-cdf by testing calculations that would underflow
     * that calculation and result in an inaccurate answer.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbabilityHighPrecision(ContinuousDistribution dist,
                                                    double[] points,
                                                    double[] values,
                                                    DoubleTolerance tolerance) {
        testSurvivalProbability(dist, points, values, tolerance);
    }

    /**
     * Test that inverse cumulative probability density calculations match expected values.
     *
     * <p>Note: Any expected values outside the support of the distribution are ignored.
     */
    @ParameterizedTest
    @MethodSource
    final void testInverseCumulativeProbability(ContinuousDistribution dist,
                                                double[] points,
                                                double[] values,
                                                DoubleTolerance tolerance) {
        final double lower = dist.getSupportLowerBound();
        final double upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final double x = values[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = points[i];
            TestUtils.assertEquals(
                x,
                dist.inverseCumulativeProbability(p),
                tolerance,
                () -> "Incorrect inverse cumulative probability value returned for " + p);
        }
    }

    /**
     * Test that an inverse mapping of the cumulative probability density values matches
     * the original point, {@code x = icdf(cdf(x))}.
     *
     * <p>Note: It is possible for two points to compute the same CDF value. In this
     * case the mapping is not a bijection. Thus a further forward mapping is performed
     * to check {@code cdf(x) = cdf(icdf(cdf(x)))} within the allowed tolerance.
     *
     * <p>Note: Any points outside the support of the distribution are ignored.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityInverseMapping(ContinuousDistribution dist,
                                                       double[] points,
                                                       DoubleTolerance tolerance) {
        final double lower = dist.getSupportLowerBound();
        final double upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = dist.cumulativeProbability(x);
            final double x1 = dist.inverseCumulativeProbability(p);
            final double p1 = dist.cumulativeProbability(x1);
            // Check the inverse CDF computed a value that will return to the
            // same probability value.
            TestUtils.assertEquals(
                p,
                p1,
                tolerance,
                () -> "Incorrect CDF(inverse CDF(CDF(x))) value returned for " + x);
        }
    }

    /**
     * Test that cumulative probability density and survival probability calculations
     * sum to approximately 1.0.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalAndCumulativeProbabilityComplement(ContinuousDistribution dist,
                                                              double[] points,
                                                              DoubleTolerance tolerance) {
        for (final double x : points) {
            TestUtils.assertEquals(
                1.0,
                dist.survivalProbability(x) + dist.cumulativeProbability(x),
                tolerance,
                () -> "survival + cumulative probability were not close to 1.0 for " + x);
        }
    }

    /**
     * Test that probability computations are consistent.
     * This checks probability(x, x) = 0; and probability(x0, x1) = CDF(x1) - CDF(x0).
     */
    @ParameterizedTest
    @MethodSource
    final void testConsistency(ContinuousDistribution dist,
                               double[] points,
                               DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x0 = points[i];

            // Check that probability(x, x) == 0
            Assertions.assertEquals(
                0.0,
                dist.probability(x0, x0),
                () -> "Non-zero probability(x, x) for " + x0);

            final double cdf0 = dist.cumulativeProbability(x0);
            for (int j = 0; j < points.length; j++) {
                final double x1 = points[j];
                // Ignore the same point
                if (x0 < x1) {
                    // Check that probability(x0, x1) = CDF(x1) - CDF(x0).
                    final double cdf1 = dist.cumulativeProbability(x1);
                    TestUtils.assertEquals(
                        cdf1 - cdf0,
                        dist.probability(x0, x1),
                        tolerance,
                        () -> "Inconsistent probability for (" + x0 + "," + x1 + ")");
                } else if (x0 > x1) {
                    Assertions.assertThrows(IllegalArgumentException.class,
                        () -> dist.probability(x0, x1),
                        "probability(double, double) should have thrown an exception that first argument is too large");
                }
            }
        }
    }

    /**
     * Test CDF and inverse CDF values at the edge of the support of the distribution return
     * expected values and the CDF outside the support returns consistent values.
     */
    @ParameterizedTest
    @MethodSource(value = "streamDistrbution")
    final void testOutsideSupport(ContinuousDistribution dist) {
        // Test various quantities when the variable is outside the support.
        final double lo = dist.getSupportLowerBound();
        Assertions.assertEquals(0.0, dist.cumulativeProbability(lo), "cdf(lower)");
        Assertions.assertEquals(lo, dist.inverseCumulativeProbability(0.0), "icdf(0.0)");
        // Test for rounding errors during inversion
        Assertions.assertTrue(lo <= dist.inverseCumulativeProbability(Double.MIN_VALUE), "lo <= icdf(min)");
        Assertions.assertTrue(lo <= dist.inverseCumulativeProbability(Double.MIN_NORMAL), "lo <= icdf(min_normal)");

        final double below = Math.nextDown(lo);
        Assertions.assertEquals(0.0, dist.density(below), "pdf(x < lower)");
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logDensity(below), "logpdf(x < lower)");
        Assertions.assertEquals(0.0, dist.cumulativeProbability(below), "cdf(x < lower)");
        Assertions.assertEquals(1.0, dist.survivalProbability(below), "sf(x < lower)");

        final double hi = dist.getSupportUpperBound();
        Assertions.assertTrue(lo <= hi, "lower <= upper");
        Assertions.assertEquals(1.0, dist.cumulativeProbability(hi), "cdf(upper)");
        Assertions.assertEquals(0.0, dist.survivalProbability(hi), "sf(upper)");
        Assertions.assertEquals(hi, dist.inverseCumulativeProbability(1.0), "icdf(1.0)");
        // Test for rounding errors during inversion
        Assertions.assertTrue(hi >= dist.inverseCumulativeProbability(Math.nextDown(1.0)), "hi >= icdf(nextDown(1.0))");

        final double above = Math.nextUp(hi);
        Assertions.assertEquals(0.0, dist.density(above), "pdf(x > upper)");
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logDensity(above), "logpdf(x > upper)");
        Assertions.assertEquals(1.0, dist.cumulativeProbability(above), "cdf(x > upper)");
        Assertions.assertEquals(0.0, dist.survivalProbability(above), "sf(x > upper)");
    }

    /**
     * Test invalid probabilities passed to computations that require a p-value in {@code [0, 1]}
     * or a range where {@code p1 <= p2}.
     */
    @ParameterizedTest
    @MethodSource(value = "streamDistrbution")
    final void testInvalidProbabilities(ContinuousDistribution dist) {
        final double lo = dist.getSupportLowerBound();
        final double hi = dist.getSupportUpperBound();
        if (lo < hi) {
            Assertions.assertThrows(DistributionException.class, () -> dist.probability(hi, lo), "x0 > x1");
        }
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(-1), "p < 0.0");
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(2), "p > 1.0");
    }

    /**
     * Test sampling from the distribution.
     */
    @ParameterizedTest
    @MethodSource
    final void testSampling(ContinuousDistribution dist) {
        final double[] quartiles = TestUtils.getDistributionQuartiles(dist);
        // The distribution quartiles are created using the inverse CDF.
        // This may not be accurate for extreme parameterizations of the distribution.
        // So use the values to compute the expected probability for each interval.
        final double[] expected = {
            dist.cumulativeProbability(quartiles[0]),
            dist.probability(quartiles[0], quartiles[1]),
            dist.probability(quartiles[1], quartiles[2]),
            1.0 - dist.cumulativeProbability(quartiles[2]),
        };
        // Fail if the quartiles are different from a quarter.
        // Note: Set the tolerance to a high value to allow the sampling test
        // to run with uneven intervals. This will determine if the sampler
        // if broken for this parameterization of the distribution that has
        // an incorrect CDF(inverse CDF(p)) mapping.
        final DoubleTolerance tolerance = DoubleTolerances.relative(1e-3);
        for (final double p : expected) {
            TestUtils.assertEquals(0.25, p, tolerance,
                () -> "Unexpected quartiles: " + Arrays.toString(expected));
        }

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
     * Test that density integrals match the distribution.
     * The (filtered, sorted) points array is used to source
     * integration limits. The integral of the density (estimated using a
     * Legendre-Gauss integrator) is compared with the cdf over the same
     * interval. Test points outside of the domain of the density function
     * are discarded.
     */
    @ParameterizedTest
    @MethodSource
    final void testDensityIntegrals(ContinuousDistribution dist,
                                    double[] points,
                                    double[] values,
                                    DoubleTolerance tolerance) {
        final BaseAbstractUnivariateIntegrator integrator =
            new IterativeLegendreGaussIntegrator(5, INTEGRATOR_REL_ACCURACY, INTEGRATOR_ABS_ACCURACY);
        final UnivariateFunction d = dist::density;
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
            TestUtils.assertEquals(
                dist.probability(x0, x1),
                integrator.integrate(1000000, // Integrals may be slow to converge
                                     d, x0, x1), tolerance,
                () -> "Invalid density integral: " + x0 + " to " + x1);
        }
    }

    /**
     * Test the support of the distribution matches the expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testSupport(ContinuousDistribution dist, double lower, double upper, boolean connected) {
        Assertions.assertEquals(lower, dist.getSupportLowerBound(), "lower bound");
        Assertions.assertEquals(upper, dist.getSupportUpperBound(), "upper bound");
        Assertions.assertEquals(connected, dist.isSupportConnected(), "is connected");
    }

    /**
     * Test the moments of the distribution matches the expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testMoments(ContinuousDistribution dist, double mean, double variance, DoubleTolerance tolerance) {
        TestUtils.assertEquals(mean, dist.getMean(), tolerance, "mean");
        TestUtils.assertEquals(variance, dist.getVariance(), tolerance, "variance");
    }
}
