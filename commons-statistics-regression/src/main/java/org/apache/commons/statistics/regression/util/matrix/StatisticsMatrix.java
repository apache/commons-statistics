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
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.ops.ConvertDMatrixStruct;
import org.ejml.ops.ConvertFMatrixStruct;
import org.ejml.simple.SimpleBase;

import java.util.Random;


/**
 * Example of how to extend "SimpleMatrix" and add your own functionality.  In this case
 * two basic statistic operations are added.  Since SimpleBase is extended and StatisticsMatrix
 * is specified as the generics type, all "SimpleMatrix" operations return a matrix of
 * type StatisticsMatrix, ensuring strong typing.
 *
 * @author Peter Abeles
 * -------------------------------------------------------------------------------------------
 * Modifications for Apache Commons Statistics Regressiom library by: Ben Nguyen
 * Under development for specific usage which are to be determined.
 * -------------------------------------------------------------------------------------------
 */
public class StatisticsMatrix extends SimpleBase<StatisticsMatrix> {

    
    /**
     * 
     */
    private static final long serialVersionUID = -82801259856161557L;

    public StatisticsMatrix( int numRows , int numCols ) {
        super(numRows,numCols);
    }

    protected StatisticsMatrix(){}

    /**
     * Wraps a StatisticsMatrix around 'm'.  Does NOT create a copy of 'm' but saves a reference
     * to it.
     */
    public static StatisticsMatrix wrap( DMatrixRMaj m ) {
        StatisticsMatrix ret = new StatisticsMatrix();
//        ret.mat = m;
        ret.setMatrix(m); // changed by Ben Nguyen to also lookup ops

        return ret;
    }
    
    //ADDED by Ben Nguyen: to satisfy the wrapMatrix method, copied from SimpleMatrix 
    public StatisticsMatrix( Matrix orig ) {
        Matrix mat;
        if( orig instanceof DMatrixRBlock) {
            DMatrixRMaj a = new DMatrixRMaj(orig.getNumRows(), orig.getNumCols());
            ConvertDMatrixStruct.convert((DMatrixRBlock) orig, a);
            mat = a;
        } else if( orig instanceof FMatrixRBlock) {
            FMatrixRMaj a = new FMatrixRMaj(orig.getNumRows(),orig.getNumCols());
            ConvertFMatrixStruct.convert((FMatrixRBlock)orig, a);
            mat = a;
        } else {
            mat = orig.copy();
        }
        setMatrix(mat);
    }

    /**
     * Computes the mean or average of all the elements.
     *
     * @return mean
     */
    public double mean() {
        double total = 0;

        final int N = getNumElements();
        for( int i = 0; i < N; i++ ) {
            total += get(i);
        }

        return total/N;
    }

    /**
     * Computes the unbiased standard deviation of all the elements.
     *
     * @return standard deviation
     */
    public double stdev() {
        double m = mean();

        double total = 0;

        final int N = getNumElements();
        if( N <= 1 )
            throw new IllegalArgumentException("There must be more than one element to compute stdev");


        for( int i = 0; i < N; i++ ) {
            double x = get(i);

            total += (x - m)*(x - m);
        }

        total /= (N-1);

        return Math.sqrt(total);
    }

    /**
     * Returns a matrix of StatisticsMatrix type so that SimpleMatrix functions create matrices
     * of the correct type.
     */
    @Override
    protected StatisticsMatrix createMatrix(int numRows, int numCols, MatrixType type) {// changed by Ben Nguyen to add 
        return new StatisticsMatrix(numRows,numCols);                //MatrixType type param to satisfy abstract method
    }


	@Override
	protected StatisticsMatrix wrapMatrix(Matrix m) {
		// TODO Auto-generated method stub
		return new StatisticsMatrix(m);
	}
	
	
    public static void main( String args[] ) {
        Random rand = new Random(24234);

        int N = 500;

        // create two vectors whose elements are drawn from uniform distributions
        StatisticsMatrix A = StatisticsMatrix.wrap(RandomMatrices_DDRM.rectangle(N,1,0,1,rand));
        StatisticsMatrix B = StatisticsMatrix.wrap(RandomMatrices_DDRM.rectangle(N,1,1,2,rand));

        // the mean should be about 0.5
        System.out.println("Mean of A is               "+A.mean());
        // the mean should be about 1.5
        System.out.println("Mean of B is               "+B.mean());

        StatisticsMatrix C = A.transpose();
//        StatisticsMatrix C = A.plus(B);

        // the mean should be about 2.0
        System.out.println("Mean of C = A + B is       "+C.mean());

        System.out.println("Standard deviation of A is "+A.stdev());
        System.out.println("Standard deviation of B is "+B.stdev());
        System.out.println("Standard deviation of C is "+C.stdev());
    }
    
    
    
}