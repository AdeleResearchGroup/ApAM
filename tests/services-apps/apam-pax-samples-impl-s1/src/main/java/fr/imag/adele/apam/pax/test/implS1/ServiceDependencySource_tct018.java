/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * ServiceDependencySource_tct018.java - 9 août 2013
 */
package fr.imag.adele.apam.pax.test.implS1;

/**
 * @author thibaud
 *
 */
public class ServiceDependencySource_tct018 {

    ServiceDependencyTarget_tct018 target;
    
    public void getAndReleaseTarget() {
	if (target != null)
	    target.use();
	else System.out.println("Target not found");
	target=null;
    }
    
    public void getAndKeepTarget() {
	if (target != null)
	    target.use();
	else System.out.println("Target not found");
    }

}
