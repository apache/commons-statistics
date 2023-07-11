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
package org.apache.commons.statistics.distribution;

import java.util.Locale;

/**
 * Package private exception class with constants for frequently used messages.
 */
class DistributionException extends IllegalArgumentException {
    /** Error message for "too large" condition when {@code x > y}. */
    static final String TOO_LARGE = "%s > %s";
    /** Error message for "too small" condition when {@code x < y}. */
    static final String TOO_SMALL = "%s < %s";
    /** Error message for "out of range" condition when "x not in [a, b]". */
    static final String OUT_OF_RANGE = "Number %s is out of range [%s, %s]";
    /** Error message for "invalid range" condition when "lower >= upper". */
    static final String INVALID_RANGE_LOW_GTE_HIGH = "Lower bound %s >= upper bound %s";
    /** Error message for "invalid range" condition when "lower > upper". */
    static final String INVALID_RANGE_LOW_GT_HIGH = "Lower bound %s > upper bound %s";
    /** Error message for "invalid probability" condition when "x not in [0, 1]". */
    static final String INVALID_PROBABILITY = "Not a probability: %s is out of range [0, 1]";
    /** Error message for "invalid non-zero probability" condition when "x not in (0, 1]". */
    static final String INVALID_NON_ZERO_PROBABILITY = "Not a non-zero probability: %s is out of range (0, 1]";
    /** Error message for "negative" condition when {@code x < 0}. */
    static final String NEGATIVE = "Number %s is negative";
    /** Error message for "not strictly positive" condition when {@code x <= 0}. */
    static final String NOT_STRICTLY_POSITIVE = "Number %s is not greater than 0";
    /** Error message for "not strictly positive finite" condition when {@code x <= 0 || x == inf}. */
    static final String NOT_STRICTLY_POSITIVE_FINITE = "Number %s is not greater than 0 and finite";

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20180119L;

    /**
     * Creates an exception.
     *
     * @param message Exception message with replaceable parameters.
     * @param formatArguments Arguments for formatting the message.
     */
    DistributionException(String message, Object... formatArguments) {
        super(String.format(Locale.ROOT, message, formatArguments));
    }
}
