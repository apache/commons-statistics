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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.descriptive.StatisticResult;
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
 * Executes a benchmark of the interpolation of {@code long} values
 * {@code a} and {@code b}, where {@code a <= b} using the interpolant
 * {@code t} is in {@code [0, 1]}.
 * <pre>
 * value = a + t * (b - a)
 * </pre>
 * <p>{@code b - a} has a 64-bit unsigned value and {@code t} is a 53-bit
 * double; the result {@code t * (b - a)} has a maximum of 117-bits and
 * requires extended precision for the exact result. This benchmark compares
 * implementations of extended precision arithmetic to compute the result.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class InterpolationPerformance {
    /** A fast generate of long values. */
    private static final RandomSource RANDOM_SOURCE = RandomSource.XO_RO_SHI_RO_128_PP;

    /**
     * Source of {@code long} data to interpolate as {@code t * (b - a)} with {@code t} in
     * [0, 1].
     */
    @State(Scope.Benchmark)
    public static class DataSource {
        /** Data bit depth. Default to a range that requires extended precision and
         * a range possible using double arithmetic but (b - a) is not a 32-bit integer. */
        @Param({"60", "40"})
        private int bitDepth;
        /** Data size. */
        @Param({"1024"})
        private int size;

        /** Min values. */
        private long[] a;

        /** Max values. */
        private long[] b;

        /** Interpolants. */
        private double[] t;

        /**
         * @return the min values {@code b}
         */
        public long[] getA() {
            return a;
        }

        /**
         * @return the max values {@code b}
         */
        public long[] getB() {
            return b;
        }

        /**
         * @return the interpolant {@code t}
         */
        public double[] getT() {
            return t;
        }

        /**
         * Create the data. Data will be randomized per iteration.
         */
        @Setup(Level.Iteration)
        public void setup() {
            final UniformRandomProvider rng = RANDOM_SOURCE.create();
            final int n = size;
            a = new long[n];
            b = new long[n];
            t = rng.doubles(n).toArray();
            // Generate the distance x between a and b in [0, 2^bitDepth).
            //  min       0         max
            //  |-----a---|-b-------|
            // x = b - a
            if ((bitDepth & 0x3f) != 0) {
                // Use rejection sampling as the random difference in the worst case
                // is half the range and so rejection will be 50% or better.
                final int shift = 64 - bitDepth;
                for (int i = 0; i < n; i++) {
                    final long x = rng.nextLong() >>> shift;
                    long min;
                    long max;
                    do {
                        min = rng.nextLong();
                        // If overflow occurs we reject the sample.
                        max = min + x;
                    } while (min > max);
                    a[i] = min;
                    b[i] = max;
                }
            } else {
                // Full range of 2^64.
                // Rejection sampling will fail if the difference is very large as very
                // few samples can be found in the available space. Here we sample a
                // random point x and sample from the remaining space.
                // The range [a, b] is created by shifting from MIN_VALUE.
                for (int i = 0; i < n; i++) {
                    final long x = rng.nextLong();
                    // Sample from the remaining space
                    long y;
                    if (x <= 0) {
                        // Rejection rate <50%
                        do {
                            y = rng.nextLong();
                        } while (y < x);
                        //  min       0         max
                        //  |-------x.|.........|
                        //          |==========y
                        //  |==========a-------b
                        //
                        //  |-----x...|.........|
                        //        |=y |
                        //  |=a-----b
                        // Use y as the end-point of [a, b]
                        // and move x to the start point a.
                        a[i] = Long.MIN_VALUE - x + y;
                        b[i] = y;
                    } else {
                        // x is strictly positive: sample y in [x, MAX]
                        y = rng.nextLong(Long.MIN_VALUE - x);
                        //  min       0         max
                        //  |---------|---x.....|
                        //                |==y  |
                        //  |==a-------------b
                        // Apply y as a shift to [min, x]
                        a[i] = Long.MIN_VALUE + y;
                        b[i] = x + y;
                    }
                }
            }
        }
    }

    /**
     * The interpolation function.
     */
    public interface InterpolationFunction {
        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <pre>
         * value = a + t * (b - a)
         * </pre>
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        StatisticResult interpolate(long a, long b, double t);
    }

    /**
     * Source of {@code long} data to interpolate as {@code t * (b - a)} with {@code t} in
     * [0, 1].
     *
     * <p>This class generates a sample for interpolation and then evaluates the result
     * using the provided function.
     */
    @State(Scope.Benchmark)
    public static class DynamicDataSource {
        /** Data bit depth. */
        @Param({"64", "32"})
        private int bitDepth;

        /** Source of randomness. */
        private UniformRandomProvider gen;

        /**
         * Create the source of randomness.
         */
        @Setup(Level.Iteration)
        public void setup() {
            gen = RANDOM_SOURCE.create();
        }

        /**
         * Evaluate the function using a random sample of the arguments.
         *
         * @param fun Function.
         * @return the result
         */
        StatisticResult evaluate(InterpolationFunction fun) {
            final UniformRandomProvider rng = this.gen;
            // Generate the distance x between a and b in [0, 2^bitDepth).
            //  min       0         max
            //  |-----a---|-b-------|
            // x = b - a
            if ((bitDepth & 0x3f) != 0) {
                // Use rejection sampling as the random difference in the worst case
                // is half the range and so rejection will be 50% or better.
                final long x = rng.nextLong() >>> (64 - bitDepth);
                long min;
                long max;
                do {
                    min = rng.nextLong();
                    // If overflow occurs we reject the sample.
                    max = min + x;
                } while (min > max);
                return fun.interpolate(min, max, rng.nextDouble());
            }

            // Full range of 2^64.
            // Rejection sampling will fail if the difference is very large as very
            // few samples can be found in the available space. Here we sample a
            // random point x and sample from the remaining space.
            // The range [a, b] is created by shifting from MIN_VALUE.
            final long x = rng.nextLong();
            // Sample from the remaining space
            long min;
            long max;
            if (x <= 0) {
                // Rejection rate <50%
                long y;
                do {
                    y = rng.nextLong();
                } while (y < x);
                //  min       0         max
                //  |-------x.|.........|
                //          |==========y
                //  |==========a-------b
                //
                //  |-----x...|.........|
                //        |=y |
                //  |=a-----b
                // Use y as the end-point of [a, b]
                // and move x to the start point a.
                min = Long.MIN_VALUE - x + y;
                max = y;
            } else {
                // x is strictly positive: sample y in [x, MAX]
                final long y = rng.nextLong(Long.MIN_VALUE - x);
                //  min       0         max
                //  |---------|---x.....|
                //                |==y  |
                //  |==a-------------b
                // Apply y as a shift to [min, x]
                min = Long.MIN_VALUE + y;
                max = x + y;
            }
            return fun.interpolate(min, max, rng.nextDouble());
        }
    }

    /**
     * Source of {@code long} interpolation function.
     */
    @State(Scope.Benchmark)
    public static class InterpolationSource {
        /** 0.5. */
        private static final double HALF = 0.5;
        /** The value 2^53 converted for comparison as an unsigned integer. */
        private static final long UNSIGNED_2_POW_53 = Long.MIN_VALUE + (1L << 53);
        /** The value 2^63. */
        private static final BigInteger TWO_POW_63 = BigInteger.ONE.shiftLeft(63);

        /** Name of the source. */
        @Param({"BigDecimal", "DD", "DD2", "DD3", "DD4", "DD5",
                "Hybrid", "Partial",
                "double", "long", "null"})
        private String name;

        /** Interpolation function. */
        private InterpolationFunction fun;

        /**
         * @return the function
         */
        public InterpolationFunction getFunction() {
            return fun;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            if ("BigDecimal".equals(name)) {
                fun = InterpolationSource::interpolateBigDecimal;
            } else if ("DD".equals(name)) {
                fun = InterpolationSource::interpolateDD;
            } else if ("DD2".equals(name)) {
                fun = InterpolationSource::interpolateDD2;
            } else if ("DD3".equals(name)) {
                fun = InterpolationSource::interpolateDD3;
            } else if ("DD4".equals(name)) {
                fun = InterpolationSource::interpolateDD4;
            } else if ("DD5".equals(name)) {
                fun = InterpolationSource::interpolateDD5;
            } else if ("Hybrid".equals(name)) {
                fun = InterpolationSource::interpolateHybrid;
            } else if ("Partial".equals(name)) {
                fun = InterpolationSource::interpolatePartial;
            } else if ("double".equals(name)) {
                fun = InterpolationSource::interpolatePartialAsDouble;
            } else if ("long".equals(name)) {
                fun = InterpolationSource::interpolatePartial2AsLong;
            } else if ("null".equals(name)) {
                fun = (a, b, t) -> null;
            } else {
                throw new IllegalStateException("Unknown function: " + name);
            }
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses BigDecimal for an exact result.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolateBigDecimal(long a, long b, double t) {
            final BigDecimal aa = BigDecimal.valueOf(a);
            // delta = (b - a)
            final long diff = b - a;
            final BigDecimal delta;
            // a <= b: overflow if negative
            if (diff < 0) {
                // Use the difference as an unsigned 64-bit long
                delta = new BigDecimal(TWO_POW_63.or(BigInteger.valueOf(diff & Long.MAX_VALUE)));
            } else {
                delta = BigDecimal.valueOf(diff);
            }
            final BigDecimal result = aa.add(delta.multiply(new BigDecimal(t)));

            final double d = result.doubleValue();

            // Round to nearest; ties to infinity
            final RoundingMode m = d >= 0 ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
            final long l = result.setScale(0, m).longValue();

            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses double-double (DD) to compute {@code a + t * (b - a)}. Note that the
         * use of a double-double (106-bits) cannot compute the delta without round-off
         * loss of precision: t * (b - a) => 53-bit * (64-bit unsigned) == 117-bit result.
         * The double result would be within 1 ULP but rounding to integer on the ties
         * boundary can be incorrect.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolateDD(long a, long b, double t) {
            final DD aa = DD.of(a);
            // delta = (b - a)
            final DD delta = DD.ofUnsigned(b - a);

            // Note: This cannot add the longValue() result of t * (b - a) to a
            // as the unsigned result can be larger than 63-bits.
            final DD result = delta.multiply(t).add(aa);

            // Round to nearest whole number, ties towards positive infinity.
            // result = v + f (with v an integer).
            // longValue always truncates towards zero.
            long v = result.longValue();
            // Fraction part f in +/- [0, 1).
            // The sign of the fraction part matches the sign of the result.
            final DD f = result.subtract(DD.of(v));

            // Note: This rounding may not produce the correct result if
            // bits have been lost in the DD computation. The maximum
            // depth of t * (b - a) is 117-bits. Added to a 63-bit signed long
            // this can lose round-off bits required for ties rounding.
            if (f.hi() > 0.5 || f.hi() == 0.5 && f.lo() >= 0) {
                // v is positive; ties @ 0.5 to positive infinity using >=
                v++;
            } else if (f.hi() < -0.5 || f.hi() == -0.5 && f.lo() < 0) {
                // v is negative; ties @ -0.5 to positive infinity using <
                v--;
            }

            // Double result may be incorrect by 1 ULP
            final double d = result.hi();
            final long l = v;

            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses double-double (DD) to compute {@code t * (b - a)}. This is then
         * decomposed into an integer and fractional part using exact operations.
         * The integer can be added to {@code a} and the fraction used for rounding.
         *
         * <p>This method may still have incorrect rounding if the product
         * {@code t * (b - a)} exceeds the 106-bit precision of the DD. The method
         * is most robust than {@link #interpolateDD(long, long, double)} as it has
         * only one computation where bits can be lost.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolateDD2(long a, long b, double t) {
            // delta = (b - a)
            final DD delta = DD.ofUnsigned(b - a);

            final DD addend = delta.multiply(t);

            // result = a + addend (addend is positive)
            // Create result = v + f (with v an integer; and f a positive remainder).
            // Check the magnitude vs 2^53.
            // If it is large then it will be even. So we can divide
            // by 2 and add it twice.
            long v = a;
            DD f;
            if (addend.hi() >= 0x1.0p53) {
                long i = (long) (addend.hi() * 0.5);
                v = v + i + i;
                // low part can be negative so use the floor to generate a positive remainder
                i = (long) Math.floor(addend.lo());
                v = v + i;
                f = DD.ofDifference(addend.lo(), i);
            } else {
                // The long value is a representable double.
                // longValue() truncates towards zero so f will be positive
                final long i = addend.longValue();
                v = v + i;
                f = addend.subtract(i);
            }

            // Fraction |high part| < 1 => |low part| < 2^-53.
            // This cannot be added to any integer value and can be omitted.
            final double d = DD.of(v).add(f.hi()).hi();

            // Round to nearest whole number, ties towards positive infinity.
            // Note: This rounding may not produce the correct result if
            // bits have been lost in the DD computation. The maximum
            // depth of t * (b - a) is 117-bits.
            if (f.hi() > 0.5 || f.hi() == 0.5 && f.lo() >= 0) {
                // ties @ 0.5 to positive infinity using >=
                v++;
            }

            final long l = v;

            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses double-double (DD) when the exact result is representable using
         * 106-bits, otherwise BigDecimal.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolateHybrid(long a, long b, double t) {
            final long diff = b - a;

            // The product t * (b - a) is exact as a double-double
            // if the factor (b - a) is a representable double.
            // Compare to 2^53 as unsigned to also detect overflow.
            if (diff + Long.MIN_VALUE < UNSIGNED_2_POW_53) {
                final DD addend = DD.ofProduct(diff, t);
                // result = a + addend
                // The positive addend has the integer component in the high part
                // if t in [0, 1]. Decompose to exact integer and fractional parts.
                // Note that if addend.lo() is negative then i is already
                // rounded towards positive infinity. Later rounding will not
                // double round up, e.g. addend=(2, -2^-53) -> i=2; f=(-2^53, 0).
                final long i = (long) addend.hi();
                final DD f = DD.ofSum(addend.hi() - i, addend.lo());

                // Integer part of the result
                final long v = a + i;

                // Round to nearest whole number, ties towards positive infinity.
                final long l = v + ((f.hi() > HALF || f.hi() == HALF && f.lo() >= 0) ? 1 : 0);

                // Fraction |high part| < 1 => |low part| < 2^-53.
                // Ignoring f.lo() creates the double result within 1 ULP.
                final double d = DD.of(v).add(f.hi()).hi();

                return new StatisticResult() {
                    @Override
                    public double getAsDouble() {
                        return d;
                    }

                    @Override
                    public long getAsLong() {
                        return l;
                    }
                };
            }

            // Revert to BigDecimal
            final BigDecimal aa = BigDecimal.valueOf(a);
            final BigDecimal delta;
            // a <= b: overflow if negative
            if (diff < 0) {
                // Use the difference as an unsigned 64-bit long
                delta = new BigDecimal(TWO_POW_63.or(BigInteger.valueOf(diff & Long.MAX_VALUE)));
            } else {
                delta = BigDecimal.valueOf(diff);
            }
            final BigDecimal result = aa.add(delta.multiply(new BigDecimal(t)));

            // --------
            // Implementation note:
            //
            // Since the difference (b - a) is big it is tempting to save a large
            // computation of the double result by using the long result when the
            // result is probably above |2^53|.
            // But this can be incorrect by 1 ULP as conversion to double
            // uses ties to even rounding. E.g.
            // var a = BigDecimal.valueOf(2 + (1L << 53)).add(new BigDecimal(0.5))
            // ==> 9007199254740994.5
            // a.setScale(0, RoundingMode.HALF_UP) == 9007199254740995
            // a.doubleValue()                     == 9.007199254740994E15
            // (double) 9007199254740995L          == 9.007199254740996E15  <- 1 ULP error
            // --------

            final double d = result.doubleValue();

            // Round to nearest; ties to infinity
            final RoundingMode m = d >= 0 ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
            final long l = result.setScale(0, m).longValue();

            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses double-double (DD) when the exact result is representable using
         * 106-bits, otherwise BigDecimal.
         *
         * <p>Differs from {@link #interpolateHybrid(long, long, double)} in the
         * BigDecimal computation by returning a result with lazy evaluation of the long
         * or double result.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolatePartial(long a, long b, double t) {
            final long diff = b - a;

            // The product t * (b - a) is exact as a double-double
            // if the factor (b - a) is a representable double.
            // Compare to 2^53 as unsigned to also detect overflow.
            if (diff + Long.MIN_VALUE < UNSIGNED_2_POW_53) {
                final DD addend = DD.ofProduct(diff, t);
                // result = a + addend
                // The positive addend has the integer component in the high part
                // if t in [0, 1]. Decompose to exact integer and fractional parts.
                // Note that if addend.lo() is negative then i is already
                // rounded towards positive infinity. Later rounding will not
                // double round up, e.g. addend=(2, -2^-53) -> i=2; f=(-2^53, 0).
                final long i = (long) addend.hi();
                final DD f = DD.ofSum(addend.hi() - i, addend.lo());

                // Integer part of the result
                final long v = a + i;

                // Round to nearest whole number, ties towards positive infinity.
                final long l = v + ((f.hi() > HALF || f.hi() == HALF && f.lo() >= 0) ? 1 : 0);

                // Fraction |high part| < 1 => |low part| < 2^-53.
                // Ignoring f.lo() creates the double result within 1 ULP.
                final double d = DD.of(v).add(f.hi()).hi();

                return new StatisticResult() {
                    @Override
                    public double getAsDouble() {
                        return d;
                    }

                    @Override
                    public long getAsLong() {
                        return l;
                    }
                };
            }

            // Revert to BigDecimal
            final BigDecimal aa = BigDecimal.valueOf(a);
            final BigDecimal delta;
            // a <= b: overflow if negative
            if (diff < 0) {
                // Use the difference as an unsigned 64-bit long
                delta = new BigDecimal(TWO_POW_63.or(BigInteger.valueOf(diff & Long.MAX_VALUE)));
            } else {
                delta = BigDecimal.valueOf(diff);
            }
            final BigDecimal result = aa.add(delta.multiply(new BigDecimal(t)));

            // Lazy evaluation. The result may have a large memory footprint.
            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return result.doubleValue();
                }

                @Override
                public long getAsLong() {
                    // Round to nearest; ties to infinity
                    final RoundingMode m = result.signum() >= 0 ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
                    return result.setScale(0, m).longValue();
                }
            };
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses the evaluated {@code double} result from
         * {@link #interpolatePartial(long, long, double)}.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolatePartialAsDouble(long a, long b, double t) {
            final double r = interpolatePartial(a, b, t).getAsDouble();
            return () -> r;
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses the evaluated {@code long} result from
         * {@link #interpolatePartial(long, long, double)}.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolatePartial2AsLong(long a, long b, double t) {
            final long r = interpolatePartial(a, b, t).getAsLong();
            return () -> r;
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses double-double (DD) to compute {@code t * (b - a)}. When (b - a)
         * is greater than 63-bits this is performed as two multiplications of
         * the value split into two. Each multiplication is exact. The two results
         * are then decomposed into integer and fractional parts. The fraction
         * parts are then added with possible loss of bits and rounding the
         * integer result may not be correct.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolateDD3(long a, long b, double t) {
            final long diff = b - a;

            // The product t * (b - a) is exact as a double-double
            // if the factor (b - a) is a representable double.
            // Compare to 2^53 as unsigned to also detect overflow.
            if (diff + Long.MIN_VALUE < UNSIGNED_2_POW_53) {
                final DD addend = DD.ofProduct(diff, t);
                // result = a + addend
                // The positive addend has the integer component in the high part
                // if t in [0, 1]. Decompose to exact integer and fractional parts.
                // Note that if addend.lo() is negative then i is already
                // rounded towards positive infinity. Later rounding will not
                // double round up, e.g. addend=(2, -2^-53) -> i=2; f=(-2^53, 0).
                final long i = (long) addend.hi();
                final DD f = DD.ofSum(addend.hi() - i, addend.lo());

                // Integer part of the result
                final long v = a + i;

                // Round to nearest whole number, ties towards positive infinity.
                final long l = v + ((f.hi() > HALF || f.hi() == HALF && f.lo() >= 0) ? 1 : 0);

                // Fraction |high part| < 1 => |low part| < 2^-53.
                // Ignoring f.lo() creates the double result within 1 ULP.
                final double d = DD.of(v).add(f.hi()).hi();

                return new StatisticResult() {
                    @Override
                    public double getAsDouble() {
                        return d;
                    }

                    @Override
                    public long getAsLong() {
                        return l;
                    }
                };
            }

            // Create two exact products using the difference (b - a) split into
            // representable doubles.
            // Subtract the integer parts to leave two fractional parts. These are
            // combined to allow rounding.

            // Two exact addends
            final DD delta = DD.ofUnsigned(diff);
            DD addend1 = DD.ofProduct(delta.hi(), t);
            DD addend2 = DD.ofProduct(delta.lo(), t);

            // Subtract integer parts and add them to min value a
            long v = a;
            long i;
            long j;
            // addend1 can be > 2^63
            if (addend1.hi() >= 0x1.0p63) {
                // The value is even so add half
                i = (long) (addend1.hi() * 0.5);
                j = (long) addend1.lo();
                v = v + i + i + j;
                addend1 = DD.ofSum((addend1.hi() * 0.5 - i) * 2, addend1.lo() - j);
            } else {
                i = (long) addend1.hi();
                j = (long) addend1.lo();
                v = v + i + j;
                addend1 = DD.ofSum(addend1.hi() - i, addend1.lo() - j);
            }
            // The low part of addend2 cannot have an integer component
            // since this was a 10-bit integer * [0, 1].
            i = (long) addend2.hi();
            v = v + i;
            addend2 = DD.ofSum(addend2.hi() - i, addend2.lo());

            // Collect addends.
            // The result addend has a magnitude in [0, 2) and
            // is 117-bits or less if integer parts were subtracted.
            // High part of addend2 overlaps with low part of addend1:
            // addend1 |----|----|
            // addend2      |----|----|
            // addend1 may be shifted right if integer component was
            // subtracted increasing the overlap.
            // DD addition should collect the low parts of addend2
            // but can potentially lose bits.
            // These should not effect rounding to integer.
            // Given the 64-bit unsigned multiplier the
            // largest separation of bits by zeros in the multiplier
            // (and thus the result) is 62 zeros. So a 0.5 cannot have
            // 105 zeros before the next 1 bit in the result. Rounding
            // of 0.5 only requires an accurate 64-bit result so the
            // precision of DD.add is sufficient.
            final DD addend = addend1.add(addend2);

            // double result is within 1 ULP
            final double d = DD.of(v).add(addend).hi();

            // Sum of fractions may generate an integer part
            double f = addend.hi();
            i = (long) f;
            v += i;
            f -= i;

            // Round v to nearest whole number using fraction f.
            // This comparison must use the parts in order. Each part is
            // smaller than the previous part.
            // Ties to infinity using f >= 0.5 and f < -0.5.
            if (f > HALF ||
                f == HALF && addend.lo() >= 0) {
                v++;
            } else if (f < -HALF) {
                // ---
                // Note: It is not possible to hit f == -0.5 && addend.lo() < 0
                // ---
                // delta (64-bits) * t (53-bits)
                // The exact result is 117-bits.
                // [1] If t has 1 significant bit (power of 2) the largest gap between
                // non-zero bits is 62 if delta = 1000...0001
                // [2] If t has multiple significant bits the largest gap between bits
                // is 51 if t = 1.000...0001 * 2^b and delta is a power of 2.
                // To generate f == -0.5 requires at least 52 zero bits after the first
                // significant bit.
                // If t is a power of 2 addend1 must be entirely positive and
                // the -0.5 is from addend2 with zero trailing bits.
                // If addend1 low bits are negative this can only result from the factor t
                // having more than 1 bit. Given [2] a -0.5 is entirely from addend1
                // and all trailing bits are zero (addend2 is zero).
                v--;
            }

            final long l = v;

            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Uses double-double (DD) to compute {@code t * (b - a)}. When (b - a)
         * is greater than 63-bits this is performed as two multiplications of
         * the value split into two. Each multiplication is exact. The two results
         * are then decomposed into integer and fractional parts. The fraction
         * part are added with special a double-double routine with no loss of bits
         * and rounding to an integer is exact.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolateDD4(long a, long b, double t) {
            final long diff = b - a;

            // The product t * (b - a) is exact as a double-double
            // if the factor (b - a) is a representable double.
            // Compare to 2^53 as unsigned to also detect overflow.
            if (diff + Long.MIN_VALUE < UNSIGNED_2_POW_53) {
                final DD addend = DD.ofProduct(diff, t);
                // result = a + addend
                // The positive addend has the integer component in the high part
                // if t in [0, 1]. Decompose to exact integer and fractional parts.
                // Note that if addend.lo() is negative then i is already
                // rounded towards positive infinity. Later rounding will not
                // double round up, e.g. addend=(2, -2^-53) -> i=2; f=(-2^53, 0).
                final long i = (long) addend.hi();
                final DD f = DD.ofSum(addend.hi() - i, addend.lo());

                // Integer part of the result
                final long v = a + i;

                // Round to nearest whole number, ties towards positive infinity.
                final long l = v + ((f.hi() > HALF || f.hi() == HALF && f.lo() >= 0) ? 1 : 0);

                // Addition includes f.lo() required for correct rounding when
                // |v| is close to 1 and t is small so addend has no integer part.
                final double d = DD.of(v).add(f).hi();

                return new StatisticResult() {
                    @Override
                    public double getAsDouble() {
                        return d;
                    }

                    @Override
                    public long getAsLong() {
                        return l;
                    }
                };
            }

            // Create two exact products using the difference (b - a) split into
            // representable doubles.
            // Subtract the integer parts to leave two fractional parts. These are
            // combined without loss of bits to allow rounding.

            // Two exact addends
            final DD delta = DD.ofUnsigned(diff);
            DD addend1 = DD.ofProduct(delta.hi(), t);
            DD addend2 = DD.ofProduct(delta.lo(), t);

            // Subtract integer parts and add them to min value a
            long v = a;
            long i;
            long j;
            // addend1 can be > 2^63
            if (addend1.hi() >= 0x1.0p63) {
                // The value is even so add half
                i = (long) (addend1.hi() * 0.5);
                j = (long) addend1.lo();
                v = v + i + i + j;
                addend1 = DD.ofSum((addend1.hi() * 0.5 - i) * 2, addend1.lo() - j);
            } else {
                i = (long) addend1.hi();
                j = (long) addend1.lo();
                v = v + i + j;
                addend1 = DD.ofSum(addend1.hi() - i, addend1.lo() - j);
            }
            // The low part cannot have an integer component
            // since this was a 10-bit integer * [0, 1].
            i = (long) addend2.hi();
            v = v + i;
            addend2 = DD.ofSum(addend2.hi() - i, addend2.lo());

            // Collect addends.
            // The result addend has a magnitude in [0, 2) and
            // is 117-bits or less if integer parts were subtracted.
            // High part of addend2 overlaps with low part of addend1:
            // addend1 |----|----|
            // addend2      |----|----|
            // addend1 may be shifted right if integer component was
            // subtracted increasing the overlap.

            // Exact sum of the addends to create triple-double (f0, f1, f2).
            // This part has a magnitude in [0, 2).
            final double[] f12 = new double[2];
            double f = add3(addend1.hi(), addend1.lo(), addend2.hi(), addend2.lo(), f12);

            // Try and carry bits from f2.
            // Add to the low part of v which can carry bits.
            // Then add the high part of v.
            final DD vv = DD.of(v);
            final double d = DD.ofSum(vv.lo(), f)
                             .add(DD.ofSum(f12[0], f12[1]))
                             .add(vv.hi())
                             .hi();

            // Sum of fractions may generate an integer part
            i = (long) f;
            v += i;
            f -= i;

            // Round v to nearest whole number using fraction (f0, f1, f2).
            // This comparison must use the parts in order. Each part is
            // smaller than the previous part.
            // Ties to infinity using f >= 0.5 and f < -0.5.
            if (f > HALF ||
                f == HALF && f12[0] > 0 ||
                f == HALF && f12[0] == 0 && f12[1] >= 0) {
                v++;
            } else if (f < -HALF ||
                f == -HALF && f12[0] < 0 ||
                f == -HALF && f12[0] == 0 && f12[1] < 0) {
                v--;
            }

            final long l = v;

            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        /**
         * Linear interpolation between <strong>sorted</strong> values {@code a <= b}
         * using the interpolant {@code t}.
         *
         * <p>Differences (b - a) above 2^53 are the same as
         * {@link #interpolateDD3(long, long, double)}. Below 2^53 uses double arithmetic.
         * This can have integer rounding errors in extreme cases, and frequent ULP errors
         * for the double value.
         *
         * @param a Min value.
         * @param b Max value.
         * @param t Interpolant in (0, 1).
         * @return the value
         */
        static StatisticResult interpolateDD5(long a, long b, double t) {
            final long diff = b - a;

            // The product t * (b - a) is exact as a double-double
            // if the factor (b - a) is a representable double.
            // Compare to 2^53 as unsigned to also detect overflow.
            if (diff + Long.MIN_VALUE < UNSIGNED_2_POW_53) {
                final double addend = diff * t;
                final long i = (long) addend;
                final double f = addend - i;

                // Integer part of the result
                final long v = a + i;

                // Round to nearest whole number, ties towards positive infinity.
                final long l = v + (f >= HALF ? 1 : 0);

                final double d = DD.of(v).add(f).hi();

                return new StatisticResult() {
                    @Override
                    public double getAsDouble() {
                        return d;
                    }

                    @Override
                    public long getAsLong() {
                        return l;
                    }
                };
            }

            // Create two exact products using the difference (b - a) split into
            // representable doubles.
            // Subtract the integer parts to leave two fractional parts. These are
            // combined to allow rounding.

            // Two exact addends
            final DD delta = DD.ofUnsigned(diff);
            DD addend1 = DD.ofProduct(delta.hi(), t);
            DD addend2 = DD.ofProduct(delta.lo(), t);

            // Subtract integer parts and add them to min value a
            long v = a;
            long i;
            long j;
            // addend1 can be > 2^63
            if (addend1.hi() >= 0x1.0p63) {
                // The value is even so add half
                i = (long) (addend1.hi() * 0.5);
                j = (long) addend1.lo();
                v = v + i + i + j;
                addend1 = DD.ofSum((addend1.hi() * 0.5 - i) * 2, addend1.lo() - j);
            } else {
                i = (long) addend1.hi();
                j = (long) addend1.lo();
                v = v + i + j;
                addend1 = DD.ofSum(addend1.hi() - i, addend1.lo() - j);
            }
            // The low part of addend2 cannot have an integer component
            // since this was a 10-bit integer * [0, 1].
            i = (long) addend2.hi();
            v = v + i;
            addend2 = DD.ofSum(addend2.hi() - i, addend2.lo());

            // Collect addends.
            // The result addend has a magnitude in [0, 2) and
            // is 117-bits or less if integer parts were subtracted.
            // High part of addend2 overlaps with low part of addend1:
            // addend1 |----|----|
            // addend2      |----|----|
            // addend1 may be shifted right if integer component was
            // subtracted increasing the overlap.
            // DD addition should collect the low parts of addend2
            // but can potentially lose bits.
            // These should not effect rounding to integer.
            // Given the 64-bit unsigned multiplier the
            // largest separation of bits by zeros in the multiplier
            // (and thus the result) is 62 zeros. So a 0.5 cannot have
            // 105 zeros before the next 1 bit in the result. Rounding
            // of 0.5 only requires an accurate 64-bit result so the
            // precision of DD.add is sufficient.
            final DD addend = addend1.add(addend2);

            // double result is within 1 ULP
            final double d = DD.of(v).add(addend).hi();

            // Sum of fractions may generate an integer part
            double f = addend.hi();
            i = (long) f;
            v += i;
            f -= i;

            // Round v to nearest whole number using fraction f.
            // This comparison must use the parts in order. Each part is
            // smaller than the previous part.
            // Ties to infinity using f >= 0.5 and f < -0.5.
            if (f > HALF ||
                f == HALF && addend.lo() >= 0) {
                v++;
            } else if (f < -HALF) {
                // ---
                // Note: It is not possible to hit f == -0.5 && addend.lo() < 0
                // ---
                // delta (64-bits) * t (53-bits)
                // The exact result is 117-bits.
                // [1] If t has 1 significant bit (power of 2) the largest gap between
                // non-zero bits is 62 if delta = 1000...0001
                // [2] If t has multiple significant bits the largest gap between bits
                // is 51 if t = 1.000...0001 * 2^b and delta is a power of 2.
                // To generate f == -0.5 requires at least 52 zero bits after the first
                // significant bit.
                // If t is a power of 2 addend1 must be entirely positive and
                // the -0.5 is from addend2 with zero trailing bits.
                // If addend1 low bits are negative this can only result from the factor t
                // having more than 1 bit. Given [2] a -0.5 is entirely from addend1
                // and all trailing bits are zero (addend2 is zero).
                v--;
            }

            final long l = v;

            return new StatisticResult() {
                @Override
                public double getAsDouble() {
                    return d;
                }

                @Override
                public long getAsLong() {
                    return l;
                }
            };
        }

        // DD routines taken from o.a.c.numbers.core.DD and DDMath

        /**
         * Compute the sum of {@code (x, xx)} and {@code (y, yy)} to triple-double precision.
         *
         * @param x High part of x.
         * @param xx Low part of x.
         * @param y High part of y.
         * @param yy Low part of y.
         * @param s12 Output parts (s1, s2)
         * @return s0
         */
        private static double add3(double x, double xx, double y, double yy, double[] s12) {
            // Expansion sum (Schewchuk Fig 7): (x, xx) + (x, yy) -> (s0, s1, s2, s3)

            // --- adapted from DD.accurateAdd ---
            DD s = DD.ofSum(xx, yy);
            double s3 = s.lo();
            s = DD.ofSum(x, s.hi());
            // (s0, s1, s2) == (s.hi(), s.lo(), s3)
            double s0 = s.hi();
            s = DD.ofSum(s.lo(), y);
            double s2 = s.lo();
            s = DD.ofSum(s0, s.hi());
            // (s0, s1, s2, s3) == (s.hi(), s.lo(), s2, s3)

            // Compress (Schewchuk Fig. 15) (s0, s1, s2, s3) -> (g0, g1, g2, g3)
            // --- adapted from DDMath.norm3 ---
            double q = s.lo();
            final double g0 = s.hi();
            final double g1 = q + s2;
            q = fastTwoSumLow(q, s2, g1);
            final double g2 = q + s3;
            final double g3 = fastTwoSumLow(q, s3, g2);
            // (g0, g1, g2, g3) -> (h0, h1, h2, h3), returned as (h0, h1, h2 + h3)
            q = g1 + g2;
            s12[1] = fastTwoSumLow(g1, g2, q) + g3;
            final double h0 = g0 + q;
            s12[0] = fastTwoSumLow(g0, q, h0);
            return h0;
        }

        /**
         * Compute the round-off of the sum of two numbers {@code a} and {@code b} using
         * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
         * {@code |a| >= |b|}.
         *
         * <p>If {@code a} is zero and {@code b} is non-zero the returned value is zero.
         *
         * @param a First part of sum.
         * @param b Second part of sum.
         * @param x Sum.
         * @return the sum round-off
         */
        private static double fastTwoSumLow(double a, double b, double x) {
            // (x, xx) = a + b
            // bVirtual = x - a
            // xx = b - bVirtual
            return b - (x - a);
        }
    }

    /**
     * Create the interpolation using the function. Uses pre-computed arguments.
     *
     * @param source Source of the data.
     * @param function Source of the function.
     * @return the interpolation result
     */
    @Benchmark
    public StatisticResult[] interpolate(DataSource source, InterpolationSource function) {
        final long[] a = source.getA();
        final long[] b = source.getB();
        final double[] t = source.getT();
        final int n = a.length;
        final InterpolationFunction f = function.getFunction();
        final StatisticResult[] r = new StatisticResult[n];
        for (int i = 0; i < n; i++) {
            r[i] = f.interpolate(a[i], b[i], t[i]);
        }
        return r;
    }

    /**
     * Create the interpolation using the function. Dynamically computes arguments.
     * This benchmark should avoid branch prediction that can be done of fixed input data.
     * The cost of data generation is small compared to the evaluation.
     *
     * @param source Source of the data.
     * @param function Source of the function.
     * @return the interpolation result
     */
    @Benchmark
    public StatisticResult evaluate(DynamicDataSource source, InterpolationSource function) {
        return source.evaluate(function.getFunction());
    }
}
