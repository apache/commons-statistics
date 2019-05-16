package org.apache.commons.statistics.regression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.Matrix;
import org.ejml.data.MatrixType;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.ops.ConvertDMatrixStruct;
import org.ejml.ops.MatrixIO;
import org.ejml.simple.SimpleBase;
import org.ejml.simple.SimpleMatrix;

public class SetupPractice {

    public static void main(String[] args) {
        System.out.println("-- SETUP PRACTICE --");

        
        
        
        
        List<String> list = new ArrayList<>();
        list.add("One");
        list.add("Abc");
        list.add("BCD");

        Collections.sort(list, (String a, String b) -> {
            return a.compareTo(b);
        });

        System.out.println(list);
        
        
        Comparator<String> comparator = (String a, String b) -> {
            return a.compareTo(b);
        };

        Comparator<String> comparatorReversed = comparator.reversed();

        Collections.sort(list, comparatorReversed);

        System.out.println(list);
        
        
//		DMatrixRMaj A = new DMatrixRMaj(2, 3, true, new double[] { 1, 2, 3, 4, 5, 6 });
//
//		try {
//			MatrixIO.saveDenseCSV(A, "matrix_file.csv");
//			DMatrixRMaj B = MatrixIO.loadCSV("matrix_file.csv", false);
//			B.print();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//		
//		SimpleMatrix As = new SimpleMatrix(2, 3, true, new double[] { 1, 2, 3, 4, 5, 6 });
//
//		try {
//			As.saveToFileCSV("matrix_file.csv");
//			SimpleMatrix B = As.loadCSV("matrix_file.csv");
//			B.print();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

//		Random rand = new Random(24234);
//		int N = 5;
//
//		int[][] mat = new int[N][N];
//		int iter = 0;
//
//		for (int[] i : mat) {
////			iter++;
////			System.out.println(iter);
//			for (int j : i) {
//				i[ j] = iter++;
////				System.out.println(iter);
//			}
//				
//		}
//		
//		for (int[] i : mat) {
////			iter++;
//			System.out.println();
//			for (int j : i) {
////				i[(int) j] = j + iter;
//				System.out.println(i[j]);
//			}
//				
//		}

        
        
        
//        double mat[][] = { { 1, 2, 3 }, { 7, 8, 9 } };
//
//        for (int i = 0; i < mat.length; i++) {
//            for (int j = 0; j < mat[i].length; j++) {
//                System.out.println("Values at arr[" + i + "][" + j + "] is " + mat[i][j]);
//            }
//        }
//
//        SimpleMatrix A = new SimpleMatrix(mat);
//        SimpleMatrix B = A.transpose();

        
        
        
        
        
        // create two vectors whose elements are drawn from uniform distributions
//        SimpleMatrix A = SimpleMatrix.wrap(RandomMatrices_DDRM.rectangle(N,1,0,1,rand));
//        SimpleMatrix B = SimpleMatrix.wrap(RandomMatrices_DDRM.rectangle(N,1,1,2,rand));

        // the mean should be about 0.5
//        System.out.println("Mean of A is               "+A.mean());
        // the mean should be about 1.5
//        System.out.println("Mean of B is               "+B.mean());

        
        
        
        
        
//        SimpleMatrix C = A.mult(B);
//
//        // the mean should be about 2.0
//        System.out.println();
//        C.print();
//        System.out.println();
//        A.print();
//        System.out.println();
//        B.print();

        
        
        
//        System.out.println("Standard deviation of A is "+A.stdev());
//        System.out.println("Standard deviation of B is "+B.stdev());
//        System.out.println("Standard deviation of C is "+C.stdev());

    }

}
