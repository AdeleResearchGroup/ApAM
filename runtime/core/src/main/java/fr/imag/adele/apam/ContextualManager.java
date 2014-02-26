package fr.imag.adele.apam;

/**
 * This interface is a marker to identify managers whose behavior depends on the type of
 * the composite in which the event handled by the manager is originated.
 * 
 * A Manager specific model can be associated with each composite type to parameterize the
 * manager's behavior in this context.
 * 
 * @author vega
 *
 */
public interface ContextualManager extends Manager {

	/**
	 * A new composite context has been created in APAM, the manager is prevented so it
	 * may initialize its internal structures.
	 * 
	 * The manager can get access to its associated model in the new context using the
	 * method {@link CompositeType#getModel(ContextualManager)}
	 * 
	 * @param composite   the new composite
	 */
	public void initializeContext(CompositeType composite);

}
