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
package org.apache.commons.statistics.inference;


import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import org.apache.commons.statistics.inference.BrentOptimizer.PointValuePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link BrentOptimizer}.
 */
final class BrentOptimizerTest {
    /** Minimum relative epsilon used by the solver. */
    private static final double MIN_REL = 2 * Math.ulp(1.0);
    /** Minimum absolute epsilon used by the solver. */
    private static final double MIN_ABS = Double.MIN_VALUE;

    @Test
    void testInvalidThresholdsThrows() {
        Assertions.assertDoesNotThrow(() -> new BrentOptimizer(MIN_REL, MIN_ABS));
        final double rel = Math.nextDown(MIN_REL);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BrentOptimizer(rel, MIN_ABS));
        final double abs = Math.nextDown(MIN_ABS);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BrentOptimizer(MIN_REL, abs));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BrentOptimizer(Double.NaN, MIN_ABS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BrentOptimizer(MIN_REL, Double.NaN));
    }

    @Test
    void testInvalidBracketThrows() {
        BrentOptimizer optimizer = new BrentOptimizer(MIN_REL, MIN_ABS);
        final DoubleUnaryOperator f = Math::sin;
        final double x = 0;
        final double fx = f.applyAsDouble(x);
        Assertions.assertDoesNotThrow(() -> optimizer.optimize(f, 0, 2 * Double.MIN_VALUE, Double.MIN_VALUE, 0), "smallest bracket");
        Assertions.assertDoesNotThrow(() -> optimizer.optimize(f, 2 * Double.MIN_VALUE, 0, Double.MIN_VALUE, 0), "smallest bracket");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, x, x, x, fx), "no bracket");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, x + 0.5, x + 1, x, fx), "start < lo");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, x, x + 1, x, fx), "start == lo");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, x - 1, x - 0.5, x, fx), "start > hi");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, x - 1, x, x, fx), "start == hi");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, Double.NaN, 1, x, fx), "lo == NaN");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, x - 1, Double.NaN, x, fx), "hi == NaN");
        Assertions.assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(f, x - 1, x + 1, Double.NaN, fx), "start == NaN");
        // The optimizer is robust to NaN function evaluations
        Assertions.assertDoesNotThrow(() -> optimizer.optimize(f, -1, 1, x, Double.NaN), "f(start) == NaN");
    }

    /**
     * Test the optimizer is insensitive to NaN. It will eventually terminate due to
     * golden section steps.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1, 0.75, 0.5, 0.1})
    void testNaN(double prob) {
        // Generate NaNs with the given probability
        final DoubleUnaryOperator f = x -> ThreadLocalRandom.current().nextDouble() < prob ? Double.NaN : 1;
        final BrentOptimizer optimizer = new BrentOptimizer(MIN_REL, MIN_ABS);

        final double a = 1.53;
        final double b = a + Math.ulp(a) * 100000;
        for (int i = 0; i < 5; i++) {
            final PointValuePair p = optimize(optimizer, f, a, b);
            Assertions.assertTrue(a < p.getPoint() && p.getPoint() < b);
        }
    }

    @Test
    void testSinMin() {
        final DoubleUnaryOperator f = Math::sin;
        final BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);

        // Note: It is not possible to get the exact answer as sin(x) == -1 over a range of x.
        PointValuePair p = optimize(optimizer, f, 4, 5);
        TestUtils.assertRelativelyEquals(3 * Math.PI / 2, p.getPoint(), 5e-12, "(4, 5)");
        Assertions.assertEquals(-1, p.getValue(), "(4, 5)");

        p = optimize(optimizer, f, 1, 5);
        TestUtils.assertRelativelyEquals(3 * Math.PI / 2, p.getPoint(), 5e-12, "(1, 5)");
        Assertions.assertEquals(-1, p.getValue(), "(1, 5)");

        Assertions.assertTrue(optimizer.getEvaluations() <= 100);
        Assertions.assertTrue(optimizer.getEvaluations() >= 15);

        // Verify that the argument order for the interval does not matter
        p = optimize(optimizer, f, 5, 1);
        TestUtils.assertRelativelyEquals(3 * Math.PI / 2, p.getPoint(), 5e-12, "(1, 5)");
        Assertions.assertEquals(-1, p.getValue(), "(1, 5)");
    }

    @Test
    void testBoundaries() {
        final double lower = -1.0;
        final double upper = +1.0;
        final DoubleUnaryOperator f = x -> {
            if (x < lower) {
                Assertions.fail("too small: " + x);
            } else if (x > upper) {
                Assertions.fail("too large: " + x);
            }
            return x;
        };
        final BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        Assertions.assertEquals(lower, optimize(optimizer, f, lower, upper).getPoint(), 1.0e-8);
    }

    @Test
    void testQuinticMin() {
        // The function has local minima at -0.27195613 and 0.82221643.
        final DoubleUnaryOperator f = quinticFunction();
        final BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        Assertions.assertEquals(-0.27195613, optimize(optimizer, f, -0.3, -0.2).getPoint(), 1.0e-8);
        Assertions.assertEquals(0.82221643, optimize(optimizer, f, 0.3, 0.9).getPoint(), 1.0e-8);
        Assertions.assertTrue(optimizer.getEvaluations() <= 50);

        // search in a large interval
        Assertions.assertEquals(-0.27195613, optimize(optimizer, f, -1.0, 0.2).getPoint(), 1.0e-8);
    }

    @Test
    void testQuinticMinStatistics() {
        final DoubleUnaryOperator f = quinticFunction();
        final BrentOptimizer optimizer = new BrentOptimizer(1e-11, 1e-14);

        final DoubleStream.Builder optValue = DoubleStream.builder();
        final IntStream.Builder eval = IntStream.builder();

        // The function has local minima at -0.27195613.
        final double min = -0.75;
        final double max = 0.25;
        final int nSamples = 200;
        final double delta = (max - min) / (nSamples + 1);
        for (int i = 1; i <= nSamples; i++) {
            final double start = min + i * delta;
            optValue.add(optimizer.optimize(f, min, max, start, f.applyAsDouble(start)).getPoint());
            eval.add(optimizer.getEvaluations());
        }

        final double meanOptValue = optValue.build().average().getAsDouble();
        Assertions.assertTrue(meanOptValue > -0.2719561281);
        Assertions.assertTrue(meanOptValue < -0.2719561280);
        int[] vals = eval.build().sorted().toArray();
        final double median = (vals[nSamples / 2] + vals[(nSamples + 1) / 2]) * 0.5;
        Assertions.assertEquals(21, median);
    }

    @Test
    void testQuinticMax() {
        // The quintic function has zeros at 0, +-0.5 and +-1.
        // The function has a local maximum at 0.27195613.
        final DoubleUnaryOperator f = quinticFunction().andThen(x -> -x);
        final BrentOptimizer optimizer = new BrentOptimizer(1e-12, 1e-14);
        Assertions.assertEquals(0.27195613, optimize(optimizer, f, 0.2, 0.3).getPoint(), 1e-8);
    }

    @Test
    void testMinEndpoints() {
        final DoubleUnaryOperator f = Math::sin;
        final BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);

        // endpoint is minimum
        PointValuePair p = optimize(optimizer, f, 3 * Math.PI / 2, 5);
        TestUtils.assertRelativelyEquals(3 * Math.PI / 2, p.getPoint(), 1e-8, "(x, 5)");
        Assertions.assertEquals(-1, p.getValue());

        p = optimize(optimizer, f, 4, 3 * Math.PI / 2);
        TestUtils.assertRelativelyEquals(3 * Math.PI / 2, p.getPoint(), 1e-8, "(4, x)");
        Assertions.assertEquals(-1, p.getValue());
    }

    @Test
    void testMath832() {
        final DoubleUnaryOperator f = x -> {
            final double sqrtX = Math.sqrt(x);
            final double a = 1e2 * sqrtX;
            final double b = 1e6 / x;
            final double c = 1e4 / sqrtX;
            return a + b + c;
        };

        final BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-8);
        final double result = optimize(optimizer, f, Double.MIN_VALUE, Double.MAX_VALUE).getPoint();

        Assertions.assertEquals(804.9355825, result, 1e-6);
    }

    /**
     * Contrived example showing that prior to the resolution of MATH-855
     * (second revision), the algorithm would not return the best point if
     * it happened to be the initial guess.
     */
    @Test
    void testKeepInitIfBest() {
        final double minSin = 3 * Math.PI / 2;
        final double offset = 1e-8;
        final double delta = 1e-7;
        final DoubleUnaryOperator f1 = Math::sin;
        final DoubleUnaryOperator f2 = new StepFunction(new double[] {minSin, minSin + offset, minSin + 2 * offset},
                                                        new double[] {0, -1, 0});
        final DoubleUnaryOperator f = x -> f1.applyAsDouble(x) + f2.applyAsDouble(x);
        // A slightly less stringent tolerance would make the test pass
        // even with the previous implementation.
        final double relTol = 1e-8;
        final BrentOptimizer optimizer = new BrentOptimizer(relTol, 1e-100);
        final double init = minSin + 1.5 * offset;
        final PointValuePair result
            = optimizer.optimize(f, minSin - 6.789 * delta,
                                    minSin + 9.876 * delta,
                                    init, f.applyAsDouble(init));

        final double sol = result.getPoint();
        final double expected = init;

//        TestUtils.printf("numEval=%d%n", optimizer.getEvaluations());
//        TestUtils.printf("min=%s f=%s%n", init, f.applyAsDouble(init));
//        TestUtils.printf("sol=%s f=%s%n", sol, f.applyAsDouble(sol));
//        TestUtils.printf("exp=%s f=%s%n", expected, f.applyAsDouble(expected));

        Assertions.assertTrue(f.applyAsDouble(sol) <= f.applyAsDouble(expected), "Best point not reported");
    }

    /**
     * Contrived example showing that prior to the resolution of MATH-855,
     * the algorithm, by always returning the last evaluated point, would
     * sometimes not report the best point it had found.
     */
    @Test
    void testMath855() {
        final double minSin = 3 * Math.PI / 2;
        final double offset = 1e-8;
        final double delta = 1e-7;
        final DoubleUnaryOperator f1 = Math::sin;
        final DoubleUnaryOperator f2 = new StepFunction(new double[] {minSin, minSin + offset, minSin + 5 * offset},
                                                        new double[] {0, -1, 0});
        final DoubleUnaryOperator f = x -> f1.applyAsDouble(x) + f2.applyAsDouble(x);
        final BrentOptimizer optimizer = new BrentOptimizer(1e-8, 1e-100);
        final PointValuePair result
            = optimize(optimizer, f, minSin - 6.789 * delta,
                                     minSin + 9.876 * delta);

        final double sol = result.getPoint();
        // Note:
        // This value may have been extracted from print statements within the BrentOptimizer.
        // It is the second to last point evaluated before convergence. The last two
        // function evaluations are -1.999999999999999 and -0.9999999999999956. The test thus
        // verifies that the best point is returned, not the latest.
        final double expected = 4.712389027602411;

        Assertions.assertTrue(f.applyAsDouble(sol) <= f.applyAsDouble(expected), "Best point not reported");
    }

    /**
     * Optimize the function using the optimizer. The start point is evaluated as the middle of
     * the provided bounds.
     *
     * @param optimizer Optimizer.
     * @param func Function.
     * @param a Lower bound (exclusive).
     * @param b Upper bound (exclusive).
     * @return the point value pair
     */
    private static PointValuePair optimize(BrentOptimizer optimizer, DoubleUnaryOperator func, double a, double b) {
        final double x = 0.5 * (a + b);
        return optimizer.optimize(func, a, b, x, func.applyAsDouble(x));
    }

    @Test
    void testQuintic() {
        final DoubleUnaryOperator func = quinticFunction();
        Assertions.assertEquals(0.0, func.applyAsDouble(0));
        Assertions.assertEquals(-0.0, func.applyAsDouble(0.5));
        Assertions.assertEquals(0.0, func.applyAsDouble(1));
        // Note: These are the result of a simple bisection search that
        // does not handle detection of a maxima in the bracket (a, b)
        Assertions.assertEquals(-0.27195612937209723, findMin(func, -0.3, -0.25));
        Assertions.assertEquals(0.8222164338827133, findMin(func, 0.8, 0.84));
        Assertions.assertEquals(0.2719561271718704, findMin(func.andThen(x -> -x), 0.25, 0.3));
    }

    /**
     * Find the min of the function using a simple bisection search that maintains the
     * two lowest points of the half interval.
     *
     * @param func Function
     * @param a Lower bound
     * @param b Upper bound
     * @return the min
     */
    private static double findMin(DoubleUnaryOperator func, double a, double b) {
        double fa = func.applyAsDouble(a);
        double fb = func.applyAsDouble(b);
        while (a + Math.ulp(a) < b) {
            final double x = 0.5 * (a + b);
            final double f = func.applyAsDouble(x);
            // Three cases
            // 1. Lower than min(fa, fb)
            // - Discard highest point
            // 2. Lower than max(fa, fb)
            // - Discard highest point
            // 3. Higher than max(fa, fb)
            // - The bracket a,b contains a maxima. Assume floating point error and
            //   discard the highest point.

            // Discard the highest point
            if (fa > fb) {
                fa = f;
                a = x;
            } else {
                fb = f;
                b = x;
            }
        }
        return fa < fb ? a : b;
    }

    /**
     * Create the quintic function.
     * The function has local minima at -0.27195613 and 0.82221643.
     * The function has zeros at 0, +-0.5 and +-1.
     * The function has a local maximum at 0.27195613.
     *
     * @return the function
     */
    private static DoubleUnaryOperator quinticFunction() {
        return x -> (x - 1) * (x - 0.5) * x * (x + 0.5) * (x + 1);
    }

    /**
     * <a href="https://en.wikipedia.org/wiki/Step_function">Step function</a>.
     */
    static class StepFunction implements DoubleUnaryOperator {
        /** Abscissae. */
        private final double[] abscissa;
        /** Ordinates. */
        private final double[] ordinate;

        /**
         * Builds a step function from a list of arguments and the corresponding
         * values. Specifically, returns the function h(x) defined by <pre><code>
         * h(x) = y[0] for all x &lt; x[1]
         *        y[1] for x[1] &le; x &lt; x[2]
         *        ...
         *        y[y.length - 1] for x &ge; x[x.length - 1]
         * </code></pre>
         * The value of {@code x[0]} is ignored, but it must be strictly less than
         * {@code x[1]}.
         *
         * <p>The input x and y are assumed to satisfy the condition where x is sorted
         * and y is the same length as x.
         *
         * @param x Domain values where the function changes value.
         * @param y Values of the function.
         */
        StepFunction(double[] x, double[] y) {
            // No argument checks. Store arrays directly.
            abscissa = x;
            ordinate = y;
        }

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(double x) {
            final int index = Arrays.binarySearch(abscissa, x);
            double fx = 0;

            if (index < -1) {
                // "x" is between "abscissa[-index-2]" and "abscissa[-index-1]".
                fx = ordinate[-index - 2];
            } else if (index >= 0) {
                // "x" is exactly "abscissa[index]".
                fx = ordinate[index];
            } else {
                // Otherwise, "x" is smaller than the first value in "abscissa"
                // (hence the returned value should be "ordinate[0]").
                fx = ordinate[0];
            }

            return fx;
        }
    }
}
