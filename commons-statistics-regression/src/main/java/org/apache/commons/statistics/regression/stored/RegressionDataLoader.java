package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.ejml.data.DMatrixRMaj;

public class RegressionDataLoader {

    private RegressionRawData inputData;

    public RegressionDataLoader(double[] y, double[][] x) {
        this.newSampleData(y, x);
    }

    public RegressionDataLoader(double[] y, double[][] x, boolean hasIntercept) {
        this.newSampleData(y, x, hasIntercept);
    }

    public RegressionDataLoader() {
        inputData = new RegressionRawData();
    }

    public void newSampleData(double[] y, double[][] x) {
        validateSampleData(y, x);
        if (inputData == null)
            inputData = new RegressionRawData();

//        else {
        newYData(y);
        newXData(x);
//        }
    }

    public void newSampleData(double[] y, double[][] x, boolean hasIntercept) {
        validateSampleData(y, x);
        if (inputData == null)
            inputData = new RegressionRawData();

//        else {
        inputData.setHasIntercept(hasIntercept);
        newYData(y);
        newXData(x);

//        }
    }

    public void newSampleData(double[] data, int nobs, int nvars) {
        newSingleArraySampleData(data, nobs, nvars);
    }

    public void newSampleData(double[] data, int nobs, int nvars, boolean hasIntercept) {
        inputData.setHasIntercept(hasIntercept);
        newSingleArraySampleData(data, nobs, nvars);
    }

    /**
     * <p>
     * Loads model x and y sample data from a flat input array, overriding any
     * previous sample.
     * </p>
     * <p>
     * Assumes that rows are concatenated with y values first in each row. For
     * example, an input <code>data</code> array containing the sequence of values
     * (1, 2, 3, 4, 5, 6, 7, 8, 9) with <code>nobs = 3</code> and
     * <code>nvars = 2</code> creates a regression dataset with two independent
     * variables, as below:
     * 
     * <pre>
     *   y   x[0]  x[1]
     *   --------------
     *   1     2     3
     *   4     5     6
     *   7     8     9
     * </pre>
     *
     * <p>
     * Note that there is no need to add an initial unitary column (column of 1's)
     * when specifying a model including an intercept term. If
     * {@link #isNoIntercept()} is <code>true</code>, the X matrix will be created
     * without an initial column of "1"s; otherwise this column will be added.
     * </p>
     * <p>
     * Throws IllegalArgumentException if any of the following preconditions fail:
     * <ul>
     * <li><code>data</code> cannot be null</li>
     * <li><code>data.length = nobs * (nvars + 1)</code></li>
     * <li>{@code nobs > nvars}</li>
     * </ul>
     *
     * @param data  input data array
     * @param nobs  number of observations (rows)
     * @param nvars number of independent variables (columns, not counting y)
     * @throws IllegalArgumentException if the data array is null
     * @throws IllegalArgumentException if the length of the data array is not equal
     *                                  to <code>nobs * (nvars + 1)</code>
     * @throws IllegalArgumentException if <code>nobs</code> is less than
     *                                  <code>nvars + 1</code>
     */
    private void newSingleArraySampleData(double[] data, int nobs, int nvars) {
        if (data == null) {
            throw new IllegalArgumentException("Null argument(s).");
        }
        if (data.length != nobs * (nvars + 1)) {
            throw new IllegalArgumentException("Dimension mismatch: data length [" + data.length
                    + "] is not equal to nobs * (nvars + 1) [" + nobs * (nvars + 1) + "]");
        }
        if (nobs <= nvars) {
            throw new IllegalArgumentException("Not enough data for number of predictors.");
        }
        double[] y = new double[nobs];
        final int cols = !inputData.getHasIntercept() ? nvars : nvars + 1;
        double[][] x = new double[nobs][cols];
        int pointer = 0;
        for (int i = 0; i < nobs; i++) {
            y[i] = data[pointer++];
            if (inputData.getHasIntercept()) {
                x[i][0] = 1.0d;
            }
            for (int j = !inputData.getHasIntercept() ? 0 : 1; j < cols; j++) {
                x[i][j] = data[pointer++];
            }
        }
        inputData.setYData(newYmatrix(y));
        inputData.setXData(newXmatrix(x));
    }

    public void newYData(double[] y) {
        inputData.setYData(newYmatrix(y));

        if (inputData.getXData() != null)
            validateSampleData(y, inputData.getXData().toArray2D());
    }

    public void newXData(double[][] x) {

        if (!inputData.getHasIntercept()) {
            inputData.setXData(newXmatrix(x));

        } else { // Augment design matrix with initial unitary column
            final int nVars = x[0].length;
            final double[][] xAug = new double[x.length][nVars + 1];
            for (int i = 0; i < x.length; i++) {
                if (x[i].length != nVars) {
                    throw new IllegalArgumentException(x[i].length + "!=" + nVars);
                }
                xAug[i][0] = 1.0d;
                System.arraycopy(x[i], 0, xAug[i], 1, nVars);
            }
            inputData.setXData(newXmatrix(xAug));
        }

        if (inputData.getYData() != null)
            validateSampleData(inputData.getYData().toArray1D(), x);
    }

    public void clearData() {
        inputData.setYData(null);
        inputData.setXData(null);
    }

    /**
     * Validates sample data. Checks that
     * <ul>
     * <li>Neither x nor y is null or empty;</li>
     * <li>The length (i.e. number of rows) of x equals the length of y</li>
     * <li>x has at least one more row than it has columns (i.e. there is sufficient
     * data to estimate regression coefficients for each of the columns in x plus an
     * intercept.</li>
     * </ul>
     *
     * @param x the [n,k] array representing the x data
     * @param y the [n,1] array representing the y data
     * @throws IllegalArgumentException if {@code x} or {@code y} is null
     * @throws IllegalArgumentException if {@code x} and {@code y} do not have the
     *                                  same length
     * @throws IllegalArgumentException if {@code x} or {@code y} are zero-length
     * @throws IllegalArgumentException if the number of rows of {@code x} is not
     *                                  larger than the number of columns + 1
     */
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

    private static StatisticsMatrix newYmatrix(double[] y) {
        return (new StatisticsMatrix(new DMatrixRMaj(y)));
    }

    private static StatisticsMatrix newXmatrix(double[][] x) {
        return (new StatisticsMatrix(new DMatrixRMaj(x)));
    }

    public void setHasIntercept(boolean hasIntercept) {
        inputData.setHasIntercept(hasIntercept);
    }

    public RegressionData getInputData() {
        return inputData;
    }

}
