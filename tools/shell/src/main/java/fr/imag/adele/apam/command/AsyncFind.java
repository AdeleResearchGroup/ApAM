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

import java.io.PrintWriter;


public class AsyncFind implements Runnable {

    private String componentName;

    private PrintWriter out;

    private Composite target;

    private boolean instantiate;
    public AsyncFind(PrintWriter out, Composite target, String componentName, boolean b) {
        this.out= out;
        this.target = target;
        this.componentName = componentName;
        this.instantiate = b;
    }



    @Override
    public void run() {
        Component  component= CST.apamResolver.findComponentByName(target, componentName);
        if (component!=null){
            out.println(">> " + component.getName() + " deployed!");
            if (instantiate){
                if (component instanceof Implementation)
                    ((Implementation)component).createInstance(target,null);
                if (component instanceof Specification) {
                    Implementation impl = CST.apamResolver.resolveSpecByName(target, componentName, null, null) ;
                    if (impl != null)
                        impl.createInstance(null, null);
                }
            }
        }

        else
            out.println(">> Deployment failed for " + componentName);
    }
}
