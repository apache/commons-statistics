package org.apache.commons.statistics.regression.stored.ols;

import org.apache.commons.statistics.regression.stored.RegressionData;
import org.apache.commons.statistics.regression.stored.RegressionDataLoader;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.decomposition.lu.LUDecompositionAlt_DDRM;
import org.ejml.dense.row.decomposition.qr.QRDecomposition_DDRB_to_DDRM;
import org.ejml.interfaces.decomposition.QRDecomposition;

public class OLSRegression extends org.apache.commons.statistics.regression.stored.AbstractRegression {

    private OLSEstimators betas;
    private OLSResiduals residuals;

    public RegressionData inputData;

    public OLSRegression(RegressionDataLoader loader) {
        this.inputData = loader.getInputData();
        this.yVector = inputData.getYData();
        this.xMatrix = inputData.getXData();
    }

    @Override
    protected StatisticsMatrix calculateBeta() {
        // TODO Auto-generated method stub
        return null;
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
     * @throws org.apache.commons.math4.linear.SingularMatrixException if the design
     *                                                                 matrix is
     *                                                                 singular
     * @throws NullPointerException                                    if the data
     *                                                                 for the model
     *                                                                 have not been
     *                                                                 loaded
     */
    @Override
    protected StatisticsMatrix calculateBetaVariance() {
        QRDecomposition<DMatrixRMaj> qr = new QRDecomposition_DDRB_to_DDRM();
        qr.decompose(xMatrix.getDDRM());
        StatisticsMatrix R = new StatisticsMatrix(qr.getR(null, false));
        int p = xMatrix.getDDRM().getNumCols();
        StatisticsMatrix Raug = R.extractMatrix(0, p - 1, 0, p - 1);
        LUDecompositionAlt_DDRM lu = new LUDecompositionAlt_DDRM();
        lu.decompose(Raug.getDDRM());
        StatisticsMatrix Rinv = new StatisticsMatrix(lu.getLU()).invert();
        return Rinv.mult(Rinv.transpose());
    }

}
