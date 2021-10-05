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
    void testEquals0Ulp() {
        final DoubleTolerance tol = DoubleTolerances.equals(0);
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
    void testEquals1Ulp() {
        final DoubleTolerance tol = DoubleTolerances.equals(1);
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
        final DoubleTolerance t2 = DoubleTolerances.equals(1);
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
}
