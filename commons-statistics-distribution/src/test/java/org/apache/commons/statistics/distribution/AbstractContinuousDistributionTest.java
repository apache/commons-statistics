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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for AbstractContinuousDistribution default implementations.
 */
class AbstractContinuousDistributionTest {

    /** Various tests related to MATH-699. */
    @Test
    void testContinuous() {
        final double x0 = 0.0;
        final double x1 = 1.0;
        final double x2 = 2.0;
        final double x3 = 3.0;
        final double p12 = 0.5;
        final AbstractContinuousDistribution distribution;
        distribution = new AbstractContinuousDistribution() {
            @Override
            public double cumulativeProbability(final double x) {
                if (x < x0 ||
                    x > x3) {
                    throw new DistributionException(DistributionException.OUT_OF_RANGE, x, x0, x3);
                }
                if (x <= x1) {
                    return p12 * (x - x0) / (x1 - x0);
                } else if (x <= x2) {
                    return p12;
                } else if (x <= x3) {
                    return p12 + (1.0 - p12) * (x - x2) / (x3 - x2);
                }
                return 0.0;
            }

            @Override
            public double density(final double x) {
                if (x < x0 ||
                    x > x3) {
                    throw new DistributionException(DistributionException.OUT_OF_RANGE, x, x0, x3);
                }
                if (x <= x1) {
                    return p12 / (x1 - x0);
                } else if (x <= x2) {
                    return 0.0;
                } else if (x <= x3) {
                    return (1.0 - p12) / (x3 - x2);
                }
                return 0.0;
            }

            @Override
            public double getMean() {
                return ((x0 + x1) * p12 + (x2 + x3) * (1.0 - p12)) / 2.0;
            }

            @Override
            public double getVariance() {
                final double meanX = getMean();
                final double meanX2;
                meanX2 = ((x0 * x0 + x0 * x1 + x1 * x1) * p12 +
                          (x2 * x2 + x2 * x3 + x3 * x3) * (1.0 - p12)) / 3.0;
                return meanX2 - meanX * meanX;
            }

            @Override
            public double getSupportLowerBound() {
                return x0;
            }

            @Override
            public double getSupportUpperBound() {
                return x3;
            }

            @Override
            public boolean isSupportConnected() {
                // This is deliberately false; the functionality is the subject of this test
                return false;
            }
        };
        // CDF is continuous before x1 and after x2. Plateau in between:
        // CDF(x1 <= X <= x2) = p12.
        // The inverse returns the infimum.
        double expected = x1;
        double actual = distribution.inverseCumulativeProbability(p12);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse CDF");

        actual = distribution.inverseSurvivalProbability(1 - p12);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse SF");

        // Test the continuous region
        expected = 0.5 * (x1 - x0);
        actual = distribution.inverseCumulativeProbability(p12 * 0.5);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse CDF");

        actual = distribution.inverseSurvivalProbability(1 - p12 * 0.5);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse SF");

        // Edge case where the result is within the solver accuracy of the lower bound
        expected = x0;
        actual = distribution.inverseCumulativeProbability(Double.MIN_VALUE);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse CDF");

        actual = distribution.inverseSurvivalProbability(Math.nextDown(1.0));
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse SF");
    }

    /** Various tests related to MATH-699. */
    @Test
    void testDiscontinuous() {
        final double x0 = 0.0;
        final double x1 = 0.25;
        final double x2 = 0.5;
        final double x3 = 0.75;
        final double x4 = 1.0;
        final double p12 = 1.0 / 3.0;
        final double p23 = 2.0 / 3.0;
        final AbstractContinuousDistribution distribution;
        distribution = new AbstractContinuousDistribution() {
            @Override
            public double cumulativeProbability(final double x) {
                if (x < x0 ||
                    x > x4) {
                    throw new DistributionException(DistributionException.OUT_OF_RANGE, x, x0, x4);
                }
                if (x <= x1) {
                    return p12 * (x - x0) / (x1 - x0);
                } else if (x <= x2) {
                    return p12;
                } else if (x <= x3) {
                    return p23;
                } else {
                    return (1.0 - p23) * (x - x3) / (x4 - x3) + p23;
                }
            }

            @Override
            public double density(final double x) {
                if (x < x0 ||
                    x > x4) {
                    throw new DistributionException(DistributionException.OUT_OF_RANGE, x, x0, x4);
                }
                if (x <= x1) {
                    return p12 / (x1 - x0);
                } else if (x <= x2) {
                    return 0.0;
                } else if (x <= x3) {
                    return 0.0;
                } else {
                    return (1.0 - p23) / (x4 - x3);
                }
            }

            @Override
            public double getMean() {
                final UnivariateFunction f = x -> x * density(x);
                final UnivariateIntegrator integrator = new RombergIntegrator();
                return integrator.integrate(Integer.MAX_VALUE, f, x0, x4);
            }

            @Override
            public double getVariance() {
                final double meanX = getMean();
                final UnivariateFunction f = x -> x * x * density(x);
                final UnivariateIntegrator integrator = new RombergIntegrator();
                final double meanX2 = integrator.integrate(Integer.MAX_VALUE,
                                                           f, x0, x4);
                return meanX2 - meanX * meanX;
            }

            @Override
            public double getSupportLowerBound() {
                return x0;
            }

            @Override
            public double getSupportUpperBound() {
                return x4;
            }

            @Override
            public boolean isSupportConnected() {
                // This is deliberately false; the functionality is the subject of this test
                return false;
            }
        };
        // CDF continuous before x1 and after x3. Two plateuas in between stepped at x2:
        // CDF(x1 <= X <= x2) = p12.
        // CDF(x2 <= X <= x3) = p23. The inverse returns the infimum.
        double expected = x2;
        double actual = distribution.inverseCumulativeProbability(p23);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse CDF");

        actual = distribution.inverseSurvivalProbability(1 - p23);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse SF");

        // Test the continuous region
        expected = 0.5 * (x1 - x0);
        actual = distribution.inverseCumulativeProbability(p12 * 0.5);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse CDF");

        actual = distribution.inverseSurvivalProbability(1 - p12 * 0.5);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse SF");

        // Edge case where the result is within the solver accuracy of the lower bound
        expected = x0;
        actual = distribution.inverseCumulativeProbability(Double.MIN_VALUE);
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse CDF");

        actual = distribution.inverseSurvivalProbability(Math.nextDown(1.0));
        Assertions.assertEquals(expected, actual, 1e-8, "Inverse SF");
    }

    /**
     * Test zero variance. This invalidates the Chebyshev inequality. If the mean is at
     * one bound and the other bound is infinite then the inequality sets the other bound
     * to the mean. This results in no bracket for the solver.
     *
     * <p>If the distribution is reporting the variance incorrectly then options are
     * to throw an exception, or safely fall back to manual bracketing. This test verifies
     * the solver reverts to manual bracketing and raises no exception.
     */
    @Test
    void testZeroVariance() {
        // Distribution must have an infinite bound but no variance.
        // This is an invalid case for the Chebyshev inequality.
        // E.g. It may occur in the Pareto distribution as it approaches a Dirac function.

        // Create a Dirac function at x=10
        final double x0 = 10.0;
        final AbstractContinuousDistribution distribution;
        distribution = new AbstractContinuousDistribution() {
            @Override
            public double cumulativeProbability(final double x) {
                return x <= x0 ? 0.0 : 1.0;
            }

            @Override
            public double density(final double x) {
                throw new AssertionError();
            }

            @Override
            public double getMean() {
                return x0;
            }

            @Override
            public double getVariance() {
                return 0.0;
            }

            @Override
            public double getSupportLowerBound() {
                return x0;
            }

            @Override
            public double getSupportUpperBound() {
                return Double.POSITIVE_INFINITY;
            }
        };
        double x = distribution.inverseCumulativeProbability(0.5);
        // The value can be anything other than x0
        Assertions.assertNotEquals(x0, x, "Inverse CDF");
        // Ideally it would be the next value after x0 but accuracy is dependent
        // on the tolerance of the solver
        Assertions.assertEquals(x0, x, 1e-8, "Inverse CDF");

        // The same functionality should be supported for the inverse survival probability
        x = distribution.inverseSurvivalProbability(0.5);
        Assertions.assertNotEquals(x0, x, "Inverse SF");
        Assertions.assertEquals(x0, x, 1e-8, "Inverse SF");
    }

    /**
     * Test infinite variance. This invalidates the Chebyshev inequality.
     *
     * <p>This test verifies the solver reverts to manual bracketing and raises no exception.
     */
    @Test
    void testInfiniteVariance() {
        // Distribution must have an infinite bound and infinite variance.
        // This is an invalid case for the Chebyshev inequality.

        // Create a triangle distribution: (a, c, b); a=lower, c=mode, b=upper
        // (-10, 0, 10)
        // Area of the first triangle [-10, 0] is set assuming the height is 10
        // => 10 * 10 * 2 / 2 = 100
        // Length of triangle to achieve half the area:
        // x = sqrt(50) = 7.07..

        final AbstractContinuousDistribution distribution;
        distribution = new AbstractContinuousDistribution() {
            @Override
            public double cumulativeProbability(final double x) {
                if (x > 0) {
                    // Use symmetry for the upper triangle
                    return 1 - cumulativeProbability(-x);
                }
                if (x < -10) {
                    return 0;
                }
                return Math.pow(x + 10, 2) / 200;
            }

            @Override
            public double density(final double x) {
                throw new AssertionError();
            }

            @Override
            public double getMean() {
                return 0;
            }

            @Override
            public double getVariance() {
                // Report variance incorrectly
                return Double.POSITIVE_INFINITY;
            }

            @Override
            public double getSupportLowerBound() {
                // Report lower bound incorrectly (it should be -10) to test cdf(0)
                return Double.NEGATIVE_INFINITY;
            }

            @Override
            public double getSupportUpperBound() {
                // Report upper bound incorrectly (it should be 10) to test cdf(1)
                return Double.POSITIVE_INFINITY;
            }
        };

        // Accuracy is dependent on the tolerance of the solver
        final double tolerance = 1e-8;

        final double x = Math.sqrt(50);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, distribution.inverseCumulativeProbability(0), "Inverse CDF");
        Assertions.assertEquals(x - 10, distribution.inverseCumulativeProbability(0.25), tolerance, "Inverse CDF");
        Assertions.assertEquals(0, distribution.inverseCumulativeProbability(0.5), tolerance, "Inverse CDF");
        Assertions.assertEquals(10 - x, distribution.inverseCumulativeProbability(0.75), tolerance, "Inverse CDF");
        Assertions.assertEquals(Double.POSITIVE_INFINITY, distribution.inverseCumulativeProbability(1), "Inverse CDF");

        // The same functionality should be supported for the inverse survival probability
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, distribution.inverseSurvivalProbability(1), "Inverse CDF");
        Assertions.assertEquals(x - 10, distribution.inverseSurvivalProbability(0.75), tolerance, "Inverse SF");
        Assertions.assertEquals(0, distribution.inverseSurvivalProbability(0.5), tolerance, "Inverse SF");
        Assertions.assertEquals(10 - x, distribution.inverseSurvivalProbability(0.25), tolerance, "Inverse SF");
        Assertions.assertEquals(Double.POSITIVE_INFINITY, distribution.inverseSurvivalProbability(0), "Inverse CDF");
    }

    /**
     * Create a distribution near positive infinity so that it is truncated by MAX_VALUE.
     */
    @Test
    void testTruncatedDistributionAtPositiveInfinity() {
        final double mean = Double.MAX_VALUE;
        final double width = Double.MAX_VALUE / 2;
        final CentredUniformDistribution dist = new CentredUniformDistribution(mean, width);
        final double x = mean - width / 2;
        Assertions.assertEquals(0, dist.cumulativeProbability(x));
        Assertions.assertTrue(dist.cumulativeProbability(Math.nextUp(x)) > 0);
        Assertions.assertEquals(0.25, dist.cumulativeProbability(mean - width / 4));
        Assertions.assertEquals(0.5, dist.cumulativeProbability(mean));

        // Truncated
        Assertions.assertEquals(1.0, dist.cumulativeProbability(Math.nextUp(mean)));

        // Inversion should be robust to return the upper infinite bound
        Assertions.assertEquals(mean, dist.inverseCumulativeProbability(0.5));
        Assertions.assertEquals(mean, dist.inverseSurvivalProbability(0.5));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.inverseCumulativeProbability(0.75));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, dist.inverseSurvivalProbability(0.25));
    }

    /**
     * Create a distribution near negative infinity so that it is truncated by -MAX_VALUE.
     */
    @Test
    void testTruncatedDistributionAtNegativeInfinity() {
        final double mean = -Double.MAX_VALUE;
        final double width = Double.MAX_VALUE / 2;
        final CentredUniformDistribution dist = new CentredUniformDistribution(mean, width);
        final double x = mean + width / 2;
        Assertions.assertEquals(1, dist.cumulativeProbability(x));
        Assertions.assertTrue(dist.cumulativeProbability(Math.nextDown(x)) < 1);
        Assertions.assertEquals(0.75, dist.cumulativeProbability(mean + width / 4));
        Assertions.assertEquals(0.5, dist.cumulativeProbability(mean));

        // Truncated
        Assertions.assertEquals(0.0, dist.cumulativeProbability(Math.nextDown(mean)));

        // Inversion should be robust to return the lower infinite bound
        Assertions.assertEquals(mean, dist.inverseCumulativeProbability(0.5));
        Assertions.assertEquals(mean, dist.inverseSurvivalProbability(0.5));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.inverseCumulativeProbability(0.25));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.inverseSurvivalProbability(0.75));
    }

    /**
     * Uniform distribution described with a centre and width.
     * This can be use to place the centre of the distribution at the limit of a finite double
     * value so that the distribution is truncated.
     */
    static final class CentredUniformDistribution extends AbstractContinuousDistribution {
        private final double mean;
        private final double width;
        private final double density;
        private final double lo;
        private final double hi;

        /**
         * @param mean Mean
         * @param width Width
         */
        CentredUniformDistribution(double mean, double width) {
            this.mean = mean;
            this.width = width;
            density = 1 / width;
            lo = mean - width / 2;
            hi = mean + width / 2;
        }

        @Override
        public double density(double x) {
            return density;
        }

        @Override
        public double cumulativeProbability(double x) {
            if (x <= lo) {
                return 0;
            }
            if (x >= hi) {
                return 1;
            }
            return 0.5 + density * (x - mean);
        }

        @Override
        public double getMean() {
            return mean;
        }

        @Override
        public double getVariance() {
            return width * width / 12;
        }

        @Override
        public double getSupportLowerBound() {
            return lo;
        }

        @Override
        public double getSupportUpperBound() {
            return hi;
        }
    }
}
