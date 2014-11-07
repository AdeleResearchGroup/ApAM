package fr.imag.adele.apam.declarations.instrumentation;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;

/**
 * An injected field declaration
 * 
 */
public abstract class InjectedField extends Instrumentation {

	protected final String field;

	private final Lazy<String> fieldType = new Lazy<String>() {
		@Override
		protected String evaluate(InstrumentedClass instrumentedClass) {
			try {
				return instrumentedClass.getFieldType(field);
			} catch (NoSuchFieldException e) {
				return null;
			}
		};

	};

	private final Lazy<Boolean> fieldMultiplicity = new Lazy<Boolean>() {
		@Override
		protected Boolean evaluate(InstrumentedClass instrumentedClass) {
			try {
				return instrumentedClass.isCollectionField(field);
			} catch (NoSuchFieldException e) {
				return false;
			}
		};

	};

	protected InjectedField(AtomicImplementationDeclaration implementation,	String field) {
		super(implementation.getReference(), implementation.getImplementationClass());
		this.field = field;
	}


	@Override
	public String getName() {
		return field;
	}

	public ResourceReference getType() {
		String target = fieldType.get();
		return target != null && !target.equals(InstrumentedClass.UNKNOWN_TYPE)? generateReference(target) : new UnknownReference(generateReference(this.toString()));
	}

	public boolean isCollection() {
		return fieldMultiplicity.get();
	}
	
	/**
	 * Generates a new reference of the appropriate class for the specified, required resource 
	 */
	protected abstract ResourceReference generateReference(String type);
	
	@Override
	public boolean isValidInstrumentation() {
		return fieldType.get() != null;
	}

	@Override
	public String toString() {
		return "field " + getName();
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		
		if (this == object)
			return true;
		
		if (object == null)
			return false;
		
		if (!(object instanceof InjectedField))
			return false;
		
		InjectedField that = (InjectedField) object;
		return this.field.equals(that.field);
	}
}