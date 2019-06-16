package org.apache.commons.statistics.regression;

import org.apache.commons.statistics.regression.matrix.StatisticsMatrix;

public class RegressionRawData implements RegressionData {

    private StatisticsMatrix xMatrix;
    private StatisticsMatrix yMatrix; // vector
    private Boolean hasIntercept;

    public RegressionRawData(StatisticsMatrix xData, StatisticsMatrix yData, Boolean hasIntercept) {
        this.xMatrix = xData;
        this.yMatrix = yData;
        this.hasIntercept = hasIntercept;
    }
    
    public RegressionRawData() {
        this.xMatrix = null;
        this.yMatrix = null;
        this.hasIntercept = null;
    }

    public void setXData(StatisticsMatrix xMatrix) {
        this.xMatrix = xMatrix;
    }

    public void setYData(StatisticsMatrix yMatrix) {
        this.yMatrix = yMatrix;
    }

    public void setHasIntercept(boolean hasIntercept) {
        this.hasIntercept = hasIntercept;
    }

    public StatisticsMatrix getXData() {
        return this.xMatrix;
    }

    public StatisticsMatrix getYData() {
        return this.yMatrix;
    }

    public Boolean getHasIntercept() {
        return this.hasIntercept;
    }
}