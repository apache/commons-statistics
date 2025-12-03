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
package org.apache.commons.statistics.inference;

/**
 * Represents an alternative hypothesis for a hypothesis test.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Alternative_hypothesis">Alternative hypothesis (Wikipedia)</a>
 * @since 1.1
 */
public enum AlternativeHypothesis {
    /**
     * Represents a two-sided test.
     * <ul>
     * <li>Null hypothesis (H<sub>0</sub>): {@code p = p0}
     * <li>Alternative hypothesis (H<sub>1</sub>): {@code p != p0}
     * </ul>
     */
    TWO_SIDED,

    /**
     * Represents a right-sided test.
     * <ul>
     * <li>Null hypothesis (H<sub>0</sub>): {@code p <= p0}
     * <li>Alternative hypothesis (H<sub>1</sub>): {@code p > p0}
     * </ul>
     */
    GREATER_THAN,

    /**
     * Represents a left-sided test.
     * <ul>
     * <li>Null hypothesis (H<sub>0</sub>): {@code p >= p0}
     * <li>Alternative hypothesis (H<sub>1</sub>): {@code p < p0}
     * </ul>
     */
    LESS_THAN
}
