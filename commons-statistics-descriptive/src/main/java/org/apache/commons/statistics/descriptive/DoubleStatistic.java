package org.apache.commons.statistics.descriptive;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Represents a state object for computing a single {@link Statistic} over {@code double} valued input(s).
 *
 * <p>Base interface implemented by all statistics.</p>
 */
public interface DoubleStatistic extends DoubleConsumer, DoubleSupplier {
}
