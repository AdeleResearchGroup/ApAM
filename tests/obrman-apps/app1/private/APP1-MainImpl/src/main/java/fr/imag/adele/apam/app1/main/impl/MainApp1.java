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
package fr.imag.adele.apam.app1.main.impl;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.app1.main.spec.App1MainSpec;
import fr.imag.adele.apam.app1.s1.spec.S1;
import fr.imag.adele.apam.app1.s2.spec.S2;
import fr.imag.adele.apam.app1.spec.App1Spec;
import fr.imag.adele.apam.app2.spec.App2Spec;

public class MainApp1 implements App1Spec, App1MainSpec {

    S1       s1;

    S2       s2;

    App2Spec app2;

    @Override
    public void callS1(String texte) {
        System.out.println("--- Calling S1 from APP1 ---");
        s1.call(texte);
    }

    @Override
    public void callS2(String texte) {
        System.out.println("--- Calling S2 from APP1 ---");
        s2.call(texte);
    }

    @Override
    public void callApp2(String texte) {
        System.out.println("--- Calling APP2 from APP1 ---");
        app2.call(texte);
    }

    @Override
    public void call(String texte) {
        texte += " >>> " + MainApp1.class.getSimpleName();
        System.out.println(texte);
        callS1(texte);
        callS2(texte);
        callApp2(texte);

    }

    public void apamStart(Instance apamInstance) {
        call("go >");

    }

    public void apamStop() {
        // TODO Auto-generated method stub

    }

    public void apamRelease() {
        // TODO Auto-generated method stub

    }
}
