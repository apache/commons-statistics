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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.LongStream;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.numbers.fraction.BigFraction;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.DoubleStatistic;
import org.apache.commons.statistics.descriptive.IntMean;
import org.apache.commons.statistics.descriptive.IntStatistic;
import org.apache.commons.statistics.descriptive.IntVariance;
import org.apache.commons.statistics.descriptive.LongMean;
import org.apache.commons.statistics.descriptive.LongStatistic;
import org.apache.commons.statistics.descriptive.LongVariance;
import org.apache.commons.statistics.descriptive.Mean;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Executes a benchmark of the moment-based statistics for integer values
 * ({@code int} or {@code long}) compared to using {@code double} values.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class IntMomentPerformance {
    /** Commons Statistics Mean implementation. */
    private static final String DOUBLE_MEAN = "DoubleMean";
    /** Integer mean implementation. */
    private static final String INT_MEAN = "IntMean";
    /** Long mean implementation. */
    private static final String LONG_MEAN = "LongMean";
    /** Sum using a long mean implementation. */
    private static final String LONG_SUM_MEAN = "LongSumMean";
    /** Sum using a BigInteger mean implementation. */
    private static final String BIG_INTEGER_SUM_MEAN = "BigIntegerSumMean";
    /** JDK Stream mean implementation. */
    private static final String STREAM_MEAN = "StreamMean";
    /** Commons Statistics Variance implementation. */
    private static final String DOUBLE_VAR = "DoubleVariance";
    /** Integer variance implementation. */
    private static final String INT_VAR = "IntVariance";
    /** Long variance implementation. */
    private static final String LONG_VAR = "LongVariance";
    /** Long variance implementation using Math.multiplyHigh. */
    private static final String LONG_VAR2 = "LongVariance2";

    /**
     * Source of array data.
     */
    @State(Scope.Benchmark)
    public static class DataSource {
        /** Data length. */
        @Param({"2", "1000"})
        private int length;

        /** Data. */
        private int[] data;

        /** Data as a double. */
        private double[] doubleData;

        /** Data as a long. */
        private long[] longData;

        /**
         * @return the data
         */
        public int[] getData() {
            return data;
        }

        /**
         * @return the data
         */
        public double[] getDoubleData() {
            return doubleData;
        }

        /**
         * @return the data
         */
        public long[] getLongData() {
            return longData;
        }

        /**
         * Create the data.
         * Data will be randomized per iteration.
         */
        @Setup(Level.Iteration)
        public void setup() {
            longData = RandomSource.XO_RO_SHI_RO_128_PP.create().longs(length).toArray();
            doubleData = Arrays.stream(longData).asDoubleStream().toArray();
            data = Arrays.stream(longData).mapToInt(x -> (int) x).toArray();
        }
    }

    /**
     * Source of a {@link IntConsumer} action.
     */
    @State(Scope.Benchmark)
    public static class IntActionSource {
        /** Name of the source. */
        @Param({DOUBLE_MEAN, INT_MEAN,
                // Disabled: Run-time ~ IntMean
                // LONG_SUM_MEAN
                DOUBLE_VAR, INT_VAR})
        private String name;

        /** The action. */
        private Supplier<IntStatistic> action;

        /**
         * @return the action
         */
        public IntStatistic getAction() {
            return action.get();
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if (DOUBLE_MEAN.equals(name)) {
                action = () -> {
                    final Mean m = Mean.create();
                    return createIntStatistic(m, m);
                };
            } else if (INT_MEAN.equals(name)) {
                action = () -> {
                    final IntMean m = IntMean.create();
                    return createIntStatistic(m, m);
                };
            } else if (LONG_SUM_MEAN.equals(name)) {
                action = () -> {
                    final LongSumMean m = new LongSumMean();
                    return createIntStatistic(m, m);
                };
            } else if (DOUBLE_VAR.equals(name)) {
                action = () -> {
                    final Variance m = Variance.create();
                    return createIntStatistic(m, m);
                };
            } else if (INT_VAR.equals(name)) {
                action = () -> {
                    final IntVariance m = IntVariance.create();
                    return createIntStatistic(m, m);
                };
            } else {
                throw new IllegalStateException("Unknown int action: " + name);
            }
        }

        /**
         * Creates the {@link IntStatistic}.
         *
         * @param c Consumer.
         * @param s Supplier.
         * @return the statistic
         */
        private static IntStatistic createIntStatistic(IntConsumer c, DoubleSupplier s) {
            return new IntStatistic() {
                @Override
                public void accept(int value) {
                    c.accept(value);
                }
                @Override
                public double getAsDouble() {
                    return s.getAsDouble();
                }
            };
        }

        /**
         * Creates the {@link IntStatistic}.
         *
         * @param c Consumer.
         * @param s Supplier.
         * @return the statistic
         */
        private static IntStatistic createIntStatistic(DoubleConsumer c, DoubleSupplier s) {
            return new IntStatistic() {
                @Override
                public void accept(int value) {
                    c.accept(value);
                }
                @Override
                public double getAsDouble() {
                    return s.getAsDouble();
                }
            };
        }
    }

    /**
     * Source of a {@link DoubleConsumer} action.
     */
    @State(Scope.Benchmark)
    public static class DoubleActionSource {
        /** Name of the source. */
        @Param({DOUBLE_MEAN, DOUBLE_VAR})
        private String name;

        /** The action. */
        private Supplier<DoubleStatistic> action;

        /**
         * @return the action
         */
        public DoubleStatistic getAction() {
            return action.get();
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if (DOUBLE_MEAN.equals(name)) {
                action = () -> {
                    final Mean m = Mean.create();
                    return createDoubleStatistic(m, m);
                };
            } else if (DOUBLE_VAR.equals(name)) {
                action = () -> {
                    final Variance m = Variance.create();
                    return createDoubleStatistic(m, m);
                };
            } else {
                throw new IllegalStateException("Unknown double action: " + name);
            }
        }

        /**
         * Creates the {@link DoubleStatistic}.
         *
         * <p>This method is here to provide parity when comparing actual instances
         * of {@link DoubleStatistic} with composed objects for the equivalent
         * int/long statistics.
         *
         * @param c Consumer.
         * @param s Supplier.
         * @return the statistic
         */
        private static DoubleStatistic createDoubleStatistic(DoubleConsumer c, DoubleSupplier s) {
            return new DoubleStatistic() {
                @Override
                public void accept(double value) {
                    c.accept(value);
                }
                @Override
                public double getAsDouble() {
                    return s.getAsDouble();
                }
            };
        }
    }

    /**
     * Source of a {@link LongConsumer} action.
     */
    @State(Scope.Benchmark)
    public static class LongActionSource {
        /** Name of the source. */
        @Param({DOUBLE_MEAN, LONG_MEAN, BIG_INTEGER_SUM_MEAN,
                DOUBLE_VAR, LONG_VAR, LONG_VAR2})
        private String name;

        /** The action. */
        private Supplier<LongStatistic> action;

        /**
         * @return the action
         */
        public LongStatistic getAction() {
            return action.get();
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if (DOUBLE_MEAN.equals(name)) {
                action = () -> {
                    final Mean m = Mean.create();
                    return createLongStatistic(m, m);
                };
            } else if (LONG_MEAN.equals(name)) {
                action = () -> {
                    final LongMean m = LongMean.create();
                    return createLongStatistic(m, m);
                };
            } else if (BIG_INTEGER_SUM_MEAN.equals(name)) {
                action = () -> {
                    final BigIntegerSumMean m = new BigIntegerSumMean();
                    return createLongStatistic(m, m);
                };
            } else if (DOUBLE_VAR.equals(name)) {
                action = () -> {
                    final Variance m = Variance.create();
                    return createLongStatistic(m, m);
                };
            } else if (LONG_VAR.equals(name)) {
                action = () -> {
                    final LongVariance m = LongVariance.create();
                    return createLongStatistic(m, m);
                };
            } else if (LONG_VAR2.equals(name)) {
                action = () -> {
                    final LongVariance2 m = LongVariance2.create();
                    return createLongStatistic(m, m);
                };
            } else {
                throw new IllegalStateException("Unknown long action: " + name);
            }
        }

        /**
         * Creates the {@link LongStatistic}.
         *
         * @param c Consumer.
         * @param s Supplier.
         * @return the statistic
         */
        private static LongStatistic createLongStatistic(LongConsumer c, DoubleSupplier s) {
            return new LongStatistic() {
                @Override
                public void accept(long value) {
                    c.accept(value);
                }
                @Override
                public double getAsDouble() {
                    return s.getAsDouble();
                }
            };
        }

        /**
         * Creates the {@link LongStatistic}.
         *
         * @param c Consumer.
         * @param s Supplier.
         * @return the statistic
         */
        private static LongStatistic createLongStatistic(DoubleConsumer c, DoubleSupplier s) {
            return new LongStatistic() {
                @Override
                public void accept(long value) {
                    c.accept(value);
                }
                @Override
                public double getAsDouble() {
                    return s.getAsDouble();
                }
            };
        }
    }

    /**
     * Source of a {@link ToDoubleFunction} for a {@code int[]}.
     */
    @State(Scope.Benchmark)
    public static class IntFunctionSource {
        /** Name of the source. */
        @Param({INT_MEAN,
            // Disabled: Run-time ~ IntMean
            //LONG_SUM_MEAN,
            STREAM_MEAN, INT_VAR})
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
        @Setup(Level.Iteration)
        public void setup() {
            if (INT_MEAN.equals(name)) {
                function = x -> IntMean.of(x).getAsDouble();
            } else if (LONG_SUM_MEAN.equals(name)) {
                function = LongSumMean::mean;
            } else if (STREAM_MEAN.equals(name)) {
                function = x -> Arrays.stream(x).average().orElse(Double.NaN);
            } else if (INT_VAR.equals(name)) {
                function = x -> IntVariance.of(x).getAsDouble();
            } else {
                throw new IllegalStateException("Unknown int function: " + name);
            }
        }
    }

    /**
     * Source of a {@link ToDoubleFunction} for a {@code double[]}.
     */
    @State(Scope.Benchmark)
    public static class DoubleFunctionSource {
        /** Name of the source. */
        @Param({DOUBLE_MEAN, DOUBLE_VAR})
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
        @Setup(Level.Iteration)
        public void setup() {
            if (DOUBLE_MEAN.equals(name)) {
                function = x -> Mean.of(x).getAsDouble();
            } else if (DOUBLE_VAR.equals(name)) {
                function = x -> Variance.of(x).getAsDouble();
            } else {
                throw new IllegalStateException("Unknown double function: " + name);
            }
        }
    }

    /**
     * Source of a {@link ToDoubleFunction} for a {@code long[]}.
     */
    @State(Scope.Benchmark)
    public static class LongFunctionSource {
        /** Name of the source. */
        @Param({LONG_MEAN, BIG_INTEGER_SUM_MEAN, LONG_VAR, LONG_VAR2})
        private String name;

        /** The action. */
        private ToDoubleFunction<long[]> function;

        /**
         * @return the function
         */
        public ToDoubleFunction<long[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if (LONG_MEAN.equals(name)) {
                function = x -> LongMean.of(x).getAsDouble();
            } else if (BIG_INTEGER_SUM_MEAN.equals(name)) {
                function = BigIntegerSumMean::mean;
            } else if (LONG_VAR.equals(name)) {
                function = x -> LongVariance.of(x).getAsDouble();
            } else if (LONG_VAR2.equals(name)) {
                function = x -> LongVariance2.of(x).getAsDouble();
            } else {
                throw new IllegalStateException("Unknown long function: " + name);
            }
        }
    }

    /**
     * Class containing the variance data.
     */
    static class IntVarianceData {
        /** Sum of the squared values. */
        private final UInt128 sumSq;
        /** Sum of the values. */
        private final Int128 sum;
        /** Count of values that have been added. */
        private long n;

        /**
         * @param sumSq Sum of the squared values.
         * @param sum Sum of the values.
         * @param n Count of values that have been added.
         */
        IntVarianceData(UInt128 sumSq, Int128 sum, long n) {
            this.sumSq = sumSq;
            this.sum = sum;
            this.n = n;
        }

        /**
         * @return the sum of the squared values
         */
        UInt128 getSumSq() {
            return new UInt128(sumSq.hi64(), sumSq.lo64());
        }

        /**
         * @return the sum
         */
        Int128 getSum() {
            return new Int128(sum.hi64(), sum.lo64());
        }

        /**
         * @return the count of values that have been added
         */
        long getN() {
            return n;
        }

        /**
         * @return the copy
         */
        IntVarianceData copy() {
            return new IntVarianceData(getSumSq(), getSum(), n);
        }

        /**
         * Adds the other instance.
         *
         * @param other the other
         * @return this instance
         */
        IntVarianceData add(IntVarianceData other) {
            // Prevent the data from becoming too large
            n = Math.addExact(n, other.n);
            sumSq.add(other.sumSq);
            sum.add(other.sum);
            return this;
        }
    }

    /**
     * Source of {@code int} variance data.
     *
     * <p>This class generates a pool of variance data from a random sample of integers
     * in a range. The pool objects are then combined with each other for a given number of
     * rounds, effectively doubling the size of pool objects each round.
     * Using the defaults will create objects in the pool of:
     * <pre>
     * E[ sum(x) ] = (511 / 2) mean value * (95 / 2) mean samples ~ 12136.25 ~ 2^8 * 2^5.5 {@code < 2^14}
     * E[ sum(x^2) = ((511 / 2)^2 mean value^2) * (95 / 2) mean samples ~ 3100811.875 ~ 2^16 * 2^5.5 {@code < 2^22}
     * Max[ sum(x) = 511 * 63 = 32193 {@code < 2^15}
     * Max[ sum(x^2) = 15^2 * 63 = 14175 {@code < 2^16}
     * man[ n ] = 63 {@code < 2^6}
     * </pre>
     * <p>The objects from this pool can be added together a maximum of 56 times before n overflows.
     * The sum of the values will overflow a long at approximately 18 combines.
     */
    @State(Scope.Benchmark)
    public static class IntVarianceDataSource {
        /** Consistent seed. */
        private static final Long SEED = ThreadLocalRandom.current().nextLong();
        /** Lower limit. */
        @Param({"0"})
        private int origin;
        /** Upper limit. */
        @Param({"512"})
        private int bound;
        /** Minimum samples. */
        @Param({"32"})
        private int minSamples;
        /** Maximum samples. */
        @Param({"64"})
        private int maxSamples;
        /** Pool size. */
        @Param({"64"})
        private int poolSize;
        /** Number of combine operations. */
        @Param({"8", "16", "24", "32", "48"})
        private int combine;

        /** Data. */
        private IntVarianceData[] data;

        /**
         * The number of data values.
         *
         * @return the size
         */
        public int size() {
            return data.length;
        }
        /**
         * Get a copy of the data for the specified index.
         *
         * @param i Index.
         * @return the data
         */
        public IntVarianceData getData(int i) {
            return data[i].copy();
        }

        /**
         * Create the data.
         */
        @Setup
        public void setup() {
            // Consistent seed so the same data is provided to all methods
            final UniformRandomProvider rng = RandomSource.XO_SHI_RO_512_SS.create(SEED);
            // Initial pool
            final IntVarianceData[] pool = new IntVarianceData[poolSize];
            for (int i = 0; i < pool.length; i++) {
                final int n = rng.nextInt(minSamples, maxSamples);
                final UInt128 sumSq = UInt128.create();
                final Int128 sum = Int128.create();
                rng.ints(n, origin, bound).forEach(x -> {
                    sumSq.addPositive((long) x * x);
                    sum.add(x);
                });
                pool[i] = new IntVarianceData(sumSq, sum, n);
            }
            // Combine to grow the average size of the pool objects
            for (int round = 0; round < combine; round++) {
                final IntVarianceData[] last = pool.clone();
                for (int i = 0; i < pool.length; i++) {
                    // Copy the instance that will be the LHS of the add operation
                    pool[i] = last[i].copy().add(last[rng.nextInt(poolSize)]);
                }
            }
            data = pool;
        }
    }

    /**
     * Source of a {@link ToDoubleFunction} for a {@code IntVarianceData}.
     */
    @State(Scope.Benchmark)
    public static class IntVarianceFunctionSource {
        /** {@link MathContext} with 20 digits of precision. */
        private static final MathContext MC_20_DIGITS = new MathContext(20);

        /** Name of the source. */
        @Param({"DD", "DD2", "BigIntegerPow", "BigIntegerMultiply",
            "SumSquareBigInteger", "SumSquareMultiplyBigInteger",
            "UIntBigInteger", "UIntDD", "UIntDD2", "UIntBigInteger2", "UIntBigInteger3",
            "UIntDouble",
            // Very slow
            //"UIntBigFraction", "UIntBigDecimal"
        })
        private String name;

        /** The action. */
        private ToDoubleFunction<IntVarianceData> function;

        /**
         * @return the function
         */
        public ToDoubleFunction<IntVarianceData> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if ("DD".equals(name)) {
                function = IntVarianceFunctionSource::varianceDD;
            } else if ("DD2".equals(name)) {
                function = IntVarianceFunctionSource::varianceDD2;
            } else if ("BigIntegerPow".equals(name)) {
                function = IntVarianceFunctionSource::varianceBigIntegerPow;
            } else if ("BigIntegerMultiply".equals(name)) {
                function = IntVarianceFunctionSource::varianceBigIntegerMultiply;
            } else if ("SumSquareBigInteger".equals(name)) {
                function = IntVarianceFunctionSource::varianceSumSquareBigInteger;
            } else if ("SumSquareMultiplyBigInteger".equals(name)) {
                function = IntVarianceFunctionSource::varianceSumSquareMultiplyIntBigInteger;
            } else if ("UIntBigInteger".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntBigInteger;
            } else if ("UIntDD".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntDD;
            } else if ("UIntDD2".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntDD2;
            } else if ("UIntBigInteger2".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntBigInteger2;
            } else if ("UIntBigInteger3".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntBigInteger3;
            } else if ("UIntDouble".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntDouble;
            } else if ("UIntBigFraction".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntBigFraction;
            } else if ("UIntBigDecimal".equals(name)) {
                function = IntVarianceFunctionSource::varianceUIntBigDecimal;
            } else {
                throw new IllegalStateException("Unknown int variance function: " + name);
            }
        }

        /**
         * Convenience method to square a BigInteger.
         *
         * @param x Value
         * @return x^2
         */
        private static BigInteger square(BigInteger x) {
            return x.multiply(x);
        }

        /**
         * Compute the variance using double-double arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceDD(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            final DD diff = sumSq.toDD().multiply(n).subtract(sum.toDD().square());
            if (diff.hi() < 0) {
                return 0;
            }
            // Divisor is an exact double
            if (n < (1L << 26)) {
                // n0*n is safe as a long
                return diff.divide(n0 * n).doubleValue();
            }
            return diff.divide(DD.of(n).multiply(DD.of(n0))).doubleValue();
        }

        /**
         * Compute the variance using double-double arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceDD2(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations: sum(x^2) - sum(x)^2 / n
            final DD ss = sumSq.toDD().subtract(sum.toDD().square().divide(n));
            if (ss.hi() < 0) {
                return 0;
            }
            return ss.divide(n0).doubleValue();
        }

        /**
         * Compute the variance using BigInteger arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceBigIntegerPow(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            final BigInteger diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n))
                .subtract(sum.toBigInteger().pow(2));
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using BigInteger arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceBigIntegerMultiply(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            final BigInteger diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n))
                .subtract(square(sum.toBigInteger()));
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using Int128 and BigInteger arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceSumSquareBigInteger(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the second term if possible using fast integer arithmetic.
            final BigInteger term1 = sumSq.toBigInteger().multiply(BigInteger.valueOf(n));
            final BigInteger term2 = sum.hi64() == 0 ? sum.squareLow().toBigInteger() : square(sum.toBigInteger());
            final BigInteger diff = term1.subtract(term2);
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using UInt128/Int128 and BigInteger arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceSumSquareMultiplyIntBigInteger(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // sum(x^2) * n will be OK when n < 2^32.
            final BigInteger term1 = n < 1L << 32 ? sumSq.unsignedMultiply((int) n).toBigInteger() :
                sumSq.toBigInteger().multiply(BigInteger.valueOf(n));
            final BigInteger term2 = sum.hi64() == 0 ? sum.squareLow().toBigInteger() : square(sum.toBigInteger());
            final BigInteger diff = term1.subtract(term2);
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using UInt128/Int128 and BigInteger arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntBigInteger(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // 128-bit sum(x^2) * n will be OK when the upper 32-bits are zero.
            // 128-bit sum(x)^2 will be OK when the upper 64-bits are zero.
            // Both are safe when n < 2^32.
            BigInteger diff;
            if ((n >>> Integer.SIZE) == 0) {
                diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toBigInteger();
            } else {
                // It may still be possible to compute the square
                BigInteger sum2;
                if (sum.hi64() == 0) {
                    sum2 = sum.squareLow().toBigInteger();
                } else {
                    sum2 = sum.toBigInteger();
                    sum2 = sum2.multiply(sum2);
                }
                diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n)).subtract(sum2);
            }
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using UInt128/Int128 and DD arithmetic.
         * The final divide uses double precision.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntDD(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // sum(x^2) * n will be OK when the upper 32-bits are zero.
            // Both are safe when n < 2^32.
            if ((n >>> Integer.SIZE) == 0) {
                DD diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toDD();
                // Divisor is an exact double
                if (n < (1L << 26)) {
                    // n0*n is safe as a long
                    return diff.divide(n0 * n).doubleValue();
                }
                return diff.divide(DD.of(n).multiply(DD.of(n0))).doubleValue();
            }
            BigInteger diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n)).subtract(square(sum.toBigInteger()));
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using UInt128/Int128 and DD arithmetic.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntDD2(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // sum(x^2) * n will be OK when the upper 32-bits are zero.
            // Both are safe when n < 2^32.
            if ((n >>> Integer.SIZE) == 0) {
                DD diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toDD();
                // Divisor is an exact double
                if (n < (1L << 26)) {
                    // n0*n is safe as a long
                    return diff.divide(n0 * n).doubleValue();
                }
                return diff.divide(DD.of(n).multiply(DD.of(n0))).doubleValue();
            }
            BigInteger diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n)).subtract(square(sum.toBigInteger()));
            // Assume n is big to overflow the sum(x)
            // Compute the divide in double-double precision
            return DD.of(diff.doubleValue()).divide(DD.of(n).multiply(DD.of(n0))).doubleValue();
        }

        /**
         * Compute the variance using unsigned integer (UInt128/Int128 or BigInteger) arithmetic.
         * The final divide uses double precision.
         *
         * <p>Note: This is similar to {@link #varianceUIntBigInteger(IntVarianceData)} but does
         * not fast compute the squared sum. This benchmarks as faster: the BigInteger multiply
         * on small values for sum(x)^2 is efficient.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntBigInteger2(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // 128-bit sum(x^2) * n will be OK when the upper 32-bits are zero.
            // 128-bit sum(x)^2 will be OK when the upper 64-bits are zero.
            // Both are safe when n < 2^32.
            BigInteger diff;
            if ((n >>> Integer.SIZE) == 0) {
                diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toBigInteger();
            } else {
                diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n)).subtract(square(sum.toBigInteger()));
            }
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using unsigned integer (UInt128/Int128 or BigInteger) arithmetic.
         * The final divide uses double precision.
         *
         * <p>Note: This is similar to {@link #varianceUIntBigInteger(IntVarianceData)} but does
         * computes the squared sum in Int128. This benchmarks slower than converting to BigInteger
         * and computing the square.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntBigInteger3(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // 128-bit sum(x^2) * n will be OK when the upper 32-bits are zero.
            // 128-bit sum(x)^2 will be OK when the upper 64-bits are zero.
            // Both are safe when n < 2^32.
            BigInteger diff;
            if ((n >>> Integer.SIZE) == 0) {
                diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toBigInteger();
            } else {
                diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n)).subtract(sum.square());
            }
            // Compute the divide in double precision
            return diff.doubleValue() / ((double) n0 * n);
        }

        /**
         * Compute the variance using unsigned integer (UInt128/Int128 or BigInteger) arithmetic.
         * The final divide uses double precision.
         *
         * <p>Note: This is similar to {@link #varianceUIntBigInteger(IntVarianceData)} but does
         * not fast compute the squared sum. This benchmarks as faster: the BigInteger multiply
         * on small values for sum(x)^2 is efficient.
         *
         * <p>This method uses the {@link UInt128#toDouble()} to avoid going via BigInteger.
         * The divisor is computed in extended precision.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntDouble(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // 128-bit sum(x^2) * n will be OK when the upper 32-bits are zero.
            // 128-bit sum(x)^2 will be OK when the upper 64-bits are zero.
            // Both are safe when n < 2^32.
            double diff;
            if ((n >>> Integer.SIZE) == 0) {
                diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toDouble();
            } else {
                diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n))
                    .subtract(square(sum.toBigInteger())).doubleValue();
            }
            // Compute the divide in double precision
            return diff / IntMath.unsignedMultiplyToDouble(n, n0);
        }

        /**
         * Compute the variance using unsigned integer (UInt128/Int128 or BigInteger) arithmetic.
         * The final divide uses double precision.
         *
         * <p>Note: This is similar to {@link #varianceUIntBigInteger(IntVarianceData)} but does
         * not fast compute the squared sum. This benchmarks as faster: the BigInteger multiply
         * on small values for sum(x)^2 is efficient.
         *
         * <p>The final divide uses BigFraction for large size, or double.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntBigFraction(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // 128-bit sum(x^2) * n will be OK when the upper 32-bits are zero.
            // 128-bit sum(x)^2 will be OK when the upper 64-bits are zero.
            // Both are safe when n < 2^32.
            BigInteger diff;
            if ((n >>> Integer.SIZE) == 0) {
                diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toBigInteger();
            } else {
                diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n)).subtract(square(sum.toBigInteger()));
            }
            if (n < (1L << 26)) {
                // Compute the divide in double precision
                return diff.doubleValue() / ((double) n0 * n);
            }
            return BigFraction.of(diff, BigInteger.valueOf(n0).multiply(BigInteger.valueOf(n)))
                .doubleValue();
        }

        /**
         * Compute the variance using unsigned integer (UInt128/Int128 or BigInteger) arithmetic.
         * The final divide uses double precision.
         *
         * <p>Note: This is similar to {@link #varianceUIntBigInteger(IntVarianceData)} but does
         * not fast compute the squared sum. This benchmarks as faster: the BigInteger multiply
         * on small values for sum(x)^2 is efficient.
         *
         * <p>The final divide uses BigDecimal for large size, or double.
         *
         * @param data Variance data.
         * @return the variance
         */
        static double varianceUIntBigDecimal(IntVarianceData data) {
            final long n = data.getN();
            if (n == 0) {
                return Double.NaN;
            }
            // Avoid a divide by zero
            if (n == 1) {
                return 0;
            }
            final UInt128 sumSq = data.getSumSq();
            final Int128 sum = data.getSum();
            // Assume unbiased
            final long n0 = n - 1;
            // Extended precision.
            // Sum-of-squared deviations precursor: n * sum(x^2) - sum(x)^2
            // Compute the term if possible using fast integer arithmetic.
            // 128-bit sum(x^2) * n will be OK when the upper 32-bits are zero.
            // 128-bit sum(x)^2 will be OK when the upper 64-bits are zero.
            // Both are safe when n < 2^32.
            BigInteger diff;
            if ((n >>> Integer.SIZE) == 0) {
                diff = sumSq.unsignedMultiply((int) n).subtract(sum.squareLow()).toBigInteger();
            } else {
                diff = sumSq.toBigInteger().multiply(BigInteger.valueOf(n)).subtract(square(sum.toBigInteger()));
            }
            if (n < (1L << 26)) {
                // Compute the divide in double precision
                return diff.doubleValue() / ((double) n0 * n);
            }
            return new BigDecimal(diff).divide(new BigDecimal(
                BigInteger.valueOf(n0).multiply(BigInteger.valueOf(n))), MC_20_DIGITS)
                .doubleValue();
        }
    }

    /**
     * Source of {@code long} array data.
     * The data is designed to overflow a sum as a long with a specified frequency.
     * There are 3 cases: positive values; negative values; any sign. The amount
     * of overflow is controlled using a shift to remove magnitude. No shift expects
     * overflow 50% of the time when summing same sign values. If both signs are used then the
     * random walk will be based around 0 with overflow occurring proportional to
     * the magnitude. Chance of overflow will rapidly drop when the values are not full
     * magnitude numbers.
     */
    @State(Scope.Benchmark)
    public static class LongDataSource {
        /** Data length: 2^10. If shift is above 10 then no overflow will occur. */
        @Param({"1024"})
        private int length;
        /** Data sign. */
        @Param({"positive", "negative", "both"})
        private String sign;
        /** Data bit shift. */
        @Param({"0", "1", "2", "4", "8", "16"})
        private int shift;

        /** Data. */
        private long[] data;

        /**
         * @return the data
         */
        public long[] getData() {
            return data;
        }

        /**
         * Create the data.
         * Data will be randomized per iteration.
         */
        @Setup(Level.Iteration)
        public void setup() {
            LongStream s = RandomSource.XO_RO_SHI_RO_128_PP.create().longs(length);
            if ("positive".equals(sign)) {
                s = s.map(x -> x >>> 1);
            } else if ("negative".equals(sign)) {
                s = s.map(x -> x | Long.MIN_VALUE);
            } else if (!"both".equals(sign)) {
                throw new IllegalStateException("Unknown sign: " + sign);
            }
            if (shift > 0) {
                final int bits = shift;
                // Signed shift maintains negative values
                s = s.map(x -> x >> bits);
            }
            data = s.toArray();
        }
    }

    /**
     * Source of a {@link ToLongFunction} for a {@code long[]}.
     */
    @State(Scope.Benchmark)
    public static class LongSumFunctionSource {
        /** Name of the source.
         * The branchless 128bitAdd2 runs at constant speed but is slower than 128bitAdd. */
        @Param({"128bitAdd", "128bitAdd2", "64bitSum"})
        private String name;

        /** The action. */
        private ToLongFunction<long[]> function;

        /**
         * @return the function
         */
        public ToLongFunction<long[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if ("128bitAdd".equals(name)) {
                function = x -> {
                    final Int128 s = Int128.create();
                    for (long y : x) {
                        s.add(y);
                    }
                    return s.hi64();
                };
            } else if ("128bitAdd2".equals(name)) {
                function = x -> {
                    final Int128 s = Int128.create();
                    for (long y : x) {
                        s.add2(y);
                    }
                    return s.hi64();
                };
            } else if ("64bitSum".equals(name)) {
                function = x -> {
                    long s = 0;
                    for (long y : x) {
                        s += y;
                    }
                    return s;
                };
            } else {
                throw new IllegalStateException("Unknown long sum function: " + name);
            }
        }
    }

    /**
     * Source of {@code long} array data to multiply as unsigned pairs.
     * Magnitude is approximately controlled using a bit shift on the values.
     */
    @State(Scope.Benchmark)
    public static class MultiplyLongDataSource {
        /** Data length. */
        @Param({"1024"})
        private int length;
        /** Data bit shift. */
        @Param({"0", "33"})
        private int shift;

        /** Data. */
        private long[] data;

        /**
         * @return the data
         */
        public long[] getData() {
            return data;
        }

        /**
         * Create the data.
         * Data will be randomized per iteration.
         */
        @Setup(Level.Iteration)
        public void setup() {
            LongStream s = RandomSource.XO_RO_SHI_RO_128_PP.create().longs(length * 2L);
            if (shift > 0) {
                final int bits = shift;
                s = s.map(x -> x >>> bits);
            }
            data = s.toArray();
        }
    }

    /**
     * Source of a {@link ToDoubleFunction} for a {@code long[]}.
     */
    @State(Scope.Benchmark)
    public static class MultiplyLongFunctionSource {
        /** Name of the source. */
        @Param({"double", "unsignedMultiplyToDoubleBigInteger", "unsignedMultiplyToDouble"})
        private String name;

        /** The action. */
        private ToDoubleFunction<long[]> function;

        /**
         * Function for two long arguments.
         */
        interface LongLongToDoubleFunction {
            /**
             * Apply the function.
             *
             * @param a Value.
             * @param b Value.
             * @return the result
             */
            double apply(long a, long b);
        }

        /**
         * @return the function
         */
        public ToDoubleFunction<long[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            final LongLongToDoubleFunction f = createFunction(name);
            function = x -> applyAll(x, f);
        }

        /**
         * Creates the function.
         *
         * @param functionName Function name.
         * @return the function
         */
        private LongLongToDoubleFunction createFunction(String functionName) {
            if ("double".equals(functionName)) {
                return (x, y) -> (double) x * y;
            } else if ("unsignedMultiplyToDoubleBigInteger".equals(name)) {
                return IntMath::unsignedMultiplyToDoubleBigInteger;
            } else if ("unsignedMultiplyToDouble".equals(name)) {
                return IntMath::unsignedMultiplyToDouble;
            } else {
                throw new IllegalStateException("Unknown multiply long function: " + name);
            }
        }

        /**
         * Apply the function to all pairs in the data.
         *
         * @param array Data.
         * @param f Function.
         * @return the result
         */
        private static double applyAll(long[] array, LongLongToDoubleFunction f) {
            double s = 0;
            for (int i = 0; i < array.length; i += 2) {
                s += f.apply(array[i], array[i + 1]);
            }
            return s;
        }
    }

    /**
     * A mean of {@code int} data using a {@code long} sum.
     */
    static class LongSumMean implements IntConsumer, DoubleSupplier {
        /** Count of values that have been added. */
        private long n;

        /** Sum of values that have been added. */
        private long s;

        @Override
        public void accept(int value) {
            s += value;
            n++;
        }

        @Override
        public double getAsDouble() {
            return (double) s / n;
        }

        /**
         * Compute the mean using a sum.
         *
         * @param data Data.
         * @return the mean
         */
        static double mean(int[] data) {
            long s = 0;
            for (final int x : data) {
                s += x;
            }
            return (double) s / data.length;
        }
    }

    /**
     * A mean of {@code long} data using a {@code BigInteger} sum.
     */
    static class BigIntegerSumMean implements LongConsumer, DoubleSupplier {
        /** Count of values that have been added. */
        private long n;

        /** Sum of values that have been added. */
        private BigInteger s = BigInteger.ZERO;

        @Override
        public void accept(long value) {
            s = s.add(BigInteger.valueOf(value));
            n++;
        }

        @Override
        public double getAsDouble() {
            return s.doubleValue() / n;
        }

        /**
         * Compute the mean using a sum.
         *
         * @param data Data.
         * @return the mean
         */
        static double mean(long[] data) {
            BigInteger s = BigInteger.ZERO;
            for (final long x : data) {
                s = s.add(BigInteger.valueOf(x));
            }
            return s.doubleValue() / data.length;
        }
    }

    /**
     * Apply the action to each {@code int} value.
     *
     * @param <T> the action type
     * @param action Action.
     * @param values Values.
     * @return the value
     */
    static <T extends IntConsumer & DoubleSupplier> double forEach(T action, int[] values) {
        for (final int x : values) {
            action.accept(x);
        }
        return action.getAsDouble();
    }

    /**
     * Apply the action to each {@code double} value.
     *
     * @param <T> the action type
     * @param action Action.
     * @param values Values.
     * @return the value
     */
    static <T extends DoubleConsumer & DoubleSupplier> double forEach(T action, double[] values) {
        for (final double x : values) {
            action.accept(x);
        }
        return action.getAsDouble();
    }

    /**
     * Apply the action to each {@code long} value.
     *
     * @param <T> the action type
     * @param action Action.
     * @param values Values.
     * @return the value
     */
    static <T extends LongConsumer & DoubleSupplier> double forEach(T action, long[] values) {
        for (final long x : values) {
            action.accept(x);
        }
        return action.getAsDouble();
    }

    /**
     * Create the statistic using a consumer of {@code int} values.
     *
     * @param action Source of the data action.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double forEachIntStatistic(IntActionSource action, DataSource source) {
        return forEach(action.getAction(), source.getData());
    }

    /**
     * Create the statistic using a consumer of {@code double} values.
     *
     * @param action Source of the data action.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double forEachDoubleStatistic(DoubleActionSource action, DataSource source) {
        return forEach(action.getAction(), source.getDoubleData());
    }

    /**
     * Create the statistic using a consumer of {@code long} values.
     *
     * @param action Source of the data action.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double forEachLongStatistic(LongActionSource action, DataSource source) {
        return forEach(action.getAction(), source.getLongData());
    }

    /**
     * Create the statistic using a {@code int[]} function.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double arrayIntStatistic(IntFunctionSource function, DataSource source) {
        return function.getFunction().applyAsDouble(source.getData());
    }

    /**
     * Create the statistic using a {@code double[]} function.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double arrayDoubleStatistic(DoubleFunctionSource function, DataSource source) {
        return function.getFunction().applyAsDouble(source.getDoubleData());
    }

    /**
     * Create the statistic using a {@code long[]} function.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public double arrayLongStatistic(LongFunctionSource function, DataSource source) {
        return function.getFunction().applyAsDouble(source.getLongData());
    }

    /**
     * Create the variance using a aggregated {@code int[]} data.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void intVariance(IntVarianceFunctionSource function, IntVarianceDataSource source, Blackhole bh) {
        final int size = source.size();
        final ToDoubleFunction<IntVarianceData> f = function.getFunction();
        for (int i = 0; i < size; i++) {
            bh.consume(f.applyAsDouble(source.getData(i)));
        }
    }

    /**
     * Create the sum using a {@code long[]} function.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @return the sum
     */
    @Benchmark
    public long longSum(LongSumFunctionSource function, LongDataSource source) {
        return function.getFunction().applyAsLong(source.getData());
    }

    /**
     * Create the product using a {@code long[]} function.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @return the sum
     */
    @Benchmark
    public double multiplyToDouble(MultiplyLongFunctionSource function, MultiplyLongDataSource source) {
        return function.getFunction().applyAsDouble(source.getData());
    }
}
