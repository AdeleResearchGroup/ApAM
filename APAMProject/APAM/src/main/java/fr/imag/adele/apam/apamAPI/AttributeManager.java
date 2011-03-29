package fr.imag.adele.apam.apamAPI;
/**
 * called when attributes of an ASM object is changed
 * @author Jacky
 *
 */
public interface AttributeManager {
	public boolean attrInstChanged (ASMInst inst, String attr, Object newValue) ;
	public boolean attrInstRemoved (ASMInst inst, String attr, Object oldValue) ;
	public boolean attrInstAdded   (ASMInst inst, String attr, Object value) ;

	public boolean attrImplChanged (ASMImpl impl, String attr, Object newValue) ;
	public boolean attrImplRemoved (ASMImpl impl, String attr, Object oldValue) ;
	public boolean attrImplAdded   (ASMImpl impl, String attr, Object value) ;

	public boolean attrSpecChanged (ASMSpec spec, String attr, Object newValue) ;
	public boolean attrSpecRemoved (ASMSpec spec, String attr, Object oldValue) ;
	public boolean attrSpecAdded   (ASMSpec spec, String attr, Object value) ;
}
