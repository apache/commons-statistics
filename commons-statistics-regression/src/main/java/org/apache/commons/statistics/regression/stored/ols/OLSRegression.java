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

import org.apache.commons.statistics.regression.stored.RegressionDataLoader;
import org.apache.commons.statistics.regression.stored.parent.AbstractRegression;
import org.apache.commons.statistics.regression.stored.parent.Regression;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public class OLSRegression extends AbstractRegression implements Regression {

    /** Container for OLS estimator functionalities. */
    private OLSEstimators betas;

    /** Container for OLS residual functionalities. */
    private OLSResiduals residuals;

    /**
     * Constructs the OLSRegression user-interface class.
     *
     * @param loader contains the inputData
     */
    public OLSRegression(RegressionDataLoader loader) {

        this.betas = new OLSEstimators(loader.getInputData());
        this.residuals = new OLSResiduals(loader.getInputData(), betas.calculateBeta());
    }

    /**
     * Please see details inside {@code OLSResiduals} class.
     *
     * @return adjusted R squared
     */
    public double calculateAdjustedRSquared() {
        return residuals.calculateAdjustedRSquared();
    }

    /**
     * Please see details inside {@code OLSResiduals} class.
     *
     * @return hat matrix
     */
    public StatisticsMatrix calculateHat() {
        return residuals.calculateHat();
    }

    /**
     * Please see details inside {@code OLSResiduals} class.
     *
     * @return Residual Sum of Squares
     */
    public double calculateResidualSumOfSquares() {
        return residuals.calculateResidualSumOfSquares();
    }

    /**
     * Please see details inside {@code OLSResiduals} class.
     *
     * @return R squared
     */
    public double calculateRSquared() {
        return residuals.calculateRSquared();
    }

    /**
     * Please see details inside {@code OLSResiduals} class.
     *
     * @return Total Sum of Squares
     */
    public double calculateTotalSumOfSquares() {
        return residuals.calculateTotalSumOfSquares();
    }

    /**
     * Estimates the variance of the error.
     *
     * @return estimate of the error variance
     * @since 2.2
     */
    public double estimateErrorVariance() {
        return residuals.calculateErrorVariance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double estimateRegressandVariance() {
        return calculateYVariance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateRegressionParameters() {
        StatisticsMatrix b = betas.calculateBeta();
        return b.toArray1D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateRegressionParametersStandardErrors() {
        double[][] betaVariance = estimateRegressionParametersVariance();
        double sigma = residuals.calculateErrorVariance();
        int length = betaVariance[0].length;
        double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            result[i] = Math.sqrt(sigma * betaVariance[i][i]);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[][] estimateRegressionParametersVariance() {
        return betas.calculateBetaVariance().toArray2D();
    }

    /**
     * Estimates the standard error of the regression.
     *
     * @return regression standard error
     * @since 2.2
     */
    public double estimateRegressionStandardError() {
        return Math.sqrt(estimateErrorVariance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateResiduals() {
        return residuals.calculateResiduals().toArray1D();
    }

}
