package fr.imag.adele.apam.pax.test.performance.util;

public class MeasureHolder {

	long timemilis;
	long timenano;
	Checkpoint c1;
	Checkpoint c2;
	
	public MeasureHolder(Checkpoint c1,Checkpoint c2,long timemilis,long nanotime){
		this.timemilis=timemilis;
		this.timenano=nanotime;
		this.c1=c1;
		this.c2=c2;
	}

	public long getTimemilis() {
		return timemilis;
	}
	
	public long getTimenano() {
		return timenano;
	}

	public String asSeconds(){
		return String.format("%.5f",(float)((float)getTimenano()/(float)1000000000));
	}
	
	public String asNano(){
		return String.format("%d", getTimenano());
	}
	
	public String asMili(){
		
		//System.out.println(String.format("%d / %d equals %f, reduced %.5f ",getTimenano(),1000000, (float)((float)getTimenano()/(float)1000000),(float)((float)getTimenano()/(float)1000000)));
		
		return String.format("%.5f", (float)((float)getTimenano()/(float)1000000));
	}	

	public String usedMemory(){
		return String.format("%d", c1.getFreememory()-c2.getFreememory());
	}
	
}
