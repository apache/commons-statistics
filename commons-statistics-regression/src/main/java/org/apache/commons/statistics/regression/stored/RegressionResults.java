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
package org.apache.commons.statistics.regression.stored;

public interface RegressionResults {

    /**
     * Gets the regression parameters b.
     *
     * @return The [k,1] array representing b
     */
    double[] getBeta();

    /**
     * Gets the variance of the regression parameters, ie Var(b).
     *
     * @return The [k,k] array representing the variance of b
     */
    double[][] getBetaVariance();

    /**
     * Gets the standard errors of the regression parameters.
     *
     * @return standard errors of returned regression parameters
     */
    double[] getBetaStandardErrors();

    /**
     * Gets the residuals, ie u = y - X*b.
     *
     * @return The [n,1] array representing the residuals
     */
    double[] getResiduals();

    /**
     * Gets the standard error of the regression.
     *
     * @return regression standard error
     */
    double getRegressionStandardError();

    /**
     * Gets the variance of the regressand, ie Var(y).
     *
     * @return The double representing the variance of y
     */
    double getRegressandVariance();

}
