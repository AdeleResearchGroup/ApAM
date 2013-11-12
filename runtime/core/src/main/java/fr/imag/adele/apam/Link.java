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

public interface Link {

    public Component getDestination();

    public String getName();

    public RelationDefinition getRelDefinition();

    public RelToResolve getRelToResolve();

    public Component getSource();

    public boolean hasConstraints();

    public boolean isInjected();

    public boolean isPromotion();

    public boolean isWire();

    /**
     * re evaluate the link. A link is automaticaly reevaluated if the target
     * disapers or if its properties are changed; or if the source properties
     * are changed and contraints use substitution.
     * 
     * If the current link does not satifies the link contraints, it is broken.
     * 
     * @param force
     *            : remove the link even if currently valid. Usefull to re
     *            interpret preferences.
     * @param eager
     *            : once the link deleted, the relation is resolved again
     *            immediately.
     */
    public void reevaluate(boolean force, boolean eager);

    public void remove();

}
