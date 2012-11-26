package fr.imag.adele.apam.declarations;

/**
 * This class declares a field or method in a java implementation that must be injected with a resource by the runtime
 * when resolving dependencies.
 * 
 * @author vega
 * 
 */
public abstract class DependencyInjection {

    /**
     * The implementation associated to this injection
     */
    protected final AtomicImplementationDeclaration implementation;

    /**
     * The dependency that must be resolved to get the injected resource.
     */
    protected DependencyDeclaration                 dependency;

    protected DependencyInjection(AtomicImplementationDeclaration implementation) {

        // bidirectional reference to declaration
        this.implementation = implementation;
        this.implementation.getDependencyInjections().add(this);
    }

    /**
     * Sets the dependency that will be injected
     */
    public void setDependency(DependencyDeclaration dependency) {

        assert dependency.getComponent() == implementation.getReference();

        // bidirectional reference to dependency
        this.dependency = dependency;
        this.dependency.getInjections().add(this);
    }

    /**
     * The dependency that must be resolved
     */
    public DependencyDeclaration getDependency() {
        return dependency;
    }

    /**
     * The type of the resource that will be injected
     */
    public abstract ResourceReference getResource();

    /**
     * An unique identifier for this injection, within the scope of the declaring implementation
     * and dependency
     */
    public abstract String getName();

    /**
     * Whether this injection accepts collection dependencies
     */
    public abstract boolean isCollection();

    /**
     * Whether the injection definition is valid in the instrumented code
     */
    public abstract boolean isValidInstrumentation();

    /**
     * An injected field declaration
     */
    public static class Field extends DependencyInjection {

        private final String fieldName;

        public Field(AtomicImplementationDeclaration implementation, String fieldName) {
            super(implementation);
            this.fieldName = fieldName;
        }

        /**
         * The name of the field to inject
         */
        @Override
        public String getName() {
            return fieldName;
        }

        @Override
        public boolean isValidInstrumentation() {
            try {
                implementation.getInstrumentation().getFieldType(fieldName);
                return true;
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        /**
         * The type of the resource that will be injected in the field
         */
        @Override
        public ResourceReference getResource() {
            try {
                return implementation.getInstrumentation().getFieldType(fieldName);
            } catch (NoSuchFieldException e) {
                return new UndefinedReference(fieldName,ResourceReference.class);
            }
        }

        /**
         * whether this field is a collection or not
         */
        @Override
        public boolean isCollection() {
            try {
                return implementation.getInstrumentation().isCollectionField(fieldName);
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "field " + getName();
        }

    }
    

    /**
     * Callback with a return
     * 
     * @author Mehdi
     * 
     */
    public static class CallbackWithReturn extends DependencyInjection {

        private final String      methodName;
        private final String      type;
        private ResourceReference resourceRef;
        private Boolean           isCollection;
        private Boolean           hasInstrumentation;

        public CallbackWithReturn(AtomicImplementationDeclaration implementation, String methodName, String type) {
            super(implementation);
            this.methodName = methodName;
            this.type = type;
        }

        @Override
        public ResourceReference getResource() {
            if (resourceRef == null) {
                try {
                    resourceRef = implementation.getInstrumentation().getCallbackReturnType(methodName, type);
                } catch (NoSuchMethodException e) {
                    resourceRef = new UndefinedReference(methodName,MessageReference.class);
                }
            }
            return resourceRef;
        }

        @Override
        public String getName() {
            return methodName;
        }

        @Override
        public boolean isCollection() {
            if (isCollection == null) {
                try {
                    isCollection = implementation.getInstrumentation().isCollectionReturn(methodName, type);
                } catch (NoSuchMethodException e) {
                    isCollection = false;
                }
            }
            return isCollection;
        }

        @Override
        public boolean isValidInstrumentation() {
            if (hasInstrumentation==null){
                try {
                    implementation.getInstrumentation().getCallbackReturnType(methodName, type);
                    hasInstrumentation= true;
                } catch (NoSuchMethodException e) {
                    hasInstrumentation= false;
                }
            }
            return hasInstrumentation;
        }

        @Override
        public String toString() {
            return "method " + getName();
        }

    }

    /**
     * Callback with argument
     * 
     * @author Mehdi
     * 
     */
    public static class CallbackWithArgument extends DependencyInjection {

        private final String      methodName;
        private final String      type;
        private ResourceReference resourceRef;
        private Boolean           isCollection;
        private Boolean           hasInstrumentation;

        public CallbackWithArgument(AtomicImplementationDeclaration implementation, String methodName, String type) {
            super(implementation);
            this.methodName = methodName;
            this.type = type;

        }

        @Override
        public ResourceReference getResource() {
            if (resourceRef == null) {
                try {
                    resourceRef = implementation.getInstrumentation().getCallbackArgType(methodName, type);
                } catch (NoSuchMethodException e) {
                    resourceRef =  new UndefinedReference(methodName,MessageReference.class);
                }
            }
            return resourceRef;
        }

        @Override
        public String getName() {
            return methodName;
        }

        @Override
        public boolean isCollection() {
            if (isCollection == null) {
                try {
                    isCollection = implementation.getInstrumentation().isCollectionArgument(methodName, type);
                } catch (NoSuchMethodException e) {
                    isCollection = false;
                }
            }
            return isCollection;
        }

        @Override
        public boolean isValidInstrumentation() {
            if (hasInstrumentation == null) {
                try {
                    resourceRef = implementation.getInstrumentation().getCallbackArgType(methodName, type);
                    hasInstrumentation = true;
                } catch (NoSuchMethodException e) {
                    hasInstrumentation = false;
                }
            }
            return hasInstrumentation;
        }

        @Override
        public String toString() {
            return "method " + getName();
        }

    }

  
}
