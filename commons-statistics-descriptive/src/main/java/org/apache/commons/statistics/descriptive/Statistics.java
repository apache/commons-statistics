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

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Utility methods for statistics.
 */
final class Statistics {
    /** A no-operation double consumer. This is exposed for testing. */
    static final DoubleConsumer DOUBLE_NOOP = new DoubleConsumer() {
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

    /** A no-operation int consumer. This is exposed for testing. */
    static final IntConsumer INT_NOOP = new IntConsumer() {
        @Override
        public void accept(int value) {
            // Do nothing
        }

        @Override
        public IntConsumer andThen(IntConsumer after) {
            // Delegate to the after consumer
            return after;
        }
    };

    /** A no-operation long consumer. This is exposed for testing. */
    static final LongConsumer LONG_NOOP = new LongConsumer() {
        @Override
        public void accept(long value) {
            // Do nothing
        }

        @Override
        public LongConsumer andThen(LongConsumer after) {
            // Delegate to the after consumer
            return after;
        }
    };

    /** Error message for an incompatible statistics. */
    private static final String INCOMPATIBLE_STATISTICS = "Incompatible statistics";

    /** No instances. */
    private Statistics() {}

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends DoubleConsumer> T add(T statistic, double[] values) {
        for (final double x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends DoubleConsumer> T add(T statistic, int[] values) {
        for (final double x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends DoubleConsumer> T add(T statistic, long[] values) {
        for (final double x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends IntConsumer> T add(T statistic, int[] values) {
        for (final int x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Add all the {@code values} to the {@code statistic}.
     *
     * @param <T> Type of the statistic
     * @param statistic Statistic.
     * @param values Values.
     * @return the statistic
     */
    static <T extends LongConsumer> T add(T statistic, long[] values) {
        for (final long x : values) {
            statistic.accept(x);
        }
        return statistic;
    }

    /**
     * Returns {@code true} if the second central moment {@code m2} is effectively
     * zero given the magnitude of the first raw moment {@code m1}.
     *
     * <p>This method shares the logic for detecting a zero variance among implementations
     * that divide by the variance (e.g. skewness, kurtosis).
     *
     * @param m1 First raw moment (mean).
     * @param m2 Second central moment (biased variance).
     * @return true if the variance is zero
     */
    static boolean zeroVariance(double m1, double m2) {
        // Note: Commons Math checks the variance is < 1e-19.
        // The absolute threshold does not account for the magnitude of the sample.
        // This checks the average squared deviation from the mean (m2)
        // is smaller than the squared precision of the mean (m1).
        // Precision is set to 15 decimal digits
        // (1e-15 ~ 4.5 eps where eps = 2^-52).
        final double meanPrecision = 1e-15 * m1;
        return m2 <= meanPrecision * meanPrecision;
    }

    /**
     * Chain the {@code consumers} into a single composite consumer. Ignore any {@code null}
     * consumer. Returns {@code null} if all arguments are {@code null}.
     *
     * @param consumers Consumers.
     * @return a composed consumer (or null)
     */
    static DoubleConsumer compose(DoubleConsumer... consumers) {
        DoubleConsumer action = DOUBLE_NOOP;
        for (final DoubleConsumer consumer : consumers) {
            if (consumer != null) {
                action = action.andThen(consumer);
            }
        }
        return action == DOUBLE_NOOP ? null : action;
    }

    /**
     * Chain the {@code consumers} into a single composite consumer. Ignore any {@code null}
     * consumer. Returns {@code null} if all arguments are {@code null}.
     *
     * @param consumers Consumers.
     * @return a composed consumer (or null)
     */
    static IntConsumer compose(IntConsumer... consumers) {
        IntConsumer action = INT_NOOP;
        for (final IntConsumer consumer : consumers) {
            if (consumer != null) {
                action = action.andThen(consumer);
            }
        }
        return action == INT_NOOP ? null : action;
    }

    /**
     * Chain the {@code consumers} into a single composite consumer. Ignore any {@code null}
     * consumer. Returns {@code null} if all arguments are {@code null}.
     *
     * @param consumers Consumers.
     * @return a composed consumer (or null)
     */
    static LongConsumer compose(LongConsumer... consumers) {
        LongConsumer action = LONG_NOOP;
        for (final LongConsumer consumer : consumers) {
            if (consumer != null) {
                action = action.andThen(consumer);
            }
        }
        return action == LONG_NOOP ? null : action;
    }

    /**
     * Gets the statistic result using the {@code int} value.
     * Return {@code null} is the statistic is {@code null}.
     *
     * @param s Statistic.
     * @return the result or null
     */
    static StatisticResult getResultAsIntOrNull(StatisticResult s) {
        if (s != null) {
            return (IntStatisticResult) s::getAsInt;
        }
        return null;
    }

    /**
     * Gets the statistic result using the {@code long} value.
     * Return {@code null} is the statistic is {@code null}.
     *
     * @param s Statistic.
     * @return the result or null
     */
    static StatisticResult getResultAsLongOrNull(StatisticResult s) {
        if (s != null) {
            return (LongStatisticResult) s::getAsLong;
        }
        return null;
    }

    /**
     * Gets the statistic result using the {@code double} value.
     * Return {@code null} is the statistic is {@code null}.
     *
     * @param s Statistic.
     * @return the result or null
     */
    static StatisticResult getResultAsDoubleOrNull(StatisticResult s) {
        if (s != null) {
            return s::getAsDouble;
        }
        return null;
    }

    /**
     * Gets the statistic result using the {@code BigInteger} value.
     * Return {@code null} is the statistic is {@code null}.
     *
     * @param s Statistic.
     * @return the result or null
     */
    static StatisticResult getResultAsBigIntegerOrNull(StatisticResult s) {
        if (s != null) {
            return (BigIntegerStatisticResult) s::getAsBigInteger;
        }
        return null;
    }

    /**
     * Check left-hand side argument {@code a} is {@code null} or else the right-hand side
     * argument {@code b} must also be non-{@code null} so the statistics can be combined.
     *
     * @param <T> {@link StatisticResult} being accumulated.
     * @param a LHS.
     * @param b RHS.
     * @throws IllegalArgumentException if the objects cannot be combined
     */
    static <T extends StatisticResult & StatisticAccumulator<T>> void checkCombineCompatible(T a, T b) {
        if (a != null && b == null) {
            throw new IllegalArgumentException(INCOMPATIBLE_STATISTICS);
        }
    }

    /**
     * Check left-hand side argument {@code a} is {@code null} or else the right-hand side
     * argument {@code b} must be run-time assignable to the same class as {@code a}
     * so the statistics can be combined.
     *
     * @param a LHS.
     * @param b RHS.
     * @throws IllegalArgumentException if the objects cannot be combined
     */
    static void checkCombineAssignable(FirstMoment a, FirstMoment b) {
        if (a != null && (b == null || !a.getClass().isAssignableFrom(b.getClass()))) {
            throw new IllegalArgumentException(INCOMPATIBLE_STATISTICS);
        }
    }

    /**
     * If the left-hand side argument {@code a} is non-{@code null}, combine it with the
     * right-hand side argument {@code b}.
     *
     * @param <T> {@link StatisticResult} being accumulated.
     * @param a LHS.
     * @param b RHS.
     */
    static <T extends StatisticResult & StatisticAccumulator<T>> void combine(T a, T b) {
        if (a != null) {
            a.combine(b);
        }
    }

    /**
     * If the left-hand side argument {@code a} is non-{@code null}, combine it with the
     * right-hand side argument {@code b}. Assumes that the RHS is run-time assignable
     * to the same class as LHS.
     *
     * @param a LHS.
     * @param b RHS.
     * @see #checkCombineAssignable(FirstMoment, FirstMoment)
     */
    static void combineMoment(FirstMoment a, FirstMoment b) {
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
}
