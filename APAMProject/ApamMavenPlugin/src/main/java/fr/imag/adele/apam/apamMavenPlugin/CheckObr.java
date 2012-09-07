package fr.imag.adele.apam.apamMavenPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

public class CheckObr {

	private static Logger logger = LoggerFactory.getLogger(CheckObr.class);

	private static final Set<String>             allFields         = new HashSet<String>();

	private static boolean                       failedChecking = false;

	/**
	 * An string value that will be used to represent mandatory attributes not specified. From CoreParser.
	 */
	public final static String                   UNDEFINED      = new String("<undefined value>");


	public static boolean getFailedChecking() {
		return CheckObr.failedChecking;
	}

	public static void error(String msg) {
		CheckObr.failedChecking = true;
		logger.error(msg);
	}

	public static void warning(String msg) {
		System.out.println(msg);
	}

	public static void setFailedParsing(boolean failed) {
		CheckObr.failedChecking = failed;
	}

	/**
	 * Checks if the constraints set on this dependency are syntacticaly valid.
	 * Only for specification dependencies.
	 * Checks if the attributes mentioned in the constraints can be set on an implementation of that specification.
	 * @param dep a dependency
	 */
	private static void checkConstraint(DependencyDeclaration dep) {
		if ((dep == null) || !(dep.getTarget() instanceof SpecificationReference))
			return;

		SpecificationReference reference = dep.getTarget().as(SpecificationReference.class);
		String spec = reference.getName();
		
		//get the spec definition
		ApamCapability cap = ApamCapability.get(reference);
		if (cap == null)
			return;
		
		//computes the attributes that can be associated with this spec implementations
		Set<String> validAttrs = cap.getValidAttrNames();

		CheckObr.checkFilterSet(dep.getImplementationConstraints(), validAttrs, spec);
		CheckObr.checkFilterList(dep.getImplementationPreferences(), validAttrs, spec);
		CheckObr.checkInstFilterSet(dep.getInstanceConstraints(), validAttrs, spec);
		CheckObr.checkInstFilterList(dep.getInstancePreferences(), validAttrs, spec);
	}

	/**
	 * In theory we cannot check a constraint on instance attributes since we do not know the implementation that
	 * will be selected, however, if the constraints contains "impl-name = xyz" we could do it.
	 * 
	 * At least we can check the spec and implem attributes
	 * 
	 */
	public static void checkInstFilterList(List<String> filters, Set<String> validAttr, String spec) {
		if ((validAttr == null) || (filters == null))
			return;

		// try to see if implementation name "impl-name" is in the constraints
		String impl = null;
		for (String f : filters) {
			try {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				if (parsedFilter == null)
					CheckObr.error("String " + f + " returns null filter.");
				else
					impl = parsedFilter.lookForAttr(CST.A_IMPLNAME);
			} catch (Exception e) {
				CheckObr.error("Invalid filter " + f);
			}
			if (impl != null)
				break ;
		}
		
		// if implementation is found, see if the constraint is Ok.
		ApamCapability cap = null;
		if (impl != null) {
			cap = ApamCapability.get(new ImplementationReference<ImplementationDeclaration>(impl));
			if (cap != null) {
				//extends validAttr with the attributes defined by the implem
				validAttr = cap.getValidAttrNames() ;
			}
			
		}
		String msg = (impl == null) ? spec : impl ;
		CheckObr.checkFilterList(filters, validAttr, msg);
	}

	public static void checkInstFilterSet(Set<String> filters, Set<String> validAttr, String spec) {
		List<String> filterSet = new ArrayList<String>(filters);
		CheckObr.checkInstFilterList(filterSet, validAttr, spec);
	}

	public static void checkFilterList(List<String> filters, Set<String> validAttr, String spec) {
		for (String f : filters) {
			ApamFilter parsedFilter = ApamFilter.newInstance(f);
			parsedFilter.validateAttr(validAttr, f, spec);
		}
	}

	public static void checkFilterSet(Set<String> filters, Set<String> validAttr, String spec) {
		for (String f : filters) {
			ApamFilter parsedFilter = ApamFilter.newInstance(f);
			parsedFilter.validateAttr(validAttr, f, spec);
		}
	}


	/**
	 * Check the consistency of an instance :
	 * -Existence of its implementation,
	 * -Validity of its attributes,
	 * -Validity of its constraints.
	 * 
	 * @param instance
	 */
	public static void checkInstance(InstanceDeclaration instance) {
		if (instance.getImplementation() == null) {
			CheckObr.error("ERROR: implementation name missing for instance " + instance.getName());
			return;
		}
	}

	/**
	 * Checks the attributes defined in the component; 
	 * if valid, they are returned.
	 * Then the attributes pertaining to the entity above are added.
	 * @param component the component to check
	 */
	public static Map<String, Object> getValidProperties(ComponentDeclaration component) {
		//the attributes to return
		Map<String, Object> ret = new HashMap <String, Object> ();
		//Properties of this component
		Map<String, Object> properties = component.getProperties();
		
		ApamCapability entCap = ApamCapability.get(component.getReference()) ;

		//return the valid attributes
		for (String attr : properties.keySet()) {
			attr = attr.toLowerCase() ;
			if (validDefObr (entCap, attr, properties.get(attr))) {
				ret.put(attr, properties.get(attr)) ;
			}
		}
		
		//add the attribute coming from "above" if not already instantiated and heritable
		ApamCapability group = entCap.getGroup() ;
		if (group != null && group.getProperties()!= null) {
			for (String prop : group.getProperties().keySet()) {
				if (ret.get(prop) == null && Util.isInheritedAttribute(prop)) {
					ret.put(prop, group.getProperties().get(prop)) ;
				}			
//						&& !Util.isReservedAttributePrefix(prop)
//						&& !prop.equals((CST.A_NAME)) 
//						&& !prop.equals(CST.COMPONENT_TYPE)) 
//				{
			}
		}		
		return ret ;
	}

	/**
	 * Checks if the attribute / values pair is valid for the component ent.
	 * 
	 * @param entName
	 * @param attr
	 * @param value
	 * @param groupProps
	 * @param superGroupProps
	 * @return
	 */
	private static boolean validDefObr (ApamCapability ent, String attr, Object value) {
		if (Util.isPredefinedAttribute(attr))return true ; ;
		if (!Util.validAttr(ent.getName(), attr)) return false  ;
		
		//Top group all is Ok
		ApamCapability group = ent.getGroup() ;
		if (group == null) return true ;
		
		if (group.getProperties().get(attr) != null)  {
			logger.error("Warning: cannot redefine attribute \"" + attr + "\"");
			return false ;
		}

		String defAttr = null ;
		while (group != null) {
			defAttr = group.getAttrDefinition(attr)  ;
			if (defAttr != null) break ;
			group = group.getGroup() ;
		}
		 
		if (defAttr == null) {
			logger.error("Warning: in " + ent.getName() + ", attribute \"" + attr + "\" used but not defined.");
			return false ;
		}

		return Util.checkAttrType(attr, value, defAttr) ;		
	}


	/**
	 * An implementation has the following provide; check if consistent with the list of provides found in "cap".
	 * 
	 * @param cap. Can be null.
	 * @param interfaces = "{I1, I2, I3}" or I1 or null
	 * @param messages= "{M1, M2, M3}" or M1 or null
	 * @return
	 */
	public static boolean checkImplProvide(String implName, String spec, Set<InterfaceReference> interfaces,
			Set<MessageReference> messages) {
		if (spec == null)
			return true;
		ApamCapability cap = ApamCapability.get(new SpecificationReference(spec));
		if (cap == null) {
			return true;
		}

		Set<MessageReference> specMessages = cap.getProvideMessages();
		Set<InterfaceReference> specInterfaces = cap.getProvideInterfaces();

		if (!(messages.containsAll(specMessages)))
			CheckObr.error("Implementation " + implName + " must produce messages "
					+ Util.toStringSetReference(specMessages)) ;

		if (!(interfaces.containsAll(specInterfaces)))
			CheckObr.error("Implementation " + implName + " must implement interfaces "
					+ Util.toStringSetReference(specInterfaces)) ;

		return true;
	}


	public static void checkCompoMain(CompositeDeclaration composite) {
		String name = composite.getName();
		String implName = composite.getMainComponent().getName();
		ApamCapability cap = ApamCapability.get(composite.getMainComponent());
		if (cap == null) {
			return;
		}
		if (composite.getSpecification() != null) {
			String spec = composite.getSpecification().getName();
			String capSpec = cap.getProperty(CST.A_PROVIDE_SPECIFICATION);
			if ((capSpec != null) && !spec.equals(capSpec)) {
				CheckObr.error("In " + name + " Invalid main implementation. " + implName
						+ " must implement specification " + spec);
			}
		}

		Set<MessageReference> mainMessages = cap.getProvideMessages();
		Set<MessageReference> compositeMessages = composite.getProvidedResources(MessageReference.class);
		if (!mainMessages.containsAll(compositeMessages))
			CheckObr.error("In " + name + " Invalid main implementation. " + implName
					+ " produces messages " + mainMessages
					+ " \n but must produce messages " + compositeMessages);

		Set<InterfaceReference> mainInterfaces = cap.getProvideInterfaces() ;
		Set<InterfaceReference> compositeInterfaces = composite.getProvidedResources(InterfaceReference.class);
		if (!mainInterfaces.containsAll(compositeInterfaces))
			CheckObr.error("In " + name + " Invalid main implementation. " + implName
					+ " implements " + mainInterfaces
					+ " \n but must implement interfaces " + compositeInterfaces);
	}

	/**
	 * For all kinds of components checks the dependencies : fields (for implems), and constraints.
	 * 
	 * @param component
	 */
	public static void checkRequire(ComponentDeclaration component) {
		Set<DependencyDeclaration> deps = component.getDependencies();
		if (deps == null)
			return;
		CheckObr.allFields.clear();
		Set<String> depIds = new HashSet<String>();
		for (DependencyDeclaration dep : deps) {
			if (depIds.contains(dep.getIdentifier())) {
				CheckObr.error("ERROR: Dependency " + dep.getIdentifier() + " allready defined.");
			} else
				depIds.add(dep.getIdentifier());
			// validating dependency constraints and preferences..
			CheckObr.checkConstraint(dep);
			// Checking fields and complex dependencies
			CheckObr.checkFieldTypeDep(dep, component);
		}
	}




	/**
	 * Provided a dependency "dep" (simple or complex) checks if the field type and attribute multiple are compatible.
	 * For complex dependency, for each field, checks if the target specification implements the field resource.
	 * 
	 * @param dep : a dependency
	 * @param component : the component currently analyzed
	 */
	private static void checkFieldTypeDep(DependencyDeclaration dep, ComponentDeclaration component) {
		if (!(component instanceof AtomicImplementationDeclaration)) return ;

		// All field must have same multiplicity, and must refer to interfaces and messages provided by the specification.

		Set<ResourceReference> specResources = new HashSet<ResourceReference>();

		if (dep.getTarget() instanceof ComponentReference<?>)
			specResources = ApamCapability.get((ComponentReference)dep.getTarget()).getProvideResources() ;
		else {
			specResources.add(dep.getTarget().as(ResourceReference.class));
		}
	
		Boolean mult = dep.isMultiple();
		for (DependencyInjection innerDep : dep.getInjections()) {
			// check if attribute "multiple" matches the fields type (Set, List Array)
			// if multiple is not explicitly defined, assume the first field multiplicity

			// Done by the parser
//			if (mult == null) {
//				//dep.setMultiple(CheckObr.isFieldMultiple(innerDep, component));
//				mult = dep.isMultiple();
//			}
//			if (mult != CheckObr.isFieldMultiple(innerDep, component)) {
//				if (!mult)
//					CheckObr.error("ERROR: in " + component.getName() + dep + "\n      Field "
//							+ innerDep.getName()
//							+ " is a collection field, while other fields in same dependency are simple.");
//				else
//					CheckObr.error("ERROR: in " + component.getName() + dep + "\n      Field "
//							+ innerDep.getName()
//							+ " is a simple field, while other fields in same dependency are collection.");
//			}
			String type = innerDep.getResource().getJavaType();

			if ((innerDep.getResource() != ResourceReference.UNDEFINED) && !(specResources.contains(innerDep.getResource()))) {
				logger.error("ERROR: in " + component.getName() + dep + "\n      Field "
						+ innerDep.getName()
						+ " is of type " + type
						+ " which is not implemented by specification or implementation " + dep.getIdentifier());
			}
		}
	}

	/**
	 * Provided an atomic dependency, returns if it is multiple or not.
	 * Checks if the same field is declared twice.
	 * 
	 * @param dep
	 * @param component
	 * @return
	 */
	public static boolean isFieldMultiple(DependencyInjection dep, ComponentDeclaration component) {
		if (CheckObr.allFields.contains(dep.getName()) && !dep.getName().equals(CheckObr.UNDEFINED)) {
			CheckObr.error("ERROR: in " + component.getName() + " field/method " + dep.getName()
					+ " allready declared");
		}
		else {
			CheckObr.allFields.add(dep.getName());
		}

		return dep.isCollection();
	}
	
}
