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
    /** Error message for "too large" condition. */
    static final String TOO_LARGE = "%s > %s";
    /** Error message for "too small" condition. */
    static final String TOO_SMALL = "%s < %s";
    /** Error message for "out of range" condition. */
    static final String OUT_OF_RANGE = "Number %s is out of range [%s, %s]";
    /** Error message for "invalid probability" condition. */
    static final String INVALID_PROBABILITY = "Not a probability: %s is out of range [0, 1]";
    /** Error message for "out of range" condition. */
    static final String NEGATIVE = "Number %s is negative";
    /** Error message for "mismatch" condition. */
    static final String MISMATCH = "Expected %s but was %s";

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20180119L;

    /**
     * Creates an exception.
     *
     * @param message Exception message with replaceable parameters.
     * @param formatArguments Arguments for formatting the message.
     */
    DistributionException(String message, Object... formatArguments) {
        super(String.format((Locale) null, message, formatArguments));
    }
}
