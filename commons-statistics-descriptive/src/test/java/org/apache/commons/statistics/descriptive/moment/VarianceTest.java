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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

class VarianceTest {
    protected double mean = 12.404545454545455d;
    protected double combMean = 13.573076923077d;
    protected double var = 10.00235930735931d;
    protected double combVar = 19.812446153846d;
    protected double tolerance = 10E-12;
    protected double[] testArray =
    {12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0,  8.2, 10.3, 11.3,
        14.1,  9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0,  8.8,
        9.0, 12.3 };

    /**Returns Variance instance.*/
    public Variance getVarianceInst() {
        return new Variance();
    }

    /**Expected variance value for testArray. */
    public double expectedValue()  {
        return this.var;
    }

    /**Expected variance value for Combined Array of testArray and array. */
    public double combExpectedValue() {
        return this.combVar;
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

    /** Verifies that accept(), getVariance(),getN(), getMean() works properly. */
    @Test
    public void testVariance() {
        Variance var2 = getVarianceInst();
        // Add testArray one value at a time and check result
        for (int i = 0; i < testArray.length; i++) {
            var2.accept(testArray[i]);
        }
        assertEquals(expectedValue(), var2.getVariance(), getTolerance());
        assertEquals(testArray.length, var2.getN());
        assertEquals(getMean(), var2.getMean(), getTolerance());
    }

    /** Verifies that combine() works properly. */
    @Test
    public void testCombine() {
        Variance var3 = getVarianceInst();
        Variance var4 = getVarianceInst();
        final double[] array = {5 * 2 + 4, 5 * 2 +  7, 5 * 2 + 13, 5 * 2 + 16};
        // Add array one value at a time in var1 object and check result
        for (int i = 0; i < testArray.length; i++) {
            var3.accept(testArray[i]);
        }
        assertEquals(expectedValue(), var3.getVariance(), getTolerance());
        assertEquals(testArray.length, var3.getN());
        assertEquals(getMean(), var3.getMean(), getTolerance());
        // Add array one value at a time in var2 object and check result
        for (int i = 0; i < array.length; i++) {
            var4.accept(array[i]);
        }
        // combining var2 with var1
        var3.combine(var4);
        // Testing getVariance(isBiasCorrected())
        assertEquals(combExpectedValue(), var3.getVariance(true), getTolerance());
        assertEquals(testArray.length + array.length, var3.getN());
        assertEquals(getCombMean(), var3.getMean(), getTolerance());
    }

    /** Make sure Double.NaN is returned iff n = 0 or 1 */
    @Test
    public void testNaN() {
        Variance var5 = getVarianceInst();
        assertTrue(Double.isNaN(var5.getVariance()));
        var5.accept(1d);
        assertTrue(Double.isNaN(var5.getVariance()));
        var5.accept(3d);
        assertEquals(2d, var5.getVariance(), 1E-14);
    }

    /** Test population version of variance. */
    @Test
    public void testPopulation() {
        final double[] values = {-1.0d, 3.1d, 4.0d, -2.1d, 22d, 11.7d, 3d, 14d};
        final double[] combArray = {-1.0d, 3.1d, 4.0d, -2.1d, 22d, 11.7d, 3d, 14d, 12.5, 12.0, 11.8,
            14.2, 14.9, 14.5, 21.0,  8.2, 10.3, 11.3, 14.1,  9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0,
            10.0, 8.8, 9.0, 12.3};
        Variance v1 = getVarianceInst();
        for (int i = 0; i < values.length; i++) {
            v1.accept(values[i]);
        }

        v1.setBiasCorrected(false);
        assertEquals(populationVariance(values), v1.getVariance(), 1E-14);
        //population variance after combine
        Variance var2 = getVarianceInst();
        for (int i = 0; i < testArray.length; i++) {
            var2.accept(testArray[i]);
        }
        // combining var1 with var2
        v1.combine(var2);
        // Testing getVariance(isBiasCorrected())
        assertEquals(populationVariance(combArray), v1.getVariance(false), 1E-14);
    }

    /** Definitional formula for population variance. */
    protected double populationVariance(double[] v) {
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
        return sum / v.length;
    }


    /**Verifies that toString() works properly.*/
    @Test
    public void testToString() {
        Variance var3 = getVarianceInst();
        StringBuilder expectedString = new StringBuilder();
        expectedString.append(var3.getClass().getSimpleName() + "{variance = " + var3.getVariance() + "}");
        assertEquals(expectedString.toString(), var3.toString());
    }

}
