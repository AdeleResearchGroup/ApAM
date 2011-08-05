package fr.imag.adele.apam;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASMImpl.ASMImplImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.CompExInst;
import fr.imag.adele.apam.apamAPI.CompExType;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Composite.Internal;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;

public class CompExTypeImpl extends ASMImplImpl implements CompExType {

    private final Composite me;
    private final String    mainImplName;
    private final ASMImpl   mainImpl;

    /**
     * Get access to the internal implementation of the wrapped instance
     */
	@Override
	public Internal asInternal() {
		return me.asInternal();
	}

    // mainSpec == this.getSpec ()
    private CompExTypeImpl(Composite meParam, Application appli, String compositeName, String implName, Implementation samImpl,
            Set<ManagerModel> models, ASMImpl mainImplparam) {
        //the composite itself as an ASMImpl
        //Warning father is itself !!
        super(meParam, compositeName, (ASMSpecImpl) mainImplparam.getSpec(), samImpl, null);
        me = meParam;
        mainImplName = implName;
        mainImpl = mainImplparam;
    }

    private CompExTypeImpl(Composite meParam, Composite father, String compositeName, String implName, Implementation samImpl,
            Set<ManagerModel> models, ASMImpl mainImplparam) {
        //the composite itself as an ASMImpl
        super(father, implName, (ASMSpecImpl) mainImplparam.getSpec(), samImpl, null);
        me = meParam;
        mainImplName = implName;
        mainImpl = mainImplparam;
    }

    public static CompExType createCompExType(Application appli, String implName, Set<ManagerModel> models) {
        ASMImpl mainImpl = null;
        if (implName == null) {
            System.err.println("ERROR : Name missing");
            new Exception("ERROR : Name missing").printStackTrace();
        }
        Composite meParam = new CompositeImpl(appli.getName(), null, appli, models);
        //BUG : the instance should not have been created. It will be lost 
        ASMInst mainInst = ((CST.apam)).resolveAppli(meParam, meParam, implName, implName, new HashSet<Filter>(),new ArrayList<Filter>());
        if (mainInst == null) {
            System.err.println("cannot find " + implName);
            return null;
        }
        mainImpl = mainInst.getImpl();
        if (mainImpl == null) {
            System.err.println("Error parameters missing for CompomEx " + implName);
            return null;
        }
        CompExType newComp = new CompExTypeImpl(meParam, appli, appli.getName(),implName, mainImpl.getSamImpl(), models, mainImpl);
        return newComp;
    }

    public static CompExType createCompExType(Composite implComposite, String compositeName, String implName, Set<ManagerModel> models) {
        ASMImpl mainImpl = null;
        Composite meParam = new CompositeImpl(compositeName, implComposite, implComposite.getApplication(), models);
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
        return new CompExTypeImpl(meParam, implComposite,compositeName, implName, mainImpl.getSamImpl(), models, mainImpl);
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
        String mainImplName = null;
        Set<ManagerModel> models = null;
        String[] interfaces = null;
        try {
            implName = (String) samImpl.getProperty(CST.PROPERTY_IMPLEMENTATION_NAME);
            mainImplName = (String) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MAIN_IMPLEMENTATION);
            specName = (String) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MAIN_SPECIFICATION);
            models = (Set<ManagerModel>) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MODELS);
            //TODO
            //interfaces = samImpl.
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        
        String compositeName = (implName != null) ? implName : samImpl.getName();
        
        ASMImpl mainImpl = null;
        Composite meParam = new CompositeImpl(compositeName, implComposite, implComposite.getApplication(), models);
        if (mainImplName != null) { //Look for by name

            mainImpl = ((CST.apam)).resolveImplByName(implComposite, implComposite, mainImplName, mainImplName,
                            null, null);
            if (mainImpl == null) {
                System.err.println("cannot find " + mainImplName);
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
        return new CompExTypeImpl(meParam, implComposite, compositeName, implName, samImpl, models, mainImpl);
    }

    public static CompExType createCompExType(Application appli, String name, Set<ManagerModel> models,
            String implName, URL url, String specName, Attributes properties) {
        ASMImpl mainImpl;
        Composite meParam = new CompositeImpl(name, null, appli, models);
        mainImpl = CST.ASMImplBroker.createImpl(meParam, implName, url, specName, properties);
        //Warning : we provide the main impl sam object instead of the composite object (which is non existing).
        return new CompExTypeImpl(meParam, appli,name, implName, mainImpl.getSamImpl(), models, mainImpl);
    }

    public static CompExType createCompExType(Composite implComposite, String name, Set<ManagerModel> models,
            String implName, URL url, String specName, Attributes properties) {
        ASMImpl mainImpl;
        Application appli = implComposite.getApplication();
        Composite meParam = new CompositeImpl(name, implComposite, appli, models);
        mainImpl = CST.ASMImplBroker.createImpl(meParam, implName, url, specName, properties);
        //Warning : we provide the main impl sam object instead of the composite object (which is non existing).
        return new CompExTypeImpl(meParam, appli, name, implName, mainImpl.getSamImpl(), models, mainImpl);
    }

    
    public static CompExType createCompExType(Application appli, String specName, String[] interfaces, Set<ManagerModel> models) {
        
        if ((specName == null) && (interfaces == null)) {
            System.err.println("ERROR : Parameter missing");
            new Exception("ERROR : Parameter missing").printStackTrace();
        }
        
        Composite meParam = new CompositeImpl(appli.getName(), null, appli, models);
        ASMImpl mainImpl = CST.apam.resolveSpecByName(meParam, meParam, interfaces, specName, new HashSet<Filter>(),new ArrayList<Filter>());
        if (mainImpl == null) {
            System.err.println("Error resolving specification for CompomEx " + specName + " interface "+ interfaces);
            return null;
        }
        CompExType newComp = new CompExTypeImpl(meParam, appli, appli.getName() ,mainImpl.getASMName(), mainImpl.getSamImpl(), models, mainImpl);
        return newComp;
        
    }

    @Override
    public ASMImpl getMainImpl() {
        return mainImpl;
    }

	@Override
	public ASMSpec getMainSpec() {
		return getMainImpl().getSpec();
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
    public Composite createComposite(String name, Set<ManagerModel> models) {
        return me.createComposite(name, models);
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
	public Set<Composite> getDependents() {
		return me.getDependents();
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

    /**
     * WARNING This method has the same signature in ASMImpl and Composite, but with very different meanings :
     * 
     * - For ASMImpl is the list of created instances (and then is a homogeneous list of CompExInst)
     * - For Composite it's the list of contained instances (an can be any instance of any type)
     * 
     * TODO Perhaps we should rename one of the methods to avoid some potential nasty bugs 
     * 
     */
    @Override
    public void addInst(ASMInst inst) {
    	/*
    	 * If this is one of my instances add it to my list
    	 */
    	if ( (inst instanceof CompExInst) && (inst.getImpl().equals(this)) ) {
    		super.addInst(inst);
    	}
    	
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



}
