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
package org.apache.commons.statistics.examples.distribution;

/**
 * Stored a distribution and its parameters.
 *
 * @param <T> The type of distribution
 */
class Distribution<T> {
    /** The distribution. */
    private final T distribution;
    /** The parameters. */
    private final String parameters;

    /**
     * @param distribution the distribution
     * @param parameters the parameters
     */
    Distribution(T distribution, String parameters) {
        this.distribution = distribution;
        this.parameters = parameters;
    }

    /**
     * Gets the distribution.
     *
     * @return the distribution
     */
    T getDistribution() {
        return distribution;
    }

    /**
     * Gets the parameters.
     *
     * @return the parameters
     */
    String getParameters() {
        return parameters;
    }
}
