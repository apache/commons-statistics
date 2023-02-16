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
package org.apache.commons.statistics.ranking;

/**
 * Strategies for handling {@link Double#NaN NaN} values in rank transformations.
 *
 * @since 1.1
 */
public enum NaNStrategy {

    /**
     * NaNs are considered minimal in the ordering, equivalent to (that is, tied
     * with) {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
     */
    MINIMAL,

    /**
     * NaNs are considered maximal in the ordering, equivalent to (that is, tied
     * with) {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     */
    MAXIMAL,

    /** NaNs are removed before rank transform is applied. */
    REMOVED,

    /**
     * NaNs are left fixed "in place", that is the rank transformation is applied to
     * the other elements in the input array, but the NaN elements are returned
     * unchanged.
     */
    FIXED,

    /** NaNs result in an exception. */
    FAILED
}
