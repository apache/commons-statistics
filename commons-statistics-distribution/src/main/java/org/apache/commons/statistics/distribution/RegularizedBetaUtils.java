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

import org.apache.commons.numbers.gamma.RegularizedBeta;

/**
 * Utilities for the <a href="http://mathworld.wolfram.com/RegularizedBetaFunction.html">
 * Regularized Beta function</a> {@code I(x, a, b)}.
 */
final class RegularizedBetaUtils {
    /** No instances. */
    private RegularizedBetaUtils() {}

    /**
     * Compute the complement of the regularized beta function {@code I(x, a, b)}.
     * <pre>
     * 1 - I(x, a, b) = I(1 - x, b, a)
     * </pre>
     *
     * @param x the value.
     * @param a Parameter {@code a}.
     * @param b Parameter {@code b}.
     * @return the complement of the regularized beta function 1 - I(x, a, b).
     */
    static double complement(double x, double a, double b) {
        // Identity of the regularized beta function: 1 - I_x(a, b) = I_{1-x}(b, a)
        // Ideally call RegularizedBeta.value(1 - x, b, a) to maximise precision.
        //
        // The implementation of the beta function will use the complement based on a condition.
        // Here we repeat the condition with a and b switched and testing 1 - x.
        // This will avoid double inversion of the parameters.
        final double mxp1 = 1 - x;
        if (mxp1 > (b + 1) / (2 + b + a)) {
            // Note: This drops the addition test '&& x <= (a + 1) / (2 + b + a)'
            // The test is to avoid infinite method call recursion which does not apply
            // in this case. See MATH-1067.

            // Direct computation of the complement with the input x.
            // Avoids loss of precision when x != 1 - (1-x)
            return 1.0 - RegularizedBeta.value(x, a, b);
        }
        // Use the identity which should be computed directly by the RegularizedBeta implementation.
        return RegularizedBeta.value(mxp1, b, a);
    }
}
