package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.array.ArrayUtils;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.ejml.data.DMatrixRMaj;

public class RegressionDataLoader {

    private RegressionRawData inputData;

    public RegressionDataLoader(double[] y, double[][] x) {
        validateSampleData(y, x);
        inputData = new RegressionRawData(newY(y), newX(x), false);
    }

    public RegressionDataLoader() {
        inputData = new RegressionRawData();
    }

    public void newSampleData(double[] y, double[][] x) {
        validateSampleData(y, x);
        if (inputData == null)
            inputData = new RegressionRawData(newY(y), newX(x), false);

        else {
            inputData.setYData(newY(y));
            inputData.setXData(newX(x));
        }
    }

    public void newYData(double[] y) {
        if (inputData != null) {
            if (inputData.getXData() != null) {

                validateSampleData(y, ArrayUtils.matrixToArray2D(inputData.getXData()));
                inputData.setYData(newY(y));
            } else
                throw new NullPointerException(
                        "X matrix is null, please use newSampleData(y, x) to input initial data.");
        } else
            throw new NullPointerException(); // not reachable?
    }

    public void newXData(double[][] x) {
        if (inputData != null) {
            if (inputData.getYData() != null) {

                validateSampleData(ArrayUtils.matrixToArray1D(inputData.getYData()), x);
                inputData.setXData(newX(x));
            } else
                throw new NullPointerException(
                        "Y matrix is null, please use newSampleData(y, x) to input initial data.");
        } else
            throw new NullPointerException(); // not reachable?
    }

    protected void validateSampleData(double[] y, double[][] x) throws IllegalArgumentException {
        if ((x == null) || (y == null)) {
            throw new IllegalArgumentException("Null argument(s).");
        }
        if (x.length != y.length) {
            throw new IllegalArgumentException("Dimension mismatch: " + y.length + " != " + x.length);
        }
        if (x.length == 0) { // Must be no y data either
            throw new IllegalArgumentException("Arrays have length 0.");
        }
        if (x[0].length + 1 > x.length) {
            throw new IllegalArgumentException("Not enough data for number of predictors.");
        }
    }

    private static StatisticsMatrix newY(double[] y) {
        return (new StatisticsMatrix(new DMatrixRMaj(y)));
    }

    private static StatisticsMatrix newX(double[][] x) {
        return (new StatisticsMatrix(new DMatrixRMaj(x)));
    }

    public RegressionData getInputData() {
        return inputData;
    }

}
