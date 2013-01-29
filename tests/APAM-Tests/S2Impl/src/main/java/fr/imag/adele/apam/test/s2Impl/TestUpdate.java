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
package fr.imag.adele.apam.test.s2Impl;

import apam.test.attr.CapteurTemp;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class TestUpdate implements ApamComponent, Runnable{

	CapteurTemp temperature ;

	@Override
	public void run() {
		int prev = temperature.getTemp () ;
		System.err.println("Got previous value : " + prev);
		while (true){ 
			try {
				if (temperature.getTemp () != prev) {
					System.err.println ("previous = " + prev + ". nouveau= " + temperature.getTemp ()) ;
					prev = temperature.getTemp () ;
				}
			} catch (Exception e) { 
				e.printStackTrace() ; 
				}
		}
	}


	@Override
	public void apamInit(Instance apamInstance) {
		new Thread(this, "test Update").start();
	}
	@Override
	public void apamRemove() {
	}

}
