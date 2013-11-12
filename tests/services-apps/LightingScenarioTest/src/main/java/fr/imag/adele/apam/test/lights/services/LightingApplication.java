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
 * LightingApplication.java - 2 juil. 2013
 */
package fr.imag.adele.apam.test.lights.services;

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.test.lights.devices.BinaryLight;
import fr.imag.adele.apam.test.lights.devices.SimpleButton;
import fr.imag.adele.apam.test.lights.devices.messages.ButtonPressed;

/**
 * @author thibaud
 * 
 */
public class LightingApplication {
    private static Logger logger = LoggerFactory
	    .getLogger(LightingApplication.class);

    private Set<BinaryLight> theLights;
    private Set<SimpleButton> theButtons;
    private String myName;

    public void aButtonHasBeenPressed(ButtonPressed event) {
	logger.debug("aButtonHasBeenPressed(ButtonPressed event)");
	// Change the lights status
	if (theLights != null && theLights.size() > 0) {
	    Iterator<BinaryLight> it = theLights.iterator();
	    while (it.hasNext()) {
		it.next().switchLightStatus();
	    }
	}
    }

    public void newButton() {
	logger.debug(myName + ".newButton() : " + theButtons.size()
		+ " buttons in the list");
    }

    public void newLight() {
	logger.debug("newLight(), There are " + theLights.size()
		+ " in this area");
	if (theLights != null && theLights.size() > 0) {
	    Iterator<BinaryLight> it = theLights.iterator();
	    while (it.hasNext()) {
		logger.debug("-> " + it.next().getName());
	    }
	}

    }

    public void removeButton() {
	logger.debug(myName + ".removeButton() : " + theButtons.size()
		+ " buttons in the list");
    }

    public void start() {
	logger.debug("start()");
	// used to make the binding with already existing instances of lights
	if (theLights != null && theLights.size() > 0) {
	    logger.debug(theLights.size() + " lights have been found !");
	} else {
	    logger.debug("no lights for the moment");
	}
    }

    public void stop() {
	logger.debug("stop()");
    }

}
