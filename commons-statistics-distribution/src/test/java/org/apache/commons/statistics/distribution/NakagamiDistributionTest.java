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
 * Test cases for {@link NakagamiDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class NakagamiDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mu = (Double) parameters[0];
        final double omega = (Double) parameters[1];
        return new NakagamiDistribution(mu, omega);
    }

    @Override
    protected double getAbsoluteTolerance() {
        return 1e-9;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 1.0},
            {-0.1, 1.0},
            {0.5, 0.0},
            {0.5, -0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"Shape", "Scale"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testExtremeLogDensity() {
        // XXX: Verify with more test data from a reference distribution
        final NakagamiDistribution dist = new NakagamiDistribution(0.5, 1);
        final double x = 50;
        Assertions.assertEquals(0.0, dist.density(x));
        Assertions.assertEquals(-1250.22579, dist.logDensity(x), 1e-4);
    }
}
