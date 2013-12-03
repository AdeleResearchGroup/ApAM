package fr.imag.adele.apam;

public interface Manager {

	/**
	 * 
	 * @return the name of that manager.
	 */
	public String getName();

	/**
	 * returns the relative priority of that manager, for the resolution
	 * algorithm
	 * 
	 * @return
	 */
	public int getPriority();

	/**
	 * A new composite, holding a model managed by this manager, has been
	 * created. The manager is supposed to read and interpret that model.
	 * 
	 * @param model
	 *            the model.
	 * @param composite
	 *            the new composite (or appli)
	 */
	public void newComposite(ManagerModel model, CompositeType composite);

}
