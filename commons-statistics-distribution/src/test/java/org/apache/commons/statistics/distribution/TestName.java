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
 * Contains the names for the standard tests.
 * These names are used as keys in the test resource properties files.
 */
enum TestName {
    PDF,
    LOGPDF,
    PMF,
    LOGPMF,
    CDF,
    SF,
    CDF_HP,
    SF_HP,
    ICDF,
    ISF,
    CDF_MAPPING,
    SF_MAPPING,
    CDF_HP_MAPPING,
    SF_HP_MAPPING,
    COMPLEMENT,
    CONSISTENCY,
    OUTSIDE_SUPPORT,
    SAMPLING,
    SAMPLING_PMF,
    INTEGRALS,
    SUPPORT,
    MOMENTS,
    MEDIAN,
    PMF_SUM;

    /** Cache the values for use in string conversion. */
    private static final TestName[] VALUES = values();

    /** The test name as a String. */
    private final String name;

    /**
     * Create an instance.
     */
    TestName() {
        name = this.name().toLowerCase(Locale.ROOT).replace('_', '.');
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Get the instance from the String representation.
     *
     * @param key String representation.
     * @return the instance (or null)
     */
    static TestName fromString(String key) {
        for (final TestName v : VALUES) {
            if (v.name.equals(key)) {
                return v;
            }
        }
        return null;
    }
}

