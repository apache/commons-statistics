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
package org.apache.commons.statistics.descriptive;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Utility class which provides the data for tests in {o.a.c.s.descriptive} module.
 */
final class TestData {

    /** Class contains only static methods. */
    private TestData() {}

    /**
     * A stream of test data for the moments. This is provided so all moment
     * statistics based on the mean (first raw moment) can use the same cases.
     *
     * @return the stream of test data
     */
    static Stream<double[]> momentTestData() {
        final double max = Double.MAX_VALUE;
        return Stream.of(
            // Large numbers
            new double[] {10E-50, 5E-100, 25E-200, 35.345E-50},
            // Small numbers
            new double[] {0.001, 0.0002, 0.00003, 10000.11, 0.000004},
            // Overflow of the sum which prevents using the sum for the mean
            new double[] {max, max},
            new double[] {-max, -max},
            new double[] {max, max, max, max},
            new double[] {max, max / 2},
            new double[] {max, max, -max},
            new double[] {-max, -max / 2, -max / 4},
            // Extreme range
            new double[] {-max, 1, 1},
            // zeros
            new double[10]
        );
    }


    /**
     * Stream the arguments to test creation methods with an invalid array range.
     * Arguments are [from, to, length].
     *
     * @return the stream
     */
    static Stream<Arguments> arrayRangeTestData() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // fromIndex < 0
        builder.add(Arguments.of(-1, 10, 10));
        builder.add(Arguments.of(Integer.MIN_VALUE, 10, 10));
        // fromIndex > toIndex
        builder.add(Arguments.of(2, 1, 10));
        builder.add(Arguments.of(20, 10, 10));
        builder.add(Arguments.of(0, -1, 10));
        // toIndex > length
        builder.add(Arguments.of(0, 11, 10));
        builder.add(Arguments.of(0, 1, 0));
        return builder.build();
    }
}
