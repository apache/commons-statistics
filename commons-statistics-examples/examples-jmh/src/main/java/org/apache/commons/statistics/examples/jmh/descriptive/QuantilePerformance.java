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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.logging.Logger;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ArraySampler;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteUniformSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateDiscreteSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.Quantile;
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
 * Executes a benchmark of the creation of a quantile from array data.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx8192M"})
public class QuantilePerformance {
    /** Use the JDK sort function. */
    private static final String JDK = "JDK";
    /** Commons Math 3 Percentile implementation. */
    private static final String CM3 = "CM3";
    /** Commons Math 4 Percentile implementation. */
    private static final String CM4 = "CM4";
    /** Commons Statistics implementation. */
    private static final String STATISTICS = "Statistics";

    /** Random source. */
    private static final RandomSource RANDOM_SOURCE = RandomSource.XO_RO_SHI_RO_128_PP;

    /**
     * Source of {@code double} array data.
     *
     * <p>By default this uses the adverse input test suite from figure 1 in Bentley and McIlroy
     * (1993) Engineering a sort function, Software, practice and experience, Vol.23(11),
     * 1249–1265.
     *
     * <p>An alternative set of data is from Valois (2000) Introspective sorting and selection
     * revisited, Software, practice and experience, Vol.30(6), 617-638.
     *
     * <p>Note
     *
     * <p>This data source has been adapted from {@code o.a.c.numbers.examples.jmh.arrays}.
     *
     * <p>Random distribution mode
     *
     * <p>The default BM configuration includes random samples generated as a family of
     * single samples created from ranges that are powers of two [0, 2^i). This small set
     * of samples is only a small representation of randomness. For small lengths this may
     * only be a few random samples.
     *
     * <p>The data source can be changed to generate a fixed number of random samples
     * using a uniform distribution [0, n]. For this purpose the distribution must be set
     * to {@link Distribution#RANDOM} and the {@code samples} set above
     * zero. The inclusive upper bound {@code n} is set using the {@code seed}.
     * If this is zero then the default is {@link Integer#MAX_VALUE}.
     */
    @State(Scope.Benchmark)
    public abstract static class AbstractDataSource {
        /** All distributions / modifications. */
        private static final String ALL = "all";
        /** All distributions / modifications in the Bentley and McIlroy test suite. */
        private static final String BM = "bm";
        /** All distributions in the Valois test suite. These currently ignore the seed.
         * To replicate Valois used a fixed seed and the copy modification. */
        private static final String VALOIS = "valois";
        /** Flag to determine if the data size should be logged. This is useful to be
         * able to determine the execution time per sample when the number of samples
         * is dynamically created based on the data length, range and seed. */
        private static final AtomicInteger LOG_SIZE = new AtomicInteger();

        /**
         * The type of distribution.
         */
        enum Distribution {
            // B&M (1993)

            /** sawtooth distribution. */
            SAWTOOTH,
            /** random distribution. */
            RANDOM,
            /** stagger distribution. */
            STAGGER,
            /** plateau distribution. */
            PLATEAU,
            /** shuffle distribution. */
            SHUFFLE,

            /** sharktooth distribution. This is an addition to the original suite of B & M
             * and is not included in the test suite by default and must be specified.
             *
             * <p>An ascending then descending sequence is also known as organpipe in
             * Valois (2000),
             * Introspective sorting and selection revisited,
             * Software–Practice and Experience 30, 617–638.
             * This version allows multiple ascending/descending runs in the same length. */
            SHARKTOOTH,

            // Valois (2000)

            /** Sorted. */
            SORTED,
            /** Permutation of ones and zeros. */
            ONEZERO,
            /** Musser's median-of-3 killer. */
            M3KILLER,
            /** A sorted sequence rotated left once. */
            ROTATED,
            /** Musser's two-faced sequence (the median-of-3 killer with two random permutations). */
            TWOFACED,
            /** An ascending then descending sequence. */
            ORGANPIPE;
        }

        /**
         * The type of data modification.
         */
        enum Modification {
            /** copy modification. */
            COPY,
            /** reverse modification. */
            REVERSE,
            /** reverse front-half modification. */
            REVERSE_FRONT,
            /** reverse back-half modification. */
            REVERSE_BACK,
            /** sort modification. */
            SORT,
            /** descending modification (this is an addition to the original suite of B & M).
             * It is useful for testing worst case performance, e.g. insertion sort performs
             * poorly on descending data. Heapselect using a max heap would perform poorly
             * if data is processed in the forward direction as all elements must be inserted.
             *
             * <p>This is not included in the test suite by default and must be specified.
             * Note that the Shuffle distribution with a very large seed 'm' is effectively an
             * ascending sequence and will be reversed to descending as part of the original
             * B&M suite of data. */
            DESCENDING,
            /** dither modification. */
            DITHER;
        }

        /** Order. This is randomized to ensure that successive calls do not partition
         * similar distributions. Randomized per invocation to avoid the JVM 'learning'
         * branch decisions to take in small data sets. */
        protected int[] order;
        /** Cached source of randomness. */
        protected UniformRandomProvider rng;

        /** Type of data. Multiple types can be specified in the same string using
         * lower/upper case, delimited using ':'. */
        @Param({BM})
        private String distribution = BM;

        /** Type of data modification. Multiple types can be specified in the same string using
         * lower/upper case, delimited using ':'. */
        @Param({BM})
        private String modification = BM;

        /** Extra range to add to the data length.
         * E.g. Use 1 to force use of odd and even length samples for the median. */
        @Param({"1"})
        private int range = 1;

        /** Sample 'seed'. This is {@code m} in Bentley and McIlroy's test suite.
         * If set to zero the default is to use powers of 2 based on sample size. */
        @Param({"0"})
        private int seed;

        /** Sample offset. This is used to shift each distribution to create different data.
         * It is advanced on each invocation of {@link #setup()}. */
        @Param({"0"})
        private int offset;

        /** Number of samples. Applies only to the random distribution. In this case
         * the length of the data is randomly chosen in {@code [length, length + range)}. */
        @Param({"0"})
        private int samples;

        /** RNG seed. Created using ThreadLocalRandom.current().nextLong(). This is advanced
         * for the random distribution mode per iteration. Each benchmark executed by
         * JMH will use the same random data, even across JVMs.
         *
         * <p>If this is zero then a random seed is chosen. */
        @Param({"-7450238124206088695"})
        private long rngSeed = -7450238124206088695L;

        /** Data. This is stored as integer data which saves memory. Note that when ranking
         * data it is not necessary to have the full range of the double data type; the same
         * number of unique values can be recorded in an array using an integer type.
         * Returning a double[] forces a copy to be generated for destructive sorting /
         * partitioning methods. */
        private int[][] data;

        /**
         * Gets the sample for the given {@code index}.
         *
         * <p>This is returned in a randomized order per iteration.
         *
         * @param index Index.
         * @return the data sample
         */
        public double[] getData(int index) {
            return getDataSample(order[index]);
        }

        /**
         * Gets the sample for the given {@code index}.
         *
         * <p>This is returned in a randomized order per iteration.
         *
         * @param index Index.
         * @return the data sample
         */
        public int[] getIntData(int index) {
            return getIntDataSample(order[index]);
        }

        /**
         * Gets the sample for the given {@code index}.
         *
         * @param index Index.
         * @return the data sample
         */
        protected double[] getDataSample(int index) {
            final int[] a = data[index];
            final double[] x = new double[a.length];
            for (int i = -1; ++i < a.length;) {
                x[i] = a[i];
            }
            return x;
        }

        /**
         * Gets the sample for the given {@code index}.
         *
         * @param index Index.
         * @return the data sample
         */
        protected int[] getIntDataSample(int index) {
            // For parity with other methods do not use data.clone()
            final int[] a = data[index];
            final int[] x = new int[a.length];
            for (int i = -1; ++i < a.length;) {
                x[i] = a[i];
            }
            return x;
        }

        /**
         * Gets the sample size for the given {@code index}.
         *
         * @param index Index.
         * @return the data sample size
         */
        public int getDataSize(int index) {
            return data[index].length;
        }

        /**
         * Get the number of data samples.
         *
         * <p>Note: This data source will create a permutation order per invocation based on
         * this size. Per-invocation control in JMH is recommended for methods that take
         * more than 1 millisecond to execute. For very small data and/or fast methods
         * this may not be achievable. Child classes may override this value to create
         * a large number of repeats of the same data per invocation. Any class performing
         * this should also override {@link #getData(int)} to prevent index out of bound errors.
         * This can be done by mapping the index to the original index using the number of repeats
         * e.g. {@code original index = index / repeats}.
         *
         * @return the number of samples
         */
        public int size() {
            return data.length;
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            Objects.requireNonNull(distribution);
            Objects.requireNonNull(modification);

            // Set-up using parameters (may throw)
            final EnumSet<Distribution> dist = getDistributions();
            final int length = getLength();
            if (length < 1) {
                throw new IllegalStateException("Unsupported length: " + length);
            }
            // Note: Bentley-McIlroy use n in {100, 1023, 1024, 1025}.
            // Here we only support a continuous range. The range is important
            // for the median as it will require one or two points to partition
            // if the length is odd or even.
            final int r = range > 0 ? range : 0;
            if (length + (long) r > Integer.MAX_VALUE) {
                throw new IllegalStateException("Unsupported upper length: " + length);
            }
            final int length2 = length + r;

            // Allow pseudorandom seeding
            if (rngSeed == 0) {
                rngSeed = RandomSource.createLong();
            }
            if (rng == null) {
                // First call, create objects
                rng = RANDOM_SOURCE.create(rngSeed);
            }

            // Special case for random distribution mode
            if (dist.contains(Distribution.RANDOM) && dist.size() == 1 && samples > 0) {
                data = new int[samples][];
                final int upper = seed > 0 ? seed : Integer.MAX_VALUE;
                final SharedStateDiscreteSampler s1 = DiscreteUniformSampler.of(rng, 0, upper);
                final SharedStateDiscreteSampler s2 = DiscreteUniformSampler.of(rng, length, length2);
                for (int i = 0; i < data.length; i++) {
                    final int[] a = new int[s2.sample()];
                    for (int j = a.length; --j >= 0;) {
                        a[j] = s1.sample();
                    }
                    data[i] = a;
                }
                return;
            }

            // New data per iteration
            data = null;
            final int o = offset;
            offset = rng.nextInt();

            final EnumSet<Modification> mod = getModifications();

            // Data using the RNG will be randomized only once.
            // Here we use the same seed for parity across methods.
            // Note that most distributions do not use the source of randomness.
            final ArrayList<int[]> sampleData = new ArrayList<>();
            for (int n = length; n <= length2; n++) {
                // Note: Large lengths may wish to limit the range of m to limit
                // the memory required to store the samples. Currently a single
                // m is supported via the seed parameter.
                // Default seed will create ceil(log2(2*n)) * 5 dist * 6 mods samples:
                // MAX  = 32 * 5 * 7 * (2^31-1) * 4 bytes == 7679 GiB
                // HUGE = 31 * 5 * 7 * 2^30 * 4 bytes == 3719 GiB
                // BIG  = 21 * 5 * 7 * 2^20 * 4 bytes == 2519 MiB  <-- within configured JVM -Xmx
                // MED  = 11 * 5 * 7 * 2^10 * 4 bytes == 1318 KiB
                // (This excludes the descending modification.)
                // It is possible to create lengths above 2^30 using a single distribution,
                // modification, and seed:
                // MAX1 = 1 * 1 * 1 * (2^31-1) * 4 bytes == 8191 MiB
                // However this is then used to create double[] data thus requiring an extra
                // ~16GiB memory for the sample to partition.
                for (final int m : createSeeds(seed, n)) {
                    final List<int[]> d = createDistributions(dist, rng, n, m, o);
                    for (int i = 0; i < d.size(); i++) {
                        final int[] x = d.get(i);
                        if (mod.contains(Modification.COPY)) {
                            // Don't copy! All other methods generate copies
                            // so we can use this in-place.
                            sampleData.add(x);
                        }
                        if (mod.contains(Modification.REVERSE)) {
                            sampleData.add(reverse(x, 0, n));
                        }
                        if (mod.contains(Modification.REVERSE_FRONT)) {
                            sampleData.add(reverse(x, 0, n >>> 1));
                        }
                        if (mod.contains(Modification.REVERSE_BACK)) {
                            sampleData.add(reverse(x, n >>> 1, n));
                        }
                        // Only sort once
                        if (mod.contains(Modification.SORT) ||
                            mod.contains(Modification.DESCENDING)) {
                            final int[] y = x.clone();
                            Arrays.sort(y);
                            if (mod.contains(Modification.DESCENDING)) {
                                sampleData.add(reverse(y, 0, n));
                            }
                            if (mod.contains(Modification.SORT)) {
                                sampleData.add(y);
                            }
                        }
                        if (mod.contains(Modification.DITHER)) {
                            sampleData.add(dither(x));
                        }
                    }
                }
            }
            data = sampleData.toArray(int[][]::new);
            if (LOG_SIZE.getAndSet(length) != length) {
                Logger.getLogger(getClass().getName()).info(
                    () -> String.format("Data length: [%d, %d] n=%d", length, length2, data.length));
            }
        }

        /**
         * Create the order to process the indices.
         *
         * <p>JMH recommends that invocations should take at
         * least 1 millisecond for timings to be usable. In practice there should be
         * enough data that processing takes much longer than a few milliseconds.
         */
        @Setup(Level.Invocation)
        public void createOrder() {
            if (order == null) {
                // First call, create objects
                order = PermutationSampler.natural(size());
            }
            ArraySampler.shuffle(rng, order);
        }

        /**
         * @return the distributions
         */
        private EnumSet<Distribution> getDistributions() {
            EnumSet<Distribution> dist;
            if (BM.equals(distribution)) {
                dist = EnumSet.of(
                    Distribution.SAWTOOTH,
                    Distribution.RANDOM,
                    Distribution.STAGGER,
                    Distribution.PLATEAU,
                    Distribution.SHUFFLE);
            } else if (VALOIS.equals(distribution)) {
                dist = EnumSet.of(
                    Distribution.RANDOM,
                    Distribution.SORTED,
                    Distribution.ONEZERO,
                    Distribution.M3KILLER,
                    Distribution.ROTATED,
                    Distribution.TWOFACED,
                    Distribution.ORGANPIPE);
            } else {
                dist = getEnumFromParam(Distribution.class, distribution);
            }
            return dist;
        }

        /**
         * @return the modifications
         */
        private EnumSet<Modification> getModifications() {
            EnumSet<Modification> mod;
            if (BM.equals(modification)) {
                // Modifications are from Bentley and McIlroy
                mod = EnumSet.allOf(Modification.class);
                // ... except descending
                mod.remove(Modification.DESCENDING);
            } else if (VALOIS.equals(modification)) {
                // For convenience alias Valois to copy
                mod = EnumSet.of(Modification.COPY);
            } else {
                mod = getEnumFromParam(Modification.class, modification);
            }
            return mod;
        }

        /**
         * Gets all the enum values of the given class from the parameters.
         *
         * @param <E> Enum type.
         * @param cls Class of the enum.
         * @param parameters Parameters (multiple values delimited by ':')
         * @return the enum values
         */
        static <E extends Enum<E>> EnumSet<E> getEnumFromParam(Class<E> cls, String parameters) {
            if (ALL.equals(parameters)) {
                return EnumSet.allOf(cls);
            }
            final EnumSet<E> set = EnumSet.noneOf(cls);
            final String s = parameters.toUpperCase(Locale.ROOT);
            for (final E e : cls.getEnumConstants()) {
                // Scan for the name
                for (int i = s.indexOf(e.name(), 0); i >= 0; i = s.indexOf(e.name(), i)) {
                    // Ensure a full match to the name:
                    // either at the end of the string, or followed by the delimiter
                    i += e.name().length();
                    if (i == s.length() || s.charAt(i) == ':') {
                        set.add(e);
                        break;
                    }
                }
            }
            if (set.isEmpty()) {
                throw new IllegalStateException("Unknown parameters: " + parameters);
            }
            return set;
        }

        /**
         * Creates the seeds.
         *
         * <p>This can be pasted into a JShell terminal to verify it works for any size
         * {@code 1 <= n < 2^31}. With the default behaviour all seeds {@code m} are
         * strictly positive powers of 2 and the highest seed should be below {@code 2*n}.
         *
         * @param seed Seed (use 0 for default; or provide a strictly positive {@code 1 <= m <= 2^31}).
         * @param n Sample size.
         * @return the seeds
         */
        private static int[] createSeeds(int seed, int n) {
            // Allow [1, 2^31] (note 2^31 is negative but handled as a power of 2)
            if (seed - 1 >= 0) {
                return new int[] {seed};
            }
            // Bentley-McIlroy use:
            // for: m = 1; m < 2 * n; m *= 2
            // This has been modified here to handle n up to MAX_VALUE
            // by knowing the count of m to generate as the power of 2 >= n.

            // ceil(log2(n)) + 1 == ceil(log2(2*n)) but handles MAX_VALUE
            int c = 33 - Integer.numberOfLeadingZeros(n - 1);
            final int[] seeds = new int[c];
            c = 0;
            for (int m = 1; c != seeds.length; m *= 2) {
                seeds[c++] = m;
            }
            return seeds;
        }

        /**
         * Creates the distribution samples. Handles {@code m = 2^31} using
         * {@link Integer#MIN_VALUE}.
         *
         * <p>The offset is used to adjust each distribution to generate a different
         * output. Only applies to distributions that do not use the source of randomness.
         *
         * <p>Distributions are generated in enum order and recorded in the output {@code sampleDist}.
         * Distributions that are a constant value at {@code m == 1} are not generated.
         * This case is handled by the plateau distribution which will be a constant value
         * except one occurrence of zero.
         *
         * @param dist Distributions.
         * @param rng Source of randomness.
         * @param n Length of the sample.
         * @param m Sample seed (in [1, 2^31])
         * @param o Offset.
         * @return the samples
         */
        private static List<int[]> createDistributions(EnumSet<Distribution> dist,
                UniformRandomProvider rng, int n, int m, int o) {
            final ArrayList<int[]> distData = new ArrayList<>(6);
            int[] x;
            // B&M (1993)
            if (dist.contains(Distribution.SAWTOOTH) && m != 1) {
                distData.add(x = new int[n]);
                // i % m
                // Typical case m is a power of 2 so we can use a mask
                // Use the offset.
                final int mask = m - 1;
                if ((m & mask) == 0) {
                    for (int i = -1; ++i < n;) {
                        x[i] = (i + o) & mask;
                    }
                } else {
                    // User input seed. Start at the offset.
                    int j = Integer.remainderUnsigned(o, m);
                    for (int i = -1; ++i < n;) {
                        j = j % m;
                        x[i] = j++;
                    }
                }
            }
            if (dist.contains(Distribution.RANDOM) && m != 1) {
                distData.add(x = new int[n]);
                // rand() % m
                // A sampler is faster than rng.nextInt(m); the sampler has an inclusive upper.
                final SharedStateDiscreteSampler s = DiscreteUniformSampler.of(rng, 0, m - 1);
                for (int i = -1; ++i < n;) {
                    x[i] = s.sample();
                }
            }
            if (dist.contains(Distribution.STAGGER)) {
                distData.add(x = new int[n]);
                // Overflow safe: (i * m + i) % n
                final long nn = n;
                final long oo = Integer.toUnsignedLong(o);
                for (int i = -1; ++i < n;) {
                    final long j = i + oo;
                    x[i] = (int) ((j * m + j) % nn);
                }
            }
            if (dist.contains(Distribution.PLATEAU)) {
                distData.add(x = new int[n]);
                // min(i, m)
                for (int i = Math.min(n, m); --i >= 0;) {
                    x[i] = i;
                }
                for (int i = m - 1; ++i < n;) {
                    x[i] = m;
                }
                // Rotate
                final int n1 = Integer.remainderUnsigned(o, n);
                if (n1 != 0) {
                    final int[] a = x.clone();
                    final int n2 = n - n1;
                    System.arraycopy(a, 0, x, n1, n2);
                    System.arraycopy(a, n2, x, 0, n1);
                }
            }
            if (dist.contains(Distribution.SHUFFLE) && m != 1) {
                distData.add(x = new int[n]);
                // rand() % m ? (j += 2) : (k += 2)
                final SharedStateDiscreteSampler s = DiscreteUniformSampler.of(rng, 0, m - 1);
                for (int i = -1, j = 0, k = 1; ++i < n;) {
                    x[i] = s.sample() != 0 ? (j += 2) : (k += 2);
                }
            }
            // Extra - based on organpipe with a variable ascending/descending length
            if (dist.contains(Distribution.SHARKTOOTH) && m != 1) {
                distData.add(x = new int[n]);
                // ascending-descending runs
                int i = -1;
                int j = (o & Integer.MAX_VALUE) % m - 1;
                OUTER:
                for (;;) {
                    while (++j < m) {
                        if (++i == n) {
                            break OUTER;
                        }
                        x[i] = j;
                    }
                    while (--j >= 0) {
                        if (++i == n) {
                            break OUTER;
                        }
                        x[i] = j;
                    }
                }
            }
            // Valois (2000)
            if (dist.contains(Distribution.SORTED)) {
                distData.add(x = new int[n]);
                for (int i = -1; ++i < n;) {
                    x[i] = i;
                }
            }
            if (dist.contains(Distribution.ONEZERO)) {
                distData.add(x = new int[n]);
                // permutation of floor(n/2) ones and ceil(n/2) zeroes.
                // For convenience this uses random ones and zeros to avoid a shuffle
                // and simply reads bits from integers. The distribution will not
                // be exactly 50:50.
                final int end = n & ~31;
                for (int i = 0; i < end; i += 32) {
                    int z = rng.nextInt();
                    for (int j = -1; ++j < 32;) {
                        x[i + j] = z & 1;
                        z >>>= 1;
                    }
                }
                for (int i = end; ++i < n;) {
                    x[i] = rng.nextBoolean() ? 1 : 0;
                }
            }
            if (dist.contains(Distribution.M3KILLER)) {
                distData.add(x = new int[n]);
                medianOf3Killer(x);
            }
            if (dist.contains(Distribution.ROTATED)) {
                distData.add(x = new int[n]);
                // sorted sequence rotated left once
                // 1, 2, 3, ..., n-1, n, 0
                for (int i = 0; i < n;) {
                    x[i] = ++i;
                }
                x[n - 1] = 0;
            }
            if (dist.contains(Distribution.TWOFACED)) {
                distData.add(x = new int[n]);
                // Musser's two faced randomly permutes a median-of-3 killer in
                // 4 floor(log2(n)) through n/2 and n/2 + 4 floor(log2(n)) through n
                medianOf3Killer(x);
                final int j = 4 * (31 - Integer.numberOfLeadingZeros(n));
                final int n2 = n >>> 1;
                ArraySampler.shuffle(rng, x, j, n2);
                ArraySampler.shuffle(rng, x, n2 + j, n);
            }
            if (dist.contains(Distribution.ORGANPIPE)) {
                distData.add(x = new int[n]);
                // 0, 1, 2, 3, ..., 3, 2, 1, 0
                // n should be even to leave two equal values in the middle, otherwise a single
                for (int i = -1, j = n; ++i <= --j;) {
                    x[i] = i;
                    x[j] = i;
                }
            }
            return distData;
        }

        /**
         * Create Musser's median-of-3 killer sequence (in-place).
         *
         * @param x Data.
         */
        private static void medianOf3Killer(int[] x) {
            // This uses the original K_2k sequence from Musser (1997)
            // Introspective sorting and selection algorithms,
            // Software—Practice and Experience, 27(8), 983–993.
            // A true median-of-3 killer requires n to be an even integer divisible by 4,
            // i.e. k is an even positive integer. This causes a median-of-3 partition
            // strategy to produce a sequence of n/4 partitions into sub-sequences of
            // length 2 and n-2, 2 and n-4, ..., 2 and n/2.
            // 1   2   3   4   5       k-2   k-1  k   k+1 k+2 k+3     2k-1  2k
            // 1, k+1, 3, k+3, 5, ..., 2k-3, k-1 2k-1  2   4   6  ... 2k-2  2k
            final int n = x.length;
            final int k = n >>> 1;
            for (int i = 0; i < k; i++) {
                x[i] = ++i;
                x[i] = k + i;
            }
            for (int i = k - 1, j = 2; ++i < n; j += 2) {
                x[i] = j;
            }
        }

        /**
         * Return a (part) reversed copy of the data.
         *
         * @param x Data.
         * @param from Start index to reverse (inclusive).
         * @param to End index to reverse (exclusive).
         * @return the copy
         */
        private static int[] reverse(int[] x, int from, int to) {
            final int[] a = x.clone();
            for (int i = from - 1, j = to; ++i < --j;) {
                final int v = a[i];
                a[i] = a[j];
                a[j] = v;
            }
            return a;
        }

        /**
         * Return a dithered copy of the data.
         *
         * @param x Data.
         * @return the copy
         */
        private static int[] dither(int[] x) {
            final int[] a = x.clone();
            for (int i = a.length; --i >= 0;) {
                // Bentley-McIlroy use i % 5.
                // It is important this is not a power of 2 so it will not coincide
                // with patterns created in the data using the default m powers-of-2.
                a[i] += i % 5;
            }
            return a;
        }

        /**
         * Gets the minimum length of the data.
         * The actual length is enumerated in {@code [length, length + range]}.
         *
         * @return the length
         */
        protected abstract int getLength();

        /**
         * Gets the range.
         *
         * @return the range
         */
        final int getRange() {
            return range;
        }
    }

    /**
     * Source of {@code double} array data.
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
     * Source of quantiles.
     */
    @State(Scope.Benchmark)
    public static class QuantileSource {
        /** Quantiles.
         * Delimited by ':' to allow use via the JMH command-line parser which
         * uses ',' as the delimiter. */
        @Param({"0.25:0.5:0.75",
                "0.01:0.99",
                "1e-100:1.0", // min,max: CM implementations do not allow p=0.0
                "0.25:0.75",
                "0.001:0.005:0.01:0.02:0.05:0.1:0.5",
                "0.01:0.05:0.1:0.5:0.9:0.95:0.99"})
        private String quantiles;

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
        @Setup
        public void setup() {
            data = Arrays.stream(quantiles.split(":")).mapToDouble(Double::parseDouble).toArray();
        }
    }

    /**
     * Source of quantiles uniformly spaced within a range.
     */
    @State(Scope.Benchmark)
    public static class QuantileRangeSource {
        /** Lower quantile. */
        @Param({"0.01"})
        private double lowerQ;
        /** Upper quantile. */
        @Param({"0.99"})
        private double upperQ;
        /** Number of quantiles. */
        @Param({"100"})
        private int quantiles;

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
        @Setup
        public void setup() {
            if (quantiles < 2) {
                throw new IllegalStateException("Bad quantile count: " + quantiles);
            }
            if (!(lowerQ >= 0 && upperQ <= 1)) {
                throw new IllegalStateException("Bad quantile range: [" + lowerQ + ", " + upperQ + "]");
            }
            data = new double[quantiles];
            for (int i = 0; i < quantiles; i++) {
                // Create u in [0, 1]
                final double u = i / (quantiles - 1.0);
                data[i] = (1 - u) * lowerQ + u * upperQ;
            }
        }
    }

    /**
     * Source of a {@link BinaryOperator} for a {@code double[]} and quantiles.
     */
    @State(Scope.Benchmark)
    public static class DoubleQuantileFunctionSource {
        /** Name of the source. */
        @Param({JDK, CM3, CM4, STATISTICS})
        private String name;

        /** The action. */
        private BinaryOperator<double[]> function;

        /**
         * @return the function
         */
        public BinaryOperator<double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            // Note: Functions should not defensively copy the data
            // as a clone is passed in from the data source.
            if (JDK.equals(name)) {
                function = DoubleQuantileFunctionSource::sortQuantile;
            } else if (CM3.equals(name)) {
                // No way to avoid a data copy here. CM does
                // defensive copying for most array input.
                final org.apache.commons.math3.stat.descriptive.rank.Percentile s =
                    new org.apache.commons.math3.stat.descriptive.rank.Percentile().withNaNStrategy(
                        org.apache.commons.math3.stat.ranking.NaNStrategy.FIXED);
                function = (x, p) -> {
                    final double[] q = new double[p.length];
                    s.setData(x);
                    for (int i = 0; i < p.length; i++) {
                        // Convert quantile to percentile
                        q[i] = s.evaluate(p[i] * 100);
                    }
                    return q;
                };
            } else if (CM4.equals(name)) {
                // CM4 differs from CM3 by using Double.compare(x, y) for comparisons.
                // This handles NaN and signed zeros but is slower than using < and >.
                final org.apache.commons.math4.legacy.stat.descriptive.rank.Percentile s =
                    new org.apache.commons.math4.legacy.stat.descriptive.rank.Percentile().withNaNStrategy(
                        org.apache.commons.math4.legacy.stat.ranking.NaNStrategy.FIXED);
                function = (x, p) -> {
                    final double[] q = new double[p.length];
                    s.setData(x);
                    for (int i = 0; i < p.length; i++) {
                        // Convert quantile to percentile
                        q[i] = s.evaluate(p[i] * 100);
                    }
                    return q;
                };
            } else if (STATISTICS.equals(name)) {
                function = Quantile.withDefaults()::evaluate;
            } else {
                throw new IllegalStateException("Unknown double[] function: " + name);
            }
        }

        /**
         * Sort the values and compute the median.
         *
         * @param values Values.
         * @param p p-th quantiles to compute.
         * @return the quantiles
         */
        private static double[] sortQuantile(double[] values, double[] p) {
            // Implicit NPE
            final int n = values.length;
            if (p.length == 0) {
                throw new IllegalArgumentException("No quantiles specified for double[] data");
            }
            for (final double pp : p) {
                checkQuantile(pp);
            }
            // Special cases
            final double[] q = new double[p.length];
            if (n <= 1) {
                Arrays.fill(q, n == 0 ? Double.NaN : values[0]);
                return q;
            }
            // A sort is required
            Arrays.sort(values);
            for (int i = 0; i < p.length; i++) {
                // EstimationMethod.HF6 (as per the Apache Commons Math Percentile
                // legacy implementation)
                final double pos = p[i] * (n + 1);
                final double fpos = Math.floor(pos);
                final int j = (int) fpos;
                final double g = pos - fpos;
                if (j < 1) {
                    q[i] = values[0];
                } else if (j >= n) {
                    q[i] = values[n - 1];
                } else {
                    q[i] = (1 - g) * values[j - 1] + g * values[j];
                }
            }
            return q;
        }
    }


    /**
     * Source of a {@link BiFunction} for an {@code int[]} and quantiles.
     */
    @State(Scope.Benchmark)
    public static class IntQuantileFunctionSource {
        /** Name of the source. */
        @Param({JDK, CM3, CM4, STATISTICS})
        private String name;

        /** The action. */
        private BiFunction<int[], double[], double[]> function;

        /**
         * @return the function
         */
        public BiFunction<int[], double[], double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            // Note: Functions should not defensively copy the data
            // as a clone is passed in from the data source.
            if (JDK.equals(name)) {
                function = IntQuantileFunctionSource::sortQuantile;
            } else if (STATISTICS.equals(name)) {
                function = Quantile.withDefaults()::evaluate;
            } else {
                throw new IllegalStateException("Unknown int[] function: " + name);
            }
        }

        /**
         * Sort the values and compute the median.
         *
         * @param values Values.
         * @param p p-th quantiles to compute.
         * @return the quantiles
         */
        private static double[] sortQuantile(int[] values, double[] p) {
            // Implicit NPE
            final int n = values.length;
            if (p.length == 0) {
                throw new IllegalArgumentException("No quantiles specified for int[] data");
            }
            for (final double pp : p) {
                checkQuantile(pp);
            }
            // Special cases
            final double[] q = new double[p.length];
            if (n <= 1) {
                Arrays.fill(q, n == 0 ? Double.NaN : values[0]);
                return q;
            }
            // A sort is required
            Arrays.sort(values);
            for (int i = 0; i < p.length; i++) {
                // EstimationMethod.HF6 (as per the Apache Commons Math Percentile
                // legacy implementation)
                final double pos = p[i] * (n + 1);
                final double fpos = Math.floor(pos);
                final int j = (int) fpos;
                final double g = pos - fpos;
                if (j < 1) {
                    q[i] = values[0];
                } else if (j >= n) {
                    q[i] = values[n - 1];
                } else {
                    q[i] = (1 - g) * values[j - 1] + g * values[j];
                }
            }
            return q;
        }
    }

    /**
     * Check the quantile {@code p} is in the range {@code [0, 1]}.
     *
     * @param p Quantile.
     * @throws IllegalArgumentException if the quantile is not in the range {@code [0, 1]}
     */
    private static void checkQuantile(double p) {
        if (!(p >= 0 && p <= 1)) {
            throw new IllegalArgumentException("Invalid quantile: " + p);
        }
    }

    /**
     * Create the statistic using an array and given quantiles.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param quantiles Source of the quantiles.
     * @param bh Data sink.
     */
    @Benchmark
    public void doubleQuantiles(DoubleQuantileFunctionSource function, DataSource source,
            QuantileSource quantiles, Blackhole bh) {
        final int size = source.size();
        final double[] p = quantiles.getData();
        final BinaryOperator<double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            bh.consume(fun.apply(source.getData(j), p));
        }
    }

    /**
     * Create the statistic using an array and given quantiles.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param quantiles Source of the quantiles.
     * @param bh Data sink.
     */
    @Benchmark
    public void doubleQuantileRange(DoubleQuantileFunctionSource function, DataSource source,
            QuantileRangeSource quantiles, Blackhole bh) {
        final int size = source.size();
        final double[] p = quantiles.getData();
        final BinaryOperator<double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            bh.consume(fun.apply(source.getData(j), p));
        }
    }

    /**
     * Create the statistic using an array and given quantiles.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param quantiles Source of the quantiles.
     * @param bh Data sink.
     */
    @Benchmark
    public void intQuantiles(IntQuantileFunctionSource function, DataSource source,
            QuantileSource quantiles, Blackhole bh) {
        final int size = source.size();
        final double[] p = quantiles.getData();
        final BiFunction<int[], double[], double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            bh.consume(fun.apply(source.getIntData(j), p));
        }
    }

    /**
     * Create the statistic using an array and given quantiles.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param quantiles Source of the quantiles.
     * @param bh Data sink.
     */
    @Benchmark
    public void intQuantileRange(IntQuantileFunctionSource function, DataSource source,
            QuantileRangeSource quantiles, Blackhole bh) {
        final int size = source.size();
        final double[] p = quantiles.getData();
        final BiFunction<int[], double[], double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            bh.consume(fun.apply(source.getIntData(j), p));
        }
    }
}
