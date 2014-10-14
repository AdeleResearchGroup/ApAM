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

package fr.imag.adele.apam.apammavenplugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.PackageReference;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.declarations.UndefinedReference;
import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;
import fr.imag.adele.apam.declarations.repository.acr.ApamComponentRepository;
import fr.imag.adele.apam.util.ApamMavenProperties;

public class ApamRepoBuilder {

	/**
	 * Metadata (in internal format).
	 */

	private final Set<ComponentReference<?>.Versioned> 	bundleRequiresSpecifications 	= new HashSet<ComponentReference<?>.Versioned>();
	private final Set<ComponentReference<?>.Versioned> 	bundleRequiresImplementations 		= new HashSet<ComponentReference<?>.Versioned>();

	private final ApamCapabilityBroker	broker;
	private final Artifact artifact;
	private final List<ComponentDeclaration> components;

	private final CheckObr validator;
	
	private static final String BEGIN_P = "      <p n='";
	private static final String END_P = "' />\n";
	private static final String END_M = "' >\n";
	private static final String ATT_V = "' v='";

	public ApamRepoBuilder(ApamCapabilityBroker broker, List<ComponentDeclaration> components, Artifact artifact, Log logger) {
		this.broker = broker;
		this.components = components;
		this.validator = new CheckObr(broker, logger);
		
		this.artifact 	= artifact;
	}
	
	public boolean hasFailedChecking() {
		return validator.hasFailedChecking();
	}

	public StringBuffer writeOBRFile() {

		// if a component is defined twice, or error is name space, remove the
		// second definition
		checkDoubleDefinition();

		StringBuffer obrContent = new StringBuffer("<obr> \n");

		// print maven groupId, artifactId and version as resource capability
		printOBRMavenElement(obrContent);

		for (ComponentDeclaration comp : components) {
			if (comp instanceof SpecificationDeclaration) {
				printOBRElement(obrContent, comp);
			}
		}
		for (ComponentDeclaration comp : components) {
			if (comp instanceof AtomicImplementationDeclaration) {
				printOBRElement(obrContent, comp);
			}
		}
		for (ComponentDeclaration comp : components) {
			if (comp instanceof CompositeDeclaration) {
				printOBRElement(obrContent, comp);
			}
		}
		for (ComponentDeclaration comp : components) {
			if (comp instanceof InstanceDeclaration) {
				printOBRElement(obrContent, comp);
			}
		}

		generateFilters(obrContent);

		obrContent.append("</obr> \n");
		return obrContent;
	}

	private void printOBRMavenElement(StringBuffer obrContent) {

		obrContent.append("   <capability name='" + CST.MAVEN + END_M);
		obrContent.append(BEGIN_P + CST.GROUP_ID + ATT_V
				+ artifact.getGroupId() + END_P);
		obrContent.append(BEGIN_P + CST.ARTIFACT_ID + ATT_V
				+ artifact.getArtifactId() + END_P);
		obrContent.append(BEGIN_P + CST.VERSION + ATT_V
				+ artifact.getVersion() + END_P);
		obrContent.append("   </capability>\n");
	}

	private void printOBRElement(StringBuffer obrContent, ComponentDeclaration component) {
		
   		String bundleVersion = this.artifact.getVersion().replace('-', '.');

		String type = "undefine";
		if (component instanceof ImplementationDeclaration) {
			type = "implementation ";
		}
		if (component instanceof CompositeDeclaration) {
			type = "composite ";
		}
		if (component instanceof InstanceDeclaration) {
			type = "instance ";
		}
		if (component instanceof SpecificationDeclaration) {
			type = "specification ";
		}
		
		validator.info("Checking " + type + component.getName() + " ...");

		// headers
		obrContent.append("   <capability name='" + CST.CAPABILITY_COMPONENT
				+ END_M);
		generateProperty(obrContent, component, CST.NAME, component.getName());
        generateTypedProperty(obrContent, component, "version", "version",bundleVersion);

		if (component instanceof ImplementationDeclaration) {
			generateProperty(obrContent, component, CST.COMPONENT_TYPE,
					CST.IMPLEMENTATION);
			generateProperty(obrContent, component, CST.IMPLNAME,
					component.getName());

            SpecificationReference spec = ((ImplementationDeclaration) component).getSpecification();
            if(spec != null ) {
                String versionRange = ((ImplementationDeclaration) component).getSpecificationVersion().getRange();

                if(versionRange!=null && versionRange.length()>0) {
                    generateProperty(obrContent, component, CST.REQUIRE_VERSION,
                            versionRange);
                }

                if(broker.get(spec.getName(), versionRange)==null) {
                    validator.error("Implementation "+component.getName()
                            +" require specification "+spec.getName()+" with version "+versionRange
                            +", which is not available !");
                }
            }
		}

        if (component instanceof AtomicImplementationDeclaration) {
            generateProperty(obrContent, component, CST.PROVIDE_CLASSNAME,
                    ((AtomicImplementationDeclaration) component).getClassName());
        }

		if (component instanceof InstanceDeclaration) {
			generateProperty(obrContent, component, CST.COMPONENT_TYPE,
					CST.INSTANCE);
			generateProperty(obrContent, component, CST.INSTNAME,
					component.getName());

            ComponentReference<?>.Versioned impl = ((InstanceDeclaration) component).getImplementationVersion();
            if(impl!= null ) {
                String versionRange = impl.getRange();

                if(versionRange!=null && versionRange.length()>0) {
                    generateProperty(obrContent, component, CST.REQUIRE_VERSION,
                            versionRange);
                }

                if(broker.get(impl.getComponent().getName(), versionRange)==null) {
                    validator.error("Instance "+component.getName()
                            +" require specification "+impl.getComponent().getName()+" with version "+versionRange
                            +", which is not available !");
                }
            }
		}

		if (component instanceof CompositeDeclaration) {
			CompositeDeclaration composite = (CompositeDeclaration) component;
			generateProperty(obrContent, component, CST.APAM_COMPOSITE,
					CST.V_TRUE);
			if (composite.getMainComponent() != null) {
				generateProperty(obrContent, component,
						CST.APAM_MAIN_COMPONENT, composite.getMainComponent()
								.getName());
			}
			validator.checkCompoMain((CompositeDeclaration) component);
			// check the information for composite : grant, start, own,
			// visibility etc.
			validator.checkCompositeContent((CompositeDeclaration) component);
		}

		if (component instanceof SpecificationDeclaration) {
			generateProperty(obrContent, component, CST.COMPONENT_TYPE,
					CST.SPECIFICATION);
			generateProperty(obrContent, component, CST.SPECNAME,
					component.getName());
		}

		// checks shared, singleton, instantiable, exclusive,
		validator.checkComponentHeader(component);

		// provide clause
		printProvided(obrContent, component);

		// properties
		printProperties(obrContent, component);

        printRelations(obrContent, component);

		// Require, fields and constraints
		printRequire(obrContent, component);

		obrContent.append(BEGIN_P + "maven.groupId" + "' t='" + "string"
				+ ATT_V + this.artifact.getGroupId() + END_P);
		obrContent.append(BEGIN_P + "maven.artifactId" + "' t='" + "string"
				+ ATT_V + this.artifact.getArtifactId() + END_P);
		obrContent.append(BEGIN_P + "maven.version" + "' t='" + "string"
				+ ATT_V + this.artifact.getVersion() + END_P);
		
		obrContent.append(BEGIN_P + "apam.version" + "' t='" + "version"
				+ ATT_V + ApamMavenProperties.mavenVersion.replace('-', '.')
				+ END_P);

        broker.get(component.getReference()).freeze();
		obrContent.append("   </capability>\n");
	}

	private void printProvided(StringBuffer obrContent,	ComponentDeclaration component) {
		if (component instanceof InstanceDeclaration) {
			ImplementationReference<?>.Versioned impl = ((InstanceDeclaration) component).getImplementationVersion();
			if ((impl != null) && !impl.getComponent().getName().isEmpty()) {
				bundleRequiresImplementations.add(impl);
			}
			return;
		}
		Set<UndefinedReference> undefinedMessages = new HashSet<UndefinedReference>();
		Set<UndefinedReference> undefinedInterfaces = new HashSet<UndefinedReference>();

		for (UndefinedReference undefinedReference : component.getProvidedResources(UndefinedReference.class)) {
			if (undefinedReference.isKind(MessageReference.class)) {
				undefinedMessages.add(undefinedReference);
			} else if (undefinedReference.isKind(InterfaceReference.class)) {
				undefinedInterfaces.add(undefinedReference);
			}
		}

		Set<InterfaceReference> interfaces = component.getProvidedResources(InterfaceReference.class);
		for (InterfaceReference ref : interfaces) {
			validator.checkInterfaceExist(ref.getName());
		}
		String val = setReference2String(interfaces);
		if (val != null) {
			generateProperty(obrContent, component, CST.PROVIDE_INTERFACES,
					setReference2String(interfaces));
		}

		Set<MessageReference> messages = component.getProvidedResources(MessageReference.class);
		for (MessageReference ref : messages) {
			validator.checkInterfaceExist(ref.getName());
		}
		val = setReference2String(messages);
		if (val != null) {
			generateProperty(obrContent, component, CST.PROVIDE_MESSAGES, val);
		}
		if (component instanceof ImplementationDeclaration) {
			ImplementationDeclaration impl = (ImplementationDeclaration) component;
			SpecificationReference.Versioned spec = impl.getSpecificationVersion();
			if ((spec != null) && !spec.getComponent().getName().isEmpty()) {
				generateProperty(obrContent, component,CST.PROVIDE_SPECIFICATION, spec.getComponent().getName());
				bundleRequiresSpecifications.add(spec);
				validator.checkImplProvide(component, interfaces, messages, undefinedInterfaces,
						undefinedMessages);
			}
		}
	}

    private void printRelations(StringBuffer obrContent,
                                 ComponentDeclaration component) {
        Set<RelationDeclaration> relations = component.getDependencies();
        if(relations !=null && relations.size()>0) {
            for (RelationDeclaration rel : relations) {
            	
            	/*
            	 * Handle resource references
            	 */
            	ResolvableReference target 	= rel.getTarget();
            	String encodedTarget 		= target.getName();
            	
            	if (target instanceof InterfaceReference) {
            		encodedTarget = "{"+ComponentParser.INTERFACE+"}"+encodedTarget;
            	}
            	else if (target instanceof MessageReference) {
            		encodedTarget = "{"+ComponentParser.MESSAGE+"}"+encodedTarget;
            	}
            	else if (target instanceof PackageReference) {
            		encodedTarget = "{"+ComponentParser.PACKAGE+"}"+encodedTarget;
            	}
            	else if (target instanceof UndefinedReference && target.as(InterfaceReference.class) != null) {
            		encodedTarget = "{"+ComponentParser.INTERFACE+"}";
            	}
            	else if (target instanceof UndefinedReference && target.as(MessageReference.class) != null) {
            		encodedTarget ="{"+ComponentParser.MESSAGE+"}";
            	}
            	
                obrContent.append(BEGIN_P + CST.RELATION_PREFIX+rel.getIdentifier()
                        + ATT_V + encodedTarget + END_P);
            }
        }


    }


	private void printProperties(StringBuffer obrContent,
			ComponentDeclaration component) {
		Map<String, Object> properties = validator.getValidProperties(component);
		for (String attr : properties.keySet()) {
			generateProperty(obrContent, component, attr, properties.get(attr)
					.toString());
		}

		// definition attributes
		List<PropertyDefinition> definitions = component
				.getPropertyDefinitions();
		for (PropertyDefinition definition : definitions) {
			if (validator.checkProperty(component, definition)) {
				generateTypedProperty(obrContent, component,
						CST.DEFINITION_PREFIX + definition.getName(),
						definition.getType(), definition.getDefaultValue());
			}
		}
	}

	/**
	 * provided a set of resources references (interface or messages)
	 * fr.mag....A , B, C references produces a string "[;fr.imag....A;B;C;]"
	 * 
	 * @param refs
	 * @return
	 */

	private String setReference2String(Set<? extends ResolvableReference> refs) {
		if (refs.isEmpty()) {
			return null;
		}
		String val = "";
		for (ResolvableReference mess : refs) {
			val += mess.getName() + ",";
		}
		// remove last ","
		return val.substring(0, val.length() - 1);
	}

	private void printRequire(StringBuffer obrContent, ComponentDeclaration component) {
		// We do not generate dependencies for specification to remain lazy
		// the spec version is mentionned in the implementations that implement
		// that spec.
		if (component instanceof ImplementationDeclaration) {
			for (RelationDeclaration dep : component.getDependencies()) {
				if (dep.getTarget().as(SpecificationReference.class) != null) {
					bundleRequiresSpecifications.add(dep.getTarget().as(SpecificationReference.class).any());
				}
			}
		}

		// all components : checks dependencies and constraints
		validator.checkRelations(component);
	}

	private void generateFilters(StringBuffer obrContent) {
		/**
		 * Generate filters for all provided specifications
		 */
		// VersionRange version ;
		for (ComponentReference<?>.Versioned res : bundleRequiresSpecifications) {
			generateRequire(obrContent, res);
		}
		for (ComponentReference<?>.Versioned res : bundleRequiresImplementations) {
			generateRequire(obrContent, res);
		}
	}

	private void generateProperty(StringBuffer obrContent,
			ComponentDeclaration component, String attr, String value) {

		if (broker.get(component.getName(),component.getProperty(CST.VERSION)).putAttr(attr, value,validator)) {
			obrContent.append(BEGIN_P + attr + ATT_V + value + END_P);
			return;
		}
		// validator.error ("Property " + attr + " already defined for  " +
		// component.getName()) ;
	}

	private void generateTypedProperty(StringBuffer obrContent,
			ComponentDeclaration component, String attr, String type,
			String value) {
		if (value == null) {
			value = "";
		}

		if (broker.get(component.getReference()).putAttr(attr, value, validator)) {
			obrContent.append(BEGIN_P + attr + "' t='" + type + ATT_V + value
					+ END_P);
			return;
		}
		// validator.error ("Property " + attr + " already defined for  " +
		// component.getName()) ;
	}

	private void generateRequire(StringBuffer obrContent, ComponentReference<?>.Versioned required) {
		
		String target 	= required.getComponent().getName();
		String version	= required.getRange();
		
		if (version == null) {
			obrContent
					.append("   <require name='apam-component' filter='(name="
							+ target
							+ ")' extend='false' multiple='false' optional='false'>"
							+ " specification relation toward " + target
							+ "</require>\n");
		} else {
			obrContent
					.append("   <require name='apam-component' filter='(&amp;(name="
							+ target
							+ ")"
							+ filter(version)
							+ ")' extend='false' multiple='false' optional='false'>"
							+ " specification relation toward "
							+ target
							+ "</require>\n");
		}
	}


	private static String filter(String range) {
		try {
			String filter = ApamComponentRepository.filter(range);
			return filter.replace("<", "&lt;").replace(">","&gt;");
		}
		catch(Exception parseException) {
			return "";
		}
	}

	
	private void checkDoubleDefinition() {
		Set<String> defined = new HashSet<String>();
		List<ComponentDeclaration> dcl = new ArrayList<ComponentDeclaration>(
				components);

		/*
		 * Verify in the current project
		 */
		for (ComponentDeclaration comp : dcl) {
			if (defined.contains(comp.getName())) {
				validator.error("In " + comp + ": component " + comp.getName()
						+ " already defined");
				components.remove(comp);
			} else {
				defined.add(comp.getName());
			}
		}

		/*
		 * Verify if the components we are building were not in already
		 * processed in another project in the same built
		 * 
		 * NOTE this causes problems because a lot of information is kept in
		 * static variables that are shared in the same build execution.
		 */
		for (ComponentDeclaration comp : new ArrayList<ComponentDeclaration>(
				components)) {

			ApamCapability existingDefinition = broker.get(comp.getReference());

			/*
			 * never built is OK to process
			 */
			if (existingDefinition == null) {
				continue;
			}

			/*
			 * already built in the repository is OK to process
			 */
			if (!existingDefinition.isFinalized()) {
				continue;
			}
			/*
			 * Already processed in this build execution
			 */
			validator.error("Component " + comp.getName()
					+ " is already defined in another project in this build");
			components.remove(comp);
		}

	}
}
