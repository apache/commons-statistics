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

/**
 * Contains the result of a test for significance.
 *
 * @since 1.1
 */
public interface SignificanceResult {
    /**
     * Returns the test statistic.
     *
     * @return the statistic
     */
    double getStatistic();

    /**
     * Returns the test statistic p-value.
     *
     * <p>The number returned is the smallest significance level at which one can
     * reject the null hypothesis.
     *
     * @return the p-value
     */
    double getPValue();

    /**
     * Returns true iff the null hypothesis can be rejected with {@code 100 * (1 - alpha)}
     * percent confidence.
     *
     * <p>The default implementation uses {@code p < alpha}.
     *
     * @param alpha Significance level of the test.
     * @return true iff null hypothesis can be rejected with confidence {@code 1 - alpha}
     * @throws IllegalArgumentException if {@code alpha} is not in the range {@code (0, 0.5]}.
     */
    default boolean reject(double alpha) {
        Arguments.checkSignificance(alpha);
        return getPValue() < alpha;
    }
}
