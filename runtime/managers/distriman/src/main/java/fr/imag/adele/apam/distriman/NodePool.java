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
package fr.imag.adele.apam.distriman;

import java.util.Map;
import java.util.Set;

/**
 * Interface used to provide information about remote apam nodes available
 * @author jander
 *
 */
public interface NodePool {

	/**
	 * Include new URL as an available remote apam node
	 * @param url
	 * @return
	 */
	public RemoteMachine newRemoteMachine(String url);
	
	/**
	 * Remove URL from the list of remote apam node
	 * @param url
	 * @return
	 */
	public RemoteMachine destroyRemoteMachine(String url);
	
	/**
	 * Return list of remote apam node available, not including the machine that calls the method
	 * @return
	 */
	public Map<String, RemoteMachine> getMachines();
	
	public Set<RemoteMachine> getRemoteMachines();
	
	public RemoteMachine getRemoteMachine(String url);
}
