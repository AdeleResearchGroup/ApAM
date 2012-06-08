package fr.imag.adele.apam;
/**
 * called when attributes of an ASM object is changed
 * @author Jacky
 *
 */
public interface AttributeManager {
	public boolean attributeChanged (Instance inst, String attr, Object newValue) ;
	public boolean attributeRemoved (Instance inst, String attr, Object oldValue) ;
	public boolean attributeAdded   (Instance inst, String attr, Object value) ;

	public boolean attributeChanged (Implementation impl, String attr, Object newValue) ;
	public boolean attributeRemoved (Implementation impl, String attr, Object oldValue) ;
	public boolean attributeAdded   (Implementation impl, String attr, Object value) ;

	public boolean attributeChanged (Specification spec, String attr, Object newValue) ;
	public boolean attributeRemoved (Specification spec, String attr, Object oldValue) ;
	public boolean attributeAdded   (Specification spec, String attr, Object value) ;
}
