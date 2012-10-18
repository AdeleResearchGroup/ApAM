package apam.test.compile;

import java.util.Set;

import fr.imag.adele.apam.test.s2.S2;

public class S2Final implements S2 {

	    public void callS2(String s) {
	    	System.out.println("In S2Final " + s);
	    }

	    Set<S2> fieldS2 ;
	    S2 anotherS2 ;
	    M1 m1 ;
	    
	    public M1 getM1 () { return null;}
	    public M1 getAlsoM1 () { return null;}	    
	   	    
	    //	    
//	    public void newT (S2 t) { }
//	    public void newT (Instance i) { }
//	    
//	    public void removedS2 () {}
//	    
	    
		@Override
		public void callBackS2(String s) {
	    	System.out.println("call back in S2Final " + s);
		}

		@Override
		public String getName() {
			
			return "S2Final";
		}

}
