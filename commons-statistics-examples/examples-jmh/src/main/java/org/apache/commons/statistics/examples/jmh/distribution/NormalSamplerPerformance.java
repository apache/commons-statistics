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

package org.apache.commons.statistics.examples.jmh.distribution;

import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InverseTransformContinuousSampler;
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
 * Executes a benchmark of the sampling from a normal distribution.
 *
 * <p>This benchmark is used to determine what sampler to use in a truncated
 * normal distribution:
 * <ul>
 *  <li>Rejection sampling from a normal distribution (ignore samples outside the truncated range)</li>
 *  <li>Inverse transform sampling</li>
 * </ul>
 *
 * <p>Rejection sampling can be used when the truncated distribution covers a
 * reasonable proportion of the standard normal distribution and so the
 * rejection rate is low. The speed of each method will be approximately equal
 * when:
 *
 * <pre>
 * t1 * n = t2
 * </pre>
 *
 * <p>Where {@code t1} is the speed of the normal distribution sampler, {@code n} is the
 * number of samples required to generate a value within the truncated range and {@code t2} is
 * the speed of the inverse transform sampler. The crossover point occurs at approximately:
 *
 * <pre>
 * t1 / t2 = 1 / n
 * </pre>
 *
 * <p>Where {@code 1 / n} is the fraction of the CDF covered by the truncated normal distribution.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class NormalSamplerPerformance {
    /** The value. Must NOT be final to prevent JVM optimisation! */
    private double value;

    /**
     * Source of a function to compute a sample from a normal distribution.
     */
    @State(Scope.Benchmark)
    public static class Source {
        /** The method. */
        @Param({"normal", "inverse_transform"})
        private String method;
        /**
         * RNG providers.
         *
         * <p>Use different speeds.</p>
         *
         * @see <a href="https://commons.apache.org/proper/commons-rng/userguide/rng.html">
         *      Commons RNG user guide</a>
         */
        @Param({"XO_RO_SHI_RO_128_PP",
                "MWC_256",
                "JDK"})
        private String randomSourceName;

        /** The generator to supply the next sample value. */
        private DoubleSupplier gen;

        /**
         * @return the next value
         */
        public double next() {
            return gen.getAsDouble();
        }

        /**
         * Create the sampler for the normal distribution.
         */
        @Setup
        public void setup() {
            final UniformRandomProvider rng = RandomSource.valueOf(randomSourceName).create();
            if ("normal".equals(method)) {
                gen = ZigguratSampler.NormalizedGaussian.of(rng)::sample;
            } else if ("inverse_transform".equals(method)) {
                final NormalDistribution dist = NormalDistribution.of(0, 1);
                gen = InverseTransformContinuousSampler.of(rng, dist::inverseCumulativeProbability)::sample;
            } else {
                throw new IllegalStateException("Unknown method: " + method);
            }
        }
    }

    /**
     * Baseline for a JMH method call returning a {@code double}.
     *
     * @return the value
     */
    @Benchmark
    public double baseline() {
        return value;
    }

    /**
     * Compute a sample.
     *
     * @param source Source of the sample.
     * @return the value
     */
    @Benchmark
    public double sample(Source source) {
        return source.next();
    }
}
