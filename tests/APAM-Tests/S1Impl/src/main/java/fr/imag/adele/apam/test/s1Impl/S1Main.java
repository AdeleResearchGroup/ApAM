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

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.test.s1.S1;

public class S1Main implements Runnable, ApamComponent {
    S1 s1;

    public void run() {
        System.out.println("=== executing  s1.callS1(\"From S1 Main \") ");
        s1.callS1("From S1 Main ");
        System.out.println("End of S1 MAIN");
        //        s1 = null; // ne fait rien, reprend la meme valeur
        //        s1.callS1("Deuxieme from S1 Main");
    }

    public void apamInit(Instance inst) {
        System.out.println("S1Main Started : " + inst.getName());
        new Thread(this, "APAM test").start();
    }

    public void apamRemove() {
    }

}
