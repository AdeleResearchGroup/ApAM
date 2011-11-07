package fr.imag.adele.apam.dependency;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMImpl.DependencyModel;
import fr.imag.adele.apam.apamAPI.ASMImpl.TargetKind;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ApamClient;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.implementation.Implementation;
import fr.imag.adele.apam.implementation.ImplementationHandler;

public class DependencyHandler extends ImplementationHandler implements ApamDependencyHandler {

	/**
	 * Configuration property to specify the dependency's name
	 */
	private final static String DEPENDENCY_NAME_PROPERTY 			= "name";

	/**
	 * Configuration property to specify the injected field
	 */
	private final static String DEPENDENCY_FIELD_PROPERTY 			= "field";

	/**
	 * Configuration property to specify the injected field
	 */
	private final static String DEPENDENCY_FIELD_ALTERNATE_PROPERTY = "fields";

	/**
	 * Configuration property to specify the promoted source for composites
	 */
	private final static String DEPENDENCY_SOURCE_PROPERTY 			= "source";

	/**
	 * Configuration property to specify the dependency's aggregate attribute
	 */
	private final static String DEPENDENCY_AGGREGATE_PROPERTY 		= "multiple";

	/**
	 * Configuration property to specify the dependency's target interface
	 */
	private final static String DEPENDENCY_INTERFACE_PROPERTY 		= "interface";

	/**
	 * Configuration property to specify the dependency's target specification
	 */
	private final static String DEPENDENCY_SPECIFICATION_PROPERTY 	= "specification";

	/**
	 * Configuration property to specify the dependency's target implementation
	 */
	private final static String DEPENDENCY_IMPLEMENTATION_PROPERTY 	= "implementation";

	/**
	 * Configuration element to handle APAM dependency constraints
	 */
	private final static String CONSTRAINTS_DECLARATION 			= "constraints";

	/**
	 * Configuration element to handle APAM dependency preferences
	 */
	private final static String PREFERENCES_DECLARATION 			= "preferences";

	/**
	 * Configuration element to handle APAM dependency constraints
	 */
	private final static String CONSTRAINT_DECLARATION 				= "constraint";

	/**
	 * Configuration property to specify the dependency's target implementation
	 */
	private final static String CONSTRAINT_FILTER_PROPERTY 			= "filter";

	/**
	 * A reference to the APAM machine.
	 */
	private ApamClient apam;

	/**
	 * A reference to the corresponding instance in the APAM application state
	 */
	private ASMInst thisInstance;

	/**
	 * The list of dependencies declared in this component
	 */
	private Map<String, Dependency> dependencies;

	/*
	 * @see
	 * org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.
	 * felix.ipojo.architecture. ComponentTypeDescription,
	 * org.apache.felix.ipojo.metadata.Element)
	 */
	@Override
	public void initializeComponentFactory(	ComponentTypeDescription componentDescriptor, Element componentMetadata) throws ConfigurationException {

		String implementationName = componentDescriptor.getName();

		boolean isApamImplementation = componentDescriptor instanceof Implementation.Description;
		Implementation.Description implementationDescription = isApamImplementation ? (Implementation.Description) componentDescriptor	: null;

		/*
		 * Validate the component class is accessible
		 */

		boolean hasInstrumentedCode = (!isApamImplementation) || implementationDescription.getFactory().hasInstrumentedCode();
		Class<?> instrumentedCode = null;
		
		try {
			instrumentedCode = hasInstrumentedCode ? getFactory().loadClass(getFactory().getClassName()) : null;

		} catch (ClassNotFoundException e) {
			throw new ConfigurationException("iPojo component "+quote(componentDescriptor.getName())+": " 
								+ "the component class "+ getFactory().getClassName() + " can not be loaded");
		}
			
		/*
		 * Statically validate the component type dependencies
		 */

		Element dependencyDeclarations[] = componentMetadata.getElements(Implementation.DEPENDENCY_DECLARATION,APAM_NAMESPACE);
		for (Element dependencyDeclaration : dependencyDeclarations) {

			String dependencyName 			= dependencyDeclaration.getAttribute(DEPENDENCY_NAME_PROPERTY);
			String dependencyFieldNames 	= dependencyDeclaration.getAttribute(DEPENDENCY_FIELD_PROPERTY);
			String dependencySourceNames 	= dependencyDeclaration.getAttribute(DEPENDENCY_SOURCE_PROPERTY);

			/*
			 * Check for alternate syntax for field attribute.
			 * 
			 * TODO This is to overcome a problem with the iPojo manipulator that treats attribute "field", in any tag, with the
			 * special semantic that it must be a declared field in the instrumented class. 
			 * 
			 * We should settle in a different attribute name, instead of trying to be backward compatible. 
			 */
			if (dependencyFieldNames == null) {
				String alternateDependencyFieldNames = dependencyDeclaration.getAttribute(DEPENDENCY_FIELD_ALTERNATE_PROPERTY);
				if (alternateDependencyFieldNames != null) {
					dependencyFieldNames = alternateDependencyFieldNames;
					dependencyDeclaration.addAttribute(new Attribute(DEPENDENCY_FIELD_PROPERTY,dependencyFieldNames));
				}
			}
			/*
			 * Validate a field or source has been specified
			 */
			if (hasInstrumentedCode && dependencyFieldNames == null) {
				throw new ConfigurationException("APAM Dependency "+quote(implementationName)+": "
									+ "a field must be specified");
			}

			if ((!hasInstrumentedCode) && dependencySourceNames == null) {
				throw new ConfigurationException("APAM Dependency "+quote(implementationName)+": "
									+ "a source must be specified");
			}
			
			/*
			 * If dependency name is not explicitly declared, use the field or source names as default
			 */
			if (dependencyName == null) {
				dependencyName = hasInstrumentedCode ? dependencyFieldNames : dependencySourceNames;
				dependencyDeclaration.addAttribute(new Attribute(DEPENDENCY_NAME_PROPERTY,dependencyName));
			}

			/*
			 * validate the specified target of the dependency is specified
			 */

			String dependencySpecification = dependencyDeclaration.getAttribute(DependencyHandler.DEPENDENCY_SPECIFICATION_PROPERTY);
			String dependencyImplementation = dependencyDeclaration.getAttribute(DependencyHandler.DEPENDENCY_IMPLEMENTATION_PROPERTY);
			String dependencyInterface = dependencyDeclaration.getAttribute(DependencyHandler.DEPENDENCY_INTERFACE_PROPERTY);
			
			
			if ((dependencySpecification != null) && (dependencyImplementation != null)) {
				throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
								+ "specification and implementation declarations are exclusive");
			}

			
			String dependencyAggregate 			= dependencyDeclaration.getAttribute(DependencyHandler.DEPENDENCY_AGGREGATE_PROPERTY);
			boolean isDependencyAggregateSet 	= dependencyAggregate != null;
			boolean isDependencyAggregate 		= isDependencyAggregateSet? Boolean.valueOf(dependencyAggregate) : false;

			/*
			 * validate referenced fields actually exist in the component class, have the right type and has been actually instrumented
			 * by iPojo. If possible, use instrumented code to infer some sensible default values for unspecified attributes.
			 */

			if (hasInstrumentedCode) {
				for (String fieldName : ParseUtils.parseArrays(dependencyFieldNames)) {

					try {
						
						Field field 				= instrumentedCode.getDeclaredField(fieldName);
						Class<?> fieldClass 		= field.getType();
						boolean isCollectionField	= fieldClass.isArray() || Collection.class.isAssignableFrom(fieldClass);


						if (getFactory().getPojoMetadata().getField(fieldName) == null) {
							throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
											+ "the specified field "+quote(fieldName)+" is not instrumented by iPojo");
						}
						
						if (isCollectionField && !Dependency.isSupportedCollection(field)) {
							throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
									+ "the specified field "+quote(fieldName)+" must be of type "+Dependency.supportedCollectionClasses());
						}
						
						/*
						 * Infer aggregate attribute from instrumented code if not specified
						 */
						if (!isDependencyAggregateSet) {
							isDependencyAggregate 		= isCollectionField;
							isDependencyAggregateSet 	= true;
							dependencyAggregate 		= Boolean.toString(isDependencyAggregate);
							dependencyDeclaration.addAttribute(new Attribute(DEPENDENCY_AGGREGATE_PROPERTY,dependencyAggregate));
						}
					
						/*
						 * validate cardinality of the dependency matches the field cardinality
						 */
						if (isCollectionField && !isDependencyAggregate) {
							throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
											+ "the class of the field "+quote(fieldName)+" cannot be a Collection or array");
						}

						if ((!isCollectionField) && isDependencyAggregate) {
							throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
											+ "the class of the field "+quote(fieldName)+" must be a Collection or array");
						}
						
						/*
						 * validate the specified target is compatible with the field declaration
						 * 
						 * NOTE it is not always possible to perform this validation, as the class of the elements of a collection
						 * can not always be inferred from the field declaration, and the interfaces provided by a given specification
						 * are not always known.
						 */

						Class<?> fieldElementClass = isDependencyAggregate ? Dependency.getCollectionElement(field) : field.getType();

						if (fieldElementClass != null && dependencyInterface != null) {
							try {

								Class<?> interfaceClass = getFactory().loadClass(dependencyInterface);
								
								if (!fieldElementClass.isAssignableFrom(interfaceClass)) {
									throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
													+ "the specified interface "+quote(dependencyInterface)+" "
													+ "can not be assigned to field "+quote(fieldName));
								}

							} catch (ClassNotFoundException e) {
								throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
													+ "the specified interface "+ quote(dependencyInterface)+ " is not accessible");
							}
						}

						if (fieldElementClass != null	&& dependencySpecification != null && !fieldElementClass.isInterface()) {
							throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
									+ "the specified field "+quote(fieldName)+" "
									+ " must be declared using an interface provided by specification "+quote(dependencySpecification));
						}
						
						/*
						 * If no target is specified, try to infer at least a target interface from the field definition
						 */
						if (dependencySpecification == null && dependencyImplementation == null	&& dependencyInterface == null) {
							if (fieldElementClass != null	&& fieldElementClass.isInterface()) {
								dependencyInterface = fieldElementClass.getCanonicalName();
								dependencyDeclaration.addAttribute(new Attribute(DEPENDENCY_INTERFACE_PROPERTY,dependencyInterface));
							}
						}


						
					} catch (SecurityException e1) {
						throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
								+ "the specified field "+ quote(fieldName)+" is not accesible in the implementation class");
					} catch (NoSuchFieldException e1) {
						throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
								+ "the specified field "+quote(fieldName)+" is not declared in the implementation class");
					}

				};
			}
			

			/*
			 * Validate a target was specified or could be inferred from the instrumented code
			 */
			if ((dependencySpecification == null) && (dependencyImplementation == null)	&& (dependencyInterface == null)) {
				throw new ConfigurationException("APAM Dependency "+quote(implementationName+"."+dependencyName)+": "
								+ "has no target; declare a target interface, specification or implementation");
			}

			/*
			 * Validate filters on constraints
			 */

			Element constraintsDeclarations[] = dependencyDeclaration.getElements(CONSTRAINTS_DECLARATION, APAM_NAMESPACE);
			
			for (Element constraintsDeclaration : optional(constraintsDeclarations)) {

				Element constraintDeclarations[] = constraintsDeclaration.getElements(CONSTRAINT_DECLARATION, APAM_NAMESPACE);
				for (Element constraintDeclaration : optional(constraintDeclarations)) {
					
					String filter = constraintDeclaration.getAttribute(CONSTRAINT_FILTER_PROPERTY);
					if (filter == null)
						throw new ConfigurationException("APAM Dependency "+quote(implementationName+"." +dependencyName)+ ": "
											+ "constraint filter not specified");

					try {
						getFactory().getBundleContext().createFilter(filter);
					} catch (InvalidSyntaxException invalidFilter) {
						throw new ConfigurationException("APAM Dependency "+quote(implementationName+"." +dependencyName)+ ": "
											+ "invalid constraint filter "+ invalidFilter.getMessage());
					}
				}
			}

			/*
			 * Validate filters on preferences
			 */

			Element preferencesDeclarations[] = dependencyDeclaration.getElements(PREFERENCES_DECLARATION, APAM_NAMESPACE);
			
			for (Element preferencesDeclaration : optional(preferencesDeclarations)) {

				Element constraintDeclarations[] = preferencesDeclaration.getElements(CONSTRAINT_DECLARATION, APAM_NAMESPACE);
				for (Element constraintDeclaration : optional(constraintDeclarations)) {
					
					String filter = constraintDeclaration.getAttribute(CONSTRAINT_FILTER_PROPERTY);
					if (filter == null)
							throw new ConfigurationException("APAM Dependency "+quote(implementationName+ "."+dependencyName)+ ": "
												+ "preference filter not specified");
					try {
						getFactory().getBundleContext().createFilter(filter);
					} catch (InvalidSyntaxException invalidFilter) {
						throw new ConfigurationException("APAM Dependency "+quote(implementationName+"." +dependencyName)+ ": "
											+ "invalid preference filter "+ invalidFilter.getMessage());
					}
					
				}
			}

			/*
			 * Calculate dependency model form metadata and register it in the implementation description
			 */

			DependencyModel dependency = new DependencyModel();

			dependency.dependencyName 	= dependencyName;
			dependency.isMultiple 		= isDependencyAggregate;
			dependency.target 			= dependencyInterface;
			dependency.targetKind 		= TargetKind.INTERFACE;
			dependency.source			= !hasInstrumentedCode ? ParseUtils.parseArrays(dependencySourceNames) : new String[0];
			
			if (dependencySpecification != null) {
				dependency.target 		= dependencySpecification;
				dependency.targetKind 	= TargetKind.SPECIFICATION;
			}

			if (dependencyImplementation != null) {
				dependency.target 		= dependencyImplementation;
				dependency.targetKind 	= TargetKind.IMPLEMENTATION;
			}

			if (isApamImplementation)
				implementationDescription.addDependency(dependency);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata
	 * .Element, java.util.Dictionary)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void configure(Element componentMetadata, Dictionary configuration)
			throws ConfigurationException {
		/*
		 * Add interceptors to delegate dependency resolution
		 * 
		 * NOTE All validations were already performed when validating the
		 * factory @see initializeComponentFactory, including initializing
		 * unspecified properties with appropriate default values. Here we just
		 * assume metadata is correct.
		 */
		dependencies = new HashMap<String, Dependency>();

		Element dependencyDeclarations[] = componentMetadata.getElements(
				Implementation.DEPENDENCY_DECLARATION,
				DependencyHandler.APAM_NAMESPACE);

		for (Element dependencyDeclaration : dependencyDeclarations) {

			String dependencyName = dependencyDeclaration
					.getAttribute(DependencyHandler.DEPENDENCY_NAME_PROPERTY);
			String dependencyFieldsName = dependencyDeclaration
					.getAttribute(DependencyHandler.DEPENDENCY_FIELD_PROPERTY);
			String dependencyAggregate = dependencyDeclaration
					.getAttribute(DependencyHandler.DEPENDENCY_AGGREGATE_PROPERTY);
			String dependencySpecification = dependencyDeclaration
					.getAttribute(DependencyHandler.DEPENDENCY_SPECIFICATION_PROPERTY);
			String dependencyImplementation = dependencyDeclaration
					.getAttribute(DependencyHandler.DEPENDENCY_IMPLEMENTATION_PROPERTY);
			String dependencyInterface = dependencyDeclaration
					.getAttribute(DependencyHandler.DEPENDENCY_INTERFACE_PROPERTY);

			String dependencyTarget = dependencyInterface;
			Dependency.Kind dependencyKind = Dependency.Kind.INTERFACE;

			if (dependencySpecification != null) {
				dependencyTarget = dependencySpecification;
				dependencyKind = Dependency.Kind.SPECIFICATION;
			}

			if (dependencyImplementation != null) {
				dependencyTarget = dependencyImplementation;
				dependencyKind = Dependency.Kind.IMPLEMENTATION;
			}

			Set<Filter> constraints = new HashSet<Filter>();
			List<Filter> preferences = new ArrayList<Filter>();

			/*
			 * Iterate over all constraints declarations
			 */
			Element constraintsDeclarations[] = dependencyDeclaration.getElements(CONSTRAINTS_DECLARATION, APAM_NAMESPACE);
			for (Element constraintsDeclaration : optional(constraintsDeclarations)) {

				Element constraintDeclarations[] = constraintsDeclaration.getElements(CONSTRAINT_DECLARATION, APAM_NAMESPACE);
				for (Element constraintDeclaration : optional(constraintDeclarations)) {
					try {
						constraints.add(getFactory().getBundleContext().createFilter(constraintDeclaration.getAttribute(CONSTRAINT_FILTER_PROPERTY)));
					} catch (InvalidSyntaxException ignored) {
					}
				}
			}

			/*
			 * Iterate over all preferences declarations
			 */
			Element preferencesDeclarations[] = dependencyDeclaration.getElements(PREFERENCES_DECLARATION, APAM_NAMESPACE);
			for (Element preferencesDeclaration : optional(preferencesDeclarations)) {

				Element constraintDeclarations[] = preferencesDeclaration.getElements(CONSTRAINT_DECLARATION, APAM_NAMESPACE);
				for (Element constraintDeclaration : optional(constraintDeclarations)) {
					try {
						preferences.add(getFactory().getBundleContext().createFilter(constraintDeclaration.getAttribute(CONSTRAINT_FILTER_PROPERTY)));
					} catch (InvalidSyntaxException ignored) {
					}
				}
			}

			/*
			 * register the dependency injector to handle all decalred fields
			 */
			Dependency dependency = new Dependency(this, getFactory().getPojoMetadata(),
											dependencyName, Boolean.valueOf(dependencyAggregate),
											dependencyTarget, dependencyKind, constraints, preferences);
			
			dependencies.put(dependencyName, dependency);
			
			for (String fieldName : ParseUtils.parseArrays(dependencyFieldsName)) {
				getInstanceManager().register(getFactory().getPojoMetadata().getField(fieldName), dependency);
			}

			
		}

	}

	/**
	 * The description of this handler instance
	 * 
	 */
	private static class Description extends HandlerDescription {

		private final DependencyHandler dependencyManager;

		public Description(DependencyHandler dependencyManager) {
			super(dependencyManager);
			this.dependencyManager = dependencyManager;
		}

		@Override
		public Element getHandlerInfo() {
			Element root = super.getHandlerInfo();

			ASMInst self = dependencyManager.thisInstance;
			root.addAttribute(new Attribute("asm-instance", self == null ? "" : self.getName()));
			for (Dependency dependency : dependencyManager.dependencies.values()) {
				
				Element dependencyDescription = new Element("dependency", "");
				dependencyDescription.addAttribute(new Attribute("name", dependency.getName()));
				dependencyDescription.addAttribute(new Attribute("isAggregate",	Boolean.toString(dependency.isAggregate())));
				dependencyDescription.addAttribute(new Attribute("target", dependency.getTarget()));
				dependencyDescription.addAttribute(new Attribute("kind", dependency.getKind().toString()));

				boolean firstElement = false;

				StringBuffer constraints = new StringBuffer();
				constraints.append("{");
				firstElement = true;
				for (Filter filter : dependency.getConstraints()) {
					if (!firstElement)
						constraints.append(",");
					constraints.append(filter.toString());
					firstElement = false;
				}
				constraints.append("}");
				dependencyDescription.addAttribute(new Attribute("constraints", constraints.toString()));

				StringBuffer preferences = new StringBuffer();
				preferences.append("{");
				firstElement = true;
				for (Filter filter : dependency.getPreferences()) {
					if (!firstElement)
						preferences.append(",");
					preferences.append(filter.toString());
					firstElement = false;
				}
				preferences.append("}");
				dependencyDescription.addAttribute(new Attribute("preferences",	preferences.toString()));

				dependencyDescription.addAttribute(new Attribute("resolved",Boolean.toString(dependency.isResolved())));

				if (dependency.isResolved()) {
					Set<ASMInst> targets = dependencyManager.thisInstance.getWireDests(dependency.getName());
					StringBuffer resolution = new StringBuffer();
					if (targets.size() > 1)
						resolution.append("{");

					firstElement = true;

					for (ASMInst target : targets) {
						if (!firstElement)
							resolution.append(",");
						resolution.append(target.getName());
						firstElement = false;
					}

					if (targets.size() > 1)
						resolution.append("}");

					dependencyDescription.addAttribute(new Attribute("asm-resolution", resolution.toString()));
				}
				root.addElement(dependencyDescription);
			}

			return root;
		}

	}

	@Override
	public HandlerDescription getDescription() {
		return new Description(this);
	}

	@Override
	public void start() {
		/*
		 * The instance is started, nothing to do; we should already be
		 * registered
		 */
	}

	/**
	 * Dynamically register this instance with APAM
	 */
	public void apamBound() {
		apam.newClientCallBack(getInstanceManager().getInstanceName(), this);
	}

	/**
	 * Avoid invoking APAM, if not available
	 */
	public void apamUnbound() {
		thisInstance = null;
	}

	/**
	 * This callback method will be invoked by APAM when the instance is
	 * effectively added to the application state model
	 */
	public void SetIdentifier(ASMInst inst) {
		thisInstance = inst;
	}

	@Override
	public void stop() {
	}

	@Override
	public String toString() {
		return "APPAM Dependency manager for "
				+ getInstanceManager().getInstanceName();
	}

	public boolean setWire(ASMInst destInst, String depName) {
//		System.out.println("Handler set wire " + depName + " :" + thisInstance	+ "->" + destInst);
		Dependency dependency = dependencies.get(depName);

		if (dependency == null)
			return false;

		dependency.addDependency(destInst);
		return true;
	}

	public boolean remWire(ASMInst destInst, String depName) {
		Dependency dependency = dependencies.get(depName);

		if (dependency == null)
			return false;

		dependency.removeDependency(destInst);
		return true;
	}

	public boolean substWire(ASMInst oldDestInst, ASMInst newDestInst,
			String depName) {
		Dependency dependency = dependencies.get(depName);

		if (dependency == null)
			return false;

		dependency.substituteDependency(oldDestInst, newDestInst);
		return true;
	}

	/**
	 * Delegate APAM to resolve a given dependency.
	 * 
	 * NOTE nothing is returned from this method, the call to APAM has as
	 * side-effect the update of the dependency.
	 * 
	 * @param dependency
	 */
	public void resolve(Dependency dependency) {

		/*
		 * This instance is not actually yet managed by APAM
		 */
		if (apam == null)
			return;

		if (thisInstance == null) {
			thisInstance = CST.ASMInstBroker.getInst(getInstanceManager().getInstanceName());
			if (thisInstance == null) {
				System.err.println("Dependency resolver: client instance " + getInstanceManager().getInstanceName() + " unkown");
			}
		}
		
		/*
		 * Make a copy of constraints and preferences before invoking resolution. This allow resolution managers to modify constraints
		 * and preferences are part of their processing.
		 */
		Set<Filter> constraints 	= new HashSet<Filter>(dependency.getConstraints());
		List<Filter> preferences	= new ArrayList<Filter>(dependency.getPreferences());
		
		
		switch (dependency.getKind()) {
		case IMPLEMENTATION:
			if (dependency.isScalar())
				apam.newWireImpl(thisInstance, dependency.getTarget(),dependency.getName(), constraints, preferences);
			else
				apam.newWireImpls(thisInstance, dependency.getTarget(),	dependency.getName(), constraints);
			break;
		case SPECIFICATION:
			if (dependency.isScalar())
				apam.newWireSpec(thisInstance, null, dependency.getTarget(), dependency.getName(), constraints, preferences);
			else
				apam.newWireSpecs(thisInstance, null, dependency.getTarget(), dependency.getName(), constraints, preferences);
			break;
		case INTERFACE:
			if (dependency.isScalar())
				apam.newWireSpec(thisInstance, dependency.getTarget(), null, dependency.getName(), constraints, preferences);
			else
				apam.newWireSpecs(thisInstance, dependency.getTarget(), null, dependency.getName(), constraints, preferences);
			break;
		}
	}

}
