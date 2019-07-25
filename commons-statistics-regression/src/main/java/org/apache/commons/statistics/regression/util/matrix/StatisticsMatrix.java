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
package org.apache.commons.statistics.regression.util.matrix;

import org.ejml.data.DMatrixRBlock;

/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRBlock;
import org.ejml.data.FMatrixRMaj;
import org.ejml.data.Matrix;
import org.ejml.data.MatrixType;
import org.ejml.ops.ConvertDMatrixStruct;
import org.ejml.ops.ConvertFMatrixStruct;
import org.ejml.simple.SimpleBase;

/**
 * Example of how to extend "SimpleMatrix" and add your own functionality. In
 * this case two basic statistic operations are added. Since SimpleBase is
 * extended and StatisticsMatrix is specified as the generics type, all
 * "SimpleMatrix" operations return a matrix of type StatisticsMatrix, ensuring
 * strong typing.
 *
 * author Peter Abeles
 * -------------------------------------------------------------------------------------------
 * Modifications for Apache Commons Statistics Regressiom library by: Ben Nguyen
 * Under development for matrix calculations API usage.
 * -------------------------------------------------------------------------------------------
 */
public class StatisticsMatrix extends SimpleBase<StatisticsMatrix> {

    /** Auto-generated serialVersionUID. */
    private static final long serialVersionUID = -82801259856161557L;

    /**
     * Constructor for internal library use only. Nothing is configured and is
     * intended for serialization.
     */
    private StatisticsMatrix() {
    }

    /**
     * Constructs a new empty matrix with a set dimension.
     *
     * @param numRows number of rows
     * @param numCols number of columns
     */
    public StatisticsMatrix(int numRows, int numCols) {
        super(numRows, numCols);
    }

    /**
     * Constructs a new StatisticsMatrix object wrapper around a Matrix interface.
     *
     * @param orig the origin matrix to be converted
     */
    public StatisticsMatrix(Matrix orig) {
        Matrix mat;
        if (orig instanceof DMatrixRBlock) {
            final DMatrixRMaj a = new DMatrixRMaj(orig.getNumRows(), orig.getNumCols());
            ConvertDMatrixStruct.convert((DMatrixRBlock) orig, a);
            mat = a;
        } else if (orig instanceof FMatrixRBlock) {
            final FMatrixRMaj a = new FMatrixRMaj(orig.getNumRows(), orig.getNumCols());
            ConvertFMatrixStruct.convert((FMatrixRBlock) orig, a);
            mat = a;
        } else {
            mat = orig.copy();
        }
        setMatrix(mat);
    }

    /**
     * Returns a matrix of StatisticsMatrix type so that SimpleMatrix functions
     * create matrices of the correct type.
     */
    @Override
    protected StatisticsMatrix createMatrix(int numRows, int numCols, MatrixType type) {
        // changed by Ben Nguyen to add MatrixType type param to satisfy abstract method
        return new StatisticsMatrix(numRows, numCols);
    }

    /**
     * Wraps a StatisticsMatrix around 'm'. Does NOT create a copy of 'm' but saves
     * a reference to it.
     *
     * @param m origin matrix to wrap
     * @return new pointer
     */
    public static StatisticsMatrix wrap(DMatrixRMaj m) {
        final StatisticsMatrix ret = new StatisticsMatrix();
        ret.setMatrix(m);
        return ret;
    }

    /**
     * Sets a new matrix by creating a new pointer object.
     */
    @Override
    protected StatisticsMatrix wrapMatrix(Matrix m) {
        return new StatisticsMatrix(m);
    }

    /**
     * Creates an identity matrix given it's square width.
     *
     * @param width size of square matrix; numCols and numRows
     * @return the width sized identity matrix
     */
    @SuppressWarnings("unchecked")
    public static StatisticsMatrix identity(int width) {
        final StatisticsMatrix ret = new StatisticsMatrix(width, width);
        ret.ops.setIdentity(ret.mat);
        return ret;
    }

    /**
     * Computes the mean or average of all the elements.
     *
     * @return mean
     */
    public double mean() {
        double total = 0;

        final int n = getNumElements();
        for (int i = 0; i < n; i++) {
            total += get(i);
        }
        return total / n;
    }

    /**
     * Computes the unbiased standard deviation of all the elements.
     *
     * @return standard deviation
     * @throws IllegalArgumentException if there is less than 2 elements.
     */
    public double stdev() {
        final int n = getNumElements();
        if (n <= 1) {
            throw new IllegalArgumentException("There must be more than one element to compute stdev");
        }

        final double m = mean();
        double total = 0;
        for (int i = 0; i < n; i++) {
            final double x = get(i);
            total += (x - m) * (x - m);
        }
        total /= n - 1;

        return Math.sqrt(total);
    }

    /**
     * Retrieves EJML's internal DDRM array data.
     *
     * @return 1D array
     */
    public double[] toArray1D() {
        return this.getDDRM().getData();
    }

    /**
     * Retrieves internal array data converted from it's 1D format to 2D.
     *
     * @return 2D array
     */
    public double[][] toArray2D() {
        final int rows = this.getDDRM().numRows;
        final int cols = this.getDDRM().numCols;
        double[][] retArr = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                retArr[i][j] = this.getDDRM().get(i, j);
            }
        }
        return retArr;
    }

}
