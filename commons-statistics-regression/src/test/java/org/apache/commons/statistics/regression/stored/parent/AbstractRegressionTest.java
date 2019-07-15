package org.apache.commons.statistics.regression.stored.parent;

import org.apache.commons.statistics.regression.stored.RegressionDataLoader;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public abstract class AbstractRegressionTest {

    protected RegressionDataLoader myData = new RegressionDataLoader();

    protected AbstractRegression regression;

    @Test
    public void canEstimateRegressandVariance() {
        if (getSampleSize() > getNumberOfRegressors()) {
            double variance = regression.estimateRegressandVariance();
            Assertions.assertTrue(variance > 0.0);
        }
    }

    @Test
    public void canEstimateRegressionParameters() {
        double[] beta = regression.estimateRegressionParameters();
        Assertions.assertEquals(getNumberOfRegressors(), beta.length);
    }

    @Test
    public void canEstimateRegressionParametersVariance() {
        double[][] variance = regression.estimateRegressionParametersVariance();
        Assertions.assertEquals(getNumberOfRegressors(), variance.length);
    }

    @Test
    public void canEstimateResiduals() {
        double[] e = regression.estimateResiduals();
        Assertions.assertEquals(getSampleSize(), e.length);
    }

    protected abstract AbstractRegression createRegression(RegressionDataLoader myData);

    protected abstract int getNumberOfRegressors();

    protected abstract int getSampleSize();

    @BeforeAll
    public void setUp() {
        regression = createRegression(myData);
    }

    /**
     * Verifies that newSampleData methods consistently insert unitary columns in
     * design matrix. Confirms the fix for MATH-411.
     */
    @Test
    public void testNewSample() {
        double[] design = new double[] {1, 19, 22, 33, 2, 20, 30, 40, 3, 25, 35, 45, 4, 27, 37, 47};
        double[] y = new double[] {1, 2, 3, 4};
        double[][] x = new double[][] {{19, 22, 33}, {20, 30, 40}, {25, 35, 45}, {27, 37, 47}};

        AbstractRegression regression = createRegression(myData);
        myData.newSampleData(design, 4, 3);
        StatisticsMatrix flatX = regression.getX().copy();
        StatisticsMatrix flatY = regression.getY().copy();
        myData.newXSampleData(x);
        myData.newYSampleData(y);
        Assertions.assertEquals(flatX, regression.getX());
        Assertions.assertEquals(flatY, regression.getY());

        // No intercept
        myData.setHasIntercept(false);
        myData.newSampleData(design, 4, 3);
        flatX = regression.getX().copy();
        flatY = regression.getY().copy();
        myData.newXSampleData(x);
        myData.newYSampleData(y);
        Assertions.assertEquals(flatX, regression.getX());
        Assertions.assertEquals(flatY, regression.getY());
    }

    @Test
    public void testNewSampleInsufficientData() {
        double[] data = new double[] {1, 2, 3, 4};
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.newSampleData(data, 1, 3));
    }

    @Test
    public void testNewSampleInvalidData() {
        double[] data = new double[] {1, 2, 3, 4};
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.newSampleData(data, 2, 3));
    }

    @Test
    public void testNewSampleNullData() {
        double[] data = null;
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.newSampleData(data, 2, 3));
    }

    @Test
    public void testXSampleDataNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.newXSampleData(null));
    }

    @Test
    public void testYSampleDataNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.newYSampleData(null));
    }

}
