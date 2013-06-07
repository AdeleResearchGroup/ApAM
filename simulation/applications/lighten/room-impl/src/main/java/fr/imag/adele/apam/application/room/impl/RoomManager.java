package fr.imag.adele.apam.application.room.impl;

import java.util.HashMap;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Instance;
import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.location.ZoneListener;
import fr.liglab.adele.icasa.simulator.SimulationManager;

public class RoomManager implements ZoneListener {

	private SimulationManager manager;
	
	private Apam apam;
	
	@Override
	public void zoneVariableAdded(Zone arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneVariableModified(Zone arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneVariableRemoved(Zone arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deviceAttached(Zone arg0, LocatedDevice arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deviceDetached(Zone arg0, LocatedDevice arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneMoved(Zone arg0, Position arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneParentModified(Zone arg0, Zone arg1) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void zoneResized(Zone arg0) {
		// TODO Auto-generated method stub
		
	}

	public void start(Instance instance){
		manager.addListener(this);
	}
	
	public void stop(Instance instance){
		manager.removeListener(this);
	}

	@Override
	public void zoneAdded(final Zone zone) {
		// TODO Auto-generated method stub
		System.out.println("------- zone added");
		CompositeType compoType=(CompositeType)CST.apamResolver.findImplByName(null, "generic-room");
		
		Instance inst=compoType.createInstance(null, new HashMap<String, String>(){{put("location",zone.getId());}});
		
		if(inst==null){
			System.out.println("----Impossible to create zone");
		}else {
			System.out.println("----Zone created");
		}
		
	}
	
	@Override
	public void zoneRemoved(Zone arg0) {
		// TODO Auto-generated method stub
		System.out.println("------- zone removed");
	}
	
}
