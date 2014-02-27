package fr.imag.adele.obrMan.internal;

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
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
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;

/**
 * This class represents an APAM component that can be deployed from a bundle repository, 
 * and searched by its associated metadata.
 */
public class DeployableComponent {

	private final static Logger 		logger = LoggerFactory.getLogger(DeployableComponent.class);

	private final OBRMan 				manager;

	private final Resource 				resource;
	private final Capability			metadata;
	private final ComponentReference<?> component;
	
	private final int 					hashCode;



	public DeployableComponent(OBRMan manager, Resource resource, Capability capability) {
		
		assert capability.getName().equals(CST.CAPABILITY_COMPONENT);

		this.manager	= manager;
		this.resource 	= resource;
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
		 * If the component exists, maybe arrived in the mean time, or from
		 * another bundle or in another version, do nothing
		 */
		Component result = CST.componentBroker.getComponent(component.getName());
		if (result != null)
			return result;
		
		/*
		 * Check if the bundle is already existing in the platform
		 */
		Bundle theBundle = manager.getBundle(resource);

		/*
		 * Normal case : bundle does not exist : deploy, wait and return the
		 * component
		 */
		if (theBundle == null) {
			return deploy(context);
		}

		boolean alreadyDeployed = false;
		
		/*
		 * the bundle is already deployed and active : it is not the version we
		 * are looking for. Do nothing and return false It may be active or
		 * starting if updated in parallel by another thread ... OK wait for the
		 * component and return it.
		 */
		if (theBundle.getState() == Bundle.ACTIVE || theBundle.getState() == Bundle.STARTING) {
			alreadyDeployed = theBundle.getVersion().equals(resource.getVersion());
			if (!alreadyDeployed) {
				logger.error("Bundle " + resource.getSymbolicName() + " is already installed under version " + theBundle.getVersion() + " while trying to deploy version " + resource.getVersion());
			}
		}

		/*
		 * the bundle is installed but is not started : try to start it, wait
		 * for the component and return it, if failed, return null
		 */
		if (theBundle.getState() == Bundle.INSTALLED || theBundle.getState() == Bundle.RESOLVED) {
			try {
				theBundle.start();
			} catch (BundleException e) {
				// Starting failed. No solution : cannot be deployed and cannot
				// be started. Make as if failed
				logger.info("The bundle " + theBundle.getSymbolicName() + " is installed but cannot be started!");
				alreadyDeployed = false;
			}

			logger.info("The bundle " + theBundle.getSymbolicName() + " is installed and has been be started!");
			alreadyDeployed = true; 
		}

		
		return alreadyDeployed ? CST.componentBroker.getWaitComponent(component.getName(), 10*1000 /* milliseconds*/) : null;
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
			 * calculate the transitive dependencies to satisfy requirements of the resource being
			 * deployed
			 */
			resolver.add(this.resource);
			resolver.resolve();

			/*
			 * If we could not resolve the resource requirements, just give up completely
			 */
			Reason[] missingRequirements = resolver.getUnsatisfiedRequirements();
			
			if (missingRequirements.length > 0) {
				
				logger.error("Unable to deploy resource: " + resource);
				for (Reason missingRequirement : missingRequirements) {
					logger.error("	unsatified requirement: " + missingRequirement.getRequirement());
				}
				
				return null;
			}

			/* Try to perform the actual installation. Concurrent bundle deployment may interfere and
			 * change the state of the local repository, which produces an IllegalStateException of
			 * the resolver. 
			 * 
			 * We should try again the resolution and installation process, in the new context.
			 * 
			 */
			try {
				
				resolver.deploy(Resolver.START);
				retrying = false;
				deployed = true;
				
			} catch (IllegalStateException e) {
				
				logger.debug("OBR changed state. Resolving again " + this.resource.getSymbolicName());
				retrying = true;
				
			} catch (Exception e) {
				
				logger.error ("Deployment of " + component + " from "+resource+"failed.") ;
				retrying = false;
				deployed = false;
			}

		} while (retrying);

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
	 * Utility methods to manipulate metadata in capabilities
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