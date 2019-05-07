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

import org.apache.commons.statistics.regression.stored.data_input.RegressionDataLoader;
import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public abstract class AbstractRegressionTest {

    protected RegressionDataLoader myData;

    protected AbstractRegression regression;

    protected abstract AbstractRegression createRegression(RegressionDataLoader data);

    protected abstract int getNumberOfRegressors();

    protected abstract int getSampleSize();

    @Test
    public void canEstimateRegressandVariance() {
        if (getSampleSize() > getNumberOfRegressors()) {
            double variance = regression.estimateRegressandVariance();
            Assertions.assertTrue(variance > 0.0);
        }
    }

    @Test
    public void canEstimateRegressionParameters() {
        double[] beta = regression.estimateBeta();
        Assertions.assertEquals(getNumberOfRegressors(), beta.length);
    }

    @Test
    public void canEstimateRegressionParametersVariance() {
        double[][] variance = regression.estimateBetaVariance();
        Assertions.assertEquals(getNumberOfRegressors(), variance.length);
    }

    @Test
    public void canEstimateResiduals() {
        double[] e = regression.estimateResiduals();
        Assertions.assertEquals(getSampleSize(), e.length);
    }

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
        RegressionDataLoader testData = new RegressionDataLoader();
        double[] design = new double[] {1, 19, 22, 33, 2, 20, 30, 40, 3, 25, 35, 45, 4, 27, 37, 47};
        double[] y = new double[] {1, 2, 3, 4};
        double[][] x = new double[][] {{19, 22, 33}, {20, 30, 40}, {25, 35, 45}, {27, 37, 47}};

        testData.inputNewSampleData(design, 4, 3);
        regression = createRegression(testData);

        StatisticsMatrix flatX = regression.getX().copy();
        StatisticsMatrix flatY = regression.getY().copy();
        testData.inputNewXSampleData(x);
        testData.inputNewYSampleData(y);
        Assertions.assertArrayEquals(flatX.toArray2D(), regression.getX().toArray2D());
        Assertions.assertArrayEquals(flatY.toArray1D(), regression.getY().toArray1D());

        // No intercept
        testData.setHasIntercept(false);
        testData.inputNewSampleData(design, 4, 3);
        flatX = regression.getX().copy();
        flatY = regression.getY().copy();
        testData.inputNewXSampleData(x);
        testData.inputNewYSampleData(y);
        Assertions.assertArrayEquals(flatX.toArray2D(), regression.getX().toArray2D());
        Assertions.assertArrayEquals(flatY.toArray1D(), regression.getY().toArray1D());
        regression = createRegression(myData);
    }

    // NOTE: Following tests were directly ported to ensure full coverage but is
    // repeated from RegressionDataLoaderTest case and will be removed later.
    @Test
    public void testNewSampleInsufficientData() {
        double[] data = new double[] {1, 2, 3, 4};
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.inputNewSampleData(data, 1, 3));
    }

    @Test
    public void testNewSampleInvalidData() {
        double[] data = new double[] {1, 2, 3, 4};
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.inputNewSampleData(data, 2, 3));
    }

    @Test
    public void testNewSampleNullData() {
        double[] data = null;
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.inputNewSampleData(data, 2, 3));
    }

    @Test
    public void testXSampleDataNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.inputNewXSampleData(null));
    }

    @Test
    public void testYSampleDataNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> myData.inputNewYSampleData(null));
    }

}
