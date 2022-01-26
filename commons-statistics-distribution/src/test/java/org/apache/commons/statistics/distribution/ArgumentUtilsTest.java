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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ArgumentUtils}.
 */
class ArgumentUtilsTest {
    @Test
    void testIsFiniteStrictlyPositive() {
        final double[] good = {Double.MIN_VALUE, 1, 2, 3, Double.MAX_VALUE};
        final double[] bad = {0, -0.0, -Double.MIN_VALUE, -Double.MAX_VALUE,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (final double x : good) {
            Assertions.assertTrue(ArgumentUtils.isFiniteStrictlyPositive(x), () -> Double.toString(x));
        }
        for (final double x : bad) {
            Assertions.assertFalse(ArgumentUtils.isFiniteStrictlyPositive(x), () -> Double.toString(x));
        }
    }

    @Test
    void testCheckProbability() {
        // Currently this allows -0.0
        final double[] good = {-0.0, 0.0, Double.MIN_VALUE, 1 - 0x1.0p-53, 1};
        final double[] bad = {-Double.MIN_VALUE, -0.1, -1, -2, Math.nextUp(1.0), 1.1, -Double.MAX_VALUE,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (final double p : good) {
            Assertions.assertDoesNotThrow(() -> ArgumentUtils.checkProbability(p), () -> Double.toString(p));
        }
        for (final double p : bad) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> ArgumentUtils.checkProbability(p), () -> Double.toString(p));
        }
    }
}
