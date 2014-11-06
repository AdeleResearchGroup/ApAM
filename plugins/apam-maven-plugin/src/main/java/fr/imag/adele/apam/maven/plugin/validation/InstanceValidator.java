package fr.imag.adele.apam.maven.plugin.validation;

import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.repository.maven.Classpath;

public class InstanceValidator extends ComponentValidator<InstanceDeclaration> {

	private final ConstrainedReferenceValidator<ConstrainedReference> triggerValidator;

	
	public InstanceValidator(CompositeValidator parent) {
		super(parent);
		this.triggerValidator = new ConstrainedReferenceValidator<ConstrainedReference>(this);
	}
	
	public InstanceValidator(ValidationContext context, Classpath classpath) {
		super(context, classpath);
		this.triggerValidator = new ConstrainedReferenceValidator<ConstrainedReference>(this);
	}

	@Override
	public Void validate(InstanceDeclaration component) {
		
		Void result = super.validate(component);
		
		validateTriggers();
		return result;
	}

	private void validateTriggers() {
		for (ConstrainedReference trigger : getComponent().getTriggers()) {
			validate(trigger,triggerValidator);
		}
	}
	
	@Override
	protected ImplementationDeclaration getGroup() {
		return (ImplementationDeclaration) super.getGroup();
	}

}
