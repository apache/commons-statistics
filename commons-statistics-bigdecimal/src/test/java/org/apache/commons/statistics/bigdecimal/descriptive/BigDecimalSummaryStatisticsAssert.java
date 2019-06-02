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

import org.assertj.core.api.AbstractAssert;

public class BigDecimalSummaryStatisticsAssert extends
    AbstractAssert<BigDecimalSummaryStatisticsAssert, BigDecimalSummaryStatistics> {

    public BigDecimalSummaryStatisticsAssert(BigDecimalSummaryStatistics bigDecimalSummaryStatistics) {
        super(bigDecimalSummaryStatistics, BigDecimalSummaryStatisticsAssert.class);
    }

    public static BigDecimalSummaryStatisticsAssert assertThat(BigDecimalSummaryStatistics actual) {
        return new BigDecimalSummaryStatisticsAssert(actual);
    }

    public BigDecimalSummaryStatisticsAssert getAverage() {
        isNotNull();
        return myself;
    }
    public BigDecimalSummaryStatisticsAssert isEqualTo(BigDecimalSummaryStatistics expected) {

        return null;
    }

//    public BigDecimalSummaryStatisticsAssert isCloseTo(Complex expected, float offset) {
//        Assertions.assertThat(actual.getImaginary())
//            .isCloseTo(expected.getImaginary(), Offset.offset(
//                (double) offset));
//        Assertions.assertThat(actual.getReal()).isCloseTo(expected.getReal(), Offset.offset(
//            (double) offset));
//        return myself;
//    }
//
//    public BigDecimalSummaryStatisticsAssert withDelta(float delta) {
//        myself.actual.getImaginary();
//        return myself;
//    }
}
