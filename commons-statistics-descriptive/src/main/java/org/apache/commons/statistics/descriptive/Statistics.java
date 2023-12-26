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

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Utility methods for statistics.
 */
final class Statistics {
    /** No instances. */
    private Statistics() {}

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends DoubleConsumer> T add(T statistic, double[] values) {
        for (final double x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends IntConsumer> T add(T statistic, int[] values) {
        for (final int x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends LongConsumer> T add(T statistic, long[] values) {
        for (final long x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Returns {@code true} if the second central moment {@code m2} is effectively
     * zero given the magnitude of the first raw moment {@code m1}.
     *
     * <p>This method shares the logic for detecting a zero variance among implementations
     * that divide by the variance (e.g. skewness, kurtosis).
     *
     * @param m1 First raw moment (mean).
     * @param m2 Second central moment (biased variance).
     * @return true if the variance is zero
     */
    static boolean zeroVariance(double m1, double m2) {
        // Note: Commons Math checks the variance is < 1e-19.
        // The absolute threshold does not account for the magnitude of the sample.
        // This checks the average squared deviation from the mean (m2)
        // is smaller than the squared precision of the mean (m1).
        // Precision is set to 15 decimal digits
        // (1e-15 ~ 4.5 eps where eps = 2^-52).
        final double meanPrecision = 1e-15 * m1;
        return m2 <= meanPrecision * meanPrecision;
    }
}
