package fr.imag.adele.apam.composite;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.InstanceStateListener;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

public class ApplicationInstance implements ComponentInstance, InstanceStateListener {

	
    /**
     * The context of the component.
     */
    private final BundleContext m_context;

    /**
     * Parent factory (ComponentFactory).
     */
    private final CompositeFactory m_factory;

    /**
     * Composite Handler list.
     */
    private HandlerManager[] m_handlers;

    /**
     * The instance description.
     */
    private final InstanceDescription m_description;
    
    /**
     * Name of the component instance.
     */
    private String m_name;

    /**
     * Component state (STOPPED at the beginning).
     */
    private int m_state = STOPPED;
	
    /**
     * Instance State Listener List.
     */
     
    private List<InstanceStateListener> m_listeners = new ArrayList<InstanceStateListener>();

	public ApplicationInstance(CompositeFactory factory, BundleContext context, HandlerManager[] handlers) {
        m_factory = factory;
        m_context = context;
        m_handlers = handlers;
        m_description = new InstanceDescription(m_factory.getComponentDescription(), this);

    }

    /**
     * Return the instance description of this instance.
     * @return the instance description.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceDescription()
     */
    public InstanceDescription getInstanceDescription() {
        return m_description;
    }

    /**
     * Return a specified handler.
     * @param name : class name of the handler to find
     * @return : the handler, or null if not found
     */
    public CompositeHandler getHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            HandlerFactory fact = (HandlerFactory) m_handlers[i].getFactory();
            if (fact.getHandlerName().equals(name)) {
                return (CompositeHandler)m_handlers[i].getHandler();
            }
        }
        return null;
    }

    /**
     * Get the instance name.
     * @return the instance name
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceName()
     */
    public String getInstanceName() {
        return m_name;
    }

    /**
     * Configure the instance manager. Stop the existing handler, clear the
     * handler list, change the metadata, recreate the handler
     * 
     * @param metadata : the component type metadata
     * @param configuration : the configuration of the instance
     * @throws ConfigurationException : occurs when the component type are incorrect.
     */
    public void configure(Element metadata, Dictionary<String,String> configuration) throws ConfigurationException {        
        // Add the name
        m_name = (String) configuration.get("instance.name");
        
        // Create the standard handlers and add these handlers to the list
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].init(this, metadata, configuration);
        }
    }

    /**
     * Reconfigure the current instance.
     * @param configuration : the new instance configuration.
     * @see org.apache.felix.ipojo.ComponentInstance#reconfigure(java.util.Dictionary)
     */
    public void reconfigure( @SuppressWarnings("rawtypes") Dictionary configuration) {
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].reconfigure(configuration);
        }
    }

    /**
     * Get the bundle context used by this instance.
     * @return the parent context of the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#getContext()
     */
    public BundleContext getContext() {
        return m_context;
    }

    /**
     * Get the factory which create this instance.
     * @return the factory of the component
     * @see org.apache.felix.ipojo.ComponentInstance#getFactory()
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }

    /**
     * Check if the instance is started.
     * @return true if the instance is started.
     * @see org.apache.felix.ipojo.ComponentInstance#isStarted()
     */
    public boolean isStarted() {
        return m_state > STOPPED;
    }

    
    /**
     * Start the instance manager.
     */
    public synchronized void start() {
        if (m_state > STOPPED) {
            return;
        } // Instance already started


        // The new state of the component is UNRESOLVED
        m_state = INVALID;

        
        // Plug handler descriptions
        for (int i = 0; i < m_handlers.length; i++) {
            m_description.addHandler( m_handlers[i].getHandler().getDescription());
        }

        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].start();
            m_handlers[i].addInstanceStateListener(this);
        }
        
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i].getState() != VALID) {
                setState(INVALID);
                return;
            }
        }
        setState(VALID);
        
    }

    /**
     * Stop the instance manager.
     */
    public synchronized void stop() {
        if (m_state <= STOPPED) {
            return;
        } // Instance already stopped

        setState(INVALID);
        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].removeInstanceStateListener(this);
            m_handlers[i].stop();
        }

        m_state = STOPPED;
        
        for (int i = 0; i < m_listeners.size(); i++) {
            ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, STOPPED);
        }
    }

    /**
     * State Change listener callback.
     * This method is notified at each time a plugged handler becomes invalid.
     * @param instance : changing instance 
     * @param newState : new state
     * @see org.apache.felix.ipojo.InstanceStateListener#stateChanged(org.apache.felix.ipojo.ComponentInstance, int)
     */
    public synchronized void stateChanged(ComponentInstance instance, int newState) {
        if (m_state <= STOPPED) { return; }
     
        // Update the component state if necessary
        if (newState == INVALID && m_state == VALID) {
            // Need to update the state to UNRESOLVED
            setState(INVALID);
            return;
        }
        if (newState == VALID && m_state == INVALID) {
            // An handler becomes valid => check if all handlers are valid
            boolean isValid = true;
            for (int i = 0; i < m_handlers.length; i++) {
                isValid = isValid && m_handlers[i].getState() == VALID;
            }
            
            if (isValid) { setState(VALID); }
        }
        if (newState == DISPOSED) {
            kill();
        }
    }

    /**
     * Kill the current instance.
     * Only the factory of this instance can call this method.
     */
    protected synchronized void kill() {
        if (m_state > STOPPED) { stop(); }
        
        for (int i = 0; i < m_listeners.size(); i++) {
            ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, DISPOSED);
        }

        // Cleaning
        m_state = DISPOSED;
        
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].dispose();
        }
        m_handlers = new HandlerManager[0];
        m_listeners.clear();
    }
    
    
    /**
     * Get the actual state of the instance.
     * @return the actual state of the instance
     * @see org.apache.felix.ipojo.ComponentInstance#getState()
     */
    public int getState() {
        return m_state;
    }

    /**
     * Set the state of the component. 
     * if the state changed call the stateChanged(int) method on the handlers.
     * @param state : new state
     */
    public void setState(int state) {
        if (m_state != state) {
            if (state > m_state) {
                // The state increases (Stopped = > IV, IV => V) => invoke handlers from the higher priority to the lower
                m_state = state;
                for (int i = 0; i < m_handlers.length; i++) {
                    m_handlers[i].getHandler().stateChanged(state);
                }
            } else {
                // The state decreases (V => IV, IV = > Stopped, Stopped => Disposed)
                m_state = state;
                for (int i = m_handlers.length - 1; i > -1; i--) {
                    m_handlers[i].getHandler().stateChanged(state);
                }
            }
            
            for (int i = 0; i < m_listeners.size(); i++) {
                ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, state);
            }
        }
    }


    /** 
     * Dispose the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#dispose()
     */
    public void dispose() {
        if (m_state > STOPPED) { stop(); }
        
        for (int i = 0; i < m_listeners.size(); i++) {
            ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, DISPOSED);
        }
        
        m_factory.disposed(this);

        // Cleaning
        m_state = DISPOSED;
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].dispose();
        }
        m_handlers = new HandlerManager[0];
        m_listeners.clear();
    }

    /**
     * Add an instance to the created instance list.
     * @param listener : the instance state listener to add.
     * @see org.apache.felix.ipojo.ComponentInstance#addInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void addInstanceStateListener(InstanceStateListener listener) {
        synchronized (m_listeners) {
            m_listeners.add(listener);
        }
    }

    /**
     * Remove an instance state listener.
     * @param listener : the listener to remove
     * @see org.apache.felix.ipojo.ComponentInstance#removeInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void removeInstanceStateListener(InstanceStateListener listener) {
        synchronized (m_listeners) {
            m_listeners.remove(listener);
        }
    }

}
