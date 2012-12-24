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
package fr.imag.adele.apam.pax.test.implS2;
import fr.imag.adele.apam.pax.test.iface.S1;
import fr.imag.adele.apam.pax.test.iface.S2;
import fr.imag.adele.apam.pax.test.iface.S4;
import fr.imag.adele.apam.pax.test.iface.S5;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;
import fr.imag.adele.apam.pax.test.iface.device.HouseMeter;

public class S2Impl implements S2
{

	Eletronic deadMansSwitch;
	
    S4 s4;
    S5 s5;
    
    HouseMeter houseMeter;
    
    S1 inner;

    public String whoami()
    {
        return this.getClass().getName();
    }
    
    public void start(){
    	System.out.println("Starting:"+this.getClass().getName());
    }
    
    public void stop(){
    	System.out.println("Stopping:"+this.getClass().getName());
    }

	public HouseMeter getHouseMeter() {
		return houseMeter;
	}

	public void setHouseMeter(HouseMeter houseMeter) {
		this.houseMeter = houseMeter;
	}

	public Eletronic getDeadMansSwitch() {
		return deadMansSwitch;
	}

	public void setDeadMansSwitch(Eletronic deadMansSwitch) {
		this.deadMansSwitch = deadMansSwitch;
	}

}
