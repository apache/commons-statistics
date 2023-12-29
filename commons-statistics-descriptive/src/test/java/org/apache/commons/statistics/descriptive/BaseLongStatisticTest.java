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
package org.apache.commons.statistics.descriptive;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract base class for statistic tests.
 * This supports testing statistics that implement {@link LongStatistic} and
 * {@link StatisticAccumulator}.
 *
 * <p>This class uses parameterized tests that are repeated for instances of a
 * statistic. The statistic is tested using standard data.
 * Additional data may be provided by implementations.
 *
 * <p>There are 4 main tests for a statistic populated with values using:
 *
 * <ol>
 *  <li>{@link java.util.function.LongConsumer#accept accept}
 *  <li>{@link java.util.function.LongConsumer#accept accept} and
 *      {@link StatisticAccumulator#combine(StatisticResult) combine}
 *  <li>{@link #create(long...)}
 *  <li>{@link #create(long...)} and
 *      {@link StatisticAccumulator#combine(StatisticResult) combine}
 * </ol>
 *
 * <p>Note: Integer statistics may return the expected result as a {@code double}, {@code int},
 * {@code long}, or {@code BigInteger}. The default is {@code double}.
 *
 * <p>A test implementation has to provide these method implementations:
 *
 * <ul>
 *  <li>{@link #getResultType()}: Get the native result type of the statistic.
 *      This is used to verify that test cases are added using the correct type and
 *      test tolerances are configured for {@code double} result types.
 *  <li>{@link #create()}: Create an empty statistic.
 *  <li>{@link #create(long...)}: Create a statistic from a set of values.
 *  <li>{@link #getEmptyValue()}: The expected value of a statistic when not enough values have
 *      been observed. The minimum number of values can be provided in {@link #getEmptySize()}.
 *  <li>{@link #getExpectedValue(long[])}: A method to compute an expected value for the
 *      statistic. This is used to create the expected value on standard test data. It should
 *      be an implementation of the canonical definition of the statistic. It can use extended
 *      precision to provided an accurate expected result.
 *  <li>{@link #getTolerance()}: The test tolerance for all tests when the expected value is
 *      a {@code double}. Tolerances are not used for integer results. This method is not used
 *      directly by the base class for test cases. It provides the default implementation for
 *      the 4 methods to provide the tolerance for each of the 4 main test cases.
 *      <em>Note that the default implementation throws an exception</em>. This forces the
 *      test implementation to either provide a single tolerance for all tests, or the 4 tests
 *      individually. For example, if a statistic provides a different
 *      precision for creation of the statistic from a stream of values or an array then
 *      it can override {@link #getToleranceAccept()}, {@link #getToleranceArray()} and
 *      their combine equivalents.
 *  <li>{@link #streamTestData()}: Provide custom data for the test. It is
 *      <em>strongly recommended</em> to provide reference data from independent implementations.
 *      Edge case data for the statistic can also be provided. See the method javadoc for
 *      details of how to add test data.
 * </ul>
 *
 * <p>Other methods that may be overridden:
 *
 * <ul>
 *  <li>{@link #getStatisticName()}: Provide the name of the statistic for error messages.
 *      The default uses the class name without the "Test" suffix.
 *  <li>{@link #isCombineSymmetric()}: Specify whether to test {@code a.combine(b)} is exactly
 *      equal to {@code b.combine(a)}. The default is {@code true}.
 *  <li>{@link #mapValue(long)}: A method to update the sample data to the valid domain
 *      for the statistic. This can be used to alter the default test data, for example
 *      by mapping any negative values to positive.
 * </ul>
 *
 * <p>The class has a single instance (see {@link Lifecycle#PER_CLASS}) to
 * allow caching the computation of the expected value of a statistic.
 *
 * @param <S> Statistic type
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class BaseLongStatisticTest<S extends LongStatistic & StatisticAccumulator<S>>
        extends BaseStatisticTest {
    /** An empty {@code long[]}. */
    private static final long[] EMPTY = {};

    // Standard test data samples using SciPy version 1.11:
    // import scipy.stats

    /** Uniform samples: scipy.stats.randint.rvs(-15, 25, size=100).
     * https://en.wikipedia.org/wiki/Discrete_uniform_distribution */
    private static final long[] V1 = {20, 8, 1, 2, 24, -13, -1, 19, 0, 23, 14, 2, -9, 0, 7, -15, -2, -9, -1, 12, -12,
        9, 2, 12, 0, 5, 18, 15, -10, 6, 14, 6, 24, 12, -2, 1, -9, 17, 10, 7, 4, -8, 7, 7, -9, 13, -13, 0, -13, 6, 10,
        -6, 23, 2, 6, -10, 12, 6, -6, 4, -5, -9, 5, 16, -14, -4, 22, -4, 20, 12, -13, 6, -13, 12, -3, 13, 1, -6, 13,
        -14, -8, 4, 24, 10, 22, 8, -3, -5, 6, 9, 2, 16, 20, -8, -5, 23, 19, 20, -10, 21};
    /** Poisson samples: scipy.stats.poisson.rvs(1.5, size=100).
     * https://en.wikipedia.org/wiki/Poisson_distribution */
    private static final long[] V2 = {3, 0, 3, 4, 0, 1, 1, 3, 2, 1, 3, 1, 5, 0, 0, 0, 3, 3, 2, 0, 3, 1, 2, 1, 1, 0, 0,
        1, 0, 1, 3, 0, 0, 2, 2, 0, 2, 1, 2, 1, 3, 2, 1, 2, 0, 2, 2, 1, 2, 0, 3, 2, 0, 1, 2, 2, 1, 2, 1, 0, 2, 0, 2, 2,
        3, 2, 1, 1, 0, 1, 2, 1, 0, 0, 6, 2, 0, 1, 1, 1, 1, 2, 2, 2, 2, 0, 2, 1, 2, 4, 2, 4, 2, 0, 0, 1, 1, 2, 2, 3};
    /** Poisson samples: scipy.stats.poisson.rvs(4.5, size=100). */
    private static final long[] V3 = {1, 5, 5, 2, 1, 7, 5, 5, 3, 5, 5, 2, 4, 4, 6, 2, 7, 5, 3, 6, 7, 3, 5, 3, 7, 6, 4,
        3, 3, 3, 3, 3, 5, 3, 2, 8, 2, 4, 3, 1, 2, 5, 4, 3, 5, 4, 4, 8, 6, 2, 4, 3, 5, 5, 6, 3, 1, 6, 8, 6, 3, 6, 10, 4,
        5, 2, 4, 2, 2, 8, 2, 2, 4, 1, 4, 2, 5, 3, 2, 4, 4, 6, 9, 4, 5, 9, 9, 6, 3, 5, 3, 3, 5, 7, 5, 2, 3, 7, 4, 5};
    /** Poisson samples: scipy.stats.poisson.rvs(45, size=100). */
    private static final long[] V4 = {42, 51, 38, 38, 49, 48, 42, 47, 51, 46, 45, 35, 39, 42, 49, 55, 53, 46, 49, 56,
        42, 46, 42, 53, 43, 55, 49, 52, 51, 45, 40, 49, 39, 40, 46, 43, 46, 48, 36, 44, 40, 49, 49, 43, 45, 44, 41, 55,
        52, 45, 57, 41, 43, 44, 38, 52, 44, 45, 43, 42, 38, 37, 47, 42, 47, 45, 70, 45, 50, 47, 46, 50, 47, 35, 43, 52,
        51, 41, 45, 42, 45, 53, 46, 48, 51, 43, 63, 48, 49, 41, 58, 51, 59, 43, 39, 32, 35, 46, 50, 50};

    /** Sizes to cut the sample data into chunks. The {@link #cut(long[], int...)} method is robust
     * to the sizes summing to more than the sample length. If they sum below then the expected
     * value may be incorrect as all data is not used. These all sum to 100 which imposes a maximum
     * length on sample data. */
    private static final int[][] SIZES = {
        {50, 50},
        {40, 60},
        {30, 70},
        {33, 33, 34},
        {20, 25, 25, 30},
        {20, 20, 20, 20, 20},
        {10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
    };

    /** Sizes to cut the extreme value sample data into chunks.
     * These all sum to 10 which imposes a maximum length on sample data. */
    private static final int[][] SIZES_EXTREME = {
        {5, 5},
        {3, 3, 4},
        {2, 2, 2, 2, 2},
    };

    /** The statistic name. */
    private String statisticName;

    /** The test data and expected values. */
    private final List<TestData> data = new ArrayList<>();
    /** The extreme value test data and expected values. */
    private final List<TestData> dataExtremeValue = new ArrayList<>();
    /** Cache of the custom test data. */
    private final List<StatisticTestData> dataCustom = new ArrayList<>();

    /**
     * Container for test data.
     *
     * <p>The statistics is expected to be either a {@code double}, {@code int}, {@code long}
     * of {@code BigInteger} value.
     */
    private static class TestData {
        /** The sample values. */
        private final long[] values;
        /** The expected statistic value. */
        private final StatisticResult expected;

        /**
         * @param values Sample values.
         * @param expected Expected statistic value.
         */
        TestData(long[] values, StatisticResult expected) {
            this.values = values;
            this.expected = expected;
        }

        /**
         * Gets the values.
         *
         * @return the values
         */
        long[] getValues() {
            return values.clone();
        }

        /**
         * Gets the expected statistic value.
         *
         * @return the expected
         */
        StatisticResult getExpected() {
            return expected;
        }
    }

    /**
     * Container for test data with tolerances for the tests. Tolerances are used only
     * for expected values as a {@code double}.
     */
    static class StatisticTestData extends TestData {
        /** The tolerance for the accept test. */
        private final DoubleTolerance tolAccept;
        /** The tolerance for the array test. */
        private final DoubleTolerance tolArray;
        /** The tolerance for the accept+combine test. */
        private final DoubleTolerance tolAcceptCombine;
        /** The tolerance for the array+combine test. */
        private final DoubleTolerance tolArrayCombine;

        /**
         * @param values Sample values.
         * @param expected Expected statistic value.
         */
        StatisticTestData(long[] values, StatisticResult expected) {
            super(values, expected);
            this.tolAccept = null;
            this.tolArray = null;
            this.tolAcceptCombine = null;
            this.tolArrayCombine = null;
        }

        /**
         * @param values Sample values.
         * @param expected Expected statistic value.
         * @param tol Tolerance for the tests.
         */
        StatisticTestData(long[] values, StatisticResult expected, DoubleTolerance tol) {
            this(values, expected, tol, tol, tol, tol);
        }

        /**
         * @param values Sample values.
         * @param expected Expected statistic value.
         * @param tolAccept Tolerance for the test of the statistic using the accept method.
         * @param tolArray Tolerance for the test of the statistic using the array create method.
         * @param tolAcceptCombine Tolerance for the test of the statistic using the accept and combine methods.
         * @param tolArrayCombine Tolerance for the test of the statistic using the array create and combine methods.
         */
        StatisticTestData(long[] values, StatisticResult expected,
                DoubleTolerance tolAccept, DoubleTolerance tolArray,
                DoubleTolerance tolAcceptCombine, DoubleTolerance tolArrayCombine) {
            super(values, expected);
            this.tolAccept = tolAccept;
            this.tolArray = tolArray;
            this.tolAcceptCombine = tolAcceptCombine;
            this.tolArrayCombine = tolArrayCombine;
        }

        /**
         * Gets the tolerance for the
         * {@link BaseLongStatisticTest#testAccept(long[], StatisticResult, DoubleTolerance)}
         * method.
         *
         * @return the tolerance
         */
        DoubleTolerance getTolAccept() {
            return tolAccept;
        }

        /**
         * Gets the tolerance for the
         * {@link BaseLongStatisticTest#testArray(long[], StatisticResult, DoubleTolerance)
         * )} method.
         *
         * @return the tolerance
         */
        DoubleTolerance getTolArray() {
            return tolArray;
        }

        /**
         * Gets the tolerance for the
         * {@link BaseLongStatisticTest#testAcceptAndCombine(long[][], StatisticResult, DoubleTolerance)}
         * method.
         *
         * @return the tolerance
         */
        DoubleTolerance getTolAcceptCombine() {
            return tolAcceptCombine;
        }

        /**
         * Gets the tolerance for the
         * {@link BaseLongStatisticTest#testArrayAndCombine(long[][], StatisticResult, DoubleTolerance)}
         * method.
         *
         * @return the tolerance
         */
        DoubleTolerance getTolArrayCombine() {
            return tolArrayCombine;
        }
    }

    /**
     * Setup the expected result for the standard test data.
     *
     * <p>The statistic name is dynamically created in {@link #getStatisticName()} and can
     * be overridden by implementing classes.
     */
    @BeforeAll
    void setup() {
        statisticName = getStatisticName();
        // Compute the expected value for the standard test data
        Stream.of(V1, V2, V3, V4).forEach(values -> {
            final long[] sample = new long[values.length];
            for (int i = 0; i < sample.length; i++) {
                sample[i] = mapValue(values[i]);
            }
            data.add(new TestData(sample, getExpectedValue(sample)));
        });
        if (!skipExtremeData()) {
            // Data with extreme values.
            // Note: The order does not matter as this is shuffled
            // in the random order tests.
            final int a = Integer.MAX_VALUE;
            final int b = Integer.MIN_VALUE;
            final long[][] extreme = {
                {a, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {b, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {a, a, 1, 1, 1, 1, 1, 1, 1, 1},
                {b, b, 1, 1, 1, 1, 1, 1, 1, 1},
                {a, b, 1, 1, 1, 1, 1, 1, 1, 1},
            };
            Stream.of(extreme).forEach(values -> {
                final long[] sample = values.clone();
                for (int i = 0; i < sample.length; i++) {
                    if (values[i] == 1) {
                        break;
                    }
                    sample[i] = mapValue(sample[i]);
                }
                dataExtremeValue.add(new TestData(sample, getExpectedValue(sample)));
            });
        }
        // Cache the custom test data so any computed expected values are calculated only once
        streamTestData().forEach(dataCustom::add);
    }

    /**
     * Creates the statistic.
     *
     * @return the statistic
     */
    protected abstract S create();

    /**
     * Creates the statistic from the {@code values}.
     *
     * @param values Values.
     * @return the statistic
     */
    protected abstract S create(long... values);

    /**
     * Map the {@code value} to the valid domain of the statistic. This method is called
     * with the example data before {@link #getExpectedValue(long[])}. It can be used by
     * implementing classes to adjust the data to a valid domain, for example if computing
     * the sum-of-logs all values can be updated to be strictly positive.
     *
     * <p>The default implementation does nothing.
     *
     * @param value Value
     * @return the mapped value
     */
    protected long mapValue(long value) {
        return value;
    }

    /**
     * Gets the expected value of the statistic from the {@code values}.
     *
     * <p>This method should be implemented using the canoncial definition of the statistic,
     * ideally in high-precision (e.g. using BigDecimal for statistics involving arithmetic).
     *
     * @param values Values.
     * @return the expected value
     */
    protected abstract StatisticResult getExpectedValue(long[] values);

    /**
     * Creates the equivalent {@link DoubleStatistic} from the {@code values}.
     * This is used to cross-validate the {@link LongStatistic} result.
     *
     * <p>The test will be skipped if this method returns {@code null}.
     *
     * @param values Values.
     * @return the statistic
     */
    protected abstract DoubleStatistic createAsDoubleStatistic(long... values);

    /**
     * Return true to skip the extreme data. This data has samples containing the {@code long}
     * max and/or min value mixed with a less extreme value. This may be difficult to compute
     * for some statistics, e.g. skewness.
     *
     * <p>The default value is false.
     *
     * @return true to skip extreme data
     */
    protected boolean skipExtremeData() {
        return false;
    }

    //------------------------ Helper Methods to create test data ---------------------------

    /**
     * Creates the test data.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code int}.
     *
     * @param expected Expected statistic value.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addReference(int expected, long... values) {
        Assertions.assertEquals(ResultType.INT, getResultType());
        return new StatisticTestData(values, createStatisticResult(expected));
    }

    /**
     * Creates the test data.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code long}.
     *
     * @param expected Expected statistic value.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addReference(long expected, long... values) {
        Assertions.assertEquals(ResultType.LONG, getResultType());
        return new StatisticTestData(values, createStatisticResult(expected));
    }

    /**
     * Creates the test data.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code BigInteger}.
     *
     * @param expected Expected statistic value.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addReference(BigInteger expected, long... values) {
        Assertions.assertEquals(ResultType.BIG_INTEGER, getResultType());
        return new StatisticTestData(values, createStatisticResult(expected));
    }

    /**
     * Creates the test data. The test tolerance uses the 4 tolerances provided
     * by the implementation for the 4 test cases.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code double}.
     *
     * @param expected Expected statistic value.
     * @param values Values.
     * @return the statistic test data
     * @see #getToleranceAccept()
     * @see #getToleranceArray()
     * @see #getToleranceAcceptAndCombine()
     * @see #getToleranceArrayAndCombine()
     */
    StatisticTestData addReference(double expected, long... values) {
        Assertions.assertEquals(ResultType.DOUBLE, getResultType());
        return new StatisticTestData(values, createStatisticResult(expected),
            getToleranceAccept(), getToleranceArray(),
            getToleranceAcceptAndCombine(), getToleranceArrayAndCombine());
    }

    /**
     * Creates the test data. The same tolerance is used for all tests.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code double}.
     *
     * @param expected Expected statistic value.
     * @param tol Test tolerance.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addReference(double expected, DoubleTolerance tol, long... values) {
        Assertions.assertEquals(ResultType.DOUBLE, getResultType());
        return new StatisticTestData(values, createStatisticResult(expected), tol);
    }

    /**
     * Creates the test data.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code double}.
     *
     * @param expected Expected statistic value.
     * @param tolAccept Tolerance for the test of the statistic using the accept method.
     * @param tolArray Tolerance for the test of the statistic using the array create method.
     * @param tolAcceptCombine Tolerance for the test of the statistic using the accept and combine methods.
     * @param tolArrayCombine Tolerance for the test of the statistic using the array create and combine methods.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addReference(double expected,
            DoubleTolerance tolAccept, DoubleTolerance tolArray,
            DoubleTolerance tolAcceptCombine, DoubleTolerance tolArrayCombine,
            long... values) {
        Assertions.assertEquals(ResultType.DOUBLE, getResultType());
        return new StatisticTestData(values, createStatisticResult(expected),
            tolAccept, tolArray, tolAcceptCombine, tolArrayCombine);
    }

    /**
     * Creates the edge-case/example test data. The test tolerance uses the 4
     * tolerances provided by the implementation for the 4 test cases.
     * The expected value is computed by the test implementation.
     *
     * <p>This method can be used to add data to target specific edge cases.
     *
     * @param values Values.
     * @return the statistic test data
     * @see #getExpectedValue(long[])
     * @see #getToleranceAccept()
     * @see #getToleranceArray()
     * @see #getToleranceAcceptAndCombine()
     * @see #getToleranceArrayAndCombine()
     */
    final StatisticTestData addCase(long... values) {
        if (getResultType() == ResultType.DOUBLE) {
            return new StatisticTestData(values, getExpectedValue(values),
                getToleranceAccept(), getToleranceArray(),
                getToleranceAcceptAndCombine(), getToleranceArrayAndCombine());
        }
        return new StatisticTestData(values, getExpectedValue(values));
    }

    /**
     * Creates the edge-case/example test data. The same tolerance is used for all tests.
     * The expected value will be computed using {@link #getExpectedValue(long[])}.
     *
     * <p>This method can be used to add data to target specific edge cases.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code double}.
     *
     * @param tol Test tolerance.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addCase(DoubleTolerance tol, long... values) {
        Assertions.assertEquals(ResultType.DOUBLE, getResultType());
        return new StatisticTestData(values, getExpectedValue(values), tol);
    }

    /**
     * Creates the edge-case/example test data.
     * The expected value will be computed using {@link #getExpectedValue(long[])}.
     *
     * <p>This method can be used to add data to target specific edge cases.
     *
     * <p>Note: Throws an exception if {@link #getResultType()} is not {@code double}.
     *
     * @param tolAccept Tolerance for the test of the statistic using the accept method.
     * @param tolArray Tolerance for the test of the statistic using the array create method.
     * @param tolAcceptCombine Tolerance for the test of the statistic using the accept and combine methods.
     * @param tolArrayCombine Tolerance for the test of the statistic using the array create and combine methods.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addCase(
            DoubleTolerance tolAccept, DoubleTolerance tolArray,
            DoubleTolerance tolAcceptCombine, DoubleTolerance tolArrayCombine,
            long... values) {
        Assertions.assertEquals(ResultType.DOUBLE, getResultType());
        return new StatisticTestData(values, getExpectedValue(values),
            tolAccept, tolArray, tolAcceptCombine, tolArrayCombine);
    }

    /**
     * Provide a stream of custom test data.
     *
     * <p><em>Use this method to customize the test of the statistic</em>.
     *
     * <p>It is strongly recommended to add test data using independently computed
     * reference values. The source of the reference value should be commented
     * in the test with name and version (e.g. [Matlab, R, SciPy] + version).
     *
     * <p>Specific data can also be added to test edge cases for the statistic. These may
     * have their expected value computed internally by the test, or provided from an independent
     * reference implementation.
     *
     * <p>Test data can be added to a Stream using the {@code add} methods which provide
     * shorthand methods to add instances of the {@link StatisticTestData}. The test data
     * will be used for all 4 main tests of the statistic:
     *
     * <ol>
     *  <li>{@link java.util.function.LongConsumer#accept accept}
     *  <li>{@link java.util.function.LongConsumer#accept accept} and
     *      {@link StatisticAccumulator#combine(StatisticResult) combine}
     *  <li>{@link #create(long...)}
     *  <li>{@link #create(long...)} and
     *      {@link StatisticAccumulator#combine(StatisticResult) combine}
     * </ol>
     *
     * <p>To test the {@code combine} method the data is split into approximately even groups
     * using {@link #getCombineGroupSizes()}. The data is also subject to the same tests
     * using a random order. The randomization is different for each invocation of the test
     * and so the tolerance should be configured appropriately.
     *
     * <p>An example implementation is:
     * <pre>
     * {@code
     * Stream.of(
     *   // Python numpy.mean (version 1.25.2)
     *   add(3.5, createRelTolerance(1e-15), 2, 3, 4, 5),
     *   add(11.5, createRelTolerance(1e-15), 2, 3, 4, 5, 13, 42),
     *   // Edge case for mean
     *   add(createRelTolerance(1e-15), Int.MAX_VALUE, -Int.MAX_VALUE)
     * )}
     * </pre>
     *
     * <p>If there are no additional test cases then return {@link Stream#empty()}.
     *
     * @return the stream
     */
    protected abstract Stream<StatisticTestData> streamTestData();

    //------------------------ Methods to stream the test data -----------------------------

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link java.util.function.LongConsumer#accept accept} method. The expected value
     * and tolerance are supplied by the implementing class.
     *
     * @return the stream
     */
    final Stream<Arguments> testAccept() {
        return streamSingleArrayData(getToleranceAccept(), StatisticTestData::getTolAccept);
    }

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link #create(long...)} method. The expected value and tolerance are supplied
     * by the implementing class.
     *
     * @return the stream
     */
    final Stream<Arguments> testArray() {
        return streamSingleArrayData(getToleranceArray(), StatisticTestData::getTolArray);
    }

    /**
     * Test the computation of the statistic against the equivalent {@link DoubleStatistic}.
     * The result is tested as a {@code double} using the configured {@link #getToleranceAsDouble()}.
     * Integer values are not asserted as they may not be an exact match.
     */
    @ParameterizedTest
    @MethodSource(value = {"testAccept"})
    final void testVsDoubleStatistic(long[] values) {
        final DoubleStatistic stat = createAsDoubleStatistic(values);
        Assumptions.assumeTrue(stat != null);
        final double expected = stat.getAsDouble();
        final DoubleTolerance tol = getToleranceAsDouble();
        TestUtils.assertEquals(expected, Statistics.add(create(), values).getAsDouble(), tol,
            () -> statisticName + " accept: " + format(values));
        TestUtils.assertEquals(expected, create(values).getAsDouble(), tol,
            () -> statisticName + " array: " + format(values));
    }

    /**
     * Stream the arguments to test the computation of the statistic using a single
     * {@code long[]} array.
     *
     * @param tol Test tolerance.
     * @param tolCustom Tolerance for any custom test data.
     * @return the stream
     */
    private Stream<Arguments> streamSingleArrayData(DoubleTolerance tol,
            Function<StatisticTestData, DoubleTolerance> tolCustom) {
        final Builder<Arguments> b = Stream.builder();
        data.forEach(d -> b.accept(Arguments.of(d.getValues(), d.getExpected(), tol)));
        dataExtremeValue.forEach(d -> b.accept(Arguments.of(d.getValues(), d.getExpected(), tol)));
        dataCustom.forEach(d -> b.accept(Arguments.of(d.getValues(), d.getExpected(), tolCustom.apply(d))));
        return b.build();
    }

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link java.util.function.LongConsumer#accept(long) accept} method for each
     * array, then the {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method. The expected value and tolerance are supplied by the implementing class.
     *
     * @return the stream
     */
    final Stream<Arguments> testAcceptAndCombine() {
        return streamMultiArrayData(getToleranceAcceptAndCombine(), StatisticTestData::getTolAcceptCombine);
    }

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link #create(long...)} method for each array, then the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method. The
     * expected value and tolerance are supplied by the implementing class.
     *
     * @return the stream
     */
    final Stream<Arguments> testArrayAndCombine() {
        return streamMultiArrayData(getToleranceArrayAndCombine(), StatisticTestData::getTolArrayCombine);
    }

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link java.util.function.LongConsumer#accept(long) accept} method for each
     * element of a parallel stream, then the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method.
     * The expected value and tolerance are supplied by the implementing class.
     *
     * <p>Note that this method uses the tolerance from {@link #getToleranceAcceptAndCombine()}.
     *
     * @return the stream
     */
    final Stream<Arguments> testAcceptParallelStream() {
        return streamSingleArrayData(getToleranceAcceptAndCombine(), StatisticTestData::getTolAcceptCombine);
    }

    /**
     * Stream the arguments to test the computation of the statistic using the multiple
     * {@code long[]} arrays and the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method.
     *
     * @param tol Test tolerance.
     * @param tolCustom Tolerance for any custom test data.
     * @return the stream
     */
    private Stream<Arguments> streamMultiArrayData(DoubleTolerance tol,
            Function<StatisticTestData, DoubleTolerance> tolCustom) {
        final Builder<Arguments> b = Stream.builder();
        data.forEach(d -> {
            // Cut the standard data into pieces
            Arrays.stream(SIZES)
                  .map(x -> cut(d.getValues(), x))
                  .forEach(e -> b.accept(Arguments.of(e, d.getExpected(), tol)));
        });
        dataExtremeValue.forEach(d -> {
            // Cut the standard data into pieces
            Arrays.stream(SIZES_EXTREME)
                  .map(x -> cut(d.getValues(), x))
                  .forEach(e -> b.accept(Arguments.of(e, d.getExpected(), tol)));
        });
        dataCustom.forEach(d -> {
            // Only split lengths above the number of groups
            final long[] values = d.getValues();
            final int n = values.length;
            for (final int groups : getCombineGroupSizes()) {
                if (groups <= n) {
                    b.accept(Arguments.of(split(values, groups), d.getExpected(), tolCustom.apply(d)));
                }
            }
        });
        return b.build();
    }

    /**
     * Cut the data into the specified sizes. If the sizes are too large for the input data
     * then the final array uses the remaining values and further sizes are ignored. If the
     * the sizes are too small then an exception is raised as all values must be used to
     * match the computation of the expected value.
     *
     * @param data Data.
     * @param sizes Sizes.
     * @return the set of arrays
     */
    private static long[][] cut(long[] data, int... sizes) {
        ArrayList<long[]> set = new ArrayList<>();
        int from = 0;
        for (final int size : sizes) {
            final int to = Math.min(data.length, from + size);
            set.add(Arrays.copyOfRange(data, from, to));
            from = to;
            if (to == data.length) {
                // Sizes too cover all the input data
                break;
            }
        }
        if (from < data.length) {
            // Error: Not all the input data was used.
            Assertions.fail(
                () -> "Sizes " + Arrays.toString(sizes) + " do not cover length " + data.length);
        }
        return set.toArray(new long[0][]);
    }

    /**
     * Split the data into approximately even groups. The number of groups must be smaller
     * than the data length.
     *
     * @param data Data.
     * @param groups Number of groups.
     * @return the set of arrays
     */
    private static long[][] split(long[] data, int groups) {
        // Note: round the size of the group.
        // The final group accounts for length difference:
        // 4/3 => 1, 1, 2
        // 5/3 => 2, 2, 1
        // 7/3 => 2, 2, 3
        final int size = (int) Math.round((double) data.length / groups);
        final long[][] values = new long[groups][];
        for (int i = 0; i < groups; i++) {
            final int from = i * size;
            // Final group may be smaller or larger
            final int to = i + 1 == groups ? data.length : from + size;
            values[i] = Arrays.copyOfRange(data, from, to);
        }
        return values;
    }

    //------------------------ Test methods -----------------------------

    /**
     * Test the uninitialized state of the statistic.
     */
    @Test
    final void testEmpty() {
        assertEmpty(create(), getToleranceAccept());
    }

    /**
     * Test the uninitialized state of the statistic created from an empty array.
     */
    @Test
    final void testEmptyArray() {
        assertEmpty(create(EMPTY), getToleranceArray());
    }

    /**
     * Assert the uninitialized state of the statistic.
     *
     * @param stat Statistic.
     * @param tol Non-empty tolerance.
     */
    private void assertEmpty(S stat, DoubleTolerance tol) {
        final StatisticResult v = getEmptyValue();
        final int size = getEmptySize();
        final long[] values = new long[size + 1];
        // Fill beyond empty
        for (int i = 0; i <= size; i++) {
            TestHelper.assertEquals(v, stat, tol, () -> statisticName + " should be empty");
            stat.accept(i + 1);
            values[i] = i + 1;
        }
        final StatisticResult expected = getExpectedValue(values);
        TestHelper.assertEquals(expected, stat, tol,
            () -> statisticName + " should not be empty");
    }

    /**
     * Test the computation of the statistic using the
     * {@link java.util.function.LongConsumer#accept(long) accept} method. The
     * statistic is created using both the {@link #create()} and the
     * {@link #create(long...)} methods; the two instances must compute the same result.
     */
    @ParameterizedTest
    @MethodSource
    final void testAccept(long[] values, StatisticResult expected, DoubleTolerance tol) {
        final S actual = assertStatistic(v -> Statistics.add(create(), v), values, expected, tol);
        // Repeat after creation with an empty array. No tolerance for exact equality.
        assertStatistic(v -> Statistics.add(create(EMPTY), v), values, actual, null);
    }

    /**
     * Test the computation of the statistic using the {@link #create(long...)} method.
     */
    @ParameterizedTest
    @MethodSource
    final void testArray(long[] values, StatisticResult expected, DoubleTolerance tol) {
        assertStatistic(this::create, values, expected, tol);
    }

    /**
     * Test the computation of the statistic using the
     * {@link java.util.function.LongConsumer#accept(long) accept} method for each
     * array, then the {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method.
     */
    @ParameterizedTest
    @MethodSource
    final void testAcceptAndCombine(long[][] values, StatisticResult expected, DoubleTolerance tol) {
        assertCombine(v -> Statistics.add(create(), v), values, expected, tol);
    }

    /**
     * Test the computation of the statistic using the
     * {@link java.util.function.LongConsumer#accept(long) accept} method for each
     * array, then the {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method.
     */
    @ParameterizedTest
    @MethodSource
    final void testArrayAndCombine(long[][] values, StatisticResult expected, DoubleTolerance tol) {
        assertCombine(this::create, values, expected, tol);
    }

    /**
     * Test the {@link StatisticAccumulator#combine(StatisticResult) combine} method
     * with an empty instance combined with a non-empty instance.
     */
    @ParameterizedTest
    @MethodSource(value = "testAccept")
    final void testEmptyCombine(long[] values) {
        final S empty = create();
        final S nonEmpty = Statistics.add(create(), values);
        final StatisticResult expected = Statistics.add(create(), values);
        final S result = assertCombine(empty, nonEmpty);
        TestHelper.assertEquals(expected, result, null,
            () -> statisticName + " empty.combine(nonEmpty)");
    }

    /**
     * Test the {@link StatisticAccumulator#combine(StatisticResult) combine} method
     * with a non-empty instance combined with an empty instance.
     */
    @ParameterizedTest
    @MethodSource(value = "testAccept")
    final void testCombineEmpty(long[] values) {
        final S empty = create();
        final S nonEmpty = Statistics.add(create(), values);
        final StatisticResult expected = Statistics.add(create(), values);
        final S result = assertCombine(nonEmpty, empty);
        TestHelper.assertEquals(expected, result, null,
            () -> statisticName + " nonEmpty.combine(empty)");
    }

    /**
     * Test the computation of the statistic using a parallel stream of {@code double}
     * values. The accumulator is the
     * {@link java.util.function.LongConsumer#accept(long) accept} method; the
     * combiner is the {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method.
     *
     * <p>Note: This method is similar to the
     * {@link #testAcceptAndCombine(long[][], StatisticResult, DoubleTolerance)} method and uses
     * the same data, concatenated to a single array, and tolerance. The difference is the
     * former method uses explicit subsets of the data and its own fixed algorithm to
     * combine instances. This method leaves the entire process of dividing the data and
     * combining to the JDK parallel stream; results may vary across platforms as it is
     * dependent on the JDK and the available processors.
     */
    @ParameterizedTest
    @MethodSource
    final void testAcceptParallelStream(long[] values, StatisticResult expected, DoubleTolerance tol) {
        final S actual = Arrays.stream(values)
            .parallel()
            .collect(this::create, LongStatistic::accept, StatisticAccumulator::combine);
        TestHelper.assertEquals(expected, actual, tol, () -> statisticName + ": " + format(values));
    }

    /**
     * Test the computation of the statistic using a parallel stream of {@code long[]}
     * arrays. The arrays are mapped to a statistic using the {@link #create(long...)}
     * method, and the stream reduced using the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method.
     *
     * <p>Note: This method is similar to the
     * {@link #testArrayAndCombine(long[][], StatisticResult, DoubleTolerance)} method and uses
     * the same data and tolerance. The difference is the former method uses its own fixed
     * algorithm to combine instances. This method leaves the process of combining to the
     * JDK parallel stream; results may vary across platforms as it is dependent on the
     * JDK and the available processors.
     */
    @ParameterizedTest
    @MethodSource(value = "testArrayAndCombine")
    final void testArrayParallelStream(long[][] values, StatisticResult expected, DoubleTolerance tol) {
        final S actual = Arrays.stream(values)
            .parallel()
            .map(this::create)
            .reduce(StatisticAccumulator::combine)
            // Return an empty instance if there is no data
            .orElseGet(this::create);
        TestHelper.assertEquals(expected, actual, tol, () -> statisticName + ": " + format(values));
    }

    /**
     * Invokes the {@link #testAccept(long[], StatisticResult, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testAccept")
    final void testAcceptRandom(long[] values, StatisticResult expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                testAccept(TestHelper.shuffle(rng, values), expected, tol);
            }
        } catch (AssertionError e) {
            rethrowWithSeedAndRepeat(e, seed, repeat);
        }
    }

    /**
     * Invokes the {@link #testArray(long[], StatisticResult, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testArray")
    final void testArrayRandom(long[] values, StatisticResult expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                testArray(TestHelper.shuffle(rng, values), expected, tol);
            }
        } catch (AssertionError e) {
            rethrowWithSeedAndRepeat(e, seed, repeat);
        }
    }

    /**
     * Invokes the {@link #testAcceptAndCombine(long[][], StatisticResult, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testAcceptAndCombine")
    final void testAcceptAndCombineRandom(long[][] values, StatisticResult expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        final long[] allValues = TestHelper.concatenate(values);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                TestHelper.shuffle(rng, allValues);
                TestHelper.unconcatenate(allValues, values);
                testAcceptAndCombine(TestHelper.shuffle(rng, values), expected, tol);
            }
        } catch (AssertionError e) {
            rethrowWithSeedAndRepeat(e, seed, repeat);
        }
    }

    /**
     * Invokes the {@link #testArrayAndCombine(long[][], StatisticResult, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testArrayAndCombine")
    final void testArrayAndCombineRandom(long[][] values, StatisticResult expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        final long[] allValues = TestHelper.concatenate(values);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                TestHelper.shuffle(rng, allValues);
                TestHelper.unconcatenate(allValues, values);
                testArrayAndCombine(TestHelper.shuffle(rng, values), expected, tol);
            }
        } catch (AssertionError e) {
            rethrowWithSeedAndRepeat(e, seed, repeat);
        }
    }

    /**
     * Test the computation of the statistic using an empty instance and the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method with
     * instances containing single values. Adding statistic instances containing 1 value
     * should be the same precision as adding a {@code double} value, although the two
     * results are not required to be exactly equal.
     */
    @ParameterizedTest
    @MethodSource(value = "testAccept")
    final void testCombineAsAccept(long[] values, StatisticResult expected, DoubleTolerance tol) {
        final S empty = create();
        assertStatistic(v ->
            Arrays.stream(values)
                  .mapToObj(x -> create(x))
                  .reduce(empty, this::assertCombine),
            values, expected, tol);
    }

    /**
     * Assert the computation of the statistic.
     *
     * @param constructor Function to create the statistic.
     * @param values Values.
     * @param expected Expected value of the statistic.
     * @param tol Test tolerance.
     * @return the computed statistic
     */
    final S assertStatistic(Function<long[], S> constructor,
                            long[] values, StatisticResult expected, DoubleTolerance tol) {
        final S stat = constructor.apply(values);
        TestHelper.assertEquals(expected, stat, tol, () -> statisticName + ": " + format(values));
        return stat;
    }

    /**
     * Assert the computation of the statistic using the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method.
     *
     * @param constructor Function to create the statistic.
     * @param values Values.
     * @param expected Expected value of the statistic.
     * @param tol Test tolerance.
     * @return the computed statistic
     */
    final S assertCombine(Function<long[], S> constructor,
                          long[][] values, StatisticResult expected, DoubleTolerance tol) {
        int n = values.length;
        Assertions.assertNotEquals(0, n, "No data");
        final List<S> stats = Arrays.stream(values)
            .map(constructor::apply)
            .collect(Collectors.toList());
        List<S> stats2;
        // If the combine is symmetric we perform the same algorithm
        // but swap a+b to b+a at each step.
        if (isCombineSymmetric()) {
            stats2 = Arrays.stream(values)
                .map(constructor::apply)
                .collect(Collectors.toList());
        } else {
            stats2 = Collections.emptyList();
        }
        while (n != 1) {
            // Process pairs (ending at an even size)
            final int end = n & ~1;
            for (int i = 0; i < end; i += 2) {
                final int target = i >> 1;
                final int lhs = i;
                final int rhs = i + 1;
                combine(stats, stats2, target, lhs, rhs);
            }
            // final value
            if (end != n) {
                // Merging with the previous pair will make the the end statistic
                // progressively larger than the others. However the final merge should
                // be limited to below a 1:2 ratio.
                // Note: Set up the indices and copy the above code.
                final int target = (n >> 1) - 1;
                final int lhs = target;
                final int rhs = end;
                combine(stats, stats2, target, lhs, rhs);
            }
            n >>= 1;
        }
        final S stat = stats.get(0);
        TestHelper.assertEquals(expected, stat, tol, () -> statisticName + ": " + format(values));
        return stat;
    }

    /**
     * Combine the statistics at index {@code lhs} with index {@code rhs} and store in
     * index {@code target}: {@code lhs.combine(rhs) -> target}.
     *
     * <p>If the statistic is symmetric then the duplicate copy of the statistics is
     * combined in reverse: {@code rhs.combine(lhs) -> target}.
     *
     * @param stats Statistics.
     * @param stats2 Duplicate copy of the statistics.
     * @param target Destination index.
     * @param lhs Left-hand side index.
     * @param rhs Right-hand side index.
     */
    private void combine(final List<S> stats, List<S> stats2, int target, int lhs, int rhs) {
        final S s1 = assertCombine(stats.get(lhs), stats.get(rhs));
        stats.set(target, s1);
        if (isCombineSymmetric()) {
            final S s2 = assertCombine(stats2.get(rhs), stats2.get(lhs));
            TestHelper.assertEquals(s1, s2, null,
                () -> statisticName + ": Non-symmetric combine");
            stats2.set(target, s2);
        }
    }

    /**
     * Format the values as a string. The maximum length is limited.
     *
     * @param values Values.
     * @return the string
     */
    static String format(long[] values) {
        if (values.length > MAX_FORMAT_VALUES) {
            return Arrays.stream(values)
                         .limit(MAX_FORMAT_VALUES)
                         .mapToObj(Long::toString)
                         .collect(Collectors.joining(", ", "[", ", ...]"));
        }
        return Arrays.toString(values);
    }

    /**
     * Format the values as a string. The maximum length of each array is limited.
     *
     * @param values Values.
     * @return the string
     */
    private static String format(long[][] values) {
        return Arrays.stream(values)
            .map(BaseLongStatisticTest::format)
            .collect(Collectors.joining(", "));
    }
}
