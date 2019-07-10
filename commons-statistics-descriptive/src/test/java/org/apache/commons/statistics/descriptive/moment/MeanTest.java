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

package org.apache.commons.statistics.descriptive.moment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 *Test cases for {@link Mean} class.
 */
class MeanTest {

    private double mean = 12.404545454545455d;
    private double combMean = 13.573076923077d;
    private double sum = 272.90d;
    private double combSum = 352.90d;
    private double tolerance = 10E-12;

    private double[] testArray =
        {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0,  8.2, 10.3, 11.3,
            14.1,  9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0,  8.8,
            9.0, 12.3 };

    private double[] testArray2 =
        {5 * 2 + 4, 5 * 2 +  7, 5 * 2 + 13, 5 * 2 + 16};

    /**
     * Verifies that accept(), getMean(),getN(), getSum() works properly for zero, one
     * and multiple additions.
     */
    @Test
    public void testMean() {
        Mean meanX = new Mean();

        //After Zero additions.
        assertEquals(Double.NaN, meanX.getMean(), tolerance);
        assertEquals(0, meanX.getN());
        assertEquals(0.0, meanX.getSum(), tolerance);

        //After 1 addition.
        meanX.accept(1.0);
        assertEquals(1.0, meanX.getMean(), tolerance);
        assertEquals(1, meanX.getN());
        assertEquals(1.0, meanX.getSum(), tolerance);

        meanX.clear();
        // After Multiple Additions
        // Add testArray one value at a time and check result
        for (int i = 0; i < testArray.length; i++) {
            meanX.accept(testArray[i]);
        }
        assertEquals(mean, meanX.getMean(), tolerance);
        assertEquals(testArray.length, meanX.getN());
        assertEquals(sum, meanX.getSum(), tolerance);
    }

    /** Verifies that combine() works properly. */
    @Test
    public void testCombine() {
        Mean mean1 = new Mean();
        Mean mean2 = new Mean();

        // Combining two empty Means.
        mean1.combine(mean2);
        assertEquals(Double.NaN, mean1.getMean(), tolerance);
        assertEquals(0, mean1.getN());
        assertEquals(0.0, mean1.getSum(), tolerance);

        //Combining an empty Mean to another non-empty Mean.
        mean2.accept(5);
        mean2.combine(mean1);
        assertEquals(5, mean2.getMean(), tolerance);
        assertEquals(1, mean2.getN());
        assertEquals(5, mean2.getSum(), tolerance);
        mean2.clear();
        mean1.clear();

        //Combining a non-empty Mean to another empty Mean.
        mean1.accept(5);
        mean2.combine(mean1);
        assertEquals(5, mean2.getMean(), tolerance);
        assertEquals(1, mean2.getN());
        assertEquals(5, mean2.getSum(), tolerance);
        mean2.clear();
        mean1.clear();

        //Combining two Mean objects with finite values.
        // Add testArray, one value at a time in mean1 object and check result
        for (int i = 0; i < testArray.length; i++) {
            mean1.accept(testArray[i]);
        }
        assertEquals(mean, mean1.getMean(), tolerance);
        assertEquals(testArray.length, mean1.getN());
        assertEquals(sum, mean1.getSum(), tolerance);

        // Add testArray2, one value at a time in mean2 object.
        for (int i = 0; i < testArray2.length; i++) {
            mean2.accept(testArray2[i]);
        }
        // combining mean2 with mean1
        mean1.combine(mean2);
        assertEquals(combMean, mean1.getMean(), tolerance);
        assertEquals(testArray.length + testArray2.length, mean1.getN());
        assertEquals(combSum, mean1.getSum(), tolerance);
    }

    /**
     * Verifies that getMean(),getN(), getSum() works properly when data is split
     * and joined.
     */
    @Test
    public void testSplit() {

        Mean mean1 = new Mean();
        Mean mean2 = new Mean();
        Mean mean3 = new Mean();

        // Add all values of testArray at once in the Mean object and check result.
        for (int i = 0; i < testArray.length; i++) {
            mean1.accept(testArray[i]);
        }
        assertEquals(mean, mean1.getMean(), tolerance);
        assertEquals(testArray.length, mean1.getN());
        assertEquals(sum, mean1.getSum(), tolerance);
        mean1.clear();

        // Divide testArray in two equal parts and add each part in a Mean object.
        // Then combine them and check result
        final double half = testArray.length / 2;
        for (int i = 0; i < testArray.length; i++) {
            if (i < half) {
                mean1.accept(testArray[i]);
            }
            else {
                mean2.accept(testArray[i]);
            }
        }
        // combining mean2 with mean1
        mean1.combine(mean2);
        assertEquals(mean, mean1.getMean(), tolerance);
        assertEquals(testArray.length, mean1.getN());
        assertEquals(sum, mean1.getSum(), tolerance);
        mean1.clear();
        mean2.clear();

        // Divide testArray in three equal parts and add each part in a Mean object.
        // Then combine them and check result
        // Add all values of testArray at once in the Mean object and check result.
        final double newMean = 12.409523809524d;
        final double newSum = 260.60d;
        final long newCount = 21;
        for (int i = 0; i < testArray.length - 1; i++) {
            mean1.accept(testArray[i]);
        }
        assertEquals(newMean, mean1.getMean(), tolerance);
        assertEquals(newCount, mean1.getN());
        assertEquals(newSum, mean1.getSum(), tolerance);
        mean1.clear();

        final double oneThird = (testArray.length - 1) / 3;
        final double twoThird = oneThird * 2;
        for (int i = 0; i < (testArray.length - 1); i++) {
            if (i < oneThird) {
                mean1.accept(testArray[i]);
            }
            else if (i < twoThird) {
                mean2.accept(testArray[i]);
            }
            else {
              mean3.accept(testArray[i]);
            }
        }
        // combining mean2 with mean1
        mean1.combine(mean3);
        mean1.combine(mean2);
        assertEquals(newMean, mean1.getMean(), tolerance);
        assertEquals(newCount, mean1.getN());
        assertEquals(newSum, mean1.getSum(), tolerance);
    }

    /**
     * Verifies that  getMean() works properly for POSITIVE_INFINITY, NEGATIVE_INFINITY
     * MAX_VALUE, -MAX_VALUE and NaN values.
     */
    @Test
    public void testSpecialValues() {
        final Mean mean = new Mean();

        mean.accept(Double.POSITIVE_INFINITY);
        mean.accept(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isNaN(mean.getMean()));
        assertEquals(2, mean.getN());
        assertEquals(Double.NaN, mean.getSum(), tolerance);

        mean.clear();
        mean.accept(Double.NEGATIVE_INFINITY);
        mean.accept(Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(mean.getMean()));
        assertEquals(2, mean.getN());
        assertEquals(Double.NaN, mean.getSum(), tolerance);


        mean.clear();
        mean.accept(Double.NaN);
        mean.accept(Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(mean.getMean()));
        assertEquals(2, mean.getN());
        assertEquals(Double.NaN, mean.getSum(), tolerance);


        mean.clear();
        mean.accept(Double.NaN);
        mean.accept(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isNaN(mean.getMean()));
        assertEquals(2, mean.getN());
        assertEquals(Double.NaN, mean.getSum(), tolerance);


        mean.clear();
        mean.accept(Double.NaN);
        mean.accept(0d);
        assertTrue(Double.isNaN(mean.getMean()));
        assertEquals(2, mean.getN());
        assertEquals(Double.NaN, mean.getSum(), tolerance);


        mean.clear();
        mean.accept(Double.MAX_VALUE);
        mean.accept(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, mean.getMean(), tolerance);
        assertEquals(2, mean.getN());
        assertEquals(Double.POSITIVE_INFINITY, mean.getSum(), tolerance);


        mean.clear();
        mean.accept(-Double.MAX_VALUE);
        mean.accept(-Double.MAX_VALUE);
        assertEquals(-Double.MAX_VALUE, mean.getMean(), tolerance);
        assertEquals(2, mean.getN());
        assertEquals(Double.NEGATIVE_INFINITY, mean.getSum(), tolerance);


        mean.clear();
        mean.accept(-Double.MAX_VALUE);
        mean.accept(Double.MAX_VALUE);
        assertEquals(Double.POSITIVE_INFINITY, mean.getMean(), tolerance);
        assertEquals(2, mean.getN());
        assertEquals(0.0, mean.getSum(), tolerance);

    }

    /**Verifies that toString() works properly.*/
    @Test
    public void testToString() {
        Mean mean6 = new Mean();
        StringBuilder expectedString = new StringBuilder();
        expectedString.append("Mean{mean = NaN , count = 0 , sum = 0.000000}");
        assertEquals(expectedString.toString(), mean6.toString());
    }

}
