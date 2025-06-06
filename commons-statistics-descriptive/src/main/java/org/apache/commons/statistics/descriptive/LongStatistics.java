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

import java.math.BigInteger;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;

/**
 * Statistics for {@code long} values.
 *
 * <p>This class provides combinations of individual statistic implementations in the
 * {@code org.apache.commons.statistics.descriptive} package.
 *
 * <p>Supports up to 2<sup>63</sup> (exclusive) observations.
 * This implementation does not check for overflow of the count.
 *
 * @since 1.1
 */
public final class LongStatistics implements LongConsumer {
    /** Error message for non configured statistics. */
    private static final String NO_CONFIGURED_STATISTICS = "No configured statistics";
    /** Error message for an unsupported statistic. */
    private static final String UNSUPPORTED_STATISTIC = "Unsupported statistic: ";

    /** Count of values recorded. */
    private long count;
    /** The consumer of values. */
    private final LongConsumer consumer;
    /** The {@link LongMin} implementation. */
    private final LongMin min;
    /** The {@link LongMax} implementation. */
    private final LongMax max;
    /** The moment implementation. May be any instance of {@link FirstMoment}.
     * This implementation uses only the third and fourth moments. */
    private final FirstMoment moment;
    /** The {@link LongSum} implementation. */
    private final LongSum sum;
    /** The {@link Product} implementation. */
    private final Product product;
    /** The {@link LongSumOfSquares} implementation. */
    private final LongSumOfSquares sumOfSquares;
    /** The {@link SumOfLogs} implementation. */
    private final SumOfLogs sumOfLogs;
    /** Configuration options for computation of statistics. */
    private StatisticsConfiguration config;

    /**
     * A builder for {@link LongStatistics}.
     */
    public static final class Builder {
        /** An empty double array. */
        private static final long[] NO_VALUES = {};

        /** The {@link LongMin} constructor. */
        private RangeFunction<long[], LongMin> min;
        /** The {@link LongMax} constructor. */
        private RangeFunction<long[], LongMax> max;
        /** The moment constructor. May return any instance of {@link FirstMoment}. */
        private RangeFunction<long[], FirstMoment> moment;
        /** The {@link LongSum} constructor. */
        private RangeFunction<long[], LongSum> sum;
        /** The {@link Product} constructor. */
        private RangeFunction<long[], Product> product;
        /** The {@link LongSumOfSquares} constructor. */
        private RangeFunction<long[], LongSumOfSquares> sumOfSquares;
        /** The {@link SumOfLogs} constructor. */
        private RangeFunction<long[], SumOfLogs> sumOfLogs;
        /** The order of the moment. It corresponds to the power computed by the {@link FirstMoment}
         * instance constructed by {@link #moment}. This should only be increased from the default
         * of zero (corresponding to no moment computation). */
        private int momentOrder;
        /** Configuration options for computation of statistics. */
        private StatisticsConfiguration config = StatisticsConfiguration.withDefaults();

        /**
         * Create an instance.
         */
        Builder() {
            // Do nothing
        }

        /**
         * Add the statistic to the statistics to compute.
         *
         * @param statistic Statistic to compute.
         * @return {@code this} instance
         */
        Builder add(Statistic statistic) {
            // Exhaustive switch statement
            switch (statistic) {
            case GEOMETRIC_MEAN:
            case SUM_OF_LOGS:
                sumOfLogs = SumOfLogs::createFromRange;
                break;
            case KURTOSIS:
                createMoment(4);
                break;
            case MAX:
                max = LongMax::createFromRange;
                break;
            case MIN:
                min = LongMin::createFromRange;
                break;
            case PRODUCT:
                product = Product::createFromRange;
                break;
            case SKEWNESS:
                createMoment(3);
                break;
            case STANDARD_DEVIATION:
            case VARIANCE:
                sum = LongSum::createFromRange;
                sumOfSquares = LongSumOfSquares::createFromRange;
                break;
            case MEAN:
            case SUM:
                sum = LongSum::createFromRange;
                break;
            case SUM_OF_SQUARES:
                sumOfSquares = LongSumOfSquares::createFromRange;
                break;
            }
            return this;
        }

        /**
         * Creates the moment constructor for the specified {@code order},
         * e.g. order=3 is sum of cubed deviations.
         *
         * @param order Order.
         */
        private void createMoment(int order) {
            if (order > momentOrder) {
                momentOrder = order;
                if (order == 4) {
                    moment = SumOfFourthDeviations::ofRange;
                } else {
                    // Assume order == 3
                    moment = SumOfCubedDeviations::ofRange;
                }
            }
        }

        /**
         * Sets the statistics configuration options for computation of statistics.
         *
         * @param v Value.
         * @return the builder
         * @throws NullPointerException if the value is null
         */
        public Builder setConfiguration(StatisticsConfiguration v) {
            config = Objects.requireNonNull(v);
            return this;
        }

        /**
         * Builds a {@code LongStatistics} instance.
         *
         * @return {@code LongStatistics} instance.
         */
        public LongStatistics build() {
            return create(NO_VALUES, 0, 0);
        }

        /**
         * Builds a {@code LongStatistics} instance using the input {@code values}.
         *
         * <p>Note: {@code LongStatistics} computed using
         * {@link LongStatistics#accept(long) accept} may be
         * different from this instance.
         *
         * @param values Values.
         * @return {@code LongStatistics} instance.
         */
        public LongStatistics build(long... values) {
            Objects.requireNonNull(values, "values");
            return create(values, 0, values.length);
        }

        /**
         * Builds a {@code LongStatistics} instance using the specified range of {@code values}.
         *
         * <p>Note: {@code LongStatistics} computed using
         * {@link LongStatistics#accept(long) accept} may be
         * different from this instance.
         *
         * @param values Values.
         * @param from Inclusive start of the range.
         * @param to Exclusive end of the range.
         * @return {@code LongStatistics} instance.
         * @throws IndexOutOfBoundsException if the sub-range is out of bounds
         * @since 1.2
         */
        public LongStatistics build(long[] values, int from, int to) {
            Statistics.checkFromToIndex(from, to, values.length);
            return create(values, from, to);
        }

        /**
         * Builds a {@code LongStatistics} instance using the input {@code values}.
         *
         * <p>Note: {@code LongStatistics} computed using
         * {@link LongStatistics#accept(long) accept} may be
         * different from this instance.
         *
         * <p>Warning: No range checks are performed.
         *
         * @param values Values.
         * @param from Inclusive start of the range.
         * @param to Exclusive end of the range.
         * @return {@code LongStatistics} instance.
         */
        private LongStatistics create(long[] values, int from, int to) {
            return new LongStatistics(
                to - from,
                create(min, values, from, to),
                create(max, values, from, to),
                create(moment, values, from, to),
                create(sum, values, from, to),
                create(product, values, from, to),
                create(sumOfSquares, values, from, to),
                create(sumOfLogs, values, from, to),
                config);
        }

        /**
         * Creates the object from the {@code values}.
         *
         * @param <S> value type
         * @param <T> object type
         * @param constructor Constructor.
         * @param values Values
         * @param from Inclusive start of the range.
         * @param to Exclusive end of the range.
         * @return the instance
         */
        private static <S, T> T create(RangeFunction<S, T> constructor, S values, int from, int to) {
            if (constructor != null) {
                return constructor.apply(values, from, to);
            }
            return null;
        }
    }

    /**
     * Create an instance.
     *
     * @param count Count of values.
     * @param min LongMin implementation.
     * @param max LongMax implementation.
     * @param moment Moment implementation.
     * @param sum LongSum implementation.
     * @param product Product implementation.
     * @param sumOfSquares Sum of squares implementation.
     * @param sumOfLogs Sum of logs implementation.
     * @param config Statistics configuration.
     */
    LongStatistics(long count, LongMin min, LongMax max, FirstMoment moment, LongSum sum,
                  Product product, LongSumOfSquares sumOfSquares, SumOfLogs sumOfLogs,
                  StatisticsConfiguration config) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.moment = moment;
        this.sum = sum;
        this.product = product;
        this.sumOfSquares = sumOfSquares;
        this.sumOfLogs = sumOfLogs;
        this.config = config;
        // The final consumer should never be null as the builder is created
        // with at least one statistic.
        consumer = Statistics.composeLongConsumers(min, max, sum, sumOfSquares,
                                                   composeAsLong(moment, product, sumOfLogs));
    }

    /**
     * Chain the {@code consumers} into a single composite {@code LongConsumer}.
     * Ignore any {@code null} consumer.
     *
     * @param consumers Consumers.
     * @return a composed consumer (or null)
     */
    private static LongConsumer composeAsLong(DoubleConsumer... consumers) {
        final DoubleConsumer c = Statistics.composeDoubleConsumers(consumers);
        if (c != null) {
            return c::accept;
        }
        return null;
    }

    /**
     * Returns a new instance configured to compute the specified {@code statistics}.
     *
     * <p>The statistics will be empty and so will return the default values for each
     * computed statistic.
     *
     * @param statistics Statistics to compute.
     * @return the instance
     * @throws IllegalArgumentException if there are no {@code statistics} to compute.
     */
    public static LongStatistics of(Statistic... statistics) {
        return builder(statistics).build();
    }

    /**
     * Returns a new instance configured to compute the specified {@code statistics}
     * populated using the input {@code values}.
     *
     * <p>Use this method to create an instance populated with a (variable) array of
     * {@code long[]} data:
     *
     * <pre>
     * LongStatistics stats = LongStatistics.of(
     *     EnumSet.of(Statistic.MIN, Statistic.MAX),
     *     1, 1, 2, 3, 5, 8, 13);
     * </pre>
     *
     * @param statistics Statistics to compute.
     * @param values Values.
     * @return the instance
     * @throws IllegalArgumentException if there are no {@code statistics} to compute.
     */
    public static LongStatistics of(Set<Statistic> statistics, long... values) {
        if (statistics.isEmpty()) {
            throw new IllegalArgumentException(NO_CONFIGURED_STATISTICS);
        }
        final Builder b = new Builder();
        statistics.forEach(b::add);
        return b.build(values);
    }

    /**
     * Returns a new instance configured to compute the specified {@code statistics}
     * populated using the specified range of {@code values}.
     *
     * <p>Use this method to create an instance populated with part of an array of
     * {@code long[]} data, e.g. to use the first half of the data:
     *
     * <pre>
     * long[] data = ...
     * LongStatistics stats = LongStatistics.of(
     *     EnumSet.of(Statistic.MIN, Statistic.MAX),
     *     data, 0, data.length / 2);
     * </pre>
     *
     * @param statistics Statistics to compute.
     * @param values Values.
     * @param from Inclusive start of the range.
     * @param to Exclusive end of the range.
     * @return the instance
     * @throws IllegalArgumentException if there are no {@code statistics} to compute.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 1.2
     */
    public static LongStatistics ofRange(Set<Statistic> statistics, long[] values, int from, int to) {
        if (statistics.isEmpty()) {
            throw new IllegalArgumentException(NO_CONFIGURED_STATISTICS);
        }
        final Builder b = new Builder();
        statistics.forEach(b::add);
        return b.build(values, from, to);
    }

    /**
     * Returns a new builder configured to create instances to compute the specified
     * {@code statistics}.
     *
     * <p>Use this method to create an instance populated with an array of {@code long[]}
     * data using the {@link Builder#build(long...)} method:
     *
     * <pre>
     * long[] data = ...
     * LongStatistics stats = LongStatistics.builder(
     *     Statistic.MIN, Statistic.MAX, Statistic.VARIANCE)
     *     .build(data);
     * </pre>
     *
     * <p>The builder can be used to create multiple instances of {@link LongStatistics}
     * to be used in parallel, or on separate arrays of {@code long[]} data. These may
     * be {@link #combine(LongStatistics) combined}. For example:
     *
     * <pre>
     * long[][] data = ...
     * LongStatistics.Builder builder = LongStatistics.builder(
     *     Statistic.MIN, Statistic.MAX, Statistic.VARIANCE);
     * LongStatistics stats = Arrays.stream(data)
     *     .parallel()
     *     .map(builder::build)
     *     .reduce(LongStatistics::combine)
     *     .get();
     * </pre>
     *
     * <p>The builder can be used to create a {@link java.util.stream.Collector} for repeat
     * use on multiple data:
     *
     * <pre>{@code
     * LongStatistics.Builder builder = LongStatistics.builder(
     *     Statistic.MIN, Statistic.MAX, Statistic.VARIANCE);
     * Collector<long[], LongStatistics, LongStatistics> collector =
     *     Collector.of(builder::build,
     *                  (s, d) -> s.combine(builder.build(d)),
     *                  LongStatistics::combine);
     *
     * // Repeated
     * long[][] data = ...
     * LongStatistics stats = Arrays.stream(data).collect(collector);
     * }</pre>
     *
     * @param statistics Statistics to compute.
     * @return the builder
     * @throws IllegalArgumentException if there are no {@code statistics} to compute.
     */
    public static Builder builder(Statistic... statistics) {
        if (statistics.length == 0) {
            throw new IllegalArgumentException(NO_CONFIGURED_STATISTICS);
        }
        final Builder b = new Builder();
        for (final Statistic s : statistics) {
            b.add(s);
        }
        return b;
    }

    /**
     * Updates the state of the statistics to reflect the addition of {@code value}.
     *
     * @param value Value.
     */
    @Override
    public void accept(long value) {
        count++;
        consumer.accept(value);
    }

    /**
     * Return the count of values recorded.
     *
     * @return the count of values
     */
    public long getCount() {
        return count;
    }

    /**
     * Check if the specified {@code statistic} is supported.
     *
     * <p>Note: This method will not return {@code false} if the argument is {@code null}.
     *
     * @param statistic Statistic.
     * @return {@code true} if supported
     * @throws NullPointerException if the {@code statistic} is {@code null}
     * @see #getResult(Statistic)
     */
    public boolean isSupported(Statistic statistic) {
        // Check for the appropriate underlying implementation
        // Exhaustive switch statement
        switch (statistic) {
        case GEOMETRIC_MEAN:
        case SUM_OF_LOGS:
            return sumOfLogs != null;
        case KURTOSIS:
            return moment instanceof SumOfFourthDeviations;
        case MAX:
            return max != null;
        case MIN:
            return min != null;
        case PRODUCT:
            return product != null;
        case SKEWNESS:
            return moment instanceof SumOfCubedDeviations;
        case STANDARD_DEVIATION:
        case VARIANCE:
            return sum != null && sumOfSquares != null;
        case MEAN:
        case SUM:
            return sum != null;
        case SUM_OF_SQUARES:
            return sumOfSquares != null;
        }
        // Unreachable code
        throw new IllegalArgumentException(UNSUPPORTED_STATISTIC + statistic);
    }

    /**
     * Gets the value of the specified {@code statistic} as a {@code double}.
     *
     * @param statistic Statistic.
     * @return the value
     * @throws IllegalArgumentException if the {@code statistic} is not supported
     * @see #isSupported(Statistic)
     * @see #getResult(Statistic)
     */
    public double getAsDouble(Statistic statistic) {
        return getResult(statistic).getAsDouble();
    }

    /**
     * Gets the value of the specified {@code statistic} as a {@code long}.
     *
     * <p>Use this method to access the {@code long} result for exact integer statistics,
     * for example {@link Statistic#MIN}.
     *
     * <p>Note: This method may throw an {@link ArithmeticException} if the result
     * overflows an {@code long}.
     *
     * @param statistic Statistic.
     * @return the value
     * @throws IllegalArgumentException if the {@code statistic} is not supported
     * @throws ArithmeticException if the {@code result} overflows an {@code long} or is not
     * finite
     * @see #isSupported(Statistic)
     * @see #getResult(Statistic)
     */
    public long getAsLong(Statistic statistic) {
        return getResult(statistic).getAsLong();
    }

    /**
     * Gets the value of the specified {@code statistic} as a {@code BigInteger}.
     *
     * <p>Use this method to access the {@code BigInteger} result for exact integer statistics,
     * for example {@link Statistic#SUM_OF_SQUARES}.
     *
     * <p>Note: This method may throw an {@link ArithmeticException} if the result
     * is not finite.
     *
     * @param statistic Statistic.
     * @return the value
     * @throws IllegalArgumentException if the {@code statistic} is not supported
     * @throws ArithmeticException if the {@code result} is not finite
     * @see #isSupported(Statistic)
     * @see #getResult(Statistic)
     */
    public BigInteger getAsBigInteger(Statistic statistic) {
        return getResult(statistic).getAsBigInteger();
    }

    /**
     * Gets a supplier for the value of the specified {@code statistic}.
     *
     * <p>The returned function will supply the correct result after
     * calls to {@link #accept(long) accept} or
     * {@link #combine(LongStatistics) combine} further values into
     * {@code this} instance.
     *
     * <p>This method can be used to perform a one-time look-up of the statistic
     * function to compute statistics as values are dynamically added.
     *
     * @param statistic Statistic.
     * @return the supplier
     * @throws IllegalArgumentException if the {@code statistic} is not supported
     * @see #isSupported(Statistic)
     * @see #getAsDouble(Statistic)
     */
    public StatisticResult getResult(Statistic statistic) {
        // Locate the implementation.
        // Statistics that wrap an underlying implementation are created in methods.
        // The return argument should be an interface reference and not an instance
        // of LongStatistic. This ensures the statistic implementation cannot
        // be updated with new values by casting the result and calling accept(long).
        StatisticResult stat = null;
        // Exhaustive switch statement
        switch (statistic) {
        case GEOMETRIC_MEAN:
            stat = getGeometricMean();
            break;
        case KURTOSIS:
            stat = getKurtosis();
            break;
        case MAX:
            stat = Statistics.getResultAsLongOrNull(max);
            break;
        case MEAN:
            stat = getMean();
            break;
        case MIN:
            stat = Statistics.getResultAsLongOrNull(min);
            break;
        case PRODUCT:
            stat = Statistics.getResultAsDoubleOrNull(product);
            break;
        case SKEWNESS:
            stat = getSkewness();
            break;
        case STANDARD_DEVIATION:
            stat = getStandardDeviation();
            break;
        case SUM:
            stat = Statistics.getResultAsBigIntegerOrNull(sum);
            break;
        case SUM_OF_LOGS:
            stat = Statistics.getResultAsDoubleOrNull(sumOfLogs);
            break;
        case SUM_OF_SQUARES:
            stat = Statistics.getResultAsBigIntegerOrNull(sumOfSquares);
            break;
        case VARIANCE:
            stat = getVariance();
            break;
        }
        if (stat != null) {
            return stat;
        }
        throw new IllegalArgumentException(UNSUPPORTED_STATISTIC + statistic);
    }

    /**
     * Gets the geometric mean.
     *
     * @return a geometric mean supplier (or null if unsupported)
     */
    private StatisticResult getGeometricMean() {
        if (sumOfLogs != null) {
            // Return a function that has access to the count and sumOfLogs
            return () -> GeometricMean.computeGeometricMean(count, sumOfLogs);
        }
        return null;
    }

    /**
     * Gets the kurtosis.
     *
     * @return a kurtosis supplier (or null if unsupported)
     */
    private StatisticResult getKurtosis() {
        if (moment instanceof SumOfFourthDeviations) {
            return new Kurtosis((SumOfFourthDeviations) moment)
                .setBiased(config.isBiased())::getAsDouble;
        }
        return null;
    }

    /**
     * Gets the mean.
     *
     * @return a mean supplier (or null if unsupported)
     */
    private StatisticResult getMean() {
        if (sum != null) {
            // Return a function that has access to the count and sum
            final Int128 s = sum.getSum();
            return () -> LongMean.computeMean(s, count);
        }
        return null;
    }

    /**
     * Gets the skewness.
     *
     * @return a skewness supplier (or null if unsupported)
     */
    private StatisticResult getSkewness() {
        if (moment instanceof SumOfCubedDeviations) {
            return new Skewness((SumOfCubedDeviations) moment)
                .setBiased(config.isBiased())::getAsDouble;
        }
        return null;
    }

    /**
     * Gets the standard deviation.
     *
     * @return a standard deviation supplier (or null if unsupported)
     */
    private StatisticResult getStandardDeviation() {
        return getVarianceOrStd(true);
    }

    /**
     * Gets the variance.
     *
     * @return a variance supplier (or null if unsupported)
     */
    private StatisticResult getVariance() {
        return getVarianceOrStd(false);
    }

    /**
     * Gets the variance or standard deviation.
     *
     * @param std Flag to control if the statistic is the standard deviation.
     * @return a variance/standard deviation supplier (or null if unsupported)
     */
    private StatisticResult getVarianceOrStd(boolean std) {
        if (sum != null && sumOfSquares != null) {
            // Return a function that has access to the count, sum and sum of squares
            final Int128 s = sum.getSum();
            final UInt192 ss = sumOfSquares.getSumOfSquares();
            final boolean biased = config.isBiased();
            return () -> LongVariance.computeVarianceOrStd(ss, s, count, biased, std);
        }
        return null;
    }

    /**
     * Combines the state of the {@code other} statistics into this one.
     * Only {@code this} instance is modified by the {@code combine} operation.
     *
     * <p>The {@code other} instance must be <em>compatible</em>. This is {@code true} if the
     * {@code other} instance returns {@code true} for {@link #isSupported(Statistic)} for
     * all values of the {@link Statistic} enum which are supported by {@code this}
     * instance.
     *
     * <p>Note that this operation is <em>not symmetric</em>. It may be possible to perform
     * {@code a.combine(b)} but not {@code b.combine(a)}. In the event that the {@code other}
     * instance is not compatible then an exception is raised before any state is modified.
     *
     * @param other Another set of statistics to be combined.
     * @return {@code this} instance after combining {@code other}.
     * @throws IllegalArgumentException if the {@code other} is not compatible
     */
    public LongStatistics combine(LongStatistics other) {
        // Check compatibility
        Statistics.checkCombineCompatible(min, other.min);
        Statistics.checkCombineCompatible(max, other.max);
        Statistics.checkCombineCompatible(sum, other.sum);
        Statistics.checkCombineCompatible(product, other.product);
        Statistics.checkCombineCompatible(sumOfSquares, other.sumOfSquares);
        Statistics.checkCombineCompatible(sumOfLogs, other.sumOfLogs);
        Statistics.checkCombineAssignable(moment, other.moment);
        // Combine
        count += other.count;
        Statistics.combine(min, other.min);
        Statistics.combine(max, other.max);
        Statistics.combine(sum, other.sum);
        Statistics.combine(product, other.product);
        Statistics.combine(sumOfSquares, other.sumOfSquares);
        Statistics.combine(sumOfLogs, other.sumOfLogs);
        Statistics.combineMoment(moment, other.moment);
        return this;
    }

    /**
     * Sets the statistics configuration.
     *
     * <p>These options only control the final computation of statistics. The configuration
     * will not affect compatibility between instances during a
     * {@link #combine(LongStatistics) combine} operation.
     *
     * <p>Note: These options will affect any future computation of statistics. Supplier functions
     * that have been previously created will not be updated with the new configuration.
     *
     * @param v Value.
     * @return {@code this} instance
     * @throws NullPointerException if the value is null
     * @see #getResult(Statistic)
     */
    public LongStatistics setConfiguration(StatisticsConfiguration v) {
        config = Objects.requireNonNull(v);
        return this;
    }
}
