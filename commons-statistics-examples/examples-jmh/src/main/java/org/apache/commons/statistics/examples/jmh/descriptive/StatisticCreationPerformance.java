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
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.DoubleStatistic;
import org.apache.commons.statistics.descriptive.GeometricMean;
import org.apache.commons.statistics.descriptive.Kurtosis;
import org.apache.commons.statistics.descriptive.Max;
import org.apache.commons.statistics.descriptive.Mean;
import org.apache.commons.statistics.descriptive.Min;
import org.apache.commons.statistics.descriptive.Product;
import org.apache.commons.statistics.descriptive.Skewness;
import org.apache.commons.statistics.descriptive.StandardDeviation;
import org.apache.commons.statistics.descriptive.Statistic;
import org.apache.commons.statistics.descriptive.Sum;
import org.apache.commons.statistics.descriptive.SumOfLogs;
import org.apache.commons.statistics.descriptive.SumOfSquares;
import org.apache.commons.statistics.descriptive.Variance;
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
         * @return the start inclusive of the sub-range.
         */
        public int from() {
            // Approximately 1/4
            return data.length >> 2;
        }

        /**
         * @return the end exclusive of the sub-range.
         */
        public int to() {
            // Approximately 3/4
            return (data.length >> 1) + (data.length >> 2);
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            // Data will be randomized per iteration.
            // Ideally the product should not underflow/overflow.
            // A product of 1 would have a sum of logs of 0.
            // Create a uniform sum of logs around zero and transform:
            // log x in [-0.5, 0.5) => x in [0.607, 1.649)
            data = RandomSource.XO_RO_SHI_RO_128_PP.create().doubles(length)
                .map(x -> Math.exp(x - 0.5)).toArray();
        }
    }

    /**
     * Source of a {@code Statistic}.
     */
    @State(Scope.Benchmark)
    public static class StatisticSource {
        /** The statistic to create. */
        @Param()
        private Statistic statistic;

        /** Statistic factory. */
        private Supplier<DoubleStatistic> supplier;

        /** Statistic factory using input data. */
        private Function<double[], DoubleStatistic> factory;

        /**
         * @return a statistic instance
         */
        public DoubleStatistic create() {
            return supplier.get();
        }

        /**
         * @param x Values.
         * @return a statistic instance
         */
        public DoubleStatistic create(double[] x) {
            return factory.apply(x);
        }

        /**
         * Create the factory functions.
         */
        @Setup(Level.Trial)
        public void setup() {
            switch (statistic) {
            case GEOMETRIC_MEAN:
                supplier = GeometricMean::create;
                factory = GeometricMean::of;
                break;
            case KURTOSIS:
                supplier = Kurtosis::create;
                factory = Kurtosis::of;
                break;
            case MAX:
                supplier = Max::create;
                factory = Max::of;
                break;
            case MEAN:
                supplier = Mean::create;
                factory = Mean::of;
                break;
            case MIN:
                supplier = Min::create;
                factory = Min::of;
                break;
            case PRODUCT:
                supplier = Product::create;
                factory = Product::of;
                break;
            case SKEWNESS:
                supplier = Skewness::create;
                factory = Skewness::of;
                break;
            case STANDARD_DEVIATION:
                supplier = StandardDeviation::create;
                factory = StandardDeviation::of;
                break;
            case SUM:
                supplier = Sum::create;
                factory = Sum::of;
                break;
            case SUM_OF_LOGS:
                supplier = SumOfLogs::create;
                factory = SumOfLogs::of;
                break;
            case SUM_OF_SQUARES:
                supplier = SumOfSquares::create;
                factory = SumOfSquares::of;
                break;
            case VARIANCE:
                supplier = Variance::create;
                factory = Variance::of;
                break;
            default:
                throw new IllegalStateException("Unsupported statistic: " + statistic);
            }
        }
    }

    /**
     * Source of a {@code Statistic} created using a custom implementation.
     * This contains alternative version of creating statistics from an array
     * for benchmarking performance.
     */
    @State(Scope.Benchmark)
    public static class CustomStatisticSource {
        /** The statistic to create. */
        @Param({"min", "product"})
        private String statistic;

        /** Statistic factory using input data. */
        private Function<double[], DoubleSupplier> factory;

        /**
         * @param x Values.
         * @return a statistic instance
         */
        public DoubleSupplier create(double[] x) {
            return factory.apply(x);
        }

        /**
         * Create the factory functions.
         */
        @Setup(Level.Trial)
        public void setup() {
            if ("min".equals(statistic)) {
                factory = CMin::of;
            } else if ("product".equals(statistic)) {
                factory = CProduct::of;
            } else {
                throw new IllegalStateException("Unsupported custom statistic: " + statistic);
            }
        }

        /** Compute the minimum. */
        static final class CMin implements DoubleSupplier {
            /** Current statistic. */
            private double s;

            /**
             * Create an instance.
             * @param s Statistic value.
             */
            private CMin(double s) {
                this.s = s;
            }

            /**
             * @param values Values.
             * @return instance.
             */
            static CMin of(double... values) {
                double s = Double.POSITIVE_INFINITY;
                for (final double x : values) {
                    s = Math.min(s, x);
                }
                return new CMin(s);
            }

            @Override
            public double getAsDouble() {
                return s;
            }
        }

        /** Compute the product. */
        static final class CProduct implements DoubleSupplier {
            /** Current statistic. */
            private double s;

            /**
             * Create an instance.
             * @param s Statistic value.
             */
            private CProduct(double s) {
                this.s = s;
            }

            /**
             * @param values Values.
             * @return instance.
             */
            static CProduct of(double... values) {
                double s = 1;
                for (final double x : values) {
                    s *= x;
                }
                return new CProduct(s);
            }

            @Override
            public double getAsDouble() {
                return s;
            }
        }
    }

    /**
     * Create the statistic using an array.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double array(DataSource dataSource, StatisticSource statisticSource) {
        return statisticSource.create(dataSource.getData()).getAsDouble();
    }

    /**
     * Create the statistic using an array.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double customArray(DataSource dataSource, CustomStatisticSource statisticSource) {
        return statisticSource.create(dataSource.getData()).getAsDouble();
    }

    /**
     * Create the statistic using a for loop.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double forLoop(DataSource dataSource, StatisticSource statisticSource) {
        final double[] data = dataSource.getData();
        final DoubleStatistic s = statisticSource.create();
        for (int i = 0; i < data.length; i++) {
            s.accept(data[i]);
        }
        return s.getAsDouble();
    }

    /**
     * Create the statistic using a for-each loop.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double forEachLoop(DataSource dataSource, StatisticSource statisticSource) {
        final double[] data = dataSource.getData();
        final DoubleStatistic s = statisticSource.create();
        for (final double x : data) {
            s.accept(x);
        }
        return s.getAsDouble();
    }

    /**
     * Create the statistic using a stream.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double streamForEach(DataSource dataSource, StatisticSource statisticSource) {
        final double[] data = dataSource.getData();
        final DoubleStatistic s = statisticSource.create();
        Arrays.stream(data).forEach(s::accept);
        return s.getAsDouble();
    }

    /**
     * Create the statistic using an array.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double arrayRange(DataSource dataSource, StatisticSource statisticSource) {
        final int from = dataSource.from();
        final int to = dataSource.to();
        final double[] data = Arrays.copyOfRange(dataSource.getData(), from, to);
        return statisticSource.create(data).getAsDouble();
    }

    /**
     * Create the statistic using an array.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double customArrayRange(DataSource dataSource, CustomStatisticSource statisticSource) {
        final int from = dataSource.from();
        final int to = dataSource.to();
        final double[] data = Arrays.copyOfRange(dataSource.getData(), from, to);
        return statisticSource.create(data).getAsDouble();
    }

    /**
     * Create the statistic using a for loop on a range of the data.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double forLoopRange(DataSource dataSource, StatisticSource statisticSource) {
        final int from = dataSource.from();
        final int to = dataSource.to();
        final double[] data = dataSource.getData();
        final DoubleStatistic s = statisticSource.create();
        for (int i = from; i < to; i++) {
            s.accept(data[i]);
        }
        return s.getAsDouble();
    }

    /**
     * Create the statistic using a for-each loop on a range of the data.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double forEachLoopRange(DataSource dataSource, StatisticSource statisticSource) {
        final int from = dataSource.from();
        final int to = dataSource.to();
        final double[] data = Arrays.copyOfRange(dataSource.getData(), from, to);
        final DoubleStatistic s = statisticSource.create();
        for (final double x : data) {
            s.accept(x);
        }
        return s.getAsDouble();
    }

    /**
     * Create the statistic using a stream on a range of the data.
     *
     * @param dataSource Source of the data.
     * @param statisticSource Source of the statistic.
     * @return the statistic
     */
    @Benchmark
    public double streamForEachRange(DataSource dataSource, StatisticSource statisticSource) {
        final int from = dataSource.from();
        final int to = dataSource.to();
        final double[] data = dataSource.getData();
        final DoubleStatistic s = statisticSource.create();
        Arrays.stream(data, from, to).forEach(s::accept);
        return s.getAsDouble();
    }
}
