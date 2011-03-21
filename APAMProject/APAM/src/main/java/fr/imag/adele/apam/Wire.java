package fr.imag.adele.apam;


import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.samAPIImpl.ASMInstImpl;

public class Wire {
	ASMInst source ;
	ASMInst destination ;
	String depName ;
	Set<Filter> constraints = new HashSet<Filter>() ;
	
	public Wire (ASMInst from, ASMInst to, String depName, Set<Filter> constraints) {
		if (checkNewWire (from, to)) {
			this.source = from ;
			this.destination = to ;
			this.depName = depName ;
			this.constraints =constraints ;
			((ASMInstImpl)from).setWire(to, this) ;
			((ASMInstImpl)to).setInvWire (from, this) ;
		}
	}

	public Wire (ASMInst from, ASMInst to, String depName, Filter filter) {
		if (checkNewWire (from, to)) {
			this.constraints.add(filter) ;
			this.source = from ;
			this.destination = to ;
			this.depName = depName ;
			((ASMInstImpl)from).setWire(to, this) ;
			((ASMInstImpl)to).setInvWire (from, this) ;
			//from.setWire(to, depName, constraints)  ;
		}
	}

	/**
	 * Check if this new wire is consistent or not.
	 * Check the shared property,
	 * Check if composite are correctly set.
	 * Checks if the wire is already existing.
	 * @param from
	 * @param to
	 * @return
	 */
	public static boolean checkNewWire (ASMInst from, ASMInst to) {
		if ((from.getComposite() == null) || (to.getComposite() == null)) {
			System.out.println("erreur : Composite not present in instance.");
			return false ;
		}
		try {
			switch (to.getShared()) {
			case ASM.PRIVATE :
				if (to.getWires() == null) return true ;
				break ;
			case ASM.LOCAL :
				if (from.getComposite() == to.getComposite()) return true ;
				break ;
			case ASM.APPLI :
				if (from.getComposite() == to.getComposite()) return true ; 
				if (from.getComposite().dependsOn(to.getComposite())) return true ; 
				break ;
			case ASM.SHAREABLE : //we suppose that it is in the shareable list.
				return true ;
			}
			//System.out.println("prohibited wire between " + from + " and " + to);
			return false ;
		} catch (Exception e) {} 
		return false ;
	}

	public ASMInst getSource () {
		return source ;
	}
	
	public ASMInst getDestination () {
		return destination ;
	}
	
	public String getDepName () {
		return depName ;
	}
	
	public Set<Filter> getConstraints () {
		return new HashSet<Filter>(constraints) ;
	}

	public void setConstraints (Set<Filter> constraints) {
		this.constraints = constraints ;
	}

	public void addFilter (Filter filter) {
		this.constraints.add(filter);
	}	
	
	public void removeFilter (Filter filter) {
		this.constraints.remove(filter);
	}
	
}
