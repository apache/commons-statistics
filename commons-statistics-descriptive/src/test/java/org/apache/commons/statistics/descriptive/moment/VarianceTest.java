package org.apache.commons.statistics.descriptive.moment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.DoubleStream;
import org.junit.jupiter.api.Test;

class VarianceTest {
	double[] arr= {1,2,3,4,5,6,7,8,9,10};
	DoubleStream doubleStream = DoubleStream.of(arr);
	Variance stats = doubleStream.collect(Variance::new, Variance::accept, Variance::combine);
	@Test
	void testGetVariance() {
		assertEquals(9.166666666666666,stats.getVariance());
	}
	
	@Test
	void testGetN() {
		assertEquals(10,stats.getN());
	}
	
	@Test
	void testGetMean() {
		assertEquals(5.5,stats.getMean());
	}

	@Test
	void testCombine() {
		double[] arr3= {1,2,3,4,5,6,7,8,9,10};
		DoubleStream doubleStream3 = DoubleStream.of(arr3);
		Variance stats3 = doubleStream3.collect(Variance::new, Variance::accept, Variance::combine);
		
		double[] arr2= {11,12,13,14,15,16,17,18,19,20};
		DoubleStream doubleStream2 = DoubleStream.of(arr2);
		Variance stats2= doubleStream2.collect(Variance::new, Variance::accept, Variance::combine);
		stats2.combine(stats3);
		assertEquals(35.0,stats2.getVariance());
		assertEquals(20,stats2.getN());
		assertEquals(10.5,stats2.getMean());
	}
	
	
}
