package fr.imag.adele.apam.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.DependencyDeclaration;

public class Select {
	
	/**
	 * 
	 * @param impls
	 * @param insts Set of valid instance. Cannot be empty nor null.
	 * @param dependency
	 */
	@SuppressWarnings("unchecked") 
	public static Instance selectBestInstance (Set<Implementation> impls, Set<Instance> insts, DependencyDeclaration dependency) {
		assert (!insts.isEmpty()) ;

		/*
		 * No choice, take that instance; and return its implem, even if not visible.
		 */
		if (insts.size() == 1) {
			return insts.iterator().next();
		}
		
		List<Filter> implPreference = Util.toFilterList(dependency.getImplementationPreferences()) ;
		List<Filter> instPreference = Util.toFilterList(dependency.getInstancePreferences()) ;
		Set<Filter> instConstraints = Util.toFilter    (dependency.getInstanceConstraints()) ;
		
		/*
		 * If no implem or no implem preference, select the best instance, and return its implem, visible or not.
		 */
		if (implPreference == null || implPreference.isEmpty() 
				|| impls==null || impls.isEmpty()) {
			return getPrefered(insts, instPreference) ;
		}
		/*
		 * Try to see if the best implem has existing instances. If not, only consider the instances.
		 */
		Implementation impl = getPrefered(impls, implPreference) ;
		Set<Instance> selectedInsts = (Set<Instance>)getConstraintsPreferedComponent(impl.getMembers(), instPreference, instConstraints) ;
		//if existing, take an instance of this implem.
		if (selectedInsts!= null &&  !selectedInsts.isEmpty()) 
			return getPrefered(selectedInsts, instPreference) ;
		else 
			return getPrefered(insts, instPreference) ;
	}

	
    /**
     * Return the sub-set of candidates that satisfy all the constraints
     * @param <T>
     * @param candidates
     * @param constraints
     * @return
     */
    public static <T extends Component> Set<T> getConstraintsComponents(Set<T> candidates, Set<Filter> constraints) {
		if (constraints == null || constraints.isEmpty()) return candidates;
        if (candidates == null) return null ;

        Set<T> ret = new HashSet <T> () ;
        for (T c : candidates) {
            if (c.match(constraints)) {
                ret.add (c) ;
            }
        }
        return ret;
    }

    /**
     * Return a candidate, selected (by default) among those satisfying the constraints
     * @param <T>
     * @param candidates
     * @param constraints
     * @return
     */
    public static  <T extends Component> T getSelectedComponent(Set<T> candidates, Set<Filter> constraints) {
        Set<T> ret = getConstraintsComponents(candidates, constraints) ;
		return getDefaultComponent(ret) ;
    }

	/**
	 * Return the set (if no preferences), of component matching the constraints, and then selected following the preferences.
	 * If preferences set, return a single component, otherwise returns all the components matching the constraints.
	 * Fills in the Insts set, the valid instances.
	 * @param <T> A component type.
	 * @param candidates
	 * @param preferences
	 * @param constraints
	 * @return
	 */
	public static <T extends Component> Set<T> getConstraintsPreferedComponent(Set<T> candidates, List<Filter> preferences, 
			Set<Filter> constraints) {
		if (candidates == null || candidates.isEmpty()) return null ;

		//Select only those implem with instances matching the constraints		
		Set<T> valids = getConstraintsComponents(candidates, constraints);

		//Valids contains all the valid components
		//If no preference return them all.
		if ((preferences == null || preferences.isEmpty())) 
			return valids ;

		//If preferences return the one and only one prefered
		T valid = getPrefered (valids, preferences) ;
		Set<T> ret = new HashSet<T> () ; 
		ret.add(valid) ;
		return ret ;
	}

    /**
	 * Return the candidates that best matches the preferences
     * Take the preferences in orden: m candidates
     * find  the n candidates that match the constraint.
     * 		if n= 0 ignore the constraint
     *      if n=1 return it.
     * iterate with the n candidates.
	 * At the end, if n > 1 returns the default one.
     *
	 * @param <T>
	 * @param candidates
	 * @param preferences
	 * @return
     */
	public static  <T extends Component> T getPrefered (Set<T> candidates, List<Filter> preferences ) {
        if (candidates == null || candidates.isEmpty()) return null ;
		if (preferences == null || preferences.isEmpty()) {
			return getDefaultComponent(candidates) ;
        }

        Set<T> valids = new HashSet<T> ();
        for (Filter f : preferences) {
            for (T compo : candidates) {
                if (compo.match(f))
                    valids.add (compo) ;
            }

			//If a single one satisfies, it is the prefered one.
            if (valids.size()==1) return valids.iterator().next();

			//If nobody satisfies the contraints check next constraint with same set of candidates
			if (valids.isEmpty()) break ;

			//continue with those that satisfy the constraint
			candidates = valids ;
			valids=new HashSet<T> () ;
		}
		
		//More than one candidate are still here: return the default one.
        return getDefaultComponent(candidates) ;
    }	

    /**
     * Return the "best" component among the candidates. 
     * Best depends on the component nature. 
     * For implems, it is those that have sharable instance or that is instantiable.
     * @param <T>
     * @param candidates
     * @return
     */
    public static <T extends Component> T getDefaultComponent (Set<T> candidates) {
		if (candidates == null || candidates.isEmpty()) return null ;
        for (T impl : candidates) {
            if (! (impl instanceof Implementation)) return impl ;
            for (Component inst : impl.getMembers()) {
                if (((Instance)inst).isSharable())
                    return impl;
            }
        }
        for (T impl : candidates) {
            if (impl.isInstantiable())
                return impl;
        }
        return candidates.iterator().next();
    }




}
