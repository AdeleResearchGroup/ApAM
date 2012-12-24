/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
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
package fr.imag.adele.apam.declarations;

/**
 * This class declares a field in a java implementation that must be injected with a reference to a
 * message producer
 * 
 * @author vega
 * 
 */
public class MessageProducerMethodInterception {

    /**
     * The atomic implementation declaring this injection
     */
    protected final AtomicImplementationDeclaration implementation;

    /**
     * The name of the field that must be injected
     */
    protected final String                          methodName;

    /**
     * The resourceReference of this producer
     */
    private ResourceReference                       resourceReference;

    /**
     * The return type is a collection
     */
    private Boolean                                 collection;

    public MessageProducerMethodInterception(AtomicImplementationDeclaration implementation, String methodName) {

        assert implementation != null;
        assert methodName != null;

        this.implementation = implementation;
        this.methodName = methodName;
    }

    /**
     * The component declaring this injection
     */
    public AtomicImplementationDeclaration getImplementation() {
        return implementation;
    }

    /**
     * The name of the field to inject
     */
    public String getMethoddName() {
        return methodName;
    }

    /**
     * The type of the resource that will be injected in the field
     */
    public ResourceReference getResource() {
        if (resourceReference == null) {
            try {
                resourceReference = implementation.getInstrumentation().getCallbackReturnType(methodName, null);
            } catch (NoSuchMethodException e) {
                resourceReference = new UndefinedReference(methodName,MessageReference.class);
            }
        }
        return resourceReference;
    }

    /**
     * whether this field is a collection or not
     */
    public boolean isCollection() {
        if (collection == null) {
            try {
                collection =  implementation.getInstrumentation().isCollectionReturn(methodName, null);
            } catch (NoSuchMethodException e) {
                collection =  false;
            }
        }
        return collection;

    }

    @Override
    public String toString() {
        return "method name: " + methodName + ". Type: " + getResource().getJavaType() + (isCollection() ? "[]" : "");
    }

}