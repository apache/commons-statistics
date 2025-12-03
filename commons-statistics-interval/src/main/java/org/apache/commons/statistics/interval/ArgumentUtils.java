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
package org.apache.commons.statistics.interval;

/**
 * Utilities for argument validation.
 *
 * @since 1.2
 */
final class ArgumentUtils {
    /** No instances. */
    private ArgumentUtils() {}

    /**
     * Check the error rate {@code alpha} is in the open interval {@code (0, 1)}.
     *
     * @param alpha Error rate.
     * @throws IllegalArgumentException if {@code alpha} is not in the open interval {@code (0, 1)}.
     */
    static void checkErrorRate(double alpha) {
        if (alpha > 0 && alpha < 1) {
            return;
        }
        // Out-of-range or NaN
        throw new IllegalArgumentException("Error rate is not in (0, 1): " + alpha);
    }
}
