package fr.imag.adele.apam.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.util.ApamFilter;
//import java.util.concurrent.ConcurrentHashMap;
//import org.apache.felix.utils.filter.FilterImpl;


public class SpecificationImpl extends PropertiesImpl implements Specification {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2752578219337076677L;
	
	private Logger logger = LoggerFactory.getLogger(SpecificationImpl.class);
    private String                    name;
    private ApformSpecification       apfSpec         = null;
    private SpecificationDeclaration  declaration;
    private final Set<Implementation> implementations = Collections
    .newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

    private final Set<Specification>  requires        = Collections
    .newSetFromMap(new ConcurrentHashMap<Specification, Boolean>()); // all
    // relations
    // requires
    private final Set<Specification>  invRequires     = Collections
    .newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());  // all
    // reverse
    // relations
    // requires

    // private static Logger logger = Logger.getLogger(ASMSpecImpl.class);

    @Override
    public boolean equals(Object o) {
        return (this == o);
    }

    public SpecificationImpl(String specName, ApformSpecification apfSpec, Set<ResourceReference> resources,
            Map<String, Object> props) {
        assert  (((specName != null) || (apfSpec != null))) ;

        if (specName == null) {
            name = apfSpec.getDeclaration().getName();
        } else
            name = specName;
        put(CST.A_SPECNAME, name);
        if (apfSpec == null) {
            apfSpec = new ApformEmptySpec(resources, props);
        }
        this.apfSpec = apfSpec;
        declaration = apfSpec.getDeclaration();

        //        interfaces = declaration.getProvidedInterfaces();
        //        messages =

        putAll(apfSpec.getDeclaration().getProperties());
        ((SpecificationBrokerImpl) CST.SpecBroker).addSpec(this);
        if (props != null)
            setAllProperties(props);
    }


    @Override
    public String getName() {
        return name;
    }

    public void addImpl(Implementation impl) {
        implementations.add(impl);
    }

    public void setName(String logicalName) {
        if ((logicalName == null) || (logicalName == ""))
            return;
        if (name == null) {
            name = logicalName;
            return;
        }
        if (name.equals(logicalName))
            return;
        if ((apfSpec != null) && name.equals(apfSpec.getDeclaration().getName())) {
            logger.debug("changing logical name, from " + name + " to " + logicalName);
            name = logicalName;
            return;
        }
        logger.error(" Error : cannot change specification name from " + name + " to " + logicalName);
    }

    @Override
    public String toString() {
        return name;
        //        String ret = "";
        //        if (name == null) {
        //            ret = " (" + apfSpec.getName() + ") ";
        //        } else {
        //            if (apfSpec == null)
        //                ret = name;
        //            else
        //                ret = name + " (" + apfSpec.getName() + ") ";
        //        }
        //        return ret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.apf.ApformSpecification#getASMImpl(java.lang.String)
     */
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

    @Override
    public Set<Implementation> getImpls(Filter filter) throws InvalidSyntaxException {
        if (filter == null)
            return getImpls();
        Set<Implementation> ret = new HashSet<Implementation>();
        for (Implementation impl : implementations) {
            if (impl.match(filter)) {
                ret.add(impl);
            }
        }
        return ret;
    }

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

    @Override
    public ApformSpecification getApformSpec() {
        return apfSpec;
    }

    protected void remove() {
        for (Implementation impl : implementations) {
            ((ImplementationBrokerImpl)CST.ImplBroker).removeImpl(impl,false);
        }
    }

    protected void removeImpl(Implementation impl) {
        implementations.remove(impl);
    }

    @Override
    public Set<Implementation> getImpls() {
        return Collections.unmodifiableSet(implementations);
    }

    public void setSpecApform(ApformSpecification apfSpec) {
        this.apfSpec = apfSpec;
    }

    @Override
    public Set<Implementation> getImpls(Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(implementations);
        Set<Implementation> ret = new HashSet<Implementation>();
        for (Implementation impl : implementations) {
            for (Filter filter : constraints) {
                if (impl.match(filter)) {
                    ret.add(impl);
                }
            }
        }
        return ret;
    }

    @Override
    public Set<Implementation> getImpls(Set<Implementation> candidates, Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(candidates);
        Set<Implementation> ret = new HashSet<Implementation>();
        for (Implementation impl : candidates) {
            for (Filter filter : constraints) {
                if (impl.match(filter)) {
                    ret.add(impl);
                }
            }
        }
        return ret;
    }

    @Override
    public Implementation getImpl(Set<Filter> constraints, List<Filter> preferences) {
        Set<Implementation> impls = null;
        if ((constraints == null) || constraints.isEmpty()) {
            impls = getImpls(constraints);
        } else
            impls = implementations;
        if ((impls == null) || impls.isEmpty())
            return null;
        return getPreferedImpl(impls, preferences);
    }

    /**
     * If no prefered, select
     * first return the implem that have available instances,
     * second an instantiable implem,
     * third, any one.
     */
    @Override
    public Implementation getPreferedImpl(Set<Implementation> candidates, List<Filter> preferences) {
        if ((preferences == null) || preferences.isEmpty()) {
            if (candidates.isEmpty())
                return null;
            else
                return getDefaultImpl(candidates);
        }
        Implementation winner = null;
        int maxMatch = -1;
        for (Implementation impl : candidates) {
            int match = 0;
            for (Filter filter : preferences) {
                if (!impl.match(filter))
                    break;
                match++;
            }
            if (match > maxMatch) {
                maxMatch = match;
                winner = impl;
            }
        }
        // System.out.println("   Selected : " + winner);
        return winner;
    }

    /**
     * In case more than one implementation are available and no preference are expressed,
     * first return the implem that have available instances,
     * second an instantiable implem,
     * third, any one.
     * 
     * @param candidates
     * @return
     */
    private Implementation getDefaultImpl(Set<Implementation> candidates) {
        for (Implementation impl : candidates) {
            for (Instance inst : impl.getInsts()) {
                if (inst.isSharable())
                    return impl;
            }
        }
        for (Implementation impl : candidates) {
            if (impl.isInstantiable())
                return impl;
        }
        return (Implementation) candidates.toArray()[0];
    }

    @Override
    public boolean match(Filter goal) {
        if (goal == null)
            return true;
        try {
            return ((ApamFilter) goal).matchCase(this);
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public SpecificationDeclaration getDeclaration() {
        return declaration;
    }

    /**
     * Created only for those specifications that do not exist in the OSGi layer.
     * Creates a minimal definition structure.
     * 
     * @author Jacky
     * 
     */
    private class ApformEmptySpec implements ApformSpecification {

        private final SpecificationDeclaration declaration;

        public ApformEmptySpec(Set<ResourceReference> resources, Map<String, Object> attributes) {
            super () ;
            declaration = new EmptySpecificationDeclaration(name, resources);
        }

        @Override
        public SpecificationDeclaration getDeclaration() {
            return declaration;
        }
    }
    private class EmptySpecificationDeclaration extends SpecificationDeclaration {
        public EmptySpecificationDeclaration(String name, Set<ResourceReference> resources) {
            super(name);
            getProvidedResources().addAll(resources);
        }
    }

}