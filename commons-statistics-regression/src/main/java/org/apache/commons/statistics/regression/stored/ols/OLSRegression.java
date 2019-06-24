package org.apache.commons.statistics.regression.stored.ols;

import org.apache.commons.statistics.regression.stored.RegressionData;
import org.apache.commons.statistics.regression.stored.RegressionDataLoader;

public class OLSRegression {
    
    private OLSEstimators betas;
    private OLSResiduals residuals;
    
    public RegressionData inputData;
    
    public OLSRegression(RegressionDataLoader loader) {
        this.inputData = loader.getInputData();
    }
    
    
}
