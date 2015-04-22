package fr.imag.adele.apam.impl;

import java.util.concurrent.ThreadFactory;


/**
 * This is the thread factory used to create all threads in the different APAM pools. 
 * 
 * This ensures that all the used threads are demons and adds a name prefix to easily recognize APAM threads. This thread
 * factory actually delegates the thread creation to the default factory
 * 
 * @author vega
 *
 */
public class ThreadPoolFactory implements ThreadFactory {

    /**
     * The wrapped thread factory on which creation is delegated.
     */
    private final ThreadFactory delegate;

    /**
     * The prefix.
     */
    private final String prefix;

    public ThreadPoolFactory(String poolName, ThreadFactory delegate) {
    	this.delegate	= delegate;
    	prefix 			= "APAM " + poolName + " ";
	}
    
    public Thread newThread(Runnable r) {
        Thread thread = delegate.newThread(r);
        thread.setName(prefix + thread.getName());
        thread.setDaemon(true);
        return thread;
    }

}
