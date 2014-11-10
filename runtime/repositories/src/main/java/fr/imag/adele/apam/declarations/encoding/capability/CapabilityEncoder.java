package fr.imag.adele.apam.declarations.encoding.capability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Requirement;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.Reporter.Severity;
import fr.imag.adele.apam.declarations.encoding.Encoder;
import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.PackageReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.declarations.repository.acr.ApamComponentRepository;
import fr.imag.adele.apam.util.Util;

/**
 * Encodes a component declaration as a capability in an OSGi Bundle Repository.
 * 
 * This is the format used by the Apam Component Repository at build time, and the OBR
 * manager at runtime.
 * 
 * @author vega
 *
 */
public class CapabilityEncoder implements Encoder<Capability> {

	private Reporter				reporter;
	
	private ComponentDeclaration 	component;
	private Builder					result;
	
	
	@Override
	public Capability encode(ComponentDeclaration component, Reporter reporter) {
		
		this.reporter	= reporter;
		this.component	= component;
		
		info("encoding ACR capability for component : " + component.getName() + " of type " + component.getKind());

		this.result = builder(CST.CAPABILITY_COMPONENT);

		/*
		 * Name and kind
		 */
		property(CST.NAME,component.getName());
		property(CST.COMPONENT_TYPE, kind(component.getKind()));

		/*
		 * Group reference
		 * 
		 * TODO We should unify the group references for all kind of components 
		 */
        VersionedReference<?> group = component.getGroupVersioned();
        if(group != null) {
        	
        	switch(group.getComponent().getKind()) {
				case SPECIFICATION:
					property(CST.PROVIDE_SPECIFICATION,group.getName());
					break;
				case IMPLEMENTATION:
					property(CST.IMPLNAME,group.getName());
					break;
				case INSTANCE:
				case COMPONENT:
				default:
					break;
        	}
        	
        	if (group.getRange() != null) {
        		property(CST.REQUIRE_VERSION,group.getRange());
        	}
        }

        /*
         * Specific properties depending on the kind of component
         */
        if (component instanceof AtomicImplementationDeclaration) {
        	AtomicImplementationDeclaration atomicComponent = (AtomicImplementationDeclaration) component;
            property(CST.PROVIDE_CLASSNAME, atomicComponent.getClassName());
        }

		if (component instanceof CompositeDeclaration) {
			property(CST.APAM_COMPOSITE,flag(true));

			CompositeDeclaration composite = (CompositeDeclaration) component;
			if (composite.getMainComponent() != null) {
				property(CST.APAM_MAIN_COMPONENT, composite.getMainComponent().getName());
			}
		}

		/*
		 * Predefined properties
		 */
        if(component.isDefinedSingleton()) {
        	property(CST.SINGLETON,flag(component.isSingleton()));
        }

        if(component.isDefinedShared()) {
        	property(CST.SHARED,flag(component.isShared()));
        }

        if(component.isDefinedInstantiable()) {
        	property(CST.INSTANTIABLE,flag(component.isInstantiable()));
        }
        

        /*
         * provided resources
         */
		provided();

		/*
		 * relations
		 */
        relations();

        /*
         * property definitions and values
         */
		properties();

		return result();
	}

	/**
	 * Encode the provided resources of the component
	 */
	private void provided() {

		Set<InterfaceReference> interfaces = component.getProvidedResources(InterfaceReference.class);
		if (!interfaces.isEmpty()) {
			property(CST.PROVIDE_INTERFACES,Util.list(interfaces));
		}

		Set<MessageReference> messages = component.getProvidedResources(MessageReference.class);
		if (!messages.isEmpty()) {
			property(CST.PROVIDE_MESSAGES, Util.list(messages));
		}
	}

	/**
	 * Encodes the properties and its definitions
	 */
	private void properties() {
		
		for (PropertyDefinition definition : component.getPropertyDefinitions()) {
			property(CST.DEFINITION_PREFIX + definition.getName(), definition.getType(), definition.getDefaultValue());
			
		}
		
		for (Map.Entry<String, String> property : component.getProperties().entrySet()) {
			
			/*
			 * Try to get the exact  type and map it to types supported by OBR
			 */
			PropertyDefinition definition  = component.getPropertyDefinition(property.getKey());
			if (definition == null) {
				property(property.getKey(),property.getValue());
			}
			else {
				property(property.getKey(),type(definition.getType()),property.getValue());
			}
		}
	}

	/**
	 * encodes the type of a property as an OBR type. 
	 * 
	 * Return null if no exact type can be inferred
	 */
	private String type(String type) {

		if (type.equalsIgnoreCase("version"))
			return Property.VERSION;
		
		return null;
	}
	/**
	 * Encodes the relations
	 */
	private void relations() {
		for (RelationDeclaration relation : component.getRelations()) {
			property(CST.RELATION_PREFIX+relation.getIdentifier(),target(relation));
		}
	}
	
	/**
	 * encodes the target of a relation
	 */
	private String target(RelationDeclaration relation) {

		ResolvableReference target	= relation.getTarget();
    	String encodedTarget 		= null;

    	if (target instanceof InterfaceReference) {
    		encodedTarget = "{"+ComponentParser.INTERFACE+"}"+target.getName();
    	}
    	else if (target instanceof MessageReference) {
    		encodedTarget = "{"+ComponentParser.MESSAGE+"}"+target.getName();
    	}
    	else if (target instanceof PackageReference) {
    		encodedTarget = "{"+ComponentParser.PACKAGE+"}"+target.getName();
    	}
    	else if (target instanceof UnknownReference && target.as(InterfaceReference.class) != null) {
    		encodedTarget = "{"+ComponentParser.INTERFACE+"}";
    	}
    	else if (target instanceof UnknownReference && target.as(MessageReference.class) != null) {
    		encodedTarget ="{"+ComponentParser.MESSAGE+"}";
    	}
    	else if (target instanceof ComponentReference) {
    		encodedTarget = target.getName();
    	}
    	else {
    		error("unknown kind of target for relation "+relation);
    	}

    	return encodedTarget;
	}

	
    /**
     * Free references to all intermediate objects (to allow reuse of this encoder) and
     * returns the final result
     */
	private Capability result() {
        this.component 	= null;
        this.reporter	= null;
        
        Capability result 	= this.result.build();
        this.result			= null;
        
        return result;
	}
	
	/**
	 * Add a new property to the result capability
	 */
	private void property(String name, String type, String value) {
		result = result.property(name, type, value);
	}

	/**
	 * Add a new untyped property to the result capability
 	 */
	private void property(String name, String value) {
		property(name,null,value);
	}

	/**
	 * Convert a boolean value to its serialization
	 */
	private static String flag(boolean flag) {
		return Boolean.toString(flag);
	}

	/**
	 * Convert a component kind to its internal representation
	 */
	private static String kind(ComponentKind componentKind) {
		
		switch (componentKind) {
			case SPECIFICATION:
				return CST.SPECIFICATION;
			case IMPLEMENTATION:
				return CST.IMPLEMENTATION;
			case INSTANCE:
				return CST.INSTANCE;
			default:
				return null;
		}
	}
	
	private final void error(String message) {
		reporter.report(Severity.ERROR, message);
	}
	
	private final void info(String message) {
		reporter.report(Severity.INFO, message);
	}
	

	/**
	 * A simple builder to construct an OBR capability
	 * 
	 * @author vega
	 *
	 */
	public static class Builder {
		
		private String 			name;
		private List<Property>	properties;
		
		private Builder() {
			this.properties	= new ArrayList<Property>();
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder property(String name, String type, String value) {
			this.properties.add(new SerializationProperty(name,type,value));
			return this;
		}

		public Builder property(String name, String value) {
			return property(name,null,value);
		}
		
		public Capability build() {
			return new SerializationCapability(name,properties);
		}
		
	}

	/**
	 * Get a new capability builder
	 */
	public static Builder builder(String name) {
		return new Builder().name(name);
	}
	
	/**
	 * Internal read only capability representation
	 * 
	 * TODO We should reuse the felix OBR implementation, but there is currently no public
	 * API to create capabilities with typed properties
	 * 
	 * @author vega
	 *
	 */
	private static class SerializationCapability implements Capability {

		private final String 	name;
		private final Property[] properties;
		
		public SerializationCapability(String name, List<Property> properties) {
			this.name		= name;
			this.properties	= properties.toArray(new Property[properties.size()]);;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Property[] getProperties() {
			return properties;
		}

		@Override
		public Map<String,?> getPropertiesAsMap() {
			throw new UnsupportedOperationException("This is capability implementation is intended only for serialization");
		}
		
	}
	
	/**
	 * Internal read only property representation
	 * 
	 * TODO We should reuse the felix OBR implementation, but there is currently no public
	 * API to create typed properties
	 * 
	 * @author vega
	 *
	 */
	private static class SerializationProperty implements Property {

		private final String name;
		private final String type;
		private final String value;

		public SerializationProperty(String name, String type, String value) {
			this.name	= name;
			this.type	= type;
			this.value	= value;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public Object getConvertedValue() {
			throw new UnsupportedOperationException("This property implementation is intended only for serialization");
		}
		
	}
	
	/**
	 * Creates a new requirement to represent a component reference
	 */
	public static Requirement requirement(VersionedReference<?> reference) {
		return new ComponentRequirement(reference);
	}
	
	/**
	 * A requirement inferred from a referenced component version
	 * 
	 * @author vega
	 *
	 */
	private static class ComponentRequirement implements Requirement {

		private final VersionedReference<?> 	reference;
		private final String 		filter;
		
		public ComponentRequirement(VersionedReference<?> reference) {
			
			this.reference	= reference;
			
			String filter	= null;
			try {
				filter = ApamComponentRepository.filter(reference);
			}
			catch(Exception parseException) {
			}
			
			this.filter		= filter;
		}
		
		@Override
		public String getName() {
			return CST.CAPABILITY_COMPONENT;
		}

		@Override
		public String getFilter() {
			return filter;
		}

		@Override
		public boolean isMultiple() {
			return false;
		}

		@Override
		public boolean isOptional() {
			return false;
		}

		@Override
		public boolean isExtend() {
			return false;
		}

		@Override
		public String getComment() {
			return "required component : "+reference.getName()+" version "+ reference.getRange();
		}

		@Override
		public boolean isSatisfied(Capability capability) {
			throw new UnsupportedOperationException("This requirement implementation is intended only for serialization");
		}
	}
}
