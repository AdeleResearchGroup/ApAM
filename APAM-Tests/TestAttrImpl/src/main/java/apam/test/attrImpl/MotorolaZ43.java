package apam.test.attrImpl;

import apam.test.attr.CapteurTemp;
import apam.test.attr.ConfCapteur;

public class MotorolaZ43 implements CapteurTemp, ConfCapteur{
     public int getTemp () {
    	 return 845 ;
     }
     public String getName () {
    	 return "MotorolaZ43" ;
     }
	@Override
	public String getMaker() {
		return "Adele";
	}
}
