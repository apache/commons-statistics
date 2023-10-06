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

package org.apache.commons.statistics.examples.jmh.descriptive;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Executes a benchmark of the creation of a statistic from {@code double} array data.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class StatisticCreationPerformance {
    /**
     * Source of {@code double} array data.
     */
    @State(Scope.Benchmark)
    public static class DataSource {
        /** Data length. */
        @Param({"0", "1", "10", "100", "1000", "10000"})
        private int length;

        /** Data. */
        private double[] data;

        /**
         * @return the data
         */
        public double[] getData() {
            return data;
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            // Data will be randomized per iteration
            data = RandomSource.XO_RO_SHI_RO_128_PP.create().doubles(length).toArray();
        }
    }

    /**
     * A sum of {@code double} data.
     */
    static class SimpleSum implements DoubleConsumer, DoubleSupplier {
        /** The sum. */
        private double sum;

        @Override
        public void accept(double value) {
            sum += value;
        }

        @Override
        public double getAsDouble() {
            return sum;
        }
    }

    /**
     * Create the statistic using a for loop.
     *
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double forLoop(DataSource source) {
        final double[] data = source.getData();
        final SimpleSum s = new SimpleSum();
        for (int i = 0; i < data.length; i++) {
            s.accept(data[i]);
        }
        return s.getAsDouble();
    }

    /**
     * Create the statistic using a for-each loop.
     *
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double forEachLoop(DataSource source) {
        final double[] data = source.getData();
        final SimpleSum s = new SimpleSum();
        for (final double x : data) {
            s.accept(x);
        }
        return s.getAsDouble();
    }

    /**
     * Create the statistic using a stream.
     *
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double streamForEach(DataSource source) {
        final double[] data = source.getData();
        final SimpleSum s = new SimpleSum();
        Arrays.stream(data).forEach(s::accept);
        return s.getAsDouble();
    }
}
