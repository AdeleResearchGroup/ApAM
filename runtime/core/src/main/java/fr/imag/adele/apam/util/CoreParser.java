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
package fr.imag.adele.apam.util;

import java.util.List;

import fr.imag.adele.apam.declarations.ComponentDeclaration;

/**
 * This class represents a tool being able to parse one of the different APAM Core representations
 * 
 * @author vega
 *
 */
public interface CoreParser {

    /**
     * Get the list of all the declared components
     */
    public List<ComponentDeclaration> getDeclarations(ErrorHandler errorHandler);

    /**
     * This interface allow parser users to be notified of all errors found during parsing
     */
    public interface ErrorHandler {

        public enum Severity {SUSPECT, WARNING, ERROR;}

        /**
         * Notifies of an error in a declaration
         */
        public void error(Severity severity, String message);
    }

    /**
     * An string value that will be used to represent mandatory attributes not specified
     */
    public final static String UNDEFINED = new String("<undefined value>");

}
