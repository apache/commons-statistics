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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Represents the result of a statistic computed over a set of values.
 *
 * <p>Base interface implemented by all statistics.
 *
 * @since 1.1
 */
@FunctionalInterface
public interface StatisticResult extends DoubleSupplier, IntSupplier, LongSupplier {
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation uses the closest representable {@code int} value of
     * the {@link #getAsDouble()} result. In the event of ties the result is towards
     * positive infinity. This will raise an {@link ArithmeticException} if the closest
     * integer result is not within the range {@code [-2^31, 2^31)}.
     *
     * @throws ArithmeticException if the {@code result} overflows an int, or is not
     * finite
     */
    @Override
    default int getAsInt() {
        // Note: Do not use (int) getAsLong() to avoid a narrowing primitive conversion.
        final double x = getAsDouble();
        final double r = Statistics.roundToInteger(x);
        if (r >= -0x1.0p31 && r < 0x1.0p31) {
            return (int) r;
        }
        throw new ArithmeticException("integer overflow: " + x);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation uses the closest representable {@code long} value of
     * the {@link #getAsDouble()} result. In the event of ties the result is rounded
     * positive infinity. This will raise an {@link ArithmeticException} if the closest
     * long integer result is not within the range {@code [-2^63, 2^63)}.
     *
     * @throws ArithmeticException if the {@code result} overflows a long, or is not
     * finite
     */
    @Override
    default long getAsLong() {
        final double x = getAsDouble();
        final double r = Statistics.roundToInteger(x);
        if (r >= -0x1.0p63 && r < 0x1.0p63) {
            return (long) r;
        }
        throw new ArithmeticException("long integer overflow: " + x);
    }

    /**
     * Gets a result as a {@link BigInteger}.
     *
     * <p>The default implementation uses the closest representable {@code BigInteger}
     * value of the {@link #getAsDouble()} result. In the event of ties the result is
     * rounded positive infinity. This will raise an {@link ArithmeticException} if the
     * result is not finite.
     *
     * @return a result
     * @throws ArithmeticException if the {@code result} is not finite
     */
    default BigInteger getAsBigInteger() {
        final double x = getAsDouble();
        if (!Double.isFinite(x)) {
            throw new ArithmeticException("BigInteger overflow: " + x);
        }
        final double r = Statistics.roundToInteger(x);
        if (r >= -0x1.0p63 && r < 0x1.0p63) {
            // Representable as a long
            return BigInteger.valueOf((long) r);
        }
        // Large result
        return new BigDecimal(r).toBigInteger();
    }
}
