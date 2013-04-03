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
package apam.test.attrImpl;

import java.util.HashSet;
import java.util.Set;

import apam.test.attr.TestAttr;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.s2.S2;

public class TestAttrImpl implements TestAttr, ApamComponent {
    S2 s2;
    String theFieldAttr ;

	@Override
	public void callS1(String s) {
        System.out.println("In TestAttr " + s);
		theFieldAttr = s ;
        //s2.callS2("From S1toS2Final" ) ;
	}

	@Override
	public void apamInit(Instance apamInstance) {
		
		System.out.println("TestAttrImpl is started. xml fieldAttr value: " + apamInstance.getProperty("fieldAttr"));
		theFieldAttr = "initial set by program" ;
		if (s2 == null)
			System.out.println(" s2 resolution failed");
		System.out.println(" initialized fieldAttr value: " + apamInstance.getProperty("fieldAttr"));
	}

	@Override
	public void apamRemove() {
	}

	public String funcAttr (Instance i) {
		return "retour de funcAttri" ;
	}

	public String ffuncAttr (Implementation i) {
		return "False retour de funcAttri" ;
	}
	public Set<String> funcAttrSet (Instance i) {
		Set<String> ret = new HashSet<String> () ;
		ret.add("retopur 1") ;
		ret.add("retopur 2") ;
		ret.add("retopur 3") ;
		return ret ;
	}

	public String fonctionTest (Instance inst) {
		System.out.println("Dans fonctionTest: Parametre instance: " + inst);
		return "de retour de la fonction" ;
	}
	
	public Set<String> fonctionTestSet (Instance inst) {
		System.out.println("Dans fonctionTestSet: Parametre instance: " + inst);
		Set<String> ret = new HashSet<String>() ;
		ret.add ("val1") ;
		ret.add ("val2") ;
		ret.add ("val3") ;
		return ret ;
	}

}
