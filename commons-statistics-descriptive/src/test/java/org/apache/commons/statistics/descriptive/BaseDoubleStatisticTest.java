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
package org.apache.commons.statistics.descriptive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ArraySampler;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.apache.commons.statistics.distribution.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract base class for statistic tests.
 * This supports testing statistics that implement {@link DoubleStatistic} and
 * {@link StatisticAccumulator}.
 *
 * <p>This class uses parameterized tests that are repeated for instances of a
 * statistic. The statistic is tested using standard finite and non-finite data.
 * Additional data may be provided by implementations.
 *
 * <p>There are 4 main tests for a statistic populated with values using:
 *
 * <ol>
 *  <li>{@link java.util.function.DoubleConsumer#accept accept}
 *  <li>{@link java.util.function.DoubleConsumer#accept accept} and
 *      {@link StatisticAccumulator#combine(StatisticResult) combine}
 *  <li>{@link #create(double...)}
 *  <li>{@link #create(double...)} and
 *      {@link StatisticAccumulator#combine(StatisticResult) combine}
 * </ol>
 *
 * <p>A test implementation has to provide these method implementations:
 *
 * <ul>
 *  <li>{@link #create()}: Create an empty statistic.
 *  <li>{@link #create(double...)}: Create a statistic from a set of values.
 *  <li>{@link #getEmptyValue()}: The expected value of a statistic when not enough values have
 *      been observed. The minimum number of values can be provided in {@link #getEmptySize()}.
 *  <li>{@link #getExpectedValue(double[])}: A method to compute an expected value for the
 *      statistic. This is used to create the expected value on standard test data. It should
 *      be an implementation of the canonical definition of the statistic. It can use extended
 *      precision to provided an accurate expected result.
 *  <li>{@link #getTolerance()}: The test tolerance for all tests. This method is not used
 *      directly by the base class for test cases. It provides the default implementation for
 *      the 4 methods to provide the tolerance for each of the 4 main test cases.
 *      <em>Note that the default implementation throws an exception</em>. This forces the
 *      test implementation to either provide a single tolerance for all tests, or the 4 tests
 *      individually. For example, if a statistic provides a different
 *      precision for creation of the statistic from a stream of values or an array then
 *      it can override {@link #getToleranceAccept()}, {@link #getToleranceArray()} and
 *      their combine equivalents.
 *  <li>{@link #getExpectedNonFiniteValue(double[])}: A method to compute an expected value
 *      for the statistic when a non-finite value is encountered. This is only used with at
 *      least one non-finite value. Typically this should return {@code NaN} or an infinity.
 *      Data tested with non-finite values will use {@link #getToleranceNonFinite()} which
 *      defaults to an exact binary equality. This may be overridden for example to return
 *      a tolerance that matches any non-finite value.
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
 *  <li>{@link #mapValue(double)}: A method to update the sample data to the valid domain
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
abstract class BaseDoubleStatisticTest<S extends DoubleStatistic & StatisticAccumulator<S>> {
    /** An empty {@code double[]}. */
    private static final double[] EMPTY = {};
    /** The number of random permutations to perform. */
    private static final int RANDOM_PERMUTATIONS = 5;
    /** The maximum number of values to output in assertion messages. This is used to provide
     * the data for the failed test which is useful to identify the failed test case. */
    private static final int MAX_FORMAT_VALUES = 100;

    // Standard test data samples using SciPy version 1.11:
    // import scipy.stats

    /** Uniform samples: scipy.stats.randint.rvs(-15, 25, size=100).
     * https://en.wikipedia.org/wiki/Discrete_uniform_distribution */
    private static final double[] V1 = {20, 8, 1, 2, 24, -13, -1, 19, 0, 23, 14, 2, -9, 0, 7, -15, -2, -9, -1, 12, -12,
        9, 2, 12, 0, 5, 18, 15, -10, 6, 14, 6, 24, 12, -2, 1, -9, 17, 10, 7, 4, -8, 7, 7, -9, 13, -13, 0, -13, 6, 10,
        -6, 23, 2, 6, -10, 12, 6, -6, 4, -5, -9, 5, 16, -14, -4, 22, -4, 20, 12, -13, 6, -13, 12, -3, 13, 1, -6, 13,
        -14, -8, 4, 24, 10, 22, 8, -3, -5, 6, 9, 2, 16, 20, -8, -5, 23, 19, 20, -10, 21};
    /** Poisson samples: scipy.stats.poisson.rvs(1.5, size=100).
     * https://en.wikipedia.org/wiki/Poisson_distribution */
    private static final double[] V2 = {3, 0, 3, 4, 0, 1, 1, 3, 2, 1, 3, 1, 5, 0, 0, 0, 3, 3, 2, 0, 3, 1, 2, 1, 1, 0, 0,
        1, 0, 1, 3, 0, 0, 2, 2, 0, 2, 1, 2, 1, 3, 2, 1, 2, 0, 2, 2, 1, 2, 0, 3, 2, 0, 1, 2, 2, 1, 2, 1, 0, 2, 0, 2, 2,
        3, 2, 1, 1, 0, 1, 2, 1, 0, 0, 6, 2, 0, 1, 1, 1, 1, 2, 2, 2, 2, 0, 2, 1, 2, 4, 2, 4, 2, 0, 0, 1, 1, 2, 2, 3};
    /** Poisson samples: scipy.stats.poisson.rvs(4.5, size=100). */
    private static final double[] V3 = {1, 5, 5, 2, 1, 7, 5, 5, 3, 5, 5, 2, 4, 4, 6, 2, 7, 5, 3, 6, 7, 3, 5, 3, 7, 6, 4,
        3, 3, 3, 3, 3, 5, 3, 2, 8, 2, 4, 3, 1, 2, 5, 4, 3, 5, 4, 4, 8, 6, 2, 4, 3, 5, 5, 6, 3, 1, 6, 8, 6, 3, 6, 10, 4,
        5, 2, 4, 2, 2, 8, 2, 2, 4, 1, 4, 2, 5, 3, 2, 4, 4, 6, 9, 4, 5, 9, 9, 6, 3, 5, 3, 3, 5, 7, 5, 2, 3, 7, 4, 5};
    /** Poisson samples: scipy.stats.poisson.rvs(45, size=100). */
    private static final double[] V4 = {42, 51, 38, 38, 49, 48, 42, 47, 51, 46, 45, 35, 39, 42, 49, 55, 53, 46, 49, 56,
        42, 46, 42, 53, 43, 55, 49, 52, 51, 45, 40, 49, 39, 40, 46, 43, 46, 48, 36, 44, 40, 49, 49, 43, 45, 44, 41, 55,
        52, 45, 57, 41, 43, 44, 38, 52, 44, 45, 43, 42, 38, 37, 47, 42, 47, 45, 70, 45, 50, 47, 46, 50, 47, 35, 43, 52,
        51, 41, 45, 42, 45, 53, 46, 48, 51, 43, 63, 48, 49, 41, 58, 51, 59, 43, 39, 32, 35, 46, 50, 50};
    /** Normal samples: scipy.stats.norm.rvs(loc=3.4, scale=2.25, size=100). */
    private static final double[] V5 = {1.06356579, -1.52552007, 7.09739891, -0.41516549, 0.17131653, 0.77923148,
        2.90491862, 4.12648256, 5.04920689, 4.20053484, 5.83485097, 4.33138009, 4.18795702, 3.269289, 2.2399589,
        4.16551591, -1.67192439, 1.44919254, 3.52270229, -1.49186865, -0.30794835, 5.82394621, 4.84755567, 4.79622486,
        5.12461983, 2.62561931, 5.12457788, 8.24460895, 4.91249002, 3.75550863, 4.35440479, 4.17587334, -0.34934393,
        2.98071452, -1.35620308, 1.93956508, 7.57171999, 5.41976186, 2.8427556, 3.04101193, 2.20374721, 4.65406057,
        5.76961878, 3.14412957, 7.60322297, 1.598286, 2.51552974, 0.67767289, 0.76514432, 3.65663671, 0.53116457,
        2.79439061, 7.58564809, 4.16735822, 2.95210392, 6.37867376, 6.57010411, 0.11837698, 9.16270054, 3.80097588,
        5.48811672, 3.83378268, 2.03669252, 5.34865676, 3.11338528, 4.70088345, 6.00069684, 0.16144587, 4.22654482,
        2.2722623, 5.39142224, 0.811471, 2.74523433, 6.32457234, 0.73033045, 9.54402353, 0.4800466, 2.00806359,
        6.06115109, 2.3072464, 5.40974674, 2.05533169, 0.97160161, 8.06915145, 4.40792026, 4.53139251, 3.32350119,
        1.53645238, 3.49059212, 3.57904997, 0.58634639, 5.87567911, 3.49424866, 5.72228178, 4.41403447, 1.27815121,
        7.13861948, 4.68209093, 6.4598438, 0.66270586};
    /** Gamma samples: scipy.stats.gamma.rvs(9, scale=0.5, size=100). */
    private static final double[] V6 = {3.46479451, 8.80950045, 3.91437318, 4.23327834, 2.6910161, 4.51122052,
        5.81939474, 3.9142699, 7.75537607, 6.06693317, 3.29388792, 3.90689471, 3.26357137, 3.6398822, 5.60048428,
        3.68248997, 5.09297897, 4.6302593, 7.01654777, 4.2244833, 2.75326355, 5.36988549, 2.88392811, 3.50131464,
        4.81183009, 4.92155284, 4.37061644, 3.8064197, 3.31941113, 5.01257676, 3.48037207, 2.62777255, 6.2447332,
        6.18425783, 3.06915179, 6.42851381, 3.8969583, 3.48723372, 3.49516941, 2.90404439, 2.25920041, 3.68515649,
        5.09607663, 3.18984299, 2.49261713, 3.9345895, 6.01480539, 8.8065787, 4.3464082, 5.03522483, 4.05315513,
        6.07365399, 4.34804323, 5.16061656, 3.24706079, 2.89888437, 6.24575902, 3.10893227, 3.74196045, 3.94099137,
        3.33951846, 4.9264514, 5.21935748, 5.06904776, 2.77543623, 3.72451685, 6.35546017, 4.42425655, 5.99568005,
        4.2602446, 3.75834066, 5.17730802, 3.60682583, 4.09703419, 5.59942582, 3.49191032, 3.02164323, 8.85183547,
        5.58631958, 3.24891648, 4.03267796, 4.30984912, 3.78187375, 5.98969913, 1.68855026, 8.43117397, 3.808258,
        4.82043745, 2.91020117, 4.12921107, 3.54350667, 4.60545934, 5.63180941, 5.07331453, 5.04419517, 3.78796082,
        4.25143811, 6.3242129, 4.25630677, 4.59158821};

    /** Sizes to cut the sample data into chunks. The {@link #cut(double[], int...)} method is robust
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

    /** Sizes to cut the non-finite sample data into chunks.
     * These all sum to 10 which imposes a maximum length on sample data. */
    private static final int[][] SIZES_NON_FINITE = {
        {5, 5},
        {3, 3, 4},
        {2, 2, 2, 2, 2},
    };

    /** The test data and expected values. */
    private final List<TestData> data = new ArrayList<>();
    /** The non-finite test data and expected values. */
    private final List<TestData> dataNonFinite = new ArrayList<>();
    /** Cache of the custom test data. */
    private final List<StatisticTestData> dataCustom = new ArrayList<>();

    /** The statistic name. */
    private String statisticName;

    /**
     * Container for test data.
     */
    private static class TestData {
        /** The sample values. */
        private final double[] values;
        /** The expected statistic value. */
        private final Double expected;

        /**
         * @param values Sample values.
         * @param expected Expected statistic value.
         */
        TestData(double[] values, double expected) {
            this.values = values;
            this.expected = Double.valueOf(expected);
        }

        /**
         * Gets the values.
         *
         * @return the values
         */
        double[] getValues() {
            return values.clone();
        }

        /**
         * Gets the expected statistic value.
         *
         * @return the expected
         */
        Double getExpected() {
            return expected;
        }
    }

    /**
     * Container for test data with tolerances for the tests.
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
         * @param tol Tolerance for the tests.
         */
        StatisticTestData(double[] values, double expected, DoubleTolerance tol) {
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
        StatisticTestData(double[] values, double expected,
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
         * {@link BaseDoubleStatisticTest#testAccept(double[], double, DoubleTolerance)}
         * method.
         *
         * @return the tolerance
         */
        DoubleTolerance getTolAccept() {
            return tolAccept;
        }

        /**
         * Gets the tolerance for the
         * {@link BaseDoubleStatisticTest#testArray(double[], double, DoubleTolerance)
         * )} method.
         *
         * @return the tolerance
         */
        DoubleTolerance getTolArray() {
            return tolArray;
        }

        /**
         * Gets the tolerance for the
         * {@link BaseDoubleStatisticTest#testAcceptAndCombine(double[][], double, DoubleTolerance)}
         * method.
         *
         * @return the tolerance
         */
        DoubleTolerance getTolAcceptCombine() {
            return tolAcceptCombine;
        }

        /**
         * Gets the tolerance for the
         * {@link BaseDoubleStatisticTest#testArrayAndCombine(double[][], double, DoubleTolerance)}
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
        Stream.of(V1, V2, V3, V4, V5, V6).forEach(values -> {
            final double[] sample = new double[values.length];
            for (int i = 0; i < sample.length; i++) {
                sample[i] = mapValue(values[i]);
            }
            final double expected = getExpectedValue(sample);
            data.add(new TestData(sample, expected));
        });
        // Data with non-finite values.
        // Note: The order does not matter as this is shuffled
        // in the random order tests.
        final double a = Double.NaN;
        final double b = Double.POSITIVE_INFINITY;
        final double c = Double.NEGATIVE_INFINITY;
        final double[][] nonFinite = {
            {a, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {b, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {c, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {a, a, 1, 1, 1, 1, 1, 1, 1, 1},
            {b, b, 1, 1, 1, 1, 1, 1, 1, 1},
            {c, c, 1, 1, 1, 1, 1, 1, 1, 1},
            {a, b, 1, 1, 1, 1, 1, 1, 1, 1},
            {a, c, 1, 1, 1, 1, 1, 1, 1, 1},
            {b, c, 1, 1, 1, 1, 1, 1, 1, 1},
            {a, b, c, 1, 1, 1, 1, 1, 1, 1},
        };
        Stream.of(nonFinite).forEach(values -> {
            final double[] sample = values.clone();
            for (int i = sample.length; i-- > 0;) {
                if (!Double.isFinite(values[i])) {
                    break;
                }
                sample[i] = mapValue(sample[i]);
            }
            final double expected = getExpectedNonFiniteValue(sample);
            dataNonFinite.add(new TestData(sample, expected));
        });
        // Cache the custom test data so any computed expected values are calculated only once
        streamTestData().forEach(dataCustom::add);
    }

    /**
     * Gets the statistic name.
     *
     * <p>The default implementation removes the text {@code "Test"} from the
     * simple class name.
     *
     * @return the distribution name
     * @see Class#getSimpleName()
     */
    protected String getStatisticName() {
        return getClass().getSimpleName().replace("Test", "");
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
    protected abstract S create(double... values);

    /**
     * Creates the statistic from the {@code values} using the range {@code [from, to)}.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return the statistic
     */
    protected abstract S create(double[] values, int from, int to);

    /**
     * Get the maximum number of values that can be added where the statistic is
     * considered empty.
     *
     * <p>The default is zero. Override this method if the statistic requires multiple
     * values to compute.
     *
     * @return the empty size
     * @see #getEmptyValue()
     */
    protected int getEmptySize() {
        return 0;
    }

    /**
     * Gets the value of the statistics when it is considered empty.
     *
     * @return the empty value
     * @see #getEmptySize()
     */
    protected abstract double getEmptyValue();

    /**
     * Map the {@code value} to the valid domain of the statistic. This method is called
     * with the example data before {@link #getExpectedValue(double[])} and
     * {@link #getExpectedNonFiniteValue(double[])}. In the later case the method is only
     * called with finite values. It can be used by implementing classes to adjust the data
     * to a valid domain, for example if computing the sum-of-logs all values can be
     * updated to be strictly positive.
     *
     * <p>The default implementation does nothing.
     *
     * @param value Value
     * @return the mapped value
     */
    protected double mapValue(double value) {
        return value;
    }

    /**
     * Gets the expected value of the statistic from the {@code values}.
     *
     * <p>This method should be implemented using the canoncial definition of the statistic,
     * ideally in high-precision (e.g. using BigDecimal for statistics involving arithmetic).
     *
     * <p>This method will not be called with non-finite values.
     *
     * @param values Values.
     * @return the expected value
     */
    protected abstract double getExpectedValue(double[] values);

    /**
     * Gets the expected value of the statistic from the {@code values}, at least one
     * of which will be non-finite. This method will be called with arrays that contain
     * combinations of {@code NaN} and {@code +/-infinity}.
     *
     * @param values Values.
     * @return the expected value
     */
    protected abstract double getExpectedNonFiniteValue(double[] values);

    /**
     * Gets the tolerance for equality of the statistic and the expected value.
     * This method is not used by the base implementation except to provide the default
     * implementation for the individual test tolerance methods.
     *
     * <p><strong>Note</strong>
     *
     * <p>The default implementation throws an exception. This forces the
     * implementation to either provide a single tolerance for all tests, or the tests
     * individually by overriding each tolerance method.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getTolerance() {
        throw new IllegalStateException("Not all test tolerances have been configured");
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for the {@link #testAccept(double[], double, DoubleTolerance)} test.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceAccept() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for the {@link #testArray(double[], double, DoubleTolerance)} test.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceArray() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for the {@link #testAcceptAndCombine(double[][], double, DoubleTolerance)} test.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceAcceptAndCombine() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for the {@link #testArrayAndCombine(double[][], double, DoubleTolerance)} test.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceArrayAndCombine() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for any test that includes data with non-finite values.
     *
     * <p>The default implementation uses binary equality.
     *
     * <p>Note: This tolerance has not been separated for different tests that use
     * the {@code accept}, {@link #create(double...)} and {@code combine} methods.
     * Computation of a statistic from non-finite values should produce either
     * {@code NaN} or {@code +/-infinity}. However this is not enforced as the expected
     * value is computed by implementations.
     *
     * @return the tolerance
     * {@link #getExpectedNonFiniteValue(double[])}
     */
    protected DoubleTolerance getToleranceNonFinite() {
        return DoubleTolerances.equals();
    }

    /**
     * Returns {@code true} if the
     * {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method is symmetric. If {@code true} then the combine will be tested with
     * duplicate instances of the statistic, combined in left-to-right and right-to-left;
     * the result must have the same statistic value.
     *
     * <p>The default is {@code true}. Override this if the implementation is not symmetric
     * due to the implementation details.
     *
     * @return {@code true} if combine is symmetric
     */
    protected boolean isCombineSymmetric() {
        return true;
    }

    //------------------------ Helper Methods to create test data ---------------------------

    /**
     * Creates the tolerance using an absolute error.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical
     * equality is used.
     *
     * @param eps Absolute tolerance
     * @return the tolerance
     */
    static final DoubleTolerance createAbsTolerance(double eps) {
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
    static final DoubleTolerance createRelTolerance(double eps) {
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
    static final DoubleTolerance createAbsOrRelTolerance(double absTolerance, double relTolerance) {
        final DoubleTolerance tol = createAbsTolerance(absTolerance);
        return relTolerance > 0 ? tol.or(DoubleTolerances.relative(relTolerance)) : tol;
    }

    /**
     * Creates the test data. The test tolerance uses the 4 tolerances provided
     * by the implementation for the 4 test cases.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * @param expected Expected statistic value.
     * @param values Values.
     * @return the statistic test data
     * @see #getToleranceAccept()
     * @see #getToleranceArray()
     * @see #getToleranceAcceptAndCombine()
     * @see #getToleranceArrayAndCombine()
     */
    final StatisticTestData addReference(double expected, double... values) {
        return new StatisticTestData(values, expected,
            getToleranceAccept(), getToleranceArray(),
            getToleranceAcceptAndCombine(), getToleranceArrayAndCombine());
    }

    /**
     * Creates the test data. The same tolerance is used for all tests.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * @param expected Expected statistic value.
     * @param tol Test tolerance.
     * @param values Values.
     * @return the statistic test data
     */
    static final StatisticTestData addReference(double expected, DoubleTolerance tol, double... values) {
        return new StatisticTestData(values, expected, tol);
    }

    /**
     * Creates the test data.
     * The expected value must be provided.
     *
     * <p>This method can be used to add data computed with external reference implementations.
     *
     * @param expected Expected statistic value.
     * @param tolAccept Tolerance for the test of the statistic using the accept method.
     * @param tolArray Tolerance for the test of the statistic using the array create method.
     * @param tolAcceptCombine Tolerance for the test of the statistic using the accept and combine methods.
     * @param tolArrayCombine Tolerance for the test of the statistic using the array create and combine methods.
     * @param values Values.
     * @return the statistic test data
     */
    static final StatisticTestData addReference(double expected,
            DoubleTolerance tolAccept, DoubleTolerance tolArray,
            DoubleTolerance tolAcceptCombine, DoubleTolerance tolArrayCombine,
            double... values) {
        return new StatisticTestData(values, expected,
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
     * @see #getExpectedValue(double[])
     * @see #getToleranceAccept()
     * @see #getToleranceArray()
     * @see #getToleranceAcceptAndCombine()
     * @see #getToleranceArrayAndCombine()
     */
    final StatisticTestData addCase(double... values) {
        return new StatisticTestData(values, getExpectedValue(values),
            getToleranceAccept(), getToleranceArray(),
            getToleranceAcceptAndCombine(), getToleranceArrayAndCombine());
    }

    /**
     * Creates the edge-case/example test data. The same tolerance is used for all tests.
     * The expected value will be computed using {@link #getExpectedValue(double[])}.
     *
     * <p>This method can be used to add data to target specific edge cases.
     *
     * @param tol Test tolerance.
     * @param values Values.
     * @return the statistic test data
     */
    final StatisticTestData addCase(DoubleTolerance tol, double... values) {
        return new StatisticTestData(values, getExpectedValue(values), tol);
    }

    /**
     * Creates the edge-case/example test data.
     * The expected value will be computed using {@link #getExpectedValue(double[])}.
     *
     * <p>This method can be used to add data to target specific edge cases.
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
            double... values) {
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
     *  <li>{@link java.util.function.DoubleConsumer#accept accept}
     *  <li>{@link java.util.function.DoubleConsumer#accept accept} and
     *      {@link StatisticAccumulator#combine(StatisticResult) combine}
     *  <li>{@link #create(double...)}
     *  <li>{@link #create(double...)} and
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
     *   add(createRelTolerance(1e-15), Double.MAX_VALUE, -Double.MAX_VALUE)
     * )}
     * </pre>
     *
     * <p>If there are no additional test cases then return {@link Stream#empty()}.
     *
     * @return the stream
     */
    protected abstract Stream<StatisticTestData> streamTestData();

    /**
     * Gets the group sizes used to test the combine method with the custom test data.
     *
     * @return the group sizes
     */
    protected int[] getCombineGroupSizes() {
        return new int[] {2, 3};
    }

    //------------------------ Methods to stream the test data -----------------------------

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link java.util.function.DoubleConsumer#accept accept} method. The expected value
     * and tolerance are supplied by the implementing class.
     *
     * @return the stream
     */
    final Stream<Arguments> testAccept() {
        return streamSingleArrayData(getToleranceAccept(), StatisticTestData::getTolAccept);
    }

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link #create(double...)} method. The expected value and tolerance are supplied
     * by the implementing class.
     *
     * @return the stream
     */
    final Stream<Arguments> testArray() {
        return streamSingleArrayData(getToleranceArray(), StatisticTestData::getTolArray);
    }

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link #create(double[], int, int)} method. The expected value is the same as
     * using the {@link #create(double...)} method with the values from
     * {@link Arrays#copyOfRange(double[], int, int)}.
     *
     * @return the stream
     */
    final Stream<double[]> testArrayRange() {
        final Builder<double[]> b = Stream.builder();
        data.forEach(d -> b.accept(d.getValues()));
        dataNonFinite.forEach(d -> b.accept(d.getValues()));
        dataCustom.forEach(d -> b.accept(d.getValues()));
        return b.build();
    }

    /**
     * Stream the arguments to test the computation of the statistic using a single
     * {@code double[]} array.
     *
     * @param tol Test tolerance.
     * @param tolCustom Tolerance for any custom test data.
     * @return the stream
     */
    private Stream<Arguments> streamSingleArrayData(DoubleTolerance tol,
            Function<StatisticTestData, DoubleTolerance> tolCustom) {
        final Builder<Arguments> b = Stream.builder();
        data.forEach(d -> b.accept(Arguments.of(d.getValues(), d.getExpected(), tol)));
        dataNonFinite.forEach(d -> b.accept(Arguments.of(d.getValues(), d.getExpected(), getToleranceNonFinite())));
        dataCustom.forEach(d -> b.accept(Arguments.of(d.getValues(), d.getExpected(), tolCustom.apply(d))));
        return b.build();
    }

    /**
     * Stream the arguments to test the computation of the statistic using the
     * {@link java.util.function.DoubleConsumer#accept(double) accept} method for each
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
     * {@link #create(double...)} method for each array, then the
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
     * {@link java.util.function.DoubleConsumer#accept(double) accept} method for each
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
     * {@code double[]} arrays and the
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
        dataNonFinite.forEach(d -> {
            // Cut the standard data into pieces
            Arrays.stream(SIZES_NON_FINITE)
                  .map(x -> cut(d.getValues(), x))
                  .forEach(e -> b.accept(Arguments.of(e, d.getExpected(), getToleranceNonFinite())));
        });
        dataCustom.forEach(d -> {
            // Only split lengths above the number of groups
            final double[] values = d.getValues();
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
    private static double[][] cut(double[] data, int... sizes) {
        final ArrayList<double[]> set = new ArrayList<>();
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
        return set.toArray(new double[0][]);
    }

    /**
     * Split the data into approximately even groups. The number of groups must be smaller
     * than the data length.
     *
     * @param data Data.
     * @param groups Number of groups.
     * @return the set of arrays
     */
    private static double[][] split(double[] data, int groups) {
        // Note: round the size of the group.
        // The final group accounts for length difference:
        // 4/3 => 1, 1, 2
        // 5/3 => 2, 2, 1
        // 7/3 => 2, 2, 3
        final int size = (int) Math.round((double) data.length / groups);
        final double[][] values = new double[groups][];
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
     * Test the uninitialized state of the statistic created from an empty range of an array.
     */
    @Test
    final void testEmptyArrayRange() {
        assertEmpty(create(new double[10], 2, 2), getToleranceArray());
    }

    /**
     * Assert the uninitialized state of the statistic.
     *
     * @param stat Statistic.
     * @param tol Non-empty tolerance.
     */
    private void assertEmpty(S stat, DoubleTolerance tol) {
        final double v = getEmptyValue();
        final int size = getEmptySize();
        final double[] values = new double[size + 1];
        // Fill beyond empty
        for (int i = 0; i <= size; i++) {
            Assertions.assertEquals(v, stat.getAsDouble(), () -> statisticName + " should be empty");
            stat.accept(i + 1);
            values[i] = i + 1;
        }
        final double expected = getExpectedValue(values);
        TestUtils.assertEquals(expected, stat.getAsDouble(), tol,
            () -> statisticName + " should not be empty");
    }

    /**
     * Test the computation of the statistic using the
     * {@link java.util.function.DoubleConsumer#accept(double) accept} method. The
     * statistic is created using both the {@link #create()} and the
     * {@link #create(double...)} methods; the two instances must compute the same result.
     */
    @ParameterizedTest
    @MethodSource
    final void testAccept(double[] values, double expected, DoubleTolerance tol) {
        final double actual = assertStatistic(v -> Statistics.add(create(), v), values, expected, tol);
        // Repeat after creation with an empty array
        assertStatistic(v -> Statistics.add(create(EMPTY), v), values, actual, tol);
    }

    /**
     * Test the computation of the statistic using the {@link #create(double...)} method.
     */
    @ParameterizedTest
    @MethodSource
    final void testArray(double[] values, double expected, DoubleTolerance tol) {
        assertStatistic(this::create, values, expected, tol);
    }

    /**
     * Test the computation of the statistic using the {@link #create(double[], int, int)} method.
     */
    @ParameterizedTest
    @MethodSource
    final void testArrayRange(double[] values) {
        // Test full range and half-range
        assertStatistic(values, 0, values.length);
        assertStatistic(values, 0, values.length >> 1);
        assertStatistic(values, values.length >> 1, values.length);
        // Random range
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        for (int repeat = RANDOM_PERMUTATIONS; --repeat >= 0;) {
            final int i = rng.nextInt(values.length);
            final int j = rng.nextInt(values.length);
            assertStatistic(values, Math.min(i, j), Math.max(i, j));
        }
    }

    /**
     * Test the {@link #create(double[], int, int)} method throws with an invalid range.
     */
    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#arrayRangeTestData"})
    final void testArrayRangeThrows(int from, int to, int length) {
        final double[] values = new double[length];
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> create(values, from, to),
            () -> String.format("%s for range [%d, %d) in length %d", statisticName, from, to, length));
    }

    /**
     * Test the computation of the statistic using the
     * {@link java.util.function.DoubleConsumer#accept(double) accept} method for each
     * array, then the {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method.
     */
    @ParameterizedTest
    @MethodSource
    final void testAcceptAndCombine(double[][] values, double expected, DoubleTolerance tol) {
        assertCombine(v -> Statistics.add(create(), v), values, expected, tol);
    }

    /**
     * Test the computation of the statistic using the
     * {@link java.util.function.DoubleConsumer#accept(double) accept} method for each
     * array, then the {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method.
     */
    @ParameterizedTest
    @MethodSource
    final void testArrayAndCombine(double[][] values, double expected, DoubleTolerance tol) {
        assertCombine(this::create, values, expected, tol);
    }

    /**
     * Test the {@link StatisticAccumulator#combine(StatisticResult) combine} method
     * with an empty instance combined with a non-empty instance.
     */
    @ParameterizedTest
    @MethodSource(value = "testAccept")
    final void testEmptyCombine(double[] values) {
        final S empty = create();
        final S nonEmpty = Statistics.add(create(), values);
        final double expected = nonEmpty.getAsDouble();
        final S result = assertCombine(empty, nonEmpty);
        Assertions.assertEquals(expected, result.getAsDouble(),
            () -> statisticName + " empty.combine(nonEmpty)");
    }

    /**
     * Test the {@link StatisticAccumulator#combine(StatisticResult) combine} method
     * with a non-empty instance combined with an empty instance.
     */
    @ParameterizedTest
    @MethodSource(value = "testAccept")
    final void testCombineEmpty(double[] values) {
        final S empty = create();
        final S nonEmpty = Statistics.add(create(), values);
        final double expected = nonEmpty.getAsDouble();
        final S result = assertCombine(nonEmpty, empty);
        Assertions.assertEquals(expected, result.getAsDouble(),
            () -> statisticName + " nonEmpty.combine(empty)");
    }

    /**
     * Test the computation of the statistic using a parallel stream of {@code double}
     * values. The accumulator is the
     * {@link java.util.function.DoubleConsumer#accept(double) accept} method; the
     * combiner is the {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method.
     *
     * <p>Note: This method is similar to the
     * {@link #testAcceptAndCombine(double[][], double, DoubleTolerance)} method and uses
     * the same data, concatenated to a single array, and tolerance. The difference is the
     * former method uses explicit subsets of the data and its own fixed algorithm to
     * combine instances. This method leaves the entire process of dividing the data and
     * combining to the JDK parallel stream; results may vary across platforms as it is
     * dependent on the JDK and the available processors.
     */
    @ParameterizedTest
    @MethodSource
    final void testAcceptParallelStream(double[] values, double expected, DoubleTolerance tol) {
        final double actual = Arrays.stream(values)
            .parallel()
            .collect(this::create, DoubleStatistic::accept, StatisticAccumulator<S>::combine)
            .getAsDouble();
        TestUtils.assertEquals(expected, actual, tol, () -> statisticName + ": " + format(values));
    }

    /**
     * Test the computation of the statistic using a parallel stream of {@code double[]}
     * arrays. The arrays are mapped to a statistic using the {@link #create(double...)}
     * method, and the stream reduced using the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method.
     *
     * <p>Note: This method is similar to the
     * {@link #testArrayAndCombine(double[][], double, DoubleTolerance)} method and uses
     * the same data and tolerance. The difference is the former method uses its own fixed
     * algorithm to combine instances. This method leaves the process of combining to the
     * JDK parallel stream; results may vary across platforms as it is dependent on the
     * JDK and the available processors.
     */
    @ParameterizedTest
    @MethodSource(value = "testArrayAndCombine")
    final void testArrayParallelStream(double[][] values, double expected, DoubleTolerance tol) {
        final double actual = Arrays.stream(values)
            .parallel()
            .map(this::create)
            .reduce(StatisticAccumulator<S>::combine)
            // Return an empty instance if there is no data
            .orElseGet(this::create)
            .getAsDouble();
        TestUtils.assertEquals(expected, actual, tol, () -> statisticName + ": " + format(values));
    }

    /**
     * Invokes the {@link #testAccept(double[], double, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testAccept")
    final void testAcceptRandom(double[] values, double expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                testAccept(ArraySampler.shuffle(rng, values), expected, tol);
            }
        } catch (final AssertionError e) {
            rethrowWithSeedAndRepeat(e, seed, repeat);
        }
    }

    /**
     * Invokes the {@link #testArray(double[], double, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testArray")
    final void testArrayRandom(double[] values, double expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                testArray(ArraySampler.shuffle(rng, values), expected, tol);
            }
        } catch (final AssertionError e) {
            rethrowWithSeedAndRepeat(e, seed, repeat);
        }
    }

    /**
     * Invokes the {@link #testAcceptAndCombine(double[][], double, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testAcceptAndCombine")
    final void testAcceptAndCombineRandom(double[][] values, double expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        final double[] allValues = TestHelper.concatenate(values);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                ArraySampler.shuffle(rng, allValues);
                TestHelper.unconcatenate(allValues, values);
                testAcceptAndCombine(ArraySampler.shuffle(rng, values), expected, tol);
            }
        } catch (final AssertionError e) {
            rethrowWithSeedAndRepeat(e, seed, repeat);
        }
    }

    /**
     * Invokes the {@link #testArrayAndCombine(double[][], double, DoubleTolerance)} method using
     * random permutations of the data.
     */
    @ParameterizedTest
    @MethodSource(value = "testArrayAndCombine")
    final void testArrayAndCombineRandom(double[][] values, double expected, DoubleTolerance tol) {
        // Obtain a seed so that it can be logged to allow repeats
        final long[] seed = TestHelper.createRNGSeed();
        final UniformRandomProvider rng = TestHelper.createRNG(seed);
        final double[] allValues = TestHelper.concatenate(values);
        int repeat = 0;
        try {
            while (repeat++ < RANDOM_PERMUTATIONS) {
                ArraySampler.shuffle(rng, allValues);
                TestHelper.unconcatenate(allValues, values);
                testArrayAndCombine(ArraySampler.shuffle(rng, values), expected, tol);
            }
        } catch (final AssertionError e) {
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
    final void testCombineAsAccept(double[] values, double expected, DoubleTolerance tol) {
        final S empty = create();
        assertStatistic(v ->
            Arrays.stream(values)
                  .mapToObj(this::create)
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
    final double assertStatistic(Function<double[], S> constructor,
                                 double[] values, double expected, DoubleTolerance tol) {
        final S stat = constructor.apply(values);
        final double actual = stat.getAsDouble();
        TestUtils.assertEquals(expected, actual, tol, () -> statisticName + ": " + format(values));
        return actual;
    }

    /**
     * Assert the computation of the statistic from the specified range of values.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     */
    final void assertStatistic(double[] values, int from, int to) {
        final double expected = create(Arrays.copyOfRange(values, from, to)).getAsDouble();
        final double actual = create(values, from, to).getAsDouble();
        Assertions.assertEquals(expected, actual,
            () -> String.format("%s : [%d, %d) %s", statisticName, from, to, format(values)));
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
    final double assertCombine(Function<double[], S> constructor,
                               double[][] values, double expected, DoubleTolerance tol) {
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
                // Merging with the previous pair will make the end statistic
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
        final double actual = stat.getAsDouble();
        TestUtils.assertEquals(expected, actual, tol, () -> statisticName + ": " + format(values));
        return actual;
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
            Assertions.assertEquals(s1.getAsDouble(), s2.getAsDouble(),
                () -> statisticName + ": Non-symmetric combine");
            stats2.set(target, s2);
        }
    }

    /**
     * Combine the two statistics. This method asserts the contract of the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method.
     * The left-hand side (LHS) argument must be returned. The right-hand side (RHS) must
     * be unchanged by the operation.
     *
     * <p>Use this method whenever combining two instances of a statistic.
     *
     * @param a LHS.
     * @param b RHS.
     * @return the result
     */
    final S assertCombine(S a, S b) {
        final double before = b.getAsDouble();
        final S c = a.combine(b);
        Assertions.assertEquals(before, b.getAsDouble(), () -> statisticName + ": Combine altered the RHS");
        Assertions.assertSame(a, c, () -> statisticName + ": Combined did not return the LHS");
        return c;
    }

    /**
     * Format the values as a string. The maximum length is limited.
     *
     * @param values Values.
     * @return the string
     */
    static String format(double[] values) {
        if (values.length > MAX_FORMAT_VALUES) {
            return Arrays.stream(values)
                         .limit(MAX_FORMAT_VALUES)
                         .mapToObj(Double::toString)
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
    private static String format(double[][] values) {
        return Arrays.stream(values)
            .map(BaseDoubleStatisticTest::format)
            .collect(Collectors.joining(", "));
    }

    /**
     * Re-throw the error wrapped in an AssertionError with a message that appends the seed
     * and repeat for the random order test.
     *
     * @param e Error.
     * @param seed Seed.
     * @param repeat Repeat of the total random permutations.
     */
    private static void rethrowWithSeedAndRepeat(AssertionError e, long[] seed, int repeat) {
        throw new AssertionError(String.format("%s; Seed=%s; Repeat=%d/%d",
            e.getMessage(), Arrays.toString(seed), repeat, RANDOM_PERMUTATIONS), e);
    }
}
