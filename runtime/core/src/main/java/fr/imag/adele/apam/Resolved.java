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
 * 
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

	public Resolved (Implementation toInstantiate, T empty) {
		this.toInstantiate = toInstantiate ;
		this.setResolved = null ;
		this.singletonResolved = null ;
	}
	
	
	public boolean isEmpty () {
		return singletonResolved == null && (setResolved == null || setResolved.isEmpty()) ;
	}
}
