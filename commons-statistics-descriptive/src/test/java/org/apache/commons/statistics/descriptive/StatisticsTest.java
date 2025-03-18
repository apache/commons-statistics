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

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Statistics} utilities.
 */
class StatisticsTest {
    @Test
    void testNoOpDoubleConsumer() {
        final DoubleConsumer c = Statistics.DOUBLE_NOOP;
        // Hit coverage
        c.accept(0);
        final double[] value = {0};
        final DoubleConsumer other = x -> value[0] = x;
        final DoubleConsumer combined = c.andThen(other);
        Assertions.assertSame(combined, other);
        final double y = 42;
        combined.accept(y);
        Assertions.assertEquals(y, value[0]);
    }

    @Test
    void testComposeDoubleConsumers() {
        Assertions.assertNull(Statistics.composeDoubleConsumers((DoubleConsumer) null));
        Assertions.assertNull(Statistics.composeDoubleConsumers((DoubleConsumer) null, (DoubleConsumer) null));
        final double[] v1 = {0};
        final double[] v2 = {0};
        final DoubleConsumer c1 = x -> v1[0] = x;
        final DoubleConsumer c2 = x -> v2[0] = x;
        final DoubleConsumer combined = Statistics.composeDoubleConsumers(c1, c2);
        final double y = 42;
        combined.accept(y);
        Assertions.assertEquals(y, v1[0]);
        Assertions.assertEquals(y, v2[0]);
    }

    @Test
    void testNoOpIntConsumer() {
        final IntConsumer c = Statistics.INT_NOOP;
        // Hit coverage
        c.accept(0);
        final int[] value = {0};
        final IntConsumer other = x -> value[0] = x;
        final IntConsumer combined = c.andThen(other);
        Assertions.assertSame(combined, other);
        final int y = 42;
        combined.accept(y);
        Assertions.assertEquals(y, value[0]);
    }

    @Test
    void testComposeIntConsumers() {
        Assertions.assertNull(Statistics.composeIntConsumers((IntConsumer) null));
        Assertions.assertNull(Statistics.composeIntConsumers((IntConsumer) null, (IntConsumer) null));
        final int[] v1 = {0};
        final int[] v2 = {0};
        final IntConsumer c1 = x -> v1[0] = x;
        final IntConsumer c2 = x -> v2[0] = x;
        final IntConsumer combined = Statistics.composeIntConsumers(c1, c2);
        final int y = 42;
        combined.accept(y);
        Assertions.assertEquals(y, v1[0]);
        Assertions.assertEquals(y, v2[0]);
    }

    @Test
    void testNoOpLongConsumer() {
        final LongConsumer c = Statistics.LONG_NOOP;
        // Hit coverage
        c.accept(0);
        final long[] value = {0};
        final LongConsumer other = x -> value[0] = x;
        final LongConsumer combined = c.andThen(other);
        Assertions.assertSame(combined, other);
        final long y = 42;
        combined.accept(y);
        Assertions.assertEquals(y, value[0]);
    }

    @Test
    void testComposeLongConsumers() {
        Assertions.assertNull(Statistics.composeLongConsumers((LongConsumer) null));
        Assertions.assertNull(Statistics.composeLongConsumers((LongConsumer) null, (LongConsumer) null));
        final long[] v1 = {0};
        final long[] v2 = {0};
        final LongConsumer c1 = x -> v1[0] = x;
        final LongConsumer c2 = x -> v2[0] = x;
        final LongConsumer combined = Statistics.composeLongConsumers(c1, c2);
        final long y = 42;
        combined.accept(y);
        Assertions.assertEquals(y, v1[0]);
        Assertions.assertEquals(y, v2[0]);
    }

    @ParameterizedTest
    @MethodSource
    void testCheckFromToIndex(int from, int to, int length) {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Statistics.checkFromToIndex(from, to, length));
    }

    static Stream<Arguments> testCheckFromToIndex() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // fromIndex < 0
        builder.add(Arguments.of(-1, 10, 10));
        builder.add(Arguments.of(Integer.MIN_VALUE, 10, 10));
        builder.add(Arguments.of(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        // fromIndex > toIndex
        builder.add(Arguments.of(2, 1, 10));
        builder.add(Arguments.of(20, 10, 10));
        builder.add(Arguments.of(0, -1, 10));
        // toIndex > length
        builder.add(Arguments.of(0, 11, 10));
        builder.add(Arguments.of(0, Integer.MAX_VALUE, Integer.MAX_VALUE - 1));
        // length < 0
        builder.add(Arguments.of(0, 1, -1));
        builder.add(Arguments.of(0, 1, Integer.MIN_VALUE));
        builder.add(Arguments.of(0, Integer.MAX_VALUE, Integer.MIN_VALUE));
        return builder.build();
    }
}
