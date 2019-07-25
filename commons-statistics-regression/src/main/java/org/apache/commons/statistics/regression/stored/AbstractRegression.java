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
import org.apache.commons.statistics.regression.stored.data_input.RegressionData;
import org.apache.commons.statistics.regression.stored.data_input.RegressionDataHolder;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

/**
 * Holds all common functionality of all Regression types.
 */
public abstract class AbstractRegression extends RegressionDataHolder implements Regression {

    /** Stored beta to avoid recalculation. */
    protected StatisticsMatrix beta;

    /** Stored beta variance to avoid recalculation. */
    protected StatisticsMatrix betaVariance;

    /** Stored beta standard error to avoid recalculation. */
    protected StatisticsMatrix betaStandardError;

    /** Stored residuals to avoid recalculation. */
    protected StatisticsMatrix residuals;

    /** Stored error variance to avoid recalculation. */
    protected double errorVariance;

    /** Stored standard error to avoid recalculation. */
    protected double standardError;

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
        return calculateRegressionStandardError();
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
     * Clears all data to default null and NaN values.
     */
    protected abstract void clearData();

    /**
     * Calculates the beta standard errors of multiple linear regression in matrix
     * notation by retrieving the diagonal elements from betaVariance, then
     * multiplying by sigma and square rooting each element.
     *
     * @return betas variance as vector
     */
    protected StatisticsMatrix calculateBetaStandardErrors() {
        if (betaStandardError == null) {
            final StatisticsMatrix betaVar = calculateBetaVariance();
            final double sigma = calculateRegressionErrorVariance();
            final StatisticsMatrix result = betaVar.diag().scale(sigma).elementPower(0.5);
            betaStandardError = result;
        }
        return betaStandardError;
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
        if (residuals == null) {
            final StatisticsMatrix b = calculateBeta();
            final StatisticsMatrix result = getY().minus(getX().mult(b));
            residuals = result;
        }
        return residuals;
    }

    /**
     * Calculates the standard error by square rotting error variance.
     *
     * @return regression's standard error
     */
    public double calculateRegressionStandardError() {
        if (Double.isNaN(standardError)) {
            standardError = Math.sqrt(calculateRegressionErrorVariance());
        }
        return standardError;
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
        if (Double.isNaN(errorVariance)) {
            final StatisticsMatrix res = calculateResiduals();
            final double result = res.dot(res) /
                                  (getX().getDDRM().getNumRows() - getX().getDDRM().getNumCols());
            errorVariance = result;
        }
        return errorVariance;
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

    /**
     * Validates that input data is not null when passed into a Regression
     * functional concrete class.
     *
     * @param data as RegressionRawData class which holds X and Y data
     * @throws IllegalArgumentException if data or it's contained X or Y
     *                                  StatisticsMatrix objects are null.
     */
    protected void validateLoadedInputData(RegressionData data) {
        if (data == null) {
            throw new IllegalArgumentException("RegressionData object is null.");
        }
        if (data.getY() == null) {
            throw new IllegalArgumentException("Y data is null.");
        }
        if (data.getX() == null) {
            throw new IllegalArgumentException("X data is null.");
        }
    }
}
