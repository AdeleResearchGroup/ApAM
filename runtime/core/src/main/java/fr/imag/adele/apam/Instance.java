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

import fr.imag.adele.apam.apform.ApformInstance;

public interface Instance extends Component {

	/**
	 * The Apform instance associated with this Apam one.
	 */
	public ApformInstance getApformInst();

	/**
	 * The composite at the end of the "father" relationship chain.
	 */
	public Composite getAppliComposite();

	/**
	 * Returns the composite to which this instance pertains.
	 */
	public Composite getComposite();

	// /**
	// * returns the wires leading to that instance
	// *
	// */
	// public Set<Link> getInvWires();
	//
	// /**
	// * returns the wires from that instance
	// *
	// */
	// public Set<Link> getWires();
	//
	//
	// public boolean hasWires() ;
	//
	// public boolean hasInvWires() ;

	/**
	 * Get the implementation.
	 * 
	 */
	public Implementation getImpl();

	/**
	 * Method getServiceObject returns an object that can be casted to the
	 * associated interface, and on which the interface methods can be directly
	 * called. The object can be the service itself or a proxy for a remote
	 * service. It is the fast way for synchronous service invocation.
	 * 
	 * @return the service object, return null, if the object no longer exists.
	 */
	public Object getServiceObject();

	/**
	 * returns the specification of that instance
	 * 
	 */
	public Specification getSpec();

	/**
	 * returns the value of the shared attribute
	 * 
	 * @return
	 */
	public boolean isSharable();

}
