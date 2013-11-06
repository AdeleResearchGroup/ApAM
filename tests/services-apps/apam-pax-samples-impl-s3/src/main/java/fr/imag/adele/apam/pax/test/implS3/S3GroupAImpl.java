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
package fr.imag.adele.apam.pax.test.implS3;

import javax.xml.parsers.ParserConfigurationException;

import fr.imag.adele.apam.pax.test.iface.S3;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;

public class S3GroupAImpl implements S3 {

    ParserConfigurationException e;

    Eletronic element;

    S3 c;

    public S3 getC() {
	return c;
    }

    public Eletronic getElement() {
	return element;
    }

    public void setC(S3 c) {
	this.c = c;
    }

    public void setElement(Eletronic element) {
	this.element = element;
    }

    @Override
    public String whoami() {
	return this.getClass().getName();
    }

}
