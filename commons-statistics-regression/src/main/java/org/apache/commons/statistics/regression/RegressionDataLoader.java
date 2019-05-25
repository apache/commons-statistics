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
package org.apache.commons.statistics.regression;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

public class RegressionDataLoader {
    
    private StatisticsMatrix xMatrix;
    private StatisticsMatrix yMatrix; //vector
    private boolean hasIntercept;
    
    
    public RegressionDataLoader() {
    	this.xMatrix = null;
    	this.yMatrix = null;
    }
    
    public RegressionDataLoader(double[][] y, double[][] x, boolean hasIntercept) {
        this.xMatrix = new StatisticsMatrix(new DMatrixRMaj(x));
        this.yMatrix = new StatisticsMatrix(new DMatrixRMaj(y));
    }
    // how to convert 1D array to 2D
    // Vectors in SimpleBase
    public void newY(double[] y) {
    	this.yMatrix = new StatisticsMatrix(new DMatrixRMaj(y));
    }
    
    public void newX(double[][] x) {
    	this.xMatrix = new StatisticsMatrix(new DMatrixRMaj(x));
    }
    
    
    //test
}
