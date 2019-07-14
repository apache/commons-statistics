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

class StandardDeviationTest {

    protected double mean = 12.404545454545455d;
    protected double combMean = 13.573076923077d;
    protected double var = 10.00235930735931d;
    protected double std = 3.1626506774159;
    protected double combVar = 19.812446153846d;
    protected double combStd = 4.4511174050845;
    protected double tolerance = 10E-12;
    protected double[] testArray =
    {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0,  8.2, 10.3, 11.3,
        14.1,  9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0,  8.8,
        9.0, 12.3 };

    /**Returns StandardDeviation instance.*/
    public StandardDeviation getStandardDeviationInst() {
        return new StandardDeviation();
    }

    /**Expected StandardDeviation value for testArray. */
    public double expectedValue()  {
        return this.std;
    }

    /**Expected StandardDeviation value for Combined Array of testArray and array. */
    public double combExpectedValue() {
        return this.combStd;
    }

    /**Getter for tolerance value.*/
    public double getTolerance() {
        return this.tolerance;
    }

    /**Getter for mean value of testArray.*/
    public double  getMean() {
        return this.mean;
    }

    /**Getter for combined mean value of testArray and array.*/
    public double  getCombMean() {
        return this.combMean;
    }

    /** Verifies that accept(), getStandardDeviation(),getN(), getMean() works properly. */
    @Test
    public void testStandardDeviation() {
        StandardDeviation sd1 = getStandardDeviationInst();
        // Add testArray one value at a time and check result
        for (int i = 0; i < testArray.length; i++) {
            sd1.accept(testArray[i]);
        }
        assertEquals(expectedValue(), sd1.getStandardDeviation(), getTolerance());
        assertEquals(testArray.length, sd1.getN());
        assertEquals(getMean(), sd1.getMean(), getTolerance());
    }

    /** Verifies that combine() works properly. */
    @Test
    public void testCombine() {
        StandardDeviation sd2 = getStandardDeviationInst();
        StandardDeviation sd3 = getStandardDeviationInst();
        final double[] array = {5 * 2 + 4, 5 * 2 +  7, 5 * 2 + 13, 5 * 2 + 16};
        // Add array one value at a time in sd2 object and check result
        for (int i = 0; i < testArray.length; i++) {
            sd2.accept(testArray[i]);
        }
        assertEquals(expectedValue(), sd2.getStandardDeviation(), getTolerance());
        assertEquals(testArray.length, sd2.getN());
        assertEquals(getMean(), sd2.getMean(), getTolerance());
        // Add array one value at a time in sd3 object and check result
        for (int i = 0; i < array.length; i++) {
            sd3.accept(array[i]);
        }
        // combining sd2 with sd3
        sd2.combine(sd3);
        // Testing getStandardDeviation(isBiasCorrected)
        assertEquals(combExpectedValue(), sd2.getStandardDeviation(true), getTolerance());
        assertEquals(testArray.length + array.length, sd2.getN());
        assertEquals(getCombMean(), sd2.getMean(), getTolerance());
    }

    /** Make sure Double.NaN is returned iff n = 0 or 1 */
    @Test
    public void testNaN() {
        StandardDeviation sd4 = getStandardDeviationInst();
        assertTrue(Double.isNaN(sd4.getStandardDeviation()));
        sd4.accept(1d);
        assertTrue(Double.isNaN(sd4.getStandardDeviation()));
        sd4.accept(3d);
        assertEquals(1.4142135623731d, sd4.getStandardDeviation(), 1E-14);
    }

    /** Test population version of Variance2. */
    @Test
    public void testPopulation() {
        final double[] values = {-1.0d, 3.1d, 4.0d, -2.1d, 22d, 11.7d, 3d, 14d};
        final double[] combArray = {-1.0d, 3.1d, 4.0d, -2.1d, 22d, 11.7d, 3d, 14d, 12.5, 12.0, 11.8,
            14.2, 14.9, 14.5, 21.0,  8.2, 10.3, 11.3, 14.1,  9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0,
            10.0, 8.8, 9.0, 12.3};
        StandardDeviation sd5 = getStandardDeviationInst();
        for (int i = 0; i < values.length; i++) {
            sd5.accept(values[i]);
        }
        sd5.setBiasCorrected(false);
        assertEquals(populationStandardDeviation(values), sd5.getStandardDeviation(), 1E-14);
        //population StandardDeviation after combine
        StandardDeviation sd6 = getStandardDeviationInst();
        for (int i = 0; i < testArray.length; i++) {
            sd6.accept(testArray[i]);
        }
        // combining sd5 with sd6
        sd5.combine(sd6);
        // Testing getStandardDeviation(isBiasCorrected())
        assertEquals(populationStandardDeviation(combArray), sd5.getStandardDeviation(false), 1E-14);
    }

    /** Definitional formula for population StandardDeviation. */
    protected double populationStandardDeviation(double[] v) {
        double meanVal;
        double arraySum = 0;
        for (int j = 0; j < v.length; j++) {
            arraySum += v[j];
        }
        meanVal = arraySum / v.length;
        double sum = 0;
        for (int i = 0; i < v.length; i++) {
            sum += (v[i] - meanVal) * (v[i] - meanVal);
        }
        return Math.sqrt(sum / v.length);
    }

    /**Verifies that toString() works properly.*/
    @Test
    public void testToString() {
        StandardDeviation sd7 = getStandardDeviationInst();
        StringBuilder expectedString = new StringBuilder();
        expectedString.append(sd7.getClass().getSimpleName() + "{standard deviation = " + sd7.getStandardDeviation() + "}");
        assertEquals(expectedString.toString(), sd7.toString());
    }

}
