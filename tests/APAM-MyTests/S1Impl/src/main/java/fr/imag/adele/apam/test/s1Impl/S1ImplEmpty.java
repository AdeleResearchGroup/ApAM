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

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;

public class S1ImplEmpty implements S1, Runnable, ApamComponent {
	S2 s2;
	String theFieldAttr ;
	String name = "unset" ;
	Instance apamInstance ;

	@Override
	public String getName () {
		return name ;
	}


	@Override
	public void callS1(String s) {
		System.out.println(name + " called " );
	}

	public void run () {
//		String sync = "" ;
		System.out.println("Started S1ImplEmpty");
//		synchronized (sync) {
//			try {
//				while (true) {
//					sync.wait (100) ;
//					apamInstance.setProperty("debit", 10);
//					System.out.println(name + " 10");
//					sync.wait (100) ;
//					apamInstance.setProperty("debit", 2);
//					System.out.println(name + " 2");
//				}
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}

	}

	@Override
	public void apamInit(Instance apamInstance) {
		this.apamInstance = apamInstance ;
		name = apamInstance.getName() ;
//		new Thread(this, "S1 Empty loop").start();
	}

	@Override
	public void apamRemove() {
	}


}
