package org.apache.commons.statistics.regression.util.array;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public class ArrayUtils {

    public static double[] matrixToArray1D(StatisticsMatrix mat) {
        return mat.getDDRM().data;
    }

    public static double[][] matrixToArray2D(StatisticsMatrix mat) {
        int rows = mat.getDDRM().numRows;
        int cols = mat.getDDRM().numCols;
        double[][] retArr = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++)
                retArr[i][j] = mat.getDDRM().get(i, j);
        }

        return retArr;
    }
    
    public static void printArray1D(double[] arr) {
        for (int i = 0; i < arr.length; i++)
            System.out.print(arr[i] + ", ");
        System.out.println();
    }

    public static void printArray2D(double[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++)
                System.out.print(arr[i][j] + ", ");
            System.out.println();
        }
    }
    
    public static void printArrayWithStreams(double[][] arr) {
        Stream.of(arr)
            .map(Arrays::toString)
            .forEach(System.out::println);
    }
    
    public static void printArrayWithStreams(double[] arr) {
        Stream.of(arr)
            .map(Arrays::toString)
            .forEach(System.out::println);
    }
}
