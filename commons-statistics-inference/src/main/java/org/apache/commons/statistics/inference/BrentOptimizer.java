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
import org.apache.commons.numbers.core.Precision;

/**
 * For a function defined on some interval {@code (lo, hi)}, this class
 * finds an approximation {@code x} to the point at which the function
 * attains its minimum.
 * It implements Richard Brent's algorithm (from his book "Algorithms for
 * Minimization without Derivatives", p. 79) for finding minima of real
 * univariate functions.
 *
 * <P>This code is an adaptation, partly based on the Python code from SciPy
 * (module "optimize.py" v0.5); the original algorithm is also modified:
 * <ul>
 *  <li>to use an initial guess provided by the user,</li>
 *  <li>to ensure that the best point encountered is the one returned.</li>
 * </ul>
 *
 * <p>This class has been extracted from {@code o.a.c.math4.optim.univariate}
 * and simplified to remove support for the UnivariateOptimizer interface.
 * This removed the options: to find the maximum; use a custom convergence checker
 * on the function value; and remove the maximum function evaluation count.
 * The class now implements a single optimize method within the provided bracket
 * from the given start position (with value).
 *
 * @since 1.1
 */
final class BrentOptimizer {
    /** Golden section. (3 - sqrt(5)) / 2. */
    private static final double GOLDEN_SECTION = 0.3819660112501051;
    /** Minimum relative tolerance. 2 * eps = 2^-51. */
    private static final double MIN_RELATIVE_TOLERANCE = 0x1.0p-51;

    /** Relative threshold. */
    private final double relativeThreshold;
    /** Absolute threshold. */
    private final double absoluteThreshold;
    /** The number of function evaluations from the most recent call to optimize. */
    private int evaluations;

    /**
     * This class holds a point and the value of an objective function at this
     * point. This is a simple immutable container.
     *
     * @since 1.1
     */
    static final class PointValuePair {
        /** Point. */
        private final double point;
        /** Value of the objective function at the point. */
        private final double value;

        /**
         * @param point Point.
         * @param value Value of an objective function at the point.
         */
        private PointValuePair(double point, double value) {
            this.point = point;
            this.value = value;
        }

        /**
         * Create a point/objective function value pair.
         *
         * @param point Point.
         * @param value Value of an objective function at the point.
         * @return the pair
         */
        static PointValuePair of(double point, double value) {
            return new PointValuePair(point, value);
        }

        /**
         * Get the point.
         *
         * @return the point.
         */
        double getPoint() {
            return point;
        }

        /**
         * Get the value of the objective function.
         *
         * @return the stored value of the objective function.
         */
        double getValue() {
            return value;
        }
    }

    /**
     * The arguments are used to implement the original stopping criterion
     * of Brent's algorithm.
     * {@code abs} and {@code rel} define a tolerance
     * {@code tol = rel |x| + abs}. {@code rel} should be no smaller than
     * <em>2 macheps</em> and preferably not much less than <em>sqrt(macheps)</em>,
     * where <em>macheps</em> is the relative machine precision. {@code abs} must
     * be positive.
     *
     * @param rel Relative threshold.
     * @param abs Absolute threshold.
     * @throws IllegalArgumentException if {@code abs <= 0}; or if {@code rel < 2 * Math.ulp(1.0)}
     */
    BrentOptimizer(double rel, double abs) {
        if (rel >= MIN_RELATIVE_TOLERANCE) {
            relativeThreshold = rel;
            absoluteThreshold = Arguments.checkStrictlyPositive(abs);
        } else {
            // relative too small, or NaN
            throw new InferenceException(InferenceException.X_LT_Y, rel, MIN_RELATIVE_TOLERANCE);
        }
    }

    /**
     * Gets the number of function evaluations from the most recent call to
     * {@link #optimize(DoubleUnaryOperator, double, double, double, double) optimize}.
     *
     * @return the function evaluations
     */
    int getEvaluations() {
        return evaluations;
    }

    /**
     * Search for the minimum inside the provided interval. The bracket must satisfy
     * the equalities {@code lo < mid < hi} or {@code hi < mid < lo}.
     *
     * <p>Note: This function accepts the initial guess and the function value at that point.
     * This is done for convenience as this internal class is used where the caller already
     * knows the function value.
     *
     * @param func Function to solve.
     * @param lo Lower bound of the search interval.
     * @param hi Higher bound of the search interval.
     * @param mid Start point.
     * @param fMid Function value at the start point.
     * @return the value where the function is minimum.
     * @throws IllegalArgumentException if start point is not within the search interval
     * @throws IllegalStateException if the maximum number of iterations is exceeded
     */
    PointValuePair optimize(DoubleUnaryOperator func,
                            double lo, double hi,
                            double mid, double fMid) {
        double a;
        double b;
        if (lo < hi) {
            a = lo;
            b = hi;
        } else {
            a = hi;
            b = lo;
        }
        if (!(a < mid && mid < b)) {
            throw new InferenceException("Invalid bounds: (%s, %s) with start %s", a, b, mid);
        }
        double x = mid;
        double v = x;
        double w = x;
        double d = 0;
        double e = 0;
        double fx = fMid;
        double fv = fx;
        double fw = fx;

        // Best point encountered so far (which is the initial guess).
        double bestX = x;
        double bestFx = fx;

        // No test for iteration count.
        // Note that the termination criterion is based purely on the size of the current
        // bracket and the current point x. If the function evaluates NaN then golden
        // section steps are taken.
        evaluations = 0;
        for (;;) {
            final double m = 0.5 * (a + b);
            final double tol1 = relativeThreshold * Math.abs(x) + absoluteThreshold;
            final double tol2 = 2 * tol1;

            // Default termination (Brent's criterion).
            if (Math.abs(x - m) <= tol2 - 0.5 * (b - a)) {
                return PointValuePair.of(bestX, bestFx);
            }

            if (Math.abs(e) > tol1) {
                // Fit parabola.
                double r = (x - w) * (fx - fv);
                double q = (x - v) * (fx - fw);
                double p = (x - v) * q - (x - w) * r;
                q = 2 * (q - r);

                if (q > 0) {
                    p = -p;
                } else {
                    q = -q;
                }

                r = e;
                e = d;

                if (p > q * (a - x) &&
                    p < q * (b - x) &&
                    Math.abs(p) < Math.abs(0.5 * q * r)) {
                    // Parabolic interpolation step.
                    d = p / q;
                    final double u = x + d;

                    // f must not be evaluated too close to a or b.
                    if (u - a < tol2 || b - u < tol2) {
                        if (x <= m) {
                            d = tol1;
                        } else {
                            d = -tol1;
                        }
                    }
                } else {
                    // Golden section step.
                    if (x < m) {
                        e = b - x;
                    } else {
                        e = a - x;
                    }
                    d = GOLDEN_SECTION * e;
                }
            } else {
                // Golden section step.
                if (x < m) {
                    e = b - x;
                } else {
                    e = a - x;
                }
                d = GOLDEN_SECTION * e;
            }

            // Update by at least "tol1".
            // Here d is never NaN so the evaluation point u is always finite.
            double u;
            if (Math.abs(d) < tol1) {
                if (d >= 0) {
                    u = x + tol1;
                } else {
                    u = x - tol1;
                }
            } else {
                u = x + d;
            }

            evaluations++;
            final double fu = func.applyAsDouble(u);

            // Maintain the best encountered result
            if (fu < bestFx) {
                bestX = u;
                bestFx = fu;
            }

            // Note:
            // Here the use of a convergence checker on f(x) previous vs current has been removed.
            // Typically when the checker requires a very small relative difference
            // the optimizer will stop before, or soon after, on Brent's criterion when that is
            // configured with the smallest recommended convergence criteria.

            // Update a, b, v, w and x.
            if (fu <= fx) {
                if (u < x) {
                    b = x;
                } else {
                    a = x;
                }
                v = w;
                fv = fw;
                w = x;
                fw = fx;
                x = u;
                fx = fu;
            } else {
                if (u < x) {
                    a = u;
                } else {
                    b = u;
                }
                if (fu <= fw ||
                    Precision.equals(w, x)) {
                    v = w;
                    fv = fw;
                    w = u;
                    fw = fu;
                } else if (fu <= fv ||
                           Precision.equals(v, x) ||
                           Precision.equals(v, w)) {
                    v = u;
                    fv = fu;
                }
            }
        }
    }
}
