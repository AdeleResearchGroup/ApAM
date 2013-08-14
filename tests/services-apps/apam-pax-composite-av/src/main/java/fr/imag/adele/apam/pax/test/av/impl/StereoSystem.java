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
 * StereoSystem.java - 1 août 2013
 */
package fr.imag.adele.apam.pax.test.av.impl;

import fr.imag.adele.apam.pax.test.av.spec.MediaRenderer;

/**
 * @author thibaud
 *
 */
public class StereoSystem implements MediaRenderer {

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.pax.test.av.spec.MediaRenderer#play(java.lang.String)
     */
    @Override
    public void play(String movieStream) {
	System.out.println("I am playing the audio of "+movieStream);
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.pax.test.av.spec.MediaRenderer#stop()
     */
    @Override
    public void stop() {
	System.out.println("I stop playing audio");


    }

}
