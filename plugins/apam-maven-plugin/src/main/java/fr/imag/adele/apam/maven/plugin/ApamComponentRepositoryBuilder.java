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

package fr.imag.adele.apam.maven.plugin;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.RepositoryAdmin;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.encoding.capability.CapabilityEncoder;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.declarations.repository.acr.ApamComponentRepository;
import fr.imag.adele.apam.declarations.repository.maven.MavenArtifactRepository;
import fr.imag.adele.apam.declarations.repository.maven.MavenProjectRepository;
import fr.imag.adele.apam.declarations.tools.Reporter;
import fr.imag.adele.apam.declarations.tools.Reporter.Severity;
import fr.imag.adele.apam.maven.plugin.validation.ValidationContext;

/**
 * This class produces the ACR representation of the currently build project.
 * 
 * It includes information regarding maven artifacts, component metadata and inferred
 * capability requirements
 *  
 * @author vega
 *
 */
public class ApamComponentRepositoryBuilder {

	private final RepositoryAdmin 		manager;


	public ApamComponentRepositoryBuilder(ApamComponentRepository repository) {
		this.manager = repository.getManager();
	}
	
	
	public String build(ValidationContext context, MavenProjectRepository project, Reporter reporter) {
		
		if (manager == null) {
			reporter.report(Severity.ERROR, "Cannot generate ACR content, invalid repository manager");
		}

		MavenArtifactRepository buildRepository = project.getBuildRepository();
		
		StringBuilder result = new StringBuilder("<obr>\n");

		/*
		 *  Add capability to represent maven inforamtion
		 */
		Capability mavenCapability = CapabilityEncoder.builder(CST.MAVEN).
										property(CST.GROUP_ID, buildRepository.getArtifact().getGroupId()).
										property(CST.ARTIFACT_ID, buildRepository.getArtifact().getArtifactId()).
										property(CST.VERSION, buildRepository.getArtifact().getVersion()).
											build();

		result.append(manager.getHelper().writeCapability(mavenCapability)).append("\n");

	
		/*
		 *  Add capabilities to represent component metadata
		 */
		CapabilityEncoder encoder 				= new CapabilityEncoder();
		Set<ComponentReference<?>> processed	= new HashSet<ComponentReference<?>>();
		Set<VersionedReference<?>> referenced			= new HashSet<VersionedReference<?>>();

		for (ComponentDeclaration component : buildRepository.getComponents()) {
			
			if (processed.contains(component.getReference())) {
				reporter.report(Severity.WARNING,"Component " + component.getName() + " already defined in this build, declaration ignored "+component);
				continue;
			}

			/*
			 * ACR repository is used at runtime by OBR Manager resolver. It uses direct searches over the
			 * properties of the component that ignore the property inheritance mechanisms of APAM.
			 * 
			 * We need to simulate inheritance by copying properties from the ancestor declarations.
			 * 
			 * NOTE notice that we create and modify an effective cloned declaration, we must be careful
			 * not to modify the original declaration in the project repository
			 * 
			 */
			ComponentDeclaration group 		= context.getComponent(component.getGroupVersioned());
			ComponentDeclaration effective	= component.getEffectiveDeclaration(group);
			
			
			/*
			 * Name property is not inherited directly, but renamed at each level
			 */
			ComponentDeclaration level = component;
			while (level != null) {


				if (level instanceof SpecificationDeclaration) {
					effective.getProperties().put(CST.SPECNAME, level.getName());
				}

				if (level instanceof ImplementationDeclaration) {
					effective.getProperties().put(CST.IMPLNAME, level.getName());
				}

				if (level instanceof InstanceDeclaration) {
					effective.getProperties().put(CST.INSTNAME, level.getName());
				}
				
				level = context.getComponent(level.getGroupVersioned());
			}
			
			/*
			 * For unvalued properties we add default values, this allows filter to be evaluated even if no value
			 * is explicitly specified
			 */
			for (PropertyDefinition property : effective.getPropertyDefinitions()) {
				
				String defaultValue = property.getDefaultValue();
				String value		= effective.getProperty(property.getName());
				
				if (value == null && defaultValue != null) {
					effective.getProperties().put(property.getName(),defaultValue);
				}
			}
			
			/*
			 * Encode the effective declaration
			 */
			Capability encodedCapability	= encoder.encode(effective,reporter);
			if (encodedCapability != null) {
				result.append(manager.getHelper().writeCapability(encodedCapability)).append("\n");
				processed.add(component.getReference());
				
				/*
				 * Update the list of referenced components
				 */
				if (component.getGroupVersioned() != null)
					referenced.add(component.getGroupVersioned());
				
				if (component instanceof ImplementationDeclaration) {
					for (RelationDeclaration relation : component.getRelations()) {
						if (relation.getTarget().as(SpecificationReference.class) != null) {
							referenced.add(VersionedReference.any(relation.getTarget().as(SpecificationReference.class)));
						}
					}
				}

			}
		}

		/*
		 * Generate requirement for referenced components. In this way we can use the OBR resolver to install
		 * transitive dependencies 
		 */
		for (VersionedReference<?> reference : referenced) {
			result.append(manager.getHelper().writeRequirement(CapabilityEncoder.requirement(reference))).append("\n");
		}

		result.append("</obr> \n");
		
		return result.toString();
	}
	
}
