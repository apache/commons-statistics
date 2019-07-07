package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public abstract class RegressionDataHolder {

    protected RegressionData inputData;
    
    public StatisticsMatrix getY() {
        return inputData.getYData();
    }
    
    public StatisticsMatrix getX() {
        return inputData.getXData();
    }
    
}
