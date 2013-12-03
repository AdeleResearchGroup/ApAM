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
 * This enumeration represents the different policies available for handling
 * events in which a relation is no resolvable
 * 
 * @author vega
 * 
 */
public enum MissingPolicy {

    /**
     * The default policy is just to ignore this event. If a client uses the
     * relation a null reference will be returned to the calling thread.
     */
    OPTIONAL,

    /**
     * Automatically creates the wire for resolving the relation when a suitable
     * instance is available. If a client tries to use the relation, the
     * invoking thread will be blocked until the relation is resolved again.
     */
    WAIT,

    /**
     * If a client effectively accesses the relation an exception will be thrown
     * to signal the missing target.
     */
    EXCEPTION

}
