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

import java.util.Set;

import apam.test.attr.CapteurTemp;
import apam.test.attr.ConfCapteur;

import fr.imag.adele.apam.test.s4.S4;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class Temp implements S2, ApamComponent, Runnable {

    // Apam injected
    Apam      apam;
    
    S4        s4 ;
    
    Set<CapteurTemp> captSet;
    ConfCapteur[]    confCaptArray;
    
    CapteurTemp     kitchenTemp;
    ConfCapteur		kitchenConfig ;
    
    
	public void run() {
        System.out.println("Dependency test Started : " + myInst.getName());
		System.out.println("s4 = " + s4.getName());
		System.out.println("kitchenTemp = " + kitchenTemp.getName());
		for (CapteurTemp capteur : captSet) 
			System.out.println("captSet : " + capteur.getName());
		for (int i = 0; i < confCaptArray.length; i++) 
			System.out.println("confCaptArray : " + confCaptArray[i].getName());
	}

    Instance   myInst;
    String     name;

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

    @Override
    public void apamRemove() {
    }

	@Override
	public void callS2(String s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void callBackS2(String s) {
		// TODO Auto-generated method stub
		
	}

}
