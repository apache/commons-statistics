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
package org.apache.commons.statistics.inference;

import java.util.function.DoubleUnaryOperator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link BracketFinder}.
 */
class BracketFinderTest {
    /** Golden section. */
    private static final double GOLD = 1.6180339887498948482;

    @Test
    void testConstructorThrows() {
        // Input arguments must be strictly positive
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BracketFinder(0, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BracketFinder(10, 0));
    }

    @Test
    void testMaxEvaluationsThrows() {
        final DoubleUnaryOperator func = x -> x * x;
        final BracketFinder bFind = new BracketFinder(100, 2);
        Assertions.assertThrows(IllegalStateException.class, () -> bFind.search(func, -1, 1, 0, 0));
    }

    @Test
    void testCubicMin() {
        final DoubleUnaryOperator func = new DoubleUnaryOperator() {
            @Override
            public double applyAsDouble(double x) {
                if (x < -2) {
                    return applyAsDouble(-2);
                }
                return (x - 1) * (x + 2) * (x + 3);
            }
        };
        final BracketFinder bFind = new BracketFinder();

        assertBracket(func, bFind,  -2, -1);
        final double tol = 1e-15;
        // Comparing with results computed in Python.
        Assertions.assertEquals(-2, bFind.getLo());
        Assertions.assertEquals(-1, bFind.getMid());
        // This value is from a single step using the golden ratio
        Assertions.assertEquals(GOLD - 1, bFind.getHi(), tol);
        Assertions.assertEquals(3, bFind.getEvaluations());
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 1",
        "1, -1",
        "1, 2",
        "2, 1",
    })
    void testIntervalBoundsOrdering(int lo, int hi) {
        final DoubleUnaryOperator func = x -> x * x;
        final BracketFinder bFind = new BracketFinder();

        assertBracket(func, bFind, lo, hi);
        Assertions.assertTrue(bFind.getLo() <= 0);
        Assertions.assertTrue(0 <= bFind.getHi());
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 1, 0, 0",
        "0, 1, 0, 0",
        "-1, 0, 0, 0",
        "-1, 1, -10, 10",
        "0, 1, -10, 10",
        "-1, 0, -10, 10",
        // Original points do not bracket the minimum
        "1, 2, -10, 10",
        "-2, -1, -10, 10",
        "5, 6, -10, 10",
        "-6, -5, -10, 10",
        // Bounds at the minimum
        "1, 2, 0, 10",
        "-2, -1, -10, 0",
        // Bounds prevent bracketing a minimum
        "5, 6, 1, 10",
        // Original points at the bounds
        "1, 2, 1, 10",
        "-2, -1, -10, -1",
        // 1 original point outside at the bounds
        "5, 20, -1, 10",
        "-20, -5, -10, 1",
    })
    void testXSquared(double a, double b, double min, double max) {
        final DoubleUnaryOperator func = x -> x * x;
        // Limit max evaluations for this test
        final BracketFinder bFind = new BracketFinder(100, 100);
        assertBracket(func, bFind, a, b, min, max);
    }

    @Test
    void testSpline1() {
        // First three points (a, b, c) of the BracketFinder create lower function values.
        // The parabolic fit of the points creates a value such that:
        // a < b < w < c
        // Use a spline node to force f(w) > f(b) and the minima is in [a, w].
        final double gold = GOLD;
        // a=-1, b=0, c=gold
        final double[] x = {-1, 0, gold / 2, gold};
        final double[] y = {1, 0, 1, -0.01};
        final PolynomialSplineFunction f = new SplineInterpolator().interpolate(x, y);
        final DoubleUnaryOperator func = f::value;
        final BracketFinder bFind = new BracketFinder();
        assertBracket(func, bFind, -1, 0);
        // Check that the bracket is found on the first iteration
        Assertions.assertEquals(-1, bFind.getLo());
        Assertions.assertEquals(0, bFind.getMid());
        Assertions.assertTrue(bFind.getHi() < gold);
    }

    @Test
    void testSpline2() {
        // First three points (a, b, c) of the BracketFinder create lower function values.
        // The parabolic fit of the points creates a value such that:
        // a < b < w < c
        // Use a spline node to force f(b) < f(w) < f(c) and the BracketFinder takes
        // a default step.
        final double gold = GOLD;
        // a=-1, b=0, c=gold
        final double[] x = {-1, 0, gold / 2, gold, 3, 4, 5};
        final double[] y = {1, 0, -0.005, -0.01, 1, 2, 3};
        final PolynomialSplineFunction f = new SplineInterpolator().interpolate(x, y);
        final DoubleUnaryOperator func = f::value;
        final BracketFinder bFind = new BracketFinder();
        assertBracket(func, bFind, -1, 0);
        // Here the bracket should advance the points on the first iteration
        // and terminate on the second
        Assertions.assertEquals(0, bFind.getLo());
        Assertions.assertEquals(gold, bFind.getMid(), 1e-15);
        Assertions.assertTrue(bFind.getHi() < 5);
    }

    @Test
    void testSpline3() {
        // First three points (a, b, c) of the BracketFinder create lower function values.
        // The parabolic fit of the points creates a value such that:
        // a < b < c < w < limit
        // Use a spline node to force f(w) > f(c) and the BracketFinder takes terminates
        // on the first iteration.
        final double gold = GOLD;
        // a=-1, b=0, c=gold
        // First 3 points are parabola: f(x) = (x-gold-0.5)^2, then really steep
        final double[] x = {-1, 0, gold, 2, 5};
        final double[] y = {9.7221359549995796, 4.4860679774997898, 0.25, 10, 100};
        final PolynomialSplineFunction f = new SplineInterpolator().interpolate(x, y);
        final DoubleUnaryOperator func = f::value;
        final BracketFinder bFind = new BracketFinder();
        assertBracket(func, bFind, -1, 0);
        // Here the bracket should advance the points on the first iteration
        // and terminate
        Assertions.assertEquals(0, bFind.getLo());
        Assertions.assertEquals(gold, bFind.getMid(), 1e-15);
        Assertions.assertTrue(bFind.getHi() < 5);
    }

    @Test
    void testParabolicMinimum() {
        // The parabolic fit of the points creates a value such that:
        // c == w (exact minimum).
        // The BracketFinder ignores this point and takes a default step.
        final double gold = GOLD;
        final DoubleUnaryOperator func = x -> x * x;
        final BracketFinder bFind = new BracketFinder();
        assertBracket(func, bFind, -gold - 1, -gold);
        // Here the bracket should advance the points on the first iteration
        // and terminate
        Assertions.assertEquals(-gold, bFind.getLo());
        Assertions.assertEquals(0, bFind.getMid(), 1e-15);
        Assertions.assertTrue(bFind.getHi() < 5);
    }

    /**
     * Assert the bracket surrounds a minima.
     *
     * @param func Function to bracket.
     * @param bFind Bracket finder.
     */
    private static void assertBracket(DoubleUnaryOperator func, final BracketFinder bFind,
                                      double a, double b) {
        assertBracket(func, bFind, a, b, 0, 0);
    }

    /**
     * Assert the bracket surrounds a minima.
     *
     * @param func Function to bracket.
     * @param bFind Bracket finder.
     * @param a Initial point.
     * @param b Initial point.
     * @param min Minimum bound of the bracket (inclusive).
     * @param max Maximum bound of the bracket (inclusive).
     */
    private static void assertBracket(DoubleUnaryOperator func, final BracketFinder bFind,
                                      double a, double b,
                                      double min, double max) {
        final boolean result = bFind.search(func, a, b, min, max);
        Assertions.assertEquals(func.applyAsDouble(bFind.getLo()), bFind.getFLo(), "f(lo)");
        Assertions.assertEquals(func.applyAsDouble(bFind.getMid()), bFind.getFMid(), "f(mid)");
        Assertions.assertEquals(func.applyAsDouble(bFind.getHi()), bFind.getFHi(), "f(hi)");
        Assertions.assertTrue(bFind.getLo() <= bFind.getMid(), "lo > mid");
        Assertions.assertTrue(bFind.getMid() <= bFind.getHi(), "hi < mid");
        Assertions.assertTrue(bFind.getFMid() <= bFind.getFLo(), "f(lo) < f(mid)");
        Assertions.assertTrue(bFind.getFMid() <= bFind.getFHi(), "f(hi) < f(mid)");
        Assertions.assertEquals(bFind.getMid() > bFind.getLo() && bFind.getMid() < bFind.getHi(), result, "middle within [lo, hi] interval");
        if (min < max) {
            Assertions.assertTrue(min <= bFind.getLo(), "lo < min");
            Assertions.assertTrue(max >= bFind.getHi(), "hi > max");
        }
    }
}
