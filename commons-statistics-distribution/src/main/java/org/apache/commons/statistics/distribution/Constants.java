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
package org.apache.commons.statistics.distribution;

/**
 * Constants for distribution calculations.
 *
 * <p>Constants should evaluate to the closest IEEE {@code double}.
 * Expressions may have been computed using an arbitrary precision math
 * library or obtained from existing online resources.
 * Some expressions will not be the closest {@code double} if evaluated
 * using the JDK's Math functions using {@code double} precision.
 */
final class Constants {
    /** sqrt(2). https://oeis.org/A002193. */
    static final double ROOT_TWO        = 1.4142135623730951;
    /** sqrt(2 / pi). https://oeis.org/A076668. */
    static final double ROOT_TWO_DIV_PI = 0.7978845608028654;
    /** sqrt(pi / 2). https://oeis.org/A069998. */
    static final double ROOT_PI_DIV_TWO = 1.2533141373155003;
    /** ln(2). https://oeis.org/A002162. */
    static final double LN_TWO          = 0.6931471805599453;
    /** 0.5 * ln(2 pi). https://oeis.org/A075700. */
    static final double HALF_LOG_TWO_PI = 0.9189385332046728;

    /** No instances. */
    private Constants() {}
}
