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
     * {@inheritDoc}
     */
    @Override
    public double[] estimateBeta() {
        return calculateBeta().toArray1D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[][] estimateBetaVariance() {
        return calculateBetaVariance().toArray2D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateBetaStandardErrors() {
        return calculateBetaStandardErrors().toArray1D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateResiduals() {
        return calculateResiduals().toArray1D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double estimateRegressionStandardError() {
        return Math.sqrt(calculateRegressionErrorVariance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double estimateRegressandVariance() {
        return new Variance().evaluate(getY().toArray1D());
    }

    /**
     * Calculates the beta of multiple linear regression in matrix notation.
     *
     * @return betas as vector
     */
    protected abstract StatisticsMatrix calculateBeta();

    /**
     * Calculates the beta variance of multiple linear regression in matrix
     * notation.
     *
     * @return betas variance as vector
     */
    protected abstract StatisticsMatrix calculateBetaVariance();

    /**
     * Calculates the beta standard errors of multiple linear regression in matrix
     * notation by retrieving the diagonal elements from betaVariance, then
     * multiplying by sigma and square rooting each element.
     *
     * @return betas variance as vector
     */
    protected StatisticsMatrix calculateBetaStandardErrors() {
        StatisticsMatrix betaVariance = calculateBetaVariance();
        double sigma = calculateRegressionErrorVariance();
        //
        return betaVariance.diag().scale(sigma).elementPower(0.5);
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
     */
    public double calculateRegressionErrorVariance() {
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
