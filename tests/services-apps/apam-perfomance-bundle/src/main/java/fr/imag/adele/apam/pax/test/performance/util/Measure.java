package fr.imag.adele.apam.pax.test.performance.util;

public class Measure {

	public static MeasureHolder time(Checkpoint c1,Checkpoint c2){
		
		long milis=c2.getCurrent().getTime()-c1.getCurrent().getTime();
		long nano=c2.getNanotime()-c1.getNanotime();
		
		return new MeasureHolder(c1,c2,milis,nano);
	}

	
}
