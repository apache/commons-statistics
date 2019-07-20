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

import org.apache.commons.math4.stat.StatUtils;
import org.apache.commons.math4.stat.descriptive.moment.SecondMoment;
import org.apache.commons.math4.stat.regression.RegressionResults;
import org.apache.commons.statistics.regression.stored.AbstractRegression;
import org.apache.commons.statistics.regression.stored.Regression;
import org.apache.commons.statistics.regression.stored.data_input.RegressionData;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.ejml.LinearSolverSafe;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.decomposition.qr.QRDecomposition_DDRB_to_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.decomposition.QRDecomposition;
import org.ejml.interfaces.linsol.LinearSolverDense;

public class OLSRegression extends AbstractRegression implements Regression {

//    /** Container for OLS estimator functionalities. */
//    private OLSEstimators betas;
//
//    /** Container for OLS residual functionalities. */
//    private OLSResiduals residuals;

    /**
     * Constructs the OLSRegression user-interface class.
     *
     * @param loader contains the inputData
     */
    public OLSRegression(RegressionData data) {
        this.inputData = data;
//        this.betas = new OLSEstimators(inputData);
//        this.residuals = new OLSResiduals(inputData, betas.calculateBeta());
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
        StatisticsMatrix b = calculateBeta();
        System.out.println("PRINTED");
        b.print();
        return b.toArray1D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateRegressionParametersStandardErrors() {
        double[][] betaVariance = estimateRegressionParametersVariance();
        double sigma = calculateErrorVariance();
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
        return calculateBetaVariance().toArray2D();
    }

    /**
     * Estimates the standard error of the regression.
     *
     * @return regression standard error
     * @since 2.2
     */
    public double estimateRegressionStandardError() {
        return Math.sqrt(calculateErrorVariance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateResiduals() {
        return calculateResiduals().toArray1D();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    /**
     * <p>
     * Compute the "hat" matrix.
     * </p>
     * <p>
     * The hat matrix is defined in terms of the design matrix X by
     * X(X<sup>T</sup>X)<sup>-1</sup>X<sup>T</sup>
     * </p>
     * <p>
     * The implementation here uses the QR decomposition to compute the hat matrix
     * as Q I<sub>p</sub>Q<sup>T</sup> where I<sub>p</sub> is the p-dimensional
     * identity matrix augmented by 0's. This computational formula is from "The Hat
     * Matrix in Regression and ANOVA", David C. Hoaglin and Roy E. Welsch, <i>The
     * American Statistician</i>, Vol. 32, No. 1 (Feb., 1978), pp. 17-22.
     * </p>
     * <p>
     * Data for the model must have been successfully loaded using one of the
     * {@code newSampleData} methods before invoking this method; otherwise a
     * {@code NullPointerException} will be thrown.
     * </p>
     *
     * @return the hat matrix
     * @throws NullPointerException unless method {@code newSampleData} has been
     *                              called beforehand.
     */
    public StatisticsMatrix calculateHat() {

        QRDecomposition<DMatrixRMaj> qr = new QRDecomposition_DDRB_to_DDRM();
        qr.decompose(getX().getDDRM());

        StatisticsMatrix qrQ = new StatisticsMatrix(qr.getQ(null, false));
        StatisticsMatrix qrR = new StatisticsMatrix(qr.getR(null, false));
        // Create augmented identity matrix
        final int p = qrR.numCols();
        final int n = qrQ.numCols();
        // No try-catch or advertised NotStrictlyPositiveException - NPE above if n < 3
        double[][] augIData = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j && i < p) {
                    augIData[i][j] = 1d;
                } else {
                    augIData[i][j] = 0d;
                }
            }
        }

        StatisticsMatrix augI = new StatisticsMatrix(new DMatrixRMaj(augIData));

        // Compute and return Hat matrix
        // No DME advertised - args valid if we get here
        return qrQ.mult(augI).mult(qrQ.transpose());
    }

    /**
     * <p>
     * Returns the sum of squared deviations of Y from its mean.
     * </p>
     *
     * <p>
     * If the model has no intercept term, <code>0</code> is used for the mean of Y
     * - i.e., what is returned is the sum of the squared Y values.
     * </p>
     *
     * <p>
     * The value returned by this method is the SSTO value used in the
     * {@link #calculateRSquared() R-squared} computation.
     * </p>
     *
     * @return SSTO - the total sum of squares
     * @throws NullPointerException if the sample has not been set
     * @see #isNoIntercept()
     * @since 2.2
     */
    public double calculateTotalSumOfSquares() {// 17.5 false
        if (getHasIntercept()) {
            return StatUtils.sumSq(getY().toArray1D());
        } else {
            return new SecondMoment().evaluate(getY().toArray1D());
        }
    }

    /**
     * Returns the sum of squared residuals.
     *
     * @return residual sum of squares
     * @since 2.2
     * @throws NullPointerException if the data for the model have not been loaded
     */
    public double calculateResidualSumOfSquares() {// 1.7670484276950664E-28
        final StatisticsMatrix residuals = calculateResiduals();
        // No advertised DME, args are valid
        return residuals.dot(residuals);
    }

    /**
     * Returns the R-Squared statistic, defined by the formula
     * <div style="white-space: pre"><code>
     * R<sup>2</sup> = 1 - SSR / SSTO
     * </code></div> where SSR is the {@link #calculateResidualSumOfSquares() sum of
     * squared residuals} and SSTO is the {@link #calculateTotalSumOfSquares() total
     * sum of squares}
     *
     * <p>
     * If there is no variance in y, i.e., SSTO = 0, NaN is returned.
     * </p>
     *
     * @return R-square statistic
     * @throws NullPointerException if the sample has not been set
     * @since 2.2
     */
    public double calculateRSquared() {
        return 1 - calculateResidualSumOfSquares() / calculateTotalSumOfSquares();
    }

    /**
     * <p>
     * Returns the adjusted R-squared statistic, defined by the formula
     * <div style="white-space: pre"><code>
     * R<sup>2</sup><sub>adj</sub> = 1 - [SSR (n - 1)] / [SSTO (n - p)]
     * </code></div> where SSR is the {@link #calculateResidualSumOfSquares() sum of
     * squared residuals}, SSTO is the {@link #calculateTotalSumOfSquares() total
     * sum of squares}, n is the number of observations and p is the number of
     * parameters estimated (including the intercept).
     *
     * <p>
     * If the regression is estimated without an intercept term, what is returned is
     *
     * <pre>
     * <code> 1 - (1 - {@link #calculateRSquared()}) * (n / (n - p)) </code>
     * </pre>
     *
     * <p>
     * If there is no variance in y, i.e., SSTO = 0, NaN is returned.
     * </p>
     *
     * @return adjusted R-Squared statistic
     * @throws NullPointerException if the sample has not been set
     * @see #isNoIntercept()
     * @since 2.2
     */
    public double calculateAdjustedRSquared() {
        final double n = getX().numRows();
        if (getHasIntercept()) {
            return 1 - (1 - calculateRSquared()) * (n / (n - getX().numCols()));
        } else {
            return 1
                - (calculateResidualSumOfSquares() * (n - 1)) / (calculateTotalSumOfSquares() * (n - getX().numCols()));
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Calculates the regression coefficients using OLS.
     *
     * <p>
     * Data for the model must have been successfully loaded using one of the
     * {@code newSampleData} methods before invoking this method; otherwise a
     * {@code NullPointerException} will be thrown.
     * </p>
     *
     * @return beta
     * @throws NullPointerException if the data for the model have not been loaded
     */
    @Override
    protected StatisticsMatrix calculateBeta() {
        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(getX().numRows(),
            getX().numCols());
        solver = new LinearSolverSafe<DMatrixRMaj>(solver);

        StatisticsMatrix betas = new StatisticsMatrix(new DMatrixRMaj(getX().numCols()));

        solver.setA(getX().getDDRM().copy());
        solver.solve(getY().getDDRM(), betas.getDDRM());

        return betas;
    }

    /**
     * <p>
     * Calculates the variance-covariance matrix of the regression parameters.
     * </p>
     * <p>
     * Var(b) = (X<sup>T</sup>X)<sup>-1</sup>
     * </p>
     * <p>
     * Uses QR decomposition to reduce (X<sup>T</sup>X)<sup>-1</sup> to
     * (R<sup>T</sup>R)<sup>-1</sup>, with only the top p rows of R included, where
     * p = the length of the beta vector.
     * </p>
     *
     * <p>
     * Data for the model must have been successfully loaded using one of the
     * {@code newSampleData} methods before invoking this method; otherwise a
     * {@code NullPointerException} will be thrown.
     * </p>
     *
     * @return The beta variance-covariance matrix
     * @throws NullPointerException if the data for the model have not been loaded
     */
    @Override
    protected StatisticsMatrix calculateBetaVariance() {
        QRDecomposition<DMatrixRMaj> qr = new QRDecomposition_DDRB_to_DDRM();
        qr.decompose(getX().getDDRM().copy());

        int p = getX().numCols();
        StatisticsMatrix qrR = new StatisticsMatrix(qr.getR(null, false)).extractMatrix(0, p, 0, p);
        StatisticsMatrix invR = qrR.invert();

        return invR.mult(invR.transpose());
    }

    public RegressionResults regress() {
        return null;
    }

}
