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

package org.apache.commons.statistics.descriptive.moment;

/**
 * This class has methods to compute First Moment.
 */
public class Mean {

    /** Total no. of values. */
    protected long n;
    /** Current value of mean. */
    protected double mean1;
    /** Sum of values added. */
    protected double sum;
    /** Last mean value. */
    protected double mean0;

    /** Create a Mean instance. */
    public Mean() {
        n = 0;
        mean1 = Double.NaN;
        mean0 = Double.NaN;
        sum = 0;
    }

    /**
     * Accept stream of values and apply Updating algorithm to calculate
     * mean value.
     * Algorithm:
     * add d:
     *     n = n + 1;
     *     mean1 = mean0 + (d - mean0) / n
     * @param d Values to calculate mean.
     */
    public void accept(double d) {
        if (n == 0) {
            mean0 = 0.0;
        } else {
            mean0 = sum / n;
        }
        sum += d;
        n++;
        mean1 = mean0 + (d - mean0) / n;
    }

    /**
     * This  method combines the object of Mean class with other object to calculate
     * combined mean value.
     * Algorithm:
     *   mean = (nA * meanA + nB * meanB) / (nA + nB)
     *    OR
     *   mean = (sumA + sumB) / (nA + nB)
     * @param m1 Object of Mean class
     */
    public void combine(Mean m1) {
        mean1 = (m1.n * m1.mean1 + getMean() * getN()) / (getN() + m1.n);
        n = getN() + m1.getN();
        sum = getSum() + m1.getSum();
    }

    /**
     * This method gives  the current mean value.
     * @return Mean value
     */
    public double getMean() {
        return mean1;
    }

    /**
     * This method gives the count of values added.
     * @return n-count of values
     */
    public long getN() {
        return n;
    }

    /**
     * This method gives the total sum of added values.
     * @return Sum of all the added values
     */
    public double getSum() {
        return sum;
    }

    /**
     * {@inheritDoc}
     * Returns a non-empty string representation of this object suitable for
     * debugging. The exact presentation format is unspecified and may vary
     * between implementations and versions.
     */
    @Override
    public String toString() {
        return String.format(
             "%s{mean=%f}",
             getClass().getSimpleName(),
             getMean());
    }

}
