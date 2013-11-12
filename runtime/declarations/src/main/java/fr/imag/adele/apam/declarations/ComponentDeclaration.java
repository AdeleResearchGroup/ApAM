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
package fr.imag.adele.apam.declarations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the common description of a component at all levels of
 * abstraction (specification, implementation, composite or instance)
 * 
 * @author vega
 * 
 */
public abstract class ComponentDeclaration {

    /**
     * The identifier of the service provider
     */
    private final String name;

    /**
     * The reference to this component declaration
     */
    private final ComponentReference<?> reference;

    /**
     * The resources provided by this service
     */
    protected final Set<ResourceReference> providedResources;

    /**
     * The resources required by this service
     */
    private final Set<RelationDeclaration> relations;

    /**
     * The predefined links of this service
     */
    private final Set<LinkDeclaration> predefinedLinks;
    /**
     * The properties describing this service provider
     */
    private final Map<String, String> properties;
    private final List<PropertyDefinition> definitions;

    /**
     * Whether instances of this component must be accessed exclusively by a
     * single client at the time
     */
    private boolean isExclusive;
    private boolean isDefinedExclusive;

    /**
     * Whether we can create instances of this component
     */
    private boolean isInstantiable;
    private boolean isDefinedInstantiable;

    /**
     * Whether there can be a single instance of this component in the execution
     * platform
     */
    private boolean isSingleton;
    private boolean isDefinedSingleton;

    /**
     * Whether the instance of this component can be shared by different clients
     */
    public boolean isShared;
    public boolean isDefinedShared;

    protected ComponentDeclaration(String name) {

	assert name != null;

	this.name = name;

	this.isInstantiable = true;
	this.isExclusive = false;
	this.isSingleton = false;
	this.isShared = true;
	this.isDefinedInstantiable = false;
	this.isDefinedExclusive = false;
	this.isDefinedSingleton = false;
	this.isDefinedShared = false;

	reference = generateReference();
	properties = new HashMap<String, String>();
	providedResources = new HashSet<ResourceReference>();
	relations = new HashSet<RelationDeclaration>();
	predefinedLinks = new HashSet<LinkDeclaration>();
	definitions = new ArrayList<PropertyDefinition>();
    }

    /**
     * Generates a unique resource identifier to reference this declaration
     */
    protected abstract ComponentReference<?> generateReference();

    /**
     * Get the declared dependencies of this component
     */
    public Set<RelationDeclaration> getDependencies() {
	return relations;
    }

    /**
     * Get the reference to the parent group of this declaration
     */
    public abstract ComponentReference<?> getGroupReference();

    /**
     * Get a relation declaration by name
     */
    public RelationDeclaration getLocalRelation(String id) {
	for (RelationDeclaration relation : relations) {
	    if (relation.getIdentifier().equals(id)) {
		return relation;
	    }
	}

	return null;
    }

    /**
     * Get the name of the provider
     */
    public String getName() {
	return name;
    }

    /**
     * Get the declared predefined links
     */
    public Set<LinkDeclaration> getPredefinedLinks() {
	return predefinedLinks;
    }

    /**
     * Get the properties describing this provider
     */
    public Map<String, String> getProperties() {
	return properties;
    }

    /**
     * Get the value of a property
     */
    public String getProperty(String property) {
	return properties.get(property);
    }

    /**
     * Get the named property definition, if declared
     */
    public PropertyDefinition getPropertyDefinition(
	    PropertyDefinition.Reference property) {

	if (!this.getReference().equals(property.getDeclaringComponent())) {
	    return null;
	}

	return getPropertyDefinition(property.getIdentifier());

    }

    /**
     * Get the named property definition, if declared
     */
    public PropertyDefinition getPropertyDefinition(String propertyName) {
	for (PropertyDefinition definition : definitions) {
	    if (definition.getName().equalsIgnoreCase(propertyName)) {
		return definition;
	    }
	}
	return null;
    }

    /**
     * Get the property definitions defined by this component
     */
    public List<PropertyDefinition> getPropertyDefinitions() {
	return definitions;
    }

    /**
     * Get the provided resources
     */
    public Set<ResourceReference> getProvidedResources() {
	return providedResources;
    }

    /**
     * Get the provided resources of a given kind, for example Services or
     * Messages.
     * 
     * We use subclasses of ResourceReference as tags to identify kinds of
     * resources. To add a new kind of resource a new subclass must be added.
     * 
     * Notice that we return a set of resource references but typed to
     * particular subtype of references, the unchecked downcast is then safe at
     * runtime.
     */
    public <T extends ResourceReference> Set<T> getProvidedResources(
	    Class<T> kind) {
	Set<T> resources = new HashSet<T>();
	for (ResourceReference resourceReference : providedResources) {
	    if (kind.isInstance(resourceReference)) {
		resources.add(kind.cast(resourceReference));
	    }
	}
	return resources;
    }

    /**
     * Get the reference to this declaration
     */
    public ComponentReference<?> getReference() {
	return reference;
    }

    /**
     * Get a relation declaration by reference
     */
    public RelationDeclaration getRelation(
	    RelationDeclaration.Reference relation) {
	if (!this.getReference().equals(relation.getDeclaringComponent())) {
	    return null;
	}

	return getLocalRelation(relation.getIdentifier());
    }

    public boolean isDefined(String propertyName) {
	return getPropertyDefinition(propertyName) != null;
    }

    public boolean isDefinedExclusive() {
	return isDefinedExclusive;
    }

    public boolean isDefinedInstantiable() {
	return isDefinedInstantiable;
    }

    public boolean isDefinedShared() {
	return isDefinedShared;
    }

    public boolean isDefinedSingleton() {
	return isDefinedSingleton;
    }

    /**
     * Whether the component is exclusive
     */
    public boolean isExclusive() {
	return isExclusive;
    }

    /**
     * Whether the component is instantiable
     */
    public boolean isInstantiable() {
	return isInstantiable;
    }

    /**
     * Check if this component requires the specified resource
     */
    public boolean isRequired(ResourceReference resource) {
	for (RelationDeclaration relation : relations) {
	    if (relation.getTarget().equals(resource)) {
		return true;
	    }
	}

	return false;
    }

    /**
     * Whether the component is shared
     */
    public boolean isShared() {
	return isShared;
    }

    /**
     * Whether the component is singleton
     */
    public boolean isSingleton() {
	return isSingleton;
    }

    /**
     * Displays the declaration on screen, indented. Same as toString, plus the
     * indentation.
     * 
     * @param indent
     *            : a number of white characters as indentation.
     */
    public String printDeclaration(String indent) {
	String nl = "\n" + indent;
	StringBuffer ret = new StringBuffer();
	ret.append(indent + " Declaration of " + name);
	if (providedResources.size() != 0) {
	    ret.append(nl + "   Provided resources: ");
	    for (ResourceReference resRef : providedResources) {
		ret.append(nl + "      " + resRef);
	    }
	}
	if (relations.size() != 0) {
	    ret.append(nl + "   Dependencies: \n");
	    for (RelationDeclaration resRef : relations) {
		ret.append(resRef.printRelationDeclaration(indent + "   ")
			+ "\n");
	    }
	}
	// if (properties.size() != 0) {
	// ret.append(nl + "   Properties: ");
	// for (Object resRef : properties.keySet()) {
	// ret.append(nl + "      " + (String) resRef + " = " +
	// properties.get(resRef));
	// }
	// }
	if (definitions.size() != 0) {
	    ret.append(nl + "   Attribute definitions ");
	    for (PropertyDefinition resRef : definitions) {
		ret.append(nl + "      " + resRef);
	    }
	}

	return ret.toString();
    }

    /**
     * Check if the specified resource is provided by this component
     * 
     */
    public boolean resolves(RelationDeclaration relation) {
	return providedResources.contains(relation.getTarget());
    }

    // Warning : should be called ONLY by CoreMetadataParser
    public void setDefinedExclusive(boolean isExclusive) {
	this.isDefinedExclusive = isExclusive;
    }

    // Warning : should be called ONLY by CoreMetadataParser
    public void setDefinedInstantiable(boolean isInstantiable) {
	this.isDefinedInstantiable = isInstantiable;
    }

    // Warning : should be called ONLY by CoreMetadataParser
    public void setDefinedShared(boolean isShared) {
	this.isDefinedShared = isShared;
    }

    // Warning : should be called ONLY by CoreMetadataParser
    public void setDefinedSingleton(boolean isSingleton) {
	this.isDefinedSingleton = isSingleton;
    }

    public void setExclusive(boolean isExclusive) {
	this.isExclusive = isExclusive;
	this.isDefinedExclusive = true;
    }

    public void setInstantiable(boolean isInstantiable) {
	this.isInstantiable = isInstantiable;
	this.isDefinedInstantiable = true;
    }

    public void setShared(boolean isShared) {
	this.isShared = isShared;
	this.isDefinedShared = true;
    }

    public void setSingleton(boolean isSingleton) {
	this.isSingleton = isSingleton;
	this.isDefinedSingleton = true;
    }

    @Override
    public String toString() {
	return printDeclaration("");
    }

}
