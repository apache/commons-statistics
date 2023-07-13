package org.apache.commons.statistics.descriptive;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Helper class for tests in {@code o.a.c.s.descriptive} module.
 */
public final class TestHelper {

    /** Class contains only static methods. */
    private TestHelper() {}

    /**
     * Shuffles the entries of the given array.
     *
     * <p>Fisher-Yates shuffle copied from
     * <a href="https://github.com/apache/commons-rng/blob/master/commons-rng-sampling/src/main/java/org/apache/commons/rng/sampling/ArraySampler.java">
     *     RNG ArraySampler.</a>
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static double[] shuffle(UniformRandomProvider rng, double[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
}
