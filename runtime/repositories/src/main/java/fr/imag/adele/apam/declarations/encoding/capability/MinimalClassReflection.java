package fr.imag.adele.apam.declarations.encoding.capability;

/**
 * Created by thibaud on 12/08/2014.
 */

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;

/**
 * This dummy CodeReflection must only be used if we have only the class name
 * In the case of ACR Parsing, the real class is not available
 */
public class MinimalClassReflection implements AtomicImplementationDeclaration.CodeReflection {

    String className;

    public MinimalClassReflection(String className) {
        this.className=className;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getFieldType(String fieldName) throws NoSuchFieldException {
        throw new NoSuchFieldException("This class has no fields (not a real class, only a ClassName holder)");
    }

    @Override
    public int getMethodParameterNumber(String methodName, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("This class has no methods (not a real class, only a ClassName holder)");
    }

    @Override
    public String getMethodParameterType(String methodName, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("This class has no methods (not a real class, only a ClassName holder)");
    }

    @Override
    public String[] getMethodParameterTypes(String methodName, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("This class has no methods (not a real class, only a ClassName holder)");
    }

    @Override
    public String getMethodReturnType(String methodName, String signature, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("This class has no methods (not a real class, only a ClassName holder)");
    }

    @Override
    public boolean isCollectionField(String fieldName) throws NoSuchFieldException {
        throw new NoSuchFieldException("This class has no fields (not a real class, only a ClassName holder)");
    }
    
    @Override
    public boolean isMessageQueueField(String fieldName) throws NoSuchFieldException {
        throw new NoSuchFieldException("This class has no fields (not a real class, only a ClassName holder)");
    }
}