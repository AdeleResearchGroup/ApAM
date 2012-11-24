package fr.imag.adele.apam.apformipojo;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

public class ApformIpojoInstanceDeclaration extends ApformIpojoComponent {

	/**
	 * A dynamic reference to the apform implementation
	 */
	protected final ImplementationTracker implementationTracker;

	/**
	 * The ipojo instance corresponding to this declaration
	 */
	private ComponentInstance iPojoInstance;
	
	
	/**
	 * Creates a new declaration
	 */
	public ApformIpojoInstanceDeclaration(BundleContext context, Element element) throws ConfigurationException {
		super(context, element);
		try {
			String classFilter		= "(" + Constants.OBJECTCLASS + "=" + Factory.class.getName() + ")";
			String factoryFilter	= "(" + "factory.name" + "=" + getDeclaration().getImplementation().getName() + ")";
			String filter			= "(& "+classFilter+factoryFilter+")";
			
			implementationTracker = new ImplementationTracker(context, context.createFilter(filter));
		} catch (InvalidSyntaxException e) {
			throw new ConfigurationException(e.getLocalizedMessage());
		}
	}

	@Override
	public InstanceDeclaration getDeclaration() {
		return (InstanceDeclaration) super.getDeclaration();
	}
	
	@Override
	public boolean hasInstrumentedCode() {
		return false;
	}

	@Override
	public boolean isInstantiable() {
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
        return this.getDeclaration().getName();
    }
	
	@Override
	public ApformIpojoInstance createApamInstance(IPojoContext context, HandlerManager[] handlers) {
		throw new UnsupportedOperationException("APAM instance declaration is not instantiable");
	}

	@Override
	protected void bindToApam(Apam apam) {
		implementationTracker.open();
	}

	@Override
	protected void unbindFromApam(Apam apam) {
		implementationTracker.close();
	}


	/**
     * A class to dynamically track the apform implementation. This allows to dynamically create the instance
     * represented by this declaration
     * 
     */
    class ImplementationTracker extends ServiceTracker {

        public ImplementationTracker(BundleContext context, Filter filter) {
            super(context,filter,null);
        }

        @Override
        public Object addingService(ServiceReference reference) {

			if (iPojoInstance != null)
				return null;

        	try {
				
	        	Factory factory 			= (Factory) this.context.getService(reference);
	        	Properties configuration	= new Properties();
	        	configuration.put(ApformIpojoInstance.ATT_DECLARATION, ApformIpojoInstanceDeclaration.this.getDeclaration());
				iPojoInstance = factory.createComponentInstance(configuration);
				
				return factory;
				
			} catch (Exception instantiationError) {
				instantiationError.printStackTrace(System.err);
				return null;
			}
            
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (iPojoInstance != null)
            	iPojoInstance.dispose();

            this.context.ungetService(reference);
            iPojoInstance = null;
        }

    }


	@Override
	public void setProperty(String attr, String value) {
		// TODO Auto-generated method stub
		// faire un refactoring pour ne pas heriter de ApformComponent
		
	}

}
