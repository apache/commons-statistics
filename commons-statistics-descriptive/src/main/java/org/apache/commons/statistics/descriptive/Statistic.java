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
 * A statistic that can be computed on univariate data, for example a stream of
 * {@code double} values.
 *
 * <p>{@code Statistic} is an enum representing the statistics that can be computed
 * by implementations in the {@code org.apache.commons.statistics.descriptive} package.
 *
 * <p><strong>Note</strong>
 *
 * <p>Implementations may provide additional parameters to control the computation of
 * the statistic, for example to compute the population (biased) or sample (unbiased) variance.
 *
 * @since 1.1
 */
public enum Statistic {
    /** Minimum. */
    MIN,
    /** Maximum. */
    MAX,
    /** Mean, or average. */
    MEAN,
    /** Standard deviation. */
    STANDARD_DEVIATION,
    /** Variance. */
    VARIANCE,
    /** Skewness. */
    SKEWNESS,
    /** Kurtosis. */
    KURTOSIS,
    /** Product. */
    PRODUCT,
    /** Sum. */
    SUM,
    /** Sum of the natural logarithm of values. */
    SUM_OF_LOGS,
    /** Sum of the squared values. */
    SUM_OF_SQUARES,
    /** Geometric mean. */
    GEOMETRIC_MEAN
}
