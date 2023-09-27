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

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test code used in the descriptive section of the user guide.
 */
class UserGuideTest {
    @Test
    void testVariance() {
        double[] values = {1, 1, 2, 3, 5, 8, 13, 21};

        double v = Variance.of(values).getAsDouble();

        double v2 = Stream.of("one", "two", "three", "four")
                          .mapToDouble(String::length)
                          .collect(Variance::create, Variance::accept, Variance::combine)
                          .getAsDouble();

        // import numpy as np
        // np.var([1, 1, 2, 3, 5, 8, 13, 21], ddof=1)
        Assertions.assertEquals(49.92857142857143, v, 1e-10);

        // np.var([3, 3, 5, 4], ddof=1)
        Assertions.assertEquals(0.9166666666666666, v2);
    }
}
