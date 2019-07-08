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
    /***/
    private double s;

    /***/
    private boolean isBiasCorrected = true;

    /***/
    private double mean1;

    /***/
    private long countN;

    /***/
    private double m2;

    /**Constructor for Variance class.*/
    public Variance() {
        m2 = Double.NaN;
    }

    /**
     * This method calculates Variance based on Welford's Algorithm.
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
        double x;
        double mean0;
        countN++;
        x = value;
        mean0 = mean1;
        mean1 += (x - mean1) / countN;
        s += (x - mean1) * (x - mean0);
        //variance = s / (countN - 1);
        m2 = s;
    }

    /**
     * <p>This method combines object of Variance class with another object of same class. </p>
     * @param var2 Variance class object
     */
    public void combine(Variance var2) {
        final double delta = var2.getMean() - mean1;
        final long sum = getN() + var2.getN();
        //double m_a = variance * (countN - 1);
        //double m_b = var2.getVariance() *(var2.getN()- 1);
        m2 = getm2() + var2.getm2() + Math.pow(delta, 2) * countN * var2.getN() / sum;
        //Variance will be calculated using getVariance() method which will yield wrong result
        //after calculation. Hence assigning s=m2; so that it will be calculated in getVariance()
        //Method accordingly.
        //variance = m2 / (getN() + var2.getN()- 1);
        //s=m2;
        mean1 = (var2.getN() * var2.getMean() + getMean() * getN()) / sum;
        countN = sum;
    }

    /**
     * <p>This method calculates Variance value when boolean parameter is provided.</p>
     * @param isBiasC Boolean value to decide Variaance type i.e. population/sample
     * @return Variance
     */
    public double getVariance(boolean isBiasC) {
        if (isBiasC) {
            return m2 / (countN - 1);
        }
        return  m2 / countN;
    }

    /**
     * <p>This method calculates Variance value when no parameter is provided.</p>
     * @return Variance
     */
    public double getVariance() {
        if (biasCorrected()) {
            return m2 / (countN - 1);
        }
        return  m2 / countN;
    }

    /**
     *@return m2
     */
    public double getm2() {
        return m2;
    }

    /**
     * This method gives the count of values added.
     * @return countN-count of values
     */
    public long getN() {
        return countN;
    }

    /**
     *@return mean value
     */
    public double getMean() {
        return mean1;
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
