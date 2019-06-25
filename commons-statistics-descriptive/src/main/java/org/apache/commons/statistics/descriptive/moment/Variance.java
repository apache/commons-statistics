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

/**
 * 
 * <pre> {@code
 * Variance stats = doubleStream.collect(Variance::new,
 *                                       Variance::accept,
 *                                       Variance::combine);
 * }</pre>
 */
public class Variance {

    /***/
    private boolean isBiasCorrected = true; 
    /***/
    private double m = 0;

    /***/
    private double s = 0;

    /***/
    private double x = 0;

    /***/
    private double oldm = 0;

    /***/
    private double variance = 0;

    /***/
    private long countN = 0;

   /**
    * This method calculates Variance based on Welford's Algorithm.
    * The Welford's Algorithm is as follows:<br>
    *<pre><code>
    *variance(samples):
    *    m := 0
    *    s := 0
    *    for k from 1 to N:
    *        x := samples[k]
    *        oldm := m
    *        m := m + (x-m)/k
    *        s := s + (x-m)*(x-oldm)
    *    return s/(N-1)</code></pre>
    *@param value stream of values
    */
   public void accept( double value) {   
	   countN++;
       x = value;
       oldm = m;
       m += (x - m) / countN;
       s += (x - m) * (x - oldm);
       variance = s / (countN - 1);
   }
   
   public void combine(Variance var2) {
	   double delta = var2.getm() - m;
	   double m_a = variance * (countN - 1);
	   double m_b = var2.getVariance() *(var2.getN()- 1);
	   double M2 = m_a + m_b + Math.pow(delta, 2)* countN *var2.getN() / (countN + var2.getN());
	   variance = M2 / (countN + var2.getN()- 1);
       countN = getN() + var2.getN();
       m = getm() + var2.getm();
	   
   }
   
   public double getVariance() {
	   return variance;
   }
  
   /**
    * This method gives the count of values added.
    * @return countN-count of values
    */
   public long getN() {
       return countN;
   }

   public double getm() {
       return m;
   }

   public boolean isBiasCorrected() {
       return isBiasCorrected;
   }

   public void setBiasCorrected(boolean biasCorrected) {
       this.isBiasCorrected = biasCorrected;
   }
   
   /**
    * {@inheritDoc}
    *
    * Returns a non-empty string representation of this object suitable for
    * debugging. The exact presentation format is unspecified and may vary
    * between implementations and versions.
    */
   @Override
   public String toString() {
       return String.format(
           "%s{variance = %f}",
           this.getClass().getSimpleName(),
           getVariance());
   } 
}