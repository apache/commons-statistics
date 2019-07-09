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
package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public class RegressionRawData implements RegressionData {

    /** Y vector data as StatisticsMatrix wrapper. */
    private StatisticsMatrix yVector;

    /** X matrix data as StatisticsMatrix wrapper. */
    private StatisticsMatrix xMatrix;

    /** boolean true if sample data has an intercept. */
    private boolean hasIntercept;

//    public RegressionRawData(StatisticsMatrix yData, StatisticsMatrix xData, Boolean hasIntercept) {
//        this.yMatrix = yData;
//        this.xMatrix = xData;
//        this.hasIntercept = hasIntercept;
//    }
//
//    public RegressionRawData(StatisticsMatrix yData, StatisticsMatrix xData) {
//        this.yMatrix = yData;
//        this.xMatrix = xData;
//        this.hasIntercept = true;
//    }

    /**
     * Creates an empty constructor.
     */
    public RegressionRawData() {
        this.xMatrix = null;
        this.yVector = null;
        this.hasIntercept = false;
    }

    /**
     * Sets Y vector data.
     *
     * @param yMatrix
     */
    public void setYData(StatisticsMatrix yMatrix) {
        this.yVector = yMatrix;
    }

    /**
     * Sets X matrix data.
     *
     * @param xMatrix
     */
    public void setXData(StatisticsMatrix xMatrix) {
        this.xMatrix = xMatrix;
    }

    /**
     * Sets hasIntercept boolean, {@code true} if sample data already contains
     * intercept (column of 1's). {@code false} if column of 1's should be created
     * when loading X data.
     *
     * @param hasIntercept
     */
    public void setHasIntercept(boolean hasIntercept) {
        this.hasIntercept = hasIntercept;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatisticsMatrix getYData() {
        return this.yVector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatisticsMatrix getXData() {
        return this.xMatrix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getHasIntercept() {
        return this.hasIntercept;
    }
}
