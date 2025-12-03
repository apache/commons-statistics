/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.distribution;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.DistributionTestData.DiscreteDistributionTestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract base class for {@link DiscreteDistribution} tests.
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
 * distribution (moments and bounds) and points to test the CDF and PMF with the expected values.
 * This information can be used to evaluate the distribution CDF and PMF but also the survival
 * function, consistency of the probability computations and random sampling.
 *
 * <p>Optionally:
 * <ul>
 * <li>Points for the PMF (and log PMF) can be specified. The default will use the CDF points.
 * Note: It is not expected that evaluation of the PMF will require different points to the CDF.
 * <li>Points and expected values for the inverse CDF can be specified. These are used in
 * addition to test the inverse mapping of the CDF values to the CDF test points.
 * <li>Expected values for the log PMF can be specified. The default will use
 * {@link Math#log(double)} on the PMF values.
 * <li>Points and expected values for the survival function can be specified. These are used in
 * addition to test the inverse mapping of the SF values to the SF test points.
 * The default will use the expected CDF values (SF = 1 - CDF).
 * <li>A tolerance for equality assertions. The default is set by {@link #getAbsoluteTolerance()}
 * and {@link #getRelativeTolerance()}.
 * </ul>
 *
 * <p>If the distribution provides higher precision implementations of
 * cumulative probability and/or survival probability as the values approach zero, then test
 * points and expected values can be provided for high-precision computations. If the default
 * absolute tolerance has been set to non-zero, then very small p-values will require the
 * high-precision absolute tolerance is configured for the test to a suitable magnitude (see below).
 *
 * <p>Per-test configuration
 *
 * <p>Each test is identified with a {@link TestName} key in the properties file. This key
 * can be used to set a per-test tolerance, or disable the test:
 * <pre>
 * cdf.hp.relative = 1e-14
 * cdf.hp.absolute = 1e-50
 * sampling.disable
 * </pre>
 *
 * <p>Note: All properties files are read during test initialization. Any errors in a single
 * property file will throw an exception, invalidating the initialization and no tests
 * will be executed.
 *
 * <p>The parameterized tests in this class are inherited. The tests are final and cannot be
 * changed. This ensures each instance of a distribution is tested for all functionality in
 * the {@link DiscreteDistribution} interface. Arguments to the parameterized tests are
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
 * numeric data, and data arrays. Multi-line values can use a {@code \} character to join lines.
 * Data in the properties file will be converted to numbers using standard parsing
 * functions appropriate to the primitive type, e.g. {@link Double#parseDouble(String)}.
 * Special double values should use NaN, Infinity and -Infinity. As a convenience
 * for creating test data parsing of doubles supports the following notations from
 * other languages ('inf', 'Inf'); parsing of ints supports 'min' and 'max' for the
 * minimum and maximum integer values.
 *
 * <p>The following is a complete properties file for a distribution:
 * <pre>
 * parameters = 0.5 1.0
 * # Computed using XYZ
 * mean = 1.0
 * variance = NaN
 * # optional (default -2147483648, Integer.MIN_VALUE)
 * lower = 0
 * # optional (default 2147483647, Integer.MAX_VALUE)
 * upper = max
 * # optional (default 1e-14 or over-ridden in getRelativeTolerance())
 * tolerance.relative = 1e-9
 * # optional (default 0.0 or over-ridden in getAbsoluteTolerance())
 * tolerance.absolute = 0.0
 * cdf.points = 0, 2
 * cdf.values = 0.0, 0.5
 * # optional (default uses cdf.points)
 * pmf.points = 0, 40000
 * pmf.values = 0.0,\
 *  0.0
 * # optional (default uses log pmf.values)
 * logpmf.values = -1900.123, -Infinity
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
 * icdf.points = 0.0, 0.5
 * icdf.values = 3, 4
 * # optional inverse CDF test (defaults to ignore)
 * isf.points = 1.0, 0.5
 * isf.values = 3, 4
 * # Optional per-test tolerance and disable
 * cdf.hp.relative = 1e-14
 * cdf.hp.absolute = 1e-50
 * sampling.disable = true
 * </pre>
 *
 * <p>See {@link BinomialDistributionTest} for an example and the resource file {@code test.binomial.1.properties}.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class BaseDiscreteDistributionTest
    extends BaseDistributionTest<DiscreteDistribution, DiscreteDistributionTestData> {

    /** Threshold for the PMF summation test for a range that is too large. */
    private static final int SUM_RANGE_TOO_LARGE = 50;

    @Override
    DiscreteDistributionTestData makeDistributionData(Properties properties) {
        return new DiscreteDistributionTestData(properties);
    }

    //------------------------ Methods to stream the test data -----------------------------

    // The @MethodSource annotation will default to a no arguments method of the same name
    // as the @ParameterizedTest method. These can be overridden by child classes to
    // stream different arguments to the test case.

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test
     * points and the test tolerance.
     *
     * @param name Name of the function under test
     * @return the stream
     */
    Stream<Arguments> streamCdfTestPoints(TestName name) {
        return stream(name,
                      DiscreteDistributionTestData::getCdfPoints);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the PMF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testProbability() {
        return stream(TestName.PMF,
                      DiscreteDistributionTestData::getPmfPoints,
                      DiscreteDistributionTestData::getPmfValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the log PMF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testLogProbability() {
        return stream(TestName.LOGPMF,
                      DiscreteDistributionTestData::getPmfPoints,
                      DiscreteDistributionTestData::getLogPmfValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbability() {
        return stream(TestName.CDF,
                      DiscreteDistributionTestData::getCdfPoints,
                      DiscreteDistributionTestData::getCdfValues);
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
        return stream(TestName.SF,
                      DiscreteDistributionTestData::getSfPoints,
                      DiscreteDistributionTestData::getSfValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test points
     * and values, and the test tolerance for high-precision computations.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbabilityHighPrecision() {
        return stream(TestName.CDF_HP,
                      DiscreteDistributionTestData::getCdfHpPoints,
                      DiscreteDistributionTestData::getCdfHpValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the survival function
     * test points and values, and the test tolerance for high-precision computations.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalProbabilityHighPrecision() {
        return stream(TestName.SF_HP,
                      DiscreteDistributionTestData::getSfHpPoints,
                      DiscreteDistributionTestData::getSfHpValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the inverse CDF test points
     * and values. No test tolerance is required as the values are integers and must be exact.
     *
     * @return the stream
     */
    Stream<Arguments> testInverseCumulativeProbability() {
        return stream(TestName.ICDF,
                      DiscreteDistributionTestData::getIcdfPoints,
                      DiscreteDistributionTestData::getIcdfValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the inverse CDF test points
     * and values. No test tolerance is required as the values are integers and must be exact.
     *
     * @return the stream
     */
    Stream<Arguments> testInverseSurvivalProbability() {
        return stream(TestName.ISF,
                      DiscreteDistributionTestData::getIsfPoints,
                      DiscreteDistributionTestData::getIsfValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test and the CDF test points.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbabilityInverseMapping() {
        return stream(TestName.CDF_MAPPING,
                      DiscreteDistributionTestData::getCdfPoints);
    }

    /**
     * Create a stream of arguments containing the distribution to test and the SF test points.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalProbabilityInverseMapping() {
        return stream(TestName.SF_MAPPING,
                      DiscreteDistributionTestData::getSfPoints);
    }

    /**
     * Create a stream of arguments containing the distribution to test and
     * high-precision CDF test points.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbabilityHighPrecisionInverseMapping() {
        return stream(TestName.CDF_HP_MAPPING,
                      DiscreteDistributionTestData::getCdfHpPoints);
    }

    /**
     * Create a stream of arguments containing the distribution to test and
     * high-precision SF test points.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalProbabilityHighPrecisionInverseMapping() {
        return stream(TestName.SF_HP_MAPPING,
                      DiscreteDistributionTestData::getSfHpPoints);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test points
     * to evaluate the CDF and survival function, and the test tolerance. CDF + SF must equal 1.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalAndCumulativeProbabilityComplement() {
        // This is not disabled based on cdf.disable && sf.disable.
        // Those flags are intended to ignore tests against reference values.
        return streamCdfTestPoints(TestName.COMPLEMENT);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test points
     * to evaluate the CDF and probability in a range, and the test tolerance.
     * Used to test CDF(x1) - CDF(x0) = probability(x0, x1).
     *
     * @return the stream
     */
    Stream<Arguments> testConsistency() {
        // This is not disabled based on cdf.disable.
        // That flag is intended to ignore tests against reference values.
        return streamCdfTestPoints(TestName.CONSISTENCY);
    }

    /**
     * Create a stream of arguments containing the distribution to test and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testOutsideSupport() {
        return stream(TestName.OUTSIDE_SUPPORT);
    }

    /**
     * Create a stream of arguments containing the distribution to test, the PMF test points
     * and values. The sampled PMF should sum to more than 50% of the distribution.
     *
     * @return the stream
     */
    Stream<Arguments> testSamplingPMF() {
        return stream(TestName.SAMPLING_PMF,
                      DiscreteDistributionTestData::getPmfPoints,
                      DiscreteDistributionTestData::getPmfValues);
    }

    /**
     * Create a stream of arguments containing the distribution to test. Sampling is tested using
     * the distribution quartiles. The quartiles should be approximately 25% of the distribution
     * PMF. Discrete distributions that have single points that contain much more than 25% of the
     * probability mass will be ignored.
     *
     * @return the stream
     */
    Stream<Arguments> testSampling() {
        return stream(TestName.SAMPLING);
    }

    /**
     * Stream the arguments to test the probability sums. The test
     * sums the probability mass function between consecutive test points for the cumulative
     * density function. The default tolerance is based on the test tolerance for evaluation
     * of single points of the CDF.
     * Override this method to change the tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testProbabilitySums() {
        // Assume the test tolerance for single CDF values can be relaxed slightly
        // when summing values.
        final double scale = 10;
        final TestName cdf = TestName.CDF;
        final Function<DiscreteDistributionTestData, DoubleTolerance> tolerance =
            d -> createAbsOrRelTolerance(d.getAbsoluteTolerance(cdf) * scale,
                                         d.getRelativeTolerance(cdf) * scale);
        final TestName name = TestName.PMF_SUM;
        return stream(d -> d.isDisabled(name),
                      DiscreteDistributionTestData::getCdfPoints,
                      DiscreteDistributionTestData::getCdfValues,
                      tolerance, name.toString());
    }

    /**
     * Create a stream of arguments containing the distribution to test, the support
     * lower and upper bound, and the support connect flag.
     *
     * @return the stream
     */
    Stream<Arguments> testSupport() {
        return streamArguments(TestName.SUPPORT,
            d -> Arguments.of(namedDistribution(d.getParameters()), d.getLower(), d.getUpper()));
    }

    /**
     * Create a stream of arguments containing the distribution to test, the mean
     * and variance, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testMoments() {
        final TestName name = TestName.MOMENTS;
        return streamArguments(name,
            d -> Arguments.of(namedDistribution(d.getParameters()), d.getMean(), d.getVariance(),
                              createTestTolerance(d, name)));
    }

    /**
     * Create a stream of arguments containing the distribution to test.
     *
     * @return the stream
     */
    Stream<Arguments> testMedian() {
        return streamArguments(TestName.MEDIAN,
            d -> Arguments.of(namedDistribution(d.getParameters())));
    }

    //------------------------ Tests -----------------------------

    // Tests are final. It is expected that the test can be modified by overriding
    // the method used to stream the arguments, for example to use a specific tolerance
    // for a test in preference to the tolerance defined in the properties file.

    // Extract the tests from the previous abstract test

    /**
     * Test that probability calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testProbability(DiscreteDistribution dist,
                               int[] points,
                               double[] values,
                               DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            TestUtils.assertEquals(values[i],
                dist.probability(x), tolerance,
                () -> "Incorrect probability mass value returned for " + x);
        }
    }

    /**
     * Test that logarithmic probability calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testLogProbability(DiscreteDistribution dist,
                                  int[] points,
                                  double[] values,
                                  DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            TestUtils.assertEquals(values[i],
                dist.logProbability(x), tolerance,
                () -> "Incorrect log probability mass value returned for " + x);
        }
    }

    /**
     * Test that cumulative probability density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbability(DiscreteDistribution dist,
                                         int[] points,
                                         double[] values,
                                         DoubleTolerance tolerance) {
        // verify cumulativeProbability(double)
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
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
    final void testSurvivalProbability(DiscreteDistribution dist,
                                       int[] points,
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
     * Test that cumulative probability is not {@code (1 - survival probability)} by testing values
     * that would result in inaccurate results if simply calculating (1 - sf).
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityHighPrecision(DiscreteDistribution dist,
                                                      int[] points,
                                                      double[] values,
                                                      DoubleTolerance tolerance) {
        assertHighPrecision(tolerance, values);
        testCumulativeProbability(dist, points, values, tolerance);
    }

    /**
     * Test that survival probability is not {@code (1 - cumulative probability)} by testing values
     * that would result in inaccurate results if simply calculating (1 - cdf).
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbabilityHighPrecision(DiscreteDistribution dist,
                                                    int[] points,
                                                    double[] values,
                                                    DoubleTolerance tolerance) {
        assertHighPrecision(tolerance, values);
        testSurvivalProbability(dist, points, values, tolerance);
    }

    /**
     * Test that inverse cumulative probability density calculations match expected values.
     *
     * <p>Note: Any expected values outside the support of the distribution are ignored.
     */
    @ParameterizedTest
    @MethodSource
    final void testInverseCumulativeProbability(DiscreteDistribution dist,
                                                double[] points,
                                                int[] values) {
        final int lower = dist.getSupportLowerBound();
        final int upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final int x = values[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = points[i];
            Assertions.assertEquals(
                x,
                dist.inverseCumulativeProbability(p),
                () -> "Incorrect inverse cumulative probability value returned for " + p);
        }
    }

    /**
     * Test that inverse survival probability density calculations match expected values.
     *
     * <p>Note: Any expected values outside the support of the distribution are ignored.
     */
    @ParameterizedTest
    @MethodSource
    final void testInverseSurvivalProbability(DiscreteDistribution dist,
                                              double[] points,
                                              int[] values) {
        final int lower = dist.getSupportLowerBound();
        final int upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final int x = values[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = points[i];
            Assertions.assertEquals(
                x,
                dist.inverseSurvivalProbability(p),
                () -> "Incorrect inverse survival probability value returned for " + p);
        }
    }

    /**
     * Test that an inverse mapping of the cumulative probability density values matches
     * the original point, {@code x = icdf(cdf(x))}.
     *
     * <p>Note: It is possible for two points to compute the same CDF value. In this
     * case the mapping is not a bijection. Any points computing a CDF=0 or 1 are ignored
     * as this is expected to be inverted to the domain bound.
     *
     * <p>Note: Any points outside the support of the distribution are ignored.
     *
     * <p>This test checks consistency of the inverse with the forward function.
     * The test checks (where applicable):
     * <ul>
     *  <li>{@code icdf( cdf(x) ) = x}
     *  <li>{@code icdf( p > cdf(x) ) >= x+1}
     *  <li>{@code icdf( cdf(x-1) < p < cdf(x) ) = x}
     * </ul>
     *
     * <p>Does not check {@code isf( 1 - cdf(x) ) = x} since the complement {@code q = 1 - p}
     * is inexact. The bound change for the isf to compute x may be different. The isf bound
     * change is verified in a separate test.
     * Thus the {@code icdf <-> cdf} mapping and {@code isf <-> sf} mapping are verified to
     * have correct boundary changes with respect to the forward function and its inverse
     * but the boundaries are allowed to be different. This can be corrected for example
     * with an implementation that has a consistent computation for {@code x > median} and
     * another for {@code x < median} with an inverse computation determined by {@code p > 0.5}.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityInverseMapping(DiscreteDistribution dist,
                                                       int[] points) {
        final int lower = dist.getSupportLowerBound();
        final int upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = dist.cumulativeProbability(x);
            if ((int) p == p) {
                // Assume mapping not a bijection and ignore
                continue;
            }
            final double x1 = dist.inverseCumulativeProbability(p);
            Assertions.assertEquals(
                x,
                x1,
                () -> "Incorrect CDF inverse value returned for " + p);
            // The next p value up should return the next value
            final double pp = Math.nextUp(p);
            if (x != upper && pp != 1 && p != dist.cumulativeProbability(x + 1)) {
                final double x2 = dist.inverseCumulativeProbability(pp);
                Assertions.assertEquals(
                    x + 1,
                    x2,
                    () -> "Incorrect CDF inverse value returned for " + pp);
            }

            // Invert a probability inside the range to the previous CDF value
            if (x != lower) {
                final double pm1 = dist.cumulativeProbability(x - 1);
                final double px = (pm1 + p) / 2;
                if (px > pm1) {
                    final double xx = dist.inverseCumulativeProbability(px);
                    Assertions.assertEquals(
                        x,
                        xx,
                        () -> "Incorrect CDF inverse value returned for " + px);
                }
            }
        }
    }

    /**
     * Test that an inverse mapping of the survival probability density values matches
     * the original point, {@code x = isf(sf(x))}.
     *
     * <p>Note: It is possible for two points to compute the same SF value. In this
     * case the mapping is not a bijection. Any points computing a SF=0 or 1 are ignored
     * as this is expected to be inverted to the domain bound.
     *
     * <p>Note: Any points outside the support of the distribution are ignored.
     *
     * <p>This test checks consistency of the inverse with the forward function.
     * The test checks (where applicable):
     * <ul>
     *  <li>{@code isf( sf(x) ) = x}
     *  <li>{@code isf( p < sf(x) ) >= x+1}
     *  <li>{@code isf( sf(x-1) > p > sf(x) ) = x}
     * </ul>
     *
     * <p>Does not check {@code icdf( 1 - sf(x) ) = x} since the complement {@code q = 1 - p}
     * is inexact. The bound change for the icdf to compute x may be different. The icdf bound
     * change is verified in a separate test.
     * Thus the {@code icdf <-> cdf} mapping and {@code isf <-> sf} mapping are verified to
     * have correct boundary changes with respect to the forward function and its inverse
     * but the boundaries are allowed to be different. This can be corrected for example
     * with an implementation that has a consistent computation for {@code x > median} and
     * another for {@code x < median} with an inverse computation determined by {@code p > 0.5}.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbabilityInverseMapping(DiscreteDistribution dist,
                                                     int[] points) {
        final int lower = dist.getSupportLowerBound();
        final int upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = dist.survivalProbability(x);
            if ((int) p == p) {
                // Assume mapping not a bijection and ignore
                continue;
            }
            final double x1 = dist.inverseSurvivalProbability(p);
            Assertions.assertEquals(
                x,
                x1,
                () -> "Incorrect SF inverse value returned for " + p);

            // The next p value down should return the next value
            final double pp = Math.nextDown(p);
            if (x != upper && pp != 0 && p != dist.survivalProbability(x + 1)) {
                final double x2 = dist.inverseSurvivalProbability(pp);
                Assertions.assertEquals(
                    x + 1,
                    x2,
                    () -> "Incorrect SF inverse value returned for " + pp);
            }

            // Invert a probability inside the range to the previous SF value
            if (x != lower) {
                final double pm1 = dist.survivalProbability(x - 1);
                final double px = (pm1 + p) / 2;
                if (px < pm1) {
                    final double xx = dist.inverseSurvivalProbability(px);
                    Assertions.assertEquals(
                        x,
                        xx,
                        () -> "Incorrect SF inverse value returned for " + px);
                }
            }
        }
    }

    /**
     * Test that an inverse mapping of the cumulative probability density values matches
     * the original point, {@code x = icdf(cdf(x))} using the points for the high-precision
     * CDF.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityHighPrecisionInverseMapping(
            DiscreteDistribution dist,
            int[] points) {
        testCumulativeProbabilityInverseMapping(dist, points);
    }

    /**
     * Test that an inverse mapping of the survival probability density values matches
     * the original point, {@code x = isf(sf(x))} using the points for the high-precision
     * SF.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbabilityHighPrecisionInverseMapping(
            DiscreteDistribution dist,
            int[] points) {
        testSurvivalProbabilityInverseMapping(dist, points);
    }

    /**
     * Test that cumulative probability density and survival probability calculations
     * sum to approximately 1.0.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalAndCumulativeProbabilityComplement(DiscreteDistribution dist,
                                                              int[] points,
                                                              DoubleTolerance tolerance) {
        for (final int x : points) {
            TestUtils.assertEquals(
                1.0,
                dist.survivalProbability(x) + dist.cumulativeProbability(x),
                tolerance,
                () -> "survival + cumulative probability were not close to 1.0 for " + x);
        }
    }

    /**
     * Test that probability computations are consistent.
     * This checks probability(x, x) = 0; probability(x, x+1) = probability(x+1)
     * and probability(x0, x1) = CDF(x1) - CDF(x0).
     */
    @ParameterizedTest
    @MethodSource
    final void testConsistency(DiscreteDistribution dist,
                               int[] points,
                               DoubleTolerance tolerance) {
        final int upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final int x0 = points[i];

            // Check that probability(x, x) == 0
            Assertions.assertEquals(
                0.0,
                dist.probability(x0, x0),
                () -> "Non-zero probability(x, x) for " + x0);

            // Check that probability(x, x + 1) == probability(x + 1)
            if (x0 < upper) {
                Assertions.assertEquals(
                    dist.probability(x0 + 1),
                    dist.probability(x0, x0 + 1),
                    () -> "probability(x + 1) != probability(x, x + 1) for " + x0);
            }

            final double cdf0 = dist.cumulativeProbability(x0);
            final double sf0 = cdf0 >= 0.5 ? dist.survivalProbability(x0) : Double.NaN;
            for (int j = 0; j < points.length; j++) {
                final int x1 = points[j];
                // Ignore adjacent points.
                // Use long arithmetic to avoid overflow if x0 is the maximum integer value
                if (x0 + 1L < x1) {
                    // Check that probability(x0, x1) = CDF(x1) - CDF(x0).
                    // If x0 is above the median it is more accurate to use the
                    // survival probability: probability(x0, x1) = SF(x0) - SF(x1).
                    double expected;
                    if (cdf0 >= 0.5) {
                        expected = sf0 - dist.survivalProbability(x1);
                    } else {
                        expected = dist.cumulativeProbability(x1) - cdf0;
                    }
                    TestUtils.assertEquals(
                        expected,
                        dist.probability(x0, x1),
                        tolerance,
                        () -> "Inconsistent probability for (" + x0 + "," + x1 + ")");
                } else if (x0 > x1) {
                    Assertions.assertThrows(IllegalArgumentException.class,
                        () -> dist.probability(x0, x1),
                        "probability(int, int) should have thrown an exception that first argument is too large");
                }
            }
        }
    }

    /**
     * Test CDF and inverse CDF values at the edge of the support of the distribution return
     * expected values and the CDF outside the support returns consistent values.
     */
    @ParameterizedTest
    @MethodSource
    final void testOutsideSupport(DiscreteDistribution dist,
                                  DoubleTolerance tolerance) {
        // Test various quantities when the variable is outside the support.
        final int lo = dist.getSupportLowerBound();
        TestUtils.assertEquals(dist.probability(lo), dist.cumulativeProbability(lo), tolerance, () -> "pmf(lower) != cdf(lower) for " + lo);
        Assertions.assertEquals(lo, dist.inverseCumulativeProbability(-0.0), "icdf(-0.0)");
        Assertions.assertEquals(lo, dist.inverseCumulativeProbability(0.0), "icdf(0.0)");
        Assertions.assertEquals(lo, dist.inverseSurvivalProbability(1.0), "isf(1.0)");

        if (lo != Integer.MIN_VALUE) {
            final int below = lo - 1;
            Assertions.assertEquals(0.0, dist.probability(below), "pmf(x < lower)");
            Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logProbability(below), "logpmf(x < lower)");
            Assertions.assertEquals(0.0, dist.cumulativeProbability(below), "cdf(x < lower)");
            Assertions.assertEquals(1.0, dist.survivalProbability(below), "sf(x < lower)");
        }

        final int hi = dist.getSupportUpperBound();
        Assertions.assertTrue(lo <= hi, "lower <= upper");
        Assertions.assertEquals(hi, dist.inverseCumulativeProbability(1.0), "icdf(1.0)");
        Assertions.assertEquals(hi, dist.inverseSurvivalProbability(-0.0), "isf(-0.0)");
        Assertions.assertEquals(hi, dist.inverseSurvivalProbability(0.0), "isf(0.0)");
        if (hi != Integer.MAX_VALUE) {
            // For distributions defined up to integer max value we cannot test that
            // the CDF is 1.0 as they may be truncated.
            Assertions.assertEquals(1.0, dist.cumulativeProbability(hi), "cdf(upper)");
            Assertions.assertEquals(0.0, dist.survivalProbability(hi), "sf(upper)");
            TestUtils.assertEquals(dist.probability(hi), dist.survivalProbability(hi - 1), tolerance, () -> "pmf(upper - 1) != sf(upper - 1) for " + hi);

            final int above = hi + 1;
            Assertions.assertEquals(0.0, dist.probability(above), "pmf(x > upper)");
            Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logProbability(above), "logpmf(x > upper)");
            Assertions.assertEquals(1.0, dist.cumulativeProbability(above), "cdf(x > upper)");
            Assertions.assertEquals(0.0, dist.survivalProbability(above), "sf(x > upper)");
        }

        // Test the logProbability at the support bound. This hits edge case coverage for logProbability.
        assertPmfAndLogPmfAtBound(dist, lo, tolerance, "lower");
        assertPmfAndLogPmfAtBound(dist, hi, tolerance, "upper");
    }

    /**
     * Assert the PMF and log PMF are consistent at the named point.
     * This method asserts either:
     * <pre>
     * log(pmf) == logpmf
     * pmf      == exp(logpmf)
     * </pre>
     *
     * @param dist Distribution
     * @param x Point
     * @param tolerance Test tolerance
     * @param name Point name
     */
    private static void assertPmfAndLogPmfAtBound(DiscreteDistribution dist, int x,
                                                  DoubleTolerance tolerance, String name) {
        // It is assumed the log probability may support a value when the plain probability will be zero.
        // Only assert Math.log(dist.probability(x)) == dist.logProbability(x) for normal values.
        final double p = dist.probability(x);
        final double logp = dist.logProbability(x);
        if (p > Double.MIN_NORMAL) {
            TestUtils.assertEquals(Math.log(p), logp, tolerance,
                () -> String.format("%s: log(pmf(%d)) != logpmf(%d)", name, x, x));
        } else {
            TestUtils.assertEquals(p, Math.exp(logp), tolerance,
                () -> String.format("%s: pmf(%d) != exp(logpmf(%d))", name, x, x));
        }
    }

    /**
     * Test invalid probabilities passed to computations that require a p-value in {@code [0, 1]}
     * or a range where {@code p1 <= p2}.
     */
    @ParameterizedTest
    @MethodSource(value = "streamDistribution")
    final void testInvalidProbabilities(DiscreteDistribution dist) {
        final int lo = dist.getSupportLowerBound();
        final int hi = dist.getSupportUpperBound();
        if (lo < hi) {
            Assertions.assertThrows(DistributionException.class, () -> dist.probability(hi, lo), "x0 > x1");
        }
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(-1), "p < 0.0");
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(2), "p > 1.0");
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseSurvivalProbability(-1), "q < 0.0");
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseSurvivalProbability(2), "q > 1.0");
    }

    /**
     * Test sampling from the distribution.
     * This test uses the points that are used to test the distribution PMF.
     * The test is skipped if the sum of the PMF values is less than 0.5.
     */
    @ParameterizedTest
    @MethodSource
    final void testSamplingPMF(DiscreteDistribution dist,
                               int[] points,
                               double[] values) {
        // This test uses the points that are used to test the distribution PMF.
        // The sum of the probability values does not have to be 1 (or very close to 1).
        // Any value generated by the sampler that is not an expected point will
        // be ignored. If the sum of probabilities is above 0.5 then at least half
        // of the samples should be counted and the test will verify these occur with
        // the expected relative frequencies. Note: The expected values are normalised
        // to 1 (i.e. relative frequencies) by the Chi-square test.
        points = points.clone();
        values = values.clone();
        final int length = TestUtils.eliminateZeroMassPoints(points, values);
        final double[] expected = Arrays.copyOf(values, length);

        // This test will not be valid if the points do not represent enough of the PMF.
        // Require at least 50%.
        final double sum = Arrays.stream(expected).sum();
        Assumptions.assumeTrue(sum > 0.5,
            () -> "Not enough of the PMF is tested during sampling: " + sum);

        // Use fixed seed.
        final DiscreteDistribution.Sampler sampler =
                dist.createSampler(RandomSource.XO_SHI_RO_256_PP.create(1234567890L));

        // Edge case for distributions with all mass in a single point
        if (length == 1) {
            final int point = points[0];
            for (int i = 0; i < 20; i++) {
                Assertions.assertEquals(point, sampler.sample());
            }
            return;
        }

        final int sampleSize = 1000;
        MathArrays.scaleInPlace(sampleSize, expected);

        final int[] sample = TestUtils.sample(sampleSize, sampler);

        final long[] counts = new long[length];
        for (int i = 0; i < sampleSize; i++) {
            final int x = sample[i];
            for (int j = 0; j < length; j++) {
                if (x == points[j]) {
                    counts[j]++;
                    break;
                }
            }
        }

        TestUtils.assertChiSquareAccept(points, expected, counts, 0.001);
    }

    /**
     * Test sampling from the distribution using quartiles.
     * This test is ignored if the range for the distribution PMF is small
     * and the quartiles do not map to approximately 0.25. When the range of
     * the distribution is small then the {@link #testSamplingPMF(DiscreteDistribution, int[], double[])}
     * method should be used with points that covers at least 50% of the PMF.
     */
    @ParameterizedTest
    @MethodSource
    final void testSampling(DiscreteDistribution dist) {
        final int[] quartiles = TestUtils.getDistributionQuartiles(dist);
        // The distribution quartiles are created using the inverse CDF.
        // This may not be accurate for extreme parameterizations of the distribution.
        // So use the values to compute the expected probability for each interval.
        final double[] expected = {
            dist.cumulativeProbability(quartiles[0]),
            dist.probability(quartiles[0], quartiles[1]),
            dist.probability(quartiles[1], quartiles[2]),
            dist.survivalProbability(quartiles[2]),
        };
        // Ignore this test if the quartiles are different from a quarter.
        // This will exclude distributions where the PMF is heavily concentrated
        // at a single point. In this case it can be realistically
        // sampled using a small number of points.
        final DoubleTolerance tolerance = DoubleTolerances.absolute(0.1);
        for (final double p : expected) {
            Assumptions.assumeTrue(tolerance.test(0.25, p),
                () -> "Unexpected quartiles: " + Arrays.toString(expected));
        }

        final int sampleSize = 1000;
        MathArrays.scaleInPlace(sampleSize, expected);

        // Use fixed seed.
        final DiscreteDistribution.Sampler sampler =
            dist.createSampler(RandomSource.XO_SHI_RO_256_PP.create(123456789L));
        final int[] sample = TestUtils.sample(sampleSize, sampler);

        final long[] counts = new long[4];
        for (int i = 0; i < sampleSize; i++) {
            TestUtils.updateCounts(sample[i], counts, quartiles);
        }

        TestUtils.assertChiSquareAccept(expected, counts, 0.001);
    }

    /**
     * Test that probability sums match the distribution.
     * The (filtered, sorted) points array is used to source
     * summation limits. The sum of the probability mass function
     * is compared with the probability over the same interval.
     * Test points outside of the domain of the probability function
     * are discarded and large intervals are ignored.
     *
     * <p>This test is ignored for large ranges.
     */
    @ParameterizedTest
    @MethodSource
    final void testProbabilitySums(DiscreteDistribution dist,
                                   int[] points,
                                   double[] values,
                                   DoubleTolerance tolerance) {
        final ArrayList<Integer> integrationTestPoints = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            if (Double.isNaN(values[i]) ||
                values[i] < 1e-5 ||
                values[i] > 1 - 1e-5) {
                continue; // exclude sums outside domain.
            }
            integrationTestPoints.add(points[i]);
        }
        Collections.sort(integrationTestPoints);

        for (int i = 1; i < integrationTestPoints.size(); i++) {
            final int x0 = integrationTestPoints.get(i - 1);
            final int x1 = integrationTestPoints.get(i);
            // Ignore large ranges
            if (x1 - x0 > SUM_RANGE_TOO_LARGE) {
                continue;
            }
            final double sum = IntStream.rangeClosed(x0 + 1, x1).mapToDouble(dist::probability).sum();
            TestUtils.assertEquals(dist.probability(x0, x1), sum, tolerance,
                () -> "Invalid probability sum: " + (x0 + 1) + " to " + x1);
        }
    }

    /**
     * Test the support of the distribution matches the expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testSupport(DiscreteDistribution dist, double lower, double upper) {
        Assertions.assertEquals(lower, dist.getSupportLowerBound(), "lower bound");
        Assertions.assertEquals(upper, dist.getSupportUpperBound(), "upper bound");
    }

    /**
     * Test the moments of the distribution matches the expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testMoments(DiscreteDistribution dist, double mean, double variance, DoubleTolerance tolerance) {
        TestUtils.assertEquals(mean, dist.getMean(), tolerance, "mean");
        TestUtils.assertEquals(variance, dist.getVariance(), tolerance, "variance");
    }

    /**
     * Test the median of the distribution is equal to the value returned from the inverse CDF.
     * The median is used internally for computation of the probability of a range
     * using either the CDF or survival function. If overridden by a distribution it should
     * be equivalent to the inverse CDF called with 0.5.
     *
     * <p>The method modifiers are asserted to check the method is not public or protected.
     */
    @ParameterizedTest
    @MethodSource
    final void testMedian(DiscreteDistribution dist) {
        if (dist instanceof AbstractDiscreteDistribution) {
            final AbstractDiscreteDistribution d = (AbstractDiscreteDistribution) dist;
            Assertions.assertEquals(d.inverseCumulativeProbability(0.5), d.getMedian(), "median");
            assertMethodNotModified(dist.getClass(), Modifier.PUBLIC | Modifier.PROTECTED, "getMedian");
        }
    }
}
