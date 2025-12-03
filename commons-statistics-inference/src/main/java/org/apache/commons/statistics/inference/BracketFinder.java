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

import java.util.function.DoubleUnaryOperator;

/**
 * Provide an interval that brackets a local minimum of a function.
 * This code is based on a Python implementation (from <em>SciPy</em>,
 * module {@code optimize.py} v0.5).
 *
 * <p>This class has been extracted from {@code o.a.c.math4.optim.univariate}
 * and modified to: remove support for bracketing a maximum; support bounds
 * on the bracket; correct the sign of the denominator when the magnitude is small;
 * and return true/false if there is a minimum strictly inside the bounds.
 *
 * @since 1.1
 */
class BracketFinder {
    /** Tolerance to avoid division by zero. */
    private static final double EPS_MIN = 1e-21;
    /** Golden section. */
    private static final double GOLD = 1.6180339887498948482;
    /** Factor for expanding the interval. */
    private final double growLimit;
    /**  Number of allowed function evaluations. */
    private final int maxEvaluations;
    /** Number of function evaluations performed in the last search. */
    private int evaluations;
    /** Lower bound of the bracket. */
    private double lo;
    /** Higher bound of the bracket. */
    private double hi;
    /** Point inside the bracket. */
    private double mid;
    /** Function value at {@link #lo}. */
    private double fLo;
    /** Function value at {@link #hi}. */
    private double fHi;
    /** Function value at {@link #mid}. */
    private double fMid;

    /**
     * Constructor with default values {@code 100, 100000} (see the
     * {@link #BracketFinder(double,int) other constructor}).
     */
    BracketFinder() {
        this(100, 100000);
    }

    /**
     * Create a bracketing interval finder.
     *
     * @param growLimit Expanding factor.
     * @param maxEvaluations Maximum number of evaluations allowed for finding
     * a bracketing interval.
     * @throws IllegalArgumentException if the {@code growLimit} or {@code maxEvalutations}
     * are not strictly positive.
     */
    BracketFinder(double growLimit, int maxEvaluations) {
        Arguments.checkStrictlyPositive(growLimit);
        Arguments.checkStrictlyPositive(maxEvaluations);
        this.growLimit = growLimit;
        this.maxEvaluations = maxEvaluations;
    }

    /**
     * Search downhill from the initial points to obtain new points that bracket a local
     * minimum of the function. Note that the initial points do not have to bracket a minimum.
     * An exception is raised if a minimum cannot be found within the configured number
     * of function evaluations.
     *
     * <p>The bracket is limited to the provided bounds if they create a positive interval
     * {@code min < max}. It is possible that the middle of the bracket is at the bounds as
     * the final bracket is {@code f(mid) <= min(f(lo), f(hi))} and {@code lo <= mid <= hi}.
     *
     * <p>No exception is raised if the initial points are not within the bounds; the points
     * are updated to be within the bounds.
     *
     * <p>No exception is raised if the initial points are equal; the bracket will be returned
     * as a single point {@code lo == mid == hi}.
     *
     * @param func Function whose optimum should be bracketed.
     * @param a Initial point.
     * @param b Initial point.
     * @param min Minimum bound of the bracket (inclusive).
     * @param max Maximum bound of the bracket (inclusive).
     * @return true if the mid-point is strictly within the final bracket {@code [lo, hi]};
     * false if there is no local minima.
     * @throws IllegalStateException if the maximum number of evaluations is exceeded.
     */
    boolean search(DoubleUnaryOperator func,
                   double a, double b,
                   double min, double max) {
        evaluations = 0;

        // Limit the range of x
        final DoubleUnaryOperator range;
        if (min < max) {
            // Limit: min <= x <= max
            range = x -> {
                if (x > min) {
                    return x < max ? x : max;
                }
                return min;
            };
        } else {
            range = DoubleUnaryOperator.identity();
        }

        double xA = range.applyAsDouble(a);
        double xB = range.applyAsDouble(b);
        double fA = value(func, xA);
        double fB = value(func, xB);
        // Ensure fB <= fA
        if (fA < fB) {
            double tmp = xA;
            xA = xB;
            xB = tmp;
            tmp = fA;
            fA = fB;
            fB = tmp;
        }

        double xC = range.applyAsDouble(xB + GOLD * (xB - xA));
        double fC = value(func, xC);

        // Note: When a [min, max] interval is provided and there is no minima then this
        // loop will terminate when B == C and both are at the min/max bound.
        while (fC < fB) {
            final double tmp1 = (xB - xA) * (fB - fC);
            final double tmp2 = (xB - xC) * (fB - fA);

            final double val = tmp2 - tmp1;
            // limit magnitude of val to a small value
            final double denom = 2 * Math.copySign(Math.max(Math.abs(val), EPS_MIN), val);

            double w = range.applyAsDouble(xB - ((xB - xC) * tmp2 - (xB - xA) * tmp1) / denom);
            final double wLim = range.applyAsDouble(xB + growLimit * (xC - xB));

            double fW;
            if ((w - xC) * (xB - w) > 0) {
                // xB < w < xC
                fW = value(func, w);
                if (fW < fC) {
                    // minimum in [xB, xC]
                    xA = xB;
                    xB = w;
                    fA = fB;
                    fB = fW;
                    break;
                } else if (fW > fB) {
                    // minimum in [xA, w]
                    xC = w;
                    fC = fW;
                    break;
                }
                // continue downhill
                w = range.applyAsDouble(xC + GOLD * (xC - xB));
                fW = value(func, w);
            } else if ((w - wLim) * (xC - w) > 0) {
                // xC < w < limit
                fW = value(func, w);
                if (fW < fC) {
                    // continue downhill
                    xB = xC;
                    xC = w;
                    w = range.applyAsDouble(xC + GOLD * (xC - xB));
                    fB = fC;
                    fC = fW;
                    fW = value(func, w);
                }
            } else if ((w - wLim) * (wLim - xC) >= 0) {
                // xC <= limit <= w
                w = wLim;
                fW = value(func, w);
            } else {
                // possibly w == xC; reject w and take a default step
                w = range.applyAsDouble(xC + GOLD * (xC - xB));
                fW = value(func, w);
            }

            xA = xB;
            fA = fB;
            xB = xC;
            fB = fC;
            xC = w;
            fC = fW;
        }

        mid = xB;
        fMid = fB;

        // Store the bracket: lo <= mid <= hi
        if (xC < xA) {
            lo = xC;
            fLo = fC;
            hi = xA;
            fHi = fA;
        } else {
            lo = xA;
            fLo = fA;
            hi = xC;
            fHi = fC;
        }

        return lo < mid && mid < hi;
    }

    /**
     * @return the number of evaluations.
     */
    int getEvaluations() {
        return evaluations;
    }

    /**
     * @return the lower bound of the bracket.
     * @see #getFLo()
     */
    double getLo() {
        return lo;
    }

    /**
     * Get function value at {@link #getLo()}.
     * @return function value at {@link #getLo()}
     */
    double getFLo() {
        return fLo;
    }

    /**
     * @return the higher bound of the bracket.
     * @see #getFHi()
     */
    double getHi() {
        return hi;
    }

    /**
     * Get function value at {@link #getHi()}.
     * @return function value at {@link #getHi()}
     */
    double getFHi() {
        return fHi;
    }

    /**
     * @return a point in the middle of the bracket.
     * @see #getFMid()
     */
    double getMid() {
        return mid;
    }

    /**
     * Get function value at {@link #getMid()}.
     * @return function value at {@link #getMid()}
     */
    double getFMid() {
        return fMid;
    }

    /**
     * Get the value of the function.
     *
     * @param func Function.
     * @param x Point.
     * @return the value
     * @throws IllegalStateException if the maximal number of evaluations is exceeded.
     */
    private double value(DoubleUnaryOperator func, double x) {
        if (evaluations >= maxEvaluations) {
            throw new IllegalStateException("Too many evaluations: " + evaluations);
        }
        evaluations++;
        return func.applyAsDouble(x);
    }
}
