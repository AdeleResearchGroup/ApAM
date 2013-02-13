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

import apam.test.attr.TestAttr;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class TestAttrImpl implements TestAttr, ApamComponent {
//    S2 s2;
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
		System.out.println(" initialized fieldAttr value: " + apamInstance.getProperty("fieldAttr"));
	}

	@Override
	public void apamRemove() {
	}


}
