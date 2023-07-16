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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Helper class for tests in {@code o.a.c.s.descriptive} module.
 */
final class TestHelper {

    /** Class contains only static methods. */
    private TestHelper() {}

    /**
     * Creates a RNG instance.
     *
     * @return A new RNG instance.
     */
    static UniformRandomProvider createRNG() {
        return RandomSource.SPLIT_MIX_64.create();
    }

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Uses Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * <p>This can be removed when {@code commons-rng-sampling 1.6} is released.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static double[] shuffle(UniformRandomProvider rng, double[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
}
