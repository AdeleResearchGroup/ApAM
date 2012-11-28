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
