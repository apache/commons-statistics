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
package org.apache.commons.statistics.inference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link SignificanceResult}.
 */
class SignificanceResultTest {

    @ParameterizedTest
    @CsvSource({
        "1.23, 0.5",
        "10, 0",
        "0, 1",
    })
    void testBaseSignificanceResult(double statistic, double p) {
        final SignificanceResult r = new BaseSignificanceResult(statistic, p);
        Assertions.assertEquals(statistic, r.getStatistic(), "statistic");
        Assertions.assertEquals(p, r.getPValue(), "p-value");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, -Double.MIN_VALUE, 0, 0.5000000000000001})
    void testRejectThrows(double alpha) {
        final SignificanceResult r = new SignificanceResult() {
            @Override
            public double getStatistic() {
                return 0;
            }
            @Override
            public double getPValue() {
                return 0;
            }
        };
        Assertions.assertThrows(IllegalArgumentException.class, () -> r.reject(alpha));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, Double.MIN_VALUE, 1e-6, 0.01, 0.05, 0.5})
    void testReject(double p) {
        final SignificanceResult r = new BaseSignificanceResult(Double.NaN, p);
        for (final double alpha : new double[] {Double.MIN_VALUE, 1e-6, 0.01, 0.05, 0.5}) {
            Assertions.assertEquals(p < alpha, r.reject(alpha), () -> "alpha: " + alpha);
        }
    }
}
