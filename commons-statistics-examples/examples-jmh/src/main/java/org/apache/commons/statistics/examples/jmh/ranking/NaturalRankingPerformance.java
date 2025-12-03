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

package org.apache.commons.statistics.examples.jmh.ranking;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.DirichletSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.ranking.NaturalRanking;
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
 * Executes a benchmark of the ranking of {@code double} array data.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class NaturalRankingPerformance {
    /**
     * Source of {@code double} array data to rank.
     */
    @State(Scope.Benchmark)
    public static class DataSource {
        /** Data length. */
        @Param({"10000"})
        private int length;
        /** Fraction of total length that has tied (equal) data. */
        @Param({"0", "0.1", "0.5"})
        private double tieFraction;
        /** Count of the number of distinct runs of tied (equal) data. */
        @Param({"20"})
        private int ties;
        /** Concentration parameter for the distribution of tie lengths. */
        @Param({"1"})
        private double alpha;

        /** Data to rank. */
        private double[] data;

        /**
         * @return the data
         */
        public double[] getData() {
            return data;
        }

        /**
         * Create the data to rank.
         */
        @Setup(Level.Iteration)
        public void setup() {
            // Data will be randomized per iteration
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            data = createData(rng, length, tieFraction, ties, alpha);
        }

        /**
         * Creates the data.
         * This is package-private for testing.
         *
         * @param rng Source of randomness
         * @param length Data length.
         * @param tieFraction Fraction of total length that has tied (equal) data.
         * @param ties Count of the number of distinct runs of tied (equal) data.
         * @param alpha Concentration parameter for the distribution of tie lengths.
         * @return the data
         */
        static double[] createData(UniformRandomProvider rng,
                                   int length,  double tieFraction, int ties, double alpha) {
            assert length > 0 : "Invalid data length";
            assert tieFraction <= 1 && tieFraction >= 0 : "Invalid tie fraction";
            assert ties >= 0 : "Invalid number of ties";
            assert alpha >= 0 : "Invalid concentration parameter";

            // The data will contain n regions of data, each with the same value,
            // then the rest is a sequence. This is then shuffled.
            final double[] data = new double[length];
            int count = 0;
            int value = 0;

            // Create tie regions
            final int tiesLength = (int) Math.round(tieFraction * length);
            if (ties > 0 && tiesLength > 0) {
                // Cut the ties length into parts.
                // Note that due to randomness some lengths may be <= 1 and therefore not a tie.
                // This increasingly occurs as alpha -> 0.
                // See: https://en.wikipedia.org/wiki/Dirichlet_distribution#String_cutting
                final double[] tieFractions = DirichletSampler.symmetric(rng, ties, alpha).sample();
                final int[] lengths = Arrays.stream(tieFractions)
                                            .mapToInt(f -> (int) Math.round(f * tiesLength))
                                            .toArray();
                for (final int len : lengths) {
                    ++value;
                    // Lengths may sum to more than tiesLength due to rounding so
                    // consume most we can
                    for (int i = Math.min(len, tiesLength - count); i > 0; i--) {
                        data[count++] = value;
                    }
                }
            }

            // Fill remaining values
            while (count < data.length) {
                data[count++] = ++value;
            }

            // Fisher-Yates shuffle
            for (int i = data.length; i > 1; i--) {
                swap(data, i - 1, rng.nextInt(i));
            }

            return data;
        }

        /**
         * Swaps the two specified elements in the specified array.
         *
         * @param array Data array
         * @param i     First index
         * @param j     Second index
         */
        private static void swap(double[] array, int i, int j) {
            final double tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    /**
     * Source of a function to rank {@code double} array data.
     */
    @State(Scope.Benchmark)
    public static class RankingSource {
        /** Name for baseline implementation. */
        private static final String METHOD_BASELINE = "baseline";
        /** Name for Commons Statistics implementation. */
        private static final String METHOD_STATISTICS = "statistics";
        /** Name for Commons Math3 implementation. */
        private static final String METHOD_MATH3 = "math3";
        /** The ranking method. */
        @Param({"baseline", METHOD_STATISTICS, METHOD_MATH3})
        private String ranking;

        /** The ranking function. */
        private UnaryOperator<double[]> fun;

        /**
         * @return the ranking function
         */
        public UnaryOperator<double[]> getFunction() {
            return fun;
        }

        /**
         * Create the ranking function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            fun = createFunction(ranking);
        }

        /**
         * Creates the ranking function.
         * This is package-private for testing.
         *
         * @param name The function name.
         * @return the ranking function
         */
        static UnaryOperator<double[]> createFunction(String name) {
            if (METHOD_BASELINE.equals(name)) {
                return new SortRanking();
            } else if (METHOD_STATISTICS.equals(name)) {
                return new NaturalRanking();
            } else if (METHOD_MATH3.equals(name)) {
                return new org.apache.commons.math3.stat.ranking.NaturalRanking()::rank;
            } else {
                throw new IllegalStateException("Unknown method: " + name);
            }
        }

        /**
         * Gets the names for valid ranking functions.
         * This is package-private for testing.
         *
         * @return the function names
         */
        static String[] getFunctionNames() {
            // Do not return the baseline method
            return new String[] {METHOD_STATISTICS, METHOD_MATH3};
        }

        /**
         * Class to create a ranking using a sort of the data. This ranking does not
         * resolve ties and is a baseline for the speed of {@link Arrays#sort(Object[])}.
         */
        private static final class SortRanking implements UnaryOperator<double[]> {
            @Override
            public double[] apply(double[] in) {
                final DataPosition[] data = new DataPosition[in.length];
                for (int i = 0; i < in.length; i++) {
                    data[i] = new DataPosition(in[i], i);
                }
                Arrays.sort(data);
                final double[] out = new double[in.length];
                for (int i = 0; i < in.length; i++) {
                    out[data[i].getPosition()] = i + 1.0;
                }
                return out;
            }

            // Copied from NaturalRanking for baseline equivalence.

            /**
             * Represents the position of a {@code double} value in a data array. The
             * Comparable interface is implemented so Arrays.sort can be used to sort an
             * array of data positions by value. Note that the implicitly defined natural
             * ordering is NOT consistent with equals.
             */
            private static class DataPosition implements Comparable<DataPosition>  {
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
                 * Compare this value to another.
                 * Only the <strong>values</strong> are compared.
                 *
                 * @param other the other pair to compare this to
                 * @return result of {@code Double.compare(value, other.value)}
                 */
                @Override
                public int compareTo(DataPosition other) {
                    return Double.compare(value, other.value);
                }

                // equals() and hashCode() are not implemented; see MATH-610 for discussion.

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
    }

    /**
     * Rank {@code double} array data.
     *
     * @param source Source of the data.
     * @param ranking Source of the ranking function.
     * @return the ranking
     */
    @Benchmark
    public double[] sample(DataSource source, RankingSource ranking) {
        return ranking.getFunction().apply(source.getData());
    }
}
