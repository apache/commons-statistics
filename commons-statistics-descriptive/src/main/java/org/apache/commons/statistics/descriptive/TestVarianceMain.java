package org.apache.commons.statistics.descriptive;

import java.util.stream.*;

import org.apache.commons.statistics.descriptive.moment.FirstMoment;
import org.apache.commons.statistics.descriptive.moment.Variance;

public class TestVarianceMain {
	static double[] arr= {1,2,3,4,5,6,7,8,9,10};
	static DoubleStream doubleStream = DoubleStream.of(arr);
	static Variance stats = doubleStream.collect(Variance::new, Variance::accept, Variance::combine);
	//System.out.println(stats);
	public static void main(String[] args) {
		double[] arr2= {11,12,13,14,15,16,17,18,19,20};
		DoubleStream doubleStream2 = DoubleStream.of(arr2);
		Variance stats2= doubleStream2.collect(Variance::new,Variance::accept,Variance::combine);
		System.out.println(stats2);
		
		System.out.println("---------------------------------------------------");
		stats2.combine(stats);
		System.out.println(stats2);
		
		System.out.println(stats2.getVariance());
		System.out.println(stats2.getN());
		System.out.println(stats2.getMean());
		
	}
	
}
