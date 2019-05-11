package org.apache.commons.statistics.regression;

import java.io.IOException;
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
		System.out.println("Magic");

		DMatrixRMaj A = new DMatrixRMaj(2, 3, true, new double[] { 1, 2, 3, 4, 5, 6 });

		try {
			MatrixIO.saveDenseCSV(A, "matrix_file.csv");
			DMatrixRMaj B = MatrixIO.loadCSV("matrix_file.csv", false);
			B.print();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		
		SimpleMatrix As = new SimpleMatrix(2, 3, true, new double[] { 1, 2, 3, 4, 5, 6 });

		try {
			As.saveToFileCSV("matrix_file.csv");
			SimpleMatrix B = As.loadCSV("matrix_file.csv");
			B.print();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
	

}
