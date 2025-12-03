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
 * Defines a transformer for {@code NaN} values in arrays.
 *
 * <p>This interface is not intended for a public API. It provides a consistent method
 * to handle partial sorting of {@code double[]} data in the {@link Median} and
 * {@link Quantile} classes.
 *
 * <p>The transformer allows pre-processing floating-point data before applying a sort algorithm.
 * This is required to handle {@code NaN}.
 *
 * <p>Note: The {@code <} relation does not provide a total order on all double
 * values: {@code -0.0 == 0.0} is {@code true} and a {@code NaN}
 * value compares neither less than, greater than, nor equal to any value,
 * even itself.
 *
 * <p>The {@link Double#compare(double, double)} method imposes the ordering:
 * {@code -0.0} is treated as less than value
 * {@code 0.0} and {@code Double.NaN} is considered greater than any
 * other value and all {@code Double.NaN} values are considered equal.
 *
 * <p>This interface allows implementations to respect the behaviour of
 * {@link Double#compare(double, double)}, or implement different behaviour.
 */
@FunctionalInterface
interface NaNTransformer {
    /**
     * Pre-process the data for partitioning.
     *
     * <p>This method will scan all the data and apply processing to {@code NaN} values.
     *
     * <p>The method will return:
     * <ul>
     * <li>An array to partition; this may be a copy.
     * <li>The {@code bounds} of the returned data as [start, end); this can be smaller than the
     * input range if the transformer is configured to exclude NaN values. The start is inclusive
     * and the end is exclusive.
     * </ul>
     *
     * <p>Implementations may assume the input {@code [from, to)} range is valid given the
     * length of the {@code data} array.
     *
     * @param data Data.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @param bounds Set to [start, end).
     * @return pre-processed data (may be a copy)
     */
    double[] apply(double[] data, int from, int to, int[] bounds);
}
