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

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Constants}.
 */
class ConstantsTest {
    @ParameterizedTest
    @MethodSource
    void testConstant(double expected, double x, int ulp) {
        TestUtils.assertEquals(expected, x, DoubleTolerances.ulps(ulp));
    }

    static Stream<Arguments> testConstant() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(Math.sqrt(2), Constants.ROOT_TWO, 0));
        builder.add(Arguments.of(Math.sqrt(2 / Math.PI), Constants.ROOT_TWO_DIV_PI, 0));
        builder.add(Arguments.of(Math.sqrt(Math.PI / 2), Constants.ROOT_PI_DIV_TWO, 1));
        builder.add(Arguments.of(Math.log(2), Constants.LN_TWO, 0));
        builder.add(Arguments.of(0.5 * Math.log(2 * Math.PI), Constants.HALF_LOG_TWO_PI, 1));
        return builder.build();
    }
}
