package fr.imag.adele.apam.apform.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;


/**
 * This is the base class of all APAM callbacks that are invoked on component instances.
 * 
 * All callbacks are methods declared in the component implementation, with an optional
 * parameter whose type depends on the kind of callback.
 * 
 * To allow some basic type checking at invocation, we use the parameter type to represent
 * the type of the argument.
 * 
 * @author vega
 *
 */
public abstract class InstanceCallback<T> extends Callback {

	/**
	 * The associated Apam component instance to be injected
	 */
	protected final ApamInstanceManager.Apform instance;

	/**
	 * Whether invocation requires an argument
	 */
    protected boolean requiresArgument;

	/**
	 * The type of the required argument
	 */
    protected Class<?> argumentType;
    
    
	protected InstanceCallback(ApamInstanceManager.Apform instance, String method) throws ConfigurationException {
		
		super(method,(String[])null,false,instance.getManager());
		
		this.instance	= instance;
	}

	/**
	 * Uses introspection to search for the callback method in the instrumented class of the
	 * component implementation
	 */
	@Override
	protected void searchMethod() throws NoSuchMethodException {
		
		
		m_methodObj 		= null;
		requiresArgument	= false;
		argumentType		= null;
		
		/*
		 * Try to find the declared method with the specified name that best matches the callback signature.
		 * Try first the locally declared methods, and only if not found search publicly declared methods
		 * of super classes.
		 * 
		 * NOTE Notice that we force class loading of the implementation class (and its super classes) by using
		 * reflection to search for methods.
		 */
		
		Method[] candidates = instance.getManager().getClazz().getDeclaredMethods();
        int match = searchMethod(candidates);
        if (match == -1) {
        	candidates = instance.getManager().getClazz().getMethods();
        	match = searchMethod(candidates);
        }
        
        if (match == -1)
        	throw new NoSuchMethodException(getMethod());
        
        m_methodObj = candidates[match];
        if (! m_methodObj.isAccessible()) { 
            m_methodObj.setAccessible(true);
        }
        
        requiresArgument	= m_methodObj.getParameterTypes().length == 1;
        argumentType		= requiresArgument ? m_methodObj.getParameterTypes()[0] : null;
	}

	/**
	 * Search the method associated with this callback in the specified array.
	 * 
   	 * We are looking for a method with an optional, single, parameter of the expected type.
   	 * 
   	 * NOTE that if the same method name is defined with different signatures, we try to use the
   	 * most specific version.
	 * 
	 */
	protected int searchMethod(Method[] methods) {

	        Method candidate 			= null;
			int candidateIndex			= -1;
	        Class<?> candidateParameter	= null;
	        
	        for (int index = 0; index < methods.length; index++) {
				
	        	Method method = methods[index];
	        	
	        	/*
	        	 * Skip all methods not matching the name, or the number and type
	        	 * of parameter
	        	 */
	        	if (! method.getName().equals(getMethod()))
					continue;

	        	Class<?>[] parameters = method.getParameterTypes();
	        	if (parameters.length > 1)
	        		continue;

	           	Class<?> parameter = parameters.length == 1 ? parameters[0] : null;
	           	if (parameter != null && ! isExpectedParameter(parameter))
	        		continue;
	        	
	           	/*
	           	 * Register the candidate, but continue searching for a better match
	           	 */
	        	if (candidate == null) {
	        		candidate 			= method;
	        		candidateIndex		= index;
	        		candidateParameter	= parameter;
	        		continue;
	        	}
	        	
	        	/*
	        	 * If we find a method with a parameter, prefer it
	        	 */
	        	if (candidateParameter == null && parameter != null) {
	        		candidate 			= method;
	        		candidateIndex		= index;
	        		candidateParameter	= parameter;
	        		continue;
	        	}
	        	
	        	/*
	        	 * If we find a method with a parameter, and the current candidate also has a parameter
	        	 * prefer the method with the most specific type 
	        	 */
	        	if (candidateParameter != null && parameter != null && candidateParameter.isAssignableFrom(parameter)) {
	        		candidate 			= method;
	        		candidateIndex		= index;
	        		candidateParameter	= parameter;
	        		continue;
	        	}
			}
	        
	        return candidateIndex;

	}
	
	/**
	 * Whether invocation actually requires an argument or not
	 */
	public final boolean requiresArgument() {
		return requiresArgument;
	}

	/**
	 * The actual type of the parameter, if required
	 */
	public final Class<?> getArgumentType() {
		return argumentType;
	}
	
	/**
	 * Invokes the callback method with the specified optional argument.
	 * 
	 * If the argument doesn't match the parameter type, we try to perform automatic
	 * casting (specifically defined for each kind of callback)
	 */
	public Object invoke(T argument) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }

        Object actualArgument = requiresArgument ? argumentType.isInstance(argument) ? argument : cast(argument): null; 

		return call(instance.getServiceObject(), requiresArgument ? new Object[] {actualArgument} : new Object[0]);
	}

	/**
	 * Casts the argument of the callback, in cases where it doesn't match the parameter type of the method.
	 * 
	 * NOTE IMPORTANT If casting is not possible, this method should return the unmodified argument. In this
	 * way if there are errors in the declarations the user will get a class cast exception. 
	 */
	protected Object cast(T argument) {
		return argument;
	}

	/**
	 * The iPOJO metadata associated with the callback method to be invoked
	 */
	public MethodMetadata getMethodMetadata() {
		
		PojoMetadata metadata 	= instance.getManager().getFactory().getPojoMetadata();
		String[] arguments		= requiresArgument ? new String[] {m_methodObj.getParameterTypes()[0].getCanonicalName()}: new String[0];
		
		return metadata.getMethod(m_methodObj.getName(),arguments);
	}
	
	/**
	 * Test whether the specified method is the one invoked by this callback 
	 */
	public boolean invokes(Method method)  {
		return m_methodObj.equals(method);
	}
	
	
	/**
	 * Whether this is the expected parameter type of the callback.
	 * 
	 * This method must be overridden by kinds of callbacks depending on the specific method
	 * signature required.
	 */
	protected abstract boolean isExpectedParameter(Class<?> parameterType);

}
