package fr.imag.adele.apam.apamImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASMImpl;
import fr.imag.adele.apam.ASMSpec;
import fr.imag.adele.apam.apformAPI.ApformSpecification;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;

//import fr.imag.adele.sam.ApformSpecification;

public class SpecificationImpl extends AttributesImpl implements ASMSpec {

    private String              name;
    // private final CompositeOLD myComposite;
    private ApformSpecification apfSpec         = null;
    private final Set<ASMImpl>  implementations = new HashSet<ASMImpl>();
    private String[]            interfaces;

    private final Set<ASMSpec>  requires        = new HashSet<ASMSpec>(); // all relations requires
    private final Set<ASMSpec>  invRequires     = new HashSet<ASMSpec>(); // all reverse relations requires

    // private int shared = ASM.SHAREABLE;
    // private final int clonable = ASM.TRUE;

    // private static Logger logger = Logger.getLogger(ASMSpecImpl.class);

    public SpecificationImpl(String specName, ApformSpecification apfSpec, String[] interfaces, Attributes props) {
        if (((specName == null) && (apfSpec == null))) {
            new Exception("Both spec name and apfSpec are null in spec constructor").printStackTrace();
            return;
        }
        if (specName == null) {
            name = apfSpec.getName();
        } else
            name = specName;
        if (apfSpec != null) {
            this.apfSpec = apfSpec;
            this.interfaces = apfSpec.getInterfaceNames();
        } else {
            this.interfaces = interfaces;
        }
        ((SpecificationBrokerImpl) CST.ASMSpecBroker).addSpec(this);
        if (props != null)
            setProperties(props.getProperties());
//        try {
//            if (props == null) {
//                props = new AttributesImpl();
//            }
//            // initialize properties. A fusion of SAM and APAM values
//            if (apfSpec != null)
//                this.setProperties(Util.mergeProperties(this, props, apfSpec.getProperties()));
//            else
//                this.setProperties(Util.mergeProperties(this, props, null));
//        } catch (ConnectionException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public String[] getInterfaces() {
        return interfaces;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addImpl(ASMImpl impl) {
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
        if ((apfSpec != null) && name.equals(apfSpec.getName())) {
            System.out.println("changing logical name, from " + name + " to " + logicalName);
            name = logicalName;
            return;
        }
        System.err.println(" Error : cannot change specification name from " + name + " to " + logicalName);
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
    public ASMImpl getImpl(String name) {
        if (name == null)
            return null;
        for (ASMImpl impl : implementations) {
            if (impl.getName().equals(name))
                return impl;
        }
        return null;
    }

    @Override
    public Set<ASMImpl> getImpls(Filter filter) throws InvalidSyntaxException {
        if (filter == null)
            return getImpls();
        Set<ASMImpl> ret = new HashSet<ASMImpl>();
        for (ASMImpl impl : implementations) {
            if (filter.match((AttributesImpl) impl.getProperties())) {
                ret.add(impl);
            }
        }
        return ret;
    }

    @Override
    public String[] getInterfaceNames() {
        return interfaces;
    }

    // relation requires control
    public void addRequires(ASMSpec dest) {
        if (requires.contains(dest))
            return;
        requires.add(dest);
        ((SpecificationImpl) dest).addInvRequires(this);
    }

    public void removeRequires(ASMSpec dest) {
        for (ASMImpl impl : implementations) {
            for (ASMImpl implDest : impl.getUses())
                if (implDest.getSpec() == dest) {
                    return; // it exists another instance that uses that destination. Do nothing.
                }
        }
        requires.remove(dest);
        ((SpecificationImpl) dest).removeInvRequires(this);
    }

    private void addInvRequires(ASMSpec orig) {
        invRequires.add(orig);
    }

    private void removeInvRequires(ASMSpec orig) {
        invRequires.remove(orig);
    }

    @Override
    public Set<ASMSpec> getRequires() {
        return Collections.unmodifiableSet(requires);
    }

    @Override
    public Set<ASMSpec> getInvRequires() {
        return Collections.unmodifiableSet(invRequires);
    }

    @Override
    public ApformSpecification getApformSpec() {
        return apfSpec;
    }

    @Override
    public void remove() {
        for (ASMImpl impl : implementations) {
            impl.remove();
        }
        CST.ASMSpecBroker.removeSpec(this);

//        // remove the APAM specific attributes in SAM
//        if (apfSpec != null) {
//            try {
//                apfSpec.removeProperty(Attributes.APAMAPPLI);
//                apfSpec.removeProperty(Attributes.APAMCOMPO);
//            } catch (ConnectionException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void removeImpl(ASMImpl impl) {
        implementations.remove(impl);
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return Collections.unmodifiableSet(implementations);
    }

    public void setSamSpec(ApformSpecification apfSpec) {
        if (apfSpec == null)
            return;
        this.apfSpec = apfSpec;
    }

    @Override
    public Set<ASMImpl> getImpls(Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(implementations);
        Set<ASMImpl> ret = new HashSet<ASMImpl>();
        for (ASMImpl impl : implementations) {
            for (Filter filter : constraints) {
                if (filter.match((AttributesImpl) impl.getProperties())) {
                    ret.add(impl);
                }
            }
        }
        return ret;
    }

    @Override
    public Set<ASMImpl> getImpls(Set<ASMImpl> candidates, Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(candidates);
        Set<ASMImpl> ret = new HashSet<ASMImpl>();
        for (ASMImpl impl : candidates) {
            for (Filter filter : constraints) {
                if (filter.match((AttributesImpl) impl)) {
                    ret.add(impl);
                }
            }
        }
        return ret;
    }

    @Override
    public ASMImpl getImpl(Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMImpl> impls = null;
        if ((preferences != null) && !preferences.isEmpty()) {
            impls = getImpls(constraints);
        } else
            impls = implementations;
        if ((constraints == null) || constraints.isEmpty())
            return ((ASMImpl) impls.toArray()[0]);

        return getPreferedImpl(impls, preferences);
    }

    @Override
    public ASMImpl getPreferedImpl(Set<ASMImpl> candidates, List<Filter> preferences) {
        if ((preferences == null) || preferences.isEmpty()) {
            if (candidates.isEmpty())
                return null;
            else
                return (ASMImpl) candidates.toArray()[0];
        }
        ASMImpl winner = null;
        int maxMatch = -1;
        for (ASMImpl impl : candidates) {
            int match = 0;
            for (Filter filter : preferences) {
                if (!filter.match((AttributesImpl) impl))
                    break;
                match++;
            }
            if (match > maxMatch) {
                maxMatch = match;
                winner = impl;
            }
        }
        System.out.println("   Selected : " + winner);
        return winner;
    }

}