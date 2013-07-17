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
    
    public boolean isValidInstrumentation() {
    	try {
			implementation.getReflection().getMethodArgumentNumber(methodName, true);
			String[] types=implementation.getReflection().getMethodArgumentTypes(methodName, true);
			
			if( types.length==0 ) {
				return true;
			} else if(types.length==1) {
				String classString=types[0]; 
				
				if(classString.equals("fr.imag.adele.apam.Component")
						||classString.equals("fr.imag.adele.apam.Instance")
						||classString.equals("fr.imag.adele.apam.Implementation")
						||classString.equals("fr.imag.adele.apam.Specification")){
					return true;
				}
				
			}
			
		} catch (NoSuchMethodException e) {
			
			return false;
			
		}
    	
    	return false;
    	
    }


    
    @Override
    public String toString() {
        return "Method name: " + methodName;
    }

}
