package fr.imag.adele.apam.apform.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.CallbackDeclaration;

/**
 * This is the callback associated to the APAM component lifecycle
 * 
 * @author vega
 */
public class LifecycleCallback extends Callback {

	private final ApamInstanceManager 					instance;
    private final AtomicImplementationDeclaration.Event trigger;
    private final CallbackDeclaration 					declaration;

    private boolean needsArgument;
    

	public LifecycleCallback(ApamInstanceManager instance, AtomicImplementationDeclaration.Event trigger, CallbackDeclaration declaration) throws ConfigurationException {
		super(declaration.getMethodName(),(String[])null,false,instance);
		
		this.instance		= instance;
		this.declaration	= declaration;
		this.trigger		= trigger;
		
		/*
		 * We force reflection meta-data calculation in the constructor to signal errors as soon as
		 * possible. This however has a cost in terms of early class loading.  
		 */
		try {
			searchMethod();
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException("invalid method declaration in callback "+declaration.getMethodName());
		}
	}
	
	
	public MethodMetadata getMethodMetadata() {
		
		PojoMetadata metadata 	= instance.getFactory().getPojoMetadata();
		String[] arguments		= needsArgument ? new String[] {m_methodObj.getParameterTypes()[0].getCanonicalName()}: new String[0];
		
		return metadata.getMethod(m_methodObj.getName(),arguments);
	}
	
	public boolean invokes(Method method)  {
		return m_methodObj.equals(method);
	}
	
	public boolean isTriggeredBy(AtomicImplementationDeclaration.Event trigger) {
		return this.trigger.equals(trigger);
	}
	
	public Object invoke(Instance instance) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }

		return call(needsArgument ? new Object[] {instance} : new Object[0]);
	}

	@Override
	protected void searchMethod() throws NoSuchMethodException {
		
		
		m_methodObj 	= null;
		needsArgument	= false;
		
		/*
		 * Try to find the declared method with the specified name that best matches the callback signature.
		 * Try first the locally declared methods, and only if not found search publicly declared methods
		 * of super classes.
		 * 
		 * NOTE Notice that we force class loading of the implementation class (and its super classes) by using
		 * reflection to search for methods.
		 */
		
		Method[] candidates = instance.getClazz().getDeclaredMethods();
        int match = searchMethod(candidates);
        if (match == -1) {
        	candidates = instance.getClazz().getMethods();
        	match = searchMethod(candidates);
        }
        
        if (match == -1)
        	throw new NoSuchMethodException(declaration.getMethodName());
        
        m_methodObj = candidates[match];
        if (! m_methodObj.isAccessible()) { 
            m_methodObj.setAccessible(true);
        }
        
        needsArgument	= m_methodObj.getParameterTypes().length == 1;
        
	}
	
	/**
	 * Search the method associated with this callback in the specified array
	 */
	private int searchMethod(Method[] methods) {

	        Method candidate 			= null;
			int candidateIndex			= -1;
	        Class<?> candidateParameter	= null;
	        
	        for (int index = 0; index < methods.length; index++) {
				
	        	Method method = methods[index];
	        	if (! method.getName().equals(declaration.getMethodName()))
					continue;

	        	/*
	        	 * We are looking for a method with an optional, single parameter of kind
	        	 * APAM instance
	        	 */
	        	
	        	Class<?>[] parameters = method.getParameterTypes();
	        	if (parameters.length > 1)
	        		continue;

	           	Class<?> parameter = parameters.length == 1 ? parameters[0] : null;
	           	if (parameter != null && ! parameter.isAssignableFrom(Instance.class))
	        		continue;
	        	
	        	if (candidate == null) {
	        		candidate 			= method;
	        		candidateIndex		= index;
	        		candidateParameter	= parameter;
	        		continue;
	        	}
	        	
	        	if (candidateParameter == null && parameter != null) {
	        		candidate 			= method;
	        		candidateIndex		= index;
	        		candidateParameter	= parameter;
	        		continue;
	        	}
	        	
	        	if (candidateParameter != null && parameter != null && candidateParameter.isAssignableFrom(parameter)) {
	        		candidate 			= method;
	        		candidateIndex		= index;
	        		candidateParameter	= parameter;
	        		continue;
	        	}
			}
	        
	        return candidateIndex;

	}
}
