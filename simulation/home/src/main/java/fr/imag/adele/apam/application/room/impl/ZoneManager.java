package fr.imag.adele.apam.application.room.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.location.ZoneListener;
import fr.liglab.adele.icasa.simulator.SimulationManager;


/**
 * The zone manager listen to zone creation events in the iCasa simulation environment,
 * and automatically instantiates the corresponding APAM representation of a room
 * 
 * @author vega
 *
 */
@Component
@Provides(specifications = ZoneListener.class)

@Instantiate
public class ZoneManager implements ZoneListener {

	@Requires(proxy = false)
	private SimulationManager manager;
	
	@SuppressWarnings("unused")
	@Requires(proxy = false)
	private Apam apam;
	
	private final Map<Zone,Instance> rooms;
	
	public ZoneManager() {
		rooms = new HashMap<Zone, Instance>();
	}
	
	@Validate
	public void start(){
		manager.addListener(this);
	}
	
	@Invalidate
	public void stop(){
		manager.removeListener(this);
		rooms.clear();
	}
	
	@Override
	public void zoneAdded(Zone zone) {

		System.out.println("------- zone added");
		
		Implementation roomType	= CST.apamResolver.findImplByName(null, "Room");
		Instance room 			= roomType.createInstance(null, Collections.singletonMap("location",zone.getId()));
		
		if(room == null) {
			System.out.println("----Impossible to create zone");
			return;
		}
		
		rooms.put(zone,room);
		System.out.println("----Zone created");
		
		/*
		 * Automatically start applications
		 */
		Implementation lightAutomation = CST.apamResolver.findImplByName(null, "BasicLightAutomation");
		lightAutomation.createInstance((Composite)room, Collections.singletonMap("location",zone.getId()));
	}
	
	@Override
	public void zoneRemoved(Zone zone) {
		/*
		 * TODO dispose Apam instance
		 */
		@SuppressWarnings("unused")
		Instance room = rooms.remove(zone);
		
		System.out.println("------- zone removed");
	}
	
	@Override
	public void zoneVariableAdded(Zone zone, String variable) {
	}

	@Override
	public void zoneVariableRemoved(Zone zone, String variable) {
	}

	@Override
	public void zoneVariableModified(Zone zone, String variable, Object value, Object oldValue) {
	}

	@Override
	public void deviceAttached(Zone zone, LocatedDevice device) {
	}

	@Override
	public void deviceDetached(Zone zone, LocatedDevice device) {
	}

	@Override
	public void zoneMoved(Zone zone, Position position, Position  oldPosition) {
	}

	@Override
	public void zoneParentModified(Zone zone, Zone parent, Zone formerParent) {
	}
	
	@Override
	public void zoneResized(Zone zone) {
	}

	
}
