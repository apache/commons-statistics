package org.apache.commons.statistics.descriptive;

/**
 * A mutable result container that accumulates a {@code DoubleStatistic}.
 *
 * @param <T>  the {@code DoubleStatistic} being accumulated.
 */
public interface DoubleStatisticAccumulator<T extends DoubleStatistic> {

    /**
     * Combines the state of another {@code DoubleStatisticAccumulator} into this one.
     *
     * @param other  another {@code DoubleStatisticAccumulator}
     * @param <U>  the type of the other {@code DoubleStatisticAccumulator} accumulating the same {@code DoubleStatistic}.
     */
    <U extends DoubleStatisticAccumulator<T>> void combine(U other);

    /**
     * Gets the {@link DoubleStatistic} being accumulated.
     *
     * @return  the {@code DoubleStatistic}.
     */
    T get();
}
