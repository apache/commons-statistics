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
 * This class has methods to compute Second Moment.
 */
class SecondMoment {
    /***/
    protected double s;

    /***/
    protected double mean1;

    /***/
    protected long countN;

    /***/
    protected double m2;

    /**Constructor for SecondMoment class.*/
    SecondMoment() {
        m2 = Double.NaN;
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

}
