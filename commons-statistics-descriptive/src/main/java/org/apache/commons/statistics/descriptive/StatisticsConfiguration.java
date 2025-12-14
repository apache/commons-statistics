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

/**
 * Configuration for computation of statistics.
 *
 * <p>This class is immutable.
 *
 * @since 1.1
 */
public final class StatisticsConfiguration {
    /** Default instance. */
    private static final StatisticsConfiguration DEFAULT = new StatisticsConfiguration(false);

    /** Flag to control if the statistic is biased, or should use a bias correction. */
    private final boolean biased;

    /**
     * Create an instance.
     *
     * @param biased Biased option.
     */
    private StatisticsConfiguration(boolean biased) {
        this.biased = biased;
    }

    /**
     * Return an instance using the default options.
     *
     * <ul>
     *  <li>{@linkplain #isBiased() Biased = false}</li>
     * </ul>
     *
     * @return default instance
     */
    public static StatisticsConfiguration withDefaults() {
        return DEFAULT;
    }

    /**
     * Return an instance with the configured biased option.
     *
     * <p>The correction of bias in a statistic is implementation dependent.
     * If set to {@code true} then bias correction will be disabled.
     *
     * <p>This option is used by:
     * <ul>
     *  <li>{@link StandardDeviation}</li>
     *  <li>{@link Variance}</li>
     *  <li>{@link Skewness}</li>
     *  <li>{@link Kurtosis}</li>
     * </ul>
     *
     * @param v Value.
     * @return an instance
     */
    public StatisticsConfiguration withBiased(boolean v) {
        return new StatisticsConfiguration(v);
    }

    /**
     * Checks if the calculation of the statistic is biased. If {@code false} the calculation
     * should use a bias correction.
     *
     * @return true if biased
     */
    public boolean isBiased() {
        return biased;
    }
}
