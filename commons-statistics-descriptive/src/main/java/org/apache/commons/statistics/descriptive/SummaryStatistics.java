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

import java.util.DoubleSummaryStatistics;

import org.apache.commons.statistics.descriptive.moment.Variance;
import org.apache.commons.statistics.descriptive.moment.StandardDeviation;

/**
* <p>
* Computes summary statistics for a stream of data values added using the
* {@link #accept(double) accept} method. The data values are not stored in
* memory, so this class can be used to compute statistics for very large data
* streams.For example, you can compute summary statistics on a stream of
* doubles with:
* </p>
* <pre> {@code
* SummaryStatistics stats = doubleStream.collect(SummaryStatistics::new,
*                                                SummaryStatistics::accept,
*                                                SummaryStatistics::combine);
* }</pre>
* <p>
* Where {@code doubleStream} is stream of primitive double-valued elements received from
* <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/DoubleStream.html"> DoubleStream</a>.
* </p>
* <p>
* This class computes count, minimum, maximum, sum,average, variance and
* standard deviation of double values in a single pass.
* </p>
*/
public class SummaryStatistics extends DoubleSummaryStatistics {

    /**An object of Variance class.*/
    private Variance variance = new Variance();

    /**An object of StandardDeviation class.*/
    private StandardDeviation stdDeviation = new StandardDeviation();

    /**
    * Construct an empty instance with zero count, zero sum,
    * {@code Double.POSITIVE_INFINITY} min, {@code Double.NEGATIVE_INFINITY}
    * max, zero average, zero stdDeviation and zero variance.
    */
    public SummaryStatistics() {
        super();
    }

    /**
     * Records another value into the summary information.
     *
     * @param value the input value
     */
    @Override
    public void accept(double value){
        super.accept(value);
        variance.setVariance(value);
        stdDeviation.setStandardDeviation(variance);
    }

    /**
     * Combines the state of another {@code SummaryStatistics} into this
     * one.
     *
     * @param other another {@code SummaryStatistics}
     */
    public void combine(SummaryStatistics other) {
        super.combine(other);
        variance.setVariance(other.variance.getVariance());
        stdDeviation.setStandardDeviation(other.variance);
    }

    /**
     * {@inheritDoc}
     *
     * Returns a non-empty string representation of this object suitable for
     * debugging. The exact presentation format is unspecified and may vary
     * between implementations and versions.
     */
    @Override
    public String toString() {
        return String.format(
            "%s{count=%d, sum=%f, min=%f, average=%f, max=%f, variance=%f, Standard Deviaiton=%f}",
            this.getClass().getSimpleName(),
            getCount(),
            getSum(),
            getMin(),
            getAverage(),
            getMax(),
            variance.getVariance(),
            stdDeviation.getStandardDeviaiton());
    }

}
