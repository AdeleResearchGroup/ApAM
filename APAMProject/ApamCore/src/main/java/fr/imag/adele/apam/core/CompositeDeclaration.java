package fr.imag.adele.apam.core;


/**
 * This class represents the declaration of a composite implementation
 * 
 * @author vega
 *
 */
public class CompositeDeclaration extends ImplementationDeclaration {

    /**
     * The main implementation of the composite
     */
    private final ImplementationReference<?> mainImplementation;


    public CompositeDeclaration(String name, SpecificationReference specification, ImplementationReference<?> mainImplementation) {
        super(name, specification);

        assert mainImplementation != null;

        this.mainImplementation = mainImplementation;
    }

	/**
	 * A reference to a composite implementation
	 */
    private static class Reference extends ImplementationReference<CompositeDeclaration> {

		public Reference(String name) {
			super(name);
		}

	}

    /**
     * Generates the reference to this implementation
     */
    @Override
    protected ImplementationReference<CompositeDeclaration> generateReference() {
    	return new Reference(getName());
    }

    /**
     * Get the main implementation
     */
    public ImplementationReference<?> getMainImplementation() {
        return mainImplementation;
    }

 
    @Override
    public String toString() {
        String ret = "\nComposite declaration " + super.toString();
        ret += "\n   Main Implementation: " + mainImplementation.getIdentifier();
        return ret;
    }

}
