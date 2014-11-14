/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ContextualManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.ApformInstance;

public class CompositeImpl extends InstanceImpl implements Composite {

	private static final long serialVersionUID = 1L;

	/**
	 * The root of the composite hierarchy
	 * 
	 */
	private static final Composite rootComposite;

	/**
	 * The list of all composites in the APAM state model
	 */
	private static final Map<String, Composite> composites = new ConcurrentHashMap<String, Composite>();

	public static Composite getComposite(String name) {
		return CompositeImpl.composites.get(name);
	}

	public static Collection<Composite> getComposites() {
		return Collections.unmodifiableCollection(CompositeImpl.composites.values());
	}

	/*
	 * the contained instances
	 */
	private Instance mainInst;
	private Set<Instance> hasInstance = Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());

	/*
	 * The father-son relationship of the composite hierarchy
	 * 
	 * This is a subset of the instance hierarchy restricted only to composites.
	 */
	private Set<Composite> sons = Collections.newSetFromMap(new ConcurrentHashMap<Composite, Boolean>());
	private Composite father; // null if root composite
	// private Composite appliComposite; // root of father relationship

	/*
	 * the dependencies between composites
	 */
	private Set<Composite> depend = Collections.newSetFromMap(new ConcurrentHashMap<Composite, Boolean>());
	private Set<Composite> invDepend = Collections.newSetFromMap(new ConcurrentHashMap<Composite, Boolean>());

	// ==== initialisations ==
	/**
	 * NOTE We can not directly initialize the field because the constructor may throw an exception, so we need
	 * to make an static block to be able to catch the exception.
	 * 
	 * The root composite bootstraps the system, so normally we SHOULD always be able to create it; if there
	 * is an exception, that means there is some bug and we cannot normally continue, so we throw a class
	 * initialization exception.
	 */
	static {
		Composite bootstrap = null;
		try {
			bootstrap = new CompositeImpl(CompositeTypeImpl.getRootCompositeType(), "rootComposite");
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		} finally {
			rootComposite = bootstrap;
		}
	}

	/**
	 * The root instance and the root composite are the same object.
	 * getComposite() returns this.
	 * 
	 * @return
	 */
	public static Composite getRootAllComposites() {
		return CompositeImpl.rootComposite;
	}

	public static Collection<Composite> getRootComposites() {
		return Collections.unmodifiableSet(CompositeImpl.rootComposite.getSons());
	}

	// ===

	public static Composite getRootInstance() {
		return CompositeImpl.rootComposite;
	}

	/**
	 * Builds a new Apam composite to represent the specified platform instance
	 * in the Apam model.
	 */
	protected CompositeImpl(Composite composite, ApformInstance apformInst) throws InvalidConfiguration {

		// First create the composite, as a normal instance
		super(composite, apformInst);

		/*
		 * Reference the enclosing composite hierarchy
		 */
		father = ((CompositeImpl) composite).isSystemRoot() ? null : composite;
		// appliComposite = father == null ? this : father.getAppliComposite();
	}

	/**
	 * This is an special constructor only used for the root instance of the
	 * system, and the dummy instance during construction
	 */
	CompositeImpl(CompositeType compoType, String name) throws InvalidConfiguration {

		super(compoType, name);

		this.mainInst = null;
		/*
		 * NOTE the root instance is automatically registered in Apam in a
		 * specific way that allows bootstraping the system
		 */
		this.father = null;
		// this.appliComposite = null;
	}

	public void addContainInst(Instance instance) {
		assert (instance != null);
		hasInstance.add(instance);
	}

	// Composite relation management ===============
	@Override
	public void addDepend(Composite destination) {
		assert destination != null && !depend.contains(destination);
		depend.add(destination);
		((CompositeImpl) destination).addInvDepend(this);
	}

	/**
	 * Ajoute une dependance inverse. Hidden, Internal;
	 * 
	 * @param origin
	 * @return
	 */

	private void addInvDepend(Composite origin) {
		assert origin != null && !invDepend.contains(origin);
		invDepend.add(origin);
		return;
	}

	// Father-son relationship management. Hidden, Internal;
	public void addSon(Composite destination) {
		assert destination != null && !sons.contains(destination);
		sons.add(destination);
	}

	@Override
	public boolean containsInst(Instance instance) {
		assert (instance != null);
		return hasInstance.contains(instance);
	}

	@Override
	public boolean dependsOn(Composite dest) {
		if (dest == null) {
			return false;
		}
		return (depend.contains(dest));
	}

	@Override
	public Composite getAppliComposite() {
		return (father == null) ? this : father.getAppliComposite();
	}

	@Override
	public final CompositeType getCompType() {
		return (CompositeType) getImpl();
	}

	@Override
	public Set<Instance> getContainInsts() {
		return Collections.unmodifiableSet(hasInstance);
	}

	@Override
	public Set<Composite> getDepend() {
		return Collections.unmodifiableSet(depend);
	}

	@Override
	public Composite getFather() {
		return father;
	}

	/**
	 * returns the reverse dependencies !
	 * 
	 * @return
	 */
	@Override
	public Set<Composite> getInvDepend() {
		return Collections.unmodifiableSet(invDepend);
	}

	@Override
	public final Implementation getMainImpl() {
		return getCompType().getMainImpl();
	}

	@Override
	public Instance getMainInst() {
		return mainInst;
	}

	@Override
	public ManagerModel getModel(ContextualManager manager) {
		return getCompType().getModel(manager);
	}

	@Override
	public Set<ManagerModel> getModels() {
		return getCompType().getModels();
	}

	/**
	 * Overrides the instance method. A composite has no object, returns the
	 * main instance object
	 */
	@Override
	public Object getServiceObject() {
		return mainInst != null ? mainInst.getApformInst().getServiceObject() : null;
	}

	@Override
	public Set<Composite> getSons() {
		return Collections.unmodifiableSet(sons);
	}

	/**
	 * Whether this is the system root composite
	 * 
	 */
	public boolean isSystemRoot() {
		return this == rootComposite;
	}

	@Override
	public void register(Map<String, String> initialProperties) throws InvalidConfiguration {

		/*
		 * Initialize the contained instances. The main instance will be eagerly created and the
		 * other components will be lazily instantiated.
		 * 
		 * TODO should there be a way to specify the properties of the main instance?
		 */
		if ((ImplementationImpl) getMainImpl() != null) {
			mainInst = ((ImplementationImpl) getMainImpl()).instantiate(this, null);
		}

		/*
		 * Opposite reference from the enclosing composite. Notice that root application are sons
		 * of the all root composite, but their father reference is null.
		 */
		((CompositeImpl) getComposite()).addSon(this);

		/*
		 * add to list of composites
		 */
		CompositeImpl.composites.put(getName(), this);

		/*
		 * Complete normal registration
		 */
		super.register(initialProperties);

		/*
		 * After composite is registered, register main instance that was eagerly created
		 */
		if (mainInst != null) {
			((InstanceImpl) mainInst).register(null);
		}
	}

	/**
	 * A composite cannot be isolated. Therefore remove is prohibited if the
	 * destination will be isolated.
	 */
	public boolean removeDepend(Composite destination) {
		assert destination != null && depend.contains(destination);
		depend.remove(destination);
		((CompositeImpl) destination).removeInvDepend(this);
		return true;
	}

	public void removeInst(Instance instance) {
		assert (instance != null);
		hasInstance.remove(instance);
	}

	/**
	 * Retire une dependance inverse. Hidden, Internal;
	 * 
	 * @param origin
	 * @return
	 */
	private boolean removeInvDepend(Composite origin) {
		assert origin != null && invDepend.contains(origin);
		invDepend.remove(origin);
		return true;
	}

	/**
	 * A son can be removed only when deleted. Warning : not checked.
	 */
	public boolean removeSon(Composite destination) {
		assert destination != null && sons.contains(destination);
		return sons.remove(destination);
	}

	@Override
	public void setOwner(Composite owner) {

		CompositeImpl previousOwner = (CompositeImpl) getComposite();
		if (owner == previousOwner) {
			return;
		}
		
		super.setOwner(owner);
		previousOwner.removeSon(this);

		this.father = owner;

		((CompositeImpl) owner).addSon(this);

	}

	@Override
	public String toString() {
		return "COMPOSITE " + getName();
	}

	@Override
	public void unregister() {

		/*
		 * Unbind from the enclosing composite
		 */
		((CompositeImpl) getComposite()).removeSon(this);

		father = null;
		// appliComposite = null;

		/*
		 * Remove depend relationships.
		 * 
		 * NOTE We have to copy the list because we update it while iterating it
		 */
		for (Composite dependsOn : new HashSet<Composite>(depend)) {
			removeDepend(dependsOn);
		}

		for (Composite dependant : new HashSet<Composite>(invDepend)) {
			((CompositeImpl) dependant).removeDepend(this);
		}

		/*
		 * TODO Should we destroy the whole hierarchy rooted at this composite?
		 * or should we try to reuse all created instances in the pool of unused
		 * instances? what to do with wires that enter or leave this hierarchy?
		 * what to do of shared instances with other hierarchies? can we reuse
		 * the main instance?
		 */

		/*
		 * Remove from list of composites
		 */
		CompositeImpl.composites.remove(getName());

		/*
		 * Notify managers and remove the instance from the broker
		 * 
		 * TODO perhaps we should notify managers before actually destroying the
		 * composite hierarchy, this will need a refactoring of the superclass
		 * to allow a more fine control of unregistration.
		 */
		super.unregister();
	}

}
