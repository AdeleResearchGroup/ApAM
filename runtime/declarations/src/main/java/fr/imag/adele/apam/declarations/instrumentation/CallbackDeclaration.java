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
package fr.imag.adele.apam.declarations.instrumentation;

import java.util.Arrays;
import java.util.List;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;

/**
 * The declaration of a method that needs to be invoked on the implementation to
 * notify an APAM event (component and dependencies lifecycle).
 * 
 * Currently all the supported callbacks take a single parameter of type
 * fr.imag.adele.apam.Component.
 * 
 * TODO currently we also allow to disable parameter validation to allow injecting
 * service objects for dependencies. However we should be more precise on the type
 * of the parameters.
 * 
 */
public class CallbackDeclaration extends Instrumentation {

	/**
	 * The name of the method that must be invoked
	 */
	private final String methodName;

	private final boolean disableValidation;
	
	private static final List<String> APAM_COMPONENTS = Arrays.asList(
			"fr.imag.adele.apam.Component",
			"fr.imag.adele.apam.Implementation",
			"fr.imag.adele.apam.Specification",
			"fr.imag.adele.apam.Instance");

	public CallbackDeclaration(AtomicImplementationDeclaration implementation,String methodName) {
		this(implementation,methodName,false);
	}
	
	public CallbackDeclaration(AtomicImplementationDeclaration implementation,String methodName, boolean disableValidation) {
		super(implementation.getReference(), implementation.getImplementationClass());

		assert methodName != null;
		this.methodName = methodName;
		
		this.disableValidation = disableValidation;
	}

	@Override
	public String getName() {
		return methodName;
	}
	
	/**
	 * The name of the method to call
	 */
	public String getMethodName() {
		return methodName;
	}

	@Override
	public boolean isValidInstrumentation() {
		try {

			int parameterNumber = instrumentedClass.getMethodParameterNumber(methodName, true);

			if (parameterNumber == 0) {
				return true;
			}

			if (parameterNumber > 1) {
				return false;
			}

			String[] types = instrumentedClass.getMethodParameterTypes(methodName, true);

			return disableValidation || APAM_COMPONENTS.contains(types[0]);

		} catch (NoSuchMethodException e) {
			return false;
		}

	}

	@Override
	public String toString() {
		return "Method name: " + methodName;
	}

}
