/**
 * Copyright 2011-2014 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.declarations.instrumentation;

/**
 * This dummy instrumented class is used when only the class name is available
 * 
 * @author thibaud
 */
public class UnloadedClassMetadata implements InstrumentedClass {

    private final String className;

    public UnloadedClassMetadata(String className) {
        this.className	= className;
    }

    @Override
    public String getName() {
        return className;
    }

    @Override
    public String getFieldType(String fieldName) throws NoSuchFieldException {
        throw new NoSuchFieldException("Unloaded class, reflection metadata not available");
    }

    @Override
	public String getDeclaredFieldType(String fieldName) throws NoSuchFieldException {
        throw new NoSuchFieldException("Unloaded class, reflection metadata not available");
    }
    
    @Override
    public int getMethodParameterNumber(String methodName, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("Unloaded class, reflection metadata not available");
    }

    @Override
    public String getMethodParameterType(String methodName, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("Unloaded class, reflection metadata not available");
    }

    @Override
    public String[] getMethodParameterTypes(String methodName, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("Unloaded class, reflection metadata not available");
    }

    @Override
    public String getMethodReturnType(String methodName, String signature, boolean includeInherited) throws NoSuchMethodException {
        throw new NoSuchMethodException("Unloaded class, reflection metadata not available");
    }

    @Override
    public boolean isCollectionField(String fieldName) throws NoSuchFieldException {
        throw new NoSuchFieldException("Unloaded class, reflection metadata not available");
    }
    
    @Override
    public boolean isMessageQueueField(String fieldName) throws NoSuchFieldException {
        throw new NoSuchFieldException("Unloaded class, reflection metadata not available");
    }
}