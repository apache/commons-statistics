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

package org.apache.commons.statistics.interval;

import java.util.EnumSet;
import org.apache.commons.statistics.descriptive.DoubleStatistics;
import org.apache.commons.statistics.descriptive.Statistic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test code used in the interval section of the user guide.
 */
class UserGuideTest {
    @Test
    void testInterval1() {
        // Results generated using Python statsmodels.stats.proportion.proportion_confint
        // >>> print('%.5f, %.5f' % proportion_confint(5, 10, 0.05, method='wilson'))
        // 0.23659, 0.76341
        BinomialConfidenceInterval method = BinomialConfidenceInterval.WILSON_SCORE;
        double alpha = 0.05;

        Interval interval = method.fromErrorRate(10, 5, alpha);
        Assertions.assertEquals(0.23659, interval.getLowerBound(), 1e-5);
        Assertions.assertEquals(0.76341, interval.getUpperBound(), 1e-5);

        assertInterval(method.fromErrorRate(100, 50, alpha), 0.40383, 0.59617, 1e-5);
        assertInterval(method.fromErrorRate(1000, 500, alpha), 0.46907, 0.53093, 1e-5);
        assertInterval(method.fromErrorRate(10000, 5000, alpha), 0.49020, 0.50980, 1e-5);
    }

    @Test
    void testInterval2() {
        // Results generated using R 4.4.3, e.g.
        // options(digits=3)
        // rnorm(15, 1.45, 0.1)
        // Create data using the rounded sample:
        // x = c(1.47, 1.40, 1.55, 1.44, 1.41, 1.38, 1.53, 1.42, 1.55, 1.55, 1.31, 1.37, 1.53, 1.47, 1.51)
        // options(digits=17)
        // mean(x); var(x); length(x)
        // t.test(x, conf.level=0.95)$conf.int
        double[] x = {1.47, 1.40, 1.55, 1.44, 1.41, 1.38, 1.53, 1.42, 1.55, 1.55, 1.31, 1.37, 1.53, 1.47, 1.51};
        DoubleStatistics stats = DoubleStatistics.of(EnumSet.of(Statistic.MEAN, Statistic.VARIANCE), x);

        double mean = stats.getAsDouble(Statistic.MEAN);
        double variance = stats.getAsDouble(Statistic.VARIANCE);
        long n = stats.getCount();
        double alpha = 0.05;

        Assertions.assertEquals(1.46, mean, 1e-2);
        Assertions.assertEquals(0.0058, variance, 1e-4);

        Interval interval = NormalConfidenceInterval.MEAN.fromErrorRate(mean, variance, n, alpha);
        Assertions.assertEquals(1.4170, interval.getLowerBound(), 1e-4);
        Assertions.assertEquals(1.5017, interval.getUpperBound(), 1e-4);
    }

    private static void assertInterval(Interval interval, double lower, double upper, double relError) {
        Assertions.assertEquals(lower, interval.getLowerBound(), lower * relError, "lower");
        Assertions.assertEquals(upper, interval.getUpperBound(), upper * relError, "upper");
    }
}
