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
package org.apache.commons.statistics.interval;

import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link NormalConfidenceInterval}.
 */
class NormalConfidenceIntervalTest {
    @ParameterizedTest
    @EnumSource
    void testInvalidArgumentsThrow(NormalConfidenceInterval method) {
        double mean = 0.1;
        double variance = 1.23;
        int n = 42;
        double alpha = 0.05;
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(mean, variance, n, alpha));
        // n < 2
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(mean, variance, 2, alpha));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, 1, alpha));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, 0, alpha));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, -1, alpha));
        // alpha not in (0, 1)
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(mean, variance, n, Math.nextUp(0.0)));
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(mean, variance, n, Math.nextDown(1.0)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, n, 0.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, n, 1.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, n, -0.01));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, n, 1.01));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(mean, variance, n, Double.NaN));
    }

    @ParameterizedTest
    @MethodSource()
    void testInterval(NormalConfidenceInterval method, double mean, double variance, int n, double alpha,
            double lower, double upper, double relativeError) {
        final Interval i = method.fromErrorRate(mean, variance, n, alpha);
        Assertions.assertEquals(lower, i.getLowerBound(), lower * relativeError, "lower");
        Assertions.assertEquals(upper, i.getUpperBound(), upper * relativeError, "upper");
    }

    static Stream<Arguments> testInterval() {
        final Builder<Arguments> builder = Stream.builder();

        // mean cases generated using R 4.4.3, e.g.
        // options(digits=17)
        // x -> c(1, 2, 3, 4)
        // mean(x); var(x); length(x)
        // t.test(x, conf.level=0.95)$conf.int
        // Data generated in r using random numbers, e.g.
        // x = runif(100); x = rnorm(50, mean=3, sd=2)
        NormalConfidenceInterval method;
        method = NormalConfidenceInterval.MEAN;
        add(builder, method, 2.5, 1.6666666666666667, 4, 0.05, 0.44573974323947924, 4.55426025676052060, 1e-14);
        add(builder, method, 0.5263914421340451, 0.079384303412544904, 100, 0.05, 0.47048569257011025,
            0.58229719169798000, 1e-14);
        add(builder, method, 2.9535381946732131, 5.2628380291790835, 50, 0.1, 2.4096097871064539, 3.4974666022399732,
            1e-14);

        // variance cases manually computed in R 4.4.3 (data x as above) e.g.
        // alpha=0.05; n=length(x); v=var(x); (n-1)*v/qchisq(alpha/2, n-1, lower.tail=F); (n-1)*v/qchisq(alpha/2, n-1)
        method = NormalConfidenceInterval.VARIANCE;
        add(builder, method, 2.5, 1.6666666666666667, 4, 0.05, 0.53485067734936409, 23.170107980137484, 1e-14);
        add(builder, method, 0.5263914421340451, 0.079384303412544904, 100, 0.05, 0.061197043596933121,
            0.10712827588348012, 1e-14);
        add(builder, method, 2.9535381946732131, 5.2628380291790835, 50, 0.1, 3.887312567406342, 7.6002576083181186,
            1e-14);

        // Approximate formula for asymptotic distributions at large n uses z critical value
        // from a normal distribution, here z_{0.025} = 1.96
        final double z = 1.96;
        final double mean = 1.23;
        final double variance = 3.45;
        final int n = 100_000;
        double dist = z * Math.sqrt(variance / n);
        add(builder, NormalConfidenceInterval.MEAN, mean, variance, n, 0.05, mean - dist, mean + dist, 1e-6);
        dist = z * Math.sqrt(2.0 / n) * variance;
        add(builder, NormalConfidenceInterval.VARIANCE, mean, variance, n, 0.05, variance - dist, variance + dist,
            1e-4);

        return builder.build();
    }

    private static void add(Builder<Arguments> builder, NormalConfidenceInterval method,
            double mean, double variance, int n, double alpha,
            double lower, double upper, double relativeError) {
        builder.accept(Arguments.of(method, mean, variance, n, alpha, lower, upper, relativeError));
    }
}
