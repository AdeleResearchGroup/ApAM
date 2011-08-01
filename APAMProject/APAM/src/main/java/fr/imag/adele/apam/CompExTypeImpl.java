package fr.imag.adele.apam;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASMImpl.ASMImplImpl;
import fr.imag.adele.apam.ASMImpl.ASMInstImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.CompExType;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;

public class CompExTypeImpl extends ASMImplImpl implements CompExType, Composite {

    //private final CompositeImpl me;
    private final Composite me;
    private final String    mainImplName;
    private final String    mainSpecName;
    private final ASMImpl   mainImpl;

    //    private final Set<ManagerModel> models;
    public CompositeImpl getCompositeMe() {
        return (CompositeImpl) me;
    }

    // mainSpec == this.getSpec ()
    private CompExTypeImpl(Composite meParam, Application appli, String implName, Implementation samImpl,
            Set<ManagerModel> models, ASMImpl mainImplparam) {
        //the composite itself as an ASMImpl
        //Warning father is itself !!
        super(meParam, implName, (ASMSpecImpl) mainImplparam.getSpec(), samImpl, null);
        me = meParam;
        mainImplName = implName;
        mainImpl = mainImplparam;
        mainSpecName = mainImpl.getSpec().getASMName();
    }

    private CompExTypeImpl(Composite meParam, Composite father, String implName, Implementation samImpl,
            Set<ManagerModel> models, ASMImpl mainImplparam) {
        //the composite itself as an ASMImpl
        super(father, implName, (ASMSpecImpl) mainImplparam.getSpec(), samImpl, null);
        me = meParam;
        mainImplName = implName;
        mainImpl = mainImplparam;
        mainSpecName = mainImpl.getSpec().getASMName();
    }

    public static CompExType createCompExType(Application appli, String implName, Set<ManagerModel> models) {
        ASMImpl mainImpl = null;
        if (implName == null) {
            System.err.println("ERROR : Name missing");
            new Exception("ERROR : Name missing").printStackTrace();
        }
        Composite meParam = new CompositeImpl(implName, null, appli, models);
        //BUG : the instance should not have been created. It will be lost 
        ASMInst mainInst = ((CST.apam)).resolveAppli(meParam, meParam, implName, implName, null, null);
        if (mainInst == null) {
            System.err.println("cannot find " + implName);
            return null;
        }
        mainImpl = mainInst.getImpl();
        if (mainImpl == null) {
            System.err.println("Error parameters missing for CompomEx " + implName);
            return null;
        }
        CompExType newComp = new CompExTypeImpl(meParam, appli, implName, mainImpl.getSamImpl(), models, mainImpl);
        return newComp;
    }

    public static CompExType createCompExType(Composite implComposite, String implName, Set<ManagerModel> models) {
        ASMImpl mainImpl = null;
        Composite meParam = new CompositeImpl(implName, implComposite, implComposite.getApplication(), models);
        if (implName == null) {
            System.err.println("ERROR : Name missing");
            new Exception("ERROR : Name missing").printStackTrace();
        }
        //BUG : the instance should not have been created. It will be lost 
        ASMInst mainInst = ((CST.apam)).resolveAppli(implComposite, implComposite, implName, implName, null, null);
        if (mainInst == null) {
            System.err.println("cannot find " + implName);
            return null;
        }
        mainImpl = mainInst.getImpl();
        if (mainImpl == null) {
            System.err.println("Error parameters missing for CompomEx " + implName);
            return null;
        }
        return new CompExTypeImpl(meParam, implComposite, implName, mainImpl.getSamImpl(), models, mainImpl);
    }

    /**
     * Create a compEx type, from its object in sam : samImpl.
     * Look for its main implementation, and deploys it if needed.
     * 
     * @param implComposite
     * @param samImpl
     * @return
     */
    public static CompExType createCompExType(Composite implComposite, Implementation samImpl) {
        String implName = null;
        String specName = null;
        Set<ManagerModel> models = null;
        String[] interfaces = null;
        try {
            implName = (String) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MAIN_IMPLEMENTATION);
            specName = (String) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MAIN_SPECIFICATION);
            models = (Set<ManagerModel>) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MODELS);
            //TODO
            //interfaces = samImpl.
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        ASMImpl mainImpl = null;
        Composite meParam = new CompositeImpl(samImpl.getName(), implComposite, implComposite.getApplication(), models);
        if (implName != null) { //Look for by name

            mainImpl = ((CST.apam)).resolveImplByName(implComposite, implComposite, implName, implName,
                            null, null);
            if (mainImpl == null) {
                System.err.println("cannot find " + implName);
                return null;
            }
        } else { //look for by its specification. Warning : do not select the composite again !
            if (specName != null) {
                mainImpl = null;
            } else //look for by its interfaces. Warning : do not select the composite again !
            if (interfaces != null) {
                mainImpl = null;
            }
            //Do not select another composite
            //            Set<Filter> constraints = new HashSet<Filter>();
            //            try {
            //                Filter f = FilterImpl.newInstance("(!(apam-composite=true))");
            //                constraints.add(f);
            //            } catch (InvalidSyntaxException e) {
            //                e.printStackTrace();
            //            }
            //resolveSpecByName
        }
        if (mainImpl == null) {
            System.err.println("Error parameters missing for CompomEx " + samImpl.getName());
            return null;
        }
        return new CompExTypeImpl(meParam, implComposite, implName, samImpl, models, mainImpl);
    }

    public static CompExType createCompExType(Application appli, String name, Set<ManagerModel> models,
            String implName, URL url, String specName, Attributes properties) {
        ASMImpl mainImpl;
        Composite meParam = new CompositeImpl(name, null, appli, models);
        mainImpl = CST.ASMImplBroker.createImpl(meParam, implName, url, specName, properties);
        //Warning : we provide the main impl sam object instead of the composite object (which is non existing).
        return new CompExTypeImpl(meParam, appli, implName, mainImpl.getSamImpl(), models, mainImpl);
    }

    public static CompExType createCompExType(Composite implComposite, String name, Set<ManagerModel> models,
            String implName, URL url, String specName, Attributes properties) {
        ASMImpl mainImpl;
        Application appli = implComposite.getApplication();
        Composite meParam = new CompositeImpl(name, implComposite, appli, models);
        mainImpl = CST.ASMImplBroker.createImpl(meParam, implName, url, specName, properties);
        //Warning : we provide the main impl sam object instead of the composite object (which is non existing).
        return new CompExTypeImpl(meParam, appli, implName, mainImpl.getSamImpl(), models, mainImpl);
    }

    public static CompExType createCompExType(Application appli, Set<ManagerModel> models, String implName, URL url,
            String specName, Attributes properties) {
        Composite meParam = new CompositeImpl(appli.getName(), null, appli, models);
        ASMImpl mainImpl = CST.ASMImplBroker.createImpl(meParam, implName, url, specName, properties);
        return new CompExTypeImpl(meParam, appli, implName, mainImpl.getSamImpl(), models, mainImpl);
    }

    @Override
    public ASMImpl getMainImpl() {
        return mainImpl;
    }

    @Override
    public ASMInst createInst(Composite instCompo, Attributes initialproperties) {
        //ASMInst mainInst = mainImpl.createInst(instCompo, initialproperties);
        CompExInstImpl compEx = (CompExInstImpl) CompExInstImpl.createCompExInst(this, instCompo, initialproperties);

        return compEx;
    }

    @Override
    public String getMainImplName() {
        return mainImplName;
    }

    @Override
    public String getName() {
        return me.getName();
    }

    @Override
    public Application getApplication() {
        return me.getApplication();
    }

    @Override
    public Composite createComposite(Composite father, String name, Set<ManagerModel> models) {
        return me.createComposite(father, name, models);
    }

    @Override
    public void addDepend(Composite destination) {
        me.addDepend(destination);
    }

    @Override
    public boolean removeDepend(Composite destination) {
        return me.removeDepend(destination);
    }

    @Override
    public Set<Composite> getDepend() {
        return me.getDepend();
    }

    @Override
    public boolean dependsOn(Composite destination) {
        return me.dependsOn(destination);
    }

    @Override
    public Composite getFather() {
        return me.getFather();
    }

    @Override
    public Set<Composite> getSons() {
        return me.getSons();
    }

    @Override
    public ManagerModel getModel(String name) {
        return me.getModel(name);
    }

    @Override
    public Set<ManagerModel> getModels() {
        return me.getModels();
    }

    @Override
    public void addSpec(ASMSpec spec) {
        me.addSpec(spec);
    }

    @Override
    public boolean containsSpec(ASMSpec spec) {
        return me.containsSpec(spec);
    }

    @Override
    public Set<ASMSpec> getSpecs() {
        return me.getSpecs();
    }

    @Override
    public void addImpl(ASMImpl impl) {
        me.addImpl(impl);
    }

    @Override
    public boolean containsImpl(ASMImpl spec) {
        return me.containsImpl(spec);
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return me.getImpls();
    }

    @Override
    public void addInst(ASMInst inst) {
        me.addInst(inst);
    }

    @Override
    public boolean containsInst(ASMInst inst) {
        return me.containsInst(inst);
    }

    @Override
    public Set<ASMInst> getInsts() {
        return me.getInsts();
    }

    @Override
    public void setMainSpec(ASMSpec spec) {
        new Exception("cannot set Spec in a CompExType").printStackTrace();
    }

    @Override
    public void setMainImpl(ASMImpl impl) {
        new Exception("cannot set Impl in a CompExType").printStackTrace();
    }

    @Override
    public void setMainInst(ASMInst inst) {
        new Exception("cannot set Inst in a CompExType").printStackTrace();
    }

    @Override
    public ASMSpec getMainSpec() {
        return mainImpl.getSpec();
    }

    @Override
    public ASMInst getMainInst() {
        new Exception("cannot get main inst in a CompExType").printStackTrace();
        return null;
    }

}
