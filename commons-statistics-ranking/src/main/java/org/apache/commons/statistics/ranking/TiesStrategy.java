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
package org.apache.commons.statistics.ranking;

/**
 * Strategies for handling tied values in rank transformations.
 *
 * @since 1.1
 */
public enum TiesStrategy {

    /**
     * Ties are assigned ranks in order of occurrence in the original array.
     *
     * <p>For example, {@code [1, 3, 4, 3]} is ranked as {@code [1, 2, 4, 3]}.
     */
    SEQUENTIAL,

    /**
     * Tied values are assigned the minimum applicable rank, or the rank of the
     * first occurrence.
     *
     * <p>For example, {@code [1, 3, 4, 3]} is ranked as {@code [1, 2, 4, 2]}.
     */
    MINIMUM,

    /**
     * Tied values are assigned the maximum applicable rank, or the rank of the last
     * occurrence. <p>For example, {@code [1, 3, 4, 3]} is ranked as {@code [1, 3, 4, 3]}.
     */
    MAXIMUM,

    /**
     * Tied values are assigned the average of the applicable ranks.
     *
     * <p>For example, {@code [1, 3, 4, 3]} is ranked as {@code [1, 2.5, 4, 2.5]}.
     */
    AVERAGE,

    /**
     * Tied values are assigned a <em>unique</em> random integral rank from among the
     * applicable values.
     *
     * <p>For example, {@code [1, 3, 4, 3]} is ranked as either {@code [1, 2, 4, 3]} or
     * {@code [1, 3, 4, 2]} where the choice is random.
     *
     * <p>The assigned rank will always be an integer, (inclusively) between the
     * values returned by the {@link #MINIMUM} and {@link #MAXIMUM} strategies.
     *
     * <p>Use of a <em>unique</em> rank ensures that ties are resolved so that the
     * rank order is stable.
     */
    RANDOM
}
