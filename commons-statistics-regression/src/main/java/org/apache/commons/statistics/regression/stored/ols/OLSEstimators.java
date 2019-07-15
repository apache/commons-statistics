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

import org.apache.commons.statistics.regression.stored.RegressionData;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.decomposition.qr.QRDecomposition_DDRB_to_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.decomposition.QRDecomposition;
import org.ejml.interfaces.linsol.LinearSolver;

public class OLSEstimators extends org.apache.commons.statistics.regression.stored.parent.AbstractEstimators {

    /**
     * Constructs the OLSEstimator to pass regression data.
     *
     * @param inputData the regression input data; X matrix and Y vector etc
     */
    protected OLSEstimators(RegressionData inputData) {
        this.inputData = inputData;
    }

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

        StatisticsMatrix xMatrix = getX().copy();
        LinearSolver<DMatrixRMaj, DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(xMatrix.numRows(),
            xMatrix.numCols());

        solver.setA(xMatrix.getDDRM());
        solver.solve(getY().getDDRM(), xMatrix.getDDRM());

        // extracts the betas out of solved xMatrix, which contained larger array
        double[] rawBetas = new double[getX().numCols()];
        for (int i = 0; i < getX().numCols(); i++) {
            rawBetas[i] = xMatrix.getDDRM().data[i];
        }

        StatisticsMatrix betas = new StatisticsMatrix(new DMatrixRMaj(rawBetas));
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
        qr.decompose(getX().getDDRM());

        StatisticsMatrix qrR = new StatisticsMatrix(qr.getR(null, false));
        StatisticsMatrix invR = qrR.invert();
        return invR.mult(invR.transpose());
    }

}
