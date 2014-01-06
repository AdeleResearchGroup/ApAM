package fr.imag.adele.apam.declarations;

/**
 * The declaration of an static wire
 * 
 * @author vega
 * 
 */
public class LinkDeclaration {

	private final ComponentReference<?> source;

	private final ComponentReference<?> target;

	private final String identifier;

	public LinkDeclaration(String identifier, ComponentReference<?> source,
			ComponentReference<?> target) {
		this.source = source;
		this.identifier = identifier;
		this.target = target;
	}

	public String getIdentifier() {
		return identifier;
	}

	public ComponentReference<?> getSource() {
		return source;
	}

	public ComponentReference<?> getTarget() {
		return target;
	}
}
