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
 * Test cases for {@link LogisticDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class LogisticDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double location = (Double) parameters[0];
        final double scale = (Double) parameters[1];
        return LogisticDistribution.of(location, scale);
    }

    @Override
    protected double getRelativeTolerance() {
        // Limited by the CDF inverse mapping
        return 1e-9;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0},
            {0.0, -0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Location", "Scale"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testExtremeDensity() {
        final LogisticDistribution dist = LogisticDistribution.of(0, 1.0);
        // Direct density (with scale = 1):
        // exp(-x) / (1 + exp(-x))^2
        // As x -> large negative then exp(-x) will overflow and a simple
        // computation will be incorrect
        final double x0 = -710;
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Math.exp(-x0));
        Assertions.assertEquals(Double.NaN, Math.exp(-x0) / Math.pow(1 + Math.exp(-x0), 2.0));

        // Computed using scipy.stats logistic
        final double[] x = {710, 720, 730, 740, 750};
        final double[] values = {4.47628622567513e-309,
            2.03223080241836e-313, 9.22631526816382e-318,
            4.19955798965060e-322, 0.00000000000000e+000};
        for (int i = 0; i < x.length; i++) {
            final double d = dist.density(x[i]);
            Assertions.assertEquals(values[i], d, (values[i] - d) * 1e-16);
            // Test symmetry. These values of x will create overflow.
            Assertions.assertEquals(d, dist.density(-x[i]));
        }
    }

    /**
     * Test a value for log density when the density computation is zero.
     */
    @Test
    void testExtremeLogDensity() {
        final double scale = 2.5;
        final LogisticDistribution dist = LogisticDistribution.of(0, scale);
        // Direct density (with scale = s):
        // exp(-x / s) / (1 + exp(-x / s))^2
        final double x = 1e160;
        Assertions.assertEquals(0.0, dist.density(x));
        // Log computation
        final double expected = -x / scale - 2 * Math.log1p(Math.exp(-x / scale));
        Assertions.assertNotEquals(Double.NEGATIVE_INFINITY, expected, "Density is zero but log density should not be -infinity");
        Assertions.assertEquals(expected, dist.logDensity(x), Math.abs(expected) * 1e-15);
    }
}
