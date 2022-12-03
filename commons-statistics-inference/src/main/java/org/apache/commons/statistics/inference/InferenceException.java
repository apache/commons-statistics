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

import java.util.Locale;

/**
 * Package private exception class with constants for frequently used messages.
 *
 * @since 1.1
 */
class InferenceException extends IllegalArgumentException {
    /** Error message for "invalid probability" condition when "x not in [0, 1]". */
    static final String INVALID_PROBABILITY = "Not a probability: %s is out of range [0, 1]";
    /** Error message for "categories {@code x < 2}". */
    static final String TWO_CATEGORIES_REQUIRED = "Categories size %s < 2";
    /** Error message for "values {@code x < 2}". */
    static final String TWO_VALUES_REQUIRED = "Values size %s < 2";
    /** Error message for "categories {@code x < y}". */
    static final String CATEGORIES_REQUIRED = "Categories size %s < %s";
    /** Error message for "values {@code x < y}". */
    static final String VALUES_REQUIRED = "Values size %s < %s";
    /** Error message for "non-rectangular matrix" when "some row lengths x != others y". */
    static final String NOT_RECTANGULAR = "Non-rectangular matrix: somes rows have size %s while others are %s";
    /** Error message for "mismatch" condition when "values x != y". */
    static final String VALUES_MISMATCH = "Values size mismatch %s != %s";
    /** Error message for "negative" condition when "{@code x < 0}". */
    static final String NEGATIVE = "%s is negative";
    /** Error message for "zero" condition when "{@code x == 0}". */
    static final String ZERO = "%s is zero";
    /** Error message for "zero" condition when "{@code x[i] == 0}". */
    static final String ZERO_AT = "%s[%s] is zero";
    /** Error message for "invalid significance" condition when "x not in (0, 0.5]". */
    static final String INVALID_SIGNIFICANCE = "Not a significance: %s is out of range (0, 0.5]";
    /** Error message for "not strictly positive" condition when "x <= 0". */
    static final String NOT_STRICTLY_POSITIVE = "Number %s is not greater than 0";
    /** Error message for "no data" condition. */
    static final String NO_DATA = "No data";
    /** Error message for "too large" condition when "x > y". */
    static final String X_GT_Y = "%s > %s";
    /** Error message for "too large" condition when "x >= y". */
    static final String X_GTE_Y = "%s >= %s";

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20221203L;

    /**
     * Creates an exception.
     *
     * @param message Exception message.
     */
    InferenceException(String message) {
        super(message);
    }

    /**
     * Creates an exception.
     *
     * @param message Exception message with replaceable parameters.
     * @param formatArguments Arguments for formatting the message.
     */
    InferenceException(String message, Object... formatArguments) {
        super(String.format(Locale.ROOT, message, formatArguments));
    }
}
