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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link FDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class FDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double df1 = (Double) parameters[0];
        final double df2 = (Double) parameters[1];
        return FDistribution.of(df1, df2);
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0, 1.0},
            {-0.1, 1.0},
            {1.0, 0.0},
            {1.0, -0.1},
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"NumeratorDegreesOfFreedom", "DenominatorDegreesOfFreedom"};
    }

    @Override
    protected double getRelativeTolerance() {
        return 8e-15;
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testAdditionalMoments() {
        FDistribution dist;

        dist = FDistribution.of(1, 2);
        Assertions.assertEquals(Double.NaN, dist.getMean());
        Assertions.assertEquals(Double.NaN, dist.getVariance());

        dist = FDistribution.of(1, 3);
        Assertions.assertEquals(3d / (3d - 2d), dist.getMean());
        Assertions.assertEquals(Double.NaN, dist.getVariance());

        dist = FDistribution.of(1, 5);
        Assertions.assertEquals(5d / (5d - 2d), dist.getMean());
        Assertions.assertEquals((2d * 5d * 5d * 4d) / 9d, dist.getVariance());
    }

    @Test
    void testLargeDegreesOfFreedom() {
        final double x0 = 0.999;
        final FDistribution fd = FDistribution.of(100000, 100000);
        final double p = fd.cumulativeProbability(x0);
        final double x = fd.inverseCumulativeProbability(p);
        Assertions.assertEquals(x0, x, 1.0e-5);
    }

    @Test
    void testSmallDegreesOfFreedom() {
        final double x0 = 0.975;
        FDistribution fd = FDistribution.of(1, 1);
        double p = fd.cumulativeProbability(x0);
        double x = fd.inverseCumulativeProbability(p);
        Assertions.assertEquals(x0, x, 1.0e-5);

        fd = FDistribution.of(1, 2);
        p = fd.cumulativeProbability(x0);
        x = fd.inverseCumulativeProbability(p);
        Assertions.assertEquals(x0, x, 1.0e-5);
    }

    @Test
    void testMath785() {
        // this test was failing due to inaccurate results from ContinuedFraction.
        final double prob = 0.01;
        final FDistribution f = FDistribution.of(200000, 200000);
        final double result = f.inverseCumulativeProbability(prob);
        Assertions.assertTrue(result < 1.0, "Failing to calculate inverse cumulative probability");
    }

    @Test
    void testAdditionalLogDensity() {
        // Computed using Boost multiprecision to 100 digits (output 25 digits).

        // Edge cases when the standard density is sub-normal or zero.
        final double[] x = new double[] {1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9};
        testLogDensity(FDistribution.of(100, 100), x,
                       new double[] {-56.96014024624318913110565,
                                     -165.8559950938238412964495,
                                     -282.3927517845117162132265,
                                     -399.7346409939330236149964,
                                     -517.157481231596055999464,
                                     -634.5884209792423525846316,
                                     -752.0201707219881824362492,
                                     -869.4520014646850073211334,
                                     -986.883840307381342156051},
                       DoubleTolerances.relative(1e-15));

        testLogDensity(FDistribution.of(952, 912), x,
                       new double[] {-509.5128641158461391223255,
                                     -1485.417858108384337659572,
                                     -2529.705750311339816652123,
                                     -3581.184004620825529681231,
                                     -4633.385040722443349533971,
                                     -5685.658392700035382988623,
                                     -6737.938976642435125691553,
                                     -7790.220283785087985050541,
                                     -8842.501663247803879775318},
                       DoubleTolerances.relative(1e-15));

        // This causes intermediate overflow of the density function
        testLogDensity(FDistribution.of(1e-100, 1),
                       new double[] {1e-200, 1e-250, 1e-300},
                       new double[] {229.5653621188446231302736,
                                     344.6946167685469072592738,
                                     459.8238714182491914891139},
                       DoubleTolerances.relative(1e-15));
    }

    @Test
    void testAdditionalDensity() {
        // Edge cases when the standard density is sub-normal.
        testDensity(FDistribution.of(100, 100),
                    new double[] {1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 3e6, 4e6, 4.5e6, 5e6, 1e7},
                    new double[] {1.830313161302986740491046e-25,
                                  9.325165326363852979476269e-73,
                                  2.282370632180103030176872e-123,
                                  2.49718772086196154389661e-174,
                                  2.51976260334572601372639e-225,
                                  2.522031398014840106819471e-276,
                                  1.171103964711921105069224e-300,
                                  4.97420298008526384736197e-307,
                                  1.224464123468993962344698e-309,
                                  5.679564178845752345371413e-312,
                                  0},
                   DoubleTolerances.relative(3e-13));

        testDensity(FDistribution.of(952, 912),
                    new double[] {10, 11, 12, 13, 14, 15, 16, 17, 18},
                    new double[] {5.264712450643104177155291e-222,
                                  1.083049754753448067375765e-237,
                                  2.996024821196787172008532e-252,
                                  7.919262482129153149257417e-266,
                                  1.511696585130734458293958e-278,
                                  1.652611434344889324846565e-290,
                                  8.522337060963566999523664e-302,
                                  1.760000675560273604454495e-312,
                                  1.266172656954210816606837e-322},
                    DoubleTolerances.relative(2e-13));

        // This causes intermediate overflow of the density function
        testDensity(FDistribution.of(1e-100, 1),
                    new double[] {1e-200, 1e-250, 1e-300},
                    new double[] {5.000000000000000189458187e+99,
                                  4.999999999999999829961813e+149,
                                  4.99999999999999997466404e+199},
                    DoubleTolerances.relative(5e-14));
    }
}
