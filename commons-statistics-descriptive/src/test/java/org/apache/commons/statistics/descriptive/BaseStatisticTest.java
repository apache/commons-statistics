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

import java.math.BigInteger;
import java.util.Arrays;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;
import org.junit.jupiter.api.Assertions;

/**
 * Abstract base class for statistic tests.
 *
 * <p>This class contains common methods used by type-specific statistic tests.
 */
abstract class BaseStatisticTest {
    /** The number of random permutations to perform. */
    static final int RANDOM_PERMUTATIONS = 5;
    /** The maximum number of values to output in assertion messages. This is used to provide
     * the data for the failed test which is useful to identify the failed test case. */
    static final int MAX_FORMAT_VALUES = 100;
    /** A marker return object to indicate that a method for a result type raised an exception. */
    private static final Object MARKER_OBJECT = new Object();

    /**
     * Gets the statistic name.
     *
     * <p>The default implementation removes the text {@code "Test"} from the
     * simple class name.
     *
     * @return the distribution name
     * @see Class#getSimpleName()
     */
    protected String getStatisticName() {
        return getClass().getSimpleName().replace("Test", "");
    }

    /**
     * Gets the native result type of the statistic.
     * The default is {@code double}.
     *
     * @return the result type
     */
    protected ResultType getResultType() {
        return ResultType.DOUBLE;
    }

    /**
     * Get the maximum number of values that can be added where the statistic is
     * considered empty.
     *
     * <p>The default is zero. Override this method if the statistic requires multiple
     * values to compute.
     *
     * @return the empty size
     * @see #getEmptyValue()
     */
    protected int getEmptySize() {
        return 0;
    }

    /**
     * Gets the value of the statistics when it is considered empty.
     *
     * @return the empty value
     * @see #getEmptySize()
     */
    protected abstract StatisticResult getEmptyValue();

    /**
     * Creates the statistic result using a {@code double} value.
     *
     * @param value Value.
     * @return the statistic result
     */
    static StatisticResult createStatisticResult(double value) {
        // This method is included for completeness.
        // It is a simple lambda to the default StatisticResult interface.
        return () -> value;
    }

    /**
     * Creates the statistic result using an {@code int} value.
     *
     * @param value Value.
     * @return the statistic result
     */
    static StatisticResult createStatisticResult(int value) {
        return (IntStatisticResult) () -> value;
    }

    /**
     * Creates the statistic result using a {@code long} value.
     *
     * @param value Value.
     * @return the statistic result
     */
    static StatisticResult createStatisticResult(long value) {
        return (LongStatisticResult) () -> value;
    }

    /**
     * Creates the statistic result using a {@code BigInteger} value.
     *
     * @param value Value.
     * @return the statistic result
     */
    static StatisticResult createStatisticResult(BigInteger value) {
        return (BigIntegerStatisticResult) () -> value;
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value as a {@code double}.
     * This method is not used by the base implementation except to provide the default
     * implementation for the individual test tolerance methods.
     *
     * <p>Tolerances are not required to be configured if the result type is not a {@code double}.
     *
     * <p><strong>Note</strong>
     *
     * <p>The default implementation throws an exception. This forces the
     * implementation to either provide a single tolerance for all tests, or the tests
     * individually by overriding each tolerance method.
     *
     * @return the tolerance
     * @see #getResultType()
     */
    protected DoubleTolerance getTolerance() {
        if (getResultType() == ResultType.DOUBLE) {
            throw new IllegalStateException("Not all test tolerances have been configured");
        }
        // Not required
        return null;
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for a test using the primitive consumer {@code accept} method.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceAccept() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for a test using creation from a primitive array.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceArray() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for a test using the primitive consumer {@code accept} method to create instances
     * that are combined using {@link StatisticAccumulator#combine(StatisticResult)}.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceAcceptAndCombine() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the expected value
     * for a test using creation from a primitive array to create instances
     * that are combined using {@link StatisticAccumulator#combine(StatisticResult)}.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceArrayAndCombine() {
        return getTolerance();
    }

    /**
     * Gets the tolerance for equality of the statistic and the equivalent {@link DoubleStatistic}.
     * This method is used to cross-validate the statistic computation against the reference
     * {@code double} implementation.
     *
     * <p>The default implementation uses {@link #getTolerance()}.
     *
     * <p>Note: Computation using {@code double} values may not be as accurate as integer
     * specialisations. This tolerance can be set appropriately to detect errors, for example
     * using a relative tolerance of 1e-12.
     *
     * @return the tolerance
     */
    protected DoubleTolerance getToleranceAsDouble() {
        return getTolerance();
    }

    /**
     * Returns {@code true} if the
     * {@link StatisticAccumulator#combine(StatisticResult) combine}
     * method is symmetric. If {@code true} then the combine will be tested with
     * duplicate instances of the statistic, combined in left-to-right and right-to-left;
     * the result must have the same statistic value.
     *
     * <p>The default is {@code true}. Override this if the implementation is not symmetric
     * due to the implementation details.
     *
     * @return {@code true} if combine is symmetric
     */
    protected boolean isCombineSymmetric() {
        return true;
    }

    //------------------------ Helper Methods to create test data ---------------------------

    /**
     * Creates the tolerance using an absolute error.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical
     * equality is used.
     *
     * @param eps Absolute tolerance
     * @return the tolerance
     */
    static final DoubleTolerance createAbsTolerance(double eps) {
        return eps > 0 ? DoubleTolerances.absolute(eps) : DoubleTolerances.ulps(0);
    }

    /**
     * Creates the tolerance using an relative error.
     *
     * <p>If the relative tolerance is zero it is ignored and a tolerance of numerical
     * equality is used.
     *
     * @param eps Relative tolerance
     * @return the tolerance
     */
    static final DoubleTolerance createRelTolerance(double eps) {
        return eps > 0 ? DoubleTolerances.relative(eps) : DoubleTolerances.ulps(0);
    }

    /**
     * Creates the tolerance using an {@code Or} combination of absolute and relative error.
     *
     * <p>If the absolute tolerance is zero it is ignored and a tolerance of numerical equality
     * is used.
     *
     * <p>If the relative tolerance is zero it is ignored.
     *
     * @param absTolerance Absolute tolerance
     * @param relTolerance Relative tolerance
     * @return the tolerance
     */
    static final DoubleTolerance createAbsOrRelTolerance(double absTolerance, double relTolerance) {
        final DoubleTolerance tol = createAbsTolerance(absTolerance);
        return relTolerance > 0 ? tol.or(DoubleTolerances.relative(relTolerance)) : tol;
    }

    /**
     * Gets the group sizes used to test the combine method with the custom test data.
     *
     * @return the group sizes
     */
    protected int[] getCombineGroupSizes() {
        return new int[] {2, 3};
    }

    /**
     * Combine the two statistics. This method asserts the contract of the
     * {@link StatisticAccumulator#combine(StatisticResult) combine} method.
     * The left-hand side (LHS) argument must be returned. The right-hand side (RHS) must
     * be unchanged by the operation.
     *
     * <p>Use this method whenever combining two instances of a statistic.
     *
     * @param <S> Type of statistic
     * @param a LHS.
     * @param b RHS.
     * @return the result
     */
    final <S extends StatisticResult & StatisticAccumulator<S>> S assertCombine(S a, S b) {
        final Object before = getNativeResult(b);
        final S c = a.combine(b);
        Assertions.assertEquals(before, getNativeResult(b), () -> getStatisticName() + ": Combine altered the RHS");
        Assertions.assertSame(a, c, () -> getStatisticName() + ": Combined did not return the LHS");
        return c;
    }

    /**
     * Gets the native result of the statistic.
     *
     * @param s Statistic.
     * @return the native result
     * @see #getResultType()
     */
    private Object getNativeResult(StatisticResult s) {
        try {
            switch (getResultType()) {
            case DOUBLE:
                return s.getAsDouble();
            case LONG:
                return s.getAsLong();
            case INT:
                return s.getAsInt();
            case BIG_INTEGER:
                return s.getAsBigInteger();
            default:
                break;
            }
        } catch (final Throwable t) {
            return MARKER_OBJECT;
        }
        throw new IllegalStateException("Unrecognised result type: " + getResultType());
    }

    /**
     * Re-throw the error wrapped in an AssertionError with a message that appends the seed
     * and repeat for the random order test.
     *
     * @param e Error.
     * @param seed Seed.
     * @param repeat Repeat of the total random permutations.
     */
    static void rethrowWithSeedAndRepeat(AssertionError e, long[] seed, int repeat) {
        throw new AssertionError(String.format("%s; Seed=%s; Repeat=%d/%d",
            e.getMessage(), Arrays.toString(seed), repeat, RANDOM_PERMUTATIONS), e);
    }
}
