package fr.imag.adele.apam.test.dependency;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s3.S3_2;
import fr.imag.adele.apam.test.s4.S4;

public class Dependency implements S2, ApamComponent, Runnable {

    // Apam injected
    Apam      apam;
    S4        s4_1;
    S4        s4_2;
    S4        s4_3;
    Set<S3_1> s3s;
    List<S3_1> s3s2;
    S3_2[]     s3_2;
    S3_2       s3;
    
    Instance   myInst;
    String     name;
    
	public void assertTrue (boolean test) {
		if (!test) {
			new Exception ("Assertion failed. Not true.").printStackTrace();
		}
	}

	public void assertEquals (Object left, Object right) {
		if (left == right) return ;
		if (left == null) {
			new Exception ("Assertion Equals failed: left side is null; right side = " + right).printStackTrace();
			return ;
		}
		if (right == null) {
			new Exception ("Assertion Equals failed right side is null; left side = " + left).printStackTrace();	
			return ;
		}
		if (left instanceof String && right instanceof String) {
				if (!left.equals(right)) {
					new Exception ("Assertion Equals failed: " + left + " != " + right).printStackTrace();
					return ;
				} else return ;
		} else {
			new Exception ("Assertion arguments not same type: " + left + " != " + right).printStackTrace();			
		}
	}

	public void assertNotEquals (Object left, Object right) {
		if (left != null && right != null) {
			if (left instanceof String && left.equals(right)) {
				new Exception ("Assertion NotEquals failed: " + left + " = " + right).printStackTrace();
			}
		}
		if (left == right) 
			new Exception ("Assertion NotEquals failed: " + left + " = " + right).printStackTrace();
	}



    @Override
    public String getName() {
        return name;
    }

    @Override
    public void apamStart(Instance inst) {
        myInst = inst;
        name = inst.getName();
		new Thread(this, "test dependency").start();
    }
    
	public void run() {
        System.out.println("Dependency test Started : " + myInst.getName());
		
		testDependency () ;
	}

	public void testDependency () {
	
		Implementation s3Impl = CST.apamResolver.findImplByName(null, "S3Impl");
		s3Impl.createInstance(null, null).getServiceObject();
		s3Impl.createInstance(null, null).getServiceObject();
		s3Impl.createInstance(null, null).getServiceObject();
		
//	    S3_2       s3;
		System.out.println("\nChecking simple dependency");
		if (s3 == null) {
			System.err.println("ERROR: s3 is null");
		}

		//multiple dependencies
//	    Set<S3_1> s3s;
//	    List<S3_1> s3s2;
//	    S3_2[]     s3_2;
		System.out.println("\nChecking multiple dependencies");
		if (s3s.size() != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3s should contain " + s3Impl.getInsts() + "\n      it contains " + s3s) ;		
		}
		if (s3_2.length != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3_2s should contain " + s3Impl.getInsts() + "\n      it contains " + s3_2) ;		
		}
		if (s3s2.size() != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3s2 should contain " + s3Impl.getInsts() + "\n      it contains " + s3s2) ;		
		}


	    
	    System.out.println("/nChecking Dynamic addition to multiple dependency" ) ;
		s3Impl.createInstance(null, null).getServiceObject();
		if (s3s.size() != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3s should contain " + s3Impl.getInsts() + "\n      it contains " + s3s) ;		
		}
		s3Impl.createInstance(null, null).getServiceObject();
		if (s3s.size() != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3s should contain " + s3Impl.getInsts() + "\n      it contains " + s3s) ;		
		}
		if (s3_2.length != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3_2s should contain " + s3Impl.getInsts() + "\n      it contains " + s3_2) ;		
		}
		if (s3s2.size() != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3s2 should contain " + s3Impl.getInsts() + "\n      it contains " + s3s2) ;		
		}

		
	    System.out.println("Checking Dynamic Wire deletion to multiple dependency" ) ;
		Wire w = (Wire)myInst.getWires("s3s").toArray()[0] ;
		Instance rmInst = w.getDestination() ;
		myInst.removeWire(w) ;
		if (s3s.contains(myInst.getServiceObject())) {
			System.err.println("ERROR: dependency s3s should not contain " + rmInst + "\n      it contains " + s3s) ;		
		}
		if (s3_2.length != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3_2s should contain " + s3Impl.getInsts() + "\n      it contains " + s3_2) ;		
		}
		if (s3s2.size() != s3Impl.getInsts().size()) {
			System.out.println("ERROR: dependency s3s2 should contain " + s3Impl.getInsts() + "\n      it contains " + s3s2) ;		
		}
		
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
        System.out.println("S2 called " + s);
        if (s4_1 != null)
            s4_1.callS4("depuis S2 (s4_1) ");
        if (s4_2 != null)
            s4_2.callS4("depuis S2 (s4_2) ");
    }

    @Override
    public void apamStop() {
    }

    @Override
    public void apamRelease() {
    }

}
