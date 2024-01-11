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
package org.apache.commons.statistics.descriptive;

/**
 * Support for creating {@link NaNTransformer} implementations.
 *
 * @since 1.1
 */
final class NaNTransformers {

    /** No instances. */
    private NaNTransformers() {}

    /**
     * Creates a {@link NaNTransformer} based on the
     * {@code nanPolicy} and data {@code copy} policy.
     *
     * <p>The transformer is thread-safe.
     *
     * @param nanPolicy NaN policy.
     * @param copy Set to {@code true} to use a copy of the data.
     * @return the transformer
     */
    static NaNTransformer createNaNTransformer(NaNPolicy nanPolicy, boolean copy) {
        if (nanPolicy == NaNPolicy.INCLUDE) {
            return new IncludeNaNTransformer(copy);
        }
        if (nanPolicy == NaNPolicy.EXCLUDE) {
            return new ExcludeNaNTransformer(copy);
        }
        // NaNPolicy.ERROR
        return new ErrorNaNTransformer(copy);
    }

    /**
     * A NaN transformer that optionally copies the data.
     * No NaN processing is done as it is assumed that downstream sorting will
     * move NaN to the end of the data.
     */
    private static final class IncludeNaNTransformer implements NaNTransformer {
        /** Set to {@code true} to use a copy of the data. */
        private final boolean copy;

        /**
         * @param copy Set to {@code true} to use a copy of the data.
         */
        IncludeNaNTransformer(boolean copy) {
            this.copy = copy;
        }

        @Override
        public double[] apply(double[] data, int[] bounds) {
            bounds[0] = data.length;
            if (copy) {
                return data.clone();
            }
            return data;
        }
    }

    /**
     * A transformer that moves {@code NaN} to the upper end of the array.
     */
    private static final class ExcludeNaNTransformer implements NaNTransformer {
        /** Set to {@code true} to use a copy of the data. */
        private final boolean copy;

        /**
         * @param copy Set to {@code true} to use a copy of the data.
         */
        ExcludeNaNTransformer(boolean copy) {
            this.copy = copy;
        }

        @Override
        public double[] apply(double[] data, int[] bounds) {
            // Optionally work on a copy
            final double[] a = copy ? data.clone() : data;
            // Move NaN to end
            int end = a.length;
            for (int i = end; --i >= 0;) {
                final double v = a[i];
                if (v != v) {
                    a[i] = a[--end];
                    a[end] = v;
                }
            }
            // Set the size excluding NaN
            bounds[0] = end;
            return a;
        }
    }

    /**
     * A transformer that errors on {@code NaN}.
     */
    private static final class ErrorNaNTransformer implements NaNTransformer {
        /** Set to {@code true} to use a copy of the data. */
        private final boolean copy;

        /**
         * @param copy Set to {@code true} to use a copy of the data.
         */
        ErrorNaNTransformer(boolean copy) {
            this.copy = copy;
        }

        @Override
        public double[] apply(double[] data, int[] bounds) {
            // Delay copy until data is checked for NaN
            final double[] a = data;
            // Error on NaN
            for (int i = a.length; --i >= 0;) {
                final double v = a[i];
                if (v != v) {
                    throw new IllegalArgumentException("NaN at " + i);
                }
            }
            bounds[0] = a.length;
            // No NaNs so copy the data if required
            if (copy) {
                return data.clone();
            }
            return data;
        }
    }
}
