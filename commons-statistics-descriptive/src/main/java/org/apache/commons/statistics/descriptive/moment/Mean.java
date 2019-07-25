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
 * Computes the arithmetic mean of a set of values. Uses the definitional
 * formula:
 * <p>
 * mean = sum(x_i) / n
 * </p>
 * <p>where <code>n</code> is the number of observations.
 * </p>
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}. For example, you can compute
 * Mean on a stream of doubles with:
 * <pre> {@code
 * Mean stats = doubleStream.collect(Mean::new,
 *                                     Mean::accept,
 *                                     Mean::combine);
 * }</pre>
 *
 * <p>Here {@link #accept(double)} is used to add data incrementally from a
 * stream of (unstored) values, the value of the statistic that
 * {@link #getMean()} returns is computed using the Welford's Algorithm.
 * The Welford's Algorithm is as follows:<br>
 *<pre><code>
 *variance(samples):
 *    mean1 := 0
 *    s := 0
 *    for k from 1 to N:
 *        x := samples[k]
 *        mean0 := mean1
 *        mean1 := mean1 + (x-mean1)/k
 *        s := s + (x-mean1)*(x-mean0)
 *    return s/(N-1)</code></pre><br>
 *  <p>Here to calculate Mean value only partial algorithm is used.</p>
 *  <p>Returns <code>Double.NaN</code> if the dataset is empty. Note that
 *  Double.NaN may also be returned if the input includes NaN and / or infinite
 *  values.
 * </p>
 */
public class Mean {

    /** Total no. of values. */
    private long n;
    /** Current value of mean. */
    private double meanValue;
    /** Sum of values added. */
    private double sum;

    /** Create a Mean instance. */
    public Mean() {
        n = 0;
        meanValue = 0.0;
        sum = 0.0;
    }

    /**
     * This method accept stream of double values and calculates Mean
     * based on Welford's Algorithm.
     * The Welford's Algorithm is as follows:<br>
     *<pre><code>
     *variance(samples):
     *    mean1 := 0
     *    s := 0
     *    for k from 1 to N:
     *        x := samples[k]
     *        mean0 := mean1
     *        mean1 := mean1 + (x-mean1)/k
     *        s := s + (x-mean1)*(x-mean0)
     *    return s/(N-1)</code></pre>
     * Here to calculate Mean value only partial algorithm is used.
     *@param value stream of double values
     */
    public void accept(double value) {
        n++;
        meanValue += (value - meanValue) / n;
        sum += value;
    }

    /**
     * This  method combines the object of Mean class with the current Mean object to calculate
     * combined mean value.
     * Algorithm:
     * <pre><code>
     *   mean = (nA * meanA + nB * meanB) / (nA + nB)
     *   </code></pre>
     *   (This will cause problem when combining two objects where one has some value
     *    and other isn't initiated. Thus mean of that object will be returned as NaN
     *    from getMean() method. And addition of NaN with finite number will result in
     *    NaN.)
     *    OR
     *    Hence we'll use the following for our calculation.
     *    <pre><code>
     *   mean = (sumA + sumB) / (nA + nB)
     *   </code></pre>
     * @param m1 Object of Mean class
     */
    public void combine(Mean m1) {
        n = getN() + m1.getN();
        sum = getSum() + m1.getSum();
        meanValue = sum / n;
    }

    /**
     * This method gives  the current mean value.
     * If the object isn't initiated i.e. if n=0 it returns NaN because
     * according to definition <code>{mean = sum / n}</code>
     * Hence 0/0 is Not defined.
     * @return Mean value
     */
    public double getMean() {
        return n == 0 ? Double.NaN : meanValue;
    }

    /**
     * This method gives the count of values added.
     * @return count of values
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
             "%s{mean = %f , count = %d , sum = %f}",
             getClass().getSimpleName(),
             getMean(),
             getN(),
             getSum());
    }

}
