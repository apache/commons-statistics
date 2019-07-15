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
 * @author Peter Abeles
 *         -------------------------------------------------------------------------------------------
 *         Modifications for Apache Commons Statistics Regressiom library by:
 *         Ben Nguyen Under development for specific usage which are to be
 *         determined.
 *         -------------------------------------------------------------------------------------------
 */
public class StatisticsMatrix extends SimpleBase<StatisticsMatrix> {

    /**
     *
     */
    private static final long serialVersionUID = -82801259856161557L;

    /**
     * Creates an identity matrix given it's square width.
     *
     * @param width
     * @return the width sized identity matrix
     */
    @SuppressWarnings("unchecked")
    public static StatisticsMatrix identity(int width) {
        StatisticsMatrix ret = new StatisticsMatrix(width, width);
        ret.ops.setIdentity(ret.mat);
        return ret;
    }

    /**
     * Wraps a StatisticsMatrix around 'm'. Does NOT create a copy of 'm' but saves
     * a reference to it.
     *
     * @param m
     * @return new pointer
     */
    public static StatisticsMatrix wrap(DMatrixRMaj m) {
        StatisticsMatrix ret = new StatisticsMatrix();
//        ret.mat = m;
        ret.setMatrix(m);

        return ret;
    }

    /**
     * Constructor for internal library use only. Nothing is configured and is
     * intended for serialization.
     */
    protected StatisticsMatrix() {
    }

    /**
     * Constructs a new empty matrix with a set dimension.
     *
     * @param numRows
     * @param numCols
     */
    public StatisticsMatrix(int numRows, int numCols) {
        super(numRows, numCols);
    }

    /**
     * Constructs a new StatisticsMatrix object wrapper around a Matrix interface.
     *
     * @param orig
     */
    public StatisticsMatrix(Matrix orig) {
        Matrix mat;
        if (orig instanceof DMatrixRBlock) {
            DMatrixRMaj a = new DMatrixRMaj(orig.getNumRows(), orig.getNumCols());
            ConvertDMatrixStruct.convert((DMatrixRBlock) orig, a);
            mat = a;
        } else if (orig instanceof FMatrixRBlock) {
            FMatrixRMaj a = new FMatrixRMaj(orig.getNumRows(), orig.getNumCols());
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
     */
    public double stdev() {
        double m = mean();

        double total = 0;

        final int n = getNumElements();
        if (n <= 1) {
            throw new IllegalArgumentException("There must be more than one element to compute stdev");
        }

        for (int i = 0; i < n; i++) {
            double x = get(i);

            total += (x - m) * (x - m);
        }

        total /= n - 1;

        return Math.sqrt(total);
    }

    /**
     * Retrieves internal array data.
     *
     * @return 1D array
     */
    public double[] toArray1D() {
        return this.getDDRM().data;
    }

    /**
     * Retrieves internal array data.
     *
     * @return 2D array
     */
    public double[][] toArray2D() {
        int rows = this.getDDRM().numRows;
        int cols = this.getDDRM().numCols;
        double[][] retArr = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                retArr[i][j] = this.getDDRM().get(i, j);
            }
        }

        return retArr;
    }

    /**
     * Sets a new matrix by creating a new pointer object.
     */
    @Override
    protected StatisticsMatrix wrapMatrix(Matrix m) {
        return new StatisticsMatrix(m);
    }

//    private static void printArrayWithStreams(double[][] arr) {
//        Stream.of(arr).map(Arrays::toString).forEach(System.out::println);
//    }
//
//    private static void printArrayWithStreams(double[] arr) {
//        Stream.of(arr).map(Arrays::toString).forEach(System.out::println);
//    }

}
