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
 * Computes the variance of the available values.  By default, the unbiased
 * "sample variance" definitional formula is used:
 * <p>
 * variance = sum((x_i - mean)^2) / (n - 1) </p>
 * <p>
 * where mean is the <a href="https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html#getAverage--">
 *  getAverage</a> and <code>n</code> is the number
 * of observations.</p>
 * <p>
 * The definitional formula does not have good numerical properties, so
 * this implementation does not compute the statistic using the definitional
 * formula.
 * <ul>
 * <li>The <code>setVariancce</code> method computes the variance using
 * updating formulas based on Welford's algorithm</li>
 * </ul>
 */
public class Variance {

    /***/
    private double M=0;

    /***/
    private double S=0;

    /***/
    private double x=0;

    /***/
    private double oldM=0;

    /***/
    private double variance=0;

    /***/
    private double count=0;

    /**Constructor for Variance class. */
    public Variance() {}

    /**
     * This method calculates Variance based on Welford's Algorithm.
     * The Welford's Algorithm is as follows:<br>
     *<pre><code>
     *variance(samples):
     *    M := 0
     *    S := 0
     *    for k from 1 to N:
     *        x := samples[k]
     *        oldM := M
     *        M := M + (x-M)/k
     *        S := S + (x-M)*(x-oldM)
     *    return S/(N-1)</code></pre>
     *@param value stream of values
     *@return variance of stream of values
     */
    public double setVariance(double value){
        count++;
        x=value;
        oldM=M;
        M += (x-M)/count;
        S += (x-M)*(x-oldM);
        variance=S/(count-1);
        return variance;
    }

    /**
     * Returns the variance of stream of values
     * @return variance of stream of values
     */
    public double getVariance(){
        return variance;
    }

}
