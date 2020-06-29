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

package org.apache.commons.statistics.distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.numbers.gamma.LanczosApproximation;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for GammaDistribution.
 * Extends ContinuousDistributionAbstractTest.  See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 */
public class GammaDistributionTest extends ContinuousDistributionAbstractTest {

    private static final double HALF_LOG_2_PI = 0.5 * Math.log(2.0 * Math.PI);

    //---------------------- Override tolerance --------------------------------

    @BeforeEach
    public void customSetUp() {
        setTolerance(1e-9);
    }

    //-------------- Implementations for abstract methods ----------------------

    /** Creates the default continuous distribution instance to use in tests. */
    @Override
    public GammaDistribution makeDistribution() {
        return new GammaDistribution(4d, 2d);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R version 2.9.2
        return new double[] {0.857104827257, 1.64649737269, 2.17973074725, 2.7326367935, 3.48953912565,
                             26.1244815584, 20.0902350297, 17.5345461395, 15.5073130559, 13.3615661365};
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] {0.001, 0.01, 0.025, 0.05, 0.1, 0.999, 0.990, 0.975, 0.950, 0.900};
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] {0.00427280075546, 0.0204117166709, 0.0362756163658, 0.0542113174239, 0.0773195272491,
                             0.000394468852816, 0.00366559696761, 0.00874649473311, 0.0166712508128, 0.0311798227954};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    public void testParameterAccessors() {
        final GammaDistribution distribution = makeDistribution();
        Assertions.assertEquals(4d, distribution.getShape());
        Assertions.assertEquals(2d, distribution.getScale());
    }

    @Test
    public void testConstructorPrecondition1() {
        Assertions.assertThrows(DistributionException.class, () -> new GammaDistribution(0, 1));
    }
    @Test
    public void testConstructorPrecondition2() {
        Assertions.assertThrows(DistributionException.class, () -> new GammaDistribution(1, 0));
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        GammaDistribution dist;

        dist = new GammaDistribution(1, 2);
        Assertions.assertEquals(2, dist.getMean(), tol);
        Assertions.assertEquals(4, dist.getVariance(), tol);

        dist = new GammaDistribution(1.1, 4.2);
        Assertions.assertEquals(1.1d * 4.2d, dist.getMean(), tol);
        Assertions.assertEquals(1.1d * 4.2d * 4.2d, dist.getVariance(), tol);
    }

    @Test
    public void testProbabilities() {
        testProbability(-1.000, 4.0, 2.0, .0000);
        testProbability(15.501, 4.0, 2.0, .9499);
        testProbability(0.504, 4.0, 1.0, .0018);
        testProbability(10.011, 1.0, 2.0, .9933);
        testProbability(5.000, 2.0, 2.0, .7127);
    }

    @Test
    public void testValues() {
        testValue(15.501, 4.0, 2.0, .9499);
        testValue(0.504, 4.0, 1.0, .0018);
        testValue(10.011, 1.0, 2.0, .9933);
        testValue(5.000, 2.0, 2.0, .7127);
    }

    private void testProbability(double x, double a, double b, double expected) {
        final GammaDistribution distribution = new GammaDistribution(a, b);
        final double actual = distribution.cumulativeProbability(x);
        Assertions.assertEquals(expected, actual, 10e-4, () -> "probability for " + x);
    }

    private void testValue(double expected, double a, double b, double p) {
        final GammaDistribution distribution = new GammaDistribution(a, b);
        final double actual = distribution.inverseCumulativeProbability(p);
        Assertions.assertEquals(expected, actual, 10e-4, () -> "critical value for " + p);
    }

    @Test
    public void testDensity() {
        final double[] x = new double[]{-0.1, 1e-6, 0.5, 1, 2, 5};
        // R2.5: print(dgamma(x, shape=1, rate=1), digits=10)
        checkDensity(1, 1, x, new double[]{0.000000000000, 0.999999000001, 0.606530659713, 0.367879441171, 0.135335283237, 0.006737946999});
        // R2.5: print(dgamma(x, shape=2, rate=1), digits=10)
        checkDensity(2, 1, x, new double[]{0.000000000000, 0.000000999999, 0.303265329856, 0.367879441171, 0.270670566473, 0.033689734995});
        // R2.5: print(dgamma(x, shape=4, rate=1), digits=10)
        checkDensity(4, 1, x, new double[]{0.000000000e+00, 1.666665000e-19, 1.263605541e-02, 6.131324020e-02, 1.804470443e-01, 1.403738958e-01});
        // R2.5: print(dgamma(x, shape=4, rate=10), digits=10)
        checkDensity(4, 10, x, new double[]{0.000000000e+00, 1.666650000e-15, 1.403738958e+00, 7.566654960e-02, 2.748204830e-05, 4.018228850e-17});
        // R2.5: print(dgamma(x, shape=.1, rate=10), digits=10)
        checkDensity(0.1, 10, x, new double[]{0.000000000e+00, 3.323953832e+04, 1.663849010e-03, 6.007786726e-06, 1.461647647e-10, 5.996008322e-24});
        // R2.5: print(dgamma(x, shape=.1, rate=20), digits=10)
        checkDensity(0.1, 20, x, new double[]{0.000000000e+00, 3.562489883e+04, 1.201557345e-05, 2.923295295e-10, 3.228910843e-19, 1.239484589e-45});
        // R2.5: print(dgamma(x, shape=.1, rate=4), digits=10)
        checkDensity(0.1, 4, x, new double[]{0.000000000e+00, 3.032938388e+04, 3.049322494e-02, 2.211502311e-03, 2.170613371e-05, 5.846590589e-11});
        // R2.5: print(dgamma(x, shape=.1, rate=1), digits=10)
        checkDensity(0.1, 1, x, new double[]{0.000000000e+00, 2.640334143e+04, 1.189704437e-01, 3.866916944e-02, 7.623306235e-03, 1.663849010e-04});
        // To force overflow condition
        // R2.5: print(dgamma(x, shape=1000, rate=100), digits=10)
        checkDensity(1000, 100, x, new double[]{0.000000000e+00, 0.000000000e+00, 0.000000000e+00, 0.000000000e+00, 0.000000000e+00, 3.304830256e-84});
    }

    private void checkDensity(double alpha, double rate, double[] x, double[] expected) {
        final GammaDistribution d = new GammaDistribution(alpha, 1 / rate);
        for (int i = 0; i < x.length; i++) {
            Assertions.assertEquals(expected[i], d.density(x[i]), Math.abs(expected[i]) * 1e-5);
        }
    }

    @Test
    public void testLogDensity() {
        final double[] x = new double[]{-0.1, 1e-6, 0.5, 1, 2, 5};
        final double inf = Double.POSITIVE_INFINITY;
        // R2.5: print(dgamma(x, shape=1, rate=1, log=TRUE), digits=10)
        checkLogDensity(1, 1, x, new double[]{-inf, -1e-06, -5e-01, -1e+00, -2e+00, -5e+00});
        // R2.5: print(dgamma(x, shape=2, rate=1, log=TRUE), digits=10)
        checkLogDensity(2, 1, x, new double[]{-inf, -13.815511558, -1.193147181, -1.000000000, -1.306852819, -3.390562088});
        // R2.5: print(dgamma(x, shape=4, rate=1, log=TRUE), digits=10)
        checkLogDensity(4, 1, x, new double[]{-inf, -43.238292143, -4.371201011, -2.791759469, -1.712317928, -1.963445732});
        // R2.5: print(dgamma(x, shape=4, rate=10, log=TRUE), digits=10)
        checkLogDensity(4, 10, x, new double[]{-inf, -34.0279607711, 0.3391393611, -2.5814190973, -10.5019775556, -37.7531053599});
        // R2.5: print(dgamma(x, shape=.1, rate=10, log=TRUE), digits=10)
        checkLogDensity(0.1, 10, x, new double[]{-inf, 10.41149536, -6.39862168, -12.02245414, -22.64628660, -53.47094826});
        // R2.5: print(dgamma(x, shape=.1, rate=20, log=TRUE), digits=10)
        checkLogDensity(0.1, 20, x, new double[]{-inf, 10.48080008, -11.32930696, -21.95313942, -42.57697189, -103.40163355});
        // R2.5: print(dgamma(x, shape=.1, rate=4, log=TRUE), digits=10)
        checkLogDensity(0.1, 4, x, new double[]{-inf, 10.319872287, -3.490250753, -6.114083216, -10.737915678, -23.562577337});
        // R2.5: print(dgamma(x, shape=.1, rate=1, log=TRUE), digits=10)
        checkLogDensity(0.1, 1, x, new double[]{-inf, 10.181245850, -2.128880189, -3.252712652, -4.876545114, -8.701206773});
        // To force overflow condition
        // R2.5: print(dgamma(x, shape=1000, rate=100, log=TRUE), digits=10)
        checkLogDensity(1000, 100, x, new double[]{-inf, -15101.7453846, -2042.5042706, -1400.0502372, -807.5962038, -192.2217627});
    }

    private void checkLogDensity(double alpha, double rate, double[] x, double[] expected) {
        final GammaDistribution d = new GammaDistribution(alpha, 1 / rate);
        for (int i = 0; i < x.length; i++) {
            Assertions.assertEquals(expected[i], d.logDensity(x[i]), Math.abs(expected[i]) * 1e-5);
        }
    }

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        setInverseCumulativeTestPoints(new double[] {0, 1});
        setInverseCumulativeTestValues(new double[] {0, Double.POSITIVE_INFINITY});
        verifyInverseCumulativeProbabilities();
    }

    public static double logGamma(double x) {
        /*
         * This is a copy of
         * double Gamma.logGamma(double)
         * prior to MATH-849
         */
        double ret;

        if (Double.isNaN(x) || (x <= 0.0)) {
            ret = Double.NaN;
        } else {
            final double sum = LanczosApproximation.value(x);
            final double tmp = x + LanczosApproximation.g() + .5;
            ret = ((x + .5) * Math.log(tmp)) - tmp +
                HALF_LOG_2_PI + Math.log(sum / x);
        }

        return ret;
    }

    public static double density(final double x,
                                 final double shape,
                                 final double scale) {
        /*
         * This is a copy of
         * double GammaDistribution.density(double)
         * prior to MATH-753.
         */
        if (x < 0) {
            return 0;
        }
        return Math.pow(x / scale, shape - 1) / scale *
               Math.exp(-x / scale) / Math.exp(logGamma(shape));
    }

    /*
     * MATH-753: large values of x or shape parameter cause density(double) to
     * overflow. Reference data is generated with the Maxima script
     * gamma-distribution.mac, which can be found in
     * src/test/resources/org/apache/commons/math3/distribution.
     */

    private void doTestMath753(final double shape,
                               final double meanNoOF, final double sdNoOF,
                               final double meanOF, final double sdOF,
                               final String resourceName)
        throws IOException {
        final GammaDistribution distribution = new GammaDistribution(shape, 1.0);
        final SummaryStatistics statOld = new SummaryStatistics();
        final SummaryStatistics statNewNoOF = new SummaryStatistics();
        final SummaryStatistics statNewOF = new SummaryStatistics();

        final InputStream resourceAsStream;
        resourceAsStream = this.getClass().getResourceAsStream(resourceName);
        Assertions.assertNotNull(resourceAsStream, () -> "Could not find resource " + resourceName);
        final BufferedReader in;
        in = new BufferedReader(new InputStreamReader(resourceAsStream));

        try {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                if (line.startsWith("#")) {
                    continue;
                }
                final String[] tokens = line.split(", ");
                Assertions.assertEquals(2, tokens.length, "expected two floating-point values");
                final double x = Double.parseDouble(tokens[0]);
                final String msg = "x = " + x + ", shape = " + shape +
                                   ", scale = 1.0";
                final double expected = Double.parseDouble(tokens[1]);
                final double ulp = Math.ulp(expected);
                final double actualOld = density(x, shape, 1.0);
                final double actualNew = distribution.density(x);
                final double errOld = Math.abs((actualOld - expected) / ulp);
                final double errNew = Math.abs((actualNew - expected) / ulp);

                if (Double.isNaN(actualOld) || Double.isInfinite(actualOld)) {
                    Assertions.assertFalse(Double.isNaN(actualNew), msg);
                    Assertions.assertFalse(Double.isInfinite(actualNew), msg);
                    statNewOF.addValue(errNew);
                } else {
                    statOld.addValue(errOld);
                    statNewNoOF.addValue(errNew);
                }
            }
            if (statOld.getN() != 0) {
                /*
                 * If no overflow occurs, check that new implementation is
                 * better than old one.
                 */
                final StringBuilder sb = new StringBuilder("shape = ");
                sb.append(shape);
                sb.append(", scale = 1.0\n");
                sb.append("Old implementation\n");
                sb.append("------------------\n");
                sb.append(statOld.toString());
                sb.append("New implementation\n");
                sb.append("------------------\n");
                sb.append(statNewNoOF.toString());
                final String msg = sb.toString();

                final double oldMin = statOld.getMin();
                final double newMin = statNewNoOF.getMin();
                Assertions.assertTrue(newMin <= oldMin, msg);

                final double oldMax = statOld.getMax();
                final double newMax = statNewNoOF.getMax();
                Assertions.assertTrue(newMax <= oldMax, msg);

                final double oldMean = statOld.getMean();
                final double newMean = statNewNoOF.getMean();
                Assertions.assertTrue(newMean <= oldMean, msg);

                final double oldSd = statOld.getStandardDeviation();
                final double newSd = statNewNoOF.getStandardDeviation();
                Assertions.assertTrue(newSd <= oldSd, msg);

                Assertions.assertTrue(newMean <= meanNoOF, msg);
                Assertions.assertTrue(newSd <= sdNoOF, msg);
            }
            if (statNewOF.getN() != 0) {
                final double newMean = statNewOF.getMean();
                final double newSd = statNewOF.getStandardDeviation();

                final StringBuilder sb = new StringBuilder("shape = ");
                sb.append(shape);
                sb.append(", scale = 1.0");
                sb.append(", max. mean error (ulps) = ");
                sb.append(meanOF);
                sb.append(", actual mean error (ulps) = ");
                sb.append(newMean);
                sb.append(", max. sd of error (ulps) = ");
                sb.append(sdOF);
                sb.append(", actual sd of error (ulps) = ");
                sb.append(newSd);
                final String msg = sb.toString();

                Assertions.assertTrue(newMean <= meanOF, msg);
                Assertions.assertTrue(newSd <= sdOF, msg);
            }
        } catch (final IOException e) {
            Assertions.fail(e.getMessage());
        } finally {
            in.close();
        }
    }

    @Test
    public void testMath753Shape1() throws IOException {
        doTestMath753(1.0, 1.5, 0.5, 0.0, 0.0, "gamma-distribution-shape-1.csv");
    }

    @Test
    public void testMath753Shape8() throws IOException {
        doTestMath753(8.0, 1.5, 1.0, 0.0, 0.0, "gamma-distribution-shape-8.csv");
    }

    @Test
    public void testMath753Shape10() throws IOException {
        doTestMath753(10.0, 1.0, 1.0, 0.0, 0.0, "gamma-distribution-shape-10.csv");
    }

    @Test
    public void testMath753Shape100() throws IOException {
        // XXX Increased tolerance ("1.5" -> "2.0") to make test pass with JDK "Math"
        // where CM used "FastMath" (cf. "XXX" comment in main source code).
        doTestMath753(100.0, 2.0, 1.0, 0.0, 0.0, "gamma-distribution-shape-100.csv");
    }

    @Test
    public void testMath753Shape142() throws IOException {
        doTestMath753(142.0, 3.3, 1.6, 40.0, 40.0, "gamma-distribution-shape-142.csv");
    }

    @Test
    public void testMath753Shape1000() throws IOException {
        // XXX Increased tolerance ("220.0" -> "230.0") to make test pass with JDK "Math"
        // where CM used "FastMath" (cf. "XXX" comment in main source code).
        doTestMath753(1000.0, 1.0, 1.0, 160.0, 230.0, "gamma-distribution-shape-1000.csv");
    }
}
