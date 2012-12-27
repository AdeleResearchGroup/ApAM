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

public class DependencyPromotion {

	/**
	 * The dependency to be promoted
	 */
	private final DependencyDeclaration.Reference source;
	
	/**
	 * The composite dependency that will be the target of the promotion
	 */
	private final DependencyDeclaration.Reference target;
	
	public DependencyPromotion(DependencyDeclaration.Reference source, DependencyDeclaration.Reference target) {
		this.source = source;
		this.target	= target;
	}
	
	/**
	 * The dependency to be promote
	 */
	public DependencyDeclaration.Reference getContentDependency() {
		return source;
	}
	
	/**
	 * The target of the promotion
	 */
	public DependencyDeclaration.Reference getCompositeDependency() {
		return target;
	}
}
