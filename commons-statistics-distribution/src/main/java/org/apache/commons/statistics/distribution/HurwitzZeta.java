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
package org.apache.commons.statistics.distribution;

/**
 * Utility class used to compute the
 * <a href="https://en.wikipedia.org/wiki/Hurwitz_zeta_function">Hurwitz zeta</a> function.
 *
 * <pre>
 *                 oo    1
 * zeta(s, a) = sum    ------
 *                 k=0      s
 *                     (k+a)
 * </pre>
 *
 * <p>The function is formally defined for complex variable {@code s} with {@code Re(s) > 1}
 * and real {@code a != 0, -1, -2, ...}. This series is absolutely convergent for the given
 * values of {@code s} and {@code a}. Note the special case zeta(s, 1) is the Riemann zeta function.
 *
 * <p>This implementation uses real-valued {@code s > 1} and {@code a >= 1}.
 * Specialisation to a smaller domain than any finite {@code a} allows optimisation for
 * a {@code double} precision result.
 *
 * <p>The implementation is performed by spitting the integral into two parts and using
 * the Euler-Maclaurin formula to approximate the second integral {@code I + T + R}
 * with a continuous integral {@code I}, a tail {@code T}, and a residual error term
 * {@code R} (not computed).
 *
 * <pre>
 *                 N-1           oo
 * zeta(s, a) = sum    f(k) + sum    f(k) = S + I + T + R
 *                 k=0           k=N
 *
 *          1
 * f(k) = ------
 *             s
 *        (a+k)
 *
 *                            1-s
 *      ,-oo   1         (a+N)
 * I =  |    ------ dt = --------
 *     -' N       s        s-1
 *           (a+t)
 *
 *            /             B     (s)      \
 *       1    | 1      M     2k      2k-1  |
 * T = ------ | - + sum    ----- --------- |
 *          s | 2      k=1 (2k)!      2k-1 |
 *     (a+N)  \                  (a+N)     /
 *
 * B   = Bernoulli number
 *  2k
 *
 *           ___n-1
 * (s)     = | |    (x+i)    (rising factorial Pochhammer function)
 *    n      | |i=0
 * </pre>
 *
 * <p>These formulas for the real-valued {@code s} are provided in Johansson (2015) as
 * equations 5-9. The implementation omits the residual term {@code R}.
 *
 * <p>References
 * <ol>
 * <li>Johansson (2015)
 * Rigorous high-precision computation of the Hurwitz zeta function and its derivatives
 * <a href="https://link.springer.com/article/10.1007/s11075-014-9893-1">Numerical Algorithms (69) 253–270</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Hurwitz_zeta_function">Hurwitz zeta function (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Riemann_zeta_function">Riemann zeta function (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Euler%E2%80%93Maclaurin_formula">Euler–Maclaurin formula (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Bernoulli_number">Bernoulli number (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Falling_and_rising_factorials">Rising and falling factorials (Wikipedia)</a></li>
 * </ol>
 *
 * @since 1.4
 */
final class HurwitzZeta {
    /** Number of terms of the series summation S. */
    private static final int N = 8;
    /** Convergence epsilon for the sum of the tail function. This prevents summation
     * of terms that do not affect the final result. */
    private static final double EPS = 0x1.0p-53;

    /**
     * Precomputed factors for {@code k}-th element of the tail function {@code T}.
     * Uses {@code 2k!} divided by Bernoulli number {@code B_2k}.
     * Provides M=14 terms. The test suite uses max 9 before convergence.
     */
    private static final double[] F = {
        12.0, // 2! / (1 / 6)
        -720.0, // 4! / (-1 / 30)
        30240.0, // 6! / (1 / 42)
        -1209600.0, // 8! / (-1 / 30)
        4.790016E7, // 10! / (5 / 66)
        -1.8924375803183792E9, // 12! / (-691 / 2730)
        7.47242496E10, // 14! / (7 / 6)
        -2.950130727918164E12, // 16! / (-3617 / 510)
        1.1646782814350067E14, // 18! / (43867 / 798)
        -4.597978722407473E15, // 20! / (-174611 / 330)
        1.81521054019435456E17, // 22! / (854513 / 138)
        -7.1661652561756672E18, // 24! / (-236364091 / 2730)
        2.82908877253043E20, // 26! / (8553103 / 6)
        -1.1168794925000445E22, // 28! / (-23749461029 / 870)
    };

    /** No instances. */
    private HurwitzZeta() {}

    /**
     * Compute the value of the Hurwitz zeta function {@code zeta(s, a)}.
     *
     * <pre>
     *                 oo    1
     * zeta(s, a) = sum    ------
     *                 k=0      s
     *                     (k+a)
     * </pre>
     *
     * <p><strong>Warning</strong>: No parameter validation is performed.
     * The domain of {@code a} is expected to be a positive integer {@code [1, 2^31)}.
     *
     * @param s Argument {@code s > 1}
     * @param a Argument {@code a >= 1}
     * @return zeta(s, a)
     */
    static double value(double s, double a) {
        final double apn = a + N;
        double p = Math.pow(apn, -s);

        // Initialise sum with the first tail term
        double sum = 0.5 * p;
        // S : k in [0, n-1]
        for (int k = N - 1; k >= 0; k--) {
            // Descending k sums in order of magnitude for increased precision.
            // Prevents early exit for large s when the term (a+k)^-s is below
            // machine epsilon of the ascending series sum.
            sum += Math.pow(a + k, -s);
        }

        // I
        sum += Math.pow(apn, 1 - s) / (s - 1);

        // T
        // The following recycles the power term p: (a+n)^-(2k-1+s).
        // This incorporates the factor for T, (a+n)^-s, into the sum terms.
        // The first power is (a+n)^-(1+s) not (a+n)^-1.
        // When s is large the loop exits before the rising factorial overflows.

        // Rising factorial term : (s)_{2k-1}
        double f = s;
        // 2k - 1
        double k2 = 1;
        // Sum of an alternating series as each F changes sign.
        // Sum until terms will not impact the result.
        double tsum = 0;
        final double stop = sum * EPS;
        int i;
        for (i = 0; i < F.length; i++) {
            // p = (a+n)^-(2k-1+s)
            p /= apn;
            final double t = f * p / F[i];
            tsum += t;
            if (Math.abs(t) <= stop) {
                break;
            }
            p /= apn;
            // f = s * (s+1) * (s+2) * ... * (s+2k-2)
            f *= s + k2;
            k2 += 1.0;
            f *= s + k2;
            k2 += 1.0;
        }
        return sum + tsum;
    }
}
