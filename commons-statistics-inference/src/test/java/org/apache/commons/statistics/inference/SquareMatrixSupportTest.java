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
package org.apache.commons.statistics.inference;

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.inference.SquareMatrixSupport.RealSquareMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link SquareMatrixSupport}.
 */
class SquareMatrixSupportTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void testCreateRealSquareMatrix(int n) {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> SquareMatrixSupport.create(n, new double[n * n + 1]));

        final double[] a = RandomSource.SPLIT_MIX_64.create().doubles(n * n).toArray();
        final RealSquareMatrix b = SquareMatrixSupport.create(n, a);
        Assertions.assertEquals(n, b.dimension(), "dimension");

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final double v = a[i * n + j];
                Assertions.assertEquals(v, b.get(i, j), "value mismatch");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void testPowerZero(int n) {
        final double[] a = RandomSource.SPLIT_MIX_64.create().doubles(n * n).toArray();
        final RealSquareMatrix b = SquareMatrixSupport.create(n, a);
        final RealSquareMatrix r = b.power(0);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Assertions.assertEquals(i == j ? 1 : 0, r.get(i, j), "value mismatch");
            }
        }
        Assertions.assertSame(r, r.power(0));
        Assertions.assertSame(r, r.power(1));
        Assertions.assertSame(r, r.power(2));
        Assertions.assertSame(r, r.power(123));
    }

    @ParameterizedTest
    @MethodSource
    void testRealSquareMatrixPower(double[][] a, int p) {
        // commons-math3 implementation
        final RealMatrix a1 = MatrixUtils.createRealMatrix(a);
        final RealMatrix r1 = a1.power(p);
        final double[] b = Arrays.stream(a).flatMapToDouble(Arrays::stream).toArray();
        final RealSquareMatrix a2 = SquareMatrixSupport.create(a.length, b);
        final RealSquareMatrix r2 = a2.power(p);
        // Note: CM3 starts with x^1 and multiplies by higher powers first.
        // This implementation starts with x^1 and squares and optionally multiplies by x^1.
        // e.g.
        // CM3 : x^27 = x^1 * x^16 * x^8 * x^2
        // CM3 : x^8  = x^1 * x^4 * x^2 * x^1
        // this: x^27 = (((x^2 * x^1)^2)^2 * x^1)^2 * x^1
        // this: x^8  = ((x^2)^2)^2
        // Thus there can be differences, but only when computing above x^3.
        // This implementation has fewer multiplications for powers of 2.
        assertEquals(r1, r2, p > 3 ? 2 : 0);
    }

    static Stream<Arguments> testRealSquareMatrixPower() {
        final double[][] a = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9},
        };
        final double[][] b = {
            {-5, 3, -6},
            {-1, -2, 3},
            {-9, 8, 7},
        };
        return Stream.of(
            Arguments.of(a, 0),
            Arguments.of(b, 0),
            Arguments.of(a, 1),
            Arguments.of(b, 1),
            Arguments.of(a, 2),
            Arguments.of(b, 2),
            Arguments.of(a, 3),
            Arguments.of(b, 3),
            Arguments.of(a, 4),
            Arguments.of(b, 4),
            Arguments.of(a, 5),
            Arguments.of(b, 5),
            Arguments.of(a, 6),
            Arguments.of(b, 6),
            Arguments.of(a, 13),
            Arguments.of(b, 13),
            Arguments.of(a, 27)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void testRealSquareMatrixPowerThrows(int p) {
        final RealSquareMatrix a = SquareMatrixSupport.create(2, new double[4]);
        Assertions.assertThrows(IllegalArgumentException.class, () -> a.power(p));
    }

    /**
     * Assert the matrices are equal.
     *
     * @param a First matrix
     * @param b Second matrix
     * @param ulp Allowed ulp tolerance
     */
    private static void assertEquals(final RealMatrix a, final RealSquareMatrix b, int ulp) {
        final int n = b.dimension();
        final int[] indices = {0, 0};
        for (int i = 0; i < n; i++) {
            indices[0] = i;
            for (int j = 0; j < n; j++) {
                indices[1] = j;
                final double v1 = a.getEntry(i, j);
                final double v2 = b.get(i, j);
                if (v1 != v2) {
                    Assertions.assertEquals(v1, v2, Math.ulp(v1) * ulp,
                        () -> String.format("[%d][%d] ulp error %d", indices[0], indices[1],
                            Double.doubleToRawLongBits(v2) - Double.doubleToRawLongBits(v1)));
                }
            }
        }
    }

    /**
     * Test the power function using a scaled matrix that would overflow without scaling support.
     * The final scale of the matrix is asserted against the expected scale if the floating-point
     * numbers had an unlimited precision exponent.
     */
    @ParameterizedTest
    @MethodSource
    void testRealSquareMatrixPowerWithScale(double[] a, int p, int n) {
        // Test against an unscaled implementation
        final int dim = (int)Math.sqrt(a.length);
        final RealSquareMatrix a1 = SquareMatrixSupport.create(dim, a);
        final RealSquareMatrix r1 = a1.power(p);
        // Scale
        final double s = Math.scalb(1.0, n);
        final double[] b = Arrays.stream(a).map(x -> x * s).toArray();
        final RealSquareMatrix a2 = SquareMatrixSupport.create(dim, b);
        final RealSquareMatrix r2 = a2.power(p);

        // Final expected exponent (if exponent bits were unlimited)
        final int pn = p * n;
        final int scale1 = r1.scale();
        final int scale2 = r2.scale();
        final int m = r2.dimension();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                final double x = r1.get(i, j);
                final double y = r2.get(i, j);
                // Raw bits without the exponent (but including the sign)
                final long bx = Double.doubleToRawLongBits(x) & 0x800f_ffff_ffff_ffffL;
                final long by = Double.doubleToRawLongBits(y) & 0x800f_ffff_ffff_ffffL;
                Assertions.assertEquals(bx, by, "Incorrect bits");
                // The exponent of the scaled result must include the 'scale'.
                // It should be different by the expected exponent of a floating-point
                // result with unlimited exponent bits.
                final int ex = Math.getExponent(x) + scale1 + pn;
                final int ey = Math.getExponent(y) + scale2;
                Assertions.assertEquals(ex, ey, "Incorrect scale");
            }
        }
    }

    static Stream<Arguments> testRealSquareMatrixPowerWithScale() {
        final double[] a = {
            1, 2, 3,
            4, 5, 6,
            7, 8, 9,
        };
        final double[] b = {
            -5, 3, -6,
            -1, -2, 3,
            -9, 8, 7,
        };
        return Stream.of(
            // power of zero will create an identity matrix. Any initial scaling is valid.
            Arguments.of(a, 0, 679),
            Arguments.of(b, 0, 1000),
            // Initial power 2^n must not create a matrix that will overflow when
            // multiplied by itself. n is limited to <= 511 for (1.0*2^n)^2. For the matrix
            // entries the limit is lower as they are above 1 and products are summed.
            // Also the overflow protection only uses the central element of the product
            // matrix (i.e. it does not check all elements).
            // Note: Only test overflow (n>=0) as the implementation does not protect underflow.
            Arguments.of(a, 1, 123),
            Arguments.of(b, 1, 117),
            Arguments.of(a, 2, 500),
            Arguments.of(b, 2, 489),
            Arguments.of(a, 3, 434),
            Arguments.of(b, 3, 312),
            Arguments.of(a, 13, 67),
            Arguments.of(b, 13, 89),
            Arguments.of(a, 27, 433)
        );
    }
}
