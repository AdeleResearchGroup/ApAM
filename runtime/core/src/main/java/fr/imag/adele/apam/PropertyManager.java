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

/**
 * called when attributes of an Apam object is changed. Defines the manager
 * class "properties".
 * 
 * @author Jacky
 * 
 */
public interface PropertyManager extends Manager {
    /**
     * The attribute "attr" has been added (instantiated for the first time).
     * 
     * @param component
     *            . The component (Spec, Implem, Instance) holding that
     *            attribute.
     * @param attr
     *            . The attribute name.
     * @param newValue
     *            . The new value of that attribute.
     */
    public void attributeAdded(Component component, String attr, String newValue);

    /**
     * The attribute "attr" has been modified.
     * 
     * @param component
     *            . The component (Spec, Implem, Instance) holding that
     *            attribute.
     * @param attr
     *            . The attribute name.
     * @param newValue
     *            . The new value of that attribute.
     * @param oldValue
     *            . The previous value of that attribute.
     */
    public void attributeChanged(Component component, String attr,
	    String newValue, String oldValue);

    /**
     * The attribute "attr" has been removed.
     * 
     * @param component
     *            . The component (Spec, Implem, Instance) holding that
     *            attribute.
     * @param attr
     *            . The attribute name.
     * @param oldValue
     *            . The previous value of that attribute.
     */
    public void attributeRemoved(Component component, String attr,
	    String oldValue);
}
