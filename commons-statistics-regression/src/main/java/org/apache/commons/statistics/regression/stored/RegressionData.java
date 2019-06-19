package org.apache.commons.statistics.regression.stored;

import org.apache.commons.statistics.regression.util.matrix.StatisticsMatrix;

public interface RegressionData {

    StatisticsMatrix getXData();

    StatisticsMatrix getYData();

    boolean getHasIntercept();

}
