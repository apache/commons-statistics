package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public abstract class AbstractRegression implements Regression {

    public RegressionData inputData;
    public StatisticsMatrix xMatrix;
    public StatisticsMatrix yVector;

    /**
     * Validates that the x data and covariance matrix have the same number of rows
     * and that the covariance matrix is square.
     *
     * @param x          the [n,k] array representing the x sample
     * @param covariance the [n,n] array representing the covariance matrix
     * @throws DimensionMismatchException if the number of rows in x is not equal to
     *                                    the number of rows in covariance
     * @throws NonSquareMatrixException   if the covariance matrix is not square
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
     * {@inheritDoc}
     */
    @Override
    public double[] estimateRegressionParameters() {
        StatisticsMatrix b = calculateBeta();
        return b.toArray1D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateResiduals() {
        StatisticsMatrix b = calculateBeta();
        StatisticsMatrix e = yVector.minus(xMatrix.mult(b)); // operate is for vec x vec in CM
        return e.toArray1D();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[][] estimateRegressionParametersVariance() {
        return calculateBetaVariance().toArray2D();
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
    public double estimateRegressandVariance() {
        return calculateYVariance();
    }

    /**
     * Estimates the variance of the error.
     *
     * @return estimate of the error variance
     * @since 2.2
     */
    public double estimateErrorVariance() {
        return calculateErrorVariance();

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
    protected double calculateYVariance() {
//        return new Variance().evaluate(yVector.toArray());
        return 0;
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
    protected double calculateErrorVariance() {
        StatisticsMatrix residuals = calculateResiduals();
        return residuals.dot(residuals) / (xMatrix.getDDRM().getNumRows() - xMatrix.getDDRM().getNumCols());
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
        StatisticsMatrix b = calculateBeta();
        return yVector.minus(xMatrix.mult(b)); // operate is for vec x vec in CM
    }
}
