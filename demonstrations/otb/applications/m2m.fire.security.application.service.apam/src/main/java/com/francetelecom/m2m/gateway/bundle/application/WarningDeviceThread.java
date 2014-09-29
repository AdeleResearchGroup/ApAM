package com.francetelecom.m2m.gateway.bundle.application;

import com.francetelecom.m2m.gateway.service.machine.zcl.element.EndPointService;
import com.francetelecom.m2m.gateway.service.machine.zcl.element.exception.ClusterCommandeException;
import com.francetelecom.m2m.gateway.service.machine.zcl.standard.cluster.IASWD;
import com.francetelecom.m2m.gateway.service.machine.zcl.standard.cluster.IASWDServerService;

public class WarningDeviceThread implements Runnable {
	
	private final EndPointService endpoint;
	private final IASWDServerService iasWDService;
	

	public WarningDeviceThread(EndPointService pEndpoint) {
		System.out.println("new warnind device thread");
		endpoint = pEndpoint;
		iasWDService = (IASWDServerService) pEndpoint
				.getServerSideCluster(IASWD.CLUSTER_NAME);
	}

	public void run() {
		System.out.println("start warning");
		try {
			iasWDService.startWarning(new Long(2), new Long(10));
		} catch (ClusterCommandeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("stop warning");

	}
	
	public void start() {
		Thread t = new Thread(this);
		t.start();
	}

}
