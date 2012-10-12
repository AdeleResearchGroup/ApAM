package fr.imag.adele.apam.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.Util;

public class ImplementationImpl extends ComponentImpl implements Implementation {

	private static Logger 				logger 				= LoggerFactory.getLogger(ImplementationImpl.class);
	private static final long   		serialVersionUID	= 1L;

	// composite in which it is contained
	private  Set<CompositeType>  		inComposites	= Collections.newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

	//The specification this implementation implements
	protected Specification      		mySpec;

	// the instances
	private Set<Instance>      			instances		= Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());

	// all relationship use and their reverse
	private Set<Implementation> 		uses			= Collections.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());
	private Set<Implementation> 		invUses			= Collections.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());


	/**
	 * This class represents the Apam root implementation. 
	 * 
	 * This is an APAM concept without mapping at the execution platform level, we build an special
	 * apform object to represent it.
	 * 
	 */
	private static class SystemRootImplementation implements ApformImplementation {

		private final CompositeDeclaration declaration;

		public SystemRootImplementation(String name) {
			this.declaration =  new CompositeDeclaration(name, 
										(SpecificationReference)null, 
										new ImplementationReference<ImplementationDeclaration>("Root Main Implem"));
		}

		@Override
		public ImplementationDeclaration getDeclaration() {
			return declaration;
		}

		@Override
		public ApformSpecification getSpecification() {
			return null;
		}

		@Override
		public ApformInstance createInstance(Map<String, String> initialproperties) {
			throw new UnsupportedOperationException("method not available in root type");
		}

		@Override
		public void setProperty(String attr,String value) {
			throw new UnsupportedOperationException("method not available in root type");
		}

		@Override
		public Bundle getBundle() {
			// No bundle at all!
			return null;
		}

	}

	/**
	 * This is an special constructor only used for the root type of the system 
	 */
	protected ImplementationImpl(String name) throws InvalidConfiguration {
		super(new SystemRootImplementation(name));
		mySpec = CST.componentBroker.createSpec(name+"_spec",new HashSet<ResourceReference>(),null);
	}


	/**
	 * Builds a new Apam implementation to represent the specified platform implementation in the Apam model.
	 */
	protected ImplementationImpl(CompositeType composite, ApformImplementation apfImpl) throws InvalidConfiguration {
		super (apfImpl) ;

		ImplementationDeclaration declaration = apfImpl.getDeclaration();

		/*
		 * Reference the declared provided specification
		 */
		if (declaration.getSpecification() != null) {
			
			String specificationName	= declaration.getSpecification().getName();
			Specification specification = CST.componentBroker.getSpec(specificationName);
			
			if (specification == null) {
				throw new InvalidConfiguration("Specification not installed " + specificationName + " for implementation " + declaration.getName());
			}
			
			assert specification != null;
			mySpec = specification;

		}

		/*
		 * If the implementation does not provides explicitly any specification, we build a dummy 
		 * specification to allow the resolution algorithm to access the provided resources of this
		 * implementation
		 */
		if (mySpec == null) {
			mySpec = CST.componentBroker.createSpec(declaration.getName() + "_spec",
					declaration.getProvidedResources(),
					(Map<String,String>)null);
		} 

		/*
		 * Reference the enclosing composite type
		 */
		addInComposites(composite);

	}

	@Override
	public void register(Map<String, String> initialProperties) throws InvalidConfiguration {

		/*
		 * Opposite references from specification and enclosing composite type
		 */
		((SpecificationImpl)mySpec).addImpl(this);

		for (CompositeType inComposite : inComposites) {
			((CompositeTypeImpl)inComposite).addImpl(this);
		}

		/*
		 * Terminates the initialization, and computes properties
		 */
		initializeProperties(initialProperties);

		/*
		 * Add to broker
		 */
		((ComponentBrokerImpl)CST.componentBroker).add(this);

		/*
		 * Notify managers
		 */
		ApamManagers.notifyAddedInApam(this);

	}


	@Override
	public void unregister() {
		logger.debug("unregister implementation " + this);
		
		/*
		 * First Remove from broker. Therefore becomes "invisible".
		 */
		((ComponentBrokerImpl) CST.componentBroker).remove(this);

		/*
		 * remove all existing instances
		 */
		for (Instance inst : instances) {
			((ComponentBrokerImpl)CST.componentBroker).removeInst(inst);
		}

		/*
		 * Remove opposite references from specification and enclosing composite types
		 */
		((SpecificationImpl) getSpec()).removeImpl(this);
		for (CompositeType inComposite : inComposites) {
			((CompositeTypeImpl)inComposite).removeImpl(this);
		}
		/*
		 * Finally Notify managers
		 */
		ApamManagers.notifyRemovedFromApam(this);

		//Do not remove inverse links, in case threads are still here.
//		mySpec = null;
//		inComposites.clear();


	}

	@Override
	public ApformImplementation getApformImpl() {
		return (ApformImplementation) getApformComponent();
	}

	@Override
	public ImplementationDeclaration getImplDeclaration() {
		return (ImplementationDeclaration)getDeclaration();
	}

	@Override
	public Specification getSpec() {
		return mySpec;
	}

	@Override
	public Set<CompositeType> getInCompositeType() {
		return Collections.unmodifiableSet(inComposites);
	}

	public void addInComposites(CompositeType compo) {
		inComposites.add(compo);
	}

	public void removeInComposites(CompositeType compo) {
		inComposites.remove(compo);
	}

	@Override
	public boolean isUsed() {
		return ! inComposites.contains(CompositeTypeImpl.getRootCompositeType()) && inComposites.size()==1;
	}

	@Override
	public boolean isInstantiable() {
		String instantiable = (String) get(CST.INSTANTIABLE);
		return (instantiable == null) ? true : instantiable.equals(CST.V_TRUE);
	}

	/**
	 * From an implementation, create an instance. Creates both the apform and APAM instances.
	 * Can be called from the API. 
	 * 
	 * Must check if source composite can instantiate this implementation.
	 */
	@Override
	public Instance createInstance(Composite composite, Map<String, String> initialProperties) {

		if ((composite != null) && !Util.checkImplVisible(composite.getCompType(), this)) {
			logger.error("cannot instantiate " + this + ". It is not visible from composite " + composite);
			return null;
		}

		if (composite == null)     	{
			composite = CompositeImpl.getRootAllComposites();
		}

    	/*
    	 * Create and register the object in the APAM state model
    	 */
		try {			
			Instance instance = instantiate(composite);
			((InstanceImpl)instance).register(initialProperties);
			return instance;		
		} catch (InvalidConfiguration configurationError) {
			logger.error("Error instantiating implementation "+this.getName()+": exception registering instance in APAM "+configurationError.getMessage());
		}	
		return null;
	}

	/**
	 * Create a new instance from this implementation in Apam and in the underlying execution platform.
	 * 
	 * WARNING The created Apam instance is not automatically published in the Apam state, nor
	 * added to the list of instances of this implementation. This is actually done when the returned
	 * instance is registered by the caller of this method.
	 * 
	 * This method is not intended to be used as external API.
	 */
	protected Instance instantiate(Composite composite) throws InvalidConfiguration {
		
		if (! this.isInstantiable()) {
			logger.error("Implementation " + this + " is not instantiable");
			return null;
		}
		if (this.isSingleton() && !instances.isEmpty()) {
			logger.error("Implementation " + this + " is a singleton and an instance exists");
			return null;		
		}

		return reify(composite,getApformImpl().createInstance(null));
	}

	/**
	 * Reifies in Apam an instance of this implementation from the information of the underlying
	 * platform. 
	 * 
	 * This method should be overridden to implement different reification semantics for different
	 * subclasses of implementation.
	 * 
	 * WARNING The reified Apam instance is not automatically published in the Apam state, nor
	 * added to the list of instances of this implementation. This is actually done when the returned
	 * instance is registered by the caller of this method.
	 * 
	 * This method is not intended to be used as external API.
	 * 
	 */
	protected Instance reify(Composite composite, ApformInstance platformInstance) throws InvalidConfiguration {
		return new InstanceImpl(composite,platformInstance);
	}


	// WARNING : no control ! Only called by instance registration.
	public void addInst(Instance instance) {
		assert instance != null && !instances.contains(instance);
		instances.add(instance);
	}

	public void removeInst(Instance instance) {
		assert instance != null && instances.contains(instance);
		instances.remove(instance);
	}

	@Override
	public Instance getInst(String targetName) {
		if (targetName == null)
			return null;
		for (Instance inst : instances) {
			if (inst.getName().equals(targetName))
				return inst;
		}
		return null;
	}

	/**
	 * returns the first instance only.
	 */
	 @Override
	 public Instance getInst() {
		 if (instances.size() == 0)
			 return null;
		 return (Instance) instances.toArray()[0];
	 }

	 @Override
	 public Set<Instance> getInsts() {
		 return Collections.unmodifiableSet(instances);
	 }


	 // relation uses control

	 @Override
	 public Set<Implementation> getUses() {
		 return Collections.unmodifiableSet(uses);
	 }

	 @Override
	 public Set<Implementation> getInvUses() {
		 return Collections.unmodifiableSet(invUses);
	 }

	 public void addUses(Implementation dest) {
		 if (uses.contains(dest))
			 return;
		 uses.add(dest);
		 ((ImplementationImpl) dest).addInvUses(this);
		 ((SpecificationImpl) getSpec()).addRequires(dest.getSpec());
	 }

	 public void removeUses(Implementation dest) {
		 for (Instance inst : instances) {
			 for (Instance instDest : inst.getWireDests())
				 if (instDest.getImpl() == dest) {
					 return; // it exists another instance that uses that destination. Do nothing.
				 }
		 }
		 uses.remove(dest);
		 ((ImplementationImpl) dest).removeInvUses(this);
		 ((SpecificationImpl) getSpec()).removeRequires(dest.getSpec());
	 }

	 private void addInvUses(Implementation orig) {
		 invUses.add(orig);
	 }

	 private void removeInvUses(Implementation orig) {
		 invUses.remove(orig);
	 }

	 @Override
	 public Set<? extends Component> getMembers() {
		 return Collections.unmodifiableSet(instances);
	 }

	 @Override
	 public Component getGroup() {
		 return getImplDeclaration().getSpecification() != null ? mySpec : null;
	 }
}
