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
package fr.imag.adele.apam.declarations;

import java.util.Arrays;
import java.util.List;

/**
 * The declaration of a method that needs to be invoked on the implementation
 * to notify an APAM event (component and dependencies lifecycle).
 * 
 * Currently all the supported callbacks take a single parameter of type
 * fr.imag.adele.apam.Component 
 */
public class CallbackDeclaration extends Instrumentation {
    
    /**
     * The name of the method that must be invoked
     */
    protected final String methodName;
    
    public CallbackDeclaration(AtomicImplementationDeclaration implementation, String methodName) {
    	super(implementation);
    	
        assert methodName != null;
        this.methodName		= methodName;
    }

    /**
     * The component declaring this injection
     */
    public AtomicImplementationDeclaration getImplementation() {
        return implementation;
    }

    /**
     * The name of the method to call
     */
    public String getMethodName() {
        return methodName;
    }
    
    private static final List<String> APAM_COMPONENTS =  Arrays.asList("fr.imag.adele.apam.Component",
    															"fr.imag.adele.apam.Instance",
    															"fr.imag.adele.apam.Implementation",
																"fr.imag.adele.apam.Specification");
    
    public boolean isValidInstrumentation() {
    	try {
			
    		int parameterNumber = implementation.getReflection().getMethodParameterNumber(methodName, true);
			
			if (parameterNumber == 0)
				return true;
			
			if (parameterNumber > 1)
				return false;
			
			String[] types = implementation.getReflection().getMethodParameterTypes(methodName, true);
			
			return APAM_COMPONENTS.contains(types[0]);
			
		} catch (NoSuchMethodException e) {
			return false;
		}
    	
    }

    @Override
    public String toString() {
        return "Method name: " + methodName;
    }

}
