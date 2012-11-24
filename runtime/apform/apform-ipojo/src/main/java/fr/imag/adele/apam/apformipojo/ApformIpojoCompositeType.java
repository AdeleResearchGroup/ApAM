package fr.imag.adele.apam.apformipojo;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.declarations.CompositeDeclaration;

public class ApformIpojoCompositeType extends ApformIpojoImplementation implements ApformCompositeType {

   
    /**
     * The list of models associated to this composite
     */
    private final Set<ManagerModel> managerModels =  new HashSet<ManagerModel>();

    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApformIpojoCompositeType(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

        @SuppressWarnings("unchecked")
        Enumeration<String> paths = context.getBundle().getEntryPaths("/");
        while (paths.hasMoreElements()) {
        	
 				String modelFileName = paths.nextElement();
 				
 				if (! modelFileName.endsWith(".cfg"))
 					continue;
 				
 				if (! modelFileName.startsWith(getDeclaration().getName()))
 					continue;
 				
	            String managerName = modelFileName.substring(getDeclaration().getName().length()+1, modelFileName.lastIndexOf(".cfg"));
	            URL modelURL = context.getBundle().getEntry(modelFileName);
				managerModels.add(new ManagerModel(managerName, modelURL));
        }
        
    }

    @Override
	public void check(Element element) throws ConfigurationException {
    	super.check(element);
	}
	
    @Override
    public CompositeDeclaration getDeclaration() {
    	return (CompositeDeclaration) super.getDeclaration();
    };
    /**
     * This factory doesn't have an associated instrumented class
     */
    @Override
    public boolean hasInstrumentedCode() {
    	return false;
    }
    
    /**
     * Gets the class name.
     * 
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    @Override
    public String getClassName() {
        return getDeclaration().getName();
    }

    /**
     * Get The list of models associated to this composite
     */
    public Set<ManagerModel> getModels() {
        return managerModels;
    }

}
