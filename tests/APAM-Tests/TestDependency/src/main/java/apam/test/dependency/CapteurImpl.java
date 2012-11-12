package apam.test.dependency;

import apam.test.attr.CapteurTemp;
import apam.test.attr.ConfCapteur;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

//import fr.imag.adele.apam.test.s5.S5;

public class CapteurImpl implements CapteurTemp, ConfCapteur, ApamComponent {


    String name;

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int getTemp() {
        return 21;
    }
    @Override
    public String getMaker() {
        return "Adele";
    }

    public String toString () {
    	return name ;
    }
    
    @Override
    public void apamInit(Instance inst) {
        name = inst.getName();
        System.out.println("Capteur Started : " + inst.getName());
    }

//    @Override
//    public void callS3_1(String s) {
//        System.out.println("S3_1 called " + s);
////        s4.callS4("from S3Impl");
//    }
//
//    @Override
//    public void callS3_2(String s) {
//        System.out.println("S3_2 called " + s);
//    }
//
//    @Override
//    public void callS3_1toS5(String msg) {
//        System.out.println("S3_1 to S5 called : " + msg);
////        s4.callS4("from S3Impl");
////        s5.callS5("from S3_1toS5 " + msg);
//    }

    @Override
    public void apamRemove() {
    }
}
