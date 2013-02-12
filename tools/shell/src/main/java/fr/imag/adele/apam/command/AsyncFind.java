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
package fr.imag.adele.apam.command;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.util.Util;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


public class AsyncFind implements Runnable {

    private String componentName;

    private PrintWriter out;

    private Composite target;

    private boolean instantiate;
    private String[] params ;
    private Map<String, String> props ;
    
    public AsyncFind(PrintWriter out, Composite target, String componentName, boolean b, String[] params) {
        this.out= out;
        this.target = target;
        this.componentName = componentName;
        this.instantiate = b;
        this.params = params ;
    }



    @Override
    public void run() {
        Component  component= CST.apamResolver.findComponentByName(target, componentName);
        if (params != null) {
        	props = new HashMap <String, String> () ;
        	props.put ("param", Util.toStringArrayString(params)) ; 
        } else props = null ;
        
        if (component!=null){
            out.println(">> " + component.getName() + " deployed!");
            if (instantiate){
                if (component instanceof Implementation)
                    ((Implementation)component).createInstance(target, props);
                if (component instanceof Specification) {
                    Implementation impl = CST.apamResolver.resolveSpecByName(target, componentName, null, null) ;
                    if (impl != null)
                        impl.createInstance(null, props);
                }
            }
        }

        else
            out.println(">> Deployment failed for " + componentName);
    }
}
