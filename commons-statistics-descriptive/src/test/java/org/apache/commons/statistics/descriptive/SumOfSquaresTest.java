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

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;

/**
 * Test for {@link SumOfSquares}.
 */
final class SumOfSquaresTest extends BaseDoubleStatisticTest<SumOfSquares> {

    @Override
    protected SumOfSquares create() {
        return SumOfSquares.create();
    }

    @Override
    protected SumOfSquares create(double... values) {
        return SumOfSquares.of(values);
    }

    @Override
    protected SumOfSquares create(double[] values, int from, int to) {
        return SumOfSquares.ofRange(values, from, to);
    }

    @Override
    protected double getEmptyValue() {
        return 0;
    }

    @Override
    protected double getExpectedValue(double[] values) {
        // The sum is not high precision.
        // Use the extended precision result from the stream sum as the reference.
        return Arrays.stream(values)
                     .map(x -> x * x)
                     .sum();
    }

    @Override
    protected double getExpectedNonFiniteValue(double[] values) {
        return getExpectedValue(values);
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.ulps(8);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        // Python Numpy v1.25.1: numpy.dot(x, x)
        builder.accept(addReference(30.0, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(1422.0, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        // numpy.set_printoptions(precision=17)
        // x = numpy.random.rand(10)
        builder.accept(addReference(4.725085909331556, DoubleTolerances.ulps(5),
            0.8824732476946039, 0.5101077911923941, 0.002185806195693085,
            0.755680536591656, 0.9065277163160296, 0.006879254422025083,
            0.3942119161179829, 0.7421088683881211, 0.92742739469409,
            0.8526248074611704));
        // Matlab v2023a: sumsqr(x)
        builder.accept(addReference(2885.0625, DoubleTolerances.ulps(3), 0.25, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 50));
        builder.accept(addReference(2.9999999999999997e-05, DoubleTolerances.ulps(3), 0.001, 0.002, 0.003, 0.004));
        return builder.build();
    }
}
