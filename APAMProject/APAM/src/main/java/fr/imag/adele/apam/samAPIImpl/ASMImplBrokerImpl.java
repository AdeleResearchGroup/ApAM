package fr.imag.adele.apam.samAPIImpl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//import org.apache.log4j.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.LocalMachine;
import fr.imag.adele.am.Machine;
//import fr.imag.adele.am.Property;
import fr.imag.adele.am.eventing.AMEventingHandler;
import fr.imag.adele.am.eventing.EventingEngine;
import fr.imag.adele.am.exception.ConnectionException;
//import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.ApamProperty;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.deployment.DeploymentUnit;
import fr.imag.adele.sam.event.EventProperty;

public class ASMImplBrokerImpl implements ASMImplBroker{

//	private Logger logger = Logger.getLogger(ASMImplBrokerImpl.class);

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
			if (impl.getASMName() == null) {
				if (impl.getSamImpl().getName().equals (implName)) return impl ;
			} else {
				if (implName.equals(impl.getASMName()))
					return impl ;
			}
		}
		return null ;
	}

	@Override
	public Set<ASMImpl> getImpls()  {
		return Collections.unmodifiableSet(implems) ;
		//return new HashSet<ASMImpl> (implems) ;
	}

	@Override
	public Set<ASMImpl> getImpls(Filter goal)
	throws InvalidSyntaxException {
		Set<ASMImpl> ret = new HashSet<ASMImpl> () ;
		for (ASMImpl impl : implems) {
			if (goal.match((ApamProperty)impl.getProperties())) 
				ret.add(impl) ;
		}
		return ret ;
	}


	@Override
	public ASMImpl addImpl(Composite compo, String implName, Implementation samImpl, String specName, Attributes properties) {
		try {
			Specification samSpec = samImpl.getSpecification() ;
			ASMImpl asmImpl = null ;
			asmImpl = getImpl(implName);
			if (asmImpl != null ) { //do not create twice
				((ASMImplImpl)asmImpl).setASMName (implName) ;
				((ASMSpecImpl)asmImpl.getSpec()).setASMName(specName) ;
				return asmImpl ; 
			}
			samSpec = samImpl.getSpecification();
			ASMSpecImpl spec = (ASMSpecImpl)ASM.ASMSpecBroker.getSpec(samSpec) ;
			if (spec == null) { //No ASM spec related to the sam spec.
				spec = (ASMSpecImpl)ASM.ASMSpecBroker.getSpec (samSpec.getInterfaceNames()) ;
				if (spec != null) { //has been created without the SAM spec. Add it now.
					spec.setSamSpec (samSpec) ;
				}
				else { // create the spec
				spec = new ASMSpecImpl (compo, specName, samSpec, properties) ;
				}
			} else {
				if (specName != null) spec.setASMName(specName) ;
			}
			ASMImplImpl impl = new ASMImplImpl (compo, implName, spec, samImpl, properties) ;
			return impl ;
		} catch (ConnectionException e) {
			e.printStackTrace();
			return null ;
		}
	}

	@Override
	public ASMImpl createImpl(Composite compo, String implName, URL url, String type, String specName, Attributes properties) {
		String implNameExpected = null ;
		Implementation samImpl  ;
		ASMImpl asmImpl = null ;
		try {
			asmImpl = getImpl(implName);
			if (asmImpl != null ) { //do not create twice
				((ASMImplImpl)asmImpl).setASMName (implName) ;
				((ASMSpecImpl)asmImpl.getSpec()).setASMName(specName) ;
				return asmImpl ; 
			}

			DeploymentUnit du = ASM.SAMDUBroker.install(url, type) ;
			Set<String> implementationsNames = du.getImplementationsName();
			implNameExpected = (String)implementationsNames.toArray()[0] ;
			du.activate();
			samImpl = eventHandler.getImplementation(implNameExpected);
			//TODO comment savoir si une instance a été créée dans la foulée, et sous quel nom ?
		} catch (ConnectionException e) {
			e.printStackTrace();
			return null ;
		}

		asmImpl = addImpl(compo, implName, samImpl, specName, properties) ;
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

	@Override
	public ASMImpl getImplSamName(String samName) throws ConnectionException {
		for (ASMImpl impl : implems) {
			if (impl.getSamImpl().getName().equals (samName)) return impl ;
		}
		return null;
	}


}
