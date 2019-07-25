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
 * This class provides the methods to add values and calculate Variance.
 * <pre> {@code
 * Variance stats = doubleStream.collect(Variance::new,
 *                                       Variance::accept,
 *                                       Variance::combine);
 * }</pre>
 */
public class Variance {


    /** SecondMoment on which this statistic is based. */
    protected SecondMoment moment;

    /***/
    private boolean isBiasCorrected = true;

    /**Constructor for Variance class.*/
    public Variance() {
        moment = new SecondMoment();
    }

    /**
     * This method calculates SecondMoment based on Welford's Algorithm.
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
     *@param value stream of values
     */
    public void accept(double value) {
        moment.accept(value);
    }

    /**
     * <p>This method combines Variance class' internal object of SecondMoment with another object of same class. </p>
     * @param var2 Variance class object.
     */
    public void combine(Variance var2) {
        moment.combine(var2.moment);
    }

    /**
     * <p>This method calculates Variance value when boolean parameter is provided.</p>
     * @param isBiasC Boolean value to decide Variance type i.e. population/sample
     * @return Variance
     */
    public double getVariance(boolean isBiasC) {
        if (isBiasC) {
            return moment.m2 / (moment.countN - 1);
        }
        return  moment.m2 / moment.countN;
    }

    /**
     * <p>This method calculates Variance value when no parameter is provided.</p>
     * @return Variance
     */
    public double getVariance() {
        if (biasCorrected()) {
            return moment.m2 / (moment.countN - 1);
        }
        return  moment.m2 / moment.countN;
    }

    /**
     * @return m2
     */
    public double getm2() {
        return moment.m2;
    }

    /**
     * This method gives the count of values added.
     * @return countN-count of values
     */
    public long getN() {
        return moment.countN;
    }

    /**
     * @return mean value
     */
    public double getMean() {
        return moment.mean1;
    }

    /**
     *@return Boolean value, for getting Population/Sample variance.
     */
    public boolean biasCorrected() {
        return isBiasCorrected;
    }

    /**
     *@param biasCorrected Boolean value for Population/Sample variance.
     */
    public void setBiasCorrected(boolean biasCorrected) {
        this.isBiasCorrected = biasCorrected;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a non-empty string representation of this object suitable for
     * debugging. The exact presentation format is unspecified and may vary
     * between implementations and versions.</p>
     */
    @Override
    public String toString() {
        return String.format(
             "%s{variance = %f}",
             getClass().getSimpleName(),
             getVariance());
    }

}
