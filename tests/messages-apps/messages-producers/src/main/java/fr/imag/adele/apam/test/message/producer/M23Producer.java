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
package fr.imag.adele.apam.test.message.producer;

import fr.imag.adele.apam.test.message.M2;
import fr.imag.adele.apam.test.message.M3;

public class M23Producer {
    
    
    public M2 produceM2() {
        double a =Math.random();
        double b = Math.random();
        return new M2(a,b) ;
    }
       
    public M3 produceM3() {
        double a =Math.random();
        double b = Math.random();
        return new M3(a,b) ;
    }

    public void start() {
        System.out.println("M23 Producer started");
    }

    public void stop() {
       System.out.println("M23 Producer stopped");
    }

}
