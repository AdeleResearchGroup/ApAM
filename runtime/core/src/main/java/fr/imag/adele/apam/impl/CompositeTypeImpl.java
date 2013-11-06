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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;

public class CompositeTypeImpl extends ImplementationImpl implements
	CompositeType {

    private static Logger logger = LoggerFactory
	    .getLogger(CompositeTypeImpl.class);
    private static final long serialVersionUID = 1L;

    /**
     * The root of the composite type hierarchy
     */
    private static CompositeType rootCompoType;

    /**
     * NOTE We can not directly initialize the field because the constructor may
     * throw an exception, so we need to make a static block to be able to catch
     * the exception. The root composite bootstraps the system, so normally we
     * SHOULD always be able to create it; if there is an exception, that means
     * there is some bug and we cannot continue so we throw a class
     * initialization exception.
     */
    static {
	CompositeType bootstrap = null;
	try {
	    bootstrap = new CompositeTypeImpl();
	} catch (Exception e) {
	    throw new ExceptionInInitializerError(e);
	} finally {
	    rootCompoType = bootstrap;
	}
    }

    private static Map<String, CompositeType> compositeTypes = new ConcurrentHashMap<String, CompositeType>();

    public static CompositeType getCompositeType(String name) {
	return CompositeTypeImpl.compositeTypes.get(name);
    }

    public static Collection<CompositeType> getCompositeTypes() {
	return Collections
		.unmodifiableCollection(CompositeTypeImpl.compositeTypes
			.values());
    }

    public static CompositeType getRootCompositeType() {
	return CompositeTypeImpl.rootCompoType;
    }

    /**
     * The list of all composites in the APAM state model
     */
    public static Collection<CompositeType> getRootCompositeTypes() {
	return CompositeTypeImpl.rootCompoType.getEmbedded();
    }

    /*
     * The models associated to this composite type to specify the different
     * strategies to handle the instances of this type.
     */
    private Set<ManagerModel> models = new HashSet<ManagerModel>();

    /*
     * The contained implementations deployed (really or logically) by this
     * composite type.
     * 
     * WARNING An implementation may be deployed by more than one composite type
     */
    private Set<Implementation> contains = Collections
	    .newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());
    private Implementation mainImpl = null;

    /*
     * The hierarchy of composite types.
     * 
     * This is a subset of the contains hierarchy restricted only to composites.
     */
    private Set<CompositeType> embedded = Collections
	    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());
    private Set<CompositeType> invEmbedded = Collections
	    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

    /*
     * all the dependencies between composite types
     */
    private Set<CompositeType> imports = Collections
	    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());
    private Set<CompositeType> invImports = Collections
	    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

    private static Map<String, RelationDefinition> ctxtDependencies = new HashMap<String, RelationDefinition>();

    /**
     * This is an special constructor only used for the root type of the system
     */
    private CompositeTypeImpl() throws InvalidConfiguration {
	super(CST.ROOT_COMPOSITE_TYPE);

	/*
	 * Look for platform models in directory "load"
	 */
	this.models = new HashSet<ManagerModel>();
	File modelDirectory = new File("conf");

	if (!modelDirectory.exists()) {
	    return;
	}

	if (!modelDirectory.isDirectory()) {
	    return;
	}

	for (File modelFile : modelDirectory.listFiles()) {
	    try {
		String modelFileName = modelFile.getName();

		if (!modelFileName.endsWith(".cfg")) {
		    continue;
		}

		if (!modelFileName.startsWith(CST.ROOT_COMPOSITE_TYPE)) {
		    continue;
		}

		String managerName = modelFileName.substring(
			CST.ROOT_COMPOSITE_TYPE.length() + 1,
			modelFileName.lastIndexOf(".cfg"));
		URL modelURL = modelFile.toURI().toURL();
		models.add(new ManagerModel(managerName, modelURL));

	    } catch (MalformedURLException e) {
	    }
	}
    }

    /**
     * Builds a new Apam composite type to represent the specified
     * implementation in the Apam model.
     */
    protected CompositeTypeImpl(CompositeType composite,
	    ApformCompositeType apfCompo) throws InvalidConfiguration {

	super(composite, apfCompo);

	/*
	 * Reference the enclosing composite hierarchy
	 */
	addInvEmbedded(composite);

	/*
	 * Get declared models
	 */
	this.models.addAll(apfCompo.getModels());
    }

    public void addEmbedded(CompositeType destination) {
	embedded.add(destination);
    }

    public void addImpl(Implementation impl) {
	contains.add(impl);
    }

    @Override
    public void addImport(CompositeType destination) {
	imports.add(destination);
	((CompositeTypeImpl) destination).addInvImport(this);
    }

    public void addInvEmbedded(CompositeType origin) {
	invEmbedded.add(origin);
    }

    public void addInvImport(CompositeType dependent) {
	invImports.add(dependent);
    }

    @Override
    public boolean containsImpl(Implementation impl) {
	return contains.contains(impl);
    }

    /**
     * Deploy (logically) a new implementation into this composite type.
     * 
     * TODO Should this method be in the public API or it is restricted to the
     * resolver and other managers?
     */
    public void deploy(Implementation impl) {

	/*
	 * Remove implementation from the unused container if this is the first
	 * deployment
	 */
	if (!impl.isUsed()) {
	    ((ImplementationImpl) impl).removeInComposites(CompositeTypeImpl
		    .getRootCompositeType());
	    ((CompositeTypeImpl) CompositeTypeImpl.getRootCompositeType())
		    .removeImpl(impl);

	    /*
	     * If the implementation is composite, it is also embedded in the
	     * unused container
	     */
	    if (impl instanceof CompositeType) {
		((CompositeTypeImpl) impl).removeInvEmbedded(CompositeTypeImpl
			.getRootCompositeType());
		((CompositeTypeImpl) CompositeTypeImpl.getRootCompositeType())
			.removeEmbedded((CompositeTypeImpl) impl);
	    }
	}

	/*
	 * Add the implementation to this composite
	 */
	((ImplementationImpl) impl).addInComposites(this);
	this.addImpl(impl);

	/*
	 * Embed in this hierarchy if the implementation is composite
	 */
	if (impl instanceof CompositeType) {
	    ((CompositeTypeImpl) impl).addInvEmbedded(this);
	    this.addEmbedded((CompositeTypeImpl) impl);
	}
    }

    @Override
    public CompositeDeclaration getCompoDeclaration() {
	return (CompositeDeclaration) getDeclaration();
    }

    /**
     * The ctxt relation must have the same Id, must indicate a source that is
     * an ancestor of parameter source, ans source must be of the right kind.
     * 
     * It is supposed that a ctxtdep has an Id and a source.
     * 
     * @param source
     * @param id
     * @return
     */
    @Override
    public RelationDefinition getCtxtRelation(Component source, String id) {
	RelationDefinition dep = ctxtDependencies.get(id);
	if (dep == null) {
	    return null;
	}
	Component group = source;
	while (group != null) {
	    if (group.getName().equals(dep.getCtxtSourceName())
	    // getLinkSource().getName())
		    && source.getKind() == dep.getSourceKind()) {
		return dep;
	    }
	}
	return null;
    }

    /**
     * The ctxt relation must have the same Id, must indicate a source that is
     * an ancestor of parameter source, ans source must be of the right kind.
     * 
     * It is supposed that a ctxtdep has an Id and a source.
     * 
     * @param source
     * @param id
     * @return
     */
    @Override
    public Set<RelationDefinition> getCtxtRelations(Component source) {
	Set<RelationDefinition> deps = new HashSet<RelationDefinition>();

	Component group;
	for (RelationDefinition dep : ctxtDependencies.values()) {
	    if (source.getKind() == dep.getSourceKind()) {
		continue;
	    }
	    group = source;
	    while (group != null) {
		if (group.getName().equals(dep.getCtxtSourceName())) {
		    deps.add(dep);
		}
	    }
	}
	return deps;
    }

    @Override
    public Set<CompositeType> getEmbedded() {
	return Collections.unmodifiableSet(embedded);
    }

    @Override
    public Set<Implementation> getImpls() {
	return Collections.unmodifiableSet(contains);
    }

    @Override
    public Set<CompositeType> getImport() {
	return Collections.unmodifiableSet(imports);
    }

    @Override
    public Set<CompositeType> getInvEmbedded() {
	return Collections.unmodifiableSet(invEmbedded);
    }

    @Override
    public Implementation getMainImpl() {
	return mainImpl;
    }

    @Override
    public ManagerModel getModel(String managerName) {
	for (ManagerModel model : models) {
	    if (model.getManagerName().equals(managerName)) {
		return model;
	    }
	}
	return null;
    }

    @Override
    public Set<ManagerModel> getModels() {
	return Collections.unmodifiableSet(models);
    }

    @Override
    public boolean isFriend(CompositeType destination) {
	return imports.contains(destination);
    }

    /**
     * Whether this is the system root composite type
     */
    public boolean isSystemRoot() {
	return this == rootCompoType;
    }

    @Override
    public void register(Map<String, String> initialProperties)
	    throws InvalidConfiguration {

	/*
	 * Opposite references from the enclosing composite types
	 */
	for (CompositeType inComposite : invEmbedded) {
	    ((CompositeTypeImpl) inComposite).addEmbedded(this);
	}

	/*
	 * Notify managers of their models.
	 * 
	 * WARNING Notice that the managers are not notified at the end of the
	 * registration, but before resolving the main implementation. This
	 * allow the resolution of the main implementation in the context of the
	 * composite, specially if the main implementation is deployed in the
	 * private repository of the composite..
	 * 
	 * Managers must be aware that the composite type is not completely
	 * registered, so they must be cautious when manipulating the state and
	 * navigating the hierarchy.
	 */
	for (ManagerModel managerModel : models) {
	    RelationManager manager = ApamManagers.getManager(managerModel
		    .getManagerName());
	    if (manager != null) {
		manager.newComposite(managerModel, this);
	    }
	}

	/*
	 * Resolve main implementation. Does nothing if it is an abstract
	 * composite.
	 */
	resolveMainImplem();

	/*
	 * add to list of composite types
	 */
	CompositeTypeImpl.compositeTypes.put(getName(), this);

	/*
	 * Compute Dependencies from relation declarations. TODO I am not sure
	 * this is needed
	 */
	for (RelationDeclaration relation : ((CompositeDeclaration) this
		.getDeclaration()).getContextualDependencies()) {
	    // Build the corresponding relation and attach it to the component
	    ctxtDependencies.put(relation.getIdentifier(),
		    new RelationDefinitionImpl(relation));
	}

	/*
	 * Complete normal registration
	 */
	super.register(initialProperties);
    }

    @Override
    protected Composite reify(Composite composite,
	    ApformInstance platformInstance) throws InvalidConfiguration {
	return new CompositeImpl(composite, platformInstance);
    }

    public boolean removeEmbedded(CompositeType destination) {
	return embedded.remove(destination);
    }

    public void removeImpl(Implementation impl) {
	contains.remove(impl);
    }

    public boolean removeImport(CompositeType destination) {
	((CompositeTypeImpl) destination).removeInvImport(this);
	return imports.remove(destination);
    }

    public boolean removeInvEmbedded(CompositeType origin) {
	return invEmbedded.remove(origin);
    }

    public boolean removeInvImport(CompositeType dependent) {
	return invImports.remove(dependent);
    }

    /**
     * Resolve main implementation.
     * 
     * First we try to find an implementation with the name of the main
     * component, if we fail to find one we assume the name corresponds to a
     * specification which is resolved. Notice that resolution of the main
     * component is done in the context of this composite type, so it will be
     * deployed in this context if necessary.
     * 
     * WARNING this is done after the composite type is added to the hierarchy
     * but before it is completely registered as a normal implementation. We do
     * not call super.register until the main implementation is resolved.
     * 
     */
    private void resolveMainImplem() throws InvalidConfiguration {
	/*
	 * Abstract Composite
	 */
	if (getCompoDeclaration().getMainComponent() == null) {
	    if (!getProvidedResources().isEmpty()) {
		unregister();
		throw new InvalidConfiguration("invalid composite type "
			+ getName()
			+ ". No Main implementation but provides resources "
			+ getProvidedResources());
	    }
	    return;
	}

	String mainComponent = getCompoDeclaration().getMainComponent()
		.getName();
	/*
	 * This is a false composite / instance, not registered anywhere. just
	 * to provide an instance to the find and resolve.
	 */
	Composite dummyComposite = new CompositeImpl(this, "dummyComposite");

	mainImpl = CST.apamResolver.findImplByName(dummyComposite,
		mainComponent);
	if (mainImpl != null) {
	    logger.debug("The main component of " + this.getName()
		    + " is an Implementation : " + mainImpl.getName());
	} else {
	    Specification spec = CST.apamResolver.findSpecByName(
		    dummyComposite, mainComponent);
	    if (spec != null) {
		logger.debug("The main component of " + this.getName()
			+ " is a Specification : " + spec.getName());
		/*
		 * It is a specification to resolve as the main implem. Do not
		 * select another composite
		 */
		Set<String> constraints = new HashSet<String>();
		constraints.add("(!(" + CST.APAM_COMPOSITETYPE + "="
			+ CST.V_TRUE + "))");
		mainImpl = CST.apamResolver.resolveSpecByName(dummyComposite,
			mainComponent, constraints, null);
	    }
	}

	/*
	 * If we cannot resolve the main implementation, we abort the
	 * registration in APAM, taking care of undoing the partial processing
	 * already performed.
	 */
	if (mainImpl == null) {
	    // logger.debug("The main component is " + mComponent);
	    unregister();
	    throw new InvalidConfiguration("Cannot find main implementation "
		    + mainComponent);
	}

	// assert mainImpl != null;

	if (!mainImpl.getInCompositeType().contains(this)) {
	    deploy(mainImpl);
	}

	/*
	 * Check that the main implementation conforms to the declaration of the
	 * composite
	 */
	boolean providesResources = mainImpl.getProvidedResources()
		.containsAll(getProvidedResources());

	/*
	 * If the main implementation is not conforming, we abort the
	 * registration in APAM, taking care of undoing the partial processing
	 * already performed.
	 */
	if (!providesResources) {
	    unregister();
	    throw new InvalidConfiguration("invalid main implementation "
		    + mainImpl.getName() + " for composite type " + getName()
		    + "Main implementation Provided resources "
		    + mainImpl.getDeclaration().getProvidedResources()
		    + "do no provide all the expected resources : "
		    + getSpec().getDeclaration().getProvidedResources());
	}

	// TODO Other control, other than provided resources ?
    }

    @Override
    public String toString() {
	return "COMPOSITETYPE " + getName();
    }

    @Override
    public void unregister() {
	/*
	 * Remove the instances and notify managers
	 */
	super.unregister();

	/*
	 * Remove import relationships. NOTE We have to copy the list because we
	 * update it while iterating it
	 */
	for (CompositeType imported : new HashSet<CompositeType>(imports)) {
	    removeImport(imported);
	}

	for (CompositeType importedBy : new HashSet<CompositeType>(invImports)) {
	    ((CompositeTypeImpl) importedBy).removeImport(this);
	}

	/*
	 * Remove opposite references from embedding composite types
	 * 
	 * TODO May be this should be done at the same type that the contains
	 * hierarchy, but this will require a refactor of the superclass to have
	 * a fine control on the order of the steps.
	 */
	for (CompositeType inComposite : invEmbedded) {
	    ((CompositeTypeImpl) inComposite).removeEmbedded(this);
	}

	invEmbedded.clear();

	/*
	 * Remove from list of composite types
	 */
	CompositeTypeImpl.compositeTypes.remove(getName());
    }

}
