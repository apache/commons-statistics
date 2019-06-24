package org.apache.commons.statistics.regression.stored.ols;

import org.junit.Assert;
//import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.math4.TestUtils;
import org.apache.commons.math4.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.statistics.regression.stored.RegressionDataLoader;
import org.apache.commons.statistics.regression.stored.ols.OLSRegression;
import org.apache.commons.statistics.regression.stored.ols.OLSRegressionTest;

public class OLSRegressionTest {

    // IN PROGRESS

    private RegressionDataLoader loader = new RegressionDataLoader();
    private double[] y;
    private double[][] x;

    @BeforeAll
    public void setUp() {
        y = new double[] { 11.0, 12.0, 13.0, 14.0, 15.0, 16.0 };
        x = new double[6][];
        x[0] = new double[] { 0, 0, 0, 0, 0 };
        x[1] = new double[] { 2.0, 0, 0, 0, 0 };
        x[2] = new double[] { 0, 3.0, 0, 0, 0 };
        x[3] = new double[] { 0, 0, 4.0, 0, 0 };
        x[4] = new double[] { 0, 0, 0, 5.0, 0 };
        x[5] = new double[] { 0, 0, 0, 0, 6.0 };
//        super.setUp();
    }

    /**
     * Test Longley dataset against certified values provided by NIST.
     * Data Source: J. Longley (1967) "An Appraisal of Least Squares
     * Programs for the Electronic Computer from the Point of View of the User"
     * Journal of the American Statistical Association, vol. 62. September,
     * pp. 819-841.
     *
     * Certified values (and data) are from NIST:
     * http://www.itl.nist.gov/div898/strd/lls/data/LINKS/DATA/Longley.dat
     */
    @Test
    public void testLongly() {
        // Y values are first, then independent vars
        // Each row is one observation
        double[] design = new double[] {
            60323,83.0,234289,2356,1590,107608,1947,
            61122,88.5,259426,2325,1456,108632,1948,
            60171,88.2,258054,3682,1616,109773,1949,
            61187,89.5,284599,3351,1650,110929,1950,
            63221,96.2,328975,2099,3099,112075,1951,
            63639,98.1,346999,1932,3594,113270,1952,
            64989,99.0,365385,1870,3547,115094,1953,
            63761,100.0,363112,3578,3350,116219,1954,
            66019,101.2,397469,2904,3048,117388,1955,
            67857,104.6,419180,2822,2857,118734,1956,
            68169,108.4,442769,2936,2798,120445,1957,
            66513,110.8,444546,4681,2637,121950,1958,
            68655,112.6,482704,3813,2552,123366,1959,
            69564,114.2,502601,3931,2514,125368,1960,
            69331,115.7,518173,4806,2572,127852,1961,
            70551,116.9,554894,4007,2827,130081,1962
        };

        final int nobs = 16;
        final int nvars = 6;

        // Estimate the model
        RegressionDataLoader loader = new RegressionDataLoader();
        OLSRegression model = new OLSRegression(loader);
        loader.newSampleData(design, nobs, nvars);

        // Check expected beta values from NIST
        double[] betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
          new double[]{-3482258.63459582, 15.0618722713733,
                -0.358191792925910E-01,-2.02022980381683,
                -1.03322686717359,-0.511041056535807E-01,
                 1829.15146461355}, 2E-8); //

        // Check expected residuals from R
        double[] residuals = model.estimateResiduals();
        TestUtils.assertEquals(residuals, new double[]{
                267.340029759711,-94.0139423988359,46.28716775752924,
                -410.114621930906,309.7145907602313,-249.3112153297231,
                -164.0489563956039,-13.18035686637081,14.30477260005235,
                 455.394094551857,-17.26892711483297,-39.0550425226967,
                -155.5499735953195,-85.6713080421283,341.9315139607727,
                -206.7578251937366},
                      1E-8);

        // Check standard errors from NIST
        double[] errors = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(new double[] {890420.383607373,
                       84.9149257747669,
                       0.334910077722432E-01,
                       0.488399681651699,
                       0.214274163161675,
                       0.226073200069370,
                       455.478499142212}, errors, 1E-6);

        // Check regression standard error against R
        Assert.assertEquals(304.8540735619638, model.estimateRegressionStandardError(), 1E-10);

        // Check R-Square statistics against R
        Assert.assertEquals(0.995479004577296, model.calculateRSquared(), 1E-12);
        Assert.assertEquals(0.992465007628826, model.calculateAdjustedRSquared(), 1E-12);

        checkVarianceConsistency(model);

        // Estimate model without intercept
        model.setNoIntercept(true);
        model.newSampleData(design, nobs, nvars);

        // Check expected beta values from R
        betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
          new double[]{-52.99357013868291, 0.07107319907358,
                -0.42346585566399,-0.57256866841929,
                -0.41420358884978, 48.41786562001326}, 1E-11);

        // Check standard errors from R
        errors = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(new double[] {129.54486693117232, 0.03016640003786,
                0.41773654056612, 0.27899087467676, 0.32128496193363,
                17.68948737819961}, errors, 1E-11);

        // Check expected residuals from R
        residuals = model.estimateResiduals();
        TestUtils.assertEquals(residuals, new double[]{
                279.90274927293092, -130.32465380836874, 90.73228661967445, -401.31252201634948,
                -440.46768772620027, -543.54512853774793, 201.32111639536299, 215.90889365977932,
                73.09368242049943, 913.21694494481869, 424.82484953610174, -8.56475876776709,
                -361.32974610842876, 27.34560497213464, 151.28955976355002, -492.49937355336846},
                      1E-10);

        // Check regression standard error against R
        Assert.assertEquals(475.1655079819517, model.estimateRegressionStandardError(), 1E-10);

        // Check R-Square statistics against R
        Assert.assertEquals(0.9999670130706, model.calculateRSquared(), 1E-12);
        Assert.assertEquals(0.999947220913, model.calculateAdjustedRSquared(), 1E-12);

    }



}
