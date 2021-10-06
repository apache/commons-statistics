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
 * Test cases for {@link BinomialDistribution}.
 * Extends {@link BaseDiscreteDistributionTest}. See javadoc of that class for details.
 */
class BinomialDistributionTest extends BaseDiscreteDistributionTest {
    @Override
    DiscreteDistribution makeDistribution(Object... parameters) {
        final int n = (Integer) parameters[0];
        final double p = (Double) parameters[1];
        return new BinomialDistribution(n, p);
    }


    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {-1, 0.1},
            {10, -0.1},
            {10, 1.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"NumberOfTrials", "ProbabilityOfSuccess"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testMath718() {
        // For large trials the evaluation of ContinuedFraction was inaccurate.
        // Do a sweep over several large trials to test if the current implementation is
        // numerically stable.

        for (int trials = 500000; trials < 20000000; trials += 100000) {
            final BinomialDistribution dist = new BinomialDistribution(trials, 0.5);
            final int p = dist.inverseCumulativeProbability(0.5);
            Assertions.assertEquals(trials / 2, p);
        }
    }
}
