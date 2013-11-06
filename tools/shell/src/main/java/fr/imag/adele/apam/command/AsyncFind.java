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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;

public class AsyncFind implements Runnable {

    private String componentName;

    private PrintWriter out;

    private Composite target;

    private boolean instantiate;
    private String[] params;
    private Map<String, String> props;

    public AsyncFind(PrintWriter out, Composite target, String componentName,
	    boolean b, String[] params) {
	this.out = out;
	this.target = target;
	this.componentName = componentName;
	this.instantiate = b;
	if (params != null) {
	    this.params = new String[params.length];
	    System.arraycopy(params, 0, this.params, 0, params.length);
	}
    }

    @Override
    public void run() {
	Component component = CST.apamResolver.findComponentByName(target,
		componentName);
	if (component == null) {
	    System.out.println("Component " + componentName + " not found.");
	    return;
	}

	if (params != null && params.length > 1) {
	    props = new HashMap<String, String>();
	    String value = "";
	    // Skip first values, which is the component name
	    for (int i = 1; i < params.length; i++) {
		value += params[i] + ", ";
	    }
	    props.put("param", value);
	} else {
	    props = null;
	}

	if (instantiate) {
	    if (component instanceof Implementation) {
		((Implementation) component).createInstance(target, props);
	    }
	    if (component instanceof Specification) {
		Implementation impl = CST.apamResolver.resolveSpecByName(
			target, componentName, null, null);
		if (impl == null) {
		    System.out.println("Component " + componentName
			    + " not found.");
		    return;
		}
		impl.createInstance(null, props);
	    }
	}
    }
}
