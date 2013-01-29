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
package fr.imag.adele.apam.test.s4Impl;

//import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s4.S4;
import fr.imag.adele.apam.test.s5.S5;

public class S4Impl implements S4 {

    S5 s5;

    @Override
    public void callS4(String s) {
        System.out.println("S4 called " + s);
        if (s5 != null)
            s5.callS5(" from s4");
    }

    @Override
    public String getName() {
    	return "S4Impl";
    }
    @Override
    public void callBackS4(String s) {
        System.out.println(" In call back S4 : " + s);
    }

    @Override
    public void callS4_final(String msg) {
        System.out.println(" S4_final called " + msg);

    }
}
