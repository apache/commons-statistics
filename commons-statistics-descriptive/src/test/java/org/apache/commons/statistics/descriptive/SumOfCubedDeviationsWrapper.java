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
package org.apache.commons.statistics.descriptive;

/**
 * Wrapper for {@link SumOfCubedDeviations} to allow testing as a statistic.
 */
final class SumOfCubedDeviationsWrapper implements
        DoubleStatistic, StatisticAccumulator<SumOfCubedDeviationsWrapper> {

    private final SumOfCubedDeviations delegate;

    /**
     * Creates an instance with the sum of cubed deviations from the mean.
     *
     * @param sc Sum of cubed deviations.
     */
    SumOfCubedDeviationsWrapper(SumOfCubedDeviations sc) {
        delegate = sc;
    }

    @Override
    public void accept(double value) {
        delegate.accept(value);
    }

    @Override
    public double getAsDouble() {
        return delegate.getSumOfCubedDeviations();
    }

    @Override
    public SumOfCubedDeviationsWrapper combine(SumOfCubedDeviationsWrapper other) {
        delegate.combine(other.delegate);
        return this;
    }
}
