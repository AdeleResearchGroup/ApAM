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
package fr.imag.adele.apam.apform.impl.handlers;

import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.RelationInjection;

/**
 * This class represents a kind of injection manager for a relation. The
 * injection manager is in charge of translating the APAM events into platform
 * specific action to inject the relation into a field.
 * 
 * @author vega
 * 
 */
public interface RelationInjectionManager extends FieldInterceptor {

	/**
	 * The interface of the external resolver that is used to bind this field. 
	 * 
	 */
	public static interface Resolver {
		
		/**
		 * Registers an injection manager with a resolver.
		 * 
		 * The resolver can asynchronously update the relation to modify the
		 * binding.
		 * 
		 * @see fr.imag.adele.apam.apform.impl.InterfaceInjectionManager.addTarget
		 * @see fr.imag.adele.apam.apform.impl.InterfaceInjectionManager.removeTarget
		 * @see fr.imag.adele.apam.apform.impl.InterfaceInjectionManager.substituteTarget
		 * 
		 */
		public void addInjection(RelationInjectionManager injection);
		
		/**
		 * Request to lazily resolve an injection.
		 * 
		 * This method is invoked by a injection manager to calculate its initial binding
		 * when it is first accessed.
		 *  
		 * The resolver must call back the manager to modify the resolved target.
		 *   
		 * @see fr.imag.adele.apam.apform.impl.InterfaceInjectionManager.addTarget
		 * @see fr.imag.adele.apam.apform.impl.InterfaceInjectionManager.removeTarget
		 * @see fr.imag.adele.apam.apform.impl.InterfaceInjectionManager.substituteTarget
		 *  
		 */
		public boolean resolve(RelationInjectionManager injection);
		
		/**
		 * Request to remove an injection.
		 * 
		 * This method is invoked by a injection manager to signify that the
		 * component wants to force the resolution of the relation the next
		 * access
		 * 
		 */
		public boolean unresolve(RelationInjectionManager injection);
		
	} 
	
	/**
	 * The relation injection that is managed by this manager
	 */
	public abstract RelationInjection getRelationInjection();

	
    /**
     * Get an XML representation of the state of this injection
     */
    public abstract Element getDescription();

    /**
     * The current state of the manager. 
     * 
     * A specific manager implementation can have dependencies on some platform services that may
     * become unavailable. In that case the translation from APAM action to platform actions is no
     * longer possible; 
     */
    
    public abstract boolean isValid();
    
	/**
	 * Adds a new target to this injection
	 */
	public abstract void addTarget(Instance target);

	/**
	 * Removes a target from the injection
	 * 
	 * @param target
	 */
	public abstract void removeTarget(Instance target);

	/**
	 * Substitutes an existing target by a new one
	 * 
	 * @param oldTarget
	 * @param newTarget
	 */
	public abstract void substituteTarget(Instance oldTarget, Instance newTarget);

}