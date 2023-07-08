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

/**
 * Represents a statistic being computed.
 */
public enum Statistic {
    /** Represents the minimum of the input value(s). */
    MIN,

    /** Represents the maximum of the input value(s). */
    MAX,

    /** Represents the sum of the input value(s). */
    SUM,

    /** Represents the arithmetic mean of the input value(s). */
    MEAN,

    /** Represents the sum of the natural logs of the input value(s). */
    SUM_OF_LOGS,

    /** Represents the sum of the squares of the input value(s). */
    SUM_OF_SQUARES,

    /** Represents the sample variance of the input value(s). */
    VARIANCE,

    /** Represents the population variance of the input value(s). */
    POPULATION_VARIANCE,

    /** Represents the standard deviation of the input value(s). */
    STANDARD_DEVIATION,

    /** Represents the geometric mean of the input value(s). */
    GEOMETRIC_MEAN,

    /** Represents the quadratic of the input value(s). */
    QUADRATIC_MEAN
}
