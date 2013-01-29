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

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface Apam {

    /**
     * Simply creates an instance of the composite type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    Composite startAppli(CompositeType compositeType);
    
    /**
     * Resolve compositeTypeName and, if successful, creates an instance of that type.
     * It starts either because it implements the ApamComponent interface,
     * of calling its getServiceObject method.
     */
    Composite startAppli(String compositeTypeName);

    /**
     * deploys the bundle found at the provided URL. Looks in that bundle for a composite type
     * with name "compositeTypeName".
     * If found creates that composite type and creates an instance of that type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    Composite startAppli(URL compoTypeURL, String compositeTypeName);

 
    /**
     * Creates a root composite type i.e. an application.
     * A single composite with this name can exist in APAM. Returns null if name conflicts.
     * 
     * @param inCompoType: name of the father composite type. Null if root (application).
     * @param name : the symbolic name. Unique.
     * @param mainImplem : The name of the main implementation or a specification name. If not found, returns null.
     * @param models optional : the associated models.
     * @param attributes optional : the initial properties to associate with this composite type (as an implementation).
     *            @ return : the created composite type
     */
    CompositeType createCompositeType(String inCompoType,
    		String name, String specName, String mainImplSpecName,
            Set<ManagerModel> models, Map<String, String> attributes);


    /**
     * Return the composite type of that name, if existing. Null otherwise.
     * 
     * @param name
     * @return
     */
    CompositeType getCompositeType(String name);

    /**
     * Return all the composite types known in the system.
     * 
     * @return
     */
    Collection<CompositeType> getCompositeTypes();

    /**
     * 
     * @return all the root composite types (embedded in the system root composite type)
     */
    Collection<CompositeType> getRootCompositeTypes();

    /**
     * 
     * @param name
     * @return the composite of that name, null if not existing.
     */
    Composite getComposite(String name);

    /**
     * 
     * @return return all the composites known by the system.
     */
    Collection<Composite> getComposites();

    /**
     * 
     * @return all the root composites. Also called "applications"
     */
    Collection<Composite> getRootComposites();

}
