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
 * UpnpMediaServer.java - 1 août 2013
 */
package fr.imag.adele.apam.pax.test.av.impl;

import fr.imag.adele.apam.pax.test.av.spec.MediaServer;

/**
 * @author thibaud
 *
 */
public class NASMediaServer implements MediaServer {

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.pax.test.av.spec.MediaServer#getMovieStream(java.lang.String)
     */
    @Override
    public String getMovieStream(String movieTitle) {
	return new String("http://myNAS/"+movieTitle+".avi");
    }

}
