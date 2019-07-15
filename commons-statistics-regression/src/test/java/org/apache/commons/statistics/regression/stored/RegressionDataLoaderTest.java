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
package org.apache.commons.statistics.regression.stored;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegressionDataLoaderTest {

    private double[] yData0n = new double[] {};
    private double[] yData1n = new double[] {1};
    private double[] yData2n = new double[] {10, 20};
    private double[] yData3n = new double[] {1, 2, 3};
    private double[] yData4n = new double[] {0.1, 0.2, 0.3, 0.4};

    private double[][] xData0p0n = new double[][] {};
    private double[][] xData1p1n = new double[][] {{1.1}};
    private double[][] xData2p2n = new double[][] {{1, 2}, {3, 4}};
    private double[][] xData2p3n = new double[][] {{-2, -1}, {0, 1}, {2, 3}};
    private double[][] xData4p3n = new double[][] {{2, 4, 8, 16}, {32, 64, 128, 256}, {512, 1024, 2048, 5096}};
    private double[][] xData3p4n = new double[][] {{0.0, 0.5, 1.0}, {1.5, 2.0, 2.5}, {3.0, 3.5, 4.0}, {4.5, 5.0, 5.5}};
    private double[][] xData2p4n = new double[][] {{1, 3}, {5, 7}, {9, 11}, {13, 15}};

    @Test
    public void basicDataLoadingTest() {
        RegressionDataLoader loader = new RegressionDataLoader(yData3n, xData2p3n, false);

        // Printing the testing arrays, before and after wrapped inside a
        // StatisticsMatrix object
//        ArrayUtils.printArrayWithStreams(yData);
//        ArrayUtils.printArrayWithStreams(xData);
//        ArrayUtils.printArrayWithStreams(ArrayUtils.matrixToArray1D(data.getInputData().getYData()));
//        ArrayUtils.printArrayWithStreams(ArrayUtils.matrixToArray2D(data.getInputData().getXData()));

        // Checks that inputted data is stored as expected
        Assertions.assertEquals(loader.getInputData().getYData().get(1), 2, 0);
        Assertions.assertEquals(loader.getInputData().getXData().get(0, 0), -2, 0);
        Assertions.assertEquals(loader.getInputData().getXData().get(1, 1), 1, 0);

        // Testing toArray methods
        Assertions.assertTrue(Arrays.equals(loader.getInputData().getYData().toArray1D(), yData3n));
        Assertions.assertArrayEquals(loader.getInputData().getXData().toArray2D(), xData2p3n);
    }

    @Test
    public void validateSampleDataTest() {
        RegressionDataLoader loader = new RegressionDataLoader();
        // Null argument(s).
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(null, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(yData3n, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(null, xData3p4n));

        // Dimension mismatch.
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(yData3n, xData3p4n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(yData2n, xData2p3n));

        // Arrays have length 0.
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(yData0n, xData0p0n));

        // Not enough data for number of predictors.
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(yData1n, xData1p1n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(yData2n, xData2p2n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newSampleData(yData3n, xData4p3n));
    }

    @Test
    public void changingDataTest() {
        RegressionDataLoader loader = new RegressionDataLoader();

        loader.setHasIntercept(false); // Not creating column of 1's
        loader.newYSampleData(yData3n);
        loader.newXSampleData(xData2p3n);

        Assertions.assertTrue(Arrays.equals(loader.getInputData().getYData().toArray1D(), yData3n));
        Assertions.assertArrayEquals(loader.getInputData().getXData().toArray2D(), xData2p3n);

        // Changing to invalid data ( see validateSampleDataTest )
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newYSampleData(yData2n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.newXSampleData(xData2p2n));

        loader.clearData(); // clearData or else n = 4 in X is mismatched with previous n = 3 in Y

        // order does not matter
        loader.newXSampleData(xData3p4n);
        loader.newYSampleData(yData4n);
        Assertions.assertTrue(Arrays.equals(loader.getInputData().getYData().toArray1D(), yData4n));
        Assertions.assertArrayEquals(loader.getInputData().getXData().toArray2D(), xData3p4n);

        // changing X does not affect Y
        loader.newXSampleData(xData2p4n);
        Assertions.assertTrue(Arrays.equals(loader.getInputData().getYData().toArray1D(), yData4n));
        Assertions.assertArrayEquals(loader.getInputData().getXData().toArray2D(), xData2p4n);
    }

//
//    private static void printArrayWithStreams(double[][] arr) {
//        Stream.of(arr)
//            .map(Arrays::toString)
//            .forEach(System.out::println);
//    }
//
//    private static void printArrayWithStreams(double[] arr) {
//        Stream.of(arr)
//            .map(Arrays::toString)
//            .forEach(System.out::println);
//    }

}
