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
package fr.imag.adele.apam.jmx;


public interface ApamJMX {

    public String test();

    /**
     * ASMSpec.
     * 
     * @param specificationName
     *            the specification name
     */

    public String up(String componentName);

    /**
     * Resolver Commands.
     */

    public String put(String componentName);

    public String put(String componentName, String compositeTarget);

    /**
     * Specifications.
     * @return 
     */

    public String specs();

    /**
     * Implementations.
     */
    public String implems();

    /**
     * Instances.
     */
    public String insts();

    /**
     * ASMSpec.
     * 
     * @param specificationName
     *            the specification name
     */
    public String spec(String specificationName);

    /**
     * ASMImpl.
     * 
     * @param implementationName
     *            the implementation name
     */

    public String implem(String implementationName);

    public String l(String componentName);

    public String launch(String componentName, String compositeTarget);

    /**
     * ASMInst.
     * 
     * @param implementationName
     *            the implementation name
     */

    public String inst(String instanceName);

    public String applis();

    public String appli(String appliName);

    public String dump();

    public String pending();

    public String compoTypes();

    public String compoType(String name);

    public String compos();

    public String compo(String compoName);

    public String wire(String instName);

}