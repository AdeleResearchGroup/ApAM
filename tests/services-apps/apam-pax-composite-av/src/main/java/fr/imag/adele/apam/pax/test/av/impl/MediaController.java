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
 * MediaController.java - 1 août 2013
 */
package fr.imag.adele.apam.pax.test.av.impl;

import java.util.Set;

import fr.imag.adele.apam.pax.test.av.spec.MediaControlPoint;
import fr.imag.adele.apam.pax.test.av.spec.MediaRenderer;
import fr.imag.adele.apam.pax.test.av.spec.MediaServer;
import fr.imag.adele.apam.pax.test.av.spec.RemoteControl;

/**
 * @author thibaud
 *
 */
public class MediaController implements MediaControlPoint {
    Set<MediaServer> theServers;
    Set<MediaRenderer> theRenderers;
    RemoteControl userInterface;
    
    @Override
    public int resolveServersNumber() {
	if (theServers != null)
	    return theServers.size();
	else return 0;
    }
    
    @Override
    public int resolveRenderersNumber() {
	if (theRenderers != null)
	    return theRenderers.size();
	else return 0;
    }
    
    @Override
    public boolean resolveRemoteControl() {
	if(userInterface!=null)
	    return true;
	else return false;
    }
    
    
}
