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

public interface Composite extends Instance {

    /**
     * Adds a new depend relationship toward "destination"
     * 
     * @param destination
     */
    public void addDepend(Composite destination);

    /**
     * return true if the instance is contained in the current one.
     * 
     * @param inst
     * @return
     */
    public boolean containsInst(Instance inst);

    /**
     * returns true if this composite depends on "destination"
     * 
     * @param destination
     * @return
     */
    public boolean dependsOn(Composite destination);

    /**
     * 
     * @return the type of that composite
     */
    public CompositeType getCompType();

    /**
     * return all the instances contained in the current composite.
     * 
     * @return
     */
    public Set<Instance> getContainInsts();

    /**
     * 
     * @return the existing depend relationships
     */
    public Set<Composite> getDepend();

    /**
     * 
     * @return the father composite i.e. the composite that contains this one.
     */
    public Composite getFather();

    /**
     * 
     * @return the composite that depend on this one.
     */
    public Set<Composite> getInvDepend();

    /**
     * 
     * @return the main implementation
     */
    public Implementation getMainImpl();

    /**
     * returns the main instance
     * 
     * @return
     */
    public Instance getMainInst();

    /**
     * returns the model with the "name"
     * 
     * @param name
     * @return
     */
    public ManagerModel getModel(String name);

    /**
     * 
     * @return all the models for that composite type
     */
    public Set<ManagerModel> getModels();

    /**
     * Overrides the instance method. Instead to return apfor.getserviceobject,
     * return the main instance object
     */
    @Override
    public Object getServiceObject();

    /**
     * 
     * @return the set of sons of this composite (i.e. the implementations it
     *         contains that are composites.
     */
    public Set<Composite> getSons();

}
