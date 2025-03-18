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
 * Returns the product of the available values.
 *
 * <ul>
 *   <li>The result is one if no values are observed.
 *   <li>The result is {@code NaN} if any of the values is {@code NaN}.
 * </ul>
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 *
 * <p><strong>This instance is not thread safe.</strong>
 * If multiple threads access an instance of this class concurrently,
 * and at least one of the threads invokes the {@link java.util.function.DoubleConsumer#accept(double) accept} or
 * {@link StatisticAccumulator#combine(StatisticResult) combine} method, it must be synchronized externally.
 *
 * <p>However, it is safe to use {@link java.util.function.DoubleConsumer#accept(double) accept}
 * and {@link StatisticAccumulator#combine(StatisticResult) combine}
 * as {@code accumulator} and {@code combiner} functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel instance of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 *
 * @since 1.1
 */
public final class Product implements DoubleStatistic, StatisticAccumulator<Product> {

    /** Product of all values. */
    private double productValue = 1;

    /**
     * Create an instance.
     */
    private Product() {
        // No-op
    }

    /**
     * Creates an instance.
     *
     * <p>The initial result is one.
     *
     * @return {@code Product} instance.
     */
    public static Product create() {
        return new Product();
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}
     * or the product at any point is a {@code NaN}.
     *
     * <p>When the input is an empty array, the result is one.
     *
     * @param values Values.
     * @return {@code Product} instance.
     */
    public static Product of(double... values) {
        return Statistics.add(new Product(), values);
    }

    /**
     * Returns an instance populated using the specified range of {@code values}.
     *
     * <p>The result is {@code NaN} if any of the values is {@code NaN}
     * or the product at any point is a {@code NaN}.
     *
     * <p>When the range is empty, the result is one.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Product} instance.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static Product ofRange(double[] values, int from, int to) {
        Statistics.checkFromToIndex(from, to, values.length);
        return createFromRange(values, from, to);
    }

    /**
     * Create an instance using the specified range of {@code values}.
     *
     * <p>Warning: No range checks are performed.
     *
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return {@code Product} instance.
     */
    static Product createFromRange(double[] values, int from, int to) {
        return Statistics.add(new Product(), values, from, to);
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is one.
     *
     * @param values Values.
     * @return {@code Product} instance.
     */
    public static Product of(int... values) {
        return Statistics.add(new Product(), values);
    }

    /**
     * Returns an instance populated using the input {@code values}.
     *
     * <p>When the input is an empty array, the result is one.
     *
     * @param values Values.
     * @return {@code Product} instance.
     */
    public static Product of(long... values) {
        return Statistics.add(new Product(), values);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        this.productValue *= value;
    }

    /**
     * Gets the product of all input values.
     *
     * <p>When no values have been added, the result is one.
     *
     * @return product of all values.
     */
    @Override
    public double getAsDouble() {
        return productValue;
    }

    @Override
    public Product combine(Product other) {
        productValue *= other.productValue;
        return this;
    }
}
