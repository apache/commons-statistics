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
package org.apache.commons.statistics.bigdecimal.descriptive;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Implementation for BigDecimalSummaryStatistics based on {@link java.util.DoubleSummaryStatistics}.
 */
public class BigDecimalSummaryStatistics implements Consumer<BigDecimal> {

    /**
     * internal counter.
     */
    private long count;
    /**
     * The total.
     */
    private BigDecimal sum;
    /**
     * The minimum.
     */
    private BigDecimal min;
    /**
     * The maximum.
     */
    private BigDecimal max;

    /**
     * Create an instance of BigDecimalSummaryStatistics. {@code count = 0} and sum = {@link
     * BigDecimal#ZERO}
     */
    public BigDecimalSummaryStatistics() {
        this.count = 0;
        this.sum = BigDecimal.ZERO;
        this.max = null;
        this.min = null;
    }

    /**
     * Constructs a non-empty instance with the specified {@code count}, {@code min}, {@code max},
     * and {@code sum}.
     *
     * <p>If {@code count} is zero then the remaining arguments are ignored and
     * an empty instance is constructed.
     *
     * <p>If the arguments are inconsistent then an {@code IllegalArgumentException}
     * is thrown. The necessary consistent argument conditions are:
     * <ul>
     * <li>{@code count >= 0}</li>
     * <li>{@code min <= max}</li>
     * </ul>
     *
     * @param count the count of values
     * @param min the minimum value
     * @param max the maximum value
     * @param sum the sum of all values
     * @throws IllegalArgumentException if the arguments are inconsistent or given with {@code
     * null}.
     */
    public BigDecimalSummaryStatistics(long count, BigDecimal min, BigDecimal max,
        BigDecimal sum) {

        if (count < 0L) {
            throw new IllegalArgumentException("count must be greater or equal to zero.");
        } else if (count > 0L) {
            if (min == null) {
                throw new IllegalArgumentException("min is not allowed to be null.");
            }
            if (max == null) {
                throw new IllegalArgumentException("max is not allowed to be null.");
            }
            if (sum == null) {
                throw new IllegalArgumentException("sum is not allowed to be null.");
            }

            if (min.compareTo(max) > 0) {
                throw new IllegalArgumentException("Minimum is greater than maximum.");
            }

            this.count = count;
            this.sum = sum;

            this.min = min;
            this.max = max;
        }

    }

    /**
     * Records a new {@code BigDecimal} value into the summary information.
     *
     * @param value the input value
     * @throws IllegalArgumentException in case of giving {@code null} for {@code value}.
     */
    @Override
    public void accept(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("value is not allowed to be null.");
        }

        count++;
        sum = sum.add(value);

        if (min == null) {
            min = value;
            max = value;
        } else {
            min = min.min(value);
            max = max.max(value);
        }
    }

    /**
     * Combines the state of another {@code BigDecimalSummaryStatistics} into this one.
     *
     * @param other another {@code BigDecimalSummaryStatistics}
     * @throws IllegalArgumentException in case of giving {@code null} for {@code value}.
     */
    public void combine(BigDecimalSummaryStatistics other) throws IllegalArgumentException {
        if (other == null) {
            throw new IllegalArgumentException("other is not allowed to be null.");
        }

        count += other.count;
        sum = sum.add(other.sum);

        if (min == null) {
            min = other.min;
            max = other.max;
        } else {
            min = min.min(other.min);
            max = max.max(other.max);
        }
    }

    /**
     * Returns the count of values recorded.
     *
     * @return the count of values
     */
    public final long getCount() {
        return count;
    }

    /**
     * Returns the sum of values recorded, or zero if no values have been recorded.
     *
     * @return the sum of values, or zero if none
     */
    public final BigDecimal getSum() {
        return sum;
    }

    /**
     * Returns the minimum value recorded.
     *
     * @return The minimun which has been calculated.
     * @throws IllegalStateException in case of {@code count=0}.
     * @implSpec We can't give back a thing like Double#POSITIVE_INFINITY cause this can't be
     * converted to a BigDecimal.
     */
    public final BigDecimal getMin() {
        if (this.count == 0) {
            throw new IllegalStateException(
                "Minimum can not be calculated cause we have no values yet.");
        }
        return min;
    }

    /**
     * Returns the maximum value recorded.
     *
     * @return the maximum value
     * @throws IllegalStateException in case of {@code count=0}.
     * @implSpec We can't give back a thing like Double#NEGATIVE_INFINITY cause this can't be
     * converted to a BigDecimal.
     */
    public final BigDecimal getMax() {
        if (this.count == 0) {
            throw new IllegalStateException(
                "Maximum can not be calculated cause we have no values yet.");
        }
        return max;
    }

    /**
     * Returns the arithmetic mean of values recorded, or zero if no values have been recorded.
     *
     * @return The arithmetic mean of values, or zero if none
     */
    public final BigDecimal getAverage() {
        if (this.count > 0) {
            return this.sum.divide(BigDecimal.valueOf(this.count));
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Returns a non-empty string representation of this object suitable for debugging. The exact
     * presentation format is unspecified and may vary between implementations and versions.
     */
    @Override
    public String toString() {
        return String.format(
            "%s{count=%s, sum=%s, min=%s, average=%s, max=%s}",
            this.getClass()
                .getSimpleName(),
            getCount(),
            getSum().toString(),
            getMin().toString(),
            getAverage().toString(),
            getMax().toString()
        );
    }

}
