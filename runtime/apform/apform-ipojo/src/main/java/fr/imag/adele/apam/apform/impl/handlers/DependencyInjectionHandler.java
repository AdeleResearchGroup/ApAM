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
package fr.imag.adele.apam.apform.impl.handlers;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.service.wireadmin.WireAdmin;

import fr.imag.adele.apam.apform.impl.ApformImplementationImpl;
import fr.imag.adele.apam.apform.impl.ApformInstanceImpl;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.CallbackMethod;
import fr.imag.adele.apam.declarations.CallbackMethod.CallbackTrigger;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.DependencyInjection;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;

public class DependencyInjectionHandler extends ApformHandler {

    /**
     * The registered name of this iPojo handler
     */
    public static final String NAME = "injection";

    /**
     * The WireAdmin reference
     */
    private WireAdmin          wireAdmin;

    /**
     * Get the WireAdmin reference
     */
    public WireAdmin getWireAdmin() {
        return wireAdmin;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        /*
         * Add interceptors to delegate dependency resolution
         * 
         * NOTE All validations were already performed when validating the
         * factory @see initializeComponentFactory, including initializing
         * unspecified properties with appropriate default values. Here we just
         * assume metadata is correct.
         */

        if (!(getFactory() instanceof ApformImplementationImpl))
            return;

        ApformImplementationImpl implementation = (ApformImplementationImpl) getFactory();
        ImplementationDeclaration declaration = implementation.getDeclaration();

        if (!(declaration instanceof AtomicImplementationDeclaration))
            return;

        AtomicImplementationDeclaration primitive = (AtomicImplementationDeclaration) declaration;
        for (DependencyInjection injection : primitive.getDependencyInjections()) {

            /*
             * Get the field interceptor depending on the kind of reference
             */

            InterfaceReference interfaceReference = injection.getResource().as(InterfaceReference.class);
            MessageReference messageReference = injection.getResource().as(MessageReference.class);
            FieldInterceptor interceptor = null;
            try {

                if (interfaceReference != null)
                    interceptor = new InterfaceInjectionManager(getFactory(), getInstanceManager(), injection);

                if (messageReference != null)
                    interceptor = new MessageInjectionManager(getFactory(), getInstanceManager(), injection);

                if (interceptor == null)
                    continue;

            } catch (ClassNotFoundException error) {
                throw new ConfigurationException("error injecting dependency " + injection.getName() + " :"
                        + error.getLocalizedMessage());
            }

            if (injection instanceof DependencyInjection.Field) {
                FieldMetadata field = getPojoMetadata().getField(injection.getName());
                if (field != null)
                    getInstanceManager().register(field, interceptor);
            }
        }

        /*
         * Load Bind Unbind callback into the ApformInstanceImpl
         */
        Map<String, Set<Callback>> callbackBind 	= new HashMap<String, Set<Callback>>();
        Map<String, Set<Callback>> callbackUnbind 	= new HashMap<String, Set<Callback>>();

        for (DependencyDeclaration dependencyDeclaration : primitive.getDependencies()) {
        	callbackBind.put(dependencyDeclaration.getIdentifier(),loadBindCallback(primitive, dependencyDeclaration, CallbackTrigger.Bind));
        	callbackUnbind.put(dependencyDeclaration.getIdentifier(),loadBindCallback(primitive, dependencyDeclaration, CallbackTrigger.Unbind));
        }
        
        getInstanceManager().addCallbackDependency(CallbackTrigger.Bind, callbackBind);
        getInstanceManager().addCallbackDependency(CallbackTrigger.Unbind, callbackUnbind);
    }

    private Set<Callback> loadBindCallback(AtomicImplementationDeclaration primitive,
            DependencyDeclaration dependencyDeclaration,
            CallbackTrigger trigger) {
        
        Set<CallbackMethod> callbackMethods = dependencyDeclaration.getCallback(trigger);
        Set<Callback> callbacks = new HashSet<Callback>();
        if (callbackMethods != null) {
            for (CallbackMethod callbackMethod : callbackMethods) {
                try {
                    Set<MethodMetadata> methodMetadatas = (Set<MethodMetadata>) primitive.getInstrumentation()
                            .getCallbacks(
                                    callbackMethod.getMethodName(), true);
                    for (MethodMetadata methodMetadata : methodMetadatas) {
                        callbacks.add(new Callback(methodMetadata, getInstanceManager()));
                    }
                } catch (NoSuchMethodException e) {
                    System.err.println("binding callback failure, when trigger : " + trigger + " " + e.getMessage());
                }
            }
        }
        return callbacks;
       
    }

    /**
     * The description of this handler instance
     * 
     */
    private static class Description extends HandlerDescription {

        private final DependencyInjectionHandler dependencyHandler;

        public Description(DependencyInjectionHandler dependencyHandler) {
            super(dependencyHandler);
            this.dependencyHandler = dependencyHandler;
        }

        @Override
        public Element getHandlerInfo() {
            Element root = super.getHandlerInfo();

            if (dependencyHandler.getInstanceManager() instanceof ApformInstanceImpl) {
                ApformInstanceImpl instance = (ApformInstanceImpl) dependencyHandler.getInstanceManager();
                for (DependencyInjectionManager dependency : instance.getInjections()) {
                    root.addElement(dependency.getDescription());
                }
            }
            return root;
        }

    }

    @Override
    public HandlerDescription getDescription() {
        return new Description(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String toString() {
        return "APAM Injection manager for "
                + getInstanceManager().getInstanceName();
    }

}
