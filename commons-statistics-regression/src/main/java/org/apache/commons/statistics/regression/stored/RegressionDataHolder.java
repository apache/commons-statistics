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

public abstract class RegressionDataHolder {

    /** Contains the loaded input data. */
    protected RegressionData inputData;

    /**
     * @return Y vector data.
     */
    public StatisticsMatrix getY() {
        return inputData.getYData();
    }

    /**
     * @return X matrix data.
     */
    public StatisticsMatrix getX() {
        return inputData.getXData();
    }

    /**
     * @return boolean if calculations should include an intercept.
     */
    public boolean getHasIntercept() {
        return inputData.getHasIntercept();
    }

}
