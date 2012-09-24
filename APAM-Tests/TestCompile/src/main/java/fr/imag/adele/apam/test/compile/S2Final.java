package fr.imag.adele.apam.test.compile;

import fr.imag.adele.apam.test.s2.S2;

public class S2Final implements S2 {

	    public void callS2(String s) {
	    	System.out.println("In S2Final " + s);
	    }

		@Override
		public void callBackS2(String s) {
	    	System.out.println("call back in S2Final " + s);
		}

		@Override
		public String getName() {
			
			return "S2Final";
		}

}
