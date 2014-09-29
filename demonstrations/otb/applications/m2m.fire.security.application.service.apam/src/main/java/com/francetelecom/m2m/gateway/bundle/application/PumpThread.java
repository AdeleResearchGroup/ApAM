package com.francetelecom.m2m.gateway.bundle.application;

import com.francetelecom.m2m.gateway.service.machine.zcl.element.EndPointService;
import com.francetelecom.m2m.gateway.service.machine.zcl.element.exception.ClusterCommandeException;
import com.francetelecom.m2m.gateway.service.machine.zcl.standard.cluster.OnOff;
import com.francetelecom.m2m.gateway.service.machine.zcl.standard.cluster.OnOffServerService;

public class PumpThread implements Runnable {

	private final EndPointService endpoint;
	private final OnOffServerService onOffPump;
	private boolean closing;

	public PumpThread(EndPointService pEndpoint, boolean pClosing) {
		System.out.println("new Pump thread");
		endpoint = pEndpoint;
		onOffPump = (OnOffServerService) endpoint
				.getServerSideCluster(OnOff.CLUSTER_NAME);
		closing = pClosing;
	}

	public void run() {
		System.out.println("closing pump");
		try {
			if (closing) {
				onOffPump.off();
			} else {
				Thread.sleep(15000l);
				onOffPump.on();
			}
		} catch (ClusterCommandeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("pump closed");
	}

	public void start() {
		Thread t = new Thread(this);
		t.start();
	}

}
