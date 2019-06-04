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

import java.math.BigDecimal;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;

public class BigDecimalSummaryStatisticsAssert extends
    AbstractAssert<BigDecimalSummaryStatisticsAssert, BigDecimalSummaryStatistics> {

    public BigDecimalSummaryStatisticsAssert(
        BigDecimalSummaryStatistics bigDecimalSummaryStatistics) {
        super(bigDecimalSummaryStatistics, BigDecimalSummaryStatisticsAssert.class);
    }

    public static BigDecimalSummaryStatisticsAssert assertThat(BigDecimalSummaryStatistics actual) {
        return new BigDecimalSummaryStatisticsAssert(actual);
    }

    public BigDecimalSummaryStatisticsAssert hasAverage(BigDecimal expected) {
        isNotNull();
        Assertions.assertThat(myself.actual.getAverage()).isEqualTo(expected);
        return myself;
    }

    public BigDecimalSummaryStatisticsAssert hasSum(double expected, Offset<Double> offset) {
        isNotNull();
        Assertions.assertThat(myself.actual.getSum().doubleValue()).isEqualTo(expected, offset);
        return myself;
    }

    public BigDecimalSummaryStatisticsAssert hasMin(double expected, Offset<Double> offset) {
        isNotNull();
        Assertions.assertThat(myself.actual.getMin().doubleValue()).isEqualTo(expected, offset);
        return myself;
    }

    public BigDecimalSummaryStatisticsAssert hasMax(BigDecimal expected) {
        isNotNull();
        Assertions.assertThat(myself.actual.getMax()).isEqualTo(expected);
        return myself;
    }

    public BigDecimalSummaryStatisticsAssert getAverage() {
        isNotNull();
        return myself;
    }

    public BigDecimalSummaryStatisticsAssert isEqualTo(BigDecimalSummaryStatistics expected) {
        System.out.println("this:" + this.getClass().getName());

        return null;
    }

}
