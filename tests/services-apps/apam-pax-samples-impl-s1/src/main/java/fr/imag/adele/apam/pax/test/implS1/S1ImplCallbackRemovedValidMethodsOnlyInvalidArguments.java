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
package fr.imag.adele.apam.pax.test.implS1;

import fr.imag.adele.apam.pax.test.iface.S1;
import fr.imag.adele.apam.pax.test.iface.S2;

public class S1ImplCallbackRemovedValidMethodsOnlyInvalidArguments implements S1
{
    
	S2 s2;
	
    public void bindWithInstance(String invalid){
    	System.out.println("Called:"+this.getClass().getName());
    	
    }
    
    public void bindWithInstance(Integer invalid){
    	System.out.println("Called:"+this.getClass().getName());
    	
    }

	@Override
	public String whoami() {
		return "titi";
	}


}
