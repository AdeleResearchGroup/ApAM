package fr.imag.adele.apam.ASMImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.broker.ImplementationBroker;

public class ASMImplImpl extends AttributesImpl implements ASMImpl {

    private static ASMImplBroker        myBroker      = CST.ASMImplBroker;
    private static ImplementationBroker samImplBroker = CST.SAMImplBroker;      ;

    private final Set<ASMImpl>          uses          = new HashSet<ASMImpl>(); // all relations uses
    private final Set<ASMImpl>          invUses       = new HashSet<ASMImpl>(); // all reverse relations uses

    private String                      name;
    private ASMSpec                     mySpec;
    private final Composite             myComposite;
    private Implementation              samImpl       = null;

    private final Set<ASMInst>          instances     = new HashSet<ASMInst>();

    // private int shared = ASM.SHAREABLE;
    // private int clonable = ASM.TRUE;

    /**
     * Instantiate a new service implementation.
     * 
     * @param instance the ASM instance
     * @param name CADSE name
     * 
     */
    public ASMImplImpl(Composite compo, String implName, ASMSpecImpl spec, Implementation impl, Attributes props) {
        name = implName;
        myComposite = compo;
        try {
            mySpec = spec;
            if (impl == null) {
                System.out.println("Sam Implementation cannot be null when creating an imple");
            }
            samImpl = impl;
            ((ASMImplBrokerImpl) ASMImplImpl.myBroker).addImpl(this);
            if (props == null) {
                props = new AttributesImpl();
            }
            props.setProperty(Attributes.APAMAPPLI, compo.getApplication().getName());
            props.setProperty(Attributes.APAMCOMPO, compo.getName());
            // initialize properties. A fusion of SAM and APAM values

            this.setProperties(Util.mergeProperties(props, impl.getProperties()));

        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        compo.addImpl(this);
    }

    @Override
    public String toString() {
        String ret;
        if (name == null)
            ret = " (" + samImpl.getName() + ") ";
        else
            ret = name + " (" + samImpl.getName() + ") ";
        return ret;
    }

    /**
     * From an implementation, create an instance. Creates both the SAM and ASM instances with the same properties.
     * 
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     * @throws ConnectionException
     */
    @Override
    public ASMInst createInst(Attributes initialproperties) {
        try {
            Instance samInst;
            if (initialproperties == null)
                samInst = ASMImplImpl.samImplBroker.createInstance(samImpl.getImplPid(), null);
            else
                samInst = ASMImplImpl.samImplBroker.createInstance(samImpl.getImplPid(),
                        initialproperties.attr2Properties());
            ASMInstImpl inst = new ASMInstImpl(this, initialproperties, samInst);
            instances.add(inst);
            return inst;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ASMSpec getSpec() {
        return mySpec;
    }

    @Override
    public ASMInst getInst(String targetName) {
        if (targetName == null)
            return null;
        for (ASMInst inst : instances) {
            if (inst.getASMName().equals(targetName))
                return inst;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.sam.Implementation#getInstances()
     */
    @Override
    public Set<ASMInst> getInsts() {
        return Collections.unmodifiableSet(instances);
        // return new HashSet <ASMInst> (instances) ;
    }

    /**
     * returns the first instance only.
     */
    @Override
    public ASMInst getInst() {
        if ((instances == null) || (instances.size() == 0))
            return null;
        return (ASMInst) instances.toArray()[0];
    }

    @Override
    public Set<ASMInst> getInsts(Filter query) throws InvalidSyntaxException {
        if (query == null)
            return getInsts();
        Set<ASMInst> ret = new HashSet<ASMInst>();
        for (ASMInst inst : instances) {
            if (query.match((AttributesImpl) inst))
                ret.add(inst);
        }
        return ret;
    }

    @Override
    public String getASMName() {
        return name;
    }

    @Override
    public void setASMName(String logicalName) {
        if ((logicalName == null) || (logicalName == ""))
            return;
        if (name == null) {
            name = logicalName;
            return;
        }
        if (!name.equals(logicalName)) {
            System.out.println("changign logical name, from " + name + " to " + logicalName);
            name = logicalName;
        }
    }

    @Override
    public Composite getComposite() {
        return myComposite;
    }

    @Override
    public String getScope() {
        String scope = (String) getProperty(CST.A_SCOPE);
        if (scope == null)
            scope = CST.V_GLOBAL;
        return (String) getProperty(CST.A_SCOPE);
    }

    @Override
    public String getShared() {
        String shared = (String) getProperty(CST.A_SHARED);
        if (shared == null)
            shared = CST.V_TRUE;
        return shared;
    }

    // @Override
    // public void setScope(String scope) {
    // if ((scope.equals(CST.V_TRUE) || (scope.equals(CST.V_FALSE))))
    // setProperty(CST.A_SCOPE, scope);
    // }

    // relation uses control
    public void addUses(ASMImpl dest) {
        if (uses.contains(dest))
            return;
        uses.add(dest);
        ((ASMImplImpl) dest).addInvUses(this);
        ((ASMSpecImpl) getSpec()).addRequires(dest.getSpec());
    }

    public void removeUses(ASMImpl dest) {
        for (ASMInst inst : instances) {
            for (ASMInst instDest : inst.getWireDests())
                if (instDest.getImpl() == dest) {
                    return; // it exists another instance that uses that destination. Do nothing.
                }
        }
        uses.remove(dest);
        ((ASMImplImpl) dest).removeInvUses(this);
        ((ASMSpecImpl) getSpec()).removeRequires(dest.getSpec());
    }

    private void addInvUses(ASMImpl orig) {
        invUses.add(orig);
    }

    private void removeInvUses(ASMImpl orig) {
        invUses.remove(orig);
    }

    @Override
    public Set<ASMImpl> getUses() {
        return Collections.unmodifiableSet(uses);
    }

    @Override
    public Set<ASMImpl> getInvUses() {
        return Collections.unmodifiableSet(invUses);
    }

    //
    @Override
    public void remove() {
        for (ASMInst inst : instances) {
            inst.remove();
        }
        ASMImplImpl.myBroker.removeImpl(this);
        // remove the APAM specific attributes in SAM
        if (samImpl != null) {
            try {
                samImpl.removeProperty(Attributes.APAMAPPLI);
                samImpl.removeProperty(Attributes.APAMCOMPO);
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Implementation getSamImpl() {
        return samImpl;
    }

    @Override
    public boolean isInstantiable() {
        return samImpl.isInstantiator();
    }

    @Override
    public String getSAMName() {
        return samImpl.getName();
    }

}
