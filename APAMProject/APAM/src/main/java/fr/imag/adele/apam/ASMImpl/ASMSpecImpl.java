package fr.imag.adele.apam.ASMImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Specification;

public class ASMSpecImpl extends AttributesImpl implements ASMSpec {

    private String             name;
    //    private final CompositeOLD          myComposite;
    private Specification      samSpec         = null;
    private final Set<ASMImpl> implementations = new HashSet<ASMImpl>();

    private final Set<ASMSpec> requires        = new HashSet<ASMSpec>(); // all relations requires
    private final Set<ASMSpec> invRequires     = new HashSet<ASMSpec>(); // all reverse relations requires

    // private int shared = ASM.SHAREABLE;
    // private final int clonable = ASM.TRUE;

    // private static Logger logger = Logger.getLogger(ASMSpecImpl.class);

    @Override
    public String getName() {
        return name;
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
        if ((samSpec != null) && name.equals(samSpec.getName())) {
            System.out.println("changing logical name, from " + name + " to " + logicalName);
            name = logicalName;
            return;
        }
        System.err.println(" Error : cannot change specification name from " + name + " to " + logicalName);
    }

    public ASMSpecImpl(String specName, Specification samSpec, Attributes props) {
        if (((name == null) && (samSpec == null))) {
            new Exception("Both spec name and samSpec are null in spec constructor");
            return;
        }
        if (specName == null) {
            name = samSpec.getName();
        } else
            name = specName;
        this.samSpec = samSpec; // may be null
        ((ASMSpecBrokerImpl) CST.ASMSpecBroker).addSpec(this);
        try {
            if (props == null) {
                props = new AttributesImpl();
            }
            // initialize properties. A fusion of SAM and APAM values
            if (samSpec != null)
                this.setProperties(Util.mergeProperties(this, props, samSpec.getProperties()));
            else
                this.setProperties(Util.mergeProperties(this, props, null));
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        String ret = "";
        if (name == null) {
            ret = " (" + samSpec.getName() + ") ";
        } else {
            if (samSpec == null)
                ret = name;
            else
                ret = name + " (" + samSpec.getName() + ") ";
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.sam.Specification#getASMImpl(java.lang.String)
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
        return samSpec.getInterfaceNames();
    }

    // relation requires control
    public void addRequires(ASMSpec dest) {
        if (requires.contains(dest))
            return;
        requires.add(dest);
        ((ASMSpecImpl) dest).addInvRequires(this);
    }

    public void removeRequires(ASMSpec dest) {
        for (ASMImpl impl : implementations) {
            for (ASMImpl implDest : impl.getUses())
                if (implDest.getSpec() == dest) {
                    return; // it exists another instance that uses that destination. Do nothing.
                }
        }
        requires.remove(dest);
        ((ASMSpecImpl) dest).removeInvRequires(this);
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
    public Specification getSamSpec() {
        return samSpec;
    }

    @Override
    public void remove() {
        for (ASMImpl impl : implementations) {
            impl.remove();
        }
        CST.ASMSpecBroker.removeSpec(this);

        // remove the APAM specific attributes in SAM
        if (samSpec != null) {
            try {
                samSpec.removeProperty(Attributes.APAMAPPLI);
                samSpec.removeProperty(Attributes.APAMCOMPO);
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return Collections.unmodifiableSet(implementations);
    }

    public void setSamSpec(Specification samSpec) {
        if (samSpec == null)
            return;
        this.samSpec = samSpec;
    }

}