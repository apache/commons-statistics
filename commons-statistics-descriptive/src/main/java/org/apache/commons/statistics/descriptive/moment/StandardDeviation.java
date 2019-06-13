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

import org.apache.commons.statistics.descriptive.moment.Variance;

/**
 * Computes the sample standard deviation.  The standard deviation
 * is the positive square root of the variance.
 *
 */
public class StandardDeviation {

    /**Standard Deviation value.*/
    private double stdD = 0;

    /**Constructs a StandardDeviation.*/
    public StandardDeviation() {}

    /**
     *Compute and store the standard deviation value into "stdD".
     *@param variance Object of Variance class
     *@return stdD - "standard deviation value."
     */
    public double setStandardDeviation(Variance variance){
        stdD = Math.sqrt(variance.getVariance());
        return stdD;
    }

    /**
     * Returns the standard deviation of the values.
     *@return  stdD - "standard deviation value."
     */
    public double getStandardDeviaiton(){
        return stdD;
    }

}
