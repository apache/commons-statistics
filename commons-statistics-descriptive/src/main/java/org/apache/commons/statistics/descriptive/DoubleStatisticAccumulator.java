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
 * A mutable result container that accumulates a {@code DoubleStatistic}.
 *
 * @param <T> {@code DoubleStatistic} being accumulated.
 */
public interface DoubleStatisticAccumulator<T extends DoubleStatistic> {

    /**
     * Combines the state of another {@code DoubleStatistic} into this one.
     *
     * @param other Another {@code DoubleStatistic} to be combined.
     * @return {@code this} instance after combining {@code other}.
     */
    T combine(T other);
}
