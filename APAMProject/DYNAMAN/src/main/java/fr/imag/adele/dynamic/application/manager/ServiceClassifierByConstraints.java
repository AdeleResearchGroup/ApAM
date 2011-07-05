package fr.imag.adele.dynamic.application.manager;

import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.query.QueryLDAPImpl;
import fr.imag.adele.apam.ASMImpl.ASMInstImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.sam.Instance;

/**
 * A representation of the equivalence class of all services satisfying a given set of constraints.
 *  
 * @author vega
 *
 */
public class ServiceClassifierByConstraints extends ServiceClassifier {

	
	/**
	 * The set of constraints
	 */
	private final Set<Filter> constraints;
	
	
	public ServiceClassifierByConstraints(Set<Filter> constraints) {
		assert constraints != null;
		
		this.constraints = constraints;
	}
	

	/**
	 * Whether the service satisfies all constraints associated to this class
	 */
	public @Override boolean contains(ASMInst instance) {
		
		/*
		 * Iterate over all constraints
		 */
		for (Filter constraint : constraints) {
			if (!constraint.match((ASMInstImpl)instance))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Whether the service satisfies all constraints associated to this class
	 */
	
	public @Override boolean contains(Instance instance) {
		/*
		 * Iterate over all constraints
		 */
		for (Filter constraint : constraints) {
			try {
				
				if (!instance.match(new QueryLDAPImpl(constraint.toString())))
					return false;
				
			} catch (InvalidSyntaxException ignored) {
			}
		}
		
		return true;
	}

	/**
	 * Equivalence of this classifier is based on the set of constraints
	 * 
	 * NOTE there is no way to know if two expressions are equivalent , we use
	 * exact syntactic matching
	 */
	public @Override boolean equals(Object object) {
		
		if (object == null)
			return false;
		
		if (!(object instanceof ServiceClassifierByConstraints ))
			return false;
		
		ServiceClassifierByConstraints that = (ServiceClassifierByConstraints)object;
		
		return this.constraints.equals(that.constraints);
	}
	
	/**
	 * Adjust hash code to be consistent to the equality definition
	 */
	public @Override int hashCode() {
		return this.constraints.hashCode();
	}


}
