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

        Mean meanY = new Mean();
        // After Multiple Additions
        // Add testArray one value at a time and check result
        for (int i = 0; i < testArray.length; i++) {
            meanY.accept(testArray[i]);
        }
        assertEquals(mean, meanY.getMean(), tolerance);
        assertEquals(testArray.length, meanY.getN());
        assertEquals(sum, meanY.getSum(), tolerance);
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

        //Combining a non-empty Mean to another empty Mean.
        Mean mean3 = new Mean();
        Mean mean4 = new Mean();
        mean3.accept(5);
        mean4.combine(mean3);
        assertEquals(5, mean4.getMean(), tolerance);
        assertEquals(1, mean4.getN());
        assertEquals(5, mean4.getSum(), tolerance);

        //Combining two Mean objects with finite values.
        Mean mean5 = new Mean();
        Mean mean6 = new Mean();
        // Add testArray, one value at a time in mean5 object and check result
        for (int i = 0; i < testArray.length; i++) {
            mean5.accept(testArray[i]);
        }
        assertEquals(mean, mean5.getMean(), tolerance);
        assertEquals(testArray.length, mean5.getN());
        assertEquals(sum, mean5.getSum(), tolerance);

        // Add testArray2, one value at a time in mean6 object.
        for (int i = 0; i < testArray2.length; i++) {
            mean6.accept(testArray2[i]);
        }
        // combining mean6 with mean5
        mean5.combine(mean6);
        assertEquals(combMean, mean5.getMean(), tolerance);
        assertEquals(testArray.length + testArray2.length, mean5.getN());
        assertEquals(combSum, mean5.getSum(), tolerance);
    }

    /**
     * Verifies that getMean(),getN(), getSum() works properly when data is split
     * and joined.
     */
    @Test
    public void testSplit() {

        Mean mean1 = new Mean();


        // Add all values of testArray at once in the Mean object and check result.
        for (int i = 0; i < testArray.length; i++) {
            mean1.accept(testArray[i]);
        }
        assertEquals(mean, mean1.getMean(), tolerance);
        assertEquals(testArray.length, mean1.getN());
        assertEquals(sum, mean1.getSum(), tolerance);

        // Divide testArray in two equal parts and add each part in a Mean object.
        // Then combine them and check result
        Mean mean2 = new Mean();
        Mean mean3 = new Mean();

        double half = testArray.length / 2;
        for (int i = 0; i < testArray.length; i++) {
            if (i < half) {
                mean2.accept(testArray[i]);
            } else {
                mean3.accept(testArray[i]);
            }
        }
        // combining mean3 with mean2
        mean2.combine(mean3);
        assertEquals(mean, mean2.getMean(), tolerance);
        assertEquals(testArray.length, mean2.getN());
        assertEquals(sum, mean2.getSum(), tolerance);

        // Divide testArray in three equal parts and add each part in a Mean object.
        // Then combine them and check result
        // Add all values of testArray at once in the Mean object and check result.
        Mean mean4 = new Mean();

        double newMean = 12.409523809524d;
        double newSum = 260.60d;
        long newCount = 21;
        for (int i = 0; i < testArray.length - 1; i++) {
            mean4.accept(testArray[i]);
        }
        assertEquals(newMean, mean4.getMean(), tolerance);
        assertEquals(newCount, mean4.getN());
        assertEquals(newSum, mean4.getSum(), tolerance);

        Mean mean5 = new Mean();
        Mean mean6 = new Mean();
        Mean mean7 = new Mean();

        double oneThird = (testArray.length - 1) / 3;
        double twoThird = oneThird * 2;
        for (int i = 0; i < (testArray.length - 1); i++) {
            if (i < oneThird) {
                mean5.accept(testArray[i]);
            } else if (i < twoThird) {
                mean6.accept(testArray[i]);
            } else {
                mean7.accept(testArray[i]);
            }
        }
        // combining mean6 and mean7 with mean5
        mean5.combine(mean6);
        mean5.combine(mean7);
        assertEquals(newMean, mean5.getMean(), tolerance);
        assertEquals(newCount, mean5.getN());
        assertEquals(newSum, mean5.getSum(), tolerance);
    }

    /**
     * Verifies that  getMean() works properly for POSITIVE_INFINITY, NEGATIVE_INFINITY
     * MAX_VALUE, -MAX_VALUE and NaN values.
     */
    @Test
    public void testSpecialValues() {
        Mean meanS = new Mean();

        meanS.accept(Double.POSITIVE_INFINITY);
        meanS.accept(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isNaN(meanS.getMean()));
        assertEquals(2, meanS.getN());
        assertEquals(Double.NaN, meanS.getSum(), tolerance);

        Mean meanS1 = new Mean();
        meanS1.accept(Double.NEGATIVE_INFINITY);
        meanS1.accept(Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(meanS1.getMean()));
        assertEquals(2, meanS1.getN());
        assertEquals(Double.NaN, meanS1.getSum(), tolerance);


        Mean meanS2 = new Mean();
        meanS2.accept(Double.NaN);
        meanS2.accept(Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(meanS2.getMean()));
        assertEquals(2, meanS2.getN());
        assertEquals(Double.NaN, meanS2.getSum(), tolerance);


        Mean meanS3 = new Mean();
        meanS3.accept(Double.NaN);
        meanS3.accept(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isNaN(meanS3.getMean()));
        assertEquals(2, meanS3.getN());
        assertEquals(Double.NaN, meanS3.getSum(), tolerance);


        Mean meanS4 = new Mean();
        meanS4.accept(Double.NaN);
        meanS4.accept(0d);
        assertTrue(Double.isNaN(meanS4.getMean()));
        assertEquals(2, meanS4.getN());
        assertEquals(Double.NaN, meanS4.getSum(), tolerance);


        Mean meanS5 = new Mean();
        meanS5.accept(Double.MAX_VALUE);
        meanS5.accept(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, meanS5.getMean(), tolerance);
        assertEquals(2, meanS5.getN());
        assertEquals(Double.POSITIVE_INFINITY, meanS5.getSum(), tolerance);


        Mean meanS6 = new Mean();
        meanS6.accept(-Double.MAX_VALUE);
        meanS6.accept(-Double.MAX_VALUE);
        assertEquals(-Double.MAX_VALUE, meanS6.getMean(), tolerance);
        assertEquals(2, meanS6.getN());
        assertEquals(Double.NEGATIVE_INFINITY, meanS6.getSum(), tolerance);


        Mean meanS7 = new Mean();
        meanS7.accept(-Double.MAX_VALUE);
        meanS7.accept(Double.MAX_VALUE);
        assertEquals(Double.POSITIVE_INFINITY, meanS7.getMean(), tolerance);
        assertEquals(2, meanS7.getN());
        assertEquals(0.0, meanS7.getSum(), tolerance);
    }

    /**Verifies that toString() works properly.*/
    @Test
    public void testToString() {
        Mean mean8 = new Mean();
        StringBuilder expectedString = new StringBuilder();
        expectedString.append("Mean{mean = NaN , count = 0 , sum = 0.000000}");
        assertEquals(expectedString.toString(), mean8.toString());
    }

}
