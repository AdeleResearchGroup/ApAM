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
package fr.imag.adele.apam.hello.world.impl;

import fr.imag.adele.apam.hello.world.spec.HelloService;

public class HelloImplementation implements HelloService{

    String lang;
    /**
     * @see HelloService#sayHello(String) 
     */
    @Override
    public void sayHello(String texte) {
        System.out.println("Hello, " + texte);
    }

    @Override
    public String getLang() {
        return lang;
    }

    //Called by APAM when an instance of this implementation is created
    public void start(){
        System.out.println("--> English HelloService Start");
    }
    
    // Called by APAM when an instance of this implementation is removed
    public void stop(){
        System.out.println("--> English HelloService Stop");
    }


}
