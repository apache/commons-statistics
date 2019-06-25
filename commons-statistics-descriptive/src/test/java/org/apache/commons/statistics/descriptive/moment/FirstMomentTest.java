package org.apache.commons.statistics.descriptive.moment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.DoubleStream;
import org.junit.jupiter.api.Test;

class FirstMomentTest {
	
	double[] arr= {1,2,3,4,5,6,7,8,9,10};
	DoubleStream doubleStream = DoubleStream.of(arr);
	FirstMoment stats= doubleStream.collect(FirstMoment::new,FirstMoment::accept,FirstMoment::combine);

    @Test
    void testGetMean() {
        assertEquals(5.5,stats.getMean());
    }

    @Test
    void testGetN() {
    	assertEquals(10,stats.getN());
    }

    @Test
    void testGetSum() {
    	assertEquals(55.0,stats.getSum());
    }
    
    @Test
    void testCombine() {
		double[] arr2= {11,12,13,14,15,16,17,18,19,20};
		DoubleStream doubleStream2 = DoubleStream.of(arr2);
		FirstMoment stats2= doubleStream2.collect(FirstMoment::new,FirstMoment::accept,FirstMoment::combine);
		stats2.combine(stats);
		assertEquals(10.5,stats2.getMean());
		assertEquals(20,stats2.getN());
		assertEquals(210,stats2.getSum());
    }

}
