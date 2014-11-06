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
package fr.imag.adele.apam.maven.plugin.validation;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.repository.maven.Classpath;

/**
 * This class perform a full component validation, including validation against
 * referenced components (available in the context).
 * 
 * This validator is stateless , and can be used concurrently. However, if the
 * same validation context is used concurrently by several validators, the result
 * may not be deterministic.
 *  
 * @author vega
 *
 */
public final class Validator {


	private final Classpath 			classpath;
	private final ValidationContext		context;
	
	
	public Validator(Classpath classpath, ValidationContext context) {
		this.context 	= context;
		this.classpath	= classpath;
	}

	@SuppressWarnings("unchecked")
	public  <T extends ComponentDeclaration> void validate(T component, Reporter reporter) {
		
		ComponentValidator<T> validator = null;
		
		switch (component.getKind()) {
		
			case SPECIFICATION :
				validator = (ComponentValidator<T>) new SpecificationValidator(context, classpath);
				break;
			
			case IMPLEMENTATION :
				if (component instanceof AtomicImplementationDeclaration)
					validator = (ComponentValidator<T>) new AtomicComponentValidator(context, classpath);
				else
					validator = (ComponentValidator<T>) new CompositeValidator(context, classpath);
				break;
				
			case INSTANCE :
				validator = (ComponentValidator<T>) new InstanceValidator(context, classpath);
			
			default:
				break;
		}

		/*
		 * Perform validation, and free the validator
		 * 
		 * TODO We could enhance this class by implementing a pool of reusable validators.
		 * Currently this class is only used at build-time, and we can afford the cost of
		 * recreating the validator for each invocation. 
		 * 
		 */
		if (validator != null) {
			validator.validate(component, reporter);
			validator = null;
		}
	}
	
}
