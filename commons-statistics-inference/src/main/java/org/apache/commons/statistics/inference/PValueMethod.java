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
 * Represents a method for computing a p-value for a test statistic.
 *
 * @since 1.1
 */
public enum PValueMethod {
    /**
     * Automatically choose the method to evaluate the p-value.
     *
     * <p>The choice will use heuristics based on the input data.
     */
    AUTO,

    /**
     * Use the exact distribution of the test statistic to evaluate the p-value.
     */
    EXACT,

    /**
     * Use the asymptotic distribution of the test statistic to evaluate the p-value.
     */
    ASYMPTOTIC,

    /**
     * Use an estimation method for the p-value.
     * For example this may use random sampling or permutations.
     */
    ESTIMATE
}
