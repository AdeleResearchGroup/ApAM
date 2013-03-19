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


public class CallbackMethod {
    
    public static enum CallbackTrigger {
        Bind, Unbind, onInit, onRemove
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
            switch (trigger) {
                case Bind:                   
                case Unbind:
                     implementation.getInstrumentation().getCallbacks(methodName,false);
                    return true;
                case onInit:
                case onRemove:
                    implementation.getInstrumentation().getCallbacks(methodName,false);
                    return true;
                default:
                    return false;
            }
           
        } catch (NoSuchMethodException e) {
            return false;
        }
    
    }


    
    @Override
    public String toString() {
        return "Methdo name: " + methodName   ;
    }

}
