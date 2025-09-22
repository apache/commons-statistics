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
package org.apache.commons.statistics.ranking;

import java.util.Arrays;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * Ranking based on the natural ordering on floating-point values.
 *
 * <p>{@link Double#NaN NaNs} are treated according to the configured
 * {@link NaNStrategy} and ties are handled using the selected
 * {@link TiesStrategy}. Configuration settings are supplied in optional
 * constructor arguments. Defaults are {@link NaNStrategy#FAILED} and
 * {@link TiesStrategy#AVERAGE}, respectively.
 *
 * <p>When using {@link TiesStrategy#RANDOM}, a generator of random values in {@code [0, x)}
 * can be supplied as a {@link IntUnaryOperator} argument; otherwise a default is created
 * on-demand. The source of randomness can be supplied using a method reference.
 * The following example creates a ranking with NaN values with the highest
 * ranking and ties resolved randomly:
 *
 * <pre>
 * NaturalRanking ranking = new NaturalRanking(NaNStrategy.MAXIMAL,
 *                                             new SplittableRandom()::nextInt);
 * </pre>
 *
 * <p>Note: Using {@link TiesStrategy#RANDOM} is not thread-safe due to the mutable
 * generator of randomness. Instances not using random resolution of ties are
 * thread-safe.
 *
 * <p>Examples:
 *
 * <table border="">
 * <caption>Examples</caption>
 * <tr><th colspan="3">
 * Input data: [20, 17, 30, 42.3, 17, 50, Double.NaN, Double.NEGATIVE_INFINITY, 17]
 * </th></tr>
 * <tr><th>NaNStrategy</th><th>TiesStrategy</th>
 * <th>{@code rank(data)}</th>
 * <tr>
 * <td>MAXIMAL</td>
 * <td>default (ties averaged)</td>
 * <td>[5, 3, 6, 7, 3, 8, 9, 1, 3]</td></tr>
 * <tr>
 * <td>MAXIMAL</td>
 * <td>MINIMUM</td>
 * <td>[5, 2, 6, 7, 2, 8, 9, 1, 2]</td></tr>
 * <tr>
 * <td>MINIMAL</td>
 * <td>default (ties averaged]</td>
 * <td>[6, 4, 7, 8, 4, 9, 1.5, 1.5, 4]</td></tr>
 * <tr>
 * <td>REMOVED</td>
 * <td>SEQUENTIAL</td>
 * <td>[5, 2, 6, 7, 3, 8, 1, 4]</td></tr>
 * <tr>
 * <td>MINIMAL</td>
 * <td>MAXIMUM</td>
 * <td>[6, 5, 7, 8, 5, 9, 2, 2, 5]</td></tr>
 * <tr>
 * <td>MINIMAL</td>
 * <td>MAXIMUM</td>
 * <td>[6, 5, 7, 8, 5, 9, 2, 2, 5]</td></tr>
 * </table>
 *
 * @since 1.1
 */
public class NaturalRanking implements RankingAlgorithm {
    /** Message for a null user-supplied {@link NaNStrategy}. */
    private static final String NULL_NAN_STRATEGY = "nanStrategy";
    /** Message for a null user-supplied {@link TiesStrategy}. */
    private static final String NULL_TIES_STRATEGY = "tiesStrategy";
    /** Message for a null user-supplied source of randomness. */
    private static final String NULL_RANDOM_SOURCE = "randomIntFunction";
    /** Default NaN strategy. */
    private static final NaNStrategy DEFAULT_NAN_STRATEGY = NaNStrategy.FAILED;
    /** Default ties strategy. */
    private static final TiesStrategy DEFAULT_TIES_STRATEGY = TiesStrategy.AVERAGE;
    /** Map values to positive infinity. */
    private static final DoubleUnaryOperator ACTION_POS_INF = x -> Double.POSITIVE_INFINITY;
    /** Map values to negative infinity. */
    private static final DoubleUnaryOperator ACTION_NEG_INF = x -> Double.NEGATIVE_INFINITY;
    /** Raise an exception for values. */
    private static final DoubleUnaryOperator ACTION_ERROR = operand -> {
        throw new IllegalArgumentException("Invalid data: " + operand);
    };

    /** NaN strategy. */
    private final NaNStrategy nanStrategy;
    /** Ties strategy. */
    private final TiesStrategy tiesStrategy;
    /** Source of randomness when ties strategy is RANDOM.
     * Function maps positive x to {@code [0, x)}.
     * Can be null to default to a JDK implementation. */
    private IntUnaryOperator randomIntFunction;

    /**
     * Creates an instance with {@link NaNStrategy#FAILED} and
     * {@link TiesStrategy#AVERAGE}.
     */
    public NaturalRanking() {
        this(DEFAULT_NAN_STRATEGY, DEFAULT_TIES_STRATEGY, null);
    }

    /**
     * Creates an instance with {@link NaNStrategy#FAILED} and the
     * specified @{@code tiesStrategy}.
     *
     * <p>If the ties strategy is {@link TiesStrategy#RANDOM RANDOM} a default
     * source of randomness is used to resolve ties.
     *
     * @param tiesStrategy TiesStrategy to use.
     * @throws NullPointerException if the strategy is {@code null}
     */
    public NaturalRanking(TiesStrategy tiesStrategy) {
        this(DEFAULT_NAN_STRATEGY,
            Objects.requireNonNull(tiesStrategy, NULL_TIES_STRATEGY), null);
    }

    /**
     * Creates an instance with the specified @{@code nanStrategy} and
     * {@link TiesStrategy#AVERAGE}.
     *
     * @param nanStrategy NaNStrategy to use.
     * @throws NullPointerException if the strategy is {@code null}
     */
    public NaturalRanking(NaNStrategy nanStrategy) {
        this(Objects.requireNonNull(nanStrategy, NULL_NAN_STRATEGY),
            DEFAULT_TIES_STRATEGY, null);
    }

    /**
     * Creates an instance with the specified @{@code nanStrategy} and the
     * specified @{@code tiesStrategy}.
     *
     * <p>If the ties strategy is {@link TiesStrategy#RANDOM RANDOM} a default
     * source of randomness is used to resolve ties.
     *
     * @param nanStrategy NaNStrategy to use.
     * @param tiesStrategy TiesStrategy to use.
     * @throws NullPointerException if any strategy is {@code null}
     */
    public NaturalRanking(NaNStrategy nanStrategy,
                          TiesStrategy tiesStrategy) {
        this(Objects.requireNonNull(nanStrategy, NULL_NAN_STRATEGY),
            Objects.requireNonNull(tiesStrategy, NULL_TIES_STRATEGY), null);
    }

    /**
     * Creates an instance with {@link NaNStrategy#FAILED},
     * {@link TiesStrategy#RANDOM} and the given the source of random index data.
     *
     * @param randomIntFunction Source of random index data.
     * Function maps positive {@code x} randomly to {@code [0, x)}
     * @throws NullPointerException if the source of randomness is {@code null}
     */
    public NaturalRanking(IntUnaryOperator randomIntFunction) {
        this(DEFAULT_NAN_STRATEGY, TiesStrategy.RANDOM,
            Objects.requireNonNull(randomIntFunction, NULL_RANDOM_SOURCE));
    }

    /**
     * Creates an instance with the specified @{@code nanStrategy},
     * {@link TiesStrategy#RANDOM} and the given the source of random index data.
     *
     * @param nanStrategy NaNStrategy to use.
     * @param randomIntFunction Source of random index data.
     * Function maps positive {@code x} randomly to {@code [0, x)}
     * @throws NullPointerException if the strategy or source of randomness are {@code null}
     */
    public NaturalRanking(NaNStrategy nanStrategy,
                          IntUnaryOperator randomIntFunction) {
        this(Objects.requireNonNull(nanStrategy, NULL_NAN_STRATEGY), TiesStrategy.RANDOM,
            Objects.requireNonNull(randomIntFunction, NULL_RANDOM_SOURCE));
    }

    /**
     * @param nanStrategy NaNStrategy to use.
     * @param tiesStrategy TiesStrategy to use.
     * @param randomIntFunction Source of random index data.
     */
    private NaturalRanking(NaNStrategy nanStrategy,
                           TiesStrategy tiesStrategy,
                           IntUnaryOperator randomIntFunction) {
        // User-supplied arguments are checked for non-null in the respective constructor
        this.nanStrategy = nanStrategy;
        this.tiesStrategy = tiesStrategy;
        this.randomIntFunction = randomIntFunction;
    }

    /**
     * Return the {@link NaNStrategy}.
     *
     * @return the strategy for handling NaN
     */
    public NaNStrategy getNanStrategy() {
        return nanStrategy;
    }

    /**
     * Return the {@link TiesStrategy}.
     *
     * @return the strategy for handling ties
     */
    public TiesStrategy getTiesStrategy() {
        return tiesStrategy;
    }

    /**
     * Rank {@code data} using the natural ordering on floating-point values, with
     * NaN values handled according to {@code nanStrategy} and ties resolved using
     * {@code tiesStrategy}.
     *
     * @throws IllegalArgumentException if the selected {@link NaNStrategy} is
     * {@code FAILED} and a {@link Double#NaN} is encountered in the input data.
     */
    @Override
    public double[] apply(double[] data) {
        // Convert data for sorting.
        // NaNs are counted for the FIXED strategy.
        final int[] nanCount = {0};
        final DataPosition[] ranks = createRankData(data, nanCount);

        // Sorting will move NaNs to the end and we do not have to resolve ties in them.
        final int nonNanSize = ranks.length - nanCount[0];

        // Edge case for empty data
        if (nonNanSize == 0) {
            // Either NaN are left in-place or removed
            return nanStrategy == NaNStrategy.FIXED ? data : new double[0];
        }

        Arrays.sort(ranks, (a, b) -> Double.compare(a.getValue(), b.getValue()));

        // Walk the sorted array, filling output array using sorted positions,
        // resolving ties as we go.
        int pos = 1;
        final double[] out = new double[ranks.length];

        DataPosition current = ranks[0];
        out[current.getPosition()] = pos;

        // Store all previous elements of a tie.
        // Note this lags behind the length of the tie sequence by 1.
        // In the event there are no ties this is not used.
        final IntList tiesTrace = new IntList(ranks.length);

        for (int i = 1; i < nonNanSize; i++) {
            final DataPosition previous = current;
            current = ranks[i];
            if (current.getValue() > previous.getValue()) {
                // Check for a previous tie sequence
                if (tiesTrace.size() != 0) {
                    resolveTie(out, tiesTrace, previous.getPosition());
                }
                pos = i + 1;
            } else {
                // Tie sequence. Add the matching previous element.
                tiesTrace.add(previous.getPosition());
            }
            out[current.getPosition()] = pos;
        }
        // Handle tie sequence at end
        if (tiesTrace.size() != 0) {
            resolveTie(out, tiesTrace, current.getPosition());
        }
        // For the FIXED strategy consume the remaining NaN elements
        if (nanStrategy == NaNStrategy.FIXED) {
            for (int i = nonNanSize; i < ranks.length; i++) {
                out[ranks[i].getPosition()] = Double.NaN;
            }
        }
        return out;
    }

    /**
     * Creates the rank data. If using {@link NaNStrategy#REMOVED} then NaNs are
     * filtered. Otherwise NaNs may be mapped to an infinite value, counted to allow
     * subsequent processing, or cause an exception to be thrown.
     *
     * @param data Source data.
     * @param nanCount Output counter for NaN values.
     * @return the rank data
     * @throws IllegalArgumentException if the data contains NaN values when using
     * {@link NaNStrategy#FAILED}.
     */
    private DataPosition[] createRankData(double[] data, final int[] nanCount) {
        return nanStrategy == NaNStrategy.REMOVED ?
                createNonNaNRankData(data) :
                createMappedRankData(data, createNaNAction(nanCount));
    }

    /**
     * Creates the NaN action.
     *
     * @param nanCount Output counter for NaN values.
     * @return the operator applied to NaN values
     */
    private DoubleUnaryOperator createNaNAction(int[] nanCount) {
        // Exhaustive switch statement
        switch (nanStrategy) {
        case MAXIMAL: // Replace NaNs with +INFs
            return ACTION_POS_INF;
        case MINIMAL: // Replace NaNs with -INFs
            return ACTION_NEG_INF;
        case REMOVED: // NaNs are removed
        case FIXED:   // NaNs are unchanged
            // Count the NaNs in the data that must be handled
            return x -> {
                nanCount[0]++;
                return x;
            };
        case FAILED:
            return ACTION_ERROR;
        }
        // Unreachable code
        throw new IllegalStateException(String.valueOf(nanStrategy));
    }

    /**
     * Creates the rank data with NaNs removed.
     *
     * @param data Source data.
     * @return the rank data
     */
    private static DataPosition[] createNonNaNRankData(double[] data) {
        final DataPosition[] ranks = new DataPosition[data.length];
        int size = 0;
        for (final double v : data) {
            if (!Double.isNaN(v)) {
                ranks[size] = new DataPosition(v, size);
                size++;
            }
        }
        return size == data.length ? ranks : Arrays.copyOf(ranks, size);
    }

    /**
     * Creates the rank data.
     *
     * @param data Source data.
     * @param nanAction Mapping operator applied to NaN values.
     * @return the rank data
     */
    private static DataPosition[] createMappedRankData(double[] data, DoubleUnaryOperator nanAction) {
        final DataPosition[] ranks = new DataPosition[data.length];
        for (int i = 0; i < data.length; i++) {
            double v = data[i];
            if (Double.isNaN(v)) {
                v = nanAction.applyAsDouble(v);
            }
            ranks[i] = new DataPosition(v, i);
        }
        return ranks;
    }

    /**
     * Resolve a sequence of ties, using the configured {@link TiesStrategy}. The
     * input {@code ranks} array is expected to take the same value for all indices
     * in {@code tiesTrace}. The common value is recoded according to the
     * tiesStrategy. For example, if ranks = [5,8,2,6,2,7,1,2], tiesTrace = [2,4,7]
     * and tiesStrategy is MINIMUM, ranks will be unchanged. The same array and
     * trace with tiesStrategy AVERAGE will come out [5,8,3,6,3,7,1,3].
     *
     * <p>Note: For convenience the final index of the trace is passed as an argument;
     * it is assumed the list is already non-empty. At the end of the method the
     * list of indices is cleared.
     *
     * @param ranks Array of ranks.
     * @param tiesTrace List of indices where {@code ranks} is constant, that is,
     * for any i and j in {@code tiesTrace}: {@code ranks[i] == ranks[j]}.
     * @param finalIndex The final index to add to the sequence of ties.
     */
    private void resolveTie(double[] ranks, IntList tiesTrace, int finalIndex) {
        tiesTrace.add(finalIndex);

        // Constant value of ranks over tiesTrace.
        // Note: c is a rank counter starting from 1 so limited to an int.
        final double c = ranks[tiesTrace.get(0)];

        // length of sequence of tied ranks
        final int length = tiesTrace.size();

        // Exhaustive switch
        switch (tiesStrategy) {
        case  AVERAGE:   // Replace ranks with average: (lower + upper) / 2
            fill(ranks, tiesTrace, (2 * c + length - 1) * 0.5);
            break;
        case MAXIMUM:    // Replace ranks with maximum values
            fill(ranks, tiesTrace, c + length - 1);
            break;
        case MINIMUM:    // Replace ties with minimum
            // Note that the tie sequence already has all values set to c so
            // no requirement to fill again.
            break;
        case SEQUENTIAL: // Fill sequentially from c to c + length - 1
        case RANDOM:     // Fill with randomized sequential values in [c, c + length - 1]
            // This cast is safe as c is a counter.
            int r = (int) c;
            if (tiesStrategy == TiesStrategy.RANDOM) {
                tiesTrace.shuffle(getRandomIntFunction());
            }
            final int size = tiesTrace.size();
            for (int i = 0; i < size; i++) {
                ranks[tiesTrace.get(i)] = r++;
            }
            break;
        }

        tiesTrace.clear();
    }

    /**
     * Sets {@code data[i] = value} for each i in {@code tiesTrace}.
     *
     * @param data Array to modify.
     * @param tiesTrace List of index values to set.
     * @param value Value to set.
     */
    private static void fill(double[] data, IntList tiesTrace, double value) {
        final int size = tiesTrace.size();
        for (int i = 0; i < size; i++) {
            data[tiesTrace.get(i)] = value;
        }
    }

    /**
     * Gets the function to map positive {@code x} randomly to {@code [0, x)}.
     * Defaults to a system provided generator if the constructor source of randomness is null.
     *
     * @return the RNG
     */
    private IntUnaryOperator getRandomIntFunction() {
        IntUnaryOperator r = randomIntFunction;
        if (r == null) {
            // Default to a SplittableRandom
            r = new SplittableRandom()::nextInt;
            randomIntFunction = r;
        }
        return r;
    }

    /**
     * An expandable list of int values. This allows tracking array positions
     * without using boxed values in a {@code List<Integer>}.
     */
    private static class IntList {
        /** The maximum size of array to allocate. */
        private final int max;

        /** The size of the list. */
        private int size;
        /** The list data. Initialised with space to store a tie of 2 values. */
        private int[] data = new int[2];

        /**
         * @param max Maximum size of array to allocate. Can use the length of the parent array
         * for which this is used to track indices.
         */
        IntList(int max) {
            this.max = max;
        }

        /**
         * Adds the value to the list.
         *
         * @param value the value
         */
        void add(int value) {
            if (size == data.length) {
                // Overflow safe doubling of the current size.
                data = Arrays.copyOf(data, (int) Math.min(max, size * 2L));
            }
            data[size++] = value;
        }

        /**
         * Gets the element at the specified {@code index}.
         *
         * @param index Element index
         * @return the element
         */
        int get(int index) {
            return data[index];
        }

        /**
         * Gets the number of elements in the list.
         *
         * @return the size
         */
        int size() {
            return size;
        }

        /**
         * Clear the list.
         */
        void clear() {
            size = 0;
        }

        /**
         * Shuffle the list.
         *
         * @param randomIntFunction Function maps positive {@code x} randomly to {@code [0, x)}.
         */
        void shuffle(IntUnaryOperator randomIntFunction) {
            // Fisher-Yates shuffle
            final int[] array = data;
            for (int i = size; i > 1; i--) {
                swap(array, i - 1, randomIntFunction.applyAsInt(i));
            }
        }

        /**
         * Swaps the two specified elements in the specified array.
         *
         * @param array Data array
         * @param i     First index
         * @param j     Second index
         */
        private static void swap(int[] array, int i, int j) {
            final int tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    /**
     * Represents the position of a {@code double} value in a data array.
     */
    private static class DataPosition {
        /** Data value. */
        private final double value;
        /** Data position. */
        private final int position;

        /**
         * Create an instance with the given value and position.
         *
         * @param value Data value.
         * @param position Data position.
         */
        DataPosition(double value, int position) {
            this.value = value;
            this.position = position;
        }

        /**
         * Returns the value.
         *
         * @return value
         */
        double getValue() {
            return value;
        }

        /**
         * Returns the data position.
         *
         * @return position
         */
        int getPosition() {
            return position;
        }
    }
}
