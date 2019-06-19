package org.apache.commons.statistics.regression.stored.ols;

import static org.junit.Assert.*;

import org.apache.commons.statistics.regression.stored.RegressionDataLoader;
import org.apache.commons.statistics.regression.stored.ols.OLSRegressionTest;
import org.junit.Test;

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
