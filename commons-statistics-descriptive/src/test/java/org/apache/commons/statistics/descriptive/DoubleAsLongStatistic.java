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

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Wrapper to allow testing a {@link DoubleStatistic} as an {@link LongStatistic}.
 */
final class DoubleAsLongStatistic implements LongStatistic, StatisticAccumulator<DoubleAsLongStatistic> {

    /** Statistic object. */
    private final Object stat;
    /** Consumer of values. */
    private final DoubleConsumer consumer;
    /** Supplier of the computed statistic. */
    private final DoubleSupplier supplier;
    /** Combiner of statistic objects. */
    private final Consumer<Object> combiner;

    /**
     * Create an instance.
     *
     * @param stat Statistic object.
     * @param consumer Consumer of values.
     * @param supplier Supplier of the computed statistic.
     * @param combiner Combiner of statistic objects.
     */
    private DoubleAsLongStatistic(Object stat, DoubleConsumer consumer,
            DoubleSupplier supplier, Consumer<Object> combiner) {
        this.stat = stat;
        this.consumer = consumer;
        this.supplier = supplier;
        this.combiner = combiner;
    }

    /**
     * Return an {@link LongStatistic} from a {@link DoubleStatistic}.
     *
     * @param <T> type of DoubleStatistic.
     * @param stat Statistic.
     * @return converted statistic
     */
    static <T extends DoubleStatistic & StatisticAccumulator<T>> DoubleAsLongStatistic from(T stat) {
        @SuppressWarnings("unchecked")
        final Consumer<Object> combiner = other -> stat.combine((T) other);
        return new DoubleAsLongStatistic(stat, stat::accept, stat::getAsDouble, combiner);
    }

    @Override
    public void accept(long value) {
        consumer.accept(value);
    }

    @Override
    public double getAsDouble() {
        return supplier.getAsDouble();
    }

    @Override
    public DoubleAsLongStatistic combine(DoubleAsLongStatistic other) {
        combiner.accept(other.stat);
        return this;
    }
}
