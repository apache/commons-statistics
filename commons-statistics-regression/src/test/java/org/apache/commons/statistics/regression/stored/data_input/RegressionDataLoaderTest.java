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
        RegressionDataLoader data = new RegressionDataLoader(yData3n, xData2p3n, true);

        // Checks that inputed data is stored as expected
        Assertions.assertEquals(data.getInputData().getY().get(1), 2, 0);
        Assertions.assertEquals(data.getInputData().getX().get(0, 0), -2, 0);
        Assertions.assertEquals(data.getInputData().getX().get(1, 1), 1, 0);

        // Testing toArray methods
        Assertions.assertTrue(Arrays.equals(data.getInputData().getY().toArray1D(), yData3n));
        Assertions.assertArrayEquals(data.getInputData().getX().toArray2D(), xData2p3n);
    }

    @Test
    public void validateSampleDataTest() {
        RegressionDataLoader data = new RegressionDataLoader();

        // Null argument(s).
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(null, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(yData3n, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(null, xData3p4n));

        // Dimension mismatch.
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(yData3n, xData3p4n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(yData2n, xData2p3n));

        // Arrays have length 0.
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(yData0n, xData0p0n));

        // Not enough data for number of predictors.
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(yData1n, xData1p1n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(yData2n, xData2p2n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewSampleData(yData3n, xData4p3n));
    }

    @Test
    public void changingDataTest() {
        RegressionDataLoader data = new RegressionDataLoader();

        data.setHasIntercept(true); // Not creating column of 1's
        data.inputNewYSampleData(yData3n);
        data.inputNewXSampleData(xData2p3n);

        Assertions.assertTrue(Arrays.equals(data.getInputData().getY().toArray1D(), yData3n));
        Assertions.assertArrayEquals(data.getInputData().getX().toArray2D(), xData2p3n);

        data.clearData(); // clearData or else n = 4 in X is mismatched with previous n = 3 in Y

        // order does not matter
        data.setHasIntercept(true); // Not creating column of 1's
        data.inputNewXSampleData(xData3p4n);
        data.inputNewYSampleData(yData4n);
        Assertions.assertTrue(Arrays.equals(data.getInputData().getY().toArray1D(), yData4n));
        Assertions.assertArrayEquals(data.getInputData().getX().toArray2D(), xData3p4n);

        // changing X does not affect Y
        data.inputNewXSampleData(xData2p4n);
        Assertions.assertTrue(Arrays.equals(data.getInputData().getY().toArray1D(), yData4n));
        Assertions.assertArrayEquals(data.getInputData().getX().toArray2D(), xData2p4n);

        data.setHasIntercept(false); // creating column of 1's

        // Changing to invalid data ( see validateSampleDataTest )
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewYSampleData(yData2n));
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.inputNewXSampleData(xData2p2n));

    }

}
