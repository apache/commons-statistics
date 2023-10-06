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

import java.util.Objects;
import java.util.function.Supplier;
import org.apache.commons.numbers.core.Precision;

/**
 * Creates instances of {@link DoubleTolerance}.
 *
 * <p>Floating-point comparisons are based on
 * {@link Precision org.apache.commons.numbers.core.Precision}.
 *
 * <p>This class is public and has public methods to allow testing within the other modules.
 */
public final class DoubleTolerances {
    /** A tolerance for numerical equality. This is immutable. */
    private static final DoubleTolerance EQUALS = new AbstractDoubleTolerance() {
        @Override
        public boolean test(double a, double b) {
            return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
        }

        @Override
        public String get() {
            return "exact";
        }
    };

    /** No instances. */
    private DoubleTolerances() {}

    /**
     * Creates a {@link DoubleTolerance} instance that uses exact binary equality.
     *
     * <p>Notes:
     * <ul>
     * <li>{@code 0.0} and {@code -0.0} are not considered equal.
     * <li>NaNs are considered equal.
     * </ul>
     *
     * @return an instance.
     * @see Double#equals(Object)
     * @see Double#doubleToLongBits(double)
     */
    public static DoubleTolerance equals() {
        return EQUALS;
    }

    /**
     * Creates a {@link DoubleTolerance} instance that uses the given range of allowed
     * error (inclusive) for determining equality.
     *
     * <p>Notes:
     * <ul>
     * <li>Two float numbers are considered equal if there are {@code (maxUlps - 1)}
     * (or fewer) floating point numbers between them. Use {@code maxUlps = 0} for
     * numerical equality.
     * <li>NaNs are considered equal.
     * <li>{@code 0.0} and {@code -0.0} are considered equal when {@code maxUlps = 0}.
     * </ul>
     *
     * <p>Use of this method with {@code maxUlps = 0} is slower than using {@link #equals()}
     * for binary equality. The results are identical with the exception that {@code 0.0} and
     * {@code -0.0} are considered equal.
     *
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * @return a new instance.
     * @throws IllegalArgumentException if {@code eps < 0} or is not finite
     * @see Precision#equalsIncludingNaN(double, double, int)
     */
    public static DoubleTolerance ulps(final int maxUlps) {
        return new AbstractDoubleTolerance() {
            @Override
            public boolean test(double a, double b) {
                return Precision.equalsIncludingNaN(a, b, maxUlps);
            }

            @Override
            public String get() {
                return "ulp=" + maxUlps;
            }
        };
    }

    /**
     * Creates a {@link DoubleTolerance} instance that uses the given absolute epsilon
     * value for determining equality.
     *
     * <p>Notes:
     * <ul>
     * <li>Two numbers are considered equal if there are 0
     * floating point numbers between them, i.e. two adjacent
     * floating point numbers are considered equal.
     * <li>NaNs are considered equal.
     * </ul>
     *
     * <p>Use of this method with {@code eps = 0.0} is not recommended as neighbouring
     * floating points numbers are considered equal. Consider using {@link #equals()}
     * for binary equality or {@link #ulps(int)} with {@code ulps=0} for numerical equality.
     *
     * @param eps Value to use for determining equality.
     * @return a new instance.
     * @throws IllegalArgumentException if {@code eps < 0} or is not finite
     * @see Precision#equalsIncludingNaN(double, double, double)
     */
    public static DoubleTolerance absolute(final double eps) {
        if (!Double.isFinite(eps) ||
            eps < 0d) {
            throw new IllegalArgumentException("Invalid epsilon value: " + eps);
        }

        return new AbstractDoubleTolerance() {
            @Override
            public boolean test(double a, double b) {
                return Precision.equalsIncludingNaN(a, b, eps);
            }

            @Override
            public String get() {
                return "abs=" + eps;
            }
        };
    }

    /**
     * Creates a {@link DoubleTolerance} instance that uses the given relative epsilon
     * value for determining equality.
     *
     * <p>Notes:
     * <ul>
     * <li>Two numbers are considered equal if there are 0
     * floating point numbers between them, i.e. two adjacent
     * floating point numbers are considered equal; or
     * the relative difference between them is less than or equal
     * to the given tolerance.
     * <li>NaNs are <strong>not</strong> considered equal.
     * <li>The relative tolerance instance is symmetric; it evaluates the same for
     * {@code (a, b)} or {@code (b, a)}.
     * </ul>
     *
     * <p>Use of this method with {@code eps = 0.0} is not recommended as neighbouring
     * floating points numbers are considered equal. Consider using {@link #equals()}
     * for binary equality or {@link #ulps(int)} with {@code ulps=0} for numerical equality.
     *
     * @param eps Value to use for determining equality.
     * @return a new instance.
     * @throws IllegalArgumentException if {@code eps < 0} or is not finite
     * @see Precision#equalsIncludingNaN(double, double, double)
     */
    public static DoubleTolerance relative(final double eps) {
        if (!Double.isFinite(eps) ||
            eps < 0d) {
            throw new IllegalArgumentException("Invalid epsilon value: " + eps);
        }

        return new AbstractDoubleTolerance() {
            @Override
            public boolean test(double a, double b) {
                return Precision.equalsWithRelativeTolerance(a, b, eps);
            }

            @Override
            public String get() {
                return "rel=" + eps;
            }
        };
    }

    /**
     * Custom implementation of the DoubleTolerance interface to allow message formatting
     * in assertions using {@link Supplier Supplier<String>}.
     */
    abstract static class AbstractDoubleTolerance implements DoubleTolerance, Supplier<String> {
        @Override
        public DoubleTolerance and(DoubleTolerance other) {
            return new DoubleAndTolerance(this, other);
        }

        @Override
        public DoubleTolerance negate() {
            return new DoubleNegateTolerance(this);
        }

        @Override
        public DoubleTolerance or(DoubleTolerance other) {
            return new DoubleOrTolerance(this, other);
        }

        @Override
        public String toString() {
            // This is overridden so that the tolerance is converted into a human readable
            // string using its description. For example this is used by JUnit to display
            // a DoubleTolerance argument for a ParameterizedTest.
            return get();
        }
    }

    /**
     * Represents a logical {@code And} of two tolerances, each tolerance testing the two
     * input values.
     *
     * <p>Supports a description {@link Supplier} for a {@link String}.
     */
    static class DoubleAndTolerance extends AbstractDoubleTolerance {
        /** The first tolerance. */
        private final DoubleTolerance tolerance1;
        /** The second tolerance. */
        private final DoubleTolerance tolerance2;

        /**
         * @param tolerance1 The first tolerance
         * @param tolerance2 The second tolerance
         */
        DoubleAndTolerance(DoubleTolerance tolerance1, DoubleTolerance tolerance2) {
            this.tolerance1 = Objects.requireNonNull(tolerance1, "Tolerance 1 is null");
            this.tolerance2 = Objects.requireNonNull(tolerance2, "Tolerance 2 is null");
        }

        @Override
        public boolean test(double value1, double value2) {
            return tolerance1.test(value1, value2) && tolerance2.test(value1, value2);
        }

        @Override
        public String get() {
            return StringUtils.andToString(tolerance1, tolerance2);
        }
    }

    /**
     * Represents a logical {@code Or} of two tolerances, each tolerance testing the two
     * input values.
     *
     * <p>Supports a description {@link Supplier} for a {@link String}.
     */
    static class DoubleOrTolerance extends AbstractDoubleTolerance {
        /** The first tolerance. */
        private final DoubleTolerance tolerance1;
        /** The second tolerance. */
        private final DoubleTolerance tolerance2;

        /**
         * @param tolerance1 The first tolerance
         * @param tolerance2 The second tolerance
         */
        DoubleOrTolerance(DoubleTolerance tolerance1, DoubleTolerance tolerance2) {
            this.tolerance1 = Objects.requireNonNull(tolerance1, "Tolerance 1 is null");
            this.tolerance2 = Objects.requireNonNull(tolerance2, "Tolerance 2 is null");
        }

        @Override
        public boolean test(double value1, double value2) {
            return tolerance1.test(value1, value2) || tolerance2.test(value1, value2);
        }

        @Override
        public String get() {
            return StringUtils.orToString(tolerance1, tolerance2);
        }
    }

    /**
     * Represents a negation of a tolerance including the string representation using
     * {@link Supplier} for a {@link String}.
     */
    static class DoubleNegateTolerance extends AbstractDoubleTolerance {
        /** The tolerance. */
        private final DoubleTolerance tolerance;

        /**
         * @param tolerance The tolerance
         */
        DoubleNegateTolerance(DoubleTolerance tolerance) {
            this.tolerance = Objects.requireNonNull(tolerance, "Tolerance is null");
        }

        @Override
        public boolean test(double value1, double value2) {
            return !tolerance.test(value1, value2);
        }

        @Override
        public DoubleTolerance negate() {
            // Back to the original
            return tolerance;
        }

        @Override
        public String get() {
            return StringUtils.negateToString(tolerance);
        }
    }
}
