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
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.numbers.core.Sum;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.Mean;
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
 * Executes a benchmark of the moment-based statistics.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class MomentPerformance {
    /** Commons Statistics Mean implementation. */
    private static final String MEAN = "Mean";
    /** Summation mean implementation. */
    private static final String SUM_MEAN = "SumMean";
    /** Extended precision summation mean implementation. */
    private static final String EXTENDED_SUM_MEAN = "ExtendedSumMean";
    /** Rolling mean implementation. */
    private static final String ROLLING_MEAN = "RollingMean";
    /** Safe rolling mean implementation. */
    private static final String SAFE_ROLLING_MEAN = "SafeRollingMean";
    /** Safe rolling mean implementation. */
    private static final String SCALED_ROLLING_MEAN = "ScaledRollingMean";
    /** Inline rolling mean implementation for array-based creation. */
    private static final String INLINE_ROLLING_MEAN = "InlineRollingMean";
    /** Inline safe rolling mean implementation for array-based creation. */
    private static final String INLINE_SAFE_ROLLING_MEAN = "InlineSafeRollingMean";
    /** Inline safe rolling mean implementation with extended precision for array-based creation. */
    private static final String INLINE_SAFE_ROLLING_MEAN_EXT = "InlineSafeRollingMeanExt";

    /**
     * Source of {@code double} array data.
     */
    @State(Scope.Benchmark)
    public static class DataSource {
        /** Data length. */
        @Param({"1", "10", "1000"})
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
     * Source of a {@link DoubleConsumer} action.
     */
    @State(Scope.Benchmark)
    public static class ActionSource {
        /** Name of the source. */
        @Param({MEAN, ROLLING_MEAN, SAFE_ROLLING_MEAN, SCALED_ROLLING_MEAN, SUM_MEAN, EXTENDED_SUM_MEAN})
        private String name;

        /** The action. */
        private Supplier<DoubleConsumer> action;

        /**
         * @return the action
         */
        public DoubleConsumer getAction() {
            return action.get();
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if (MEAN.equals(name)) {
                action = Mean::create;
            } else if (ROLLING_MEAN.equals(name)) {
                action = RollingFirstMoment::new;
            } else if (SAFE_ROLLING_MEAN.equals(name)) {
                action = SafeRollingFirstMoment::new;
            } else if (SCALED_ROLLING_MEAN.equals(name)) {
                action = ScaledRollingFirstMoment::new;
            } else if (SUM_MEAN.equals(name)) {
                action = SumFirstMoment::new;
            } else if (EXTENDED_SUM_MEAN.equals(name)) {
                action = ExtendedSumFirstMoment::new;
            } else {
                throw new IllegalStateException("Unknown action: " + name);
            }
        }
    }

    /**
     * Source of a {@link Function} for a {@code double[]}.
     */
    @State(Scope.Benchmark)
    public static class FunctionSource {
        /** Name of the source. */
        @Param({MEAN, ROLLING_MEAN, SAFE_ROLLING_MEAN, SCALED_ROLLING_MEAN,
            INLINE_SAFE_ROLLING_MEAN, INLINE_SAFE_ROLLING_MEAN_EXT,
            SUM_MEAN, EXTENDED_SUM_MEAN, "DDMean",
            // Same speed as the ROLLING_MEAN, i.e. the DoubleConsumer is not an overhead
            //INLINE_ROLLING_MEAN
            // Higher moments
            "SumOfCubed", "SumOfCubedPow",
            "SumOfFourth", "SumOfFourthPow",
        })
        private String name;

        /** The action. */
        private Function<double[], Object> function;

        /**
         * @return the function
         */
        public Function<double[], Object> getFunction() {
            return function;
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if (MEAN.equals(name)) {
                function = Mean::of;
            } else if (ROLLING_MEAN.equals(name)) {
                function = MomentPerformance::arrayRollingFirstMoment;
            } else if (SAFE_ROLLING_MEAN.equals(name)) {
                function = MomentPerformance::arraySafeRollingFirstMoment;
            } else if (SCALED_ROLLING_MEAN.equals(name)) {
                function = MomentPerformance::arrayScaledRollingFirstMoment;
            } else if (INLINE_ROLLING_MEAN.equals(name)) {
                function = MomentPerformance::arrayInlineRollingFirstMoment;
            } else if (INLINE_SAFE_ROLLING_MEAN.equals(name)) {
                function = MomentPerformance::arrayInlineSafeRollingFirstMoment;
            } else if (INLINE_SAFE_ROLLING_MEAN_EXT.equals(name)) {
                function = MomentPerformance::arrayInlineSafeRollingFirstMomentExt;
            } else if (SUM_MEAN.equals(name)) {
                function = MomentPerformance::arraySumMean;
            } else if (EXTENDED_SUM_MEAN.equals(name)) {
                // This returns the IEEE result for non-finite input, or overflow
                function = x -> Sum.of(x).getAsDouble() / x.length;
            } else if ("DDMean".equals(name)) {
                function = MomentPerformance::arrayDDSumMean;
            } else if ("SumOfCubed".equals(name)) {
                function = MomentPerformance::arraySumOfCubed;
            } else if ("SumOfCubedPow".equals(name)) {
                function = MomentPerformance::arraySumOfCubedPow;
            } else if ("SumOfFourth".equals(name)) {
                function = MomentPerformance::arraySumOfFourth;
            } else if ("SumOfFourthPow".equals(name)) {
                function = MomentPerformance::arraySumOfFourthPow;
            } else {
                throw new IllegalStateException("Unknown function: " + name);
            }
        }
    }

    /**
     * A rolling first raw moment of {@code double} data.
     */
    static class RollingFirstMoment implements DoubleConsumer, DoubleSupplier {
        /** Count of values that have been added. */
        private long n;

        /** First moment of values that have been added. */
        private double m1;

        @Override
        public void accept(double value) {
            m1 += (value - m1) / ++n;
        }

        @Override
        public double getAsDouble() {
            // NaN for all non-finite results
            return Double.isFinite(m1) && n != 0 ? m1 : Double.NaN;
        }
    }

    /**
     * A rolling first raw moment of {@code double} data safe to overflow of any finite
     * values (e.g. [MAX_VALUE, -MAX_VALUE]).
     */
    static class SafeRollingFirstMoment implements DoubleConsumer, DoubleSupplier {
        /** Count of values that have been added. */
        private long n;

        /** First moment of values that have been added. */
        private double m1;

        @Override
        public void accept(double value) {
            m1 += ((value * 0.5 - m1 * 0.5) / ++n) * 2;
        }

        @Override
        public double getAsDouble() {
            // NaN for all non-finite results
            return Double.isFinite(m1) && n != 0 ? m1 : Double.NaN;
        }
    }

    /**
     * A rolling first raw moment of {@code double} data safe to overflow of any finite
     * values (e.g. [MAX_VALUE, -MAX_VALUE]).
     */
    static class ScaledRollingFirstMoment implements DoubleConsumer, DoubleSupplier {
        /** Count of values that have been added. */
        private long n;

        /** First moment of values that have been added. */
        private double m1;

        @Override
        public void accept(double value) {
            m1 += (value * 0.5 - m1) / ++n;
        }

        @Override
        public double getAsDouble() {
            // NaN for all non-finite results
            return n != 0 && Double.isFinite(m1 * 2) ? m1 * 2 : Double.NaN;
        }
    }

    /**
     * A mean using a sum.
     */
    static class SumFirstMoment implements DoubleConsumer, DoubleSupplier {
        /** Count of values that have been added. */
        private long n;

        /** Sum of values that have been added. */
        private double sum;

        @Override
        public void accept(double value) {
            n++;
            sum += value;
        }

        @Override
        public double getAsDouble() {
            return sum / n;
        }
    }

    /**
     * A mean using an extended precision sum.
     *
     * <p>This type of summation is used in DoubleStream to compute the sum and derive the
     * mean. This method acts as a proxy to compare the speed of the rolling algorithm to
     * collect a stream verses a high-precision sum using
     * {@link java.util.stream.DoubleStream#sum()}.
     */
    static class ExtendedSumFirstMoment implements DoubleConsumer, DoubleSupplier {
        /** Count of values that have been added. */
        private long n;

        /** Sum of values that have been added. */
        private double sum;
        /** A running compensation for lost low-order bits. */
        private double c;

        @Override
        public void accept(double value) {
            n++;
            // Kahan summation
            // https://en.wikipedia.org/wiki/Kahan_summation_algorithm
            final double y = value - c;
            final double t = sum + y;
            c = (t - sum) - y;
            sum = t;
        }

        @Override
        public double getAsDouble() {
            return sum / n;
        }
    }

    /**
     * Apply the action to each value.
     *
     * @param <T> the action type
     * @param action Action.
     * @param values Values.
     * @return the action
     */
    static <T extends DoubleConsumer> T forEach(T action, double[] values) {
        for (final double x : values) {
            action.accept(x);
        }
        return action;
    }

    /**
     * Correct the mean using a second pass over the data.
     *
     * @param data Data.
     * @param xbar Current mean.
     * @return the mean
     */
    private static double correctMean(double[] data, double xbar) {
        double correction = 0;
        for (final double x : data) {
            correction += x - xbar;
        }
        // Note: Correction may be infinite
        if (Double.isFinite(correction)) {
            return xbar + correction / data.length;
        }
        return xbar;
    }

    /**
     * Correct the mean using a second pass over the data.
     *
     * @param data Data.
     * @param xbar Current mean.
     * @return the mean
     */
    private static double correctMeanKahan(double[] data, double xbar) {
        // Second pass (Kahan summation)
        double correction = 0;
        double c = 0;
        for (final double x : data) {
            final double dx = x - xbar;
            final double y = dx - c;
            final double t = correction + y;
            c = (t - correction) - y;
            correction = t;
        }
        // Note: Correction may be infinite
        if (Double.isFinite(correction)) {
            return xbar + correction / data.length;
        }
        return xbar;
    }

    /**
     * Create the two-pass mean using a rolling first moment.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arrayRollingFirstMoment(double[] data) {
        final RollingFirstMoment m1 = new RollingFirstMoment();
        for (final double x : data) {
            m1.accept(x);
        }
        final double xbar = m1.getAsDouble();
        if (!Double.isFinite(xbar)) {
            // Note: Also occurs when the input is empty
            return xbar;
        }
        return correctMean(data, xbar);
    }

    /**
     * Create the two-pass mean using a rolling first moment
     * safe to overflow.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arraySafeRollingFirstMoment(double[] data) {
        final SafeRollingFirstMoment m1 = new SafeRollingFirstMoment();
        for (final double x : data) {
            m1.accept(x);
        }
        final double xbar = m1.getAsDouble();
        if (!Double.isFinite(xbar)) {
            // Note: Also occurs when the input is empty
            return xbar;
        }
        return correctMean(data, xbar);
    }

    /**
     * Create the two-pass mean using a rolling first moment
     * safe to overflow.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arrayScaledRollingFirstMoment(double[] data) {
        final ScaledRollingFirstMoment m1 = new ScaledRollingFirstMoment();
        for (final double x : data) {
            m1.accept(x);
        }
        final double xbar = m1.getAsDouble();
        if (!Double.isFinite(xbar)) {
            // Note: Also occurs when the input is empty
            return xbar;
        }
        return correctMean(data, xbar);
    }

    /**
     * Create the two-pass mean using a rolling first moment inline.
     *
     * <p>Note: This method is effectively the same as {@link #arrayRollingFirstMoment(double[])}
     * and timing tests show there is no overhead to using an object to aggregate the first moment,
     * i.e. this is not faster.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arrayInlineRollingFirstMoment(double[] data) {
        double m1 = 0;
        int n = 0;
        for (final double x : data) {
            m1 += (x - m1) / ++n;
        }
        if (!Double.isFinite(m1) || n == 0) {
            return Double.NaN;
        }
        return correctMean(data, m1);
    }

    /**
     * Create the two-pass mean using a rolling first moment inline.
     * The result is safe for all finite input by using downscaling.
     * Upscaling is applied to the end result.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arrayInlineSafeRollingFirstMoment(double[] data) {
        double m1 = 0;
        int n = 0;
        for (final double x : data) {
            m1 += (x * 0.5 - m1) / ++n;
        }
        m1 *= 2;
        if (!Double.isFinite(m1) || n == 0) {
            return Double.NaN;
        }
        return correctMean(data, m1);
    }

    /**
     * Create the two-pass mean using a rolling first moment inline.
     * The result is safe for all finite input by using downscaling.
     * Upscaling is applied to the end result.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arrayInlineSafeRollingFirstMomentExt(double[] data) {
        double m1 = 0;
        int n = 0;
        for (final double x : data) {
            m1 += (x * 0.5 - m1) / ++n;
        }
        m1 *= 2;
        if (!Double.isFinite(m1) || n == 0) {
            return Double.NaN;
        }
        return correctMeanKahan(data, m1);
    }

    /**
     * Create the mean using a single pass sum.
     * The mean is not safe against overflow.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arraySumMean(double[] data) {
        double s = 0;
        for (final double x : data) {
            s += x;
        }
        return s / data.length;
    }

    /**
     * Create themean using a single pass sum using double-double precision.
     * The mean is not safe against overflow or non-finite input.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arrayDDSumMean(double[] data) {
        DD s = DD.ZERO;
        for (final double x : data) {
            s = s.add(x);
        }
        return s.doubleValue() / data.length;
    }

    /**
     * Create the sum-of-cubed deviations from the mean.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arraySumOfCubed(double[] data) {
        final double m = arrayInlineSafeRollingFirstMoment(data);
        double s = 0;
        for (final double x : data) {
            final double dx = x - m;
            s += dx * dx * dx;
        }
        return s;
    }

    /**
     * Create the sum-of-cubed deviations from the mean using the
     * {@link Math#pow(double, double)} function.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arraySumOfCubedPow(double[] data) {
        final double m = arrayInlineSafeRollingFirstMoment(data);
        double s = 0;
        for (final double x : data) {
            s += Math.pow(x - m, 3);
        }
        return s;
    }

    /**
     * Create the sum-of-fourth deviations from the mean.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arraySumOfFourth(double[] data) {
        final double m = arrayInlineSafeRollingFirstMoment(data);
        double s = 0;
        for (final double x : data) {
            double dx = x - m;
            dx *= dx;
            s += dx * dx;
        }
        return s;
    }

    /**
     * Create the sum-of-fourth deviations from the mean using the
     * {@link Math#pow(double, double)} function.
     *
     * @param data Data.
     * @return the statistic
     */
    static double arraySumOfFourthPow(double[] data) {
        final double m = arrayInlineSafeRollingFirstMoment(data);
        double s = 0;
        for (final double x : data) {
            s += Math.pow(x - m, 4);
        }
        return s;
    }

    /**
     * Create the mean from a stream of {@code double} values.
     *
     * @param source Source of the data.
     * @return the mean
     */
    @Benchmark
    public Object streamMean(DataSource source) {
        return Arrays.stream(source.getData()).average();
    }

    /**
     * Create the statistic using a consumer of {@code double} values.
     *
     * @param action Source of the data action.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public Object forEachStatistic(ActionSource action, DataSource source) {
        return forEach(action.getAction(), source.getData());
    }

    /**
     * Create the statistic using a {@code double[]} function.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @return the statistic
     */
    @Benchmark
    public Object arrayStatistic(FunctionSource function, DataSource source) {
        return function.getFunction().apply(source.getData());
    }
}
