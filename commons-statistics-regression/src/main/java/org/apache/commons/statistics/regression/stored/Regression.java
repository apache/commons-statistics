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

/**
 * Interface for estimating common regression statistics.
 */
public interface Regression {

    /**
     * Estimates the regression parameters b.
     *
     * @return The [k,1] array representing b
     */
    double[] estimateBeta();

    /**
     * Estimates the variance of the regression parameters, ie Var(b).
     *
     * @return The [k,k] array representing the variance of b
     */
    double[][] estimateBetaVariance();

    /**
     * Returns the standard errors of the regression parameters.
     *
     * @return standard errors of estimated regression parameters
     */
    double[] estimateBetaStandardErrors();

    /**
     * Estimates the residuals, ie u = y - X*b.
     *
     * @return The [n,1] array representing the residuals
     */
    double[] estimateResiduals();

    /**
     * Estimates the standard error of the regression.
     *
     * @return regression standard error
     */
    double estimateRegressionStandardError();

    /**
     * Estimates the variance of the regressand, ie Var(y).
     *
     * @return The double representing the variance of y
     */
    double estimateRegressandVariance();

}
