package fr.imag.adele.apam.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformSpecification;


public class SpecificationImpl extends ComponentImpl implements Specification {
	
	private static final long serialVersionUID = -2752578219337076677L;
	
    private final Set<Implementation> implementations = Collections
    .newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

    /*
     * All relation requires, derived from all the used implementations
     */
    private final Set<Specification>  requires        = Collections
    .newSetFromMap(new ConcurrentHashMap<Specification, Boolean>()); 

    /*
     * All reverse requires, the opposite of requires
     */
    private final Set<Specification>  invRequires     = Collections
    .newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());  // all

//    //spec created empty only to be associated with implementations that do not implement a spec.
//    private boolean dummySpec = false ;
    
    protected SpecificationImpl(ApformSpecification apfSpec) throws InvalidConfiguration {
    	super(apfSpec);
    }

   
    @Override
	public void register(Map<String,String> initialProperties) throws InvalidConfiguration {
        /*
         * Terminates the initalisation, and computes properties
         */
        initializeProperties(initialProperties) ;

    	/*
    	 * Add to broker
    	 */
        ((ComponentBrokerImpl) CST.componentBroker).add(this);
        
        /*
    	 * Notify managers
    	 * 
    	 * Add call back to add specification?
         */
        ApamManagers.notifyAddedInApam(this) ;
	}

    @Override
    public void unregister() {
    	
    	/*
    	 * Notify managers
    	 * 
    	 * TODO Add call back to remove specification?
    	 */
    	

    	/*
    	 * Remove all implementations providing this specification
    	 * 
    	 * TODO Is this really necessary? We should consider the special case of
    	 * updates because we probably can reduce the impact of the modification.  
    	 */
        for (Implementation impl : implementations) {
            ((ComponentBrokerImpl)CST.componentBroker).removeImpl(impl,false);
        }
    	
        /*
         * TODO What to do with implementations that reference this specification
         * in its dependencies?
         */
        
        /*
         * remove from broker
         */
        ((ComponentBrokerImpl)CST.componentBroker).remove(this);
    	
    }

    @Override
    public ApformSpecification getApformSpec() {
        return (ApformSpecification)getApformComponent();
    }

    public void addImpl(Implementation impl) {
        implementations.add(impl);
    }

    @Override
    public Implementation getImpl(String name) {
        if (name == null)
            return null;
        for (Implementation impl : implementations) {
            if (impl.getName().equals(name))
                return impl;
        }
        return null;
    }

//    @Override
//    public Set<Implementation> getImpls(Filter filter) throws InvalidSyntaxException {
//        if (filter == null)
//            return getImpls();
//        Set<Implementation> ret = new HashSet<Implementation>();
//        for (Implementation impl : implementations) {
//            if (impl.match(filter)) {
//                ret.add(impl);
//            }
//        }
//        return ret;
//    }

    //    @Override
    //    public Set<String> getInterfaceNames() {
    //        return declaration.getProvidedRessourceNames(ResourceType.INTERFACE);
    //    }

    // relation requires control
    public void addRequires(Specification dest) {
        if (requires.contains(dest))
            return;
        requires.add(dest);
        ((SpecificationImpl) dest).addInvRequires(this);
    }

    public void removeRequires(Specification dest) {
        for (Implementation impl : implementations) {
            for (Implementation implDest : impl.getUses())
                if (implDest.getSpec() == dest) {
                    return; // it exists another instance that uses that destination. Do nothing.
                }
        }
        requires.remove(dest);
        ((SpecificationImpl) dest).removeInvRequires(this);
    }

    private void addInvRequires(Specification orig) {
        invRequires.add(orig);
    }

    private void removeInvRequires(Specification orig) {
        invRequires.remove(orig);
    }

    @Override
    public Set<Specification> getRequires() {
        return Collections.unmodifiableSet(requires);
    }

    @Override
    public Set<Specification> getInvRequires() {
        return Collections.unmodifiableSet(invRequires);
    }


    protected void removeImpl(Implementation impl) {
        implementations.remove(impl);
    }

    @Override
    public Set<Implementation> getImpls() {
        return Collections.unmodifiableSet(implementations);
    }
    
	@Override
	public Set<? extends Component> getMembers() {
		//return new HashSet <Component> (implementations) ;
		return Collections.unmodifiableSet(implementations);
	}

	@Override
	public Component getGroup() {
		return null;
	}
}

//    @Override
//    public Set<Implementation> getImpls(Set<Filter> constraints) {
//        if ((constraints == null) || constraints.isEmpty())
//            return Collections.unmodifiableSet(implementations);
//        Set<Implementation> ret = new HashSet<Implementation>();
//        for (Implementation impl : implementations) {
//            for (Filter filter : constraints) {
//                if (impl.match(filter)) {
//                    ret.add(impl);
//                }
//            }
//        }
//        return ret;
//    }
//
//    @Override
//    public Set<Implementation> getImpls(Set<Implementation> candidates, Set<Filter> constraints) {
//        if ((constraints == null) || constraints.isEmpty())
//            return Collections.unmodifiableSet(candidates);
//        Set<Implementation> ret = new HashSet<Implementation>();
//        for (Implementation impl : candidates) {
//            for (Filter filter : constraints) {
//                if (impl.match(filter)) {
//                    ret.add(impl);
//                }
//            }
//        }
//        return ret;
//    }
//
//    @Override
//    public Implementation getImpl(Set<Filter> constraints, List<Filter> preferences) {
//        Set<Implementation> impls = null;
//        if ((constraints == null) || constraints.isEmpty()) {
//            impls = getImpls(constraints);
//        } else
//            impls = implementations;
//        if ((impls == null) || impls.isEmpty())
//            return null;
//        return getPreferedComponent(impls, preferences);
//    }

//    /**
//     * If no prefered, select
//     * first return the implem that have available instances,
//     * second an instantiable implem,
//     * third, any one.
//     */
//    public <T extends Component> T getPreferedImpl(Set<T> candidates, List<Filter> preferences) {
//        if ((preferences == null) || preferences.isEmpty()) {
//            if (candidates.isEmpty())
//                return null;
//            else
//                return getDefaultImpl(candidates);
//        }
//        T winner = null;
//        int maxMatch = -1;
//        for (T compo : candidates) {
//            int match = 0;
//            for (Filter filter : preferences) {
//                if (!compo.match(filter))
//                    break;
//                match++;
//            }
//            if (match > maxMatch) {
//                maxMatch = match;
//                winner = compo;
//            }
//        }
//        // System.out.println("   Selected : " + winner);
//        return winner;
//
//    }
//    @Override
//    public Implementation getPreferedImpl(Set<Implementation> candidates, List<Filter> preferences) {
//        if ((preferences == null) || preferences.isEmpty()) {
//            if (candidates.isEmpty())
//                return null;
//            else
//                return getDefaultImpl(candidates);
//        }
//        Implementation winner = null;
//        int maxMatch = -1;
//        for (Implementation impl : candidates) {
//            int match = 0;
//            for (Filter filter : preferences) {
//                if (!impl.match(filter))
//                    break;
//                match++;
//            }
//            if (match > maxMatch) {
//                maxMatch = match;
//                winner = impl;
//            }
//        }
//        // System.out.println("   Selected : " + winner);
//        return winner;
//    }
//
//    /**
//     * In case more than one implementation are available and no preference are expressed,
//     * first return the implem that have available instances,
//     * second an instantiable implem,
//     * third, any one.
//     * 
//     * @param candidates
//     * @return
//     */
//    
//    //public abstract <T extends Component> T getDefaultComponent (Set<T> candidates) ;
//
//    public Implementation getDefaultComponent(Set<Implementation> candidates) {
//        for (Implementation impl : candidates) {
//            for (Instance inst : impl.getInsts()) {
//                if (inst.isSharable())
//                    return impl;
//            }
//        }
//        for (Implementation impl : candidates) {
//            if (impl.isInstantiable())
//                return impl;
//        }
//        return (Implementation) candidates.toArray()[0];
//    }
//
//
//}