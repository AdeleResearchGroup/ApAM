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
package fr.imag.adele.apam.mainApam;


import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
//import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;

public class Promotions implements Runnable, ApamComponent {
    // injected
    Apam apam;

    public void run() {
        System.out.println("====================================\n" +
                "======= Starting Promotions ========\n" +
        "====================================");

        System.out.println("\n\nLoading S5");
        CST.apamResolver.findSpecByName(null, "S5");
        System.out.println("S5 Loaded\n\n ");

        System.out.println("\n\nLoading S3Impl to get the instances");
        CST.apamResolver.findImplByName(null, "S3Impl");
        System.out.println("S3Impl Loaded\n\n ");

        System.out.println("\n\ncreating S1Main-Appli only loading the bundle containing S1Main");
        CST.apamResolver.findImplByName(null, "S1Impl");
        System.out.println("after apamResolver.findImplByName(null, \"S1Main\")\n\n");

    }

    public void apamInit(Instance apamInstance) {
        new Thread(this, "APAM test").start();
    }

    public void apamRemove() {

    }

    public void apamRelease() {

    }
}