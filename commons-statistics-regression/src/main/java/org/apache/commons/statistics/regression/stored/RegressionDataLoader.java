package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;
import org.ejml.data.DMatrixRMaj;

public class RegressionDataLoader {
    
    private RegressionRawData inputData;
    
    public RegressionDataLoader(double [] y, double [][] x) {
        inputData = new RegressionRawData(newY(y), newX(x), false);
    }
    
    public RegressionDataLoader() {
        inputData = new RegressionRawData();
    }
    
    public void newSampleData(double [] y, double [][] x) {
        if(inputData == null) 
            inputData = new RegressionRawData(newY(y), newX(x), false);
        
        else {
            inputData.setYData(newY(y));
            inputData.setXData(newX(x));
        }
    }
    
    public void newYData(double [] y) {
        inputData.setYData(newY(y));
    }
    
    public void newXData(double [][] x) {
        inputData.setXData(newX(x));
    }
    
    private static StatisticsMatrix newY(double [] y) {
        return (new StatisticsMatrix(new DMatrixRMaj(y)));
    }
    

    private static StatisticsMatrix newX(double [][] x) {
        return (new StatisticsMatrix(new DMatrixRMaj(x)));
    }
    
    public RegressionData getInputData() {
        return inputData;
    }

}
