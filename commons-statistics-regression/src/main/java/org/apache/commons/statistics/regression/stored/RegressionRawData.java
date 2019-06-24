package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public class RegressionRawData implements RegressionData {

    private StatisticsMatrix yMatrix; // vector
    private StatisticsMatrix xMatrix;
    private boolean hasIntercept = true;

    public RegressionRawData(StatisticsMatrix yData, StatisticsMatrix xData, Boolean hasIntercept) {
        this.yMatrix = yData;
        this.xMatrix = xData;
        this.hasIntercept = hasIntercept;
    }
    
    public RegressionRawData() {
        this.xMatrix = null;
        this.yMatrix = null;
        this.hasIntercept = true;
    }

    public void setYData(StatisticsMatrix yMatrix) {
        this.yMatrix = yMatrix;
    }
    
    public void setXData(StatisticsMatrix xMatrix) {
        this.xMatrix = xMatrix;
    }

    public void setHasIntercept(boolean hasIntercept) {
        this.hasIntercept = hasIntercept;
    }

    public StatisticsMatrix getYData() {
        return this.yMatrix;
    }
    
    public StatisticsMatrix getXData() {
        return this.xMatrix;
    }

    public boolean getHasIntercept() {
        return this.hasIntercept;
    }
}