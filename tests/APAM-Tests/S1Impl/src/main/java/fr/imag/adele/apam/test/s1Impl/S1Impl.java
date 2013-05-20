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
package fr.imag.adele.apam.test.s1Impl;

import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s3.S3_2;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance ;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Specification;

public class S1Impl implements S1, Runnable, ApamComponent {

	// Apam handler injected
	S2       s2Spec;
	S2       s2Interf;
	S2       s2Msg;

	S3_1     s3_1;
	S3_2     s3_2;

	List<S3_1> s3List;
	S3_1[]     s3ListBis;

	S2         lastS2;

	//Injected Links
	Instance s2Inst ;
	Implementation s2Impl ;
	Specification s2Specif ;
	Set<Instance> s3Insts ;

	Instance thisInst ;

	public void callS1(String s) {

		System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S2  s2Spec; " + s2Spec.getName());
		System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S2  s2Interf; " + s2Interf.getName());
		System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S3_1 " + s3_1.getName());
		System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S3_2 " + s3_2.getName());
	
		System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S2Inst " + s2Inst);
		System.out.println("=== In fr.imag.adele.apam.test.s1Impl; s2Impl " + s2Impl);
		System.out.println("=== In fr.imag.adele.apam.test.s1Impl; s2Specif " + s2Specif);

		System.out.print("=== In S1Impl;  s3Insts :");
		if (s3Insts == null) {
			System.out.print("    s3Insts is null " );        	
		}
		else for (Instance s3 : s3Insts) {
			System.out.print("     =======" + s3);
		}
		
		Link lk = thisInst.getLink("linkS2") ;
		System.out.println("linkS2 : "  + lk);
		
		lk = thisInst.getLink("testID") ;
		System.out.println("testId : "  + lk);

		lk = thisInst.getLink("testIDS3") ;
		System.out.println("testId : "  + lk);
		

		lk = thisInst.getLink("spec2interf") ;
		System.out.println("spec2interf : "  + lk);

		Set<Link>lks = thisInst.getLinks("towardInterfS2") ;
		for (Link mk : lks) 
			System.out.println("towardInterfS2 : "  + mk);

		
		lk = thisInst.getLink("withoutkinds") ;
		System.out.println("withoutkinds : "  + lk);
		

		lk = thisInst.getLink("IL s2Impl") ;
		System.out.println("S2Impl : "  + lk);


//		
//		System.out.println("===========before calling S2 =========");
//
//		s2Spec.callS2("============= From S1Impl to S2 ...") ;
//
//		System.out.print("=== In S1Impl; S3_1  s3List :");
//		for (S3_1 s3 : s3List) {
//			System.out.print("     =======" + s3.getName());
//			s3.callS3_1("======from S1Impl, s3List ") ;
//		}
//		System.out.println("");
//		System.out.print("=== In S1Impl; S3_1  s3ListBis :");
//		for (S3_1 s3 : s3ListBis) {
//			System.out.print("     " + s3.getName());
//		}
//		System.out.println("");
//
//		System.out.println("=== In S1Impl; lastS2 " + lastS2.getName());

		//        System.out.println("S2  s2Spec; " + s2Spec);
		//        System.out.println("S1 called " + s);
		//        s2.callS2("from S1Impl internal");
		//        s4.callS4_final("From S1Impl external");
	}


	public void run() {
		System.out.println("=== executing  s1.callS1(\"From S1Impl \") ");
		callS1("From SImpl itself ");
		System.out.println("End of S1Impl");
	}

	public void apamInit(Instance inst) {
		thisInst = inst ;
		System.out.println("S1Impl Started : " + inst.getName());
		new Thread(this, "APAM test").start();
	}

	public void apamRemove() {
	}

}
