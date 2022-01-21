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

package org.apache.commons.statistics.examples.jmh.distribution;

import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import org.apache.commons.rng.sampling.distribution.ContinuousUniformSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.NormalDistribution;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Executes a benchmark of the probability density function for
 * the normal distribution.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class NormalPDFPerformance {

    /**
     * Source of a function to compute the normal distribution PDF.
     * The source of the random X deviate is implemented in sub-classes.
     */
    @State(Scope.Benchmark)
    public abstract static class Source {
        /** The method. */
        @Param({"baseline", "std", "hp"})
        private String method;

        /** The generator to supply the next density value. */
        private DoubleSupplier gen;

        /**
         * @return the next value
         */
        public double next() {
            return gen.getAsDouble();
        }

        /**
         * Create the normal distribution PDF and a supplier of random X deviates.
         * This is used to create a supplier of density values.
         */
        @Setup
        public void setup() {
            // Note:
            // These exist to exercise the full the density function.
            // If these are different from a standard normal distribution N(0, 1)
            // then the supplier of values may not provide a suitable distribution
            // of random X deviates.
            final double mean = 0;
            final double sd = 1;

            DoubleUnaryOperator fun;
            if ("baseline".equals(method)) {
                // No density function. This tests baseline speed of generating the X deviate.
                fun = x -> x;
            } else if ("std".equals(method)) {
                // Standard precision implementation
                fun = new StandardNormalDistribution(mean, sd)::density;
            } else if ("hp".equals(method)) {
                // High-precision implementation in the NormalDistribution class
                fun = NormalDistribution.of(mean, sd)::density;
            } else {
                throw new IllegalStateException("Unknown method: " + method);
            }
            final DoubleSupplier nextValue = createValues();
            gen = () -> fun.applyAsDouble(nextValue.getAsDouble());
        }

        /**
         * Creates the generator of random X deviates used to test the normal distribution.
         *
         * @return the generator
         */
        protected abstract DoubleSupplier createValues();

        /**
         * Implementation of the normal distribution using standard precision.
         */
        private static class StandardNormalDistribution {
            /** The mean. */
            private final double mean;
            /** The standard deviation. */
            private final double sd;
            /** Density factor sd * sqrt(2 pi). */
            private final double sdSqrt2pi;

            /**
             * @param mean Mean
             * @param sd Standard deviation
             */
            StandardNormalDistribution(double mean, double sd) {
                this.mean = mean;
                this.sd = sd;
                sdSqrt2pi = sd * Math.sqrt(2 * Math.PI);
            }

            /**
             * Compute the probability density function.
             *
             * @param x Point x
             * @return pdf(x)
             */
            double density(double x) {
                final double z = (x - mean) / sd;
                return Math.exp(-0.5 * z * z) / sdSqrt2pi;
            }
        }
    }

    /**
     * Provides a source of random X deviates from a uniform distribution.
     */
    @State(Scope.Benchmark)
    public static class UniformSource extends Source {
        /** The lower bound. */
        @Param({"0"})
        private double low;
        /**
         * The higher bound.
         *
         * <p>Note: A standard normal distribution density function is zero above approximately 38.5.
         *
         * <p>Note: The high precision normal distribution uses a high precision exponential
         * function when x^2 is less than 2.0. The worse case scenario for a 50-50 random branch
         * for the distribution to choose a standard or high precision can be created using a
         * range of [0, 2 * sqrt(2)], or [0, 2.828].
         */
        @Param({"1", "2.828", "4", "16", "64"})
        private double high;

        /** {@inheritDoc} */
        @Override
        protected DoubleSupplier createValues() {
            return ContinuousUniformSampler.of(RandomSource.XO_RO_SHI_RO_128_PP.create(), low, high)::sample;
        }
    }

    /**
     * Provides a source of random X deviates from a normal distribution.
     */
    @State(Scope.Benchmark)
    public static class NormalSource extends Source {
        /** {@inheritDoc} */
        @Override
        protected DoubleSupplier createValues() {
            return ZigguratSampler.NormalizedGaussian.of(RandomSource.XO_RO_SHI_RO_128_PP.create())::sample;
        }
    }

    /**
     * Compute the PDF from a uniformally distributed X deviate.
     *
     * @param source source of the function
     * @return the value
     */
    @Benchmark
    public double uniform(UniformSource source) {
        return source.next();
    }

    /**
     * Compute the PDF from a normally distributed X deviate.
     *
     * @param source source of the function
     * @return the value
     */
    @Benchmark
    public double normal(NormalSource source) {
        return source.next();
    }
}
