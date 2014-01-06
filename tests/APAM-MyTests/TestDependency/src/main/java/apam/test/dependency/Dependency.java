/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package apam.test.dependency;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.message.MessageProducer;
import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s3.S3_2;

public class Dependency implements S2, ApamComponent, Runnable {

    // Apam injected
    public Apam      apam;
    public S4        s4_1;
    public S4        s4_2;
    public S4        s4_3;
    public Set<S3_1> s3_1set;
    
	public S3_2[]    s3_2array;


	public S3_1      s3;
    public S3_2      s3bis;

	public Set<S3_1> s3_1;
    public S3_2[]    s3_2;

    public List<S3_1> s3s2;
    public Set<S3_1> s3s;
   
    public Instance   myInst;
    public String     name;

    public MessageProducer<M1> p1;
    public MessageProducer<M1> producerM1;
    
    S3_2 s3ImplWindowsBedroomTry1;
    S3_2 s3ImplWindowsBedroomTry2;
    
    // Called (by Apam) each time an M3 message is available.
    public void getMyMessage (M2 m2) {
    	M1 m1 = null;
    	p1.push(m1) ;	
    }
    
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
    public void apamInit(Instance inst) {
        myInst = inst;
        name = inst.getName();
		new Thread(this, "test dependency").start();
    }
    
	public void run() {
//        System.out.println("Dependency test Started : " + myInst.getName());
//		System.out.println("S3bis = " + s3bis.getName());
//		for (S3_1 s3 : s3_1set) 
//			System.out.println("s3_1set : " + s3.getName());
//		for (int i = 0; i < s3_2array.length; i++) 
//			System.out.println("s3_2array : " + s3_2array[i].getName());
//		testDependency () ;
	}

	public Map<String, Instance> S3Insts;
	public Implementation s3Impl;
	public Instance s3Inst;

	public void p1(){
		S3Insts = new HashMap<String, Instance> () ;
		//Test simple dependency
		s3Impl = CST.apamResolver.findImplByName(null, "S3Impl");
	}
	
	public void p2(){
		s3Inst = CST.componentBroker.getInstService(s3bis) ;
	}
	
	public void p3(){
		System.out.println("/nChecking Dynamic addition to multiple dependency" ) ;	    
	    s3Inst = s3Impl.createInstance(null, null);
		
	}
	
	Component rmInst;
	
	

	public void p4(){
		System.out.println("Checking Dynamic Wire deletion to multiple dependency" ) ;
		Link w = (Link)myInst.getRawLinks().toArray()[0] ;
		rmInst = w.getDestination() ;
//		myInst.removeLink(w) ;
	}
	
	public void testDependency () {
	
//		Map<String, Instance> S3Insts = new HashMap<String, Instance> () ;
//		//Test simple dependency
//		Implementation s3Impl = CST.apamResolver.findImplByName(null, "apam.test.dependency.S3Impl");
//		Instance s3Inst ;
		p1();

				
		//System.out.println("\nChecking simple dependency");
		assertTrue(s3bis != null) ;
		assertTrue(s3 != null) ;
		assertEquals (CST.componentBroker.getInstService(s3).getName(), s3.getName()) ;
		assertEquals (CST.componentBroker.getInstService(s3bis).getName(), s3bis.getName()) ;
		
		//Checking constraints
		//s3Inst = CST.componentBroker.getInstService(s3bis) ;
		p2();
		
		assertTrue (s3Inst.match("(OS*>Android)" )) ;
		assertTrue (s3Inst.match("(&amp;(location=living)(MyBool=true))")) ;

		//multiple dependencies
		assertTrue (s3_1set.size() != 0) ;
		assertTrue (s3_1set.containsAll (Arrays.asList(s3_2array))) ;

		//Checking Dynamic addition to multiple dependency
//	    System.out.println("/nChecking Dynamic addition to multiple dependency" ) ;	    
//	    s3Inst = s3Impl.createInstance(null, null);
		p3();
		assertTrue (s3_1set.contains(s3Inst.getServiceObject())) ;
		assertTrue (s3_1set.containsAll (Arrays.asList(s3_2array))) ;
		
		//Checking Dynamic Wire deletion to multiple dependency
//	    System.out.println("Checking Dynamic Wire deletion to multiple dependency" ) ;
//		Wire w = (Wire)myInst.getWires().toArray()[0] ;
//		Instance rmInst = w.getDestination() ;
//		myInst.removeWire(w) ;
		p4();
		assertTrue (!s3_1set.contains(((Instance)rmInst).getServiceObject()));
		assertTrue (s3_1set.containsAll (Arrays.asList(s3_2array))) ;
		
		//test delete instances
		
		
		//contraintes multiple
		//contraintes implementations
		//contraintes instances
		
		//heritage de contraintes
		//contraintes générique
		
		
		
		//preferences
		
		//instantiable
		
		//shared
		
		//singleton
		
		//resolution interface
		//resolution message
		//resolution Spec
		//resolution Implem
		//resolution instance
		
		
		//fail
		//exception
		//override exception
		//override hidden
		//wait
		
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
    public void apamRemove() {
    }

    public Set<S3_1> getS3_1set() {
		return s3_1set;
	}

	public void setS3_1set(Set<S3_1> s3_1set) {
		this.s3_1set = s3_1set;
	}
	
    public S3_2[] getS3_2array() {
		return s3_2array;
	}

	public void setS3_2array(S3_2[] s3_2array) {
		this.s3_2array = s3_2array;
	}
    
	public Instance getRmInst() {
		return (Instance)rmInst;
	}

	public void setRmInst(Instance rmInst) {
		this.rmInst = rmInst;
	}
	
    public S3_2 getS3bis() {
		return s3bis;
	}

	public void setS3bis(S3_2 s3bis) {
		this.s3bis = s3bis;
	}
	
	public Instance getS3Inst() {
		return s3Inst;
	}

	public void setS3Inst(Instance s3Inst) {
		this.s3Inst = s3Inst;
	}

	public S3_2 getS3ImplWindowsBedroomTry1() {
		return s3ImplWindowsBedroomTry1;
	}

	public void setS3ImplWindowsBedroomTry1(S3_2 s3ImplWindowsBedroomTry1) {
		this.s3ImplWindowsBedroomTry1 = s3ImplWindowsBedroomTry1;
	}

	public S3_2 getS3ImplWindowsBedroomTry2() {
		return s3ImplWindowsBedroomTry2;
	}

	public void setS3ImplWindowsBedroomTry2(S3_2 s3ImplWindowsBedroomTry2) {
		this.s3ImplWindowsBedroomTry2 = s3ImplWindowsBedroomTry2;
	}
}
