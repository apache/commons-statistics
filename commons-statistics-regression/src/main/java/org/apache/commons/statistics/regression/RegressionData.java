package org.apache.commons.statistics.regression;

import org.apache.commons.statistics.regression.matrix.StatisticsMatrix;

public interface RegressionData {

    StatisticsMatrix getXData();

    StatisticsMatrix getYData();

    Boolean getHasIntercept();

}
