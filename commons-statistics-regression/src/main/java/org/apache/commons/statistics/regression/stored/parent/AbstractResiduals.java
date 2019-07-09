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
package org.apache.commons.statistics.regression.stored.parent;

import org.apache.commons.statistics.regression.stored.RegressionDataHolder;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public abstract class AbstractResiduals extends RegressionDataHolder {

    /**
     * The calculated betas (estimators) from calculateBeta method inside
     * AbstractEstimators class.
     */
    protected StatisticsMatrix betasMatrix;

    /**
     * <p>
     * Calculates the variance of the error term.
     * </p>
     * Uses the formula
     *
     * <pre>
     * var(u) = u &middot; u / (n - k)
     * </pre>
     *
     * where n and k are the row and column dimensions of the design matrix X.
     *
     * @return error variance estimate
     * @since 2.2
     */
    protected double calculateErrorVariance() {
        StatisticsMatrix residuals = calculateResiduals();
        return residuals.dot(residuals) / (getX().getDDRM().getNumRows() - getX().getDDRM().getNumCols());
    }

    /**
     * Calculates the residuals of multiple linear regression in matrix notation.
     *
     * <pre>
     * u = y - X * b
     * </pre>
     *
     * @return The residuals [n,1] matrix
     */
    protected StatisticsMatrix calculateResiduals() {
        StatisticsMatrix b = betasMatrix;
        return getY().minus(getX().mult(b)); // operate is for vec x vec in CM
    }

}
