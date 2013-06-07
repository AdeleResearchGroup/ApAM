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
package fr.imag.adele.apam;

import java.util.Set;

/*
 * This object is used to return the result of a resolution, provided a relation.
 * 
 * If the relation is a singleton, the result (if found) must be in singletonResolved, 
 * 		if multiple, in setResolved.
 * 
 * In case the target is an Instance, an that the Implementation is found but no instance is available, 
 *      the Implementation must be provided in toInstantiate, and the other fields at null.
 *      
 *  In all cases, only one field is provided, others are null.
 */

public class Resolved <T extends Component> {
	public Implementation toInstantiate ;
	public T singletonResolved ; 
	public Set<T> setResolved ;
	
	public Resolved (Set<T> setResolved) {
		this.singletonResolved = null ;
		this.setResolved = setResolved ;
	}

	public Resolved (T singletonResolved) {
		this.singletonResolved = singletonResolved ;
		this.setResolved = null ;
	}

	/*
	 * The boolean is useless, only to make a different signature.
	 */
	public Resolved(Implementation impl, boolean toInstanciate) {
		this.toInstantiate = impl;
		this.singletonResolved = (T) null;
		this.setResolved = null;
	}
	
	
	public boolean isEmpty () {
		return singletonResolved == null && toInstantiate == null && (setResolved == null || setResolved.isEmpty()) ;
	}
}
