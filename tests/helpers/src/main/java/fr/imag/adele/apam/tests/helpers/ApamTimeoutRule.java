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
package fr.imag.adele.apam.tests.helpers;

import org.junit.rules.Timeout;

public class ApamTimeoutRule extends Timeout {

    public ApamTimeoutRule(Integer millis) {
	super(millis == null ? Integer.MAX_VALUE : millis.intValue());

	System.err.println("TIMEOUT ADOPTED : "
		+ (millis == null ? Integer.MAX_VALUE : millis));

	// RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
	// List<String> arguments = RuntimemxBean.getInputArguments();
	//
	// for (String string : arguments) {
	// System.err.println("-------------------------"+string);
	// }

    }
}
