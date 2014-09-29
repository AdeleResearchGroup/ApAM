package com.francetelecom.m2m.gateway.bundle.application;

import java.util.ArrayList;
import java.util.List;

public class ApplicationStatus {

	/**
	 * GREEN_STATE --> Fire application is fully functionnal. Service is enabled
	 * and all devices are available.
	 */
	private static final int GREEN_STATE = 0;

	/**
	 * ORANGE_STATE --> missing either pump devices or warning devices
	 */
	private static final int ORANGE_STATE = 1;

	/**
	 * RED_STATE --> missing all devices or missing smoke detectors or service
	 * disabled
	 */
	private static final int RED_STATE = 2;

	/**
	 * Report the service is disabled (cf {@link #errorCodes})
	 */
	private static final String SERVICE_DISABLED = "SERVICE_DISABLED";

	/**
	 * Report no smoke detectors are available (cf {@link #errorCodes})
	 */
	private static final String NO_SMOKE_DETECTOR_DEVICES = "NO_SMOKE_DETECTOR_DEVICES";

	/**
	 * Report no warning devices are available (cf {@link #errorCodes})
	 */
	private static final String NO_WARNING_DEVICES = "NO_WARNING_DEVICES";

	/**
	 * Report no pump devices are available (cf {@link #errorCodes})
	 */
	private static final String NO_PUMP_DEVICES = "NO_PUMP_DEVICES";

	/**
	 * current state. One of:
	 * <ul>
	 * <li>{@link ApplicationStatus#GREEN_STATE}</li>
	 * <li>{@link ApplicationStatus#RED_STATE}</li>
	 * <li>{@link ApplicationStatus#ORANGE_STATE}</li>
	 * </ul>
	 */
	private int currentState;

	/**
	 * List of Error code explaining why the application is in
	 * {@link #RED_STATE} state or {@link #ORANGE_STATE} state. This list may
	 * contain the following strings:
	 * <ul>
	 * <li>{@link #SERVICE_DISABLED}</li>
	 * <li>{@link #NO_PUMP_DEVICES}</li>
	 * <li>{@link #NO_SMOKE_DETECTOR_DEVICES}</li>
	 * <li>{@link #NO_WARNING_DEVICES}</li>
	 * </ul>
	 */
	private final List errorCodes;

	/**
	 * true if at least one smoke detector device is available.
	 */
	private boolean smokeDetectorsAvailable = false;

	/**
	 * true if at least one warning device is available
	 */
	private boolean warningDevicesAvailable = false;

	/**
	 * true if at least one pump device is available
	 */
	private boolean pumpsAvailable = false;

	/**
	 * true if the service is enabled
	 */
	private boolean serviceEnabled = false;

	/**
	 * Default constructor.
	 */
	public ApplicationStatus() {
		errorCodes = new ArrayList();
		evaluateState();
	}
	
	/**
	 * Private constructor.
	 * SHOULD be only used for cloning.
	 * @param state current state
	 * @param pErrorCodes error code
	 */
	private ApplicationStatus(int state, List pErrorCodes) {
		currentState = state;
		errorCodes = pErrorCodes;
	}

	/**
	 * Set smoke detectors device availability
	 * 
	 * @param available
	 *            true if at least one smoke detector is available.
	 */
	public void setSmokeDetectorsAvailable(boolean available) {
		smokeDetectorsAvailable = available;
		evaluateState();
	}

	/**
	 * Set warning devices availability.
	 * 
	 * @param available
	 *            true if at least one warning device is available.
	 */
	public void setWarningDevicesAvailable(boolean available) {
		warningDevicesAvailable = available;
		evaluateState();
	}

	/**
	 * Set pump devices availability
	 * 
	 * @param available
	 *            true if at least one pump device is available.
	 */
	public void setPumpDevicesAvailable(boolean available) {
		pumpsAvailable = available;
		evaluateState();
	}

	/**
	 * Set whether the service is enabled or not.
	 * 
	 * @param enabled
	 *            true if the service is enabled.
	 */
	public void setServiceEnabled(boolean enabled) {
		serviceEnabled = enabled;
		evaluateState();
	}
	
	/**
	 * Retrieves current state
	 * @return current state
	 */
	public int getCurrentState() {
		return currentState;
	}
	
	/**
	 * Retrieves error codes.
	 * @return error codes.
	 */
	public List getErrorCodes() {
		return errorCodes;
	}

	/**
	 * Compute the current state of the application based on the following
	 * rules:
	 * <ul>
	 * <li>RED state if
	 * <ul>
	 * <li>no smoke detector are available</li>
	 * <li>OR no warning device and no pumps are available</li>
	 * <li>OR the service is disabled</li>
	 * </ul>
	 * </li>
	 * <li>YELLOW state whether the service is enabled and whether :
	 * <ul>
	 * <li>no warning devices are available</li>
	 * <li>OR no pump devices are available</li>
	 * </ul>
	 * </li>
	 * <li>GREEN state if all devices are available and the service is enabled</li>
	 * </ul>
	 */
	private void evaluateState() {
		// clear previous error codes
		errorCodes.clear();

		if (!serviceEnabled) {
			currentState = RED_STATE;
			errorCodes.add(SERVICE_DISABLED);
		}

		if (!pumpsAvailable) {
			errorCodes.add(NO_PUMP_DEVICES);
		}

		if (!smokeDetectorsAvailable) {
			errorCodes.add(NO_SMOKE_DETECTOR_DEVICES);
		}

		if (!warningDevicesAvailable) {
			errorCodes.add(NO_WARNING_DEVICES);
		}

		// compute current state
		if (errorCodes.isEmpty()) {
			currentState = GREEN_STATE;
		} else if (errorCodes.contains(SERVICE_DISABLED)) {
			currentState = RED_STATE;
		} else if (errorCodes.contains(NO_SMOKE_DETECTOR_DEVICES)) {
			currentState = RED_STATE;
		} else if (errorCodes.contains(NO_PUMP_DEVICES)
				&& errorCodes.contains(NO_SMOKE_DETECTOR_DEVICES)
				&& errorCodes.contains(NO_WARNING_DEVICES)) {
			currentState = RED_STATE;
		} else if (errorCodes.contains(NO_PUMP_DEVICES)
				|| errorCodes.contains(NO_WARNING_DEVICES)) {
			currentState = ORANGE_STATE;
		}
	}
	
	/**
	 * Clone the current ApplicationStatus.
	 */
	protected Object clone() throws CloneNotSupportedException {
		ApplicationStatus as = new ApplicationStatus(currentState, errorCodes);
		return as;
	}

}
