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

import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * Statistics for {@code double} values.
 *
 * <p>This class provides combinations of individual statistic implementations in the
 * {@code org.apache.commons.statistics.descriptive} package.
 *
 * @since 1.1
 */
public final class DoubleStatistics implements DoubleConsumer {
    /** A no-operation double consumer. This is exposed for testing. */
    static final DoubleConsumer NOOP = new DoubleConsumer() {
        @Override
        public void accept(double value) {
            // Do nothing
        }

        @Override
        public DoubleConsumer andThen(DoubleConsumer after) {
            // Delegate to the after consumer
            return after;
        }
    };
    /** Error message for non configured statistics. */
    private static final String NO_CONFIGURED_STATISTICS = "No configured statistics";
    /** Error message for an unsupported statistic. */
    private static final String UNSUPPORTED_STATISTIC = "Unsupported statistic: ";
    /** Error message for an incompatible statistics. */
    private static final String INCOMPATIBLE_STATISTICS = "Incompatible statistics";

    /** Count of values recorded. */
    private long count;
    /** The consumer of values. */
    private final DoubleConsumer consumer;
    /** The {@link Min} implementation. */
    private final Min min;
    /** The {@link Max} implementation. */
    private final Max max;
    /** The moment implementation. May be any instance of {@link FirstMoment}. */
    private final FirstMoment moment;
    /** The {@link Sum} implementation. */
    private final Sum sum;
    /** The {@link Product} implementation. */
    private final Product product;
    /** The {@link SumOfSquares} implementation. */
    private final SumOfSquares sumOfSquares;
    /** The {@link SumOfLogs} implementation. */
    private final SumOfLogs sumOfLogs;
    /** Configuration options for computation of statistics. */
    private StatisticsConfiguration config;

    /**
     * A builder for {@link DoubleStatistics}.
     */
    public static final class Builder {
        /** An empty double array. */
        private static final double[] NO_VALUES = {};

        /** The {@link Min} constructor. */
        private Function<double[], Min> min;
        /** The {@link Max} constructor. */
        private Function<double[], Max> max;
        /** The moment constructor. May return any instance of {@link FirstMoment}. */
        private Function<double[], FirstMoment> moment;
        /** The {@link Sum} constructor. */
        private Function<double[], Sum> sum;
        /** The {@link Product} constructor. */
        private Function<double[], Product> product;
        /** The {@link SumOfSquares} constructor. */
        private Function<double[], SumOfSquares> sumOfSquares;
        /** The {@link SumOfLogs} constructor. */
        private Function<double[], SumOfLogs> sumOfLogs;
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
            switch (statistic) {
            case GEOMETRIC_MEAN:
            case SUM_OF_LOGS:
                sumOfLogs = SumOfLogs::of;
                break;
            case KURTOSIS:
                createMoment(4);
                break;
            case MAX:
                max = Max::of;
                break;
            case MEAN:
                createMoment(1);
                break;
            case MIN:
                min = Min::of;
                break;
            case PRODUCT:
                product = Product::of;
                break;
            case SKEWNESS:
                createMoment(3);
                break;
            case STANDARD_DEVIATION:
            case VARIANCE:
                createMoment(2);
                break;
            case SUM:
                sum = Sum::of;
                break;
            case SUM_OF_SQUARES:
                sumOfSquares = SumOfSquares::of;
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_STATISTIC + statistic);
            }
            return this;
        }

        /**
         * Creates the moment constructor for the specified {@code order},
         * e.g. order=2 is sum of squared deviations.
         *
         * @param order Order.
         */
        private void createMoment(int order) {
            if (order > momentOrder) {
                momentOrder = order;
                if (order == 4) {
                    moment = SumOfFourthDeviations::of;
                } else if (order == 3) {
                    moment = SumOfCubedDeviations::of;
                } else if (order == 2) {
                    moment = SumOfSquaredDeviations::of;
                } else {
                    // Assume order == 1
                    moment = FirstMoment::of;
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
         * Builds a {@code DoubleStatistics} instance.
         *
         * @return {@code DoubleStatistics} instance.
         */
        public DoubleStatistics build() {
            return build(NO_VALUES);
        }

        /**
         * Builds a {@code DoubleStatistics} instance using the input {@code values}.
         *
         * <p>Note: {@code DoubleStatistics} computed using
         * {@link DoubleStatistics#accept(double) accept} may be
         * different from this instance.
         *
         * @param values Values.
         * @return {@code DoubleStatistics} instance.
         */
        public DoubleStatistics build(double... values) {
            Objects.requireNonNull(values, "values");
            return new DoubleStatistics(
                values.length,
                create(min, values),
                create(max, values),
                create(moment, values),
                create(sum, values),
                create(product, values),
                create(sumOfSquares, values),
                create(sumOfLogs, values),
                config);
        }

        /**
         * Creates the object from the {@code values}.
         *
         * @param <T> object type
         * @param constructor Constructor.
         * @param values Values
         * @return the instance
         */
        private static <T> T create(Function<double[], T> constructor, double[] values) {
            if (constructor != null) {
                return constructor.apply(values);
            }
            return null;
        }
    }

    /**
     * Create an instance.
     *
     * @param count Count of values.
     * @param min Min implementation.
     * @param max Max implementation.
     * @param moment Moment implementation.
     * @param sum Sum implementation.
     * @param product Product implementation.
     * @param sumOfSquares Sum of squares implementation.
     * @param sumOfLogs Sum of logs implementation.
     * @param config Statistics configuration.
     */
    DoubleStatistics(long count, Min min, Max max, FirstMoment moment, Sum sum,
                     Product product, SumOfSquares sumOfSquares, SumOfLogs sumOfLogs,
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
        consumer = compose(min, max, moment, sum, product, sumOfSquares, sumOfLogs);
    }

    /**
     * Chain the {@code consumers} into a single composite consumer. Ignore any {@code null}
     * consumer.
     *
     * @param consumers Consumers.
     * @return a composed consumer
     */
    private static DoubleConsumer compose(DoubleConsumer... consumers) {
        DoubleConsumer action = NOOP;
        for (final DoubleConsumer consumer : consumers) {
            if (consumer != null) {
                action = action.andThen(consumer);
            }
        }
        if (action == NOOP) {
            // This should not be possible
            throw new IllegalStateException(NO_CONFIGURED_STATISTICS + ": Please file a bug report");
        }
        return action;
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
    public static DoubleStatistics of(Statistic... statistics) {
        return builder(statistics).build();
    }

    /**
     * Returns a new instance configured to compute the specified {@code statistics}
     * populated using the input {@code values}.
     *
     * <p>Use this method to create an instance populated with a (variable) array of
     * {@code double[]} data:
     *
     * <pre>
     * DoubleStatistics stats = DoubleStatistics.of(
     *     EnumSet.of(Statistic.MIN, Statistic.MAX),
     *     1, 1, 2, 3, 5, 8, 13);
     * </pre>
     *
     * @param statistics Statistics to compute.
     * @param values Values.
     * @return the instance
     * @throws IllegalArgumentException if there are no {@code statistics} to compute.
     */
    public static DoubleStatistics of(Set<Statistic> statistics, double... values) {
        if (statistics.isEmpty()) {
            throw new IllegalArgumentException(NO_CONFIGURED_STATISTICS);
        }
        final Builder b = new Builder();
        statistics.forEach(b::add);
        return b.build(values);
    }

    /**
     * Returns a new builder configured to create instances to compute the specified
     * {@code statistics}.
     *
     * <p>Use this method to create an instance populated with an array of {@code double[]}
     * data using the {@link Builder#build(double...)} method:
     *
     * <pre>
     * double[] data = ...
     * DoubleStatistics stats = DoubleStatistics.builder(
     *     Statistic.MIN, Statistic.MAX, Statistic.VARIANCE)
     *     .build(data);
     * </pre>
     *
     * <p>The builder can be used to create multiple instances of {@link DoubleStatistics}
     * to be used in parallel, or on separate arrays of {@code double[]} data. These may
     * be {@link #combine(DoubleStatistics) combined}. For example:
     *
     * <pre>
     * double[][] data = ...
     * DoubleStatistics.Builder builder = DoubleStatistics.builder(
     *     Statistic.MIN, Statistic.MAX, Statistic.VARIANCE);
     * DoubleStatistics stats = Arrays.stream(data)
     *     .parallel()
     *     .map(builder::build)
     *     .reduce(DoubleStatistics::combine)
     *     .get();
     * </pre>
     *
     * <p>The builder can be used to create a {@link java.util.stream.Collector} for repeat
     * use on multiple data:
     *
     * <pre>{@code
     * DoubleStatistics.Builder builder = DoubleStatistics.builder(
     *     Statistic.MIN, Statistic.MAX, Statistic.VARIANCE);
     * Collector<double[], DoubleStatistics, DoubleStatistics> collector =
     *     Collector.of(builder::build,
     *                  (s, d) -> s.combine(builder.build(d)),
     *                  DoubleStatistics::combine);
     *
     * // Repeated
     * double[][] data = ...
     * DoubleStatistics stats = Arrays.stream(data).collect(collector);
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
    public void accept(double value) {
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
     * @see #get(Statistic)
     */
    public boolean isSupported(Statistic statistic) {
        // Check for the appropriate underlying implementation
        switch (statistic) {
        case GEOMETRIC_MEAN:
        case SUM_OF_LOGS:
            return sumOfLogs != null;
        case KURTOSIS:
            return moment instanceof SumOfFourthDeviations;
        case MAX:
            return max != null;
        case MEAN:
            return moment != null;
        case MIN:
            return min != null;
        case PRODUCT:
            return product != null;
        case SKEWNESS:
            return moment instanceof SumOfCubedDeviations;
        case STANDARD_DEVIATION:
        case VARIANCE:
            return moment instanceof SumOfSquaredDeviations;
        case SUM:
            return sum != null;
        case SUM_OF_SQUARES:
            return sumOfSquares != null;
        default:
            return false;
        }
    }

    /**
     * Gets the value of the specified {@code statistic}.
     *
     * @param statistic Statistic.
     * @return the double
     * @throws IllegalArgumentException if the {@code statistic} is not supported
     * @see #isSupported(Statistic)
     * @see #getSupplier(Statistic)
     */
    public double get(Statistic statistic) {
        return getSupplier(statistic).getAsDouble();
    }

    /**
     * Gets a supplier for the value of the specified {@code statistic}.
     *
     * <p>The returned function will supply the correct result after
     * calls to {@link #accept(double) accept} or
     * {@link #combine(DoubleStatistics) combine} further values into
     * {@code this} instance.
     *
     * <p>This method can be used to perform a one-time look-up of the statistic
     * function to compute statistics as values are dynamically added.
     *
     * @param statistic Statistic.
     * @return the supplier
     * @throws IllegalArgumentException if the {@code statistic} is not supported
     * @see #isSupported(Statistic)
     * @see #get(Statistic)
     */
    public DoubleSupplier getSupplier(Statistic statistic) {
        // Locate the implementation.
        // Statistics that wrap an underlying implementation are created in methods.
        // The return argument should be a method reference and not an instance
        // of DoubleStatistic. This ensures the statistic implementation cannot
        // be updated with new values by casting the result and calling accept(double).
        DoubleSupplier stat = null;
        switch (statistic) {
        case GEOMETRIC_MEAN:
            stat = getGeometricMean();
            break;
        case KURTOSIS:
            stat = getKurtosis();
            break;
        case MAX:
            stat = max;
            break;
        case MEAN:
            stat = getMean();
            break;
        case MIN:
            stat = min;
            break;
        case PRODUCT:
            stat = product;
            break;
        case SKEWNESS:
            stat = getSkewness();
            break;
        case STANDARD_DEVIATION:
            stat = getStandardDeviation();
            break;
        case SUM:
            stat = sum;
            break;
        case SUM_OF_LOGS:
            stat = sumOfLogs;
            break;
        case SUM_OF_SQUARES:
            stat = sumOfSquares;
            break;
        case VARIANCE:
            stat = getVariance();
            break;
        default:
            break;
        }
        if (stat != null) {
            return stat instanceof DoubleStatistic ?
                ((DoubleStatistic) stat)::getAsDouble :
                stat;
        }
        throw new IllegalArgumentException(UNSUPPORTED_STATISTIC + statistic);
    }

    /**
     * Gets the geometric mean.
     *
     * @return a geometric mean supplier (or null if unsupported)
     */
    private DoubleSupplier getGeometricMean() {
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
    private DoubleSupplier getKurtosis() {
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
    private DoubleSupplier getMean() {
        if (moment != null) {
            // Special case where wrapping with a Mean is not required
            return moment::getFirstMoment;
        }
        return null;
    }

    /**
     * Gets the skewness.
     *
     * @return a skewness supplier (or null if unsupported)
     */
    private DoubleSupplier getSkewness() {
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
    private DoubleSupplier getStandardDeviation() {
        if (moment instanceof SumOfSquaredDeviations) {
            return new StandardDeviation((SumOfSquaredDeviations) moment)
                .setBiased(config.isBiased())::getAsDouble;
        }
        return null;
    }

    /**
     * Gets the variance.
     *
     * @return a variance supplier (or null if unsupported)
     */
    private DoubleSupplier getVariance() {
        if (moment instanceof SumOfSquaredDeviations) {
            return new Variance((SumOfSquaredDeviations) moment)
                .setBiased(config.isBiased())::getAsDouble;
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
    public DoubleStatistics combine(DoubleStatistics other) {
        // Check compatibility
        checkNullOrElseOtherNonNull(min, other.min);
        checkNullOrElseOtherNonNull(max, other.max);
        checkNullOrElseOtherNonNull(sum, other.sum);
        checkNullOrElseOtherNonNull(product, other.product);
        checkNullOrElseOtherNonNull(sumOfSquares, other.sumOfSquares);
        checkNullOrElseOtherNonNull(sumOfLogs, other.sumOfLogs);
        checkNullOrElseOtherIsAssignable(moment, other.moment);
        // Combine
        count += other.count;
        combine(min, other.min);
        combine(max, other.max);
        combine(sum, other.sum);
        combine(product, other.product);
        combine(sumOfSquares, other.sumOfSquares);
        combine(sumOfLogs, other.sumOfLogs);
        combineMoment(moment, other.moment);
        return this;
    }

    /**
     * Check left-hand side argument {@code a} is {@code null} or else the right-hand side
     * argument {@code b} must also be non-{@code null}.
     *
     * @param a LHS.
     * @param b RHS.
     */
    private static void checkNullOrElseOtherNonNull(Object a, Object b) {
        if (a != null && b == null) {
            throw new IllegalArgumentException(INCOMPATIBLE_STATISTICS);
        }
    }

    /**
     * Check left-hand side argument {@code a} is {@code null} or else the right-hand side
     * argument {@code b} must be run-time assignable to the same class as {@code a}.
     *
     * @param a LHS.
     * @param b RHS.
     */
    private static void checkNullOrElseOtherIsAssignable(Object a, Object b) {
        if (a != null && (b == null || !a.getClass().isAssignableFrom(b.getClass()))) {
            throw new IllegalArgumentException(INCOMPATIBLE_STATISTICS);
        }
    }

    /**
     * If the left-hand side argument {@code a} is non-{@code null}, combined it with the
     * right-hand side argument {@code b}.
     *
     * @param <T> {@link DoubleStatistic} being accumulated.
     * @param a LHS.
     * @param b RHS.
     */
    private static <T extends DoubleStatistic & DoubleStatisticAccumulator<T>> void combine(T a, T b) {
        if (a != null) {
            a.combine(b);
        }
    }

    /**
     * If the left-hand side argument {@code a} is non-{@code null}, combined it with the
     * right-hand side argument {@code b}. Assumes that the RHS is run-time assignable
     * to the same class as LHS.
     *
     * @param a LHS.
     * @param b RHS.
     */
    private static void combineMoment(FirstMoment a, FirstMoment b) {
        // Avoid reflection and use the simpler instanceof
        if (a instanceof SumOfFourthDeviations) {
            ((SumOfFourthDeviations) a).combine((SumOfFourthDeviations) b);
        } else if (a instanceof SumOfCubedDeviations) {
            ((SumOfCubedDeviations) a).combine((SumOfCubedDeviations) b);
        } else if (a instanceof SumOfSquaredDeviations) {
            ((SumOfSquaredDeviations) a).combine((SumOfSquaredDeviations) b);
        } else if (a != null) {
            a.combine(b);
        }
    }

    /**
     * Sets the statistics configuration.
     *
     * <p>These options only control the final computation of statistics. The configuration
     * will not affect compatibility between instances during a
     * {@link #combine(DoubleStatistics) combine} operation.
     *
     * <p>Note: These options will affect any future computation of statistics. Supplier functions
     * that have been previously created will not be updated with the new configuration.
     *
     * @param v Value.
     * @return {@code this} instance
     * @throws NullPointerException if the value is null
     * @see #getSupplier(Statistic)
     */
    public DoubleStatistics setConfiguration(StatisticsConfiguration v) {
        config = Objects.requireNonNull(v);
        return this;
    }
}
