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

/**
 * Represents the {@code long} result of a statistic computed over a set of values.
 *
 * <p>This is a helper interface to map the native type of the expected value to other result types.
 *
 * @since 1.1
 */
@FunctionalInterface
interface LongStatisticResult extends StatisticResult  {

    @Override
    long getAsLong();

    @Override
    default double getAsDouble() {
        return getAsLong();
    }

    @Override
    default int getAsInt() {
        return Math.toIntExact(getAsLong());
    }

    @Override
    default BigInteger getAsBigInteger() {
        return BigInteger.valueOf(getAsLong());
    }
}
