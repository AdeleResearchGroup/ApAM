package fr.imag.adele.apam.core;


public class CallbackMethod {
    
    public static enum CallbackTrigger {
        Added, Removed, onInit, onRemove
    }
    /**
     * The atomic implementation declaring this injection
     */
    protected final AtomicImplementationDeclaration implementation;
    
    /**
     * The name of the method that must be called
     */
    protected final String methodName;
    
    /**
     * The name of the trigger 
     */
    protected final CallbackTrigger trigger;
    
    /**
     * The method has Instance as a unique argument
     */
    protected boolean hasInstanceArgument = false;

    
    public CallbackMethod(AtomicImplementationDeclaration implementation, CallbackTrigger trigger, String methodName) {

        assert implementation != null;
        assert trigger != null;
        assert methodName != null;
        
        this.implementation = implementation;
        this.methodName      = methodName;
        this.trigger = trigger;
    }

    /**
     * The component declaring this injection
     */
    public AtomicImplementationDeclaration getImplementation() {
        return implementation;
    }

    /**
     * The name of the method to call
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * The name of the method trigger 
     */
    public CallbackTrigger getTrigger() {
        return trigger;
    }

    
    public boolean isValidInstrumentation() {
        try {
            hasInstanceArgument = implementation.getInstrumentation().checkCallback(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public boolean hasAnInstanceArgument(){
        return hasInstanceArgument;
    }
    
    @Override
    public String toString() {
        return "Methdo name: " + methodName   ;
    }

}
