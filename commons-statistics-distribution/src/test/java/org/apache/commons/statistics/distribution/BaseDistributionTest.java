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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
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
 * Abstract base class for distribution tests.
 *
 * <p>This class uses parameterized tests that are repeated for instances of a
 * distribution. The distribution, test input and expected values are generated
 * dynamically from properties files loaded from resources.
 *
 * <p>The class has two specializations for testing {@link ContinuousDistribution} and
 * {@link DiscreteDistribution}. It is not intended to extend this class when creating
 * a test for a new distribution. This class exists for the sole purpose of containing
 * common functionality to search for and load properties files containing the distribution
 * data.
 *
 * <p>To test a new distribution extend the specialized classes:
 * <ul>
 * <li>{@link BaseContinuousDistributionTest}
 * <li>{@link BaseDiscreteDistributionTest}
 * </ul>
 *
 * @param <T> Distribution type
 * @param <D> Distribution data type
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class BaseDistributionTest<T, D extends DistributionTestData> {
    /**
     * The smallest value (epsilon) for the relative error of a {@code double}.
     * Set the relative error to an integer factor of this to test very
     * small differences as errors of units in the last place (ULP).
     * Assumes the relative error is:
     * <pre>
     *      |x - y|
     *   -------------
     *   max(|x|, |y|)
     * </pre>
     *
     * <p>Value is 2.220446049250313E-16.
     */
    static final double RELATIVE_EPS = Math.ulp(1.0);

    /** The test data. Protected to allow use in sub-classes. */
    protected final List<D> data = new ArrayList<>();

    /**
     * Setup the test using data loaded from resource files.
     * Resource files are assumed to be named sequentially from 1:
     * <pre>
     * test.distname.1.properties
     * test.distname.2.properties
     * </pre>
     * <p>Where {@code distname} is the name of the distribution. The name
     * is dynamically created in {@link #getDistributionName()} and can be
     * overridden by implementing classes.
     */
    @BeforeAll
    void setup() {
        final String key = getDistributionName().toLowerCase(Locale.ROOT);
        // Set defaults
        final Properties defaults = new Properties();
        defaults.setProperty(DistributionTestData.KEY_TOLERANCE_ABSOLUTE, String.valueOf(getAbsoluteTolerance()));
        defaults.setProperty(DistributionTestData.KEY_TOLERANCE_RELATIVE, String.valueOf(getRelativeTolerance()));
        for (int i = 1; ; i++) {
            final String filename = String.format("test.%s.%d.properties", key, i);
            try (InputStream resource = this.getClass().getResourceAsStream(
                    filename)) {
                if (resource == null) {
                    break;
                }
                // Load properties file
                final Properties prop = new Properties(defaults);
                prop.load(resource);
                // Convert the properties to a D instance
                data.add(makeDistributionData(prop));
            } catch (IOException | NullPointerException | IllegalArgumentException e) {
                Assertions.fail("Failed to load test data: " + filename, e);
            }
        }
    }

    /**
     * Gets the default absolute tolerance used in comparing expected and returned values.
     *
     * <p>The initial value is 0.0 (disabled).
     *
     * <p>Override this method to set the <strong>default</strong> absolute tolerance for all test
     * cases defined by a properties file. Any properties file with an absolute tolerance entry
     * ignores this value.
     *
     * <p>Notes: Floating-point values are considered equal using the absolute or the relative tolerance.
     * See {@link #createTolerance()}.
     *
     * @return the absolute tolerance
     */
    protected double getAbsoluteTolerance() {
        return 0.0;
    }

    /**
     * Gets the default relative tolerance used in comparing expected and returned values.
     *
     * <p>The initial value is 1e-12.
     *
     * <p>Override this method to set the <strong>default</strong> relative tolerance for all test
     * cases defined by a properties file. Any properties file with a relative tolerance entry
     * ignores this value.
     *
     * <p>Notes: Floating-point values are considered equal using the absolute or the relative tolerance.
     * See {@link #createTolerance()}.
     *
     * @return the relative tolerance
     */
    protected double getRelativeTolerance() {
        return 1e-12;
    }

    /**
     * Gets the distribution name. This is used to search for test case resource files.
     *
     * <p>The default implementation removes the text {@code DistributionTest} from the
     * simple class name.
     *
     * @return the distribution name
     * @see Class#getSimpleName()
     */
    String getDistributionName() {
        return getClass().getSimpleName().replace("DistributionTest", "");
    }

    /**
     * Create a new distribution data instance from the properties.
     *
     * @param properties Properties
     * @return the distribution data
     */
    abstract D makeDistributionData(Properties properties);

    /**
     * Create a new distribution instance from the parameters.
     * It is assumed the parameters match the order of the parameter constructor.
     *
     * @param parameters Parameters of the distribution.
     * @return the distribution
     */
    abstract T makeDistribution(Object... parameters);

    /** Creates invalid parameters that are expected to throw an exception when passed to
     * the {@link #makeDistribution(Object...)} method.
     *
     * <p>This may return as many inner parameter arrays as is required to test all permutations
     * of invalid parameters to the distribution.
     * @return Array of invalid parameter arrays
     */
    abstract Object[][] makeInvalidParameters();

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


    //------------------------ Helper Methods to create test tolerances---------------------------

    /**
     * Creates the tolerance using an absolute error.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical
     * equality is used.
     *
     * @param eps Absolute tolerance
     * @return the tolerance
     */
    DoubleTolerance createAbsTolerance(double eps) {
        return eps > 0 ? DoubleTolerances.absolute(eps) : DoubleTolerances.ulps(0);
    }

    /**
     * Creates the tolerance using an relative error.
     *
     * <p>If the relative tolerance is zero it is ignored and a tolerance of numerical
     * equality is used.
     *
     * @param eps Relative tolerance
     * @return the tolerance
     */
    DoubleTolerance createRelTolerance(double eps) {
        return eps > 0 ? DoubleTolerances.relative(eps) : DoubleTolerances.ulps(0);
    }

    /**
     * Creates the tolerance using an {@code Or} combination of absolute and relative error.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical equality
     * is used.
     *
     * <p>If the relative tolerance is zero it is ignored.
     *
     * @param absTolerance Absolute tolerance
     * @param relTolerance Relative tolerance
     * @return the tolerance
     */
    DoubleTolerance createAbsOrRelTolerance(double absTolerance, double relTolerance) {
        final DoubleTolerance tol = createAbsTolerance(absTolerance);
        return relTolerance > 0 ? tol.or(DoubleTolerances.relative(relTolerance)) : tol;
    }

    /**
     * Creates the tolerance using an {@code Or} combination of absolute and relative error
     * defined in the test data.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical equality
     * is used.
     *
     * <p>If the relative tolerance is zero it is ignored.
     *
     * @param testData Test data
     * @return the tolerance
     */
    DoubleTolerance createTestTolerance(D testData) {
        final double abs = testData.getAbsoluteTolerance();
        final double rel = testData.getRelativeTolerance();
        return createAbsOrRelTolerance(abs, rel);
    }

    /**
     * Creates the tolerance for the named test using an {@code Or} combination of absolute
     * and relative error defined in the test data. If the named test tolerance is not defined
     * then this uses the default tolerance.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical equality
     * is used.
     *
     * <p>If the relative tolerance is zero it is ignored.
     *
     * @param testData Test data
     * @param name Name of the function under test
     * @return the tolerance
     */
    DoubleTolerance createTestTolerance(D testData, TestName name) {
        final double abs = testData.getAbsoluteTolerance(name);
        final double rel = testData.getRelativeTolerance(name);
        return createAbsOrRelTolerance(abs, rel);
    }

    /**
     * Creates the default tolerance.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical equality
     * is used.
     *
     * <p>If the relative tolerance is zero it is ignored.
     *
     * @return the tolerance
     */
    DoubleTolerance createTolerance() {
        return createAbsOrRelTolerance(getAbsoluteTolerance(),
                                       getRelativeTolerance());
    }

    //------------------------ Methods to stream the test data -----------------------------

    // The @MethodSource annotation will default to a no arguments method of the same name
    // as the @ParameterizedTest method. These can be overridden by child classes to
    // stream different arguments to the test case.

    /**
     * Create a named argument for the distribution from the parameters.
     * This is a convenience method to present the distribution with a short name in a test report.
     *
     * <p>This is used to create a new instance of the distribution for a test.
     *
     * @param parameters Parameters of the distribution.
     * @return the distribution argument
     */
    Named<T> namedDistribution(Object... parameters) {
        final T dist = makeDistribution(parameters);
        final String name = dist.getClass().getSimpleName() + " " + Arrays.toString(parameters);
        return Named.of(name, dist);
    }

    /**
     * Create a named argument for the array.
     * This is a convenience method to present arrays with a short name in a test report.
     * May be overridden for example to output more array details.
     *
     * @param name Name
     * @param array Array
     * @return the named argument
     */
    Named<?> namedArray(String name, Object array) {
        if (array instanceof double[]) {
            return namedArray(name, (double[]) array);
        }
        if (array instanceof int[]) {
            return namedArray(name, (int[]) array);
        }
        return Named.of(name, array);
    }

    /**
     * Create a named argument for the array.
     * This is a convenience method to present arrays with a short name in a test report.
     * May be overridden for example to output more array details.
     *
     * @param name Name
     * @param array Array
     * @return the named argument
     */
    Named<double[]> namedArray(String name, double[] array) {
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

    /**
     * Create a named argument for the array.
     * This is a convenience method to present arrays with a short name in a test report.
     * May be overridden for example to output more array details.
     *
     * @param name Name
     * @param array Array
     * @return the named argument
     */
    Named<int[]> namedArray(String name, int[] array) {
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

    /**
     * Create a stream of arguments containing the distribution to test.
     *
     * @return the stream
     */
    Stream<Arguments> streamDistribution() {
        return data.stream().map(d -> Arguments.of(namedDistribution(d.getParameters())));
    }

    /**
     * Create a stream of arguments containing the distribution to test and the test tolerance.
     * The tolerance is identified using functions on the test instance data.
     * The test data will be skipped if disabled.
     *
     * @param name Name of the function under test
     * @return the stream
     */
    Stream<Arguments> stream(TestName name) {
        final Builder<Arguments> b = Stream.builder();
        final int[] size = {0};
        data.forEach(d -> {
            if (d.isDisabled(name)) {
                return;
            }
            size[0]++;
            b.accept(Arguments.of(namedDistribution(d.getParameters()),
                     createTestTolerance(d, name)));
        });
        Assumptions.assumeTrue(size[0] != 0, () -> "Distribution has no data for " + name);
        return b.build();
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test
     * points, and the test tolerance. The points and tolerance
     * are identified using functions on the test instance data.
     * The test data will be skipped if disabled or the length of the points is zero.
     *
     * <p>If all test data is skipped then a
     * {@link org.opentest4j.TestAbortedException TestAbortedException} is raised.
     *
     * @param name Name of the function under test
     * @param points Function to create the points
     * @return the stream
     */
    <P> Stream<Arguments> stream(TestName name,
                                 Function<D, P> points) {
        final Builder<Arguments> b = Stream.builder();
        final int[] size = {0};
        data.forEach(d -> {
            final P p = points.apply(d);
            if (d.isDisabled(name) || TestUtils.getLength(p) == 0) {
                return;
            }
            size[0]++;
            b.accept(Arguments.of(namedDistribution(d.getParameters()),
                     namedArray("points", p),
                     createTestTolerance(d, name)));
        });
        Assumptions.assumeTrue(size[0] != 0, () -> "Distribution has no data for " + name);
        return b.build();
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test
     * points, test values and the test tolerance. The points, values and tolerance
     * are identified using functions on the test instance data.
     * The test data will be skipped if disabled or the length of the points or values is zero.
     *
     * <p>If all test data is skipped then a
     * {@link org.opentest4j.TestAbortedException TestAbortedException} is raised.
     *
     * @param name Name of the function under test
     * @param points Function to create the points
     * @param values Function to create the values
     * @return the stream
     */
    <P, V> Stream<Arguments> stream(TestName name,
                                    Function<D, P> points,
                                    Function<D, V> values) {
        // Delegate
        return stream(d -> d.isDisabled(name),
                      points,
                      values,
                      d -> createTestTolerance(d, name),
                      name.toString());
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test
     * points, test values and the test tolerance. The points, values and tolerance
     * are identified using functions on the test instance data.
     * The test data will be skipped if disabled or the length of the points or values is zero.
     *
     * <p>If all test data is skipped then a
     * {@link org.opentest4j.TestAbortedException TestAbortedException} is raised.
     *
     * @param filter Filter applied on the test data. If true the data is ignored.
     * @param points Function to create the points
     * @param values Function to create the values
     * @param tolerance Function to create the tolerance
     * @param name Name of the function under test
     * @return the stream
     */
    <P, V> Stream<Arguments> stream(Predicate<D> filter,
                                    Function<D, P> points,
                                    Function<D, V> values,
                                    Function<D, DoubleTolerance> tolerance,
                                    String name) {
        final Builder<Arguments> b = Stream.builder();
        final int[] size = {0};
        data.forEach(d -> {
            final P p = points.apply(d);
            final V v = values.apply(d);
            if (filter.test(d) || TestUtils.getLength(p) == 0 || TestUtils.getLength(v) == 0) {
                return;
            }
            size[0]++;
            b.accept(Arguments.of(namedDistribution(d.getParameters()),
                     namedArray("points", p),
                     namedArray("values", v),
                     tolerance.apply(d)));
        });
        Assumptions.assumeTrue(size[0] != 0, () -> "Distribution has no data for " + name);
        return b.build();
    }

    /**
     * Create a stream of arguments built using the provided mapping function.
     * The test data will be skipped if disabled.
     *
     * <p>If all test data is skipped then a
     * {@link org.opentest4j.TestAbortedException TestAbortedException} is raised.
     *
     * @param name Name of the function under test
     * @param mappingFunction Function to create the arguments for the test data
     * @return the stream
     */
    Stream<Arguments> streamArguments(TestName name,
                                      Function<D, Arguments> mappingFunction) {
        final Builder<Arguments> b = Stream.builder();
        final int[] size = {0};
        data.forEach(d -> {
            if (d.isDisabled(name)) {
                return;
            }
            size[0]++;
            b.accept(mappingFunction.apply(d));
        });
        Assumptions.assumeTrue(size[0] != 0, () -> "Distribution has no data for " + name);
        return b.build();
    }

    /**
     * Assert the probabilities require a high-precision computation. This verifies
     * the approximation {@code (1 - p) ~ 1}. The tolerance is set at 2 ULP for the
     * smallest p-value.
     *
     * <p>The test tolerance is verified that it can distinguish values a and b when
     * separated by an absolute distance of 2 EPSILON (4.44e-16). This is a quick
     * check to ensure any tests that have overridden the default absolute tolerance
     * of 0 have correctly configured the absolute tolerance for the high-precision
     * probabilities (which are expected to have some p-values {@code < 1e-16}).
     *
     * @param tolerance Test tolerance
     * @param probabilities Probabilities
     */
    void assertHighPrecision(DoubleTolerance tolerance, double... probabilities) {
        final double b = 2 * RELATIVE_EPS;
        Assertions.assertFalse(tolerance.test(0.0, b),
            () -> "Test tolerance cannot separate small values 0.0 and " + b + ": " + tolerance);

        final long one = Double.doubleToRawLongBits(1.0);
        final double expected = 2;
        final int[] ulps = Arrays.stream(probabilities)
                                 .mapToInt(p -> (int) (one - Double.doubleToRawLongBits(1.0 - p)))
                                 .toArray();
        final double min = Arrays.stream(ulps).min().orElse(0);
        Assertions.assertFalse(min < 0, () -> "Invalid probability above 1.0: " + Arrays.toString(probabilities));
        Assertions.assertTrue(min <= expected,
            () -> "Not high-precision p-values: (1 - p) ulps from 1 = " + Arrays.toString(ulps));
    }

    /**
     * Create arguments to test invalid parameters of the distribution. Each Object[]
     * will be expected to raise an exception when passed to the {@link #makeDistribution(Object...)}
     * method.
     *
     * @return the arguments
     */
    Object[][] testInvalidParameters() {
        final Object[][] params = makeInvalidParameters();
        Assumptions.assumeTrue(params != null, "Distribution has no invalid parameters");
        return params;
    }

    /**
     * Create a stream of arguments containing the parameters used to construct a distribution
     * using {@link #makeDistribution(Object...)}.
     *
     * @return the stream
     */
    Stream<Arguments> testParameterAccessors() {
        return data.stream().map(d -> Arguments.of(d.getParameters()));
    }

    //------------------------ Tests -----------------------------

    // Tests are final. It is expected that the test can be modified by overriding
    // the method used to stream the arguments, for example to use a specific tolerance
    // for a test in preference to the tolerance defined in the properties file.

    /**
     * Test invalid parameters will raise an exception when used to construct a distribution.
     */
    @ParameterizedTest
    @MethodSource
    final void testInvalidParameters(@AggregateWith(value = ArrayAggregator.class) Object[] parameters) {
        Assertions.assertThrows(DistributionException.class, () -> makeDistribution(parameters));
    }

    /**
     * Test the parameter accessors using the reflection API.
     */
    @ParameterizedTest
    @MethodSource
    final void testParameterAccessors(@AggregateWith(value = ArrayAggregator.class) Object[] parameters) {
        final String[] names = getParameterNames();
        Assumptions.assumeTrue(names != null, "No parameter accessors");
        Assertions.assertEquals(parameters.length, names.length, "Parameter <-> names length mismatch");

        final T dist = makeDistribution(parameters);
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
