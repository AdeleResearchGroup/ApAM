package fr.imag.adele.apam;
/**
 * called when attributes of an Apam object is changed.
 * Defines the manager class "properties".
 * @author Jacky
 *
 */
public interface PropertyManager {
	/**
	 * The attribute "attr" has been modified.
	 * @param component. The component (Spec, Implem, Instance) holding that attribute.
	 * @param attr. The attribute name.
	 * @param newValue. The new value of that attribute.
	 * @param oldValue. The previous value of that attribute.
	 */
	public void attributeChanged (Component component, String attr, String newValue, String oldValue) ;

	/**
	 * The attribute "attr" has been removed.
	 * @param component. The component (Spec, Implem, Instance) holding that attribute.
	 * @param attr. The attribute name.
	 * @param oldValue. The previous value of that attribute.
	 */
	public void attributeRemoved (Component component, String attr, String oldValue) ;

	/**
	 * The attribute "attr" has been added (instantiated for the first time).
	 * @param component. The component (Spec, Implem, Instance) holding that attribute.
	 * @param attr. The attribute name.
	 * @param newValue. The new value of that attribute.
	 */
	public void attributeAdded   (Component component, String attr, String newValue) ;
}
