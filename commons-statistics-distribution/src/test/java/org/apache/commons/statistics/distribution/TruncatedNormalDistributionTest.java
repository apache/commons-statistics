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
 * Test class for {@link TruncatedNormalDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 * All test values were computed using Python with SciPy v1.6.0.
 */
class TruncatedNormalDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double mean = (Double) parameters[0];
        final double sd = (Double) parameters[1];
        final double upper = (Double) parameters[2];
        final double lower = (Double) parameters[3];
        return new TruncatedNormalDistribution(mean, sd, upper, lower);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 0.0, -1.0, 1.0},
            {0.0, -0.1, -1.0, 1.0},
            {0.0, 1.0, 1.0, -1.0},
        };
    }

    @Override
    String[] getParameterNames() {
        // Input mean and standard deviation refer to the underlying normal distribution.
        // The constructor arguments do not match the mean and SD of the truncated distribution.
        return new String[] {null, null, "SupportLowerBound", "SupportUpperBound"};
    }
}
