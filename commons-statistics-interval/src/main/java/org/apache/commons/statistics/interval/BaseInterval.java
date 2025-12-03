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
 * Base class representing an {@link Interval}.
 *
 * @since 1.2
 */
final class BaseInterval implements Interval {
    /** Lower bound. */
    private final double lower;
    /** Upper bound. */
    private final double upper;

    /**
     * Create an instance.
     *
     * @param lower Lower bound.
     * @param upper Upper bound.
     */
    BaseInterval(double lower, double upper) {
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    public double getLowerBound() {
        return lower;
    }

    @Override
    public double getUpperBound() {
        return upper;
    }
}
