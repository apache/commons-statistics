package org.apache.commons.statistics.regression.stored;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.statistics.regression.util.array.ArrayUtils;
import org.junit.Test;

public class RegressionDataLoaderTest {

    @Test
    public void loadingDataTest() {

        double[] yData = new double[] { 10, 11, 12 };
        double[][] xData = new double[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        RegressionDataLoader data = new RegressionDataLoader();
        data.newSampleData(yData, xData);

        // Printing the testing arrays, before and after wrapped inside a StatisticsMatrix object
//        ArrayUtils.printArrayWithStreams(yData);
//        ArrayUtils.printArrayWithStreams(xData);
//        ArrayUtils.printArrayWithStreams(ArrayUtils.matrixToArray1D(data.getInputData().getYData()));
//        ArrayUtils.printArrayWithStreams(ArrayUtils.matrixToArray2D(data.getInputData().getXData()));

        assertEquals(data.getInputData().getYData().get(1), 11, 0);
        assertEquals(data.getInputData().getXData().get(0, 0), 1, 0);
        assertEquals(data.getInputData().getXData().get(1, 1), 5, 0);

        assertTrue(Arrays.equals(ArrayUtils.matrixToArray1D(data.getInputData().getYData()), yData));
        assertArrayEquals(ArrayUtils.matrixToArray2D(data.getInputData().getXData()), xData);

    }

}
