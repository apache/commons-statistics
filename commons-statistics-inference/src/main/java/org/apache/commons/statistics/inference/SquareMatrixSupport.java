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

/**
 * Provide support for square matrix basic algebraic operations.
 *
 * <p>Matrix element indexing is 0-based e.g. {@code get(0, 0)}
 * returns the element in the first row, first column of the matrix.
 *
 * <p>This class supports computations in the {@link KolmogorovSmirnovTest}.
 *
 * @since 1.1
 */
final class SquareMatrixSupport {
    /**
     * Define a real-valued square matrix.
     *
     * <p>This matrix supports a scale to protect against overflow. The true value
     * of any matrix value is multiplied by {@code 2^scale}. This is readily performed
     * using {@link Math#scalb(double, int)}.
     */
    interface RealSquareMatrix {
        /**
         * Gets the dimension for the rows and columns.
         *
         * @return the dimension
         */
        int dimension();

        /**
         * Gets the scale of the matrix values.
         * The true value is the value returned from {@link #get(int, int)} multiplied by
         * {@code 2^scale}.
         *
         * @return the scale
         */
        int scale();

        /**
         * Gets the value. This is a scaled value. The true value is the value returned
         * multiplied by {@code 2^scale}.
         *
         * @param i Row
         * @param j Column
         * @return the value
         * @see #scale
         */
        double get(int i, int j);

        /**
         * Returns the result of multiplying {@code this} with itself {@code n} times.
         *
         * @param n raise {@code this} to power {@code n}
         * @return {@code this^n}
         * @throws IllegalArgumentException if {@code n < 0}
         */
        RealSquareMatrix power(int n);
    }

    /**
     * Implementation of {@link RealSquareMatrix} using a {@code double[]} array to
     * store entries. Values are addressed using {@code i*dim + j} where {@code dim} is
     * the square dimension.
     *
     * <p>Scaling is supported using the central element {@code [m][m]} where
     * {@code m = dimension/2}. Scaling is only implemented post-multiplication
     * to protect against overflow during repeat multiplication operations.
     *
     * <p>Note: The scaling is implemented to support computation of Kolmogorov's
     * distribution as described in:
     * <ul>
     * <li>
     * Marsaglia, G., Tsang, W. W., &amp; Wang, J. (2003).
     * <a href="https://doi.org/10.18637/jss.v008.i18">Evaluating Kolmogorov's Distribution.</a>
     * Journal of Statistical Software, 8(18), 1–4.</li>
     * </ul>
     */
    private static class ArrayRealSquareMatrix implements RealSquareMatrix {
        /** The scaling threshold. Marsaglia used 1e140. This uses 2^400 ~ 2.58e120 */
        private static final double SCALE_THRESHOLD = 0x1.0p400;
        /** Dimension. */
        private final int dim;
        /** Entries of the matrix. */
        private final double[] data;
        /** Matrix scale. */
        private final int exp;

        /**
         * @param dimension Matrix dimension.
         * @param data Matrix data.
         * @param scale Matrix scale.
         */
        ArrayRealSquareMatrix(int dimension, double[] data, int scale) {
            this.dim = dimension;
            this.data = data;
            this.exp = scale;
        }

        @Override
        public int dimension() {
            return dim;
        }

        @Override
        public int scale() {
            return exp;
        }

        @Override
        public double get(int i, int j) {
            return data[i * dim + j];
        }

        @Override
        public RealSquareMatrix power(int n) {
            checkExponent(n);
            if (n == 0) {
                return identity();
            }
            if (n == 1) {
                return this;
            }

            // Here at least 1 multiplication occurs.
            // Compute the power by repeat squaring and multiplication:
            // 13 = 1101
            // x^13 = x^8 * x^4 * x^1
            //      = ((x^2 * x)^2)^2 * x
            // 21 = 10101
            // x^21 = x^16 * x^4 * x^1
            //      = (((x^2)^2 * x)^2)^2 * x
            // 1. Find highest set bit in n
            // 2. Initialise result as x
            // 3. For remaining bits (0 or 1) below the highest set bit:
            //    - square the current result
            //    - if the current bit is 1 then multiply by x
            // In this scheme we require 2 matrix array allocations and a column array.

            // Working arrays
            final double[] col = new double[dim];
            double[] b = new double[data.length];
            double[] tmp;

            // Initialise result as A^1.
            final double[] a = data;
            final int ea = exp;
            double[] r = a.clone();
            int er = ea;

            // Shift the highest set bit off the top.
            // Any remaining bits are detected in the sign bit.
            final int shift = Integer.numberOfLeadingZeros(n) + 1;
            int bits = n << shift;

            // Process remaining bits below highest set bit.
            for (int i = 32 - shift; i != 0; i--, bits <<= 1) {
                // Square the result
                er = multiply(r, er, r, er, col, b);
                // Recycle working array
                tmp = b;
                b = r;
                r = tmp;
                if (bits < 0) {
                    // Multiply by A
                    er = multiply(r, er, a, ea, col, b);
                    // Recycle working array
                    tmp = b;
                    b = r;
                    r = tmp;
                }
            }

            return new ArrayRealSquareMatrix(dim, r, er);
        }

        /**
         * Creates the identity matrix I with the same dimension as {@code this}.
         *
         * @return I
         */
        private RealSquareMatrix identity() {
            final int n = dimension();
            return new RealSquareMatrix() {
                @Override
                public int dimension() {
                    return n;
                }

                @Override
                public int scale() {
                    return 0;
                }

                @Override
                public double get(int i, int j) {
                    return i == j ? 1 : 0;
                }

                @Override
                public RealSquareMatrix power(int p) {
                    return this;
                }
            };
        }

        /**
         * Returns the result of postmultiplying {@code a} by {@code b}. It is expected
         * the scale of the result will be the sum of the scale of the arguments; this
         * may be adjusted by the scale power if the result is scaled by a power of two
         * for overflow protection.
         *
         * @param a Matrix.
         * @param sa Scale of matrix a.
         * @param b Matrix to postmultiply by.
         * @param sb Scale of matrix b.
         * @param col Working array for a column of the matrix.
         * @param out Output {@code a * b}
         * @return Scale of {@code a * b}
         */
        private static int multiply(double[] a, int sa, double[] b, int sb, double[] col, double[] out) {
            final int m = col.length;
            // Rows are contiguous; Columns are non-contiguous
            int k;
            for (int c = 0; c < m; c++) {
                // Extract column from b to contiguous memory
                k = c;
                for (int i = 0; i < m; i++, k += m) {
                    col[i] = b[k];
                }
                // row * col
                k = 0;
                for (int r = 0; r < m; r++) {
                    double sum = 0;
                    for (int i = 0; i < m; i++, k++) {
                        sum += a[k] * col[i];
                    }
                    out[r * m + c] = sum;
                }
            }
            int s = sa + sb;
            // Overflow protection. Ideally we would check all elements but for speed
            // we check the central one only.
            k = m >> 1;
            if (out[k * m + k] > SCALE_THRESHOLD) {
                // Downscale
                // We could downscale by the inverse of SCALE_THRESHOLD.
                // However this does not account for how far above the threshold
                // the central element is. Here we downscale so the central element
                // is roughly 1 allowing other elements to be larger and still protected
                // from overflow.
                final int exp = Math.getExponent(out[k * m + k]);
                final double downScale = Math.scalb(1.0, -exp);
                s += exp;
                for (int i = 0; i < out.length; i++) {
                    out[i] *= downScale;
                }
            }
            return s;
        }

        /**
         * Check the exponent is not negative.
         *
         * @param p Exponent.
         * @throws IllegalArgumentException if the exponent is negative
         */
        private static void checkExponent(int p) {
            if (p < 0) {
                throw new IllegalArgumentException("Not positive exponent: " + p);
            }
        }
    }

    /** No instances. */
    private SquareMatrixSupport() {}

    /**
     * Creates a square matrix. Data may be used in-place.
     *
     * <p>Values are addressed using {@code a[i][j] = i*dimension + j}.
     *
     * @param dimension Matrix dimension.
     * @param data Matrix data.
     * @return the square matrix
     * @throws IllegalArgumentException if the matrix data is not square (length = dimension * dimension)
     */
    static RealSquareMatrix create(int dimension, double[] data) {
        if (dimension * dimension != data.length) {
            // Note: %<d is 'relative indexing' to re-use the last argument
            throw new IllegalArgumentException(String.format("Not square: %d * %<d != %d", dimension, data.length));
        }
        return new ArrayRealSquareMatrix(dimension, data, 0);
    }
}
