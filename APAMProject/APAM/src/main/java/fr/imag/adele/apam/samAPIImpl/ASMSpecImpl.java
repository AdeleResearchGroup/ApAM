package fr.imag.adele.apam.samAPIImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Specification;

public class ASMSpecImpl extends AttributesImpl implements ASMSpec {

    private String             name;
    private Composite          myComposite;
    private Specification      samSpec         = null;
    private final Set<ASMImpl> implementations = new HashSet<ASMImpl>();

    // private int shared = ASM.SHAREABLE;
    // private final int clonable = ASM.TRUE;

    // private static Logger logger = Logger.getLogger(ASMSpecImpl.class);

    public void setASMName(String logicalName) {
        if ((logicalName == null) || (logicalName == ""))
            return;
        if (name == null) {
            name = logicalName;
            return;
        }
        if (!name.equals(logicalName)) {
            System.out.println("changing logical name, from " + name + " to " + logicalName);
            name = logicalName;
        }
    }

    public ASMSpecImpl(Composite compo, String specName, Specification samSpec, Attributes props) {
        myComposite = compo;
        name = specName; // may be null
        this.samSpec = samSpec; // may be null
        compo.addSpec(this);
        ((ASMSpecBrokerImpl) ASM.ASMSpecBroker).addSpec(this);
        try {
            if (props == null) {
                props = new AttributesImpl();
            }
            props.setProperty(Attributes.APAMAPPLI, compo.getApplication().getName());
            props.setProperty(Attributes.APAMCOMPO, compo.getName());
            // initialize properties. A fusion of SAM and APAM values
            if (samSpec != null) {
                this.setProperties(Util.mergeProperties(props, samSpec.getProperties()));
                return;
            } else
                this.setProperties(props.getProperties());
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
            if (impl.getASMName().equals(name))
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

    @Override
    public Set<ASMSpec> getRequires() {
        return null;
    }

    @Override
    public Set<ASMSpec> getInvRequires() {
        return null;
    }

    @Override
    public String getASMName() {
        return name;
    }

    @Override
    public String getClonable() {
        return (String) getProperty(Attributes.CLONABLE);
    }

    @Override
    public Composite getComposite() {
        return myComposite;
    }

    @Override
    public Specification getSamSpec() {
        return samSpec;
    }

    @Override
    public String getShared() {
        return (String) getProperty(Attributes.SHARED);
    }

    @Override
    public void remove() {
        // TODO Auto-generated method stub

    }

    // @Override
    // public void setShared(int newShared) {
    // if ((newShared < Attributes.PRIVATE) || (newShared > Attributes.SHARABLE)) {
    // System.err.println("ERROR : invalid shared value : " + newShared);
    // return;
    // }
    // for (ASMImpl impl : implementations) {
    // if (impl.getShared() > newShared) {
    // System.out.println("cannot change shared prop of " + getASMName()
    // + " some implementations have higher shared prop");
    // return; // do not change anything
    // }
    // }
    // shared = newShared;
    // }

    @Override
    public Set<ASMImpl> getImpls() {
        return Collections.unmodifiableSet(implementations);
    }

    @Override
    public String getSAMName() {
        return samSpec.getName();
    }

    public void setSamSpec(Specification samSpec) {
        if (samSpec == null)
            return;
        this.samSpec = samSpec;
    }

}