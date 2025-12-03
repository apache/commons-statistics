/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link DoubleTolerance} instances create by {@link DoubleTolerances}.
 */
class DoubleTolerancesTest  {
    @Test
    void testEquals() {
        final DoubleTolerance tol = DoubleTolerances.equals();
        Assertions.assertEquals("exact", StringUtils.toString(tol));
        // Standard tests
        Assertions.assertTrue(tol.test(1.5, 1.5));
        Assertions.assertFalse(tol.test(1.5, 1.51));
        Assertions.assertFalse(tol.test(1.5, 1.50001));
        Assertions.assertFalse(tol.test(1.5, Math.nextDown(1.5)));
        Assertions.assertFalse(tol.test(0.0, -0.0));
        Assertions.assertFalse(tol.test(0.0, Double.MIN_VALUE));
        Assertions.assertFalse(tol.test(-Double.MIN_VALUE, Double.MIN_VALUE));
        Assertions.assertTrue(tol.test(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Assertions.assertFalse(tol.test(Double.MAX_VALUE, Double.POSITIVE_INFINITY));
        Assertions.assertFalse(tol.test(Double.MAX_VALUE, Math.nextDown(Double.MAX_VALUE)));
        Assertions.assertFalse(tol.test(1.0, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(tol.test(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(tol.test(-1.0, Double.NEGATIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NaN, Double.NaN));
    }

    /**
     * This should match {@link #testEquals()} with the exception that {@code -0.0 == 0.0}.
     */
    @Test
    void testUlps0() {
        final DoubleTolerance tol = DoubleTolerances.ulps(0);
        Assertions.assertEquals("ulp=0", StringUtils.toString(tol));
        // Standard tests
        Assertions.assertTrue(tol.test(1.5, 1.5));
        Assertions.assertFalse(tol.test(1.5, 1.51));
        Assertions.assertFalse(tol.test(1.5, 1.50001));
        Assertions.assertFalse(tol.test(1.5, Math.nextDown(1.5)));
        // Allow zero equality
        Assertions.assertTrue(tol.test(0.0, -0.0));
        Assertions.assertFalse(tol.test(0.0, Double.MIN_VALUE));
        Assertions.assertFalse(tol.test(-Double.MIN_VALUE, Double.MIN_VALUE));
        Assertions.assertTrue(tol.test(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Assertions.assertFalse(tol.test(Double.MAX_VALUE, Double.POSITIVE_INFINITY));
        Assertions.assertFalse(tol.test(Double.MAX_VALUE, Math.nextDown(Double.MAX_VALUE)));
        Assertions.assertFalse(tol.test(1.0, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(tol.test(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(tol.test(-1.0, Double.NEGATIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NaN, Double.NaN));
    }

    @Test
    void testUlps1() {
        final DoubleTolerance tol = DoubleTolerances.ulps(1);
        Assertions.assertEquals("ulp=1", StringUtils.toString(tol));
        // Standard tests
        Assertions.assertTrue(tol.test(1.5, 1.5));
        Assertions.assertFalse(tol.test(1.5, 1.51));
        Assertions.assertFalse(tol.test(1.5, 1.501));
        Assertions.assertFalse(tol.test(15, 15.01));
        Assertions.assertTrue(tol.test(1.5, Math.nextDown(1.5)));
        Assertions.assertTrue(tol.test(0.0, -0.0));
        Assertions.assertTrue(tol.test(0.0, Double.MIN_VALUE));
        Assertions.assertFalse(tol.test(-Double.MIN_VALUE, Double.MIN_VALUE));
        Assertions.assertTrue(tol.test(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.MAX_VALUE, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.MAX_VALUE, Math.nextDown(Double.MAX_VALUE)));
        Assertions.assertFalse(tol.test(Double.MAX_VALUE, Math.nextDown(Math.nextDown(Double.MAX_VALUE))));
        Assertions.assertFalse(tol.test(1.0, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        Assertions.assertTrue(tol.test(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(tol.test(-1.0, Double.NEGATIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NaN, Double.NaN));
    }

    @Test
    void testAbsolute() {
        final double eps = 1e-3;
        final DoubleTolerance tol = DoubleTolerances.absolute(eps);
        Assertions.assertEquals("abs=" + eps, StringUtils.toString(tol));
        // Standard tests
        Assertions.assertTrue(tol.test(1.5, 1.5));
        Assertions.assertFalse(tol.test(1.5, 1.51));
        Assertions.assertTrue(tol.test(1.5, 1.501));
        // Not equal absolutely
        Assertions.assertFalse(tol.test(15, 15.01));
        Assertions.assertTrue(tol.test(1.5, Math.nextDown(1.5)));
        Assertions.assertTrue(tol.test(0.0, -0.0));
        Assertions.assertTrue(tol.test(0.0, Double.MIN_VALUE));
        Assertions.assertTrue(tol.test(-Double.MIN_VALUE, Double.MIN_VALUE));
        Assertions.assertTrue(tol.test(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.MAX_VALUE, Double.POSITIVE_INFINITY));
        // 1 ULP is OK
        Assertions.assertTrue(tol.test(Double.MAX_VALUE, Math.nextDown(Double.MAX_VALUE)));
        // Not equal as absolute difference is large
        Assertions.assertFalse(tol.test(Double.MAX_VALUE, Math.nextDown(Math.nextDown(Double.MAX_VALUE))));
        Assertions.assertFalse(tol.test(1.0, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        Assertions.assertTrue(tol.test(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(tol.test(-1.0, Double.NEGATIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NaN, Double.NaN));
    }

    @Test
    void testRelative() {
        final double eps = 1e-3;
        final DoubleTolerance tol = DoubleTolerances.relative(eps);
        Assertions.assertEquals("rel=" + eps, StringUtils.toString(tol));
        // Standard tests
        Assertions.assertTrue(tol.test(1.5, 1.5));
        Assertions.assertFalse(tol.test(1.5, 1.51));
        Assertions.assertTrue(tol.test(1.5, 1.501));
        // Equal relatively
        Assertions.assertTrue(tol.test(0.15, 0.1501));
        Assertions.assertTrue(tol.test(1.5, Math.nextDown(1.5)));
        Assertions.assertTrue(tol.test(0.0, -0.0));
        Assertions.assertTrue(tol.test(0.0, Double.MIN_VALUE));
        // Not equals relatively: due to the sign change the relative difference is big
        Assertions.assertFalse(tol.test(-Double.MIN_VALUE, Double.MIN_VALUE));
        Assertions.assertTrue(tol.test(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.MAX_VALUE, Double.POSITIVE_INFINITY));
        // Equal as relative difference is small
        Assertions.assertTrue(tol.test(Double.MAX_VALUE, Math.nextDown(Double.MAX_VALUE)));
        Assertions.assertTrue(tol.test(Double.MAX_VALUE, Math.nextDown(Math.nextDown(Double.MAX_VALUE))));
        Assertions.assertFalse(tol.test(1.0, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(tol.test(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        Assertions.assertTrue(tol.test(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(tol.test(-1.0, Double.NEGATIVE_INFINITY));
        // Relative error does not check NaN are equal
        Assertions.assertFalse(tol.test(Double.NaN, Double.NaN));
    }

    @Test
    void testMessageFormatting() {
        final DoubleTolerance t1 = DoubleTolerances.equals();
        final DoubleTolerance t2 = DoubleTolerances.ulps(1);
        final DoubleTolerance t3 = DoubleTolerances.absolute(1e-3);
        final DoubleTolerance t4 = DoubleTolerances.relative(1e-4);
        final String s1 = StringUtils.toString(t1);
        final String s2 = StringUtils.toString(t2);
        final String s3 = StringUtils.toString(t3);
        final String s4 = StringUtils.toString(t4);

        // Combination of tolerance descriptions adds parentheses when operators are different.
        // This could be made smarter using operator precedence but it is assumed
        // that not many tolerance objects will be concatenated.
        Assertions.assertEquals(s1 + " || " + s2, StringUtils.toString(t1.or(t2)));
        Assertions.assertEquals(s2 + " || " + s1, StringUtils.toString(t2.or(t1)));
        Assertions.assertEquals(s3 + " || " + s4, StringUtils.toString(t3.or(t4)));
        Assertions.assertEquals(s3 + " && " + s4, StringUtils.toString(t3.and(t4)));
        Assertions.assertEquals(s3 + " && " + s4 + " && " + s1,
            StringUtils.toString(t3.and(t4).and(t1)));
        Assertions.assertEquals(s3 + " || " + s4 + " || " + s1,
            StringUtils.toString(t3.or(t4).or(t1)));
        Assertions.assertEquals("(" + s3 + " && " + s4 + ") || " + s1,
            StringUtils.toString(t3.and(t4).or(t1)));
        Assertions.assertEquals("(" + s3 + " && " + s4 + ") || (" + s1 + " && " + s2 + ")",
            StringUtils.toString(t3.and(t4).or(t1.and(t2))));
        Assertions.assertEquals(s3 + " && (" + s1 + " || " + s2 + ")",
            StringUtils.toString(t3.and(t1.or(t2))));

        // Negation
        Assertions.assertEquals("!(" + s1 + ")", StringUtils.toString(t1.negate()));
        Assertions.assertEquals(s1, StringUtils.toString(t1.negate().negate()));
        Assertions.assertEquals(s1 + " || !(" + s2 + ")", StringUtils.toString(t1.or(t2.negate())));
        Assertions.assertEquals("!(" + s1 + " || " + s2 + ")", StringUtils.toString(t1.or(t2).negate()));
    }

    @Test
    void testCombinations() {
        final DoubleTolerance eq = DoubleTolerances.equals();
        final DoubleTolerance ulp = DoubleTolerances.ulps(2);
        final DoubleTolerance abs = DoubleTolerances.absolute(1e-3);
        final DoubleTolerance rel = DoubleTolerances.relative(1e-6);

        // a,b are absolutely equal
        // c,d are absolutely and ulp equal
        // e,f are relatively equal
        double a = 0.12345;
        double b = 0.123456;
        double c = Double.MIN_VALUE;
        double d = -c;
        double e = 12345.67;
        double f = 12345.678;

        // Verify the test conditions
        Assertions.assertTrue(eq.test(a, a));
        Assertions.assertTrue(ulp.test(a, a));
        Assertions.assertTrue(abs.test(a, a));
        Assertions.assertTrue(abs.test(a, b));
        Assertions.assertFalse(eq.negate().test(a, a));
        Assertions.assertFalse(ulp.negate().test(a, a));
        Assertions.assertFalse(abs.negate().test(a, a));
        Assertions.assertFalse(abs.negate().test(a, b));

        Assertions.assertFalse(eq.test(a, b));
        Assertions.assertFalse(ulp.test(a, b));
        Assertions.assertTrue(abs.test(a, b), "absolute");
        Assertions.assertFalse(rel.test(a, b));

        Assertions.assertTrue(eq.test(c, c));
        Assertions.assertTrue(ulp.test(c, c));
        Assertions.assertTrue(abs.test(c, c));
        Assertions.assertTrue(abs.test(c, c));

        Assertions.assertFalse(eq.test(c, d));
        Assertions.assertTrue(ulp.test(c, d), "ulp");
        Assertions.assertTrue(abs.test(c, d), "absolute");
        Assertions.assertFalse(rel.test(c, d));

        Assertions.assertTrue(eq.test(e, e));
        Assertions.assertTrue(ulp.test(e, e));
        Assertions.assertTrue(abs.test(e, e));
        Assertions.assertTrue(abs.test(e, e));

        Assertions.assertFalse(eq.test(e, f));
        Assertions.assertFalse(ulp.test(e, f));
        Assertions.assertFalse(abs.test(e, f));
        Assertions.assertTrue(rel.test(e, f), "relative");


        // Test and/or
        final DoubleTolerance[] tol = {eq, ulp, abs, rel};
        final double[][] pairs = {{a, b}, {c, d}, {e, f}};
        for (final double[] pair : pairs) {
            final double x = pair[0];
            final double y = pair[1];
            for (final DoubleTolerance t1 : tol) {
                final boolean ex1 = t1.test(x, y);
                for (final DoubleTolerance t2 : tol) {
                    final boolean ex2 = t2.test(x, y);

                    // Note:
                    // We can use %s for the error message as the
                    // custom DoubleTolerance overrides toString()
                    Assertions.assertEquals(ex1 || ex2, t1.or(t2).test(x, y),
                        () -> String.format("%s or %s test (%s, %s", t1, t2, x, y));
                    Assertions.assertEquals(ex1 && ex2, t1.and(t2).test(x, y),
                        () -> String.format("%s and %s test (%s, %s", t1, t2, x, y));

                    for (final DoubleTolerance t3 : tol) {
                        final boolean ex3 = t3.test(x, y);
                        Assertions.assertEquals((ex1 || ex2) && ex3, t1.or(t2).and(t3).test(x, y),
                            () -> String.format("(%s or %s) and %s test (%s, %s", t1, t2, t3, x, y));
                        Assertions.assertEquals((ex1 || ex2) || ex3, t1.or(t2).or(t3).test(x, y),
                            () -> String.format("(%s or %s) or %s test (%s, %s", t1, t2, t3, x, y));
                        Assertions.assertEquals((ex1 && ex2) && ex3, t1.and(t2).and(t3).test(x, y),
                            () -> String.format("(%s and %s) and %s test (%s, %s", t1, t2, t3, x, y));
                        Assertions.assertEquals((ex1 && ex2) || ex3, t1.and(t2).or(t3).test(x, y),
                            () -> String.format("(%s and %s) or %s test (%s, %s", t1, t2, t3, x, y));

                        Assertions.assertEquals(ex1 || (ex2 && ex3), t1.or(t2.and(t3)).test(x, y),
                            () -> String.format("%s or (%s and %s) test (%s, %s", t1, t2, t3, x, y));
                        Assertions.assertEquals(ex1 || (ex2 || ex3), t1.or(t2.or(t3)).test(x, y),
                            () -> String.format("%s or (%s or %s) test (%s, %s", t1, t2, t3, x, y));
                        Assertions.assertEquals(ex1 && (ex2 && ex3), t1.and(t2.and(t3)).test(x, y),
                            () -> String.format("%s and (%s and %s) test (%s, %s", t1, t2, t3, x, y));
                        Assertions.assertEquals(ex1 && (ex2 || ex3), t1.and(t2.or(t3)).test(x, y),
                            () -> String.format("%s and (%s or %s) test (%s, %s", t1, t2, t3, x, y));
                    }
                }
            }
        }
    }

    @Test
    void testSymmetry() {
        final int maxUlps = 4;
        final double absTolerance = 1e-3;
        final double relTolerance = 1e-6;
        final DoubleTolerance eq = DoubleTolerances.equals();
        final DoubleTolerance ulp = DoubleTolerances.ulps(maxUlps);
        final DoubleTolerance abs = DoubleTolerances.absolute(absTolerance);
        final DoubleTolerance rel = DoubleTolerances.relative(relTolerance);
        final UniformRandomProvider rng = RandomSource.JSF_64.create();
        final long one = Double.doubleToLongBits(1.0);
        // Test a range of values that cover true/false cases for each tolerance
        for (final int exp : new int[] {
            // Include sub-normal and small numbers
            -1074, -1073, -1066, -1022, -567, -123, -20,
            // Realistic scales for probabilities: 1e-8 to 1e-1
            -8, -7, -6, -5, -4, -3, -2, -1,
            // Ranges > 1
            0, 1, 2, 3, 4, 8, 156, 1022, 1023
        }) {
            final double scale = Math.scalb(1.0, exp);
            for (int i = 0; i < 50; i++) {
                // A random number in [1, 2).
                // Combine the exponent from 1.0 with 52 bits in the mantissa
                final double u = Double.longBitsToDouble(one | (rng.nextLong() >>> 12));
                Assertions.assertTrue(u >= 1.0 && u < 2.0, "Unexpected range");
                // Scale
                final double x = u * scale;
                Assertions.assertTrue(x > 0 && x <= Double.MAX_VALUE, () -> "Unexpected scaled number: " + x);
                // Create variants that pass and fail each tolerance test.
                // Note: Failure of the absolute tolerance is dependent on the scale.
                final double dx1 = Math.ulp(x) * maxUlps;
                final double dx2 = absTolerance;
                final double dx3 = x * relTolerance;
                final double[] ys = {
                    x,
                    x + dx1 * 0.5, x + dx1, x + dx1 * 2, x - dx1 * 0.5, x - dx1, x - dx1 * 2,
                    x + dx2 * 0.5, x + dx2, x + dx2 * 2, x - dx2 * 0.5, x - dx2, x - dx2 * 2,
                    x + dx3 * 0.5, x + dx3, x + dx3 * 2, x - dx3 * 0.5, x - dx3, x - dx3 * 2,
                };
                for (final double y : ys) {
                    Assertions.assertEquals(eq.test(x, y), eq.test(y, x), "eq");
                    Assertions.assertEquals(ulp.test(x, y), ulp.test(y, x), "ulp");
                    Assertions.assertEquals(abs.test(x, y), abs.test(y, x), "abs");
                    Assertions.assertEquals(rel.test(x, y), rel.test(y, x), "rel");
                    // Support negative values
                    Assertions.assertEquals(eq.test(x, y), eq.test(-y, -x), "eq");
                    Assertions.assertEquals(ulp.test(x, y), ulp.test(-y, -x), "ulp");
                    Assertions.assertEquals(abs.test(x, y), abs.test(-y, -x), "abs");
                    Assertions.assertEquals(rel.test(x, y), rel.test(-y, -x), "rel");
                }
            }
        }
    }
}
