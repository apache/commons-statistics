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

/**
 * Utility class used by various distributions to accurately compute their
 * respective probability mass functions. The implementation for this class is
 * based on the Catherine Loader's
 * <a href="http://www.herine.net/stat/software/dbinom.html">dbinom</a> routines.
 *
 * This class is not intended to be called directly.
 */
final class SaddlePointExpansionUtils {
    /** 2 &pi;. */
    private static final double TWO_PI = 2 * Math.PI;
    /** 1/10. */
    private static final double ONE_TENTH = 0.1;
    /** The threshold value for switching the method to compute th Stirling error. */
    private static final int STIRLING_ERROR_THRESHOLD = 15;

    /** Exact Stirling expansion error for certain values. */
    private static final double[] EXACT_STIRLING_ERRORS = {
        0.0, /* 0.0 */
        0.1534264097200273452913848, /* 0.5 */
        0.0810614667953272582196702, /* 1.0 */
        0.0548141210519176538961390, /* 1.5 */
        0.0413406959554092940938221, /* 2.0 */
        0.03316287351993628748511048, /* 2.5 */
        0.02767792568499833914878929, /* 3.0 */
        0.02374616365629749597132920, /* 3.5 */
        0.02079067210376509311152277, /* 4.0 */
        0.01848845053267318523077934, /* 4.5 */
        0.01664469118982119216319487, /* 5.0 */
        0.01513497322191737887351255, /* 5.5 */
        0.01387612882307074799874573, /* 6.0 */
        0.01281046524292022692424986, /* 6.5 */
        0.01189670994589177009505572, /* 7.0 */
        0.01110455975820691732662991, /* 7.5 */
        0.010411265261972096497478567, /* 8.0 */
        0.009799416126158803298389475, /* 8.5 */
        0.009255462182712732917728637, /* 9.0 */
        0.008768700134139385462952823, /* 9.5 */
        0.008330563433362871256469318, /* 10.0 */
        0.007934114564314020547248100, /* 10.5 */
        0.007573675487951840794972024, /* 11.0 */
        0.007244554301320383179543912, /* 11.5 */
        0.006942840107209529865664152, /* 12.0 */
        0.006665247032707682442354394, /* 12.5 */
        0.006408994188004207068439631, /* 13.0 */
        0.006171712263039457647532867, /* 13.5 */
        0.005951370112758847735624416, /* 14.0 */
        0.005746216513010115682023589, /* 14.5 */
        0.005554733551962801371038690 /* 15.0 */
    };

    /**
     * Forbid construction.
     */
    private SaddlePointExpansionUtils() {}

    /**
     * Compute the error of Stirling's series at the given value.
     * <p>
     * References:
     * <ol>
     * <li>Eric W. Weisstein. "Stirling's Series." From MathWorld--A Wolfram Web
     * Resource. <a target="_blank"
     * href="https://mathworld.wolfram.com/StirlingsSeries.html">
     * https://mathworld.wolfram.com/StirlingsSeries.html</a></li>
     * </ol>
     * </p>
     *
     * <p>Note: This function has been modified for integer {@code z}.</p>
     *
     * @param z the value.
     * @return the Striling's series error.
     */
    static double getStirlingError(int z) {
        double ret;
        if (z <= STIRLING_ERROR_THRESHOLD) {
            ret = EXACT_STIRLING_ERRORS[2 * z];
        } else {
            final double z2 = (double) z * z;
            ret = (0.083333333333333333333 -
                    (0.00277777777777777777778 -
                            (0.00079365079365079365079365 -
                                    (0.000595238095238095238095238 -
                                            0.0008417508417508417508417508 /
                                            z2) / z2) / z2) / z2) / z;
        }
        return ret;
    }

    /**
     * A part of the deviance portion of the saddle point approximation.
     * <p>
     * References:
     * <ol>
     * <li>Catherine Loader (2000). "Fast and Accurate Computation of Binomial
     * Probabilities.". <a target="_blank"
     * href="http://www.herine.net/stat/papers/dbinom.pdf">
     * http://www.herine.net/stat/papers/dbinom.pdf</a></li>
     * </ol>
     * </p>
     *
     * <p>Note: This function has been modified for integer {@code x}.</p>
     *
     * @param x the x value.
     * @param mu the average.
     * @return a part of the deviance.
     */
    static double getDeviancePart(int x, double mu) {
        double ret;
        if (Math.abs(x - mu) < 0.1 * (x + mu)) {
            final double d = x - mu;
            double v = d / (x + mu);
            double s1 = v * d;
            double s = Double.NaN;
            double ej = 2.0 * x * v;
            v *= v;
            int j = 1;
            while (s1 != s) {
                s = s1;
                ej *= v;
                s1 = s + ej / ((j * 2) + 1);
                ++j;
            }
            ret = s1;
        } else {
            if (x == 0) {
                return mu;
            }
            ret = x * Math.log(x / mu) + mu - x;
        }
        return ret;
    }

    /**
     * Compute the logarithm of the PMF for a binomial distribution
     * using the saddle point expansion.
     *
     * @param x the value at which the probability is evaluated.
     * @param n the number of trials.
     * @param p the probability of success.
     * @param q the probability of failure (1 - p).
     * @return log(p(x)).
     */
    static double logBinomialProbability(int x, int n, double p, double q) {
        double ret;
        if (x == 0) {
            if (p < ONE_TENTH) {
                ret = -getDeviancePart(n, n * q) - n * p;
            } else {
                if (n == 0) {
                    return 0;
                }
                ret = n * Math.log(q);
            }
        } else if (x == n) {
            if (q < ONE_TENTH) {
                ret = -getDeviancePart(n, n * p) - n * q;
            } else {
                ret = n * Math.log(p);
            }
        } else {
            final int nMx = n - x;
            ret = getStirlingError(n) - getStirlingError(x) -
                  getStirlingError(nMx) - getDeviancePart(x, n * p) -
                  getDeviancePart(nMx, n * q);
            final double f = (TWO_PI * x * nMx) / n;
            ret = -0.5 * Math.log(f) + ret;
        }
        return ret;
    }
}
