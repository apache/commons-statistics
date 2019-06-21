package org.apache.commons.statistics.regression.stored;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.statistics.regression.util.array.ArrayUtils;

public class RegressionDataLoaderTest {

    private double[] yData = new double[] { 10, 11, 12, 13 };
    private double[][] xData = new double[][] { { -2, -1, 0 }, { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

    private double[] yData2 = new double[] { 10.2, 11.2, 12.2 };
    private double[][] xData2 = new double[][] { { 1.2, 2.2, 3.2 }, { 4.2, 5.2, 6.2 }, { 7.2, 8.2, 9.2 } };

    @Test
    public void newSampleDataTest() {
        RegressionDataLoader data = new RegressionDataLoader();
        data.newSampleData(yData, xData);

        // Printing the testing arrays, before and after wrapped inside a
        // StatisticsMatrix object
//        ArrayUtils.printArrayWithStreams(yData);
//        ArrayUtils.printArrayWithStreams(xData);
//        ArrayUtils.printArrayWithStreams(ArrayUtils.matrixToArray1D(data.getInputData().getYData()));
//        ArrayUtils.printArrayWithStreams(ArrayUtils.matrixToArray2D(data.getInputData().getXData()));

        Assertions.assertEquals(data.getInputData().getYData().get(1), 11, 0);
        Assertions.assertEquals(data.getInputData().getXData().get(0, 0), -2, 0);
        Assertions.assertEquals(data.getInputData().getXData().get(1, 1), 2, 0);

        Assertions.assertTrue(Arrays.equals(ArrayUtils.matrixToArray1D(data.getInputData().getYData()), yData));
        Assertions.assertArrayEquals(ArrayUtils.matrixToArray2D(data.getInputData().getXData()), xData);

    }

    @Test
    public void newYDataTest() {

    }

    @Test
    public void newXDataTest() {

    }

}
