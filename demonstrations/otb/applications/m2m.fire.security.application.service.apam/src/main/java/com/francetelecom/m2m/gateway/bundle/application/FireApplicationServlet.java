package com.francetelecom.m2m.gateway.bundle.application;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import com.francetelecom.m2m.gateway.service.machine.zcl.element.EndPointService;
import com.orange.openthebox.hab.HueLightDevice;
import com.st.greennet.service.Actuator;
import com.st.greennet.service.Device;

/**
 * This servlet exposes as a REST API the Fire Application. This servlet is
 * available at /FireApplication/rest. It provides the following commands :
 * <ul>
 * <li>GET /status returns as a json string the status of the Fire application.
 * See</li>
 * </ul>
 * 
 * 
 * @author mpcy8647
 * 
 */
public class FireApplicationServlet extends HttpServlet {

	/**
	 * Rest servlet alias
	 */
	private static final String REST_SERVLET_ALIAS = "/FireApplication/rest";

	/**
	 * IHM
	 */
	private static final String IHM_ALIAS = "/FireApplication";

	/**
	 * log prefix
	 */
	private static final String LOG_PREFIX = "[FIRE_APPLICATION_REST_SERVLET] ";

	/**
	 * status path
	 */
	private static final String STATUS_PATH = "/status";

	/**
	 * enable path
	 */
	private static final String ENABLE_PATH = "/enable";

	/**
	 * disable path
	 */
	private static final String DISABLE_PATH = "/disable";

	/**
	 * init demo path.
	 */
	private static final String INIT_DEMO_PATH = "/initDemo";

	/**
	 * devices path
	 */
	private static final String DEVICES = "/devices";

	/**
	 * Zigbee techno
	 */
	private static final String ZIGBEE_TECHNO = "ZigBee";
	
	/**
	 * Hue techno
	 */
	private static final String HUE_TECHNO = "Hue";
	
	/**
	 * X3D techno
	 */
	private static final String X3D_TECHNO = "X3D";
	
	/**
	 * GreenNet techno
	 */
	private static final String GREENNET_TECHNO = "GreenNet";

	/**
	 * PUMP img.
	 */
	private static final String PUMP_IMG = "img/zigbeePump.png";

	/**
	 * SMOKE DETECTOR IMG
	 */
	private static final String SMOKE_DETECTOR_IMG = "img/zigbeeSmokeDetector.png";

	/**
	 * WARNING IMG
	 */
	private static final String WARNING_IMG = "img/zigbeeWarning.png";
	
	/**
	 * HUE LIGHT IMG
	 */
	private static final String HUE_LIGHT_IMG = "img/hueLight.png";
	
	/**
	 * X3D DIMMER IMG
	 */
	private static final String X3D_DIMMER_IMG = "img/x3dDimmer.png";
	
	/**
	 * GREENNET AIR EJECTOR IMG
	 */
	private static final String GREENNET_AIR_EJECTOR_IMG = "img/greennetAirEjector.png";

	/**
	 * PUMPS property name for device.
	 */
	private static final String PUMPS_PROP = "pumps";

	/**
	 * IAS ZONES property name for device.
	 */
	private static final String IAS_ZONES_PROP = "iasZones";

	/**
	 * IAS WARNING property name for device.
	 */
	private static final String IAS_WARNING_PROP = "iasWarnings";

	/**
	 * PUMP prefix id
	 */
	private static final String PUMP_PREFIX_ID = "pump_";

	/**
	 * IAS ZONES prefix id
	 */
	private static final String IAS_ZONES_PREFIX_ID = "iasZone_";

	/**
	 * IAS WARNING prefix id
	 */
	private static final String IAS_WARNING_PREFIX_ID = "iasWarning_";
	
	/**
	 * HUE PREFIX id
	 */
	private static final String HUE_PREFIX_ID = "hueLight_";
	
	/**
	 * x3d dimmer prefix id
	 */
	private static final String X3D_DIMMER_PREFIX_ID = "x3dDimmer_";

	/**
	 * NAME property name for device
	 */
	private static final String NAME_PROP = "name";

	/**
	 * ID property name for device
	 */
	private static final String ID_PROP = "id";

	/**
	 * TECHNO property name for device
	 */
	private static final String TECHNO_PROP = "techno";

	/**
	 * IMG property name for device
	 */
	private static final String IMG_PROP = "img";

	/**
	 * http service (mandatory)
	 */
	private final HttpService httpService;

	/**
	 * logService (mandatory)
	 */
	private LogService logService;

	/**
	 * fire application service
	 */
	private final FireApplicationService_Impl fireApplication;

	public FireApplicationServlet(final HttpService pHttpService,
			final FireApplicationService_Impl pFireApplication) {
		httpService = pHttpService;
		fireApplication = pFireApplication;
	}

	/**
	 * This method is called when an HTTP get is received by the servlet
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String path = req.getPathInfo();

		if (STATUS_PATH.equals(path)) {
			executeDoGetStatus(req, resp);
		} else if (DEVICES.equals(path)) {
			executeDoGetDevices(req, resp);
		} else {
			// invalid path
			// returns 404
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String path = req.getPathInfo();

		if (ENABLE_PATH.equals(path)) {
			executeDoPostEnable(req, resp);
		} else if (DISABLE_PATH.equals(path)) {
			executeDoPostDisable(req, resp);
		} else if (INIT_DEMO_PATH.equals(path)) {

		} else {
			// invalid path
			// returns 404
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * Register the rest servlet.
	 */
	protected void registerRestServlet() {
		try {
			httpService.registerServlet(REST_SERVLET_ALIAS, this, null, null);
		} catch (ServletException e) {
			log(LogService.LOG_ERROR, "unable to register the REST servlet "
					+ e.getMessage(), e);
		} catch (NamespaceException e) {
			log(LogService.LOG_ERROR, "unable to register the REST servlet "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Unregister this rest servlet.
	 */
	protected void unregisterRestServlet() {
		httpService.unregister(REST_SERVLET_ALIAS);
	}

	/**
	 * Register IHM resource (html, js) servlet.
	 */
	protected void registerResourceServlet() {
		try {
			httpService.registerResources(IHM_ALIAS, "WEB-INF", null);
		} catch (NamespaceException e) {
			log(LogService.LOG_ERROR,
					"unable to register the IHM servlet " + e.getMessage(), e);
		}
	}

	/**
	 * Unregister IHM resource servlet.
	 */
	protected void unregisterResourceServlet() {
		httpService.unregister(IHM_ALIAS);
	}

	/**
	 * Set the log service.
	 * 
	 * @param pLogService
	 *            log service
	 */
	protected void setLogService(LogService pLogService) {
		logService = pLogService;
	}

	protected void unsetLogService(LogService pLogService) {
		logService = null;
	}

	/**
	 * Retrieves the status as a Json list. this list contains the status
	 * boolean parameter.
	 * 
	 * @param request
	 *            request
	 * @param response
	 *            response
	 */
	private void executeDoGetStatus(final HttpServletRequest request,
			final HttpServletResponse response) {

		ApplicationStatus as = fireApplication.getStatus();
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("status", new Integer(as.getCurrentState()));

		JSONArray errorCodeJson = new JSONArray();
		errorCodeJson.addAll(as.getErrorCodes());
		jsonObject.put("errorCodes", errorCodeJson);

		response.setStatus(HttpServletResponse.SC_OK);
		try {
			response.getWriter().println(jsonObject.toString());
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log(LogService.LOG_ERROR,
					"unable to write response about application status", e);
		}
	}

	private void executeDoGetDevices(HttpServletRequest req,
			HttpServletResponse resp) {
		JSONObject devices = new JSONObject();

		// pumps
		List pumps = fireApplication.getPumps();
		JSONArray pumpsArray = new JSONArray();
		for (Iterator it = pumps.iterator(); it.hasNext();) {
			EndPointService es = (EndPointService) it.next();
			String name = es.getName();
			String friendlyName = getDeviceFriendlyNameId(es);
			name = name + " " + friendlyName;

			// remove first # from friendlyname
			friendlyName = friendlyName.replaceAll("#", "");

			String id = PUMP_PREFIX_ID + friendlyName;
			Map properties = new HashMap();
			properties.put(ID_PROP, id);
			properties.put(NAME_PROP, name);
			properties.put(TECHNO_PROP, ZIGBEE_TECHNO);
			properties.put(IMG_PROP, PUMP_IMG);
			JSONObject pumpJs = new JSONObject(properties);
			pumpsArray.add(pumpJs);
		}
		
		// GreenNet switches is also viewed as a pump (i.e. actuator)
		List greenNetSwitches = fireApplication.getGreenNetSwitches();
		for(Iterator it = greenNetSwitches.iterator(); it.hasNext();) {
			Device greenNetSwitch = (Device) it.next();
			String id = ((Actuator)greenNetSwitch).getNode().getIdentifier();
			String name = "Air extractor";
			Map properties = new HashMap();
			properties.put(ID_PROP, id);
			properties.put(NAME_PROP, name);
			properties.put(TECHNO_PROP, GREENNET_TECHNO);
			properties.put(IMG_PROP, GREENNET_AIR_EJECTOR_IMG);
			JSONObject pumpJs = new JSONObject(properties);
			pumpsArray.add(pumpJs);
		}
		devices.put(PUMPS_PROP, pumpsArray);

		// ias zones
		List zones = fireApplication.getZoneDevices();
		JSONArray zonesArray = new JSONArray();
		for (Iterator it = zones.iterator(); it.hasNext();) {
			EndPointService es = (EndPointService) it.next();
			String name = es.getName();
			String friendlyName = getDeviceFriendlyNameId(es);
			name = name + " " + friendlyName;

			// remove first # from friendlyname
			friendlyName = friendlyName.replaceAll("#", "");
			String id = IAS_ZONES_PREFIX_ID + friendlyName;
			Map properties = new HashMap();
			properties.put(ID_PROP, id);
			properties.put(NAME_PROP, name);
			properties.put(TECHNO_PROP, ZIGBEE_TECHNO);
			properties.put(IMG_PROP, SMOKE_DETECTOR_IMG);
			JSONObject zoneJs = new JSONObject(properties);
			zonesArray.add(zoneJs);
		}
		devices.put(IAS_ZONES_PROP, zonesArray);

		// warning devices
		List warnings = fireApplication.getWarningDevices();
		JSONArray warningArray = new JSONArray();
		for (Iterator it = warnings.iterator(); it.hasNext();) {
			EndPointService es = (EndPointService) it.next();
			String name = es.getName();
			String friendlyName = getDeviceFriendlyNameId(es);
			name = name + " " + friendlyName;

			// remove first # from friendlyname
			friendlyName = friendlyName.replaceAll("#", "");
			String id = IAS_WARNING_PREFIX_ID + friendlyName;
			Map properties = new HashMap();
			properties.put(ID_PROP, id);
			properties.put(NAME_PROP, name);
			properties.put(TECHNO_PROP, ZIGBEE_TECHNO);
			properties.put(IMG_PROP, WARNING_IMG);
			JSONObject warningJs = new JSONObject(properties);
			warningArray.add(warningJs);
		}
		
		// x3d dimmers are also considered as warning devices
		List x3dDimmers = fireApplication.getX3DDimmers();
		int index = 1;
		for(Iterator it = x3dDimmers.iterator(); it.hasNext();) {
			X3DLightDimmer dimmer = (X3DLightDimmer) it.next();
			
			String id = X3D_DIMMER_PREFIX_ID + dimmer.getX3DDevice().getDeviceUri();
			id = id.replaceAll(" ", "");
			id = id.replaceAll("/", "");
			id = id.replaceAll(":", "");
			String name = "X3D Dimmer #" + index;
			index++;
			
			Map properties = new HashMap();
			properties.put(ID_PROP, id);
			properties.put(NAME_PROP, name);
			properties.put(TECHNO_PROP, X3D_TECHNO);
			properties.put(IMG_PROP, X3D_DIMMER_IMG);
			JSONObject warningJs = new JSONObject(properties);
			warningArray.add(warningJs);
		}
		
		// hue light are also considered as warning devices
		List hueLights = fireApplication.getHueLights();
		for(Iterator it = hueLights.iterator(); it.hasNext();) {
			HueLightDevice hld = (HueLightDevice) it.next();
			String name = hld.getName();
			String id = HUE_PREFIX_ID + hld.getId();
			
			Map properties = new HashMap();
			properties.put(ID_PROP, id);
			properties.put(NAME_PROP, name);
			properties.put(TECHNO_PROP, HUE_TECHNO);
			properties.put(IMG_PROP, HUE_LIGHT_IMG);
			JSONObject warningJs = new JSONObject(properties);
			warningArray.add(warningJs);
		}
		devices.put(IAS_WARNING_PROP, warningArray);
		
		

		try {
			resp.getWriter().print(devices.toJSONString());
			resp.setStatus(HttpServletResponse.SC_OK);
		} catch (IOException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * Enable the application.
	 * 
	 * @param request
	 *            request
	 * @param response
	 *            response (200 OK)
	 */
	private void executeDoPostEnable(final HttpServletRequest request,
			final HttpServletResponse response) {
		fireApplication.enable();
		response.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Disable the application.
	 * 
	 * @param request
	 *            request
	 * @param response
	 *            response (200 OK)
	 */
	private void executeDoPostDisable(final HttpServletRequest request,
			final HttpServletResponse response) {
		fireApplication.disable();
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	/**
	 * Init the demo, i.e. open all pumps.
	 * 
	 * @param request
	 * @param response
	 */
	private void executeDoPostInitDemo(final HttpServletRequest request,
			final HttpServletResponse response) {
		fireApplication.initService();
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	/**
	 * Log message using the log service if available
	 * 
	 * @param logLevel
	 *            Http service log level
	 * @param msg
	 *            message
	 * @param e
	 *            exception. May be null
	 */
	private void log(int logLevel, String msg, Throwable e) {
		if (logService != null) {
			logService.log(logLevel, LOG_PREFIX + msg, e);
		}
	}

	private static String getDeviceFriendlyNameId(EndPointService es) {
		String fnId = "#";

		String technoId = es.getCapillaryElementService().getTechnoId();
		// get the four last character of the technoId field
		fnId = fnId
				+ technoId.substring(technoId.length() - 4, technoId.length());

		return fnId;
	}

}
