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

import org.apache.commons.math4.stat.descriptive.moment.Variance;
import org.apache.commons.statistics.regression.stored.data_input.RegressionDataHolder;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public abstract class AbstractRegression extends RegressionDataHolder implements Regression {

    /**
     * Calculates the beta of multiple linear regression in matrix notation.
     *
     * @return beta
     */
    protected abstract StatisticsMatrix calculateBeta();

    /**
     * Calculates the beta variance of multiple linear regression in matrix
     * notation.
     *
     * @return beta variance
     */
    protected abstract StatisticsMatrix calculateBetaVariance();

    /**
     * Calculates the variance of the y values.
     *
     * @return Y variance
     */
    public double calculateYVariance() {
        return new Variance().evaluate(getY().toArray1D());
    }

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
    public double calculateErrorVariance() {
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
    public StatisticsMatrix calculateResiduals() {
        StatisticsMatrix b = calculateBeta();
        return getY().minus(getX().mult(b)); // operate is for vec x vec in CM
    }

    /**
     * Validates that the x data and covariance matrix have the same number of rows
     * and that the covariance matrix is square.
     *
     * @param x          the [n,k] array representing the x sample
     * @param covariance the [n,n] array representing the covariance matrix
     * @throws IllegalArgumentException if the number of rows in x is not equal to
     *                                  the number of rows in covariance
     * @throws IllegalArgumentException if the covariance matrix is not square
     */
    protected void validateCovarianceData(double[][] x, double[][] covariance) {
        if (x.length != covariance.length) {
            throw new IllegalArgumentException("x.length = " + x.length + "  covariance.lenth = " + covariance.length);
        }
        if (covariance.length > 0 && covariance.length != covariance[0].length) {
            throw new IllegalArgumentException(
                "covariance.length = " + covariance.length + "  covariance[0].length = " + covariance[0].length);
        }
    }
}
