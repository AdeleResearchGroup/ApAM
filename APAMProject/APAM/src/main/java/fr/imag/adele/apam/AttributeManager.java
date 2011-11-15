package fr.imag.adele.apam;
/**
 * called when attributes of an ASM object is changed
 * @author Jacky
 *
 */
public interface AttributeManager {
	public boolean attrInstChanged (Instance inst, String attr, Object newValue) ;
	public boolean attrInstRemoved (Instance inst, String attr, Object oldValue) ;
	public boolean attrInstAdded   (Instance inst, String attr, Object value) ;

	public boolean attrImplChanged (Implementation impl, String attr, Object newValue) ;
	public boolean attrImplRemoved (Implementation impl, String attr, Object oldValue) ;
	public boolean attrImplAdded   (Implementation impl, String attr, Object value) ;

	public boolean attrSpecChanged (Specification spec, String attr, Object newValue) ;
	public boolean attrSpecRemoved (Specification spec, String attr, Object oldValue) ;
	public boolean attrSpecAdded   (Specification spec, String attr, Object value) ;
}
