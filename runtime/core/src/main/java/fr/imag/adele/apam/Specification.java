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
import fr.imag.adele.apam.apform.ApformSpecification;

public interface Specification extends Component {

    /**
     * return the apform specification associated with this specification.
     * 
     * @return
     */
    public ApformSpecification getApformSpec();


    /**
     * Return the implementation that implement that specification and has the provided name.
     * 
     * @param implemName the name
     * @return the implementation
     */
    public Implementation getImpl(String implemName);

    /**
     * Return all the implementation of that specification. If no services implementation are found,
     * returns null.
     * 
     * @return the implementations
     * @throws ConnectionException the connection exception
     */
    public Set<Implementation> getImpls();

    /**
     * Return the list of currently required specification.
     * WARNING : does not include required interfaces and messages.
     * 
     * @return the list of currently required specification. Null if none
     */
    public Set<Specification> getRequires();

    /**
     * Return the list of specification that currently require that spec.
     * 
     * @return the list of specifications using that spec. Null if none
     * @throws ConnectionException the connection exception
     */
    public Set<Specification> getInvRequires();

}
