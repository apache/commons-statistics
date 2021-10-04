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
 * Utilities for argument validation.
 */
final class ArgumentUtils {
    /** No instances. */
    private ArgumentUtils() {}

    /**
     * Checks if the value {@code x} is finite and strictly positive.
     *
     * @param x Value
     * @return true if {@code x > 0} and is finite
     */
    static boolean isFiniteStrictlyPositive(double x) {
        return x > 0 && x < Double.POSITIVE_INFINITY;
    }

    /**
     * Check the probability {@code p} is in the interval {@code [0, 1]}.
     *
     * @param p Probability
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
     */
    static void checkProbability(double p) {
        if (p >= 0 && p <= 1) {
            return;
        }
        // Out-of-range or NaN
        throw new DistributionException(DistributionException.INVALID_PROBABILITY, p);
    }
}
