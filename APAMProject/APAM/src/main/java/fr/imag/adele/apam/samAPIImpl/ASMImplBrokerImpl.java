package fr.imag.adele.apam.samAPIImpl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.LocalMachine;
import fr.imag.adele.am.Machine;
import fr.imag.adele.am.eventing.AMEventingHandler;
import fr.imag.adele.am.eventing.EventingEngine;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.deployment.DeploymentUnit;
import fr.imag.adele.sam.event.EventProperty;

public class ASMImplBrokerImpl implements ASMImplBroker{

	private Logger logger = Logger.getLogger(ASMImplBrokerImpl.class);

	private Set <ASMImpl> implems = new HashSet<ASMImpl> () ;
	private SamImplEventHandler eventHandler ;

	public ASMImplBrokerImpl () {
		try {
			Machine machine = LocalMachine.localMachine;
			EventingEngine eventingEngine = machine.getEventingEngine();
			eventHandler = new SamImplEventHandler();
			eventingEngine.subscribe(eventHandler, EventProperty.TOPIC_IMPLEMENTATION);
		} catch (Exception e) {}
	}

	public void stopSubscribe (AMEventingHandler handler) {
		try {
		Machine machine = LocalMachine.localMachine;
		EventingEngine eventingEngine = machine.getEventingEngine();
		eventingEngine.unsubscribe(handler, EventProperty.TOPIC_IMPLEMENTATION) ;
		} catch (Exception e) {}
	}
	
	//Not in the interface. No control
	public void addImpl (ASMImpl impl) {
		implems.add(impl) ;
	}

	//Not in the interface. No control
	public void removeImpl (ASMImpl impl) {
		implems.remove(impl) ;
	}
	
	@Override
	public ASMImpl getImpl(String implName)
	throws ConnectionException {
		for (ASMImpl impl : implems) {
			if (implName.equals(impl.getASMName()))
					return impl ;
		}
		return null ;
	}

	@Override
	public Set<ASMImpl> getImpls()
	throws ConnectionException, ConnectionException {
		return Collections.unmodifiableSet(implems) ;
		//return new HashSet<ASMImpl> (implems) ;
	}

	@Override
	public Set<ASMImpl> getImpls(Filter goal)
	throws ConnectionException, InvalidSyntaxException {
		Set<ASMImpl> ret = new HashSet<ASMImpl> () ;
		for (ASMImpl impl : implems) {
			if (goal.match((PropertyImpl)impl.getProperties())) 
					ret.add(impl) ;
		}
		return ret ;
	}


	@Override
	public ASMImpl addImpl(Composite compo, String name, Implementation samImpl) {
		Specification samSpec = null ;
		
		try {
			//samSpec = samImpl.getSpecification();
			samSpec = (Specification)samImpl.getSpecifications().toArray()[0];
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
		ASMSpecImpl spec = new ASMSpecImpl (compo, name, samSpec, null) ;
		ASMImplImpl impl = new ASMImplImpl (compo, name, spec, samImpl, null, null) ;
		return impl ;
	}

	@Override
	public ASMImpl createImpl(Composite compo, String name, URL url, String type) {
		String implName = null ;
		Implementation samImpl;
		ASMImpl asmImpl ;
		try {
			DeploymentUnit du = ASM.SAMDUBroker.install(url, type) ;
			Set<String> implementationsNames = du.getImplementationsName();
			implName = (String)implementationsNames.toArray()[0] ;
			du.activate();
			samImpl = eventHandler.getImplementation(implName);
			//TODO comment savoir si une instance a été créée dans la foulée, et sous quel nom ?
		} catch (ConnectionException e) {
			e.printStackTrace();
			return null ;
		}
		
		asmImpl =ASM.ASMImplBroker.addImpl(compo, name, samImpl) ;
		return asmImpl ;
	}

	@Override
	public ASMImpl getImpl(Implementation samImpl) {
		for (ASMImpl implem : implems) {
			if (implem.getSamImpl() == samImpl) {
				return implem ;
			}
		}
		return null;
	}


}
