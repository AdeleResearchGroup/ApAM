package apam.test.compile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.message.MessageProducer;
import fr.imag.adele.apam.message.MessageConsumer;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s3.S3_2;
import fr.imag.adele.apam.test.s4.S4;

public class S2Impl implements S2, ApamComponent {

    // Apam injected
    Apam      apam;
    S4        s4_1;
    S4        s4_2;
    Set<S3_1> s3s;
    List<S3_1> s3s2;
    S3_2[]     s3_2;
    S3_2       s3;
    Instance   myInst;
    
    M2 	   fM2 ;
    String     name;

    M1 m1 ;
    M2 m2;

    MessageProducer<M1> p1;
    MessageProducer<M2> p2;

    MessageProducer<M1> producerM3;
    
    public String getT2 (String param) {
    	p1.push(m1) ;
    	Map <String, Object> props = new HashMap <String, Object> () ;
    	p2.push(m2, props) ;
    	return "ok" ;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void apamInit(Instance inst) {
        myInst = inst;
        name = inst.getName();
        System.out.println("S2Impl Started : " + inst.getName());
    }

    // @Override
    @Override
    public void callBackS2(String s) {
        System.out.println("Back in S2 : " + s);
    }

    // @Override
    @Override
    public void callS2(String s) {
        int i = 1;
        for (S3_2 s3 : s3_2) {
            s3.callS3_2(i + " from S2Impl");
            i++;

        }
        i = 1;
        for (S3_1 s3 : s3s) {
            s3.callS3_1(i + " from S2Impl");
            i++;
        }

        //        Attributes c2Attrs = new AttributesImpl();
        //        c2Attrs.setProperty(CST.A_SHARED, CST.V_TRUE);
        //        // implBroker.addImpl(compo2, "ApamS4impl", "S4Impl", "S4In", c2Attrs);
        //
        //        Attributes c3Attrs = new AttributesImpl();
        //        c3Attrs.setProperty(CST.A_SHARED, CST.V_FALSE);
        //        // implBroker.addImpl(compo3, "ApamS5impl", "S5Impl", "S5", c3Attrs);

        System.out.println("S2 called " + s);
        if (s4_1 != null)
            s4_1.callS4("depuis S2 (s4_1) ");
        if (s4_2 != null)
            s4_2.callS4("depuis S2 (s4_2) ");
    }

    @Override
    public void apamRemove() {
    }

}
