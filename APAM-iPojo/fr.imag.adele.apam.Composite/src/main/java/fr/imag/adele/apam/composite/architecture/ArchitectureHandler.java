package fr.imag.adele.apam.composite.architecture;

import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;

import fr.imag.adele.apam.composite.ApplicationInstance;
import fr.imag.adele.apam.composite.CompositeFactory;
import fr.imag.adele.apam.composite.CompositeHandler;

/**
 * Composite Architecture Handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ArchitectureHandler extends CompositeHandler implements Architecture {

    /**
     * Composite Handler type.
     */
    public static final String HANDLER_TYPE = "apam.composite";

    /**
     * Name of the component.
     */
    private String m_name;

    
    /**
     * Reference on the composite manager.
     */
    private ApplicationInstance m_instance;
    
    /**
     * Composite Factory.
     */
    private CompositeFactory m_factory;

    @Override public final void setFactory(Factory factory) {
        m_factory = (CompositeFactory) factory;
    }

    @Override protected final void attach(ComponentInstance instance) {
    	m_instance = (ApplicationInstance) instance;
    }
    
    
    @Override public final Logger getLogger() {
        return m_factory.getLogger();
    }
    
    @Override public final Handler getHandler(String name) {
        return m_instance.getHandler(name);
    }

    /**
     * Configure the handler.
     * 
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    
	@Override @SuppressWarnings("rawtypes") public void configure(Element metadata, Dictionary configuration) {
        m_name = (String) configuration.get("instance.name");
    }

    /**
     * Stop the handler.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
	@Override public void stop() {
        // Nothing to do.
    }

    /**
     * Start the handler.
     * @see org.apache.felix.ipojo.Handler#start()
     */
	@Override public void start() { 
        info("Start apam.composite architecture handler with " + m_name + " name");
    }

    /**
     * Get the instance description.
     * @return the instance description
     * @see org.apache.felix.ipojo.architecture.Architecture#getDescription()
     */
    public InstanceDescription getInstanceDescription() {
        return m_instance.getInstanceDescription();
    }


}
