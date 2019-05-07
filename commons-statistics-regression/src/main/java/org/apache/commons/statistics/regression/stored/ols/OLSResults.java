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
package org.apache.commons.statistics.regression.stored.ols;

import java.io.Serializable;

import org.apache.commons.statistics.regression.stored.RegressionResults;

public class OLSResults implements RegressionResults, Serializable {

    /** Auto-generated serialVersionUID. */
    private static final long serialVersionUID = 814805422428533467L;

    /** calculated beta. */
    private final double[] beta;
    /** calculated betaVariance. */
    private final double[][] betaVariance;
    /** calculated betaStandardErrors. */
    private final double[] betaStandardErrors;
    /** calculated residuals. */
    private final double[] residuals;
    /** calculated regressionStandardErrors. */
    private final double regressionStandardErrors;
    /** calculated regressandVariance. */
    private final double regressandVariance;

    /** calculated hat matrix. */
    private final double[][] hatMatrix;
    /** calculated total sum of squares. */
    private final double totalSumOfSquares;
    /** calculated residual sum of squares. */
    private final double residualSumOfSquares;
    /** calculated R Squared. */
    private final double regRSquared;
    /** calculated adjusted R Squared. */
    private final double adjustedRSquared;

    /**
     * <p>
     * Creates the OLSResults object by calculating/estimating all OLS related
     * statistics which are then ready to be retrieved (without having to be
     * recalculated).
     * </p>
     * <p>
     * If OLS statistics are to be calculated individually, please see corresponding
     * public methods in OLSRegression, which are used in this constructor as well.
     * </p>
     *
     * @param ols contains all OLS functionality and pointer to original input data.
     */
    protected OLSResults(OLSRegression ols) {
        this.beta = ols.estimateBeta();
        this.betaVariance = ols.estimateBetaVariance();
        this.betaStandardErrors = ols.estimateBetaStandardErrors();
        this.residuals = ols.estimateResiduals();
        this.regressionStandardErrors = ols.estimateRegressionStandardError();
        this.regressandVariance = ols.estimateRegressandVariance();

        this.hatMatrix = ols.calculateHat().toArray2D();
        this.totalSumOfSquares = ols.calculateTotalSumOfSquares();
        this.residualSumOfSquares = ols.calculateResidualSumOfSquares();
        this.regRSquared = ols.calculateRSquared();
        this.adjustedRSquared = ols.calculateAdjustedRSquared();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getBeta() {
        return beta;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[][] getBetaVariance() {
        return betaVariance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getBetaStandardErrors() {
        return betaStandardErrors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getResiduals() {
        return residuals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRegressionStandardError() {
        return regressionStandardErrors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRegressandVariance() {
        return regressandVariance;
    }

    /**
     * <p>
     * Returns the computed "hat" matrix.
     * </p>
     * <p>
     * See {@code calculateHat()} (which returns as a {@code StatisticsMatrix}
     * object) in {@code OLSRegression} for more details.
     * </p>
     * <p>
     * The hat matrix is defined in terms of the design matrix X by
     * X(X<sup>T</sup>X)<sup>-1</sup>X<sup>T</sup>
     * </p>
     *
     * @return the hat matrix
     */
    public double[][] getHatMatrix() {
        return hatMatrix;
    }

    /**
     * <p>
     * Returns the sum of squared deviations of Y from its mean.
     * </p>
     * <p>
     * See {@code calculateTotalSumOfSquares()} in {@code OLSRegression} class for
     * more details.
     * </p>
     *
     * @return SSTO - the total sum of squares
     */
    public double getTotalSumOfSquares() {
        return totalSumOfSquares;
    }

    /**
     * <p>
     * Returns the sum of squared residuals.
     * </p>
     * <p>
     * See {@code calculateResidualSumOfSquares()} in {@code OLSRegression} class
     * for more details.
     * </p>
     *
     * @return residual sum of squares
     */
    public double getResidualSumOfSquares() {
        return residualSumOfSquares;
    }

    /**
     * <p>
     * Returns the R-Squared statistic.
     * </p>
     * <p>
     * See {@code calculateRSquared()} in {@code OLSRegression} class for more
     * details.
     * </p>
     *
     * @return R-squared statistic
     */
    public double getRSquared() {
        return regRSquared;
    }

    /**
     * <p>
     * Returns the adjusted R-Squared statistic.
     * </p>
     * <p>
     * See {@code calculateAdjustedRSquared()} in {@code OLSRegression} class for
     * more details.
     * </p>
     *
     * @return Adjusted R-squared statistic
     */
    public double getAdjustedRSquared() {
        return adjustedRSquared;
    }

}
