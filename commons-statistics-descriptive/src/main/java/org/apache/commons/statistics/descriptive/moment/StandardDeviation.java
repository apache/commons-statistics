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
 * Computes the sample standard deviation.  The standard deviation
 * is the positive square root of the variance.  This implementation extends
 * {@link SecondMoment} class. The <code>isBiasCorrected</code> property is used,
 * so that this class can be used to compute both the "sample standard deviation"
 * (the square root of the bias-corrected "sample variance") or the "population
 * standard deviation"(the square root of the non-bias-corrected "population variance").
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the <code>accept()</code> or
 * <code>clear()</code> method, it must be synchronized externally.</p>
 *
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}. For example, you can compute
 * summary statistics on a stream of doubles with:
 * <pre> {@code
 * StandardDeviation stats = doubleStream.collect(StandardDeviation::new,
 *                                                      StandardDeviation::accept,
 *                                                      StandardDeviation::combine);
 * }</pre>
 *
 *
 */
public class StandardDeviation {

    /** SecondMoment on which this statistic is based. */
    protected SecondMoment moment;

    /**
     * Whether or not bias correction is applied when computing the
     * value of the statistic. True means that bias is corrected.
     */
    private boolean isBiasCorrected = true;

    /**Constructs a StandardDeviation.*/
    public StandardDeviation() {
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
     * @return m2
     */
    public double getm2() {
        return moment.getm2();
    }


    /**
     * This method gives the count of values added.
     * @return countN-count of values
     */
    public long getN() {
        return moment.getN();
    }


    /**
     * @return mean value
     */
    public double getMean() {
        return moment.getMean();
    }

    /**
     * <p>This method calculates StandardDeviation value when no parameter is provided.</p>
     * @return Variance
     */
    public double getStandardDeviation() {
        if (biasCorrected()) {
            return Math.sqrt(moment.m2 / (moment.countN - 1));
        }
        return  Math.sqrt(moment.m2 / moment.countN);
    }

    /**
     * <p>This method calculates StandardDeviation value when boolean parameter is provided.</p>
     * @param isBiasC Boolean value to decide StandardDeviation type i.e. population/sample
     * @return StandardDeviation
     */
    public double getStandardDeviation(boolean isBiasC) {
        if (isBiasC) {
            return Math.sqrt(moment.m2 / (moment.countN - 1));
        }
        return Math.sqrt(moment.m2 / moment.countN);
    }

    /**
     * <p>This method combines StandardDeviation class' internal object of SecondMoment with another object of same class. </p>
     * @param stdDev StandardDeviation class object.
     */
    public void combine(StandardDeviation stdDev) {
        moment.combine(stdDev.moment);
    }

    /**
     * @return Returns the isBiasCorrected.
     */
    public boolean biasCorrected() {
        return isBiasCorrected;
    }

    /**
     * @param biasCorrected The isBiasCorrected to set.
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
             "%s{standard deviation = %f}",
             getClass().getSimpleName(),
             getStandardDeviation());
    }

}
