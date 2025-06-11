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
 * Test cases for {@link BinomialConfidenceInterval}.
 */
class BinomialConfidenceIntervalTest {
    @ParameterizedTest
    @EnumSource
    void testInvalidArgumentsThrow(BinomialConfidenceInterval method) {
        int n = 10;
        int x = 5;
        double alpha = 0.05;
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(n, x, alpha));
        // n <= 0
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(-1, x, alpha));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(0, x, alpha));
        // x < 0
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(n, 0, alpha));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(n, -1, alpha));
        // x > n
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(n, n, alpha));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(n, n + 1, alpha));
        // alpha not in (0, 1)
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(n, x, Math.nextUp(0.0)));
        Assertions.assertDoesNotThrow(() -> method.fromErrorRate(n, x, Math.nextDown(1.0)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(n, x, 0.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(n, x, 1.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(n, x, -0.01));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(n, x, 1.01));
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.fromErrorRate(n, x, Double.NaN));
    }

    @ParameterizedTest
    @MethodSource()
    void testInterval(BinomialConfidenceInterval method, int n, int x, double alpha,
            double lower, double upper, double relativeError) {
        final Interval i = method.fromErrorRate(n, x, alpha);
        Assertions.assertEquals(lower, i.getLowerBound(), lower * relativeError, "lower");
        Assertions.assertEquals(upper, i.getUpperBound(), upper * relativeError, "upper");
    }

    static Stream<Arguments> testInterval() {
        final Builder<Arguments> builder = Stream.builder();
        // Cases taken from Commons Math.
        // Results generated using Python statsmodels.stats.proportion.proportion_confint
        // with method parameter:
        // normal : asymptotic normal approximation
        // agresti_coull : Agresti-Coull interval
        // beta : Clopper-Pearson interval based on Beta distribution
        // wilson : Wilson Score interval
        // jeffreys : Jeffreys Bayesian Interval
        // E.g.
        // proportion_confint(0,10,method='beta') = (0, 0.3084971078187608)
        final int n = 500;
        final int x = 50;
        final double alpha = 0.1;
        add(builder, BinomialConfidenceInterval.NORMAL_APPROXIMATION, n, x, alpha, 0.07793197286259657, 0.12206802713740345, 1e-15);
        add(builder, BinomialConfidenceInterval.WILSON_SCORE, n, x, alpha, 0.0800391858824593, 0.12426638582141426, 1e-15);
        add(builder, BinomialConfidenceInterval.JEFFREYS, n, x, alpha, 0.07963646817350203, 0.1237728842019873, 1e-15);
        add(builder, BinomialConfidenceInterval.CLOPPER_PEARSON, n, x, alpha, 0.07873857004520295, 0.1248658074138089, 1e-15);
        add(builder, BinomialConfidenceInterval.AGRESTI_COULL, n, x, alpha, 0.07993520614825012, 0.12437036555562345, 1e-15);
        // Expand Commons Math Clopper-Pearson test to all methods.
        // Test MATH-1421: lower >= 0 when x=0
        BinomialConfidenceInterval method;
        method = BinomialConfidenceInterval.NORMAL_APPROXIMATION;
        add(builder, method, 10, 0, 0.05, 0, 0, 1e-15);
        add(builder, method, 10, 10, 0.05, 1, 1, 1e-15);
        add(builder, method, 10, 3, 0.05, 0.015974234910674567, 0.5840257650893255, 1e-15);
        add(builder, method, 400, 20, 0.05, 0.028641787646026474, 0.07135821235397354, 1e-15);
        add(builder, method, 19436, 0, 0.05, 0, 0, 1e-15);
        // Interval requires clipping to [0, 1]
        add(builder, method, 100, 1, 0.05, 0, 0.02950139541798788, 1e-15);
        add(builder, method, 100, 99, 0.05, 0.9704986045820121, 1, 1e-15);
        method = BinomialConfidenceInterval.WILSON_SCORE;
        add(builder, method, 10, 0, 0.05, 0.0, 0.27753279986288926, 1e-15);
        add(builder, method, 10, 10, 0.05, 0.7224672001371106, 1.0, 1e-15);
        add(builder, method, 10, 3, 0.05, 0.10779126740630104, 0.6032218525388546, 1e-15);
        add(builder, method, 400, 20, 0.05, 0.03259742983714725, 0.07596363506371961, 1e-15);
        add(builder, method, 19436, 0, 0.05, 1.3552527156068805e-20, 0.00019760751798472573, 1e-15);
        method = BinomialConfidenceInterval.JEFFREYS;
        // Note: Java implementation sets limits when x=0 or n to 0 or 1 respectively
        add(builder, method, 10, 0, 0.05, /*4.7890433157581984e-05*/ 0, 0.21719626750921053, 1e-14);
        add(builder, method, 10, 10, 0.05, 0.7828037324907895, /*0.9999521095668424*/ 1, 1e-15);
        add(builder, method, 10, 3, 0.05, 0.09269459393815319, 0.6058183181486713, 1e-15);
        add(builder, method, 400, 20, 0.05, 0.031795039152749435, 0.07467797318472456, 1e-15);
        add(builder, method, 19436, 0, 0.05, /*2.5263852458384886e-08*/ 0, 0.00012923175911984633, 1e-13);
        method = BinomialConfidenceInterval.CLOPPER_PEARSON;
        add(builder, method, 10, 0, 0.05, 0, 0.3084971078187608, 1e-15);
        add(builder, method, 10, 10, 0.05, 0.6915028921812392, 1, 1e-15);
        add(builder, method, 10, 3, 0.05, 0.06673951117773447, 0.6524528500599972, 1e-15);
        add(builder, method, 400, 20, 0.05, 0.030805241143265938, 0.07616697275514255, 1e-15);
        // proportion_confint does not match this implementation.
        // Computed using code from Wikipedia:
        // from scipy.stats import beta
        // import numpy as np
        // k = 0
        // n = 19436
        // alpha = 0.05
        // p_u, p_o = beta.ppf([alpha / 2, 1 - alpha / 2], [k, k + 1], [n - k + 1, n - k])
        add(builder, method, 19436, 0, 0.05, 0.0, 0.0001897782161226719, 1e-12);
        method = BinomialConfidenceInterval.AGRESTI_COULL;
        add(builder, method, 10, 0, 0.05, 0.0, 0.3208873057505458, 1e-15);
        add(builder, method, 10, 10, 0.05, 0.6791126942494543, 1.0, 1e-15);
        add(builder, method, 10, 3, 0.05, 0.10333841792242526, 0.6076747020227304, 1e-15);
        add(builder, method, 400, 20, 0.05, 0.03218289448554276, 0.0763781704153241, 1e-15);
        add(builder, method, 19436, 0, 0.05, 0.0, 0.00023852647189663768, 1e-15);
        return builder.build();
    }

    private static void add(Builder<Arguments> builder, BinomialConfidenceInterval method,
            int n, int x, double alpha,
            double lower, double upper, double relativeError) {
        builder.accept(Arguments.of(method, n, x, alpha, lower, upper, relativeError));
    }
}
