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

package org.apache.commons.statistics.examples.jmh.descriptive;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import org.apache.commons.statistics.descriptive.Median;
import org.apache.commons.statistics.examples.jmh.descriptive.QuantilePerformance.AbstractDataSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Executes a benchmark of the creation of a median from array data.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx8192M"})
public class MedianPerformance {
    /** Use the JDK sort function. */
    private static final String JDK = "JDK";
    /** Commons Math 3 Percentile implementation. */
    private static final String CM3 = "CM3";
    /** Commons Math 4 Percentile implementation. */
    private static final String CM4 = "CM4";
    /** Commons Statistics implementation. */
    private static final String STATISTICS = "Statistics";

    /**
     * Source of {@code double} array data.
     *
     * <p>This uses the same data class as {@link QuantilePerformance}.
     * This enables reuse of the various data distributions provided.
     */
    @State(Scope.Benchmark)
    public static class DataSource extends AbstractDataSource {
        /** Data length. */
        @Param({"1000", "100000"})
        private int length;

        /** {@inheritDoc} */
        @Override
        protected int getLength() {
            return length;
        }
    }

    /**
     * Source of a {@link ToDoubleFunction} for a {@code double[]}.
     */
    @State(Scope.Benchmark)
    public static class DoubleFunctionSource {
        /** Name of the source. */
        @Param({JDK, CM3, CM4, STATISTICS})
        private String name;

        /** The action. */
        private ToDoubleFunction<double[]> function;

        /**
         * @return the function
         */
        public ToDoubleFunction<double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            // Note: Functions defensively copy the data by default
            // Note: KeyStratgey does not matter for single / paired keys but
            // we set it anyway for completeness.
            Objects.requireNonNull(name);
            if (JDK.equals(name)) {
                function = DoubleFunctionSource::sortMedian;
            } else if (CM3.equals(name)) {
                final org.apache.commons.math3.stat.descriptive.rank.Median m =
                    new org.apache.commons.math3.stat.descriptive.rank.Median();
                function = m::evaluate;
            } else if (CM4.equals(name)) {
                final org.apache.commons.math4.legacy.stat.descriptive.rank.Median m =
                    new org.apache.commons.math4.legacy.stat.descriptive.rank.Median();
                function = m::evaluate;
            } else if (STATISTICS.equals(name)) {
                function = Median.withDefaults()::evaluate;
            } else {
                throw new IllegalStateException("Unknown double[] function: " + name);
            }
        }

        /**
         * Sort the values and compute the median.
         *
         * @param values Values.
         * @return the median
         */
        private static double sortMedian(double[] values) {
            // Implicit NPE
            final int n = values.length;
            // Special cases
            if (n <= 2) {
                switch (n) {
                case 2:
                    return (values[0] + values[1]) * 0.5;
                case 1:
                    return values[0];
                default:
                    return Double.NaN;
                }
            }
            // A sort is required
            Arrays.sort(values);
            final int k = n >>> 1;
            // Odd
            if ((n & 0x1) == 0x1) {
                return values[k];
            }
            // Even
            return (values[k - 1] + values[k]) * 0.5;
        }
    }

    /**
     * Source of a {@link ToDoubleFunction} for a {@code int[]}.
     */
    @State(Scope.Benchmark)
    public static class IntFunctionSource {
        /** Name of the source. */
        @Param({JDK, STATISTICS})
        private String name;

        /** The action. */
        private ToDoubleFunction<int[]> function;

        /**
         * @return the function
         */
        public ToDoubleFunction<int[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            // Note: Functions defensively copy the data by default
            // Note: KeyStratgey does not matter for single / paired keys but
            // we set it anyway for completeness.
            Objects.requireNonNull(name);
            if (JDK.equals(name)) {
                function = IntFunctionSource::sortMedian;
            } else if (STATISTICS.equals(name)) {
                function = Median.withDefaults()::evaluate;
            } else {
                throw new IllegalStateException("Unknown int[] function: " + name);
            }
        }

        /**
         * Sort the values and compute the median.
         *
         * @param values Values.
         * @return the median
         */
        private static double sortMedian(int[] values) {
            // Implicit NPE
            final int n = values.length;
            // Special cases
            if (n <= 2) {
                switch (n) {
                case 2:
                    return (values[0] + values[1]) * 0.5;
                case 1:
                    return values[0];
                default:
                    return Double.NaN;
                }
            }
            // A sort is required
            Arrays.sort(values);
            final int k = n >>> 1;
            // Odd
            if ((n & 0x1) == 0x1) {
                return values[k];
            }
            // Even
            return (values[k - 1] + values[k]) * 0.5;
        }
    }

    /**
     * Create the statistic using an array.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void doubleMedian(DoubleFunctionSource function, DataSource source, Blackhole bh) {
        final int size = source.size();
        final ToDoubleFunction<double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            bh.consume(fun.applyAsDouble(source.getData(j)));
        }
    }

    /**
     * Create the statistic using an array.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void intMedian(IntFunctionSource function, DataSource source, Blackhole bh) {
        final int size = source.size();
        final ToDoubleFunction<int[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            bh.consume(fun.applyAsDouble(source.getIntData(j)));
        }
    }
}
