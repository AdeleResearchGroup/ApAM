package fr.imag.adele.apam.pax.test.performance.util;

import java.util.Calendar;
import java.util.Date;

public class Checkpoint {

	private Date current;
	private long nanotime;
	private long freememory;
	private long totalmemory;
	
	public Checkpoint(){
		this.current=Calendar.getInstance().getTime();
		this.nanotime=System.nanoTime();
		this.freememory=Runtime.getRuntime().freeMemory();
		this.totalmemory=Runtime.getRuntime().totalMemory();
	}

	public Date getCurrent() {
		return current;
	}

	public long getNanotime() {
		return nanotime;
	}

	public long getFreememory() {
		return freememory;
	}

	public long getTotalmemory() {
		return totalmemory;
	}
	
}
