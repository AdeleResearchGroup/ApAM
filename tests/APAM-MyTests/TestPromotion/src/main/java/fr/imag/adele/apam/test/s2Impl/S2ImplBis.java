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

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s4.S4;

public class S2ImplBis implements S2, ApamComponent {

    // Apam injected
    Apam     apam;
    S4       s4Bis;
    //    S4      s4_2;
    //    Set<S3> s3s;

    Instance myInst;
    String   name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void apamInit(Instance inst) {
        myInst = inst;
        name = inst.getName();
        System.out.println("S2ImplBis Started : " + inst.getName());
    }

    // @Override
    @Override
    public void callBackS2(String s) {
        System.out.println("Back in S2 : " + s);
    }

    // @Override
    @Override
    public void callS2(String s) {
        System.out.println("S2 Bis called " + s);
        if (s4Bis != null)
            s4Bis.callS4("depuis S2 Bis (s4Bis) ");
        //        if (s4_2 != null)
        //            s4_2.callS4("depuis S2 (s4_2) ");
    }

    @Override
    public void apamRemove() {
    }
}
