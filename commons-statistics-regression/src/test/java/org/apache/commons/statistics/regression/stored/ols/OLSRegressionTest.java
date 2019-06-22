package org.apache.commons.statistics.regression.stored.ols;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.commons.statistics.regression.stored.RegressionDataLoader;
import org.apache.commons.statistics.regression.stored.ols.OLSRegressionTest;


public class OLSRegressionTest {

	//IN PROGRESS
	
	@Test
	public void loadingData() {
		RegressionDataLoader data = new RegressionDataLoader(null, null);
	}
	
	protected OLSRegression createOLSRegression() {
		OLSRegression reg = new OLSRegression(null);
		
		return reg;
	}
	
	

}
