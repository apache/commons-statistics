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

/**
 * Computes the sum of squared deviations from the sample mean. This
 * statistic is related to the second moment.
 *
 * <p>
 * The following recursive updating formula is used:
 * <p>
 * Let <ul>
 * <li> dev = (current obs - previous mean) </li>
 * <li> n = number of observations (including current obs) </li>
 * </ul>
 * Then
 * <p>
 * new value = old value + dev^2 * (n -1) / n.
 * <p>
 *
 * Returns the sum of squared deviations of all values seen so far.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the <code>increment()</code> or
 * <code>clear()</code> method, it must be synchronized externally.
 *
 * <p>However, it is safe to use <code>accept()</code> and <code>combine()</code>
 * as <code>accumulator</code> and <code>combiner</code> functions of
 * {@link java.util.stream.Collector Collector} on a parallel stream,
 * because the parallel implementation of {@link java.util.stream.Stream#collect Stream.collect()}
 * provides the necessary partitioning, isolation, and merging of results for
 * safe and efficient parallel execution.
 */
class SumOfSquaredDeviations implements DoubleStatistic, DoubleStatisticAccumulator<SumOfSquaredDeviations> {
    /** Sum of squared deviations of the values that have been added. */
    private double squaredDevSum;

    /** First moment instance which holds the first moment of values that have been added.
     * This is required because {@link FirstMoment#getDev()} and {@link FirstMoment#getDevNormalizedByN()} are
     * used in the updating the sum of squared deviations.
     */
    private final FirstMoment firstMoment;

    /**
     * Create a FirstMoment instance.
     */
    SumOfSquaredDeviations() {
        firstMoment = new FirstMoment();
    }

    /**
     * Create a SumOfSquaredDeviations instance with the given sum of
     * squared deviations and a FirstMoment instance.
     *
     * @param squaredDevSum Sum of squared deviations.
     * @param mean Mean of values.
     * @param n Number of values.
     * @param nonFiniteValue Sum of values.
     */
    SumOfSquaredDeviations(double squaredDevSum, double mean, long n, double nonFiniteValue) {
        this.squaredDevSum = squaredDevSum;
        firstMoment = new FirstMoment(mean, n, nonFiniteValue);
    }

    /**
     * Updates the state of the statistic to reflect the addition of {@code value}.
     * @param value Value.
     */
    @Override
    public void accept(double value) {
        firstMoment.accept(value);
        if (Double.isInfinite(value)) {
            squaredDevSum = Double.NaN;
            return;
        }
        final double n = firstMoment.getN();
        squaredDevSum += (n - 1) * firstMoment.getDev() * firstMoment.getDevNormalizedByN();
    }

    /**
     * Gets the sum of squared deviations of all input values.
     *
     * @return {@code SumOfSquaredDeviations} of all values seen so far.
     */
    @Override
    public double getAsDouble() {
        return squaredDevSum;
    }

    /** {@inheritDoc} */
    @Override
    public SumOfSquaredDeviations combine(SumOfSquaredDeviations other) {
        final long oldN = firstMoment.getN();
        final long otherN = other.getN();
        if (oldN == 0) {
            squaredDevSum = other.squaredDevSum;
        } else if (otherN != 0) {
            final double sqDiffOfMean =
                Math.pow((other.firstMoment.getAsDouble() * 0.5 - firstMoment.getAsDouble() * 0.5) * 2, 2);
            squaredDevSum += other.squaredDevSum + sqDiffOfMean * ((double) (oldN * otherN) / (oldN + otherN));
        }
        firstMoment.combine(other.firstMoment);
        return this;
    }

    /**
     * @return Number of values seen so far.
     */
    long getN() {
        return firstMoment.getN();
    }
}
