/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.SpecificationReference;

public class UtilComp {
	
	private static Logger logger = LoggerFactory.getLogger(UtilComp.class);

	/**
	 * 
	 * @param impls
	 * @param insts Set of valid instance. Cannot be empty nor null.
	 * @param dependency
	 */
	@SuppressWarnings("unchecked") 
	public static Instance selectBestInstance (Set<Implementation> impls, Set<Instance> insts, DependencyDeclaration dependency) {
		//assert (!insts.isEmpty()) ;
		if (insts == null || insts.isEmpty()) 
			return null ;
		/*
		 * No choice, take that instance; and return its implem, even if not visible.
		 */
		if (insts.size() == 1) {
			return insts.iterator().next();
		}
		
		List<ApamFilter> implPreference = Util.toFilterList(dependency.getImplementationPreferences()) ;
		List<ApamFilter> instPreference = Util.toFilterList(dependency.getInstancePreferences()) ;
		Set<ApamFilter> instConstraints = Util.toFilter    (dependency.getInstanceConstraints()) ;
		
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
    public static <T extends Component> Set<T> getConstraintsComponents(Set<T> candidates, Set<ApamFilter> constraints) {
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
    public static  <T extends Component> T getSelectedComponent(Set<T> candidates, Set<ApamFilter> constraints) {
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
	public static <T extends Component> Set<T> getConstraintsPreferedComponent(Set<T> candidates, List<ApamFilter> preferences, 
			Set<ApamFilter> constraints) {
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
	public static  <T extends Component> T getPrefered (Set<T> candidates, List<ApamFilter> preferences ) {
        if (candidates == null || candidates.isEmpty()) return null ;
		if (candidates.size() == 1) return candidates.iterator().next() ;
		if (preferences == null || preferences.isEmpty()) {
			return getDefaultComponent(candidates) ;
        }

        Set<T> valids = new HashSet<T> ();
        for (ApamFilter f : preferences) {
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
    private static <T extends Component> T getDefaultComponent (Set<T> candidates) {
		if (candidates == null || candidates.isEmpty()) return null ;
    	if (!(candidates.iterator().next() instanceof Implementation)) 
    		return candidates.iterator().next() ;
    	
        for (T impl : candidates) {
            if (impl.isInstantiable())
                return impl;
            for (Component inst : impl.getMembers()) {
                if (((Instance)inst).isSharable())
                    return impl;
            }
        }
        return candidates.iterator().next();
    }


	/**
	 * Provided an instance, computes all the dependency declaration that applies to that instance/
	 * If can be defined on the instance, the implementation, the specification, or on both.
	 * In case the same dependency is defined multiple time, it is the most concrete one that must be taken into account.
	 * There is no attempt to compute all the constraints that apply on a given dependency;
	 * We are only interested in the target.
	 * @param client
	 * @return
	 */
	public static Set<DependencyDeclaration> computeAllDependencies (Instance client) {
		Set<DependencyDeclaration> allDeps = new HashSet<DependencyDeclaration> () ;
		allDeps.addAll(client.getDeclaration().getDependencies());
	
		boolean found ;
		for (DependencyDeclaration dep : client.getImpl().getDeclaration().getDependencies()) {
			found= false ;
			for (DependencyDeclaration allDep : allDeps) {
				if (allDep.getIdentifier().equals(dep.getIdentifier())) {
					found= true;
					break ;
				}
			}
			if (!found) allDeps.add(dep) ;
		}
		for (DependencyDeclaration dep : client.getSpec().getDeclaration().getDependencies()) {
			found= false ;
			for (DependencyDeclaration allDep : allDeps) {
				if (allDep.getIdentifier().equals(dep.getIdentifier())) {
					found= true;
					break ;
				}
			}
			if (!found) allDeps.add(dep) ;
		}
		return allDeps ;
	}


	/**
	 * Provided an instance, computes all the dependency declaration that applies to that instance.
	 * If can be defined on the instance, the implementation, the specification, or on both.
	 * For each dependency, we clone it, and we aggregate the constraints as found at all level,
	 * including the generic ones found in the composite type.
	 * The dependencies returned are clones of the original ones.
	 *
	 */
	public static Set<DependencyDeclaration> computeAllEffectiveDependency (Instance client) {
		if (client == null) return null ;
		Set<DependencyDeclaration> allDeps = new HashSet <DependencyDeclaration> ();
		for (DependencyDeclaration dep : computeAllDependencies (client)) {
			allDeps.add(computeEffectiveDependency(client, dep.getIdentifier())) ;
		}
		return allDeps ;
	}


	/**
	 * We aggregate the constraints with the generic one found in the composite type.
	 * We compute also the dependency flags.
	 *
	 * @param client
	 * @param dependency
	 * @return
	 */
	public static DependencyDeclaration computeEffectiveDependency (Instance client, String depName) {
		//Find the first dependency declaration.
		DependencyDeclaration dependency = client.getDeclaration().getDependency(depName) ;
		if (dependency == null) {
			//Only defined in the implementation
			dependency = client.getGroup().getDeclaration().getDependency(depName) ;
		} 
	
		//Should never happen
		if (dependency == null || dependency.getTarget() == null) {
			logger.error("Invalid dependency " + depName + " for instance " + client + ".  Not declared ") ;
			return null ;
		}
	
		List<DependencyDeclaration> ctxtDcl = client.getComposite().getCompType().getCompoDeclaration().getContextualDependencies() ;
		if (ctxtDcl == null || ctxtDcl.isEmpty())
			return dependency ;
	
		//Add the composite generic constraints
		//But do not change the declared dependency
		dependency = dependency.clone() ;	
		Map<String, String> validAttrs = client.getValidAttributes() ;
		for ( DependencyDeclaration  genDep  : ctxtDcl) {
			if (matchGenericDependency(client, genDep, dependency)) {
				overrideDepFlags (dependency, genDep, true) ;
	
				if (Util.checkFilters(genDep.getImplementationConstraints(), null, validAttrs, client.getName())) {
					dependency.getImplementationConstraints().addAll(genDep.getImplementationConstraints()) ;
				}
				dependency.getInstanceConstraints().addAll(genDep.getInstanceConstraints()) ;
				if (Util.checkFilters(null, genDep.getImplementationPreferences(), validAttrs, client.getName())) {
					dependency.getImplementationPreferences().addAll(genDep.getImplementationPreferences()) ;
				}
				if (Util.checkFilters(null, genDep.getInstancePreferences(), validAttrs, client.getName())) {
					dependency.getInstancePreferences().addAll(genDep.getInstancePreferences()) ;
				}
			}
		}
		return dependency ;
	}


	///About dependencies
	/**
	 * Provided a client instance, checks if its dependency "clientDep", matches another dependency: "compoDep".
	 *
	 * matches only based on same name (same resource or same component).
	 * If client cardinality is multiple, compo cardinallity must be multiple too.
	 * No provision for the client constraints or characteristics (missing, eager)
	 *
	 * @param compoInst the composite instance containing the client
	 * @param compoDep the dependency that matches or not
	 * @param clientDep the client dependency we are trying to resolve
	 * @return
	 */
	public static boolean matchDependency(Instance compoInst, DependencyDeclaration compoDep, DependencyDeclaration clientDep) {
		boolean multiple = clientDep.isMultiple();
		//Look for same dependency: the same specification, the same implementation or same resource name
		//Constraints are not taken into account
	
		// if same nature (spec, implem, internface ... make a direct comparison.
		if (compoDep.getTarget().getClass().equals(clientDep.getTarget().getClass())) { 
			if (compoDep.getTarget().equals(clientDep.getTarget())) {
				if (!multiple || compoDep.isMultiple()) {
					return true;
				}
			}
		}
	
		//Look for a compatible dependency.
		//Stop at the first dependency matching only based on same name (same resource or same component)
		//No provision for : cardinality, constraints or characteristics (missing, eager)
	
		//Look if the client requires one of the resources provided by the specification
		if (compoDep.getTarget() instanceof SpecificationReference) {
			Specification spec = CST.apamResolver.findSpecByName(compoInst,
					((SpecificationReference) compoDep.getTarget()).getName());
			if ((spec != null) && spec.getDeclaration().getProvidedResources().contains(clientDep.getTarget())
					&& (!multiple || compoDep.isMultiple())) {
				return true;
			}
		} 
	
		//If the composite has a dependency toward an implementation
		//and the client requires a resource provided by that implementation
		else {
			if (compoDep.getTarget() instanceof ImplementationReference) {
				String implName = ((ImplementationReference<?>) compoDep.getTarget()).getName();
				Implementation impl = CST.apamResolver.findImplByName(compoInst, implName);
				if (impl != null) {
					//The client requires the specification implemented by that implementation
					if (clientDep.getTarget() instanceof SpecificationReference) {
						String clientReqSpec = ((SpecificationReference) clientDep.getTarget()).getName();
						if (impl.getImplDeclaration().getSpecification().getName().equals(clientReqSpec)
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					} else {
						//The client requires a resource provided by that implementation
						if (impl.getImplDeclaration().getProvidedResources().contains(clientDep.getTarget())
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}


	/**
	 * Provided a composite (compoInst), checks if the provided generic dependency constraint declaration
	 * matches the compoClient dependency declaration.
	 *
	 * @param compoInst the composite instance containing the client
	 * @param genericDeps the dependencies of the composite: a regExpression
	 * @param clientDep the client dependency we are trying to resolve.
	 * @return
	 */
	public static boolean matchGenericDependency(Instance compoInst, DependencyDeclaration compoDep, DependencyDeclaration clientDep) {
	
		String pattern = compoDep.getTarget().getName() ;
		//Look for same dependency: the same specification, the same implementation or same resource name
		//Constraints are not taken into account
	
		// same nature: direct comparison
		if (compoDep.getTarget().getClass().equals(clientDep.getTarget().getClass())
				&& (clientDep.getTarget().getName().matches(pattern))) {
			return true;
		}
	
		//If the client dep is an implementation dependency, check if the specification matches the pattern
		if (compoDep.getTarget() instanceof SpecificationReference
				&& clientDep.getTarget() instanceof ImplementationReference) {
			String implName = ((ImplementationReference<?>) clientDep.getTarget()).getName();
			Implementation impl = CST.apamResolver.findImplByName(compoInst, implName);
			if (impl != null && impl.getSpec().getName().matches(pattern)) {
				return true ;
			}
		}
		return false;
	}


	/**
	 * A dependency may have properties fail= null, wait, exception; exception = null, exception
	 * A contextual dependency can also have hide: null, true, false and eager: null, true, false
	 * The most local definition overrides the others.
	 * Exception null can be overriden by an exception; only generic exception overrides another non null one.
	 *
	 * @param dependency : the low level one that will be changed if needed
	 * @param dep: the group dependency
	 * @param generic: the dep comes from the composite type. It can override the exception, and has hidden and eager.
	 * @return
	 */
	public static void overrideDepFlags (DependencyDeclaration dependency, DependencyDeclaration dep, boolean generic) {
		//If set, cannot be changed by the group definition.
		//NOTE: This strategy is because it cannot be compiled, and we do not want to make an error during resolution
		if (dependency.getMissingPolicy() == null || (generic && dep.getMissingPolicy() != null)) {
			dependency.setMissingPolicy(dep.getMissingPolicy()) ;
		}
	
		if (dependency.getMissingException() == null || (generic && dep.getMissingException() != null)) {
			dependency.setMissingException(dep.getMissingException()) ;
		}
	
		if (generic) {
			dependency.setHide(dep.isHide()) ;
			dependency.setEager(dep.isEager()) ;
		}
	}




}
