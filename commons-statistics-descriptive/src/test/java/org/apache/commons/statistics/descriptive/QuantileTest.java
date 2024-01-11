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

    /**
     * Interface to test the quantile for a single probability.
     */
    interface QuantileFunction {
        double evaluate(Quantile m, double[] values, double p);
    }

    /**
     * Interface to test the quantiles for a multiple probabilities.
     */
    interface QuantileFunction2 {
        double[] evaluate(Quantile m, double[] values, double[] p);
    }

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
        final double[] values = {3, 4, 2, 1, 0};
        final Quantile m = Quantile.withDefaults();
        for (final double p : new double[] {-0.5, 1.2, Double.NaN}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values, p));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values, new double[] {p}));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(10, i -> 1, p));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(10, i -> 1, new double[] {p}));
        }
    }

    @Test
    void testNoQuantilesThrows() {
        final double[] values = {3, 4, 2, 1, 0};
        final Quantile m = Quantile.withDefaults();
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values));
        Assertions.assertThrows(IllegalArgumentException.class, () -> m.evaluate(values, new double[0]));
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

    @ParameterizedTest
    @MethodSource(value = {"testQuantile"})
    void testQuantile(double[] values, double[] p, double[][] expected, double delta) {
        assertQuantile(Quantile.withDefaults(), values, p, expected, delta,
            Quantile::evaluate, Quantile::evaluate);
    }

    @ParameterizedTest
    @MethodSource(value = {"testQuantile"})
    void testQuantileExcludeNaN(double[] values, double[] p, double[][] expected, double delta) {
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
    @MethodSource(value = {"testQuantile"})
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
        QuantileFunction f1, QuantileFunction2 f2) {
        Assertions.assertEquals(expected.length, TYPES.length);
        for (int i = 0; i < TYPES.length; i++) {
            final EstimationMethod type = TYPES[i];
            m = m.with(type);
            // Single quantiles
            for (int j = 0; j < p.length; j++) {
                if (f1 != null) {
                    assertEqualsOrExactlyEqual(expected[i][j], f1.evaluate(m, values.clone(), p[j]), delta,
                        () -> type.toString());
                }
                assertEqualsOrExactlyEqual(expected[i][j], f2.evaluate(m, values.clone(), new double[] {p[j]})[0], delta,
                    () -> type.toString());
            }
            // Bulk quantiles
            if (delta < 0) {
                Assertions.assertArrayEquals(expected[i], f2.evaluate(m, values.clone(), p),
                    () -> type.toString());
            } else {
                Assertions.assertArrayEquals(expected[i], f2.evaluate(m, values.clone(), p), delta,
                    () -> type.toString());
            }
        }
    }

    /**
     * Assert that {@code expected} and {@code actual} are equal within the given{@code delta}.
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

    static Stream<Arguments> testQuantile() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Special cases
        final double nan = Double.NaN;
        addQuantiles(builder, new double[] {}, new double[] {0.75}, 1e-5,
            new double[] {nan, nan, nan, nan, nan, nan, nan, nan, nan});
        addQuantiles(builder, new double[] {42}, new double[] {0.75}, 1e-5,
            new double[] {42, 42, 42, 42, 42, 42, 42, 42, 42});
        // Cases from Commons Math PercentileTest
        addQuantiles(builder, new double[] {1, 2, 3}, new double[] {0.75}, 1e-5,
            new double[] {3, 3, 2, 2.25, 2.75, 3, 2.5, 2.83333, 2.81250});
        addQuantiles(builder, new double[] {0, 1}, new double[] {0.25}, 1e-5,
            new double[] {0, 0, 0, 0, 0, 0, 0.25, 0, 0});
        final double[] d = new double[] {1, 3, 2, 4};
        addQuantiles(builder, d, new double[] {0.3, 0.25, 0.75, 0.5}, 1e-5,
            new double[] {2, 2, 1, 1.2, 1.7, 1.5, 1.9, 1.63333, 1.65},
            new double[] {1, 1.5, 1, 1, 1.5, 1.25, 1.75, 1.41667, 1.43750},
            new double[] {3, 3.5, 3, 3, 3.5, 3.75, 3.25, 3.58333, 3.56250},
            new double[] {2, 2.5, 2, 2, 2.5, 2.5, 2.5, 2.5, 2.5});
        // NIST example
        addQuantiles(builder,
            new double[] {95.1772, 95.1567, 95.1937, 95.1959, 95.1442, 95.0610, 95.1591, 95.1195, 95.1772, 95.0925,
                95.1990, 95.1682},
            new double[] {0.9}, 1e-4,
            new double[] {95.19590, 95.19590, 95.19590, 95.19546, 95.19683, 95.19807, 95.19568, 95.19724, 95.19714});
        addQuantiles(builder,
            new double[] {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0, 8.2, 10.3, 11.3, 14.1, 9.9, 12.2, 12.0, 12.1, 11.0,
                19.8, 11.0, 10.0, 8.8, 9.0, 12.3},
            new double[] {0.05}, 1e-4,
            new double[] {8.8000, 8.8000, 8.2000, 8.2600, 8.5600, 8.2900, 8.8100, 8.4700, 8.4925});
        // Special values tests
        addQuantiles(builder,
            new double[] {nan},
            new double[] {0.5}, 1e-4,
            new double[] {nan, nan, nan, nan, nan, nan, nan, nan, nan});
        addQuantiles(builder,
            new double[] {nan, nan},
            new double[] {0.5}, 1e-4,
            new double[] {nan, nan, nan, nan, nan, nan, nan, nan, nan});
        addQuantiles(builder,
            new double[] {1, nan},
            new double[] {0.5}, 1e-4,
            new double[] {1, nan, 1, 1, nan, nan, nan, nan, nan});
        addQuantiles(builder,
            new double[] {1, 2, nan},
            new double[] {0.5}, 1e-4,
            new double[] {2, 2, 2, 1.5, 2, 2, 2, 2, 2});
        addQuantiles(builder,
            new double[] {1, 2, nan, nan},
            new double[] {0.5}, 1e-4,
            new double[] {2, nan, 2, 2, nan, nan, nan, nan, nan});
        // min/max test. This hits edge cases for bounds clipping when computing
        // the index in [0, n).
        addQuantiles(builder,
            new double[] {5, 4, 3, 2, 1},
            new double[] {0.0, 1.0}, 0.0,
            new double[] {1, 1, 1, 1, 1, 1, 1, 1, 1},
            new double[] {5, 5, 5, 5, 5, 5, 5, 5, 5});
        // Note: This tests interpolation between -0.0 and -0.0, and -0.0 and 0.0.
        // When the quantile requires interpolation, the sign should be maintained
        // if the upper bound is -0.0.
        // No interpolation
        addQuantiles(builder,
            new double[] {-0.0, 0.0, 0.0},
            new double[] {0.0}, -1,
            new double[] {-0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0});
        // Interpolation between negative zeros
        addQuantiles(builder,
            new double[] {-0.0, -0.0, -0.0, -0.0, -0.0},
            new double[] {0.45}, -1,
            new double[] {-0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0});
        // Interpolation between -0.0 and 0.0
        addQuantiles(builder,
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
    private static void addQuantiles(Stream.Builder<Arguments> builder,
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
    void testQuantileWithCopy() {
        final double[] values = {3, 4, 2, 1, 0};
        final double[] original = values.clone();
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(true).evaluate(values, 0.5));
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(false).evaluate(values, 0.5));
        Assertions.assertFalse(Arrays.equals(original, values));
    }

    @Test
    void testQuantileWithCopy2() {
        final double[] values = {3, 4, 2, 1, 0};
        final double[] original = values.clone();
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(true).evaluate(values, new double[] {0.5})[0]);
        Assertions.assertArrayEquals(original, values);
        Assertions.assertEquals(2, Quantile.withDefaults().withCopy(false).evaluate(values, new double[] {0.5})[0]);
        Assertions.assertFalse(Arrays.equals(original, values));
    }
}
