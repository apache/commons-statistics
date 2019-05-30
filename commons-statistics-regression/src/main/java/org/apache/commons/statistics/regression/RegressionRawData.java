package org.apache.commons.statistics.regression;

import org.apache.commons.statistics.regression.matrix.StatisticsMatrix;

public class RegressionRawData {

    private StatisticsMatrix xMatrix;
    private StatisticsMatrix yMatrix; // vector
    private boolean hasIntercept;
    
    public RegressionRawData(StatisticsMatrix xData, StatisticsMatrix yData, boolean hasIntercept) {
        this.xMatrix = xData;
        this.yMatrix = yData;
        this.hasIntercept = hasIntercept;
    }

    
    public StatisticsMatrix getXData() {
        return this.xMatrix;
    }

    public StatisticsMatrix getYData() {
        return this.yMatrix;
    }

    public boolean getHasIntercept() {
        return this.hasIntercept;
    }
}