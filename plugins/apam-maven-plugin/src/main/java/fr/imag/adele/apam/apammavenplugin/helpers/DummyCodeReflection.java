package fr.imag.adele.apam.apammavenplugin.helpers;

/**
 * Created by thibaud on 12/08/2014.
 */

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;

/**
 * This dummy CodeReflection must only be used if we have only the class name
 * In the case of ACR Parsing, the real class is not available
 */
public class DummyCodeReflection implements AtomicImplementationDeclaration.CodeReflection {

    String className;

    public DummyCodeReflection(String className) {
        this.className=className;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public ResourceReference getFieldType(String fieldName) throws NoSuchFieldException {
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
}