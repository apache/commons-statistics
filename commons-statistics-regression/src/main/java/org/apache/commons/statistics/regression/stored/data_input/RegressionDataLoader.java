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
package org.apache.commons.statistics.regression.stored.data_input;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.ejml.data.DMatrixRMaj;

public final class RegressionDataLoader {

    /** Contains Y vector and X matrix data and a hasIntercept boolean. */
    private RegressionRawData inputData;

    /**
     * <p>
     * Creates new RegressionDataLoader object and loads new Y and X data as double
     * arrays.
     * </p>
     *
     * <p>
     * boolean hasIntercept which should be set to {@code true} if sample data
     * already contains intercept (column of 1's) will be set to {@code false} by
     * default inside constructor of {@code RegressionRawData} class.
     * </p>
     *
     * @param y data vector double 1D array
     * @param x data matrix double 2D array
     */
    public RegressionDataLoader(double[] y, double[][] x) {
        this.inputNewSampleData(y, x);
    }

    /**
     * <p>
     * Creates new RegressionDataLoader object and loads new Y and X data as double
     * arrays and hasIntercept which should be true if sample data already contains
     * intercept; column of 1's.
     * </p>
     *
     * @param y            data vector double 1D array
     * @param x            data matrix double 2D array
     * @param hasIntercept true if intercept is included in input data
     */
    public RegressionDataLoader(double[] y, double[][] x, boolean hasIntercept) {
        this.inputNewSampleData(y, x, hasIntercept);
    }

    /**
     * Creates empty RegressionDataLoader object with data starting as {@code null}
     * and hasIntercept as {@code false}.
     */
    public RegressionDataLoader() {
        inputData = new RegressionRawData();
    }

    /**
     * <p>
     * Loads new Y and X data as double arrays.
     * </p>
     *
     * <p>
     * boolean hasIntercept which should be set to {@code true} if sample data
     * already contains intercept (column of 1's) will be set to {@code false} by
     * default inside constructor of {@code RegressionRawData} class.
     * </p>
     *
     * @param y data vector double 1D array
     * @param x data matrix double 2D array
     */
    public void inputNewSampleData(double[] y, double[][] x) {
        validateSampleData(y, x);
        if (inputData == null) {
            inputData = new RegressionRawData();
        }
        inputData.setHasIntercept(false);
        inputNewYSampleData(y);
        inputNewXSampleData(x);
    }

    /**
     * <p>
     * Loads new Y and X data as double arrays and hasIntercept which should be true
     * if sample data already contains intercept; column of 1's.
     * </p>
     *
     * @param y            data vector double 1D array
     * @param x            data matrix double 2D array
     * @param hasIntercept boolean true if intercept is included in input data
     */
    public void inputNewSampleData(double[] y, double[][] x, boolean hasIntercept) {
        validateSampleData(y, x);
        if (inputData == null) {
            inputData = new RegressionRawData();
        }
        inputData.setHasIntercept(hasIntercept);
        inputNewYSampleData(y);
        inputNewXSampleData(x);
    }

    /**
     * <p>
     * Loads model x and y sample data from a flat input array, overriding any
     * previous sample. Please see {@code newSingleArraySampleData} for more
     * details.
     * </p>
     *
     * @param data  input double data array
     * @param nobs  int number of observations (rows)
     * @param nvars int number of independent variables (columns, not counting y)
     */
    public void inputNewSampleData(double[] data, int nobs, int nvars) {
//        inputData.setHasIntercept(false);
        inputSingleArraySampleData(data, nobs, nvars);
    }

    /**
     * <p>
     * Loads model x and y sample data from a flat input array, overriding any
     * previous sample. Please see {@code newSingleArraySampleData} for more
     * details.
     * </p>
     *
     * <p>
     * Includes option to set hasIntercept in this single method.
     * </p>
     *
     * @param data         input double data array
     * @param nobs         int number of observations (rows)
     * @param nvars        int number of independent variables (columns, not
     *                     counting y)
     * @param hasIntercept boolean true if sample data already has intercept; column
     *                     of 1's
     */
    public void inputNewSampleData(double[] data, int nobs, int nvars, boolean hasIntercept) {
        inputData.setHasIntercept(hasIntercept);
        inputSingleArraySampleData(data, nobs, nvars);
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
     * @param data  input double data array
     * @param nobs  int number of observations (rows)
     * @param nvars int number of independent variables (columns, not counting y)
     * @throws IllegalArgumentException if the data array is null
     * @throws IllegalArgumentException if the length of the data array is not equal
     *                                  to <code>nobs * (nvars + 1)</code>
     * @throws IllegalArgumentException if <code>nobs</code> is less than
     *                                  <code>nvars + 1</code>
     */
    private void inputSingleArraySampleData(double[] data, int nobs, int nvars) {
        if (data == null) {
            throw new IllegalArgumentException("Null data argument.");
        }
        if (data.length != nobs * (nvars + 1)) {
            throw new IllegalArgumentException("Dimension mismatch: data length [" + data.length +
                                               "] is not equal to nobs * (nvars + 1) [" + nobs * (nvars + 1) + "]");
        }
        if (nobs <= nvars) {
            throw new IllegalArgumentException("Not enough data for number of predictors: nobs <= nvars");
        }

        final int cols = inputData.getHasIntercept() ? nvars : nvars + 1;
        double[] y = new double[nobs];
        double[][] x = new double[nobs][cols];
        int pointer = 0;

        for (int i = 0; i < nobs; i++) {
            y[i] = data[pointer++];
            if (!inputData.getHasIntercept()) {
                x[i][0] = 1.0d;
            }
            for (int j = inputData.getHasIntercept() ? 0 : 1; j < cols; j++) {
                x[i][j] = data[pointer++];
            }
        }
        inputData.setYData(createYmatrix(y));
        inputData.setXData(createXmatrix(x));
    }

    /**
     * <p>
     * Sets Y data by creating a new StatisticsMatrix object from given array (see
     * {@code newYmatrix}).
     * </p>
     *
     * <p>
     * Note that previous X sample data must be compatible or {@code null} if it's
     * initial input.
     * </p>
     *
     * @param y 1D double array
     */
    public void inputNewYSampleData(double[] y) {
        if (y == null) {
            throw new IllegalArgumentException("Null y argument.");
        }
        if (inputData.getX() != null) {
            validateSampleData(y, inputData.getX().toArray2D());
        }
        inputData.setYData(createYmatrix(y));
    }

    /**
     * <p>
     * Sets X data by creating a new StatisticsMatrix object from given array (see
     * {@code newXmatrix}).
     * </p>
     *
     * <p>
     * If {@code hasIntercept} is false then method will add the intercept column of
     * 1's. Method will also validate the data (see {@code validateSampleData})
     * </p>
     *
     * <p>
     * Note that previous Y sample data must be compatible or {@code null} if it's
     * initial input.
     * </p>
     *
     * @param x 2D double array
     */
    public void inputNewXSampleData(double[][] x) {
        if (x == null) {
            throw new IllegalArgumentException("Null x argument.");
        }

        if (inputData.getHasIntercept()) {
            inputData.setXData(createXmatrix(x));

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
            inputData.setXData(createXmatrix(xAug));
        }

        if (inputData.getY() != null) {
            validateSampleData(inputData.getY().toArray1D(), inputData.getX().toArray2D());
        }
    }

    /**
     * Sets Y and X StatisticsMatrix objects as null and hasIntercept to it's
     * default: {@code false}.
     */
    public void clearData() {
        inputData.setHasIntercept(false);
        inputData.setYData(null);
        inputData.setXData(null);
    }

    /**
     * Validates sample data. Checks that:
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

        int numPredictors = x[0].length;
        int numObs = x.length;

        if (numPredictors > numObs) {
            throw new IllegalArgumentException(
                "Not enough data for number of predictors." + numPredictors + " > " + numObs);
        }

    }

    /**
     * Creates a new StatisticsMatrix object from 1D double array.
     *
     * @param y data as 1D array
     * @return new StatisticsMatrix object containing the Y vector.
     */
    private static StatisticsMatrix createYmatrix(double[] y) {
        return new StatisticsMatrix(new DMatrixRMaj(y));
    }

    /**
     * Creates a new StatisticsMatrix object from 2D double array.
     *
     * @param x data as 2D array
     * @return new StatisticsMatrix object containing the X matrix.
     */
    private static StatisticsMatrix createXmatrix(double[][] x) {
        return new StatisticsMatrix(new DMatrixRMaj(x));
    }

    /**
     * Sets whether the input data has an intercept included; false if column of 1's
     * should be created when loading X data.
     *
     * @param hasIntercept boolean true if sample data already contains intercept.
     */
    public void setHasIntercept(boolean hasIntercept) {
        inputData.setHasIntercept(hasIntercept);
    }

    /**
     * Returns the inputData object as a restricted interface.
     *
     * @return inputData loaded as StatisticsMatrix objects
     */
    public RegressionData getInputData() {
        return inputData;
    }

}
