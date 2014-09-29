/*
 * Created on 1/1/2010
 *
 * Projet EnergySavingApplicationService
 */
package com.francetelecom.m2m.gateway.bundle.application;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.x3d.IX3DDevice;

import com.francetelecom.m2m.gateway.service.machine.zcl.element.ClusterService;
import com.francetelecom.m2m.gateway.service.machine.zcl.element.EndPointService;
import com.francetelecom.m2m.gateway.service.machine.zcl.element.exception.ClusterCommandeException;
import com.francetelecom.m2m.gateway.service.machine.zcl.standard.cluster.IASZoneServerService;
import com.francetelecom.m2m.gateway.service.machine.zcl.standard.cluster.OnOff;
import com.francetelecom.m2m.gateway.service.machine.zcl.standard.cluster.OnOffServerService;
import com.orange.openthebox.hab.HueLightDevice;
import com.st.greennet.service.Device;

/**
 * @author Fabrice Blache
 */
public class FireApplicationService_Impl implements EventHandler {

	/*
	 * Reference sur le service de log
	 */
	protected LogService logService = null;


	private Vector zones = new Vector();

	private Vector wds = new Vector();

	private Vector pumps = new Vector();
	
	private Vector x3dLightDimmers = new Vector();
	
	private Vector hueLights = new Vector();
	
	private Vector greennetSwitchs = new Vector();

	private boolean isEnabled = false;

	private HttpService httpService;

	private FireApplicationServlet servlet;

	private boolean hasToBeTreated = true;;

	private ApplicationStatus status;
	
	private boolean onFire = false;
	

	/**
	 * Constructeur
	 * 
	 * @param componentContext
	 */
	public FireApplicationService_Impl() {
		status = new ApplicationStatus();

	}

	/**
	 * Methode de l'interface LifeCycle Appelle apres le constructeur et apres
	 * les set des services requis
	 */
	public void activate() {

		System.out.println("Activate M2M Fire Application");

		enable();

	}

	public void handleEvent(final Event event) {

		printMessage(LogService.LOG_DEBUG, this.getClass().getName()
				+ "________________________" + event.toString());

		String topic = event.getTopic();
		Long zoneStatus = (Long) event.getProperty("Zone Status");

		if ("zcl/command/received/ZoneStatusChangeNotification".equals(topic)) {

			if ((new Long(4452).equals(zoneStatus))) {

				// alarm

				if (hasToBeTreated && isEnabled) {
					onFire = true;
					hasToBeTreated = false;

					synchronized (wds) {
						for (Enumeration e = wds.elements(); e
								.hasMoreElements();) {
							WarningDeviceThread adt = new WarningDeviceThread(
									(EndPointService) e.nextElement());
							adt.start();
						}
					}

					synchronized (pumps) {
						for (Enumeration e = pumps.elements(); e.hasMoreElements();) {
							PumpThread pt = new PumpThread(
									(EndPointService) e.nextElement(), true);
							pt.start();
						}
					}
					
					synchronized (x3dLightDimmers) {
						for(Iterator it = x3dLightDimmers.iterator(); it.hasNext();) {
							X3DLightDimmer shutter = (X3DLightDimmer) it.next();
							X3DLightThread xt = new X3DLightThread(shutter, 10000);
							xt.start();
						}
					}
					
					synchronized (hueLights) {
						for(Iterator it = hueLights.iterator(); it.hasNext();) {
							HueLightDevice hueLight = (HueLightDevice) it.next();
							HueLightThread hlt = new HueLightThread(hueLight, 10000);
							hlt.start();
						}
					}
					
					synchronized (greennetSwitchs) {
						for(Iterator it = greennetSwitchs.iterator(); it.hasNext();) {
							Device greennetSwitch = (Device) it.next();
							GreenNetSwitchThread gst = new GreenNetSwitchThread(greennetSwitch, true);
							gst.start();
						}
					}
					

				} else {
					System.out
							.println("event is not treated as it has been treated previously");
					return;
				}

			} else if (new Long(4196).equals(zoneStatus)) {
				// reset alarm
				System.out.println("receive 4196");
				hasToBeTreated = true;
				for (Enumeration e = pumps.elements(); e.hasMoreElements();) {
					PumpThread pt = new PumpThread(
							(EndPointService) e.nextElement(), false);
					pt.start();
				}
				
				// stop breathing
				synchronized (greennetSwitchs) {
					for(Iterator it = greennetSwitchs.iterator(); it.hasNext();) {
						Device greennetSwitch = (Device) it.next();
						GreenNetSwitchThread gst = new GreenNetSwitchThread(greennetSwitch, false);
						gst.start();
					}
				}

				onFire = false;
			}

		}

		// Thread traiteEvent = new Thread() {
		//
		// public void run() {
		//
		// // PostEvent command : On {bundle.symbolicName=command,
		// // clusterId=6,
		// // endpointId=2, technoId=58020000007A1300,
		// // timestamp=1268225001399,
		// // mtomId=22:58020000007A1300,
		// //
		// service.objectClass=com.francetelecom.m2m.gateway.bundle.machine.zigbee.standard.cluster.proxysides.OnOffClient}
		// try {
		// EndPointService endPointService = null;
		// IASWDServerService iaswd = null;
		// OnOffServerService onOffPump = null;
		//
		// String[] propertyNames = event.getPropertyNames();
		// if (propertyNames.length == 0) {
		// System.out.println("no property");
		// } else {
		// for (int i = 0; i < propertyNames.length; i++) {
		// String propertyName = propertyNames[i];
		// Object propertyValue = event
		// .getProperty(propertyName);
		// System.out.println("propertyName = " + propertyName
		// + ", propertyValue = " + propertyValue);
		// }
		// }
		// String topic = event.getTopic();
		// Long zoneStatus = (Long) event.getProperty("Zone Status");
		//
		//
		// if ("zcl/command/received/ZoneStatusChangeNotification"
		// .equals(topic) && (new Long(4452).equals(zoneStatus))) {
		// // if ("Fire indication".equals((String)
		// // event.getProperty("Alarm")) ||
		// // "CO indication".equals((String)
		// // event.getProperty("Alarm"))) {
		// printMessage(LogService.LOG_DEBUG, this.getClass()
		// .getName()
		// + "________________________"
		// + (String) event.getProperty("Alarm"));
		//
		//
		//
		// for (Enumeration e = wds.elements(); e
		// .hasMoreElements();) {
		// endPointService = (EndPointService) e.nextElement();
		// iaswd = (IASWDServerService) endPointService
		// .getServerSideCluster(IASWD.CLUSTER_NAME);
		//
		// System.out.println("start warning");
		// iaswd.startWarning(new Long(2), new Long(10));
		// System.out.println("end warning");
		// // iaswd.armed(IASWD.HIGH_LEVEL_SOUND, true);
		// // iaswd.fire(10, true);
		// }
		// for (Enumeration e = pumps.elements(); e
		// .hasMoreElements();) {
		// System.out.println("send pump off");
		// endPointService = (EndPointService) e.nextElement();
		// onOffPump = (OnOffServerService) endPointService
		// .getServerSideCluster(OnOff.CLUSTER_NAME);
		// onOffPump.off();
		// System.out.println("pump off end");
		// }
		// }
		//
		// } catch (Exception ex) {
		// printMessage(LogService.LOG_ERROR, "handleEvent", ex);
		//
		// ex.printStackTrace();
		// }
		// }
		// };

		if (isEnabled) {
			// traiteEvent.setName("Incendie");
			// traiteEvent.start();
			// try {
			// traiteEvent.join();
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
		}

	}

	/**
	 * Cette methode est appellee par le framwork osgi sur demande du
	 * servicebinder quand le service de log apparait
	 * 
	 * @param reference
	 *            de service
	 */
	public void setLogServiceReference(LogService refLogService) {
		this.logService = refLogService;
	}

	/**
	 * Cette methode est appellee par le framwork osgi sur demande du
	 * servicebinder quand le service de log disparait
	 * 
	 * @param reference
	 *            de service
	 */
	protected void unsetLogServiceReference(LogService refLogService) {
		this.logService = null;
	}

	protected void setZoneDetectorService(EndPointService refEndPointService) {
		printMessage(LogService.LOG_DEBUG, this.getClass().getName()
				+ "________________________Dectecteur");

		Hashtable clusters = refEndPointService.getServerSideClusters();
		for (Iterator it = clusters.keySet().iterator(); it.hasNext();) {
			// System.out.println("cluster key = " + it.next());
			ClusterService cs = refEndPointService
					.getServerSideCluster((Integer) it.next());

			// A revoir !!!!!!!!!!!!!!!!!!!!!!
			if (cs.getName().startsWith("IAS")) {
				IASZoneServerService izss = (IASZoneServerService) cs;
				try {
					izss.zoneEnrollResponse(new Long(0), new Long(0));
				} catch (ClusterCommandeException e) {
					e.printStackTrace();
				}
				
				break;
			}
		}

		synchronized (zones) {
			zones.add(refEndPointService);
		}

		status.setSmokeDetectorsAvailable(true);
	}

	protected void unsetZoneDetectorService(EndPointService refEndPointService) {
		synchronized (zones) {
			zones.remove(refEndPointService);
			if (zones.isEmpty()) {
				status.setSmokeDetectorsAvailable(false);
			}
		}
	}

	protected void setWarningDeviceService(EndPointService refEndPointService) {
		printMessage(LogService.LOG_DEBUG, this.getClass().getName()
				+ "________________________Sirene");
		synchronized (wds) {
			wds.add(refEndPointService);
		}
		status.setWarningDevicesAvailable(true);

	}

	protected void unsetWarningDeviceService(EndPointService refEndPointService) {
		synchronized (wds) {
			wds.remove(refEndPointService);
			if (wds.isEmpty()) {
				status.setWarningDevicesAvailable(false);
			}
		}
	}

	protected void setPumpDeviceService(EndPointService refEndPointService) {
		printMessage(LogService.LOG_DEBUG, this.getClass().getName()
				+ "________________________Pump");
		synchronized (pumps) {
			pumps.add(refEndPointService);
		}

		status.setPumpDevicesAvailable(true);
		openPump(refEndPointService);

	}

	protected void unsetPumpDeviceService(EndPointService refEndPointService) {
		synchronized (pumps) {
			pumps.remove(refEndPointService);
			if (pumps.isEmpty()) {
				status.setPumpDevicesAvailable(false);
			}
		}
	}

	/**
	 * set the http service. Cardinality = 1..1
	 * 
	 * @param pHttpService
	 */
	protected void setHttpService(HttpService pHttpService) {
		httpService = pHttpService;

		servlet = new FireApplicationServlet(httpService, this);
		servlet.registerRestServlet();
		servlet.registerResourceServlet();
	}

	/**
	 * Unset http service
	 * 
	 * @param pHttpService
	 */
	protected void unsetHttpService(HttpService pHttpService) {
		servlet.unregisterRestServlet();
		servlet.unregisterResourceServlet();
		servlet = null;

		httpService = null;
	}

	protected void setX3DLightDimmer(IX3DDevice lightDimmer) {
		System.out.println("set new X3D shutter");
		X3DLightDimmer x3dLight = new X3DLightDimmer(lightDimmer);
		synchronized (x3dLightDimmers) {
			x3dLightDimmers.add(x3dLight);
		}
	}
	
	protected void unsetX3DLightDimmer(IX3DDevice lightDimmer) {
		synchronized (x3dLightDimmers) {
			for(Iterator it = x3dLightDimmers.iterator(); it.hasNext();) {
				X3DLightDimmer x3dLight = (X3DLightDimmer)it.next();
				if (lightDimmer.equals(x3dLight.getX3DDevice())) {
					it.remove();
				}
			}
		}
	}
	
	
	protected void setHueLight(HueLightDevice hueLight) {
		synchronized (hueLights) {
			hueLights.add(hueLight);
		}
		
	}
	
	protected void unsetHueLight(HueLightDevice hueLight) {
		synchronized (hueLights) {
			hueLights.remove(hueLight);
		}
	}

	protected void setGreenetSwitch(Device greenNetSwitch) {
		System.out.println("add new switch " + greenNetSwitch);
		synchronized (greennetSwitchs) {
			greennetSwitchs.add(greenNetSwitch);
		}
	}
	
	protected void unsetGreenetSwitch(Device greenNetSwitch) {
		synchronized (greennetSwitchs) {
			greennetSwitchs.remove(greenNetSwitch);
		}
	}
	
	/**
	 * print message by using the LogService.
	 * 
	 * @param level
	 * @param message
	 */
	public void printMessage(int level, String message) {
		printMessage(level, message, null);
	}

	/**
	 * print message by using the LogService.
	 * 
	 * @param level
	 * @param message
	 * @param t
	 */
	public void printMessage(int level, String message, Throwable t) {
		if (logService != null) {
			synchronized (logService) {
				if (t != null) {
					logService.log(level, message, t);
				} else {
					logService.log(level, message);
				}
			}
		} else {
			if (t != null) {
				System.out.println(message);
				t.printStackTrace();
			} else {
				System.out.println(message);
			}
		}
	}

	protected void initService() {
		for (Enumeration e = pumps.elements(); e.hasMoreElements();) {
			EndPointService pumpEndpointService = (EndPointService) e
					.nextElement();
			openPump(pumpEndpointService);
		}
	}

	/**
	 * Enable the service. From now, when a Warning event is received from the
	 * smoke detectors, the pumps are going to be closed and the warning device
	 * are squarking.
	 */
	protected void enable() {
		isEnabled = true;
		status.setServiceEnabled(true);
	}

	/**
	 * Disable the service. Warning device and pumps are deactivated.
	 */
	protected void disable() {
		isEnabled = false;
		status.setServiceEnabled(false);
	}

	/**
	 * Get all pumps
	 * 
	 * @return pumps
	 */
	protected List getPumps() {
		List l = new ArrayList();
		synchronized (pumps) {
			l.addAll(pumps);
		}
		return l;
	}

	/**
	 * Get all warning devices.
	 * 
	 * @return warning devices.
	 */
	protected List getWarningDevices() {
		List l = new ArrayList();
		synchronized (wds) {
			l.addAll(wds);
		}

		return l;
	}

	/**
	 * Get all zone device.
	 * 
	 * @return zone devices
	 */
	protected List getZoneDevices() {
		List l = new ArrayList();
		synchronized (zones) {
			l.addAll(zones);
		}
		return l;
	}
	
	/**
	 * Get all x3d dimmers.
	 * @return x3d dimmers
	 */
	protected List getX3DDimmers() {
		List l = new ArrayList();
		synchronized (x3dLightDimmers) {
			l.addAll(x3dLightDimmers);
		}
		return l;
	}
	
	/**
	 * Get all hue ligths
	 * @return hue lights
	 */
	protected List getHueLights() {
		List l = new ArrayList();
		synchronized (hueLights) {
			l.addAll(hueLights);
		}
		return l;
	}
	
	/**
	 * Get all GreenNet switches
	 * @return greennet switches
	 */
	protected List getGreenNetSwitches() {
		List l = new ArrayList();
		synchronized (greennetSwitchs) {
			l.addAll(greennetSwitchs);
		}
		return l;
	}

	/**
	 * Returns true if the Fire service is activated.
	 * 
	 * @return
	 */
	protected ApplicationStatus getStatus() {
		ApplicationStatus as = null;
		try {
			as = (ApplicationStatus) status.clone();
		} catch (CloneNotSupportedException e) {
		}
		return as;
	}

	/**
	 * Open a pump
	 * 
	 * @param pumpEndpointService
	 */
	private void openPump(EndPointService pumpEndpointService) {
		System.out.println("open pump");
		OnOffServerService onOffPump = null;
		onOffPump = (OnOffServerService) pumpEndpointService
				.getServerSideCluster(OnOff.CLUSTER_NAME);
		try {
			onOffPump.on();
		} catch (ClusterCommandeException e) {
			printMessage(LogService.LOG_ERROR, "unable to open the pump");
		}
	}

}
