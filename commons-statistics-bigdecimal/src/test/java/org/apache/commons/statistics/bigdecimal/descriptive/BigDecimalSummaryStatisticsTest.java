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
package org.apache.commons.statistics.bigdecimal.descriptive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.math.BigDecimal;
import java.util.stream.LongStream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The Unit Test for {@link BigDecimalSummaryStatistics}.
 */
@DisplayName("Unit Tests for BigDecimalSummaryStatistics")
class BigDecimalSummaryStatisticsTest {

    /**
     * expected accuracy for results.
     */
    private static final Offset<Double> EPSILON = Offset.offset(1E-15d);

    @Nested
    @DisplayName("Constructor parameter validation test")
    class ConstructorParameterTest {

        @Test
        @DisplayName("should fail if count is given less than zero.")
        void shouldFailWithNegativeValueForCount() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new BigDecimalSummaryStatistics(-1L, null, null, null))
                .withMessage("count must be greater or equal to zero.");
        }

        @Test
        @DisplayName("should fail if max is given null.")
        void shouldFailWithNullForMin() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new BigDecimalSummaryStatistics(1L, null, null, null))
                .withMessage("min is not allowed to be null.");
        }

        @Test
        @DisplayName("should fail if max is given null.")
        void shouldFailWithNullForMax() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new BigDecimalSummaryStatistics(1L, BigDecimal.ZERO, null, null))
                .withMessage("max is not allowed to be null.");
        }

        @Test
        @DisplayName("should fail if sum is given null.")
        void shouldFailWithNullForSum() {
            assertThatIllegalArgumentException()
                .isThrownBy(
                    () -> new BigDecimalSummaryStatistics(1L, BigDecimal.ZERO, BigDecimal.TEN,
                        null))
                .withMessage("sum is not allowed to be null.");
        }

        @Test
        @DisplayName("should fail if min is greater than max.")
        void shouldFailForMinGreaterThanMax() {
            assertThatIllegalArgumentException()
                .isThrownBy(
                    () -> new BigDecimalSummaryStatistics(1L, BigDecimal.ONE, BigDecimal.ZERO,
                        BigDecimal.ZERO))
                .withMessage("Minimum is greater than maximum.");
        }
    }

    @Nested
    @DisplayName("Parameter test for method")
    class ParameterTest {

        private BigDecimalSummaryStatistics bigDecimalSummaryStatistics;

        @BeforeEach
        void beforeEach() {
            this.bigDecimalSummaryStatistics = new BigDecimalSummaryStatistics();
        }

        @Test
        @DisplayName("accept should fail if given null")
        void acceptShouldFailWhileGivingNull() {
            assertThatIllegalArgumentException()
                .isThrownBy(
                    () -> this.bigDecimalSummaryStatistics.accept(null))
                .withMessage("value is not allowed to be null.");
        }

        @Test
        @DisplayName("combine should fail if given null")
        void combineShouldFailWhileGivingNull() {
            assertThatIllegalArgumentException()
                .isThrownBy(
                    () -> this.bigDecimalSummaryStatistics.combine(null))
                .withMessage("other is not allowed to be null.");
        }

        @Test
        @DisplayName("average should result in Zero if no values have been provided.")
        void averageShouldResultInZeroWhenNoValuesHaveBeenProvided() {
            assertThat(this.bigDecimalSummaryStatistics.getAverage()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("count should result in zero if no values have been provided.")
        void countShouldResultInZeroWhenNoValuesHaveBeenProvided() {
            assertThat(this.bigDecimalSummaryStatistics.getCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("sum should result in zero if no values have been provided.")
        void sumShouldResultInZeroWhenNoValuesHaveBeenProvided() {
            assertThat(this.bigDecimalSummaryStatistics.getSum()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("min should result in exception if no values have been provided.")
        void minShouldResultInIllegalStateExceptionWhenNoValuesHaveBeenProvided() {
            assertThatIllegalStateException()
                .isThrownBy(() -> this.bigDecimalSummaryStatistics.getMin())
                .withMessage("Minimum can not be calculated cause we have no values yet.");
        }

        @Test
        @DisplayName("max should result in exception if no values have been provided.")
        void maxShouldResultInIllegalArgumentExceptionWhenNoValuesHaveBeenProvided() {
            assertThatIllegalStateException()
                .isThrownBy(() -> this.bigDecimalSummaryStatistics.getMax())
                .withMessage("Maximum can not be calculated cause we have no values yet.");
        }
    }

    @Nested
    @DisplayName("Edge case test")
    class EdgeCaseTest {

        @Test
        @DisplayName("statistics for one element.")
        void summaryStatisticsForOneElement() {
            BigDecimalSummaryStatistics collect =
                LongStream.rangeClosed(1, 1)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(BigDecimalSummaryStatistics::new,
                        BigDecimalSummaryStatistics::accept,
                        BigDecimalSummaryStatistics::combine);

            assertThat(collect.getCount()).isEqualTo(1L);
            assertThat(collect.getAverage()).isEqualTo(BigDecimal.ONE);
            assertThat(collect.getMin()).isEqualTo(BigDecimal.ONE);
            assertThat(collect.getMax()).isEqualTo(BigDecimal.ONE);
            assertThat(collect.getSum()).isEqualTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("statistics for elements 1..10.")
        void summaryStatisticsForOneToTen() {
            BigDecimalSummaryStatistics collect =
                LongStream.rangeClosed(1, 10)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(BigDecimalSummaryStatistics::new,
                        BigDecimalSummaryStatistics::accept,
                        BigDecimalSummaryStatistics::combine);

            assertThat(collect.getCount()).isEqualTo(10L);
            assertThat(collect.getAverage().doubleValue()).isEqualTo(5.5d, EPSILON);
//        BigDecimalSummaryStatisticsAssert.assertThat(collect).getAverage().isEqualTo(5.5d, EPSILON);
            assertThat(collect.getSum().doubleValue()).isEqualTo(55d, EPSILON);
            assertThat(collect.getMin()).isEqualTo(BigDecimal.ONE);
            assertThat(collect.getMax()).isEqualTo(BigDecimal.TEN);

        }

        @Test
        @DisplayName("statistics for elements 1..42.")
        void summaryStatisticsForOneToFourtyTwo() {
            BigDecimalSummaryStatistics collect =
                LongStream.rangeClosed(1, 42)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(BigDecimalSummaryStatistics::new,
                        BigDecimalSummaryStatistics::accept,
                        BigDecimalSummaryStatistics::combine);

            assertThat(collect.getCount()).isEqualTo(42L);
            assertThat(collect.getAverage().doubleValue()).isEqualTo(21.5d, EPSILON);
//        BigDecimalSummaryStatisticsAssert.assertThat(collect).getAverage().isEqualTo(5.5d, EPSILON);
            assertThat(collect.getSum().doubleValue()).isEqualTo(903d, EPSILON);
            assertThat(collect.getMin()).isEqualTo(BigDecimal.ONE);
            assertThat(collect.getMax()).isEqualTo(BigDecimal.valueOf(42));

        }

        @Test
        @DisplayName("statistics for elements 1..1_234_567.")
        void summaryStatisticsForOneTXXXX() {
            BigDecimalSummaryStatistics collect =
                LongStream.rangeClosed(1, 1_234_567)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(BigDecimalSummaryStatistics::new,
                        BigDecimalSummaryStatistics::accept,
                        BigDecimalSummaryStatistics::combine);

            assertThat(collect.getCount()).isEqualTo(1_234_567L);
            assertThat(collect.getAverage().doubleValue()).isEqualTo(617_284d, EPSILON);
//        BigDecimalSummaryStatisticsAssert.assertThat(collect).getAverage().isEqualTo(5.5d, EPSILON);
            assertThat(collect.getSum().doubleValue()).isEqualTo(762_078_456_028d, EPSILON);
            assertThat(collect.getMin()).isEqualTo(BigDecimal.ONE);
            assertThat(collect.getMax()).isEqualTo(BigDecimal.valueOf(1_234_567));

        }
    }

}
