package fr.imag.adele.apam.declarations;

import fr.imag.adele.apam.declarations.references.Reference;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;

/**
 * A reference to a feature of a component declaration.
 * 
 * Notice that we use the component as a name space, then feature identifiers must be
 * only unique in the context of their defining component declaration.
 * 
 * @author vega
 *
 */
public abstract class FeatureReference extends Reference {

	private final String identifier;

	protected FeatureReference(ComponentReference<?> definingComponent, String identifier) {
		super(definingComponent);
		this.identifier = identifier;
	}

	public ComponentReference<?> getDeclaringComponent() {
		return (ComponentReference<?>) namespace;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

}
