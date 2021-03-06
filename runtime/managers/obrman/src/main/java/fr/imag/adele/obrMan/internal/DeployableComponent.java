package fr.imag.adele.obrMan.internal;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.InstanceReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;

/**
 * This class represents an APAM component that can be deployed from a bundle repository, 
 * and searched by its associated metadata.
 */
public class DeployableComponent {

	private final static Logger 		logger = LoggerFactory.getLogger(DeployableComponent.class);

	private final OBRMan 				manager;

	private final String				repository;
	
	private final Resource 				resource;
	private final Capability			metadata;
	private final ComponentReference<?> component;
	
	private final int 					hashCode;



	public DeployableComponent(Repository repository, OBRMan manager, Resource resource, Capability capability) {
		
		assert capability.getName().equals(CST.CAPABILITY_COMPONENT);

		this.manager	= manager;
		this.resource 	= resource;
		
		this.repository	= repository.getURI();
		this.metadata	= capability;

		this.component	= getComponent(metadata);
		
		this.hashCode	= this.resource.getURI().hashCode()^
						  this.component.getName().hashCode();

	}
	
	/**
	 * The component in this deployment unit
	 */
	public ComponentReference<?> getReference() {
		return component;
	}

	/**
	 * The list of components within the same deployment unit as this component.
	 * 
	 * A deployable component can be packaged in the same deployment unit along with other components.
	 * Installing or updating it then can have some side-effects on other components.
	 */
	public Set<String> getDeploymentUnitComponents() {
		Set<String> resourceComponents = new HashSet<String>();
		
		for (Capability capability : resource.getCapabilities()) {
			if (capability.getName().equals(CST.CAPABILITY_COMPONENT)) {
				Object name = capability.getPropertiesAsMap().get(CST.NAME);
				if (name != null)
					resourceComponents.add(name.toString());
			}
		}
		
		return resourceComponents;
	}

	/**
	 * Whether this component satisfies the specified requirement, the requirement must concern the
	 * component metadata in the repository 
	 */
	public boolean satisfies(Requirement requirement) {
		assert requirement.getName().equals(CST.CAPABILITY_COMPONENT);
		return requirement.isSatisfied(metadata);
			
	}

	/**
	 * Whether this component satisfies the specified filter, the filter must concern the component 
	 * metadata in the repository
	 * 
	 * TODO Currently there seems to be a problem with repository requirements as superset and subset 
	 * operators used by APAM doesn't appear to work. Needs further investigation, just using APAM
	 * filter directly by now
	 *
	 */
	public boolean satisfies(ApamFilter filter) {
		return filter.matchCase(metadata.getPropertiesAsMap());
	}


	/**
	 * Whether this component satisfies requirement specified in the given relation.
	 * 
	 * TODO NOTE Currently, because of filter substitutions,  we can not translate the APAM constraints
	 * into repository requirements, and use a single search/resolution mechanism.
	 * 
	 * An alternative implementation of substitution is to evaluate variables in the context of the
	 * source when the RelToResolve object is created, and to translate the constraints and preferences
	 * into normal ldap filters that can be used with standard OSGi services.
	 */
	@SuppressWarnings("unchecked")
	public boolean satisfies(RelToResolve relation) {
		return relation.matchRelationConstraints(this.getReference().getKind(), (Map<String,Object>) metadata.getPropertiesAsMap());
	}
	
	/**
	 * The ranking of this component as a candidate to satisfy the given relation
	 */
	@SuppressWarnings("unchecked")
	public int ranking(RelToResolve relation) {
		return relation.ranking(this.getReference().getKind(), (Map<String,Object>) metadata.getPropertiesAsMap());
	}


	/**
	 * The version of this component
	 */
	public Version getVersion() {
		Object version = this.metadata.getPropertiesAsMap().get(Resource.VERSION);
		return (version != null) && (version instanceof Version) ? (Version) version : Version.emptyVersion;
	}

	/**
	 * Whether the receiver represents a version of the specified component 
	 */
	public boolean isVersionOf(DeployableComponent that) {
		return this.component.equals(that.component);
	}

	/**
	 * Whether the receiver represents a preferred version of the specified component 
	 * 
	 * The preferred version has the higher version number, and in case both deployable 
	 * units have the same version we prefer the resource providing more capabilities
	 */
	public boolean isPreferedVersionThan(DeployableComponent that) {
		
		assert this.isVersionOf(that);
		
		int deltaVersion =	this.getVersion().compareTo(that.getVersion());
		
		return 	deltaVersion > 0 || 
				(deltaVersion == 0 && this.resource.getCapabilities().length > that.resource.getCapabilities().length);
		
	}

	/**
	 * Installs the component in the platform using the specified context to resolve requirements, and waits
	 * until it is reified in APAM.
	 * 
	 * Several cases must be considered as installations can proceed in parallel in several threads, we try
	 * to avoid starting several deployment unnecessarily.
	 */
	public Component install(OBRManager context) {

		/*
		 * If the component exists, maybe arrived in the mean time, or from another bundle or in another version, do nothing
		 */
		Component result = CST.componentBroker.getComponent(component.getName());
		if (result != null)
			return result;
		
		/*
		 * Check if the bundle is already existing in the platform
		 */
		Bundle installed = getBundle(resource);

		return  installed != null ? CST.componentBroker.getWaitComponent(component.getName(), 10*1000 /* milliseconds*/) : deploy(context);

	}

	/**
	 * Get the installed bundle corresponding to the specified resource. Activates the bundle if necessary.
	 */
	private final Bundle getBundle(Resource resource) {

		/*
		 * Get the platform bundle with the same symbolic name, if any
		 */
		Bundle bundle = manager.getBundle(resource);
		
		if (bundle == null) {
			return null;
		}

		/*
		 * the bundle is installed but is not started : try to start it before checking version numbers to be sure that any updates are finished
		 */
		if (bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED) {
			try {
				bundle.start();
				logger.info("The bundle " + bundle.getSymbolicName() + " is installed and has been started!");
			} catch (BundleException e) {
				logger.info("The bundle " + bundle.getSymbolicName() + " is installed but cannot be started!");
			}

		}

		/*
		 * Verify the version, notice that there can be only a single version of a bundle in the platform, so if we find another installed version,
		 * there may be conflicting updates in progress.
		 * 
		 */
		boolean matchVersion 	= bundle.getVersion().equals(resource.getVersion());
		boolean active			= (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING);

		if (!matchVersion) {
			logger.error("Bundle " + resource.getSymbolicName() + " is already installed under version " + bundle.getVersion() + " while trying to deploy version " + resource.getVersion());
		}


		
		return active && matchVersion ? bundle : null;

	}

	/**
	 * Deploy this resource on the platform and waits for the component to be reified in APAM.
	 * 
	 * Return null if deployment is not possible.
	 * 
	 */
	private Component deploy(OBRManager context) {
		
		Resolver resolver = manager.getResolver(context);

		/*
		 * Try to perform requirement resolution and deployment. This may need to be retried several times
		 * as other installations can be ongoing in parallel, and the resource requirement resolution must
		 * be recalculated in the new resource context.
		 */
		boolean deployed = false;
		boolean retrying = true;
		
		do {

			/*
			 * calculate the transitive dependencies to satisfy requirements of the resource being deployed
			 */
			resolver.add(this.resource);
			resolver.resolve();

			/*
			 * If we can not resolve the resource requirements, just give up
			 */
			if (resolver.getUnsatisfiedRequirements().length > 0) {
				
				retrying = false;
				deployed = false;
				
				continue;
			}

			/* 
			 * Perform the actual installation, 
			 */
			try {

				resolver.deploy(Resolver.START+Resolver.NO_OPTIONAL_RESOURCES);

				retrying = false;
				deployed = true;
				
				/*
				 * Verify that all required resources were actually installed. A resource may not be installed if a 
				 * concurrent deployment has installed a conflicting version.
				 * 
				 * In this case there is not much we can do, we just give up the deployment
				 */
				List<Resource> deployedResources = new ArrayList<Resource>();
				
				deployedResources.addAll(Arrays.asList(resolver.getAddedResources()));
				deployedResources.addAll(Arrays.asList(resolver.getRequiredResources()));
				

				for (Resource deployedResource : deployedResources) {
					if (getBundle(deployedResource) == null)
						deployed = false;
				}

				
			} 
			
			/* 
			 * Concurrent bundle deployment may interfere with installation and change the state of the local repository,
			 * which produces an IllegalStateException of the resolver. 
			 * 
			 * We can recover by trying again the resolution and installation process, using the new local bundles.
			 * 
			 */
			catch (IllegalStateException e) {
				
				logger.debug("OBR changed state. Resolving again " + this.resource.getSymbolicName());
				
				retrying = true;
				deployed = false;
			} 

			/* 
			 * If we can not recover from the error, just give up
			 * 
			 */
			catch (Exception e) {
				
				logger.error ("Deployment of " + component + " from "+resource+" failed.",e) ;
				
				retrying = false;
				deployed = false;
			}

		} while (retrying);

		
		/*
		 * Log deployment result 
		 */

		logger.debug("Component: " + component+ (deployed ? " successfully " : " not ") + "deployed");
		logger.debug("	repository: " + repository);
		
		List<Resource> deployedResources = new ArrayList<Resource>();
		
		deployedResources.addAll(Arrays.asList(resolver.getAddedResources()));
		deployedResources.addAll(Arrays.asList(resolver.getRequiredResources()));
		
		for (Resource deployedResource : deployedResources) {
			logger.debug("	required resource: " + deployedResource+(getBundle(deployedResource) != null ? " (installed) ":" (not installed) "));
			logger.debug("		location: " + deployedResource.getURI());
			
			Reason[] reasons = resolver.getReason(deployedResource);
			for(Reason reason : reasons != null ? reasons : new Reason[0]) {
				logger.debug("		satisfies requirement: " + reason.getRequirement()+" of "+reason.getResource());
			}
		}

		for (Reason missingRequirement : resolver.getUnsatisfiedRequirements()) {
			logger.error("	unsatisfied requirement: " + missingRequirement.getRequirement());
		}

		return deployed ? CST.componentBroker.getWaitComponent(component.getName(), 10*1000 /* milliseconds*/) : null;
	} 


	/**
	 * Whether this unit represents the repository version of an installed component in the platform.
	 * 
	 * This is useful to determine if the installed component can be updated from this unit, notice that
	 * to be considered versions of each other the component name AND the bundle symbolic name must match.
	 * Otherwise, component packaging has changed, and we can not perform an update.
	 * 
	 */
	public boolean isRepositoryVersionOf(Component component) {
		return	component.getDeclaration().getReference().equals(this.component) &&
				component.getApformComponent().getBundle().getSymbolicName().equals(this.resource.getSymbolicName());
	}

	/**
	 * Updates the currently deployed component from this repository version
	 */
	public void update(OBRManager context, Component component) throws Exception  {
		
		assert this.isRepositoryVersionOf(component);

		URL updateLocation = (new URI(resource.getURI())).toURL();
		component.getApformComponent().getBundle().update(updateLocation.openStream());
	}

	
	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		
		if (object == null)
			return false;
		
		if (! (object instanceof DeployableComponent))
			return false;
		
		DeployableComponent that = (DeployableComponent) object;

		return	this.resource.getURI().equals(that.resource.getURI()) &&
				this.component.equals(that.component);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return this.component.getName()+"["+getVersion()+"] @ "+resource.getURI();
	}
	
	
	/*
	 * Utility methods to manipulate APAM metadata in capabilities
	 */
	
	private final static ComponentReference<?> getComponent(Capability metadata) {
		
		String componentName 	= getAttributeInCapability(metadata, CST.NAME);
		String componentkind	= getAttributeInCapability(metadata, CST.COMPONENT_TYPE);
		
		if (CST.SPECIFICATION.equals(componentkind)) {
			return new SpecificationReference(componentName);
		}
		
		if (CST.IMPLEMENTATION.equals(componentkind)) {
			return new ImplementationReference<ImplementationDeclaration>(componentName);
		}
		
		if (CST.INSTANCE.equals(componentkind)) {
			return new InstanceReference(componentName);
		}
		
		return new ComponentReference<ComponentDeclaration>(componentName);
	}
	
	
	private final static String getAttributeInCapability(Capability aCap, String attr) {
		return (String) (aCap.getPropertiesAsMap().get(attr));
	}


}