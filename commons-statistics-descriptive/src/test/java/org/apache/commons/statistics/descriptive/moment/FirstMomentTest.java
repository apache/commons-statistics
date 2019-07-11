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
 *Test cases for {@link FirstMoment} class.
 */
class FirstMomentTest {

    protected double mean = 12.404545454545455d;
    protected double combMean = 13.573076923077d;
    protected double sum = 272.90d;
    protected double combSum = 352.90d;
    protected double tolerance = 10E-12;

    protected double[] testArray =
        {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0,  8.2, 10.3, 11.3,
            14.1,  9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0,  8.8,
            9.0, 12.3 };

    /**Getter for tolerance value.*/
    public double getTolerance() {
        return tolerance;
    }

    /**Expected mean value for testArray. */
    public double expectedValue()  {
        return this.mean;
    }

    /**Expected mean value for Combined Array of testArray and array. */
    public double combExpectedValue()  {
        return this.combMean;
    }

    /**Getter for Sum value of testArray.*/
    public double getSum()  {
        return this.sum;
    }

    /**Getter for combined sum value of testArray and array.*/
    public double getCombSum()  {
        return this.combSum;
    }

    /**Returns FirstMoment instance.*/
    public FirstMoment getFirstMomentInst() {
        return new FirstMoment();
    }

    /** Verifies that accept(), getMean(),getN(), getSum() works properly. */
    @Test
    public void testFirstMoment() {
        FirstMoment meanX = getFirstMomentInst();
        // Add testArray one value at a time and check result
        for (int i = 0; i < testArray.length; i++) {
            meanX.accept(testArray[i]);
        }
        assertEquals(expectedValue(), meanX.getMean(), getTolerance());
        assertEquals(testArray.length, meanX.getN());
        assertEquals(getSum(), meanX.getSum(), getTolerance());
    }

    /**Verifies that  getMean() works properly for a single value. */
    @Test
    public void testSmallSamples() {
        FirstMoment meanY = getFirstMomentInst();
        assertTrue(Double.isNaN(meanY.getMean()));
        meanY.accept(1d);
        assertEquals(1d, meanY.getMean(), 0);
    }

    /** Verifies that combine() works properly. */
    @Test
    public void testCombine() {
        FirstMoment mean1 = getFirstMomentInst();
        FirstMoment mean2 = getFirstMomentInst();
        final double[] array = {5 * 2 + 4, 5 * 2 +  7, 5 * 2 + 13, 5 * 2 + 16};
        // Add array one value at a time in var1 object and check result
        for (int i = 0; i < testArray.length; i++) {
            mean1.accept(testArray[i]);
        }
        assertEquals(expectedValue(), mean1.getMean(), getTolerance());
        assertEquals(testArray.length, mean1.getN());
        assertEquals(getSum(), mean1.getSum(), getTolerance());
        // Add array one value at a time in var2 object and check result
        for (int i = 0; i < array.length; i++) {
            mean2.accept(array[i]);
        }
        // combining var2 with var1
        mean1.combine(mean2);
        assertEquals(combExpectedValue(), mean1.getMean(), getTolerance());
        assertEquals(testArray.length + array.length, mean1.getN());
        assertEquals(getCombSum(), mean1.getSum(), getTolerance());
    }

    /**Verifies that  getMean() works properly for POSITIVE_INFINITY, NEGATIVE_INFINITY
     * and NaN values.
     */
    @Test
    public void testSpecialValues() {
        final FirstMoment meanZ = getFirstMomentInst();

        //mean.clear();
        meanZ.accept(Double.POSITIVE_INFINITY);
        meanZ.accept(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isNaN(meanZ.getMean()));

        //mean.clear();
        final FirstMoment mean2 = getFirstMomentInst();
        mean2.accept(Double.NEGATIVE_INFINITY);
        mean2.accept(Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(mean2.getMean()));

        //mean.clear();
        final FirstMoment mean3 = getFirstMomentInst();
        mean3.accept(Double.NaN);
        mean3.accept(Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(mean3.getMean()));

        //mean.clear();
        final FirstMoment mean4 = getFirstMomentInst();
        mean4.accept(Double.NaN);
        mean4.accept(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isNaN(mean4.getMean()));

        //mean.clear();
        final FirstMoment mean5 = getFirstMomentInst();
        mean5.accept(Double.NaN);
        mean5.accept(0d);
        assertTrue(Double.isNaN(mean5.getMean()));
    }

    /**Verifies that toString() works properly.*/
    @Test
    public void testToString() {
        FirstMoment mean6 = getFirstMomentInst();
        StringBuilder expectedString = new StringBuilder();
        expectedString.append(mean6.getClass().getSimpleName() + "{mean=" + mean6.getMean() + "}");
        assertEquals(expectedString.toString(), mean6.toString());
    }

}
