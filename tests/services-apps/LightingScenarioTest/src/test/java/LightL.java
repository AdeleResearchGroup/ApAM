import fr.imag.adele.apam.test.lights.binarylight.SwingBinaryLightImpl;

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
 * LightL.java - 2 juil. 2013
 */

/**
 * @author thibaud
 *
 */
public class LightL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingBinaryLightImpl defaultLight = new SwingBinaryLightImpl();
		defaultLight.started();
//		defaultLight.setLightStatus(true);
//		defaultLight.setLightStatus(false);
		defaultLight.stopped();
	}

}
