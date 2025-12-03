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

import java.util.Objects;

/**
 * Represents a tolerance predicate (boolean-valued function) of two {@code double}-valued
 * argument. This is the {@code double}-consuming primitive type specialization
 * of {@link java.util.function.BiPredicate BiPredicate}.
 *
 * <p>This interface is intended for comparing outputs of a computation where floating
 * point errors may have occurred.
 *
 * <p>This interface is public and has public methods to allow testing within the other modules.
 */
@FunctionalInterface
public interface DoubleTolerance {
    /**
     * Evaluates this tolerance predicate on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @return {@code true} if the input arguments match the tolerance predicate,
     * otherwise {@code false}
     */
    boolean test(double a, double b);

    /**
     * Returns a composed tolerance predicate that represents a short-circuiting logical
     * AND of this tolerance predicate and another.  When evaluating the composed
     * tolerance predicate, if this tolerance predicate is {@code false}, then the {@code other}
     * tolerance predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either tolerance predicate are relayed
     * to the caller; if evaluation of this tolerance predicate throws an exception, the
     * {@code other} tolerance predicate will not be evaluated.
     *
     * @param other a tolerance predicate that will be logically-ANDed with this
     *              tolerance predicate
     * @return a composed tolerance predicate that represents the short-circuiting logical
     * AND of this tolerance predicate and the {@code other} tolerance predicate
     * @throws NullPointerException if other is null
     */
    default DoubleTolerance and(DoubleTolerance other) {
        Objects.requireNonNull(other);
        return (a, b) -> test(a, b) && other.test(a, b);
    }

    /**
     * Returns a tolerance predicate that represents the logical negation of this
     * tolerance predicate.
     *
     * @return a tolerance predicate that represents the logical negation of this
     * tolerance predicate
     */
    default DoubleTolerance negate() {
        return (a, b) -> !test(a, b);
    }

    /**
     * Returns a composed tolerance predicate that represents a short-circuiting logical
     * OR of this tolerance predicate and another.  When evaluating the composed
     * tolerance predicate, if this tolerance predicate is {@code true}, then the {@code other}
     * tolerance predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either tolerance predicate are relayed
     * to the caller; if evaluation of this tolerance predicate throws an exception, the
     * {@code other} tolerance predicate will not be evaluated.
     *
     * @param other a tolerance predicate that will be logically-ORed with this
     *              tolerance predicate
     * @return a composed tolerance predicate that represents the short-circuiting logical
     * OR of this tolerance predicate and the {@code other} tolerance predicate
     * @throws NullPointerException if other is null
     */
    default DoubleTolerance or(DoubleTolerance other) {
        Objects.requireNonNull(other);
        return (a, b) -> test(a, b) || other.test(a, b);
    }
}
