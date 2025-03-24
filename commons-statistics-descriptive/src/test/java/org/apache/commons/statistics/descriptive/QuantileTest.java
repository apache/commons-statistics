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

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.Quantile.EstimationMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Quantile}.
 */
class QuantileTest {
    /** Estimation types to test. */
    private static final EstimationMethod[] TYPES = EstimationMethod.values();
    /** The number of random trials to perform. */
    private static final int RANDOM_TRIALS = 5;

    @Test
    void testNullPropertyThrows() {
        final Quantile m = Quantile.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () -> m.with((NaNPolicy) null));
        Assertions.assertThrows(NullPointerException.class, () -> m.with((EstimationMethod) null));
    }

    @Test
    void testProbabilitiesThrows() {
        for (final int n : new int[] {-1, -42, Integer.MIN_VALUE}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(n));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(n, 0.5, 0.75));
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(1, -0.5, 0.75));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(1, 0.5, 1.75));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(1, 0.75, 0.75));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(1, 0.75, 0.5));
        final double nan = Double.NaN;
        Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(1, nan, 0.5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(1, 0.5, nan));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Quantile.probabilities(1, nan, nan));
    }

    @ParameterizedTest
    @MethodSource(value = {"testProbabilities"})
    void testProbabilities(int n, double p1, double p2, double[] expected) {
        Assertions.assertArrayEquals(expected, Quantile.probabilities(n, p1, p2), 1e-10);
    }

    static Stream<Arguments> testProbabilities() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(1, 0.0, 1.0, new double[] {0.5}));
        builder.add(Arguments.of(2, 0.0, 1.0, new double[] {1.0 / 3, 2.0 / 3}));
        builder.add(Arguments.of(5, 0.0, 1.0, new double[] {1.0 / 6, 2.0 / 6, 3.0 / 6, 4.0 / 6, 5.0 / 6}));
        builder.add(Arguments.of(1, 0.25, 0.75, new double[] {0.5}));
        builder.add(Arguments.of(2, 0.25, 0.75, new double[] {0.25 + 1.0 / 6, 0.25 + 2.0 / 6}));
        builder.add(Arguments.of(1, 0.0, 0.5, new double[] {0.25}));
        builder.add(Arguments.of(2, 0.0, 0.5, new double[] {1.0 / 6, 2.0 / 6}));
        return builder.build();
    }

    @Test
    void testBadQuantileThrows() {
        final double[] values1 = {3, 4, 2, 1, 0};
        final int[] values2 = {3, 4, 2, 1, 0};
        final Quantile m = Quantile.withDefaults();
        for (final double p : new double[] {-0.5, 1.2, Double.NaN}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values1, p));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values1, new double[] {p}));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values2, p));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values2, new double[] {p}));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values1, 0, values1.length, p));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values1, 0, values1.length, new double[] {p}));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values2, 0, values2.length, p));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values2, 0, values2.length, new double[] {p}));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(10, i -> 1, p));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(10, i -> 1, new double[] {p}));
        }
    }

    @Test
    void testNoQuantilesThrows() {
        final double[] values1 = {3, 4, 2, 1, 0};
        final int[] values2 = {3, 4, 2, 1, 0};
        final Quantile m = Quantile.withDefaults();
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values1, new double[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values2, new double[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values1, 0, values1.length));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values1, 0, values1.length, new double[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values2, 0, values2.length));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluateRange(values2, 0, values2.length, new double[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(10, i -> 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(10, i -> 1, new double[0]));
    }

    @Test
    void testInvalidSizeThrows() {
        final Quantile m = Quantile.withDefaults();
        for (final int n : new int[] {-1, -42, Integer.MIN_VALUE}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(n, i -> 1, 0.5));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(n, i -> 1, 0.5, 0.75));
        }
    }

    // double[]

    /**
     * Interface to test the quantile for a single probability.
     */
    interface DoubleQuantileFunction {
        double evaluate(Quantile m, double[] values, double p);
    }

    /**
     * Interface to test the quantiles for a multiple probabilities.
     */
    interface DoubleQuantileFunctionN {
        double[] evaluate(Quantile m, double[] values, double[] p);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleQuantile"})
    void testDoubleQuantile(double[] values, double[] p, double[][] expected, double delta) {
        assertQuantile(Quantile.withDefaults(), values, p, expected, delta,
            Quantile::evaluate, Quantile::evaluate);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleQuantile"})
    void testDoubleQuantileExcludeNaN(double[] values, double[] p, double[][] expected, double delta) {
        // If NaN is present then the result will change from expected so ignore this
        Assumptions.assumeTrue(Arrays.stream(values).filter(Double::isNaN).count() == 0);
        // Note: Use copy here. This checks that the copy of the data
        // (with excluded NaNs) is used for special cases.
        final Quantile q = Quantile.withDefaults().with(NaNPolicy.EXCLUDE).withCopy(true);
        // Insert some "random" NaN data.
        // Position can be in [0, n].
        for (final int pos : new int[] {0, values.length >>> 1, values.length,
                                        42 % (values.length + 1),
                                        1267836813 % (values.length + 1)}) {
            final double[] x = new double[values.length + 1];
            System.arraycopy(values, 0, x, 0, pos);
            x[pos] = Double.NaN;
            System.arraycopy(values, pos, x, pos + 1, values.length - pos);
            assertQuantile(q, x, p, expected, delta,
                Quantile::evaluate, Quantile::evaluate);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleQuantile"})
    void testDoubleQuantileErrorNaN(double[] values, double[] p, double[][] expected, double delta) {
        final Quantile q = Quantile.withDefaults().with(NaNPolicy.ERROR);
        final double[] y = values.clone();
        if (Arrays.stream(values).anyMatch(Double::isNaN)) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> q.evaluate(values));
            Assertions.assertArrayEquals(y, values, "Input was modified");
        } else {
            assertQuantile(q, values, p, expected, delta,
                Quantile::evaluate, Quantile::evaluate);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleQuantile"})
    void testQuantileSorted(double[] values, double[] p, double[][] expected, double delta) {
        assertQuantile(Quantile.withDefaults(), values, p, expected, delta,
            (m, x, q) -> {
                // No clone here as later calls with the same array will also sort it
                Arrays.sort(x);
                return m.evaluate(x.length, i -> x[i], q);
            },
            (m, x, q) -> {
                // No clone here as later calls with the same array will also sort it
                Arrays.sort(x);
                return m.evaluate(x.length, i -> x[i], q);
            });
    }

    private static void assertQuantile(Quantile m, double[] values, double[] p,
        double[][] expected, double delta,
        DoubleQuantileFunction f1, DoubleQuantileFunctionN fn) {
        Assertions.assertEquals(expected.length, TYPES.length);
        for (int i = 0; i < TYPES.length; i++) {
            final EstimationMethod type = TYPES[i];
            m = m.with(type);
            // Single quantiles
            for (int j = 0; j < p.length; j++) {
                if (f1 != null) {
                    assertEqualsOrExactlyEqual(expected[i][j], f1.evaluate(m, values.clone(), p[j]), delta,
                        type::toString);
                }
                assertEqualsOrExactlyEqual(expected[i][j], fn.evaluate(m, values.clone(), new double[] {p[j]})[0], delta,
                    type::toString);
            }
            // Bulk quantiles
            if (delta < 0) {
                Assertions.assertArrayEquals(expected[i], fn.evaluate(m, values.clone(), p),
                    type::toString);
            } else {
                Assertions.assertArrayEquals(expected[i], fn.evaluate(m, values.clone(), p), delta,
                    type::toString);
            }
        }
    }

    /**
     * Assert that {@code expected} and {@code actual} are equal within the given {@code delta}.
     * If the {@code delta} is negative it is ignored and values must be exactly equal.
     *
     * @param expected Expected
     * @param actual Actual
     * @param delta Delta
     * @param messageSupplier Failure message.
     */
    private static void assertEqualsOrExactlyEqual(double expected, double actual, double delta,
        Supplier<String> messageSupplier) {
        if (delta < 0) {
            Assertions.assertEquals(expected, actual, messageSupplier);
        } else {
            Assertions.assertEquals(expected, actual, delta, messageSupplier);
        }
    }

    static Stream<Arguments> testDoubleQuantile() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Special cases
        final double nan = Double.NaN;
        addDoubleQuantiles(builder, new double[] {}, new double[] {0.75}, 1e-5,
            new double[] {nan, nan, nan, nan, nan, nan, nan, nan, nan});
        addDoubleQuantiles(builder, new double[] {42}, new double[] {0.75}, 1e-5,
            new double[] {42, 42, 42, 42, 42, 42, 42, 42, 42});
        // Cases from Commons Math PercentileTest
        addDoubleQuantiles(builder, new double[] {1, 2, 3}, new double[] {0.75}, 1e-5,
            new double[] {3, 3, 2, 2.25, 2.75, 3, 2.5, 2.83333, 2.81250});
        addDoubleQuantiles(builder, new double[] {0, 1}, new double[] {0.25}, 1e-5,
            new double[] {0, 0, 0, 0, 0, 0, 0.25, 0, 0});
        final double[] d = new double[] {1, 3, 2, 4};
        addDoubleQuantiles(builder, d, new double[] {0.3, 0.25, 0.75, 0.5}, 1e-5,
            new double[] {2, 2, 1, 1.2, 1.7, 1.5, 1.9, 1.63333, 1.65},
            new double[] {1, 1.5, 1, 1, 1.5, 1.25, 1.75, 1.41667, 1.43750},
            new double[] {3, 3.5, 3, 3, 3.5, 3.75, 3.25, 3.58333, 3.56250},
            new double[] {2, 2.5, 2, 2, 2.5, 2.5, 2.5, 2.5, 2.5});
        // NIST example
        addDoubleQuantiles(builder,
            new double[] {95.1772, 95.1567, 95.1937, 95.1959, 95.1442, 95.0610, 95.1591, 95.1195, 95.1772, 95.0925,
                95.1990, 95.1682},
            new double[] {0.9}, 1e-5,
            new double[] {95.19590, 95.19590, 95.19590, 95.19546, 95.19683, 95.19807, 95.19568, 95.19724, 95.19714});
        addDoubleQuantiles(builder,
            new double[] {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0, 8.2, 10.3, 11.3, 14.1, 9.9, 12.2, 12.0, 12.1, 11.0,
                19.8, 11.0, 10.0, 8.8, 9.0, 12.3},
            new double[] {0.05}, 1e-4,
            new double[] {8.8000, 8.8000, 8.2000, 8.2600, 8.5600, 8.2900, 8.8100, 8.4700, 8.4925});
        // Special values tests
        addDoubleQuantiles(builder,
            new double[] {nan},
            new double[] {0.5}, 1e-4,
            new double[] {nan, nan, nan, nan, nan, nan, nan, nan, nan});
        addDoubleQuantiles(builder,
            new double[] {nan, nan},
            new double[] {0.5}, 1e-4,
            new double[] {nan, nan, nan, nan, nan, nan, nan, nan, nan});
        addDoubleQuantiles(builder,
            new double[] {1, nan},
            new double[] {0.5}, 1e-4,
            new double[] {1, nan, 1, 1, nan, nan, nan, nan, nan});
        addDoubleQuantiles(builder,
            new double[] {1, 2, nan},
            new double[] {0.5}, 1e-4,
            new double[] {2, 2, 2, 1.5, 2, 2, 2, 2, 2});
        addDoubleQuantiles(builder,
            new double[] {1, 2, nan, nan},
            new double[] {0.5}, 1e-4,
            new double[] {2, nan, 2, 2, nan, nan, nan, nan, nan});
        // min/max test. This hits edge cases for bounds clipping when computing
        // the index in [0, n).
        addDoubleQuantiles(builder,
            new double[] {5, 4, 3, 2, 1},
            new double[] {0.0, 1.0}, -1,
            new double[] {1, 1, 1, 1, 1, 1, 1, 1, 1},
            new double[] {5, 5, 5, 5, 5, 5, 5, 5, 5});
        // Note: This tests interpolation between -0.0 and -0.0, and -0.0 and 0.0.
        // When the quantile requires interpolation, the sign should be maintained
        // if the upper bound is -0.0.
        // No interpolation
        addDoubleQuantiles(builder,
            new double[] {-0.0, 0.0, 0.0},
            new double[] {0.0}, -1,
            new double[] {-0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0});
        // Interpolation between negative zeros
        addDoubleQuantiles(builder,
            new double[] {-0.0, -0.0, -0.0, -0.0, -0.0},
            new double[] {0.45}, -1,
            new double[] {-0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0});
        // Interpolation between -0.0 and 0.0
        addDoubleQuantiles(builder,
            new double[] {-0.0, -0.0, 0.0, 0.0, 0.0},
            new double[] {0.45}, -1,
            // Here only HF3 rounds to k=2; other discrete methods to k=3;
            // all continuous distributions interpolate to 0.0
            new double[] {0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});

        // Test data samples using R version 4.3.3
        // require(stats)
        // x = c(1, 2, 3)               %% data
        // p = c(0.1, 0.25, 0.5, 0.975) %% probabilities
        // for (t in c(1:9)) { cat('{', paste(quantile(x, p, type=t), collapse=', '), "}, \n", sep="") }

        final double[] p = {0.1, 0.25, 0.5, 0.975};

        // Use some of the standard data from BaseDoubleStatisticTest

        /** Poisson samples: scipy.stats.poisson.rvs(45, size=100). */
        final double[] v4 = {42, 51, 38, 38, 49, 48, 42, 47, 51, 46, 45, 35, 39, 42, 49, 55, 53, 46, 49, 56,
            42, 46, 42, 53, 43, 55, 49, 52, 51, 45, 40, 49, 39, 40, 46, 43, 46, 48, 36, 44, 40, 49, 49, 43, 45, 44, 41, 55,
            52, 45, 57, 41, 43, 44, 38, 52, 44, 45, 43, 42, 38, 37, 47, 42, 47, 45, 70, 45, 50, 47, 46, 50, 47, 35, 43, 52,
            51, 41, 45, 42, 45, 53, 46, 48, 51, 43, 63, 48, 49, 41, 58, 51, 59, 43, 39, 32, 35, 46, 50, 50};

        builder.add(Arguments.of(v4, p, new double[][] {
            {38, 42, 46, 59},
            {38.5, 42, 46, 59},
            {38, 42, 46, 59},
            {38, 42, 46, 58.5},
            {38.5, 42, 46, 59},
            {38.1, 42, 46, 60.9},
            {38.9, 42, 46, 58.525},
            {38.3666666666667, 42, 46, 59.6333333333333},
            {38.4, 42, 46, 59.475},
        }, 1e-13));

        /** Normal samples: scipy.stats.norm.rvs(loc=3.4, scale=2.25, size=100). */
        final double[] v5 = {1.06356579, -1.52552007, 7.09739891, -0.41516549, 0.17131653, 0.77923148,
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

        builder.add(Arguments.of(v5, p, new double[][] {
            {0.17131653, 1.598286, 3.57904997, 8.24460895},
            {0.325681565, 1.76892554, 3.61784334, 8.24460895},
            {0.17131653, 1.598286, 3.57904997, 8.24460895},
            {0.17131653, 1.598286, 3.57904997, 8.1568802},
            {0.325681565, 1.76892554, 3.61784334, 8.24460895},
            {0.202189537, 1.68360577, 3.61784334, 8.68070245524999},
            {0.449173593, 1.85424531, 3.61784334, 8.1612666375},
            {0.284517555666667, 1.74048561666667, 3.61784334, 8.38997345175},
            {0.294808558, 1.7475955975, 3.61784334, 8.35363232631249},
        }, 1e-14));

        return builder.build();
    }

    /**
     * Adds the quantiles.
     *
     * @param builder Builder.
     * @param x Data.
     * @param p Quantiles to compute.
     * @param expected Expected result for each p for every estimation type.
     */
    private static void addDoubleQuantiles(Stream.Builder<Arguments> builder,
        double[] x, double[] p, double delta, double[]... expected) {
        Assertions.assertEquals(p.length, expected.length);
        for (final double[] e : expected) {
            Assertions.assertEquals(e.length, TYPES.length);
        }
        // Transpose
        final double[][] t = new double[TYPES.length][p.length];
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < p.length; j++) {
                t[i][j] = expected[j][i];
            }
        }
        builder.add(Arguments.of(x, p, t, delta));
    }

    @Test
    void testDoubleQuantileWithCopy() {
        final double[] values = {3, 4, 2, 1, 0};
        final double[] original = values.clone();
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(true).evaluate(values, 0.5));
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(false).evaluate(values, 0.5));
        Assertions.assertFalse(Arrays.equals(original, values));
    }

    @Test
    void testDoubleQuantileWithCopy2() {
        final double[] values = {3, 4, 2, 1, 0};
        final double[] original = values.clone();
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(true).evaluate(values, new double[] {0.5})[0]);
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(false).evaluate(values, new double[] {0.5})[0]);
        Assertions.assertFalse(Arrays.equals(original, values));
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#arrayRangeTestData"})
    final void testDoubleQuantileRangeThrows(int from, int to, int length) {
        final double[] values = new double[length];
        final Supplier<String> msg = () -> String.format("range [%d, %d) in length %d", from, to, length);
        final Quantile q = Quantile.withDefaults();
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> q.evaluateRange(values, from, to, 0.5), msg);
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> q.evaluateRange(values, from, to, 0.25, 0.5), msg);
    }

    /**
     * Test data with an internal region evaluates exactly the same when using
     * a copy of the internal region evaluated as a full length array,
     * or the range method on the full array.
     */
    @Test
    void testDoubleQuantileRange() {
        // Empty range
        assertQuantileRange(new double[] {1, 2, 3, 4, 5}, 2, 2);
        // Range range
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (int count = RANDOM_TRIALS; --count >= 0;) {
            final int n = 10 + count;
            final double[] x = rng.doubles(n).toArray();
            final int i = rng.nextInt(n);
            final int j = rng.nextInt(n);
            assertQuantileRange(x, Math.min(i, j), Math.max(i, j));
        }
        // NaN in the range
        final double[] x = rng.doubles(10).toArray();
        x[5] = Double.NaN;
        assertQuantileRange(x.clone(), 2, 8);
        assertQuantileRange(x.clone(), 2, 9);
    }

    private static void assertQuantileRange(double[] values, int from, int to) {
        // Test all NaN policies as these apply to double[] data
        for (final NaNPolicy p : NaNPolicy.values()) {
            // Using p={0, 1} ensures quantiles with no interpolation are included
            for (final double prob : new double[] {0, 0.5, 0.75, 1}) {
                assertQuantileRange(values.clone(), from, to, p, prob);
            }
        }
    }

    private static void assertQuantileRange(double[] values, int from, int to, NaNPolicy nanPolicy, double prob) {
        final Supplier<String> msg = () -> String.format("NaN=%s; p=%.2f; range [%d, %d) in length %d",
            nanPolicy, prob, from, to, values.length);
        final double[] original = values.clone();
        final double[] x = Arrays.copyOfRange(values, from, to);
        final double[] p = {prob};
        // Test with/without modification of the input
        final Quantile q = Quantile.withDefaults().with(nanPolicy).withCopy(false);
        final Quantile qCopy = Quantile.withDefaults().with(nanPolicy).withCopy(true);
        try {
            // Reference result operating in-place
            final double expected = q.evaluate(x, p[0]);
            // With copy the input is unchanged
            Assertions.assertEquals(expected, qCopy.evaluateRange(values, from, to, p[0]), msg);
            Assertions.assertArrayEquals(original, values, msg);
            Assertions.assertEquals(expected, qCopy.evaluateRange(values, from, to, p)[0], msg);
            Assertions.assertArrayEquals(original, values, msg);
            // Without copy only the values inside the range should be modified.
            // Compose the expected result.
            System.arraycopy(x, 0, original, from, x.length);
            final double[] copy = values.clone();
            Assertions.assertEquals(expected, q.evaluateRange(values, from, to, p)[0], msg);
            Assertions.assertArrayEquals(original, values, msg);
            Assertions.assertEquals(expected, q.evaluateRange(copy, from, to, p[0]), msg);
            Assertions.assertArrayEquals(original, copy, msg);
        } catch (IllegalArgumentException e) {
            // NaN input
            Assertions.assertThrows(e.getClass(), () -> qCopy.evaluateRange(values, from, to, p[0]), msg);
            Assertions.assertThrows(e.getClass(), () -> qCopy.evaluateRange(values, from, to, p), msg);
            Assertions.assertThrows(e.getClass(), () -> q.evaluateRange(values, from, to, p[0]), msg);
            Assertions.assertThrows(e.getClass(), () -> q.evaluateRange(values, from, to, p), msg);
            Assertions.assertArrayEquals(original, values, msg);
        }
    }

    // int[]

    /**
     * Interface to test the quantile for a single probability.
     */
    interface IntQuantileFunction {
        double evaluate(Quantile m, int[] values, double p);
    }

    /**
     * Interface to test the quantiles for a multiple probabilities.
     */
    interface IntQuantileFunctionN {
        double[] evaluate(Quantile m, int[] values, double[] p);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntQuantile"})
    void testIntQuantile(int[] values, double[] p, double[][] expected, double delta) {
        assertQuantile(Quantile.withDefaults(), values, p, expected, delta,
            Quantile::evaluate, Quantile::evaluate);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntQuantile"})
    void testQuantileSorted(int[] values, double[] p, double[][] expected, double delta) {
        assertQuantile(Quantile.withDefaults(), values, p, expected, delta,
            (m, x, q) -> {
                // No clone here as later calls with the same array will also sort it
                Arrays.sort(x);
                return m.evaluate(x.length, i -> x[i], q);
            },
            (m, x, q) -> {
                // No clone here as later calls with the same array will also sort it
                Arrays.sort(x);
                return m.evaluate(x.length, i -> x[i], q);
            });
    }

    private static void assertQuantile(Quantile m, int[] values, double[] p,
        double[][] expected, double delta,
        IntQuantileFunction f1, IntQuantileFunctionN fn) {
        Assertions.assertEquals(expected.length, TYPES.length);
        for (int i = 0; i < TYPES.length; i++) {
            final EstimationMethod type = TYPES[i];
            m = m.with(type);
            // Single quantiles
            for (int j = 0; j < p.length; j++) {
                if (f1 != null) {
                    assertEqualsOrExactlyEqual(expected[i][j], f1.evaluate(m, values.clone(), p[j]), delta,
                        type::toString);
                }
                assertEqualsOrExactlyEqual(expected[i][j], fn.evaluate(m, values.clone(), new double[] {p[j]})[0], delta,
                    type::toString);
            }
            // Bulk quantiles
            if (delta < 0) {
                Assertions.assertArrayEquals(expected[i], fn.evaluate(m, values.clone(), p),
                    type::toString);
            } else {
                Assertions.assertArrayEquals(expected[i], fn.evaluate(m, values.clone(), p), delta,
                    type::toString);
            }
        }
    }

    static Stream<Arguments> testIntQuantile() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Special cases
        final double nan = Double.NaN;
        addIntQuantiles(builder, new int[] {}, new double[] {0.75}, 1e-5,
            new double[] {nan, nan, nan, nan, nan, nan, nan, nan, nan});
        addIntQuantiles(builder, new int[] {42}, new double[] {0.75}, 1e-5,
            new double[] {42, 42, 42, 42, 42, 42, 42, 42, 42});
        // Cases from Commons Math PercentileTest
        addIntQuantiles(builder, new int[] {1, 2, 3}, new double[] {0.75}, 1e-5,
            new double[] {3, 3, 2, 2.25, 2.75, 3, 2.5, 2.83333, 2.81250});
        addIntQuantiles(builder, new int[] {0, 1}, new double[] {0.25}, 1e-5,
            new double[] {0, 0, 0, 0, 0, 0, 0.25, 0, 0});
        final int[] d = new int[] {1, 3, 2, 4};
        addIntQuantiles(builder, d, new double[] {0.3, 0.25, 0.75, 0.5}, 1e-5,
            new double[] {2, 2, 1, 1.2, 1.7, 1.5, 1.9, 1.63333, 1.65},
            new double[] {1, 1.5, 1, 1, 1.5, 1.25, 1.75, 1.41667, 1.43750},
            new double[] {3, 3.5, 3, 3, 3.5, 3.75, 3.25, 3.58333, 3.56250},
            new double[] {2, 2.5, 2, 2, 2.5, 2.5, 2.5, 2.5, 2.5});
        // NIST example
        // Scale example 1 by 1e4
        addIntQuantiles(builder,
            new int[] {951772, 951567, 951937, 951959, 951442, 950610, 951591, 951195, 951772, 950925,
                951990, 951682},
            new double[] {0.9}, 1e-1,
            new double[] {951959.0, 951959.0, 951959.0, 951954.6, 951968.3, 951980.7, 951956.8, 951972.4, 951971.4});
        // Scale example 2 by 10
        addIntQuantiles(builder,
            new int[] {125, 120, 118, 142, 149, 145, 210, 82, 103, 113, 141, 99, 122, 120, 121, 110,
                198, 110, 100, 88, 90, 123},
            new double[] {0.05}, 1e-3,
            new double[] {88.000, 88.000, 82.000, 82.600, 85.600, 82.900, 88.100, 84.700, 84.925});
        // min/max test. This hits edge cases for bounds clipping when computing
        // the index in [0, n).
        addIntQuantiles(builder,
            new int[] {5, 4, 3, 2, 1},
            new double[] {0.0, 1.0}, -1,
            new double[] {1, 1, 1, 1, 1, 1, 1, 1, 1},
            new double[] {5, 5, 5, 5, 5, 5, 5, 5, 5});
        // Interpolation between zeros
        addIntQuantiles(builder,
            new int[] {0, 0, 0, 0, 0},
            new double[] {0.45}, -1,
            new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});

        // Test data samples using R version 4.3.3
        // require(stats)
        // x = as.integer(c(1, 2, 3))   %% data
        // p = c(0.1, 0.25, 0.5, 0.975) %% probabilities
        // for (t in c(1:9)) { cat('{', paste(quantile(x, p, type=t), collapse=', '), "}, \n", sep="") }

        final double[] p = {0.1, 0.25, 0.5, 0.975};

        /** Poisson samples: scipy.stats.poisson.rvs(45, size=100). */
        final int[] v4 = {42, 51, 38, 38, 49, 48, 42, 47, 51, 46, 45, 35, 39, 42, 49, 55, 53, 46, 49, 56,
            42, 46, 42, 53, 43, 55, 49, 52, 51, 45, 40, 49, 39, 40, 46, 43, 46, 48, 36, 44, 40, 49, 49, 43, 45, 44, 41, 55,
            52, 45, 57, 41, 43, 44, 38, 52, 44, 45, 43, 42, 38, 37, 47, 42, 47, 45, 70, 45, 50, 47, 46, 50, 47, 35, 43, 52,
            51, 41, 45, 42, 45, 53, 46, 48, 51, 43, 63, 48, 49, 41, 58, 51, 59, 43, 39, 32, 35, 46, 50, 50};

        builder.add(Arguments.of(v4, p, new double[][] {
            {38, 42, 46, 59},
            {38.5, 42, 46, 59},
            {38, 42, 46, 59},
            {38, 42, 46, 58.5},
            {38.5, 42, 46, 59},
            {38.1, 42, 46, 60.9},
            {38.9, 42, 46, 58.525},
            {38.3666666666667, 42, 46, 59.6333333333333},
            {38.4, 42, 46, 59.475},
        }, 1e-12));

        // Discrete 'normal' samples: scipy.stats.norm.rvs(loc=3.4, scale=2250000, size=100).astype(int)
        final int[] v = {659592, -849723, -1765944, 2610353, 2192883, -265784, 1126824, 1412069, 918175, -1066899,
            -1289922, -1359925, 549577, 2891935, 5498383, 649055, -3774137, 3026349, 4317084, 16068, 1747179, -94833,
            -891275, 146951, 659679, -1298483, -9717, -2372749, -213892, -956213, -2380241, -3265588, 515620, 334156,
            2595489, -463102, -1490342, -700231, -245959, -1596233, 1702451, 1265231, 2338985, -1298796, -2493882,
            -1679698, 251933, -3446511, 3437103, -2940127, -1996991, -695605, -3127437, 2895523, 1659299, 935033,
            609115, -1245544, -1131839, 1645603, -1673754, -4318740, -163129, -1733950, 2609546, -536282, -2873472,
            -204545, 872775, 448272, 1048969, -1781997, -3602571, -2346653, 2084923, 1289364, 2450980, -1809052,
            -422631, 1895287, 72169, -4933595, 2602830, -1106753, 1295126, 1671634, 929809, -3094175, -787509, -769431,
            -209387, 1517866, -1861080, 1863380, -699593, -174200, 2132930, 1957489, -340803, -2263330};

        builder.add(Arguments.of(v, p, new double[][] {
            {-2873472, -1490342, -204545, 3437103},
            {-2683677, -1425133.5, -189372.5, 3437103},
            {-2873472, -1490342, -204545, 3437103},
            {-2873472, -1490342, -204545, 3231726},
            {-2683677, -1425133.5, -189372.5, 3437103},
            {-2835513, -1457737.75, -189372.5, 3855093.975},
            {-2531841, -1392529.25, -189372.5, 3241994.85},
            {-2734289, -1436001.58333333, -189372.5, 3576433.325},
            {-2721636, -1433284.5625, -189372.5, 3541600.74374999},
        }, 1e-8));

        return builder.build();
    }

    /**
     * Adds the quantiles.
     *
     * @param builder Builder.
     * @param x Data.
     * @param p Quantiles to compute.
     * @param expected Expected result for each p for every estimation type.
     */
    private static void addIntQuantiles(Stream.Builder<Arguments> builder,
        int[] x, double[] p, double delta, double[]... expected) {
        Assertions.assertEquals(p.length, expected.length);
        for (final double[] e : expected) {
            Assertions.assertEquals(e.length, TYPES.length);
        }
        // Transpose
        final double[][] t = new double[TYPES.length][p.length];
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < p.length; j++) {
                t[i][j] = expected[j][i];
            }
        }
        builder.add(Arguments.of(x, p, t, delta));
    }

    @Test
    void testIntQuantileWithCopy() {
        final int[] values = {3, 4, 2, 1, 0};
        final int[] original = values.clone();
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(true).evaluate(values, 0.5));
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(false).evaluate(values, 0.5));
        Assertions.assertFalse(Arrays.equals(original, values));
    }

    @Test
    void testIntQuantileWithCopy2() {
        final int[] values = {3, 4, 2, 1, 0};
        final int[] original = values.clone();
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(true).evaluate(values, new double[] {0.5})[0]);
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(false).evaluate(values, new double[] {0.5})[0]);
        Assertions.assertFalse(Arrays.equals(original, values));
    }

    @ParameterizedTest
    @MethodSource(value = {"org.apache.commons.statistics.descriptive.TestData#arrayRangeTestData"})
    final void testIntQuantileRangeThrows(int from, int to, int length) {
        final int[] values = new int[length];
        final Supplier<String> msg = () -> String.format("range [%d, %d) in length %d", from, to, length);
        final Quantile q = Quantile.withDefaults();
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> q.evaluateRange(values, from, to, 0.5), msg);
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> q.evaluateRange(values, from, to, 0.25, 0.5), msg);
    }

    /**
     * Test data with an internal region evaluates exactly the same when using
     * a copy of the internal region evaluated as a full length array,
     * or the range method on the full array.
     */
    @Test
    void testIntQuantileRange() {
        // Empty range
        assertQuantileRange(new int[] {1, 2, 3, 4, 5}, 2, 2);
        // Range range
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (int count = RANDOM_TRIALS; --count >= 0;) {
            final int n = 10 + count;
            final int[] x = rng.ints(n).toArray();
            final int i = rng.nextInt(n);
            final int j = rng.nextInt(n);
            assertQuantileRange(x, Math.min(i, j), Math.max(i, j));
        }
    }

    private static void assertQuantileRange(int[] values, int from, int to) {
        // Using p={0, 1} ensures quantiles with no interpolation are included
        for (final double p : new double[] {0, 0.5, 0.75, 1}) {
            assertQuantileRange(values.clone(), from, to, p);
        }
    }

    private static void assertQuantileRange(int[] values, int from, int to, double prob) {
        final Supplier<String> msg = () -> String.format("p=%.2f range [%d, %d) in length %d",
            prob, from, to, values.length);
        final int[] original = values.clone();
        final int[] x = Arrays.copyOfRange(values, from, to);
        final double[] p = {prob};
        // Test with/without modification of the input
        final Quantile q = Quantile.withDefaults().withCopy(false);
        final Quantile qCopy = Quantile.withDefaults().withCopy(true);
        // Reference result operating in-place
        final double expected = q.evaluate(x, p[0]);
        // With copy the input is unchanged
        Assertions.assertEquals(expected, qCopy.evaluateRange(values, from, to, p[0]), msg);
        Assertions.assertArrayEquals(original, values, msg);
        Assertions.assertEquals(expected, qCopy.evaluateRange(values, from, to, p)[0], msg);
        Assertions.assertArrayEquals(original, values, msg);
        // Without copy only the values inside the range should be modified.
        // Compose the expected result.
        System.arraycopy(x, 0, original, from, x.length);
        final int[] copy = values.clone();
        Assertions.assertEquals(expected, q.evaluateRange(values, from, to, p)[0], msg);
        Assertions.assertArrayEquals(original, values, msg);
        Assertions.assertEquals(expected, q.evaluateRange(copy, from, to, p[0]), msg);
        Assertions.assertArrayEquals(original, copy, msg);
    }
}
